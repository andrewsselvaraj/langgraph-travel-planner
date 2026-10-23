package com.travelplanner.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.client.MockRestServiceServer;

import com.travelplanner.config.AgentClientConfig;
import com.travelplanner.config.AgentServiceProperties;
import com.travelplanner.model.TripPlan;
import com.travelplanner.model.TripRequest;

@RestClientTest(AgentServiceClient.class)
@Import(AgentClientConfig.class)
@EnableConfigurationProperties(AgentServiceProperties.class)
@TestPropertySource(properties = "agent-service.base-url=http://agents.test")
class AgentServiceClientTest {

    @Autowired
    AgentServiceClient client;

    @Autowired
    MockRestServiceServer server;

    @Test
    void sendsSnakeCaseAndParsesPlan() {
        server.expect(requestTo("http://agents.test/plan"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.customer_name").value("Ana"))
                .andExpect(jsonPath("$.travel_class").value("economy"))
                .andRespond(withSuccess("""
                        {"customer_name":"Ana","destination":"Lisbon","status":"READY",
                         "flight":{"airline":"BlueJet","flight_number":"EZ237","depart":"2026-12-01T09:00",
                                   "arrive":"+11h","stops":0,"cabin":"economy","price_per_person":603.9},
                         "hotel":{"name":"Garden Suites","stars":4,"neighborhood":"Old Town",
                                  "price_per_night":266.4,"rating":4.3},
                         "itinerary":[{"day":1,"date":"2026-12-01","title":"Arrival",
                                       "activities":["Check in"],"estimated_cost":50.0}],
                         "budget":{"flights":2415.6,"hotel":1065.6,"activities":50,"food_and_transport":450,
                                   "total":3981.2,"budget":4000,"within_budget":true,"notes":["ok"]},
                         "summary":"Enjoy!","trace":[{"agent":"flight_agent","summary":"chose"}]}
                        """, MediaType.APPLICATION_JSON));

        TripPlan plan = client.plan(new TripRequest("Ana", "SFO", "Lisbon", LocalDate.of(2026, 12, 1),
                LocalDate.of(2026, 12, 5), 2, 4000.0, null, List.of(), null));

        assertThat(plan.flight().pricePerPerson()).isEqualTo(603.9);
        assertThat(plan.budget().withinBudget()).isTrue();
        assertThat(plan.itinerary()).hasSize(1);
        server.verify();
    }
}
