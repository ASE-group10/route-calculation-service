package nl.ase_wayfinding.routecalc.service;

import com.graphhopper.*;
import com.graphhopper.config.Profile;
import com.graphhopper.util.JsonFeature;
import com.graphhopper.util.JsonFeatureCollection;
import com.graphhopper.util.CustomModel;
import com.graphhopper.util.PointList;
import com.graphhopper.util.shapes.GHPoint;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.operation.union.CascadedPolygonUnion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static com.graphhopper.json.Statement.If;
import static com.graphhopper.json.Statement.Op.MULTIPLY;

@Service
public class GraphHopperService {

    private static final Logger logger = LoggerFactory.getLogger(GraphHopperService.class);
    private GraphHopper hopper;
    private final GeometryFactory geometryFactory = new GeometryFactory();
    private Map<String, List<double[]>> busRoutes = new HashMap<>();

    @PostConstruct
    public void init() {
        String osmFile = "src/main/resources/data/ireland-and-northern-ireland-latest.osm.pbf";
        String graphFolder = "graph-cache";

        File osmData = new File(osmFile);
        if (!osmData.exists()) {
            throw new IllegalStateException("OSM file not found: " + osmData.getAbsolutePath());
        }

        hopper = new GraphHopper()
                .setOSMFile(osmFile)
                .setGraphHopperLocation(graphFolder)
                .setProfiles(
                        new Profile("car").setVehicle("car").setWeighting("custom"),
                        new Profile("bike").setVehicle("bike").setWeighting("custom"),
                        new Profile("walk").setVehicle("foot").setWeighting("custom")
                        // 🚫 Removed "pt" profile because GraphHopper does NOT support it
                );


        hopper.importOrLoad();
        loadGTFSData();
        logger.info("✅ GraphHopper initialized with OSM data.");
    }

    private void loadGTFSData() {
        try {
            List<String> lines = Files.readAllLines(Paths.get("src/main/resources/gtfs/shapes.txt"));

            // ✅ Skip header row (first line)
            boolean isFirstLine = true;
            for (String line : lines) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue; // ✅ Skip column names
                }

                String[] parts = line.split(",");
                if (parts.length < 4) continue;

                String shapeId = parts[0];
                double lat, lon;

                try {
                    lat = Double.parseDouble(parts[1]);  // shape_pt_lat
                    lon = Double.parseDouble(parts[2]);  // shape_pt_lon
                } catch (NumberFormatException e) {
                    logger.warn("⚠️ Skipping invalid line in GTFS file: {}", line);
                    continue;
                }

                busRoutes.computeIfAbsent(shapeId, k -> new ArrayList<>()).add(new double[]{lon, lat});
            }

            logger.info("🚌 GTFS data loaded successfully! {} bus routes available.", busRoutes.size());

        } catch (Exception e) {
            logger.error("❌ Failed to load GTFS data: {}", e.getMessage());
        }
    }


    public Map<String, Object> getOptimizedRoute(GHRequest request, String mode) { // ✅ Add mode parameter
        request.setProfile(mode);
        int maxIterations = 5;
        int currentIteration = 0;
        GHResponse bestResponse = null;
        int minBadCoords = Integer.MAX_VALUE;

        request.setProfile(mode); // ✅ Ensure the request uses the correct transport mode

        while (currentIteration < maxIterations) {
            currentIteration++;
            logger.info("🔄 Iteration {}: Calculating route for mode {} ...", currentIteration, mode);

            GHResponse response = hopper.route(request);
            if (response == null || response.hasErrors() || response.getAll().isEmpty()) {
                logger.error("❌ Iteration {}: Failed to calculate route for mode {}. Errors: {}",
                        currentIteration, mode, response != null ? response.getErrors() : "No response");
                if (currentIteration == 1) return Map.of("error", "Failed to calculate route.");
                continue;
            }

            List<Coordinate> routeCoords = extractCoordinates(response);
            logger.info("📍 Extracted {} route coordinates in Iteration #{} for mode {}", routeCoords.size(), currentIteration, mode);
            logRouteCoordinates(routeCoords, "Iteration_" + currentIteration);

            List<Coordinate> badCoords = identifyBadCoordinates(routeCoords);
            if (badCoords.isEmpty()) {
                logger.info("✅ Iteration {}: Route is GOOD for mode {} (no bad waypoints).", currentIteration, mode);
                return Map.of("iterations", currentIteration, "bestPath", response.getBest(), "response", response, "bad_areas", new ArrayList<>());
            } else {
                logger.warn("❌ Iteration {}: Found {} bad coords => Creating new 'bad_area_{}' for mode {}",
                        currentIteration, badCoords.size(), currentIteration, mode);
                updateCustomModel(request, badCoords, currentIteration);

                // Store best available response
                if (badCoords.size() < minBadCoords) {
                    bestResponse = response;
                    minBadCoords = badCoords.size();
                }
            }
        }

        if (bestResponse == null || bestResponse.getAll().isEmpty()) {
            return Map.of("error", "No valid route found after multiple attempts for mode " + mode);
        }

        return Map.of("iterations", maxIterations, "bestPath", bestResponse.getBest(), "response", bestResponse, "bad_areas", new ArrayList<>());
    }


    public Map<String, Object> getBusRoute(List<List<Double>> points) {
        double startLat = points.get(0).get(1);
        double startLon = points.get(0).get(0);
        double endLat = points.get(1).get(1);
        double endLon = points.get(1).get(0);

        double minStartDist = Double.MAX_VALUE;
        double minEndDist = Double.MAX_VALUE;
        String bestRoute = null;

        for (Map.Entry<String, List<double[]>> entry : busRoutes.entrySet()) {
            double[] firstStop = entry.getValue().get(0);
            double[] lastStop = entry.getValue().get(entry.getValue().size() - 1);

            double startDist = distance(startLat, startLon, firstStop[1], firstStop[0]);
            double endDist = distance(endLat, endLon, lastStop[1], lastStop[0]);

            if (startDist < minStartDist && endDist < minEndDist) {
                minStartDist = startDist;
                minEndDist = endDist;
                bestRoute = entry.getKey();
            }
        }

        if (bestRoute != null) {
            logger.info("🚌 Best bus route found: {}", bestRoute);
            return Map.of("mode", "bus", "busRoute", bestRoute, "points", busRoutes.get(bestRoute));
        } else {
            return Map.of("error", "No bus route found for this journey.");
        }
    }

    private List<Coordinate> extractCoordinates(GHResponse response) {
        List<Coordinate> coords = new ArrayList<>();
        if (response.getAll().isEmpty()) return coords;

        PointList points = response.getBest().getPoints();
        for (int i = 0; i < points.size(); i += 10) {
            coords.add(new Coordinate(points.getLon(i), points.getLat(i)));
        }
        return coords;
    }

    private List<Coordinate> identifyBadCoordinates(List<Coordinate> coords) {
        List<Coordinate> badCoords = new ArrayList<>();
        for (Coordinate c : coords) {
            if (Math.random() < 0.1) { // Mocking bad environmental conditions
                badCoords.add(c);
            }
        }
        return badCoords;
    }

    private double distance(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371e3; // Earth radius in meters
        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double deltaLat = Math.toRadians(lat2 - lat1);
        double deltaLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                Math.cos(phi1) * Math.cos(phi2) *
                        Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private void logRouteCoordinates(List<Coordinate> coords, String label) {
        if (coords.isEmpty()) {
            logger.warn("⚠️ No coordinates found for {}.", label);
            return;
        }

        StringBuilder logMessage = new StringBuilder("\n📌 **Route Coordinates for " + label + "**:\n[\n");
        for (Coordinate c : coords) {
            logMessage.append(String.format("  [%.6f, %.6f],\n", c.x, c.y));
        }
        logMessage.append("]\n");
        logger.info(logMessage.toString());
    }

    private void updateCustomModel(GHRequest request, List<Coordinate> badCoords, int iteration) {
        CustomModel cm = request.getCustomModel();
        if (cm == null) {
            cm = new CustomModel();
            request.setCustomModel(cm);
        }

        JsonFeatureCollection fc = cm.getAreas();
        if (fc == null) {
            fc = new JsonFeatureCollection();
            cm.setAreas(fc);
        }

        String areaId = "bad_area_" + iteration;
        Geometry badArea = createAvoidancePolygon(badCoords);

        if (badArea != null) {
            JsonFeature feature = new JsonFeature(areaId, "Feature", null, badArea, new HashMap<>());
            fc.getFeatures().add(feature);
            cm.addToPriority(If("in_" + areaId, MULTIPLY, "0.01"));

            logger.info("🔧 Updated priority => if(in_{}) then multiply by 0.01", areaId);
            logPolygonCoordinates(badArea, areaId);
        }
    }

    private void logPolygonCoordinates(Geometry polygon, String areaId) {
        if (polygon instanceof MultiPolygon) {
            logger.info("📌 **MultiPolygon detected for {} ({}) parts.**", areaId, ((MultiPolygon) polygon).getNumGeometries());
            for (int i = 0; i < ((MultiPolygon) polygon).getNumGeometries(); i++) {
                logPolygonCoordinates(((MultiPolygon) polygon).getGeometryN(i), areaId + "_part" + i);
            }
        } else if (polygon instanceof Polygon) {
            StringBuilder logMessage = new StringBuilder("\n📌 **Polygon Coordinates for " + areaId + "**:\n[\n");
            for (Coordinate c : polygon.getCoordinates()) {
                logMessage.append(String.format("  [%.6f, %.6f],\n", c.x, c.y));
            }
            logMessage.append("]\n");
            logger.info(logMessage.toString());
        }
    }

    private Geometry createAvoidancePolygon(List<Coordinate> badCoords) {
        if (badCoords.isEmpty()) return null;

        List<Polygon> polygons = badCoords.stream()
                .map(coord -> generateHexagon(coord, 100))
                .collect(Collectors.toList());

        return CascadedPolygonUnion.union(polygons);
    }

    private Polygon generateHexagon(Coordinate center, double radius) {
        Coordinate[] corners = new Coordinate[7];
        for (int i = 0; i < 6; i++) {
            double angleRad = Math.toRadians(60 * i);
            corners[i] = new Coordinate(
                    center.x + (radius * Math.cos(angleRad) / 111320d),  // Convert meters to degrees longitude
                    center.y + (radius * Math.sin(angleRad) / 111320d)   // Convert meters to degrees latitude
            );
        }
        corners[6] = corners[0]; // Close the hexagon

        return geometryFactory.createPolygon(geometryFactory.createLinearRing(corners));
    }

}
