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
    assert parse_statuses(SAMPLE)["victoria"] == {"good": True, "status": "Good Service", "reasons": []}


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


def test_warnings_only_for_lines_on_the_journey():
    warnings = warnings_for(["victoria", "central"], parse_statuses(SAMPLE))
    assert [w["line"] for w in warnings] == ["central"]


def test_no_warnings_without_live_data():
    assert warnings_for(["central"], {}) == []
