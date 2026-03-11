package com.android.settings.security.screenlock;

import android.app.Fragment;
import android.content.Context;
import android.os.UserHandle;
import android.provider.SearchIndexableResource;
import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.security.OwnerInfoPreferenceController;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import java.util.ArrayList;
import java.util.List;

public class ScreenLockSettings extends DashboardFragment implements OwnerInfoPreferenceController.OwnerInfoCallback {
    private static final int MY_USER_ID = UserHandle.myUserId();
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() {
        @Override
        public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean z) {
            ArrayList arrayList = new ArrayList();
            SearchIndexableResource searchIndexableResource = new SearchIndexableResource(context);
            searchIndexableResource.xmlResId = R.xml.screen_lock_settings;
            arrayList.add(searchIndexableResource);
            return arrayList;
        }

        @Override
        public List<AbstractPreferenceController> createPreferenceControllers(Context context) {
            return ScreenLockSettings.buildPreferenceControllers(context, null, null, new LockPatternUtils(context));
        }

        @Override
        public List<String> getNonIndexableKeys(Context context) {
            List<String> nonIndexableKeys = super.getNonIndexableKeys(context);
            nonIndexableKeys.add("security_settings_password_sub_screen");
            return nonIndexableKeys;
        }
    };
    private LockPatternUtils mLockPatternUtils;

    @Override
    public int getMetricsCategory() {
        return 1265;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.screen_lock_settings;
    }

    @Override
    protected String getLogTag() {
        return "ScreenLockSettings";
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        this.mLockPatternUtils = new LockPatternUtils(context);
        return buildPreferenceControllers(context, this, getLifecycle(), this.mLockPatternUtils);
    }

    @Override
    public void onOwnerInfoUpdated() {
        ((OwnerInfoPreferenceController) use(OwnerInfoPreferenceController.class)).updateSummary();
    }

    public static List<AbstractPreferenceController> buildPreferenceControllers(Context context, Fragment fragment, Lifecycle lifecycle, LockPatternUtils lockPatternUtils) {
        ArrayList arrayList = new ArrayList();
        arrayList.add(new PatternVisiblePreferenceController(context, MY_USER_ID, lockPatternUtils));
        arrayList.add(new PowerButtonInstantLockPreferenceController(context, MY_USER_ID, lockPatternUtils));
        arrayList.add(new LockAfterTimeoutPreferenceController(context, MY_USER_ID, lockPatternUtils));
        arrayList.add(new OwnerInfoPreferenceController(context, fragment, lifecycle));
        return arrayList;
    }
}
