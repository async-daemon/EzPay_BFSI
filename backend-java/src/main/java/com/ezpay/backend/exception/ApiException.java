package com.ezpay.backend.exception;

import org.springframework.http.HttpStatus;

/** Carries an HTTP status + a JSON-serializable detail, mirroring FastAPI's HTTPException. */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final Object detail;

    public ApiException(HttpStatus status, String detail) {
        super(detail);
        this.status = status;
        this.detail = detail;
    }

    public ApiException(HttpStatus status, Object detail) {
        super(String.valueOf(detail));
        this.status = status;
        this.detail = detail;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public Object getDetail() {
        return detail;
    }
}
