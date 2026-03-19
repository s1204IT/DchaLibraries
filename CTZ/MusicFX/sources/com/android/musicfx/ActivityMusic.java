package com.android.musicfx;

import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.AudioSystem;
import android.media.audiofx.AudioEffect;
import android.media.audiofx.Virtualizer;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import com.android.musicfx.ControlPanelEffect;
import java.util.Formatter;
import java.util.Locale;

public class ActivityMusic extends Activity implements SeekBar.OnSeekBarChangeListener {
    private static final int[][] EQViewElementIds = {new int[]{R.id.EQBand0TextView, R.id.EQBand0SeekBar}, new int[]{R.id.EQBand1TextView, R.id.EQBand1SeekBar}, new int[]{R.id.EQBand2TextView, R.id.EQBand2SeekBar}, new int[]{R.id.EQBand3TextView, R.id.EQBand3SeekBar}, new int[]{R.id.EQBand4TextView, R.id.EQBand4SeekBar}, new int[]{R.id.EQBand5TextView, R.id.EQBand5SeekBar}, new int[]{R.id.EQBand6TextView, R.id.EQBand6SeekBar}, new int[]{R.id.EQBand7TextView, R.id.EQBand7SeekBar}, new int[]{R.id.EQBand8TextView, R.id.EQBand8SeekBar}, new int[]{R.id.EQBand9TextView, R.id.EQBand9SeekBar}, new int[]{R.id.EQBand10TextView, R.id.EQBand10SeekBar}, new int[]{R.id.EQBand11TextView, R.id.EQBand11SeekBar}, new int[]{R.id.EQBand12TextView, R.id.EQBand12SeekBar}, new int[]{R.id.EQBand13TextView, R.id.EQBand13SeekBar}, new int[]{R.id.EQBand14TextView, R.id.EQBand14SeekBar}, new int[]{R.id.EQBand15TextView, R.id.EQBand15SeekBar}, new int[]{R.id.EQBand16TextView, R.id.EQBand16SeekBar}, new int[]{R.id.EQBand17TextView, R.id.EQBand17SeekBar}, new int[]{R.id.EQBand18TextView, R.id.EQBand18SeekBar}, new int[]{R.id.EQBand19TextView, R.id.EQBand19SeekBar}, new int[]{R.id.EQBand20TextView, R.id.EQBand20SeekBar}, new int[]{R.id.EQBand21TextView, R.id.EQBand21SeekBar}, new int[]{R.id.EQBand22TextView, R.id.EQBand22SeekBar}, new int[]{R.id.EQBand23TextView, R.id.EQBand23SeekBar}, new int[]{R.id.EQBand24TextView, R.id.EQBand24SeekBar}, new int[]{R.id.EQBand25TextView, R.id.EQBand25SeekBar}, new int[]{R.id.EQBand26TextView, R.id.EQBand26SeekBar}, new int[]{R.id.EQBand27TextView, R.id.EQBand27SeekBar}, new int[]{R.id.EQBand28TextView, R.id.EQBand28SeekBar}, new int[]{R.id.EQBand29TextView, R.id.EQBand29SeekBar}, new int[]{R.id.EQBand30TextView, R.id.EQBand30SeekBar}, new int[]{R.id.EQBand31TextView, R.id.EQBand31SeekBar}};
    private static final String[] PRESETREVERBPRESETSTRINGS = {"None", "SmallRoom", "MediumRoom", "LargeRoom", "MediumHall", "LargeHall", "Plate"};
    private boolean mBassBoostSupported;
    private Context mContext;
    private int mEQPreset;
    private String[] mEQPresetNames;
    private int mEQPresetPrevious;
    private int[] mEQPresetUserBandLevelsPrev;
    private int mEqualizerMinBandLevel;
    private boolean mEqualizerSupported;
    private int mNumberEqualizerBands;
    private int mPRPreset;
    private int mPRPresetPrevious;
    private boolean mPresetReverbSupported;
    private CompoundButton mToggleSwitch;
    private boolean mVirtualizerIsHeadphoneOnly;
    private boolean mVirtualizerSupported;
    private final SeekBar[] mEqualizerSeekBar = new SeekBar[32];
    private int mEQPresetUserPos = 1;
    private boolean mIsHeadsetOn = false;
    private StringBuilder mFormatBuilder = new StringBuilder();
    private Formatter mFormatter = new Formatter(this.mFormatBuilder, Locale.getDefault());
    private String mCallingPackageName = "empty";
    private int mAudioSession = -4;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int deviceClass;
            String action = intent.getAction();
            boolean z = ActivityMusic.this.mIsHeadsetOn;
            AudioManager audioManager = (AudioManager) ActivityMusic.this.getSystemService("audio");
            if (action.equals("android.intent.action.HEADSET_PLUG")) {
                ActivityMusic.this.mIsHeadsetOn = intent.getIntExtra("state", 0) == 1 || audioManager.isBluetoothA2dpOn();
            } else if (action.equals("android.bluetooth.device.action.ACL_CONNECTED")) {
                int deviceClass2 = ((BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE")).getBluetoothClass().getDeviceClass();
                if (deviceClass2 == 1048 || deviceClass2 == 1028) {
                    ActivityMusic.this.mIsHeadsetOn = true;
                }
            } else if (action.equals("android.media.AUDIO_BECOMING_NOISY")) {
                ActivityMusic.this.mIsHeadsetOn = audioManager.isBluetoothA2dpOn() || audioManager.isWiredHeadsetOn();
            } else if (action.equals("android.bluetooth.device.action.ACL_DISCONNECTED") && ((deviceClass = ((BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE")).getBluetoothClass().getDeviceClass()) == 1048 || deviceClass == 1028)) {
                ActivityMusic.this.mIsHeadsetOn = audioManager.isWiredHeadsetOn();
            }
            if (z != ActivityMusic.this.mIsHeadsetOn) {
                ActivityMusic.this.updateUIHeadset();
            }
        }
    };

    @Override
    public void onCreate(Bundle bundle) throws Throwable {
        super.onCreate(bundle);
        this.mContext = this;
        this.mAudioSession = getIntent().getIntExtra("android.media.extra.AUDIO_SESSION", -4);
        Log.v("MusicFXActivityMusic", "audio session: " + this.mAudioSession);
        this.mCallingPackageName = getCallingPackage();
        if (this.mCallingPackageName == null) {
            Log.e("MusicFXActivityMusic", "Package name is null");
            setResult(0);
            finish();
            return;
        }
        setResult(-1);
        Log.v("MusicFXActivityMusic", this.mCallingPackageName + " (" + this.mAudioSession + ")");
        ControlPanelEffect.initEffectsPreferences(this.mContext, this.mCallingPackageName, this.mAudioSession);
        AudioEffect.Descriptor[] descriptorArrQueryEffects = AudioEffect.queryEffects();
        Log.v("MusicFXActivityMusic", "Available effects:");
        int length = descriptorArrQueryEffects.length;
        for (int i = 0; i < length; i++) {
            AudioEffect.Descriptor descriptor = descriptorArrQueryEffects[i];
            Log.v("MusicFXActivityMusic", descriptor.name.toString() + ", type: " + descriptor.type.toString());
            if (descriptor.type.equals(AudioEffect.EFFECT_TYPE_VIRTUALIZER)) {
                this.mVirtualizerSupported = true;
                this.mVirtualizerIsHeadphoneOnly = true ^ isVirtualizerTransauralSupported();
            } else if (descriptor.type.equals(AudioEffect.EFFECT_TYPE_BASS_BOOST)) {
                this.mBassBoostSupported = true;
            } else if (descriptor.type.equals(AudioEffect.EFFECT_TYPE_EQUALIZER)) {
                this.mEqualizerSupported = true;
            } else if (descriptor.type.equals(AudioEffect.EFFECT_TYPE_PRESET_REVERB)) {
                this.mPresetReverbSupported = true;
            }
        }
        setContentView(R.layout.music_main);
        final ViewGroup viewGroup = (ViewGroup) findViewById(R.id.contentSoundEffects);
        findViewById(R.id.bBStrengthText).setLabelFor(R.id.bBStrengthSeekBar);
        findViewById(R.id.vIStrengthText).setLabelFor(R.id.vIStrengthSeekBar);
        int parameterInt = ControlPanelEffect.getParameterInt(this.mContext, this.mCallingPackageName, this.mAudioSession, ControlPanelEffect.Key.eq_num_presets);
        this.mEQPresetNames = new String[parameterInt + 2];
        for (short s = 0; s < parameterInt; s = (short) (s + 1)) {
            this.mEQPresetNames[s] = ControlPanelEffect.getParameterString(this.mContext, this.mCallingPackageName, this.mAudioSession, ControlPanelEffect.Key.eq_preset_name, s);
        }
        this.mEQPresetNames[parameterInt] = getString(R.string.ci_extreme);
        int i2 = parameterInt + 1;
        this.mEQPresetNames[i2] = getString(R.string.user);
        this.mEQPresetUserPos = i2;
        if (this.mVirtualizerSupported || this.mBassBoostSupported || this.mEqualizerSupported || this.mPresetReverbSupported) {
            this.mToggleSwitch = new Switch(this);
            this.mToggleSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean z) {
                    ControlPanelEffect.setParameterBoolean(ActivityMusic.this.mContext, ActivityMusic.this.mCallingPackageName, ActivityMusic.this.mAudioSession, ControlPanelEffect.Key.global_enabled, z);
                    ActivityMusic.this.setEnabledAllChildren(viewGroup, z);
                    ActivityMusic.this.updateUIHeadset();
                }
            });
            if (this.mVirtualizerSupported) {
                findViewById(R.id.vILayout).setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View view, MotionEvent motionEvent) {
                        if (motionEvent.getAction() == 1) {
                            ActivityMusic.this.showHeadsetMsg();
                            return false;
                        }
                        return false;
                    }
                });
                SeekBar seekBar = (SeekBar) findViewById(R.id.vIStrengthSeekBar);
                seekBar.setMax(1000);
                seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar2, int i3, boolean z) {
                        ControlPanelEffect.setParameterInt(ActivityMusic.this.mContext, ActivityMusic.this.mCallingPackageName, ActivityMusic.this.mAudioSession, ControlPanelEffect.Key.virt_strength, i3);
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar2) {
                        if (seekBar2.getProgress() == 0) {
                            ControlPanelEffect.setParameterBoolean(ActivityMusic.this.mContext, ActivityMusic.this.mCallingPackageName, ActivityMusic.this.mAudioSession, ControlPanelEffect.Key.virt_enabled, true);
                        }
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar2) {
                        if (seekBar2.getProgress() == 0) {
                            ControlPanelEffect.setParameterBoolean(ActivityMusic.this.mContext, ActivityMusic.this.mCallingPackageName, ActivityMusic.this.mAudioSession, ControlPanelEffect.Key.virt_enabled, false);
                        }
                    }
                });
                ((Switch) findViewById(R.id.vIStrengthToggle)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean z) {
                        ControlPanelEffect.setParameterBoolean(ActivityMusic.this.mContext, ActivityMusic.this.mCallingPackageName, ActivityMusic.this.mAudioSession, ControlPanelEffect.Key.virt_enabled, z);
                    }
                });
            }
            if (this.mBassBoostSupported) {
                findViewById(R.id.bBLayout).setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View view, MotionEvent motionEvent) {
                        if (motionEvent.getAction() == 1) {
                            ActivityMusic.this.showHeadsetMsg();
                            return false;
                        }
                        return false;
                    }
                });
                SeekBar seekBar2 = (SeekBar) findViewById(R.id.bBStrengthSeekBar);
                seekBar2.setMax(1000);
                seekBar2.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar3, int i3, boolean z) {
                        ControlPanelEffect.setParameterInt(ActivityMusic.this.mContext, ActivityMusic.this.mCallingPackageName, ActivityMusic.this.mAudioSession, ControlPanelEffect.Key.bb_strength, i3);
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar3) {
                        if (seekBar3.getProgress() == 0) {
                            ControlPanelEffect.setParameterBoolean(ActivityMusic.this.mContext, ActivityMusic.this.mCallingPackageName, ActivityMusic.this.mAudioSession, ControlPanelEffect.Key.bb_enabled, true);
                        }
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar3) {
                        if (seekBar3.getProgress() == 0) {
                            ControlPanelEffect.setParameterBoolean(ActivityMusic.this.mContext, ActivityMusic.this.mCallingPackageName, ActivityMusic.this.mAudioSession, ControlPanelEffect.Key.bb_enabled, false);
                        }
                    }
                });
            }
            if (this.mEqualizerSupported) {
                this.mEQPreset = ControlPanelEffect.getParameterInt(this.mContext, this.mCallingPackageName, this.mAudioSession, ControlPanelEffect.Key.eq_current_preset);
                if (this.mEQPreset >= this.mEQPresetNames.length) {
                    this.mEQPreset = 0;
                }
                this.mEQPresetPrevious = this.mEQPreset;
                equalizerSpinnerInit((Spinner) findViewById(R.id.eqSpinner));
                equalizerBandsInit(findViewById(R.id.eqcontainer));
            }
            if (this.mPresetReverbSupported) {
                this.mPRPreset = ControlPanelEffect.getParameterInt(this.mContext, this.mCallingPackageName, this.mAudioSession, ControlPanelEffect.Key.pr_current_preset);
                this.mPRPresetPrevious = this.mPRPreset;
                reverbSpinnerInit((Spinner) findViewById(R.id.prSpinner));
            }
            ActionBar actionBar = getActionBar();
            this.mToggleSwitch.setPadding(0, 0, getResources().getDimensionPixelSize(R.dimen.action_bar_switch_padding), 0);
            actionBar.setCustomView(this.mToggleSwitch, new ActionBar.LayoutParams(-2, -2, 21));
            actionBar.setDisplayOptions(24);
            return;
        }
        viewGroup.setVisibility(8);
        ((TextView) findViewById(R.id.noEffectsTextView)).setVisibility(0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (this.mVirtualizerSupported || this.mBassBoostSupported || this.mEqualizerSupported || this.mPresetReverbSupported) {
            IntentFilter intentFilter = new IntentFilter("android.intent.action.HEADSET_PLUG");
            intentFilter.addAction("android.bluetooth.device.action.ACL_CONNECTED");
            intentFilter.addAction("android.bluetooth.device.action.ACL_DISCONNECTED");
            intentFilter.addAction("android.media.AUDIO_BECOMING_NOISY");
            registerReceiver(this.mReceiver, intentFilter);
            AudioManager audioManager = (AudioManager) getSystemService("audio");
            this.mIsHeadsetOn = audioManager.isWiredHeadsetOn() || audioManager.isBluetoothA2dpOn();
            Log.v("MusicFXActivityMusic", "onResume: mIsHeadsetOn : " + this.mIsHeadsetOn);
            updateUI();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (this.mVirtualizerSupported || this.mBassBoostSupported || this.mEqualizerSupported || this.mPresetReverbSupported) {
            unregisterReceiver(this.mReceiver);
        }
    }

    private void reverbSpinnerInit(Spinner spinner) {
        ArrayAdapter arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item, PRESETREVERBPRESETSTRINGS);
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter((SpinnerAdapter) arrayAdapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long j) {
                if (i != ActivityMusic.this.mPRPresetPrevious) {
                    ActivityMusic.this.presetReverbSetPreset(i);
                }
                ActivityMusic.this.mPRPresetPrevious = i;
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
        spinner.setSelection(this.mPRPreset);
    }

    private void equalizerSpinnerInit(Spinner spinner) {
        ArrayAdapter arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item, this.mEQPresetNames);
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter((SpinnerAdapter) arrayAdapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long j) {
                if (i != ActivityMusic.this.mEQPresetPrevious) {
                    ActivityMusic.this.equalizerSetPreset(i);
                }
                ActivityMusic.this.mEQPresetPrevious = i;
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
        spinner.setSelection(this.mEQPreset);
    }

    private void setEnabledAllChildren(ViewGroup viewGroup, boolean z) {
        int childCount = viewGroup.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View childAt = viewGroup.getChildAt(i);
            if (childAt instanceof ViewGroup) {
                setEnabledAllChildren((ViewGroup) childAt, z);
            }
            childAt.setEnabled(z);
        }
    }

    private void updateUI() {
        boolean zBooleanValue = ControlPanelEffect.getParameterBoolean(this.mContext, this.mCallingPackageName, this.mAudioSession, ControlPanelEffect.Key.global_enabled).booleanValue();
        this.mToggleSwitch.setChecked(zBooleanValue);
        setEnabledAllChildren((ViewGroup) findViewById(R.id.contentSoundEffects), zBooleanValue);
        updateUIHeadset();
        if (this.mVirtualizerSupported) {
            SeekBar seekBar = (SeekBar) findViewById(R.id.vIStrengthSeekBar);
            Switch r1 = (Switch) findViewById(R.id.vIStrengthToggle);
            int parameterInt = ControlPanelEffect.getParameterInt(this.mContext, this.mCallingPackageName, this.mAudioSession, ControlPanelEffect.Key.virt_strength);
            seekBar.setProgress(parameterInt);
            if (ControlPanelEffect.getParameterBoolean(this.mContext, this.mCallingPackageName, this.mAudioSession, ControlPanelEffect.Key.virt_strength_supported).booleanValue()) {
                r1.setVisibility(8);
            } else {
                seekBar.setVisibility(8);
                r1.setChecked(r1.isEnabled() && parameterInt != 0);
            }
        }
        if (this.mBassBoostSupported) {
            ((SeekBar) findViewById(R.id.bBStrengthSeekBar)).setProgress(ControlPanelEffect.getParameterInt(this.mContext, this.mCallingPackageName, this.mAudioSession, ControlPanelEffect.Key.bb_strength));
        }
        if (this.mEqualizerSupported) {
            equalizerUpdateDisplay();
        }
        if (this.mPresetReverbSupported) {
            ((Spinner) findViewById(R.id.prSpinner)).setSelection(ControlPanelEffect.getParameterInt(this.mContext, this.mCallingPackageName, this.mAudioSession, ControlPanelEffect.Key.pr_current_preset));
        }
    }

    private void updateUIHeadset() {
        if (this.mToggleSwitch.isChecked()) {
            ((TextView) findViewById(R.id.vIStrengthText)).setEnabled(this.mIsHeadsetOn || !this.mVirtualizerIsHeadphoneOnly);
            ((SeekBar) findViewById(R.id.vIStrengthSeekBar)).setEnabled(this.mIsHeadsetOn || !this.mVirtualizerIsHeadphoneOnly);
            findViewById(R.id.vILayout).setEnabled((this.mIsHeadsetOn && this.mVirtualizerIsHeadphoneOnly) ? false : true);
            ((TextView) findViewById(R.id.bBStrengthText)).setEnabled(this.mIsHeadsetOn);
            ((SeekBar) findViewById(R.id.bBStrengthSeekBar)).setEnabled(this.mIsHeadsetOn);
            findViewById(R.id.bBLayout).setEnabled(!this.mIsHeadsetOn);
        }
    }

    private void equalizerBandsInit(View view) {
        this.mNumberEqualizerBands = ControlPanelEffect.getParameterInt(this.mContext, this.mCallingPackageName, this.mAudioSession, ControlPanelEffect.Key.eq_num_bands);
        this.mEQPresetUserBandLevelsPrev = ControlPanelEffect.getParameterIntArray(this.mContext, this.mCallingPackageName, this.mAudioSession, ControlPanelEffect.Key.eq_preset_user_band_level);
        int[] parameterIntArray = ControlPanelEffect.getParameterIntArray(this.mContext, this.mCallingPackageName, this.mAudioSession, ControlPanelEffect.Key.eq_center_freq);
        int[] parameterIntArray2 = ControlPanelEffect.getParameterIntArray(this.mContext, this.mCallingPackageName, this.mAudioSession, ControlPanelEffect.Key.eq_level_range);
        this.mEqualizerMinBandLevel = Math.max(-1000, parameterIntArray2[0]);
        int iMin = Math.min(1000, parameterIntArray2[1]);
        for (int i = 0; i < this.mNumberEqualizerBands; i++) {
            float f = parameterIntArray[i] / 1000;
            String str = "";
            if (f >= 1000.0f) {
                f /= 1000.0f;
                str = "k";
            }
            ((TextView) view.findViewById(EQViewElementIds[i][0])).setText(format("%.0f ", Float.valueOf(f)) + str + "Hz");
            this.mEqualizerSeekBar[i] = (SeekBar) view.findViewById(EQViewElementIds[i][1]);
            view.findViewById(EQViewElementIds[i][0]).setLabelFor(EQViewElementIds[i][1]);
            this.mEqualizerSeekBar[i].setMax(iMin - this.mEqualizerMinBandLevel);
            this.mEqualizerSeekBar[i].setOnSeekBarChangeListener(this);
        }
        for (int i2 = this.mNumberEqualizerBands; i2 < 32; i2++) {
            view.findViewById(EQViewElementIds[i2][0]).setVisibility(8);
            view.findViewById(EQViewElementIds[i2][1]).setVisibility(8);
        }
        ((TextView) findViewById(R.id.maxLevelText)).setText(String.format("+%d dB", Integer.valueOf((int) Math.ceil(iMin / 100))));
        ((TextView) findViewById(R.id.centerLevelText)).setText("0 dB");
        ((TextView) findViewById(R.id.minLevelText)).setText(String.format("%d dB", Integer.valueOf((int) Math.floor(this.mEqualizerMinBandLevel / 100))));
        equalizerUpdateDisplay();
    }

    private String format(String str, Object... objArr) {
        this.mFormatBuilder.setLength(0);
        this.mFormatter.format(str, objArr);
        return this.mFormatBuilder.toString();
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean z) {
        int id = seekBar.getId();
        for (short s = 0; s < this.mNumberEqualizerBands; s = (short) (s + 1)) {
            if (id == EQViewElementIds[s][1]) {
                short s2 = (short) (i + this.mEqualizerMinBandLevel);
                if (z) {
                    equalizerBandUpdate(s, s2);
                    return;
                }
                return;
            }
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        int[] parameterIntArray = ControlPanelEffect.getParameterIntArray(this.mContext, this.mCallingPackageName, this.mAudioSession, ControlPanelEffect.Key.eq_band_level);
        for (short s = 0; s < this.mNumberEqualizerBands; s = (short) (s + 1)) {
            equalizerBandUpdate(s, parameterIntArray[s]);
        }
        equalizerSetPreset(this.mEQPresetUserPos);
        ((Spinner) findViewById(R.id.eqSpinner)).setSelection(this.mEQPresetUserPos);
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        equalizerUpdateDisplay();
    }

    private void equalizerUpdateDisplay() {
        int[] parameterIntArray = ControlPanelEffect.getParameterIntArray(this.mContext, this.mCallingPackageName, this.mAudioSession, ControlPanelEffect.Key.eq_band_level);
        for (short s = 0; s < this.mNumberEqualizerBands; s = (short) (s + 1)) {
            this.mEqualizerSeekBar[s].setProgress(parameterIntArray[s] - this.mEqualizerMinBandLevel);
        }
    }

    private void equalizerBandUpdate(int i, int i2) {
        ControlPanelEffect.setParameterInt(this.mContext, this.mCallingPackageName, this.mAudioSession, ControlPanelEffect.Key.eq_band_level, i2, i);
    }

    private void equalizerSetPreset(int i) {
        ControlPanelEffect.setParameterInt(this.mContext, this.mCallingPackageName, this.mAudioSession, ControlPanelEffect.Key.eq_current_preset, i);
        equalizerUpdateDisplay();
    }

    private void presetReverbSetPreset(int i) {
        ControlPanelEffect.setParameterInt(this.mContext, this.mCallingPackageName, this.mAudioSession, ControlPanelEffect.Key.pr_current_preset, i);
    }

    private void showHeadsetMsg() {
        Toast toastMakeText = Toast.makeText(getApplicationContext(), getString(R.string.headset_plug), 0);
        toastMakeText.setGravity(17, toastMakeText.getXOffset() / 2, toastMakeText.getYOffset() / 2);
        toastMakeText.show();
    }

    private static boolean isVirtualizerTransauralSupported() throws Throwable {
        Virtualizer virtualizer;
        Virtualizer virtualizer2 = null;
        try {
            virtualizer = new Virtualizer(0, AudioSystem.newAudioSessionId());
        } catch (Exception e) {
        } catch (Throwable th) {
            th = th;
        }
        try {
            boolean zCanVirtualize = virtualizer.canVirtualize(12, 3);
            virtualizer.release();
            return zCanVirtualize;
        } catch (Exception e2) {
            virtualizer2 = virtualizer;
            if (virtualizer2 == null) {
                return false;
            }
            virtualizer2.release();
            return false;
        } catch (Throwable th2) {
            th = th2;
            virtualizer2 = virtualizer;
            if (virtualizer2 != null) {
                virtualizer2.release();
            }
            throw th;
        }
    }
}
