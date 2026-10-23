package com.travelplanner.agent;

import static com.travelplanner.agent.TravelInventory.round2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.stereotype.Component;

import com.travelplanner.model.TripPlan.Budget;
import com.travelplanner.model.TripPlan.DayPlan;
import com.travelplanner.model.TripPlan.Flight;
import com.travelplanner.model.TripPlan.Hotel;
import com.travelplanner.model.TripRequest;

/**
 * Totals the plan. If it is over budget and a cheaper hotel tier exists, it
 * lowers the star ceiling so the supervisor sends the hotel agent back.
 */
@Component
public class BudgetAgent implements NodeAction<PlannerState> {

    public static final String NAME = "budget_agent";
    static final double DAILY_FOOD_TRANSPORT_PER_PERSON = 45.0;

    @Override
    public Map<String, Object> apply(PlannerState state) {
        TripRequest req = state.request();
        Flight flight = state.flight().orElseThrow();
        Hotel hotel = state.hotel().orElseThrow();
        double flights = round2(flight.pricePerPerson() * req.travelers() * 2); // round trip
        double hotelCost = hotelCost(hotel, req);
        double activities = round2(state.itinerary().orElseThrow().stream().mapToDouble(DayPlan::estimatedCost).sum());
        double food = round2(DAILY_FOOD_TRANSPORT_PER_PERSON * req.travelers() * (req.nights() + 1));
        double total = round2(flights + hotelCost + activities + food);
        boolean within = total <= req.budget();

        Map<String, Object> update = new HashMap<>();
        List<String> notes = new ArrayList<>();
        if (within) {
            notes.add("%.2f %s left as buffer.".formatted(req.budget() - total, req.currency()));
        } else if (hotel.stars() > 2) {
            update.put(PlannerState.MAX_HOTEL_STARS, hotel.stars() - 1);
            update.put(PlannerState.REVISIONS, state.revisions() + 1);
            notes.add("Over by %.2f; asking hotel agent for <= %d stars.".formatted(total - req.budget(), hotel.stars() - 1));
        } else {
            notes.add("Over by %.2f even with budget options; consider shorter dates or economy class."
                    .formatted(total - req.budget()));
        }

        update.put(PlannerState.BUDGET,
                new Budget(flights, hotelCost, activities, food, total, req.budget(), within, List.copyOf(notes)));
        update.putAll(PlannerState.step(NAME,
                "Total %.2f vs budget %.2f. %s".formatted(total, req.budget(), notes.getFirst())));
        return update;
    }

    static double hotelCost(Hotel hotel, TripRequest req) {
        return round2(hotel.pricePerNight() * req.nights());
    }
}
