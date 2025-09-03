package co.com.pragma.solicitud.usecase.exceptions;

public class ResourceNotFoundException extends DomainException{

    public ResourceNotFoundException(String message) {
        super("RESOURCE_NOT_FOUND: ", message);
    }
    public ResourceNotFoundException(String resource, String id) {
        super("RESOURCE_NOT_FOUND: ", resource + " no encontrado: " + id);
    }
}
