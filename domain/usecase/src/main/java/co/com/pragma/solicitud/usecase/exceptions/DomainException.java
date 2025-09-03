package co.com.pragma.solicitud.usecase.exceptions;

public class DomainException extends RuntimeException{

    private final String code;
    protected DomainException(String code, String message) {
        super(message);
        this.code = code;
    }

    protected DomainException(String message) {
        super(message);
        this.code = "ERROR_DE_DOMINIO";
    }
    public String getCode() { return code; }

}
