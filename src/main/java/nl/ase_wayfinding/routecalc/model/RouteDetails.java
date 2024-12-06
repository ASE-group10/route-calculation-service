package nl.ase_wayfinding.routecalc.model;

import lombok.Data;

import java.util.List;

@Data
public class RouteDetails {

    private String routeId;
    private List<String> waypoints;
    private String eta;
    private String costBreakdown;

    public String getRouteId() {
        return routeId;
    }

    public List<String> getWaypoints() {
        return waypoints;
    }

    public String getEta() {
        return eta;
    }

    public String getCostBreakdown() {
        return costBreakdown;
    }

    public void setRouteId(String routeId) {
        this.routeId = routeId;
    }

    public void setWaypoints(List<String> waypoints) {
        this.waypoints = waypoints;
    }

    public void setEta(String eta) {
        this.eta = eta;
    }

    public void setCostBreakdown(String costBreakdown) {
        this.costBreakdown = costBreakdown;
    }
}
