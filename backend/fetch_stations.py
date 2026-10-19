import os
import requests
import psycopg
from dotenv import load_dotenv

load_dotenv("../.env")
api_key = os.getenv("TFL_APP_KEY")

url = "https://api.tfl.gov.uk/Line/victoria/Route/Sequence/outbound"
response = requests.get(url, params={"app_key": api_key})
response.raise_for_status()
stations = response.json()["stopPointSequences"][0]["stopPoint"]

# Open a connection to the local database
conn = psycopg.connect(
    dbname="tube_alarm",
    user="postgres",
    password=os.getenv("DB_PASSWORD"),
    host="localhost",
)

with conn:
    for station in stations:
        # Skip stations that are already saved, so the script can be rerun safely
        conn.execute(
            """
            INSERT INTO stations (id, name, lat, lon)
            VALUES (%s, %s, %s, %s)
            ON CONFLICT (id) DO NOTHING
            """,
            (station["id"], station["name"], station["lat"], station["lon"]),
        )

print(f"Saved {len(stations)} stations")