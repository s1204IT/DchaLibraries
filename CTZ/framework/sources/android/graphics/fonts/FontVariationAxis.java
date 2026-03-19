package android.graphics.fonts;

import android.text.TextUtils;
import java.util.ArrayList;
import java.util.regex.Pattern;

public final class FontVariationAxis {
    private final float mStyleValue;
    private final int mTag;
    private final String mTagString;
    private static final Pattern TAG_PATTERN = Pattern.compile("[ -~]{4}");
    private static final Pattern STYLE_VALUE_PATTERN = Pattern.compile("-?(([0-9]+(\\.[0-9]+)?)|(\\.[0-9]+))");

    public FontVariationAxis(String str, float f) {
        if (!isValidTag(str)) {
            throw new IllegalArgumentException("Illegal tag pattern: " + str);
        }
        this.mTag = makeTag(str);
        this.mTagString = str;
        this.mStyleValue = f;
    }

    public int getOpenTypeTagValue() {
        return this.mTag;
    }

    public String getTag() {
        return this.mTagString;
    }

    public float getStyleValue() {
        return this.mStyleValue;
    }

    public String toString() {
        return "'" + this.mTagString + "' " + Float.toString(this.mStyleValue);
    }

    private static boolean isValidTag(String str) {
        return str != null && TAG_PATTERN.matcher(str).matches();
    }

    private static boolean isValidValueFormat(String str) {
        return str != null && STYLE_VALUE_PATTERN.matcher(str).matches();
    }

    public static int makeTag(String str) {
        return str.charAt(3) | (str.charAt(0) << 24) | (str.charAt(1) << 16) | (str.charAt(2) << '\b');
    }

    public static FontVariationAxis[] fromFontVariationSettings(String str) {
        int i;
        if (str == null || str.isEmpty()) {
            return null;
        }
        ArrayList arrayList = new ArrayList();
        int length = str.length();
        int i2 = 0;
        while (i2 < length) {
            char cCharAt = str.charAt(i2);
            if (!Character.isWhitespace(cCharAt)) {
                if ((cCharAt == '\'' || cCharAt == '\"') && length >= (i = i2 + 6)) {
                    int i3 = i2 + 5;
                    if (str.charAt(i3) == cCharAt) {
                        String strSubstring = str.substring(i2 + 1, i3);
                        int iIndexOf = str.indexOf(44, i);
                        if (iIndexOf == -1) {
                            iIndexOf = length;
                        }
                        try {
                            arrayList.add(new FontVariationAxis(strSubstring, Float.parseFloat(str.substring(i, iIndexOf))));
                            i2 = iIndexOf;
                        } catch (NumberFormatException e) {
                            throw new IllegalArgumentException("Failed to parse float string: " + e.getMessage());
                        }
                    }
                }
                throw new IllegalArgumentException("Tag should be wrapped with double or single quote: " + str);
            }
            i2++;
        }
        if (arrayList.isEmpty()) {
            return null;
        }
        return (FontVariationAxis[]) arrayList.toArray(new FontVariationAxis[0]);
    }

    public static String toFontVariationSettings(FontVariationAxis[] fontVariationAxisArr) {
        if (fontVariationAxisArr == null) {
            return "";
        }
        return TextUtils.join(",", fontVariationAxisArr);
    }
}
