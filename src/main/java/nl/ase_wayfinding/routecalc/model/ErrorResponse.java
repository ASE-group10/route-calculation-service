package nl.ase_wayfinding.routecalc.model;

import lombok.Data;

@Data
public class ErrorResponse {
    private String errorMessage;    // Description of the error
    private int errorCode;          // HTTP error code
}
