package com.android.systemui.qs;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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

public class QSFooter implements DialogInterface.OnClickListener, View.OnClickListener {
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
    private final Callback mCallback = new Callback();
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
        this.mHandler = new H(host.getLooper());
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

    public boolean hasFooter() {
        return this.mRootView.getVisibility() != 8;
    }

    @Override
    public void onClick(View v) {
        this.mHandler.sendEmptyMessage(0);
    }

    private void handleClick() {
        this.mHost.collapsePanels();
        createDialog();
    }

    public void refreshState() {
        this.mHandler.sendEmptyMessage(1);
    }

    private void handleRefreshState() {
        if (this.mSecurityController.hasDeviceOwner()) {
            this.mFooterTextId = R.string.device_owned_footer;
            this.mIsVisible = true;
            this.mIsIconVisible = false;
        } else if (this.mSecurityController.hasProfileOwner()) {
            this.mFooterTextId = R.string.profile_owned_footer;
            this.mIsVisible = true;
            this.mIsIconVisible = false;
        } else if (this.mSecurityController.isVpnEnabled()) {
            this.mFooterTextId = R.string.vpn_footer;
            this.mIsVisible = true;
            this.mIsIconVisible = true;
        } else {
            this.mIsVisible = false;
            this.mIsIconVisible = false;
        }
        this.mMainHandler.post(this.mUpdateDisplayState);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == -2) {
            this.mSecurityController.disconnectFromVpn();
        }
    }

    private void createDialog() {
        this.mDialog = new SystemUIDialog(this.mContext);
        this.mDialog.setTitle(getTitle());
        this.mDialog.setMessage(getMessage());
        this.mDialog.setButton(-1, getPositiveButton(), this);
        if (this.mSecurityController.isVpnEnabled()) {
            this.mDialog.setButton(-2, getNegativeButton(), this);
        }
        this.mDialog.show();
    }

    private String getNegativeButton() {
        return this.mSecurityController.isLegacyVpn() ? this.mContext.getString(R.string.disconnect_vpn) : this.mContext.getString(R.string.disable_vpn);
    }

    private String getPositiveButton() {
        return this.mContext.getString(R.string.quick_settings_done);
    }

    private String getMessage() {
        return this.mSecurityController.hasDeviceOwner() ? this.mSecurityController.hasProfileOwner() ? this.mSecurityController.isVpnEnabled() ? this.mSecurityController.isLegacyVpn() ? this.mContext.getString(R.string.monitoring_description_legacy_vpn_device_and_profile_owned, this.mSecurityController.getDeviceOwnerName(), this.mSecurityController.getProfileOwnerName(), this.mSecurityController.getLegacyVpnName()) : this.mContext.getString(R.string.monitoring_description_vpn_device_and_profile_owned, this.mSecurityController.getDeviceOwnerName(), this.mSecurityController.getProfileOwnerName(), this.mSecurityController.getVpnApp()) : this.mContext.getString(R.string.monitoring_description_device_and_profile_owned, this.mSecurityController.getDeviceOwnerName(), this.mSecurityController.getProfileOwnerName()) : this.mSecurityController.isVpnEnabled() ? this.mSecurityController.isLegacyVpn() ? this.mContext.getString(R.string.monitoring_description_legacy_vpn_device_owned, this.mSecurityController.getDeviceOwnerName(), this.mSecurityController.getLegacyVpnName()) : this.mContext.getString(R.string.monitoring_description_vpn_device_owned, this.mSecurityController.getDeviceOwnerName(), this.mSecurityController.getVpnApp()) : this.mContext.getString(R.string.monitoring_description_device_owned, this.mSecurityController.getDeviceOwnerName()) : this.mSecurityController.hasProfileOwner() ? this.mSecurityController.isVpnEnabled() ? this.mSecurityController.isLegacyVpn() ? this.mContext.getString(R.string.monitoring_description_legacy_vpn_profile_owned, this.mSecurityController.getProfileOwnerName(), this.mSecurityController.getLegacyVpnName()) : this.mContext.getString(R.string.monitoring_description_vpn_profile_owned, this.mSecurityController.getProfileOwnerName(), this.mSecurityController.getVpnApp()) : this.mContext.getString(R.string.monitoring_description_profile_owned, this.mSecurityController.getProfileOwnerName()) : this.mSecurityController.isLegacyVpn() ? this.mContext.getString(R.string.monitoring_description_legacy_vpn, this.mSecurityController.getLegacyVpnName()) : this.mContext.getString(R.string.monitoring_description_vpn, this.mSecurityController.getVpnApp());
    }

    private int getTitle() {
        if (this.mSecurityController.hasDeviceOwner()) {
            return R.string.monitoring_title_device_owned;
        }
        if (this.mSecurityController.hasProfileOwner()) {
            return R.string.monitoring_title_profile_owned;
        }
        return R.string.monitoring_title;
    }

    private class Callback implements SecurityController.SecurityControllerCallback {
        private Callback() {
        }

        @Override
        public void onStateChanged() {
            QSFooter.this.refreshState();
        }
    }

    private class H extends Handler {
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
