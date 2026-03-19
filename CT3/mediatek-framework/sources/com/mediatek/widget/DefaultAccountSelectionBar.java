package com.mediatek.widget;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import com.mediatek.widget.CustomAccountRemoteViews;
import java.util.List;

public class DefaultAccountSelectionBar {
    public static final String SELECT_OTHER_ACCOUNTS_ACTION = "SELECT_OTHER_ACCOUNTS";
    private static final String TAG = "DefaultAccountSelectionBar";
    private Context mContext;
    private CustomAccountRemoteViews mCustomAccountRemoteViews;
    private Notification mNotification;
    private NotificationManager mNotificationManager;
    private String mPackageName;
    private boolean mIsRegister = false;
    private BroadcastReceiver mReceiver = null;

    public DefaultAccountSelectionBar(Context context, String packageName, List<CustomAccountRemoteViews.AccountInfo> data) {
        this.mContext = context;
        this.mPackageName = packageName;
        configureView(data);
        this.mNotificationManager = (NotificationManager) this.mContext.getSystemService("notification");
        this.mNotification = new Notification.Builder(this.mContext).setSmallIcon(134348800).setWhen(System.currentTimeMillis()).setPriority(2).build();
        this.mNotification.flags = 34;
    }

    public void updateData(List<CustomAccountRemoteViews.AccountInfo> data) {
        configureView(data);
    }

    public void show() {
        this.mNotification.contentView = this.mCustomAccountRemoteViews.getNormalRemoteViews();
        this.mNotification.bigContentView = this.mCustomAccountRemoteViews.getBigRemoteViews();
        this.mNotificationManager.notify(135331910, this.mNotification);
        Log.d(TAG, "In package show accountBar: " + this.mPackageName + " ,mIsRegister: " + this.mIsRegister);
        if (this.mIsRegister || this.mCustomAccountRemoteViews.getOtherAccounts() == null) {
            return;
        }
        registerReceiver(this.mContext.getApplicationContext());
        this.mIsRegister = true;
    }

    public void hide() {
        this.mNotificationManager.cancel(135331910);
        Log.d(TAG, "In package hide accountBar: " + this.mPackageName + " ,mIsRegister: " + this.mIsRegister);
        if (!this.mIsRegister || this.mCustomAccountRemoteViews.getOtherAccounts() == null) {
            return;
        }
        unregisterReceiver(this.mContext.getApplicationContext());
        this.mIsRegister = false;
    }

    private void configureView(List<CustomAccountRemoteViews.AccountInfo> data) {
        this.mCustomAccountRemoteViews = new CustomAccountRemoteViews(this.mContext, this.mPackageName, data);
        this.mCustomAccountRemoteViews.configureView();
    }

    private void registerReceiver(Context context) {
        OtherAccountSelectionReceiver otherAccountSelectionReceiver = null;
        if (this.mReceiver == null) {
            this.mReceiver = new OtherAccountSelectionReceiver(this, otherAccountSelectionReceiver);
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(SELECT_OTHER_ACCOUNTS_ACTION);
        context.registerReceiver(this.mReceiver, intentFilter);
    }

    private void unregisterReceiver(Context context) {
        if (this.mReceiver != null) {
            context.unregisterReceiver(this.mReceiver);
        }
        this.mReceiver = null;
    }

    private void hideNotification(Context context) {
        Intent intent = new Intent("android.intent.action.CLOSE_SYSTEM_DIALOGS");
        context.sendBroadcast(intent);
    }

    private class OtherAccountSelectionReceiver extends BroadcastReceiver {
        OtherAccountSelectionReceiver(DefaultAccountSelectionBar this$0, OtherAccountSelectionReceiver otherAccountSelectionReceiver) {
            this();
        }

        private OtherAccountSelectionReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(DefaultAccountSelectionBar.TAG, "[onReceive] action = " + action);
            List<CustomAccountRemoteViews.AccountInfo> accountItems = DefaultAccountSelectionBar.this.mCustomAccountRemoteViews.getOtherAccounts();
            if (DefaultAccountSelectionBar.SELECT_OTHER_ACCOUNTS_ACTION.equals(action) && (DefaultAccountSelectionBar.this.mContext instanceof Activity)) {
                if (((Activity) DefaultAccountSelectionBar.this.mContext).isFinishing() || ((Activity) DefaultAccountSelectionBar.this.mContext).isDestroyed()) {
                    Log.d(DefaultAccountSelectionBar.TAG, "--- wrong activity status ---");
                    return;
                }
                FragmentManager fm = ((Activity) DefaultAccountSelectionBar.this.mContext).getFragmentManager();
                if (fm.findFragmentByTag(DefaultAccountPickerDialog.TAG) == null) {
                    DefaultAccountPickerDialog defaultAccountPickerDialog = DefaultAccountPickerDialog.build(DefaultAccountSelectionBar.this.mContext).setData(accountItems);
                    FragmentTransaction ft = ((Activity) DefaultAccountSelectionBar.this.mContext).getFragmentManager().beginTransaction();
                    ft.add(defaultAccountPickerDialog, DefaultAccountPickerDialog.TAG);
                    ft.commitAllowingStateLoss();
                }
            } else {
                Log.d(DefaultAccountSelectionBar.TAG, "--- wrong context ---");
            }
            DefaultAccountSelectionBar.this.hideNotification(DefaultAccountSelectionBar.this.mContext);
        }
    }
}
