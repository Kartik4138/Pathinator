from pydantic import BaseModel, Field
from datetime import datetime, date
from typing import List, Optional

class UserCreate(BaseModel):
    username: str
    password: str


class UserOut(BaseModel):
    id: int
    username: str

    class Config:
        orm_mode = True

class Token(BaseModel):
    access_token: str
    refresh_token: str
    token_type: str = "bearer"


class RefreshTokenOut(BaseModel):
    user_id: int
    token: str
    revoked: bool

    class Config:
        orm_mode = True

class SessionCreate(BaseModel):
    name: str


class SessionBase(BaseModel):
    id: int
    name: str
    created_at: datetime

    class Config:
        orm_mode = True


class SessionOut(BaseModel):
    id: int
    name: str
    created_at: datetime

    class Config:
        orm_mode = True

class PathPointCreate(BaseModel):
    latitude: float
    longitude: float


class PathPointOut(BaseModel):
    latitude: float
    longitude: float

    class Config:
        orm_mode = True


class SessionWithPaths(SessionBase):
    path_points: List[PathPointOut] = Field(default_factory=list)


class SessionPathOut(BaseModel):
    name: str
    points: List[PathPointOut]

    class Config:
        orm_mode = True  

class RefreshRequest(BaseModel):
    refresh_token: str