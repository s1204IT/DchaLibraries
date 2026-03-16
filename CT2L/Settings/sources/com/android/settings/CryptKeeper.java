package com.android.settings;

import android.app.Activity;
import android.app.StatusBarManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.storage.IMountService;
import android.provider.Settings;
import android.telecom.TelecomManager;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockPatternView;
import java.util.List;

public class CryptKeeper extends Activity implements TextWatcher, View.OnKeyListener, View.OnTouchListener, TextView.OnEditorActionListener {
    private AudioManager mAudioManager;
    private boolean mCorrupt;
    private boolean mEncryptionGoneBad;
    private LockPatternView mLockPatternView;
    private EditText mPasswordEntry;
    private StatusBarManager mStatusBar;
    private boolean mValidationComplete;
    private boolean mValidationRequested;
    PowerManager.WakeLock mWakeLock;
    private boolean mIgnoreBack = false;
    private boolean mCooldown = false;
    private int mNotificationCountdown = 0;
    private int mReleaseWakeLockCountdown = 0;
    private int mStatusString = R.string.enter_password;
    private final Runnable mFakeUnlockAttemptRunnable = new Runnable() {
        @Override
        public void run() {
            CryptKeeper.this.handleBadAttempt(1);
        }
    };
    private final Runnable mClearPatternRunnable = new Runnable() {
        @Override
        public void run() {
            CryptKeeper.this.mLockPatternView.clearPattern();
        }
    };
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    CryptKeeper.this.updateProgress();
                    break;
                case 2:
                    CryptKeeper.this.notifyUser();
                    break;
            }
        }
    };
    protected LockPatternView.OnPatternListener mChooseNewLockPatternListener = new LockPatternView.OnPatternListener() {
        public void onPatternStart() {
            CryptKeeper.this.mLockPatternView.removeCallbacks(CryptKeeper.this.mClearPatternRunnable);
        }

        public void onPatternCleared() {
        }

        public void onPatternDetected(List<LockPatternView.Cell> pattern) {
            CryptKeeper.this.mLockPatternView.setEnabled(false);
            if (pattern.size() < 4) {
                CryptKeeper.this.fakeUnlockAttempt(CryptKeeper.this.mLockPatternView);
            } else {
                new DecryptTask().execute(LockPatternUtils.patternToString(pattern));
            }
        }

        public void onPatternCellAdded(List<LockPatternView.Cell> pattern) {
        }
    };

    private static class NonConfigurationInstanceState {
        final PowerManager.WakeLock wakelock;

        NonConfigurationInstanceState(PowerManager.WakeLock _wakelock) {
            this.wakelock = _wakelock;
        }
    }

    private class DecryptTask extends AsyncTask<String, Void, Integer> {
        private DecryptTask() {
        }

        private void hide(int id) {
            View view = CryptKeeper.this.findViewById(id);
            if (view != null) {
                view.setVisibility(8);
            }
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            CryptKeeper.this.beginAttempt();
        }

        @Override
        protected Integer doInBackground(String... params) {
            IMountService service = CryptKeeper.this.getMountService();
            try {
                return Integer.valueOf(service.decryptStorage(params[0]));
            } catch (Exception e) {
                Log.e("CryptKeeper", "Error while decrypting...", e);
                return -1;
            }
        }

        @Override
        protected void onPostExecute(Integer failedAttempts) {
            if (failedAttempts.intValue() == 0) {
                if (CryptKeeper.this.mLockPatternView != null) {
                    CryptKeeper.this.mLockPatternView.removeCallbacks(CryptKeeper.this.mClearPatternRunnable);
                    CryptKeeper.this.mLockPatternView.postDelayed(CryptKeeper.this.mClearPatternRunnable, 500L);
                }
                TextView status = (TextView) CryptKeeper.this.findViewById(R.id.status);
                status.setText(R.string.starting_android);
                hide(R.id.passwordEntry);
                hide(R.id.switch_ime_button);
                hide(R.id.lockPattern);
                hide(R.id.owner_info);
                hide(R.id.emergencyCallButton);
                return;
            }
            if (failedAttempts.intValue() == 30) {
                Intent intent = new Intent("android.intent.action.MASTER_CLEAR");
                intent.addFlags(268435456);
                intent.putExtra("android.intent.extra.REASON", "CryptKeeper.MAX_FAILED_ATTEMPTS");
                CryptKeeper.this.sendBroadcast(intent);
                return;
            }
            if (failedAttempts.intValue() != -1) {
                CryptKeeper.this.handleBadAttempt(failedAttempts);
            } else {
                CryptKeeper.this.setContentView(R.layout.crypt_keeper_progress);
                CryptKeeper.this.showFactoryReset(true);
            }
        }
    }

    private void beginAttempt() {
        TextView status = (TextView) findViewById(R.id.status);
        status.setText(R.string.checking_decryption);
    }

    private void handleBadAttempt(Integer failedAttempts) {
        if (this.mLockPatternView != null) {
            this.mLockPatternView.setDisplayMode(LockPatternView.DisplayMode.Wrong);
            this.mLockPatternView.removeCallbacks(this.mClearPatternRunnable);
            this.mLockPatternView.postDelayed(this.mClearPatternRunnable, 1500L);
        }
        if (failedAttempts.intValue() % 10 == 0) {
            this.mCooldown = true;
            cooldown();
            return;
        }
        TextView status = (TextView) findViewById(R.id.status);
        int remainingAttempts = 30 - failedAttempts.intValue();
        if (remainingAttempts < 10) {
            CharSequence warningTemplate = getText(R.string.crypt_keeper_warn_wipe);
            CharSequence warning = TextUtils.expandTemplate(warningTemplate, Integer.toString(remainingAttempts));
            status.setText(warning);
        } else {
            int passwordType = 0;
            try {
                IMountService service = getMountService();
                passwordType = service.getPasswordType();
            } catch (Exception e) {
                Log.e("CryptKeeper", "Error calling mount service " + e);
            }
            if (passwordType == 3) {
                status.setText(R.string.cryptkeeper_wrong_pin);
            } else if (passwordType == 2) {
                status.setText(R.string.cryptkeeper_wrong_pattern);
            } else {
                status.setText(R.string.cryptkeeper_wrong_password);
            }
        }
        if (this.mLockPatternView != null) {
            this.mLockPatternView.setDisplayMode(LockPatternView.DisplayMode.Wrong);
            this.mLockPatternView.setEnabled(true);
        }
        if (this.mPasswordEntry != null) {
            this.mPasswordEntry.setEnabled(true);
            InputMethodManager imm = (InputMethodManager) getSystemService("input_method");
            imm.showSoftInput(this.mPasswordEntry, 0);
            setBackFunctionality(true);
        }
    }

    private class ValidationTask extends AsyncTask<Void, Void, Boolean> {
        int state;

        private ValidationTask() {
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            boolean zValueOf;
            IMountService service = CryptKeeper.this.getMountService();
            try {
                Log.d("CryptKeeper", "Validating encryption state.");
                this.state = service.getEncryptionState();
                if (this.state == 1) {
                    Log.w("CryptKeeper", "Unexpectedly in CryptKeeper even though there is no encryption.");
                    zValueOf = true;
                } else {
                    zValueOf = Boolean.valueOf(this.state == 0);
                }
                return zValueOf;
            } catch (RemoteException e) {
                Log.w("CryptKeeper", "Unable to get encryption state properly");
                return true;
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            CryptKeeper.this.mValidationComplete = true;
            if (Boolean.FALSE.equals(result)) {
                Log.w("CryptKeeper", "Incomplete, or corrupted encryption detected. Prompting user to wipe.");
                CryptKeeper.this.mEncryptionGoneBad = true;
                CryptKeeper.this.mCorrupt = this.state == -4;
            } else {
                Log.d("CryptKeeper", "Encryption state validated. Proceeding to configure UI");
            }
            CryptKeeper.this.setupUi();
        }
    }

    private boolean isDebugView() {
        return getIntent().hasExtra("com.android.settings.CryptKeeper.DEBUG_FORCE_VIEW");
    }

    private boolean isDebugView(String viewType) {
        return viewType.equals(getIntent().getStringExtra("com.android.settings.CryptKeeper.DEBUG_FORCE_VIEW"));
    }

    private void notifyUser() {
        if (this.mNotificationCountdown > 0) {
            this.mNotificationCountdown--;
        } else if (this.mAudioManager != null) {
            try {
                this.mAudioManager.playSoundEffect(5, 100);
            } catch (Exception e) {
                Log.w("CryptKeeper", "notifyUser: Exception while playing sound: " + e);
            }
        }
        this.mHandler.removeMessages(2);
        this.mHandler.sendEmptyMessageDelayed(2, 5000L);
        if (this.mWakeLock.isHeld()) {
            if (this.mReleaseWakeLockCountdown > 0) {
                this.mReleaseWakeLockCountdown--;
            } else {
                this.mWakeLock.release();
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (!this.mIgnoreBack) {
            super.onBackPressed();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String state = SystemProperties.get("vold.decrypt");
        if (!isDebugView() && ("".equals(state) || "trigger_restart_framework".equals(state))) {
            PackageManager pm = getPackageManager();
            ComponentName name = new ComponentName(this, (Class<?>) CryptKeeper.class);
            pm.setComponentEnabledSetting(name, 2, 1);
            finish();
            return;
        }
        try {
            if (getResources().getBoolean(R.bool.crypt_keeper_allow_rotation)) {
                setRequestedOrientation(-1);
            }
        } catch (Resources.NotFoundException e) {
        }
        this.mStatusBar = (StatusBarManager) getSystemService("statusbar");
        this.mStatusBar.disable(53936128);
        setAirplaneModeIfNecessary();
        this.mAudioManager = (AudioManager) getSystemService("audio");
        Object lastInstance = getLastNonConfigurationInstance();
        if (lastInstance instanceof NonConfigurationInstanceState) {
            NonConfigurationInstanceState retained = (NonConfigurationInstanceState) lastInstance;
            this.mWakeLock = retained.wakelock;
            Log.d("CryptKeeper", "Restoring wakelock from NonConfigurationInstanceState");
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        setupUi();
    }

    private void setupUi() {
        if (this.mEncryptionGoneBad || isDebugView("error")) {
            setContentView(R.layout.crypt_keeper_progress);
            showFactoryReset(this.mCorrupt);
            return;
        }
        String progress = SystemProperties.get("vold.encrypt_progress");
        if (!"".equals(progress) || isDebugView("progress")) {
            setContentView(R.layout.crypt_keeper_progress);
            encryptionProgressInit();
        } else if (this.mValidationComplete || isDebugView("password")) {
            new AsyncTask<Void, Void, Void>() {
                String owner_info;
                int passwordType = 0;
                boolean pattern_visible;

                @Override
                public Void doInBackground(Void... v) {
                    try {
                        IMountService service = CryptKeeper.this.getMountService();
                        this.passwordType = service.getPasswordType();
                        this.owner_info = service.getField("OwnerInfo");
                        this.pattern_visible = !"0".equals(service.getField("PatternVisible"));
                        return null;
                    } catch (Exception e) {
                        Log.e("CryptKeeper", "Error calling mount service " + e);
                        return null;
                    }
                }

                @Override
                public void onPostExecute(Void v) {
                    if (this.passwordType == 3) {
                        CryptKeeper.this.setContentView(R.layout.crypt_keeper_pin_entry);
                        CryptKeeper.this.mStatusString = R.string.enter_pin;
                    } else if (this.passwordType == 2) {
                        CryptKeeper.this.setContentView(R.layout.crypt_keeper_pattern_entry);
                        CryptKeeper.this.setBackFunctionality(false);
                        CryptKeeper.this.mStatusString = R.string.enter_pattern;
                    } else {
                        CryptKeeper.this.setContentView(R.layout.crypt_keeper_password_entry);
                        CryptKeeper.this.mStatusString = R.string.enter_password;
                    }
                    TextView status = (TextView) CryptKeeper.this.findViewById(R.id.status);
                    status.setText(CryptKeeper.this.mStatusString);
                    TextView ownerInfo = (TextView) CryptKeeper.this.findViewById(R.id.owner_info);
                    ownerInfo.setText(this.owner_info);
                    ownerInfo.setSelected(true);
                    CryptKeeper.this.passwordEntryInit();
                    if (CryptKeeper.this.mLockPatternView != null) {
                        CryptKeeper.this.mLockPatternView.setInStealthMode(this.pattern_visible ? false : true);
                    }
                    if (CryptKeeper.this.mCooldown) {
                        CryptKeeper.this.setBackFunctionality(false);
                        CryptKeeper.this.cooldown();
                    }
                }
            }.execute(new Void[0]);
        } else if (!this.mValidationRequested) {
            new ValidationTask().execute((Void[]) null);
            this.mValidationRequested = true;
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        this.mHandler.removeMessages(1);
        this.mHandler.removeMessages(2);
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        NonConfigurationInstanceState state = new NonConfigurationInstanceState(this.mWakeLock);
        Log.d("CryptKeeper", "Handing wakelock off to NonConfigurationInstanceState");
        this.mWakeLock = null;
        return state;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (this.mWakeLock != null) {
            Log.d("CryptKeeper", "Releasing and destroying wakelock");
            this.mWakeLock.release();
            this.mWakeLock = null;
        }
    }

    private void encryptionProgressInit() {
        Log.d("CryptKeeper", "Encryption progress screen initializing.");
        if (this.mWakeLock == null) {
            Log.d("CryptKeeper", "Acquiring wakelock.");
            PowerManager pm = (PowerManager) getSystemService("power");
            this.mWakeLock = pm.newWakeLock(26, "CryptKeeper");
            this.mWakeLock.acquire();
        }
        ((ProgressBar) findViewById(R.id.progress_bar)).setIndeterminate(true);
        setBackFunctionality(false);
        updateProgress();
    }

    private void showFactoryReset(final boolean corrupt) {
        findViewById(R.id.encroid).setVisibility(8);
        Button button = (Button) findViewById(R.id.factory_reset);
        button.setVisibility(0);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent("android.intent.action.MASTER_CLEAR");
                intent.addFlags(268435456);
                intent.putExtra("android.intent.extra.REASON", "CryptKeeper.showFactoryReset() corrupt=" + corrupt);
                CryptKeeper.this.sendBroadcast(intent);
            }
        });
        if (corrupt) {
            ((TextView) findViewById(R.id.title)).setText(R.string.crypt_keeper_data_corrupt_title);
            ((TextView) findViewById(R.id.status)).setText(R.string.crypt_keeper_data_corrupt_summary);
        } else {
            ((TextView) findViewById(R.id.title)).setText(R.string.crypt_keeper_failed_title);
            ((TextView) findViewById(R.id.status)).setText(R.string.crypt_keeper_failed_summary);
        }
        View view = findViewById(R.id.bottom_divider);
        if (view != null) {
            view.setVisibility(0);
        }
    }

    private void updateProgress() {
        String state = SystemProperties.get("vold.encrypt_progress");
        if ("error_partially_encrypted".equals(state)) {
            showFactoryReset(false);
            return;
        }
        CharSequence status = getText(R.string.crypt_keeper_setup_description);
        int percent = 0;
        try {
            percent = isDebugView() ? 50 : Integer.parseInt(state);
        } catch (Exception e) {
            Log.w("CryptKeeper", "Error parsing progress: " + e.toString());
        }
        String progress = Integer.toString(percent);
        Log.v("CryptKeeper", "Encryption progress: " + progress);
        try {
            String timeProperty = SystemProperties.get("vold.encrypt_time_remaining");
            int time = Integer.parseInt(timeProperty);
            if (time >= 0) {
                progress = DateUtils.formatElapsedTime(((time + 9) / 10) * 10);
                status = getText(R.string.crypt_keeper_setup_time_remaining);
            }
        } catch (Exception e2) {
        }
        TextView tv = (TextView) findViewById(R.id.status);
        if (tv != null) {
            tv.setText(TextUtils.expandTemplate(status, progress));
        }
        this.mHandler.removeMessages(1);
        this.mHandler.sendEmptyMessageDelayed(1, 1000L);
    }

    private void cooldown() {
        if (this.mPasswordEntry != null) {
            this.mPasswordEntry.setEnabled(false);
        }
        if (this.mLockPatternView != null) {
            this.mLockPatternView.setEnabled(false);
        }
        TextView status = (TextView) findViewById(R.id.status);
        status.setText(R.string.crypt_keeper_force_power_cycle);
    }

    private final void setBackFunctionality(boolean isEnabled) {
        this.mIgnoreBack = !isEnabled;
        if (isEnabled) {
            this.mStatusBar.disable(53936128);
        } else {
            this.mStatusBar.disable(58130432);
        }
    }

    private void fakeUnlockAttempt(View postingView) {
        beginAttempt();
        postingView.postDelayed(this.mFakeUnlockAttemptRunnable, 1000L);
    }

    private void passwordEntryInit() {
        View emergencyCall;
        this.mPasswordEntry = (EditText) findViewById(R.id.passwordEntry);
        if (this.mPasswordEntry != null) {
            this.mPasswordEntry.setOnEditorActionListener(this);
            this.mPasswordEntry.requestFocus();
            this.mPasswordEntry.setOnKeyListener(this);
            this.mPasswordEntry.setOnTouchListener(this);
            this.mPasswordEntry.addTextChangedListener(this);
        }
        this.mLockPatternView = findViewById(R.id.lockPattern);
        if (this.mLockPatternView != null) {
            this.mLockPatternView.setOnPatternListener(this.mChooseNewLockPatternListener);
        }
        if (!getTelephonyManager().isVoiceCapable() && (emergencyCall = findViewById(R.id.emergencyCallButton)) != null) {
            Log.d("CryptKeeper", "Removing the emergency Call button");
            emergencyCall.setVisibility(8);
        }
        View imeSwitcher = findViewById(R.id.switch_ime_button);
        final InputMethodManager imm = (InputMethodManager) getSystemService("input_method");
        if (imeSwitcher != null && hasMultipleEnabledIMEsOrSubtypes(imm, false)) {
            imeSwitcher.setVisibility(0);
            imeSwitcher.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    imm.showInputMethodPicker();
                }
            });
        }
        if (this.mWakeLock == null) {
            Log.d("CryptKeeper", "Acquiring wakelock.");
            PowerManager pm = (PowerManager) getSystemService("power");
            if (pm != null) {
                this.mWakeLock = pm.newWakeLock(26, "CryptKeeper");
                this.mWakeLock.acquire();
                this.mReleaseWakeLockCountdown = 96;
            }
        }
        if (this.mLockPatternView == null && !this.mCooldown) {
            this.mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    imm.showSoftInputUnchecked(0, null);
                }
            }, 0L);
        }
        updateEmergencyCallButtonState();
        this.mHandler.removeMessages(2);
        this.mHandler.sendEmptyMessageDelayed(2, 120000L);
        getWindow().addFlags(4718592);
    }

    private boolean hasMultipleEnabledIMEsOrSubtypes(InputMethodManager imm, boolean shouldIncludeAuxiliarySubtypes) {
        List<InputMethodInfo> enabledImis = imm.getEnabledInputMethodList();
        int filteredImisCount = 0;
        for (InputMethodInfo imi : enabledImis) {
            if (filteredImisCount > 1) {
                return true;
            }
            List<InputMethodSubtype> subtypes = imm.getEnabledInputMethodSubtypeList(imi, true);
            if (subtypes.isEmpty()) {
                filteredImisCount++;
            } else {
                int auxCount = 0;
                for (InputMethodSubtype subtype : subtypes) {
                    if (subtype.isAuxiliary()) {
                        auxCount++;
                    }
                }
                int nonAuxCount = subtypes.size() - auxCount;
                if (nonAuxCount > 0 || (shouldIncludeAuxiliarySubtypes && auxCount > 1)) {
                    filteredImisCount++;
                }
            }
        }
        return filteredImisCount > 1 || imm.getEnabledInputMethodSubtypeList(null, false).size() > 1;
    }

    private IMountService getMountService() {
        IBinder service = ServiceManager.getService("mount");
        if (service != null) {
            return IMountService.Stub.asInterface(service);
        }
        return null;
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId != 0 && actionId != 6) {
            return false;
        }
        String password = v.getText().toString();
        if (TextUtils.isEmpty(password)) {
            return true;
        }
        v.setText((CharSequence) null);
        this.mPasswordEntry.setEnabled(false);
        setBackFunctionality(false);
        if (password.length() >= 4) {
            new DecryptTask().execute(password);
        } else {
            fakeUnlockAttempt(this.mPasswordEntry);
        }
        return true;
    }

    private final void setAirplaneModeIfNecessary() {
        boolean isLteDevice = getTelephonyManager().getLteOnCdmaMode() == 1;
        if (!isLteDevice) {
            Log.d("CryptKeeper", "Going into airplane mode.");
            Settings.Global.putInt(getContentResolver(), "airplane_mode_on", 1);
            Intent intent = new Intent("android.intent.action.AIRPLANE_MODE");
            intent.putExtra("state", true);
            sendBroadcastAsUser(intent, UserHandle.ALL);
        }
    }

    private void updateEmergencyCallButtonState() {
        int textId;
        Button emergencyCall = (Button) findViewById(R.id.emergencyCallButton);
        if (emergencyCall != null) {
            if (isEmergencyCallCapable()) {
                emergencyCall.setVisibility(0);
                emergencyCall.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        CryptKeeper.this.takeEmergencyCallAction();
                    }
                });
                if (getTelecomManager().isInCall()) {
                    textId = R.string.cryptkeeper_return_to_call;
                } else {
                    textId = R.string.cryptkeeper_emergency_call;
                }
                emergencyCall.setText(textId);
                return;
            }
            emergencyCall.setVisibility(8);
        }
    }

    private boolean isEmergencyCallCapable() {
        return getResources().getBoolean(android.R.^attr-private.externalRouteEnabledDrawable);
    }

    private void takeEmergencyCallAction() {
        TelecomManager telecomManager = getTelecomManager();
        if (telecomManager.isInCall()) {
            telecomManager.showInCallScreen(false);
        } else {
            launchEmergencyDialer();
        }
    }

    private void launchEmergencyDialer() {
        Intent intent = new Intent("com.android.phone.EmergencyDialer.DIAL");
        intent.setFlags(276824064);
        setBackFunctionality(true);
        startActivity(intent);
    }

    private TelephonyManager getTelephonyManager() {
        return (TelephonyManager) getSystemService("phone");
    }

    private TelecomManager getTelecomManager() {
        return (TelecomManager) getSystemService("telecom");
    }

    private void delayAudioNotification() {
        this.mNotificationCountdown = 20;
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        delayAudioNotification();
        return false;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        delayAudioNotification();
        return false;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        delayAudioNotification();
    }

    @Override
    public void afterTextChanged(Editable s) {
    }
}
