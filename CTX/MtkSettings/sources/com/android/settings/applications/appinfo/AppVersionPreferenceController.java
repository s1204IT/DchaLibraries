package com.android.settings.applications.appinfo;

import android.content.Context;
import android.text.BidiFormatter;
import com.android.settings.R;

public class AppVersionPreferenceController extends AppInfoPreferenceControllerBase {
    public AppVersionPreferenceController(Context context, String str) {
        super(context, str);
    }

    @Override
    public CharSequence getSummary() {
        return this.mContext.getString(R.string.version_text, BidiFormatter.getInstance().unicodeWrap(this.mParent.getPackageInfo().versionName));
    }
}
