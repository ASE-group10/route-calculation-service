package nl.ase_wayfinding.routecalc.model;

import lombok.Data;

@Data
public class UserPreferences {
    private boolean avoidTraffic;   // Preference to avoid traffic
    private boolean avoidTolls;     // Preference to avoid toll roads
    private boolean avoidPollution; // Preference to avoid polluted areas
}
