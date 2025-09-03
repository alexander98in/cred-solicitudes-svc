package co.com.pragma.solicitud.api.exceptions;

import java.util.Map;

public class ResponseStatusException extends RuntimeException{
    private final Map<String, String> errors;
    public ResponseStatusException(String message, Map<String, String> errors) {
        super(message);
        this.errors = errors;
    }
    public  ResponseStatusException(String code, String msg)
    {
        super(msg);
        this.errors = Map.of(code, msg);
    }

    public Map<String, String> getErrors() { return errors; }
}
