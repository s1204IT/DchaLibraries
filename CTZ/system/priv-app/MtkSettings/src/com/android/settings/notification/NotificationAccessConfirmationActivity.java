package com.android.settings.notification;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Slog;
import android.view.accessibility.AccessibilityEvent;
import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;
import com.android.settings.R;

/* loaded from: classes.dex */
public class NotificationAccessConfirmationActivity extends Activity implements DialogInterface {
    private ComponentName mComponentName;
    private NotificationManager mNm;
    private int mUserId;

    @Override // android.app.Activity
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        getWindow().addPrivateFlags(524288);
        this.mNm = (NotificationManager) getSystemService("notification");
        this.mComponentName = (ComponentName) getIntent().getParcelableExtra("component_name");
        this.mUserId = getIntent().getIntExtra("user_id", -10000);
        String stringExtra = getIntent().getStringExtra("package_title");
        AlertController.AlertParams alertParams = new AlertController.AlertParams(this);
        alertParams.mTitle = getString(R.string.notification_listener_security_warning_title, new Object[]{stringExtra});
        alertParams.mMessage = getString(R.string.notification_listener_security_warning_summary, new Object[]{stringExtra});
        alertParams.mPositiveButtonText = getString(R.string.allow);
        alertParams.mPositiveButtonListener = new DialogInterface.OnClickListener() { // from class: com.android.settings.notification.-$$Lambda$NotificationAccessConfirmationActivity$UvveyFMEwlZ6m4ViLmcVExulBE8
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                this.f$0.onAllow();
            }
        };
        alertParams.mNegativeButtonText = getString(R.string.deny);
        alertParams.mNegativeButtonListener = new DialogInterface.OnClickListener() { // from class: com.android.settings.notification.-$$Lambda$NotificationAccessConfirmationActivity$hd7i7CSD_dVpjvK__hXE8eDM2I0
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                this.f$0.cancel();
            }
        };
        AlertController.create(this, this, getWindow()).installContent(alertParams);
        getWindow().setCloseOnTouchOutside(false);
    }

    @Override // android.app.Activity
    public void onResume() {
        super.onResume();
        getWindow().addFlags(524288);
    }

    @Override // android.app.Activity
    public void onPause() {
        getWindow().clearFlags(524288);
        super.onPause();
    }

    private void onAllow() {
        try {
            if (!"android.permission.BIND_NOTIFICATION_LISTENER_SERVICE".equals(getPackageManager().getServiceInfo(this.mComponentName, 0).permission)) {
                Slog.e("NotificationAccessConfirmationActivity", "Service " + this.mComponentName + " lacks permission android.permission.BIND_NOTIFICATION_LISTENER_SERVICE");
                return;
            }
            this.mNm.setNotificationListenerAccessGranted(this.mComponentName, true);
            finish();
        } catch (PackageManager.NameNotFoundException e) {
            Slog.e("NotificationAccessConfirmationActivity", "Failed to get service info for " + this.mComponentName, e);
        }
    }

    @Override // android.app.Activity, android.view.Window.Callback
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        return AlertActivity.dispatchPopulateAccessibilityEvent(this, accessibilityEvent);
    }

    @Override // android.app.Activity
    public void onBackPressed() {
    }

    @Override // android.content.DialogInterface
    public void cancel() {
        finish();
    }

    @Override // android.content.DialogInterface
    public void dismiss() {
        if (!isFinishing()) {
            finish();
        }
    }
}
