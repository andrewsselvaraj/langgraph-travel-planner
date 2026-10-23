package com.travelplanner.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.travelplanner.model.TripPlan;
import com.travelplanner.model.TripRequest;

/** HTTP client for the LangGraph multi-agent service ({@code POST /plan}). */
@Component
public class AgentServiceClient {

    private final RestClient restClient;

    public AgentServiceClient(@Qualifier("agentRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public TripPlan plan(TripRequest request) {
        try {
            return restClient.post()
                    .uri("/plan")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(TripPlan.class);
        } catch (RestClientException e) {
            throw new AgentServiceException("Trip planning agents are unavailable: " + e.getMessage(), e);
        }
    }
}
