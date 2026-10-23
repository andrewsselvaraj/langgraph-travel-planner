# LangGraph Travel Planner (Java / Spring Boot)

A customer-facing, multi-agent travel planner written entirely in Java:
**Spring Boot 3.5** for the REST API, **[LangGraph4j](https://github.com/langgraph4j/langgraph4j)**
(the Java port of LangGraph) for the agent graph, and the **Anthropic Java SDK**
so Claude can write itineraries.

```
Customer ──► POST /api/v1/trips ──► TripService ──► TripPlannerGraph (LangGraph4j)
                                                        │
                                                   ┌────┴─────┐
                                         START ──► │supervisor│◄────────────────┐
                                                   └────┬─────┘                 │
                        ┌──────────────┬──────────────┬─┴────────────┐          │
                   flight_agent   hotel_agent   itinerary_agent  budget_agent ──┘
                                                        │
                                                    finalize ──► END
```

## The agents

| Agent | Class | Job |
|---|---|---|
| **supervisor** | `Supervisor` | Looks at what the plan still lacks and routes to the next specialist, looping until the plan is complete. |
| **flight_agent** | `FlightAgent` | Searches flights and picks the best value (cheapest, preferring fewer stops within 15%). |
| **hotel_agent** | `HotelAgent` | Picks the highest-rated hotel that fits ~40% of the budget under the current star ceiling; on a budget revision it picks the cheapest. |
| **itinerary_agent** | `ItineraryAgent` | Builds a day-by-day plan with Claude (`claude-opus-5-5`, structured JSON output); falls back to a rule-based planner offline. |
| **budget_agent** | `BudgetAgent` | Totals flights, hotel, activities, food and transport. If over budget it lowers the hotel star ceiling and the supervisor sends the hotel agent back. |
| **finalize** | `Finalizer` | Writes the customer-facing summary (polished by Claude when available). |

All agents live in `src/main/java/com/travelplanner/agent`. Flight and hotel
data come from the deterministic mock in `TravelInventory`; replace its two
methods with real providers (Amadeus, Skyscanner, Booking.com, ...).

## Run

Requires Java 21 and Maven.

```bash
# Optional: let Claude write itineraries. Without a key the agents run offline.
export ANTHROPIC_API_KEY=sk-ant-...        # Windows PowerShell: $env:ANTHROPIC_API_KEY="sk-ant-..."
mvn spring-boot:run
```

Or with Docker:

```bash
docker build -t travel-planner .
docker run -p 8080:8080 -e ANTHROPIC_API_KEY=$ANTHROPIC_API_KEY travel-planner
```

## API

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

The response holds the chosen flight and hotel, the itinerary, a budget
breakdown, a `status` of `READY` or `OVER_BUDGET`, and a `trace` of each
agent's decision. Errors use RFC 7807 problem details (400 for invalid input,
404 for unknown trips, 503 if planning fails). Trips are kept in memory;
swap the map in `TripService` for a JPA repository to persist them.

## Configuration

| Environment variable | Default |
|---|---|
| `ANTHROPIC_API_KEY` | unset (offline mode) |
| `ANTHROPIC_MODEL` | `claude-opus-5-5` |
| `ANTHROPIC_EFFORT` | `medium` |

## Tests

```bash
mvn test
```
