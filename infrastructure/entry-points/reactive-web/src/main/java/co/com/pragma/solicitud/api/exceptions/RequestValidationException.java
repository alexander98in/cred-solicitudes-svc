package co.com.pragma.solicitud.api.exceptions;

import java.util.Map;

public class RequestValidationException extends RuntimeException {

    private final Map<String, String> errors;

    public RequestValidationException(String message, Map<String, String> errors) {
        super(message);
        this.errors = errors;
    }

    public Map<String, String> getErrors() {
        return errors;
    }
}
