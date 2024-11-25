package nl.ase_wayfinding.routecalc.model;

import lombok.Data;

@Data
public class RouteDetails {
    private String routeId;         // Unique identifier for the route
    private String[] waypoints;     // Array of waypoints for the route
    private String eta;             // Estimated Time of Arrival
    private String costBreakdown;   // Cost details (e.g., tolls, fuel costs)
}
