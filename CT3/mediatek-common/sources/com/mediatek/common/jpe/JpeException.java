package com.mediatek.common.jpe;

public class JpeException extends SecurityException {
    private String errorMessage;

    public JpeException(String message) {
        super(message, null);
        this.errorMessage = null;
        this.errorMessage = message;
    }

    @Override
    public String getMessage() {
        StringBuffer value = new StringBuffer();
        if (this.errorMessage != null) {
            value.append("error - ").append(this.errorMessage).append("\n");
        } else {
            value.append(super.getMessage());
        }
        return value.toString();
    }
}
