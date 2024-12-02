package nl.ase_wayfinding.routecalc.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FeatureCollectionTest {

    @Test
    void testFeatureCollection() {
        FeatureCollection featureCollection = new FeatureCollection();
        FeatureCollection.CRS crs = new FeatureCollection.CRS();
        FeatureCollection.CRS.Properties properties = new FeatureCollection.CRS.Properties();

        properties.setName("EPSG:4326");
        crs.setType("name");
        crs.setProperties(properties);

        featureCollection.setCrs(crs);
        featureCollection.setName("Environmental Data");

        assertEquals("name", featureCollection.getCrs().getType());
        assertEquals("EPSG:4326", featureCollection.getCrs().getProperties().getName());
        assertEquals("Environmental Data", featureCollection.getName());
    }
}
