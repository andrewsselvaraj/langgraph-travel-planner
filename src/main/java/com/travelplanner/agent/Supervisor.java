package com.travelplanner.agent;

import java.util.HashMap;
import java.util.Map;

import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.stereotype.Component;

import com.travelplanner.model.TripPlan.Budget;
import com.travelplanner.model.TripPlan.Hotel;

/**
 * Routes to the next specialist based on what the plan still lacks.
 * Routing is deterministic on purpose so the workflow stays predictable for
 * customers; the specialists themselves can use Claude.
 */
@Component
public class Supervisor implements NodeAction<PlannerState> {

    public static final String NAME = "supervisor";
    public static final String FINISH = "finish";
    static final int MAX_REVISIONS = 3;

    @Override
    public Map<String, Object> apply(PlannerState state) {
        String next = route(state);
        Map<String, Object> update = new HashMap<>(PlannerState.step(NAME, "Routing to " + next + "."));
        update.put(PlannerState.NEXT, next);
        return update;
    }

    static String route(PlannerState state) {
        Budget budget = state.budget().orElse(null);
        if (state.flight().isEmpty()) {
            return FlightAgent.NAME;
        }
        if (state.hotel().isEmpty() || (budget != null && !budget.withinBudget() && hotelNeedsRework(state))) {
            return HotelAgent.NAME;
        }
        if (state.itinerary().isEmpty()) {
            return ItineraryAgent.NAME;
        }
        if (budget == null || BudgetAgent.hotelCost(state.hotel().get(), state.request()) != budget.hotel()) {
            return BudgetAgent.NAME;
        }
        return FINISH;
    }

    private static boolean hotelNeedsRework(PlannerState state) {
        Hotel hotel = state.hotel().orElseThrow();
        return state.revisions() <= MAX_REVISIONS && hotel.stars() > state.maxHotelStars();
    }
}
