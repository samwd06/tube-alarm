# Tube Alarm

An app that stops train passengers sleeping past their stop. It tracks progress along a TfL route and sounds an alarm shortly before the destination, and it watches live disruptions that might mean getting off earlier or later. This is a portfolio project for a 2nd year computer science student applying for placements.

## Stack

- Backend: Python (FastAPI later), in `backend/`
- Database: PostgreSQL 18 (no PostGIS yet)
- Mobile app: Android (Kotlin), in `android/` (not started)
- Data science: pandas and scikit-learn notebooks, in `notebooks/`
- Data source: TfL Unified API (api.tfl.gov.uk)

## Setup notes

- The TfL key is in `.env` in the project root as `TFL_APP_KEY`. Never print it, commit it or hard-code it.
- The Python virtual environment is `backend/venv`. Activate it with `source venv/bin/activate` from `backend/`, and run scripts with `python <file>`.
- The user is on macOS and works in VS Code's terminal.

## Progress

- Done: issue #1. `backend/fetch_stations.py` fetches the Victoria line route and prints its 16 stations in order.
- Next: issue #2. Create a Postgres database and a `stations` table (id, name, lat, lon), then insert the stations from the script.
- Then: issue #3. List the stops between two stations, using `stopPointSequences` for the travel order.
- Later: add a `route_sequences` table (line, direction, position, station id), and expand from Victoria to all Tube lines, handling branches.

## How to work with me

- I'm a beginner. Keep code and workflow simple, human-like and easy to follow, but concise and academic in tone.
- Explain what each command or block of code does, in plain words.
- Use brief comments only where they help with step-by-step understanding. Don't add many comments.
- No emojis.
- Work in small steps, and let me run things myself so I learn.
