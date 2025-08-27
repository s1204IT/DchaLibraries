package com.android.settings.inputmethod;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.SearchIndexableResource;
import android.support.v7.preference.Preference;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import com.android.internal.util.Preconditions;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settingslib.inputmethod.InputMethodAndSubtypeUtil;
import com.android.settingslib.inputmethod.InputMethodPreference;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/* loaded from: classes.dex */
public final class VirtualKeyboardFragment extends SettingsPreferenceFragment implements Indexable {
    private static final Drawable NO_ICON = new ColorDrawable(0);
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() { // from class: com.android.settings.inputmethod.VirtualKeyboardFragment.1
        @Override // com.android.settings.search.BaseSearchIndexProvider, com.android.settings.search.Indexable.SearchIndexProvider
        public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean z) {
            SearchIndexableResource searchIndexableResource = new SearchIndexableResource(context);
            searchIndexableResource.xmlResId = R.xml.virtual_keyboard_settings;
            return Arrays.asList(searchIndexableResource);
        }

        @Override // com.android.settings.search.BaseSearchIndexProvider, com.android.settings.search.Indexable.SearchIndexProvider
        public List<String> getNonIndexableKeys(Context context) {
            List<String> nonIndexableKeys = super.getNonIndexableKeys(context);
            nonIndexableKeys.add("add_virtual_keyboard_screen");
            return nonIndexableKeys;
        }
    };
    private Preference mAddVirtualKeyboardScreen;
    private DevicePolicyManager mDpm;
    private InputMethodManager mImm;
    private final ArrayList<InputMethodPreference> mInputMethodPreferenceList = new ArrayList<>();

    @Override // com.android.settings.core.InstrumentedPreferenceFragment, android.support.v14.preference.PreferenceFragment
    public void onCreatePreferences(Bundle bundle, String str) {
        Activity activity = (Activity) Preconditions.checkNotNull(getActivity());
        addPreferencesFromResource(R.xml.virtual_keyboard_settings);
        this.mImm = (InputMethodManager) Preconditions.checkNotNull((InputMethodManager) activity.getSystemService(InputMethodManager.class));
        this.mDpm = (DevicePolicyManager) Preconditions.checkNotNull((DevicePolicyManager) activity.getSystemService(DevicePolicyManager.class));
        this.mAddVirtualKeyboardScreen = (Preference) Preconditions.checkNotNull(findPreference("add_virtual_keyboard_screen"));
    }

    @Override // com.android.settings.SettingsPreferenceFragment, com.android.settings.core.InstrumentedPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onResume() throws PackageManager.NameNotFoundException {
        super.onResume();
        updateInputMethodPreferenceViews();
    }

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 345;
    }

    private void updateInputMethodPreferenceViews() throws PackageManager.NameNotFoundException {
        int size;
        Drawable applicationIcon;
        this.mInputMethodPreferenceList.clear();
        List permittedInputMethodsForCurrentUser = this.mDpm.getPermittedInputMethodsForCurrentUser();
        Context prefContext = getPrefContext();
        List<InputMethodInfo> enabledInputMethodList = this.mImm.getEnabledInputMethodList();
        if (enabledInputMethodList != null) {
            size = enabledInputMethodList.size();
        } else {
            size = 0;
        }
        for (int i = 0; i < size; i++) {
            InputMethodInfo inputMethodInfo = enabledInputMethodList.get(i);
            boolean z = permittedInputMethodsForCurrentUser == null || permittedInputMethodsForCurrentUser.contains(inputMethodInfo.getPackageName());
            try {
                applicationIcon = getActivity().getPackageManager().getApplicationIcon(inputMethodInfo.getPackageName());
            } catch (Exception e) {
                applicationIcon = NO_ICON;
            }
            InputMethodPreference inputMethodPreference = new InputMethodPreference(prefContext, inputMethodInfo, false, z, (InputMethodPreference.OnSavePreferenceListener) null);
            inputMethodPreference.setIcon(applicationIcon);
            this.mInputMethodPreferenceList.add(inputMethodPreference);
        }
        final Collator collator = Collator.getInstance();
        this.mInputMethodPreferenceList.sort(new Comparator() { // from class: com.android.settings.inputmethod.-$$Lambda$VirtualKeyboardFragment$3eczHKaadmVH3sZXf9rlrdYqLjw
            @Override // java.util.Comparator
            public final int compare(Object obj, Object obj2) {
                return ((InputMethodPreference) obj).compareTo((InputMethodPreference) obj2, collator);
            }
        });
        getPreferenceScreen().removeAll();
        for (int i2 = 0; i2 < size; i2++) {
            InputMethodPreference inputMethodPreference2 = this.mInputMethodPreferenceList.get(i2);
            inputMethodPreference2.setOrder(i2);
            getPreferenceScreen().addPreference(inputMethodPreference2);
            InputMethodAndSubtypeUtil.removeUnnecessaryNonPersistentPreference(inputMethodPreference2);
            inputMethodPreference2.updatePreferenceViews();
        }
        this.mAddVirtualKeyboardScreen.setIcon(R.drawable.ic_add_24dp);
        this.mAddVirtualKeyboardScreen.setOrder(size);
        getPreferenceScreen().addPreference(this.mAddVirtualKeyboardScreen);
    }
}
