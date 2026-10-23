from __future__ import annotations

from ..models import AgentStep
from ..state import PlannerState

MAX_REVISIONS = 3


def supervisor(state: PlannerState) -> dict:
    """Route to the next specialist based on what the plan still lacks.

    Routing is deterministic on purpose: it keeps the workflow predictable for
    customers, while the specialists themselves can use Claude.
    """
    budget = state.get("budget")
    if not state.get("flight"):
        nxt = "flight_agent"
    elif not state.get("hotel") or (budget and not budget.within_budget and _hotel_needs_rework(state)):
        nxt = "hotel_agent"
    elif not state.get("itinerary"):
        nxt = "itinerary_agent"
    elif budget is None or _hotel_changed_since_budget(state):
        nxt = "budget_agent"
    else:
        nxt = "finish"
    return {"next_agent": nxt, "trace": [AgentStep(agent="supervisor", summary=f"Routing to {nxt}.")]}


def _hotel_needs_rework(state: PlannerState) -> bool:
    return (
        state.get("revisions", 0) <= MAX_REVISIONS
        and state["hotel"].stars > state.get("max_hotel_stars", 5)
    )


def _hotel_changed_since_budget(state: PlannerState) -> bool:
    budget, hotel, req = state["budget"], state["hotel"], state["request"]
    return round(hotel.price_per_night * req.nights, 2) != budget.hotel
