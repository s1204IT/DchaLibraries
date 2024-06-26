package jp.co.benesse.dcha.systemsettings;

import android.os.Bundle;
import android.os.IPowerManager;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import jp.co.benesse.dcha.util.Logger;
/* loaded from: s.zip:jp/co/benesse/dcha/systemsettings/LightSettingActivity.class */
public class LightSettingActivity extends ParentSettingActivity implements View.OnClickListener, SeekBar.OnSeekBarChangeListener {
    private ImageView mBackBtn;
    private ImageView mBrightBtn;
    private ImageView mDarkBtn;
    private ImageView mDoneBtn;
    private boolean mIsDecision;
    private int mOldBrightness;
    private int mScreenBrightnessMaximum;
    private int mScreenBrightnessMinimum;
    private SeekBar mSeekBar;
    private int mResumeBrightness = -1;
    private int mCurBrightness = -1;

    private int getBrightness() {
        float f;
        int i = 0;
        Logger.d("LightSettingActivity", "getBrightness 0001");
        Logger.i("LightSettingActivity", "getBrightness mCurBrightness = " + this.mCurBrightness);
        if (this.mCurBrightness < 0) {
            Logger.d("LightSettingActivity", "getBrightness 0002");
            Logger.i("LightSettingActivity", "getBrightness : currentBrightness < 0");
            f = Settings.System.getInt(getContentResolver(), "screen_brightness", 100);
        } else {
            Logger.d("LightSettingActivity", "getBrightness 0003");
            Logger.i("LightSettingActivity", "getBrightness : currentBrightness > 0");
            f = this.mCurBrightness;
        }
        float f2 = (f - this.mScreenBrightnessMinimum) / (this.mScreenBrightnessMaximum - this.mScreenBrightnessMinimum);
        Logger.d("LightSettingActivity", "getBrightness 0004");
        if (f2 > 0.0f) {
            i = Math.round(4.0f * f2);
        }
        return i;
    }

    private void restoreOldState() {
        Logger.d("LightSettingActivity", "restoreOldState 0001");
        setBrightness(this.mOldBrightness, false);
        this.mCurBrightness = -1;
        Logger.d("LightSettingActivity", "restoreOldState 0002");
    }

    private void setBrightness(int i, boolean z) {
        Logger.d("LightSettingActivity", "setBrightness 0001");
        int i2 = ((i * (this.mScreenBrightnessMaximum - this.mScreenBrightnessMinimum)) / 4) + this.mScreenBrightnessMinimum;
        try {
            Logger.d("LightSettingActivity", "setBrightness 0002");
            IPowerManager asInterface = IPowerManager.Stub.asInterface(ServiceManager.getService("power"));
            if (asInterface != null) {
                Logger.d("LightSettingActivity", "setBrightness 0003");
                asInterface.setTemporaryScreenBrightnessSettingOverride(i2);
            }
            if (z) {
                Logger.d("LightSettingActivity", "setBrightness 0004");
                this.mCurBrightness = -1;
                Settings.System.putInt(getContentResolver(), "screen_brightness", i2);
            } else {
                Logger.d("LightSettingActivity", "setBrightness 0005");
                this.mCurBrightness = i2;
            }
            Logger.d("LightSettingActivity", "setBrightness 0006");
        } catch (RemoteException e) {
            Logger.d("LightSettingActivity", "setBrightness 0007");
            Logger.e("LightSettingActivity", "setBrightness", e);
        }
        Logger.d("LightSettingActivity", "setBrightness 0008");
    }

    @Override // android.view.View.OnClickListener
    public void onClick(View view) {
        Logger.d("LightSettingActivity", "onClick 0001");
        if (view.getId() == this.mDoneBtn.getId()) {
            Logger.d("LightSettingActivity", "onClick 0002");
            this.mDoneBtn.setClickable(false);
            this.mBackBtn.setClickable(false);
            this.mIsDecision = true;
            setBrightness(this.mSeekBar.getProgress(), true);
            moveSettingActivity();
            finish();
        } else if (view.getId() == this.mBackBtn.getId()) {
            Logger.d("LightSettingActivity", "onClick 0003");
            this.mDoneBtn.setClickable(false);
            this.mBackBtn.setClickable(false);
            restoreOldState();
            moveSettingActivity();
            finish();
        } else if (view.getId() == this.mDarkBtn.getId()) {
            Logger.d("LightSettingActivity", "onClick 0004");
            this.mSeekBar.setProgress(getBrightness() - 1);
        } else if (view.getId() == this.mBrightBtn.getId()) {
            Logger.d("LightSettingActivity", "onClick 0005");
            this.mSeekBar.setProgress(getBrightness() + 1);
        }
        Logger.d("LightSettingActivity", "onClick 0006");
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // jp.co.benesse.dcha.systemsettings.ParentSettingActivity, android.app.Activity
    public void onCreate(Bundle bundle) {
        Logger.d("LightSettingActivity", "onCreate 0001");
        super.onCreate(bundle);
        setContentView(2130903042);
        PowerManager powerManager = (PowerManager) getSystemService("power");
        this.mScreenBrightnessMinimum = powerManager.getMinimumScreenBrightnessSetting() + 50;
        this.mScreenBrightnessMaximum = powerManager.getMaximumScreenBrightnessSetting();
        this.mDoneBtn = (ImageView) findViewById(2131361816);
        this.mBackBtn = (ImageView) findViewById(2131361814);
        this.mDarkBtn = (ImageView) findViewById(2131361818);
        this.mBrightBtn = (ImageView) findViewById(2131361819);
        this.mSeekBar = (SeekBar) findViewById(2131361817);
        this.mDoneBtn.setOnClickListener(this);
        this.mBackBtn.setOnClickListener(this);
        this.mDarkBtn.setOnClickListener(this);
        this.mBrightBtn.setOnClickListener(this);
        this.mSeekBar.setMax(4);
        this.mOldBrightness = getBrightness();
        this.mSeekBar.setProgress(this.mOldBrightness);
        this.mSeekBar.setOnSeekBarChangeListener(this);
        this.mSeekBar.setEnabled(false);
        this.mIsDecision = false;
        Logger.d("LightSettingActivity", "onCreate 0002");
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // jp.co.benesse.dcha.systemsettings.ParentSettingActivity, android.app.Activity
    public void onDestroy() {
        Logger.d("LightSettingActivity", "onDestroy 0001");
        super.onDestroy();
        this.mDoneBtn.setOnClickListener(null);
        this.mBackBtn.setOnClickListener(null);
        this.mDarkBtn.setOnClickListener(null);
        this.mBrightBtn.setOnClickListener(null);
        this.mSeekBar.setOnSeekBarChangeListener(null);
        this.mDoneBtn = null;
        this.mBackBtn = null;
        this.mDarkBtn = null;
        this.mBrightBtn = null;
        this.mSeekBar = null;
        Logger.d("LightSettingActivity", "onDestroy 0002");
    }

    @Override // android.app.Activity
    protected void onPause() {
        Logger.d("LightSettingActivity", "onPause 0001");
        super.onPause();
        this.mResumeBrightness = getBrightness();
        if (!this.mIsDecision) {
            Logger.d("LightSettingActivity", "onPause 0002");
            restoreOldState();
        }
        Logger.d("LightSettingActivity", "onPause 0003");
    }

    @Override // android.widget.SeekBar.OnSeekBarChangeListener
    public void onProgressChanged(SeekBar seekBar, int i, boolean z) {
        Logger.d("LightSettingActivity", "onProgressChanged 0001");
        setBrightness(i, false);
        Logger.d("LightSettingActivity", "onProgressChanged 0002");
    }

    @Override // android.app.Activity
    protected void onResume() {
        super.onResume();
        Logger.d("LightSettingActivity", "onResume 0001");
        if (this.mResumeBrightness != -1) {
            setBrightness(this.mResumeBrightness, false);
        }
        Logger.d("LightSettingActivity", "onResume 0002");
    }

    @Override // android.widget.SeekBar.OnSeekBarChangeListener
    public void onStartTrackingTouch(SeekBar seekBar) {
        Logger.d("LightSettingActivity", "onStartTrackingTouch 0001");
    }

    @Override // android.widget.SeekBar.OnSeekBarChangeListener
    public void onStopTrackingTouch(SeekBar seekBar) {
        Logger.d("LightSettingActivity", "onStopTrackingTouch 0001");
    }
}
