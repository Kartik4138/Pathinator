from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.future import select
from jose import JWTError, jwt
from datetime import datetime
from typing import List
from sqlalchemy.orm import selectinload

from database import AsyncSessionLocal
from models import User, Session as PathSession, PathPoint
from schemas import PathPointCreate, SessionCreate, SessionOut, SessionPathOut, PathPointOut
from fastapi.security import OAuth2PasswordBearer

SECRET_KEY = "supersecretkey123"
ALGORITHM = "HS256"

router = APIRouter(prefix="/sessions", tags=["Sessions"])
oauth2_scheme = OAuth2PasswordBearer(tokenUrl="login")


async def get_db():
    async with AsyncSessionLocal() as session:
        yield session


async def get_current_user(token: str = Depends(oauth2_scheme), db: AsyncSession = Depends(get_db)):
    try:
        payload = jwt.decode(token, SECRET_KEY, algorithms=[ALGORITHM])
        username: str = payload.get("sub")
        if username is None:
            raise HTTPException(status_code=401, detail="Invalid token")    
    except JWTError:
        raise HTTPException(status_code=401, detail="Invalid token")

    result = await db.execute(select(User).where(User.username == username))
    user = result.scalars().first()
    
    if not user:
        raise HTTPException(status_code=401, detail="User not found")
    return user


@router.post("/create")
async def start_session(session: SessionCreate, token: str = Depends(oauth2_scheme), db: AsyncSession = Depends(get_db)):
    user = await get_current_user(token, db)

    result = await db.execute(
        select(PathSession).where(PathSession.user_id == user.id, PathSession.is_active == True)
    )
    active_session = result.scalars().first()
    if active_session:
        raise HTTPException(status_code=400, detail="Session already active")

    new_session = PathSession(user_id=user.id, name=session.name)
    db.add(new_session)
    await db.commit()
    await db.refresh(new_session)

    return {"name": new_session.name, "date": new_session.created_at.date().isoformat()}


@router.post("/stop")
async def stop_session(token: str = Depends(oauth2_scheme), db: AsyncSession = Depends(get_db)):
    user = await get_current_user(token, db)

    result = await db.execute(
        select(PathSession).where(PathSession.user_id == user.id, PathSession.is_active == True)
    )
    active_session = result.scalars().first()
    if not active_session:
        raise HTTPException(status_code=400, detail="No active session")

    active_session.is_active = False
    await db.commit()
    return {"message": "Session stopped", "session_id": active_session.id}


@router.post("/{sessionName}/add_point")
async def add_point(sessionName: str, point: PathPointCreate, token: str = Depends(oauth2_scheme), db: AsyncSession = Depends(get_db)):
    user = await get_current_user(token, db)

    result = await db.execute(
        select(PathSession).where(
            PathSession.user_id == user.id,
            PathSession.is_active == True,
            PathSession.name == sessionName
        )
    )
    active_session = result.scalars().first()
    if not active_session:
        raise HTTPException(status_code=400, detail="No active session")

    new_point = PathPoint(
    latitude=point.latitude,
    longitude=point.longitude,
    timestamp=datetime.now(),
    session_id=active_session.id
    )
    db.add(new_point)
    await db.commit()
    await db.refresh(new_point)

    return {"lat": new_point.latitude, "lng": new_point.longitude}

@router.get("/{sessionName}/path", response_model=List[PathPointOut])
async def get_session_path(sessionName: str, db: AsyncSession = Depends(get_db), token: str = Depends(oauth2_scheme)):
    user = await get_current_user(token, db)
    result = await db.execute(
        select(PathSession)
        .options(selectinload(PathSession.path_points))
        .where(PathSession.name == sessionName, PathSession.user_id == user.id)
    )
    session = result.scalars().first()
    
    if not session:
        raise HTTPException(status_code=404, detail="Session not found")

    return [
        PathPointOut(
            id=point.id,
            latitude=point.latitude,
            longitude=point.longitude,
            timestamp=point.timestamp
        )
        for point in session.path_points
    ]



@router.get("/get_all", response_model=List[SessionOut])
async def list_sessions(token: str = Depends(oauth2_scheme), db: AsyncSession = Depends(get_db)):
    user = await get_current_user(token, db)

    result = await db.execute(select(PathSession).where(PathSession.user_id == user.id))
    sessions = result.scalars().all()

    return [SessionOut(id=s.id, name=s.name, created_at=s.created_at.date()) for s in sessions]
