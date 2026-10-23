package com.travelplanner.service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.travelplanner.client.AgentServiceClient;
import com.travelplanner.model.TripPlan;
import com.travelplanner.model.TripRequest;
import com.travelplanner.model.TripResponse;

/**
 * Plans trips via the agent service and keeps them in memory.
 * Replace the map with a JPA repository for persistence.
 */
@Service
public class TripService {

    private final AgentServiceClient agents;
    private final Map<UUID, TripResponse> trips = new ConcurrentHashMap<>();

    public TripService(AgentServiceClient agents) {
        this.agents = agents;
    }

    public TripResponse plan(TripRequest request) {
        TripPlan plan = agents.plan(request);
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
