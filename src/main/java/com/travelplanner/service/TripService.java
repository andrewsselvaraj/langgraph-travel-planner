package com.travelplanner.service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.travelplanner.agent.TripPlannerGraph;
import com.travelplanner.model.TripPlan;
import com.travelplanner.model.TripRequest;
import com.travelplanner.model.TripResponse;

/**
 * Plans trips with the multi-agent graph and keeps them in memory.
 * Replace the map with a JPA repository for persistence.
 */
@Service
public class TripService {

    private final TripPlannerGraph planner;
    private final Map<UUID, TripResponse> trips = new ConcurrentHashMap<>();

    public TripService(TripPlannerGraph planner) {
        this.planner = planner;
    }

    public TripResponse plan(TripRequest request) {
        TripPlan plan;
        try {
            plan = planner.plan(request);
        } catch (RuntimeException e) {
            throw new TripPlanningException(e);
        }
        TripResponse trip = new TripResponse(UUID.randomUUID(), Instant.now(), request, plan);
        trips.put(trip.id(), trip);
        return trip;
    }

    public TripResponse get(UUID id) {
        TripResponse trip = trips.get(id);
        if (trip == null) {
            throw new TripNotFoundException(id);
        }
        return trip;
    }

    public List<TripResponse> list(String customerName) {
        return trips.values().stream()
                .filter(t -> customerName == null || t.request().customerName().equalsIgnoreCase(customerName))
                .sorted(Comparator.comparing(TripResponse::createdAt).reversed())
                .toList();
    }
}
