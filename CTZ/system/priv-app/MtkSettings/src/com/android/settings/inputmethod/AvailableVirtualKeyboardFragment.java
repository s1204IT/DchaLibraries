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
import android.provider.SearchIndexableResource;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settingslib.inputmethod.InputMethodAndSubtypeUtil;
import com.android.settingslib.inputmethod.InputMethodPreference;
import com.android.settingslib.inputmethod.InputMethodSettingValuesWrapper;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/* loaded from: classes.dex */
public final class AvailableVirtualKeyboardFragment extends SettingsPreferenceFragment implements Indexable, InputMethodPreference.OnSavePreferenceListener {
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() { // from class: com.android.settings.inputmethod.AvailableVirtualKeyboardFragment.1
        @Override // com.android.settings.search.BaseSearchIndexProvider, com.android.settings.search.Indexable.SearchIndexProvider
        public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean z) {
            ArrayList arrayList = new ArrayList();
            SearchIndexableResource searchIndexableResource = new SearchIndexableResource(context);
            searchIndexableResource.xmlResId = R.xml.available_virtual_keyboard;
            arrayList.add(searchIndexableResource);
            return arrayList;
        }
    };
    private DevicePolicyManager mDpm;
    private InputMethodManager mImm;
    private final ArrayList<InputMethodPreference> mInputMethodPreferenceList = new ArrayList<>();
    private InputMethodSettingValuesWrapper mInputMethodSettingValues;

    @Override // com.android.settings.core.InstrumentedPreferenceFragment, android.support.v14.preference.PreferenceFragment
    public void onCreatePreferences(Bundle bundle, String str) {
        addPreferencesFromResource(R.xml.available_virtual_keyboard);
        Activity activity = getActivity();
        this.mInputMethodSettingValues = InputMethodSettingValuesWrapper.getInstance(activity);
        this.mImm = (InputMethodManager) activity.getSystemService(InputMethodManager.class);
        this.mDpm = (DevicePolicyManager) activity.getSystemService(DevicePolicyManager.class);
    }

    @Override // com.android.settings.SettingsPreferenceFragment, com.android.settings.core.InstrumentedPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onResume() {
        super.onResume();
        this.mInputMethodSettingValues.refreshAllInputMethodAndSubtypes();
        updateInputMethodPreferenceViews();
    }

    @Override // com.android.settingslib.inputmethod.InputMethodPreference.OnSavePreferenceListener
    public void onSaveInputMethodPreference(InputMethodPreference inputMethodPreference) {
        InputMethodAndSubtypeUtil.saveInputMethodSubtypeList(this, getContentResolver(), this.mImm.getInputMethodList(), getResources().getConfiguration().keyboard == 2);
        this.mInputMethodSettingValues.refreshAllInputMethodAndSubtypes();
        Iterator<InputMethodPreference> it = this.mInputMethodPreferenceList.iterator();
        while (it.hasNext()) {
            it.next().updatePreferenceViews();
        }
    }

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 347;
    }

    private static Drawable loadDrawable(PackageManager packageManager, String str, int i, ApplicationInfo applicationInfo) {
        if (i == 0) {
            return null;
        }
        try {
            return packageManager.getDrawable(str, i, applicationInfo);
        } catch (Exception e) {
            return null;
        }
    }

    private static Drawable getInputMethodIcon(PackageManager packageManager, InputMethodInfo inputMethodInfo) {
        ServiceInfo serviceInfo = inputMethodInfo.getServiceInfo();
        ApplicationInfo applicationInfo = serviceInfo != null ? serviceInfo.applicationInfo : null;
        String packageName = inputMethodInfo.getPackageName();
        if (serviceInfo == null || applicationInfo == null || packageName == null) {
            return new ColorDrawable(0);
        }
        Drawable drawableLoadDrawable = loadDrawable(packageManager, packageName, serviceInfo.logo, applicationInfo);
        if (drawableLoadDrawable != null) {
            return drawableLoadDrawable;
        }
        Drawable drawableLoadDrawable2 = loadDrawable(packageManager, packageName, serviceInfo.icon, applicationInfo);
        if (drawableLoadDrawable2 != null) {
            return drawableLoadDrawable2;
        }
        Drawable drawableLoadDrawable3 = loadDrawable(packageManager, packageName, applicationInfo.logo, applicationInfo);
        if (drawableLoadDrawable3 != null) {
            return drawableLoadDrawable3;
        }
        Drawable drawableLoadDrawable4 = loadDrawable(packageManager, packageName, applicationInfo.icon, applicationInfo);
        if (drawableLoadDrawable4 != null) {
            return drawableLoadDrawable4;
        }
        return new ColorDrawable(0);
    }

    private void updateInputMethodPreferenceViews() {
        int size;
        this.mInputMethodSettingValues.refreshAllInputMethodAndSubtypes();
        this.mInputMethodPreferenceList.clear();
        List permittedInputMethodsForCurrentUser = this.mDpm.getPermittedInputMethodsForCurrentUser();
        Context prefContext = getPrefContext();
        PackageManager packageManager = getActivity().getPackageManager();
        List<InputMethodInfo> inputMethodList = this.mInputMethodSettingValues.getInputMethodList();
        if (inputMethodList != null) {
            size = inputMethodList.size();
        } else {
            size = 0;
        }
        for (int i = 0; i < size; i++) {
            InputMethodInfo inputMethodInfo = inputMethodList.get(i);
            InputMethodPreference inputMethodPreference = new InputMethodPreference(prefContext, inputMethodInfo, true, permittedInputMethodsForCurrentUser == null || permittedInputMethodsForCurrentUser.contains(inputMethodInfo.getPackageName()), (InputMethodPreference.OnSavePreferenceListener) this);
            inputMethodPreference.setIcon(getInputMethodIcon(packageManager, inputMethodInfo));
            this.mInputMethodPreferenceList.add(inputMethodPreference);
        }
        final Collator collator = Collator.getInstance();
        this.mInputMethodPreferenceList.sort(new Comparator() { // from class: com.android.settings.inputmethod.-$$Lambda$AvailableVirtualKeyboardFragment$jwIjaxSxVSRnK0I3ZX1KVHtd2wk
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
    }
}
