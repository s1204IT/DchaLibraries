package com.android.settings.applications.appinfo;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.UserHandle;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import com.android.settings.R;
import com.android.settings.applications.AppStoreUtil;
import com.android.settings.applications.LayoutPreference;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.applications.AppUtils;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnCreateOptionsMenu;
import com.android.settingslib.core.lifecycle.events.OnOptionsItemSelected;
import com.android.settingslib.core.lifecycle.events.OnPrepareOptionsMenu;
import com.android.settingslib.wrapper.PackageManagerWrapper;
import java.util.Iterator;

/* loaded from: classes.dex */
public class InstantAppButtonsPreferenceController extends BasePreferenceController implements DialogInterface.OnClickListener, LifecycleObserver, OnCreateOptionsMenu, OnOptionsItemSelected, OnPrepareOptionsMenu {
    private static final String KEY_INSTANT_APP_BUTTONS = "instant_app_buttons";
    private static final String META_DATA_DEFAULT_URI = "default-url";
    private MenuItem mInstallMenu;
    private String mLaunchUri;
    private final PackageManagerWrapper mPackageManagerWrapper;
    private final String mPackageName;
    private final AppInfoDashboardFragment mParent;
    private LayoutPreference mPreference;

    public InstantAppButtonsPreferenceController(Context context, AppInfoDashboardFragment appInfoDashboardFragment, String str, Lifecycle lifecycle) {
        super(context, KEY_INSTANT_APP_BUTTONS);
        this.mParent = appInfoDashboardFragment;
        this.mPackageName = str;
        this.mPackageManagerWrapper = new PackageManagerWrapper(context.getPackageManager());
        this.mLaunchUri = getDefaultLaunchUri();
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @Override // com.android.settings.core.BasePreferenceController
    public int getAvailabilityStatus() {
        return AppUtils.isInstant(this.mParent.getPackageInfo().applicationInfo) ? 0 : 3;
    }

    @Override // com.android.settings.core.BasePreferenceController, com.android.settingslib.core.AbstractPreferenceController
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        this.mPreference = (LayoutPreference) preferenceScreen.findPreference(KEY_INSTANT_APP_BUTTONS);
        initButtons(this.mPreference.findViewById(R.id.instant_app_button_container));
    }

    @Override // com.android.settingslib.core.lifecycle.events.OnCreateOptionsMenu
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        if (!TextUtils.isEmpty(this.mLaunchUri)) {
            menu.add(0, 3, 2, R.string.install_text).setShowAsAction(0);
        }
    }

    @Override // com.android.settingslib.core.lifecycle.events.OnOptionsItemSelected
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == 3) {
            Intent appStoreLink = AppStoreUtil.getAppStoreLink(this.mContext, this.mPackageName);
            if (appStoreLink != null) {
                this.mParent.startActivity(appStoreLink);
                return true;
            }
            return true;
        }
        return false;
    }

    @Override // com.android.settingslib.core.lifecycle.events.OnPrepareOptionsMenu
    public void onPrepareOptionsMenu(Menu menu) {
        this.mInstallMenu = menu.findItem(3);
        if (this.mInstallMenu != null && AppStoreUtil.getAppStoreLink(this.mContext, this.mPackageName) == null) {
            this.mInstallMenu.setEnabled(false);
        }
    }

    @Override // android.content.DialogInterface.OnClickListener
    public void onClick(DialogInterface dialogInterface, int i) {
        FeatureFactory.getFactory(this.mContext).getMetricsFeatureProvider().action(this.mContext, 923, this.mPackageName, new Pair[0]);
        this.mPackageManagerWrapper.deletePackageAsUser(this.mPackageName, null, 0, UserHandle.myUserId());
    }

    AlertDialog createDialog(int i) {
        if (i != 4) {
            return null;
        }
        return new AlertDialog.Builder(this.mContext).setPositiveButton(R.string.clear_instant_app_data, this).setNegativeButton(R.string.cancel, (DialogInterface.OnClickListener) null).setTitle(R.string.clear_instant_app_data).setMessage(this.mContext.getString(R.string.clear_instant_app_confirmation)).create();
    }

    private void initButtons(View view) {
        Button button = (Button) view.findViewById(R.id.install);
        Button button2 = (Button) view.findViewById(R.id.clear_data);
        Button button3 = (Button) view.findViewById(R.id.launch);
        if (!TextUtils.isEmpty(this.mLaunchUri)) {
            button.setVisibility(8);
            final Intent intent = new Intent("android.intent.action.VIEW");
            intent.addCategory("android.intent.category.BROWSABLE");
            intent.setPackage(this.mPackageName);
            intent.setData(Uri.parse(this.mLaunchUri));
            intent.addFlags(268435456);
            button3.setOnClickListener(new View.OnClickListener() { // from class: com.android.settings.applications.appinfo.-$$Lambda$InstantAppButtonsPreferenceController$2vM5nla3CEsaIUNVk7alr9UEbBA
                @Override // android.view.View.OnClickListener
                public final void onClick(View view2) {
                    this.f$0.mParent.startActivity(intent);
                }
            });
        } else {
            button3.setVisibility(8);
            final Intent appStoreLink = AppStoreUtil.getAppStoreLink(this.mContext, this.mPackageName);
            if (appStoreLink != null) {
                button.setOnClickListener(new View.OnClickListener() { // from class: com.android.settings.applications.appinfo.-$$Lambda$InstantAppButtonsPreferenceController$oBWjqqdf33bi3sDY5lE6TGLlFJM
                    @Override // android.view.View.OnClickListener
                    public final void onClick(View view2) {
                        this.f$0.mParent.startActivity(appStoreLink);
                    }
                });
            } else {
                button.setEnabled(false);
            }
        }
        button2.setOnClickListener(new View.OnClickListener() { // from class: com.android.settings.applications.appinfo.-$$Lambda$InstantAppButtonsPreferenceController$f8slAx9lBDdGAmwfjMjp59JCarA
            @Override // android.view.View.OnClickListener
            public final void onClick(View view2) {
                InstantAppButtonsPreferenceController.lambda$initButtons$2(this.f$0, view2);
            }
        });
    }

    public static /* synthetic */ void lambda$initButtons$2(InstantAppButtonsPreferenceController instantAppButtonsPreferenceController, View view) {
        AppInfoDashboardFragment appInfoDashboardFragment = instantAppButtonsPreferenceController.mParent;
        AppInfoDashboardFragment appInfoDashboardFragment2 = instantAppButtonsPreferenceController.mParent;
        appInfoDashboardFragment.showDialogInner(4, 0);
    }

    private String getDefaultLaunchUri() {
        PackageManager packageManager = this.mContext.getPackageManager();
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.addCategory("android.intent.category.LAUNCHER");
        intent.setPackage(this.mPackageName);
        Iterator<ResolveInfo> it = packageManager.queryIntentActivities(intent, 8388736).iterator();
        while (it.hasNext()) {
            Bundle bundle = it.next().activityInfo.metaData;
            if (bundle != null) {
                String string = bundle.getString(META_DATA_DEFAULT_URI);
                if (!TextUtils.isEmpty(string)) {
                    return string;
                }
            }
        }
        return null;
    }
}
