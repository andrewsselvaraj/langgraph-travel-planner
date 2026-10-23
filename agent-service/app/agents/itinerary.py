from __future__ import annotations

import logging
from datetime import timedelta

from pydantic import BaseModel

from ..llm import get_llm
from ..models import AgentStep, DayPlan, TripRequest
from ..state import PlannerState

log = logging.getLogger(__name__)

DEFAULT_INTERESTS = ["sightseeing", "food", "culture"]
ACTIVITY_LIBRARY = {
    "food": ["Food market tasting tour", "Dinner at a local favourite", "Cooking class"],
    "culture": ["Main history museum", "Guided old-town walk", "Evening live performance"],
    "sightseeing": ["Panoramic viewpoint", "Landmark highlights tour", "River or harbour cruise"],
    "nature": ["Day hike nearby", "Botanical garden", "Sunset at the park"],
    "shopping": ["Local designers district", "Artisan craft market"],
    "nightlife": ["Rooftop bar", "Live music venue"],
    "beach": ["Morning at the beach", "Snorkelling trip"],
    "adventure": ["Kayaking excursion", "Zip-line park"],
}


class _LLMDay(BaseModel):
    title: str
    activities: list[str]
    estimated_cost: float


class _LLMItinerary(BaseModel):
    days: list[_LLMDay]


def _rule_based(req: TripRequest) -> list[DayPlan]:
    interests = [i.lower() for i in req.interests] or DEFAULT_INTERESTS
    pool = [a for i in interests for a in ACTIVITY_LIBRARY.get(i, [f"Explore {i} spots"])]
    days = []
    for d in range(req.nights + 1):
        if d == 0:
            title, acts = "Arrival & settle in", ["Check in to hotel", "Neighbourhood stroll", "Welcome dinner"]
        elif d == req.nights:
            title, acts = "Departure", ["Breakfast and souvenir shopping", "Transfer to airport"]
        else:
            acts = [pool[(d * 2 + k) % len(pool)] for k in range(2)]
            title = f"{req.destination.title()}: {acts[0]}"
        days.append(DayPlan(
            day=d + 1,
            date=req.start_date + timedelta(days=d),
            title=title,
            activities=acts,
            estimated_cost=round(25.0 * len(acts) * req.travelers, 2),
        ))
    return days


def _llm_based(req: TripRequest) -> list[DayPlan] | None:
    llm = get_llm()
    if llm is None:
        return None
    prompt = (
        f"Plan a {req.nights + 1}-day itinerary in {req.destination} for {req.travelers} traveller(s), "
        f"{req.start_date} to {req.end_date}. Interests: {', '.join(req.interests) or 'general'}. "
        f"Total trip budget {req.budget} {req.currency} (flights and hotel are booked separately). "
        "Day 1 is arrival and the last day is departure. For each day give a short title, 2-4 concrete "
        f"activities, and the estimated activity cost for the whole group in {req.currency}. "
        f"Return exactly {req.nights + 1} days."
    )
    try:
        result = llm.with_structured_output(_LLMItinerary, method="json_schema").invoke(prompt)
    except Exception:  # network, auth, refusal — fall back rather than fail the trip
        log.exception("LLM itinerary generation failed; using rule-based fallback")
        return None
    if len(result.days) != req.nights + 1:
        return None
    return [
        DayPlan(day=i + 1, date=req.start_date + timedelta(days=i), title=d.title,
                activities=d.activities, estimated_cost=round(d.estimated_cost, 2))
        for i, d in enumerate(result.days)
    ]


def itinerary_agent(state: PlannerState) -> dict:
    req = state["request"]
    days = _llm_based(req)
    source = "Claude"
    if days is None:
        days, source = _rule_based(req), "rule-based planner"
    return {
        "itinerary": days,
        "trace": [AgentStep(agent="itinerary_agent", summary=f"Built a {len(days)}-day itinerary with the {source}.")],
    }
