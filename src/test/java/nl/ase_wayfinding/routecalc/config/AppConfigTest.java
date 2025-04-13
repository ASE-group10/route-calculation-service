// File: AppConfigTest.java
package nl.ase_wayfinding.routecalc.config;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class AppConfigTest {

    @Test
    void testRestTemplateBean() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class)) {
            RestTemplate rt = context.getBean(RestTemplate.class);
            assertNotNull(rt);
        }
    }
}
