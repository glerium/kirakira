package com.kirakira.entity.exception;

public class UserNotFoundException extends CodeforcesApiException {
    public UserNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
    public UserNotFoundException(String message) {
        super(message);
    }
}
