package com.travelplanner.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.travelplanner.model.TripPlan;
import com.travelplanner.model.TripRequest;
import com.travelplanner.model.TripResponse;
import com.travelplanner.service.TripNotFoundException;
import com.travelplanner.service.TripPlanningException;
import com.travelplanner.service.TripService;

@WebMvcTest(TripController.class)
class TripControllerTest {

    private static final LocalDate START = LocalDate.now().plusDays(30);

    @Autowired
    MockMvc mvc;

    @MockitoBean
    TripService trips;

    private String body(LocalDate start, LocalDate end) {
        return """
                {"customerName":"Ana","origin":"SFO","destination":"Lisbon",
                 "startDate":"%s","endDate":"%s","travelers":2,"budget":4000,
                 "interests":["food"]}""".formatted(start, end);
    }

    @Test
    void plansTrip() throws Exception {
        TripRequest req = new TripRequest("Ana", "SFO", "Lisbon", START, START.plusDays(4), 2, 4000.0,
                null, List.of("food"), null);
        TripPlan plan = new TripPlan("Ana", "Lisbon", TripPlan.Status.READY, null, null, List.of(), null, "Enjoy!", List.of());
        UUID id = UUID.randomUUID();
        when(trips.plan(any())).thenReturn(new TripResponse(id, Instant.now(), req, plan));

        mvc.perform(post("/api/v1/trips").contentType(MediaType.APPLICATION_JSON)
                        .content(body(START, START.plusDays(4))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/trips/" + id))
                .andExpect(jsonPath("$.plan.status").value("READY"))
                .andExpect(jsonPath("$.request.currency").value("USD"));
    }

    @Test
    void rejectsInvertedDates() throws Exception {
        mvc.perform(post("/api/v1/trips").contentType(MediaType.APPLICATION_JSON)
                        .content(body(START, START.minusDays(1))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returns503WhenPlanningFails() throws Exception {
        when(trips.plan(any())).thenThrow(new TripPlanningException(new IllegalStateException("boom")));
        mvc.perform(post("/api/v1/trips").contentType(MediaType.APPLICATION_JSON)
                        .content(body(START, START.plusDays(4))))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void returns404ForUnknownTrip() throws Exception {
        UUID id = UUID.randomUUID();
        when(trips.get(id)).thenThrow(new TripNotFoundException(id));
        mvc.perform(get("/api/v1/trips/" + id)).andExpect(status().isNotFound());
    }
}
