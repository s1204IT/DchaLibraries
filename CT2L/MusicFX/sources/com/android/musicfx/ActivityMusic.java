package com.android.musicfx;

import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.audiofx.AudioEffect;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import com.android.musicfx.ControlPanelEffect;
import com.android.musicfx.seekbar.SeekBar;
import java.util.Formatter;
import java.util.Locale;
import java.util.UUID;

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
            boolean isHeadsetOnPrev = ActivityMusic.this.mIsHeadsetOn;
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
            if (isHeadsetOnPrev != ActivityMusic.this.mIsHeadsetOn) {
                ActivityMusic.this.updateUIHeadset();
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) throws Throwable {
        super.onCreate(savedInstanceState);
        this.mContext = this;
        Intent intent = getIntent();
        this.mAudioSession = intent.getIntExtra("android.media.extra.AUDIO_SESSION", -4);
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
        AudioEffect.Descriptor[] effects = AudioEffect.queryEffects();
        Log.v("MusicFXActivityMusic", "Available effects:");
        for (AudioEffect.Descriptor effect : effects) {
            Log.v("MusicFXActivityMusic", effect.name.toString() + ", type: " + effect.type.toString());
            if (effect.type.equals(AudioEffect.EFFECT_TYPE_VIRTUALIZER)) {
                this.mVirtualizerSupported = true;
                if (effect.uuid.equals(UUID.fromString("1d4033c0-8557-11df-9f2d-0002a5d5c51b"))) {
                    this.mVirtualizerIsHeadphoneOnly = true;
                }
            } else if (effect.type.equals(AudioEffect.EFFECT_TYPE_BASS_BOOST)) {
                this.mBassBoostSupported = true;
            } else if (effect.type.equals(AudioEffect.EFFECT_TYPE_EQUALIZER)) {
                this.mEqualizerSupported = true;
            } else if (effect.type.equals(AudioEffect.EFFECT_TYPE_PRESET_REVERB)) {
                this.mPresetReverbSupported = true;
            }
        }
        setContentView(R.layout.music_main);
        final ViewGroup viewGroup = (ViewGroup) findViewById(R.id.contentSoundEffects);
        int numPresets = ControlPanelEffect.getParameterInt(this.mContext, this.mCallingPackageName, this.mAudioSession, ControlPanelEffect.Key.eq_num_presets);
        this.mEQPresetNames = new String[numPresets + 2];
        for (short i = 0; i < numPresets; i = (short) (i + 1)) {
            this.mEQPresetNames[i] = ControlPanelEffect.getParameterString(this.mContext, this.mCallingPackageName, this.mAudioSession, ControlPanelEffect.Key.eq_preset_name, i);
        }
        this.mEQPresetNames[numPresets] = getString(R.string.ci_extreme);
        this.mEQPresetNames[numPresets + 1] = getString(R.string.user);
        this.mEQPresetUserPos = numPresets + 1;
        if (this.mVirtualizerSupported || this.mBassBoostSupported || this.mEqualizerSupported || this.mPresetReverbSupported) {
            this.mToggleSwitch = new Switch(this);
            this.mToggleSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    ControlPanelEffect.setParameterBoolean(ActivityMusic.this.mContext, ActivityMusic.this.mCallingPackageName, ActivityMusic.this.mAudioSession, ControlPanelEffect.Key.global_enabled, isChecked);
                    ActivityMusic.this.setEnabledAllChildren(viewGroup, isChecked);
                    ActivityMusic.this.updateUIHeadset();
                }
            });
            if (this.mVirtualizerSupported) {
                findViewById(R.id.vILayout).setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        if (event.getAction() == 1) {
                            ActivityMusic.this.showHeadsetMsg();
                            return false;
                        }
                        return false;
                    }
                });
                SeekBar seekbar = (SeekBar) findViewById(R.id.vIStrengthSeekBar);
                seekbar.setMax(1000);
                seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        ControlPanelEffect.setParameterInt(ActivityMusic.this.mContext, ActivityMusic.this.mCallingPackageName, ActivityMusic.this.mAudioSession, ControlPanelEffect.Key.virt_strength, progress);
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                        if (seekBar.getProgress() == 0) {
                            ControlPanelEffect.setParameterBoolean(ActivityMusic.this.mContext, ActivityMusic.this.mCallingPackageName, ActivityMusic.this.mAudioSession, ControlPanelEffect.Key.virt_enabled, true);
                        }
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        if (seekBar.getProgress() == 0) {
                            ControlPanelEffect.setParameterBoolean(ActivityMusic.this.mContext, ActivityMusic.this.mCallingPackageName, ActivityMusic.this.mAudioSession, ControlPanelEffect.Key.virt_enabled, false);
                        }
                    }
                });
                Switch sw = (Switch) findViewById(R.id.vIStrengthToggle);
                sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        ControlPanelEffect.setParameterBoolean(ActivityMusic.this.mContext, ActivityMusic.this.mCallingPackageName, ActivityMusic.this.mAudioSession, ControlPanelEffect.Key.virt_enabled, isChecked);
                    }
                });
            }
            if (this.mBassBoostSupported) {
                findViewById(R.id.bBLayout).setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        if (event.getAction() == 1) {
                            ActivityMusic.this.showHeadsetMsg();
                            return false;
                        }
                        return false;
                    }
                });
                SeekBar seekbar2 = (SeekBar) findViewById(R.id.bBStrengthSeekBar);
                seekbar2.setMax(1000);
                seekbar2.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        ControlPanelEffect.setParameterInt(ActivityMusic.this.mContext, ActivityMusic.this.mCallingPackageName, ActivityMusic.this.mAudioSession, ControlPanelEffect.Key.bb_strength, progress);
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                        if (seekBar.getProgress() == 0) {
                            ControlPanelEffect.setParameterBoolean(ActivityMusic.this.mContext, ActivityMusic.this.mCallingPackageName, ActivityMusic.this.mAudioSession, ControlPanelEffect.Key.bb_enabled, true);
                        }
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        if (seekBar.getProgress() == 0) {
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
        } else {
            viewGroup.setVisibility(8);
            ((TextView) findViewById(R.id.noEffectsTextView)).setVisibility(0);
        }
        ActionBar ab = getActionBar();
        int padding = getResources().getDimensionPixelSize(R.dimen.action_bar_switch_padding);
        this.mToggleSwitch.setPadding(0, 0, padding, 0);
        ab.setCustomView(this.mToggleSwitch, new ActionBar.LayoutParams(-2, -2, 21));
        ab.setDisplayOptions(24);
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
        unregisterReceiver(this.mReceiver);
    }

    private void reverbSpinnerInit(Spinner spinner) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, PRESETREVERBPRESETSTRINGS);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter((SpinnerAdapter) adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position != ActivityMusic.this.mPRPresetPrevious) {
                    ActivityMusic.this.presetReverbSetPreset(position);
                }
                ActivityMusic.this.mPRPresetPrevious = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        spinner.setSelection(this.mPRPreset);
    }

    private void equalizerSpinnerInit(Spinner spinner) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, this.mEQPresetNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter((SpinnerAdapter) adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position != ActivityMusic.this.mEQPresetPrevious) {
                    ActivityMusic.this.equalizerSetPreset(position);
                }
                ActivityMusic.this.mEQPresetPrevious = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        spinner.setSelection(this.mEQPreset);
    }

    private void setEnabledAllChildren(ViewGroup viewGroup, boolean enabled) {
        int count = viewGroup.getChildCount();
        for (int i = 0; i < count; i++) {
            View view = viewGroup.getChildAt(i);
            if ((view instanceof LinearLayout) || (view instanceof RelativeLayout)) {
                ViewGroup vg = (ViewGroup) view;
                setEnabledAllChildren(vg, enabled);
            }
            view.setEnabled(enabled);
        }
    }

    private void updateUI() {
        boolean isEnabled = ControlPanelEffect.getParameterBoolean(this.mContext, this.mCallingPackageName, this.mAudioSession, ControlPanelEffect.Key.global_enabled).booleanValue();
        this.mToggleSwitch.setChecked(isEnabled);
        setEnabledAllChildren((ViewGroup) findViewById(R.id.contentSoundEffects), isEnabled);
        updateUIHeadset();
        if (this.mVirtualizerSupported) {
            SeekBar bar = (SeekBar) findViewById(R.id.vIStrengthSeekBar);
            Switch sw = (Switch) findViewById(R.id.vIStrengthToggle);
            int strength = ControlPanelEffect.getParameterInt(this.mContext, this.mCallingPackageName, this.mAudioSession, ControlPanelEffect.Key.virt_strength);
            bar.setProgress(strength);
            boolean hasStrength = ControlPanelEffect.getParameterBoolean(this.mContext, this.mCallingPackageName, this.mAudioSession, ControlPanelEffect.Key.virt_strength_supported).booleanValue();
            if (hasStrength) {
                sw.setVisibility(8);
            } else {
                bar.setVisibility(8);
                sw.setChecked(sw.isEnabled() && strength != 0);
            }
        }
        if (this.mBassBoostSupported) {
            ((SeekBar) findViewById(R.id.bBStrengthSeekBar)).setProgress(ControlPanelEffect.getParameterInt(this.mContext, this.mCallingPackageName, this.mAudioSession, ControlPanelEffect.Key.bb_strength));
        }
        if (this.mEqualizerSupported) {
            equalizerUpdateDisplay();
        }
        if (this.mPresetReverbSupported) {
            int reverb = ControlPanelEffect.getParameterInt(this.mContext, this.mCallingPackageName, this.mAudioSession, ControlPanelEffect.Key.pr_current_preset);
            ((Spinner) findViewById(R.id.prSpinner)).setSelection(reverb);
        }
    }

    private void updateUIHeadset() {
        if (this.mToggleSwitch.isChecked()) {
            ((TextView) findViewById(R.id.vIStrengthText)).setEnabled(this.mIsHeadsetOn || !this.mVirtualizerIsHeadphoneOnly);
            ((SeekBar) findViewById(R.id.vIStrengthSeekBar)).setEnabled(this.mIsHeadsetOn || !this.mVirtualizerIsHeadphoneOnly);
            findViewById(R.id.vILayout).setEnabled((this.mIsHeadsetOn && this.mVirtualizerIsHeadphoneOnly) ? false : true);
            ((TextView) findViewById(R.id.bBStrengthText)).setEnabled(this.mIsHeadsetOn);
            ((SeekBar) findViewById(R.id.bBStrengthSeekBar)).setEnabled(this.mIsHeadsetOn);
            findViewById(R.id.bBLayout).setEnabled(this.mIsHeadsetOn ? false : true);
        }
    }

    private void equalizerBandsInit(View eqcontainer) {
        this.mNumberEqualizerBands = ControlPanelEffect.getParameterInt(this.mContext, this.mCallingPackageName, this.mAudioSession, ControlPanelEffect.Key.eq_num_bands);
        this.mEQPresetUserBandLevelsPrev = ControlPanelEffect.getParameterIntArray(this.mContext, this.mCallingPackageName, this.mAudioSession, ControlPanelEffect.Key.eq_preset_user_band_level);
        int[] centerFreqs = ControlPanelEffect.getParameterIntArray(this.mContext, this.mCallingPackageName, this.mAudioSession, ControlPanelEffect.Key.eq_center_freq);
        int[] bandLevelRange = ControlPanelEffect.getParameterIntArray(this.mContext, this.mCallingPackageName, this.mAudioSession, ControlPanelEffect.Key.eq_level_range);
        this.mEqualizerMinBandLevel = bandLevelRange[0];
        int mEqualizerMaxBandLevel = bandLevelRange[1];
        for (int band = 0; band < this.mNumberEqualizerBands; band++) {
            int centerFreq = centerFreqs[band] / 1000;
            float centerFreqHz = centerFreq;
            String unitPrefix = "";
            if (centerFreqHz >= 1000.0f) {
                centerFreqHz /= 1000.0f;
                unitPrefix = "k";
            }
            ((TextView) eqcontainer.findViewById(EQViewElementIds[band][0])).setText(format("%.0f ", Float.valueOf(centerFreqHz)) + unitPrefix + "Hz");
            this.mEqualizerSeekBar[band] = (SeekBar) eqcontainer.findViewById(EQViewElementIds[band][1]);
            this.mEqualizerSeekBar[band].setMax(mEqualizerMaxBandLevel - this.mEqualizerMinBandLevel);
            this.mEqualizerSeekBar[band].setOnSeekBarChangeListener(this);
        }
        for (int band2 = this.mNumberEqualizerBands; band2 < 32; band2++) {
            eqcontainer.findViewById(EQViewElementIds[band2][0]).setVisibility(8);
            eqcontainer.findViewById(EQViewElementIds[band2][1]).setVisibility(8);
        }
        TextView tv = (TextView) findViewById(R.id.maxLevelText);
        tv.setText("+15 dB");
        TextView tv2 = (TextView) findViewById(R.id.centerLevelText);
        tv2.setText("0 dB");
        TextView tv3 = (TextView) findViewById(R.id.minLevelText);
        tv3.setText("-15 dB");
        equalizerUpdateDisplay();
    }

    private String format(String format, Object... args) {
        this.mFormatBuilder.setLength(0);
        this.mFormatter.format(format, args);
        return this.mFormatBuilder.toString();
    }

    @Override
    public void onProgressChanged(SeekBar seekbar, int progress, boolean fromUser) {
        int id = seekbar.getId();
        for (short band = 0; band < this.mNumberEqualizerBands; band = (short) (band + 1)) {
            if (id == EQViewElementIds[band][1]) {
                short level = (short) (this.mEqualizerMinBandLevel + progress);
                if (fromUser) {
                    equalizerBandUpdate(band, level);
                    return;
                }
                return;
            }
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekbar) {
        int[] bandLevels = ControlPanelEffect.getParameterIntArray(this.mContext, this.mCallingPackageName, this.mAudioSession, ControlPanelEffect.Key.eq_band_level);
        for (short band = 0; band < this.mNumberEqualizerBands; band = (short) (band + 1)) {
            equalizerBandUpdate(band, bandLevels[band]);
        }
        equalizerSetPreset(this.mEQPresetUserPos);
        ((Spinner) findViewById(R.id.eqSpinner)).setSelection(this.mEQPresetUserPos);
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekbar) {
        equalizerUpdateDisplay();
    }

    private void equalizerUpdateDisplay() {
        int[] bandLevels = ControlPanelEffect.getParameterIntArray(this.mContext, this.mCallingPackageName, this.mAudioSession, ControlPanelEffect.Key.eq_band_level);
        for (short band = 0; band < this.mNumberEqualizerBands; band = (short) (band + 1)) {
            int level = bandLevels[band];
            int progress = level - this.mEqualizerMinBandLevel;
            this.mEqualizerSeekBar[band].setProgress(progress);
        }
    }

    private void equalizerBandUpdate(int band, int level) {
        ControlPanelEffect.setParameterInt(this.mContext, this.mCallingPackageName, this.mAudioSession, ControlPanelEffect.Key.eq_band_level, level, band);
    }

    private void equalizerSetPreset(int preset) {
        ControlPanelEffect.setParameterInt(this.mContext, this.mCallingPackageName, this.mAudioSession, ControlPanelEffect.Key.eq_current_preset, preset);
        equalizerUpdateDisplay();
    }

    private void presetReverbSetPreset(int preset) {
        ControlPanelEffect.setParameterInt(this.mContext, this.mCallingPackageName, this.mAudioSession, ControlPanelEffect.Key.pr_current_preset, preset);
    }

    private void showHeadsetMsg() {
        Context context = getApplicationContext();
        Toast toast = Toast.makeText(context, getString(R.string.headset_plug), 0);
        toast.setGravity(17, toast.getXOffset() / 2, toast.getYOffset() / 2);
        toast.show();
    }
}
