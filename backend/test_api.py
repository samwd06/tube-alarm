import requests
from fastapi.testclient import TestClient

import api

client = TestClient(api.app)


def line(status="Good Service", serious=False, reason=None, stops=()):
    # The shape parse_statuses gives back, with one problem at most
    good = status == "Good Service"
    problem = {"serious": serious, "status": status, "reason": reason, "stops": list(stops)}
    return {
        "good": good, "serious": serious, "status": status,
        "reasons": [reason] if reason else [], "problems": [] if good else [problem],
    }


def fake_statuses():
    return {
        "victoria": line("Minor Delays", reason="Signal failure at Brixton."),
        "northern": line(),
    }


def northern_suspended():
    statuses = fake_statuses()
    statuses["northern"] = line("Part Suspended", serious=True)
    return statuses


def victoria_suspended():
    statuses = fake_statuses()
    statuses["victoria"] = line("Suspended", serious=True)
    return statuses


def tfl_is_down():
    raise requests.ConnectionError("no connection")


def test_stations_search():
    response = client.get("/stations", params={"q": "hammersmith"})
    assert response.status_code == 200
    assert len(response.json()) == 2


def test_stops_on_one_line():
    response = client.get("/stops", params={"start": "Stockwell", "end": "Green Park"})
    assert response.json()["stops"][-1] == "Green Park"


def test_stops_needing_a_change_is_404():
    response = client.get("/stops", params={"start": "Brixton", "end": "Bank"})
    assert response.status_code == 404


def test_journey_warns_about_delays(monkeypatch):
    # Use made-up statuses so the test doesn't depend on today's real service
    monkeypatch.setattr(api, "line_statuses", fake_statuses)
    body = client.get("/journey", params={"start": "Brixton", "end": "Bank"}).json()
    assert [leg["line"] for leg in body["legs"]] == ["victoria", "northern"]
    assert [w["line"] for w in body["warnings"]] == ["victoria"]
    assert body["minutes"] > 0


def test_journey_still_works_when_tfl_is_down(monkeypatch):
    monkeypatch.setattr(api, "line_statuses", tfl_is_down)
    response = client.get("/journey", params={"start": "Brixton", "end": "Bank"})
    assert response.status_code == 200
    assert response.json()["warnings"] == []


def test_status_when_tfl_is_down(monkeypatch):
    monkeypatch.setattr(api, "line_statuses", tfl_is_down)
    assert client.get("/status").status_code == 503


def test_progress_near_the_end(monkeypatch):
    monkeypatch.setattr(api, "line_statuses", fake_statuses)
    params = {"start": "Brixton", "end": "Bank", "current": "London Bridge"}
    body = client.get("/progress", params=params).json()
    assert body["wake_up"] is True
    assert body["stops_left"] == 1


def test_nearest_needs_numbers():
    assert client.get("/nearest", params={"lat": "abc", "lon": "0"}).status_code == 422
    assert client.get("/nearest", params={"lat": 51.5079, "lon": -0.1390}).json()[0]["name"] == "Green Park"


def test_progress_warns_about_lines_still_ahead(monkeypatch):
    monkeypatch.setattr(api, "line_statuses", fake_statuses)
    params = {"start": "Brixton", "end": "Bank", "current": "Brixton"}
    body = client.get("/progress", params=params).json()
    assert [w["line"] for w in body["warnings"]] == ["victoria"]


def test_progress_forgets_lines_already_left(monkeypatch):
    # Stockwell is the change, so only the Northern line is left and its service is good
    monkeypatch.setattr(api, "line_statuses", fake_statuses)
    params = {"start": "Brixton", "end": "Bank", "current": "Stockwell"}
    assert client.get("/progress", params=params).json()["warnings"] == []


def test_progress_still_works_when_tfl_is_down(monkeypatch):
    monkeypatch.setattr(api, "line_statuses", tfl_is_down)
    params = {"start": "Brixton", "end": "Bank", "current": "Brixton"}
    response = client.get("/progress", params=params)
    assert response.status_code == 200
    assert response.json()["warnings"] == []


def test_progress_has_no_reroute_for_minor_delays(monkeypatch):
    monkeypatch.setattr(api, "line_statuses", fake_statuses)
    params = {"start": "Brixton", "end": "Bank", "current": "Brixton"}
    assert client.get("/progress", params=params).json()["reroute"] is None


def test_progress_suggests_a_way_round_a_suspension(monkeypatch):
    monkeypatch.setattr(api, "line_statuses", northern_suspended)
    params = {"start": "Brixton", "end": "Bank", "current": "Brixton"}
    reroute = client.get("/progress", params=params).json()["reroute"]
    assert reroute["avoid"] == ["northern"]
    assert "northern" not in [leg["line"] for leg in reroute["legs"]]
    assert reroute["legs"][0]["stops"][0] == "Brixton"
    assert reroute["legs"][-1]["stops"][-1] == "Bank"


def test_progress_has_no_reroute_when_there_is_no_way_round(monkeypatch):
    # Brixton is only on the Victoria line
    monkeypatch.setattr(api, "line_statuses", victoria_suspended)
    params = {"start": "Brixton", "end": "Bank", "current": "Brixton"}
    assert client.get("/progress", params=params).json()["reroute"] is None


def test_progress_follows_the_way_round_once_chosen(monkeypatch):
    monkeypatch.setattr(api, "line_statuses", northern_suspended)
    params = {"start": "Brixton", "end": "Bank", "current": "Stockwell", "avoid": "northern"}
    body = client.get("/progress", params=params).json()
    # Without avoid this would say get off at Stockwell for the Northern line
    assert body["next_stop_off"] == "Oxford Circus"


def test_the_way_round_stays_the_same_as_the_passenger_moves(monkeypatch):
    # From Green Park itself the best way round is the Jubilee line, but someone already on
    # the Victoria line heading for Oxford Circus should still be told to get off there
    monkeypatch.setattr(api, "line_statuses", northern_suspended)
    params = {"start": "Brixton", "end": "Bank", "current": "Green Park", "avoid": "northern"}
    body = client.get("/progress", params=params).json()
    assert body["next_stop_off"] == "Oxford Circus"
    assert body["wake_up"] is True
    # The Northern line isn't on the new route, so it's no longer a warning or a reason to reroute
    assert "northern" not in [w["line"] for w in body["warnings"]]
    assert body["reroute"] is None


def test_avoid_keeps_the_old_lines_when_another_goes_wrong(monkeypatch):
    def central_suspended_too():
        statuses = northern_suspended()
        statuses["central"] = line("Suspended", serious=True)
        return statuses

    monkeypatch.setattr(api, "line_statuses", central_suspended_too)
    params = {"start": "Brixton", "end": "Bank", "current": "Brixton", "avoid": "northern"}
    reroute = client.get("/progress", params=params).json()["reroute"]
    assert reroute["avoid"] == ["central", "northern"]
    assert not {"central", "northern"} & {leg["line"] for leg in reroute["legs"]}


def test_part_suspension_elsewhere_on_the_line_is_ignored(monkeypatch):
    def northern_suspended_up_north():
        statuses = fake_statuses()
        statuses["northern"] = line("Part Suspended", serious=True, stops=["Edgware", "Burnt Oak", "Colindale"])
        return statuses

    monkeypatch.setattr(api, "line_statuses", northern_suspended_up_north)
    params = {"start": "Brixton", "end": "Bank", "current": "Brixton"}
    body = client.get("/progress", params=params).json()
    assert "northern" not in [w["line"] for w in body["warnings"]]
    assert body["reroute"] is None


def test_progress_includes_the_whole_route(monkeypatch):
    monkeypatch.setattr(api, "line_statuses", fake_statuses)
    params = {"start": "Brixton", "end": "Bank", "current": "Oval"}
    legs = client.get("/progress", params=params).json()["legs"]
    assert [leg["line"] for leg in legs] == ["victoria", "northern"]
    # From the start, not just from where the passenger is now
    assert legs[0]["stops"][0] == "Brixton"


def test_progress_route_is_the_way_round_once_chosen(monkeypatch):
    monkeypatch.setattr(api, "line_statuses", northern_suspended)
    params = {"start": "Stockwell", "end": "Bank", "current": "Stockwell", "avoid": "northern"}
    legs = client.get("/progress", params=params).json()["legs"]
    assert "northern" not in [leg["line"] for leg in legs]
    assert legs[0]["stops"][0] == "Stockwell"
