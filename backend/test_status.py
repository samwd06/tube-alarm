from status import parse_statuses, warnings_for


def status(severity, description, reason=None):
    return {"statusSeverity": severity, "statusSeverityDescription": description, "reason": reason}


# A cut-down copy of what TfL sends back
SAMPLE = [
    {"id": "victoria", "lineStatuses": [status(10, "Good Service")]},
    {"id": "central", "lineStatuses": [status(9, "Minor Delays", "Central Line: Minor delays due to train cancellations.")]},
    {"id": "district", "lineStatuses": [
        status(5, "Part Closure", "No service between Upminster and Barking."),
        status(9, "Minor Delays", "Minor delays on the rest of the line."),
    ]},
]


def test_good_service():
    assert parse_statuses(SAMPLE)["victoria"] == {
        "good": True, "serious": False, "status": "Good Service", "reasons": [], "problems": [],
    }


def test_minor_delays_are_not_serious():
    assert parse_statuses(SAMPLE)["central"]["serious"] is False


def test_a_part_closure_is_serious():
    assert parse_statuses(SAMPLE)["district"]["serious"] is True


def test_delays_keep_the_reason():
    central = parse_statuses(SAMPLE)["central"]
    assert central["good"] is False
    assert central["status"] == "Minor Delays"
    assert central["reasons"] == ["Central Line: Minor delays due to train cancellations."]


def test_more_than_one_status():
    district = parse_statuses(SAMPLE)["district"]
    assert district["good"] is False
    assert district["status"] == "Part Closure, Minor Delays"
    assert len(district["reasons"]) == 2


def test_repeated_status_only_shows_once():
    circle = [{"id": "circle", "lineStatuses": [
        status(3, "Part Suspended", "No service between Edgware Road and Aldgate."),
        status(3, "Part Suspended", "No service between Hammersmith and Paddington."),
    ]}]
    result = parse_statuses(circle)["circle"]
    assert result["status"] == "Part Suspended"
    assert len(result["reasons"]) == 2


def leg(line, *stops):
    return {"line": line, "stops": list(stops)}


def test_warnings_only_for_lines_on_the_journey():
    legs = [leg("victoria", "Brixton", "Stockwell"), leg("central", "Bank", "Liverpool Street")]
    warnings = warnings_for(legs, parse_statuses(SAMPLE))
    assert [w["line"] for w in warnings] == ["central"]


def test_no_warnings_without_live_data():
    assert warnings_for([leg("central", "Bank", "Liverpool Street")], {}) == []


def piccadilly_closed():
    # With detail=true, TfL lists the stations a part closure covers, including the ones at each end
    closure = status(5, "Part Closure", "No service between Cockfosters and Uxbridge.")
    closure["disruption"] = {"affectedStops": [
        {"commonName": "Barons Court Underground Station"},
        {"commonName": "Hammersmith (Dist&Picc Line) Underground Station"},
        {"commonName": "Acton Town Underground Station"},
    ]}
    return parse_statuses([{"id": "piccadilly", "lineStatuses": [closure]}])


def test_part_closure_keeps_its_stations():
    stops = piccadilly_closed()["piccadilly"]["problems"][0]["stops"]
    assert stops == ["Barons Court", "Hammersmith (Dist&Picc Line)", "Acton Town"]


def test_whole_line_problems_have_no_stations():
    assert parse_statuses(SAMPLE)["central"]["problems"][0]["stops"] == []


def test_warns_about_a_part_closure_on_the_way():
    legs = [leg("piccadilly", "Barons Court", "Hammersmith (Dist&Picc Line)", "Acton Town")]
    assert [w["line"] for w in warnings_for(legs, piccadilly_closed())] == ["piccadilly"]


def test_starting_at_the_end_of_a_closure_is_fine():
    # Trains still run from Acton Town towards Heathrow
    legs = [leg("piccadilly", "Acton Town", "South Ealing", "Northfields")]
    assert warnings_for(legs, piccadilly_closed()) == []


def test_one_warning_per_line():
    legs = [leg("central", "Bank", "Liverpool Street"), leg("walk", "Liverpool Street"), leg("central", "Stratford", "Leyton")]
    assert len(warnings_for(legs, parse_statuses(SAMPLE))) == 1
