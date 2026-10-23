from __future__ import annotations

from ..models import AgentStep, BudgetBreakdown
from ..state import PlannerState

DAILY_FOOD_TRANSPORT_PER_PERSON = 45.0


def budget_agent(state: PlannerState) -> dict:
    """Total up the plan. If it is over budget and a cheaper hotel tier exists,
    lower the star ceiling so the supervisor re-runs the hotel agent."""
    req = state["request"]
    flight, hotel = state["flight"], state["hotel"]
    flights = round(flight.price_per_person * req.travelers * 2, 2)  # round trip
    hotel_cost = round(hotel.price_per_night * req.nights, 2)
    activities = round(sum(d.estimated_cost for d in state["itinerary"]), 2)
    food = round(DAILY_FOOD_TRANSPORT_PER_PERSON * req.travelers * (req.nights + 1), 2)
    total = round(flights + hotel_cost + activities + food, 2)
    within = total <= req.budget

    notes: list[str] = []
    update: dict = {}
    if within:
        notes.append(f"{req.budget - total:.2f} {req.currency} left as buffer.")
    elif hotel.stars > 2:
        update["max_hotel_stars"] = hotel.stars - 1
        update["revisions"] = state.get("revisions", 0) + 1
        notes.append(f"Over by {total - req.budget:.2f}; asking hotel agent for <= {hotel.stars - 1} stars.")
    else:
        notes.append(f"Over by {total - req.budget:.2f} even with budget options; consider shorter dates or economy class.")

    breakdown = BudgetBreakdown(flights=flights, hotel=hotel_cost, activities=activities,
                                food_and_transport=food, total=total, budget=req.budget,
                                within_budget=within, notes=notes)
    return {
        **update,
        "budget": breakdown,
        "trace": [AgentStep(agent="budget_agent", summary=f"Total {total:.2f} vs budget {req.budget:.2f}. {notes[0]}")],
    }
