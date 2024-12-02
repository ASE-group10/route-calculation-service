package nl.ase_wayfinding.routecalc.service;

import nl.ase_wayfinding.routecalc.model.*;
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
        RoutePreference preferences = externalServiceClient.fetchUserPreferences(request.getUserId());
        RealTimeData realTimeData = externalServiceClient.fetchRealTimeData();
        FeatureCollection environmentalData = externalServiceClient.fetchEnvironmentalData();

        RouteDetails route = new RouteDetails();
        route.setRouteId("R" + System.currentTimeMillis());
        route.setWaypoints(request.getWaypoints());
        route.setEta("15 mins");
        route.setCostBreakdown("Fuel: $5, Toll: $2");

        // Adjust ETA based on preferences and real-time data
//        if (preferences.getAvoidDangerousStreets() && "Heavy Traffic".equals(realTimeData.getTrafficStatus())) {
        if (preferences.getAvoidDangerousStreets() && !realTimeData.getTrafficStatus().isEmpty()) {
            route.setEta("20 mins (avoiding dangerous streets)");
        }

        // Eco-friendly route adjustment
        if (preferences.getEcoFriendly() && containsHighPollution(environmentalData)) {
            route.setCostBreakdown("Eco-Friendly Route: Fuel: $4");
        }

        // Adjust cost breakdown if toll avoidance is enabled
        if (preferences.getAvoidTolls() != null && preferences.getAvoidTolls()) {
            route.setCostBreakdown("Fuel: $5, Toll: $0");
        }

        return route;
    }

    public RouteDetails getAlternativeRoute(RouteRequest request) {
        RoutePreference preferences = externalServiceClient.fetchUserPreferences(request.getUserId());
        RealTimeData realTimeData = externalServiceClient.fetchRealTimeData();

        RouteDetails alternativeRoute = new RouteDetails();
        alternativeRoute.setRouteId("A" + System.currentTimeMillis());
        alternativeRoute.setWaypoints(List.of("AlternativeStart", "AlternativeMid", "AlternativeEnd"));
        alternativeRoute.setEta("25 mins");
        alternativeRoute.setCostBreakdown("Fuel: $6, Toll: $0");

        // Adjust alternative route based on preferences
        if (preferences.getAvoidHighways() && "Heavy Traffic".equals(realTimeData.getTrafficStatus())) {
            alternativeRoute.setEta("30 mins (avoiding highways)");
        }
        if (preferences.getMinimizeCo2()) {
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

    public boolean containsHighPollution(FeatureCollection featureCollection) {
        if (featureCollection == null || featureCollection.getFeatures() == null) {
            return false;
        }

        for (Feature feature : featureCollection.getFeatures()) {
            if (feature.getProperties() != null && feature.getProperties().getCO2_mgm3() > 50.0) {
                return true;
            }
        }
        return false;
    }
}
