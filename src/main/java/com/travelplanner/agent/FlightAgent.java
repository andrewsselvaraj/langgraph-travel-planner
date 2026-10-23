package com.travelplanner.agent;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.stereotype.Component;

import com.travelplanner.model.TripPlan.Flight;
import com.travelplanner.model.TripRequest;

/** Picks the best-value flight: cheapest, preferring fewer stops within 15% of the cheapest fare. */
@Component
public class FlightAgent implements NodeAction<PlannerState> {

    public static final String NAME = "flight_agent";

    private final TravelInventory inventory;

    public FlightAgent(TravelInventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Map<String, Object> apply(PlannerState state) {
        TripRequest req = state.request();
        List<Flight> options = inventory.searchFlights(req);
        double cheapest = options.getFirst().pricePerPerson();
        Flight best = options.stream()
                .filter(f -> f.pricePerPerson() <= cheapest * 1.15)
                .min(Comparator.comparingInt(Flight::stops).thenComparingDouble(Flight::pricePerPerson))
                .orElseThrow();

        Map<String, Object> update = new HashMap<>(PlannerState.step(NAME,
                "Compared %d flights; chose %s %s (%d stop(s)) at %.2f %s/person."
                        .formatted(options.size(), best.airline(), best.flightNumber(), best.stops(),
                                best.pricePerPerson(), req.currency())));
        update.put(PlannerState.FLIGHT, best);
        return update;
    }
}
