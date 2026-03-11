package com.android.settings.inputmethod;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.preference.PreferenceScreen;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.inputmethod.InputMethodPreference;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class AvailableVirtualKeyboardFragment extends SettingsPreferenceFragment implements InputMethodPreference.OnSavePreferenceListener {
    private DevicePolicyManager mDpm;
    private InputMethodManager mImm;
    private final ArrayList<InputMethodPreference> mInputMethodPreferenceList = new ArrayList<>();
    private InputMethodSettingValuesWrapper mInputMethodSettingValues;

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        Activity activity = getActivity();
        PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(activity);
        screen.setTitle(activity.getString(R.string.available_virtual_keyboard_category));
        setPreferenceScreen(screen);
        this.mInputMethodSettingValues = InputMethodSettingValuesWrapper.getInstance(activity);
        this.mImm = (InputMethodManager) activity.getSystemService(InputMethodManager.class);
        this.mDpm = (DevicePolicyManager) activity.getSystemService(DevicePolicyManager.class);
    }

    @Override
    public void onResume() {
        super.onResume();
        this.mInputMethodSettingValues.refreshAllInputMethodAndSubtypes();
        updateInputMethodPreferenceViews();
    }

    @Override
    public void onSaveInputMethodPreference(InputMethodPreference pref) {
        boolean hasHardwareKeyboard = getResources().getConfiguration().keyboard == 2;
        InputMethodAndSubtypeUtil.saveInputMethodSubtypeList(this, getContentResolver(), this.mImm.getInputMethodList(), hasHardwareKeyboard);
        this.mInputMethodSettingValues.refreshAllInputMethodAndSubtypes();
        for (InputMethodPreference p : this.mInputMethodPreferenceList) {
            p.updatePreferenceViews();
        }
    }

    @Override
    protected int getMetricsCategory() {
        return 347;
    }

    private static Drawable loadDrawable(PackageManager packageManager, String packageName, int resId, ApplicationInfo applicationInfo) {
        if (resId == 0) {
            return null;
        }
        try {
            return packageManager.getDrawable(packageName, resId, applicationInfo);
        } catch (Exception e) {
            return null;
        }
    }

    private static Drawable getInputMethodIcon(PackageManager packageManager, InputMethodInfo imi) {
        ServiceInfo si = imi.getServiceInfo();
        ApplicationInfo ai = si.applicationInfo;
        String packageName = imi.getPackageName();
        if (si == null || ai == null || packageName == null) {
            return new ColorDrawable(0);
        }
        Drawable drawable = loadDrawable(packageManager, packageName, si.logo, ai);
        if (drawable != null) {
            return drawable;
        }
        Drawable drawable2 = loadDrawable(packageManager, packageName, si.icon, ai);
        if (drawable2 != null) {
            return drawable2;
        }
        Drawable drawable3 = loadDrawable(packageManager, packageName, ai.logo, ai);
        if (drawable3 != null) {
            return drawable3;
        }
        Drawable drawable4 = loadDrawable(packageManager, packageName, ai.icon, ai);
        if (drawable4 != null) {
            return drawable4;
        }
        return new ColorDrawable(0);
    }

    private void updateInputMethodPreferenceViews() {
        boolean zContains;
        this.mInputMethodSettingValues.refreshAllInputMethodAndSubtypes();
        this.mInputMethodPreferenceList.clear();
        List<String> permittedList = this.mDpm.getPermittedInputMethodsForCurrentUser();
        Context context = getPrefContext();
        PackageManager packageManager = getActivity().getPackageManager();
        List<InputMethodInfo> imis = this.mInputMethodSettingValues.getInputMethodList();
        int N = imis == null ? 0 : imis.size();
        for (int i = 0; i < N; i++) {
            InputMethodInfo imi = imis.get(i);
            if (permittedList == null) {
                zContains = true;
            } else {
                zContains = permittedList.contains(imi.getPackageName());
            }
            InputMethodPreference pref = new InputMethodPreference(context, imi, true, zContains, this);
            pref.setIcon(getInputMethodIcon(packageManager, imi));
            this.mInputMethodPreferenceList.add(pref);
        }
        final Collator collator = Collator.getInstance();
        Collections.sort(this.mInputMethodPreferenceList, new Comparator<InputMethodPreference>() {
            @Override
            public int compare(InputMethodPreference lhs, InputMethodPreference rhs) {
                return lhs.compareTo(rhs, collator);
            }
        });
        getPreferenceScreen().removeAll();
        for (int i2 = 0; i2 < N; i2++) {
            InputMethodPreference pref2 = this.mInputMethodPreferenceList.get(i2);
            pref2.setOrder(i2);
            getPreferenceScreen().addPreference(pref2);
            InputMethodAndSubtypeUtil.removeUnnecessaryNonPersistentPreference(pref2);
            pref2.updatePreferenceViews();
        }
    }
}
