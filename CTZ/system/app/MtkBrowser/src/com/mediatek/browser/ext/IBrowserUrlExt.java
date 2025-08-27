package com.mediatek.browser.ext;

import android.content.Context;
import android.text.InputFilter;

/* loaded from: classes.dex */
public interface IBrowserUrlExt {
    String checkAndTrimUrl(String str);

    InputFilter[] checkUrlLengthLimit(Context context);

    String getNavigationBarTitle(String str, String str2);

    String getOverrideFocusContent(boolean z, String str, String str2, String str3);

    String getOverrideFocusTitle(String str, String str2);

    boolean redirectCustomerUrl(String str);
}
