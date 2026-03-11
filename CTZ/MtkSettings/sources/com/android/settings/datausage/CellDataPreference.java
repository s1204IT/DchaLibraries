package com.android.settings.datausage;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.NetworkTemplate;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.Settings;
import android.support.v4.content.res.TypedArrayUtils;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.telecom.TelecomManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Checkable;
import android.widget.Toast;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.datausage.TemplatePreference;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.CustomDialogPreference;
import com.mediatek.settings.UtilsExt;
import com.mediatek.settings.ext.IDataUsageSummaryExt;
import com.mediatek.settings.sim.TelephonyUtils;
import java.util.List;

public class CellDataPreference extends CustomDialogPreference implements TemplatePreference {
    private boolean mAlertForCdmaCompetition;
    public boolean mChecked;
    private final DataStateListener mDataStateListener;
    private IDataUsageSummaryExt mDataUsageSummaryExt;
    private boolean mIsAirplaneModeOn;
    public boolean mMultiSimDialog;
    final SubscriptionManager.OnSubscriptionsChangedListener mOnSubscriptionsChangeListener;
    private BroadcastReceiver mReceiver;
    public int mSubId;
    SubscriptionManager mSubscriptionManager;
    private TelephonyManager mTelephonyManager;

    public CellDataPreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet, TypedArrayUtils.getAttr(context, R.attr.switchPreferenceStyle, android.R.attr.switchPreferenceStyle));
        this.mSubId = -1;
        this.mOnSubscriptionsChangeListener = new SubscriptionManager.OnSubscriptionsChangedListener() {
            @Override
            public void onSubscriptionsChanged() {
                CellDataPreference.this.updateScreenEnableState();
            }
        };
        this.mDataStateListener = new DataStateListener() {
            @Override
            public void onChange(boolean z) {
                CellDataPreference.this.log("data state changed");
                CellDataPreference.this.updateChecked();
            }
        };
        this.mAlertForCdmaCompetition = false;
        this.mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                String action = intent.getAction();
                CellDataPreference.this.log("onReceive broadcast , action =  " + action);
                if (action.equals("android.intent.action.AIRPLANE_MODE")) {
                    CellDataPreference.this.mIsAirplaneModeOn = intent.getBooleanExtra("state", false);
                    CellDataPreference.this.updateScreenEnableState();
                } else if (action.equals("android.intent.action.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED")) {
                    CellDataPreference.this.onCdmaCompetitionHandled(intent);
                    CellDataPreference.this.updateScreenEnableState();
                } else if (action.equals("android.intent.action.ACTION_SET_RADIO_CAPABILITY_DONE") || action.equals("android.intent.action.ACTION_SET_RADIO_CAPABILITY_FAILED") || CellDataPreference.this.mDataUsageSummaryExt.customDualReceiver(action)) {
                    CellDataPreference.this.updateScreenEnableState();
                }
            }
        };
    }

    @Override
    protected void onRestoreInstanceState(Parcelable parcelable) {
        CellDataState cellDataState = (CellDataState) parcelable;
        super.onRestoreInstanceState(cellDataState.getSuperState());
        this.mTelephonyManager = TelephonyManager.from(getContext());
        this.mChecked = cellDataState.mChecked;
        this.mMultiSimDialog = cellDataState.mMultiSimDialog;
        if (this.mSubId == -1) {
            this.mSubId = cellDataState.mSubId;
            setKey(getKey() + this.mSubId);
        }
        notifyChanged();
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        CellDataState cellDataState = new CellDataState(super.onSaveInstanceState());
        cellDataState.mChecked = this.mChecked;
        cellDataState.mMultiSimDialog = this.mMultiSimDialog;
        cellDataState.mSubId = this.mSubId;
        return cellDataState;
    }

    @Override
    public void onAttached() {
        log("onAttached...");
        super.onAttached();
        this.mDataStateListener.setListener(true, this.mSubId, getContext());
        if (this.mSubscriptionManager != null) {
            this.mSubscriptionManager.addOnSubscriptionsChangedListener(this.mOnSubscriptionsChangeListener);
        }
        this.mDataUsageSummaryExt = UtilsExt.getDataUsageSummaryExt(getContext().getApplicationContext());
        this.mDataStateListener.setListener(true, this.mSubId, getContext());
        this.mIsAirplaneModeOn = TelephonyUtils.isAirplaneModeOn(getContext());
        IntentFilter intentFilter = new IntentFilter("android.intent.action.AIRPLANE_MODE");
        intentFilter.addAction("android.intent.action.ACTION_SET_RADIO_CAPABILITY_DONE");
        intentFilter.addAction("android.intent.action.ACTION_SET_RADIO_CAPABILITY_FAILED");
        intentFilter.addAction("android.intent.action.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED");
        this.mDataUsageSummaryExt.customReceiver(intentFilter);
        getContext().registerReceiver(this.mReceiver, intentFilter);
        updateScreenEnableState();
        if (this.mDataUsageSummaryExt != null) {
            this.mDataUsageSummaryExt.setPreferenceSummary(this);
        }
    }

    @Override
    public void onDetached() {
        log("onDetached...");
        this.mDataStateListener.setListener(false, this.mSubId, getContext());
        if (this.mSubscriptionManager != null) {
            this.mSubscriptionManager.removeOnSubscriptionsChangedListener(this.mOnSubscriptionsChangeListener);
        }
        super.onDetached();
        getContext().unregisterReceiver(this.mReceiver);
        this.mAlertForCdmaCompetition = false;
    }

    @Override
    public void setTemplate(NetworkTemplate networkTemplate, int i, TemplatePreference.NetworkServices networkServices) {
        if (i == -1) {
            throw new IllegalArgumentException("CellDataPreference needs a SubscriptionInfo");
        }
        this.mSubscriptionManager = SubscriptionManager.from(getContext());
        this.mTelephonyManager = TelephonyManager.from(getContext());
        this.mSubscriptionManager.addOnSubscriptionsChangedListener(this.mOnSubscriptionsChangeListener);
        if (this.mSubId == -1) {
            this.mSubId = i;
            setKey(getKey() + i);
        }
        updateScreenEnableState();
        updateChecked();
    }

    public void updateChecked() {
        setChecked(this.mTelephonyManager.getDataEnabled(this.mSubId));
    }

    @Override
    public void performClick(View view) {
        Context context = getContext();
        FeatureFactory.getFactory(context).getMetricsFeatureProvider().action(context, 178, !this.mChecked);
        SubscriptionInfo activeSubscriptionInfo = this.mSubscriptionManager.getActiveSubscriptionInfo(this.mSubId);
        SubscriptionInfo defaultDataSubscriptionInfo = this.mSubscriptionManager.getDefaultDataSubscriptionInfo();
        Log.i("CellDataPreference", "performClick, currentSir=" + activeSubscriptionInfo + ", nextSir=" + defaultDataSubscriptionInfo);
        if (this.mChecked) {
            if (!Utils.showSimCardTile(getContext()) || (defaultDataSubscriptionInfo != null && activeSubscriptionInfo != null && activeSubscriptionInfo.getSubscriptionId() == defaultDataSubscriptionInfo.getSubscriptionId())) {
                setMobileDataEnabled(false);
                if (defaultDataSubscriptionInfo != null && activeSubscriptionInfo != null && activeSubscriptionInfo.getSubscriptionId() == defaultDataSubscriptionInfo.getSubscriptionId() && this.mDataUsageSummaryExt.isAllowDataDisableForOtherSubscription()) {
                    disableDataForOtherSubscriptions(this.mSubId);
                    return;
                }
                return;
            }
            this.mMultiSimDialog = false;
            super.performClick(view);
            return;
        }
        if (Utils.showSimCardTile(getContext())) {
            this.mMultiSimDialog = true;
            if (defaultDataSubscriptionInfo != null && activeSubscriptionInfo != null && activeSubscriptionInfo.getSubscriptionId() == defaultDataSubscriptionInfo.getSubscriptionId()) {
                setMobileDataEnabled(true);
                if (this.mDataUsageSummaryExt.isAllowDataDisableForOtherSubscription()) {
                    disableDataForOtherSubscriptions(this.mSubId);
                    return;
                }
                return;
            }
            super.performClick(view);
            return;
        }
        setMobileDataEnabled(true);
    }

    private void setMobileDataEnabled(boolean z) {
        this.mTelephonyManager.setDataEnabled(this.mSubId, z);
        setChecked(z);
    }

    private void setChecked(boolean z) {
        log("setChecked " + z);
        if (this.mChecked == z) {
            return;
        }
        this.mChecked = z;
        notifyChanged();
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder preferenceViewHolder) {
        super.onBindViewHolder(preferenceViewHolder);
        View viewFindViewById = preferenceViewHolder.findViewById(android.R.id.switch_widget);
        viewFindViewById.setClickable(false);
        ((Checkable) viewFindViewById).setChecked(this.mChecked);
        this.mDataUsageSummaryExt.onBindViewHolder(getContext(), preferenceViewHolder.itemView, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CellDataPreference.this.performClick(view);
            }
        });
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder, DialogInterface.OnClickListener onClickListener) {
        if (this.mMultiSimDialog) {
            showMultiSimDialog(builder, onClickListener);
        } else if (this.mDataUsageSummaryExt.onDisablingData(this.mSubId)) {
            showDisableDialog(builder, onClickListener);
        }
    }

    private void showDisableDialog(AlertDialog.Builder builder, DialogInterface.OnClickListener onClickListener) {
        builder.setTitle((CharSequence) null).setMessage(R.string.data_usage_disable_mobile).setPositiveButton(android.R.string.ok, onClickListener).setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null);
    }

    private void showMultiSimDialog(AlertDialog.Builder builder, DialogInterface.OnClickListener onClickListener) {
        String string;
        SubscriptionInfo activeSubscriptionInfo = this.mSubscriptionManager.getActiveSubscriptionInfo(this.mSubId);
        SubscriptionInfo defaultDataSubscriptionInfo = this.mSubscriptionManager.getDefaultDataSubscriptionInfo();
        if (defaultDataSubscriptionInfo == null) {
            string = getContext().getResources().getString(R.string.sim_selection_required_pref);
        } else {
            string = defaultDataSubscriptionInfo.getDisplayName().toString();
        }
        builder.setTitle(R.string.sim_change_data_title);
        Context context = getContext();
        Object[] objArr = new Object[2];
        objArr[0] = String.valueOf(activeSubscriptionInfo != null ? activeSubscriptionInfo.getDisplayName() : null);
        objArr[1] = string;
        builder.setMessage(context.getString(R.string.sim_change_data_message, objArr));
        builder.setPositiveButton(R.string.okay, onClickListener);
        builder.setNegativeButton(R.string.cancel, (DialogInterface.OnClickListener) null);
    }

    private void disableDataForOtherSubscriptions(int i) {
        List<SubscriptionInfo> activeSubscriptionInfoList = this.mSubscriptionManager.getActiveSubscriptionInfoList();
        if (activeSubscriptionInfoList != null) {
            for (SubscriptionInfo subscriptionInfo : activeSubscriptionInfoList) {
                if (subscriptionInfo.getSubscriptionId() != i) {
                    this.mTelephonyManager.setDataEnabled(subscriptionInfo.getSubscriptionId(), false);
                }
            }
        }
    }

    @Override
    protected void onClick(DialogInterface dialogInterface, int i) {
        if (i != -1) {
            return;
        }
        log("onClick, mMultiSimDialog = " + this.mMultiSimDialog);
        if (this.mMultiSimDialog) {
            if (TelecomManager.from(getContext()).isInCall()) {
                Toast.makeText(getContext(), R.string.default_data_switch_err_msg1, 0).show();
                log("in Call, RETURN!");
                return;
            }
            this.mSubscriptionManager.setDefaultDataSubId(this.mSubId);
            setMobileDataEnabled(true);
            if (this.mDataUsageSummaryExt.isAllowDataDisableForOtherSubscription()) {
                disableDataForOtherSubscriptions(this.mSubId);
                return;
            }
            return;
        }
        setMobileDataEnabled(false);
    }

    public static abstract class DataStateListener extends ContentObserver {
        public DataStateListener() {
            super(new Handler(Looper.getMainLooper()));
        }

        public void setListener(boolean z, int i, Context context) {
            if (z) {
                Uri uriFor = Settings.Global.getUriFor("mobile_data");
                if (TelephonyManager.getDefault().getSimCount() != 1) {
                    uriFor = Settings.Global.getUriFor("mobile_data" + i);
                }
                context.getContentResolver().registerContentObserver(uriFor, false, this);
                return;
            }
            context.getContentResolver().unregisterContentObserver(this);
        }
    }

    public static class CellDataState extends Preference.BaseSavedState {
        public static final Parcelable.Creator<CellDataState> CREATOR = new Parcelable.Creator<CellDataState>() {
            @Override
            public CellDataState createFromParcel(Parcel parcel) {
                return new CellDataState(parcel);
            }

            @Override
            public CellDataState[] newArray(int i) {
                return new CellDataState[i];
            }
        };
        public boolean mChecked;
        public boolean mMultiSimDialog;
        public int mSubId;

        public CellDataState(Parcelable parcelable) {
            super(parcelable);
        }

        public CellDataState(Parcel parcel) {
            super(parcel);
            this.mChecked = parcel.readByte() != 0;
            this.mMultiSimDialog = parcel.readByte() != 0;
            this.mSubId = parcel.readInt();
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            super.writeToParcel(parcel, i);
            parcel.writeByte(this.mChecked ? (byte) 1 : (byte) 0);
            parcel.writeByte(this.mMultiSimDialog ? (byte) 1 : (byte) 0);
            parcel.writeInt(this.mSubId);
        }
    }

    public void onCdmaCompetitionHandled(Intent intent) {
        int intExtra = intent.getIntExtra("subscription", -1);
        log("defaultDataSubId: " + intExtra + " mAlertForCdmaCompetition: " + this.mAlertForCdmaCompetition);
        if (this.mAlertForCdmaCompetition && intExtra == this.mSubId) {
            setMobileDataEnabled(true);
            if (this.mDataUsageSummaryExt.isAllowDataDisableForOtherSubscription()) {
                disableDataForOtherSubscriptions(this.mSubId);
            }
            this.mAlertForCdmaCompetition = false;
        }
    }

    public void updateScreenEnableState() {
        boolean zIsCapabilitySwitching = TelephonyUtils.isCapabilitySwitching();
        log("updateScreenEnableState, mIsAirplaneModeOn = " + this.mIsAirplaneModeOn + ", isCapabilitySwitching = " + zIsCapabilitySwitching);
        boolean z = (this.mIsAirplaneModeOn || zIsCapabilitySwitching) ? false : true;
        if (this.mDataUsageSummaryExt != null) {
            z = z && this.mDataUsageSummaryExt.isAllowDataEnable(this.mSubId);
            log("enabled = " + z);
        }
        setEnabled(z);
    }

    public void log(String str) {
        Log.d("CellDataPreference[" + this.mSubId + "]", str);
    }
}
