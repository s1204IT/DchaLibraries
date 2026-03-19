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

        public SelectRequest(HdmiControlService service, int id, IHdmiControlCallback callback) {
            this.mService = service;
            this.mId = id;
            this.mCallback = callback;
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

        private void invokeCallback(int reason) {
            try {
                if (this.mCallback == null) {
                    return;
                }
                this.mCallback.onComplete(reason);
            } catch (RemoteException e) {
                Slog.e(SelectRequestBuffer.TAG, "Invoking callback failed:" + e);
            }
        }
    }

    public static class DeviceSelectRequest extends SelectRequest {
        DeviceSelectRequest(HdmiControlService srv, int id, IHdmiControlCallback callback, DeviceSelectRequest deviceSelectRequest) {
            this(srv, id, callback);
        }

        private DeviceSelectRequest(HdmiControlService srv, int id, IHdmiControlCallback callback) {
            super(srv, id, callback);
        }

        @Override
        public void process() {
            if (!isLocalDeviceReady()) {
                return;
            }
            Slog.v(SelectRequestBuffer.TAG, "calling delayed deviceSelect id:" + this.mId);
            tv().deviceSelect(this.mId, this.mCallback);
        }
    }

    public static class PortSelectRequest extends SelectRequest {
        PortSelectRequest(HdmiControlService srv, int id, IHdmiControlCallback callback, PortSelectRequest portSelectRequest) {
            this(srv, id, callback);
        }

        private PortSelectRequest(HdmiControlService srv, int id, IHdmiControlCallback callback) {
            super(srv, id, callback);
        }

        @Override
        public void process() {
            if (!isLocalDeviceReady()) {
                return;
            }
            Slog.v(SelectRequestBuffer.TAG, "calling delayed portSelect id:" + this.mId);
            tv().doManualPortSwitching(this.mId, this.mCallback);
        }
    }

    public static DeviceSelectRequest newDeviceSelect(HdmiControlService srv, int id, IHdmiControlCallback callback) {
        return new DeviceSelectRequest(srv, id, callback, null);
    }

    public static PortSelectRequest newPortSelect(HdmiControlService srv, int id, IHdmiControlCallback callback) {
        return new PortSelectRequest(srv, id, callback, null);
    }

    public void set(SelectRequest request) {
        this.mRequest = request;
    }

    public void process() {
        if (this.mRequest == null) {
            return;
        }
        this.mRequest.process();
        clear();
    }

    public void clear() {
        this.mRequest = null;
    }
}
