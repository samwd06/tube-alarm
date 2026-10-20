import os
from pathlib import Path

import psycopg
from dotenv import load_dotenv

# The .env file is in the project root, one folder up from this file
load_dotenv(Path(__file__).resolve().parent.parent / ".env")

conn = psycopg.connect(
    dbname="tube_alarm",
    user="postgres",
    password=os.getenv("DB_PASSWORD"),
    host="localhost",
)
