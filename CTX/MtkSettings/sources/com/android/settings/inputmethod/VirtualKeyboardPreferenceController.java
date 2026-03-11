package com.android.settings.inputmethod;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v7.preference.Preference;
import android.text.BidiFormatter;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class VirtualKeyboardPreferenceController extends AbstractPreferenceController implements PreferenceControllerMixin {
    private final DevicePolicyManager mDpm;
    private final InputMethodManager mImm;
    private final PackageManager mPm;

    public VirtualKeyboardPreferenceController(Context context) {
        super(context);
        this.mPm = this.mContext.getPackageManager();
        this.mDpm = (DevicePolicyManager) context.getSystemService("device_policy");
        this.mImm = (InputMethodManager) this.mContext.getSystemService("input_method");
    }

    @Override
    public boolean isAvailable() {
        return this.mContext.getResources().getBoolean(R.bool.config_show_virtual_keyboard_pref);
    }

    @Override
    public String getPreferenceKey() {
        return "virtual_keyboard_pref";
    }

    @Override
    public void updateState(Preference preference) {
        List<InputMethodInfo> enabledInputMethodList = this.mImm.getEnabledInputMethodList();
        if (enabledInputMethodList == null) {
            preference.setSummary(R.string.summary_empty);
            return;
        }
        List permittedInputMethodsForCurrentUser = this.mDpm.getPermittedInputMethodsForCurrentUser();
        ArrayList<String> arrayList = new ArrayList();
        Iterator<InputMethodInfo> it = enabledInputMethodList.iterator();
        while (true) {
            boolean z = true;
            if (!it.hasNext()) {
                break;
            }
            InputMethodInfo next = it.next();
            if (permittedInputMethodsForCurrentUser != null && !permittedInputMethodsForCurrentUser.contains(next.getPackageName())) {
                z = false;
            }
            if (z) {
                arrayList.add(next.loadLabel(this.mPm).toString());
            }
        }
        if (arrayList.isEmpty()) {
            preference.setSummary(R.string.summary_empty);
            return;
        }
        BidiFormatter bidiFormatter = BidiFormatter.getInstance();
        String strUnicodeWrap = null;
        for (String str : arrayList) {
            if (strUnicodeWrap == null) {
                strUnicodeWrap = bidiFormatter.unicodeWrap(str);
            } else {
                strUnicodeWrap = this.mContext.getString(R.string.join_many_items_middle, strUnicodeWrap, bidiFormatter.unicodeWrap(str));
            }
        }
        preference.setSummary(strUnicodeWrap);
    }
}
