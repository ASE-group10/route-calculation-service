package nl.ase_wayfinding.routecalc.controller;

import nl.ase_wayfinding.routecalc.model.RouteDetails;
import nl.ase_wayfinding.routecalc.model.RouteRequest;
import nl.ase_wayfinding.routecalc.service.RouteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RouteControllerTest {

    @Mock
    private RouteService routeService;

    @InjectMocks
    private RouteController routeController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testCalculateRoute() {
        RouteRequest request = new RouteRequest();
        request.setUserId("testUser");
        request.setWaypoints(List.of("PointA", "PointB"));

        RouteDetails routeDetails = new RouteDetails();
        routeDetails.setRouteId("R1");
        routeDetails.setEta("15 mins");

        when(routeService.calculateRoute(any(RouteRequest.class))).thenReturn(routeDetails);

        ResponseEntity<RouteDetails> response = routeController.calculateRoute(request);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals("R1", response.getBody().getRouteId());
        assertEquals("15 mins", response.getBody().getEta());

        verify(routeService, times(1)).calculateRoute(any(RouteRequest.class));
    }

    @Test
    void testGetAlternativeRoutes() {
        RouteRequest request = new RouteRequest();
        request.setUserId("testUser");

        RouteDetails alternativeRoute = new RouteDetails();
        alternativeRoute.setRouteId("A1");
        alternativeRoute.setEta("25 mins");

        when(routeService.getAlternativeRoute(any(RouteRequest.class))).thenReturn(alternativeRoute);

        ResponseEntity<RouteDetails> response = routeController.getAlternativeRoutes(request);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals("A1", response.getBody().getRouteId());
        assertEquals("25 mins", response.getBody().getEta());

        verify(routeService, times(1)).getAlternativeRoute(any(RouteRequest.class));
    }

    @Test
    void testCalculateMultiTransportRoute() {
        RouteRequest request = new RouteRequest();
        request.setUserId("testUser");

        RouteDetails multiTransportRoute = new RouteDetails();
        multiTransportRoute.setRouteId("MT1");
        multiTransportRoute.setEta("45 mins");

        when(routeService.calculateMultiTransportRoute(any(RouteRequest.class))).thenReturn(multiTransportRoute);

        ResponseEntity<RouteDetails> response = routeController.calculateMultiTransportRoute(request);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals("MT1", response.getBody().getRouteId());
        assertEquals("45 mins", response.getBody().getEta());

        verify(routeService, times(1)).calculateMultiTransportRoute(any(RouteRequest.class));
    }

    @Test
    void testValidateStop() {
        when(routeService.validateStop("R1", 10.0, 20.0)).thenReturn(true);

        ResponseEntity<Boolean> response = routeController.validateStop("R1", 10.0, 20.0);

        assertEquals(200, response.getStatusCodeValue());
        assertTrue(response.getBody());

        verify(routeService, times(1)).validateStop("R1", 10.0, 20.0);
    }

    @Test
    void testCalculateRouteWithNullRequest() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            routeController.calculateRoute(null);
        });

        assertEquals("Request body cannot be null", exception.getMessage());
    }
}
