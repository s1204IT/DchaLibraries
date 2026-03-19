package com.mediatek.phone;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.view.ContextThemeWrapper;
import com.android.phone.SubscriptionInfoHelper;
import com.android.services.telephony.Log;
import com.mediatek.services.telephony.MtkTelephonyConnectionServiceUtil;
import java.util.ArrayList;

public class MtkSimErrorDialog extends AlertDialog implements DialogInterface.OnClickListener {
    public IntentFilter mIntentFilter;
    public BroadcastReceiver mReceiver;

    public MtkSimErrorDialog(Context context, ArrayList<String> arrayList) {
        super(new ContextThemeWrapper(context, (Resources.Theme) null));
        this.mIntentFilter = new IntentFilter("android.intent.action.CLOSE_SYSTEM_DIALOGS");
        this.mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                String stringExtra;
                String action = intent.getAction();
                Log.d(this, "onReceive: cancel the request dialog. action = " + action, new Object[0]);
                if (action.equals("android.intent.action.CLOSE_SYSTEM_DIALOGS") && (stringExtra = intent.getStringExtra("reason")) != null && stringExtra.equals("homekey")) {
                    MtkSimErrorDialog.this.dismiss();
                }
            }
        };
        getWindow().setType(2002);
        if (arrayList == null || arrayList.size() != 4) {
            Log.d(this, "Finish this with illegle dialog information : ", arrayList);
            return;
        }
        setTitle(arrayList.get(0));
        setMessage(arrayList.get(1));
        setButton(-1, arrayList.get(2), this);
        setButton(-2, arrayList.get(3), this);
        setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                MtkTelephonyConnectionServiceUtil.getInstance().cellConnMgrShowAlertingFinalize();
                MtkSimErrorDialog.this.setMtkSimErrorDialog(null);
                Log.d(MtkSimErrorDialog.this, "SimErrorDialog dismissed: " + MtkSimErrorDialog.this, new Object[0]);
            }
        });
        setMtkSimErrorDialog(this);
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        switch (i) {
            case -2:
                MtkTelephonyConnectionServiceUtil.getInstance().cellConnMgrShowAlertingFinalize();
                Log.d(this, "SimErrorDialog onClick cancel.", new Object[0]);
                break;
            case SubscriptionInfoHelper.NO_SUB_ID:
                if (MtkTelephonyConnectionServiceUtil.getInstance().isCellConnMgrAlive()) {
                    MtkTelephonyConnectionServiceUtil.getInstance().cellConnMgrHandleEvent();
                }
                Log.d(this, "SimErrorDialog onClick ok.", new Object[0]);
                break;
            default:
                Log.d(this, "SimErrorDialog onClick.", new Object[0]);
                break;
        }
    }

    private void setMtkSimErrorDialog(MtkSimErrorDialog mtkSimErrorDialog) {
        MtkTelephonyConnectionServiceUtil.getInstance().cellConnMgrSetSimErrorDialogActivity(mtkSimErrorDialog);
    }

    @Override
    protected void onStart() {
        super.onStart();
        getContext().registerReceiver(this.mReceiver, this.mIntentFilter);
    }

    @Override
    protected void onStop() {
        super.onStop();
        getContext().unregisterReceiver(this.mReceiver);
    }
}
