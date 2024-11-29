package nl.ase_wayfinding.routecalc.service;

import nl.ase_wayfinding.routecalc.model.RealTimeData;
import nl.ase_wayfinding.routecalc.model.UserPreferences;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class ExternalServiceClient {

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

    public UserPreferences fetchUserPreferences(String userId) {
        String url = userProfileServiceUrl + "/preferences?userId=" + userId;
        return restTemplate.getForObject(url, UserPreferences.class);
    }

    public RealTimeData fetchRealTimeData() {
        String url = incidentServiceUrl + "/incidents";
        return restTemplate.getForObject(url, RealTimeData.class);
    }

    public String fetchEnvironmentalData() {
        String url = environmentalDataServiceUrl + "/environment";
        return restTemplate.getForObject(url, String.class);
    }
}
