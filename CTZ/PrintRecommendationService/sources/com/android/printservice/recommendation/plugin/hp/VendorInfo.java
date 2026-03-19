package com.android.printservice.recommendation.plugin.hp;

import android.content.res.Resources;
import java.util.Arrays;

public final class VendorInfo {
    public final String[] mDNSValues;
    public final int mID;
    public final String mPackageName;
    public final String mVendorID;

    public VendorInfo(Resources resources, int i) {
        String[] strArr;
        this.mID = i;
        String[] stringArray = resources.getStringArray(i);
        stringArray = (stringArray == null || stringArray.length < 2) ? new String[]{null, null} : stringArray;
        this.mPackageName = stringArray[0];
        this.mVendorID = stringArray[1];
        if (stringArray.length > 2) {
            strArr = (String[]) Arrays.copyOfRange(stringArray, 2, stringArray.length);
        } else {
            strArr = new String[0];
        }
        this.mDNSValues = strArr;
    }
}
