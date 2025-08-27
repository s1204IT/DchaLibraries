package com.android.systemui.net;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.net.INetworkPolicyManager;
import android.net.NetworkTemplate;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import com.android.systemui.R;

/* loaded from: classes.dex */
public class NetworkOverLimitActivity extends Activity {
    @Override // android.app.Activity
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        final NetworkTemplate parcelableExtra = getIntent().getParcelableExtra("android.net.NETWORK_TEMPLATE");
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getLimitedDialogTitleForTemplate(parcelableExtra));
        builder.setMessage(R.string.data_usage_disabled_dialog);
        builder.setPositiveButton(android.R.string.ok, (DialogInterface.OnClickListener) null);
        builder.setNegativeButton(R.string.data_usage_disabled_dialog_enable, new DialogInterface.OnClickListener() { // from class: com.android.systemui.net.NetworkOverLimitActivity.1
            @Override // android.content.DialogInterface.OnClickListener
            public void onClick(DialogInterface dialogInterface, int i) {
                NetworkOverLimitActivity.this.snoozePolicy(parcelableExtra);
            }
        });
        AlertDialog alertDialogCreate = builder.create();
        alertDialogCreate.getWindow().setType(2003);
        alertDialogCreate.setOnDismissListener(new DialogInterface.OnDismissListener() { // from class: com.android.systemui.net.NetworkOverLimitActivity.2
            @Override // android.content.DialogInterface.OnDismissListener
            public void onDismiss(DialogInterface dialogInterface) {
                NetworkOverLimitActivity.this.finish();
            }
        });
        alertDialogCreate.show();
    }

    private void snoozePolicy(NetworkTemplate networkTemplate) {
        try {
            INetworkPolicyManager.Stub.asInterface(ServiceManager.getService("netpolicy")).snoozeLimit(networkTemplate);
        } catch (RemoteException e) {
            Log.w("NetworkOverLimitActivity", "problem snoozing network policy", e);
        }
    }

    private static int getLimitedDialogTitleForTemplate(NetworkTemplate networkTemplate) {
        if (networkTemplate.getMatchRule() == 1) {
            return R.string.data_usage_disabled_dialog_mobile_title;
        }
        return R.string.data_usage_disabled_dialog_title;
    }
}
