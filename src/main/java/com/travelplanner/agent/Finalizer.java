package com.travelplanner.agent;

import java.util.HashMap;
import java.util.Map;

import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.stereotype.Component;

import com.travelplanner.agent.llm.ClaudeService;
import com.travelplanner.model.TripPlan.Budget;
import com.travelplanner.model.TripPlan.Flight;
import com.travelplanner.model.TripPlan.Hotel;
import com.travelplanner.model.TripRequest;

/** Writes the customer-facing summary, polished by Claude when available. */
@Component
public class Finalizer implements NodeAction<PlannerState> {

    public static final String NAME = "finalize";

    private final ClaudeService claude;

    public Finalizer(ClaudeService claude) {
        this.claude = claude;
    }

    @Override
    public Map<String, Object> apply(PlannerState state) {
        TripRequest req = state.request();
        Flight flight = state.flight().orElseThrow();
        Hotel hotel = state.hotel().orElseThrow();
        Budget budget = state.budget().orElseThrow();
        String summary = ("Hi %s! Your %d-night trip to %s is planned: %s %s from %s, %d nights at %s, "
                + "and a %d-day itinerary. Estimated total %,.2f %s against your %,.2f budget.")
                .formatted(req.customerName(), req.nights(), TravelInventory.capitalize(req.destination()),
                        flight.airline(), flight.flightNumber(), req.origin().toUpperCase(), req.nights(),
                        hotel.name(), state.itinerary().orElseThrow().size(), budget.total(), req.currency(),
                        req.budget());
        String polished = claude.text("Rewrite this travel plan summary as a warm, concise 2-3 sentence message "
                + "to the customer. Keep every number exactly as given. Reply with the message only.\n\n" + summary)
                .orElse(summary);

        Map<String, Object> update = new HashMap<>(PlannerState.step(NAME, "Wrote customer summary."));
        update.put(PlannerState.SUMMARY, polished);
        return update;
    }
}
