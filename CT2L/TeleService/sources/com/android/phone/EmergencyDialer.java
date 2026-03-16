package com.android.phone;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.StatusBarManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.PhoneNumberUtils;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.DialerKeyListener;
import android.text.style.TtsSpan;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import com.android.phone.common.HapticFeedback;
import com.android.phone.common.dialpad.DialpadKeyButton;
import com.android.phone.common.util.ViewUtil;

public class EmergencyDialer extends Activity implements TextWatcher, View.OnClickListener, View.OnKeyListener, View.OnLongClickListener, DialpadKeyButton.OnPressedListener {
    private static final int[] DIALER_KEYS = {R.id.one, R.id.two, R.id.three, R.id.four, R.id.five, R.id.six, R.id.seven, R.id.eight, R.id.nine, R.id.star, R.id.zero, R.id.pound};
    private boolean mDTMFToneEnabled;
    private View mDelete;
    private View mDialButton;
    EditText mDigits;
    private String mLastNumber;
    private StatusBarManager mStatusBarManager;
    private ToneGenerator mToneGenerator;
    private Object mToneGeneratorLock = new Object();
    private HapticFeedback mHaptic = new HapticFeedback();
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.SCREEN_OFF".equals(intent.getAction())) {
                EmergencyDialer.this.finish();
            }
        }
    };

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence input, int start, int before, int changeCount) {
    }

    @Override
    public void afterTextChanged(Editable input) {
        if (SpecialCharSequenceMgr.handleCharsForLockedDevice(this, input.toString(), this)) {
            this.mDigits.getText().clear();
        }
        updateDialAndDeleteButtonStateEnabledAttr();
        updateTtsSpans();
    }

    @Override
    protected void onCreate(Bundle icicle) {
        String number;
        super.onCreate(icicle);
        this.mStatusBarManager = (StatusBarManager) getSystemService("statusbar");
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.flags |= 524288;
        getWindow().setAttributes(lp);
        setContentView(R.layout.emergency_dialer);
        this.mDigits = (EditText) findViewById(R.id.digits);
        this.mDigits.setKeyListener(DialerKeyListener.getInstance());
        this.mDigits.setOnClickListener(this);
        this.mDigits.setOnKeyListener(this);
        this.mDigits.setLongClickable(false);
        maybeAddNumberFormatting();
        View view = findViewById(R.id.one);
        if (view != null) {
            setupKeypad();
        }
        this.mDelete = findViewById(R.id.deleteButton);
        this.mDelete.setOnClickListener(this);
        this.mDelete.setOnLongClickListener(this);
        this.mDialButton = findViewById(R.id.floating_action_button);
        Resources res = getResources();
        if (res.getBoolean(R.bool.config_show_onscreen_dial_button)) {
            this.mDialButton.setOnClickListener(this);
        } else {
            this.mDialButton.setVisibility(8);
        }
        View floatingActionButtonContainer = findViewById(R.id.floating_action_button_container);
        ViewUtil.setupFloatingActionButton(floatingActionButtonContainer, getResources());
        if (icicle != null) {
            super.onRestoreInstanceState(icicle);
        }
        Uri data = getIntent().getData();
        if (data != null && "tel".equals(data.getScheme()) && (number = PhoneNumberUtils.getNumberFromIntent(getIntent(), this)) != null) {
            this.mDigits.setText(number);
        }
        synchronized (this.mToneGeneratorLock) {
            if (this.mToneGenerator == null) {
                try {
                    this.mToneGenerator = new ToneGenerator(8, 80);
                } catch (RuntimeException e) {
                    Log.w("EmergencyDialer", "Exception caught while creating local tone generator: " + e);
                    this.mToneGenerator = null;
                }
            }
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.SCREEN_OFF");
        registerReceiver(this.mBroadcastReceiver, intentFilter);
        try {
            this.mHaptic.init(this, res.getBoolean(R.bool.config_enable_dialer_key_vibration));
        } catch (Resources.NotFoundException nfe) {
            Log.e("EmergencyDialer", "Vibrate control bool missing.", nfe);
        }
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
    protected void onRestoreInstanceState(Bundle icicle) {
        this.mLastNumber = icicle.getString("lastNumber");
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("lastNumber", this.mLastNumber);
    }

    protected void maybeAddNumberFormatting() {
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        this.mDigits.addTextChangedListener(this);
    }

    private void setupKeypad() {
        int[] arr$ = DIALER_KEYS;
        for (int id : arr$) {
            DialpadKeyButton key = (DialpadKeyButton) findViewById(id);
            key.setOnPressedListener(this);
        }
        View view = findViewById(R.id.zero);
        view.setOnLongClickListener(this);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case 5:
                if (TextUtils.isEmpty(this.mDigits.getText().toString())) {
                    finish();
                } else {
                    placeCall();
                }
                return true;
            default:
                return super.onKeyDown(keyCode, event);
        }
    }

    private void keyPressed(int keyCode) {
        this.mHaptic.vibrate();
        KeyEvent event = new KeyEvent(0, keyCode);
        this.mDigits.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKey(View view, int keyCode, KeyEvent event) {
        switch (view.getId()) {
            case R.id.digits:
                if (keyCode == 66 && event.getAction() == 1) {
                    placeCall();
                    return true;
                }
            default:
                return false;
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.digits:
                if (this.mDigits.length() != 0) {
                    this.mDigits.setCursorVisible(true);
                }
                break;
            case R.id.deleteButton:
                keyPressed(67);
                break;
            case R.id.floating_action_button:
                this.mHaptic.vibrate();
                placeCall();
                break;
        }
    }

    @Override
    public void onPressed(View view, boolean pressed) {
        if (pressed) {
            switch (view.getId()) {
                case R.id.two:
                    playTone(2);
                    keyPressed(9);
                    break;
                case R.id.three:
                    playTone(3);
                    keyPressed(10);
                    break;
                case R.id.four:
                    playTone(4);
                    keyPressed(11);
                    break;
                case R.id.five:
                    playTone(5);
                    keyPressed(12);
                    break;
                case R.id.six:
                    playTone(6);
                    keyPressed(13);
                    break;
                case R.id.seven:
                    playTone(7);
                    keyPressed(14);
                    break;
                case R.id.eight:
                    playTone(8);
                    keyPressed(15);
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
                case R.id.star:
                    playTone(10);
                    keyPressed(17);
                    break;
                case R.id.zero:
                    playTone(0);
                    keyPressed(7);
                    break;
            }
        }
    }

    @Override
    public boolean onLongClick(View view) {
        int id = view.getId();
        switch (id) {
            case R.id.zero:
                removePreviousDigitIfPossible();
                keyPressed(81);
                break;
            case R.id.deleteButton:
                this.mDigits.getText().clear();
                break;
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.mDTMFToneEnabled = Settings.System.getInt(getContentResolver(), "dtmf_tone", 1) == 1;
        this.mHaptic.checkSystemSetting();
        synchronized (this.mToneGeneratorLock) {
            if (this.mToneGenerator == null) {
                try {
                    this.mToneGenerator = new ToneGenerator(8, 80);
                } catch (RuntimeException e) {
                    Log.w("EmergencyDialer", "Exception caught while creating local tone generator: " + e);
                    this.mToneGenerator = null;
                }
            }
        }
        this.mStatusBarManager.disable(65536);
        updateDialAndDeleteButtonStateEnabledAttr();
    }

    @Override
    public void onPause() {
        this.mStatusBarManager.disable(0);
        super.onPause();
        synchronized (this.mToneGeneratorLock) {
            if (this.mToneGenerator != null) {
                this.mToneGenerator.release();
                this.mToneGenerator = null;
            }
        }
    }

    private void placeCall() {
        this.mLastNumber = this.mDigits.getText().toString();
        if (PhoneNumberUtils.isLocalEmergencyNumber(this, this.mLastNumber)) {
            if (this.mLastNumber == null || !TextUtils.isGraphic(this.mLastNumber)) {
                playTone(26);
                return;
            }
            Intent intent = new Intent("android.intent.action.CALL_EMERGENCY");
            intent.setData(Uri.fromParts("tel", this.mLastNumber, null));
            intent.setFlags(268435456);
            startActivity(intent);
        } else {
            showDialog(0);
        }
        this.mDigits.getText().delete(0, this.mDigits.getText().length());
    }

    void playTone(int tone) {
        if (this.mDTMFToneEnabled) {
            AudioManager audioManager = (AudioManager) getSystemService("audio");
            int ringerMode = audioManager.getRingerMode();
            if (ringerMode != 0 && ringerMode != 1) {
                synchronized (this.mToneGeneratorLock) {
                    if (this.mToneGenerator == null) {
                        Log.w("EmergencyDialer", "playTone: mToneGenerator == null, tone: " + tone);
                    } else {
                        this.mToneGenerator.startTone(tone, 150);
                    }
                }
            }
        }
    }

    private CharSequence createErrorMessage(String number) {
        return !TextUtils.isEmpty(number) ? getString(R.string.dial_emergency_error, new Object[]{this.mLastNumber}) : getText(R.string.dial_emergency_empty_error).toString();
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        if (id != 0) {
            return null;
        }
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle(getText(R.string.emergency_enable_radio_dialog_title)).setMessage(createErrorMessage(this.mLastNumber)).setPositiveButton(R.string.ok, (DialogInterface.OnClickListener) null).setCancelable(true).create();
        dialog.getWindow().addFlags(4);
        dialog.getWindow().addFlags(524288);
        return dialog;
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        super.onPrepareDialog(id, dialog);
        if (id == 0) {
            AlertDialog alert = (AlertDialog) dialog;
            alert.setMessage(createErrorMessage(this.mLastNumber));
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId != 16908332) {
            return super.onOptionsItemSelected(item);
        }
        onBackPressed();
        return true;
    }

    private void updateDialAndDeleteButtonStateEnabledAttr() {
        boolean notEmpty = this.mDigits.length() != 0;
        this.mDelete.setEnabled(notEmpty);
    }

    private void removePreviousDigitIfPossible() {
        int currentPosition = this.mDigits.getSelectionStart();
        if (currentPosition > 0) {
            this.mDigits.setSelection(currentPosition);
            this.mDigits.getText().delete(currentPosition - 1, currentPosition);
        }
    }

    private void updateTtsSpans() {
        TtsSpan[] arr$ = (TtsSpan[]) this.mDigits.getText().getSpans(0, this.mDigits.getText().length(), TtsSpan.class);
        for (TtsSpan o : arr$) {
            this.mDigits.getText().removeSpan(o);
        }
        PhoneNumberUtils.ttsSpanAsPhoneNumber(this.mDigits.getText(), 0, this.mDigits.getText().length());
    }
}
