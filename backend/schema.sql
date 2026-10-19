-- One row per station, using the TfL station id
CREATE TABLE IF NOT EXISTS stations (
    id   TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    lat  DOUBLE PRECISION NOT NULL,
    lon  DOUBLE PRECISION NOT NULL
);

-- The order of stations along each route, one row per stop
CREATE TABLE IF NOT EXISTS route_sequences (
    line       TEXT    NOT NULL,
    direction  TEXT    NOT NULL,
    route      INTEGER NOT NULL,
    position   INTEGER NOT NULL,
    station_id TEXT    NOT NULL REFERENCES stations (id),
    PRIMARY KEY (line, direction, route, position)
);
