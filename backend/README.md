# Backend (FastAPI)

Python code that loads Tube data from the TfL Unified API into PostgreSQL and answers questions about routes.

## Files

- `schema.sql` creates the `stations` and `route_sequences` tables.
- `db.py` opens the database connection that the other files share.
- `load_routes.py` saves every Tube line's stations and route order. Each branch is stored as its own route.
- `stops_between.py` finds the stops between two stations on the same line, in travel order.
- `journey.py` plans journeys that change lines, preferring fewer changes and then fewer stops. It also knows a few short walks between separate stations, such as Bank and Monument.
- `log_status.py` saves a snapshot of every line's live status to the `status_log` table, building up disruption data for the notebooks.
- `api.py` serves the same data over HTTP.
- `test_stops_between.py` and `test_journey.py` have the tests.

## Setup

You need PostgreSQL 18 and a TfL API key. The `.env` file in the project root should contain:

```
TFL_APP_KEY=your-tfl-key
DB_PASSWORD=your-postgres-password
```

Then, from this folder:

```
python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt
createdb -U postgres tube_alarm
psql -U postgres -d tube_alarm -f schema.sql
python load_routes.py
```

`load_routes.py` can be run again at any time to refresh the data.

To collect disruption data, run `python log_status.py` regularly. For example, this `crontab -e` line saves a snapshot every 15 minutes:

```
*/15 * * * * cd /path/to/tube-alarm/backend && venv/bin/python log_status.py
```

## Running

```
python -m pytest
uvicorn api:app --reload
```

The API then runs at http://localhost:8000. Open http://localhost:8000/docs to try it in the browser.

- `GET /stations?q=hammersmith` lists matching stations.
- `GET /stops?start=Stockwell&end=Green Park` lists the stops in order. It returns 404 if a name is unclear or the journey needs a change of line.
- `GET /journey?start=Brixton&end=Bank` plans a journey across lines and returns one leg per line, a rough time in minutes (from station distances, see `estimate.py`), plus warnings for any of those lines that are disrupted right now.
- `GET /status` shows the live status of every Tube line.
- `GET /progress?start=Brixton&end=Bank&current=London Bridge` says how many stops and roughly how many minutes are left, and whether to wake the passenger. The alarm goes off one stop before each change and before the destination, or earlier with `wake_before=2`. It also lists live disruptions on the lines still ahead, like `/journey` does, so the app can warn about trouble that starts mid-journey.
- `GET /nearest?lat=51.5079&lon=-0.1390` lists the closest stations with their distance in metres.
