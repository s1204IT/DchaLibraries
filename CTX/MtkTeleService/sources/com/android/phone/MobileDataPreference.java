package com.android.phone;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.DialogPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Checkable;
import com.android.internal.logging.MetricsLogger;
import java.util.List;

public class MobileDataPreference extends DialogPreference {
    private static final boolean DBG = false;
    private static final String TAG = "MobileDataPreference";
    public boolean mChecked;
    private final DataStateListener mListener;
    public boolean mMultiSimDialog;
    public int mSubId;
    private SubscriptionManager mSubscriptionManager;
    private TelephonyManager mTelephonyManager;

    public MobileDataPreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet, android.R.attr.switchPreferenceStyle);
        this.mSubId = -1;
        this.mListener = new DataStateListener() {
            @Override
            public void onChange(boolean z) {
                MobileDataPreference.this.updateChecked();
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
    protected void onAttachedToActivity() {
        super.onAttachedToActivity();
        this.mListener.setListener(true, this.mSubId, getContext());
    }

    @Override
    protected void onPrepareForRemoval() {
        this.mListener.setListener(false, this.mSubId, getContext());
        super.onPrepareForRemoval();
    }

    public void initialize(int i) {
        if (i == -1) {
            throw new IllegalArgumentException("MobileDataPreference needs a SubscriptionInfo");
        }
        this.mSubscriptionManager = SubscriptionManager.from(getContext());
        this.mTelephonyManager = TelephonyManager.from(getContext());
        if (this.mSubId != i) {
            this.mSubId = i;
            setKey(getKey() + i);
        }
        updateChecked();
    }

    private void updateChecked() {
        setChecked(this.mTelephonyManager.getDataEnabled(this.mSubId));
    }

    public void performClick(PreferenceScreen preferenceScreen) {
        if (!isEnabled() || !SubscriptionManager.isValidSubscriptionId(this.mSubId)) {
            return;
        }
        SubscriptionInfo activeSubscriptionInfo = this.mSubscriptionManager.getActiveSubscriptionInfo(this.mSubId);
        SubscriptionInfo defaultDataSubscriptionInfo = this.mSubscriptionManager.getDefaultDataSubscriptionInfo();
        boolean z = this.mTelephonyManager.getSimCount() > 1;
        Log.i(TAG, "performClick, currentSir=" + activeSubscriptionInfo + ", nextSir=" + defaultDataSubscriptionInfo);
        if (this.mChecked) {
            if (!z || (defaultDataSubscriptionInfo != null && activeSubscriptionInfo != null && activeSubscriptionInfo.getSubscriptionId() == defaultDataSubscriptionInfo.getSubscriptionId())) {
                setMobileDataEnabled(false);
                if (defaultDataSubscriptionInfo != null && activeSubscriptionInfo != null) {
                    activeSubscriptionInfo.getSubscriptionId();
                    defaultDataSubscriptionInfo.getSubscriptionId();
                    return;
                }
                return;
            }
            this.mMultiSimDialog = false;
            super.performClick(preferenceScreen);
            return;
        }
        if (z) {
            this.mMultiSimDialog = true;
            if (defaultDataSubscriptionInfo != null && activeSubscriptionInfo != null && activeSubscriptionInfo.getSubscriptionId() == defaultDataSubscriptionInfo.getSubscriptionId()) {
                setMobileDataEnabled(true);
                return;
            } else {
                super.performClick(preferenceScreen);
                return;
            }
        }
        setMobileDataEnabled(true);
    }

    private void setMobileDataEnabled(boolean z) {
        MetricsLogger.action(getContext(), 1081, z);
        this.mTelephonyManager.setDataEnabled(this.mSubId, z);
        setChecked(z);
    }

    private void setChecked(boolean z) {
        if (this.mChecked == z) {
            return;
        }
        this.mChecked = z;
        notifyChanged();
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        View viewFindViewById = view.findViewById(android.R.id.switch_widget);
        viewFindViewById.setClickable(false);
        ((Checkable) viewFindViewById).setChecked(this.mChecked);
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        if (this.mMultiSimDialog) {
            showMultiSimDialog(builder);
        } else {
            showDisableDialog(builder);
        }
    }

    private void showDisableDialog(AlertDialog.Builder builder) {
        builder.setTitle((CharSequence) null).setMessage(R.string.data_usage_disable_mobile).setPositiveButton(android.R.string.ok, this).setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null);
    }

    private void showMultiSimDialog(AlertDialog.Builder builder) {
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
        builder.setPositiveButton(R.string.ok, this);
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
    public void onClick(DialogInterface dialogInterface, int i) {
        if (i != -1) {
            return;
        }
        if (this.mMultiSimDialog) {
            this.mSubscriptionManager.setDefaultDataSubId(this.mSubId);
            setMobileDataEnabled(true);
        } else {
            setMobileDataEnabled(false);
        }
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
}
