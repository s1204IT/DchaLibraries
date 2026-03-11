package com.android.settings.enterprise;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.os.UserHandle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceScreen;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.applications.ApplicationFeatureProvider;
import com.android.settings.applications.EnterpriseDefaultApps;
import com.android.settings.applications.UserAppInfo;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.users.UserFeatureProvider;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.utils.ThreadUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;

public class EnterpriseSetDefaultAppsListPreferenceController extends AbstractPreferenceController implements PreferenceControllerMixin {
    private final ApplicationFeatureProvider mApplicationFeatureProvider;
    private List<EnumMap<EnterpriseDefaultApps, List<ApplicationInfo>>> mApps;
    private final EnterprisePrivacyFeatureProvider mEnterprisePrivacyFeatureProvider;
    private final SettingsPreferenceFragment mParent;
    private final PackageManager mPm;
    private final UserFeatureProvider mUserFeatureProvider;
    private List<UserInfo> mUsers;

    public EnterpriseSetDefaultAppsListPreferenceController(Context context, SettingsPreferenceFragment settingsPreferenceFragment, PackageManager packageManager) {
        super(context);
        this.mUsers = Collections.emptyList();
        this.mApps = Collections.emptyList();
        this.mPm = packageManager;
        this.mParent = settingsPreferenceFragment;
        FeatureFactory factory = FeatureFactory.getFactory(context);
        this.mApplicationFeatureProvider = factory.getApplicationFeatureProvider(context);
        this.mEnterprisePrivacyFeatureProvider = factory.getEnterprisePrivacyFeatureProvider(context);
        this.mUserFeatureProvider = factory.getUserFeatureProvider(context);
        buildAppList();
    }

    private void buildAppList() {
        this.mUsers = new ArrayList();
        this.mApps = new ArrayList();
        for (UserHandle userHandle : this.mUserFeatureProvider.getUserProfiles()) {
            EnumMap<EnterpriseDefaultApps, List<ApplicationInfo>> enumMap = null;
            boolean z = false;
            for (EnterpriseDefaultApps enterpriseDefaultApps : EnterpriseDefaultApps.values()) {
                List<UserAppInfo> listFindPersistentPreferredActivities = this.mApplicationFeatureProvider.findPersistentPreferredActivities(userHandle.getIdentifier(), enterpriseDefaultApps.getIntents());
                if (!listFindPersistentPreferredActivities.isEmpty()) {
                    if (!z) {
                        this.mUsers.add(listFindPersistentPreferredActivities.get(0).userInfo);
                        enumMap = new EnumMap<>(EnterpriseDefaultApps.class);
                        this.mApps.add(enumMap);
                        z = true;
                    }
                    ArrayList arrayList = new ArrayList();
                    Iterator<UserAppInfo> it = listFindPersistentPreferredActivities.iterator();
                    while (it.hasNext()) {
                        arrayList.add(it.next().appInfo);
                    }
                    enumMap.put(enterpriseDefaultApps, arrayList);
                }
            }
        }
        ThreadUtils.postOnMainThread(new Runnable() {
            @Override
            public final void run() {
                this.f$0.updateUi();
            }
        });
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return null;
    }

    public void updateUi() {
        Context context = this.mParent.getPreferenceManager().getContext();
        PreferenceScreen preferenceScreen = this.mParent.getPreferenceScreen();
        if (preferenceScreen == null) {
            return;
        }
        if (!this.mEnterprisePrivacyFeatureProvider.isInCompMode() && this.mUsers.size() == 1) {
            createPreferences(context, preferenceScreen, this.mApps.get(0));
            return;
        }
        for (int i = 0; i < this.mUsers.size(); i++) {
            UserInfo userInfo = this.mUsers.get(i);
            PreferenceCategory preferenceCategory = new PreferenceCategory(context);
            preferenceScreen.addPreference(preferenceCategory);
            if (userInfo.isManagedProfile()) {
                preferenceCategory.setTitle(R.string.managed_device_admin_title);
            } else {
                preferenceCategory.setTitle(R.string.personal_device_admin_title);
            }
            preferenceCategory.setOrder(i);
            createPreferences(context, preferenceCategory, this.mApps.get(i));
        }
    }

    private void createPreferences(Context context, PreferenceGroup preferenceGroup, EnumMap<EnterpriseDefaultApps, List<ApplicationInfo>> enumMap) {
        if (preferenceGroup == null) {
            return;
        }
        for (EnterpriseDefaultApps enterpriseDefaultApps : EnterpriseDefaultApps.values()) {
            List<ApplicationInfo> list = enumMap.get(enterpriseDefaultApps);
            if (list != null && !list.isEmpty()) {
                Preference preference = new Preference(context);
                preference.setTitle(getTitle(context, enterpriseDefaultApps, list.size()));
                preference.setSummary(buildSummaryString(context, list));
                preference.setOrder(enterpriseDefaultApps.ordinal());
                preference.setSelectable(false);
                preferenceGroup.addPreference(preference);
            }
        }
    }

    private CharSequence buildSummaryString(Context context, List<ApplicationInfo> list) {
        CharSequence[] charSequenceArr = new String[list.size()];
        for (int i = 0; i < list.size(); i++) {
            charSequenceArr[i] = list.get(i).loadLabel(this.mPm);
        }
        if (list.size() == 1) {
            return charSequenceArr[0];
        }
        if (list.size() == 2) {
            return context.getString(R.string.app_names_concatenation_template_2, charSequenceArr[0], charSequenceArr[1]);
        }
        return context.getString(R.string.app_names_concatenation_template_3, charSequenceArr[0], charSequenceArr[1], charSequenceArr[2]);
    }

    private String getTitle(Context context, EnterpriseDefaultApps enterpriseDefaultApps, int i) {
        switch (enterpriseDefaultApps) {
            case BROWSER:
                return context.getString(R.string.default_browser_title);
            case CALENDAR:
                return context.getString(R.string.default_calendar_app_title);
            case CONTACTS:
                return context.getString(R.string.default_contacts_app_title);
            case PHONE:
                return context.getResources().getQuantityString(R.plurals.default_phone_app_title, i);
            case MAP:
                return context.getString(R.string.default_map_app_title);
            case EMAIL:
                return context.getResources().getQuantityString(R.plurals.default_email_app_title, i);
            case CAMERA:
                return context.getResources().getQuantityString(R.plurals.default_camera_app_title, i);
            default:
                throw new IllegalStateException("Unknown type of default " + enterpriseDefaultApps);
        }
    }
}
