package com.android.systemui.qs;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.UserInfo;
import android.os.BenesseExtension;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.UserManager;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.systemui.Dependency;
import com.android.systemui.FontSizeUtils;
import com.android.systemui.R;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.statusbar.phone.SystemUIDialog;
import com.android.systemui.statusbar.policy.SecurityController;

/* loaded from: classes.dex */
public class QSSecurityFooter implements DialogInterface.OnClickListener, View.OnClickListener {
    protected static final boolean DEBUG = Log.isLoggable("QSSecurityFooter", 3);
    private final ActivityStarter mActivityStarter;
    private final Context mContext;
    private AlertDialog mDialog;
    private final View mDivider;
    private final ImageView mFooterIcon;
    private int mFooterIconId;
    private final TextView mFooterText;
    protected H mHandler;
    private QSTileHost mHost;
    private boolean mIsVisible;
    private final Handler mMainHandler;
    private final View mRootView;
    private final SecurityController mSecurityController;
    private final UserManager mUm;
    private final Callback mCallback = new Callback();
    private CharSequence mFooterTextContent = null;
    private final Runnable mUpdateIcon = new Runnable() { // from class: com.android.systemui.qs.QSSecurityFooter.1
        AnonymousClass1() {
        }

        @Override // java.lang.Runnable
        public void run() {
            QSSecurityFooter.this.mFooterIcon.setImageResource(QSSecurityFooter.this.mFooterIconId);
        }
    };
    private final Runnable mUpdateDisplayState = new Runnable() { // from class: com.android.systemui.qs.QSSecurityFooter.2
        AnonymousClass2() {
        }

        @Override // java.lang.Runnable
        public void run() {
            if (QSSecurityFooter.this.mFooterTextContent != null) {
                QSSecurityFooter.this.mFooterText.setText(QSSecurityFooter.this.mFooterTextContent);
            }
            QSSecurityFooter.this.mRootView.setVisibility(QSSecurityFooter.this.mIsVisible ? 0 : 8);
            if (QSSecurityFooter.this.mDivider != null) {
                QSSecurityFooter.this.mDivider.setVisibility(QSSecurityFooter.this.mIsVisible ? 8 : 0);
            }
        }
    };

    public QSSecurityFooter(QSPanel qSPanel, Context context) {
        this.mRootView = LayoutInflater.from(context).inflate(R.layout.quick_settings_footer, (ViewGroup) qSPanel, false);
        this.mRootView.setOnClickListener(this);
        this.mFooterText = (TextView) this.mRootView.findViewById(R.id.footer_text);
        this.mFooterIcon = (ImageView) this.mRootView.findViewById(R.id.footer_icon);
        this.mFooterIconId = R.drawable.ic_info_outline;
        this.mContext = context;
        this.mMainHandler = new Handler(Looper.getMainLooper());
        this.mActivityStarter = (ActivityStarter) Dependency.get(ActivityStarter.class);
        this.mSecurityController = (SecurityController) Dependency.get(SecurityController.class);
        this.mHandler = new H((Looper) Dependency.get(Dependency.BG_LOOPER));
        this.mDivider = qSPanel != null ? qSPanel.getDivider() : null;
        this.mUm = (UserManager) this.mContext.getSystemService("user");
    }

    public void setHostEnvironment(QSTileHost qSTileHost) {
        this.mHost = qSTileHost;
    }

    public void setListening(boolean z) {
        if (z) {
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

    @Override // android.view.View.OnClickListener
    public void onClick(View view) {
        this.mHandler.sendEmptyMessage(0);
    }

    private void handleClick() {
        showDeviceMonitoringDialog();
    }

    public void showDeviceMonitoringDialog() {
        this.mHost.collapsePanels();
        createDialog();
    }

    public void refreshState() {
        this.mHandler.sendEmptyMessage(1);
    }

    private void handleRefreshState() {
        boolean zIsDeviceManaged = this.mSecurityController.isDeviceManaged();
        UserInfo userInfo = this.mUm.getUserInfo(ActivityManager.getCurrentUser());
        boolean z = UserManager.isDeviceInDemoMode(this.mContext) && userInfo != null && userInfo.isDemo();
        boolean zHasWorkProfile = this.mSecurityController.hasWorkProfile();
        boolean zHasCACertInCurrentUser = this.mSecurityController.hasCACertInCurrentUser();
        boolean zHasCACertInWorkProfile = this.mSecurityController.hasCACertInWorkProfile();
        boolean zIsNetworkLoggingEnabled = this.mSecurityController.isNetworkLoggingEnabled();
        String primaryVpnName = this.mSecurityController.getPrimaryVpnName();
        String workProfileVpnName = this.mSecurityController.getWorkProfileVpnName();
        CharSequence deviceOwnerOrganizationName = this.mSecurityController.getDeviceOwnerOrganizationName();
        CharSequence workProfileOrganizationName = this.mSecurityController.getWorkProfileOrganizationName();
        this.mIsVisible = (zIsDeviceManaged && !z) || zHasCACertInCurrentUser || zHasCACertInWorkProfile || primaryVpnName != null || workProfileVpnName != null;
        this.mFooterTextContent = getFooterText(zIsDeviceManaged, zHasWorkProfile, zHasCACertInCurrentUser, zHasCACertInWorkProfile, zIsNetworkLoggingEnabled, primaryVpnName, workProfileVpnName, deviceOwnerOrganizationName, workProfileOrganizationName);
        int i = R.drawable.ic_info_outline;
        if (primaryVpnName != null || workProfileVpnName != null) {
            if (this.mSecurityController.isVpnBranded()) {
                i = R.drawable.ic_qs_branded_vpn;
            } else {
                i = R.drawable.ic_qs_vpn;
            }
        }
        if (this.mFooterIconId != i) {
            this.mFooterIconId = i;
            this.mMainHandler.post(this.mUpdateIcon);
        }
        this.mMainHandler.post(this.mUpdateDisplayState);
    }

    protected CharSequence getFooterText(boolean z, boolean z2, boolean z3, boolean z4, boolean z5, String str, String str2, CharSequence charSequence, CharSequence charSequence2) {
        if (z) {
            if (z3 || z4 || z5) {
                return charSequence == null ? this.mContext.getString(R.string.quick_settings_disclosure_management_monitoring) : this.mContext.getString(R.string.quick_settings_disclosure_named_management_monitoring, charSequence);
            }
            if (str != null && str2 != null) {
                return charSequence == null ? this.mContext.getString(R.string.quick_settings_disclosure_management_vpns) : this.mContext.getString(R.string.quick_settings_disclosure_named_management_vpns, charSequence);
            }
            if (str == null && str2 == null) {
                return charSequence == null ? this.mContext.getString(R.string.quick_settings_disclosure_management) : this.mContext.getString(R.string.quick_settings_disclosure_named_management, charSequence);
            }
            if (charSequence == null) {
                Context context = this.mContext;
                Object[] objArr = new Object[1];
                if (str == null) {
                    str = str2;
                }
                objArr[0] = str;
                return context.getString(R.string.quick_settings_disclosure_management_named_vpn, objArr);
            }
            Context context2 = this.mContext;
            Object[] objArr2 = new Object[2];
            objArr2[0] = charSequence;
            if (str == null) {
                str = str2;
            }
            objArr2[1] = str;
            return context2.getString(R.string.quick_settings_disclosure_named_management_named_vpn, objArr2);
        }
        if (z4) {
            return charSequence2 == null ? this.mContext.getString(R.string.quick_settings_disclosure_managed_profile_monitoring) : this.mContext.getString(R.string.quick_settings_disclosure_named_managed_profile_monitoring, charSequence2);
        }
        if (z3) {
            return this.mContext.getString(R.string.quick_settings_disclosure_monitoring);
        }
        if (str != null && str2 != null) {
            return this.mContext.getString(R.string.quick_settings_disclosure_vpns);
        }
        if (str2 != null) {
            return this.mContext.getString(R.string.quick_settings_disclosure_managed_profile_named_vpn, str2);
        }
        if (str != null) {
            return z2 ? this.mContext.getString(R.string.quick_settings_disclosure_personal_profile_named_vpn, str) : this.mContext.getString(R.string.quick_settings_disclosure_named_vpn, str);
        }
        return null;
    }

    @Override // android.content.DialogInterface.OnClickListener
    public void onClick(DialogInterface dialogInterface, int i) {
        if (i == -2) {
            Intent intent = new Intent("android.settings.ENTERPRISE_PRIVACY_SETTINGS");
            this.mDialog.dismiss();
            if (BenesseExtension.getDchaState() != 0) {
                return;
            }
            this.mActivityStarter.postStartActivityDismissingKeyguard(intent, 0);
        }
    }

    private void createDialog() {
        boolean zIsDeviceManaged = this.mSecurityController.isDeviceManaged();
        boolean zHasWorkProfile = this.mSecurityController.hasWorkProfile();
        CharSequence deviceOwnerOrganizationName = this.mSecurityController.getDeviceOwnerOrganizationName();
        boolean zHasCACertInCurrentUser = this.mSecurityController.hasCACertInCurrentUser();
        boolean zHasCACertInWorkProfile = this.mSecurityController.hasCACertInWorkProfile();
        boolean zIsNetworkLoggingEnabled = this.mSecurityController.isNetworkLoggingEnabled();
        String primaryVpnName = this.mSecurityController.getPrimaryVpnName();
        String workProfileVpnName = this.mSecurityController.getWorkProfileVpnName();
        this.mDialog = new SystemUIDialog(this.mContext);
        this.mDialog.requestWindowFeature(1);
        View viewInflate = LayoutInflater.from(new ContextThemeWrapper(this.mContext, 2131886643)).inflate(R.layout.quick_settings_footer_dialog, (ViewGroup) null, false);
        this.mDialog.setView(viewInflate);
        this.mDialog.setButton(-1, getPositiveButton(), this);
        CharSequence managementMessage = getManagementMessage(zIsDeviceManaged, deviceOwnerOrganizationName);
        if (managementMessage != null) {
            viewInflate.findViewById(R.id.device_management_disclosures).setVisibility(0);
            ((TextView) viewInflate.findViewById(R.id.device_management_warning)).setText(managementMessage);
            this.mDialog.setButton(-2, getSettingsButton(), this);
        } else {
            viewInflate.findViewById(R.id.device_management_disclosures).setVisibility(8);
        }
        CharSequence caCertsMessage = getCaCertsMessage(zIsDeviceManaged, zHasCACertInCurrentUser, zHasCACertInWorkProfile);
        if (caCertsMessage != null) {
            viewInflate.findViewById(R.id.ca_certs_disclosures).setVisibility(0);
            TextView textView = (TextView) viewInflate.findViewById(R.id.ca_certs_warning);
            textView.setText(caCertsMessage);
            textView.setMovementMethod(new LinkMovementMethod());
        } else {
            viewInflate.findViewById(R.id.ca_certs_disclosures).setVisibility(8);
        }
        CharSequence networkLoggingMessage = getNetworkLoggingMessage(zIsNetworkLoggingEnabled);
        if (networkLoggingMessage != null) {
            viewInflate.findViewById(R.id.network_logging_disclosures).setVisibility(0);
            ((TextView) viewInflate.findViewById(R.id.network_logging_warning)).setText(networkLoggingMessage);
        } else {
            viewInflate.findViewById(R.id.network_logging_disclosures).setVisibility(8);
        }
        CharSequence vpnMessage = getVpnMessage(zIsDeviceManaged, zHasWorkProfile, primaryVpnName, workProfileVpnName);
        if (vpnMessage != null) {
            viewInflate.findViewById(R.id.vpn_disclosures).setVisibility(0);
            TextView textView2 = (TextView) viewInflate.findViewById(R.id.vpn_warning);
            textView2.setText(vpnMessage);
            textView2.setMovementMethod(new LinkMovementMethod());
        } else {
            viewInflate.findViewById(R.id.vpn_disclosures).setVisibility(8);
        }
        configSubtitleVisibility(managementMessage != null, caCertsMessage != null, networkLoggingMessage != null, vpnMessage != null, viewInflate);
        this.mDialog.show();
        this.mDialog.getWindow().setLayout(-1, -2);
    }

    protected void configSubtitleVisibility(boolean z, boolean z2, boolean z3, boolean z4, View view) {
        if (z) {
            return;
        }
        int i = 0;
        if (z2) {
            i = 1;
        }
        if (z3) {
            i++;
        }
        if (z4) {
            i++;
        }
        if (i != 1) {
            return;
        }
        if (z2) {
            view.findViewById(R.id.ca_certs_subtitle).setVisibility(8);
        }
        if (z3) {
            view.findViewById(R.id.network_logging_subtitle).setVisibility(8);
        }
        if (z4) {
            view.findViewById(R.id.vpn_subtitle).setVisibility(8);
        }
    }

    private String getSettingsButton() {
        return this.mContext.getString(R.string.monitoring_button_view_policies);
    }

    private String getPositiveButton() {
        return this.mContext.getString(R.string.ok);
    }

    protected CharSequence getManagementMessage(boolean z, CharSequence charSequence) {
        if (z) {
            return charSequence != null ? this.mContext.getString(R.string.monitoring_description_named_management, charSequence) : this.mContext.getString(R.string.monitoring_description_management);
        }
        return null;
    }

    protected CharSequence getCaCertsMessage(boolean z, boolean z2, boolean z3) {
        if (!z2 && !z3) {
            return null;
        }
        if (z) {
            return this.mContext.getString(R.string.monitoring_description_management_ca_certificate);
        }
        if (z3) {
            return this.mContext.getString(R.string.monitoring_description_managed_profile_ca_certificate);
        }
        return this.mContext.getString(R.string.monitoring_description_ca_certificate);
    }

    protected CharSequence getNetworkLoggingMessage(boolean z) {
        if (z) {
            return this.mContext.getString(R.string.monitoring_description_management_network_logging);
        }
        return null;
    }

    protected CharSequence getVpnMessage(boolean z, boolean z2, String str, String str2) {
        if (str == null && str2 == null) {
            return null;
        }
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder();
        if (z) {
            if (str == null || str2 == null) {
                Context context = this.mContext;
                Object[] objArr = new Object[1];
                if (str == null) {
                    str = str2;
                }
                objArr[0] = str;
                spannableStringBuilder.append((CharSequence) context.getString(R.string.monitoring_description_named_vpn, objArr));
            } else {
                spannableStringBuilder.append((CharSequence) this.mContext.getString(R.string.monitoring_description_two_named_vpns, str, str2));
            }
        } else if (str != null && str2 != null) {
            spannableStringBuilder.append((CharSequence) this.mContext.getString(R.string.monitoring_description_two_named_vpns, str, str2));
        } else if (str2 != null) {
            spannableStringBuilder.append((CharSequence) this.mContext.getString(R.string.monitoring_description_managed_profile_named_vpn, str2));
        } else if (z2) {
            spannableStringBuilder.append((CharSequence) this.mContext.getString(R.string.monitoring_description_personal_profile_named_vpn, str));
        } else {
            spannableStringBuilder.append((CharSequence) this.mContext.getString(R.string.monitoring_description_named_vpn, str));
        }
        spannableStringBuilder.append((CharSequence) this.mContext.getString(R.string.monitoring_description_vpn_settings_separator));
        spannableStringBuilder.append(this.mContext.getString(R.string.monitoring_description_vpn_settings), new VpnSpan(), 0);
        return spannableStringBuilder;
    }

    /* renamed from: com.android.systemui.qs.QSSecurityFooter$1 */
    class AnonymousClass1 implements Runnable {
        AnonymousClass1() {
        }

        @Override // java.lang.Runnable
        public void run() {
            QSSecurityFooter.this.mFooterIcon.setImageResource(QSSecurityFooter.this.mFooterIconId);
        }
    }

    /* renamed from: com.android.systemui.qs.QSSecurityFooter$2 */
    class AnonymousClass2 implements Runnable {
        AnonymousClass2() {
        }

        @Override // java.lang.Runnable
        public void run() {
            if (QSSecurityFooter.this.mFooterTextContent != null) {
                QSSecurityFooter.this.mFooterText.setText(QSSecurityFooter.this.mFooterTextContent);
            }
            QSSecurityFooter.this.mRootView.setVisibility(QSSecurityFooter.this.mIsVisible ? 0 : 8);
            if (QSSecurityFooter.this.mDivider != null) {
                QSSecurityFooter.this.mDivider.setVisibility(QSSecurityFooter.this.mIsVisible ? 8 : 0);
            }
        }
    }

    private class Callback implements SecurityController.SecurityControllerCallback {
        private Callback() {
        }

        /* synthetic */ Callback(QSSecurityFooter qSSecurityFooter, AnonymousClass1 anonymousClass1) {
            this();
        }

        @Override // com.android.systemui.statusbar.policy.SecurityController.SecurityControllerCallback
        public void onStateChanged() {
            QSSecurityFooter.this.refreshState();
        }
    }

    private class H extends Handler {
        /* synthetic */ H(QSSecurityFooter qSSecurityFooter, Looper looper, AnonymousClass1 anonymousClass1) {
            this(looper);
        }

        private H(Looper looper) {
            super(looper);
        }

        /* JADX DEBUG: Failed to insert an additional move for type inference into block B:44:0x0005 */
        /* JADX WARN: Multi-variable type inference failed */
        /* JADX WARN: Type inference failed for: r0v0 */
        /* JADX WARN: Type inference failed for: r0v1, types: [java.lang.String] */
        /* JADX WARN: Type inference failed for: r0v4 */
        /* JADX WARN: Type inference failed for: r1v0, types: [java.lang.StringBuilder] */
        /* JADX WARN: Type inference failed for: r5v6, types: [java.lang.String] */
        /* JADX WARN: Type inference failed for: r5v8, types: [java.lang.String] */
        @Override // android.os.Handler
        public void handleMessage(Message message) {
            ?? r0 = null;
            try {
            } catch (Throwable th) {
                th = th;
            }
            try {
                if (message.what == 1) {
                    ?? r5 = "handleRefreshState";
                    QSSecurityFooter.this.handleRefreshState();
                    message = r5;
                } else if (message.what == 0) {
                    ?? r52 = "handleClick";
                    QSSecurityFooter.this.handleClick();
                    message = r52;
                }
            } catch (Throwable th2) {
                r0 = message;
                th = th2;
                String str = "Error in " + r0;
                Log.w("QSSecurityFooter", str, th);
                QSSecurityFooter.this.mHost.warn(str, th);
            }
        }
    }

    protected class VpnSpan extends ClickableSpan {
        protected VpnSpan() {
        }

        @Override // android.text.style.ClickableSpan
        public void onClick(View view) {
            Intent intent = new Intent("android.settings.VPN_SETTINGS");
            QSSecurityFooter.this.mDialog.dismiss();
            if (BenesseExtension.getDchaState() == 0) {
                QSSecurityFooter.this.mActivityStarter.postStartActivityDismissingKeyguard(intent, 0);
            }
        }

        public boolean equals(Object obj) {
            return obj instanceof VpnSpan;
        }

        public int hashCode() {
            return 314159257;
        }
    }
}
