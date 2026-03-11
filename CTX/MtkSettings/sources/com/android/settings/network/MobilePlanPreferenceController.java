package com.android.settings.network;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.support.v7.preference.Preference;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.Utils;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnCreate;
import com.android.settingslib.core.lifecycle.events.OnSaveInstanceState;
import java.util.List;

public class MobilePlanPreferenceController extends AbstractPreferenceController implements PreferenceControllerMixin, LifecycleObserver, OnCreate, OnSaveInstanceState {
    private ConnectivityManager mCm;
    private final MobilePlanPreferenceHost mHost;
    private final boolean mIsSecondaryUser;
    private String mMobilePlanDialogMessage;
    private TelephonyManager mTm;
    private final UserManager mUserManager;

    public interface MobilePlanPreferenceHost {
        void showMobilePlanMessageDialog();
    }

    public MobilePlanPreferenceController(Context context, MobilePlanPreferenceHost mobilePlanPreferenceHost) {
        super(context);
        this.mHost = mobilePlanPreferenceHost;
        this.mCm = (ConnectivityManager) context.getSystemService("connectivity");
        this.mTm = (TelephonyManager) context.getSystemService("phone");
        this.mUserManager = (UserManager) context.getSystemService("user");
        this.mIsSecondaryUser = !this.mUserManager.isAdminUser();
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (this.mHost != null && "manage_mobile_plan".equals(preference.getKey())) {
            this.mMobilePlanDialogMessage = null;
            onManageMobilePlanClick();
            return false;
        }
        return false;
    }

    @Override
    public void onCreate(Bundle bundle) {
        if (bundle != null) {
            this.mMobilePlanDialogMessage = bundle.getString("mManageMobilePlanMessage");
        }
        Log.d("MobilePlanPrefContr", "onCreate: mMobilePlanDialogMessage=" + this.mMobilePlanDialogMessage);
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        if (!TextUtils.isEmpty(this.mMobilePlanDialogMessage)) {
            bundle.putString("mManageMobilePlanMessage", this.mMobilePlanDialogMessage);
        }
    }

    public String getMobilePlanDialogMessage() {
        return this.mMobilePlanDialogMessage;
    }

    public void setMobilePlanDialogMessage(String str) {
        this.mMobilePlanDialogMessage = str;
    }

    @Override
    public boolean isAvailable() {
        return (!this.mIsSecondaryUser && !Utils.isWifiOnly(this.mContext) && !RestrictedLockUtils.hasBaseUserRestriction(this.mContext, "no_config_mobile_networks", UserHandle.myUserId())) && this.mContext.getResources().getBoolean(R.bool.config_show_mobile_plan);
    }

    @Override
    public String getPreferenceKey() {
        return "manage_mobile_plan";
    }

    private void onManageMobilePlanClick() {
        Resources resources = this.mContext.getResources();
        NetworkInfo activeNetworkInfo = this.mCm.getActiveNetworkInfo();
        if (this.mTm.hasIccCard() && activeNetworkInfo != null) {
            Intent intent = new Intent("android.intent.action.CARRIER_SETUP");
            List carrierPackageNamesForIntent = this.mTm.getCarrierPackageNamesForIntent(intent);
            if (carrierPackageNamesForIntent != null && !carrierPackageNamesForIntent.isEmpty()) {
                if (carrierPackageNamesForIntent.size() != 1) {
                    Log.w("MobilePlanPrefContr", "Multiple matching carrier apps found, launching the first.");
                }
                intent.setPackage((String) carrierPackageNamesForIntent.get(0));
                this.mContext.startActivity(intent);
                return;
            }
            String mobileProvisioningUrl = this.mCm.getMobileProvisioningUrl();
            if (!TextUtils.isEmpty(mobileProvisioningUrl)) {
                Intent intentMakeMainSelectorActivity = Intent.makeMainSelectorActivity("android.intent.action.MAIN", "android.intent.category.APP_BROWSER");
                intentMakeMainSelectorActivity.setData(Uri.parse(mobileProvisioningUrl));
                intentMakeMainSelectorActivity.setFlags(272629760);
                try {
                    this.mContext.startActivity(intentMakeMainSelectorActivity);
                } catch (ActivityNotFoundException e) {
                    Log.w("MobilePlanPrefContr", "onManageMobilePlanClick: startActivity failed" + e);
                }
            } else {
                String simOperatorName = this.mTm.getSimOperatorName();
                if (TextUtils.isEmpty(simOperatorName)) {
                    String networkOperatorName = this.mTm.getNetworkOperatorName();
                    if (TextUtils.isEmpty(networkOperatorName)) {
                        this.mMobilePlanDialogMessage = resources.getString(R.string.mobile_unknown_sim_operator);
                    } else {
                        this.mMobilePlanDialogMessage = resources.getString(R.string.mobile_no_provisioning_url, networkOperatorName);
                    }
                } else {
                    this.mMobilePlanDialogMessage = resources.getString(R.string.mobile_no_provisioning_url, simOperatorName);
                }
            }
        } else if (!this.mTm.hasIccCard()) {
            this.mMobilePlanDialogMessage = resources.getString(R.string.mobile_insert_sim_card);
        } else {
            this.mMobilePlanDialogMessage = resources.getString(R.string.mobile_connect_to_internet);
        }
        if (!TextUtils.isEmpty(this.mMobilePlanDialogMessage)) {
            Log.d("MobilePlanPrefContr", "onManageMobilePlanClick: message=" + this.mMobilePlanDialogMessage);
            if (this.mHost != null) {
                this.mHost.showMobilePlanMessageDialog();
            } else {
                Log.d("MobilePlanPrefContr", "Missing host fragment, cannot show message dialog.");
            }
        }
    }
}
