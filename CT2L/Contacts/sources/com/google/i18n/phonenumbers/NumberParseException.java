package com.google.i18n.phonenumbers;

public class NumberParseException extends Exception {
    private ErrorType errorType;
    private String message;

    public enum ErrorType {
        INVALID_COUNTRY_CODE,
        NOT_A_NUMBER,
        TOO_SHORT_AFTER_IDD,
        TOO_SHORT_NSN,
        TOO_LONG
    }

    public NumberParseException(ErrorType errorType, String message) {
        super(message);
        this.message = message;
        this.errorType = errorType;
    }

    public ErrorType getErrorType() {
        return this.errorType;
    }

    @Override
    public String toString() {
        String strValueOf = String.valueOf(String.valueOf(this.errorType));
        String strValueOf2 = String.valueOf(String.valueOf(this.message));
        return new StringBuilder(strValueOf.length() + 14 + strValueOf2.length()).append("Error type: ").append(strValueOf).append(". ").append(strValueOf2).toString();
    }
}
