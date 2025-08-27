package com.android.settings.core;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Xml;
import com.android.internal.R;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.xmlpull.v1.XmlPullParserException;

/* loaded from: classes.dex */
public class PreferenceXmlParserUtils {
    static final String PREF_SCREEN_TAG = "PreferenceScreen";
    private static final List<String> SUPPORTED_PREF_TYPES = Arrays.asList("Preference", "PreferenceCategory", PREF_SCREEN_TAG);

    @Deprecated
    public static String getDataKey(Context context, AttributeSet attributeSet) {
        return getStringData(context, attributeSet, R.styleable.Preference, 6);
    }

    @Deprecated
    public static String getDataTitle(Context context, AttributeSet attributeSet) {
        return getStringData(context, attributeSet, R.styleable.Preference, 4);
    }

    public static List<Bundle> extractMetadata(Context context, int i, int i2) throws XmlPullParserException, Resources.NotFoundException, IOException {
        int next;
        ArrayList arrayList = new ArrayList();
        if (i <= 0) {
            Log.d("PreferenceXmlParserUtil", i + " is invalid.");
            return arrayList;
        }
        XmlResourceParser xml = context.getResources().getXml(i);
        do {
            next = xml.next();
            if (next == 1) {
                break;
            }
        } while (next != 2);
        int depth = xml.getDepth();
        while (true) {
            if (next == 2) {
                String name = xml.getName();
                if ((hasFlag(i2, 1) || !TextUtils.equals(PREF_SCREEN_TAG, name)) && (SUPPORTED_PREF_TYPES.contains(name) || name.endsWith("Preference"))) {
                    Bundle bundle = new Bundle();
                    TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(Xml.asAttributeSet(xml), com.android.settings.R.styleable.Preference);
                    if (hasFlag(i2, 4)) {
                        bundle.putString("type", name);
                    }
                    if (hasFlag(i2, 2)) {
                        bundle.putString("key", getKey(typedArrayObtainStyledAttributes));
                    }
                    if (hasFlag(i2, 8)) {
                        bundle.putString("controller", getController(typedArrayObtainStyledAttributes));
                    }
                    if (hasFlag(i2, 16)) {
                        bundle.putString("title", getTitle(typedArrayObtainStyledAttributes));
                    }
                    if (hasFlag(i2, 32)) {
                        bundle.putString("summary", getSummary(typedArrayObtainStyledAttributes));
                    }
                    if (hasFlag(i2, 64)) {
                        bundle.putInt("icon", getIcon(typedArrayObtainStyledAttributes));
                    }
                    if (hasFlag(i2, 128)) {
                        bundle.putBoolean("platform_slice", getPlatformSlice(typedArrayObtainStyledAttributes));
                    }
                    if (hasFlag(i2, 256)) {
                        bundle.putString("keywords", getKeywords(typedArrayObtainStyledAttributes));
                    }
                    arrayList.add(bundle);
                    typedArrayObtainStyledAttributes.recycle();
                }
            }
            next = xml.next();
            if (next == 1 || (next == 3 && xml.getDepth() <= depth)) {
                break;
            }
        }
        xml.close();
        return arrayList;
    }

    @Deprecated
    private static String getStringData(Context context, AttributeSet attributeSet, int[] iArr, int i) {
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, iArr);
        String string = typedArrayObtainStyledAttributes.getString(i);
        typedArrayObtainStyledAttributes.recycle();
        return string;
    }

    private static boolean hasFlag(int i, int i2) {
        return (i & i2) != 0;
    }

    private static String getKey(TypedArray typedArray) {
        return typedArray.getString(6);
    }

    private static String getTitle(TypedArray typedArray) {
        return typedArray.getString(4);
    }

    private static String getSummary(TypedArray typedArray) {
        return typedArray.getString(7);
    }

    private static String getController(TypedArray typedArray) {
        return typedArray.getString(18);
    }

    private static int getIcon(TypedArray typedArray) {
        return typedArray.getResourceId(0, 0);
    }

    private static boolean getPlatformSlice(TypedArray typedArray) {
        return typedArray.getBoolean(31, false);
    }

    private static String getKeywords(TypedArray typedArray) {
        return typedArray.getString(27);
    }
}
