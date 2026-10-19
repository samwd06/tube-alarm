import os
import requests
import psycopg
from dotenv import load_dotenv

load_dotenv("../.env")
api_key = os.getenv("TFL_APP_KEY")

# Ask TfL for every Tube line, e.g. "bakerloo", "victoria"
response = requests.get("https://api.tfl.gov.uk/Line/Mode/tube", params={"app_key": api_key})
response.raise_for_status()
LINES = [line["id"] for line in response.json()]

conn = psycopg.connect(
    dbname="tube_alarm",
    user="postgres",
    password=os.getenv("DB_PASSWORD"),
    host="localhost",
)

with conn:
    for line in LINES:
        for direction in ["outbound", "inbound"]:
            url = f"https://api.tfl.gov.uk/Line/{line}/Route/Sequence/{direction}"
            response = requests.get(url, params={"app_key": api_key})
            response.raise_for_status()
            data = response.json()

            # Stops in each sequence use the same ids as the ordered routes
            for sequence in data["stopPointSequences"]:
                for station in sequence["stopPoint"]:
                    conn.execute(
                        """
                        INSERT INTO stations (id, name, lat, lon)
                        VALUES (%s, %s, %s, %s)
                        ON CONFLICT (id) DO NOTHING
                        """,
                        (station["id"], station["name"], station["lat"], station["lon"]),
                    )

            # Remove the old order first, so the script can be rerun safely
            conn.execute(
                "DELETE FROM route_sequences WHERE line = %s AND direction = %s",
                (line, direction),
            )

            # Each ordered route is one full journey, so every branch gets its own route number
            for route, ordered in enumerate(data["orderedLineRoutes"]):
                for position, station_id in enumerate(ordered["naptanIds"]):
                    conn.execute(
                        """
                        INSERT INTO route_sequences (line, direction, route, position, station_id)
                        VALUES (%s, %s, %s, %s, %s)
                        """,
                        (line, direction, route, position, station_id),
                    )
        print(f"Saved {line}")
