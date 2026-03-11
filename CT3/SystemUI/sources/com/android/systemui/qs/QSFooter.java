package com.android.systemui.qs;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.BenesseExtension;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.systemui.FontSizeUtils;
import com.android.systemui.R;
import com.android.systemui.statusbar.phone.QSTileHost;
import com.android.systemui.statusbar.phone.SystemUIDialog;
import com.android.systemui.statusbar.policy.SecurityController;

public class QSFooter implements View.OnClickListener, DialogInterface.OnClickListener {
    protected static final boolean DEBUG = Log.isLoggable("QSFooter", 3);
    private final Context mContext;
    private AlertDialog mDialog;
    private final ImageView mFooterIcon;
    private final TextView mFooterText;
    private int mFooterTextId;
    private Handler mHandler;
    private QSTileHost mHost;
    private boolean mIsIconVisible;
    private boolean mIsVisible;
    private final Handler mMainHandler;
    private final View mRootView;
    private SecurityController mSecurityController;
    private final Callback mCallback = new Callback(this, null);
    private final Runnable mUpdateDisplayState = new Runnable() {
        @Override
        public void run() {
            if (QSFooter.this.mFooterTextId != 0) {
                QSFooter.this.mFooterText.setText(QSFooter.this.mFooterTextId);
            }
            QSFooter.this.mRootView.setVisibility(QSFooter.this.mIsVisible ? 0 : 8);
            QSFooter.this.mFooterIcon.setVisibility(QSFooter.this.mIsIconVisible ? 0 : 4);
        }
    };

    public QSFooter(QSPanel qsPanel, Context context) {
        this.mRootView = LayoutInflater.from(context).inflate(R.layout.quick_settings_footer, (ViewGroup) qsPanel, false);
        this.mRootView.setOnClickListener(this);
        this.mFooterText = (TextView) this.mRootView.findViewById(R.id.footer_text);
        this.mFooterIcon = (ImageView) this.mRootView.findViewById(R.id.footer_icon);
        this.mContext = context;
        this.mMainHandler = new Handler();
    }

    public void setHost(QSTileHost host) {
        this.mHost = host;
        this.mSecurityController = host.getSecurityController();
        this.mHandler = new H(this, host.getLooper(), null);
    }

    public void setListening(boolean listening) {
        if (listening) {
            this.mSecurityController.addCallback(this.mCallback);
        } else {
            this.mSecurityController.removeCallback(this.mCallback);
        }
    }

    public void onConfigurationChanged() {
        FontSizeUtils.updateFontSize(this.mFooterText, R.dimen.qs_tile_text_size);
    }

    public View getView() {
        return this.mRootView;
    }

    @Override
    public void onClick(View v) {
        this.mHandler.sendEmptyMessage(0);
    }

    public void handleClick() {
        this.mHost.collapsePanels();
        createDialog();
    }

    public void refreshState() {
        this.mHandler.sendEmptyMessage(1);
    }

    public void handleRefreshState() {
        this.mIsIconVisible = this.mSecurityController.isVpnEnabled();
        if (this.mSecurityController.isDeviceManaged()) {
            this.mFooterTextId = R.string.device_owned_footer;
            this.mIsVisible = true;
        } else {
            this.mFooterTextId = R.string.vpn_footer;
            this.mIsVisible = this.mIsIconVisible;
        }
        this.mMainHandler.post(this.mUpdateDisplayState);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which != -2 || BenesseExtension.getDchaState() != 0) {
            return;
        }
        Intent settingsIntent = new Intent("android.settings.VPN_SETTINGS");
        this.mHost.startActivityDismissingKeyguard(settingsIntent);
    }

    private void createDialog() {
        String deviceOwner = this.mSecurityController.getDeviceOwnerName();
        String profileOwner = this.mSecurityController.getProfileOwnerName();
        String primaryVpn = this.mSecurityController.getPrimaryVpnName();
        String profileVpn = this.mSecurityController.getProfileVpnName();
        boolean managed = this.mSecurityController.hasProfileOwner();
        this.mDialog = new SystemUIDialog(this.mContext);
        this.mDialog.setTitle(getTitle(deviceOwner));
        this.mDialog.setMessage(getMessage(deviceOwner, profileOwner, primaryVpn, profileVpn, managed));
        this.mDialog.setButton(-1, getPositiveButton(), this);
        if (this.mSecurityController.isVpnEnabled() && !this.mSecurityController.isVpnRestricted()) {
            this.mDialog.setButton(-2, getSettingsButton(), this);
        }
        this.mDialog.show();
    }

    private String getSettingsButton() {
        return this.mContext.getString(R.string.status_bar_settings_settings_button);
    }

    private String getPositiveButton() {
        return this.mContext.getString(R.string.quick_settings_done);
    }

    private String getMessage(String deviceOwner, String profileOwner, String primaryVpn, String profileVpn, boolean primaryUserIsManaged) {
        if (deviceOwner != null) {
            if (primaryVpn != null) {
                return this.mContext.getString(R.string.monitoring_description_vpn_app_device_owned, deviceOwner, primaryVpn);
            }
            return this.mContext.getString(R.string.monitoring_description_device_owned, deviceOwner);
        }
        if (primaryVpn != null) {
            if (profileVpn != null) {
                return this.mContext.getString(R.string.monitoring_description_app_personal_work, profileOwner, profileVpn, primaryVpn);
            }
            return this.mContext.getString(R.string.monitoring_description_app_personal, primaryVpn);
        }
        if (profileVpn != null) {
            return this.mContext.getString(R.string.monitoring_description_app_work, profileOwner, profileVpn);
        }
        if (profileOwner == null || !primaryUserIsManaged) {
            return null;
        }
        return this.mContext.getString(R.string.monitoring_description_device_owned, profileOwner);
    }

    private int getTitle(String deviceOwner) {
        if (deviceOwner != null) {
            return R.string.monitoring_title_device_owned;
        }
        return R.string.monitoring_title;
    }

    private class Callback implements SecurityController.SecurityControllerCallback {
        Callback(QSFooter this$0, Callback callback) {
            this();
        }

        private Callback() {
        }

        @Override
        public void onStateChanged() {
            QSFooter.this.refreshState();
        }
    }

    private class H extends Handler {
        H(QSFooter this$0, Looper looper, H h) {
            this(looper);
        }

        private H(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            String name = null;
            try {
                if (msg.what == 1) {
                    name = "handleRefreshState";
                    QSFooter.this.handleRefreshState();
                } else if (msg.what == 0) {
                    name = "handleClick";
                    QSFooter.this.handleClick();
                }
            } catch (Throwable t) {
                String error = "Error in " + name;
                Log.w("QSFooter", error, t);
                QSFooter.this.mHost.warn(error, t);
            }
        }
    }
}
