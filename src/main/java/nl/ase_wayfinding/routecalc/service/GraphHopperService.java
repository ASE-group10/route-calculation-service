package nl.ase_wayfinding.routecalc.service;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.ResponsePath;
import com.graphhopper.config.Profile;
import com.graphhopper.util.CustomModel;
import com.graphhopper.util.PointList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.graphhopper.json.Statement.If;
import static com.graphhopper.json.Statement.Op.LIMIT;
import static com.graphhopper.json.Statement.Op.MULTIPLY;

@Service
public class GraphHopperService {

    // Main loggers for different concerns
    private static final Logger flowLogger = LoggerFactory.getLogger("RouteFlowLogger");
    private static final Logger envLogger = LoggerFactory.getLogger("EnvironmentalLogger");
    private static final Logger altLogger = LoggerFactory.getLogger("AlternativeRouteLogger");

    private GraphHopper hopper;
    // Example threshold for environmental sustainability (range: 0 to 1)
    private static final double ENVIRONMENTAL_THRESHOLD = 0.75;
    // Maximum recursion depth to avoid infinite loops
    private static final int MAX_ALT_ROUTE_DEPTH = 5;

    @PostConstruct
    public void init() {
        // Paths for your OSM file and graph cache
        String osmFile = "src/main/resources/data/ireland-and-northern-ireland-latest.osm.pbf";
        String graphFolder = "graph-cache";

        // Verify that the OSM file exists
        File osmData = new File(osmFile);
        if (!osmData.exists()) {
            throw new IllegalStateException("OSM file not found: " + osmData.getAbsolutePath());
        }
        CustomModel customModel = new CustomModel()
                .addToPriority(If("road_access == DESTINATION", MULTIPLY, "0.1"))
                .addToSpeed(If("true", LIMIT, "max_speed")); // Fixed line

        hopper = new GraphHopper()
                .setOSMFile(osmFile)
                .setGraphHopperLocation(graphFolder)
                .setProfiles(new Profile("car")
                        .setVehicle("car")
                        .setWeighting("custom")
                        .setCustomModel(customModel));

        hopper.importOrLoad();
        flowLogger.info("GraphHopper initialized with OSM file: {} and graph folder: {}", osmFile, graphFolder);
    }

    public GHResponse getRoute(double fromLat, double fromLon, double toLat, double toLon) {
        GHRequest request = new GHRequest(fromLat, fromLon, toLat, toLon)
                .setProfile("car");

        flowLogger.info("Calculating route from ({}, {}) to ({}, {})", fromLat, fromLon, toLat, toLon);
        return hopper.route(request);
    }

    // Updated method to return waypoints while checking environmental factors.
    // For every waypoint with a failing environmental score, an alternative route is calculated.
    public Map<String, Object> getWaypoints(double fromLat, double fromLon, double toLat, double toLon) {
        GHRequest request = new GHRequest(fromLat, fromLon, toLat, toLon)
                .setProfile("car");

        flowLogger.info("Fetching waypoints for route from ({}, {}) to ({}, {})", fromLat, fromLon, toLat, toLon);
        GHResponse response = hopper.route(request);
        if (response.hasErrors()) {
            flowLogger.error("Error in route calculation: {}", response.getErrors());
            throw new RuntimeException("Error: " + response.getErrors());
        }

        ResponsePath path = response.getBest();
        PointList points = path.getPoints();
        List<Map<String, Object>> waypoints = new ArrayList<>();

        // Process each waypoint on the primary route
        for (int i = 0; i < points.size(); i++) {
            double lat = points.getLat(i);
            double lon = points.getLon(i);
            Map<String, Object> waypoint = new HashMap<>();
            waypoint.put("lat", lat);
            waypoint.put("lon", lon);

            flowLogger.debug("Processing waypoint {}: lat={}, lon={}", i, lat, lon);
            double envScore = checkEnvironmentalFactors(lat, lon);
            waypoint.put("environmentalScore", envScore);
            envLogger.debug("Waypoint {} environmental score: {}", i, envScore);

            // If the environmental score is below the threshold, calculate an alternative route recursively.
            if (envScore < ENVIRONMENTAL_THRESHOLD) {
                envLogger.info("Environmental score {} below threshold {} at waypoint {}. Calculating alternative route.",
                        envScore, ENVIRONMENTAL_THRESHOLD, i);
                Map<String, Object> altRoute = calculateAlternativeRouteRecursive(lat, lon, toLat, toLon, 0);
                waypoint.put("alternativeRoute", altRoute);
                waypoint.put("redirected", true);
            } else {
                waypoint.put("redirected", false);
                envLogger.debug("Waypoint {} passed environmental threshold (score: {})", i, envScore);
            }
            waypoints.add(waypoint);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("distance_km", path.getDistance() / 1000.0);
        result.put("time_min", path.getTime() / 60000.0);
        result.put("waypoints", waypoints);
        flowLogger.info("Route fetched with {} waypoints", waypoints.size());
        return result;
    }

    // Simulated method to evaluate environmental factors for a coordinate.
    private double checkEnvironmentalFactors(double lat, double lon) {
        double latRad = Math.toRadians(lat);
        double lonRad = Math.toRadians(lon);
        double rawScore = (Math.sin(latRad) + Math.cos(lonRad)) / 2.0;
        double adjustedScore = rawScore * 0.5 + 0.5;
        envLogger.debug("For lat={}, lon={}, rawScore={}, adjustedScore={}", lat, lon, rawScore, adjustedScore);
        return Math.max(0, Math.min(1, adjustedScore));
    }

    // Recursive method to calculate an alternative route.
    // It calculates an alternative route from a given starting point to the destination,
    // then checks the candidate new starting point (the second waypoint) and, if necessary,
    // recalculates further until the candidate passes the environmental check or max depth is reached.
    private Map<String, Object> calculateAlternativeRouteRecursive(double fromLat, double fromLon, double toLat, double toLon, int depth) {
        if (depth > MAX_ALT_ROUTE_DEPTH) {
            altLogger.warn("Max alternative route recursion depth reached at fromLat={}, fromLon={}", fromLat, fromLon);
            return null;
        }
        altLogger.info("Calculating alternative route (depth {}) from ({}, {}) to ({}, {})", depth, fromLat, fromLon, toLat, toLon);
        GHRequest altRequest = new GHRequest(fromLat, fromLon, toLat, toLon)
                .setProfile("car");
        GHResponse altResponse = hopper.route(altRequest);
        if (altResponse.hasErrors()) {
            altLogger.error("Error calculating alternative route: {}", altResponse.getErrors());
            return null;
        }
        ResponsePath altPath = altResponse.getBest();
        PointList altPoints = altPath.getPoints();
        List<Map<String, Double>> altWaypoints = new ArrayList<>();
        for (int i = 0; i < altPoints.size(); i++) {
            Map<String, Double> wp = new HashMap<>();
            wp.put("lat", altPoints.getLat(i));
            wp.put("lon", altPoints.getLon(i));
            altWaypoints.add(wp);
        }
        Map<String, Object> altRouteData = new HashMap<>();
        altRouteData.put("distance_km", altPath.getDistance() / 1000.0);
        altRouteData.put("time_min", altPath.getTime() / 60000.0);
        altRouteData.put("waypoints", altWaypoints);
        altLogger.debug("Alternative route (depth {}) computed: {} waypoints, distance: {} km, time: {} minutes",
                depth, altWaypoints.size(), altRouteData.get("distance_km"), altRouteData.get("time_min"));

        // Use the second waypoint (index 1) as the candidate new starting point if available
        if (altWaypoints.size() > 1) {
            double newFromLat = altWaypoints.get(1).get("lat");
            double newFromLon = altWaypoints.get(1).get("lon");
            double candidateEnvScore = checkEnvironmentalFactors(newFromLat, newFromLon);
            altLogger.debug("Depth {} candidate starting point: lat={}, lon={}, environmental score={}",
                    depth, newFromLat, newFromLon, candidateEnvScore);
            if (candidateEnvScore < ENVIRONMENTAL_THRESHOLD) {
                altLogger.info("Candidate starting point at alternative route depth {} fails environmental check (score: {}). Recalculating alternative route.",
                        depth, candidateEnvScore);
                return calculateAlternativeRouteRecursive(newFromLat, newFromLon, toLat, toLon, depth + 1);
            } else {
                altLogger.info("Candidate starting point at alternative route depth {} passes environmental check (score: {}).",
                        depth, candidateEnvScore);
            }
        }
        return altRouteData;
    }
}
