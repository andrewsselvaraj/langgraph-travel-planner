from __future__ import annotations

from ..models import AgentStep
from ..state import PlannerState
from ..tools import search_hotels


def hotel_agent(state: PlannerState) -> dict:
    """Choose the highest-rated hotel whose nightly rate fits ~40% of the budget."""
    req = state["request"]
    max_stars = state.get("max_hotel_stars", 5)
    options = search_hotels(req, max_stars=max_stars)
    if not options:  # nothing at this tier; fall back to the cheapest stay overall
        options = sorted(search_hotels(req), key=lambda h: h.price_per_night)[:1]
    nightly_cap = req.budget * 0.4 / req.nights
    affordable = [h for h in options if h.price_per_night <= nightly_cap]
    choice = affordable[0] if affordable else min(options, key=lambda h: h.price_per_night)
    note = "within the nightly target" if affordable else "cheapest available (over nightly target)"
    return {
        "hotel": choice,
        "max_hotel_stars": max(max_stars, choice.stars),
        "trace": [AgentStep(
            agent="hotel_agent",
            summary=f"Searched {len(options)} hotels (<= {max_stars} stars); chose {choice.name} "
                    f"({choice.stars}*, {choice.rating}/5) at {choice.price_per_night:.2f}/night, {note}.",
        )],
    }
