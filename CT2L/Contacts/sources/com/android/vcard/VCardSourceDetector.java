package com.android.vcard;

import android.text.TextUtils;
import android.util.Log;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class VCardSourceDetector implements VCardInterpreter {
    private String mSpecifiedCharset;
    private static Set<String> APPLE_SIGNS = new HashSet(Arrays.asList("X-PHONETIC-FIRST-NAME", "X-PHONETIC-MIDDLE-NAME", "X-PHONETIC-LAST-NAME", "X-ABADR", "X-ABUID"));
    private static Set<String> JAPANESE_MOBILE_PHONE_SIGNS = new HashSet(Arrays.asList("X-GNO", "X-GN", "X-REDUCTION"));
    private static Set<String> WINDOWS_MOBILE_PHONE_SIGNS = new HashSet(Arrays.asList("X-MICROSOFT-ASST_TEL", "X-MICROSOFT-ASSISTANT", "X-MICROSOFT-OFFICELOC"));
    private static Set<String> FOMA_SIGNS = new HashSet(Arrays.asList("X-SD-VERN", "X-SD-FORMAT_VER", "X-SD-CATEGORIES", "X-SD-CLASS", "X-SD-DCREATED", "X-SD-DESCRIPTION"));
    private static String TYPE_FOMA_CHARSET_SIGN = "X-SD-CHAR_CODE";
    private int mParseType = 0;
    private int mVersion = -1;

    @Override
    public void onVCardStarted() {
    }

    @Override
    public void onVCardEnded() {
    }

    @Override
    public void onEntryStarted() {
    }

    @Override
    public void onEntryEnded() {
    }

    @Override
    public void onPropertyCreated(VCardProperty property) {
        String propertyName = property.getName();
        List<String> valueList = property.getValueList();
        if (propertyName.equalsIgnoreCase("VERSION") && valueList.size() > 0) {
            String versionString = valueList.get(0);
            if (versionString.equals("2.1")) {
                this.mVersion = 0;
            } else if (versionString.equals("3.0")) {
                this.mVersion = 1;
            } else if (versionString.equals("4.0")) {
                this.mVersion = 2;
            } else {
                Log.w("vCard", "Invalid version string: " + versionString);
            }
        } else if (propertyName.equalsIgnoreCase(TYPE_FOMA_CHARSET_SIGN)) {
            this.mParseType = 3;
            if (valueList.size() > 0) {
                this.mSpecifiedCharset = valueList.get(0);
            }
        }
        if (this.mParseType == 0) {
            if (WINDOWS_MOBILE_PHONE_SIGNS.contains(propertyName)) {
                this.mParseType = 4;
                return;
            }
            if (FOMA_SIGNS.contains(propertyName)) {
                this.mParseType = 3;
            } else if (JAPANESE_MOBILE_PHONE_SIGNS.contains(propertyName)) {
                this.mParseType = 2;
            } else if (APPLE_SIGNS.contains(propertyName)) {
                this.mParseType = 1;
            }
        }
    }

    public int getEstimatedType() {
        switch (this.mParseType) {
            case 2:
                return 402653192;
            case 3:
                return 939524104;
            default:
                if (this.mVersion == 0) {
                    return -1073741824;
                }
                if (this.mVersion == 1) {
                    return -1073741823;
                }
                if (this.mVersion == 2) {
                    return -1073741822;
                }
                return 0;
        }
    }

    public String getEstimatedCharset() {
        if (TextUtils.isEmpty(this.mSpecifiedCharset)) {
            return this.mSpecifiedCharset;
        }
        switch (this.mParseType) {
            case 1:
                return "UTF-8";
            case 2:
            case 3:
            case 4:
                return "SHIFT_JIS";
            default:
                return null;
        }
    }
}
