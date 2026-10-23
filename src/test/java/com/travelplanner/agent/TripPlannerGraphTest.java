package com.travelplanner.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.travelplanner.agent.llm.ClaudeProperties;
import com.travelplanner.agent.llm.ClaudeService;
import com.travelplanner.model.TripPlan;
import com.travelplanner.model.TripPlan.AgentStep;
import com.travelplanner.model.TripRequest;

class TripPlannerGraphTest {

    private TripPlannerGraph planner;

    @BeforeEach
    void setUp() throws Exception {
        ClaudeService offline = new ClaudeService(new ClaudeProperties("", null, null));
        TravelInventory inventory = new TravelInventory();
        planner = new TripPlannerGraph(new Supervisor(), new FlightAgent(inventory), new HotelAgent(inventory),
                new ItineraryAgent(offline), new BudgetAgent(), new Finalizer(offline));
    }

    private static TripRequest request(String destination, double budget) {
        return new TripRequest("Ana", "SFO", destination, LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 5),
                2, budget, null, List.of("food", "culture"), null);
    }

    @Test
    void fullPlanVisitsEveryAgent() {
        TripPlan plan = planner.plan(request("Lisbon", 6000));

        Set<String> agents = plan.trace().stream().map(AgentStep::agent).collect(Collectors.toSet());
        assertThat(agents).contains(Supervisor.NAME, FlightAgent.NAME, HotelAgent.NAME, ItineraryAgent.NAME,
                BudgetAgent.NAME, Finalizer.NAME);
        assertThat(plan.itinerary()).hasSize(5);
        TripPlan.Budget b = plan.budget();
        assertThat(b.total()).isCloseTo(b.flights() + b.hotel() + b.activities() + b.foodAndTransport(), within(0.05));
        assertThat(plan.summary()).startsWith("Hi Ana");
    }

    @Test
    void tightBudgetSendsHotelAgentBackForCheaperStay() {
        TripPlan plan = planner.plan(request("Lisbon", 3000));

        List<AgentStep> hotelRuns = plan.trace().stream().filter(s -> s.agent().equals(HotelAgent.NAME)).toList();
        assertThat(hotelRuns.size() >= 2 || plan.hotel().stars() == 2).isTrue();
    }

    @Test
    void impossibleBudgetIsReported() {
        TripPlan plan = planner.plan(request("Lisbon", 100));

        assertThat(plan.status()).isEqualTo(TripPlan.Status.OVER_BUDGET);
        assertThat(plan.budget().withinBudget()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"Tokyo", "Paris", "Cusco", "Nairobi", "Reykjavik", "Bali"})
    void terminatesForManyDestinations(String destination) {
        assertThat(planner.plan(request(destination, 500)).budget()).isNotNull();
    }
}
