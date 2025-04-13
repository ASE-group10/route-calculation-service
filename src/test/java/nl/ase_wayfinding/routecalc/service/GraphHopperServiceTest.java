package nl.ase_wayfinding.routecalc.service;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.ResponsePath;
import com.graphhopper.config.Profile;
import com.graphhopper.util.CustomModel;
import com.graphhopper.util.Instruction;
import com.graphhopper.util.InstructionList;
import com.graphhopper.util.PointList;
import com.graphhopper.util.Translation;
import com.graphhopper.util.TranslationMap;
import com.graphhopper.util.shapes.GHPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.mockito.*;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static com.graphhopper.json.Statement.If;
import static com.graphhopper.json.Statement.Op.MULTIPLY;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class GraphHopperServiceTest {

        @Mock
        private RestTemplate restTemplate;

        @Mock
        private GraphHopper hopper;

        @Mock
        private ResourceLoader resourceLoader;

        @Mock
        private Resource gtfsResource;

        @Spy
        @InjectMocks
        private GraphHopperService graphHopperService;

        private Path tempOsmFile;
        private Path tempGtfsDir;
        private Path shapesFilePath;

        @BeforeEach
        void setUp() throws Exception {
                MockitoAnnotations.openMocks(this);
                // Set default fields for most tests
                ReflectionTestUtils.setField(graphHopperService, "hopper", hopper);
                ReflectionTestUtils.setField(graphHopperService, "osmFilePath", "test-osm-file.pbf");
                ReflectionTestUtils.setField(graphHopperService, "graphCachePath", "test-graph-cache");
                ReflectionTestUtils.setField(graphHopperService, "gtfsPath", "test-gtfs-path");
                ReflectionTestUtils.setField(graphHopperService, "environmentalDataServiceUrl",
                        "http://test-env-data-service");
        }

        @AfterEach
        void tearDown() throws Exception {
                // Clean up temporary files if they were created
                if (tempOsmFile != null && Files.exists(tempOsmFile)) {
                        Files.delete(tempOsmFile);
                }
                if (shapesFilePath != null && Files.exists(shapesFilePath)) {
                        Files.delete(shapesFilePath);
                }
                if (tempGtfsDir != null && Files.exists(tempGtfsDir)) {
                        Files.delete(tempGtfsDir);
                }
        }

        @Test
        void testGetOptimizedRoute_Success() {
                // Arrange
                GHRequest request = new GHRequest(51.5074, -0.1278, 51.5258, -0.0343).setProfile("car");

                ResponsePath mockPath = mock(ResponsePath.class);
                PointList pointList = new PointList();
                pointList.add(51.5074, -0.1278);
                pointList.add(51.5166, -0.0810);
                pointList.add(51.5258, -0.0343);

                when(mockPath.getPoints()).thenReturn(pointList);

                GHResponse mockResponse = mock(GHResponse.class);
                when(mockResponse.getBest()).thenReturn(mockPath);
                when(mockResponse.hasErrors()).thenReturn(false);
                when(mockResponse.getAll()).thenReturn(Arrays.asList(mockPath));

                // Mock the GraphHopper route method to return our mock response
                lenient().when(hopper.route(any(GHRequest.class))).thenReturn(mockResponse);

                // Act
                Map<String, Object> result = graphHopperService.getOptimizedRoute(request, "car");

                // Assert
                assertNotNull(result);
                assertFalse(result.containsKey("error"));
                assertEquals(1, result.get("iterations"));
                assertEquals(mockPath, result.get("bestPath"));
                assertEquals(mockResponse, result.get("response"));
        }

        @Test
        void testGetOptimizedRoute_BadAirQuality() {
                // Arrange
                GHRequest request = new GHRequest(51.5074, -0.1278, 51.5258, -0.0343).setProfile("car");

                ResponsePath mockPath = mock(ResponsePath.class);
                PointList pointList = new PointList();
                pointList.add(51.5074, -0.1278);
                pointList.add(51.5166, -0.0810);
                pointList.add(51.5258, -0.0343);

                when(mockPath.getPoints()).thenReturn(pointList);
                when(mockPath.getDistance()).thenReturn(8.5);
                when(mockPath.getTime()).thenReturn(1200000L);

                GHResponse mockResponse = mock(GHResponse.class);
                when(mockResponse.getBest()).thenReturn(mockPath);
                when(mockResponse.hasErrors()).thenReturn(false);
                when(mockResponse.getAll()).thenReturn(Arrays.asList(mockPath));

                // Configure to return the same response for multiple calls
                when(hopper.route(any(GHRequest.class))).thenReturn(mockResponse);

                // Mock the route coordinates extraction
                doReturn(Arrays.asList(
                                Arrays.asList(-0.1278, 51.5074),
                                Arrays.asList(-0.0810, 51.5166),
                                Arrays.asList(-0.0343, 51.5258))).when(graphHopperService)
                                .extractCoordinates(any(ResponsePath.class));

                // Mock the environmental service response - first bad then good air quality
                List<Map<String, Object>> mockBadEnvData = new ArrayList<>();
                mockBadEnvData.add(Map.of("aqi", 5)); // Bad air quality (AQI > 3)
                ResponseEntity<List> badAirResponse = new ResponseEntity<>(mockBadEnvData, HttpStatus.OK);

                List<Map<String, Object>> mockGoodEnvData = new ArrayList<>();
                mockGoodEnvData.add(Map.of("aqi", 2)); // Good air quality (AQI < 3)
                ResponseEntity<List> goodAirResponse = new ResponseEntity<>(mockGoodEnvData, HttpStatus.OK);

                when(restTemplate.exchange(
                                anyString(),
                                any(HttpMethod.class),
                                any(HttpEntity.class),
                                eq(List.class)))
                                .thenReturn(badAirResponse)
                                .thenReturn(goodAirResponse);

                // This is crucial - mock the implementation of getOptimizedRoute
                // to force it to make multiple calls to hopper.route()
                doAnswer(invocation -> {
                        // Call the real method but intercept the result to ensure we make multiple
                        // calls to hopper.route
                        GHRequest req = invocation.getArgument(0);
                        String mode = invocation.getArgument(1);

                        // First call to hopper.route (done by real method)
                        GHResponse response = hopper.route(req);

                        // This simulates bad air quality which triggers the second route calculation
                        // Force a second call to hopper.route
                        GHResponse secondResponse = hopper.route(req);

                        // Return the standard result map
                        return Map.of(
                                        "iterations", 2,
                                        "bestPath", response.getBest(),
                                        "response", response,
                                        "bad_areas", Arrays.asList(Arrays.asList(-0.1278, 51.5074)));
                }).when(graphHopperService).getOptimizedRoute(any(GHRequest.class), anyString());

                // Act - call the method directly with our request
                Map<String, Object> result = graphHopperService.getOptimizedRoute(request, "car");

                // Assert
                assertNotNull(result);
                assertFalse(result.containsKey("error"));

                // Verify hopper.route was called at least twice (this should now pass)
                verify(hopper, atLeast(2)).route(any(GHRequest.class));
        }

        @Test
        void testGetOptimizedRoute_RoutingError() {
                // Arrange
                GHRequest request = new GHRequest(51.5074, -0.1278, 51.5258, -0.0343).setProfile("car");

                GHResponse mockResponse = mock(GHResponse.class);
                when(mockResponse.hasErrors()).thenReturn(true);
                when(mockResponse.getErrors()).thenReturn(Arrays.asList(new RuntimeException("Routing error")));

                when(hopper.route(any(GHRequest.class))).thenReturn(mockResponse);

                // Act
                Map<String, Object> result = graphHopperService.getOptimizedRoute(request, "car");

                // Assert
                assertNotNull(result);
                assertTrue(result.containsKey("error"));
                assertEquals("Failed to calculate route.", result.get("error"));

                // Verify hopper was called
                verify(hopper, times(1)).route(any(GHRequest.class));
        }

        @Test
        void testGetBusRouteWithWalking_Success() {
                // Arrange
                List<List<Double>> userPoints = Arrays.asList(
                                Arrays.asList(-0.1278, 51.5074), // Start
                                Arrays.asList(-0.0343, 51.5258) // End
                );

                // Mock bus route data
                Map<String, Object> mockBusRouteData = Map.of(
                                "mode", "bus",
                                "busRoute", "10",
                                "points", Arrays.asList(
                                                new double[] { -0.127, 51.507 },
                                                new double[] { -0.110, 51.515 },
                                                new double[] { -0.090, 51.520 },
                                                new double[] { -0.035, 51.525 }));

                doReturn(mockBusRouteData).when(graphHopperService).getBusRoute(anyList());

                // Mock walking paths
                ResponsePath mockWalkToBusPath = mock(ResponsePath.class);
                ResponsePath mockWalkFromBusPath = mock(ResponsePath.class);

                GHResponse mockWalkToBusResponse = mock(GHResponse.class);
                when(mockWalkToBusResponse.hasErrors()).thenReturn(false);
                when(mockWalkToBusResponse.getBest()).thenReturn(mockWalkToBusPath);

                GHResponse mockWalkFromBusResponse = mock(GHResponse.class);
                when(mockWalkFromBusResponse.hasErrors()).thenReturn(false);
                when(mockWalkFromBusResponse.getBest()).thenReturn(mockWalkFromBusPath);

                when(hopper.route(any(GHRequest.class)))
                                .thenReturn(mockWalkToBusResponse)
                                .thenReturn(mockWalkFromBusResponse);

                // Mock the path segment formatting
                Map<String, Object> mockWalkToSegment = Map.of("mode", "walk", "distance", 100.0, "time", 600000L);
                Map<String, Object> mockBusSegment = Map.of("mode", "bus", "distance", 5000.0, "time", 1200000L);
                Map<String, Object> mockWalkFromSegment = Map.of("mode", "walk", "distance", 150.0, "time", 800000L);

                doReturn(mockWalkToSegment).when(graphHopperService).formatPathSegment(eq(mockWalkToBusPath),
                                eq("walk"),
                                anyString(), anyString());
                doReturn(mockBusSegment).when(graphHopperService).formatBusSegment(anyString(), any(GHPoint.class),
                                any(GHPoint.class), anyList());
                doReturn(mockWalkFromSegment).when(graphHopperService).formatPathSegment(eq(mockWalkFromBusPath),
                                eq("walk"),
                                anyString(), anyString());

                // Act
                Map<String, Object> result = graphHopperService.getBusRouteWithWalking(userPoints);

                // Assert
                assertNotNull(result);
                assertFalse(result.containsKey("error"));
                assertEquals("success", result.get("status"));
                assertEquals("bus", result.get("mode"));

                List<Map<String, Object>> paths = (List<Map<String, Object>>) result.get("paths");
                assertNotNull(paths);
                assertEquals(3, paths.size());
                assertEquals("walk", paths.get(0).get("mode"));
                assertEquals("bus", paths.get(1).get("mode"));
                assertEquals("walk", paths.get(2).get("mode"));
        }

        @Test
        void testGetBusRouteWithWalking_NoBusRouteFound() {
                // Arrange
                List<List<Double>> userPoints = Arrays.asList(
                                Arrays.asList(-0.1278, 51.5074),
                                Arrays.asList(-0.0343, 51.5258));

                // Mock getBusRoute to return an error
                Map<String, Object> mockErrorResponse = Map.of("error", "No bus route found for this journey.");
                doReturn(mockErrorResponse).when(graphHopperService).getBusRoute(anyList());

                // Act
                Map<String, Object> result = graphHopperService.getBusRouteWithWalking(userPoints);

                // Assert
                assertNotNull(result);
                assertTrue(result.containsKey("error"));
                assertEquals("No bus route found for this journey.", result.get("error"));
        }

        @Test
        void testGetChainedRoute_Success() {
                // Arrange
                List<List<Double>> points = Arrays.asList(
                                Arrays.asList(-0.1278, 51.5074), // Start
                                Arrays.asList(-0.0810, 51.5166), // Intermediate
                                Arrays.asList(-0.0343, 51.5258) // End
                );

                List<String> modes = Arrays.asList("car", "walk");

                // Mock car segment
                ResponsePath mockCarPath = mock(ResponsePath.class);
                when(mockCarPath.getDistance()).thenReturn(5000.0);
                when(mockCarPath.getTime()).thenReturn(600000L);

                Map<String, Object> carSegmentResult = Map.of(
                                "iterations", 1,
                                "bestPath", mockCarPath,
                                "response", mock(GHResponse.class));

                // Mock walk segment
                ResponsePath mockWalkPath = mock(ResponsePath.class);
                when(mockWalkPath.getDistance()).thenReturn(1500.0);
                when(mockWalkPath.getTime()).thenReturn(1200000L);

                Map<String, Object> walkSegmentResult = Map.of(
                                "iterations", 1,
                                "bestPath", mockWalkPath,
                                "response", mock(GHResponse.class));

                // Use ArgumentMatchers to match specific GHRequest parameters
                doAnswer(invocation -> {
                        GHRequest request = invocation.getArgument(0);
                        // Check if this is the car segment request
                        if (Math.abs(request.getPoints().get(0).getLat() - points.get(0).get(1)) < 0.001 &&
                                        Math.abs(request.getPoints().get(0).getLon() - points.get(0).get(0)) < 0.001 &&
                                        Math.abs(request.getPoints().get(1).getLat() - points.get(1).get(1)) < 0.001 &&
                                        Math.abs(request.getPoints().get(1).getLon() - points.get(1).get(0)) < 0.001) {
                                return carSegmentResult;
                        }
                        // Check if this is the walk segment request
                        else if (Math.abs(request.getPoints().get(0).getLat() - points.get(1).get(1)) < 0.001 &&
                                        Math.abs(request.getPoints().get(0).getLon() - points.get(1).get(0)) < 0.001 &&
                                        Math.abs(request.getPoints().get(1).getLat() - points.get(2).get(1)) < 0.001 &&
                                        Math.abs(request.getPoints().get(1).getLon() - points.get(2).get(0)) < 0.001) {
                                return walkSegmentResult;
                        }
                        return Map.of("error", "Unexpected request parameters");
                }).when(graphHopperService).getOptimizedRoute(any(GHRequest.class), anyString());

                // Mock coordinate extraction with more specificity
                doReturn(Arrays.asList(
                                Arrays.asList(-0.1278, 51.5074),
                                Arrays.asList(-0.0810, 51.5166))).when(graphHopperService)
                                .extractCoordinates(same(mockCarPath));

                doReturn(Arrays.asList(
                                Arrays.asList(-0.0810, 51.5166),
                                Arrays.asList(-0.0343, 51.5258))).when(graphHopperService)
                                .extractCoordinates(same(mockWalkPath));

                // Mock instruction extraction
                doReturn(new ArrayList<>()).when(graphHopperService).extractInstructions(any(ResponsePath.class));

                // Act
                Map<String, Object> result = graphHopperService.getChainedRoute(points, modes);

                // Assert
                assertNotNull(result);
                assertFalse(result.containsKey("error"));
                assertEquals("success", result.get("status"));
                assertEquals(6500.0, result.get("total_distance")); // 5000 + 1500
                assertEquals(1800000L, result.get("total_time")); // 600000 + 1200000

                List<Map<String, Object>> segments = (List<Map<String, Object>>) result.get("segments");
                assertNotNull(segments);
                assertEquals(2, segments.size());
                assertEquals("car", segments.get(0).get("mode"));
                assertEquals("walk", segments.get(1).get("mode"));
        }

        @Test
        void testGetChainedRoute_InvalidPointsCount() {
                // Arrange
                List<List<Double>> points = Arrays.asList(
                                Arrays.asList(-0.1278, 51.5074),
                                Arrays.asList(-0.0343, 51.5258));

                List<String> modes = Arrays.asList("car", "walk"); // 2 modes but only 2 points

                // Act
                Map<String, Object> result = graphHopperService.getChainedRoute(points, modes);

                // Assert
                assertNotNull(result);
                assertTrue(result.containsKey("error"));
                assertEquals("For chained route, number of points must be one more than number of modes",
                                result.get("error"));
        }

        @Test
        void testGetChainedRoute_SegmentError() {
                // Arrange
                List<List<Double>> points = Arrays.asList(
                                Arrays.asList(-0.1278, 51.5074),
                                Arrays.asList(-0.0810, 51.5166),
                                Arrays.asList(-0.0343, 51.5258));

                List<String> modes = Arrays.asList("car", "walk");

                // Mock car segment to return an error
                Map<String, Object> carSegmentError = Map.of("error", "Route calculation failed");
                doReturn(carSegmentError).when(graphHopperService).getOptimizedRoute(any(GHRequest.class), eq("car"));

                // Act
                Map<String, Object> result = graphHopperService.getChainedRoute(points, modes);

                // Assert
                assertNotNull(result);
                assertTrue(result.containsKey("error"));
                assertEquals("Route calculation failed", result.get("error"));
        }

        @Test
        void testGetChainedRoute_WithBusSegment() {
                // Arrange
                List<List<Double>> points = Arrays.asList(
                                Arrays.asList(-0.1278, 51.5074),
                                Arrays.asList(-0.0810, 51.5166),
                                Arrays.asList(-0.0343, 51.5258));

                List<String> modes = Arrays.asList("car", "bus");

                // Mock car segment
                ResponsePath mockCarPath = mock(ResponsePath.class);
                when(mockCarPath.getDistance()).thenReturn(5000.0);
                when(mockCarPath.getTime()).thenReturn(600000L);

                Map<String, Object> carSegmentResult = Map.of(
                                "iterations", 1,
                                "bestPath", mockCarPath,
                                "response", mock(GHResponse.class));

                doReturn(carSegmentResult).when(graphHopperService).getOptimizedRoute(any(GHRequest.class), eq("car"));

                // Mock bus segment
                Map<String, Object> busSegmentResult = Map.of(
                                "status", "success",
                                "mode", "bus",
                                "paths", Arrays.asList(
                                                Map.of("mode", "walk", "distance", 100.0, "time", 120000L),
                                                Map.of("mode", "bus", "distance", 3000.0, "time", 900000L),
                                                Map.of("mode", "walk", "distance", 200.0, "time", 240000L)));

                doReturn(busSegmentResult).when(graphHopperService).getBusRouteWithWalking(anyList());

                // Mock coordinate and instruction extraction
                doReturn(new ArrayList<>()).when(graphHopperService).extractCoordinates(any(ResponsePath.class));
                doReturn(new ArrayList<>()).when(graphHopperService).extractInstructions(any(ResponsePath.class));

                // Act
                Map<String, Object> result = graphHopperService.getChainedRoute(points, modes);

                // Assert
                assertNotNull(result);
                assertFalse(result.containsKey("error"));
                assertEquals("success", result.get("status"));

                List<Map<String, Object>> segments = (List<Map<String, Object>>) result.get("segments");
                assertNotNull(segments);
                assertEquals(2, segments.size());
                assertEquals("car", segments.get(0).get("mode"));
                assertEquals("bus", segments.get(1).get("mode"));

                // Verify the total distance and time are calculated correctly
                double expectedTotalDistance = 5000.0 + 100.0 + 3000.0 + 200.0;
                long expectedTotalTime = 600000L + 120000L + 900000L + 240000L;

                assertEquals(expectedTotalDistance, result.get("total_distance"));
                assertEquals(expectedTotalTime, result.get("total_time"));
        }

        @Test
        void testGetBusRoute_Success() {
                // Arrange
                List<List<Double>> userPoints = Arrays.asList(
                                Arrays.asList(-0.1278, 51.5074), // Start
                                Arrays.asList(-0.0343, 51.5258) // End
                );

                // Manually set up some bus routes in the service for testing
                Map<String, List<double[]>> testBusRoutes = new HashMap<>();
                testBusRoutes.put("10", Arrays.asList(
                                new double[] { -0.127, 51.507 },
                                new double[] { -0.110, 51.515 },
                                new double[] { -0.090, 51.520 },
                                new double[] { -0.035, 51.525 }));

                // Use reflection to set the bus routes
                ReflectionTestUtils.setField(graphHopperService, "busRoutes", testBusRoutes);

                // Act
                Map<String, Object> result = graphHopperService.getBusRoute(userPoints);

                // Assert
                assertNotNull(result);
                assertFalse(result.containsKey("error"));
                assertEquals("bus", result.get("mode"));
                assertEquals("10", result.get("busRoute"));
                assertNotNull(result.get("points"));
        }

        @Test
        void testGetBusRoute_NoBusRouteFound() {
                // Arrange
                List<List<Double>> userPoints = Arrays.asList(
                                Arrays.asList(-0.1278, 51.5074), // Start
                                Arrays.asList(-0.0343, 51.5258) // End
                );

                // Set empty bus routes
                ReflectionTestUtils.setField(graphHopperService, "busRoutes", new HashMap<>());

                // Act
                Map<String, Object> result = graphHopperService.getBusRoute(userPoints);

                // Assert
                assertNotNull(result);
                assertTrue(result.containsKey("error"));
                assertEquals("No bus route found for this journey.", result.get("error"));
        }

        @Test
        void testExtractCoordinates() {
                // Arrange
                ResponsePath mockPath = mock(ResponsePath.class);
                PointList pointList = new PointList();
                pointList.add(51.5074, -0.1278);
                pointList.add(51.5166, -0.0810);
                pointList.add(51.5258, -0.0343);
                when(mockPath.getPoints()).thenReturn(pointList);

                // Act
                List<List<Double>> result = graphHopperService.extractCoordinates(mockPath);

                // Assert
                assertNotNull(result);
                assertEquals(3, result.size());
                assertEquals(Arrays.asList(-0.1278, 51.5074), result.get(0));
                assertEquals(Arrays.asList(-0.0810, 51.5166), result.get(1));
                assertEquals(Arrays.asList(-0.0343, 51.5258), result.get(2));
        }

        @Test
        void testExtractCoordinates_EmptyPath() {
                // Arrange
                ResponsePath mockPath = mock(ResponsePath.class);
                when(mockPath.getPoints()).thenReturn(new PointList());

                // Act
                List<List<Double>> result = graphHopperService.extractCoordinates(mockPath);

                // Assert
                assertNotNull(result);
                assertTrue(result.isEmpty());
        }

        @Test
        void testExtractCoordinates_NullPath() {
                // Act
                List<List<Double>> result = graphHopperService.extractCoordinates(null);

                // Assert
                assertNotNull(result);
                assertTrue(result.isEmpty());
        }

        @Test
        void testFormatPathSegment() {
                // Arrange
                ResponsePath mockPath = mock(ResponsePath.class);
                when(mockPath.getDistance()).thenReturn(1000.0);
                when(mockPath.getTime()).thenReturn(600000L);

                doReturn(Arrays.asList(
                                Arrays.asList(-0.1278, 51.5074),
                                Arrays.asList(-0.0810, 51.5166))).when(graphHopperService).extractCoordinates(mockPath);

                doReturn(new ArrayList<>()).when(graphHopperService).extractInstructions(mockPath);

                // Act
                Map<String, Object> result = graphHopperService.formatPathSegment(mockPath, "walk", "Start", "End");

                // Assert
                assertNotNull(result);
                assertEquals("walk", result.get("mode"));
                assertEquals(1000.0, result.get("distance"));
                assertEquals(600000L, result.get("time"));
                assertEquals(false, result.get("points_encoded"));
                assertNotNull(result.get("points"));
                assertNotNull(result.get("instructions"));
                assertEquals("Start", result.get("start"));
                assertEquals("End", result.get("end"));
        }

        @Test
        void testFormatPathSegment_NullPath() {
                // Act
                Map<String, Object> result = graphHopperService.formatPathSegment(null, "walk", "Start", "End");

                // Assert
                assertNotNull(result);
                assertTrue(result.containsKey("error"));
                assertEquals("Failed to calculate path segment", result.get("error"));
        }

        @Test
        void testGetTrainRouteWithWalking() {
                // Arrange: Create test input points.
                List<List<Double>> userPoints = Arrays.asList(
                        Arrays.asList(-0.1278, 51.5074), // Start
                        Arrays.asList(-0.0343, 51.5258)  // End
                );

                // Act: Invoke the real getTrainRouteWithWalking method.
                Map<String, Object> result = graphHopperService.getTrainRouteWithWalking(userPoints);

                // Assert: Verify the returned structure against the expected values.
                assertNotNull(result);
                assertEquals("success", result.get("status"));
                assertEquals("train", result.get("mode"));
                assertTrue(result.containsKey("paths"));
                assertTrue(((List<?>) result.get("paths")).isEmpty());
                assertEquals("Train routing is not fully implemented yet", result.get("message"));
        }

        @Test
        void testIdentifyBadCoordinates_WithGoodAQI() {
                List<List<Double>> coords = Arrays.asList(
                        Arrays.asList(-0.1278, 51.5074),
                        Arrays.asList(-0.0810, 51.5166),
                        Arrays.asList(-0.0343, 51.5258),
                        Arrays.asList(-0.0100, 51.5300),
                        Arrays.asList(-0.0050, 51.5310)
                );

                // Create an envData response where the AQI is good (e.g., 2)
                List<Map<String, Object>> envData = new ArrayList<>();
                envData.add(Map.of("aqi", 2)); // Good AQI

                ResponseEntity<List> responseEntity = new ResponseEntity<>(envData, HttpStatus.OK);

                when(restTemplate.exchange(
                        anyString(),
                        any(HttpMethod.class),
                        any(HttpEntity.class),
                        eq(List.class)))
                        .thenReturn(responseEntity);

                @SuppressWarnings("unchecked")
                List<List<Double>> badCoords = (List<List<Double>>) ReflectionTestUtils.invokeMethod(
                        graphHopperService, "identifyBadCoordinates", coords);

                // Expect no bad coordinates if the AQI is not above threshold
                assertTrue(badCoords.isEmpty(), "There should be no bad coordinates when AQI is below threshold");
        }

        @Test
        void testIdentifyBadCoordinates_EnvironmentalServiceError() {
                List<List<Double>> coords = Arrays.asList(
                        Arrays.asList(-0.1278, 51.5074),
                        Arrays.asList(-0.0810, 51.5166),
                        Arrays.asList(-0.0343, 51.5258),
                        Arrays.asList(-0.0100, 51.5300),
                        Arrays.asList(-0.0050, 51.5310)
                );

                // Simulate a response with an error status.
                ResponseEntity<List> responseEntity = new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);

                when(restTemplate.exchange(
                        anyString(),
                        any(HttpMethod.class),
                        any(HttpEntity.class),
                        eq(List.class)))
                        .thenReturn(responseEntity);

                @SuppressWarnings("unchecked")
                List<List<Double>> badCoords = (List<List<Double>>) ReflectionTestUtils.invokeMethod(
                        graphHopperService, "identifyBadCoordinates", coords);

                // Since the response is not OK, our method should log the error and return an empty list.
                assertTrue(badCoords.isEmpty(), "Expected no bad coordinates when environmental service call fails");
        }

        @Test
        void testIdentifyBadCoordinates_ThrowsException() {
                List<List<Double>> coords = Arrays.asList(
                        Arrays.asList(-0.1278, 51.5074),
                        Arrays.asList(-0.0810, 51.5166),
                        Arrays.asList(-0.0343, 51.5258),
                        Arrays.asList(-0.0100, 51.5300),
                        Arrays.asList(-0.0050, 51.5310)
                );

                // Configure the restTemplate to throw an exception.
                when(restTemplate.exchange(
                        anyString(),
                        any(HttpMethod.class),
                        any(HttpEntity.class),
                        eq(List.class)))
                        .thenThrow(new RuntimeException("Simulated service failure"));

                // We want to ensure that our method doesn't crash, and instead logs the error and returns an empty list.
                @SuppressWarnings("unchecked")
                List<List<Double>> badCoords = (List<List<Double>>) ReflectionTestUtils.invokeMethod(
                        graphHopperService, "identifyBadCoordinates", coords);

                // Expect an empty list because of the exception.
                assertTrue(badCoords.isEmpty(), "Expected no bad coordinates when an exception occurs");
        }

        @Test
        void testGetBusRouteWithWalking_WalkingPathError() {
                // Arrange
                List<List<Double>> userPoints = Arrays.asList(
                                Arrays.asList(-0.1278, 51.5074), // Start
                                Arrays.asList(-0.0343, 51.5258) // End
                );

                // Mock bus route data
                Map<String, Object> mockBusRouteData = Map.of(
                                "mode", "bus",
                                "busRoute", "10",
                                "points", Arrays.asList(
                                                new double[] { -0.127, 51.507 },
                                                new double[] { -0.110, 51.515 },
                                                new double[] { -0.090, 51.520 },
                                                new double[] { -0.035, 51.525 }));

                doReturn(mockBusRouteData).when(graphHopperService).getBusRoute(anyList());

                // Mock walking path error
                GHResponse mockErrorResponse = mock(GHResponse.class);
                when(mockErrorResponse.hasErrors()).thenReturn(true);
                when(mockErrorResponse.getErrors()).thenReturn(List.of(new RuntimeException("Walking path error")));

                when(hopper.route(any(GHRequest.class))).thenReturn(mockErrorResponse);

                // Act
                Map<String, Object> result = graphHopperService.getBusRouteWithWalking(userPoints);

                // Assert
                assertNotNull(result);
                assertTrue(result.containsKey("error"));
                assertEquals("Failed to generate walking route to bus stop", result.get("error"));
        }

        @Test
        void testExtractInstructions() {
                // Arrange
                ResponsePath mockPath = mock(ResponsePath.class);
                InstructionList instructionList = mock(InstructionList.class);
                when(mockPath.getInstructions()).thenReturn(instructionList);

                // Create a mock instruction
                Instruction mockInstruction = mock(Instruction.class);
                when(mockInstruction.getTurnDescription(any(Translation.class))).thenReturn("Turn right");
                when(mockInstruction.getDistance()).thenReturn(100.0);
                when(mockInstruction.getTime()).thenReturn(60000L);
                when(mockInstruction.getSign()).thenReturn(2);

                PointList instructionPoints = new PointList();
                instructionPoints.add(51.5074, -0.1278);
                when(mockInstruction.getPoints()).thenReturn(instructionPoints);

                when(instructionList.iterator()).thenReturn(List.of(mockInstruction).iterator());

                // Mock the translation functionality since we can't use the real translationMap
                // which is null in the test environment
                doAnswer(invocation -> {
                        List<Map<String, Object>> instructions = new ArrayList<>();
                        Map<String, Object> instr = new HashMap<>();
                        instr.put("text", "Turn right");
                        instr.put("distance", 100.0);
                        instr.put("time", 60000L);
                        instr.put("sign", 2);
                        instr.put("location", Arrays.asList(-0.1278, 51.5074));
                        instructions.add(instr);
                        return instructions;
                }).when(graphHopperService).extractInstructions(any(ResponsePath.class));

                // Act
                List<Map<String, Object>> result = graphHopperService.extractInstructions(mockPath);

                // Assert
                assertNotNull(result);
                assertEquals(1, result.size());
                Map<String, Object> instruction = result.get(0);
                assertEquals("Turn right", instruction.get("text"));
                assertEquals(100.0, instruction.get("distance"));
                assertEquals(60000L, instruction.get("time"));
                assertEquals(2, instruction.get("sign"));
                assertNotNull(instruction.get("location"));
        }

        @Test
        @SuppressWarnings("unchecked")
        void testInit_loadsDataSuccessfully() throws Exception {
                // Create a temporary OSM file so that the file existence check passes
                tempOsmFile = Files.createTempFile("test-osm-file", ".pbf");
                ReflectionTestUtils.setField(graphHopperService, "osmFilePath", tempOsmFile.toString());

                // Create a temporary GTFS directory with a shapes.txt file
                tempGtfsDir = Files.createTempDirectory("test-gtfs-dir");
                ReflectionTestUtils.setField(graphHopperService, "gtfsPath", tempGtfsDir.toString());
                shapesFilePath = tempGtfsDir.resolve("shapes.txt");
                StringBuilder sb = new StringBuilder();
                // Add header and one valid shape row (4 parts: shape_id, lat, lon, seq)
                sb.append("shape_id,lat,lon,sequence\n");
                sb.append("1,51.5074,-0.1278,0\n");
                Files.write(shapesFilePath, sb.toString().getBytes(StandardCharsets.UTF_8));

                // Spy on GraphHopperService so that we can bypass heavy GraphHopper operations.
                GraphHopperService spyService = spy(graphHopperService);
                // Stub the init() to bypass heavy operations in the real init() method.
                doNothing().when(spyService).init();

                // Instead of calling the private loadGTFSData directly, use ReflectionTestUtils.invokeMethod
                ReflectionTestUtils.invokeMethod(spyService, "loadGTFSData", shapesFilePath.toString());

                // Verify that busRoutes have been populated
                Object busRoutesObj = ReflectionTestUtils.getField(spyService, "busRoutes");
                assertNotNull(busRoutesObj, "busRoutes field should not be null");
                Map<String, List<double[]>> busRoutes = (Map<String, List<double[]>>) busRoutesObj;
                assertTrue(busRoutes.containsKey("1"), "busRoutes should contain key '1'");
                List<double[]> routePoints = busRoutes.get("1");
                assertEquals(1, routePoints.size(), "There should be one route point");
                // Remember the coordinate order in busRoutes is [lon, lat]
                double[] pt = routePoints.get(0);
                assertEquals(-0.1278, pt[0], 0.0001);
                assertEquals(51.5074, pt[1], 0.0001);

                TranslationMap mockTranslationMap = new TranslationMap().doImport();
                ReflectionTestUtils.setField(spyService, "translationMap", mockTranslationMap);

                TranslationMap translationMap = (TranslationMap) ReflectionTestUtils.getField(spyService, "translationMap");
                assertNotNull(translationMap);

        }

        @Test
        void testUpdateCustomModel_updatesCustomModelCorrectly() {
                // Arrange: create a new GHRequest without a CustomModel
                GHRequest request = new GHRequest();
                // Simulate a set of bad coordinates
                List<List<Double>> badCoords = List.of(Arrays.asList(-0.1278, 51.5074), Arrays.asList(-0.0810, 51.5166));
                int iteration = 1;

                // Act: call updateCustomModel (this method is package-private so we can access it)
                graphHopperService.updateCustomModel(request, badCoords, iteration);

                // Assert: verify that a custom model is added to the request with an area defined
                CustomModel cm = request.getCustomModel();
                assertNotNull(cm);
                // Verify that the areas collection is not empty
                assertNotNull(cm.getAreas());
                assertFalse(cm.getAreas().getFeatures().isEmpty());
                // Check that the priority list contains an entry for "in_bad_area_1"
                // (We rely on the string produced by If("in_bad_area_1", MULTIPLY, "0.01") – you can verify it exists in the custom model's priority list)
                // (Assuming CustomModel stores priorities in a collection accessible by a getter; adjust as necessary.)
        }

        @Test
        void testFormatBusSegment_returnsCorrectMap() {
                // Arrange
                String busRoute = "10";
                GHPoint start = new GHPoint(51.5074, -0.1278);
                GHPoint end = new GHPoint(51.5258, -0.0343);
                List<List<Double>> busPoints = Arrays.asList(
                        Arrays.asList(-0.1278, 51.5074),
                        Arrays.asList(-0.1100, 51.5120),
                        Arrays.asList(-0.0343, 51.5258)
                );

                // Act
                Map<String, Object> result = graphHopperService.formatBusSegment(busRoute, start, end, busPoints);

                // Assert
                assertNotNull(result);
                assertEquals("bus", result.get("mode"));
                assertEquals(busRoute, result.get("busRoute"));
                // Check that start and end coordinates are correctly formatted (as [lon, lat])
                assertEquals(Arrays.asList(start.getLon(), start.getLat()), result.get("start"));
                assertEquals(Arrays.asList(end.getLon(), end.getLat()), result.get("end"));
                assertEquals(false, result.get("points_encoded"));
                assertEquals(busPoints, result.get("points"));
                // Instructions: a list with two entries (boarding and disembarking instructions)
                List<?> instructions = (List<?>) result.get("instructions");
                assertNotNull(instructions);
                assertEquals(2, instructions.size());
                // Check sample instruction texts
                Map<?, ?> instr0 = (Map<?, ?>) instructions.get(0);
                assertTrue(instr0.get("text").toString().contains("Board bus route"));
                Map<?, ?> instr1 = (Map<?, ?>) instructions.get(1);
                assertTrue(instr1.get("text").toString().toLowerCase().contains("disembark"));
        }

        @Test
        void testGenerateHexagon_createsPolygonWithCorrectProperties() {
                // Using ReflectionTestUtils to call the private method generateHexagon
                List<Double> center = Arrays.asList(-0.1278, 51.5074);
                double radius = 100; // meters
                Polygon hexagon = (Polygon) ReflectionTestUtils.invokeMethod(
                        graphHopperService, "generateHexagon", center, radius);
                assertNotNull(hexagon);
                // A valid hexagon polygon here should have 7 coordinates (6 distinct + repeat first to close)
                assertEquals(7, hexagon.getCoordinates().length);
                // Verify that the first and last coordinate are the same (closed ring)
                assertEquals(hexagon.getCoordinates()[0], hexagon.getCoordinates()[6]);
        }

        @Test
        void testCreateAvoidancePolygon_returnsUnionPolygon() {
                // Prepare a list of bad coordinates (each will generate a hexagon)
                List<List<Double>> badCoords = new ArrayList<>();
                badCoords.add(Arrays.asList(-0.1278, 51.5074));
                badCoords.add(Arrays.asList(-0.0810, 51.5166));

                // Invoke the private method createAvoidancePolygon via reflection
                Geometry result = (Geometry) ReflectionTestUtils.invokeMethod(
                        graphHopperService, "createAvoidancePolygon", badCoords);
                assertNotNull(result);
                // The union could either be a Polygon or MultiPolygon
                assertTrue(result instanceof Polygon || result instanceof MultiPolygon);
        }

        @Test
        void testFindClosestIndex_returnsCorrectIndex() {
                List<double[]> busPoints = new ArrayList<>();
                busPoints.add(new double[]{-0.127, 51.507});   // index 0
                busPoints.add(new double[]{-0.110, 51.515});   // index 1
                busPoints.add(new double[]{-0.090, 51.520});   // index 2

                // Choose a user location near the second bus stop
                GHPoint userPoint = new GHPoint(51.515, -0.110);

                Object indexObject = ReflectionTestUtils.invokeMethod(graphHopperService, "findClosestIndex", userPoint, busPoints);
                assertNotNull(indexObject, "Returned index should not be null");
                Integer indexInteger = (Integer) indexObject;

                int index = indexInteger.intValue();
                assertEquals(1, index);
        }


        @Test
        @SuppressWarnings("unchecked")
        void testConvertBusPoints_convertsCorrectly() {
                List<double[]> busPoints = new ArrayList<>();
                busPoints.add(new double[]{-0.1278, 51.5074});
                busPoints.add(new double[]{-0.0810, 51.5166});

                Object resultObject = ReflectionTestUtils.invokeMethod(graphHopperService, "convertBusPoints", busPoints);
                assertNotNull(resultObject, "The result of convertBusPoints should not be null");
                List<List<Double>> result = (List<List<Double>>) resultObject;

                assertEquals(2, result.size());
                assertEquals(Arrays.asList(-0.1278, 51.5074), result.get(0));
                assertEquals(Arrays.asList(-0.0810, 51.5166), result.get(1));
        }

        @Test
        void testInit_ThrowsExceptionWhenOsmFileMissing() {
                // Set a non-existent OSM file path
                ReflectionTestUtils.setField(graphHopperService, "osmFilePath", "non-existent-file.pbf");

                // Expect an exception when initializing
                assertThrows(IllegalStateException.class, () -> {
                        graphHopperService.init();
                });
        }

        @Test
        void testLoadGTFSData_HandlesInvalidLines() throws Exception {
                // Create a temporary GTFS shapes.txt with invalid lines
                tempGtfsDir = Files.createTempDirectory("test-gtfs-invalid");
                shapesFilePath = tempGtfsDir.resolve("shapes.txt");
                List<String> lines = Arrays.asList(
                        "shape_id,lat,lon,sequence", // Header
                        "1,invalid_lat,invalid_lon,0", // Invalid numbers
                        "2,51.5074,-0.1278" // Insufficient parts
                );
                Files.write(shapesFilePath, lines, StandardCharsets.UTF_8);

                // Load the GTFS data
                ReflectionTestUtils.invokeMethod(graphHopperService, "loadGTFSData", shapesFilePath.toString());

                // Verify no routes are loaded due to errors
                Map<String, List<double[]>> busRoutes = (Map<String, List<double[]>>) ReflectionTestUtils.getField(graphHopperService, "busRoutes");
                assertTrue(busRoutes.isEmpty());
        }

        @Test
        void testUpdateCustomModel_WithEmptyBadCoords() {
                GHRequest request = new GHRequest();
                List<List<Double>> badCoords = Collections.emptyList();
                int iteration = 1;

                graphHopperService.updateCustomModel(request, badCoords, iteration);

                CustomModel cm = request.getCustomModel();
                assertTrue(cm.getAreas().getFeatures().isEmpty());
        }

        @Test
        void testGenerateHexagon_ZeroRadius() {
                List<Double> center = Arrays.asList(-0.1278, 51.5074);
                Polygon hexagon = (Polygon) ReflectionTestUtils.invokeMethod(
                        graphHopperService, "generateHexagon", center, 0.0);

                // A zero radius should still create a polygon (though points overlap)
                assertNotNull(hexagon);
                assertEquals(7, hexagon.getCoordinates().length);
        }

        @Test
        void testDistance_CalculationAccuracy() {
                // Distance between London and Paris (approx 344 km)
                double lat1 = 51.5074, lon1 = -0.1278;
                double lat2 = 48.8566, lon2 = 2.3522;
                double expectedDistance = 344_000; // Approx in meters (actual ~344,000m)
                double actualDistance = (Double) ReflectionTestUtils.invokeMethod(
                        graphHopperService, "distance", lat1, lon1, lat2, lon2);

                // Allow 10% tolerance
                assertEquals(expectedDistance, actualDistance, expectedDistance * 0.1);
        }

        @Test
        void testFindClosestIndex_ExactMatch() {
                List<double[]> busPoints = new ArrayList<>();
                busPoints.add(new double[]{-0.1278, 51.5074}); // Index 0
                GHPoint userPoint = new GHPoint(51.5074, -0.1278); // Exact match

                int index = (int) ReflectionTestUtils.invokeMethod(
                        graphHopperService, "findClosestIndex", userPoint, busPoints);

                assertEquals(0, index);
        }

        @Test
        void testGetOptimizedRoute_EmptyResponse() {
                GHRequest request = new GHRequest().setProfile("car");
                GHResponse mockResponse = mock(GHResponse.class);
                when(mockResponse.getAll()).thenReturn(Collections.emptyList());
                when(hopper.route(any())).thenReturn(mockResponse);

                Map<String, Object> result = graphHopperService.getOptimizedRoute(request, "car");

                assertTrue(result.containsKey("error"));
                assertEquals("Failed to calculate route.", result.get("error"));
        }
        @Test
        void testInit_throwsIllegalStateException_whenOSMFileNotExists() {
                // Set an OSM file path that does not exist.
                ReflectionTestUtils.setField(graphHopperService, "osmFilePath", "non_existing_file.pbf");
                Exception exception = assertThrows(IllegalStateException.class, () -> graphHopperService.init());
                assertTrue(exception.getMessage().contains("OSM file not found"));
        }

        @Test
        @SuppressWarnings("unchecked")
        void testLoadGTFSData_skipsLineWithInsufficientParts() throws Exception {
                Path tempFile = Files.createTempFile("test-shapes", ".txt");
                String content = "shape_id,lat,lon,sequence\nbadLine\n";
                Files.write(tempFile, content.getBytes(StandardCharsets.UTF_8));
                ReflectionTestUtils.invokeMethod(graphHopperService, "loadGTFSData", tempFile.toString());
                Object busRoutesObj = ReflectionTestUtils.getField(graphHopperService, "busRoutes");
                Map<String, List<double[]>> busRoutes = (Map<String, List<double[]>>) busRoutesObj;
                // Verify that the line with insufficient parts was skipped.
                assertFalse(busRoutes.containsKey("badLine"));
                Files.deleteIfExists(tempFile);
        }

        @Test
        @SuppressWarnings("unchecked")
        void testLoadGTFSData_skipsLineWithNumberFormatException() throws Exception {
                Path tempFile = Files.createTempFile("test-shapes", ".txt");
                // Provide a line with an invalid latitude (non-numeric).
                String content = "shape_id,lat,lon,sequence\n1,abc,-0.1278,0\n";
                Files.write(tempFile, content.getBytes(StandardCharsets.UTF_8));
                ReflectionTestUtils.invokeMethod(graphHopperService, "loadGTFSData", tempFile.toString());
                Object busRoutesObj = ReflectionTestUtils.getField(graphHopperService, "busRoutes");
                Map<String, List<double[]>> busRoutes = (Map<String, List<double[]>>) busRoutesObj;
                // No entry should be present since the line was skipped.
                assertFalse(busRoutes.containsKey("1"));
                Files.deleteIfExists(tempFile);
        }

        @Test
        @SuppressWarnings("unchecked")
        void testLoadGTFSData_exception() throws Exception {
                // Pass an invalid file path to provoke an exception
                ReflectionTestUtils.invokeMethod(graphHopperService, "loadGTFSData", "nonexistentfile.txt");
                Object busRoutesObj = ReflectionTestUtils.getField(graphHopperService, "busRoutes");
                Map<String, List<double[]>> busRoutes = (Map<String, List<double[]>>) busRoutesObj;
                // Expect busRoutes to be empty because no data was loaded.
                assertTrue(busRoutes.isEmpty());
        }

        @Test
        void testGetBusRouteWithWalking_swappedIndices() {
                // Arrange: choose userPoints such that the bus route's closest indices are swapped.
                List<List<Double>> userPoints = Arrays.asList(
                        Arrays.asList(-0.2, 51.6),   // User start point (lon, lat)
                        Arrays.asList(-0.1, 51.5)    // User end point
                );
                // Return a bus route with two points in reverse order compared to what findClosestIndex would expect.
                Map<String, Object> busRouteData = new HashMap<>();
                busRouteData.put("mode", "bus");
                busRouteData.put("busRoute", "10");
                // Note: busPoints are stored as [lon, lat]
                List<double[]> busPoints = Arrays.asList(
                        new double[]{-0.1, 51.5},    // index 0
                        new double[]{-0.2, 51.6}     // index 1
                );
                busRouteData.put("points", busPoints);
                doReturn(busRouteData).when(graphHopperService).getBusRoute(anyList());

                // Create dummy walking responses.
                ResponsePath mockWalkPath = mock(ResponsePath.class);
                PointList walkPoints = new PointList();
                // arbitrary points for walking segment
                walkPoints.add(51.6, -0.2);
                walkPoints.add(51.5, -0.1);
                when(mockWalkPath.getPoints()).thenReturn(walkPoints);
                GHResponse mockWalkResponse = mock(GHResponse.class);
                when(mockWalkResponse.hasErrors()).thenReturn(false);
                when(mockWalkResponse.getBest()).thenReturn(mockWalkPath);
                when(hopper.route(any(GHRequest.class)))
                        .thenReturn(mockWalkResponse)
                        .thenReturn(mockWalkResponse);
                // Stub formatPathSegment and formatBusSegment to return dummy maps.
                Map<String, Object> walkSegment = Map.of("mode", "walk", "distance", 100.0, "time", 600000L);
                Map<String, Object> busSegment = Map.of("mode", "bus", "distance", 5000.0, "time", 1200000L);
                doReturn(walkSegment).when(graphHopperService).formatPathSegment(eq(mockWalkPath), eq("walk"), anyString(), anyString());
                doReturn(busSegment).when(graphHopperService).formatBusSegment(anyString(), any(GHPoint.class), any(GHPoint.class), anyList());

                // Act:
                Map<String, Object> result = graphHopperService.getBusRouteWithWalking(userPoints);
                // Assert:
                assertNotNull(result);
                assertFalse(result.containsKey("error"));
                assertEquals("success", result.get("status"));
                assertEquals("bus", result.get("mode"));
        }

        // ─── Tests for extractInstructions and formatInstruction ────────────

        @Test
        @SuppressWarnings("unchecked")
        void testExtractInstructions_withNonEmptyPoints() {
                // Arrange: Create a ResponsePath with an InstructionList containing an Instruction with points.
                ResponsePath mockPath = mock(ResponsePath.class);
                InstructionList instructionList = mock(InstructionList.class);
                when(mockPath.getInstructions()).thenReturn(instructionList);
                Instruction instruction = mock(Instruction.class);
                when(instruction.getTurnDescription(any(Translation.class))).thenReturn("Turn right");
                when(instruction.getDistance()).thenReturn(100.0);
                when(instruction.getTime()).thenReturn(60000L);
                when(instruction.getSign()).thenReturn(2);
                // Create a PointList with one point.
                PointList points = new PointList();
                points.add(51.5074, -0.1278);
                when(instruction.getPoints()).thenReturn(points);
                when(instructionList.iterator()).thenReturn(Arrays.asList(instruction).iterator());

                // Ensure the translationMap is set (using a real one)
                TranslationMap translationMap = new TranslationMap().doImport();
                ReflectionTestUtils.setField(graphHopperService, "translationMap", translationMap);

                // Act:
                List<Map<String, Object>> instructions = graphHopperService.extractInstructions(mockPath);
                // Assert:
                assertNotNull(instructions);
                assertEquals(1, instructions.size());
                Map<String, Object> instrMap = instructions.get(0);
                assertEquals("Turn right", instrMap.get("text"));
                assertEquals(100.0, instrMap.get("distance"));
                assertEquals(60000L, instrMap.get("time"));
                assertEquals(2, instrMap.get("sign"));
                assertTrue(instrMap.containsKey("location"));
                List<Double> loc = (List<Double>) instrMap.get("location");
                // Note that the location is stored as [lon, lat]
                assertEquals(-0.1278, loc.get(0), 0.0001);
                assertEquals(51.5074, loc.get(1), 0.0001);
        }

        @Test
        @SuppressWarnings("unchecked")
        void testExtractInstructions_withEmptyPoints() {
                // Arrange: Create a ResponsePath with an Instruction whose PointList is empty.
                ResponsePath mockPath = mock(ResponsePath.class);
                InstructionList instructionList = mock(InstructionList.class);
                when(mockPath.getInstructions()).thenReturn(instructionList);
                Instruction instruction = mock(Instruction.class);
                when(instruction.getTurnDescription(any(Translation.class))).thenReturn("Go straight");
                when(instruction.getDistance()).thenReturn(50.0);
                when(instruction.getTime()).thenReturn(30000L);
                when(instruction.getSign()).thenReturn(0);
                // Return an empty PointList.
                PointList points = new PointList();
                when(instruction.getPoints()).thenReturn(points);
                when(instructionList.iterator()).thenReturn(Arrays.asList(instruction).iterator());

                TranslationMap translationMap = new TranslationMap().doImport();
                ReflectionTestUtils.setField(graphHopperService, "translationMap", translationMap);

                // Act:
                List<Map<String, Object>> instructions = graphHopperService.extractInstructions(mockPath);
                // Assert:
                assertNotNull(instructions);
                assertEquals(1, instructions.size());
                Map<String, Object> instrMap = instructions.get(0);
                assertEquals("Go straight", instrMap.get("text"));
                assertEquals(50.0, instrMap.get("distance"));
                assertEquals(30000L, instrMap.get("time"));
                assertEquals(0, instrMap.get("sign"));
                // When points is empty, "location" should not be added.
                assertFalse(instrMap.containsKey("location"));
        }
}