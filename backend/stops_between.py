import os
import psycopg
from dotenv import load_dotenv

load_dotenv("../.env")

conn = psycopg.connect(
    dbname="tube_alarm",
    user="postgres",
    password=os.getenv("DB_PASSWORD"),
    host="localhost",
)

# Station names without " Underground Station", so they are easy to type
SHORT_NAME = "REPLACE(REPLACE(name, ' Underground Station', ''), '-Underground', '')"


def find_stations(text):
    rows = conn.execute(
        f"SELECT id, {SHORT_NAME} FROM stations WHERE {SHORT_NAME} ILIKE %s ORDER BY 2",
        (f"%{text}%",),
    ).fetchall()

    # An exact name wins, so "Paddington" doesn't also match "Paddington (H&C Line)"
    exact = [row for row in rows if row[1].lower() == text.lower()]
    return exact or rows


def pick_station(text):
    matches = find_stations(text)
    if not matches:
        raise ValueError(f"No station called {text}")
    if len(matches) > 1:
        names = ", ".join(name for _, name in matches)
        raise ValueError(f"{text} could mean: {names}")
    return matches[0][0]


def stops_between(start, end):
    start_id = pick_station(start)
    end_id = pick_station(end)

    # Find a route that reaches start before end, and keep the shortest journey
    match = conn.execute(
        """
        SELECT a.line, a.direction, a.route, a.position, b.position
        FROM route_sequences a
        JOIN route_sequences b
            ON a.line = b.line AND a.direction = b.direction AND a.route = b.route
        WHERE a.station_id = %s AND b.station_id = %s AND a.position <= b.position
        ORDER BY b.position - a.position
        LIMIT 1
        """,
        (start_id, end_id),
    ).fetchone()

    if match is None:
        raise ValueError(f"No single line goes from {start} to {end}")

    line, direction, route, first, last = match
    rows = conn.execute(
        f"""
        SELECT {SHORT_NAME}
        FROM route_sequences
        JOIN stations ON stations.id = route_sequences.station_id
        WHERE line = %s AND direction = %s AND route = %s
            AND position BETWEEN %s AND %s
        ORDER BY position
        """,
        (line, direction, route, first, last),
    ).fetchall()
    return [name for (name,) in rows]


if __name__ == "__main__":
    for name in stops_between("Stockwell", "Green Park"):
        print(name)
