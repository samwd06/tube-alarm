import pytest

from stops_between import find_stations, stops_between


def test_victoria_line_in_order():
    assert stops_between("Stockwell", "Green Park") == [
        "Stockwell", "Vauxhall", "Pimlico", "Victoria", "Green Park",
    ]


def test_other_direction_is_reversed():
    assert stops_between("Green Park", "Stockwell") == [
        "Green Park", "Victoria", "Pimlico", "Vauxhall", "Stockwell",
    ]


def test_names_ignore_case():
    assert stops_between("stockwell", "vauxhall") == ["Stockwell", "Vauxhall"]


def test_northern_line_branch():
    stops = stops_between("Mill Hill East", "Finchley Central")
    assert stops == ["Mill Hill East", "Finchley Central"]


def test_same_station():
    assert stops_between("Victoria", "Victoria") == ["Victoria"]


def test_change_of_line_is_not_supported_yet():
    with pytest.raises(ValueError, match="No single line"):
        stops_between("Brixton", "Bank")


def test_unclear_name():
    assert len(find_stations("Hammersmith")) == 2
    with pytest.raises(ValueError, match="could mean"):
        stops_between("Hammersmith", "Barons Court")


def test_unknown_name():
    with pytest.raises(ValueError, match="No station called"):
        stops_between("Nowhere", "Bank")
