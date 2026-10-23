package com.travelplanner.service;

public class TripPlanningException extends RuntimeException {

    public TripPlanningException(Throwable cause) {
        super("Trip planning failed: " + cause.getMessage(), cause);
    }
}
