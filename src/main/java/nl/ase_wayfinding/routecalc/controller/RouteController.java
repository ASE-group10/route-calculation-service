package nl.ase_wayfinding.routecalc.controller;

import nl.ase_wayfinding.routecalc.model.RouteDetails;
import nl.ase_wayfinding.routecalc.model.RouteRequest;
import nl.ase_wayfinding.routecalc.service.RouteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/route")
public class RouteController {

    private final RouteService routeService;

    public RouteController(RouteService routeService) {
        this.routeService = routeService;
    }

    @PostMapping("/calculate")
    public ResponseEntity<RouteDetails> calculateRoute(@RequestBody RouteRequest request) {
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
}
