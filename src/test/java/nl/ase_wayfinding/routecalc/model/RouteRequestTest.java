package nl.ase_wayfinding.routecalc.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RouteRequestTest {

    @Test
    void testRouteRequest() {
        RouteRequest routeRequest = new RouteRequest();
        routeRequest.setUserId("user123");
        routeRequest.setWaypoints(List.of("Start", "End"));

        assertEquals("user123", routeRequest.getUserId());
        assertEquals(List.of("Start", "End"), routeRequest.getWaypoints());
    }
}
