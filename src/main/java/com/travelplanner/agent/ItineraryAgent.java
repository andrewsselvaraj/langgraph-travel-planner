package com.travelplanner.agent;

import static com.travelplanner.agent.TravelInventory.round2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.travelplanner.agent.llm.ClaudeService;
import com.travelplanner.model.TripPlan.DayPlan;
import com.travelplanner.model.TripRequest;

/** Builds the day-by-day itinerary with Claude, or a rule-based planner offline. */
@Component
public class ItineraryAgent implements NodeAction<PlannerState> {

    public static final String NAME = "itinerary_agent";

    private static final List<String> DEFAULT_INTERESTS = List.of("sightseeing", "food", "culture");
    private static final Map<String, List<String>> ACTIVITY_LIBRARY = Map.of(
            "food", List.of("Food market tasting tour", "Dinner at a local favourite", "Cooking class"),
            "culture", List.of("Main history museum", "Guided old-town walk", "Evening live performance"),
            "sightseeing", List.of("Panoramic viewpoint", "Landmark highlights tour", "River or harbour cruise"),
            "nature", List.of("Day hike nearby", "Botanical garden", "Sunset at the park"),
            "shopping", List.of("Local designers district", "Artisan craft market"),
            "nightlife", List.of("Rooftop bar", "Live music venue"),
            "beach", List.of("Morning at the beach", "Snorkelling trip"),
            "adventure", List.of("Kayaking excursion", "Zip-line park"));

    record LlmDay(String title,
                  @JsonPropertyDescription("2-4 concrete activities") List<String> activities,
                  @JsonPropertyDescription("Activity cost for the whole group") double estimatedCost) {
    }

    record LlmItinerary(List<LlmDay> days) {
    }

    private final ClaudeService claude;

    public ItineraryAgent(ClaudeService claude) {
        this.claude = claude;
    }

    @Override
    public Map<String, Object> apply(PlannerState state) {
        TripRequest req = state.request();
        Optional<List<DayPlan>> fromClaude = withClaude(req);
        List<DayPlan> days = fromClaude.orElseGet(() -> ruleBased(req));
        String source = fromClaude.isPresent() ? "Claude" : "rule-based planner";

        Map<String, Object> update = new HashMap<>(PlannerState.step(NAME,
                "Built a %d-day itinerary with the %s.".formatted(days.size(), source)));
        update.put(PlannerState.ITINERARY, days);
        return update;
    }

    private Optional<List<DayPlan>> withClaude(TripRequest req) {
        int dayCount = req.nights() + 1;
        String prompt = """
                Plan a %d-day itinerary in %s for %d traveller(s), %s to %s. Interests: %s.
                Total trip budget %.0f %s (flights and hotel are booked separately).
                Day 1 is arrival and the last day is departure. For each day give a short title,
                2-4 concrete activities, and the estimated activity cost for the whole group in %s.
                Return exactly %d days."""
                .formatted(dayCount, req.destination(), req.travelers(), req.startDate(), req.endDate(),
                        req.interests().isEmpty() ? "general" : String.join(", ", req.interests()),
                        req.budget(), req.currency(), req.currency(), dayCount);
        return claude.structured(prompt, LlmItinerary.class)
                .filter(it -> it.days() != null && it.days().size() == dayCount)
                .map(it -> IntStream.range(0, dayCount)
                        .mapToObj(i -> {
                            LlmDay d = it.days().get(i);
                            return new DayPlan(i + 1, req.startDate().plusDays(i), d.title(),
                                    List.copyOf(d.activities()), round2(d.estimatedCost()));
                        })
                        .toList());
    }

    static List<DayPlan> ruleBased(TripRequest req) {
        List<String> interests = req.interests().isEmpty() ? DEFAULT_INTERESTS : req.interests();
        List<String> pool = new ArrayList<>();
        for (String interest : interests) {
            pool.addAll(ACTIVITY_LIBRARY.getOrDefault(interest.toLowerCase(), List.of("Explore " + interest + " spots")));
        }
        List<DayPlan> days = new ArrayList<>();
        for (int d = 0; d <= req.nights(); d++) {
            String title;
            List<String> acts;
            if (d == 0) {
                title = "Arrival & settle in";
                acts = List.of("Check in to hotel", "Neighbourhood stroll", "Welcome dinner");
            } else if (d == req.nights()) {
                title = "Departure";
                acts = List.of("Breakfast and souvenir shopping", "Transfer to airport");
            } else {
                acts = List.of(pool.get((d * 2) % pool.size()), pool.get((d * 2 + 1) % pool.size()));
                title = TravelInventory.capitalize(req.destination()) + ": " + acts.getFirst();
            }
            days.add(new DayPlan(d + 1, req.startDate().plusDays(d), title, acts,
                    round2(25.0 * acts.size() * req.travelers())));
        }
        return List.copyOf(days);
    }
}
