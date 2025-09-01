from fastapi import APIRouter, Depends, HTTPException
from datetime import datetime, timedelta
from jose import JWTError, jwt
from fastapi.security import OAuth2PasswordBearer
from sqlalchemy.ext.asyncio import AsyncSession

from database import AsyncSessionLocal
from models import User , Session as UserSession
from schemas import UserCreate, UserOut, Token
from utils import hash_password, verify_password
from dotenv import load_dotenv
from sqlalchemy.future import select

SECRET_KEY = "supersecretkey123"
ALGORITHM = "HS256"
ACCESS_TOKEN_EXPIRE_MINUTES = 30

router = APIRouter(prefix="/auth", tags=["Auth"])

oauth2_scheme = OAuth2PasswordBearer(tokenUrl="auth/login")

async def get_db():
    async with AsyncSessionLocal() as session:
        yield session

def create_access_token(data: dict, expires_delta: timedelta | None = None):
    to_encode = data.copy()
    expire = datetime.utcnow() + (expires_delta or timedelta(minutes=ACCESS_TOKEN_EXPIRE_MINUTES))
    to_encode.update({"exp": expire})
    return jwt.encode(to_encode, SECRET_KEY, algorithm=ALGORITHM)

def create_refresh_token(data: dict, expires_delta: timedelta | None = None):
    to_encode = data.copy()
    expire = datetime.utcnow() + (expires_delta or timedelta(days=7))
    to_encode.update({"exp": expire})
    return jwt.encode(to_encode, SECRET_KEY, algorithm=ALGORITHM)

@router.post("/register", response_model=UserOut)
async def register(user: UserCreate, db: AsyncSession = Depends(get_db)):
    result = await db.execute(select(User).where(User.username == user.username))
    db_user = result.scalars().first()

    if db_user:
        raise HTTPException(status_code=400, detail="Username already registered")

    new_user = User(
        username=user.username,
        hashed_password=hash_password(user.password)
    )
    db.add(new_user)
    await db.commit()
    await db.refresh(new_user)
    return new_user

@router.post("/login", response_model=Token)
async def login(user: UserCreate, db: AsyncSession = Depends(get_db)):
    result = await db.execute(select(User).where(User.username == user.username))
    db_user = result.scalars().first()

    if not db_user or not verify_password(user.password, db_user.hashed_password):
        raise HTTPException(status_code=401, detail="Invalid Credentials")

    token_data = {"sub": db_user.username, "user_id": db_user.id}
    access_token = create_access_token(token_data)
    refresh_token = create_refresh_token(token_data)

    db_user.refresh_token = refresh_token
    db_user.expires_at = datetime.utcnow() + timedelta(days=7)
    db_user.revoked = False

    db.add(db_user)
    await db.commit()
    await db.refresh(db_user)

    return {
        "access_token": access_token,
        "refresh_token": refresh_token,
        "token_type": "bearer"
    }



from pydantic import BaseModel

class RefreshRequest(BaseModel):
    refresh_token: str

@router.post("/refresh", response_model=Token)
async def refresh_token(request: RefreshRequest, db: AsyncSession = Depends(get_db)):
    refresh_token = request.refresh_token
    try:
        payload = jwt.decode(refresh_token, SECRET_KEY, algorithms=[ALGORITHM])
        user_id: int = payload.get("user_id")
        if user_id is None:
            raise HTTPException(status_code=401, detail="Invalid refresh token")
    except JWTError:
        raise HTTPException(status_code=401, detail="Invalid refresh token")

    result = await db.execute(
        select(User).where(
            User.refresh_token == refresh_token,
            User.revoked == False,
            User.expires_at > datetime.utcnow()
        )
    )
    db_token = result.scalars().first()

    if not db_token:
        raise HTTPException(status_code=401, detail="Refresh token expired or revoked")

    new_access_token = create_access_token({"sub": payload["sub"], "user_id": user_id})

    return {
        "access_token": new_access_token,
        "refresh_token": refresh_token,
        "token_type": "bearer"
    }


@router.post("/logout")
async def logout(db: AsyncSession = Depends(get_db), token: str = Depends(oauth2_scheme)):
    try:
        payload = jwt.decode(token, SECRET_KEY, algorithms=[ALGORITHM])
        user_id: int = payload.get("user_id")
        if user_id is None:
            raise HTTPException(status_code=401, detail="Invalid token")
    except JWTError:
        raise HTTPException(status_code=401, detail="Invalid token")

    await db.execute(
        User.__table__.update()
        .where(User.id == user_id)
        .values(refresh_token=None, revoked=True, expires_at=None)
    )
    await db.commit()

    return {"msg": "Successfully logged out"}

