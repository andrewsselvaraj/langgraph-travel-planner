package com.travelplanner.model;

import java.time.Instant;
import java.util.UUID;

public record TripResponse(UUID id, Instant createdAt, TripRequest request, TripPlan plan) {
}
