package nl.ase_wayfinding.routecalc.service;

import nl.ase_wayfinding.routecalc.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RouteServiceTest {

    @Mock
    private ExternalServiceClient externalServiceClient;

    @InjectMocks
    private RouteService routeService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testCalculateRoute() {
        RouteRequest request = new RouteRequest();
        request.setUserId("user1");
        request.setWaypoints(List.of("PointA", "PointB"));

        RoutePreference preferences = new RoutePreference();
        preferences.setEcoFriendly(true);
        preferences.setAvoidDangerousStreets(true);

        RealTimeData realTimeData = new RealTimeData();
        realTimeData.setTrafficStatus("Heavy Traffic");

        FeatureCollection environmentalData = new FeatureCollection();

        when(externalServiceClient.fetchUserPreferences("user1")).thenReturn(preferences);
        when(externalServiceClient.fetchRealTimeData()).thenReturn(realTimeData);
        when(externalServiceClient.fetchEnvironmentalData()).thenReturn(environmentalData);

        RouteDetails result = routeService.calculateRoute(request);

        assertNotNull(result);
        assertEquals("PointA", result.getWaypoints().get(0));
        assertTrue(result.getEta().contains("mins"));
        verify(externalServiceClient, times(1)).fetchUserPreferences("user1");
        verify(externalServiceClient, times(1)).fetchRealTimeData();
        verify(externalServiceClient, times(1)).fetchEnvironmentalData();
    }

    @Test
    void testGetAlternativeRoute() {
        RouteRequest request = new RouteRequest();
        request.setUserId("user1");

        RoutePreference preferences = new RoutePreference();
        preferences.setAvoidHighways(true);

        RealTimeData realTimeData = new RealTimeData();
        realTimeData.setTrafficStatus("Moderate Traffic");

        when(externalServiceClient.fetchUserPreferences("user1")).thenReturn(preferences);
        when(externalServiceClient.fetchRealTimeData()).thenReturn(realTimeData);

        RouteDetails result = routeService.getAlternativeRoute(request);

        assertNotNull(result);
        assertTrue(result.getEta().contains("mins"));
        verify(externalServiceClient, times(1)).fetchUserPreferences("user1");
        verify(externalServiceClient, times(1)).fetchRealTimeData();
    }

    @Test
    void testCalculateMultiTransportRoute() {
        RouteRequest request = new RouteRequest();
        request.setUserId("user1");

        RealTimeData realTimeData = new RealTimeData();
        realTimeData.setTrafficStatus("Heavy Traffic");

        when(externalServiceClient.fetchRealTimeData()).thenReturn(realTimeData);

        RouteDetails result = routeService.calculateMultiTransportRoute(request);

        assertNotNull(result);
        assertEquals("MT", result.getRouteId().substring(0, 2));
        verify(externalServiceClient, times(1)).fetchRealTimeData();
    }

    @Test
    void testValidateStop() {
        boolean result = routeService.validateStop("R1", 10.0, 20.0);
        assertFalse(result);
    }
}
