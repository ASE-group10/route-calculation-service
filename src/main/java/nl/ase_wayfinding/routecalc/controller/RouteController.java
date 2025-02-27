package nl.ase_wayfinding.routecalc.controller;

import com.graphhopper.GHResponse;
import nl.ase_wayfinding.routecalc.service.GraphHopperService;
import org.springframework.web.bind.annotation.*;

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
    public String getRoute(@RequestParam double fromLat, @RequestParam double fromLon,
                           @RequestParam double toLat, @RequestParam double toLon) {
        GHResponse response = graphHopperService.getRoute(fromLat, fromLon, toLat, toLon);
        if (response.hasErrors()) {
            return "Error: " + response.getErrors();
        }
        return "Distance: " + response.getBest().getDistance() / 1000.0 + " km, "
                + "Time: " + response.getBest().getTime() / 60000.0 + " minutes.";
    }

    @CrossOrigin(origins = "http://localhost:3000") // Allow frontend origin
    @GetMapping("/waypoints")
    public Map<String, Object> getWaypoints(@RequestParam double fromLat, @RequestParam double fromLon,
                                            @RequestParam double toLat, @RequestParam double toLon) {
        return graphHopperService.getWaypoints(fromLat, fromLon, toLat, toLon);
    }
}
