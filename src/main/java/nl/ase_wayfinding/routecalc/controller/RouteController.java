package nl.ase_wayfinding.routecalc.controller;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.ResponsePath;
import com.graphhopper.util.PointList;
import com.graphhopper.util.Instruction;
import com.graphhopper.util.details.PathDetail;
import nl.ase_wayfinding.routecalc.service.GraphHopperService;
import nl.ase_wayfinding.routecalc.service.DublinBusService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.graphhopper.util.TranslationMap;
import com.graphhopper.util.Translation;

import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/route")
public class RouteController {
    private final TranslationMap translationMap = new TranslationMap().doImport();
    private final GraphHopperService graphHopperService;
    private final DublinBusService dublinBusService;

    public RouteController(GraphHopperService graphHopperService, DublinBusService dublinBusService) {
        this.graphHopperService = graphHopperService;
        this.dublinBusService = dublinBusService;
    }

    @PostMapping
    public ResponseEntity<?> calculateRoute(@RequestBody Map<String, Object> body) {
        List<List<Double>> points = (List<List<Double>>) body.get("points");
        if (points.size() < 2) {
            return ResponseEntity.badRequest().body(Map.of("error", "At least two points required"));
        }

        // Get mode dynamically; default to "car"
        String mode = (String) body.getOrDefault("mode", "car");

        if (mode.equalsIgnoreCase("bus")) {
            Map<String, Object> busRoute = graphHopperService.getBusRoute(points);
            return ResponseEntity.ok(busRoute);
        }

        GHRequest request = new GHRequest(
                points.get(0).get(1), points.get(0).get(0),
                points.get(1).get(1), points.get(1).get(0)
        ).setProfile(mode);

        Map<String, Object> routeResult = graphHopperService.getOptimizedRoute(request, mode);

        if (routeResult.containsKey("error")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(routeResult);
        }

        int actualIterations = (int) routeResult.get("iterations");
        ResponsePath bestPath = (ResponsePath) routeResult.get("bestPath");
        GHResponse response = (GHResponse) routeResult.get("response");

        if (response == null || bestPath == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to generate a valid route"));
        }

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

        formattedResponse.put("paths", List.of(pathDetails));
        formattedResponse.put("bad_areas", routeResult.get("bad_areas") instanceof List && ((List<?>) routeResult.get("bad_areas")).isEmpty()
                ? null
                : routeResult.get("bad_areas"));

        return ResponseEntity.ok(formattedResponse);
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
            PointList points = instruction.getPoints();
            if (!points.isEmpty()) {
                double lon = points.getLon(0);
                double lat = points.getLat(0);
                instr.put("location", Arrays.asList(lon, lat));
                if (points.size() > 1) {
                    double lon2 = points.getLon(1);
                    double lat2 = points.getLat(1);
                    instr.put("bearing", calculateBearing(lat, lon, lat2, lon2));
                }
            }
            instructions.add(instr);
        }
        return instructions;
    }

    private double calculateBearing(double lat1, double lon1, double lat2, double lon2) {
        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double deltaLambda = Math.toRadians(lon2 - lon1);
        double y = Math.sin(deltaLambda) * Math.cos(phi2);
        double x = Math.cos(phi1) * Math.sin(phi2) - Math.sin(phi1) * Math.cos(phi2) * Math.cos(deltaLambda);
        return (Math.toDegrees(Math.atan2(y, x)) + 360) % 360;
    }

    private List<Map<String, Object>> extractRoadClassDetails(ResponsePath path) {
        List<Map<String, Object>> details = new ArrayList<>();
        List<PathDetail> roadClassDetails = path.getPathDetails().get("road_class");
        if (roadClassDetails != null) {
            for (PathDetail detail : roadClassDetails) {
                Map<String, Object> roadDetail = new HashMap<>();
                roadDetail.put("value", detail.getValue());
                roadDetail.put("first", detail.getFirst());
                roadDetail.put("last", detail.getLast());
                details.add(roadDetail);
            }
        }
        return details;
    }
}
