package nl.ase_wayfinding.routecalc;

import nl.ase_wayfinding.routecalc.config.AwsSecretsInitializer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"nl.ase_wayfinding.routecalc"})
public class RouteCalculationServiceApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(RouteCalculationServiceApplication.class);
        app.addInitializers(new AwsSecretsInitializer());
        app.run(args);
    }
}
