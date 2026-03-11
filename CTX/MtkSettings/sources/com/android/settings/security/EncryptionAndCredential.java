package com.android.settings.security;

import android.content.Context;
import android.os.UserManager;
import android.provider.SearchIndexableResource;
import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.widget.PreferenceCategoryController;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EncryptionAndCredential extends DashboardFragment {
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new SecuritySearchIndexProvider();

    @Override
    public int getMetricsCategory() {
        return 846;
    }

    @Override
    protected String getLogTag() {
        return "EncryptionAndCredential";
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return buildPreferenceControllers(context, getLifecycle());
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.encryption_and_credential;
    }

    public static List<AbstractPreferenceController> buildPreferenceControllers(Context context, Lifecycle lifecycle) {
        ArrayList arrayList = new ArrayList();
        EncryptionStatusPreferenceController encryptionStatusPreferenceController = new EncryptionStatusPreferenceController(context, "encryption_and_credentials_encryption_status");
        arrayList.add(encryptionStatusPreferenceController);
        arrayList.add(new PreferenceCategoryController(context, "encryption_and_credentials_status_category").setChildren(Arrays.asList(encryptionStatusPreferenceController)));
        arrayList.add(new CredentialStoragePreferenceController(context));
        arrayList.add(new UserCredentialsPreferenceController(context));
        arrayList.add(new ResetCredentialsPreferenceController(context, lifecycle));
        arrayList.add(new InstallCredentialsPreferenceController(context));
        return arrayList;
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_encryption;
    }

    private static class SecuritySearchIndexProvider extends BaseSearchIndexProvider {
        private SecuritySearchIndexProvider() {
        }

        @Override
        public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean z) {
            SearchIndexableResource searchIndexableResource = new SearchIndexableResource(context);
            searchIndexableResource.xmlResId = R.xml.encryption_and_credential;
            return Arrays.asList(searchIndexableResource);
        }

        @Override
        public List<AbstractPreferenceController> createPreferenceControllers(Context context) {
            return EncryptionAndCredential.buildPreferenceControllers(context, null);
        }

        @Override
        protected boolean isPageSearchEnabled(Context context) {
            return ((UserManager) context.getSystemService("user")).isAdminUser();
        }
    }
}
