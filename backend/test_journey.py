import pytest

from journey import plan_journey


def lines_used(legs):
    return [leg["line"] for leg in legs]


def test_one_line_needs_no_change():
    legs = plan_journey("Stockwell", "Green Park")
    assert lines_used(legs) == ["victoria"]
    assert legs[0]["stops"] == ["Stockwell", "Vauxhall", "Pimlico", "Victoria", "Green Park"]


def test_brixton_to_bank_changes_at_stockwell():
    legs = plan_journey("Brixton", "Bank")
    assert lines_used(legs) == ["victoria", "northern"]
    assert legs[0]["stops"][-1] == "Stockwell"
    assert legs[1]["stops"][0] == "Stockwell"
    assert legs[1]["stops"][-1] == "Bank"


def test_two_changes():
    legs = plan_journey("Upminster", "Amersham")
    assert len(legs) == 3
    assert legs[0]["stops"][0] == "Upminster"
    assert legs[-1]["stops"][-1] == "Amersham"


def test_walks_between_the_two_hammersmiths():
    legs = plan_journey("Hammersmith (H&C Line)", "Barons Court")
    assert lines_used(legs)[0] == "walk"
    assert legs[0]["stops"] == ["Hammersmith (H&C Line)", "Hammersmith (Dist&Picc Line)"]
    assert legs[-1]["stops"][-1] == "Barons Court"


def test_avoiding_a_line():
    legs = plan_journey("Brixton", "Bank", avoid={"northern"})
    assert "northern" not in lines_used(legs)
    assert legs[0]["line"] == "victoria"
    assert legs[-1]["stops"][-1] == "Bank"


def test_avoiding_the_only_line_finds_nothing():
    # Brixton is only on the Victoria line, so there's no way out without it
    with pytest.raises(ValueError, match="No route found"):
        plan_journey("Brixton", "Bank", avoid={"victoria"})


def test_same_station():
    assert plan_journey("Victoria", "Victoria") == [{"line": None, "stops": ["Victoria"]}]


def test_unknown_station():
    with pytest.raises(ValueError, match="No station called"):
        plan_journey("Nowhere", "Bank")
