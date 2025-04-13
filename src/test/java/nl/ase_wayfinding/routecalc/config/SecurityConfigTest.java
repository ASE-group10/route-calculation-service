package nl.ase_wayfinding.routecalc.config;

import nl.ase_wayfinding.routecalc.service.GraphHopperService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SecurityConfigTest.DummyController.class)
@Import(SecurityConfigTest.SecurityTestConfig.class)
@TestPropertySource(properties = "spring.web.resources.add-mappings=false")
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SecurityFilterChain securityFilterChain;

    // Provide a mock for GraphHopperService even if not directly used here.
    @MockBean
    private GraphHopperService graphHopperService;

    @Test
    void testSecurityFilterChainIsLoaded() {
        assertThat(securityFilterChain).isNotNull();
    }

    @Test
    void testProtectedEndpoint() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().isUnauthorized());
    }

    @Controller
    static class DummyController {
        @GetMapping("/test")
        @ResponseBody
        public String testEndpoint() {
            return "OK";
        }
    }

    @Configuration
    static class SecurityTestConfig {

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            http
                    .authorizeRequests()
                    .antMatchers("/test").permitAll()
                    .anyRequest().authenticated()
                    .and()
                    .httpBasic(); // For protected endpoints, returns 401

            return http.build();
        }
    }
}
