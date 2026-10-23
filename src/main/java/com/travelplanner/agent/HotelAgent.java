package com.travelplanner.agent;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.stereotype.Component;

import com.travelplanner.model.TripPlan.Hotel;
import com.travelplanner.model.TripRequest;

/**
 * Chooses the highest-rated hotel whose nightly rate fits ~40% of the budget.
 * When the budget agent has sent it back, it picks the cheapest stay instead.
 */
@Component
public class HotelAgent implements NodeAction<PlannerState> {

    public static final String NAME = "hotel_agent";

    private final TravelInventory inventory;

    public HotelAgent(TravelInventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Map<String, Object> apply(PlannerState state) {
        TripRequest req = state.request();
        int maxStars = state.maxHotelStars();
        List<Hotel> options = inventory.searchHotels(req, maxStars);
        if (options.isEmpty()) { // nothing at this tier; fall back to the cheapest stay overall
            options = inventory.searchHotels(req, 5).stream()
                    .sorted(Comparator.comparingDouble(Hotel::pricePerNight))
                    .limit(1)
                    .toList();
        }
        double nightlyCap = req.budget() * 0.4 / req.nights();
        Comparator<Hotel> preference = state.revisions() > 0
                ? Comparator.comparingDouble(Hotel::pricePerNight)
                : Comparator.comparingDouble(Hotel::rating).reversed().thenComparingDouble(Hotel::pricePerNight);
        List<Hotel> affordable = options.stream()
                .filter(h -> h.pricePerNight() <= nightlyCap)
                .sorted(preference)
                .toList();
        Hotel choice = affordable.isEmpty()
                ? options.stream().min(Comparator.comparingDouble(Hotel::pricePerNight)).orElseThrow()
                : affordable.getFirst();
        String note = affordable.isEmpty() ? "cheapest available (over nightly target)" : "within the nightly target";

        Map<String, Object> update = new HashMap<>(PlannerState.step(NAME,
                "Searched %d hotels (<= %d stars); chose %s (%d*, %.1f/5) at %.2f/night, %s."
                        .formatted(options.size(), maxStars, choice.name(), choice.stars(), choice.rating(),
                                choice.pricePerNight(), note)));
        update.put(PlannerState.HOTEL, choice);
        update.put(PlannerState.MAX_HOTEL_STARS, Math.max(maxStars, choice.stars()));
        return update;
    }
}
