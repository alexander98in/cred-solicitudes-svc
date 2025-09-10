package co.com.pragma.solicitud.usecase.exceptions;

public class ResourceNotFoundException extends DomainException{

    public ResourceNotFoundException(String message) {
        super("RESOURCE_NOT_FOUND: ", message);
    }

    public ResourceNotFoundException(String code, String message) {
        super(code, message);
    }
}
