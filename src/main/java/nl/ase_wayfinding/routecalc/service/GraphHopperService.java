package nl.ase_wayfinding.routecalc.service;

import com.graphhopper.*;
import com.graphhopper.config.Profile;
import com.graphhopper.util.JsonFeature;
import com.graphhopper.util.JsonFeatureCollection;
import com.graphhopper.util.CustomModel;
import com.graphhopper.util.PointList;
import com.graphhopper.util.shapes.GHPoint;
import org.springframework.beans.factory.annotation.Value;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.operation.union.CascadedPolygonUnion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.graphhopper.util.Instruction;
import com.graphhopper.util.InstructionList;
import com.graphhopper.util.Translation;
import com.graphhopper.util.TranslationMap;
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
    private TranslationMap translationMap;

    @Value("${osm.file.path}")
    private String osmFilePath;

    @Value("${gtfs.path}")
    private String gtfsPath;

    @Value("${graph.cache.path}")
    private String graphCachePath;

    @PostConstruct
    public void init() {
        File osmData = new File(osmFilePath);
        if (!osmData.exists()) {
            throw new IllegalStateException("❌ OSM file not found: " + osmData.getAbsolutePath());
        }

        hopper = new GraphHopper()
                .setOSMFile(osmFilePath)
                .setGraphHopperLocation(graphCachePath)
                .setProfiles(
                        new Profile("car").setVehicle("car").setWeighting("custom"),
                        new Profile("bike").setVehicle("bike").setWeighting("custom"),
                        new Profile("walk").setVehicle("foot").setWeighting("custom")
                );

        hopper.importOrLoad();

        String shapesFile = Paths.get(gtfsPath, "shapes.txt").toString();
        loadGTFSData(shapesFile);

        logger.info("✅ GraphHopper initialized with OSM file: {}", osmFilePath);
        logger.info("✅ GTFS data loaded from: {}", gtfsPath);
        logger.info("✅ Graph cache path: {}", graphCachePath);
    }

    private void loadGTFSData(String shapesFile) {
        try {
            List<String> lines = Files.readAllLines(Paths.get(shapesFile));
            boolean isFirstLine = true;
            for (String line : lines) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;  // Skip header row
                }
                String[] parts = line.split(",");
                if (parts.length < 4) continue;

                String shapeId = parts[0];
                double lat, lon;
                try {
                    lat = Double.parseDouble(parts[1]);
                    lon = Double.parseDouble(parts[2]);
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

    public Map<String, Object> getOptimizedRoute(GHRequest request, String mode) {
        request.setProfile(mode);
        int maxIterations = 5;
        int currentIteration = 0;
        GHResponse bestResponse = null;
        int minBadCoords = Integer.MAX_VALUE;
        List<List<Double>> lastBadAreas = new ArrayList<>();

        request.setProfile(mode);

        while (currentIteration < maxIterations) {
            currentIteration++;
            logger.info("🔄 Iteration {}: Calculating route for mode {} ...", currentIteration, mode);

            GHResponse response = hopper.route(request);
            if (response == null || response.hasErrors() || response.getAll().isEmpty()) {
                logger.error("❌ Iteration {}: Failed to calculate route for mode {}. Errors: {}",
                        currentIteration, mode, response != null ? response.getErrors() : "No response");
                if (currentIteration == 1)
                    return Map.of("error", "Failed to calculate route.");
                continue;
            }

            List<List<Double>> routeCoords = extractCoordinates(response.getBest());
            logger.info("📍 Extracted {} route coordinates in Iteration #{} for mode {}",
                    routeCoords.size(), currentIteration, mode);
            logRouteCoordinates(routeCoords, "Iteration_" + currentIteration);

            List<List<Double>> badCoords = identifyBadCoordinates(routeCoords);
            if (badCoords.isEmpty()) {
                logger.info("✅ Iteration {}: Route is GOOD for mode {} (no bad waypoints).", currentIteration, mode);
                return Map.of(
                        "iterations", currentIteration,
                        "bestPath", response.getBest(),
                        "response", response,
                        "bad_areas", lastBadAreas.isEmpty() ? List.of() : lastBadAreas
                );
            } else {
                logger.warn("❌ Iteration {}: Found {} bad coords => Creating new 'bad_area_{}' for mode {}",
                        currentIteration, badCoords.size(), currentIteration, mode);
                lastBadAreas.addAll(badCoords);
                updateCustomModel(request, badCoords, currentIteration);

                if (badCoords.size() < minBadCoords) {
                    bestResponse = response;
                    minBadCoords = badCoords.size();
                }
            }
        }

        if (bestResponse == null || bestResponse.getAll().isEmpty()) {
            return Map.of("error", "No valid route found after multiple attempts for mode " + mode);
        }

        return Map.of(
                "iterations", maxIterations,
                "bestPath", bestResponse.getBest(),
                "response", bestResponse,
                "bad_areas", lastBadAreas.isEmpty() ? null : lastBadAreas
        );
    }

    public Map<String, Object> getBusRouteWithWalking(List<List<Double>> userPoints) {
        GHPoint start = new GHPoint(userPoints.get(0).get(1), userPoints.get(0).get(0));
        GHPoint end = new GHPoint(userPoints.get(1).get(1), userPoints.get(1).get(0));

        Map<String, Object> busRouteData = getBusRoute(userPoints);
        if (busRouteData.containsKey("error")) {
            return busRouteData;
        }

        List<double[]> busPoints = (List<double[]>) busRouteData.get("points");
        GHPoint startBusStop = closestPoint(start, busPoints);
        GHPoint endBusStop = closestPoint(end, busPoints);

        GHRequest toBusStopRequest = new GHRequest(start, startBusStop).setProfile("walk");
        GHRequest fromBusStopRequest = new GHRequest(endBusStop, end).setProfile("walk");

        GHResponse walkToBusStopResp = hopper.route(toBusStopRequest);
        GHResponse walkFromBusStopResp = hopper.route(fromBusStopRequest);

        List<List<Double>> formattedBusPoints = convertBusPoints(busPoints);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "success");
        response.put("mode", "bus");

        Map<String, Object> walkToBusStop = formatPathSegment(walkToBusStopResp.getBest(), "walk", "Origin", "Bus Stop");
        Map<String, Object> busSegment = formatBusSegment(
                busRouteData.get("busRoute").toString(), startBusStop, endBusStop, formattedBusPoints);
        Map<String, Object> walkFromBusStop = formatPathSegment(walkFromBusStopResp.getBest(), "walk", "Bus Stop", "Destination");

        response.put("paths", List.of(walkToBusStop, busSegment, walkFromBusStop));
        return response;
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

    private GHPoint closestPoint(GHPoint userLocation, List<double[]> busRoutePoints) {
        GHPoint closest = null;
        double minDist = Double.MAX_VALUE;

        for (double[] coord : busRoutePoints) {
            GHPoint busStop = new GHPoint(coord[1], coord[0]);
            double dist = distance(userLocation.getLat(), userLocation.getLon(), busStop.getLat(), busStop.getLon());
            if (dist < minDist) {
                minDist = dist;
                closest = busStop;
            }
        }
        return closest;
    }

    private Map<String, Object> formatPathSegment(ResponsePath path, String mode, String startName, String endName) {
        if (path == null) {
            return Map.of("error", "Failed to calculate path segment");
        }
        return Map.of(
                "mode", mode,
                "distance", path.getDistance(),
                "time", path.getTime(),
                "points_encoded", false,
                "points", extractCoordinates(path),
                "instructions", extractInstructions(path),
                "start", startName,
                "end", endName
        );
    }

    private List<Map<String, Object>> extractInstructions(ResponsePath path) {
        List<Map<String, Object>> instructions = new ArrayList<>();
        Translation tr = translationMap.getWithFallBack(Locale.ENGLISH);
        InstructionList instructionList = path.getInstructions();

        for (Instruction instruction : instructionList) {
            instructions.add(formatInstruction(instruction, tr));
        }
        return instructions;
    }

    private Map<String, Object> formatInstruction(Instruction instruction, Translation tr) {
        Map<String, Object> instr = new HashMap<>();
        instr.put("text", instruction.getTurnDescription(tr));
        instr.put("distance", instruction.getDistance());
        instr.put("time", instruction.getTime());
        instr.put("sign", instruction.getSign());

        PointList points = instruction.getPoints();
        if (!points.isEmpty()) {
            double lon = points.getLon(0);
            double lat = points.getLat(0);
            instr.put("location", Arrays.asList(lon, lat));
        }
        return instr;
    }

    private Map<String, Object> formatBusSegment(String busRoute, GHPoint start, GHPoint end, List<List<Double>> busPoints) {
        return Map.of(
                "mode", "bus",
                "busRoute", busRoute,
                "start", Arrays.asList(start.getLon(), start.getLat()),
                "end", Arrays.asList(end.getLon(), end.getLat()),
                "points_encoded", false,
                "points", busPoints,
                "instructions", List.of(
                        Map.of(
                                "text", "Board bus route " + busRoute,
                                "distance", 0,
                                "time", 0,
                                "sign", 0,
                                "location", start
                        ),
                        Map.of(
                                "text", "Disembark bus at final stop",
                                "distance", 0,
                                "time", 0,
                                "sign", 4,
                                "location", end
                        )
                )
        );
    }

    private List<List<Double>> convertBusPoints(List<double[]> busPoints) {
        List<List<Double>> coordinates = new ArrayList<>();
        for (double[] point : busPoints) {
            coordinates.add(Arrays.asList(point[0], point[1]));
        }
        return coordinates;
    }

    private List<List<Double>> extractCoordinates(ResponsePath path) {
        List<List<Double>> coords = new ArrayList<>();
        if (path == null || path.getPoints().isEmpty()) return coords;
        PointList points = path.getPoints();
        for (int i = 0; i < points.size(); i++) {
            coords.add(Arrays.asList(points.getLon(i), points.getLat(i)));
        }
        return coords;
    }

    private List<List<Double>> identifyBadCoordinates(List<List<Double>> coords) {
        List<List<Double>> badCoords = new ArrayList<>();
        for (List<Double> c : coords) {
            if (Math.random() < 0.01) {
                badCoords.add(c);
            }
        }
        return badCoords;
    }

    private double distance(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371e3;
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

    private void logRouteCoordinates(List<List<Double>> coords, String label) {
        if (coords.isEmpty()) {
            logger.warn("⚠️ No coordinates found for {}.", label);
            return;
        }
        StringBuilder logMessage = new StringBuilder("\n📌 **Route Coordinates for " + label + "**:\n[\n");
        for (List<Double> c : coords) {
            logMessage.append(String.format("  [%.6f, %.6f],\n", c.get(0), c.get(1)));
        }
        logMessage.append("]\n");
        logger.info(logMessage.toString());
    }

    private void updateCustomModel(GHRequest request, List<List<Double>> badCoords, int iteration) {
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
            List<List<Double>> coordinates = new ArrayList<>();
            for (Coordinate c : polygon.getCoordinates()) {
                coordinates.add(Arrays.asList(c.x, c.y));
            }
            logger.info("📌 **Polygon Coordinates for {}**: {}", areaId, coordinates);
        }
    }

    private Geometry createAvoidancePolygon(List<List<Double>> badCoords) {
        if (badCoords.isEmpty()) return null;
        List<Polygon> polygons = badCoords.stream()
                .map(coord -> generateHexagon(coord, 100))
                .collect(Collectors.toList());
        return CascadedPolygonUnion.union(polygons);
    }

    private Polygon generateHexagon(List<Double> center, double radius) {
        Coordinate[] corners = new Coordinate[7];
        double lon = center.get(0);
        double lat = center.get(1);
        for (int i = 0; i < 6; i++) {
            double angleRad = Math.toRadians(60 * i);
            corners[i] = new Coordinate(
                    lon + (radius * Math.cos(angleRad) / 111320d),
                    lat + (radius * Math.sin(angleRad) / 111320d)
            );
        }
        corners[6] = corners[0];
        return geometryFactory.createPolygon(geometryFactory.createLinearRing(corners));
    }
}
