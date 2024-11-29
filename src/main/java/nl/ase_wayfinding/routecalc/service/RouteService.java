package nl.ase_wayfinding.routecalc.service;

import nl.ase_wayfinding.routecalc.model.RealTimeData;
import nl.ase_wayfinding.routecalc.model.RouteDetails;
import nl.ase_wayfinding.routecalc.model.RouteRequest;
import nl.ase_wayfinding.routecalc.model.UserPreferences;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RouteService {

    private final ExternalServiceClient externalServiceClient;

    public RouteService(ExternalServiceClient externalServiceClient) {
        this.externalServiceClient = externalServiceClient;
    }

    public RouteDetails calculateRoute(RouteRequest request) {
        UserPreferences preferences = externalServiceClient.fetchUserPreferences(request.getUserId());
        RealTimeData realTimeData = externalServiceClient.fetchRealTimeData();
        String environmentalData = externalServiceClient.fetchEnvironmentalData();

        RouteDetails route = new RouteDetails();
        route.setRouteId("R" + System.currentTimeMillis());
        route.setWaypoints(request.getWaypoints());
        route.setEta("15 mins");
        route.setCostBreakdown("Fuel: $5, Toll: $2");

        // Adjust ETA based on preferences and real-time data
        if (preferences.isAvoidTraffic() && "Heavy Traffic".equals(realTimeData.getTrafficStatus())) {
            route.setEta("20 mins (avoiding traffic)");
        }

        // Eco-friendly route option
        if (preferences.isAvoidPollution() && "High Pollution".equals(environmentalData)) {
            route.setCostBreakdown("Eco-Friendly Route: Fuel: $4");
        }

        // Adjust cost breakdown if toll avoidance is enabled
        if (preferences.isAvoidTolls()) {
            route.setCostBreakdown("Fuel: $5, Toll: $0");
        }

        return route;
    }

    public RouteDetails getAlternativeRoute(RouteRequest request) {
        UserPreferences preferences = externalServiceClient.fetchUserPreferences(request.getUserId());
        RealTimeData realTimeData = externalServiceClient.fetchRealTimeData();

        RouteDetails alternativeRoute = new RouteDetails();
        alternativeRoute.setRouteId("A" + System.currentTimeMillis());
        alternativeRoute.setWaypoints(List.of("AlternativeStart", "AlternativeMid", "AlternativeEnd"));
        alternativeRoute.setEta("25 mins");
        alternativeRoute.setCostBreakdown("Fuel: $6, Toll: $0");

        // Adjust alternative route based on preferences
        if (preferences.isAvoidTraffic() && "Heavy Traffic".equals(realTimeData.getTrafficStatus())) {
            alternativeRoute.setEta("30 mins (avoiding traffic)");
        }
        if (preferences.isAvoidPollution()) {
            alternativeRoute.setCostBreakdown("Eco-Friendly Route: Fuel: $5");
        }

        return alternativeRoute;
    }

    public RouteDetails calculateMultiTransportRoute(RouteRequest request) {
        RealTimeData realTimeData = externalServiceClient.fetchRealTimeData();

        RouteDetails multiTransportRoute = new RouteDetails();
        multiTransportRoute.setRouteId("MT" + System.currentTimeMillis());
        multiTransportRoute.setWaypoints(List.of("TrainStation", "BusStop", "Destination"));
        multiTransportRoute.setEta("45 mins");
        multiTransportRoute.setCostBreakdown("Train: $10, Bus: $3");

        // Adjust ETA based on real-time traffic data
        if ("Heavy Traffic".equals(realTimeData.getTrafficStatus())) {
            multiTransportRoute.setEta("50 mins (adjusted for traffic delays)");
        }

        return multiTransportRoute;
    }

    public boolean validateStop(String routeId, double stopLat, double stopLng) {
        // Mocked data for validation; in a real scenario, fetch route details from a database or external service
        List<String> waypoints = List.of("StartPoint", "MidPoint", "EndPoint");
        String stopCoordinate = stopLat + "," + stopLng;

        return waypoints.contains(stopCoordinate);
    }
}
