import pytest

from alarm import progress

# Brixton to Bank, as plan_journey returns it
LEGS = [
    {"line": "victoria", "stops": ["Brixton", "Stockwell"]},
    {"line": "northern", "stops": ["Stockwell", "Oval", "Kennington", "Elephant & Castle",
                                   "Borough", "London Bridge", "Bank"]},
]


def test_start_of_journey():
    result = progress(LEGS, "Brixton")
    assert result["stops_left"] == 7
    assert result["next_stop_off"] == "Stockwell"
    assert result["wake_up"] is True


def test_quiet_in_the_middle():
    result = progress(LEGS, "Oval")
    assert result["next_stop_off"] == "Bank"
    assert result["stops_to_next_stop_off"] == 5
    assert result["wake_up"] is False


def test_wakes_one_stop_before_the_end():
    assert progress(LEGS, "London Bridge")["wake_up"] is True
    assert progress(LEGS, "Borough")["wake_up"] is False


def test_wake_earlier_if_asked():
    assert progress(LEGS, "Borough", wake_before=2)["wake_up"] is True


def test_name_ignores_case():
    assert progress(LEGS, "london bridge")["current"] == "London Bridge"


def test_station_not_on_journey():
    with pytest.raises(ValueError, match="not on this journey"):
        progress(LEGS, "Victoria")
