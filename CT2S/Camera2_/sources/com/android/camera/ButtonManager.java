package com.android.camera;

import android.content.Context;
import android.content.res.TypedArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import com.android.camera.MultiToggleImageButton;
import com.android.camera.app.AppController;
import com.android.camera.app.CameraAppUI;
import com.android.camera.settings.Keys;
import com.android.camera.settings.SettingsManager;
import com.android.camera.ui.RadioOptions;
import com.android.camera.util.PhotoSphereHelper;
import com.android.camera.widget.ModeOptions;
import com.android.camera2.R;

public class ButtonManager implements SettingsManager.OnSettingChangedListener {
    public static final int BUTTON_AUTO_FOCUS = 13;
    public static final int BUTTON_CAMERA = 3;
    public static final int BUTTON_CANCEL = 6;
    public static final int BUTTON_COUNTDOWN = 12;
    public static final int BUTTON_DONE = 7;
    public static final int BUTTON_EXPOSURE_COMPENSATION = 11;
    public static final int BUTTON_FLASH = 0;
    public static final int BUTTON_GRID_LINES = 10;
    public static final int BUTTON_HDR = 5;
    public static final int BUTTON_HDR_PLUS = 4;
    public static final int BUTTON_HDR_PLUS_FLASH = 2;
    public static final int BUTTON_RETAKE = 8;
    public static final int BUTTON_REVIEW = 9;
    public static final int BUTTON_TORCH = 1;
    public static final int OFF = 0;
    public static final int ON = 1;
    private static int sGcamIndex;
    private final AppController mAppController;
    private MultiToggleImageButton mButtonAutoFocus;
    private MultiToggleImageButton mButtonCamera;
    private ImageButton mButtonCancel;
    private MultiToggleImageButton mButtonCountdown;
    private ImageButton mButtonDone;
    private ImageButton mButtonExposureCompensation;
    private MultiToggleImageButton mButtonFlash;
    private MultiToggleImageButton mButtonGridlines;
    private MultiToggleImageButton mButtonHdr;
    private ImageButton mButtonRetake;
    private boolean mCameraMod = false;
    private ImageButton mExposure0;
    private float mExposureCompensationStep;
    private ImageButton mExposureN1;
    private ImageButton mExposureN2;
    private ImageButton mExposureP1;
    private ImageButton mExposureP2;
    private ButtonStatusListener mListener;
    private int mMaxExposureCompensation;
    private int mMinExposureCompensation;
    private ModeOptions mModeOptions;
    private View mModeOptionsButtons;
    private RadioOptions mModeOptionsExposure;
    private RadioOptions mModeOptionsPano;
    private final SettingsManager mSettingsManager;

    public interface ButtonCallback {
        void onStateChanged(int i);
    }

    public interface ButtonStatusListener {
        void onButtonEnabledChanged(ButtonManager buttonManager, int i);

        void onButtonVisibilityChanged(ButtonManager buttonManager, int i);
    }

    public ButtonManager(AppController app) {
        this.mAppController = app;
        Context context = app.getAndroidContext();
        sGcamIndex = context.getResources().getInteger(R.integer.camera_mode_gcam);
        this.mSettingsManager = app.getSettingsManager();
        this.mSettingsManager.addListener(this);
    }

    public void load(View root) {
        getButtonsReferences(root);
    }

    public void setListener(ButtonStatusListener listener) {
        this.mListener = listener;
    }

    private void getButtonsReferences(View root) {
        this.mButtonCamera = (MultiToggleImageButton) root.findViewById(R.id.camera_toggle_button);
        this.mButtonFlash = (MultiToggleImageButton) root.findViewById(R.id.flash_toggle_button);
        this.mButtonHdr = (MultiToggleImageButton) root.findViewById(R.id.hdr_plus_toggle_button);
        this.mButtonGridlines = (MultiToggleImageButton) root.findViewById(R.id.grid_lines_toggle_button);
        this.mButtonAutoFocus = (MultiToggleImageButton) root.findViewById(R.id.auto_focus_toggle_button);
        this.mButtonCancel = (ImageButton) root.findViewById(R.id.cancel_button);
        this.mButtonDone = (ImageButton) root.findViewById(R.id.done_button);
        this.mButtonRetake = (ImageButton) root.findViewById(R.id.retake_button);
        this.mButtonExposureCompensation = (ImageButton) root.findViewById(R.id.exposure_button);
        this.mExposureN2 = (ImageButton) root.findViewById(R.id.exposure_n2);
        this.mExposureN1 = (ImageButton) root.findViewById(R.id.exposure_n1);
        this.mExposure0 = (ImageButton) root.findViewById(R.id.exposure_0);
        this.mExposureP1 = (ImageButton) root.findViewById(R.id.exposure_p1);
        this.mExposureP2 = (ImageButton) root.findViewById(R.id.exposure_p2);
        this.mModeOptionsExposure = (RadioOptions) root.findViewById(R.id.mode_options_exposure);
        this.mModeOptionsPano = (RadioOptions) root.findViewById(R.id.mode_options_pano);
        this.mModeOptionsButtons = root.findViewById(R.id.mode_options_buttons);
        this.mModeOptions = (ModeOptions) root.findViewById(R.id.mode_options);
        this.mButtonCountdown = (MultiToggleImageButton) root.findViewById(R.id.countdown_toggle_button);
    }

    @Override
    public void onSettingChanged(SettingsManager settingsManager, String key) {
        MultiToggleImageButton button = null;
        int index = 0;
        if (key.equals(Keys.KEY_FLASH_MODE)) {
            index = this.mSettingsManager.getIndexOfCurrentValue(this.mAppController.getCameraScope(), Keys.KEY_FLASH_MODE);
            button = getButtonOrError(0);
        } else if (key.equals(Keys.KEY_VIDEOCAMERA_FLASH_MODE)) {
            index = this.mSettingsManager.getIndexOfCurrentValue(this.mAppController.getCameraScope(), Keys.KEY_VIDEOCAMERA_FLASH_MODE);
            button = getButtonOrError(1);
        } else if (key.equals(Keys.KEY_HDR_PLUS_FLASH_MODE)) {
            index = this.mSettingsManager.getIndexOfCurrentValue(this.mAppController.getModuleScope(), Keys.KEY_HDR_PLUS_FLASH_MODE);
            button = getButtonOrError(2);
        } else if (key.equals(Keys.KEY_CAMERA_ID)) {
            index = this.mSettingsManager.getIndexOfCurrentValue(this.mAppController.getModuleScope(), Keys.KEY_CAMERA_ID);
            button = getButtonOrError(3);
        } else if (key.equals(Keys.KEY_CAMERA_HDR_PLUS)) {
            index = this.mSettingsManager.getIndexOfCurrentValue(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_HDR_PLUS);
            button = getButtonOrError(4);
        } else if (key.equals(Keys.KEY_CAMERA_HDR)) {
            index = this.mSettingsManager.getIndexOfCurrentValue(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_HDR);
            button = getButtonOrError(5);
        } else if (key.equals(Keys.KEY_CAMERA_GRID_LINES)) {
            index = this.mSettingsManager.getIndexOfCurrentValue(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_GRID_LINES);
            button = getButtonOrError(10);
        } else if (key.equals(Keys.KEY_CAMERA_PANO_ORIENTATION)) {
            updatePanoButtons();
        } else if (key.equals(Keys.KEY_EXPOSURE)) {
            updateExposureButtons();
        } else if (key.equals(Keys.KEY_COUNTDOWN_DURATION)) {
            index = this.mSettingsManager.getIndexOfCurrentValue(SettingsManager.SCOPE_GLOBAL, Keys.KEY_COUNTDOWN_DURATION);
            button = getButtonOrError(12);
        } else if (key.equals(Keys.KEY_CAMERA_AUTO_FOCUS)) {
            index = this.mSettingsManager.getIndexOfCurrentValue(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_AUTO_FOCUS);
            button = getButtonOrError(13);
        }
        if (button != null && button.getState() != index) {
            button.setState(Math.max(index, 0), false);
        }
    }

    private MultiToggleImageButton getButtonOrError(int buttonId) {
        switch (buttonId) {
            case 0:
                if (this.mButtonFlash == null) {
                    throw new IllegalStateException("Flash button could not be found.");
                }
                return this.mButtonFlash;
            case 1:
                if (this.mButtonFlash == null) {
                    throw new IllegalStateException("Torch button could not be found.");
                }
                return this.mButtonFlash;
            case 2:
                if (this.mButtonFlash == null) {
                    throw new IllegalStateException("Hdr plus torch button could not be found.");
                }
                return this.mButtonFlash;
            case 3:
                if (this.mButtonCamera == null) {
                    throw new IllegalStateException("Camera button could not be found.");
                }
                return this.mButtonCamera;
            case 4:
                if (this.mButtonHdr == null) {
                    throw new IllegalStateException("Hdr plus button could not be found.");
                }
                return this.mButtonHdr;
            case 5:
                if (this.mButtonHdr == null) {
                    throw new IllegalStateException("Hdr button could not be found.");
                }
                return this.mButtonHdr;
            case 6:
            case 7:
            case 8:
            case 9:
            case 11:
            default:
                throw new IllegalArgumentException("button not known by id=" + buttonId);
            case 10:
                if (this.mButtonGridlines == null) {
                    throw new IllegalStateException("Grid lines button could not be found.");
                }
                return this.mButtonGridlines;
            case 12:
                if (this.mButtonCountdown == null) {
                    throw new IllegalStateException("Countdown button could not be found.");
                }
                return this.mButtonCountdown;
            case BUTTON_AUTO_FOCUS:
                if (this.mButtonAutoFocus == null) {
                    throw new IllegalStateException("Auto Focus button could not be found.");
                }
                return this.mButtonAutoFocus;
        }
    }

    private ImageButton getImageButtonOrError(int buttonId) {
        switch (buttonId) {
            case 6:
                if (this.mButtonCancel == null) {
                    throw new IllegalStateException("Cancel button could not be found.");
                }
                return this.mButtonCancel;
            case 7:
                if (this.mButtonDone == null) {
                    throw new IllegalStateException("Done button could not be found.");
                }
                return this.mButtonDone;
            case 8:
                if (this.mButtonRetake == null) {
                    throw new IllegalStateException("Retake button could not be found.");
                }
                return this.mButtonRetake;
            case 9:
                if (this.mButtonRetake == null) {
                    throw new IllegalStateException("Review button could not be found.");
                }
                return this.mButtonRetake;
            case 10:
            default:
                throw new IllegalArgumentException("button not known by id=" + buttonId);
            case 11:
                if (this.mButtonExposureCompensation == null) {
                    throw new IllegalStateException("Exposure Compensation button could not be found.");
                }
                return this.mButtonExposureCompensation;
        }
    }

    public void initializeButton(int buttonId, ButtonCallback cb) {
        MultiToggleImageButton button = getButtonOrError(buttonId);
        switch (buttonId) {
            case 0:
                initializeFlashButton(button, cb, R.array.camera_flashmode_icons);
                break;
            case 1:
                initializeTorchButton(button, cb, R.array.video_flashmode_icons);
                break;
            case 2:
                initializeHdrPlusFlashButton(button, cb, R.array.camera_flashmode_icons);
                break;
            case 3:
                initializeCameraButton(button, cb, R.array.camera_id_icons);
                break;
            case 4:
                initializeHdrPlusButton(button, cb, R.array.pref_camera_hdr_plus_icons);
                break;
            case 5:
                initializeHdrButton(button, cb, R.array.pref_camera_hdr_icons);
                break;
            case 6:
            case 7:
            case 8:
            case 9:
            case 11:
            default:
                throw new IllegalArgumentException("button not known by id=" + buttonId);
            case 10:
                initializeGridLinesButton(button, cb, R.array.grid_lines_icons);
                break;
            case 12:
                initializeCountdownButton(button, cb, R.array.countdown_duration_icons);
                break;
            case BUTTON_AUTO_FOCUS:
                initializeAutoFocusButton(button, cb, R.array.auto_focus_icons);
                break;
        }
        if (buttonId == 3) {
            if (this.mCameraMod) {
                disableButton(buttonId);
            } else {
                enableButton(buttonId);
            }
            this.mCameraMod = false;
            return;
        }
        enableButton(buttonId);
    }

    public void initializePushButton(int buttonId, View.OnClickListener cb, int imageId) {
        ImageButton button = getImageButtonOrError(buttonId);
        button.setOnClickListener(cb);
        button.setImageResource(imageId);
        if (!button.isEnabled()) {
            button.setEnabled(true);
            if (this.mListener != null) {
                this.mListener.onButtonEnabledChanged(this, buttonId);
            }
        }
        button.setTag(R.string.tag_enabled_id, Integer.valueOf(buttonId));
        if (button.getVisibility() != 0) {
            button.setVisibility(0);
            if (this.mListener != null) {
                this.mListener.onButtonVisibilityChanged(this, buttonId);
            }
        }
    }

    public void initializePushButton(int buttonId, View.OnClickListener cb) {
        ImageButton button = getImageButtonOrError(buttonId);
        if (cb != null) {
            button.setOnClickListener(cb);
        }
        if (!button.isEnabled()) {
            button.setEnabled(true);
            if (this.mListener != null) {
                this.mListener.onButtonEnabledChanged(this, buttonId);
            }
        }
        button.setTag(R.string.tag_enabled_id, Integer.valueOf(buttonId));
        if (button.getVisibility() != 0) {
            button.setVisibility(0);
            if (this.mListener != null) {
                this.mListener.onButtonVisibilityChanged(this, buttonId);
            }
        }
    }

    public void disableButton(int buttonId) {
        MultiToggleImageButton button = getButtonOrError(buttonId);
        if (buttonId == 4) {
            initializeHdrPlusButtonIcons(button, R.array.pref_camera_hdr_plus_icons);
        } else if (buttonId == 5) {
            initializeHdrButtonIcons(button, R.array.pref_camera_hdr_icons);
        }
        if (button.isEnabled()) {
            button.setEnabled(false);
            if (this.mListener != null) {
                this.mListener.onButtonEnabledChanged(this, buttonId);
            }
        }
        button.setTag(R.string.tag_enabled_id, null);
        if (button.getVisibility() != 0) {
            button.setVisibility(0);
            if (this.mListener != null) {
                this.mListener.onButtonVisibilityChanged(this, buttonId);
            }
        }
    }

    public void enableButton(int buttonId) {
        ImageButton button = getButtonOrError(buttonId);
        if (!button.isEnabled()) {
            button.setEnabled(true);
            if (this.mListener != null) {
                this.mListener.onButtonEnabledChanged(this, buttonId);
            }
        }
        button.setTag(R.string.tag_enabled_id, Integer.valueOf(buttonId));
        if (button.getVisibility() != 0) {
            button.setVisibility(0);
            if (this.mListener != null) {
                this.mListener.onButtonVisibilityChanged(this, buttonId);
            }
        }
    }

    public void disableButtonClick(int buttonId) {
        ImageButton button = getButtonOrError(buttonId);
        if (button instanceof MultiToggleImageButton) {
            ((MultiToggleImageButton) button).setClickEnabled(false);
        }
    }

    public void enableButtonClick(int buttonId) {
        ImageButton button = getButtonOrError(buttonId);
        if (button instanceof MultiToggleImageButton) {
            ((MultiToggleImageButton) button).setClickEnabled(true);
        }
    }

    public void hideButton(int buttonId) {
        View button;
        try {
            button = getButtonOrError(buttonId);
        } catch (IllegalArgumentException e) {
            button = getImageButtonOrError(buttonId);
        }
        if (button.getVisibility() == 0) {
            button.setVisibility(8);
            if (this.mListener != null) {
                this.mListener.onButtonVisibilityChanged(this, buttonId);
            }
        }
    }

    public void setToInitialState() {
        this.mModeOptions.setMainBar(0);
    }

    public void setExposureCompensationCallback(final CameraAppUI.BottomBarUISpec.ExposureCompensationSetCallback cb) {
        if (cb == null) {
            this.mModeOptionsExposure.setOnOptionClickListener(null);
        } else {
            this.mModeOptionsExposure.setOnOptionClickListener(new RadioOptions.OnOptionClickListener() {
                @Override
                public void onOptionClicked(View v) {
                    int comp = Integer.parseInt((String) v.getTag());
                    if (ButtonManager.this.mExposureCompensationStep != 0.0f) {
                        int compValue = Math.round(comp / ButtonManager.this.mExposureCompensationStep);
                        cb.setExposure(compValue);
                    }
                }
            });
        }
    }

    public void setExposureCompensationParameters(int min, int max, float step) {
        this.mMaxExposureCompensation = max;
        this.mMinExposureCompensation = min;
        this.mExposureCompensationStep = step;
        setVisible(this.mExposureN2, Math.round(((float) min) * step) <= -2);
        setVisible(this.mExposureN1, Math.round(((float) min) * step) <= -1);
        setVisible(this.mExposureP1, Math.round(((float) max) * step) >= 1);
        setVisible(this.mExposureP2, Math.round(((float) max) * step) >= 2);
        updateExposureButtons();
    }

    private static void setVisible(View v, boolean visible) {
        if (visible) {
            v.setVisibility(0);
        } else {
            v.setVisibility(4);
        }
    }

    public float getExposureCompensationStep() {
        return this.mExposureCompensationStep;
    }

    public boolean isEnabled(int buttonId) {
        View button;
        try {
            button = getButtonOrError(buttonId);
        } catch (IllegalArgumentException e) {
            button = getImageButtonOrError(buttonId);
        }
        Integer enabledId = (Integer) button.getTag(R.string.tag_enabled_id);
        return enabledId != null && enabledId.intValue() == buttonId && button.isEnabled();
    }

    public boolean isVisible(int buttonId) {
        View button;
        try {
            button = getButtonOrError(buttonId);
        } catch (IllegalArgumentException e) {
            button = getImageButtonOrError(buttonId);
        }
        return button.getVisibility() == 0;
    }

    private void initializeFlashButton(MultiToggleImageButton button, final ButtonCallback cb, int resIdImages) {
        if (resIdImages > 0) {
            button.overrideImageIds(resIdImages);
        }
        button.overrideContentDescriptions(R.array.camera_flash_descriptions);
        int index = this.mSettingsManager.getIndexOfCurrentValue(this.mAppController.getCameraScope(), Keys.KEY_FLASH_MODE);
        if (index < 0) {
            index = 0;
        }
        button.setState(index, false);
        button.setOnStateChangeListener(new MultiToggleImageButton.OnStateChangeListener() {
            @Override
            public void stateChanged(View view, int state) {
                ButtonManager.this.mSettingsManager.setValueByIndex(ButtonManager.this.mAppController.getCameraScope(), Keys.KEY_FLASH_MODE, state);
                if (cb != null) {
                    cb.onStateChanged(state);
                }
            }
        });
    }

    private void initializeTorchButton(MultiToggleImageButton button, final ButtonCallback cb, int resIdImages) {
        if (resIdImages > 0) {
            button.overrideImageIds(resIdImages);
        }
        button.overrideContentDescriptions(R.array.video_flash_descriptions);
        int index = this.mSettingsManager.getIndexOfCurrentValue(this.mAppController.getCameraScope(), Keys.KEY_VIDEOCAMERA_FLASH_MODE);
        if (index < 0) {
            index = 0;
        }
        button.setState(index, false);
        button.setOnStateChangeListener(new MultiToggleImageButton.OnStateChangeListener() {
            @Override
            public void stateChanged(View view, int state) {
                ButtonManager.this.mSettingsManager.setValueByIndex(ButtonManager.this.mAppController.getCameraScope(), Keys.KEY_VIDEOCAMERA_FLASH_MODE, state);
                if (cb != null) {
                    cb.onStateChanged(state);
                }
            }
        });
    }

    private void initializeHdrPlusFlashButton(MultiToggleImageButton button, final ButtonCallback cb, int resIdImages) {
        if (resIdImages > 0) {
            button.overrideImageIds(resIdImages);
        }
        button.overrideContentDescriptions(R.array.hdr_plus_flash_descriptions);
        int index = this.mSettingsManager.getIndexOfCurrentValue(this.mAppController.getModuleScope(), Keys.KEY_HDR_PLUS_FLASH_MODE);
        if (index < 0) {
            index = 0;
        }
        button.setState(index, false);
        button.setOnStateChangeListener(new MultiToggleImageButton.OnStateChangeListener() {
            @Override
            public void stateChanged(View view, int state) {
                ButtonManager.this.mSettingsManager.setValueByIndex(ButtonManager.this.mAppController.getModuleScope(), Keys.KEY_HDR_PLUS_FLASH_MODE, state);
                if (cb != null) {
                    cb.onStateChanged(state);
                }
            }
        });
    }

    private void initializeCameraButton(final MultiToggleImageButton button, final ButtonCallback cb, int resIdImages) {
        if (resIdImages > 0) {
            button.overrideImageIds(resIdImages);
        }
        int index = this.mSettingsManager.getIndexOfCurrentValue(this.mAppController.getModuleScope(), Keys.KEY_CAMERA_ID);
        if (index < 0) {
            index = 0;
        }
        button.setState(index, false);
        button.setOnStateChangeListener(new MultiToggleImageButton.OnStateChangeListener() {
            @Override
            public void stateChanged(View view, int state) {
                ButtonManager.this.mSettingsManager.setValueByIndex(ButtonManager.this.mAppController.getModuleScope(), Keys.KEY_CAMERA_ID, state);
                int cameraId = ButtonManager.this.mSettingsManager.getInteger(ButtonManager.this.mAppController.getModuleScope(), Keys.KEY_CAMERA_ID).intValue();
                ButtonManager.this.disableButton(3);
                ButtonManager.this.mCameraMod = true;
                button.setEnabled(false);
                if (cb != null) {
                    cb.onStateChanged(cameraId);
                }
                ButtonManager.this.mAppController.getCameraAppUI().onChangeCamera();
            }
        });
    }

    private void initializeHdrPlusButton(MultiToggleImageButton button, final ButtonCallback cb, int resIdImages) {
        initializeHdrPlusButtonIcons(button, resIdImages);
        int index = this.mSettingsManager.getIndexOfCurrentValue(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_HDR_PLUS);
        if (index < 0) {
            index = 0;
        }
        button.setState(index, false);
        button.setOnStateChangeListener(new MultiToggleImageButton.OnStateChangeListener() {
            @Override
            public void stateChanged(View view, int state) {
                ButtonManager.this.mSettingsManager.setValueByIndex(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_HDR_PLUS, state);
                if (cb != null) {
                    cb.onStateChanged(state);
                }
            }
        });
    }

    private void initializeHdrPlusButtonIcons(MultiToggleImageButton button, int resIdImages) {
        if (resIdImages > 0) {
            button.overrideImageIds(resIdImages);
        }
        button.overrideContentDescriptions(R.array.hdr_plus_descriptions);
    }

    private void initializeHdrButton(MultiToggleImageButton button, final ButtonCallback cb, int resIdImages) {
        initializeHdrButtonIcons(button, resIdImages);
        int index = this.mSettingsManager.getIndexOfCurrentValue(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_HDR);
        if (index < 0) {
            index = 0;
        }
        button.setState(index, false);
        button.setOnStateChangeListener(new MultiToggleImageButton.OnStateChangeListener() {
            @Override
            public void stateChanged(View view, int state) {
                ButtonManager.this.mSettingsManager.setValueByIndex(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_HDR, state);
                if (cb != null) {
                    cb.onStateChanged(state);
                }
            }
        });
    }

    private void initializeHdrButtonIcons(MultiToggleImageButton button, int resIdImages) {
        if (resIdImages > 0) {
            button.overrideImageIds(resIdImages);
        }
        button.overrideContentDescriptions(R.array.hdr_descriptions);
    }

    private void initializeCountdownButton(MultiToggleImageButton button, final ButtonCallback cb, int resIdImages) {
        if (resIdImages > 0) {
            button.overrideImageIds(resIdImages);
        }
        int index = this.mSettingsManager.getIndexOfCurrentValue(SettingsManager.SCOPE_GLOBAL, Keys.KEY_COUNTDOWN_DURATION);
        if (index < 0) {
            index = 0;
        }
        button.setState(index, false);
        button.setOnStateChangeListener(new MultiToggleImageButton.OnStateChangeListener() {
            @Override
            public void stateChanged(View view, int state) {
                ButtonManager.this.mSettingsManager.setValueByIndex(SettingsManager.SCOPE_GLOBAL, Keys.KEY_COUNTDOWN_DURATION, state);
                if (cb != null) {
                    cb.onStateChanged(state);
                }
            }
        });
    }

    public void updateExposureButtons() {
        int compValue = this.mSettingsManager.getInteger(this.mAppController.getCameraScope(), Keys.KEY_EXPOSURE).intValue();
        if (this.mExposureCompensationStep != 0.0f) {
            int comp = Math.round(compValue * this.mExposureCompensationStep);
            this.mModeOptionsExposure.setSelectedOptionByTag(String.valueOf(comp));
        }
    }

    private void initializeGridLinesButton(MultiToggleImageButton button, final ButtonCallback cb, int resIdImages) {
        if (resIdImages > 0) {
            button.overrideImageIds(resIdImages);
        }
        button.overrideContentDescriptions(R.array.grid_lines_descriptions);
        button.setOnStateChangeListener(new MultiToggleImageButton.OnStateChangeListener() {
            @Override
            public void stateChanged(View view, int state) {
                ButtonManager.this.mSettingsManager.setValueByIndex(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_GRID_LINES, state);
                if (cb != null) {
                    cb.onStateChanged(state);
                }
            }
        });
        int index = this.mSettingsManager.getIndexOfCurrentValue(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_GRID_LINES);
        if (index < 0) {
            index = 0;
        }
        button.setState(index, true);
    }

    private void initializeAutoFocusButton(MultiToggleImageButton button, final ButtonCallback cb, int resIdImages) {
        if (resIdImages > 0) {
            button.overrideImageIds(resIdImages);
        }
        button.overrideContentDescriptions(R.array.auto_focus_descriptions);
        button.setOnStateChangeListener(new MultiToggleImageButton.OnStateChangeListener() {
            @Override
            public void stateChanged(View view, int state) {
                ButtonManager.this.mSettingsManager.setValueByIndex(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_AUTO_FOCUS, state);
                if (cb != null) {
                    cb.onStateChanged(state);
                }
            }
        });
        int index = this.mSettingsManager.getIndexOfCurrentValue(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_AUTO_FOCUS);
        if (index < 0) {
            index = 0;
        }
        button.setState(index, true);
    }

    public boolean isPanoEnabled() {
        return this.mModeOptions.getMainBar() == 1;
    }

    public void initializePanoOrientationButtons(final ButtonCallback cb) {
        int resIdImages = PhotoSphereHelper.getPanoramaOrientationOptionArrayId();
        int resIdDescriptions = PhotoSphereHelper.getPanoramaOrientationDescriptions();
        if (resIdImages > 0) {
            TypedArray imageIds = null;
            TypedArray descriptionIds = null;
            try {
                this.mModeOptions.setMainBar(1);
                imageIds = this.mAppController.getAndroidContext().getResources().obtainTypedArray(resIdImages);
                descriptionIds = this.mAppController.getAndroidContext().getResources().obtainTypedArray(resIdDescriptions);
                this.mModeOptionsPano.removeAllViews();
                boolean isHorizontal = this.mModeOptionsPano.getOrientation() == 0;
                int numImageIds = imageIds.length();
                for (int index = 0; index < numImageIds; index++) {
                    int i = isHorizontal ? index : (numImageIds - index) - 1;
                    int imageId = imageIds.getResourceId(i, 0);
                    if (imageId > 0) {
                        ImageButton imageButton = (ImageButton) LayoutInflater.from(this.mAppController.getAndroidContext()).inflate(R.layout.mode_options_imagebutton_template, (ViewGroup) this.mModeOptionsPano, false);
                        imageButton.setImageResource(imageId);
                        imageButton.setTag(String.valueOf(i));
                        this.mModeOptionsPano.addView(imageButton);
                        int descriptionId = descriptionIds.getResourceId(i, 0);
                        if (descriptionId > 0) {
                            imageButton.setContentDescription(this.mAppController.getAndroidContext().getString(descriptionId));
                        }
                    }
                }
                this.mModeOptionsPano.updateListeners();
                this.mModeOptionsPano.setOnOptionClickListener(new RadioOptions.OnOptionClickListener() {
                    @Override
                    public void onOptionClicked(View v) {
                        if (cb != null) {
                            int state = Integer.parseInt((String) v.getTag());
                            ButtonManager.this.mSettingsManager.setValueByIndex(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_PANO_ORIENTATION, state);
                            cb.onStateChanged(state);
                        }
                    }
                });
                updatePanoButtons();
            } finally {
                if (imageIds != null) {
                    imageIds.recycle();
                }
                if (descriptionIds != null) {
                    descriptionIds.recycle();
                }
            }
        }
    }

    private void updatePanoButtons() {
        int modeIndex = this.mSettingsManager.getIndexOfCurrentValue(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_PANO_ORIENTATION);
        this.mModeOptionsPano.setSelectedOptionByTag(String.valueOf(modeIndex));
    }
}
