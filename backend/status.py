import os
import requests
from dotenv import load_dotenv

load_dotenv("../.env")
api_key = os.getenv("TFL_APP_KEY")

GOOD_SERVICE = 10


def parse_statuses(data):
    lines = {}
    for line in data:
        # A line can have more than one status at a time, e.g. part closure and minor delays
        statuses = line["lineStatuses"]
        lines[line["id"]] = {
            "good": all(s["statusSeverity"] == GOOD_SERVICE for s in statuses),
            "status": ", ".join(s["statusSeverityDescription"] for s in statuses),
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
