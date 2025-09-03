package co.com.pragma.solicitud.usecase.exceptions;

public class ResourceAlreadyExistsException extends DomainException {

    public ResourceAlreadyExistsException(String resource, String id) {
        super("RECURSO_EXISTENTE: ", resource + " encontrado: " + id);
    }

    public ResourceAlreadyExistsException(String message) {
        super("RECURSO_EXISTENTE: " + message);
    }
}
