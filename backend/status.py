import os
import requests
from dotenv import load_dotenv

load_dotenv("../.env")
api_key = os.getenv("TFL_APP_KEY")

GOOD_SERVICE = 10
# TfL's codes for a line that's closed, suspended or badly delayed, at least in part.
# Minor delays (9) and reduced service (7) aren't worth changing route for
SERIOUS = {1, 2, 3, 4, 5, 6, 11, 16, 20}


def parse_statuses(data):
    lines = {}
    for line in data:
        # A line can have more than one status at a time, e.g. part closure and minor delays
        statuses = line["lineStatuses"]
        lines[line["id"]] = {
            "good": all(s["statusSeverity"] == GOOD_SERVICE for s in statuses),
            "serious": any(s["statusSeverity"] in SERIOUS for s in statuses),
            # dict.fromkeys drops repeats but keeps the order, so "Part Suspended" only shows once
            "status": ", ".join(dict.fromkeys(s["statusSeverityDescription"] for s in statuses)),
            "reasons": [s["reason"] for s in statuses if s.get("reason")],
        }
    return lines


def warnings_for(lines, statuses):
    return [
        {"line": line, "status": statuses[line]["status"], "reasons": statuses[line]["reasons"]}
        for line in lines
        if line in statuses and not statuses[line]["good"]
    ]


def line_statuses():
    url = "https://api.tfl.gov.uk/Line/Mode/tube/Status"
    # Don't let a slow TfL response hold up the whole request
    response = requests.get(url, params={"app_key": api_key}, timeout=5)
    response.raise_for_status()
    return parse_statuses(response.json())


if __name__ == "__main__":
    for line, info in line_statuses().items():
        print(line, "-", info["status"])
