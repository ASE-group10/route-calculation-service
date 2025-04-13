package nl.ase_wayfinding.routecalc.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.graphhopper.GHResponse;
import com.graphhopper.ResponsePath;
import com.graphhopper.util.InstructionList;
import com.graphhopper.util.PointList;
import com.graphhopper.util.details.PathDetail;
import nl.ase_wayfinding.routecalc.config.TestSecurityConfig;
import nl.ase_wayfinding.routecalc.service.GraphHopperService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import com.graphhopper.util.PMap;
import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import nl.ase_wayfinding.routecalc.config.TestConfig;

@WebMvcTest(RouteController.class)
@TestPropertySource(properties = {
                "pyroscope.enabled=false"
})
@Import({ TestConfig.class, TestSecurityConfig.class })
@MockitoSettings(strictness = Strictness.LENIENT)
public class RouteControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private GraphHopperService graphHopperService;

        @Autowired
        private ObjectMapper objectMapper;

        @Mock
        private ResponsePath mockPath;

        @Mock
        private PointList mockPointList;

        @Mock
        private InstructionList mockInstructionList;

        private List<List<Double>> testPoints;
        private Map<String, Object> successfulRouteResult;
        private Map<String, Object> errorRouteResult;
        private Map<String, Object> formattedBusResponse;

        @BeforeEach
        void setUp() {
                // Test coordinates (New York to Boston)
                testPoints = Arrays.asList(
                                Arrays.asList(-74.0060, 40.7128), // New York
                                Arrays.asList(-71.0589, 42.3601) // Boston
                );

                // Setup mock point list behavior
                when(mockPointList.size()).thenReturn(3); // Ensure we have points to extract
                when(mockPointList.getLat(anyInt())).thenReturn(41.0); // Mock latitude
                when(mockPointList.getLon(anyInt())).thenReturn(-72.0); // Mock longitude

                // Setup mock instruction list behavior
                List<com.graphhopper.util.Instruction> instructions = new ArrayList<>();
                com.graphhopper.util.Instruction instruction = mock(com.graphhopper.util.Instruction.class);
                when(instruction.getDistance()).thenReturn(100.0);
                when(instruction.getTime()).thenReturn(1000L);
                when(instruction.getSign()).thenReturn(0);
                when(instruction.getTurnDescription(any())).thenReturn("Continue");
                instructions.add(instruction);
                when(mockInstructionList.iterator()).thenReturn(instructions.iterator());

                // Setup mock path behavior
                when(mockPath.getTime()).thenReturn(10000L);
                when(mockPath.getDistance()).thenReturn(500.0);
                when(mockPath.getRouteWeight()).thenReturn(600.0);
                when(mockPath.getPoints()).thenReturn(mockPointList);
                when(mockPath.getInstructions()).thenReturn(mockInstructionList);
                when(mockPath.getPathDetails())
                                .thenReturn(Collections.singletonMap("road_class", Collections.emptyList()));

                // Create a mock GHResponse
                GHResponse mockResponse = mock(GHResponse.class);
                PMap hintsPMap = new PMap();
                hintsPMap.put("visited_nodes.sum", "500");
                hintsPMap.put("visited_nodes.average", "250");
                when(mockResponse.getHints()).thenReturn(hintsPMap);
                when(mockResponse.hasErrors()).thenReturn(false);
                when(mockResponse.getBest()).thenReturn(mockPath);

                // Mock successful route response
                successfulRouteResult = new HashMap<>();
                successfulRouteResult.put("iterations", 3);
                successfulRouteResult.put("bestPath", mockPath);
                successfulRouteResult.put("response", mockResponse);
                successfulRouteResult.put("bad_areas", Collections.emptyList());

                // Mock error response
                errorRouteResult = new HashMap<>();
                errorRouteResult.put("error", "No valid route found");

                // Mock formatted bus response
                formattedBusResponse = new HashMap<>();
                formattedBusResponse.put("status", "success");
                formattedBusResponse.put("mode", "bus");
                formattedBusResponse.put("paths", Collections.emptyList());
        }

        @Test
        void calculateRoute_SingleMode_Success() throws Exception {
                // Given
                when(graphHopperService.getOptimizedRoute(any(), eq("car"))).thenReturn(successfulRouteResult);

                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("points", testPoints);
                requestBody.put("mode", "car");

                // When & Then
                mockMvc.perform(post("/route")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestBody)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("success"))
                                .andExpect(jsonPath("$.message").value("Optimal route found!"))
                                .andExpect(jsonPath("$.mode").value("car"));

                verify(graphHopperService, times(1)).getOptimizedRoute(any(), eq("car"));
        }

        @Test
        void calculateRoute_BusMode_Success() throws Exception {
                // Given
                when(graphHopperService.getBusRouteWithWalking(any())).thenReturn(formattedBusResponse);

                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("points", testPoints);
                requestBody.put("mode", "bus");

                // When & Then
                mockMvc.perform(post("/route")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestBody)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("success"))
                                .andExpect(jsonPath("$.mode").value("bus"));

                verify(graphHopperService, times(1)).getBusRouteWithWalking(any());
        }

        @Test
        void calculateRoute_ChainedMode_Success() throws Exception {
                // Given
                Map<String, Object> chainedResponse = new HashMap<>();
                chainedResponse.put("status", "success");
                chainedResponse.put("total_distance", 600.0);
                chainedResponse.put("total_time", 15000L);
                chainedResponse.put("segments", Collections.emptyList());

                when(graphHopperService.getChainedRoute(any(), any())).thenReturn(chainedResponse);

                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("points", Arrays.asList(
                                Arrays.asList(-74.0060, 40.7128), // New York
                                Arrays.asList(-73.7949, 40.6438), // JFK Airport
                                Arrays.asList(-71.0589, 42.3601) // Boston
                ));
                requestBody.put("modes", Arrays.asList("car", "bus"));

                // When & Then
                mockMvc.perform(post("/route")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestBody)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("success"));

                verify(graphHopperService, times(1)).getChainedRoute(any(), any());
        }

        @Test
        void calculateRoute_NotEnoughPoints_BadRequest() throws Exception {
                // Given
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("points", Collections.singletonList(Arrays.asList(-74.0060, 40.7128)));
                requestBody.put("mode", "car");

                // When & Then
                mockMvc.perform(post("/route")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestBody)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error").value("At least two points required"));

                verify(graphHopperService, never()).getOptimizedRoute(any(), anyString());
        }

        @Test
        void calculateRoute_InvalidModesCount_BadRequest() throws Exception {
                // Given
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("points", Arrays.asList(
                                Arrays.asList(-74.0060, 40.7128), // New York
                                Arrays.asList(-73.7949, 40.6438), // JFK Airport
                                Arrays.asList(-71.0589, 42.3601) // Boston
                ));
                requestBody.put("modes", Collections.singletonList("car")); // Only one mode for two segments

                // When & Then
                mockMvc.perform(post("/route")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestBody)))
                                .andExpect(status().isBadRequest());

                verify(graphHopperService, never()).getChainedRoute(any(), any());
        }

        @Test
        void calculateRoute_ServiceReturnsError_BadRequest() throws Exception {
                // Given
                when(graphHopperService.getOptimizedRoute(any(), eq("car"))).thenReturn(errorRouteResult);

                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("points", testPoints);
                requestBody.put("mode", "car");

                // When & Then
                mockMvc.perform(post("/route")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestBody)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error").value("No valid route found"));

                verify(graphHopperService, times(1)).getOptimizedRoute(any(), eq("car"));
        }

        @Test
        void calculateRoute_BusServiceThrowsException_InternalServerError() throws Exception {
                // Given
                when(graphHopperService.getBusRouteWithWalking(any()))
                                .thenThrow(new RuntimeException("Service unavailable"));

                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("points", testPoints);
                requestBody.put("mode", "bus");

                // When & Then
                mockMvc.perform(post("/route")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestBody)))
                                .andExpect(status().isInternalServerError())
                                .andExpect(jsonPath("$.error").exists());

                verify(graphHopperService, times(1)).getBusRouteWithWalking(any());
        }

        @Test
        void calculateRoute_ChainedRouteThrowsException_InternalServerError() throws Exception {
                // Given
                when(graphHopperService.getChainedRoute(any(), any()))
                                .thenThrow(new RuntimeException("Failed to calculate chained route"));

                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("points", Arrays.asList(
                                Arrays.asList(-74.0060, 40.7128),
                                Arrays.asList(-73.7949, 40.6438),
                                Arrays.asList(-71.0589, 42.3601)));
                requestBody.put("modes", Arrays.asList("car", "bus"));

                // When & Then
                mockMvc.perform(post("/route")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestBody)))
                                .andExpect(status().isInternalServerError())
                                .andExpect(jsonPath("$.error").exists());

                verify(graphHopperService, times(1)).getChainedRoute(any(), any());
        }

        @Test
        void calculateRoute_MalformedRequest_BadRequest() throws Exception {
                // When & Then
                mockMvc.perform(post("/route")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{invalid-json}"))
                                .andExpect(status().isBadRequest());

                verify(graphHopperService, never()).getOptimizedRoute(any(), anyString());
        }

        @Test
        void calculateRoute_MissingMode_DefaultsToCarMode() throws Exception {
                // Given
                when(graphHopperService.getOptimizedRoute(any(), eq("car"))).thenReturn(successfulRouteResult);

                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("points", testPoints);
                // No mode specified, should default to "car"

                // When & Then
                mockMvc.perform(post("/route")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestBody)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.mode").value("car"));

                verify(graphHopperService, times(1)).getOptimizedRoute(any(), eq("car"));
        }

        @Test
        void calculateRoute_NullResponseFromService_InternalServerError() throws Exception {
                // Given
                Map<String, Object> nullResponse = new HashMap<>();
                nullResponse.put("iterations", 1);
                nullResponse.put("bestPath", null);
                nullResponse.put("response", null);
                when(graphHopperService.getOptimizedRoute(any(), anyString())).thenReturn(nullResponse);

                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("points", testPoints);
                requestBody.put("mode", "car");

                // When & Then
                mockMvc.perform(post("/route")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestBody)))
                                .andExpect(status().isInternalServerError())
                                .andExpect(jsonPath("$.error").value("Failed to generate a valid route"));

                verify(graphHopperService, times(1)).getOptimizedRoute(any(), anyString());
        }

        @Test
        void calculateRoute_DetailedPathProperties_Success() throws Exception {
                // Given
                when(graphHopperService.getOptimizedRoute(any(), eq("car"))).thenReturn(successfulRouteResult);

                // Setup mock path details
                List<PathDetail> roadClassDetails = new ArrayList<>();
                PathDetail detail = mock(PathDetail.class);
                when(detail.getValue()).thenReturn("primary");
                when(detail.getFirst()).thenReturn(0);
                when(detail.getLast()).thenReturn(100);
                roadClassDetails.add(detail);

                Map<String, List<PathDetail>> pathDetails = new HashMap<>();
                pathDetails.put("road_class", roadClassDetails);
                when(mockPath.getPathDetails()).thenReturn(pathDetails);

                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("points", testPoints);
                requestBody.put("mode", "car");

                // When & Then
                mockMvc.perform(post("/route")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestBody)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.paths[0].details.road_class[0].value").value("primary"))
                                .andExpect(jsonPath("$.paths[0].details.road_class[0].first").value(0))
                                .andExpect(jsonPath("$.paths[0].details.road_class[0].last").value(100));

                verify(graphHopperService, times(1)).getOptimizedRoute(any(), eq("car"));
        }

        @Test
        void calculateRoute_OptimizedRouteWithBadAreas_Success() throws Exception {
                // Given
                List<List<Double>> badAreas = Arrays.asList(
                                Arrays.asList(-73.0, 41.0),
                                Arrays.asList(-72.0, 41.5));

                Map<String, Object> routeWithBadAreas = new HashMap<>(successfulRouteResult);
                routeWithBadAreas.put("bad_areas", badAreas);

                when(graphHopperService.getOptimizedRoute(any(), eq("car"))).thenReturn(routeWithBadAreas);

                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("points", testPoints);
                requestBody.put("mode", "car");

                // When & Then
                mockMvc.perform(post("/route")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestBody)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.bad_areas").isArray())
                                .andExpect(jsonPath("$.bad_areas[0][0]").value(-73.0));

                verify(graphHopperService, times(1)).getOptimizedRoute(any(), eq("car"));
        }

        @Test
        void calculateRoute_BikeMode_Success() throws Exception {
                // Given
                when(graphHopperService.getOptimizedRoute(any(), eq("bike"))).thenReturn(successfulRouteResult);

                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("points", testPoints);
                requestBody.put("mode", "bike");

                // When & Then
                mockMvc.perform(post("/route")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestBody)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.mode").value("bike"))
                                .andExpect(jsonPath("$.paths").isArray())
                                .andExpect(jsonPath("$.paths[0].distance").exists());

                verify(graphHopperService, times(1)).getOptimizedRoute(any(), eq("bike"));
        }

        @Test
        void calculateRoute_FootMode_Success() throws Exception {
                // Given
                when(graphHopperService.getOptimizedRoute(any(), eq("foot"))).thenReturn(successfulRouteResult);

                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("points", testPoints);
                requestBody.put("mode", "foot");

                // When & Then
                mockMvc.perform(post("/route")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestBody)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.mode").value("foot"))
                                .andExpect(jsonPath("$.paths[0].time").exists());

                verify(graphHopperService, times(1)).getOptimizedRoute(any(), eq("foot"));
        }

        @Test
        void calculateRoute_MultipleWaypoints_Success() throws Exception {
                // Given
                when(graphHopperService.getOptimizedRoute(any(), eq("car"))).thenReturn(successfulRouteResult);

                // Three waypoints
                List<List<Double>> multiWaypoints = Arrays.asList(
                                Arrays.asList(-74.0060, 40.7128), // New York
                                Arrays.asList(-73.7949, 40.6438), // JFK Airport
                                Arrays.asList(-71.0589, 42.3601) // Boston
                );

                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("points", multiWaypoints);
                requestBody.put("mode", "car");

                // When & Then
                mockMvc.perform(post("/route")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestBody)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("success"));

                verify(graphHopperService, times(1)).getOptimizedRoute(any(), eq("car"));
        }

        @Test
        void calculateRoute_MixedModeChaining_Success() throws Exception {
                // Given
                Map<String, Object> chainedResponse = new HashMap<>();
                chainedResponse.put("status", "success");
                chainedResponse.put("total_distance", 750.0);
                chainedResponse.put("total_time", 20000L);
                chainedResponse.put("segments", Arrays.asList(
                                Map.of("mode", "car", "distance", 300.0, "time", 8000L),
                                Map.of("mode", "foot", "distance", 450.0, "time", 12000L)));

                when(graphHopperService.getChainedRoute(any(), any())).thenReturn(chainedResponse);

                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("points", Arrays.asList(
                                Arrays.asList(-74.0060, 40.7128),
                                Arrays.asList(-73.7949, 40.6438),
                                Arrays.asList(-71.0589, 42.3601)));
                requestBody.put("modes", Arrays.asList("car", "foot"));

                // When & Then
                mockMvc.perform(post("/route")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestBody)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("success"))
                                .andExpect(jsonPath("$.total_distance").value(750.0))
                                .andExpect(jsonPath("$.segments[0].mode").value("car"))
                                .andExpect(jsonPath("$.segments[1].mode").value("foot"));

                verify(graphHopperService, times(1)).getChainedRoute(any(), any());
        }

        @Test
        void calculateRoute_WithHints_Success() throws Exception {
                // Given
                // Create a new mock response with proper hints
                GHResponse mockResponseWithHints = mock(GHResponse.class);
                PMap hintsPMap = new PMap();
                hintsPMap.put("visited_nodes.sum", "500");
                hintsPMap.put("visited_nodes.average", "250");
                when(mockResponseWithHints.getHints()).thenReturn(hintsPMap);
                when(mockResponseWithHints.hasErrors()).thenReturn(false);
                when(mockResponseWithHints.getBest()).thenReturn(mockPath);

                // Create a copy of the successful result with this enhanced mock response
                Map<String, Object> resultWithHints = new HashMap<>(successfulRouteResult);
                resultWithHints.put("response", mockResponseWithHints);

                when(graphHopperService.getOptimizedRoute(any(), eq("car"))).thenReturn(resultWithHints);

                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("points", testPoints);
                requestBody.put("mode", "car");

                // When & Then
                mockMvc.perform(post("/route")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestBody)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.hints").exists())
                                .andExpect(jsonPath("$.hints['visited_nodes.sum']").value(500))
                                .andExpect(jsonPath("$.hints['visited_nodes.average']").value(250));

                verify(graphHopperService, times(1)).getOptimizedRoute(any(), eq("car"));
        }

        @Test
        void calculateRoute_InvalidCoordinates_BadRequest() throws Exception {
                // Given
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("points", Arrays.asList(
                                Arrays.asList(-74.0060), // Invalid coordinate (missing lat)
                                Arrays.asList(-71.0589, 42.3601)));
                requestBody.put("mode", "car");

                // When & Then
                mockMvc.perform(post("/route")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestBody)))
                                .andExpect(status().isBadRequest());

                verify(graphHopperService, never()).getOptimizedRoute(any(), anyString());
        }

        @Test
        void calculateRoute_UnsupportedMode_BadRequest() throws Exception {
                // Given
                when(graphHopperService.getOptimizedRoute(any(), eq("spaceship")))
                                .thenReturn(Map.of("error", "Unsupported transport mode"));

                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("points", testPoints);
                requestBody.put("mode", "spaceship");

                // When & Then
                mockMvc.perform(post("/route")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestBody)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error").exists());

                verify(graphHopperService, times(1)).getOptimizedRoute(any(), eq("spaceship"));
        }

        @Test
        void calculateRoute_EmptyRequestBody_BadRequest() throws Exception {
                // When & Then
                mockMvc.perform(post("/route")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                                .andExpect(status().isBadRequest());

                verify(graphHopperService, never()).getOptimizedRoute(any(), anyString());
        }
}
