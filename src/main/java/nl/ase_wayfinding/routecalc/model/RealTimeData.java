package nl.ase_wayfinding.routecalc.model;

import lombok.Data;

@Data
public class RealTimeData {
    private String trafficStatus;   // Current traffic conditions
    private String weatherConditions; // Weather information
    private String eventWarnings;   // Events impacting the route
}
