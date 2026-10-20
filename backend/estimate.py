from db import conn
from nearest import distance_m
from stops_between import SHORT_NAME

# Rough figures for now. Real timings could replace these later.
TRAIN_SPEED_KMH = 40
SECONDS_PER_STOP = 30
MINUTES_PER_CHANGE = 5
WALKING_SPEED_KMH = 5

coordinates = {
    name: (lat, lon)
    for name, lat, lon in conn.execute(f"SELECT {SHORT_NAME}, lat, lon FROM stations").fetchall()
}


def leg_minutes(stops, walking=False):
    metres = sum(distance_m(*coordinates[a], *coordinates[b]) for a, b in zip(stops, stops[1:]))
    if walking:
        return metres / 1000 / WALKING_SPEED_KMH * 60
    # The train only stops at the stations in between, not the first or last
    stops_in_between = max(len(stops) - 2, 0)
    return metres / 1000 / TRAIN_SPEED_KMH * 60 + stops_in_between * SECONDS_PER_STOP / 60


def journey_minutes(legs):
    total = sum(leg_minutes(leg["stops"], walking=leg["line"] == "walk") for leg in legs)
    # Waiting for the next train only happens between train legs
    trains = [leg for leg in legs if leg["line"] != "walk"]
    total += MINUTES_PER_CHANGE * max(len(trains) - 1, 0)
    return round(total)
