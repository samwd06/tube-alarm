def remaining_legs(legs, current):
    for i, leg in enumerate(legs):
        names = [stop.lower() for stop in leg["stops"]]
        if current.lower() not in names:
            continue
        here = names.index(current.lower())
        # At a change station, what's left starts with the next line, not the end of this one
        if here == len(names) - 1 and i < len(legs) - 1:
            continue
        return [{"line": leg["line"], "stops": leg["stops"][here:]}] + legs[i + 1:]
    raise ValueError(f"{current} is not on this journey")


def progress(legs, current, wake_before=1):
    # One list of every stop, with each change station only counted once
    stops = legs[0]["stops"] + [stop for leg in legs[1:] for stop in leg["stops"][1:]]
    lowered = [stop.lower() for stop in stops]
    if current.lower() not in lowered:
        raise ValueError(f"{current} is not on this journey")
    here = lowered.index(current.lower())

    # The passenger has to get off at every change and at the end, so the alarm cares about the next one
    get_off = [len(leg["stops"]) - 1 for leg in legs]
    for i in range(1, len(get_off)):
        get_off[i] += get_off[i - 1]
    next_off = next(i for i in get_off if i >= here)

    return {
        "current": stops[here],
        "stops_left": len(stops) - 1 - here,
        "next_stop_off": stops[next_off],
        "stops_to_next_stop_off": next_off - here,
        "wake_up": next_off - here <= wake_before,
    }
