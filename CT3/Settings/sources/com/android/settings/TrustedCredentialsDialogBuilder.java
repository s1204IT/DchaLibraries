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
import java.util.List;
import java.util.function.IntConsumer;

class TrustedCredentialsDialogBuilder extends AlertDialog.Builder {
    private final DialogEventHandler mDialogEventHandler;

    public interface DelegateInterface {
        List<X509Certificate> getX509CertsFromCertHolder(TrustedCredentialsSettings.CertHolder certHolder);

        void removeOrInstallCert(TrustedCredentialsSettings.CertHolder certHolder);

        boolean startConfirmCredentialIfNotConfirmed(int i, IntConsumer intConsumer);
    }

    public TrustedCredentialsDialogBuilder(Activity activity, DelegateInterface delegate) {
        super(activity);
        this.mDialogEventHandler = new DialogEventHandler(activity, delegate);
        initDefaultBuilderParams();
    }

    public TrustedCredentialsDialogBuilder setCertHolder(TrustedCredentialsSettings.CertHolder certHolder) {
        return setCertHolders(certHolder == null ? new TrustedCredentialsSettings.CertHolder[0] : new TrustedCredentialsSettings.CertHolder[]{certHolder});
    }

    public TrustedCredentialsDialogBuilder setCertHolders(TrustedCredentialsSettings.CertHolder[] certHolders) {
        this.mDialogEventHandler.setCertHolders(certHolders);
        return this;
    }

    @Override
    public AlertDialog create() {
        AlertDialog dialog = super.create();
        dialog.setOnShowListener(this.mDialogEventHandler);
        this.mDialogEventHandler.setDialog(dialog);
        return dialog;
    }

    private void initDefaultBuilderParams() {
        setTitle(android.R.string.ime_action_previous);
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

        public DialogEventHandler(Activity activity, DelegateInterface delegate) {
            this.mActivity = activity;
            this.mDpm = (DevicePolicyManager) activity.getSystemService(DevicePolicyManager.class);
            this.mUserManager = (UserManager) activity.getSystemService(UserManager.class);
            this.mDelegate = delegate;
            this.mRootContainer = new LinearLayout(this.mActivity);
            this.mRootContainer.setOrientation(1);
        }

        public void setDialog(AlertDialog dialog) {
            this.mDialog = dialog;
        }

        public void setCertHolders(TrustedCredentialsSettings.CertHolder[] certHolder) {
            this.mCertHolders = certHolder;
        }

        @Override
        public void onShow(DialogInterface dialogInterface) {
            nextOrDismiss();
        }

        @Override
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
            if (view != this.mNegativeButton) {
                return;
            }
            onClickRemove();
        }

        private void onClickOk() {
            nextOrDismiss();
        }

        private void onClickTrust() {
            TrustedCredentialsSettings.CertHolder certHolder = getCurrentCertInfo();
            if (this.mDelegate.startConfirmCredentialIfNotConfirmed(certHolder.getUserId(), new IntConsumer() {
                @Override
                public void accept(int arg0) {
                    DialogEventHandler.this.m543xbf23ed93(arg0);
                }
            })) {
                return;
            }
            this.mDpm.approveCaCert(certHolder.getAlias(), certHolder.getUserId(), true);
            nextOrDismiss();
        }

        private void onClickRemove() {
            final TrustedCredentialsSettings.CertHolder certHolder = getCurrentCertInfo();
            new AlertDialog.Builder(this.mActivity).setMessage(getButtonConfirmation(certHolder)).setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    DialogEventHandler.this.mDelegate.removeOrInstallCert(certHolder);
                    dialog.dismiss();
                    DialogEventHandler.this.nextOrDismiss();
                }
            }).setNegativeButton(android.R.string.no, (DialogInterface.OnClickListener) null).show();
        }

        public void m543xbf23ed93(int userId) {
            if (!this.mDialog.isShowing() || !this.mNeedsApproval || getCurrentCertInfo() == null || getCurrentCertInfo().getUserId() != userId) {
                return;
            }
            onClickTrust();
        }

        private TrustedCredentialsSettings.CertHolder getCurrentCertInfo() {
            if (this.mCurrentCertIndex < this.mCertHolders.length) {
                return this.mCertHolders[this.mCurrentCertIndex];
            }
            return null;
        }

        public void nextOrDismiss() {
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

        private boolean isUserSecure(int userId) {
            LockPatternUtils lockPatternUtils = new LockPatternUtils(this.mActivity);
            if (lockPatternUtils.isSecure(userId)) {
                return true;
            }
            UserInfo parentUser = this.mUserManager.getProfileParent(userId);
            if (parentUser == null) {
                return false;
            }
            return lockPatternUtils.isSecure(parentUser.id);
        }

        private void updatePositiveButton() {
            int i;
            boolean z = false;
            TrustedCredentialsSettings.CertHolder certHolder = getCurrentCertInfo();
            if (!certHolder.isSystemCert() && isUserSecure(certHolder.getUserId()) && !this.mDpm.isCaCertApproved(certHolder.getAlias(), certHolder.getUserId())) {
                z = true;
            }
            this.mNeedsApproval = z;
            boolean isProfileOrDeviceOwner = RestrictedLockUtils.getProfileOrDeviceOwner(this.mActivity, certHolder.getUserId()) != null;
            Activity activity = this.mActivity;
            if (!isProfileOrDeviceOwner && this.mNeedsApproval) {
                i = R.string.trusted_credentials_trust_label;
            } else {
                i = android.R.string.ok;
            }
            CharSequence displayText = activity.getText(i);
            this.mPositiveButton = updateButton(-1, displayText);
        }

        private void updateNegativeButton() {
            TrustedCredentialsSettings.CertHolder certHolder = getCurrentCertInfo();
            boolean showRemoveButton = !this.mUserManager.hasUserRestriction("no_config_credentials", new UserHandle(certHolder.getUserId()));
            CharSequence displayText = this.mActivity.getText(getButtonLabel(certHolder));
            this.mNegativeButton = updateButton(-2, displayText);
            this.mNegativeButton.setVisibility(showRemoveButton ? 0 : 8);
        }

        private Button updateButton(int buttonType, CharSequence displayText) {
            this.mDialog.setButton(buttonType, displayText, (DialogInterface.OnClickListener) null);
            Button button = this.mDialog.getButton(buttonType);
            button.setText(displayText);
            button.setOnClickListener(this);
            return button;
        }

        private void updateViewContainer() {
            TrustedCredentialsSettings.CertHolder certHolder = getCurrentCertInfo();
            LinearLayout nextCertLayout = getCertLayout(certHolder);
            if (this.mCurrentCertLayout == null) {
                this.mCurrentCertLayout = nextCertLayout;
                this.mRootContainer.addView(this.mCurrentCertLayout);
            } else {
                animateViewTransition(nextCertLayout);
            }
        }

        private LinearLayout getCertLayout(TrustedCredentialsSettings.CertHolder certHolder) {
            final ArrayList<View> views = new ArrayList<>();
            ArrayList<String> titles = new ArrayList<>();
            List<X509Certificate> certificates = this.mDelegate.getX509CertsFromCertHolder(certHolder);
            if (certificates != null) {
                for (X509Certificate certificate : certificates) {
                    SslCertificate sslCert = new SslCertificate(certificate);
                    views.add(sslCert.inflateCertificateView(this.mActivity));
                    titles.add(sslCert.getIssuedTo().getCName());
                }
            }
            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this.mActivity, android.R.layout.simple_spinner_item, titles);
            arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            Spinner spinner = new Spinner(this.mActivity);
            spinner.setAdapter((SpinnerAdapter) arrayAdapter);
            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    int i = 0;
                    while (i < views.size()) {
                        ((View) views.get(i)).setVisibility(i == position ? 0 : 8);
                        i++;
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
            LinearLayout certLayout = new LinearLayout(this.mActivity);
            certLayout.setOrientation(1);
            certLayout.addView(spinner);
            int i = 0;
            while (i < views.size()) {
                View certificateView = views.get(i);
                certificateView.setVisibility(i == 0 ? 0 : 8);
                certLayout.addView(certificateView);
                i++;
            }
            return certLayout;
        }

        private static int getButtonConfirmation(TrustedCredentialsSettings.CertHolder certHolder) {
            if (!certHolder.isSystemCert()) {
                return R.string.trusted_credentials_remove_confirmation;
            }
            if (certHolder.isDeleted()) {
                return R.string.trusted_credentials_enable_confirmation;
            }
            return R.string.trusted_credentials_disable_confirmation;
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

        private void animateViewTransition(final View nextCertView) {
            animateOldContent(new Runnable() {
                @Override
                public void run() {
                    DialogEventHandler.this.addAndAnimateNewContent(nextCertView);
                }
            });
        }

        private void animateOldContent(Runnable callback) {
            this.mCurrentCertLayout.animate().alpha(0.0f).setDuration(300L).setInterpolator(AnimationUtils.loadInterpolator(this.mActivity, android.R.interpolator.fast_out_linear_in)).withEndAction(callback).start();
        }

        public void addAndAnimateNewContent(View nextCertLayout) {
            this.mCurrentCertLayout = nextCertLayout;
            this.mRootContainer.removeAllViews();
            this.mRootContainer.addView(nextCertLayout);
            this.mRootContainer.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                @Override
                public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                    DialogEventHandler.this.mRootContainer.removeOnLayoutChangeListener(this);
                    int containerWidth = DialogEventHandler.this.mRootContainer.getWidth();
                    DialogEventHandler.this.mCurrentCertLayout.setTranslationX(containerWidth);
                    DialogEventHandler.this.mCurrentCertLayout.animate().translationX(0.0f).setInterpolator(AnimationUtils.loadInterpolator(DialogEventHandler.this.mActivity, android.R.interpolator.linear_out_slow_in)).setDuration(200L).start();
                }
            });
        }
    }
}
