package com.travelplanner.agent;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.springframework.stereotype.Component;

import com.travelplanner.model.TripPlan.Flight;
import com.travelplanner.model.TripPlan.Hotel;
import com.travelplanner.model.TripRequest;

/**
 * Mock flight and hotel inventory. Results are deterministic for a given
 * route and dates so plans are reproducible. Replace these methods with real
 * providers (Amadeus, Skyscanner, Booking.com, ...).
 */
@Component
public class TravelInventory {

    private static final Map<String, Double> CABIN_MULTIPLIER =
            Map.of("economy", 1.0, "premium_economy", 1.6, "business", 3.2);
    private static final List<String> AIRLINES = List.of("SkyWays", "Oceanic", "BlueJet", "AeroNova", "TransGlobal");
    private static final List<String> HOTEL_PREFIXES = List.of("Grand", "Harbor", "Old Town", "Riverside", "Central", "Garden");
    private static final List<String> HOTEL_SUFFIXES = List.of("Hotel", "Suites", "Inn", "Residences", "Lodge");
    private static final List<String> NEIGHBORHOODS =
            List.of("City Center", "Old Town", "Waterfront", "Arts District", "Museum Quarter");

    public List<Flight> searchFlights(TripRequest req) {
        Random rng = seeded("flights", req.origin().toLowerCase(), req.destination().toLowerCase(), req.startDate());
        double base = uniform(rng, 180, 900) * CABIN_MULTIPLIER.get(req.travelClass());
        List<Flight> options = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            int stops = pick(rng, List.of(0, 0, 1, 1, 2));
            int depHour = 6 + rng.nextInt(16);
            int duration = 2 + rng.nextInt(11) + stops * 2;
            options.add(new Flight(
                    pick(rng, AIRLINES),
                    "" + "ABCDEFGH".charAt(rng.nextInt(8)) + "XYZ".charAt(rng.nextInt(3)) + (100 + rng.nextInt(900)),
                    "%sT%02d:%s".formatted(req.startDate(), depHour, pick(rng, List.of("00", "15", "30", "45"))),
                    "+" + duration + "h",
                    stops,
                    req.travelClass(),
                    round2(base * (1.25 - 0.15 * stops) * uniform(rng, 0.85, 1.2))));
        }
        options.sort(Comparator.comparingDouble(Flight::pricePerPerson));
        return options;
    }

    /** Hotels at or below {@code maxStars}, best rated first. */
    public List<Hotel> searchHotels(TripRequest req, int maxStars) {
        Random rng = seeded("hotels", req.destination().toLowerCase(), req.startDate(), req.endDate());
        String city = capitalize(req.destination());
        List<Hotel> all = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            int stars = 2 + rng.nextInt(4);
            all.add(new Hotel(
                    pick(rng, HOTEL_PREFIXES) + " " + city + " " + pick(rng, HOTEL_SUFFIXES),
                    stars,
                    pick(rng, NEIGHBORHOODS),
                    round2(stars * uniform(rng, 35, 70) * Math.max(1, req.travelers() / 2.0)),
                    Math.round(Math.min(5.0, stars * 0.8 + uniform(rng, 0.5, 1.4)) * 10) / 10.0));
        }
        return all.stream()
                .filter(h -> h.stars() <= maxStars)
                .limit(6)
                .sorted(Comparator.comparingDouble(Hotel::rating).reversed()
                        .thenComparingDouble(Hotel::pricePerNight))
                .toList();
    }

    private static Random seeded(Object... parts) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(String.join("|", java.util.Arrays.stream(parts).map(String::valueOf).toList())
                            .getBytes(StandardCharsets.UTF_8));
            long seed = 0;
            for (int i = 0; i < 8; i++) {
                seed = (seed << 8) | (hash[i] & 0xff);
            }
            return new Random(seed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static double uniform(Random rng, double lo, double hi) {
        return lo + rng.nextDouble() * (hi - lo);
    }

    private static <T> T pick(Random rng, List<T> items) {
        return items.get(rng.nextInt(items.size()));
    }

    static double round2(double v) {
        return Math.round(v * 100) / 100.0;
    }

    static String capitalize(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }
}
