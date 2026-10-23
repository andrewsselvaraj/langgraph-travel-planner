# LangGraph Travel Planner

A customer-facing, multi-agent travel planner.

```
Customer ──► Spring Boot customer-api (:8080) ──► LangGraph agent-service (:8000)
                /api/v1/trips                        POST /plan
                                                         │
                                                    ┌────┴─────┐
                                                    │supervisor│◄──────────────┐
                                                    └────┬─────┘               │
                     ┌──────────────┬──────────────┬─────┴────────┐            │
                flight_agent   hotel_agent   itinerary_agent  budget_agent ────┘
                                                         │
                                                      finalize
```

## How the agents work

| Agent | Job |
|---|---|
| **supervisor** | Looks at what the plan still lacks and routes to the next specialist. Loops until the plan is complete. |
| **flight_agent** | Searches flights and picks the best value (cheapest, preferring fewer stops within 15%). |
| **hotel_agent** | Picks the highest-rated hotel that fits ~40% of the budget, under the current star ceiling. |
| **itinerary_agent** | Builds a day-by-day plan with Claude (`claude-opus-5-5`); falls back to a rule-based planner offline. |
| **budget_agent** | Totals flights, hotel, activities, food/transport. If over budget, it lowers the hotel star ceiling and the supervisor sends the hotel agent back for a cheaper stay. |
| **finalize** | Writes the customer-facing summary (polished by Claude when available). |

Flight and hotel data come from deterministic mock providers in
`agent-service/app/tools.py`; replace those functions with real APIs
(Amadeus, Skyscanner, Booking.com, ...).

## Run locally

### 1. Agent service (Python 3.11+)

```bash
cd agent-service
python -m venv .venv
# Windows: .venv\Scripts\activate    macOS/Linux: source .venv/bin/activate
pip install -r requirements-dev.txt
# Optional: set ANTHROPIC_API_KEY to let Claude write itineraries
uvicorn app.main:app --port 8000
```

### 2. Customer API (Java 21, Maven)

```bash
cd customer-api
mvn spring-boot:run
```

### Or both with Docker

```bash
cp .env.example .env   # add ANTHROPIC_API_KEY if you have one
docker compose up --build
```

## Customer API

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/v1/trips` | Plan a trip (201 + `Location` header) |
| `GET` | `/api/v1/trips/{id}` | Fetch a planned trip |
| `GET` | `/api/v1/trips?customerName=Ana` | List a customer's trips |
| `GET` | `/actuator/health` | Health check |

```bash
curl -X POST localhost:8080/api/v1/trips -H "Content-Type: application/json" -d '{
  "customerName": "Ana",
  "origin": "SFO",
  "destination": "Lisbon",
  "startDate": "2026-12-01",
  "endDate": "2026-12-05",
  "travelers": 2,
  "budget": 6000,
  "currency": "USD",
  "interests": ["food", "culture"],
  "travelClass": "economy"
}'
```

The response includes the chosen flight and hotel, the itinerary, a budget
breakdown, a `status` of `READY` or `OVER_BUDGET`, and a `trace` showing
each agent's decision. Errors use RFC 7807 problem details (400 for invalid
input, 404 for unknown trips, 502 when the agent service is down).

## Tests

```bash
cd agent-service && pytest
cd customer-api && mvn test
```

## Configuration

| Variable | Service | Default |
|---|---|---|
| `ANTHROPIC_API_KEY` | agent-service | unset (offline mode) |
| `ANTHROPIC_MODEL` | agent-service | `claude-opus-5-5` |
| `ANTHROPIC_EFFORT` | agent-service | `medium` |
| `TRAVEL_PLANNER_OFFLINE` | agent-service | set to `1` to force offline mode |
| `AGENT_SERVICE_URL` | customer-api | `http://localhost:8000` |
