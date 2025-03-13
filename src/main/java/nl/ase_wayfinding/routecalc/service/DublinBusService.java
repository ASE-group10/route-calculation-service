package nl.ase_wayfinding.routecalc.service;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class DublinBusService {

    private final RestTemplate restTemplate = new RestTemplate();
    // New GTFS-R API endpoint for trip updates (v2)
    private final String busApiEndpoint = "https://gtfsr.transportforireland.ie/v2/TripUpdates";
    // If the API requires an API key, add it here:
    private final String apiKey = "YOUR_API_KEY_HERE";

    /**
     * Fetch a bus route using the new GTFS-Realtime API.
     * Adjust query parameters and processing as per the new API documentation.
     *
     * @param points List of two coordinate pairs [[lon, lat], [lon, lat]]
     * @return A Map containing bus route information
     */
    public Map<String, Object> getBusRoute(List<List<Double>> points) {
        // Prepare coordinates as "lat,lon" strings
        String start = points.get(0).get(1) + "," + points.get(0).get(0);
        String end = points.get(1).get(1) + "," + points.get(1).get(0);
        long timestamp = Instant.now().getEpochSecond();

        // Build the URL with query parameters. Adjust parameter names as needed.
        String url = busApiEndpoint
                + "?start=" + start
                + "&end=" + end
                + "&timestamp=" + timestamp;

        // Set up headers (including a User-Agent and API key if required)
        HttpHeaders headers = new HttpHeaders();
        headers.add("User-Agent", "Mozilla/5.0 (compatible; YourAppName/1.0)");
        headers.add("Accept", "application/json");
        headers.add("Authorization", "Bearer " + apiKey);  // API Key for authentication

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            return response.getBody();  // Return bus route data
        } catch (Exception e) {
            throw new RuntimeException("Error fetching bus route: " + e.getMessage(), e);
        }
    }
}
