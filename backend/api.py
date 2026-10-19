from fastapi import FastAPI, HTTPException

from stops_between import find_stations, stops_between

app = FastAPI(title="Tube Alarm")


@app.get("/stations")
def stations(q: str):
    return [{"id": station_id, "name": name} for station_id, name in find_stations(q)]


@app.get("/stops")
def stops(start: str, end: str):
    try:
        return {"start": start, "end": end, "stops": stops_between(start, end)}
    except ValueError as error:
        raise HTTPException(status_code=404, detail=str(error))
