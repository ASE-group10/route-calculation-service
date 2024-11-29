package nl.ase_wayfinding.routecalc.model;

import lombok.Data;

@Data
public class RealTimeData {
    private String trafficStatus;
    private String weatherConditions;
    private String tollCosts;
    private String co2Impact;
}
