package nl.ase_wayfinding.routecalc.config;

import org.junit.jupiter.api.Test;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.StandardEnvironment;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AwsSecretsInitializerTest {

    @Test
    void testInitializeWithLocalProfile() {
        ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);
        ConfigurableEnvironment environment = new StandardEnvironment();
        environment.getSystemProperties().put("spring.profiles.active", "local");

        when(context.getEnvironment()).thenReturn(environment);

        AwsSecretsInitializer initializer = new AwsSecretsInitializer();
        initializer.initialize(context); // Should skip without error
    }

    @Test
    void testInitializeThrowsException() {
        ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);
        ConfigurableEnvironment environment = mock(ConfigurableEnvironment.class);
        when(context.getEnvironment()).thenReturn(environment);
        when(environment.getProperty("spring.profiles.active", "default")).thenReturn("dev");

        // No secrets manager mock = this will likely fail and throw
        AwsSecretsInitializer initializer = new AwsSecretsInitializer();

        assertThrows(RuntimeException.class, () -> initializer.initialize(context));
    }
}
