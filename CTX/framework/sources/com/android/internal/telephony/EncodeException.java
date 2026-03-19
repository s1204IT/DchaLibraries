package com.android.internal.telephony;

public class EncodeException extends Exception {
    public EncodeException() {
    }

    public EncodeException(String str) {
        super(str);
    }

    public EncodeException(char c) {
        super("Unencodable char: '" + c + "'");
    }
}
