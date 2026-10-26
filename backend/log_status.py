from datetime import datetime, timezone

import requests

from db import conn
from status import line_statuses

# Every line gets the same timestamp, so one run is easy to pick out later
checked_at = datetime.now(timezone.utc).replace(microsecond=0)
try:
    statuses = line_statuses()
except requests.RequestException as error:
    # The full error has the request URL in it, app key and all, and this ends up in a log file.
    # It mostly happens when the computer is half asleep with no internet, so a short line will do
    print(f"Nothing saved at {checked_at:%H:%M} UTC, couldn't reach TfL ({type(error).__name__})")
    raise SystemExit(1)

with conn:
    for line, info in statuses.items():
        conn.execute(
            """
            INSERT INTO status_log (checked_at, line, good, status, reasons)
            VALUES (%s, %s, %s, %s, %s)
            """,
            (checked_at, line, info["good"], info["status"], " | ".join(info["reasons"]) or None),
        )

disrupted = [line for line, info in statuses.items() if not info["good"]]
print(f"Saved {len(statuses)} lines at {checked_at:%H:%M} UTC, {len(disrupted)} disrupted: {', '.join(disrupted) or 'none'}")
