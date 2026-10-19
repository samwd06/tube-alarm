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


def stops_between(start, end):
    # Find a route that reaches start before end, and keep the shortest journey
    match = conn.execute(
        f"""
        SELECT a.line, a.direction, a.route, a.position, b.position
        FROM route_sequences a
        JOIN route_sequences b
            ON a.line = b.line AND a.direction = b.direction AND a.route = b.route
        JOIN stations sa ON sa.id = a.station_id
        JOIN stations sb ON sb.id = b.station_id
        WHERE {SHORT_NAME.replace('name', 'sa.name')} = %s
            AND {SHORT_NAME.replace('name', 'sb.name')} = %s
            AND a.position <= b.position
        ORDER BY b.position - a.position
        LIMIT 1
        """,
        (start, end),
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
