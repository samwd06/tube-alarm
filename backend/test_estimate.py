from estimate import MINUTES_PER_CHANGE, journey_minutes, leg_minutes
from journey import plan_journey


def test_no_travel_takes_no_time():
    assert journey_minutes([{"line": None, "stops": ["Victoria"]}]) == 0


def test_brixton_to_bank_is_a_sensible_length():
    # TfL's journey planner says about 17 minutes
    assert 12 <= journey_minutes(plan_journey("Brixton", "Bank")) <= 25


def test_a_change_adds_time():
    one_leg = [{"line": "victoria", "stops": ["Brixton", "Stockwell"]}]
    two_legs = one_leg + [{"line": "northern", "stops": ["Stockwell", "Oval"]}]
    assert journey_minutes(two_legs) >= journey_minutes(one_leg) + MINUTES_PER_CHANGE


def test_longer_leg_takes_longer():
    assert leg_minutes(["Brixton", "Stockwell", "Vauxhall"]) > leg_minutes(["Brixton", "Stockwell"])
