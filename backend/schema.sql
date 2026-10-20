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

-- Snapshots of each line's live status, saved by log_status.py for later analysis
CREATE TABLE IF NOT EXISTS status_log (
    checked_at TIMESTAMPTZ NOT NULL,
    line       TEXT        NOT NULL,
    good       BOOLEAN     NOT NULL,
    status     TEXT        NOT NULL,
    reasons    TEXT,
    PRIMARY KEY (checked_at, line)
);
