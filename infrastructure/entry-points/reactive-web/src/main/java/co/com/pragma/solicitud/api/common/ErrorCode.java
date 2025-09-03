package co.com.pragma.solicitud.api.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    GENERIC("AUT-0001"),
    VALIDATION("AUT-0002"),
    CONFLICT("AUT-0003"),
    NOT_FOUND("AUT-0004"),
    PAYLOAD_INVALID("AUT-0005"),
    BUSINESS_RULE("AUT-0006");

    private final String code;
}
