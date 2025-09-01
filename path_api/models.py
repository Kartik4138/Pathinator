from sqlalchemy import Column, Integer, String, DateTime, Boolean, ForeignKey, Float
from sqlalchemy.orm import relationship
from datetime import datetime
from database import Base


class User(Base):
    __tablename__ = "users"

    id = Column(Integer, primary_key=True, index=True)
    username = Column(String, unique=True, index=True, nullable=False)
    hashed_password = Column(String, nullable=False)

    refresh_token = Column(String, unique=True, index=True, nullable=True)
    revoked = Column(Boolean, default=False)
    expires_at = Column(DateTime, nullable=True)

    
    sessions = relationship("Session", back_populates="user", cascade="all, delete-orphan")


class Session(Base):
    __tablename__ = "sessions"

    id = Column(Integer, primary_key=True, index=True)
    name = Column(String, nullable=False)
    created_at = Column(DateTime, default=datetime.utcnow)
    is_active = Column(Boolean, default=True)

    user_id = Column(Integer, ForeignKey("users.id"))
    user = relationship("User", back_populates="sessions")

    path_points = relationship("PathPoint", back_populates="session", cascade="all, delete-orphan")


class PathPoint(Base):
    __tablename__ = "path_points"

    id = Column(Integer, primary_key=True, index=True)
    latitude = Column(Float, nullable=False)
    longitude = Column(Float, nullable=False)
    timestamp = Column(DateTime, default=datetime.utcnow)

    session_id = Column(Integer, ForeignKey("sessions.id"))
    session = relationship("Session", back_populates="path_points")


