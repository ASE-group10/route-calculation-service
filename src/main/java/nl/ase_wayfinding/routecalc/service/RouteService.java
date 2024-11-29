package nl.ase_wayfinding.routecalc.service;

import nl.ase_wayfinding.routecalc.model.RouteDetails;
import nl.ase_wayfinding.routecalc.model.RouteRequest;
import nl.ase_wayfinding.routecalc.model.UserPreferences;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RouteService {

    private final ExternalServiceClient externalServiceClient;

    public RouteService(ExternalServiceClient externalServiceClient) {
        this.externalServiceClient = externalServiceClient;
    }

    public RouteDetails calculateRoute(RouteRequest request) {
        UserPreferences preferences = externalServiceClient.fetchUserPreferences(request.getUserId());
        String environmentalData = externalServiceClient.fetchEnvironmentalData();

        RouteDetails route = new RouteDetails();
        route.setRouteId("R" + System.currentTimeMillis());
        route.setWaypoints(request.getWaypoints());
        route.setEta("15 mins");

        if (preferences.isAvoidTraffic()) {
            route.setEta("20 mins (avoiding traffic)");
        }
        if (preferences.isAvoidPollution()) {
            route.setCostBreakdown("Eco-Friendly Route");
        }

        return route;
    }

    public RouteDetails getAlternativeRoute(RouteRequest request) {
        // Similar to calculateRoute but with alternative route logic
        return new RouteDetails();
    }

    public RouteDetails calculateMultiTransportRoute(RouteRequest request) {
        // Logic for multi-transport route
        return new RouteDetails();
    }

    public boolean validateStop(String routeId, double stopLat, double stopLng) {
        // Mocked validation logic
        return true;
    }
}
