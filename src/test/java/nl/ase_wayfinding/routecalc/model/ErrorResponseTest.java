package nl.ase_wayfinding.routecalc.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ErrorResponseTest {

    @Test
    void testErrorResponse() {
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setErrorMessage("Not Found");
        errorResponse.setErrorCode(404);

        assertEquals("Not Found", errorResponse.getErrorMessage());
        assertEquals(404, errorResponse.getErrorCode());
    }
}
