package com.android.settings.fingerprint;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.hardware.fingerprint.Fingerprint;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.os.UserHandle;
import android.os.UserManager;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.PreferenceViewHolder;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.SubSettings;
import com.android.settings.Utils;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settings.fingerprint.FingerprintAuthenticateSidecar;
import com.android.settings.fingerprint.FingerprintRemoveSidecar;
import com.android.settings.password.ChooseLockGeneric;
import com.android.settings.password.ChooseLockSettingsHelper;
import com.android.settings.utils.AnnotationSpan;
import com.android.settingslib.HelpUtils;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.TwoTargetPreference;
import com.android.settingslib.widget.FooterPreference;
import java.util.HashMap;
import java.util.List;

/* loaded from: classes.dex */
public class FingerprintSettings extends SubSettings {
    @Override // com.android.settings.SettingsActivity, android.app.Activity
    public Intent getIntent() {
        Intent intent = new Intent(super.getIntent());
        intent.putExtra(":settings:show_fragment", FingerprintSettingsFragment.class.getName());
        return intent;
    }

    @Override // com.android.settings.SubSettings, com.android.settings.SettingsActivity
    protected boolean isValidFragment(String str) {
        return FingerprintSettingsFragment.class.getName().equals(str);
    }

    @Override // com.android.settings.SettingsActivity, com.android.settingslib.drawer.SettingsDrawerActivity, android.app.Activity
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setTitle(getText(R.string.security_settings_fingerprint_preference_title));
    }

    public static class FingerprintSettingsFragment extends SettingsPreferenceFragment implements Preference.OnPreferenceChangeListener, FingerprintPreference.OnDeleteClickListener {
        private FingerprintAuthenticateSidecar mAuthenticateSidecar;
        private FingerprintManager mFingerprintManager;
        private HashMap<Integer, String> mFingerprintsRenaming;
        private Drawable mHighlightDrawable;
        private boolean mInFingerprintLockout;
        private boolean mLaunchedConfirm;
        private FingerprintRemoveSidecar mRemovalSidecar;
        private byte[] mToken;
        private int mUserId;
        FingerprintAuthenticateSidecar.Listener mAuthenticateListener = new FingerprintAuthenticateSidecar.Listener() { // from class: com.android.settings.fingerprint.FingerprintSettings.FingerprintSettingsFragment.1
            @Override // com.android.settings.fingerprint.FingerprintAuthenticateSidecar.Listener
            public void onAuthenticationSucceeded(FingerprintManager.AuthenticationResult authenticationResult) {
                FingerprintSettingsFragment.this.mHandler.obtainMessage(1001, authenticationResult.getFingerprint().getFingerId(), 0).sendToTarget();
            }

            @Override // com.android.settings.fingerprint.FingerprintAuthenticateSidecar.Listener
            public void onAuthenticationFailed() {
                FingerprintSettingsFragment.this.mHandler.obtainMessage(1002).sendToTarget();
            }

            @Override // com.android.settings.fingerprint.FingerprintAuthenticateSidecar.Listener
            public void onAuthenticationError(int i, CharSequence charSequence) {
                FingerprintSettingsFragment.this.mHandler.obtainMessage(1003, i, 0, charSequence).sendToTarget();
            }

            @Override // com.android.settings.fingerprint.FingerprintAuthenticateSidecar.Listener
            public void onAuthenticationHelp(int i, CharSequence charSequence) {
                FingerprintSettingsFragment.this.mHandler.obtainMessage(1004, i, 0, charSequence).sendToTarget();
            }
        };
        FingerprintRemoveSidecar.Listener mRemovalListener = new FingerprintRemoveSidecar.Listener() { // from class: com.android.settings.fingerprint.FingerprintSettings.FingerprintSettingsFragment.2
            @Override // com.android.settings.fingerprint.FingerprintRemoveSidecar.Listener
            public void onRemovalSucceeded(Fingerprint fingerprint) {
                FingerprintSettingsFragment.this.mHandler.obtainMessage(1000, fingerprint.getFingerId(), 0).sendToTarget();
                updateDialog();
            }

            @Override // com.android.settings.fingerprint.FingerprintRemoveSidecar.Listener
            public void onRemovalError(Fingerprint fingerprint, int i, CharSequence charSequence) {
                Activity activity = FingerprintSettingsFragment.this.getActivity();
                if (activity != null) {
                    Toast.makeText(activity, charSequence, 0);
                }
                updateDialog();
            }

            private void updateDialog() {
                RenameDialog renameDialog = (RenameDialog) FingerprintSettingsFragment.this.getFragmentManager().findFragmentByTag(RenameDialog.class.getName());
                if (renameDialog != null) {
                    renameDialog.enableDelete();
                }
            }
        };
        private final Handler mHandler = new Handler() { // from class: com.android.settings.fingerprint.FingerprintSettings.FingerprintSettingsFragment.3
            @Override // android.os.Handler
            public void handleMessage(Message message) throws Resources.NotFoundException {
                switch (message.what) {
                    case 1000:
                        FingerprintSettingsFragment.this.removeFingerprintPreference(message.arg1);
                        FingerprintSettingsFragment.this.updateAddPreference();
                        FingerprintSettingsFragment.this.retryFingerprint();
                        break;
                    case 1001:
                        FingerprintSettingsFragment.this.highlightFingerprintItem(message.arg1);
                        FingerprintSettingsFragment.this.retryFingerprint();
                        break;
                    case 1003:
                        FingerprintSettingsFragment.this.handleError(message.arg1, (CharSequence) message.obj);
                        break;
                }
            }
        };
        private final Runnable mFingerprintLockoutReset = new Runnable() { // from class: com.android.settings.fingerprint.FingerprintSettings.FingerprintSettingsFragment.5
            @Override // java.lang.Runnable
            public void run() {
                FingerprintSettingsFragment.this.mInFingerprintLockout = false;
                FingerprintSettingsFragment.this.retryFingerprint();
            }
        };

        protected void handleError(int i, CharSequence charSequence) {
            Activity activity;
            if (i == 5) {
                return;
            }
            if (i == 7) {
                this.mInFingerprintLockout = true;
                if (!this.mHandler.hasCallbacks(this.mFingerprintLockoutReset)) {
                    this.mHandler.postDelayed(this.mFingerprintLockoutReset, 30000L);
                }
            } else if (i == 9) {
                this.mInFingerprintLockout = true;
            }
            if (this.mInFingerprintLockout && (activity = getActivity()) != null) {
                Toast.makeText(activity, charSequence, 0).show();
            }
            retryFingerprint();
        }

        private void retryFingerprint() {
            if (!this.mRemovalSidecar.inProgress() && this.mFingerprintManager.getEnrolledFingerprints(this.mUserId).size() != 0 && !this.mLaunchedConfirm && !this.mInFingerprintLockout) {
                this.mAuthenticateSidecar.startAuthentication(this.mUserId);
                this.mAuthenticateSidecar.setListener(this.mAuthenticateListener);
            }
        }

        @Override // com.android.settingslib.core.instrumentation.Instrumentable
        public int getMetricsCategory() {
            return 49;
        }

        @Override // com.android.settings.SettingsPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
        public void onCreate(Bundle bundle) {
            int i;
            super.onCreate(bundle);
            final Activity activity = getActivity();
            this.mFingerprintManager = Utils.getFingerprintManagerOrNull(activity);
            this.mAuthenticateSidecar = (FingerprintAuthenticateSidecar) getFragmentManager().findFragmentByTag("authenticate_sidecar");
            if (this.mAuthenticateSidecar == null) {
                this.mAuthenticateSidecar = new FingerprintAuthenticateSidecar();
                getFragmentManager().beginTransaction().add(this.mAuthenticateSidecar, "authenticate_sidecar").commit();
            }
            this.mAuthenticateSidecar.setFingerprintManager(this.mFingerprintManager);
            this.mRemovalSidecar = (FingerprintRemoveSidecar) getFragmentManager().findFragmentByTag("removal_sidecar");
            if (this.mRemovalSidecar == null) {
                this.mRemovalSidecar = new FingerprintRemoveSidecar();
                getFragmentManager().beginTransaction().add(this.mRemovalSidecar, "removal_sidecar").commit();
            }
            this.mRemovalSidecar.setFingerprintManager(this.mFingerprintManager);
            this.mRemovalSidecar.setListener(this.mRemovalListener);
            RenameDialog renameDialog = (RenameDialog) getFragmentManager().findFragmentByTag(RenameDialog.class.getName());
            if (renameDialog != null) {
                renameDialog.setDeleteInProgress(this.mRemovalSidecar.inProgress());
            }
            this.mFingerprintsRenaming = new HashMap<>();
            if (bundle != null) {
                this.mFingerprintsRenaming = (HashMap) bundle.getSerializable("mFingerprintsRenaming");
                this.mToken = bundle.getByteArray("hw_auth_token");
                this.mLaunchedConfirm = bundle.getBoolean("launched_confirm", false);
            }
            this.mUserId = getActivity().getIntent().getIntExtra("android.intent.extra.USER_ID", UserHandle.myUserId());
            if (this.mToken == null && !this.mLaunchedConfirm) {
                this.mLaunchedConfirm = true;
                launchChooseOrConfirmLock();
            }
            FooterPreference footerPreferenceCreateFooterPreference = this.mFooterPreferenceMixin.createFooterPreference();
            final RestrictedLockUtils.EnforcedAdmin enforcedAdminCheckIfKeyguardFeaturesDisabled = RestrictedLockUtils.checkIfKeyguardFeaturesDisabled(activity, 32, this.mUserId);
            AnnotationSpan.LinkInfo linkInfo = new AnnotationSpan.LinkInfo("admin_details", new View.OnClickListener() { // from class: com.android.settings.fingerprint.-$$Lambda$FingerprintSettings$FingerprintSettingsFragment$yE_lJ-MtxexMYsEgD8_Zrh5Z2iY
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    RestrictedLockUtils.sendShowAdminSupportDetailsIntent(activity, enforcedAdminCheckIfKeyguardFeaturesDisabled);
                }
            });
            AnnotationSpan.LinkInfo linkInfo2 = new AnnotationSpan.LinkInfo(activity, "url", HelpUtils.getHelpIntent(activity, getString(getHelpResource()), activity.getClass().getName()));
            if (enforcedAdminCheckIfKeyguardFeaturesDisabled != null) {
                i = R.string.security_settings_fingerprint_enroll_disclaimer_lockscreen_disabled;
            } else {
                i = R.string.security_settings_fingerprint_enroll_disclaimer;
            }
            footerPreferenceCreateFooterPreference.setTitle(AnnotationSpan.linkify(getText(i), linkInfo2, linkInfo));
        }

        protected void removeFingerprintPreference(int i) {
            String strGenKey = genKey(i);
            Preference preferenceFindPreference = findPreference(strGenKey);
            if (preferenceFindPreference != null) {
                if (!getPreferenceScreen().removePreference(preferenceFindPreference)) {
                    Log.w("FingerprintSettings", "Failed to remove preference with key " + strGenKey);
                    return;
                }
                return;
            }
            Log.w("FingerprintSettings", "Can't find preference to remove: " + strGenKey);
        }

        private PreferenceScreen createPreferenceHierarchy() throws Resources.NotFoundException {
            PreferenceScreen preferenceScreen = getPreferenceScreen();
            if (preferenceScreen != null) {
                preferenceScreen.removeAll();
            }
            addPreferencesFromResource(R.xml.security_settings_fingerprint);
            PreferenceScreen preferenceScreen2 = getPreferenceScreen();
            addFingerprintItemPreferences(preferenceScreen2);
            setPreferenceScreen(preferenceScreen2);
            return preferenceScreen2;
        }

        private void addFingerprintItemPreferences(PreferenceGroup preferenceGroup) throws Resources.NotFoundException {
            preferenceGroup.removeAll();
            List enrolledFingerprints = this.mFingerprintManager.getEnrolledFingerprints(this.mUserId);
            int size = enrolledFingerprints.size();
            for (int i = 0; i < size; i++) {
                Fingerprint fingerprint = (Fingerprint) enrolledFingerprints.get(i);
                FingerprintPreference fingerprintPreference = new FingerprintPreference(preferenceGroup.getContext(), this);
                fingerprintPreference.setKey(genKey(fingerprint.getFingerId()));
                fingerprintPreference.setTitle(fingerprint.getName());
                fingerprintPreference.setFingerprint(fingerprint);
                fingerprintPreference.setPersistent(false);
                fingerprintPreference.setIcon(R.drawable.ic_fingerprint_24dp);
                if (this.mRemovalSidecar.isRemovingFingerprint(fingerprint.getFingerId())) {
                    fingerprintPreference.setEnabled(false);
                }
                if (this.mFingerprintsRenaming.containsKey(Integer.valueOf(fingerprint.getFingerId()))) {
                    fingerprintPreference.setTitle(this.mFingerprintsRenaming.get(Integer.valueOf(fingerprint.getFingerId())));
                }
                preferenceGroup.addPreference(fingerprintPreference);
                fingerprintPreference.setOnPreferenceChangeListener(this);
            }
            Preference preference = new Preference(preferenceGroup.getContext());
            preference.setKey("key_fingerprint_add");
            preference.setTitle(R.string.fingerprint_add_title);
            preference.setIcon(R.drawable.ic_menu_add);
            preferenceGroup.addPreference(preference);
            preference.setOnPreferenceChangeListener(this);
            updateAddPreference();
        }

        private void updateAddPreference() throws Resources.NotFoundException {
            if (getActivity() == null) {
                return;
            }
            int integer = getContext().getResources().getInteger(android.R.integer.config_datause_polling_period_sec);
            boolean z = false;
            boolean z2 = this.mFingerprintManager.getEnrolledFingerprints(this.mUserId).size() >= integer;
            boolean zInProgress = this.mRemovalSidecar.inProgress();
            String string = z2 ? getContext().getString(R.string.fingerprint_add_max, Integer.valueOf(integer)) : "";
            Preference preferenceFindPreference = findPreference("key_fingerprint_add");
            preferenceFindPreference.setSummary(string);
            if (!z2 && !zInProgress) {
                z = true;
            }
            preferenceFindPreference.setEnabled(z);
        }

        private static String genKey(int i) {
            return "key_fingerprint_item_" + i;
        }

        @Override // com.android.settings.SettingsPreferenceFragment, com.android.settings.core.InstrumentedPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
        public void onResume() throws Resources.NotFoundException {
            super.onResume();
            this.mInFingerprintLockout = false;
            updatePreferences();
            if (this.mRemovalSidecar != null) {
                this.mRemovalSidecar.setListener(this.mRemovalListener);
            }
        }

        private void updatePreferences() throws Resources.NotFoundException {
            createPreferenceHierarchy();
            retryFingerprint();
        }

        @Override // com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
        public void onPause() {
            super.onPause();
            if (this.mRemovalSidecar != null) {
                this.mRemovalSidecar.setListener(null);
            }
            if (this.mAuthenticateSidecar != null) {
                this.mAuthenticateSidecar.setListener(null);
                this.mAuthenticateSidecar.stopAuthentication();
            }
        }

        @Override // com.android.settings.SettingsPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
        public void onSaveInstanceState(Bundle bundle) {
            bundle.putByteArray("hw_auth_token", this.mToken);
            bundle.putBoolean("launched_confirm", this.mLaunchedConfirm);
            bundle.putSerializable("mFingerprintsRenaming", this.mFingerprintsRenaming);
        }

        @Override // android.support.v14.preference.PreferenceFragment, android.support.v7.preference.PreferenceManager.OnPreferenceTreeClickListener
        public boolean onPreferenceTreeClick(Preference preference) {
            if ("key_fingerprint_add".equals(preference.getKey())) {
                Intent intent = new Intent();
                intent.setClassName("com.android.settings", FingerprintEnrollEnrolling.class.getName());
                intent.putExtra("android.intent.extra.USER_ID", this.mUserId);
                intent.putExtra("hw_auth_token", this.mToken);
                startActivityForResult(intent, 10);
            } else if (preference instanceof FingerprintPreference) {
                showRenameDialog(((FingerprintPreference) preference).getFingerprint());
            }
            return super.onPreferenceTreeClick(preference);
        }

        @Override // com.android.settings.fingerprint.FingerprintSettings.FingerprintPreference.OnDeleteClickListener
        public void onDeleteClick(FingerprintPreference fingerprintPreference) {
            boolean z = this.mFingerprintManager.getEnrolledFingerprints(this.mUserId).size() > 1;
            Parcelable fingerprint = fingerprintPreference.getFingerprint();
            if (z) {
                if (this.mRemovalSidecar.inProgress()) {
                    Log.d("FingerprintSettings", "Fingerprint delete in progress, skipping");
                    return;
                } else {
                    DeleteFingerprintDialog.newInstance(fingerprint, this).show(getFragmentManager(), DeleteFingerprintDialog.class.getName());
                    return;
                }
            }
            ConfirmLastDeleteDialog confirmLastDeleteDialog = new ConfirmLastDeleteDialog();
            boolean zIsManagedProfile = UserManager.get(getContext()).isManagedProfile(this.mUserId);
            Bundle bundle = new Bundle();
            bundle.putParcelable("fingerprint", fingerprint);
            bundle.putBoolean("isProfileChallengeUser", zIsManagedProfile);
            confirmLastDeleteDialog.setArguments(bundle);
            confirmLastDeleteDialog.setTargetFragment(this, 0);
            confirmLastDeleteDialog.show(getFragmentManager(), ConfirmLastDeleteDialog.class.getName());
        }

        private void showRenameDialog(Fingerprint fingerprint) {
            RenameDialog renameDialog = new RenameDialog();
            Bundle bundle = new Bundle();
            if (this.mFingerprintsRenaming.containsKey(Integer.valueOf(fingerprint.getFingerId()))) {
                bundle.putParcelable("fingerprint", new Fingerprint(this.mFingerprintsRenaming.get(Integer.valueOf(fingerprint.getFingerId())), fingerprint.getGroupId(), fingerprint.getFingerId(), fingerprint.getDeviceId()));
            } else {
                bundle.putParcelable("fingerprint", fingerprint);
            }
            renameDialog.setDeleteInProgress(this.mRemovalSidecar.inProgress());
            renameDialog.setArguments(bundle);
            renameDialog.setTargetFragment(this, 0);
            renameDialog.show(getFragmentManager(), RenameDialog.class.getName());
        }

        @Override // android.support.v7.preference.Preference.OnPreferenceChangeListener
        public boolean onPreferenceChange(Preference preference, Object obj) {
            String key = preference.getKey();
            if (!"fingerprint_enable_keyguard_toggle".equals(key)) {
                Log.v("FingerprintSettings", "Unknown key:" + key);
                return true;
            }
            return true;
        }

        @Override // com.android.settings.support.actionbar.HelpResourceProvider
        public int getHelpResource() {
            return R.string.help_url_fingerprint;
        }

        @Override // android.app.Fragment
        public void onActivityResult(int i, int i2, Intent intent) {
            super.onActivityResult(i, i2, intent);
            if (i == 102 || i == 101) {
                this.mLaunchedConfirm = false;
                if ((i2 == 1 || i2 == -1) && intent != null) {
                    this.mToken = intent.getByteArrayExtra("hw_auth_token");
                }
            } else if (i == 10 && i2 == 3) {
                Activity activity = getActivity();
                activity.setResult(3);
                activity.finish();
            }
            if (this.mToken == null) {
                getActivity().finish();
            }
        }

        @Override // com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
        public void onDestroy() {
            int iPostEnroll;
            super.onDestroy();
            if (getActivity().isFinishing() && (iPostEnroll = this.mFingerprintManager.postEnroll()) < 0) {
                Log.w("FingerprintSettings", "postEnroll failed: result = " + iPostEnroll);
            }
        }

        private Drawable getHighlightDrawable() {
            Activity activity;
            if (this.mHighlightDrawable == null && (activity = getActivity()) != null) {
                this.mHighlightDrawable = activity.getDrawable(R.drawable.preference_highlight);
            }
            return this.mHighlightDrawable;
        }

        private void highlightFingerprintItem(int i) {
            FingerprintPreference fingerprintPreference = (FingerprintPreference) findPreference(genKey(i));
            Drawable highlightDrawable = getHighlightDrawable();
            if (highlightDrawable != null && fingerprintPreference != null) {
                final View view = fingerprintPreference.getView();
                highlightDrawable.setHotspot(view.getWidth() / 2, view.getHeight() / 2);
                view.setBackground(highlightDrawable);
                view.setPressed(true);
                view.setPressed(false);
                this.mHandler.postDelayed(new Runnable() { // from class: com.android.settings.fingerprint.FingerprintSettings.FingerprintSettingsFragment.4
                    @Override // java.lang.Runnable
                    public void run() {
                        view.setBackground(null);
                    }
                }, 500L);
            }
        }

        private void launchChooseOrConfirmLock() {
            Intent intent = new Intent();
            long jPreEnroll = this.mFingerprintManager.preEnroll();
            if (!new ChooseLockSettingsHelper(getActivity(), this).launchConfirmationActivity(101, getString(R.string.security_settings_fingerprint_preference_title), (CharSequence) null, (CharSequence) null, jPreEnroll, this.mUserId)) {
                intent.setClassName("com.android.settings", ChooseLockGeneric.class.getName());
                intent.putExtra("minimum_quality", 65536);
                intent.putExtra("hide_disabled_prefs", true);
                intent.putExtra("has_challenge", true);
                intent.putExtra("android.intent.extra.USER_ID", this.mUserId);
                intent.putExtra("challenge", jPreEnroll);
                intent.putExtra("android.intent.extra.USER_ID", this.mUserId);
                startActivityForResult(intent, 102);
            }
        }

        void deleteFingerPrint(Fingerprint fingerprint) throws Resources.NotFoundException {
            this.mRemovalSidecar.startRemove(fingerprint, this.mUserId);
            findPreference(genKey(fingerprint.getFingerId())).setEnabled(false);
            updateAddPreference();
        }

        private void renameFingerPrint(int i, String str) throws Resources.NotFoundException {
            this.mFingerprintManager.rename(i, this.mUserId, str);
            if (!TextUtils.isEmpty(str)) {
                this.mFingerprintsRenaming.put(Integer.valueOf(i), str);
            }
            updatePreferences();
        }

        public static class DeleteFingerprintDialog extends InstrumentedDialogFragment implements DialogInterface.OnClickListener {
            private AlertDialog mAlertDialog;
            private Fingerprint mFp;

            public static DeleteFingerprintDialog newInstance(Fingerprint fingerprint, FingerprintSettingsFragment fingerprintSettingsFragment) {
                DeleteFingerprintDialog deleteFingerprintDialog = new DeleteFingerprintDialog();
                Bundle bundle = new Bundle();
                bundle.putParcelable("fingerprint", fingerprint);
                deleteFingerprintDialog.setArguments(bundle);
                deleteFingerprintDialog.setTargetFragment(fingerprintSettingsFragment, 0);
                return deleteFingerprintDialog;
            }

            @Override // com.android.settingslib.core.instrumentation.Instrumentable
            public int getMetricsCategory() {
                return 570;
            }

            @Override // android.app.DialogFragment
            public Dialog onCreateDialog(Bundle bundle) {
                this.mFp = getArguments().getParcelable("fingerprint");
                this.mAlertDialog = new AlertDialog.Builder(getActivity()).setTitle(getString(R.string.fingerprint_delete_title, new Object[]{this.mFp.getName()})).setMessage(R.string.fingerprint_delete_message).setPositiveButton(R.string.security_settings_fingerprint_enroll_dialog_delete, this).setNegativeButton(R.string.cancel, (DialogInterface.OnClickListener) null).create();
                return this.mAlertDialog;
            }

            @Override // android.content.DialogInterface.OnClickListener
            public void onClick(DialogInterface dialogInterface, int i) throws Resources.NotFoundException {
                if (i == -1) {
                    int fingerId = this.mFp.getFingerId();
                    Log.v("FingerprintSettings", "Removing fpId=" + fingerId);
                    this.mMetricsFeatureProvider.action(getContext(), 253, fingerId);
                    ((FingerprintSettingsFragment) getTargetFragment()).deleteFingerPrint(this.mFp);
                }
            }
        }

        public static class RenameDialog extends InstrumentedDialogFragment {
            private AlertDialog mAlertDialog;
            private boolean mDeleteInProgress;
            private EditText mDialogTextField;
            private String mFingerName;
            private Fingerprint mFp;
            private Boolean mTextHadFocus;
            private int mTextSelectionEnd;
            private int mTextSelectionStart;

            public void setDeleteInProgress(boolean z) {
                this.mDeleteInProgress = z;
            }

            @Override // android.app.DialogFragment
            public Dialog onCreateDialog(Bundle bundle) {
                this.mFp = getArguments().getParcelable("fingerprint");
                if (bundle != null) {
                    this.mFingerName = bundle.getString("fingerName");
                    this.mTextHadFocus = Boolean.valueOf(bundle.getBoolean("textHadFocus"));
                    this.mTextSelectionStart = bundle.getInt("startSelection");
                    this.mTextSelectionEnd = bundle.getInt("endSelection");
                }
                this.mAlertDialog = new AlertDialog.Builder(getActivity()).setView(R.layout.fingerprint_rename_dialog).setPositiveButton(R.string.security_settings_fingerprint_enroll_dialog_ok, new DialogInterface.OnClickListener() { // from class: com.android.settings.fingerprint.FingerprintSettings.FingerprintSettingsFragment.RenameDialog.1
                    @Override // android.content.DialogInterface.OnClickListener
                    public void onClick(DialogInterface dialogInterface, int i) throws Resources.NotFoundException {
                        String string = RenameDialog.this.mDialogTextField.getText().toString();
                        CharSequence name = RenameDialog.this.mFp.getName();
                        if (!TextUtils.equals(string, name)) {
                            Log.d("FingerprintSettings", "rename " + ((Object) name) + " to " + string);
                            RenameDialog.this.mMetricsFeatureProvider.action(RenameDialog.this.getContext(), 254, RenameDialog.this.mFp.getFingerId());
                            ((FingerprintSettingsFragment) RenameDialog.this.getTargetFragment()).renameFingerPrint(RenameDialog.this.mFp.getFingerId(), string);
                        }
                        dialogInterface.dismiss();
                    }
                }).create();
                this.mAlertDialog.setOnShowListener(new DialogInterface.OnShowListener() { // from class: com.android.settings.fingerprint.FingerprintSettings.FingerprintSettingsFragment.RenameDialog.2
                    @Override // android.content.DialogInterface.OnShowListener
                    public void onShow(DialogInterface dialogInterface) {
                        RenameDialog.this.mDialogTextField = (EditText) RenameDialog.this.mAlertDialog.findViewById(R.id.fingerprint_rename_field);
                        RenameDialog.this.mDialogTextField.setText(RenameDialog.this.mFingerName == null ? RenameDialog.this.mFp.getName() : RenameDialog.this.mFingerName);
                        if (RenameDialog.this.mTextHadFocus == null) {
                            RenameDialog.this.mDialogTextField.selectAll();
                        } else {
                            RenameDialog.this.mDialogTextField.setSelection(RenameDialog.this.mTextSelectionStart, RenameDialog.this.mTextSelectionEnd);
                        }
                        if (RenameDialog.this.mDeleteInProgress) {
                            RenameDialog.this.mAlertDialog.getButton(-2).setEnabled(false);
                        }
                        RenameDialog.this.mDialogTextField.requestFocus();
                    }
                });
                if (this.mTextHadFocus == null || this.mTextHadFocus.booleanValue()) {
                    this.mAlertDialog.getWindow().setSoftInputMode(5);
                }
                return this.mAlertDialog;
            }

            public void enableDelete() {
                this.mDeleteInProgress = false;
                if (this.mAlertDialog != null) {
                    this.mAlertDialog.getButton(-2).setEnabled(true);
                }
            }

            @Override // android.app.DialogFragment, android.app.Fragment
            public void onSaveInstanceState(Bundle bundle) {
                super.onSaveInstanceState(bundle);
                if (this.mDialogTextField != null) {
                    bundle.putString("fingerName", this.mDialogTextField.getText().toString());
                    bundle.putBoolean("textHadFocus", this.mDialogTextField.hasFocus());
                    bundle.putInt("startSelection", this.mDialogTextField.getSelectionStart());
                    bundle.putInt("endSelection", this.mDialogTextField.getSelectionEnd());
                }
            }

            @Override // com.android.settingslib.core.instrumentation.Instrumentable
            public int getMetricsCategory() {
                return 570;
            }
        }

        public static class ConfirmLastDeleteDialog extends InstrumentedDialogFragment {
            private Fingerprint mFp;

            @Override // com.android.settingslib.core.instrumentation.Instrumentable
            public int getMetricsCategory() {
                return 571;
            }

            @Override // android.app.DialogFragment
            public Dialog onCreateDialog(Bundle bundle) {
                int i;
                this.mFp = getArguments().getParcelable("fingerprint");
                boolean z = getArguments().getBoolean("isProfileChallengeUser");
                AlertDialog.Builder title = new AlertDialog.Builder(getActivity()).setTitle(R.string.fingerprint_last_delete_title);
                if (z) {
                    i = R.string.fingerprint_last_delete_message_profile_challenge;
                } else {
                    i = R.string.fingerprint_last_delete_message;
                }
                return title.setMessage(i).setPositiveButton(R.string.fingerprint_last_delete_confirm, new DialogInterface.OnClickListener() { // from class: com.android.settings.fingerprint.FingerprintSettings.FingerprintSettingsFragment.ConfirmLastDeleteDialog.2
                    @Override // android.content.DialogInterface.OnClickListener
                    public void onClick(DialogInterface dialogInterface, int i2) throws Resources.NotFoundException {
                        ((FingerprintSettingsFragment) ConfirmLastDeleteDialog.this.getTargetFragment()).deleteFingerPrint(ConfirmLastDeleteDialog.this.mFp);
                        dialogInterface.dismiss();
                    }
                }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() { // from class: com.android.settings.fingerprint.FingerprintSettings.FingerprintSettingsFragment.ConfirmLastDeleteDialog.1
                    @Override // android.content.DialogInterface.OnClickListener
                    public void onClick(DialogInterface dialogInterface, int i2) {
                        dialogInterface.dismiss();
                    }
                }).create();
            }
        }
    }

    public static class FingerprintPreference extends TwoTargetPreference {
        private View mDeleteView;
        private Fingerprint mFingerprint;
        private final OnDeleteClickListener mOnDeleteClickListener;
        private View mView;

        public interface OnDeleteClickListener {
            void onDeleteClick(FingerprintPreference fingerprintPreference);
        }

        public FingerprintPreference(Context context, OnDeleteClickListener onDeleteClickListener) {
            super(context);
            this.mOnDeleteClickListener = onDeleteClickListener;
        }

        public View getView() {
            return this.mView;
        }

        public void setFingerprint(Fingerprint fingerprint) {
            this.mFingerprint = fingerprint;
        }

        public Fingerprint getFingerprint() {
            return this.mFingerprint;
        }

        @Override // com.android.settingslib.TwoTargetPreference
        protected int getSecondTargetResId() {
            return R.layout.preference_widget_delete;
        }

        @Override // com.android.settingslib.TwoTargetPreference, android.support.v7.preference.Preference
        public void onBindViewHolder(PreferenceViewHolder preferenceViewHolder) {
            super.onBindViewHolder(preferenceViewHolder);
            this.mView = preferenceViewHolder.itemView;
            this.mDeleteView = preferenceViewHolder.itemView.findViewById(R.id.delete_button);
            this.mDeleteView.setOnClickListener(new View.OnClickListener() { // from class: com.android.settings.fingerprint.FingerprintSettings.FingerprintPreference.1
                @Override // android.view.View.OnClickListener
                public void onClick(View view) {
                    if (FingerprintPreference.this.mOnDeleteClickListener != null) {
                        FingerprintPreference.this.mOnDeleteClickListener.onDeleteClick(FingerprintPreference.this);
                    }
                }
            });
        }
    }
}
