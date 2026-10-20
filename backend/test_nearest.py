from nearest import distance_m, nearest_stations


def test_same_point_is_zero():
    assert distance_m(51.5, -0.1, 51.5, -0.1) == 0


def test_brixton_to_stockwell_is_about_1200_metres():
    assert 1150 < distance_m(51.462618, -0.114888, 51.472184, -0.122644) < 1250


def test_standing_at_a_station_finds_it_first():
    closest = nearest_stations(51.506947, -0.142787)[0]
    assert closest == {"name": "Green Park", "metres": 0}


def test_closest_first_and_limit():
    stations = nearest_stations(51.5079, -0.1390, limit=5)
    assert len(stations) == 5
    distances = [station["metres"] for station in stations]
    assert distances == sorted(distances)
