package nl.ase_wayfinding.routecalc.controller;

import nl.ase_wayfinding.routecalc.model.RouteDetails;
import nl.ase_wayfinding.routecalc.model.RouteRequest;
import nl.ase_wayfinding.routecalc.service.RouteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/route")
public class RouteController {

    private final RouteService routeService;
    private final RestTemplate restTemplate;


    public RouteController(RouteService routeService, RestTemplate restTemplate) {
        this.routeService = routeService;
        this.restTemplate = restTemplate;
    }

    @PostMapping("/calculate")
    public ResponseEntity<RouteDetails> calculateRoute(@RequestBody RouteRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body cannot be null");
        }
        RouteDetails route = routeService.calculateRoute(request);
        return ResponseEntity.ok(route);
    }

    @GetMapping("/alternative")
    public ResponseEntity<RouteDetails> getAlternativeRoutes(@RequestBody RouteRequest request) {
        RouteDetails route = routeService.getAlternativeRoute(request);
        return ResponseEntity.ok(route);
    }

    @PostMapping("/multiTransport")
    public ResponseEntity<RouteDetails> calculateMultiTransportRoute(@RequestBody RouteRequest request) {
        RouteDetails route = routeService.calculateMultiTransportRoute(request);
        return ResponseEntity.ok(route);
    }

    @GetMapping("/validateStop")
    public ResponseEntity<Boolean> validateStop(@RequestParam String routeId,
                                                @RequestParam double stopLat,
                                                @RequestParam double stopLng) {
        boolean isValid = routeService.validateStop(routeId, stopLat, stopLng);
        return ResponseEntity.ok(isValid);
    }
    @GetMapping("/fetch-reward-data")
    public String fetchRewardData() {
        String rewardServiceUrl = "http://localhost:8080/hello-reward";
        String response = restTemplate.getForObject(rewardServiceUrl, String.class);
        return "Route Calculation Service received data: " + response;
    }
    @GetMapping("/retrieve-rewards")
    public String retrieveRewards() {
        String rewardServiceUrl = "http://localhost:8080/rewards";
        String response = restTemplate.getForObject(rewardServiceUrl, String.class);
        return "Route Calculation Service received rewards: " + response;
    }
}
