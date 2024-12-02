package nl.ase_wayfinding.routecalc.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FeatureTest {

    @Test
    void testFeatureProperties() {
        Feature feature = new Feature();
        Feature.Properties properties = new Feature.Properties();
        properties.setCO2_mgm3(40.5);
        properties.setRoad_id("1234");

        feature.setProperties(properties);
        feature.setType("Feature");

        assertEquals("Feature", feature.getType());
        assertEquals(40.5, feature.getProperties().getCO2_mgm3());
        assertEquals("1234", feature.getProperties().getRoad_id());
    }

    @Test
    void testGeometry() {
        Feature.Geometry geometry = new Feature.Geometry();
        geometry.setType("LineString");
        geometry.setCoordinates(List.of(List.of(10.0, 20.0), List.of(30.0, 40.0)));

        assertEquals("LineString", geometry.getType());
        assertEquals(10.0, geometry.getCoordinates().get(0).get(0));
    }
}
