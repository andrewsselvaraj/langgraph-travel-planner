"""Claude model factory. Returns None when no API key is configured so the
agents fall back to deterministic, rule-based behaviour (useful for tests and
local development)."""
from __future__ import annotations

import os
from functools import lru_cache

from langchain_anthropic import ChatAnthropic

DEFAULT_MODEL = "claude-opus-5-5"


@lru_cache(maxsize=1)
def get_llm() -> ChatAnthropic | None:
    if os.getenv("TRAVEL_PLANNER_OFFLINE") == "1" or not os.getenv("ANTHROPIC_API_KEY"):
        return None
    return ChatAnthropic(
        model=os.getenv("ANTHROPIC_MODEL", DEFAULT_MODEL),
        max_tokens=16000,
        effort=os.getenv("ANTHROPIC_EFFORT", "medium"),
        timeout=120,
        max_retries=2,
    )
