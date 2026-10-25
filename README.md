# tube-alarm

An app that stops passengers sleeping past their stop: it tracks progress along a TfL route and sounds an alarm shortly before the destination, while watching live disruptions that might mean getting off earlier or later. It combines a real user problem, live APIs, a database and a genuine data science challenge.

## What works so far

The backend (Python, FastAPI, PostgreSQL) can:

- store every Tube line's stations and route order, including branches
- plan a journey between any two stations, changing lines where needed
- warn when a line on the journey has live disruptions
- work out how many stops are left and when to wake the passenger
- find the nearest stations to a GPS position

The Android app (Kotlin, Jetpack Compose) can:

- plan a journey and show each line, the stops and a rough time
- follow the journey by GPS once the alarm is started
- vibrate and show a "Wake up!" notification one stop before each change and the destination, even with the screen locked
- notify you if a line still ahead starts having problems during the journey

See [backend/README.md](backend/README.md) for the backend setup and API, and [android/README.md](android/README.md) for running the app.

## Layout

- `backend/` Python code, database schema and tests
- `android/` the Android app
- `notebooks/` data science notebooks
