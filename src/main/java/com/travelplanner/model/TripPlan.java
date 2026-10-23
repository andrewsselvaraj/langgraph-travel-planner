package com.travelplanner.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

/** The finished plan produced by the agent graph. */
public record TripPlan(
        String customerName,
        String destination,
        Status status,
        Flight flight,
        Hotel hotel,
        List<DayPlan> itinerary,
        Budget budget,
        String summary,
        List<AgentStep> trace) implements Serializable {

    public enum Status { READY, OVER_BUDGET }

    public record Flight(String airline, String flightNumber, String depart, String arrive,
                         int stops, String cabin, double pricePerPerson) implements Serializable {
    }

    public record Hotel(String name, int stars, String neighborhood, double pricePerNight,
                        double rating) implements Serializable {
    }

    public record DayPlan(int day, LocalDate date, String title, List<String> activities,
                          double estimatedCost) implements Serializable {
    }

    public record Budget(double flights, double hotel, double activities, double foodAndTransport,
                         double total, double budget, boolean withinBudget,
                         List<String> notes) implements Serializable {
    }

    public record AgentStep(String agent, String summary) implements Serializable {
    }
}
