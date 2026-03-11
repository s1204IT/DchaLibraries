package com.android.settings.applications;

import android.app.Activity;
import android.content.Context;
import android.provider.SearchIndexableResource;
import android.text.TextUtils;
import com.android.settings.R;
import com.android.settings.applications.assist.DefaultAssistPreferenceController;
import com.android.settings.applications.defaultapps.DefaultBrowserPreferenceController;
import com.android.settings.applications.defaultapps.DefaultEmergencyPreferenceController;
import com.android.settings.applications.defaultapps.DefaultHomePreferenceController;
import com.android.settings.applications.defaultapps.DefaultPaymentSettingsPreferenceController;
import com.android.settings.applications.defaultapps.DefaultPhonePreferenceController;
import com.android.settings.applications.defaultapps.DefaultSmsPreferenceController;
import com.android.settings.applications.defaultapps.DefaultWorkBrowserPreferenceController;
import com.android.settings.applications.defaultapps.DefaultWorkPhonePreferenceController;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.widget.PreferenceCategoryController;
import com.android.settingslib.core.AbstractPreferenceController;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DefaultAppSettings extends DashboardFragment {
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() {
        @Override
        public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean z) {
            SearchIndexableResource searchIndexableResource = new SearchIndexableResource(context);
            searchIndexableResource.xmlResId = R.xml.app_default_settings;
            return Arrays.asList(searchIndexableResource);
        }

        @Override
        public List<String> getNonIndexableKeys(Context context) {
            List<String> nonIndexableKeys = super.getNonIndexableKeys(context);
            nonIndexableKeys.add("assist_and_voice_input");
            nonIndexableKeys.add("work_default_phone_app");
            nonIndexableKeys.add("work_default_browser");
            return nonIndexableKeys;
        }

        @Override
        public List<AbstractPreferenceController> createPreferenceControllers(Context context) {
            return DefaultAppSettings.buildPreferenceControllers(context);
        }
    };
    public static final SummaryLoader.SummaryProviderFactory SUMMARY_PROVIDER_FACTORY = new SummaryLoader.SummaryProviderFactory() {
        @Override
        public SummaryLoader.SummaryProvider createSummaryProvider(Activity activity, SummaryLoader summaryLoader) {
            return new SummaryProvider(activity, summaryLoader);
        }
    };

    @Override
    protected String getLogTag() {
        return "DefaultAppSettings";
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.app_default_settings;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return buildPreferenceControllers(context);
    }

    @Override
    public int getMetricsCategory() {
        return 130;
    }

    public static List<AbstractPreferenceController> buildPreferenceControllers(Context context) {
        ArrayList arrayList = new ArrayList();
        ArrayList arrayList2 = new ArrayList();
        arrayList2.add(new DefaultWorkPhonePreferenceController(context));
        arrayList2.add(new DefaultWorkBrowserPreferenceController(context));
        arrayList.addAll(arrayList2);
        arrayList.add(new PreferenceCategoryController(context, "work_app_defaults").setChildren(arrayList2));
        arrayList.add(new DefaultAssistPreferenceController(context, "assist_and_voice_input", false));
        arrayList.add(new DefaultBrowserPreferenceController(context));
        arrayList.add(new DefaultPhonePreferenceController(context));
        arrayList.add(new DefaultSmsPreferenceController(context));
        arrayList.add(new DefaultEmergencyPreferenceController(context));
        arrayList.add(new DefaultHomePreferenceController(context));
        arrayList.add(new DefaultPaymentSettingsPreferenceController(context));
        return arrayList;
    }

    static class SummaryProvider implements SummaryLoader.SummaryProvider {
        private final Context mContext;
        private final DefaultBrowserPreferenceController mDefaultBrowserPreferenceController;
        private final DefaultPhonePreferenceController mDefaultPhonePreferenceController;
        private final DefaultSmsPreferenceController mDefaultSmsPreferenceController;
        private final SummaryLoader mSummaryLoader;

        public SummaryProvider(Context context, SummaryLoader summaryLoader) {
            this.mContext = context;
            this.mSummaryLoader = summaryLoader;
            this.mDefaultSmsPreferenceController = new DefaultSmsPreferenceController(this.mContext);
            this.mDefaultBrowserPreferenceController = new DefaultBrowserPreferenceController(this.mContext);
            this.mDefaultPhonePreferenceController = new DefaultPhonePreferenceController(this.mContext);
        }

        @Override
        public void setListening(boolean z) {
            if (!z) {
                return;
            }
            CharSequence charSequenceConcatSummaryText = concatSummaryText(concatSummaryText(this.mDefaultBrowserPreferenceController.getDefaultAppLabel(), this.mDefaultPhonePreferenceController.getDefaultAppLabel()), this.mDefaultSmsPreferenceController.getDefaultAppLabel());
            if (!TextUtils.isEmpty(charSequenceConcatSummaryText)) {
                this.mSummaryLoader.setSummary(this, charSequenceConcatSummaryText);
            }
        }

        private CharSequence concatSummaryText(CharSequence charSequence, CharSequence charSequence2) {
            if (TextUtils.isEmpty(charSequence)) {
                return charSequence2;
            }
            return TextUtils.isEmpty(charSequence2) ? charSequence : this.mContext.getString(R.string.join_many_items_middle, charSequence, charSequence2);
        }
    }
}
