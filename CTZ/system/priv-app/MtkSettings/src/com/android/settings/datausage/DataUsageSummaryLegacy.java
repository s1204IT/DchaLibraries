package com.android.settings.datausage;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkPolicyManager;
import android.net.NetworkTemplate;
import android.os.Bundle;
import android.os.UserManager;
import android.provider.SearchIndexableResource;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.telephony.SubscriptionInfo;
import android.text.BidiFormatter;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.text.style.RelativeSizeSpan;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import com.android.settings.R;
import com.android.settings.SummaryPreference;
import com.android.settings.Utils;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settings.datausage.BillingCycleSettings;
import com.android.settings.datausage.DataUsageSummaryLegacy;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settingslib.NetworkPolicyEditor;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.net.DataUsageController;
import java.util.ArrayList;
import java.util.List;

/* loaded from: classes.dex */
public class DataUsageSummaryLegacy extends DataUsageBaseFragment implements DataUsageEditController, Indexable {
    private DataUsageInfoController mDataInfoController;
    private DataUsageController mDataUsageController;
    private int mDataUsageTemplate;
    private NetworkTemplate mDefaultTemplate;
    private Preference mLimitPreference;
    private NetworkPolicyEditor mPolicyEditor;
    private SummaryPreference mSummaryPreference;
    public static final SummaryLoader.SummaryProviderFactory SUMMARY_PROVIDER_FACTORY = new SummaryLoader.SummaryProviderFactory() { // from class: com.android.settings.datausage.-$$Lambda$7QiUIfMd3seAu_emb68cbM9H0Io
        @Override // com.android.settings.dashboard.SummaryLoader.SummaryProviderFactory
        public final SummaryLoader.SummaryProvider createSummaryProvider(Activity activity, SummaryLoader summaryLoader) {
            return new DataUsageSummaryLegacy.SummaryProvider(activity, summaryLoader);
        }
    };
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() { // from class: com.android.settings.datausage.DataUsageSummaryLegacy.1
        @Override // com.android.settings.search.BaseSearchIndexProvider, com.android.settings.search.Indexable.SearchIndexProvider
        public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean z) {
            ArrayList arrayList = new ArrayList();
            SearchIndexableResource searchIndexableResource = new SearchIndexableResource(context);
            searchIndexableResource.xmlResId = R.xml.data_usage_legacy;
            arrayList.add(searchIndexableResource);
            SearchIndexableResource searchIndexableResource2 = new SearchIndexableResource(context);
            searchIndexableResource2.xmlResId = R.xml.data_usage_cellular;
            arrayList.add(searchIndexableResource2);
            SearchIndexableResource searchIndexableResource3 = new SearchIndexableResource(context);
            searchIndexableResource3.xmlResId = R.xml.data_usage_wifi;
            arrayList.add(searchIndexableResource3);
            return arrayList;
        }

        @Override // com.android.settings.search.BaseSearchIndexProvider, com.android.settings.search.Indexable.SearchIndexProvider
        public List<String> getNonIndexableKeys(Context context) {
            List<String> nonIndexableKeys = super.getNonIndexableKeys(context);
            if (!DataUsageUtils.hasMobileData(context)) {
                nonIndexableKeys.add("mobile_category");
                nonIndexableKeys.add("data_usage_enable");
                nonIndexableKeys.add("cellular_data_usage");
                nonIndexableKeys.add("billing_preference");
            }
            if (!DataUsageUtils.hasWifiRadio(context)) {
                nonIndexableKeys.add("wifi_data_usage");
            }
            nonIndexableKeys.add("wifi_category");
            return nonIndexableKeys;
        }
    };

    @Override // com.android.settings.support.actionbar.HelpResourceProvider
    public int getHelpResource() {
        return R.string.help_url_data_usage;
    }

    @Override // com.android.settings.datausage.DataUsageBaseFragment, com.android.settings.dashboard.DashboardFragment, com.android.settings.SettingsPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onCreate(Bundle bundle) {
        int i;
        super.onCreate(bundle);
        Context context = getContext();
        this.mPolicyEditor = new NetworkPolicyEditor(NetworkPolicyManager.from(context));
        boolean zHasMobileData = DataUsageUtils.hasMobileData(context);
        this.mDataUsageController = new DataUsageController(context);
        this.mDataInfoController = new DataUsageInfoController();
        int defaultSubscriptionId = DataUsageUtils.getDefaultSubscriptionId(context);
        if (defaultSubscriptionId == -1) {
            zHasMobileData = false;
        }
        this.mDefaultTemplate = DataUsageUtils.getDefaultTemplate(context, defaultSubscriptionId);
        this.mSummaryPreference = (SummaryPreference) findPreference("status_header");
        if (!zHasMobileData || !isAdmin()) {
            removePreference("restrict_background_legacy");
        }
        if (zHasMobileData) {
            this.mLimitPreference = findPreference("limit_summary");
            List<SubscriptionInfo> activeSubscriptionInfoList = this.services.mSubscriptionManager.getActiveSubscriptionInfoList();
            if (activeSubscriptionInfoList == null || activeSubscriptionInfoList.size() == 0) {
                addMobileSection(defaultSubscriptionId);
            }
            for (int i2 = 0; activeSubscriptionInfoList != null && i2 < activeSubscriptionInfoList.size(); i2++) {
                SubscriptionInfo subscriptionInfo = activeSubscriptionInfoList.get(i2);
                if (activeSubscriptionInfoList.size() > 1) {
                    addMobileSection(subscriptionInfo.getSubscriptionId(), subscriptionInfo);
                } else {
                    addMobileSection(subscriptionInfo.getSubscriptionId());
                }
            }
            this.mSummaryPreference.setSelectable(true);
        } else {
            removePreference("limit_summary");
            this.mSummaryPreference.setSelectable(false);
        }
        boolean zHasWifiRadio = DataUsageUtils.hasWifiRadio(context);
        if (zHasWifiRadio) {
            addWifiSection();
        }
        if (hasEthernet(context)) {
            addEthernetSection();
        }
        if (zHasMobileData) {
            i = R.string.cell_data_template;
        } else {
            i = zHasWifiRadio ? R.string.wifi_data_template : R.string.ethernet_data_template;
        }
        this.mDataUsageTemplate = i;
        setHasOptionsMenu(true);
    }

    @Override // com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        if (UserManager.get(getContext()).isAdminUser()) {
            menuInflater.inflate(R.menu.data_usage, menu);
        }
        menu.removeItem(R.id.data_usage_menu_cellular_networks);
        menu.removeItem(R.id.data_usage_menu_cellular_data_control);
        super.onCreateOptionsMenu(menu, menuInflater);
    }

    @Override // com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == R.id.data_usage_menu_cellular_networks) {
            Intent intent = new Intent("android.intent.action.MAIN");
            intent.setComponent(new ComponentName("com.android.phone", "com.android.phone.MobileNetworkSettings"));
            startActivity(intent);
            return true;
        }
        return false;
    }

    @Override // com.android.settings.dashboard.DashboardFragment, android.support.v14.preference.PreferenceFragment, android.support.v7.preference.PreferenceManager.OnPreferenceTreeClickListener
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference == findPreference("status_header")) {
            BillingCycleSettings.BytesEditorFragment.show((DataUsageEditController) this, false);
            return false;
        }
        return super.onPreferenceTreeClick(preference);
    }

    @Override // com.android.settings.dashboard.DashboardFragment, com.android.settings.core.InstrumentedPreferenceFragment
    protected int getPreferenceScreenResId() {
        return R.xml.data_usage_legacy;
    }

    @Override // com.android.settings.dashboard.DashboardFragment
    protected String getLogTag() {
        return "DataUsageSummaryLegacy";
    }

    @Override // com.android.settings.dashboard.DashboardFragment
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return null;
    }

    private void addMobileSection(int i) {
        addMobileSection(i, null);
    }

    private void addMobileSection(int i, SubscriptionInfo subscriptionInfo) {
        TemplatePreferenceCategory templatePreferenceCategory = (TemplatePreferenceCategory) inflatePreferences(R.xml.data_usage_cellular);
        templatePreferenceCategory.setTemplate(getNetworkTemplate(i), i, this.services);
        templatePreferenceCategory.pushTemplates(this.services);
        if (subscriptionInfo != null && !TextUtils.isEmpty(subscriptionInfo.getDisplayName())) {
            templatePreferenceCategory.findPreference("mobile_category").setTitle(subscriptionInfo.getDisplayName());
        }
    }

    private void addWifiSection() {
        ((TemplatePreferenceCategory) inflatePreferences(R.xml.data_usage_wifi)).setTemplate(NetworkTemplate.buildTemplateWifiWildcard(), 0, this.services);
    }

    private void addEthernetSection() {
        ((TemplatePreferenceCategory) inflatePreferences(R.xml.data_usage_ethernet)).setTemplate(NetworkTemplate.buildTemplateEthernet(), 0, this.services);
    }

    private Preference inflatePreferences(int i) {
        PreferenceScreen preferenceScreenInflateFromResource = getPreferenceManager().inflateFromResource(getPrefContext(), i, null);
        Preference preference = preferenceScreenInflateFromResource.getPreference(0);
        preferenceScreenInflateFromResource.removeAll();
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        preference.setOrder(preferenceScreen.getPreferenceCount());
        preferenceScreen.addPreference(preference);
        return preference;
    }

    private NetworkTemplate getNetworkTemplate(int i) {
        return NetworkTemplate.normalize(NetworkTemplate.buildTemplateMobileAll(this.services.mTelephonyManager.getSubscriberId(i)), this.services.mTelephonyManager.getMergedSubscriberIds());
    }

    @Override // com.android.settings.datausage.DataUsageBaseFragment, com.android.settings.dashboard.DashboardFragment, com.android.settings.SettingsPreferenceFragment, com.android.settings.core.InstrumentedPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onResume() {
        super.onResume();
        updateState();
    }

    static CharSequence formatUsage(Context context, String str, long j) {
        Formatter.BytesResult bytes = Formatter.formatBytes(context.getResources(), j, 2);
        SpannableString spannableString = new SpannableString(bytes.value);
        spannableString.setSpan(new RelativeSizeSpan(1.5625f), 0, spannableString.length(), 18);
        CharSequence charSequenceExpandTemplate = TextUtils.expandTemplate(new SpannableString(context.getString(android.R.string.config_defaultAssistantAccessComponent).replace("%1$s", "^1").replace("%2$s", "^2")), spannableString, bytes.units);
        SpannableString spannableString2 = new SpannableString(str);
        spannableString2.setSpan(new RelativeSizeSpan(0.64f), 0, spannableString2.length(), 18);
        return TextUtils.expandTemplate(spannableString2, BidiFormatter.getInstance().unicodeWrap(charSequenceExpandTemplate.toString()));
    }

    private void updateState() {
        DataUsageController.DataUsageInfo dataUsageInfo = this.mDataUsageController.getDataUsageInfo(this.mDefaultTemplate);
        Context context = getContext();
        this.mDataInfoController.updateDataLimit(dataUsageInfo, this.services.mPolicyEditor.getPolicy(this.mDefaultTemplate));
        if (this.mSummaryPreference != null) {
            this.mSummaryPreference.setTitle(formatUsage(context, getString(this.mDataUsageTemplate), dataUsageInfo.usageLevel));
            long summaryLimit = this.mDataInfoController.getSummaryLimit(dataUsageInfo);
            this.mSummaryPreference.setSummary(dataUsageInfo.period);
            if (summaryLimit <= 0) {
                this.mSummaryPreference.setChartEnabled(false);
            } else {
                this.mSummaryPreference.setChartEnabled(true);
                this.mSummaryPreference.setLabels(Formatter.formatFileSize(context, 0L), Formatter.formatFileSize(context, summaryLimit));
                float f = summaryLimit;
                this.mSummaryPreference.setRatios(dataUsageInfo.usageLevel / f, 0.0f, (summaryLimit - dataUsageInfo.usageLevel) / f);
            }
        }
        if (this.mLimitPreference != null && (dataUsageInfo.warningLevel > 0 || dataUsageInfo.limitLevel > 0)) {
            this.mLimitPreference.setSummary(getString(dataUsageInfo.limitLevel <= 0 ? R.string.cell_warning_only : R.string.cell_warning_and_limit, new Object[]{Formatter.formatFileSize(context, dataUsageInfo.warningLevel), Formatter.formatFileSize(context, dataUsageInfo.limitLevel)}));
        } else if (this.mLimitPreference != null) {
            this.mLimitPreference.setSummary((CharSequence) null);
        }
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        for (int i = 1; i < preferenceScreen.getPreferenceCount(); i++) {
            ((TemplatePreferenceCategory) preferenceScreen.getPreference(i)).pushTemplates(this.services);
        }
    }

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 37;
    }

    @Override // com.android.settings.datausage.DataUsageEditController
    public NetworkPolicyEditor getNetworkPolicyEditor() {
        return this.services.mPolicyEditor;
    }

    @Override // com.android.settings.datausage.DataUsageEditController
    public NetworkTemplate getNetworkTemplate() {
        return this.mDefaultTemplate;
    }

    @Override // com.android.settings.datausage.DataUsageEditController
    public void updateDataUsage() {
        updateState();
    }

    private static class SummaryProvider implements SummaryLoader.SummaryProvider {
        private final Activity mActivity;
        private final DataUsageController mDataController;
        private final SummaryLoader mSummaryLoader;

        public SummaryProvider(Activity activity, SummaryLoader summaryLoader) {
            this.mActivity = activity;
            this.mSummaryLoader = summaryLoader;
            this.mDataController = new DataUsageController(activity);
        }

        @Override // com.android.settings.dashboard.SummaryLoader.SummaryProvider
        public void setListening(boolean z) {
            String percentage;
            if (z) {
                DataUsageController.DataUsageInfo dataUsageInfo = this.mDataController.getDataUsageInfo();
                if (dataUsageInfo == null) {
                    percentage = Formatter.formatFileSize(this.mActivity, 0L);
                } else if (dataUsageInfo.limitLevel <= 0) {
                    percentage = Formatter.formatFileSize(this.mActivity, dataUsageInfo.usageLevel);
                } else {
                    percentage = Utils.formatPercentage(dataUsageInfo.usageLevel, dataUsageInfo.limitLevel);
                }
                this.mSummaryLoader.setSummary(this, this.mActivity.getString(R.string.data_usage_summary_format, new Object[]{percentage}));
            }
        }
    }
}
