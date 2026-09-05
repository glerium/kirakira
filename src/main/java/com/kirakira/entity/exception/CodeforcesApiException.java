package com.kirakira.entity.exception;

public class CodeforcesApiException extends RuntimeException {
    public CodeforcesApiException(String message, Throwable cause) {
        super(message, cause);
    }
     public CodeforcesApiException(String message) {
        super(message);
    }
}
