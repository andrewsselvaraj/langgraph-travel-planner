"""Request/response schemas shared by the graph and the HTTP API."""
from __future__ import annotations

from datetime import date
from typing import Literal

from pydantic import BaseModel, Field, model_validator


class TripRequest(BaseModel):
    customer_name: str = Field(min_length=1)
    origin: str = Field(min_length=2, description="City or IATA code")
    destination: str = Field(min_length=2, description="City or IATA code")
    start_date: date
    end_date: date
    travelers: int = Field(default=1, ge=1, le=10)
    budget: float = Field(gt=0, description="Total trip budget")
    currency: str = Field(default="USD", min_length=3, max_length=3)
    interests: list[str] = Field(default_factory=list)
    travel_class: Literal["economy", "premium_economy", "business"] = "economy"

    @model_validator(mode="after")
    def _check_dates(self) -> "TripRequest":
        if self.end_date <= self.start_date:
            raise ValueError("end_date must be after start_date")
        return self

    @property
    def nights(self) -> int:
        return (self.end_date - self.start_date).days


class FlightOption(BaseModel):
    airline: str
    flight_number: str
    depart: str
    arrive: str
    stops: int
    cabin: str
    price_per_person: float


class HotelOption(BaseModel):
    name: str
    stars: int
    neighborhood: str
    price_per_night: float
    rating: float


class DayPlan(BaseModel):
    day: int
    date: date
    title: str
    activities: list[str]
    estimated_cost: float


class BudgetBreakdown(BaseModel):
    flights: float
    hotel: float
    activities: float
    food_and_transport: float
    total: float
    budget: float
    within_budget: bool
    notes: list[str] = Field(default_factory=list)


class AgentStep(BaseModel):
    agent: str
    summary: str


class TripPlan(BaseModel):
    customer_name: str
    destination: str
    status: Literal["READY", "OVER_BUDGET"]
    flight: FlightOption | None
    hotel: HotelOption | None
    itinerary: list[DayPlan]
    budget: BudgetBreakdown | None
    summary: str
    trace: list[AgentStep]
