package com.library.support.orrai.exception;

/**
 * Thrown when the caller lacks permission for the operation. Maps to HTTP 403 (FORBIDDEN).
 */
public class ForbiddenException extends BusinessException {

    private static final long serialVersionUID = 1L;

    public ForbiddenException(String message) {
        super(ErrorCode.FORBIDDEN, message);
    }

    public ForbiddenException(String message, Throwable cause) {
        super(ErrorCode.FORBIDDEN, message, cause);
    }
}
