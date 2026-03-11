package com.android.settings.inputmethod;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import com.android.internal.util.Preconditions;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class VirtualKeyboardFragment extends SettingsPreferenceFragment {
    private static final Drawable NO_ICON = new ColorDrawable(0);
    private Preference mAddVirtualKeyboardScreen;
    private DevicePolicyManager mDpm;
    private InputMethodManager mImm;
    private final ArrayList<InputMethodPreference> mInputMethodPreferenceList = new ArrayList<>();

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        Activity activity = (Activity) Preconditions.checkNotNull(getActivity());
        addPreferencesFromResource(R.xml.virtual_keyboard_settings);
        this.mImm = (InputMethodManager) Preconditions.checkNotNull((InputMethodManager) activity.getSystemService(InputMethodManager.class));
        this.mDpm = (DevicePolicyManager) Preconditions.checkNotNull((DevicePolicyManager) activity.getSystemService(DevicePolicyManager.class));
        this.mAddVirtualKeyboardScreen = (Preference) Preconditions.checkNotNull(findPreference("add_virtual_keyboard_screen"));
    }

    @Override
    public void onResume() {
        super.onResume();
        updateInputMethodPreferenceViews();
    }

    @Override
    protected int getMetricsCategory() {
        return 345;
    }

    private void updateInputMethodPreferenceViews() {
        boolean zContains;
        Drawable icon;
        this.mInputMethodPreferenceList.clear();
        List<String> permittedList = this.mDpm.getPermittedInputMethodsForCurrentUser();
        Context context = getPrefContext();
        List<InputMethodInfo> imis = this.mImm.getEnabledInputMethodList();
        int N = imis == null ? 0 : imis.size();
        for (int i = 0; i < N; i++) {
            InputMethodInfo imi = imis.get(i);
            if (permittedList == null) {
                zContains = true;
            } else {
                zContains = permittedList.contains(imi.getPackageName());
            }
            try {
                icon = getActivity().getPackageManager().getApplicationIcon(imi.getPackageName());
            } catch (Exception e) {
                icon = NO_ICON;
            }
            InputMethodPreference pref = new InputMethodPreference(context, imi, false, zContains, null);
            pref.setIcon(icon);
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
        this.mAddVirtualKeyboardScreen.setIcon(R.drawable.ic_add_24dp);
        this.mAddVirtualKeyboardScreen.setOrder(N);
        getPreferenceScreen().addPreference(this.mAddVirtualKeyboardScreen);
    }
}
