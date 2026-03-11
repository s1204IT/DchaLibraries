package com.android.settings.widget;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v7.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import com.android.settings.R;
import com.android.settingslib.CustomDialogPreference;
import com.android.settingslib.wifi.AccessPoint;
import java.util.ArrayList;

public class HotspotApBandSelectionPreference extends CustomDialogPreference implements DialogInterface.OnShowListener, CompoundButton.OnCheckedChangeListener {
    static final String KEY_CHECKED_BANDS = "checked_bands";
    static final String KEY_HOTSPOT_SUPER_STATE = "hotspot_super_state";
    private String[] mBandEntries;
    CheckBox mBox2G;
    CheckBox mBox5G;
    private int mExistingConfigValue;
    ArrayList<Integer> mRestoredBands;
    boolean mShouldRestore;

    public HotspotApBandSelectionPreference(Context context) {
        super(context);
        this.mExistingConfigValue = AccessPoint.UNREACHABLE_RSSI;
    }

    public HotspotApBandSelectionPreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mExistingConfigValue = AccessPoint.UNREACHABLE_RSSI;
    }

    public HotspotApBandSelectionPreference(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mExistingConfigValue = AccessPoint.UNREACHABLE_RSSI;
    }

    public HotspotApBandSelectionPreference(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mExistingConfigValue = AccessPoint.UNREACHABLE_RSSI;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable parcelable) {
        SavedState savedState = (SavedState) parcelable;
        super.onRestoreInstanceState(savedState.getSuperState());
        this.mShouldRestore = savedState.shouldRestore;
        if (this.mShouldRestore) {
            this.mRestoredBands = new ArrayList<>();
            if (savedState.enabled2G) {
                this.mRestoredBands.add(0);
            }
            if (savedState.enabled5G) {
                this.mRestoredBands.add(1);
            }
        } else {
            this.mRestoredBands = null;
        }
        updatePositiveButton();
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        Context context = getContext();
        setOnShowListener(this);
        this.mBandEntries = context.getResources().getStringArray(R.array.wifi_ap_band_config_full);
        addApBandViews((LinearLayout) view);
        updatePositiveButton();
        this.mRestoredBands = null;
        this.mShouldRestore = false;
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        SavedState savedState = new SavedState(super.onSaveInstanceState());
        boolean z = false;
        savedState.shouldRestore = getDialog() != null;
        savedState.enabled2G = this.mBox2G != null && this.mBox2G.isChecked();
        if (this.mBox5G != null && this.mBox5G.isChecked()) {
            z = true;
        }
        savedState.enabled5G = z;
        return savedState;
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean z) {
        if (!(compoundButton instanceof CheckBox)) {
            return;
        }
        updatePositiveButton();
    }

    @Override
    protected void onClick(DialogInterface dialogInterface, int i) {
        if (i == -1) {
            if (this.mBox2G.isChecked() || this.mBox5G.isChecked()) {
                int wifiBand = getWifiBand();
                this.mExistingConfigValue = wifiBand;
                callChangeListener(Integer.valueOf(wifiBand));
            }
        }
    }

    public void setExistingConfigValue(int i) {
        this.mExistingConfigValue = i;
    }

    private void addApBandViews(LinearLayout linearLayout) {
        this.mBox2G = (CheckBox) linearLayout.findViewById(R.id.box_2g);
        this.mBox2G.setText(this.mBandEntries[0]);
        this.mBox2G.setChecked(restoreBandIfNeeded(0));
        this.mBox2G.setOnCheckedChangeListener(this);
        this.mBox5G = (CheckBox) linearLayout.findViewById(R.id.box_5g);
        this.mBox5G.setText(this.mBandEntries[1]);
        this.mBox5G.setChecked(restoreBandIfNeeded(1));
        this.mBox5G.setOnCheckedChangeListener(this);
    }

    private boolean restoreBandIfNeeded(int i) {
        return (isBandPreviouslySelected(i) && !this.mShouldRestore) || (this.mShouldRestore && this.mRestoredBands.contains(Integer.valueOf(i)));
    }

    private void updatePositiveButton() {
        AlertDialog alertDialog = (AlertDialog) getDialog();
        Button button = alertDialog == null ? null : alertDialog.getButton(-1);
        if (button != null && this.mBox5G != null && this.mBox2G != null) {
            button.setEnabled(this.mBox2G.isChecked() || this.mBox5G.isChecked());
        }
    }

    int getWifiBand() {
        boolean zIsChecked = this.mBox2G.isChecked();
        boolean zIsChecked2 = this.mBox5G.isChecked();
        if (zIsChecked && zIsChecked2) {
            return -1;
        }
        if (zIsChecked && !zIsChecked2) {
            return 0;
        }
        if (zIsChecked2 && !zIsChecked) {
            return 1;
        }
        throw new IllegalStateException("Wifi Config only supports selecting one or all bands");
    }

    private boolean isBandPreviouslySelected(int i) {
        switch (this.mExistingConfigValue) {
            case -1:
                return true;
            case 0:
                return i == 0;
            case 1:
                return i == 1;
            default:
                return false;
        }
    }

    @Override
    public void onShow(DialogInterface dialogInterface) {
        updatePositiveButton();
    }

    private static class SavedState extends Preference.BaseSavedState {
        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel parcel) {
                return new SavedState(parcel);
            }

            @Override
            public SavedState[] newArray(int i) {
                return new SavedState[i];
            }
        };
        boolean enabled2G;
        boolean enabled5G;
        boolean shouldRestore;

        public SavedState(Parcelable parcelable) {
            super(parcelable);
        }

        private SavedState(Parcel parcel) {
            super(parcel);
            this.shouldRestore = parcel.readByte() == 1;
            this.enabled2G = parcel.readByte() == 1;
            this.enabled5G = parcel.readByte() == 1;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            super.writeToParcel(parcel, i);
            parcel.writeByte(this.shouldRestore ? (byte) 1 : (byte) 0);
            parcel.writeByte(this.enabled2G ? (byte) 1 : (byte) 0);
            parcel.writeByte(this.enabled5G ? (byte) 1 : (byte) 0);
        }

        public String toString() {
            return "HotspotApBandSelectionPreference.SavedState{" + Integer.toHexString(System.identityHashCode(this)) + " shouldRestore=" + this.shouldRestore + " enabled2G=" + this.enabled2G + " enabled5G=" + this.enabled5G + "}";
        }
    }
}
