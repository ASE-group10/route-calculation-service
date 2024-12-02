package nl.ase_wayfinding.routecalc.service;

import nl.ase_wayfinding.routecalc.model.FeatureCollection;
import nl.ase_wayfinding.routecalc.model.RealTimeData;
import nl.ase_wayfinding.routecalc.model.RoutePreference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ExternalServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(ExternalServiceClient.class);

    private final RestTemplate restTemplate;

    @Value("${user.profile.service.url}")
    private String userProfileServiceUrl;

    @Value("${incident.service.url}")
    private String incidentServiceUrl;

    @Value("${environmental.data.service.url}")
    private String environmentalDataServiceUrl;

    public ExternalServiceClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public RoutePreference fetchUserPreferences(String userId) {
        String url = userProfileServiceUrl + "/preferences?userId=" + userId;
        logger.info("calling fetchUserPreferences url {}", url);
        return restTemplate.getForObject(url, RoutePreference.class);
    }

    public RealTimeData fetchRealTimeData() {
        String url = incidentServiceUrl + "/api/routes/near-incident";
        logger.info("calling fetchRealTimeData url {}", url);
        return restTemplate.getForObject(url, RealTimeData.class);
    }

    public FeatureCollection fetchEnvironmentalData() {
        String url = environmentalDataServiceUrl + "/environmental-data/geojson-data";
        logger.info("calling fetchEnvironmentalData url {}", url);
        return restTemplate.getForObject(url, FeatureCollection.class);
    }
}
