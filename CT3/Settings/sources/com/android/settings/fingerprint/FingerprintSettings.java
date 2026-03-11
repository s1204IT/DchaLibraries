package com.android.settings.fingerprint;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.hardware.fingerprint.Fingerprint;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Message;
import android.os.UserHandle;
import android.os.UserManager;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.PreferenceViewHolder;
import android.text.Annotation;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.android.internal.logging.MetricsLogger;
import com.android.settings.ChooseLockGeneric;
import com.android.settings.ChooseLockSettingsHelper;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.SubSettings;
import com.android.settings.Utils;
import com.android.settingslib.HelpUtils;
import com.android.settingslib.RestrictedLockUtils;
import java.util.List;

public class FingerprintSettings extends SubSettings {
    @Override
    public Intent getIntent() {
        Intent modIntent = new Intent(super.getIntent());
        modIntent.putExtra(":settings:show_fragment", FingerprintSettingsFragment.class.getName());
        return modIntent;
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return FingerprintSettingsFragment.class.getName().equals(fragmentName);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CharSequence msg = getText(R.string.security_settings_fingerprint_preference_title);
        setTitle(msg);
    }

    public static class FingerprintSettingsFragment extends SettingsPreferenceFragment implements Preference.OnPreferenceChangeListener {
        private CancellationSignal mFingerprintCancel;
        private FingerprintManager mFingerprintManager;
        private Drawable mHighlightDrawable;
        private boolean mInFingerprintLockout;
        private boolean mLaunchedConfirm;
        private byte[] mToken;
        private int mUserId;
        private FingerprintManager.AuthenticationCallback mAuthCallback = new FingerprintManager.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(FingerprintManager.AuthenticationResult result) {
                int fingerId = result.getFingerprint().getFingerId();
                FingerprintSettingsFragment.this.mHandler.obtainMessage(1001, fingerId, 0).sendToTarget();
            }

            @Override
            public void onAuthenticationFailed() {
                FingerprintSettingsFragment.this.mHandler.obtainMessage(1002).sendToTarget();
            }

            @Override
            public void onAuthenticationError(int errMsgId, CharSequence errString) {
                FingerprintSettingsFragment.this.mHandler.obtainMessage(1003, errMsgId, 0, errString).sendToTarget();
            }

            @Override
            public void onAuthenticationHelp(int helpMsgId, CharSequence helpString) {
                FingerprintSettingsFragment.this.mHandler.obtainMessage(1004, helpMsgId, 0, helpString).sendToTarget();
            }
        };
        private FingerprintManager.RemovalCallback mRemoveCallback = new FingerprintManager.RemovalCallback() {
            public void onRemovalSucceeded(Fingerprint fingerprint) {
                FingerprintSettingsFragment.this.mHandler.obtainMessage(1000, fingerprint.getFingerId(), 0).sendToTarget();
            }

            public void onRemovalError(Fingerprint fp, int errMsgId, CharSequence errString) {
                Activity activity = FingerprintSettingsFragment.this.getActivity();
                if (activity == null) {
                    return;
                }
                Toast.makeText(activity, errString, 0);
            }
        };
        private final Handler mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 1000:
                        FingerprintSettingsFragment.this.removeFingerprintPreference(msg.arg1);
                        FingerprintSettingsFragment.this.updateAddPreference();
                        FingerprintSettingsFragment.this.retryFingerprint();
                        break;
                    case 1001:
                        FingerprintSettingsFragment.this.mFingerprintCancel = null;
                        FingerprintSettingsFragment.this.highlightFingerprintItem(msg.arg1);
                        FingerprintSettingsFragment.this.retryFingerprint();
                        break;
                    case 1003:
                        FingerprintSettingsFragment.this.handleError(msg.arg1, (CharSequence) msg.obj);
                        break;
                }
            }
        };
        private final Runnable mFingerprintLockoutReset = new Runnable() {
            @Override
            public void run() {
                FingerprintSettingsFragment.this.mInFingerprintLockout = false;
                FingerprintSettingsFragment.this.retryFingerprint();
            }
        };

        private void stopFingerprint() {
            if (this.mFingerprintCancel != null && !this.mFingerprintCancel.isCanceled()) {
                this.mFingerprintCancel.cancel();
            }
            this.mFingerprintCancel = null;
        }

        protected void handleError(int errMsgId, CharSequence msg) {
            this.mFingerprintCancel = null;
            switch (errMsgId) {
                case 5:
                    return;
                case 7:
                    this.mInFingerprintLockout = true;
                    if (!this.mHandler.hasCallbacks(this.mFingerprintLockoutReset)) {
                        this.mHandler.postDelayed(this.mFingerprintLockoutReset, 30000L);
                    }
                    break;
            }
            Activity activity = getActivity();
            if (activity != null) {
                Toast.makeText(activity, msg, 0);
            }
            retryFingerprint();
        }

        public void retryFingerprint() {
            if (this.mInFingerprintLockout) {
                return;
            }
            this.mFingerprintCancel = new CancellationSignal();
            this.mFingerprintManager.authenticate(null, this.mFingerprintCancel, 0, this.mAuthCallback, null, this.mUserId);
        }

        @Override
        protected int getMetricsCategory() {
            return 49;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            if (savedInstanceState != null) {
                this.mToken = savedInstanceState.getByteArray("hw_auth_token");
                this.mLaunchedConfirm = savedInstanceState.getBoolean("launched_confirm", false);
            }
            this.mUserId = getActivity().getIntent().getIntExtra("android.intent.extra.USER_ID", UserHandle.myUserId());
            Activity activity = getActivity();
            this.mFingerprintManager = (FingerprintManager) activity.getSystemService("fingerprint");
            if (this.mToken != null || this.mLaunchedConfirm) {
                return;
            }
            this.mLaunchedConfirm = true;
            launchChooseOrConfirmLock();
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            int i;
            super.onViewCreated(view, savedInstanceState);
            TextView v = (TextView) LayoutInflater.from(view.getContext()).inflate(R.layout.fingerprint_settings_footer, (ViewGroup) null);
            RestrictedLockUtils.EnforcedAdmin admin = RestrictedLockUtils.checkIfKeyguardFeaturesDisabled(getActivity(), 32, this.mUserId);
            if (admin != null) {
                i = R.string.security_settings_fingerprint_enroll_disclaimer_lockscreen_disabled;
            } else {
                i = R.string.security_settings_fingerprint_enroll_disclaimer;
            }
            v.setText(LearnMoreSpan.linkify(getText(i), getString(getHelpResource()), admin));
            v.setMovementMethod(new LinkMovementMethod());
            setFooterView(v);
        }

        protected void removeFingerprintPreference(int fingerprintId) {
            String name = genKey(fingerprintId);
            Preference prefToRemove = findPreference(name);
            if (prefToRemove != null) {
                if (getPreferenceScreen().removePreference(prefToRemove)) {
                    return;
                }
                Log.w("FingerprintSettings", "Failed to remove preference with key " + name);
                return;
            }
            Log.w("FingerprintSettings", "Can't find preference to remove: " + name);
        }

        private PreferenceScreen createPreferenceHierarchy() {
            PreferenceScreen root = getPreferenceScreen();
            if (root != null) {
                root.removeAll();
            }
            addPreferencesFromResource(R.xml.security_settings_fingerprint);
            PreferenceScreen root2 = getPreferenceScreen();
            addFingerprintItemPreferences(root2);
            setPreferenceScreen(root2);
            return root2;
        }

        private void addFingerprintItemPreferences(PreferenceGroup root) {
            root.removeAll();
            List<Fingerprint> items = this.mFingerprintManager.getEnrolledFingerprints(this.mUserId);
            int fingerprintCount = items.size();
            for (int i = 0; i < fingerprintCount; i++) {
                Fingerprint item = items.get(i);
                FingerprintPreference pref = new FingerprintPreference(root.getContext());
                pref.setKey(genKey(item.getFingerId()));
                pref.setTitle(item.getName());
                pref.setFingerprint(item);
                pref.setPersistent(false);
                root.addPreference(pref);
                pref.setOnPreferenceChangeListener(this);
            }
            Preference addPreference = new Preference(root.getContext());
            addPreference.setKey("key_fingerprint_add");
            addPreference.setTitle(R.string.fingerprint_add_title);
            addPreference.setIcon(R.drawable.ic_add_24dp);
            root.addPreference(addPreference);
            addPreference.setOnPreferenceChangeListener(this);
            updateAddPreference();
        }

        public void updateAddPreference() {
            int max = getContext().getResources().getInteger(android.R.integer.config_externalDisplayPeakHeight);
            boolean tooMany = this.mFingerprintManager.getEnrolledFingerprints(this.mUserId).size() >= max;
            String maxSummary = tooMany ? getContext().getString(R.string.fingerprint_add_max, Integer.valueOf(max)) : "";
            Preference addPreference = findPreference("key_fingerprint_add");
            addPreference.setSummary(maxSummary);
            addPreference.setEnabled(tooMany ? false : true);
        }

        private static String genKey(int id) {
            return "key_fingerprint_item_" + id;
        }

        @Override
        public void onResume() {
            super.onResume();
            updatePreferences();
        }

        private void updatePreferences() {
            createPreferenceHierarchy();
            retryFingerprint();
        }

        @Override
        public void onPause() {
            super.onPause();
            stopFingerprint();
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            outState.putByteArray("hw_auth_token", this.mToken);
            outState.putBoolean("launched_confirm", this.mLaunchedConfirm);
        }

        @Override
        public boolean onPreferenceTreeClick(Preference pref) {
            String key = pref.getKey();
            if ("key_fingerprint_add".equals(key)) {
                Intent intent = new Intent();
                intent.setClassName("com.android.settings", FingerprintEnrollEnrolling.class.getName());
                intent.putExtra("android.intent.extra.USER_ID", this.mUserId);
                intent.putExtra("hw_auth_token", this.mToken);
                startActivityForResult(intent, 10);
                return true;
            }
            if (pref instanceof FingerprintPreference) {
                FingerprintPreference fpref = (FingerprintPreference) pref;
                Fingerprint fp = fpref.getFingerprint();
                showRenameDeleteDialog(fp);
                return super.onPreferenceTreeClick(pref);
            }
            return true;
        }

        private void showRenameDeleteDialog(Fingerprint fp) {
            RenameDeleteDialog renameDeleteDialog = new RenameDeleteDialog();
            Bundle args = new Bundle();
            args.putParcelable("fingerprint", fp);
            renameDeleteDialog.setArguments(args);
            renameDeleteDialog.setTargetFragment(this, 0);
            renameDeleteDialog.show(getFragmentManager(), RenameDeleteDialog.class.getName());
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String key = preference.getKey();
            if (!"fingerprint_enable_keyguard_toggle".equals(key)) {
                Log.v("FingerprintSettings", "Unknown key:" + key);
            }
            return true;
        }

        @Override
        protected int getHelpResource() {
            return R.string.help_url_fingerprint;
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            if (requestCode == 102 || requestCode == 101) {
                if ((resultCode == 1 || resultCode == -1) && data != null) {
                    this.mToken = data.getByteArrayExtra("hw_auth_token");
                }
            } else if (requestCode == 10 && resultCode == 3) {
                Activity activity = getActivity();
                activity.setResult(3);
                activity.finish();
            }
            if (this.mToken != null) {
                return;
            }
            getActivity().finish();
        }

        @Override
        public void onDestroy() {
            int result;
            super.onDestroy();
            if (!getActivity().isFinishing() || (result = this.mFingerprintManager.postEnroll()) >= 0) {
                return;
            }
            Log.w("FingerprintSettings", "postEnroll failed: result = " + result);
        }

        private Drawable getHighlightDrawable() {
            Activity activity;
            if (this.mHighlightDrawable == null && (activity = getActivity()) != null) {
                this.mHighlightDrawable = activity.getDrawable(R.drawable.preference_highlight);
            }
            return this.mHighlightDrawable;
        }

        public void highlightFingerprintItem(int fpId) {
            String prefName = genKey(fpId);
            FingerprintPreference fpref = (FingerprintPreference) findPreference(prefName);
            Drawable highlight = getHighlightDrawable();
            if (highlight == null) {
                return;
            }
            final View view = fpref.getView();
            int centerX = view.getWidth() / 2;
            int centerY = view.getHeight() / 2;
            highlight.setHotspot(centerX, centerY);
            view.setBackground(highlight);
            view.setPressed(true);
            view.setPressed(false);
            this.mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    view.setBackground(null);
                }
            }, 500L);
        }

        private void launchChooseOrConfirmLock() {
            Intent intent = new Intent();
            long challenge = this.mFingerprintManager.preEnroll();
            ChooseLockSettingsHelper helper = new ChooseLockSettingsHelper(getActivity(), this);
            if (helper.launchConfirmationActivity(101, getString(R.string.security_settings_fingerprint_preference_title), (CharSequence) null, (CharSequence) null, challenge, this.mUserId)) {
                return;
            }
            intent.setClassName("com.android.settings", ChooseLockGeneric.class.getName());
            intent.putExtra("minimum_quality", 65536);
            intent.putExtra("hide_disabled_prefs", true);
            intent.putExtra("has_challenge", true);
            intent.putExtra("android.intent.extra.USER_ID", this.mUserId);
            intent.putExtra("challenge", challenge);
            intent.putExtra("android.intent.extra.USER_ID", this.mUserId);
            startActivityForResult(intent, 102);
        }

        public void deleteFingerPrint(Fingerprint fingerPrint) {
            this.mFingerprintManager.remove(fingerPrint, this.mUserId, this.mRemoveCallback);
        }

        public void renameFingerPrint(int fingerId, String newName) {
            this.mFingerprintManager.rename(fingerId, this.mUserId, newName);
            updatePreferences();
        }

        public static class RenameDeleteDialog extends DialogFragment {
            private EditText mDialogTextField;
            private String mFingerName;
            private Fingerprint mFp;
            private Boolean mTextHadFocus;
            private int mTextSelectionEnd;
            private int mTextSelectionStart;

            @Override
            public Dialog onCreateDialog(Bundle savedInstanceState) {
                this.mFp = getArguments().getParcelable("fingerprint");
                if (savedInstanceState != null) {
                    this.mFingerName = savedInstanceState.getString("fingerName");
                    this.mTextHadFocus = Boolean.valueOf(savedInstanceState.getBoolean("textHadFocus"));
                    this.mTextSelectionStart = savedInstanceState.getInt("startSelection");
                    this.mTextSelectionEnd = savedInstanceState.getInt("endSelection");
                }
                final AlertDialog alertDialog = new AlertDialog.Builder(getActivity()).setView(R.layout.fingerprint_rename_dialog).setPositiveButton(R.string.security_settings_fingerprint_enroll_dialog_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String newName = RenameDeleteDialog.this.mDialogTextField.getText().toString();
                        CharSequence name = RenameDeleteDialog.this.mFp.getName();
                        if (!newName.equals(name)) {
                            Log.v("FingerprintSettings", "rename " + name + " to " + newName);
                            MetricsLogger.action(RenameDeleteDialog.this.getContext(), 254, RenameDeleteDialog.this.mFp.getFingerId());
                            FingerprintSettingsFragment parent = (FingerprintSettingsFragment) RenameDeleteDialog.this.getTargetFragment();
                            parent.renameFingerPrint(RenameDeleteDialog.this.mFp.getFingerId(), newName);
                        }
                        dialog.dismiss();
                    }
                }).setNegativeButton(R.string.security_settings_fingerprint_enroll_dialog_delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        RenameDeleteDialog.this.onDeleteClick(dialog);
                    }
                }).create();
                alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface dialog) {
                        RenameDeleteDialog.this.mDialogTextField = (EditText) alertDialog.findViewById(R.id.fingerprint_rename_field);
                        CharSequence name = RenameDeleteDialog.this.mFingerName == null ? RenameDeleteDialog.this.mFp.getName() : RenameDeleteDialog.this.mFingerName;
                        RenameDeleteDialog.this.mDialogTextField.setText(name);
                        if (RenameDeleteDialog.this.mTextHadFocus == null) {
                            RenameDeleteDialog.this.mDialogTextField.selectAll();
                        } else {
                            RenameDeleteDialog.this.mDialogTextField.setSelection(RenameDeleteDialog.this.mTextSelectionStart, RenameDeleteDialog.this.mTextSelectionEnd);
                        }
                    }
                });
                if (this.mTextHadFocus == null || this.mTextHadFocus.booleanValue()) {
                    alertDialog.getWindow().setSoftInputMode(5);
                }
                return alertDialog;
            }

            public void onDeleteClick(DialogInterface dialog) {
                Log.v("FingerprintSettings", "Removing fpId=" + this.mFp.getFingerId());
                MetricsLogger.action(getContext(), 253, this.mFp.getFingerId());
                FingerprintSettingsFragment parent = (FingerprintSettingsFragment) getTargetFragment();
                boolean isProfileChallengeUser = Utils.isManagedProfile(UserManager.get(getContext()), parent.mUserId);
                if (parent.mFingerprintManager.getEnrolledFingerprints(parent.mUserId).size() > 1) {
                    parent.deleteFingerPrint(this.mFp);
                } else {
                    ConfirmLastDeleteDialog lastDeleteDialog = new ConfirmLastDeleteDialog();
                    Bundle args = new Bundle();
                    args.putParcelable("fingerprint", this.mFp);
                    args.putBoolean("isProfileChallengeUser", isProfileChallengeUser);
                    lastDeleteDialog.setArguments(args);
                    lastDeleteDialog.setTargetFragment(getTargetFragment(), 0);
                    lastDeleteDialog.show(getFragmentManager(), ConfirmLastDeleteDialog.class.getName());
                }
                dialog.dismiss();
            }

            @Override
            public void onSaveInstanceState(Bundle outState) {
                super.onSaveInstanceState(outState);
                if (this.mDialogTextField == null) {
                    return;
                }
                outState.putString("fingerName", this.mDialogTextField.getText().toString());
                outState.putBoolean("textHadFocus", this.mDialogTextField.hasFocus());
                outState.putInt("startSelection", this.mDialogTextField.getSelectionStart());
                outState.putInt("endSelection", this.mDialogTextField.getSelectionEnd());
            }
        }

        public static class ConfirmLastDeleteDialog extends DialogFragment {
            private Fingerprint mFp;

            @Override
            public Dialog onCreateDialog(Bundle savedInstanceState) {
                this.mFp = getArguments().getParcelable("fingerprint");
                boolean isProfileChallengeUser = getArguments().getBoolean("isProfileChallengeUser");
                AlertDialog alertDialog = new AlertDialog.Builder(getActivity()).setTitle(R.string.fingerprint_last_delete_title).setMessage(isProfileChallengeUser ? R.string.fingerprint_last_delete_message_profile_challenge : R.string.fingerprint_last_delete_message).setPositiveButton(R.string.fingerprint_last_delete_confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        FingerprintSettingsFragment parent = (FingerprintSettingsFragment) ConfirmLastDeleteDialog.this.getTargetFragment();
                        parent.deleteFingerPrint(ConfirmLastDeleteDialog.this.mFp);
                        dialog.dismiss();
                    }
                }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).create();
                return alertDialog;
            }
        }
    }

    public static class FingerprintPreference extends Preference {
        private Fingerprint mFingerprint;
        private View mView;

        public FingerprintPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
            super(context, attrs, defStyleAttr, defStyleRes);
        }

        public FingerprintPreference(Context context, AttributeSet attrs, int defStyleAttr) {
            this(context, attrs, defStyleAttr, 0);
        }

        public FingerprintPreference(Context context, AttributeSet attrs) {
            this(context, attrs, android.R.attr.preferenceStyle);
        }

        public FingerprintPreference(Context context) {
            this(context, null);
        }

        public View getView() {
            return this.mView;
        }

        public void setFingerprint(Fingerprint item) {
            this.mFingerprint = item;
        }

        public Fingerprint getFingerprint() {
            return this.mFingerprint;
        }

        @Override
        public void onBindViewHolder(PreferenceViewHolder view) {
            super.onBindViewHolder(view);
            this.mView = view.itemView;
        }
    }

    private static class LearnMoreSpan extends URLSpan {
        private static final Typeface TYPEFACE_MEDIUM = Typeface.create("sans-serif-medium", 0);
        private RestrictedLockUtils.EnforcedAdmin mEnforcedAdmin;

        private LearnMoreSpan(String url) {
            super(url);
            this.mEnforcedAdmin = null;
        }

        private LearnMoreSpan(RestrictedLockUtils.EnforcedAdmin admin) {
            super((String) null);
            this.mEnforcedAdmin = null;
            this.mEnforcedAdmin = admin;
        }

        @Override
        public void onClick(View widget) {
            Context ctx = widget.getContext();
            if (this.mEnforcedAdmin != null) {
                RestrictedLockUtils.sendShowAdminSupportDetailsIntent(ctx, this.mEnforcedAdmin);
                return;
            }
            Intent intent = HelpUtils.getHelpIntent(ctx, getURL(), ctx.getClass().getName());
            try {
                widget.startActivityForResult(intent, 0);
            } catch (ActivityNotFoundException e) {
                Log.w("FingerprintSettings", "Actvity was not found for intent, " + intent.toString());
            }
        }

        @Override
        public void updateDrawState(TextPaint ds) {
            super.updateDrawState(ds);
            ds.setUnderlineText(false);
            ds.setTypeface(TYPEFACE_MEDIUM);
        }

        public static CharSequence linkify(CharSequence rawText, String uri, RestrictedLockUtils.EnforcedAdmin admin) {
            SpannableString msg = new SpannableString(rawText);
            Annotation[] spans = (Annotation[]) msg.getSpans(0, msg.length(), Annotation.class);
            if (TextUtils.isEmpty(uri)) {
                CharSequence ret = rawText;
                for (Annotation annotation : spans) {
                    int start = msg.getSpanStart(annotation);
                    int end = msg.getSpanEnd(annotation);
                    ret = TextUtils.concat(ret.subSequence(0, start), msg.subSequence(end, msg.length()));
                }
                return ret;
            }
            SpannableStringBuilder builder = new SpannableStringBuilder(msg);
            for (Annotation annotation2 : spans) {
                String key = annotation2.getValue();
                int start2 = msg.getSpanStart(annotation2);
                int end2 = msg.getSpanEnd(annotation2);
                Object learnMoreSpan = null;
                if ("url".equals(key)) {
                    learnMoreSpan = new LearnMoreSpan(uri);
                } else if ("admin_details".equals(key)) {
                    learnMoreSpan = new LearnMoreSpan(admin);
                }
                if (learnMoreSpan != null) {
                    builder.setSpan(learnMoreSpan, start2, end2, msg.getSpanFlags(learnMoreSpan));
                }
            }
            return builder;
        }
    }

    public static Preference getFingerprintPreferenceForUser(Context context, final int userId) {
        final String clazz;
        FingerprintManager fpm = (FingerprintManager) context.getSystemService("fingerprint");
        if (fpm == null || !fpm.isHardwareDetected()) {
            Log.v("FingerprintSettings", "No fingerprint hardware detected!!");
            return null;
        }
        Preference fingerprintPreference = new Preference(context);
        fingerprintPreference.setKey("fingerprint_settings");
        fingerprintPreference.setTitle(R.string.security_settings_fingerprint_preference_title);
        List<Fingerprint> items = fpm.getEnrolledFingerprints(userId);
        int fingerprintCount = items != null ? items.size() : 0;
        if (fingerprintCount > 0) {
            fingerprintPreference.setSummary(context.getResources().getQuantityString(R.plurals.security_settings_fingerprint_preference_summary, fingerprintCount, Integer.valueOf(fingerprintCount)));
            clazz = FingerprintSettings.class.getName();
        } else {
            fingerprintPreference.setSummary(R.string.security_settings_fingerprint_preference_summary_none);
            clazz = FingerprintEnrollIntroduction.class.getName();
        }
        fingerprintPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Context context2 = preference.getContext();
                UserManager userManager = UserManager.get(context2);
                if (Utils.startQuietModeDialogIfNecessary(context2, userManager, userId)) {
                    return false;
                }
                Intent intent = new Intent();
                intent.setClassName("com.android.settings", clazz);
                intent.putExtra("android.intent.extra.USER_ID", userId);
                context2.startActivity(intent);
                return true;
            }
        });
        return fingerprintPreference;
    }
}
