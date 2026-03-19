package com.mediatek.camera.portability;

import android.hardware.Camera;
import android.os.Build;
import android.os.Message;
import android.util.Log;
import com.mediatek.camera.common.device.v2.Camera2Proxy;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class CameraEx {
    private VendorDataCallback mVendorDataCallback;
    private static Method sCameraSetPropertyMethod = ReflectUtil.getMethod(Camera.class, "setProperty", String.class, String.class);
    private static Method sCameraGetPropertyMethod = ReflectUtil.getMethod(Camera.class, "getProperty", String.class, String.class);
    private static Method sSetUncompressedImageCallbackMethod = ReflectUtil.getMethod(Camera.class, "setUncompressedImageCallback", Camera.PictureCallback.class);

    public interface VendorDataCallback {
        void onDataCallback(int i, byte[] bArr, int i2, int i3);

        void onDataTaken(Message message);
    }

    public static Camera openLegacy(int i, int i2) {
        if (Build.VERSION.SDK_INT > 21 && Build.VERSION.SDK_INT < 28) {
            try {
                return Camera.openLegacy(i, i2);
            } catch (RuntimeException e) {
                Log.e("CamAp_CameraEx", "[openLegacy] exception:" + e);
                throw e;
            }
        }
        return Camera.open(i);
    }

    public void sendCommand(Camera camera, int i, int i2, int i3) {
        switch (i) {
            case 268435457:
                enableRaw16(camera, i2 == 1);
                break;
            case 268435465:
                camera.startAutoRama(i2);
                break;
            case 268435466:
                camera.stopAutoRama(i2);
                break;
            case 268435469:
                camera.cancelContinuousShot();
                break;
            case 268435470:
                camera.setContinuousShotSpeed(i2);
                break;
        }
    }

    public void setVendorDataCallback(Camera camera, int i, VendorDataCallback vendorDataCallback) {
        this.mVendorDataCallback = vendorDataCallback;
        switch (i) {
            case Camera2Proxy.TEMPLATE_PREVIEW:
                camera.setAutoRamaCallback(new AutoRamaCallbackImpl());
                camera.setAutoRamaMoveCallback(new AutoRamaMoveCallbackImpl());
                break;
            case Camera2Proxy.TEMPLATE_STILL_CAPTURE:
                camera.setAsdCallback(new AsdCallbackImpl());
                break;
            case Camera2Proxy.TEMPLATE_MANUAL:
                camera.setContinuousShotCallback(new ContinuousShotCallbackImpl());
                break;
            case 17:
                camera.setStereoCameraDataCallback(new StereoDatasCallbackImpl());
                break;
            case 20:
                camera.setStereoCameraWarningCallback(new StereoWarningCallbackImpl());
                break;
            case 22:
                setDngCallback(camera);
                break;
            case 23:
                if (Build.VERSION.SDK_INT > 22 && sSetUncompressedImageCallbackMethod != null) {
                    ReflectUtil.callMethodOnObject(camera, sSetUncompressedImageCallbackMethod, new UncompressedImageCallbackImpl());
                    break;
                }
                break;
            case 32:
                camera.setAFDataCallback(new AFDataCallbackImpl());
                break;
        }
    }

    private class ContinuousShotCallbackImpl implements Camera.ContinuousShotCallback {
        private ContinuousShotCallbackImpl() {
        }

        public void onConinuousShotDone(int i) {
            CameraEx.this.mVendorDataCallback.onDataCallback(6, null, i, 0);
        }
    }

    private void setDngCallback(Camera camera) {
        try {
            Class<?> cls = Class.forName("android.hardware.Camera$MetadataCallback");
            Object objNewProxyInstance = Proxy.newProxyInstance(cls.getClassLoader(), new Class[]{cls}, new InvocationHandler() {
                @Override
                public Object invoke(Object obj, Method method, Object[] objArr) throws Throwable {
                    if (method.getName().equals("onMetadataReceived")) {
                        Log.d("CamAp_CameraEx", "onMetadataReceived");
                        Message message = new Message();
                        message.arg1 = 22;
                        message.obj = CameraEx.this.new MessageInfo(objArr[0], objArr[1]);
                        CameraEx.this.mVendorDataCallback.onDataTaken(message);
                        return 1;
                    }
                    return -1;
                }
            });
            Method method = ReflectUtil.getMethod(Camera.class, "setRaw16Callback", cls, Camera.PictureCallback.class);
            Log.d("CamAp_CameraEx", "setDngCallback: " + method);
            ReflectUtil.callMethodOnObject(camera, method, objNewProxyInstance, new DngPictureCallbackImpl());
        } catch (Exception e) {
            Log.e("CamAp_CameraEx", "metadata not defined");
        }
    }

    private void enableRaw16(Camera camera, boolean z) {
        try {
            ReflectUtil.callMethodOnObject(camera, ReflectUtil.getMethod(Camera.class, "enableRaw16", Boolean.TYPE), Boolean.valueOf(z));
        } catch (Exception e) {
            Log.e("CamAp_CameraEx", "enableRaw16 not defined");
        }
    }

    private class DngPictureCallbackImpl implements Camera.PictureCallback {
        private DngPictureCallbackImpl() {
        }

        @Override
        public void onPictureTaken(byte[] bArr, Camera camera) {
            CameraEx.this.mVendorDataCallback.onDataCallback(19, bArr, 0, 0);
        }
    }

    private class AutoRamaCallbackImpl implements Camera.AutoRamaCallback {
        private AutoRamaCallbackImpl() {
        }

        public void onCapture(byte[] bArr) {
            CameraEx.this.mVendorDataCallback.onDataCallback(1, bArr, 0, -1);
        }
    }

    private class AutoRamaMoveCallbackImpl implements Camera.AutoRamaMoveCallback {
        private AutoRamaMoveCallbackImpl() {
        }

        public void onFrame(int i, int i2) {
            CameraEx.this.mVendorDataCallback.onDataCallback(1, null, i, i2);
        }
    }

    private class AFDataCallbackImpl implements Camera.AFDataCallback {
        private AFDataCallbackImpl() {
        }

        public void onAFData(byte[] bArr, Camera camera) {
            CameraEx.this.mVendorDataCallback.onDataCallback(32, bArr, -1, -1);
        }
    }

    private class AsdCallbackImpl implements Camera.AsdCallback {
        private AsdCallbackImpl() {
        }

        public void onDetected(int i) {
            CameraEx.this.mVendorDataCallback.onDataCallback(2, null, i, 0);
        }
    }

    private class UncompressedImageCallbackImpl implements Camera.PictureCallback {
        private UncompressedImageCallbackImpl() {
        }

        @Override
        public void onPictureTaken(byte[] bArr, Camera camera) {
            CameraEx.this.mVendorDataCallback.onDataCallback(23, bArr, 0, 0);
        }
    }

    private class StereoWarningCallbackImpl implements Camera.StereoCameraWarningCallback {
        private StereoWarningCallbackImpl() {
        }

        public void onWarning(int i) {
            CameraEx.this.mVendorDataCallback.onDataCallback(20, null, i, 0);
        }
    }

    private class StereoDatasCallbackImpl implements Camera.StereoCameraDataCallback {
        private StereoDatasCallbackImpl() {
        }

        public void onJpsCapture(byte[] bArr) {
            CameraEx.this.mVendorDataCallback.onDataCallback(17, bArr, 17, 0);
        }

        public void onMaskCapture(byte[] bArr) {
            CameraEx.this.mVendorDataCallback.onDataCallback(17, bArr, 18, 0);
        }

        public void onDepthMapCapture(byte[] bArr) {
            CameraEx.this.mVendorDataCallback.onDataCallback(17, bArr, 20, 0);
        }

        public void onClearImageCapture(byte[] bArr) {
            CameraEx.this.mVendorDataCallback.onDataCallback(17, bArr, 21, 0);
        }

        public void onLdcCapture(byte[] bArr) {
            CameraEx.this.mVendorDataCallback.onDataCallback(17, bArr, 22, 0);
        }

        public void onN3dCapture(byte[] bArr) {
            CameraEx.this.mVendorDataCallback.onDataCallback(17, bArr, 25, 0);
        }

        public void onDepthWrapperCapture(byte[] bArr) {
            CameraEx.this.mVendorDataCallback.onDataCallback(17, bArr, 32, 0);
        }
    }

    public class MessageInfo {
        public final Object mArg1;
        public final Object mArg2;

        MessageInfo(Object obj, Object obj2) {
            this.mArg1 = obj;
            this.mArg2 = obj2;
        }
    }
}
