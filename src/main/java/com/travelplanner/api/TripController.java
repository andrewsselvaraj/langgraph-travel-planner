package com.travelplanner.api;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.travelplanner.model.TripRequest;
import com.travelplanner.model.TripResponse;
import com.travelplanner.service.TripService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/trips")
public class TripController {

    private final TripService trips;

    public TripController(TripService trips) {
        this.trips = trips;
    }

    @PostMapping
    public ResponseEntity<TripResponse> plan(@Valid @RequestBody TripRequest request) {
        TripResponse trip = trips.plan(request);
        return ResponseEntity.created(URI.create("/api/v1/trips/" + trip.id())).body(trip);
    }

    @GetMapping("/{id}")
    public TripResponse get(@PathVariable UUID id) {
        return trips.get(id);
    }

    @GetMapping
    public List<TripResponse> list(@RequestParam(required = false) String customerName) {
        return trips.list(customerName);
    }
}
