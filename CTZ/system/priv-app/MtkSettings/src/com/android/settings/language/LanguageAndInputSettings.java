package com.android.settings.language;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.speech.tts.TtsEngines;
import android.text.TextUtils;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import com.android.settings.R;
import com.android.settings.applications.defaultapps.DefaultAutofillPreferenceController;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settings.inputmethod.PhysicalKeyboardPreferenceController;
import com.android.settings.inputmethod.SpellCheckerPreferenceController;
import com.android.settings.inputmethod.VirtualKeyboardPreferenceController;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.widget.PreferenceCategoryController;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.mediatek.settings.inputmethod.VoiceWakeupPreferenceController;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/* loaded from: classes.dex */
public class LanguageAndInputSettings extends DashboardFragment {
    public static final SummaryLoader.SummaryProviderFactory SUMMARY_PROVIDER_FACTORY = new SummaryLoader.SummaryProviderFactory() { // from class: com.android.settings.language.-$$Lambda$LanguageAndInputSettings$VvwbgRiPWoRSuoMu5QPyPqZ5AEc
        @Override // com.android.settings.dashboard.SummaryLoader.SummaryProviderFactory
        public final SummaryLoader.SummaryProvider createSummaryProvider(Activity activity, SummaryLoader summaryLoader) {
            return LanguageAndInputSettings.lambda$static$0(activity, summaryLoader);
        }
    };
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() { // from class: com.android.settings.language.LanguageAndInputSettings.1
        @Override // com.android.settings.search.BaseSearchIndexProvider, com.android.settings.search.Indexable.SearchIndexProvider
        public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean z) {
            SearchIndexableResource searchIndexableResource = new SearchIndexableResource(context);
            searchIndexableResource.xmlResId = R.xml.language_and_input;
            return Arrays.asList(searchIndexableResource);
        }

        @Override // com.android.settings.search.BaseSearchIndexProvider
        public List<AbstractPreferenceController> createPreferenceControllers(Context context) {
            return LanguageAndInputSettings.buildPreferenceControllers(context, null);
        }

        @Override // com.android.settings.search.BaseSearchIndexProvider, com.android.settings.search.Indexable.SearchIndexProvider
        public List<String> getNonIndexableKeys(Context context) {
            List<String> nonIndexableKeys = super.getNonIndexableKeys(context);
            nonIndexableKeys.add("tts_settings_summary");
            nonIndexableKeys.add("physical_keyboard_pref");
            return nonIndexableKeys;
        }
    };

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 750;
    }

    @Override // com.android.settings.dashboard.DashboardFragment
    protected String getLogTag() {
        return "LangAndInputSettings";
    }

    @Override // com.android.settings.dashboard.DashboardFragment, com.android.settings.SettingsPreferenceFragment, com.android.settings.core.InstrumentedPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onResume() {
        super.onResume();
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        activity.setTitle(R.string.language_settings);
    }

    @Override // com.android.settings.dashboard.DashboardFragment, com.android.settings.core.InstrumentedPreferenceFragment
    protected int getPreferenceScreenResId() {
        return R.xml.language_and_input;
    }

    @Override // com.android.settings.dashboard.DashboardFragment
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return buildPreferenceControllers(context, getLifecycle());
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(Context context, Lifecycle lifecycle) {
        ArrayList arrayList = new ArrayList();
        arrayList.add(new PhoneLanguagePreferenceController(context));
        VirtualKeyboardPreferenceController virtualKeyboardPreferenceController = new VirtualKeyboardPreferenceController(context);
        PhysicalKeyboardPreferenceController physicalKeyboardPreferenceController = new PhysicalKeyboardPreferenceController(context, lifecycle);
        arrayList.add(virtualKeyboardPreferenceController);
        arrayList.add(physicalKeyboardPreferenceController);
        arrayList.add(new PreferenceCategoryController(context, "keyboards_category").setChildren(Arrays.asList(virtualKeyboardPreferenceController, physicalKeyboardPreferenceController)));
        TtsPreferenceController ttsPreferenceController = new TtsPreferenceController(context, new TtsEngines(context));
        arrayList.add(ttsPreferenceController);
        PointerSpeedController pointerSpeedController = new PointerSpeedController(context);
        arrayList.add(pointerSpeedController);
        arrayList.add(new PreferenceCategoryController(context, "pointer_and_tts_category").setChildren(Arrays.asList(pointerSpeedController, ttsPreferenceController)));
        arrayList.add(new SpellCheckerPreferenceController(context));
        arrayList.add(new DefaultAutofillPreferenceController(context));
        arrayList.add(new UserDictionaryPreferenceController(context));
        arrayList.add(new VoiceWakeupPreferenceController(context));
        return arrayList;
    }

    private static class SummaryProvider implements SummaryLoader.SummaryProvider {
        private final Context mContext;
        private final SummaryLoader mSummaryLoader;

        public SummaryProvider(Context context, SummaryLoader summaryLoader) {
            this.mContext = context;
            this.mSummaryLoader = summaryLoader;
        }

        @Override // com.android.settings.dashboard.SummaryLoader.SummaryProvider
        public void setListening(boolean z) {
            ContentResolver contentResolver = this.mContext.getContentResolver();
            if (z) {
                String string = Settings.Secure.getString(contentResolver, "default_input_method");
                if (!TextUtils.isEmpty(string)) {
                    PackageManager packageManager = this.mContext.getPackageManager();
                    String packageName = ComponentName.unflattenFromString(string).getPackageName();
                    for (InputMethodInfo inputMethodInfo : ((InputMethodManager) this.mContext.getSystemService("input_method")).getInputMethodList()) {
                        if (TextUtils.equals(inputMethodInfo.getPackageName(), packageName)) {
                            this.mSummaryLoader.setSummary(this, inputMethodInfo.loadLabel(packageManager));
                            return;
                        }
                    }
                }
                this.mSummaryLoader.setSummary(this, "");
            }
        }
    }

    /* JADX DEBUG: Can't inline method, not implemented redirect type for insn: 0x0002: CONSTRUCTOR (r1v0 android.app.Activity), (r2v0 com.android.settings.dashboard.SummaryLoader) A[MD:(android.content.Context, com.android.settings.dashboard.SummaryLoader):void (m)] (LINE:169) call: com.android.settings.language.LanguageAndInputSettings.SummaryProvider.<init>(android.content.Context, com.android.settings.dashboard.SummaryLoader):void type: CONSTRUCTOR */
    static /* synthetic */ SummaryLoader.SummaryProvider lambda$static$0(Activity activity, SummaryLoader summaryLoader) {
        return new SummaryProvider(activity, summaryLoader);
    }
}
