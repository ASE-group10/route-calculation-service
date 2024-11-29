package nl.ase_wayfinding.routecalc.model;

import lombok.Data;

import java.util.List;

@Data
public class RouteDetails {
    private String routeId;
    private List<String> waypoints;
    private String eta;
    private String costBreakdown;
}
