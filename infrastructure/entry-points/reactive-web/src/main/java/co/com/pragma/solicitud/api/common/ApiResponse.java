package co.com.pragma.solicitud.api.common;

import java.time.OffsetDateTime;

public class ApiResponse<T> {

    private int httpStatus;
    private String message;
    private T data;
    private String path;
    private OffsetDateTime timestamp;

    public ApiResponse() {
        this.timestamp = OffsetDateTime.now();
    }

    public ApiResponse(int httpStatus, String message, T data, String path) {
        this.httpStatus = httpStatus;
        this.message = message;
        this.data = data;
        this.path = path;
        this.timestamp = OffsetDateTime.now();
    }

    public static <T> ApiResponse<T> of(int status, String message, T data, String path) {
        return new ApiResponse<>(status, message, data, path);
    }

    public int getHttpStatus() { return httpStatus; }
    public void setHttpStatus(int httpStatus) { this.httpStatus = httpStatus; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public OffsetDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(OffsetDateTime timestamp) { this.timestamp = timestamp; }
}
