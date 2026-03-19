package com.mediatek.vcalendar.parameter;

public class Encoding extends Parameter {
    public static final String BASE64 = "BASE64";
    public static final String BIT8 = "8BIT";
    public static final String QUOTED_PRINTABLE = "QUOTED-PRINTABLE";

    public Encoding(String str) {
        super(Parameter.ENCODING, str);
    }
}
