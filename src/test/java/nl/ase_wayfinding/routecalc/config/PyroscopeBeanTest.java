package nl.ase_wayfinding.routecalc.config;

import io.pyroscope.javaagent.PyroscopeAgent;
import io.pyroscope.javaagent.config.Config;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.lang.reflect.Field;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PyroscopeBeanTest {

    @Test
    void testInitStartsPyroscopeAgent() throws Exception {
        System.setProperty("os.name", "Linux");

        PyroscopeBean bean = new PyroscopeBean();

        // Inject values via reflection
        inject(bean, "activeProfile", "prod");
        inject(bean, "applicationName", "myapp");
        inject(bean, "pyroscopeServerAddress", "http://pyro");
        inject(bean, "pyroscopeServerAuthUser", "admin");
        inject(bean, "pyroscopeServerAuthPassword", "securepass");

        try (MockedStatic<PyroscopeAgent> mocked = Mockito.mockStatic(PyroscopeAgent.class)) {
            mocked.when(() -> PyroscopeAgent.start(any(Config.class)))
                    .thenAnswer(invocation -> {
                        System.out.println("✅ PyroscopeAgent.start() was called");
                        return null;
                    });

            bean.init();

            mocked.verify(() -> PyroscopeAgent.start(any(Config.class)), times(1));
        }
    }

    private void inject(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
