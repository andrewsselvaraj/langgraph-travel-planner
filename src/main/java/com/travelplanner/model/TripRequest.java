package com.travelplanner.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record TripRequest(
        @NotBlank String customerName,
        @NotBlank @Size(min = 2) String origin,
        @NotBlank @Size(min = 2) String destination,
        @NotNull @Future LocalDate startDate,
        @NotNull LocalDate endDate,
        @Min(1) @Max(10) Integer travelers,
        @NotNull @Positive Double budget,
        @Pattern(regexp = "[A-Z]{3}") String currency,
        List<@NotBlank String> interests,
        @Pattern(regexp = "economy|premium_economy|business") String travelClass) implements Serializable {

    public TripRequest {
        travelers = travelers == null ? 1 : travelers;
        currency = currency == null ? "USD" : currency;
        interests = interests == null ? List.of() : List.copyOf(interests);
        travelClass = travelClass == null ? "economy" : travelClass;
    }

    public int nights() {
        return (int) java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
    }

    @AssertTrue(message = "endDate must be after startDate")
    boolean isDateRangeValid() {
        return startDate == null || endDate == null || endDate.isAfter(startDate);
    }
}
