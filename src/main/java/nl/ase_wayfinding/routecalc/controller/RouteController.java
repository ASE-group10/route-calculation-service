package nl.ase_wayfinding.routecalc.controller;

import com.graphhopper.GHResponse;
import com.graphhopper.ResponsePath;
import com.graphhopper.util.PointList;
import nl.ase_wayfinding.routecalc.service.GraphHopperService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "http://localhost:3000") // Allow frontend origin
@RestController
@RequestMapping("/route")
public class RouteController {

    private final GraphHopperService graphHopperService;

    public RouteController(GraphHopperService graphHopperService) {
        this.graphHopperService = graphHopperService;
    }

    @GetMapping
    public ResponseEntity<?> getRoute(@RequestParam double fromLat,
                                      @RequestParam double fromLon,
                                      @RequestParam double toLat,
                                      @RequestParam double toLon) {
        try {
            GHResponse response = graphHopperService.getRoute(fromLat, fromLon, toLat, toLon);
            if (response.hasErrors()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", response.getErrors().toString()));
            }
            Map<String, Object> result = new HashMap<>();
            result.put("distance_km", response.getBest().getDistance() / 1000.0);
            result.put("time_min", response.getBest().getTime() / 60000.0);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            // Log error details here if needed
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/waypoints")
    public ResponseEntity<?> getWaypoints(@RequestParam double fromLat,
                                          @RequestParam double fromLon,
                                          @RequestParam double toLat,
                                          @RequestParam double toLon) {
        try {
            Map<String, Object> result = graphHopperService.getWaypoints(fromLat, fromLon, toLat, toLon);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            // Log error details here if needed
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // New endpoint: returns the basic route details as computed by GraphHopper
    @GetMapping("/basic")
    public ResponseEntity<?> getBasicRoute(@RequestParam double fromLat,
                                           @RequestParam double fromLon,
                                           @RequestParam double toLat,
                                           @RequestParam double toLon) {
        try {
            GHResponse response = graphHopperService.getRoute(fromLat, fromLon, toLat, toLon);
            if (response.hasErrors()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", response.getErrors().toString()));
            }
            ResponsePath bestPath = response.getBest();
            Map<String, Object> route = new HashMap<>();
            route.put("distance_km", bestPath.getDistance() / 1000.0);
            route.put("time_min", bestPath.getTime() / 60000.0);

            // Build a list of points for the route
            PointList points = bestPath.getPoints();
            List<Map<String, Double>> pointList = new ArrayList<>();
            for (int i = 0; i < points.size(); i++) {
                Map<String, Double> point = new HashMap<>();
                point.put("lat", points.getLat(i));
                point.put("lon", points.getLon(i));
                pointList.add(point);
            }
            route.put("points", pointList);

            return ResponseEntity.ok(route);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
