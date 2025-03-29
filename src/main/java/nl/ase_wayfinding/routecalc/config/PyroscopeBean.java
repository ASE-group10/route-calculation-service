package nl.ase_wayfinding.routecalc.config;

import io.pyroscope.http.Format;
import io.pyroscope.javaagent.EventType;
import io.pyroscope.javaagent.PyroscopeAgent;
import io.pyroscope.javaagent.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@Configuration
public class PyroscopeBean {

    @Value("${spring.profiles.active}")
    private String activeProfile;

    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${pyroscope.server.address}")
    private String pyroscopeServerAddress;

    @Value("${pyroscope.auth.user}")
    private String pyroscopeServerAuthUser;

    @Value("${pyroscope.auth.password}")
    private String pyroscopeServerAuthPassword;

    public PyroscopeBean() {
        System.out.println("PyroscopeBean created");
    }

    @PostConstruct
    public void init() {

//        if (activeProfile.equals("local") || pyroscopeServerAuthUser.isEmpty() || pyroscopeServerAuthPassword.isEmpty()) {
//            System.out.println("Pyroscope is disabled in local profile");
//            return;
//        }

        PyroscopeAgent.start(
                new Config.Builder()
                        .setApplicationName(applicationName + "-" + activeProfile)
                        .setProfilingEvent(EventType.ITIMER)
                        .setProfilingEvent(EventType.CPU)
                        .setFormat(Format.JFR)
                        .setServerAddress(pyroscopeServerAddress)
                        .setBasicAuthUser(pyroscopeServerAuthUser)
                        .setBasicAuthPassword(pyroscopeServerAuthPassword)
                        .setProfilingAlloc("512k")
                        .build()
        );
    }
}
