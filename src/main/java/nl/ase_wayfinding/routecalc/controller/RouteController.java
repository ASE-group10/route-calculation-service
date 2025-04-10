package nl.ase_wayfinding.routecalc.controller;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.ResponsePath;
import com.graphhopper.util.PointList;
import com.graphhopper.util.Instruction;
import com.graphhopper.util.details.PathDetail;
import nl.ase_wayfinding.routecalc.service.GraphHopperService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.graphhopper.util.TranslationMap;
import com.graphhopper.util.Translation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/route")
public class RouteController {
    private static final Logger logger = LoggerFactory.getLogger(RouteController.class);

    private final TranslationMap translationMap = new TranslationMap().doImport();
    private final GraphHopperService graphHopperService;

    public RouteController(GraphHopperService graphHopperService) {
        this.graphHopperService = graphHopperService;
    }

    @PostMapping
    public ResponseEntity<?> calculateRoute(@RequestBody Map<String, Object> body) {
        logger.info("Received route calculation request: {}", body);
        List<List<Double>> points = (List<List<Double>>) body.get("points");
        if (points.size() < 2) {
            logger.warn("Invalid request: Less than 2 points");
            return ResponseEntity.badRequest().body(Map.of("error", "At least two points required"));
        }

        // Check for chained routing (multiple segments)
        if (body.containsKey("modes")) {
            List<String> modes = (List<String>) body.get("modes");
            logger.info("Chained routing with modes: {}", modes);
            if (modes.size() != points.size() - 1) {
                logger.warn("Invalid mode count: Expected {} but got {}", points.size() - 1, modes.size());
                return ResponseEntity.badRequest().body(Map.of("error", "For chained route, number of modes must be one less than number of points"));
            }
            Map<String, Object> chainedRoute = graphHopperService.getChainedRoute(points, modes);
            logger.info("Chained route response: {}", chainedRoute);
            return ResponseEntity.ok(chainedRoute);
        }

        String mode = (String) body.getOrDefault("mode", "car");
        logger.info("Single-mode route requested, mode: {}", mode);

        if (mode.equalsIgnoreCase("bus")) {
            logger.info("Handling bus route with walking fallback");
            try {
                Map<String, Object> busRoute = graphHopperService.getBusRouteWithWalking(points);
                logger.info("Bus route response: {}", busRoute);
                return ResponseEntity.ok(busRoute);
            } catch (Exception ex) {
                logger.error("Error generating bus route", ex);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Bus route calculation failed", "details", ex.getMessage()));
            }
        }

        GHRequest request = new GHRequest(
                points.get(0).get(1), points.get(0).get(0),
                points.get(1).get(1), points.get(1).get(0)
        ).setProfile(mode);

        logger.info("GraphHopper request created: {}", request);

        Map<String, Object> routeResult = graphHopperService.getOptimizedRoute(request, mode);

        logger.info("Route result: {}", routeResult);

        if (routeResult.containsKey("error")) {
            logger.warn("Error in route result: {}", routeResult.get("error"));
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(routeResult);
        }
        int actualIterations = (int) routeResult.get("iterations");
        ResponsePath bestPath = (ResponsePath) routeResult.get("bestPath");
        GHResponse response = (GHResponse) routeResult.get("response");
        if (response == null || bestPath == null) {
            logger.error("Null response or best path in route result");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to generate a valid route"));
        }
        Map<String, Object> formattedResponse = buildFormattedResponse(actualIterations, mode, response, bestPath, routeResult);

        logger.info("Formatted response ready to return");
        return ResponseEntity.ok(formattedResponse);
    }

    private Map<String, Object> buildFormattedResponse(int actualIterations, String mode, GHResponse response, ResponsePath bestPath, Map<String, Object> routeResult) {
        Map<String, Object> formattedResponse = new HashMap<>();
        formattedResponse.put("status", "success");
        formattedResponse.put("message", "Optimal route found!");
        formattedResponse.put("iterations", actualIterations);
        formattedResponse.put("mode", mode);
        formattedResponse.put("hints", Map.of(
                "visited_nodes.sum", response.getHints().getInt("visited_nodes.sum", 0),
                "visited_nodes.average", response.getHints().getInt("visited_nodes.average", 0)
        ));
        formattedResponse.put("info", Map.of(
                "took", bestPath.getTime() / 1000,
                "roadDataTimestamp", Instant.now().toString()
        ));
        formattedResponse.put("paths", List.of(buildPathDetails(bestPath, response)));
        formattedResponse.put("bad_areas", routeResult.get("bad_areas") instanceof List && ((List<?>) routeResult.get("bad_areas")).isEmpty()
                ? null : routeResult.get("bad_areas"));
        return formattedResponse;
    }

    private Map<String, Object> buildPathDetails(ResponsePath bestPath, GHResponse response) {
        Map<String, Object> pathDetails = new HashMap<>();
        pathDetails.put("distance", bestPath.getDistance());
        pathDetails.put("weight", bestPath.getRouteWeight());
        pathDetails.put("time", bestPath.getTime());
        pathDetails.put("transfers", 0);
        pathDetails.put("points_encoded", false);
        pathDetails.put("points_encoded_multiplier", response.getHints().getInt("points_encoded_multiplier", 100000));
        pathDetails.put("points", extractCoordinates(bestPath));
        pathDetails.put("instructions", extractInstructions(bestPath));
        pathDetails.put("details", Map.of("road_class", extractRoadClassDetails(bestPath)));
        return pathDetails;
    }

    private List<List<Double>> extractCoordinates(ResponsePath path) {
        List<List<Double>> points = new ArrayList<>();
        PointList pointList = path.getPoints();
        for (int i = 0; i < pointList.size(); i++) {
            points.add(Arrays.asList(pointList.getLon(i), pointList.getLat(i)));
        }
        return points;
    }

    private List<Map<String, Object>> extractInstructions(ResponsePath path) {
        List<Map<String, Object>> instructions = new ArrayList<>();
        Translation tr = translationMap.getWithFallBack(Locale.ENGLISH);
        for (Instruction instruction : path.getInstructions()) {
            Map<String, Object> instr = new HashMap<>();
            instr.put("text", instruction.getTurnDescription(tr));
            instr.put("distance", instruction.getDistance());
            instr.put("time", instruction.getTime());
            instr.put("sign", instruction.getSign());
            instructions.add(instr);
        }
        return instructions;
    }

    private List<Map<String, Object>> extractRoadClassDetails(ResponsePath path) {
        List<Map<String, Object>> details = new ArrayList<>();
        List<PathDetail> roadClassDetails = path.getPathDetails().get("road_class");
        if (roadClassDetails != null) {
            for (PathDetail detail : roadClassDetails) {
                details.add(Map.of(
                        "value", detail.getValue(),
                        "first", detail.getFirst(),
                        "last", detail.getLast()
                ));
            }
        }
        return details;
    }
}
