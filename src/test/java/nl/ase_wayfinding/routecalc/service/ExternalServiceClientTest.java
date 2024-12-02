package nl.ase_wayfinding.routecalc.service;

import nl.ase_wayfinding.routecalc.model.FeatureCollection;
import nl.ase_wayfinding.routecalc.model.RealTimeData;
import nl.ase_wayfinding.routecalc.model.RoutePreference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ExternalServiceClientTest {

    @Mock
    private RestTemplate restTemplate;

    private ExternalServiceClient externalServiceClient;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        externalServiceClient = new ExternalServiceClient(restTemplate);

        setPrivateField(externalServiceClient, "userProfileServiceUrl", "http://user-service");
        setPrivateField(externalServiceClient, "incidentServiceUrl", "http://incident-service");
        setPrivateField(externalServiceClient, "environmentalDataServiceUrl", "http://environmental-service");
    }

    @Test
    void testFetchUserPreferences() {
        String userId = "testUser";
        RoutePreference mockPreference = new RoutePreference();
        mockPreference.setEcoFriendly(true);

        when(restTemplate.getForObject("http://user-service/preferences?userId=testUser", RoutePreference.class))
                .thenReturn(mockPreference);

        RoutePreference result = externalServiceClient.fetchUserPreferences(userId);

        assertNotNull(result);
        assertTrue(result.getEcoFriendly());
        verify(restTemplate, times(1))
                .getForObject("http://user-service/preferences?userId=testUser", RoutePreference.class);
    }

    @Test
    void testFetchRealTimeData() {
        RealTimeData mockData = new RealTimeData();
        mockData.setTrafficStatus("Heavy Traffic");

        when(restTemplate.getForObject("http://incident-service/api/routes/near-incident", RealTimeData.class))
                .thenReturn(mockData);

        RealTimeData result = externalServiceClient.fetchRealTimeData();

        assertNotNull(result);
        assertEquals("Heavy Traffic", result.getTrafficStatus());
        verify(restTemplate, times(1))
                .getForObject("http://incident-service/api/routes/near-incident", RealTimeData.class);
    }

    @Test
    void testFetchEnvironmentalData() {
        FeatureCollection mockFeatureCollection = new FeatureCollection();
        mockFeatureCollection.setName("Environmental Data");

        when(restTemplate.getForObject("http://environmental-service/environmental-data/geojson-data", FeatureCollection.class))
                .thenReturn(mockFeatureCollection);

        FeatureCollection result = externalServiceClient.fetchEnvironmentalData();

        assertNotNull(result);
        assertEquals("Environmental Data", result.getName());
        verify(restTemplate, times(1))
                .getForObject("http://environmental-service/environmental-data/geojson-data", FeatureCollection.class);
    }

    private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
