package jp.co.benesse.dcha.systemsettings;

import android.content.ContentResolver;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Logger.d("LightSettingActivity", "onCreate 0001");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_light);
        PowerManager pm = (PowerManager) getSystemService("power");
        this.mScreenBrightnessMinimum = pm.getMinimumScreenBrightnessSetting() + 50;
        this.mScreenBrightnessMaximum = pm.getMaximumScreenBrightnessSetting();
        this.mDoneBtn = (ImageView) findViewById(R.id.done_btn);
        this.mBackBtn = (ImageView) findViewById(R.id.back_btn);
        this.mDarkBtn = (ImageView) findViewById(R.id.dark_btn);
        this.mBrightBtn = (ImageView) findViewById(R.id.bright_btn);
        this.mSeekBar = (SeekBar) findViewById(R.id.seekbar);
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

    @Override
    protected void onResume() {
        super.onResume();
        Logger.d("LightSettingActivity", "onResume 0001");
        if (this.mResumeBrightness != -1) {
            setBrightness(this.mResumeBrightness, false);
        }
        Logger.d("LightSettingActivity", "onResume 0002");
    }

    @Override
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

    @Override
    protected void onDestroy() {
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

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {
        Logger.d("LightSettingActivity", "onProgressChanged 0001");
        setBrightness(progress, false);
        Logger.d("LightSettingActivity", "onProgressChanged 0002");
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        Logger.d("LightSettingActivity", "onStartTrackingTouch 0001");
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        Logger.d("LightSettingActivity", "onStopTrackingTouch 0001");
    }

    private int getBrightness() {
        float brightness;
        Logger.d("LightSettingActivity", "getBrightness 0001");
        Logger.i("LightSettingActivity", "getBrightness mCurBrightness = " + this.mCurBrightness);
        if (this.mCurBrightness < 0) {
            Logger.d("LightSettingActivity", "getBrightness 0002");
            Logger.i("LightSettingActivity", "getBrightness : currentBrightness < 0");
            brightness = Settings.System.getInt(getContentResolver(), "screen_brightness", 100);
        } else {
            Logger.d("LightSettingActivity", "getBrightness 0003");
            Logger.i("LightSettingActivity", "getBrightness : currentBrightness > 0");
            brightness = this.mCurBrightness;
        }
        float brightness2 = (brightness - this.mScreenBrightnessMinimum) / (this.mScreenBrightnessMaximum - this.mScreenBrightnessMinimum);
        Logger.d("LightSettingActivity", "getBrightness 0004");
        if (brightness2 > 0.0f) {
            return Math.round(4.0f * brightness2);
        }
        return 0;
    }

    private void restoreOldState() {
        Logger.d("LightSettingActivity", "restoreOldState 0001");
        setBrightness(this.mOldBrightness, false);
        this.mCurBrightness = -1;
        Logger.d("LightSettingActivity", "restoreOldState 0002");
    }

    private void setBrightness(int brightness, boolean write) {
        Logger.d("LightSettingActivity", "setBrightness 0001");
        int range = this.mScreenBrightnessMaximum - this.mScreenBrightnessMinimum;
        int brightness2 = ((brightness * range) / 4) + this.mScreenBrightnessMinimum;
        try {
            Logger.d("LightSettingActivity", "setBrightness 0002");
            IPowerManager power = IPowerManager.Stub.asInterface(ServiceManager.getService("power"));
            if (power != null) {
                Logger.d("LightSettingActivity", "setBrightness 0003");
                power.setTemporaryScreenBrightnessSettingOverride(brightness2);
            }
            if (write) {
                Logger.d("LightSettingActivity", "setBrightness 0004");
                this.mCurBrightness = -1;
                ContentResolver resolver = getContentResolver();
                Settings.System.putInt(resolver, "screen_brightness", brightness2);
            } else {
                Logger.d("LightSettingActivity", "setBrightness 0005");
                this.mCurBrightness = brightness2;
            }
            Logger.d("LightSettingActivity", "setBrightness 0006");
        } catch (RemoteException e) {
            Logger.d("LightSettingActivity", "setBrightness 0007");
            Logger.e("LightSettingActivity", "setBrightness", e);
        }
        Logger.d("LightSettingActivity", "setBrightness 0008");
    }

    @Override
    public void onClick(View v) {
        Logger.d("LightSettingActivity", "onClick 0001");
        if (v.getId() == this.mDoneBtn.getId()) {
            Logger.d("LightSettingActivity", "onClick 0002");
            this.mDoneBtn.setClickable(false);
            this.mBackBtn.setClickable(false);
            this.mIsDecision = true;
            setBrightness(this.mSeekBar.getProgress(), true);
            moveSettingActivity();
            finish();
        } else if (v.getId() == this.mBackBtn.getId()) {
            Logger.d("LightSettingActivity", "onClick 0003");
            this.mDoneBtn.setClickable(false);
            this.mBackBtn.setClickable(false);
            restoreOldState();
            moveSettingActivity();
            finish();
        } else if (v.getId() == this.mDarkBtn.getId()) {
            Logger.d("LightSettingActivity", "onClick 0004");
            this.mSeekBar.setProgress(getBrightness() - 1);
        } else if (v.getId() == this.mBrightBtn.getId()) {
            Logger.d("LightSettingActivity", "onClick 0005");
            this.mSeekBar.setProgress(getBrightness() + 1);
        }
        Logger.d("LightSettingActivity", "onClick 0006");
    }
}
