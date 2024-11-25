package nl.ase_wayfinding.routecalc.controller;

import nl.ase_wayfinding.routecalc.model.*;
import nl.ase_wayfinding.routecalc.service.RouteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/route")
public class RouteController {

    private final RouteService routeService;

    public RouteController(RouteService routeService) {
        this.routeService = routeService;
    }

    @PostMapping("/calculate")
    public ResponseEntity<RouteDetails> calculateRoute(@RequestBody UserPreferences preferences) {
        RouteDetails route = routeService.calculateRoute(preferences);
        return ResponseEntity.ok(route);
    }

    @GetMapping("/alternative")
    public ResponseEntity<RouteDetails> getAlternativeRoute() {
        RouteDetails alternativeRoute = routeService.getAlternativeRoute();
        return ResponseEntity.ok(alternativeRoute);
    }

    @PostMapping("/multiTransport")
    public ResponseEntity<RouteDetails> calculateMultiTransportRoute(@RequestBody UserPreferences preferences) {
        RouteDetails multiTransportRoute = routeService.calculateMultiTransportRoute(preferences);
        return ResponseEntity.ok(multiTransportRoute);
    }

    @GetMapping("/validateStop")
    public ResponseEntity<?> validateStop(@RequestParam String stopName) {
        boolean isValid = routeService.validateStop(stopName);
        if (isValid) {
            return ResponseEntity.ok("Stop is valid and fits the route.");
        } else {
            ErrorResponse error = new ErrorResponse();
            error.setErrorMessage("Stop does not align with the calculated route.");
            error.setErrorCode(400);
            return ResponseEntity.badRequest().body(error);
        }
    }
}
