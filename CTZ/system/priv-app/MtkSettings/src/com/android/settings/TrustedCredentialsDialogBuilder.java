package com.android.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.DialogInterface;
import android.content.pm.UserInfo;
import android.net.http.SslCertificate;
import android.os.UserHandle;
import android.os.UserManager;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import com.android.internal.widget.LockPatternUtils;
import com.android.settings.TrustedCredentialsSettings;
import com.android.settingslib.RestrictedLockUtils;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.IntConsumer;

/* loaded from: classes.dex */
class TrustedCredentialsDialogBuilder extends AlertDialog.Builder {
    private final DialogEventHandler mDialogEventHandler;

    public interface DelegateInterface {
        List<X509Certificate> getX509CertsFromCertHolder(TrustedCredentialsSettings.CertHolder certHolder);

        void removeOrInstallCert(TrustedCredentialsSettings.CertHolder certHolder);

        boolean startConfirmCredentialIfNotConfirmed(int i, IntConsumer intConsumer);
    }

    public TrustedCredentialsDialogBuilder(Activity activity, DelegateInterface delegateInterface) {
        super(activity);
        this.mDialogEventHandler = new DialogEventHandler(activity, delegateInterface);
        initDefaultBuilderParams();
    }

    public TrustedCredentialsDialogBuilder setCertHolder(TrustedCredentialsSettings.CertHolder certHolder) {
        TrustedCredentialsSettings.CertHolder[] certHolderArr;
        if (certHolder == null) {
            certHolderArr = new TrustedCredentialsSettings.CertHolder[0];
        } else {
            certHolderArr = new TrustedCredentialsSettings.CertHolder[]{certHolder};
        }
        return setCertHolders(certHolderArr);
    }

    public TrustedCredentialsDialogBuilder setCertHolders(TrustedCredentialsSettings.CertHolder[] certHolderArr) {
        this.mDialogEventHandler.setCertHolders(certHolderArr);
        return this;
    }

    @Override // android.app.AlertDialog.Builder
    public AlertDialog create() {
        AlertDialog alertDialogCreate = super.create();
        alertDialogCreate.setOnShowListener(this.mDialogEventHandler);
        this.mDialogEventHandler.setDialog(alertDialogCreate);
        return alertDialogCreate;
    }

    private void initDefaultBuilderParams() {
        setTitle(android.R.string.mime_type_compressed_ext);
        setView(this.mDialogEventHandler.mRootContainer);
        setPositiveButton(R.string.trusted_credentials_trust_label, (DialogInterface.OnClickListener) null);
        setNegativeButton(android.R.string.ok, (DialogInterface.OnClickListener) null);
    }

    private static class DialogEventHandler implements DialogInterface.OnShowListener, View.OnClickListener {
        private final Activity mActivity;
        private final DelegateInterface mDelegate;
        private AlertDialog mDialog;
        private final DevicePolicyManager mDpm;
        private boolean mNeedsApproval;
        private Button mNegativeButton;
        private Button mPositiveButton;
        private final LinearLayout mRootContainer;
        private final UserManager mUserManager;
        private int mCurrentCertIndex = -1;
        private TrustedCredentialsSettings.CertHolder[] mCertHolders = new TrustedCredentialsSettings.CertHolder[0];
        private View mCurrentCertLayout = null;

        public DialogEventHandler(Activity activity, DelegateInterface delegateInterface) {
            this.mActivity = activity;
            this.mDpm = (DevicePolicyManager) activity.getSystemService(DevicePolicyManager.class);
            this.mUserManager = (UserManager) activity.getSystemService(UserManager.class);
            this.mDelegate = delegateInterface;
            this.mRootContainer = new LinearLayout(this.mActivity);
            this.mRootContainer.setOrientation(1);
        }

        public void setDialog(AlertDialog alertDialog) {
            this.mDialog = alertDialog;
        }

        public void setCertHolders(TrustedCredentialsSettings.CertHolder[] certHolderArr) {
            this.mCertHolders = certHolderArr;
        }

        @Override // android.content.DialogInterface.OnShowListener
        public void onShow(DialogInterface dialogInterface) {
            nextOrDismiss();
        }

        @Override // android.view.View.OnClickListener
        public void onClick(View view) {
            if (view == this.mPositiveButton) {
                if (this.mNeedsApproval) {
                    onClickTrust();
                    return;
                } else {
                    onClickOk();
                    return;
                }
            }
            if (view == this.mNegativeButton) {
                onClickEnableOrDisable();
            }
        }

        private void onClickOk() {
            nextOrDismiss();
        }

        private void onClickTrust() {
            TrustedCredentialsSettings.CertHolder currentCertInfo = getCurrentCertInfo();
            if (!this.mDelegate.startConfirmCredentialIfNotConfirmed(currentCertInfo.getUserId(), new IntConsumer() { // from class: com.android.settings.-$$Lambda$TrustedCredentialsDialogBuilder$DialogEventHandler$l-T7FQ-tmXq7wOC1gAhDRR6fZzQ
                @Override // java.util.function.IntConsumer
                public final void accept(int i) {
                    this.f$0.onCredentialConfirmed(i);
                }
            })) {
                this.mDpm.approveCaCert(currentCertInfo.getAlias(), currentCertInfo.getUserId(), true);
                nextOrDismiss();
            }
        }

        private void onClickEnableOrDisable() {
            final TrustedCredentialsSettings.CertHolder currentCertInfo = getCurrentCertInfo();
            DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() { // from class: com.android.settings.TrustedCredentialsDialogBuilder.DialogEventHandler.1
                @Override // android.content.DialogInterface.OnClickListener
                public void onClick(DialogInterface dialogInterface, int i) {
                    DialogEventHandler.this.mDelegate.removeOrInstallCert(currentCertInfo);
                    DialogEventHandler.this.nextOrDismiss();
                }
            };
            if (currentCertInfo.isSystemCert()) {
                onClickListener.onClick(null, -1);
            } else {
                new AlertDialog.Builder(this.mActivity).setMessage(R.string.trusted_credentials_remove_confirmation).setPositiveButton(android.R.string.yes, onClickListener).setNegativeButton(android.R.string.no, (DialogInterface.OnClickListener) null).show();
            }
        }

        private void onCredentialConfirmed(int i) {
            if (this.mDialog.isShowing() && this.mNeedsApproval && getCurrentCertInfo() != null && getCurrentCertInfo().getUserId() == i) {
                onClickTrust();
            }
        }

        private TrustedCredentialsSettings.CertHolder getCurrentCertInfo() {
            if (this.mCurrentCertIndex < this.mCertHolders.length) {
                return this.mCertHolders[this.mCurrentCertIndex];
            }
            return null;
        }

        private void nextOrDismiss() {
            this.mCurrentCertIndex++;
            while (this.mCurrentCertIndex < this.mCertHolders.length && getCurrentCertInfo() == null) {
                this.mCurrentCertIndex++;
            }
            if (this.mCurrentCertIndex >= this.mCertHolders.length) {
                this.mDialog.dismiss();
                return;
            }
            updateViewContainer();
            updatePositiveButton();
            updateNegativeButton();
        }

        private boolean isUserSecure(int i) {
            LockPatternUtils lockPatternUtils = new LockPatternUtils(this.mActivity);
            if (lockPatternUtils.isSecure(i)) {
                return true;
            }
            UserInfo profileParent = this.mUserManager.getProfileParent(i);
            if (profileParent == null) {
                return false;
            }
            return lockPatternUtils.isSecure(profileParent.id);
        }

        private void updatePositiveButton() {
            int i;
            TrustedCredentialsSettings.CertHolder currentCertInfo = getCurrentCertInfo();
            this.mNeedsApproval = (currentCertInfo.isSystemCert() || !isUserSecure(currentCertInfo.getUserId()) || this.mDpm.isCaCertApproved(currentCertInfo.getAlias(), currentCertInfo.getUserId())) ? false : true;
            boolean z = RestrictedLockUtils.getProfileOrDeviceOwner(this.mActivity, currentCertInfo.getUserId()) != null;
            Activity activity = this.mActivity;
            if (!z && this.mNeedsApproval) {
                i = R.string.trusted_credentials_trust_label;
            } else {
                i = android.R.string.ok;
            }
            this.mPositiveButton = updateButton(-1, activity.getText(i));
        }

        private void updateNegativeButton() {
            TrustedCredentialsSettings.CertHolder currentCertInfo = getCurrentCertInfo();
            boolean z = !this.mUserManager.hasUserRestriction("no_config_credentials", new UserHandle(currentCertInfo.getUserId()));
            this.mNegativeButton = updateButton(-2, this.mActivity.getText(getButtonLabel(currentCertInfo)));
            this.mNegativeButton.setVisibility(z ? 0 : 8);
        }

        private Button updateButton(int i, CharSequence charSequence) {
            this.mDialog.setButton(i, charSequence, (DialogInterface.OnClickListener) null);
            Button button = this.mDialog.getButton(i);
            button.setText(charSequence);
            button.setOnClickListener(this);
            return button;
        }

        private void updateViewContainer() {
            LinearLayout certLayout = getCertLayout(getCurrentCertInfo());
            if (this.mCurrentCertLayout == null) {
                this.mCurrentCertLayout = certLayout;
                this.mRootContainer.addView(this.mCurrentCertLayout);
            } else {
                animateViewTransition(certLayout);
            }
        }

        private LinearLayout getCertLayout(TrustedCredentialsSettings.CertHolder certHolder) {
            final ArrayList arrayList = new ArrayList();
            ArrayList arrayList2 = new ArrayList();
            List<X509Certificate> x509CertsFromCertHolder = this.mDelegate.getX509CertsFromCertHolder(certHolder);
            if (x509CertsFromCertHolder != null) {
                Iterator<X509Certificate> it = x509CertsFromCertHolder.iterator();
                while (it.hasNext()) {
                    SslCertificate sslCertificate = new SslCertificate(it.next());
                    arrayList.add(sslCertificate.inflateCertificateView(this.mActivity));
                    arrayList2.add(sslCertificate.getIssuedTo().getCName());
                }
            }
            ArrayAdapter arrayAdapter = new ArrayAdapter(this.mActivity, android.R.layout.simple_spinner_item, arrayList2);
            arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            Spinner spinner = new Spinner(this.mActivity);
            spinner.setAdapter((SpinnerAdapter) arrayAdapter);
            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() { // from class: com.android.settings.TrustedCredentialsDialogBuilder.DialogEventHandler.2
                @Override // android.widget.AdapterView.OnItemSelectedListener
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long j) {
                    int i2 = 0;
                    while (i2 < arrayList.size()) {
                        ((View) arrayList.get(i2)).setVisibility(i2 == i ? 0 : 8);
                        i2++;
                    }
                }

                @Override // android.widget.AdapterView.OnItemSelectedListener
                public void onNothingSelected(AdapterView<?> adapterView) {
                }
            });
            LinearLayout linearLayout = new LinearLayout(this.mActivity);
            linearLayout.setOrientation(1);
            linearLayout.addView(spinner);
            int i = 0;
            while (i < arrayList.size()) {
                View view = (View) arrayList.get(i);
                view.setVisibility(i == 0 ? 0 : 8);
                linearLayout.addView(view);
                i++;
            }
            return linearLayout;
        }

        private static int getButtonLabel(TrustedCredentialsSettings.CertHolder certHolder) {
            if (!certHolder.isSystemCert()) {
                return R.string.trusted_credentials_remove_label;
            }
            if (certHolder.isDeleted()) {
                return R.string.trusted_credentials_enable_label;
            }
            return R.string.trusted_credentials_disable_label;
        }

        private void animateViewTransition(final View view) {
            animateOldContent(new Runnable() { // from class: com.android.settings.TrustedCredentialsDialogBuilder.DialogEventHandler.3
                @Override // java.lang.Runnable
                public void run() {
                    DialogEventHandler.this.addAndAnimateNewContent(view);
                }
            });
        }

        private void animateOldContent(Runnable runnable) {
            this.mCurrentCertLayout.animate().alpha(0.0f).setDuration(300L).setInterpolator(AnimationUtils.loadInterpolator(this.mActivity, android.R.interpolator.fast_out_linear_in)).withEndAction(runnable).start();
        }

        private void addAndAnimateNewContent(View view) {
            this.mCurrentCertLayout = view;
            this.mRootContainer.removeAllViews();
            this.mRootContainer.addView(view);
            this.mRootContainer.addOnLayoutChangeListener(new View.OnLayoutChangeListener() { // from class: com.android.settings.TrustedCredentialsDialogBuilder.DialogEventHandler.4
                @Override // android.view.View.OnLayoutChangeListener
                public void onLayoutChange(View view2, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
                    DialogEventHandler.this.mRootContainer.removeOnLayoutChangeListener(this);
                    DialogEventHandler.this.mCurrentCertLayout.setTranslationX(DialogEventHandler.this.mRootContainer.getWidth());
                    DialogEventHandler.this.mCurrentCertLayout.animate().translationX(0.0f).setInterpolator(AnimationUtils.loadInterpolator(DialogEventHandler.this.mActivity, android.R.interpolator.linear_out_slow_in)).setDuration(200L).start();
                }
            });
        }
    }
}
