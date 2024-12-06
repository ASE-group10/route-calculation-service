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

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public List<String> getWaypoints() {
        return waypoints;
    }

    public void setWaypoints(List<String> waypoints) {
        this.waypoints = waypoints;
    }

    public boolean isAvoidTraffic() {
        return avoidTraffic;
    }

    public void setAvoidTraffic(boolean avoidTraffic) {
        this.avoidTraffic = avoidTraffic;
    }

    public boolean isAvoidTolls() {
        return avoidTolls;
    }

    public void setAvoidTolls(boolean avoidTolls) {
        this.avoidTolls = avoidTolls;
    }

    public boolean isAvoidPollution() {
        return avoidPollution;
    }

    public void setAvoidPollution(boolean avoidPollution) {
        this.avoidPollution = avoidPollution;
    }
}
