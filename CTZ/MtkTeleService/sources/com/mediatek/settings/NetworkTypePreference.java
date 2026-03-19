package com.mediatek.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.util.Log;
import com.mediatek.phone.PhoneFeatureConstants;

public class NetworkTypePreference extends DialogPreference implements DialogInterface.OnMultiChoiceClickListener {
    private int mAct;
    private int[] mActArray;
    private boolean[] mCheckState;
    private AlertDialog mDialog;
    private String[] mNetworkTypeArray;
    private int mNetworkTypeNum;

    public NetworkTypePreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mActArray = new int[]{1, 4, 8};
        String[] strArr = {"2G", "3G"};
        if (TelephonyUtils.isUSIMCard(context, context instanceof Activity ? ((Activity) context).getIntent().getIntExtra("plmn_sub", -1) : -1) && PhoneFeatureConstants.FeatureOption.isMtkLteSupport()) {
            this.mNetworkTypeArray = new String[strArr.length + 1];
            for (int i = 0; i < strArr.length; i++) {
                this.mNetworkTypeArray[i] = strArr[i];
            }
            this.mNetworkTypeArray[this.mNetworkTypeArray.length - 1] = "4G";
        } else {
            this.mNetworkTypeArray = strArr;
        }
        if (this.mNetworkTypeArray != null) {
            this.mNetworkTypeNum = this.mNetworkTypeArray.length;
        }
        this.mCheckState = new boolean[this.mNetworkTypeNum];
    }

    public NetworkTypePreference(Context context) {
        this(context, null);
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);
        builder.setMultiChoiceItems(this.mNetworkTypeArray, this.mCheckState, this);
    }

    @Override
    protected void onDialogClosed(boolean z) {
        super.onDialogClosed(z);
        if (z) {
            this.mAct = getTypeCheckResult();
            callChangeListener(Integer.valueOf(this.mAct));
        } else {
            initCheckState(this.mAct);
        }
        this.mDialog = null;
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i, boolean z) {
        this.mCheckState[i] = z;
        this.mDialog = (AlertDialog) getDialog();
        int typeCheckResult = getTypeCheckResult();
        if (this.mDialog != null) {
            this.mDialog.getButton(-1).setEnabled(typeCheckResult != 0);
        }
    }

    private int getTypeCheckResult() {
        int i = 0;
        for (int i2 = 0; i2 < this.mNetworkTypeNum; i2++) {
            if (this.mCheckState[i2]) {
                i += this.mActArray[i2];
            }
        }
        Log.d("NetworkTypePreference", "act = " + i);
        return i;
    }

    public void initCheckState(int i) {
        Log.d("NetworkTypePreference", "init CheckState: " + i);
        if (i > 13 || i < 1) {
            return;
        }
        this.mAct = i;
        for (int i2 = 0; i2 < this.mNetworkTypeNum; i2++) {
            this.mCheckState[i2] = (this.mActArray[i2] & i) != 0;
        }
    }
}
