package com.travelplanner.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.travelplanner.client.AgentServiceException;
import com.travelplanner.service.TripNotFoundException;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(TripNotFoundException.class)
    ProblemDetail notFound(TripNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(AgentServiceException.class)
    ProblemDetail agentsDown(AgentServiceException e) {
        log.error("Agent service call failed", e);
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY,
                "We couldn't plan your trip right now. Please try again shortly.");
    }
}
