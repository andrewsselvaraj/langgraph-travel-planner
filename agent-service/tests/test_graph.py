from datetime import date

import pytest
from fastapi.testclient import TestClient

from app.graph import plan_trip
from app.main import app
from app.models import TripRequest


@pytest.fixture(autouse=True)
def offline(monkeypatch):
    monkeypatch.setenv("TRAVEL_PLANNER_OFFLINE", "1")
    from app.llm import get_llm
    get_llm.cache_clear()


def _req(**kw):
    base = dict(customer_name="Ana", origin="SFO", destination="Lisbon",
                start_date=date(2026, 12, 1), end_date=date(2026, 12, 5),
                travelers=2, budget=6000, interests=["food", "culture"])
    return TripRequest(**(base | kw))


def test_full_plan_visits_every_agent():
    plan = plan_trip(_req())
    agents = {s.agent for s in plan.trace}
    assert {"supervisor", "flight_agent", "hotel_agent", "itinerary_agent", "budget_agent", "finalize"} <= agents
    assert len(plan.itinerary) == 5
    assert plan.budget.total == pytest.approx(
        plan.budget.flights + plan.budget.hotel + plan.budget.activities + plan.budget.food_and_transport, abs=0.05)


def test_tight_budget_triggers_hotel_revision():
    plan = plan_trip(_req(budget=2500))
    hotel_runs = [s for s in plan.trace if s.agent == "hotel_agent"]
    assert len(hotel_runs) >= 2 or plan.hotel.stars == 2
    assert plan.status in {"READY", "OVER_BUDGET"}


def test_impossible_budget_is_reported():
    plan = plan_trip(_req(budget=100))
    assert plan.status == "OVER_BUDGET"
    assert not plan.budget.within_budget


def test_api_validates_dates():
    client = TestClient(app)
    r = client.post("/plan", json={"customer_name": "A", "origin": "SFO", "destination": "LIS",
                                   "start_date": "2026-12-05", "end_date": "2026-12-01", "budget": 1000})
    assert r.status_code == 422


def test_api_plan():
    client = TestClient(app)
    r = client.post("/plan", json=_req().model_dump(mode="json"))
    assert r.status_code == 200
    assert r.json()["summary"].startswith("Hi Ana")


@pytest.mark.parametrize("dest", ["Tokyo", "Paris", "Cusco", "Nairobi", "Reykjavik", "Bali"])
def test_terminates_for_many_destinations(dest):
    plan = plan_trip(_req(destination=dest, budget=500))
    assert plan.budget is not None
