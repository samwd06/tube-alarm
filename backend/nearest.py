import math

from stops_between import SHORT_NAME, conn

EARTH_RADIUS_M = 6_371_000


def distance_m(lat1, lon1, lat2, lon2):
    # Haversine formula: distance along the Earth's surface between two points
    lat1, lon1, lat2, lon2 = map(math.radians, (lat1, lon1, lat2, lon2))
    a = (math.sin((lat2 - lat1) / 2) ** 2
         + math.cos(lat1) * math.cos(lat2) * math.sin((lon2 - lon1) / 2) ** 2)
    return 2 * EARTH_RADIUS_M * math.asin(math.sqrt(a))


def nearest_stations(lat, lon, limit=3):
    rows = conn.execute(f"SELECT {SHORT_NAME}, lat, lon FROM stations").fetchall()
    rows.sort(key=lambda row: distance_m(lat, lon, row[1], row[2]))
    return [
        {"name": name, "metres": round(distance_m(lat, lon, station_lat, station_lon))}
        for name, station_lat, station_lon in rows[:limit]
    ]


if __name__ == "__main__":
    # Somewhere on Piccadilly, between Green Park and Piccadilly Circus
    for station in nearest_stations(51.5079, -0.1390):
        print(station["name"], station["metres"], "m")
