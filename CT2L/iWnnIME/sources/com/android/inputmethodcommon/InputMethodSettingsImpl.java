package com.android.inputmethodcommon;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;
import com.android.common.speech.LoggingEvents;
import java.util.List;

class InputMethodSettingsImpl implements InputMethodSettingsInterface {
    private boolean mIsStartSettings = false;
    private CharSequence mSubtypeEnablerTitle;
    private int mSubtypeEnablerTitleRes;

    InputMethodSettingsImpl() {
    }

    public static InputMethodInfo getMyImi(Context context, InputMethodManager imm) {
        if (imm != null) {
            List<InputMethodInfo> imis = imm.getInputMethodList();
            for (int i = 0; i < imis.size(); i++) {
                InputMethodInfo imi = imis.get(i);
                if (imis.get(i).getPackageName().equals(context.getPackageName())) {
                    return imi;
                }
            }
        }
        return null;
    }

    public static String getEnabledSubtypesLabel(Context context, InputMethodManager imm, InputMethodInfo imi) {
        if (context == null || imm == null || imi == null) {
            return LoggingEvents.EXTRA_CALLING_APP_NAME;
        }
        List<InputMethodSubtype> subtypes = imm.getEnabledInputMethodSubtypeList(imi, true);
        StringBuilder sb = new StringBuilder();
        int N = subtypes.size();
        for (int i = 0; i < N; i++) {
            InputMethodSubtype subtype = subtypes.get(i);
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(subtype.getDisplayName(context, imi.getPackageName(), imi.getServiceInfo().applicationInfo));
        }
        return sb.toString();
    }

    @Override
    public void setInputMethodSettingsCategoryTitle(int resId) {
    }

    @Override
    public void setInputMethodSettingsCategoryTitle(CharSequence title) {
    }

    @Override
    public void setSubtypeEnablerTitle(int resId) {
        this.mSubtypeEnablerTitleRes = resId;
    }

    @Override
    public void setSubtypeEnablerTitle(CharSequence title) {
        this.mSubtypeEnablerTitleRes = 0;
        this.mSubtypeEnablerTitle = title;
    }

    @Override
    public void setSubtypeEnablerIcon(int resId) {
    }

    @Override
    public void setSubtypeEnablerIcon(Drawable drawable) {
    }

    private CharSequence getSubtypeEnablerTitle(Context context) {
        return this.mSubtypeEnablerTitleRes != 0 ? context.getString(this.mSubtypeEnablerTitleRes) : this.mSubtypeEnablerTitle;
    }

    public void startInputMethodSubTypeSettings(Context context, InputMethodInfo imi) {
        if (imi != null) {
            CharSequence title = getSubtypeEnablerTitle(context);
            Intent intent = new Intent("android.settings.INPUT_METHOD_SUBTYPE_SETTINGS");
            intent.putExtra("input_method_id", imi.getId());
            if (!TextUtils.isEmpty(title)) {
                intent.putExtra("android.intent.extra.TITLE", title);
            }
            intent.setFlags(337641472);
            context.startActivity(intent);
            this.mIsStartSettings = true;
        }
    }

    public boolean isStartSettings() {
        return this.mIsStartSettings;
    }

    public void clearIsStartSettings() {
        this.mIsStartSettings = false;
    }
}
