package nl.ase_wayfinding.routecalc.model;

import lombok.Data;

import java.util.List;

@Data
public class RouteRequest {
    private String userId;
    private List<String> waypoints;
    private boolean avoidTraffic;
    private boolean avoidTolls;
    private boolean avoidPollution;
}
