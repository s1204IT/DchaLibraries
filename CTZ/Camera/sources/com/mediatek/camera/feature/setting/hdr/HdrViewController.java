package com.mediatek.camera.feature.setting.hdr;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import com.mediatek.camera.R;
import com.mediatek.camera.common.IAppUiListener;
import com.mediatek.camera.common.app.IApp;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.device.v2.Camera2Proxy;
import com.mediatek.camera.common.widget.RotateImageView;

public class HdrViewController {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(HdrViewController.class.getSimpleName());
    private final IApp mApp;
    private View mChoiceViewLayout;
    private final Hdr mHdr;
    private ImageView mHdrAutoIcon;
    private View mHdrChoiceView;
    private ImageView mHdrEntryView;
    private ImageView mHdrIndicatorView;
    private ImageView mHdrOffIcon;
    private ImageView mHdrOnIcon;
    private MainHandler mMainHandler;
    private final View.OnClickListener mHdrEntryListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (HdrViewController.this.mHdr.getEntryValues().size() > 1) {
                if (HdrViewController.this.mHdr.getEntryValues().size() > 2) {
                    HdrViewController.this.initializeHdrChoiceView();
                    HdrViewController.this.updateChoiceView();
                    HdrViewController.this.mApp.getAppUi().showQuickSwitcherOption(HdrViewController.this.mChoiceViewLayout);
                } else {
                    String str = HdrViewController.this.mHdr.getEntryValues().get(0);
                    if (str.equals(HdrViewController.this.mHdr.getValue())) {
                        str = HdrViewController.this.mHdr.getEntryValues().get(1);
                    }
                    HdrViewController.this.updateHdrViewState(str);
                    HdrViewController.this.mHdr.onHdrValueChanged(str);
                }
            }
        }
    };
    private final View.OnClickListener mHdrChoiceViewListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            String str;
            if (HdrViewController.this.mHdrAutoIcon != view) {
                if (HdrViewController.this.mHdrOnIcon == view) {
                    str = "on";
                } else {
                    str = "off";
                }
            } else {
                str = "auto";
            }
            HdrViewController.this.mApp.getAppUi().hideQuickSwitcherOption();
            HdrViewController.this.updateHdrViewState(str);
            HdrViewController.this.mHdr.onHdrValueChanged(str);
        }
    };
    private final IAppUiListener.OnShutterButtonListener mShutterListener = new IAppUiListener.OnShutterButtonListener() {
        @Override
        public boolean onShutterButtonFocus(boolean z) {
            if (z) {
                HdrViewController.this.onChoiceViewClosed();
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

    public HdrViewController(IApp iApp, Hdr hdr) {
        this.mApp = iApp;
        this.mHdr = hdr;
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
        this.mMainHandler.obtainMessage(5, Boolean.valueOf(z)).sendToTarget();
    }

    public void closeHdrChoiceView() {
        this.mMainHandler.sendEmptyMessage(4);
    }

    public void showHdrIndicator(boolean z) {
        this.mMainHandler.obtainMessage(3, Boolean.valueOf(z)).sendToTarget();
    }

    private class MainHandler extends Handler {
        public MainHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 0:
                    HdrViewController.this.mHdrEntryView = HdrViewController.this.initHdrEntryView();
                    break;
                case Camera2Proxy.TEMPLATE_PREVIEW:
                    HdrViewController.this.mApp.getAppUi().addToQuickSwitcher(HdrViewController.this.mHdrEntryView, 10);
                    HdrViewController.this.updateHdrViewState(HdrViewController.this.mHdr.getValue());
                    HdrViewController.this.mApp.getAppUi().registerOnShutterButtonListener(HdrViewController.this.mShutterListener, 60);
                    break;
                case Camera2Proxy.TEMPLATE_STILL_CAPTURE:
                    HdrViewController.this.mApp.getAppUi().removeFromQuickSwitcher(HdrViewController.this.mHdrEntryView);
                    HdrViewController.this.updateHdrIndicator(false);
                    HdrViewController.this.mApp.getAppUi().unregisterOnShutterButtonListener(HdrViewController.this.mShutterListener);
                    break;
                case Camera2Proxy.TEMPLATE_RECORD:
                    HdrViewController.this.updateHdrIndicator(((Boolean) message.obj).booleanValue());
                    break;
                case Camera2Proxy.TEMPLATE_VIDEO_SNAPSHOT:
                    HdrViewController.this.onChoiceViewClosed();
                    break;
                case Camera2Proxy.TEMPLATE_ZERO_SHUTTER_LAG:
                    if (((Boolean) message.obj).booleanValue()) {
                        HdrViewController.this.mHdrEntryView.setVisibility(0);
                        HdrViewController.this.updateHdrViewState(HdrViewController.this.mHdr.getValue());
                    } else {
                        HdrViewController.this.mHdrEntryView.setVisibility(8);
                        HdrViewController.this.updateHdrIndicator(false);
                    }
                    break;
            }
        }
    }

    private ImageView initHdrEntryView() {
        Activity activity = this.mApp.getActivity();
        RotateImageView rotateImageView = (RotateImageView) activity.getLayoutInflater().inflate(R.layout.hdr_icon, (ViewGroup) null);
        rotateImageView.setOnClickListener(this.mHdrEntryListener);
        this.mHdrIndicatorView = (RotateImageView) activity.getLayoutInflater().inflate(R.layout.hdr_indicator, (ViewGroup) null);
        return rotateImageView;
    }

    private void onChoiceViewClosed() {
        if (this.mHdrChoiceView != null && this.mHdrChoiceView.isShown()) {
            this.mApp.getAppUi().hideQuickSwitcherOption();
            updateHdrViewState(this.mHdr.getValue());
        }
    }

    private void initializeHdrChoiceView() {
        if (this.mHdrChoiceView == null || this.mChoiceViewLayout == null) {
            this.mChoiceViewLayout = this.mApp.getActivity().getLayoutInflater().inflate(R.layout.hdr_option, this.mApp.getAppUi().getModeRootView(), false);
            this.mHdrChoiceView = this.mChoiceViewLayout.findViewById(R.id.hdr_choice);
            this.mHdrOnIcon = (ImageView) this.mChoiceViewLayout.findViewById(R.id.hdr_on);
            this.mHdrOffIcon = (ImageView) this.mChoiceViewLayout.findViewById(R.id.hdr_off);
            this.mHdrAutoIcon = (ImageView) this.mChoiceViewLayout.findViewById(R.id.hdr_auto);
            this.mHdrOffIcon.setOnClickListener(this.mHdrChoiceViewListener);
            this.mHdrOnIcon.setOnClickListener(this.mHdrChoiceViewListener);
            this.mHdrAutoIcon.setOnClickListener(this.mHdrChoiceViewListener);
        }
    }

    private void updateChoiceView() {
        if ("on".equals(this.mHdr.getValue())) {
            this.mHdrOnIcon.setImageResource(R.drawable.ic_hdr_on_selected);
            this.mHdrOffIcon.setImageResource(R.drawable.ic_hdr_off);
            this.mHdrAutoIcon.setImageResource(R.drawable.ic_hdr_auto);
        } else if ("off".equals(this.mHdr.getValue())) {
            this.mHdrOnIcon.setImageResource(R.drawable.ic_hdr_on);
            this.mHdrOffIcon.setImageResource(R.drawable.ic_hdr_off_selected);
            this.mHdrAutoIcon.setImageResource(R.drawable.ic_hdr_auto);
        } else {
            this.mHdrOnIcon.setImageResource(R.drawable.ic_hdr_on);
            this.mHdrOffIcon.setImageResource(R.drawable.ic_hdr_off);
            this.mHdrAutoIcon.setImageResource(R.drawable.ic_hdr_auto_selected);
        }
    }

    private void updateHdrViewState(String str) {
        updateHdrEntryView(str);
        updateHdrIndicator("on".equals(str));
    }

    private void updateHdrEntryView(String str) {
        LogHelper.d(TAG, "updateHdrEntryView, value: " + str);
        if ("on".equals(str)) {
            this.mHdrEntryView.setImageResource(R.drawable.ic_hdr_on);
            this.mHdrEntryView.setContentDescription(this.mApp.getActivity().getResources().getString(R.string.accessibility_hdr_on));
        } else if ("auto".equals(str)) {
            this.mHdrEntryView.setImageResource(R.drawable.ic_hdr_auto);
            this.mHdrEntryView.setContentDescription(this.mApp.getActivity().getResources().getString(R.string.accessibility_hdr_auto));
        } else {
            this.mHdrEntryView.setImageResource(R.drawable.ic_hdr_off);
            this.mHdrEntryView.setContentDescription(this.mApp.getActivity().getResources().getString(R.string.accessibility_hdr_off));
        }
    }

    private void updateHdrIndicator(boolean z) {
        if (z) {
            this.mApp.getAppUi().addToIndicatorView(this.mHdrIndicatorView, 10);
        } else {
            this.mApp.getAppUi().removeFromIndicatorView(this.mHdrIndicatorView);
        }
    }
}
