package nl.ase_wayfinding.routecalc.service;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.ResponsePath;
import com.graphhopper.config.Profile;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.util.CustomModel;
import com.graphhopper.util.Parameters;
import com.graphhopper.util.PointList;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.graphhopper.json.Statement.If;
import static com.graphhopper.json.Statement.Op.LIMIT;
import static com.graphhopper.json.Statement.Op.MULTIPLY;

@Service
public class GraphHopperService {

    private GraphHopper hopper;

    @PostConstruct
    public void init() {
        // Paths for your OSM file and graph cache
        String osmFile = "src/main/resources/data/ireland-and-northern-ireland-latest.osm.pbf";
        String graphFolder = "graph-cache";

        // Verify that the OSM file exists
        File osmData = new File(osmFile);
        if (!osmData.exists()) {
            throw new IllegalStateException("OSM file not found: " + osmData.getAbsolutePath());
        }
        CustomModel customModel = new CustomModel()
                .addToPriority(If("road_access == DESTINATION", MULTIPLY, "0.1"))
                .addToSpeed(If("true", LIMIT, "max_speed")); // Fixed line

        hopper = new GraphHopper()
                .setOSMFile(osmFile)
                .setGraphHopperLocation(graphFolder)
                .setProfiles(new Profile("car")
                        .setVehicle("car")
                        .setWeighting("custom")
                        .setCustomModel(customModel));

        hopper.importOrLoad();
    }

    public GHResponse getRoute(double fromLat, double fromLon, double toLat, double toLon) {
        GHRequest request = new GHRequest(fromLat, fromLon, toLat, toLon)
                .setProfile("car");

        // Then route
        return hopper.route(request);
    }
    // New method to return all waypoints between the start and end points
    public Map<String, Object> getWaypoints(double fromLat, double fromLon, double toLat, double toLon) {
        GHRequest request = new GHRequest(fromLat, fromLon, toLat, toLon)
                .setProfile("car");

        GHResponse response = hopper.route(request);

        if (response.hasErrors()) {
            throw new RuntimeException("Error: " + response.getErrors());
        }

        ResponsePath path = response.getBest();
        PointList points = path.getPoints();

        // Convert waypoints to a list
        List<Map<String, Double>> waypoints = new ArrayList<>();
        for (int i = 0; i < points.size(); i++) {
            Map<String, Double> waypoint = new HashMap<>();
            waypoint.put("lat", points.getLat(i));
            waypoint.put("lon", points.getLon(i));
            waypoints.add(waypoint);
        }

        // Return waypoints along with distance & time
        Map<String, Object> result = new HashMap<>();
        result.put("distance_km", path.getDistance() / 1000.0);
        result.put("time_min", path.getTime() / 60000.0);
        result.put("waypoints", waypoints);

        return result;
    }
}
