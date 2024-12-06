package nl.ase_wayfinding.routecalc.model;

import lombok.Data;

@Data
public class ErrorResponse {
    private String errorMessage;    // Description of the error

    public int getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    private int errorCode;          // HTTP error code
}
