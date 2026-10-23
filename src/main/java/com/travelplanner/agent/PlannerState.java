package com.travelplanner.agent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.Channel;
import org.bsc.langgraph4j.state.Channels;

import com.travelplanner.model.TripPlan.AgentStep;
import com.travelplanner.model.TripPlan.Budget;
import com.travelplanner.model.TripPlan.DayPlan;
import com.travelplanner.model.TripPlan.Flight;
import com.travelplanner.model.TripPlan.Hotel;
import com.travelplanner.model.TripRequest;

/** Shared state passed between the supervisor and the specialist agents. */
public class PlannerState extends AgentState {

    public static final String REQUEST = "request";
    public static final String FLIGHT = "flight";
    public static final String HOTEL = "hotel";
    public static final String ITINERARY = "itinerary";
    public static final String BUDGET = "budget";
    /** Hotel star ceiling; the budget agent lowers it to ask for cheaper stays. */
    public static final String MAX_HOTEL_STARS = "maxHotelStars";
    public static final String REVISIONS = "revisions";
    public static final String NEXT = "next";
    public static final String SUMMARY = "summary";
    public static final String TRACE = "trace";

    public static final Map<String, Channel<?>> SCHEMA = Map.of(
            TRACE, Channels.appenderWithDuplicate(ArrayList::new));

    public PlannerState(Map<String, Object> data) {
        super(data);
    }

    public TripRequest request() {
        return this.<TripRequest>value(REQUEST).orElseThrow();
    }

    public Optional<Flight> flight() {
        return value(FLIGHT);
    }

    public Optional<Hotel> hotel() {
        return value(HOTEL);
    }

    public Optional<List<DayPlan>> itinerary() {
        return value(ITINERARY);
    }

    public Optional<Budget> budget() {
        return value(BUDGET);
    }

    public int maxHotelStars() {
        return value(MAX_HOTEL_STARS, 5);
    }

    public int revisions() {
        return value(REVISIONS, 0);
    }

    public String next() {
        return value(NEXT, "");
    }

    public String summary() {
        return value(SUMMARY, "");
    }

    public List<AgentStep> trace() {
        return value(TRACE, List::of);
    }

    static Map<String, Object> step(String agent, String summary) {
        return Map.of(TRACE, List.of(new AgentStep(agent, summary)));
    }
}
