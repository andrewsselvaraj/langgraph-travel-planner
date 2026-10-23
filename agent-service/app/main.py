from __future__ import annotations

import logging

from fastapi import FastAPI
from fastapi.concurrency import run_in_threadpool

from .graph import plan_trip
from .llm import get_llm
from .models import TripPlan, TripRequest

logging.basicConfig(level=logging.INFO)
app = FastAPI(title="LangGraph Travel Planner Agents", version="0.1.0")


@app.get("/health")
def health() -> dict:
    return {"status": "ok", "llm": "claude" if get_llm() else "offline"}


@app.post("/plan", response_model=TripPlan)
async def plan(req: TripRequest) -> TripPlan:
    return await run_in_threadpool(plan_trip, req)
