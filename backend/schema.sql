-- One row per station, using the TfL station id
CREATE TABLE IF NOT EXISTS stations (
    id   TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    lat  DOUBLE PRECISION NOT NULL,
    lon  DOUBLE PRECISION NOT NULL
);
