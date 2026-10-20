import requests
from fastapi import FastAPI, HTTPException

from alarm import progress
from estimate import journey_minutes
from journey import plan_journey
from nearest import nearest_stations
from status import line_statuses, warnings_for
from stops_between import find_stations, stops_between

app = FastAPI(title="Tube Alarm")


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

    # The journey is still useful without live data, so a TfL outage only drops the warnings
    try:
        statuses = line_statuses()
    except requests.RequestException:
        statuses = {}

    lines = [leg["line"] for leg in legs if leg["line"]]
    return {
        "start": start,
        "end": end,
        "minutes": journey_minutes(legs),
        "legs": legs,
        "warnings": warnings_for(lines, statuses),
    }


@app.get("/progress")
def journey_progress(start: str, end: str, current: str, wake_before: int = 1):
    try:
        return progress(plan_journey(start, end), current, wake_before)
    except ValueError as error:
        raise HTTPException(status_code=404, detail=str(error))
