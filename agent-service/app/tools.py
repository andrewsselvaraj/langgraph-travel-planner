"""Mock travel inventory.

Deterministic stand-ins for real flight/hotel providers so the agents can run
offline. Swap these functions for Amadeus, Skyscanner, Booking.com, etc.
"""
from __future__ import annotations

import hashlib
import random

from .models import FlightOption, HotelOption, TripRequest

CABIN_MULTIPLIER = {"economy": 1.0, "premium_economy": 1.6, "business": 3.2}
AIRLINES = ["SkyWays", "Oceanic", "BlueJet", "AeroNova", "TransGlobal"]
HOTEL_PREFIXES = ["Grand", "Harbor", "Old Town", "Riverside", "Central", "Garden"]
HOTEL_SUFFIXES = ["Hotel", "Suites", "Inn", "Residences", "Lodge"]
NEIGHBORHOODS = ["City Center", "Old Town", "Waterfront", "Arts District", "Museum Quarter"]


def _rng(*parts: object) -> random.Random:
    seed = hashlib.sha256("|".join(map(str, parts)).encode()).hexdigest()
    return random.Random(int(seed[:16], 16))


def search_flights(req: TripRequest, limit: int = 5) -> list[FlightOption]:
    rng = _rng("flights", req.origin.lower(), req.destination.lower(), req.start_date)
    base = rng.uniform(180, 900) * CABIN_MULTIPLIER[req.travel_class]
    options = []
    for i in range(limit):
        stops = rng.choice([0, 0, 1, 1, 2])
        dep_hour = rng.randint(6, 21)
        duration = rng.randint(2, 12) + stops * 2
        options.append(
            FlightOption(
                airline=rng.choice(AIRLINES),
                flight_number=f"{rng.choice('ABCDEFGH')}{rng.choice('XYZ')}{rng.randint(100, 999)}",
                depart=f"{req.start_date}T{dep_hour:02d}:{rng.choice(['00', '15', '30', '45'])}",
                arrive=f"+{duration}h",
                stops=stops,
                cabin=req.travel_class,
                price_per_person=round(base * (1.25 - 0.15 * stops) * rng.uniform(0.85, 1.2), 2),
            )
        )
    return sorted(options, key=lambda f: f.price_per_person)


def search_hotels(req: TripRequest, max_stars: int = 5, limit: int = 6) -> list[HotelOption]:
    rng = _rng("hotels", req.destination.lower(), req.start_date, req.end_date)
    options = []
    for _ in range(limit * 2):
        stars = rng.randint(2, 5)
        options.append(
            HotelOption(
                name=f"{rng.choice(HOTEL_PREFIXES)} {req.destination.title()} {rng.choice(HOTEL_SUFFIXES)}",
                stars=stars,
                neighborhood=rng.choice(NEIGHBORHOODS),
                price_per_night=round(stars * rng.uniform(35, 70) * max(1, req.travelers / 2), 2),
                rating=round(min(5.0, stars * 0.8 + rng.uniform(0.5, 1.4)), 1),
            )
        )
    options = [h for h in options if h.stars <= max_stars][:limit]
    return sorted(options, key=lambda h: (-h.rating, h.price_per_night))
