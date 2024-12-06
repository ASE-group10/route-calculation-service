package nl.ase_wayfinding.routecalc.model;

import lombok.Data;

@Data
public class RealTimeData {
    private String trafficStatus;
    private String weatherConditions;
    private String tollCosts;
    private String co2Impact;

    public String getTrafficStatus() {
        return trafficStatus;
    }

    public void setTrafficStatus(String trafficStatus) {
        this.trafficStatus = trafficStatus;
    }

    public String getWeatherConditions() {
        return weatherConditions;
    }

    public void setWeatherConditions(String weatherConditions) {
        this.weatherConditions = weatherConditions;
    }

    public String getTollCosts() {
        return tollCosts;
    }

    public void setTollCosts(String tollCosts) {
        this.tollCosts = tollCosts;
    }

    public String getCo2Impact() {
        return co2Impact;
    }

    public void setCo2Impact(String co2Impact) {
        this.co2Impact = co2Impact;
    }
}
