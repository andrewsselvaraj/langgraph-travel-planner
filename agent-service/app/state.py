from __future__ import annotations

import operator
from typing import Annotated, TypedDict

from .models import AgentStep, BudgetBreakdown, DayPlan, FlightOption, HotelOption, TripRequest


class PlannerState(TypedDict, total=False):
    request: TripRequest
    flight: FlightOption | None
    hotel: HotelOption | None
    itinerary: list[DayPlan]
    budget: BudgetBreakdown | None
    # Hotel star ceiling; the budget agent lowers it to ask the hotel agent for cheaper stays.
    max_hotel_stars: int
    revisions: int
    next_agent: str
    summary: str
    trace: Annotated[list[AgentStep], operator.add]
