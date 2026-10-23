package com.travelplanner.service;

import java.util.UUID;

public class TripNotFoundException extends RuntimeException {

    public TripNotFoundException(UUID id) {
        super("Trip " + id + " not found");
    }
}
