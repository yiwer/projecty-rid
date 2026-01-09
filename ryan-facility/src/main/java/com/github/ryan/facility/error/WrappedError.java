package com.github.ryan.facility.error;

import lombok.Getter;

/**
 * <b> WrappedError </b>
 *
 * @author : yvvb
 * @since : 12/16/2025
 */
@Getter
public class WrappedError {

    private final ErrorTypeInterface errorType;

    private final Exception exception;

    private final Object[] args;

    public WrappedError(ErrorTypeInterface errorType, Exception exception, Object[] args) {
        this.args = args != null ? args : new Object[0];
        this.errorType = errorType;
        this.exception = exception;
    }

    public WrappedError(ErrorTypeInterface errorType, Exception exception) {
        this(errorType, exception, new Object[0]);
    }

    public WrappedError(ErrorTypeInterface errorType) {
        this(errorType, null, new Object[0]);
    }


    public static WrappedError of(ErrorTypeInterface errorType, Exception exception, Object[] args) {
        return new WrappedError(errorType, exception, args);
    }

    public static WrappedError of(ErrorTypeInterface errorType, Exception exception) {
        return new WrappedError(errorType, exception);
    }

    public static WrappedError of(ErrorTypeInterface errorType) {
        return new WrappedError(errorType);
    }
}
