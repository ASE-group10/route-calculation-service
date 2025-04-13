package nl.ase_wayfinding.routecalc.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Configuration
@Import(SecurityConfig.class)
public class SecurityTestConfig {

    // A dummy REST controller to provide an endpoint for testing.
    @RestController
    public static class DummyController {
        @GetMapping("/test")
        public String test() {
            return "OK";
        }
    }
}
