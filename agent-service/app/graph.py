"""LangGraph wiring: a supervisor routes between specialist agents until the
plan is complete, then a finalizer writes the customer-facing summary."""
from __future__ import annotations

from functools import lru_cache

from langgraph.graph import END, START, StateGraph

from .agents import budget_agent, flight_agent, hotel_agent, itinerary_agent, supervisor
from .llm import get_llm
from .models import AgentStep, TripPlan, TripRequest
from .state import PlannerState

SPECIALISTS = {
    "flight_agent": flight_agent,
    "hotel_agent": hotel_agent,
    "itinerary_agent": itinerary_agent,
    "budget_agent": budget_agent,
}


def finalize(state: PlannerState) -> dict:
    req, flight, hotel, budget = state["request"], state["flight"], state["hotel"], state["budget"]
    summary = (
        f"Hi {req.customer_name}! Your {req.nights}-night trip to {req.destination.title()} is planned: "
        f"{flight.airline} {flight.flight_number} from {req.origin.upper()}, {req.nights} nights at "
        f"{hotel.name}, and a {len(state['itinerary'])}-day itinerary. Estimated total "
        f"{budget.total:,.2f} {req.currency} against your {req.budget:,.2f} budget."
    )
    llm = get_llm()
    if llm is not None:
        try:
            msg = llm.invoke(
                "Rewrite this travel plan summary as a warm, concise 2-3 sentence message to the customer. "
                f"Keep every number exactly as given.\n\n{summary}"
            )
            summary = msg.text.strip() or summary
        except Exception:
            pass
    return {"summary": summary, "trace": [AgentStep(agent="finalize", summary="Wrote customer summary.")]}


def build_graph():
    g = StateGraph(PlannerState)
    g.add_node("supervisor", supervisor)
    for name, fn in SPECIALISTS.items():
        g.add_node(name, fn)
        g.add_edge(name, "supervisor")
    g.add_node("finalize", finalize)
    g.add_edge(START, "supervisor")
    g.add_conditional_edges(
        "supervisor",
        lambda s: s["next_agent"],
        {**{n: n for n in SPECIALISTS}, "finish": "finalize"},
    )
    g.add_edge("finalize", END)
    return g.compile()


@lru_cache(maxsize=1)
def get_graph():
    return build_graph()


def plan_trip(req: TripRequest) -> TripPlan:
    final = get_graph().invoke({"request": req, "trace": []}, {"recursion_limit": 40})
    budget = final["budget"]
    return TripPlan(
        customer_name=req.customer_name,
        destination=req.destination,
        status="READY" if budget.within_budget else "OVER_BUDGET",
        flight=final["flight"],
        hotel=final["hotel"],
        itinerary=final["itinerary"],
        budget=budget,
        summary=final["summary"],
        trace=final["trace"],
    )
