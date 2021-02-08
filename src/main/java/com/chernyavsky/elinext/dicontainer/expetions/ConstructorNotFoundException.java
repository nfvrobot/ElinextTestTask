package com.chernyavsky.elinext.dicontainer.expetions;

public class ConstructorNotFoundException extends RuntimeException {

    public ConstructorNotFoundException() {
    }

    public ConstructorNotFoundException(String message) {
        super(message);
    }

    public ConstructorNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConstructorNotFoundException(Throwable cause) {
        super(cause);
    }
}
