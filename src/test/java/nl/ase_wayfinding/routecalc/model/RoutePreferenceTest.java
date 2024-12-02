package nl.ase_wayfinding.routecalc.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RoutePreferenceTest {

    @Test
    void testRoutePreference() {
        RoutePreference routePreference = new RoutePreference();
        routePreference.setAvoidHighways(true);
        routePreference.setEcoFriendly(false);

        assertTrue(routePreference.getAvoidHighways());
        assertFalse(routePreference.getEcoFriendly());
    }
}
