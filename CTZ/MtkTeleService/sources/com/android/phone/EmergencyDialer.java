package com.android.phone;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Point;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.telecom.TelecomManager;
import android.telephony.CarrierConfigManager;
import android.telephony.PhoneNumberUtils;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.DialerKeyListener;
import android.text.style.TtsSpan;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import com.android.internal.colorextraction.ColorExtractor;
import com.android.internal.colorextraction.drawable.GradientDrawable;
import com.android.phone.common.dialpad.DialpadKeyButton;
import com.android.phone.common.util.ViewUtil;
import com.android.phone.common.widget.ResizingTextEditText;

public class EmergencyDialer extends Activity implements TextWatcher, View.OnClickListener, View.OnKeyListener, View.OnLongClickListener, ColorExtractor.OnColorsChangedListener, DialpadKeyButton.OnPressedListener {
    public static final String ACTION_DIAL = "com.android.phone.EmergencyDialer.DIAL";
    private static final int BACKGROUND_GRADIENT_ALPHA = 230;
    private static final int BAD_EMERGENCY_NUMBER_DIALOG = 0;
    private static final boolean DBG = false;
    private static final int[] DIALER_KEYS = {R.id.one, R.id.two, R.id.three, R.id.four, R.id.five, R.id.six, R.id.seven, R.id.eight, R.id.nine, R.id.star, R.id.zero, R.id.pound};
    private static final int DIAL_TONE_STREAM_TYPE = 8;
    private static final String LAST_NUMBER = "lastNumber";
    private static final String LOG_TAG = "EmergencyDialer";
    private static final int TONE_LENGTH_MS = 150;
    private static final int TONE_RELATIVE_VOLUME = 80;
    private GradientDrawable mBackgroundGradient;
    private ColorExtractor mColorExtractor;
    private boolean mDTMFToneEnabled;
    private float mDefaultDigitsTextSize;
    private View mDelete;
    private View mDialButton;
    ResizingTextEditText mDigits;
    private EmergencyActionGroup mEmergencyActionGroup;
    private boolean mIsWfcEmergencyCallingWarningEnabled;
    private String mLastNumber;
    private boolean mSupportsDarkText;
    private ToneGenerator mToneGenerator;
    private Object mToneGeneratorLock = new Object();
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.SCREEN_OFF".equals(intent.getAction())) {
                EmergencyDialer.this.finishAndRemoveTask();
            }
        }
    };

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        maybeChangeHintSize();
    }

    @Override
    public void afterTextChanged(Editable editable) {
        if (UserHandle.myUserId() == 0 && SpecialCharSequenceMgr.handleCharsForLockedDevice(this, editable.toString(), this)) {
            this.mDigits.getText().clear();
        }
        updateDialAndDeleteButtonStateEnabledAttr();
        updateTtsSpans();
    }

    @Override
    protected void onCreate(Bundle bundle) {
        String numberFromIntent;
        super.onCreate(bundle);
        WindowManager.LayoutParams attributes = getWindow().getAttributes();
        attributes.flags |= 524288;
        getWindow().setAttributes(attributes);
        this.mColorExtractor = new ColorExtractor(this);
        updateTheme(this.mColorExtractor.getColors(2, 2).supportsDarkText());
        setContentView(R.layout.emergency_dialer);
        this.mDigits = (ResizingTextEditText) findViewById(R.id.digits);
        this.mDigits.setKeyListener(DialerKeyListener.getInstance());
        this.mDigits.setOnClickListener(this);
        this.mDigits.setOnKeyListener(this);
        this.mDigits.setLongClickable(false);
        this.mDigits.setInputType(0);
        this.mDefaultDigitsTextSize = this.mDigits.getScaledTextSize();
        maybeAddNumberFormatting();
        this.mBackgroundGradient = new GradientDrawable(this);
        Point point = new Point();
        ((WindowManager) getSystemService("window")).getDefaultDisplay().getSize(point);
        this.mBackgroundGradient.setScreenSize(point.x, point.y);
        this.mBackgroundGradient.setAlpha(BACKGROUND_GRADIENT_ALPHA);
        getWindow().setBackgroundDrawable(this.mBackgroundGradient);
        if (findViewById(R.id.one) != null) {
            setupKeypad();
        }
        this.mDelete = findViewById(R.id.deleteButton);
        this.mDelete.setOnClickListener(this);
        this.mDelete.setOnLongClickListener(this);
        this.mDialButton = findViewById(R.id.floating_action_button);
        PersistableBundle configForSubId = ((CarrierConfigManager) getSystemService("carrier_config")).getConfigForSubId(SubscriptionManager.getDefaultVoiceSubscriptionId());
        if (configForSubId.getBoolean("show_onscreen_dial_button_bool")) {
            this.mDialButton.setOnClickListener(this);
        } else {
            this.mDialButton.setVisibility(8);
        }
        this.mIsWfcEmergencyCallingWarningEnabled = configForSubId.getInt("emergency_notification_delay_int") > -1;
        maybeShowWfcEmergencyCallingWarning();
        ViewUtil.setupFloatingActionButton(this.mDialButton, getResources());
        if (bundle != null) {
            super.onRestoreInstanceState(bundle);
        }
        Uri data = getIntent().getData();
        if (data != null && "tel".equals(data.getScheme()) && (numberFromIntent = PhoneNumberUtils.getNumberFromIntent(getIntent(), this)) != null) {
            this.mDigits.setText(numberFromIntent);
        }
        synchronized (this.mToneGeneratorLock) {
            if (this.mToneGenerator == null) {
                try {
                    this.mToneGenerator = new ToneGenerator(8, 80);
                } catch (RuntimeException e) {
                    Log.w(LOG_TAG, "Exception caught while creating local tone generator: " + e);
                    this.mToneGenerator = null;
                }
            }
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.SCREEN_OFF");
        registerReceiver(this.mBroadcastReceiver, intentFilter);
        this.mEmergencyActionGroup = (EmergencyActionGroup) findViewById(R.id.emergency_action_group);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        synchronized (this.mToneGeneratorLock) {
            if (this.mToneGenerator != null) {
                this.mToneGenerator.release();
                this.mToneGenerator = null;
            }
        }
        unregisterReceiver(this.mBroadcastReceiver);
    }

    @Override
    protected void onRestoreInstanceState(Bundle bundle) {
        this.mLastNumber = bundle.getString(LAST_NUMBER);
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putString(LAST_NUMBER, this.mLastNumber);
    }

    protected void maybeAddNumberFormatting() {
    }

    @Override
    protected void onPostCreate(Bundle bundle) {
        super.onPostCreate(bundle);
        this.mDigits.addTextChangedListener(this);
    }

    private void setupKeypad() {
        for (int i : DIALER_KEYS) {
            ((DialpadKeyButton) findViewById(i)).setOnPressedListener(this);
        }
        findViewById(R.id.zero).setOnLongClickListener(this);
    }

    @Override
    public boolean onKeyDown(int i, KeyEvent keyEvent) {
        if (i == 5) {
            if (TextUtils.isEmpty(this.mDigits.getText().toString())) {
                finish();
                return true;
            }
            placeCall();
            return true;
        }
        return super.onKeyDown(i, keyEvent);
    }

    private void keyPressed(int i) {
        this.mDigits.performHapticFeedback(1);
        this.mDigits.onKeyDown(i, new KeyEvent(0, i));
    }

    @Override
    public boolean onKey(View view, int i, KeyEvent keyEvent) {
        if (view.getId() == R.id.digits && i == 66 && keyEvent.getAction() == 1) {
            placeCall();
            return true;
        }
        return false;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent motionEvent) {
        this.mEmergencyActionGroup.onPreTouchEvent(motionEvent);
        boolean zDispatchTouchEvent = super.dispatchTouchEvent(motionEvent);
        this.mEmergencyActionGroup.onPostTouchEvent(motionEvent);
        return zDispatchTouchEvent;
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.deleteButton) {
            keyPressed(67);
            return;
        }
        if (id == R.id.digits) {
            if (this.mDigits.length() != 0) {
                this.mDigits.setCursorVisible(true);
            }
        } else if (id == R.id.floating_action_button) {
            view.performHapticFeedback(1);
            placeCall();
        }
    }

    @Override
    public void onPressed(View view, boolean z) {
        if (!z) {
        }
        switch (view.getId()) {
            case R.id.eight:
                playTone(8);
                keyPressed(15);
                break;
            case R.id.five:
                playTone(5);
                keyPressed(12);
                break;
            case R.id.four:
                playTone(4);
                keyPressed(11);
                break;
            case R.id.nine:
                playTone(9);
                keyPressed(16);
                break;
            case R.id.one:
                playTone(1);
                keyPressed(8);
                break;
            case R.id.pound:
                playTone(11);
                keyPressed(18);
                break;
            case R.id.seven:
                playTone(7);
                keyPressed(14);
                break;
            case R.id.six:
                playTone(6);
                keyPressed(13);
                break;
            case R.id.star:
                playTone(10);
                keyPressed(17);
                break;
            case R.id.three:
                playTone(3);
                keyPressed(10);
                break;
            case R.id.two:
                playTone(2);
                keyPressed(9);
                break;
            case R.id.zero:
                playTone(0);
                keyPressed(7);
                break;
        }
    }

    @Override
    public boolean onLongClick(View view) {
        int id = view.getId();
        if (id == R.id.deleteButton) {
            this.mDigits.getText().clear();
            return true;
        }
        if (id == R.id.zero) {
            removePreviousDigitIfPossible();
            keyPressed(81);
            return true;
        }
        return false;
    }

    @Override
    protected void onStart() {
        super.onStart();
        this.mColorExtractor.addOnColorsChangedListener(this);
        ColorExtractor.GradientColors colors = this.mColorExtractor.getColors(2, 2);
        this.mBackgroundGradient.setColors(colors, false);
        updateTheme(colors.supportsDarkText());
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.mDTMFToneEnabled = Settings.System.getInt(getContentResolver(), "dtmf_tone", 1) == 1;
        synchronized (this.mToneGeneratorLock) {
            if (this.mToneGenerator == null) {
                try {
                    this.mToneGenerator = new ToneGenerator(8, 80);
                } catch (RuntimeException e) {
                    Log.w(LOG_TAG, "Exception caught while creating local tone generator: " + e);
                    this.mToneGenerator = null;
                }
            }
        }
        updateDialAndDeleteButtonStateEnabledAttr();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        this.mColorExtractor.removeOnColorsChangedListener(this);
    }

    private void updateTheme(boolean z) {
        int i;
        if (this.mSupportsDarkText == z) {
            return;
        }
        this.mSupportsDarkText = z;
        if (this.mBackgroundGradient != null) {
            recreate();
            return;
        }
        int systemUiVisibility = getWindow().getDecorView().getSystemUiVisibility();
        if (z) {
            i = systemUiVisibility | 16 | 8192;
            setTheme(R.style.EmergencyDialerThemeDark);
        } else {
            i = systemUiVisibility & 16 & 8192;
            setTheme(R.style.EmergencyDialerTheme);
        }
        getWindow().getDecorView().setSystemUiVisibility(i);
    }

    private void placeCall() {
        this.mLastNumber = this.mDigits.getText().toString();
        this.mLastNumber = PhoneNumberUtils.convertToEmergencyNumber(this, this.mLastNumber);
        if (PhoneNumberUtils.isLocalEmergencyNumber(this, this.mLastNumber)) {
            if (this.mLastNumber == null || !TextUtils.isGraphic(this.mLastNumber)) {
                playTone(26);
                return;
            }
            ((TelecomManager) getSystemService("telecom")).placeCall(Uri.fromParts("tel", this.mLastNumber, null), null);
        } else {
            showDialog(0);
        }
        this.mDigits.getText().delete(0, this.mDigits.getText().length());
    }

    void playTone(int i) {
        int ringerMode;
        if (!this.mDTMFToneEnabled || (ringerMode = ((AudioManager) getSystemService("audio")).getRingerMode()) == 0 || ringerMode == 1) {
            return;
        }
        synchronized (this.mToneGeneratorLock) {
            if (this.mToneGenerator == null) {
                Log.w(LOG_TAG, "playTone: mToneGenerator == null, tone: " + i);
                return;
            }
            this.mToneGenerator.startTone(i, TONE_LENGTH_MS);
        }
    }

    private CharSequence createErrorMessage(String str) {
        if (!TextUtils.isEmpty(str)) {
            String string = getString(R.string.dial_emergency_error, new Object[]{str});
            int iIndexOf = string.indexOf(str);
            int length = str.length() + iIndexOf;
            SpannableString spannableString = new SpannableString(string);
            PhoneNumberUtils.addTtsSpan(spannableString, iIndexOf, length);
            return spannableString;
        }
        return getText(R.string.dial_emergency_empty_error).toString();
    }

    @Override
    protected Dialog onCreateDialog(int i) {
        if (i != 0) {
            return null;
        }
        AlertDialog alertDialogCreate = new AlertDialog.Builder(this, R.style.EmergencyDialerAlertDialogTheme).setTitle(getText(R.string.emergency_enable_radio_dialog_title)).setMessage(createErrorMessage(this.mLastNumber)).setPositiveButton(R.string.ok, (DialogInterface.OnClickListener) null).setCancelable(true).create();
        alertDialogCreate.getWindow().addFlags(4);
        setShowWhenLocked(true);
        return alertDialogCreate;
    }

    @Override
    protected void onPrepareDialog(int i, Dialog dialog) {
        super.onPrepareDialog(i, dialog);
        if (i == 0) {
            ((AlertDialog) dialog).setMessage(createErrorMessage(this.mLastNumber));
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == 16908332) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    private void updateDialAndDeleteButtonStateEnabledAttr() {
        this.mDelete.setEnabled(this.mDigits.length() != 0);
    }

    private void removePreviousDigitIfPossible() {
        int selectionStart = this.mDigits.getSelectionStart();
        if (selectionStart > 0) {
            this.mDigits.setSelection(selectionStart);
            this.mDigits.getText().delete(selectionStart - 1, selectionStart);
        }
    }

    private void updateTtsSpans() {
        for (TtsSpan ttsSpan : (TtsSpan[]) this.mDigits.getText().getSpans(0, this.mDigits.getText().length(), TtsSpan.class)) {
            this.mDigits.getText().removeSpan(ttsSpan);
        }
        PhoneNumberUtils.ttsSpanAsPhoneNumber(this.mDigits.getText(), 0, this.mDigits.getText().length());
    }

    public void onColorsChanged(ColorExtractor colorExtractor, int i) {
        if ((i & 2) != 0) {
            ColorExtractor.GradientColors colors = colorExtractor.getColors(2, 2);
            this.mBackgroundGradient.setColors(colors);
            updateTheme(colors.supportsDarkText());
        }
    }

    private void maybeShowWfcEmergencyCallingWarning() {
        if (!this.mIsWfcEmergencyCallingWarningEnabled) {
            Log.i(LOG_TAG, "maybeShowWfcEmergencyCallingWarning: warning disabled by carrier.");
        } else {
            new AsyncTask<Void, Void, Boolean>() {
                @Override
                protected Boolean doInBackground(Void... voidArr) {
                    TelephonyManager telephonyManager = (TelephonyManager) EmergencyDialer.this.getSystemService("phone");
                    boolean zIsWifiCallingAvailable = telephonyManager.isWifiCallingAvailable();
                    ServiceState serviceState = telephonyManager.getServiceState();
                    boolean z = false;
                    boolean z2 = serviceState.getRilVoiceRadioTechnology() != 0;
                    Log.i(EmergencyDialer.LOG_TAG, "showWfcWarningTask: isWfcAvailable=" + zIsWifiCallingAvailable + " isCellAvailable=" + z2 + "(rat=" + serviceState.getRilVoiceRadioTechnology() + ")");
                    if (zIsWifiCallingAvailable && !z2) {
                        z = true;
                    }
                    return Boolean.valueOf(z);
                }

                @Override
                protected void onPostExecute(Boolean bool) {
                    if (bool.booleanValue()) {
                        Log.i(EmergencyDialer.LOG_TAG, "showWfcWarningTask: showing ecall warning");
                        EmergencyDialer.this.mDigits.setHint(R.string.dial_emergency_calling_not_available);
                    } else {
                        Log.i(EmergencyDialer.LOG_TAG, "showWfcWarningTask: hiding ecall warning");
                        EmergencyDialer.this.mDigits.setHint("");
                    }
                    EmergencyDialer.this.maybeChangeHintSize();
                }
            }.execute((Void) null);
        }
    }

    private void maybeChangeHintSize() {
        if (TextUtils.isEmpty(this.mDigits.getHint()) || !TextUtils.isEmpty(this.mDigits.getText().toString())) {
            this.mDigits.setTextSize(2, this.mDefaultDigitsTextSize);
            this.mDigits.setResizeEnabled(true);
            Log.i(LOG_TAG, "no hint - setting to " + this.mDigits.getScaledTextSize());
            return;
        }
        this.mDigits.setTextSize(0, getResources().getDimensionPixelSize(R.dimen.emergency_call_warning_size));
        this.mDigits.setResizeEnabled(false);
        Log.i(LOG_TAG, "hint - setting to " + this.mDigits.getScaledTextSize());
    }
}
