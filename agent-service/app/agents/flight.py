from __future__ import annotations

from ..models import AgentStep
from ..state import PlannerState
from ..tools import search_flights


def flight_agent(state: PlannerState) -> dict:
    """Pick the best-value flight: cheapest option, preferring fewer stops when
    the price difference is under 15%."""
    req = state["request"]
    options = search_flights(req)
    cheapest = options[0]
    best = min(
        (f for f in options if f.price_per_person <= cheapest.price_per_person * 1.15),
        key=lambda f: (f.stops, f.price_per_person),
    )
    return {
        "flight": best,
        "trace": [AgentStep(
            agent="flight_agent",
            summary=f"Compared {len(options)} flights; chose {best.airline} {best.flight_number} "
                    f"({best.stops} stop(s)) at {best.price_per_person:.2f} {req.currency}/person.",
        )],
    }
