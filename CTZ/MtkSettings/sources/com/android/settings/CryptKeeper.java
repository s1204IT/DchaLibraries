package com.android.settings;

import android.app.Activity;
import android.app.StatusBarManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.os.storage.IStorageManager;
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
import android.widget.ProgressBar;
import android.widget.TextView;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockPatternView;
import com.android.settings.widget.ImeAwareEditText;
import java.util.Iterator;
import java.util.List;

public class CryptKeeper extends Activity implements TextWatcher, View.OnKeyListener, View.OnTouchListener, TextView.OnEditorActionListener {
    private AudioManager mAudioManager;
    private boolean mCorrupt;
    private boolean mEncryptionGoneBad;
    private LockPatternView mLockPatternView;
    private ImeAwareEditText mPasswordEntry;
    private PhoneStateBroadcastReceiver mPhoneStateReceiver;
    private StatusBarManager mStatusBar;
    private boolean mValidationComplete;
    private boolean mValidationRequested;
    PowerManager.WakeLock mWakeLock;
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
        public void handleMessage(Message message) {
            switch (message.what) {
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

        public void onPatternDetected(List<LockPatternView.Cell> list) {
            CryptKeeper.this.mLockPatternView.setEnabled(false);
            if (list.size() < 4) {
                CryptKeeper.this.fakeUnlockAttempt(CryptKeeper.this.mLockPatternView);
            } else {
                new DecryptTask().execute(LockPatternUtils.patternToString(list));
            }
        }

        public void onPatternCellAdded(List<LockPatternView.Cell> list) {
        }
    };

    private static class NonConfigurationInstanceState {
        final PowerManager.WakeLock wakelock;

        NonConfigurationInstanceState(PowerManager.WakeLock wakeLock) {
            this.wakelock = wakeLock;
        }
    }

    private class DecryptTask extends AsyncTask<String, Void, Integer> {
        private DecryptTask() {
        }

        private void hide(int i) {
            View viewFindViewById = CryptKeeper.this.findViewById(i);
            if (viewFindViewById != null) {
                viewFindViewById.setVisibility(8);
            }
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            CryptKeeper.this.beginAttempt();
        }

        @Override
        protected Integer doInBackground(String... strArr) {
            try {
                return Integer.valueOf(CryptKeeper.this.getStorageManager().decryptStorage(strArr[0]));
            } catch (Exception e) {
                Log.e("CryptKeeper", "Error while decrypting...", e);
                return -1;
            }
        }

        @Override
        protected void onPostExecute(Integer num) {
            Log.d("CryptKeeper", "failedAttempts : " + num);
            if (num.intValue() == 0) {
                if (CryptKeeper.this.mLockPatternView != null) {
                    CryptKeeper.this.mLockPatternView.removeCallbacks(CryptKeeper.this.mClearPatternRunnable);
                    CryptKeeper.this.mLockPatternView.postDelayed(CryptKeeper.this.mClearPatternRunnable, 500L);
                }
                ((TextView) CryptKeeper.this.findViewById(R.id.status)).setText(R.string.starting_android);
                hide(R.id.passwordEntry);
                hide(R.id.switch_ime_button);
                hide(R.id.lockPattern);
                hide(R.id.owner_info);
                hide(R.id.emergencyCallButton);
                return;
            }
            if (num.intValue() == 30) {
                Intent intent = new Intent("android.intent.action.FACTORY_RESET");
                intent.setPackage("android");
                intent.addFlags(268435456);
                intent.putExtra("android.intent.extra.REASON", "CryptKeeper.MAX_FAILED_ATTEMPTS");
                CryptKeeper.this.sendBroadcast(intent);
                return;
            }
            if (num.intValue() != -1) {
                CryptKeeper.this.handleBadAttempt(num);
            } else {
                CryptKeeper.this.setContentView(R.layout.crypt_keeper_progress);
                CryptKeeper.this.showFactoryReset(true);
            }
        }
    }

    private void beginAttempt() {
        ((TextView) findViewById(R.id.status)).setText(R.string.checking_decryption);
    }

    private void handleBadAttempt(Integer num) {
        int passwordType;
        if (this.mLockPatternView != null) {
            this.mLockPatternView.setDisplayMode(LockPatternView.DisplayMode.Wrong);
            this.mLockPatternView.removeCallbacks(this.mClearPatternRunnable);
            this.mLockPatternView.postDelayed(this.mClearPatternRunnable, 1500L);
        }
        if (num.intValue() % 10 == 0) {
            this.mCooldown = true;
            cooldown();
            return;
        }
        TextView textView = (TextView) findViewById(R.id.status);
        int iIntValue = 30 - num.intValue();
        if (iIntValue < 10) {
            textView.setText(TextUtils.expandTemplate(getText(R.string.crypt_keeper_warn_wipe), Integer.toString(iIntValue)));
        } else {
            try {
                passwordType = getStorageManager().getPasswordType();
            } catch (Exception e) {
                Log.e("CryptKeeper", "Error calling mount service " + e);
                passwordType = 0;
            }
            if (passwordType == 3) {
                textView.setText(R.string.cryptkeeper_wrong_pin);
            } else if (passwordType == 2) {
                textView.setText(R.string.cryptkeeper_wrong_pattern);
            } else {
                textView.setText(R.string.cryptkeeper_wrong_password);
            }
        }
        if (this.mLockPatternView != null) {
            this.mLockPatternView.setDisplayMode(LockPatternView.DisplayMode.Wrong);
            this.mLockPatternView.setEnabled(true);
        }
        if (this.mPasswordEntry != null) {
            this.mPasswordEntry.setEnabled(true);
            this.mPasswordEntry.scheduleShowSoftInput();
            setBackFunctionality(true);
        }
    }

    private class ValidationTask extends AsyncTask<Void, Void, Boolean> {
        int state;

        private ValidationTask() {
        }

        @Override
        protected Boolean doInBackground(Void... voidArr) {
            IStorageManager storageManager = CryptKeeper.this.getStorageManager();
            try {
                Log.d("CryptKeeper", "Validating encryption state.");
                this.state = storageManager.getEncryptionState();
                if (this.state != 1) {
                    return Boolean.valueOf(this.state == 0);
                }
                Log.w("CryptKeeper", "Unexpectedly in CryptKeeper even though there is no encryption.");
                return true;
            } catch (RemoteException e) {
                Log.w("CryptKeeper", "Unable to get encryption state properly");
                return true;
            }
        }

        @Override
        protected void onPostExecute(Boolean bool) {
            CryptKeeper.this.mValidationComplete = true;
            if (Boolean.FALSE.equals(bool)) {
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

    private boolean isDebugView(String str) {
        return str.equals(getIntent().getStringExtra("com.android.settings.CryptKeeper.DEBUG_FORCE_VIEW"));
    }

    private void notifyUser() {
        if (this.mNotificationCountdown > 0) {
            Log.d("CryptKeeper", "Counting down to notify user..." + this.mNotificationCountdown);
            this.mNotificationCountdown = this.mNotificationCountdown + (-1);
        } else if (this.mAudioManager != null) {
            Log.d("CryptKeeper", "Notifying user that we are waiting for input...");
            try {
                this.mAudioManager.playSoundEffect(5);
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
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Log.d("CryptKeeper", "onCreate()");
        String str = SystemProperties.get("vold.decrypt");
        if (!isDebugView() && ("".equals(str) || "trigger_restart_framework".equals(str))) {
            disableCryptKeeperComponent(this);
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
        this.mStatusBar.disable(52887552);
        if (bundle != null) {
            this.mCooldown = bundle.getBoolean("cooldown");
        }
        setAirplaneModeIfNecessary();
        this.mAudioManager = (AudioManager) getSystemService("audio");
        Object lastNonConfigurationInstance = getLastNonConfigurationInstance();
        if (lastNonConfigurationInstance instanceof NonConfigurationInstanceState) {
            this.mWakeLock = ((NonConfigurationInstanceState) lastNonConfigurationInstance).wakelock;
            Log.d("CryptKeeper", "Restoring wakelock from NonConfigurationInstanceState");
        }
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        bundle.putBoolean("cooldown", this.mCooldown);
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d("CryptKeeper", "onStart()");
        listenPhoneStateBroadcast(this);
        setupUi();
    }

    private void setupUi() {
        if (this.mEncryptionGoneBad || isDebugView("error")) {
            setContentView(R.layout.crypt_keeper_progress);
            showFactoryReset(this.mCorrupt);
            return;
        }
        if (!"".equals(SystemProperties.get("vold.encrypt_progress")) || isDebugView("progress")) {
            setContentView(R.layout.crypt_keeper_progress);
            encryptionProgressInit();
        } else if (this.mValidationComplete || isDebugView("password")) {
            new AsyncTask<Void, Void, Void>() {
                String owner_info;
                int passwordType = 0;
                boolean password_visible;
                boolean pattern_visible;

                @Override
                public Void doInBackground(Void... voidArr) {
                    try {
                        IStorageManager storageManager = CryptKeeper.this.getStorageManager();
                        this.passwordType = storageManager.getPasswordType();
                        this.owner_info = storageManager.getField("OwnerInfo");
                        this.pattern_visible = !"0".equals(storageManager.getField("PatternVisible"));
                        this.password_visible = !"0".equals(storageManager.getField("PasswordVisible"));
                        return null;
                    } catch (Exception e) {
                        Log.e("CryptKeeper", "Error calling mount service " + e);
                        return null;
                    }
                }

                @Override
                public void onPostExecute(Void r4) {
                    Settings.System.putInt(CryptKeeper.this.getContentResolver(), "show_password", this.password_visible ? 1 : 0);
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
                    ((TextView) CryptKeeper.this.findViewById(R.id.status)).setText(CryptKeeper.this.mStatusString);
                    TextView textView = (TextView) CryptKeeper.this.findViewById(R.id.owner_info);
                    textView.setText(this.owner_info);
                    textView.setSelected(true);
                    CryptKeeper.this.passwordEntryInit();
                    CryptKeeper.this.findViewById(android.R.id.content).setSystemUiVisibility(4194816);
                    if (CryptKeeper.this.mLockPatternView != null) {
                        CryptKeeper.this.mLockPatternView.setInStealthMode(true ^ this.pattern_visible);
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
        removePhoneStateBroadcast(this);
        this.mHandler.removeMessages(1);
        this.mHandler.removeMessages(2);
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        NonConfigurationInstanceState nonConfigurationInstanceState = new NonConfigurationInstanceState(this.mWakeLock);
        Log.d("CryptKeeper", "Handing wakelock off to NonConfigurationInstanceState");
        this.mWakeLock = null;
        return nonConfigurationInstanceState;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("CryptKeeper", "onDestroy()");
        if (this.mWakeLock != null) {
            Log.d("CryptKeeper", "Releasing and destroying wakelock");
            if (this.mWakeLock.isHeld()) {
                this.mWakeLock.release();
            }
            this.mWakeLock = null;
        }
        if (this.mStatusBar != null) {
            this.mStatusBar.disable(0);
        }
    }

    private void encryptionProgressInit() {
        Log.d("CryptKeeper", "Encryption progress screen initializing.");
        if (this.mWakeLock == null) {
            Log.d("CryptKeeper", "Acquiring wakelock.");
            this.mWakeLock = ((PowerManager) getSystemService("power")).newWakeLock(26, "CryptKeeper");
            this.mWakeLock.acquire();
        }
        ((ProgressBar) findViewById(R.id.progress_bar)).setIndeterminate(true);
        setBackFunctionality(false);
        updateProgress();
    }

    private void showFactoryReset(final boolean z) {
        findViewById(R.id.encroid).setVisibility(8);
        Button button = (Button) findViewById(R.id.factory_reset);
        button.setVisibility(0);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent("android.intent.action.FACTORY_RESET");
                intent.setPackage("android");
                intent.addFlags(268435456);
                intent.putExtra("android.intent.extra.REASON", "CryptKeeper.showFactoryReset() corrupt=" + z);
                CryptKeeper.this.sendBroadcast(intent);
            }
        });
        if (z) {
            ((TextView) findViewById(R.id.title)).setText(R.string.crypt_keeper_data_corrupt_title);
            ((TextView) findViewById(R.id.status)).setText(R.string.crypt_keeper_data_corrupt_summary);
        } else {
            ((TextView) findViewById(R.id.title)).setText(R.string.crypt_keeper_failed_title);
            ((TextView) findViewById(R.id.status)).setText(R.string.crypt_keeper_failed_summary);
        }
        View viewFindViewById = findViewById(R.id.bottom_divider);
        if (viewFindViewById != null) {
            viewFindViewById.setVisibility(0);
        }
    }

    private void updateProgress() {
        int i;
        String str = SystemProperties.get("vold.encrypt_progress");
        if ("error_partially_encrypted".equals(str)) {
            showFactoryReset(false);
            return;
        }
        CharSequence text = getText(R.string.crypt_keeper_setup_description);
        try {
            i = isDebugView() ? 50 : Integer.parseInt(str);
        } catch (Exception e) {
            Log.w("CryptKeeper", "Error parsing progress: " + e.toString());
            i = 0;
        }
        String string = Integer.toString(i);
        Log.v("CryptKeeper", "Encryption progress: " + string);
        try {
            if (Integer.parseInt(SystemProperties.get("vold.encrypt_time_remaining")) >= 0) {
                String elapsedTime = DateUtils.formatElapsedTime(((r3 + 9) / 10) * 10);
                try {
                    text = getText(R.string.crypt_keeper_setup_time_remaining);
                    string = elapsedTime;
                } catch (Exception e2) {
                    string = elapsedTime;
                }
            }
        } catch (Exception e3) {
        }
        TextView textView = (TextView) findViewById(R.id.status);
        if (textView != null) {
            textView.setText(TextUtils.expandTemplate(text, string));
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
        ((TextView) findViewById(R.id.status)).setText(R.string.crypt_keeper_force_power_cycle);
    }

    private final void setBackFunctionality(boolean z) {
        if (z) {
            this.mStatusBar.disable(52887552);
        } else {
            this.mStatusBar.disable(57081856);
        }
    }

    private void fakeUnlockAttempt(View view) {
        beginAttempt();
        view.postDelayed(this.mFakeUnlockAttemptRunnable, 1000L);
    }

    private void passwordEntryInit() {
        View viewFindViewById;
        Log.d("CryptKeeper", "passwordEntryInit().");
        this.mPasswordEntry = (ImeAwareEditText) findViewById(R.id.passwordEntry);
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
        if (!getTelephonyManager().isVoiceCapable() && (viewFindViewById = findViewById(R.id.emergencyCallButton)) != null) {
            Log.d("CryptKeeper", "Removing the emergency Call button");
            viewFindViewById.setVisibility(8);
        }
        View viewFindViewById2 = findViewById(R.id.switch_ime_button);
        final InputMethodManager inputMethodManager = (InputMethodManager) getSystemService("input_method");
        if (viewFindViewById2 != null && !isPatternLockType() && hasMultipleEnabledIMEsOrSubtypes(inputMethodManager, false)) {
            viewFindViewById2.setVisibility(0);
            viewFindViewById2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    inputMethodManager.showInputMethodPicker(false);
                }
            });
        }
        if (this.mWakeLock == null) {
            Log.d("CryptKeeper", "Acquiring wakelock.");
            PowerManager powerManager = (PowerManager) getSystemService("power");
            if (powerManager != null) {
                this.mWakeLock = powerManager.newWakeLock(26, "CryptKeeper");
                this.mWakeLock.acquire();
                this.mReleaseWakeLockCountdown = 96;
            }
        }
        if (this.mLockPatternView == null && !this.mCooldown) {
            getWindow().setSoftInputMode(5);
            if (this.mPasswordEntry != null) {
                this.mPasswordEntry.scheduleShowSoftInput();
            }
        }
        updateEmergencyCallButtonState();
        this.mHandler.removeMessages(2);
        this.mHandler.sendEmptyMessageDelayed(2, 120000L);
        getWindow().addFlags(4718592);
    }

    private boolean hasMultipleEnabledIMEsOrSubtypes(InputMethodManager inputMethodManager, boolean z) {
        int i = 0;
        for (InputMethodInfo inputMethodInfo : inputMethodManager.getEnabledInputMethodList()) {
            if (i > 1) {
                return true;
            }
            List<InputMethodSubtype> enabledInputMethodSubtypeList = inputMethodManager.getEnabledInputMethodSubtypeList(inputMethodInfo, true);
            if (enabledInputMethodSubtypeList.isEmpty()) {
                i++;
            } else {
                Iterator<InputMethodSubtype> it = enabledInputMethodSubtypeList.iterator();
                int i2 = 0;
                while (it.hasNext()) {
                    if (it.next().isAuxiliary()) {
                        i2++;
                    }
                }
                if (enabledInputMethodSubtypeList.size() - i2 > 0 || (z && i2 > 1)) {
                    i++;
                }
            }
        }
        return i > 1 || inputMethodManager.getEnabledInputMethodSubtypeList(null, false).size() > 1;
    }

    private IStorageManager getStorageManager() {
        IBinder service = ServiceManager.getService("mount");
        if (service != null) {
            return IStorageManager.Stub.asInterface(service);
        }
        return null;
    }

    @Override
    public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
        if (i != 0 && i != 6) {
            return false;
        }
        String string = textView.getText().toString();
        if (TextUtils.isEmpty(string)) {
            return true;
        }
        textView.setText((CharSequence) null);
        this.mPasswordEntry.setEnabled(false);
        setBackFunctionality(false);
        if (string.length() >= 4) {
            new DecryptTask().execute(string);
        } else {
            fakeUnlockAttempt(this.mPasswordEntry);
        }
        return true;
    }

    private final void setAirplaneModeIfNecessary() {
        boolean z;
        if (getTelephonyManager().getLteOnCdmaMode() != 1) {
            z = false;
        } else {
            z = true;
        }
        if (!z) {
            Log.d("CryptKeeper", "Going into airplane mode.");
            Settings.Global.putInt(getContentResolver(), "airplane_mode_on", 1);
            Intent intent = new Intent("android.intent.action.AIRPLANE_MODE");
            intent.putExtra("state", true);
            sendBroadcastAsUser(intent, UserHandle.ALL);
        }
    }

    private void updateEmergencyCallButtonState() {
        int i;
        Button button = (Button) findViewById(R.id.emergencyCallButton);
        if (button == null) {
            return;
        }
        if (isEmergencyCallCapable()) {
            button.setVisibility(0);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    CryptKeeper.this.takeEmergencyCallAction();
                }
            });
            if (getTelecomManager().isInCall()) {
                i = R.string.cryptkeeper_return_to_call;
                Log.d("CryptKeeper", "show cryptkeeper_return_to_call");
            } else {
                i = R.string.cryptkeeper_emergency_call;
                Log.d("CryptKeeper", "show cryptkeeper_emergency_call");
            }
            button.setText(i);
            return;
        }
        button.setVisibility(8);
    }

    private boolean isEmergencyCallCapable() {
        return getResources().getBoolean(android.R.^attr-private.popupPromptView);
    }

    private void takeEmergencyCallAction() {
        TelecomManager telecomManager = getTelecomManager();
        Log.d("CryptKeeper", "onClick Button telecomManager.isInCall() = " + telecomManager.isInCall());
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
    public boolean onKey(View view, int i, KeyEvent keyEvent) {
        delayAudioNotification();
        return false;
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        delayAudioNotification();
        return false;
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        delayAudioNotification();
    }

    @Override
    public void afterTextChanged(Editable editable) {
    }

    private static void disableCryptKeeperComponent(Context context) {
        PackageManager packageManager = context.getPackageManager();
        ComponentName componentName = new ComponentName(context, (Class<?>) CryptKeeper.class);
        Log.d("CryptKeeper", "Disabling component " + componentName);
        packageManager.setComponentEnabledSetting(componentName, 2, 1);
    }

    private class PhoneStateBroadcastReceiver extends BroadcastReceiver {
        private PhoneStateBroadcastReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.intent.action.PHONE_STATE".equals(action)) {
                Log.d("CryptKeeper", "PhoneStateBroadcastReceiver action:" + action + " state:" + intent.getExtra("state"));
                CryptKeeper.this.updateEmergencyCallButtonState();
            }
        }
    }

    private void listenPhoneStateBroadcast(Activity activity) {
        this.mPhoneStateReceiver = new PhoneStateBroadcastReceiver();
        activity.registerReceiver(this.mPhoneStateReceiver, new IntentFilter("android.intent.action.PHONE_STATE"));
    }

    private void removePhoneStateBroadcast(Activity activity) {
        if (this.mPhoneStateReceiver != null) {
            activity.unregisterReceiver(this.mPhoneStateReceiver);
            this.mPhoneStateReceiver = null;
        }
    }

    private boolean isPatternLockType() {
        try {
            IStorageManager storageManager = getStorageManager();
            if (storageManager == null) {
                return false;
            }
            storageManager.getPasswordType();
            return storageManager.getPasswordType() == 2;
        } catch (Exception e) {
            Log.e("CryptKeeper", "Error calling mount service " + e);
            return false;
        }
    }
}
