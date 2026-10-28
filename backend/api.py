import requests
from fastapi import FastAPI, HTTPException

from alarm import progress, remaining_legs
from estimate import journey_minutes
from journey import plan_journey
from nearest import nearest_stations
from status import line_statuses, problems_on, warnings_for
from stops_between import find_stations, stops_between

app = FastAPI(title="Tube Alarm")


def live_statuses():
    # A journey is still useful without live data, so a TfL outage only drops the warnings
    try:
        return line_statuses()
    except requests.RequestException:
        return {}


def reroute(current, end, legs, statuses, avoiding=()):
    # Only worth suggesting if a line still ahead is badly affected and there's a way round it
    serious = {leg["line"] for leg in legs if any(p["serious"] for p in problems_on(leg, statuses))}
    if not serious:
        return None
    # Keep away from lines the passenger is already avoiding, as well as the new ones
    avoid = serious | set(avoiding)
    try:
        new_legs = plan_journey(current, end, avoid=avoid)
    except ValueError:
        return None
    return {"avoid": sorted(avoid), "minutes": journey_minutes(new_legs), "legs": new_legs}



@app.get("/stations")
def stations(q: str):
    return [{"id": station_id, "name": name} for station_id, name in find_stations(q)]


@app.get("/nearest")
def nearest(lat: float, lon: float, limit: int = 3):
    return nearest_stations(lat, lon, limit)


@app.get("/stops")
def stops(start: str, end: str):
    try:
        return {"start": start, "end": end, "stops": stops_between(start, end)}
    except ValueError as error:
        raise HTTPException(status_code=404, detail=str(error))


@app.get("/status")
def status():
    try:
        return line_statuses()
    except requests.RequestException:
        raise HTTPException(status_code=503, detail="TfL is not responding, try again shortly")


@app.get("/journey")
def journey(start: str, end: str):
    try:
        legs = plan_journey(start, end)
    except ValueError as error:
        raise HTTPException(status_code=404, detail=str(error))

    return {
        "start": start,
        "end": end,
        "minutes": journey_minutes(legs),
        "legs": legs,
        "warnings": warnings_for(legs, live_statuses()),
    }


@app.get("/progress")
def journey_progress(start: str, end: str, current: str, wake_before: int = 1, avoid: str = ""):
    # After switching to a way round, the app sends where it started from and the lines to avoid,
    # so every check follows that same route rather than planning a new one from each station
    avoiding = {line for line in avoid.split(",") if line}
    try:
        legs = plan_journey(start, end, avoid=avoiding)
        result = progress(legs, current, wake_before)
        left = remaining_legs(legs, current)
    except ValueError as error:
        raise HTTPException(status_code=404, detail=str(error))
    result["minutes_left"] = journey_minutes(left)
    # Only the lines still ahead matter, so a delay on a line already left behind isn't shown
    statuses = live_statuses()
    result["warnings"] = warnings_for(left, statuses)
    result["reroute"] = reroute(current, end, left, statuses, avoiding)
    return result
