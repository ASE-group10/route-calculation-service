package nl.ase_wayfinding.routecalc.model;

import java.util.List;

public class Feature {

    private String type;
    private Properties properties;
    private Geometry geometry;

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

    public Geometry getGeometry() {
        return geometry;
    }

    public void setGeometry(Geometry geometry) {
        this.geometry = geometry;
    }

    // Nested Properties class
    public static class Properties {
        private String road_id;
        private String osm_id;
        private String osm_code;
        private String osm_fclass;
        private String osm_name;
        private String osm_ref;
        private String osm_oneway;
        private String osm_maxspe;
        private String osm_layer;
        private String osm_bridge;
        private String osm_tunnel;
        private Integer NO2points;
        private String NO2drives;
        private Double NO2_ugm3;
        private Integer NOpoints;
        private String NOdrives;
        private Double NO_ugm3;
        private Integer CO2points;
        private String CO2drives;
        private Double CO2_mgm3;
        private Integer COpoints;
        private String COdrives;
        private Double CO_mgm3;
        private Integer O3points;
        private String O3drives;
        private Double O3_ugm3;
        private Integer PM25points;
        private String PM25drives;
        private Double PM25_ugm3;

        // Getters and setters
        public String getRoad_id() {
            return road_id;
        }

        public void setRoad_id(String road_id) {
            this.road_id = road_id;
        }

        public String getOsm_id() {
            return osm_id;
        }

        public void setOsm_id(String osm_id) {
            this.osm_id = osm_id;
        }

        public String getOsm_code() {
            return osm_code;
        }

        public void setOsm_code(String osm_code) {
            this.osm_code = osm_code;
        }

        public String getOsm_fclass() {
            return osm_fclass;
        }

        public void setOsm_fclass(String osm_fclass) {
            this.osm_fclass = osm_fclass;
        }

        public String getOsm_name() {
            return osm_name;
        }

        public void setOsm_name(String osm_name) {
            this.osm_name = osm_name;
        }

        public String getOsm_ref() {
            return osm_ref;
        }

        public void setOsm_ref(String osm_ref) {
            this.osm_ref = osm_ref;
        }

        public String getOsm_oneway() {
            return osm_oneway;
        }

        public void setOsm_oneway(String osm_oneway) {
            this.osm_oneway = osm_oneway;
        }

        public String getOsm_maxspe() {
            return osm_maxspe;
        }

        public void setOsm_maxspe(String osm_maxspe) {
            this.osm_maxspe = osm_maxspe;
        }

        public String getOsm_layer() {
            return osm_layer;
        }

        public void setOsm_layer(String osm_layer) {
            this.osm_layer = osm_layer;
        }

        public String getOsm_bridge() {
            return osm_bridge;
        }

        public void setOsm_bridge(String osm_bridge) {
            this.osm_bridge = osm_bridge;
        }

        public String getOsm_tunnel() {
            return osm_tunnel;
        }

        public void setOsm_tunnel(String osm_tunnel) {
            this.osm_tunnel = osm_tunnel;
        }

        public Integer getNO2points() {
            return NO2points;
        }

        public void setNO2points(Integer NO2points) {
            this.NO2points = NO2points;
        }

        public String getNO2drives() {
            return NO2drives;
        }

        public void setNO2drives(String NO2drives) {
            this.NO2drives = NO2drives;
        }

        public Double getNO2_ugm3() {
            return NO2_ugm3;
        }

        public void setNO2_ugm3(Double NO2_ugm3) {
            this.NO2_ugm3 = NO2_ugm3;
        }

        public Integer getNOpoints() {
            return NOpoints;
        }

        public void setNOpoints(Integer NOpoints) {
            this.NOpoints = NOpoints;
        }

        public String getNOdrives() {
            return NOdrives;
        }

        public void setNOdrives(String NOdrives) {
            this.NOdrives = NOdrives;
        }

        public Double getNO_ugm3() {
            return NO_ugm3;
        }

        public void setNO_ugm3(Double NO_ugm3) {
            this.NO_ugm3 = NO_ugm3;
        }

        public Integer getCO2points() {
            return CO2points;
        }

        public void setCO2points(Integer CO2points) {
            this.CO2points = CO2points;
        }

        public String getCO2drives() {
            return CO2drives;
        }

        public void setCO2drives(String CO2drives) {
            this.CO2drives = CO2drives;
        }

        public Double getCO2_mgm3() {
            return CO2_mgm3;
        }

        public void setCO2_mgm3(Double CO2_mgm3) {
            this.CO2_mgm3 = CO2_mgm3;
        }

        public Integer getCOpoints() {
            return COpoints;
        }

        public void setCOpoints(Integer COpoints) {
            this.COpoints = COpoints;
        }

        public String getCOdrives() {
            return COdrives;
        }

        public void setCOdrives(String COdrives) {
            this.COdrives = COdrives;
        }

        public Double getCO_mgm3() {
            return CO_mgm3;
        }

        public void setCO_mgm3(Double CO_mgm3) {
            this.CO_mgm3 = CO_mgm3;
        }

        public Integer getO3points() {
            return O3points;
        }

        public void setO3points(Integer O3points) {
            this.O3points = O3points;
        }

        public String getO3drives() {
            return O3drives;
        }

        public void setO3drives(String O3drives) {
            this.O3drives = O3drives;
        }

        public Double getO3_ugm3() {
            return O3_ugm3;
        }

        public void setO3_ugm3(Double O3_ugm3) {
            this.O3_ugm3 = O3_ugm3;
        }

        public Integer getPM25points() {
            return PM25points;
        }

        public void setPM25points(Integer PM25points) {
            this.PM25points = PM25points;
        }

        public String getPM25drives() {
            return PM25drives;
        }

        public void setPM25drives(String PM25drives) {
            this.PM25drives = PM25drives;
        }

        public Double getPM25_ugm3() {
            return PM25_ugm3;
        }

        public void setPM25_ugm3(Double PM25_ugm3) {
            this.PM25_ugm3 = PM25_ugm3;
        }
    }

    // Nested Geometry class
    public static class Geometry {
        private String type;
        private List<List<Double>> coordinates;

        // Getter and setter
        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public List<List<Double>> getCoordinates() {
            return coordinates;
        }

        public void setCoordinates(List<List<Double>> coordinates) {
            this.coordinates = coordinates;
        }
    }
}
