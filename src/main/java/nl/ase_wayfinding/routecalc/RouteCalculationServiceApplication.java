package nl.ase_wayfinding.routecalc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "nl.ase_wayfinding.routecalc")
public class RouteCalculationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(RouteCalculationServiceApplication.class, args);
    }
}
