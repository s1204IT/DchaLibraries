package com.mediatek.vcalendar.parameter;

import com.mediatek.vcalendar.utils.LogUtil;
import java.util.Locale;

public final class ParameterFactory {
    private static final String TAG = "ParameterFactory";

    private ParameterFactory() {
    }

    public static Parameter createParameter(String str, String str2) {
        LogUtil.d(TAG, "createParameter(): name: " + str + " value: " + str2);
        if (str == null) {
            LogUtil.e(TAG, "createParameter(): Cannot create a parameter without giving defined name");
            return null;
        }
        String upperCase = str.toUpperCase(Locale.US);
        if (Parameter.CN.equals(upperCase)) {
            return new Cn(str2);
        }
        if (Parameter.ENCODING.equals(upperCase)) {
            return new Encoding(str2);
        }
        if (Parameter.PARTSTAT.equals(upperCase)) {
            return new PartStat(str2);
        }
        if (Parameter.ROLE.equals(upperCase)) {
            return new Role(str2);
        }
        if ("TZID".equals(upperCase)) {
            return new TzId(str2);
        }
        if (Parameter.VALUE.equals(upperCase)) {
            return new Value(str2);
        }
        if (Parameter.X_RELATIONSHIP.equals(upperCase)) {
            return new XRelationship(str2);
        }
        return new Parameter(upperCase, str2);
    }
}
