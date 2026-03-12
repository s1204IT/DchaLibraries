package com.android.settings.intelligence.search.indexing;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import com.android.settings.intelligence.R;

public class XmlParserUtils {
    public static String getDataKey(Context context, AttributeSet attributeSet) {
        return getData(context, attributeSet, R.styleable.Preference, 6);
    }

    public static String getDataTitle(Context context, AttributeSet attributeSet) {
        return getData(context, attributeSet, R.styleable.Preference, 4);
    }

    public static String getDataSummary(Context context, AttributeSet attributeSet) {
        return getData(context, attributeSet, R.styleable.Preference, 7);
    }

    public static String getDataSummaryOn(Context context, AttributeSet attributeSet) {
        return getData(context, attributeSet, R.styleable.CheckBoxPreference, 0);
    }

    public static String getDataEntries(Context context, AttributeSet attributeSet) {
        return getDataEntries(context, attributeSet, R.styleable.ListPreference, 0);
    }

    public static String getDataKeywords(Context context, AttributeSet attributeSet) {
        String attributeValue = attributeSet.getAttributeValue("http://schemas.android.com/apk/res-auto", "keywords");
        if (TextUtils.isEmpty(attributeValue)) {
            return null;
        }
        if (!attributeValue.startsWith("@")) {
            return attributeValue;
        }
        try {
            return context.getString(Integer.parseInt(attributeValue.substring(1)));
        } catch (NumberFormatException e) {
            Log.w("XmlParserUtils", "Failed to parse keyword attribute, skipping " + attributeValue);
            return null;
        }
    }

    public static int getDataIcon(Context context, AttributeSet attributeSet) {
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.Preference);
        int resourceId = typedArrayObtainStyledAttributes.getResourceId(0, 0);
        typedArrayObtainStyledAttributes.recycle();
        return resourceId;
    }

    public static String getDataChildFragment(Context context, AttributeSet attributeSet) {
        return getData(context, attributeSet, R.styleable.Preference, 13);
    }

    private static String getData(Context context, AttributeSet attributeSet, int[] iArr, int i) {
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, iArr);
        String string = typedArrayObtainStyledAttributes.getString(i);
        typedArrayObtainStyledAttributes.recycle();
        return string;
    }

    private static String getDataEntries(Context context, AttributeSet attributeSet, int[] iArr, int i) {
        String[] stringArray;
        int length;
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, iArr);
        TypedValue typedValuePeekValue = typedArrayObtainStyledAttributes.peekValue(i);
        typedArrayObtainStyledAttributes.recycle();
        if (typedValuePeekValue != null && typedValuePeekValue.type == 1 && typedValuePeekValue.resourceId != 0) {
            stringArray = context.getResources().getStringArray(typedValuePeekValue.resourceId);
        } else {
            stringArray = null;
        }
        if (stringArray != null) {
            length = stringArray.length;
        } else {
            length = 0;
        }
        if (length == 0) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (int i2 = 0; i2 < length; i2++) {
            sb.append(stringArray[i2]);
            sb.append("|");
        }
        return sb.toString();
    }
}
