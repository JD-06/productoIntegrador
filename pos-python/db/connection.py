import os
import psycopg2
from psycopg2.extras import RealDictCursor
from pathlib import Path
from dotenv import load_dotenv
from urllib.parse import urlparse, parse_qs


class DB:
    _conn = None
    _kwargs: dict = {}
    IMAGES_PATH: str = ""
    JSON_PATH: str = ""

    @classmethod
    def init(cls):
        # search for .env walking up from this file
        candidates = [
            Path(__file__).resolve().parent.parent.parent / "puntoDeVenta" / "pos-ui" / ".env",
            Path(__file__).resolve().parent.parent / "puntoDeVenta" / "pos-ui" / ".env",
        ]
        # also walk up looking for .env
        here = Path(__file__).resolve().parent
        for _ in range(8):
            candidates.append(here / ".env")
            here = here.parent

        found = False
        for candidate in candidates:
            if candidate.exists():
                load_dotenv(candidate)
                found = True
                break
        if not found:
            load_dotenv()

        db_url = os.getenv("DB_URL", "")
        user   = os.getenv("DB_USER", "")
        pw     = os.getenv("DB_PASSWORD", "")
        cls.IMAGES_PATH = os.getenv("IMAGES_PATH", "")
        cls.JSON_PATH   = os.getenv("JSON_PATH", "")

        url = db_url.replace("jdbc:postgresql://", "postgresql://")
        p = urlparse(url)
        qs = parse_qs(p.query)
        cls._kwargs = {
            "host": p.hostname,
            "port": p.port or 5432,
            "dbname": p.path.lstrip("/"),
            "user": user,
            "password": pw,
        }
        if "disable" in qs.get("sslmode", []):
            cls._kwargs["sslmode"] = "disable"

    @classmethod
    def conn(cls):
        if cls._conn is None or cls._conn.closed:
            cls._conn = psycopg2.connect(**cls._kwargs)
            cls._conn.autocommit = False
        return cls._conn

    @classmethod
    def cursor(cls):
        return cls.conn().cursor(cursor_factory=RealDictCursor)

    @classmethod
    def fetchall(cls, sql, params=None):
        with cls.cursor() as cur:
            cur.execute(sql, params)
            return [dict(r) for r in cur.fetchall()]

    @classmethod
    def fetchone(cls, sql, params=None):
        with cls.cursor() as cur:
            cur.execute(sql, params)
            row = cur.fetchone()
            return dict(row) if row else None

    @classmethod
    def execute(cls, sql, params=None):
        with cls.cursor() as cur:
            cur.execute(sql, params)
            cls.conn().commit()
            try:
                return cur.fetchone()
            except Exception:
                return None

    @classmethod
    def execute_returning(cls, sql, params=None):
        with cls.cursor() as cur:
            cur.execute(sql, params)
            cls.conn().commit()
            row = cur.fetchone()
            return dict(row) if row else None

    @classmethod
    def commit(cls):
        cls.conn().commit()

    @classmethod
    def rollback(cls):
        cls.conn().rollback()
