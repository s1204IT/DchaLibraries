package com.android.settings.security;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.fingerprint.FingerprintManager;
import android.provider.SearchIndexableResource;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settings.enterprise.EnterprisePrivacyPreferenceController;
import com.android.settings.enterprise.ManageDeviceAdminPreferenceController;
import com.android.settings.fingerprint.FingerprintProfileStatusPreferenceController;
import com.android.settings.fingerprint.FingerprintStatusPreferenceController;
import com.android.settings.location.LocationPreferenceController;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.security.screenlock.LockScreenPreferenceController;
import com.android.settings.security.trustagent.ManageTrustAgentsPreferenceController;
import com.android.settings.security.trustagent.TrustAgentListPreferenceController;
import com.android.settings.widget.PreferenceCategoryController;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.mediatek.settings.security.AutoBootManagementPreferenceController;
import com.mediatek.settings.security.DataprotectionPreferenceController;
import com.mediatek.settings.security.PermissionControlPreferenceController;
import com.mediatek.settings.security.PplPreferenceController;
import java.util.ArrayList;
import java.util.List;

/* loaded from: classes.dex */
public class SecuritySettings extends DashboardFragment {
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() { // from class: com.android.settings.security.SecuritySettings.1
        @Override // com.android.settings.search.BaseSearchIndexProvider, com.android.settings.search.Indexable.SearchIndexProvider
        public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean z) {
            ArrayList arrayList = new ArrayList();
            SearchIndexableResource searchIndexableResource = new SearchIndexableResource(context);
            searchIndexableResource.xmlResId = R.xml.security_dashboard_settings;
            arrayList.add(searchIndexableResource);
            return arrayList;
        }

        @Override // com.android.settings.search.BaseSearchIndexProvider
        public List<AbstractPreferenceController> createPreferenceControllers(Context context) {
            return SecuritySettings.buildPreferenceControllers(context, null, null);
        }
    };
    public static final SummaryLoader.SummaryProviderFactory SUMMARY_PROVIDER_FACTORY = new SummaryLoader.SummaryProviderFactory() { // from class: com.android.settings.security.SecuritySettings.2
        @Override // com.android.settings.dashboard.SummaryLoader.SummaryProviderFactory
        public SummaryLoader.SummaryProvider createSummaryProvider(Activity activity, SummaryLoader summaryLoader) {
            return new SummaryProvider(activity, summaryLoader);
        }
    };

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 87;
    }

    @Override // com.android.settings.dashboard.DashboardFragment, com.android.settings.core.InstrumentedPreferenceFragment
    protected int getPreferenceScreenResId() {
        return R.xml.security_dashboard_settings;
    }

    @Override // com.android.settings.dashboard.DashboardFragment
    protected String getLogTag() {
        return "SecuritySettings";
    }

    @Override // com.android.settings.support.actionbar.HelpResourceProvider
    public int getHelpResource() {
        return R.string.help_url_security;
    }

    @Override // com.android.settings.dashboard.DashboardFragment
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return buildPreferenceControllers(context, getLifecycle(), this);
    }

    @Override // android.app.Fragment
    public void onActivityResult(int i, int i2, Intent intent) {
        if (((TrustAgentListPreferenceController) use(TrustAgentListPreferenceController.class)).handleActivityResult(i, i2) || ((LockUnificationPreferenceController) use(LockUnificationPreferenceController.class)).handleActivityResult(i, i2, intent)) {
            return;
        }
        super.onActivityResult(i, i2, intent);
    }

    void launchConfirmDeviceLockForUnification() {
        ((LockUnificationPreferenceController) use(LockUnificationPreferenceController.class)).launchConfirmDeviceLockForUnification();
    }

    void unifyUncompliantLocks() {
        ((LockUnificationPreferenceController) use(LockUnificationPreferenceController.class)).unifyUncompliantLocks();
    }

    void updateUnificationPreference() {
        ((LockUnificationPreferenceController) use(LockUnificationPreferenceController.class)).updateState(null);
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(Context context, Lifecycle lifecycle, SecuritySettings securitySettings) {
        ArrayList arrayList = new ArrayList();
        arrayList.add(new LocationPreferenceController(context, lifecycle));
        arrayList.add(new ManageDeviceAdminPreferenceController(context));
        arrayList.add(new EnterprisePrivacyPreferenceController(context));
        arrayList.add(new ManageTrustAgentsPreferenceController(context));
        arrayList.add(new ScreenPinningPreferenceController(context));
        arrayList.add(new SimLockPreferenceController(context));
        arrayList.add(new ShowPasswordPreferenceController(context));
        arrayList.add(new EncryptionStatusPreferenceController(context, "encryption_and_credential"));
        arrayList.add(new TrustAgentListPreferenceController(context, securitySettings, lifecycle));
        arrayList.add(new DataprotectionPreferenceController(context));
        arrayList.add(new PplPreferenceController(context, lifecycle));
        arrayList.add(new PermissionControlPreferenceController(context));
        arrayList.add(new AutoBootManagementPreferenceController(context));
        ArrayList arrayList2 = new ArrayList();
        arrayList2.add(new FingerprintStatusPreferenceController(context));
        arrayList2.add(new LockScreenPreferenceController(context, lifecycle));
        arrayList2.add(new ChangeScreenLockPreferenceController(context, securitySettings));
        arrayList.add(new PreferenceCategoryController(context, "security_category").setChildren(arrayList2));
        arrayList.addAll(arrayList2);
        ArrayList arrayList3 = new ArrayList();
        arrayList3.add(new ChangeProfileScreenLockPreferenceController(context, securitySettings));
        arrayList3.add(new LockUnificationPreferenceController(context, securitySettings));
        arrayList3.add(new VisiblePatternProfilePreferenceController(context, lifecycle));
        arrayList3.add(new FingerprintProfileStatusPreferenceController(context));
        arrayList.add(new PreferenceCategoryController(context, "security_category_profile").setChildren(arrayList3));
        arrayList.addAll(arrayList3);
        return arrayList;
    }

    static class SummaryProvider implements SummaryLoader.SummaryProvider {
        private final Context mContext;
        private final SummaryLoader mSummaryLoader;

        public SummaryProvider(Context context, SummaryLoader summaryLoader) {
            this.mContext = context;
            this.mSummaryLoader = summaryLoader;
        }

        @Override // com.android.settings.dashboard.SummaryLoader.SummaryProvider
        public void setListening(boolean z) {
            if (z) {
                FingerprintManager fingerprintManagerOrNull = Utils.getFingerprintManagerOrNull(this.mContext);
                if (fingerprintManagerOrNull != null && fingerprintManagerOrNull.isHardwareDetected()) {
                    this.mSummaryLoader.setSummary(this, this.mContext.getString(R.string.security_dashboard_summary));
                } else {
                    this.mSummaryLoader.setSummary(this, this.mContext.getString(R.string.security_dashboard_summary_no_fingerprint));
                }
            }
        }
    }
}
