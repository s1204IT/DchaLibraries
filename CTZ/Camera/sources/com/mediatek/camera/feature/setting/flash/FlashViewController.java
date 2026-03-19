package com.mediatek.camera.feature.setting.flash;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import com.mediatek.camera.R;
import com.mediatek.camera.common.IAppUiListener;
import com.mediatek.camera.common.app.IApp;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.device.v2.Camera2Proxy;
import com.mediatek.camera.common.utils.CameraUtil;
import com.mediatek.camera.common.widget.RotateImageView;

public class FlashViewController {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(FlashViewController.class.getSimpleName());
    private final IApp mApp;
    private final Flash mFlash;
    private ImageView mFlashAutoIcon;
    private View mFlashChoiceView;
    private ImageView mFlashEntryView;
    private ImageView mFlashIndicatorView;
    private ImageView mFlashOffIcon;
    private ImageView mFlashOnIcon;
    private MainHandler mMainHandler;
    private View mOptionLayout;
    private final View.OnClickListener mFlashEntryListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (FlashViewController.this.mFlash.getEntryValues().size() > 1) {
                if (FlashViewController.this.mFlash.getEntryValues().size() > 2) {
                    FlashViewController.this.initializeFlashChoiceView();
                    FlashViewController.this.updateChoiceView();
                    FlashViewController.this.mApp.getAppUi().showQuickSwitcherOption(FlashViewController.this.mOptionLayout);
                } else {
                    String str = FlashViewController.this.mFlash.getEntryValues().get(0);
                    if (str.equals(FlashViewController.this.mFlash.getValue())) {
                        str = FlashViewController.this.mFlash.getEntryValues().get(1);
                    }
                    FlashViewController.this.updateFlashEntryView(str);
                    FlashViewController.this.mFlash.onFlashValueChanged(str);
                }
            }
        }
    };
    private View.OnClickListener mFlashChoiceViewListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            String str;
            if (FlashViewController.this.mFlashAutoIcon != view) {
                if (FlashViewController.this.mFlashOnIcon == view) {
                    str = "on";
                } else {
                    str = "off";
                }
            } else {
                str = "auto";
            }
            FlashViewController.this.mApp.getAppUi().hideQuickSwitcherOption();
            FlashViewController.this.updateFlashEntryView(str);
            FlashViewController.this.mFlash.onFlashValueChanged(str);
        }
    };
    private final IAppUiListener.OnShutterButtonListener mShutterListener = new IAppUiListener.OnShutterButtonListener() {
        @Override
        public boolean onShutterButtonFocus(boolean z) {
            if (z) {
                FlashViewController.this.hideFlashChoiceView();
                return false;
            }
            return false;
        }

        @Override
        public boolean onShutterButtonClick() {
            return false;
        }

        @Override
        public boolean onShutterButtonLongPressed() {
            return false;
        }
    };

    public FlashViewController(Flash flash, IApp iApp) {
        this.mFlash = flash;
        this.mApp = iApp;
        this.mMainHandler = new MainHandler(iApp.getActivity().getMainLooper());
        this.mMainHandler.sendEmptyMessage(0);
    }

    public void addQuickSwitchIcon() {
        this.mMainHandler.sendEmptyMessage(1);
    }

    public void removeQuickSwitchIcon() {
        this.mMainHandler.sendEmptyMessage(2);
    }

    public void showQuickSwitchIcon(boolean z) {
        this.mMainHandler.obtainMessage(4, Boolean.valueOf(z)).sendToTarget();
    }

    public void hideFlashChoiceView() {
        this.mMainHandler.sendEmptyMessage(3);
    }

    protected IApp.KeyEventListener getKeyEventListener() {
        return new IApp.KeyEventListener() {
            @Override
            public boolean onKeyDown(int i, KeyEvent keyEvent) {
                if ((i != 34 && i != 35) || !CameraUtil.isSpecialKeyCodeEnabled()) {
                    return false;
                }
                return true;
            }

            @Override
            public boolean onKeyUp(int i, KeyEvent keyEvent) {
                if (!CameraUtil.isSpecialKeyCodeEnabled()) {
                    return false;
                }
                if (i != 34 && i != 35) {
                    return false;
                }
                if (FlashViewController.this.mFlashEntryView == null) {
                    LogHelper.e(FlashViewController.TAG, "[onKeyUp] mFlashEntryView is null");
                    return false;
                }
                if (i == 34) {
                    LogHelper.i(FlashViewController.TAG, "[onKeyUp] update flash on");
                    FlashViewController.this.updateFlashEntryView("on");
                    FlashViewController.this.mFlash.onFlashValueChanged("on");
                    return true;
                }
                if (i == 35) {
                    LogHelper.i(FlashViewController.TAG, "[onKeyUp] update flash off");
                    FlashViewController.this.updateFlashEntryView("off");
                    FlashViewController.this.mFlash.onFlashValueChanged("off");
                    return true;
                }
                return true;
            }
        };
    }

    private class MainHandler extends Handler {
        public MainHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            LogHelper.d(FlashViewController.TAG, "view handleMessage: " + message.what);
            switch (message.what) {
                case 0:
                    FlashViewController.this.mFlashEntryView = FlashViewController.this.initFlashEntryView();
                    break;
                case Camera2Proxy.TEMPLATE_PREVIEW:
                    FlashViewController.this.mApp.getAppUi().addToQuickSwitcher(FlashViewController.this.mFlashEntryView, 30);
                    FlashViewController.this.updateFlashEntryView(FlashViewController.this.mFlash.getValue());
                    FlashViewController.this.mApp.getAppUi().registerOnShutterButtonListener(FlashViewController.this.mShutterListener, 70);
                    break;
                case Camera2Proxy.TEMPLATE_STILL_CAPTURE:
                    FlashViewController.this.mApp.getAppUi().removeFromQuickSwitcher(FlashViewController.this.mFlashEntryView);
                    FlashViewController.this.mApp.getAppUi().unregisterOnShutterButtonListener(FlashViewController.this.mShutterListener);
                    break;
                case Camera2Proxy.TEMPLATE_RECORD:
                    if (FlashViewController.this.mFlashChoiceView != null && FlashViewController.this.mFlashChoiceView.isShown()) {
                        FlashViewController.this.mApp.getAppUi().hideQuickSwitcherOption();
                        FlashViewController.this.updateFlashEntryView(FlashViewController.this.mFlash.getValue());
                        break;
                    }
                    break;
                case Camera2Proxy.TEMPLATE_VIDEO_SNAPSHOT:
                    if (((Boolean) message.obj).booleanValue()) {
                        FlashViewController.this.mFlashEntryView.setVisibility(0);
                        FlashViewController.this.updateFlashEntryView(FlashViewController.this.mFlash.getValue());
                    } else {
                        FlashViewController.this.mFlashEntryView.setVisibility(8);
                    }
                    break;
            }
        }
    }

    private void updateFlashEntryView(String str) {
        LogHelper.d(TAG, "[updateFlashView] currentValue = " + this.mFlash.getValue());
        if ("on".equals(str)) {
            this.mFlashEntryView.setImageResource(R.drawable.ic_flash_status_on);
            this.mFlashEntryView.setContentDescription(this.mApp.getActivity().getResources().getString(R.string.accessibility_flash_on));
        } else if ("auto".equals(str)) {
            this.mFlashEntryView.setImageResource(R.drawable.ic_flash_status_auto);
            this.mFlashEntryView.setContentDescription(this.mApp.getActivity().getResources().getString(R.string.accessibility_flash_auto));
        } else {
            this.mFlashEntryView.setImageResource(R.drawable.ic_flash_status_off);
            this.mFlashEntryView.setContentDescription(this.mApp.getActivity().getResources().getString(R.string.accessibility_flash_off));
        }
    }

    private ImageView initFlashEntryView() {
        Activity activity = this.mApp.getActivity();
        RotateImageView rotateImageView = (RotateImageView) activity.getLayoutInflater().inflate(R.layout.flash_icon, (ViewGroup) null);
        rotateImageView.setOnClickListener(this.mFlashEntryListener);
        this.mFlashIndicatorView = (RotateImageView) activity.getLayoutInflater().inflate(R.layout.flash_indicator, (ViewGroup) null);
        return rotateImageView;
    }

    private void updateChoiceView() {
        if ("on".equals(this.mFlash.getValue())) {
            this.mFlashOnIcon.setImageResource(R.drawable.ic_flash_status_on_selected);
            this.mFlashOffIcon.setImageResource(R.drawable.ic_flash_status_off);
            this.mFlashAutoIcon.setImageResource(R.drawable.ic_flash_status_auto);
        } else if ("off".equals(this.mFlash.getValue())) {
            this.mFlashOnIcon.setImageResource(R.drawable.ic_flash_status_on);
            this.mFlashOffIcon.setImageResource(R.drawable.ic_flash_status_off_selected);
            this.mFlashAutoIcon.setImageResource(R.drawable.ic_flash_status_auto);
        } else {
            this.mFlashOnIcon.setImageResource(R.drawable.ic_flash_status_on);
            this.mFlashOffIcon.setImageResource(R.drawable.ic_flash_status_off);
            this.mFlashAutoIcon.setImageResource(R.drawable.ic_flash_status_auto_selected);
        }
    }

    private void initializeFlashChoiceView() {
        if (this.mFlashChoiceView == null || this.mOptionLayout == null) {
            this.mOptionLayout = this.mApp.getActivity().getLayoutInflater().inflate(R.layout.flash_option, this.mApp.getAppUi().getModeRootView(), false);
            this.mFlashChoiceView = this.mOptionLayout.findViewById(R.id.flash_choice);
            this.mFlashOnIcon = (ImageView) this.mOptionLayout.findViewById(R.id.flash_on);
            this.mFlashOffIcon = (ImageView) this.mOptionLayout.findViewById(R.id.flash_off);
            this.mFlashAutoIcon = (ImageView) this.mOptionLayout.findViewById(R.id.flash_auto);
            this.mFlashOffIcon.setOnClickListener(this.mFlashChoiceViewListener);
            this.mFlashOnIcon.setOnClickListener(this.mFlashChoiceViewListener);
            this.mFlashAutoIcon.setOnClickListener(this.mFlashChoiceViewListener);
        }
    }
}
