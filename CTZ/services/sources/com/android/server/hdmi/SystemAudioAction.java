package com.android.server.hdmi;

import android.hardware.hdmi.IHdmiControlCallback;
import android.os.RemoteException;
import android.util.Slog;
import com.android.server.hdmi.HdmiControlService;
import java.util.List;

abstract class SystemAudioAction extends HdmiCecFeatureAction {
    private static final int MAX_SEND_RETRY_COUNT = 2;
    private static final int OFF_TIMEOUT_MS = 2000;
    private static final int ON_TIMEOUT_MS = 5000;
    private static final int STATE_CHECK_ROUTING_IN_PRGRESS = 1;
    private static final int STATE_WAIT_FOR_SET_SYSTEM_AUDIO_MODE = 2;
    private static final String TAG = "SystemAudioAction";
    protected final int mAvrLogicalAddress;
    private final IHdmiControlCallback mCallback;
    private int mSendRetryCount;
    protected boolean mTargetAudioStatus;

    SystemAudioAction(HdmiCecLocalDevice hdmiCecLocalDevice, int i, boolean z, IHdmiControlCallback iHdmiControlCallback) {
        super(hdmiCecLocalDevice);
        this.mSendRetryCount = 0;
        HdmiUtils.verifyAddressType(i, 5);
        this.mAvrLogicalAddress = i;
        this.mTargetAudioStatus = z;
        this.mCallback = iHdmiControlCallback;
    }

    protected void sendSystemAudioModeRequest() {
        List actions = getActions(RoutingControlAction.class);
        if (!actions.isEmpty()) {
            this.mState = 1;
            ((RoutingControlAction) actions.get(0)).addOnFinishedCallback(this, new Runnable() {
                @Override
                public void run() {
                    SystemAudioAction.this.sendSystemAudioModeRequestInternal();
                }
            });
        } else {
            sendSystemAudioModeRequestInternal();
        }
    }

    private void sendSystemAudioModeRequestInternal() {
        sendCommand(HdmiCecMessageBuilder.buildSystemAudioModeRequest(getSourceAddress(), this.mAvrLogicalAddress, getSystemAudioModeRequestParam(), this.mTargetAudioStatus), new HdmiControlService.SendMessageCallback() {
            @Override
            public void onSendCompleted(int i) {
                if (i != 0) {
                    HdmiLogger.debug("Failed to send <System Audio Mode Request>:" + i, new Object[0]);
                    SystemAudioAction.this.setSystemAudioMode(false);
                    SystemAudioAction.this.finishWithCallback(7);
                }
            }
        });
        this.mState = 2;
        addTimer(this.mState, this.mTargetAudioStatus ? ON_TIMEOUT_MS : 2000);
    }

    private int getSystemAudioModeRequestParam() {
        if (tv().getActiveSource().isValid()) {
            return tv().getActiveSource().physicalAddress;
        }
        int activePath = tv().getActivePath();
        if (activePath != 65535) {
            return activePath;
        }
        return 0;
    }

    private void handleSendSystemAudioModeRequestTimeout() {
        if (this.mTargetAudioStatus) {
            int i = this.mSendRetryCount;
            this.mSendRetryCount = i + 1;
            if (i < 2) {
                sendSystemAudioModeRequest();
                return;
            }
        }
        HdmiLogger.debug("[T]:wait for <Set System Audio Mode>.", new Object[0]);
        setSystemAudioMode(false);
        finishWithCallback(1);
    }

    protected void setSystemAudioMode(boolean z) {
        tv().setSystemAudioMode(z);
    }

    @Override
    final boolean processCommand(HdmiCecMessage hdmiCecMessage) {
        if (hdmiCecMessage.getSource() != this.mAvrLogicalAddress || this.mState != 2) {
            return false;
        }
        if (hdmiCecMessage.getOpcode() == 0 && (hdmiCecMessage.getParams()[0] & 255) == 112) {
            HdmiLogger.debug("Failed to start system audio mode request.", new Object[0]);
            setSystemAudioMode(false);
            finishWithCallback(5);
            return true;
        }
        if (hdmiCecMessage.getOpcode() != 114 || !HdmiUtils.checkCommandSource(hdmiCecMessage, this.mAvrLogicalAddress, TAG)) {
            return false;
        }
        boolean commandParamSystemAudioStatus = HdmiUtils.parseCommandParamSystemAudioStatus(hdmiCecMessage);
        if (commandParamSystemAudioStatus == this.mTargetAudioStatus) {
            setSystemAudioMode(commandParamSystemAudioStatus);
            startAudioStatusAction();
            return true;
        }
        HdmiLogger.debug("Unexpected system audio mode request:" + commandParamSystemAudioStatus, new Object[0]);
        finishWithCallback(5);
        return false;
    }

    protected void startAudioStatusAction() {
        addAndStartAction(new SystemAudioStatusAction(tv(), this.mAvrLogicalAddress, this.mCallback));
        finish();
    }

    protected void removeSystemAudioActionInProgress() {
        removeActionExcept(SystemAudioActionFromTv.class, this);
        removeActionExcept(SystemAudioActionFromAvr.class, this);
    }

    @Override
    final void handleTimerEvent(int i) {
        if (this.mState == i && this.mState == 2) {
            handleSendSystemAudioModeRequestTimeout();
        }
    }

    protected void finishWithCallback(int i) {
        if (this.mCallback != null) {
            try {
                this.mCallback.onComplete(i);
            } catch (RemoteException e) {
                Slog.e(TAG, "Failed to invoke callback.", e);
            }
        }
        finish();
    }
}
