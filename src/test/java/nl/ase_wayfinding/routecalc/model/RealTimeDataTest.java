package nl.ase_wayfinding.routecalc.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RealTimeDataTest {

    @Test
    void testRealTimeData() {
        RealTimeData realTimeData = new RealTimeData();
        realTimeData.setTrafficStatus("Heavy Traffic");
        realTimeData.setWeatherConditions("Rainy");

        assertEquals("Heavy Traffic", realTimeData.getTrafficStatus());
        assertEquals("Rainy", realTimeData.getWeatherConditions());
    }
}
