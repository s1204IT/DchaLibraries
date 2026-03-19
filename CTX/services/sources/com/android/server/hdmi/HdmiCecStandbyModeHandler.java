package com.android.server.hdmi;

import android.net.util.NetworkConstants;
import android.util.SparseArray;

public final class HdmiCecStandbyModeHandler {
    private final CecMessageHandler mAutoOnHandler;
    private final CecMessageHandler mBypasser;
    private final CecMessageHandler mBystander;
    private final HdmiControlService mService;
    private final HdmiCecLocalDeviceTv mTv;
    private final UserControlProcessedHandler mUserControlProcessedHandler;
    private final SparseArray<CecMessageHandler> mCecMessageHandlers = new SparseArray<>();
    private final CecMessageHandler mDefaultHandler = new Aborter(0);
    private final CecMessageHandler mAborterIncorrectMode = new Aborter(1);
    private final CecMessageHandler mAborterRefused = new Aborter(4);

    private interface CecMessageHandler {
        boolean handle(HdmiCecMessage hdmiCecMessage);
    }

    private static final class Bystander implements CecMessageHandler {
        private Bystander() {
        }

        @Override
        public boolean handle(HdmiCecMessage hdmiCecMessage) {
            return true;
        }
    }

    private static final class Bypasser implements CecMessageHandler {
        private Bypasser() {
        }

        @Override
        public boolean handle(HdmiCecMessage hdmiCecMessage) {
            return false;
        }
    }

    private final class Aborter implements CecMessageHandler {
        private final int mReason;

        public Aborter(int i) {
            this.mReason = i;
        }

        @Override
        public boolean handle(HdmiCecMessage hdmiCecMessage) {
            HdmiCecStandbyModeHandler.this.mService.maySendFeatureAbortCommand(hdmiCecMessage, this.mReason);
            return true;
        }
    }

    private final class AutoOnHandler implements CecMessageHandler {
        private AutoOnHandler() {
        }

        @Override
        public boolean handle(HdmiCecMessage hdmiCecMessage) {
            if (!HdmiCecStandbyModeHandler.this.mTv.getAutoWakeup()) {
                HdmiCecStandbyModeHandler.this.mAborterRefused.handle(hdmiCecMessage);
                return true;
            }
            return false;
        }
    }

    private final class UserControlProcessedHandler implements CecMessageHandler {
        private UserControlProcessedHandler() {
        }

        @Override
        public boolean handle(HdmiCecMessage hdmiCecMessage) {
            if (HdmiCecLocalDevice.isPowerOnOrToggleCommand(hdmiCecMessage)) {
                return false;
            }
            if (!HdmiCecLocalDevice.isPowerOffOrToggleCommand(hdmiCecMessage)) {
                return HdmiCecStandbyModeHandler.this.mAborterIncorrectMode.handle(hdmiCecMessage);
            }
            return true;
        }
    }

    public HdmiCecStandbyModeHandler(HdmiControlService hdmiControlService, HdmiCecLocalDeviceTv hdmiCecLocalDeviceTv) {
        this.mAutoOnHandler = new AutoOnHandler();
        this.mBypasser = new Bypasser();
        this.mBystander = new Bystander();
        this.mUserControlProcessedHandler = new UserControlProcessedHandler();
        this.mService = hdmiControlService;
        this.mTv = hdmiCecLocalDeviceTv;
        addHandler(4, this.mAutoOnHandler);
        addHandler(13, this.mAutoOnHandler);
        addHandler(130, this.mBystander);
        addHandler(NetworkConstants.ICMPV6_ROUTER_SOLICITATION, this.mBystander);
        addHandler(128, this.mBystander);
        addHandler(NetworkConstants.ICMPV6_ECHO_REPLY_TYPE, this.mBystander);
        addHandler(NetworkConstants.ICMPV6_ROUTER_ADVERTISEMENT, this.mBystander);
        addHandler(54, this.mBystander);
        addHandler(50, this.mBystander);
        addHandler(NetworkConstants.ICMPV6_NEIGHBOR_SOLICITATION, this.mBystander);
        addHandler(69, this.mBystander);
        addHandler(144, this.mBystander);
        addHandler(0, this.mBystander);
        addHandler(157, this.mBystander);
        addHandler(126, this.mBystander);
        addHandler(122, this.mBystander);
        addHandler(10, this.mBystander);
        addHandler(15, this.mAborterIncorrectMode);
        addHandler(192, this.mAborterIncorrectMode);
        addHandler(197, this.mAborterIncorrectMode);
        addHandler(131, this.mBypasser);
        addHandler(HdmiCecKeycode.UI_BROADCAST_DIGITAL_COMMNICATIONS_SATELLITE_2, this.mBypasser);
        addHandler(132, this.mBypasser);
        addHandler(140, this.mBypasser);
        addHandler(70, this.mBypasser);
        addHandler(71, this.mBypasser);
        addHandler(68, this.mUserControlProcessedHandler);
        addHandler(143, this.mBypasser);
        addHandler(255, this.mBypasser);
        addHandler(159, this.mBypasser);
        addHandler(160, this.mAborterIncorrectMode);
        addHandler(114, this.mAborterIncorrectMode);
    }

    private void addHandler(int i, CecMessageHandler cecMessageHandler) {
        this.mCecMessageHandlers.put(i, cecMessageHandler);
    }

    boolean handleCommand(HdmiCecMessage hdmiCecMessage) {
        CecMessageHandler cecMessageHandler = this.mCecMessageHandlers.get(hdmiCecMessage.getOpcode());
        if (cecMessageHandler != null) {
            return cecMessageHandler.handle(hdmiCecMessage);
        }
        return this.mDefaultHandler.handle(hdmiCecMessage);
    }
}
