package com.mediatek.common.jpe;

public class JpeException extends SecurityException {
    private String errorMessage;

    public JpeException(String str) {
        super(str, null);
        this.errorMessage = null;
        this.errorMessage = str;
    }

    @Override
    public String getMessage() {
        StringBuffer stringBuffer = new StringBuffer();
        if (this.errorMessage != null) {
            stringBuffer.append("error - ");
            stringBuffer.append(this.errorMessage);
            stringBuffer.append("\n");
        } else {
            stringBuffer.append(super.getMessage());
        }
        return stringBuffer.toString();
    }
}
