package nl.ase_wayfinding.routecalc.model;

import org.junit.jupiter.api.Test;

import java.sql.Timestamp;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    void testUserModel() {
        User user = new User();
        user.setEmail("test@example.com");
        user.setName("Test User");

        assertEquals("test@example.com", user.getEmail());
        assertEquals("Test User", user.getName());
    }
}
