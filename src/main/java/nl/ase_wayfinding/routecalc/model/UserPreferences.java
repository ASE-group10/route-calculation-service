package nl.ase_wayfinding.routecalc.model;

import lombok.Data;

@Data
public class UserPreferences {
    private boolean avoidTraffic;
    private boolean avoidTolls;
    private boolean avoidPollution;
}
