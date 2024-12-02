package nl.ase_wayfinding.routecalc.model;

import java.util.List;

public class FeatureCollection {

    private String type;
    private String name;
    private CRS crs;  // Coordinate Reference System (CRS)
    private List<Feature> features;  // List of features in this collection

    // Getters and setters

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public CRS getCrs() {
        return crs;
    }

    public void setCrs(CRS crs) {
        this.crs = crs;
    }

    public List<Feature> getFeatures() {
        return features;
    }

    public void setFeatures(List<Feature> features) {
        this.features = features;
    }

    // Nested CRS class
    public static class CRS {
        private String type;
        private Properties properties;

        // Getters and setters
        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Properties getProperties() {
            return properties;
        }

        public void setProperties(Properties properties) {
            this.properties = properties;
        }

        // Nested Properties class
        public static class Properties {
            private String name;

            // Getter and setter
            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }
        }
    }
}
