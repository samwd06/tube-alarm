import heapq

from db import conn
from stops_between import SHORT_NAME, pick_station

# A change of line counts the same as this many extra stops
CHANGE_COST = 5

# Separate stations that are a short, well-known walk apart
WALKS = [
    ("Bank", "Monument"),
    ("Paddington", "Paddington (H&C Line)"),
    ("Edgware Road (Bakerloo)", "Edgware Road (Circle Line)"),
    ("Hammersmith (Dist&Picc Line)", "Hammersmith (H&C Line)"),
    ("Euston", "Euston Square"),
    ("White City", "Wood Lane"),
]


def load_network():
    names = dict(conn.execute(f"SELECT id, {SHORT_NAME} FROM stations").fetchall())

    rows = conn.execute(
        """
        SELECT line, direction, route, station_id
        FROM route_sequences
        ORDER BY line, direction, route, position
        """
    ).fetchall()

    # For each station, the stations you can reach next and on which line
    next_stops = {}
    previous = None
    for line, direction, route, station_id in rows:
        if previous and previous[:3] == (line, direction, route):
            next_stops.setdefault(previous[3], set()).add((station_id, line))
        previous = (line, direction, route, station_id)

    # Walking works both ways, and counts as its own "line" so it shows up as a leg
    ids = {name: station_id for station_id, name in names.items()}
    for a, b in WALKS:
        next_stops.setdefault(ids[a], set()).add((ids[b], "walk"))
        next_stops.setdefault(ids[b], set()).add((ids[a], "walk"))
    return names, next_stops


names, next_stops = load_network()


def plan_journey(start, end, avoid=()):
    start_id = pick_station(start)
    end_id = pick_station(end)
    if start_id == end_id:
        return [{"line": None, "stops": [names[start_id]]}]

    # Dijkstra's algorithm, where each step remembers the line it was on
    queue = [(0, start_id, None, [start_id], [None])]
    done = set()
    while queue:
        cost, station, line, path, lines = heapq.heappop(queue)
        if station == end_id:
            return make_legs(path, lines)
        if (station, line) in done:
            continue
        done.add((station, line))

        for next_station, next_line in next_stops.get(station, ()):
            # Used to plan around a line that's badly disrupted
            if next_line in avoid:
                continue
            step = 1 if line in (None, next_line) else 1 + CHANGE_COST
            heapq.heappush(queue, (
                cost + step, next_station, next_line,
                path + [next_station], lines + [next_line],
            ))

    raise ValueError(f"No route found from {start} to {end}")


def make_legs(path, lines):
    # Split the path into one leg per line. A change station ends one leg and starts the next.
    legs = []
    for i in range(1, len(path)):
        if not legs or legs[-1]["line"] != lines[i]:
            legs.append({"line": lines[i], "stops": [names[path[i - 1]]]})
        legs[-1]["stops"].append(names[path[i]])
    return legs


if __name__ == "__main__":
    for leg in plan_journey("Brixton", "Bank"):
        print(leg["line"], ":", " -> ".join(leg["stops"]))
