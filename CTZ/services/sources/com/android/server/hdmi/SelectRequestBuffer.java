package com.android.server.hdmi;

import android.hardware.hdmi.IHdmiControlCallback;
import android.os.RemoteException;
import android.util.Slog;

public class SelectRequestBuffer {
    public static final SelectRequestBuffer EMPTY_BUFFER = new SelectRequestBuffer() {
        @Override
        public void process() {
        }
    };
    private static final String TAG = "SelectRequestBuffer";
    private SelectRequest mRequest;

    public static abstract class SelectRequest {
        protected final IHdmiControlCallback mCallback;
        protected final int mId;
        protected final HdmiControlService mService;

        public abstract void process();

        public SelectRequest(HdmiControlService hdmiControlService, int i, IHdmiControlCallback iHdmiControlCallback) {
            this.mService = hdmiControlService;
            this.mId = i;
            this.mCallback = iHdmiControlCallback;
        }

        protected HdmiCecLocalDeviceTv tv() {
            return this.mService.tv();
        }

        protected boolean isLocalDeviceReady() {
            if (tv() == null) {
                Slog.e(SelectRequestBuffer.TAG, "Local tv device not available");
                invokeCallback(2);
                return false;
            }
            return true;
        }

        private void invokeCallback(int i) {
            try {
                if (this.mCallback != null) {
                    this.mCallback.onComplete(i);
                }
            } catch (RemoteException e) {
                Slog.e(SelectRequestBuffer.TAG, "Invoking callback failed:" + e);
            }
        }
    }

    public static class DeviceSelectRequest extends SelectRequest {
        private DeviceSelectRequest(HdmiControlService hdmiControlService, int i, IHdmiControlCallback iHdmiControlCallback) {
            super(hdmiControlService, i, iHdmiControlCallback);
        }

        @Override
        public void process() {
            if (isLocalDeviceReady()) {
                Slog.v(SelectRequestBuffer.TAG, "calling delayed deviceSelect id:" + this.mId);
                tv().deviceSelect(this.mId, this.mCallback);
            }
        }
    }

    public static class PortSelectRequest extends SelectRequest {
        private PortSelectRequest(HdmiControlService hdmiControlService, int i, IHdmiControlCallback iHdmiControlCallback) {
            super(hdmiControlService, i, iHdmiControlCallback);
        }

        @Override
        public void process() {
            if (isLocalDeviceReady()) {
                Slog.v(SelectRequestBuffer.TAG, "calling delayed portSelect id:" + this.mId);
                tv().doManualPortSwitching(this.mId, this.mCallback);
            }
        }
    }

    public static DeviceSelectRequest newDeviceSelect(HdmiControlService hdmiControlService, int i, IHdmiControlCallback iHdmiControlCallback) {
        return new DeviceSelectRequest(hdmiControlService, i, iHdmiControlCallback);
    }

    public static PortSelectRequest newPortSelect(HdmiControlService hdmiControlService, int i, IHdmiControlCallback iHdmiControlCallback) {
        return new PortSelectRequest(hdmiControlService, i, iHdmiControlCallback);
    }

    public void set(SelectRequest selectRequest) {
        this.mRequest = selectRequest;
    }

    public void process() {
        if (this.mRequest != null) {
            this.mRequest.process();
            clear();
        }
    }

    public void clear() {
        this.mRequest = null;
    }
}
