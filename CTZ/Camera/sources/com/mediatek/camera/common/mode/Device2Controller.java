package com.mediatek.camera.common.mode;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import com.mediatek.camera.common.device.v2.Camera2Proxy;

public abstract class Device2Controller {
    protected final ModeHandler mModeHandler = new ModeHandler(Looper.myLooper());

    protected abstract void doCameraDisconnected(Camera2Proxy camera2Proxy);

    protected abstract void doCameraError(Camera2Proxy camera2Proxy, int i);

    protected abstract void doCameraOpened(Camera2Proxy camera2Proxy);

    private class ModeHandler extends Handler {
        public ModeHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 0:
                    Device2Controller.this.doCameraOpened((Camera2Proxy) message.obj);
                    break;
                case Camera2Proxy.TEMPLATE_PREVIEW:
                    Device2Controller.this.doCameraClosed((Camera2Proxy) message.obj);
                    break;
                case Camera2Proxy.TEMPLATE_STILL_CAPTURE:
                    Device2Controller.this.doCameraDisconnected((Camera2Proxy) message.obj);
                    break;
                case Camera2Proxy.TEMPLATE_RECORD:
                    Device2Controller.this.doCameraError((Camera2Proxy) message.obj, message.arg1);
                    break;
            }
        }
    }

    public class DeviceStateCallback extends Camera2Proxy.StateCallback {
        public DeviceStateCallback() {
        }

        @Override
        public void onOpened(Camera2Proxy camera2Proxy) {
            Device2Controller.this.mModeHandler.obtainMessage(0, camera2Proxy).sendToTarget();
        }

        @Override
        public void onClosed(Camera2Proxy camera2Proxy) {
            Device2Controller.this.mModeHandler.obtainMessage(1, camera2Proxy).sendToTarget();
        }

        @Override
        public void onDisconnected(Camera2Proxy camera2Proxy) {
            Device2Controller.this.mModeHandler.obtainMessage(2, camera2Proxy).sendToTarget();
        }

        @Override
        public void onError(Camera2Proxy camera2Proxy, int i) {
            Device2Controller.this.mModeHandler.obtainMessage(3, i, 0, camera2Proxy).sendToTarget();
        }
    }

    protected void doCameraClosed(Camera2Proxy camera2Proxy) {
        this.mModeHandler.removeMessages(0);
    }
}
