package com.android.settings.accounts;

import android.app.Activity;
import android.content.Context;
import android.os.UserHandle;
import android.provider.SearchIndexableResource;
import android.text.BidiFormatter;
import android.text.TextUtils;
import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.users.AutoSyncDataPreferenceController;
import com.android.settings.users.AutoSyncPersonalDataPreferenceController;
import com.android.settings.users.AutoSyncWorkDataPreferenceController;
import com.android.settingslib.accounts.AuthenticatorHelper;
import com.android.settingslib.core.AbstractPreferenceController;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AccountDashboardFragment extends DashboardFragment {
    public static final SummaryLoader.SummaryProviderFactory SUMMARY_PROVIDER_FACTORY = new SummaryLoader.SummaryProviderFactory() {
        @Override
        public SummaryLoader.SummaryProvider createSummaryProvider(Activity activity, SummaryLoader summaryLoader) {
            return new SummaryProvider(activity, summaryLoader);
        }
    };
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() {
        @Override
        public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean z) {
            SearchIndexableResource searchIndexableResource = new SearchIndexableResource(context);
            searchIndexableResource.xmlResId = R.xml.accounts_dashboard_settings;
            return Arrays.asList(searchIndexableResource);
        }
    };

    @Override
    public int getMetricsCategory() {
        return 8;
    }

    @Override
    protected String getLogTag() {
        return "AccountDashboardFrag";
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.accounts_dashboard_settings;
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_user_and_account_dashboard;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        ArrayList arrayList = new ArrayList();
        AccountPreferenceController accountPreferenceController = new AccountPreferenceController(context, this, getIntent().getStringArrayExtra("authorities"));
        getLifecycle().addObserver(accountPreferenceController);
        arrayList.add(accountPreferenceController);
        arrayList.add(new AutoSyncDataPreferenceController(context, this));
        arrayList.add(new AutoSyncPersonalDataPreferenceController(context, this));
        arrayList.add(new AutoSyncWorkDataPreferenceController(context, this));
        return arrayList;
    }

    private static class SummaryProvider implements SummaryLoader.SummaryProvider {
        private final Context mContext;
        private final SummaryLoader mSummaryLoader;

        public SummaryProvider(Context context, SummaryLoader summaryLoader) {
            this.mContext = context;
            this.mSummaryLoader = summaryLoader;
        }

        @Override
        public void setListening(boolean z) {
            CharSequence string;
            if (z) {
                AuthenticatorHelper authenticatorHelper = new AuthenticatorHelper(this.mContext, UserHandle.of(UserHandle.myUserId()), null);
                String[] enabledAccountTypes = authenticatorHelper.getEnabledAccountTypes();
                BidiFormatter bidiFormatter = BidiFormatter.getInstance();
                if (enabledAccountTypes == null || enabledAccountTypes.length == 0) {
                    string = this.mContext.getString(R.string.account_dashboard_default_summary);
                } else {
                    int iMin = Math.min(3, enabledAccountTypes.length);
                    string = null;
                    for (int i = 0; i < enabledAccountTypes.length && iMin > 0; i++) {
                        CharSequence labelForType = authenticatorHelper.getLabelForType(this.mContext, enabledAccountTypes[i]);
                        if (!TextUtils.isEmpty(labelForType)) {
                            if (string == null) {
                                string = bidiFormatter.unicodeWrap(labelForType);
                            } else {
                                string = this.mContext.getString(R.string.join_many_items_middle, string, bidiFormatter.unicodeWrap(labelForType));
                            }
                            iMin--;
                        }
                    }
                }
                this.mSummaryLoader.setSummary(this, string);
            }
        }
    }
}
