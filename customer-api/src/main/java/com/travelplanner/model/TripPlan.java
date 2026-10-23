package com.travelplanner.model;

import java.time.LocalDate;
import java.util.List;

/** Plan returned by the LangGraph agent service. */
public record TripPlan(
        String customerName,
        String destination,
        String status,
        Flight flight,
        Hotel hotel,
        List<DayPlan> itinerary,
        Budget budget,
        String summary,
        List<AgentStep> trace) {

    public record Flight(String airline, String flightNumber, String depart, String arrive,
                         int stops, String cabin, double pricePerPerson) {
    }

    public record Hotel(String name, int stars, String neighborhood, double pricePerNight, double rating) {
    }

    public record DayPlan(int day, LocalDate date, String title, List<String> activities, double estimatedCost) {
    }

    public record Budget(double flights, double hotel, double activities, double foodAndTransport,
                         double total, double budget, boolean withinBudget, List<String> notes) {
    }

    public record AgentStep(String agent, String summary) {
    }
}
