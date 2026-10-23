package com.travelplanner.agent;

import static org.bsc.langgraph4j.GraphDefinition.END;
import static org.bsc.langgraph4j.GraphDefinition.START;
import static org.bsc.langgraph4j.action.AsyncEdgeAction.edge_async;
import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

import java.util.List;
import java.util.Map;

import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.StateGraph;
import org.springframework.stereotype.Component;

import com.travelplanner.model.TripPlan;
import com.travelplanner.model.TripRequest;

/**
 * LangGraph4j wiring: the supervisor routes between specialist agents until
 * the plan is complete, then the finalizer writes the customer summary.
 *
 * <pre>
 * START -> supervisor -> {flight | hotel | itinerary | budget}_agent -> supervisor -> ... -> finalize -> END
 * </pre>
 */
@Component
public class TripPlannerGraph {

    private final CompiledGraph<PlannerState> graph;

    public TripPlannerGraph(Supervisor supervisor, FlightAgent flight, HotelAgent hotel,
                            ItineraryAgent itinerary, BudgetAgent budget, Finalizer finalizer)
            throws GraphStateException {
        Map<String, org.bsc.langgraph4j.action.NodeAction<PlannerState>> specialists = Map.of(
                FlightAgent.NAME, flight,
                HotelAgent.NAME, hotel,
                ItineraryAgent.NAME, itinerary,
                BudgetAgent.NAME, budget);

        StateGraph<PlannerState> g = new StateGraph<>(PlannerState.SCHEMA, PlannerState::new)
                .addNode(Supervisor.NAME, node_async(supervisor))
                .addNode(Finalizer.NAME, node_async(finalizer))
                .addEdge(START, Supervisor.NAME)
                .addEdge(Finalizer.NAME, END);
        for (var entry : specialists.entrySet()) {
            g.addNode(entry.getKey(), node_async(entry.getValue()));
            g.addEdge(entry.getKey(), Supervisor.NAME);
        }
        g.addConditionalEdges(Supervisor.NAME, edge_async(PlannerState::next), Map.of(
                FlightAgent.NAME, FlightAgent.NAME,
                HotelAgent.NAME, HotelAgent.NAME,
                ItineraryAgent.NAME, ItineraryAgent.NAME,
                BudgetAgent.NAME, BudgetAgent.NAME,
                Supervisor.FINISH, Finalizer.NAME));
        this.graph = g.compile();
    }

    public TripPlan plan(TripRequest req) {
        PlannerState s = graph.invoke(Map.of(PlannerState.REQUEST, req),
                        RunnableConfig.builder().recursionLimit(40).build())
                .orElseThrow(() -> new IllegalStateException("Planner graph produced no state"));
        TripPlan.Budget budget = s.budget().orElseThrow();
        return new TripPlan(
                req.customerName(),
                req.destination(),
                budget.withinBudget() ? TripPlan.Status.READY : TripPlan.Status.OVER_BUDGET,
                s.flight().orElseThrow(),
                s.hotel().orElseThrow(),
                s.itinerary().orElse(List.of()),
                budget,
                s.summary(),
                List.copyOf(s.trace()));
    }
}
