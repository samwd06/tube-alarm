import os
import requests
from dotenv import load_dotenv

load_dotenv("../.env")
api_key = os.getenv("TFL_APP_KEY")

GOOD_SERVICE = 10
# TfL's codes for a line that's closed, suspended or badly delayed, at least in part.
# Minor delays (9) and reduced service (7) aren't worth changing route for
SERIOUS = {1, 2, 3, 4, 5, 6, 11, 16, 20}
# Part Suspended and Part Closure, the only ones that cover just some of the line
PART = {3, 5}


def short_name(name):
    # The same trimming the stations table gets, so the names match the journey's stops
    return name.replace(" Underground Station", "").replace("-Underground", "")


def affected_stops(status):
    # An empty list means the whole line
    if status["statusSeverity"] not in PART:
        return []
    stops = (status.get("disruption") or {}).get("affectedStops", [])
    return [short_name(stop["commonName"]) for stop in stops]


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
            "problems": [
                {
                    "serious": s["statusSeverity"] in SERIOUS,
                    "status": s["statusSeverityDescription"],
                    "reason": s.get("reason"),
                    "stops": affected_stops(s),
                }
                for s in statuses
                if s["statusSeverity"] != GOOD_SERVICE
            ],
        }
    return lines


def hits(problem, leg):
    # TfL counts the stations at each end of a closure as affected, even though trains still
    # run there. So a leg is only hit if it goes between two affected stations, not just to one
    stops = set(problem["stops"])
    if not stops:
        return True
    return any(a in stops and b in stops for a, b in zip(leg["stops"], leg["stops"][1:]))


def problems_on(leg, statuses):
    line = statuses.get(leg["line"])
    if not line:
        return []
    return [problem for problem in line["problems"] if hits(problem, leg)]


def warnings_for(legs, statuses):
    warnings = []
    for leg in legs:
        problems = problems_on(leg, statuses)
        if not problems or leg["line"] in [w["line"] for w in warnings]:
            continue
        warnings.append({
            "line": leg["line"],
            "status": ", ".join(dict.fromkeys(p["status"] for p in problems)),
            "reasons": [p["reason"] for p in problems if p["reason"]],
        })
    return warnings


def get(path, **params):
    # Don't let a slow TfL response hold up the whole request
    response = requests.get(f"https://api.tfl.gov.uk{path}", params={"app_key": api_key, **params}, timeout=5)
    response.raise_for_status()
    return response.json()


def line_statuses():
    data = get("/Line/Mode/tube/Status")
    # Only the detailed version says which stations a part closure covers. It's slow for every
    # line at once, so ask for it just for the lines that need it
    part = [line["id"] for line in data if any(s["statusSeverity"] in PART for s in line["lineStatuses"])]
    if part:
        detailed = {line["id"]: line for line in get(f"/Line/{','.join(part)}/Status", detail="true")}
        data = [detailed.get(line["id"], line) for line in data]
    return parse_statuses(data)


if __name__ == "__main__":
    for line, info in line_statuses().items():
        print(line, "-", info["status"])
