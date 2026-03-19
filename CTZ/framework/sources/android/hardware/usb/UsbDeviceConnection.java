package android.hardware.usb;

import android.annotation.SuppressLint;
import android.annotation.SystemApi;
import android.content.Context;
import android.os.ParcelFileDescriptor;
import com.android.internal.location.GpsNetInitiatedHandler;
import com.android.internal.util.Preconditions;
import dalvik.system.CloseGuard;
import java.io.FileDescriptor;
import java.util.concurrent.TimeoutException;

public class UsbDeviceConnection {
    private static final String TAG = "UsbDeviceConnection";
    private final CloseGuard mCloseGuard = CloseGuard.get();
    private Context mContext;
    private final UsbDevice mDevice;
    private long mNativeContext;

    private native int native_bulk_request(int i, byte[] bArr, int i2, int i3, int i4);

    private native boolean native_claim_interface(int i, boolean z);

    private native void native_close();

    private native int native_control_request(int i, int i2, int i3, int i4, byte[] bArr, int i5, int i6, int i7);

    private native byte[] native_get_desc();

    private native int native_get_fd();

    private native String native_get_serial();

    private native boolean native_open(String str, FileDescriptor fileDescriptor);

    private native boolean native_release_interface(int i);

    private native UsbRequest native_request_wait(long j) throws TimeoutException;

    private native boolean native_reset_device();

    private native boolean native_set_configuration(int i);

    private native boolean native_set_interface(int i, int i2);

    public UsbDeviceConnection(UsbDevice usbDevice) {
        this.mDevice = usbDevice;
    }

    boolean open(String str, ParcelFileDescriptor parcelFileDescriptor, Context context) {
        this.mContext = context.getApplicationContext();
        boolean zNative_open = native_open(str, parcelFileDescriptor.getFileDescriptor());
        if (zNative_open) {
            this.mCloseGuard.open("close");
        }
        return zNative_open;
    }

    public Context getContext() {
        return this.mContext;
    }

    public void close() {
        if (this.mNativeContext != 0) {
            native_close();
            this.mCloseGuard.close();
        }
    }

    public int getFileDescriptor() {
        return native_get_fd();
    }

    public byte[] getRawDescriptors() {
        return native_get_desc();
    }

    public boolean claimInterface(UsbInterface usbInterface, boolean z) {
        return native_claim_interface(usbInterface.getId(), z);
    }

    public boolean releaseInterface(UsbInterface usbInterface) {
        return native_release_interface(usbInterface.getId());
    }

    public boolean setInterface(UsbInterface usbInterface) {
        return native_set_interface(usbInterface.getId(), usbInterface.getAlternateSetting());
    }

    public boolean setConfiguration(UsbConfiguration usbConfiguration) {
        return native_set_configuration(usbConfiguration.getId());
    }

    public int controlTransfer(int i, int i2, int i3, int i4, byte[] bArr, int i5, int i6) {
        return controlTransfer(i, i2, i3, i4, bArr, 0, i5, i6);
    }

    public int controlTransfer(int i, int i2, int i3, int i4, byte[] bArr, int i5, int i6, int i7) {
        checkBounds(bArr, i5, i6);
        return native_control_request(i, i2, i3, i4, bArr, i5, i6, i7);
    }

    public int bulkTransfer(UsbEndpoint usbEndpoint, byte[] bArr, int i, int i2) {
        return bulkTransfer(usbEndpoint, bArr, 0, i, i2);
    }

    public int bulkTransfer(UsbEndpoint usbEndpoint, byte[] bArr, int i, int i2, int i3) {
        checkBounds(bArr, i, i2);
        return native_bulk_request(usbEndpoint.getAddress(), bArr, i, (this.mContext.getApplicationInfo().targetSdkVersion >= 28 || i2 <= 16384) ? i2 : 16384, i3);
    }

    @SystemApi
    @SuppressLint({"Doclava125"})
    public boolean resetDevice() {
        return native_reset_device();
    }

    public UsbRequest requestWait() {
        UsbRequest usbRequestNative_request_wait;
        try {
            usbRequestNative_request_wait = native_request_wait(-1L);
        } catch (TimeoutException e) {
            usbRequestNative_request_wait = null;
        }
        if (usbRequestNative_request_wait != null) {
            usbRequestNative_request_wait.dequeue(this.mContext.getApplicationInfo().targetSdkVersion >= 26);
        }
        return usbRequestNative_request_wait;
    }

    public UsbRequest requestWait(long j) throws TimeoutException {
        UsbRequest usbRequestNative_request_wait = native_request_wait(Preconditions.checkArgumentNonnegative(j, GpsNetInitiatedHandler.NI_INTENT_KEY_TIMEOUT));
        if (usbRequestNative_request_wait != null) {
            usbRequestNative_request_wait.dequeue(true);
        }
        return usbRequestNative_request_wait;
    }

    public String getSerial() {
        return native_get_serial();
    }

    private static void checkBounds(byte[] bArr, int i, int i2) {
        int length = bArr != null ? bArr.length : 0;
        if (i2 < 0 || i < 0 || i + i2 > length) {
            throw new IllegalArgumentException("Buffer start or length out of bounds.");
        }
    }

    protected void finalize() throws Throwable {
        try {
            if (this.mCloseGuard != null) {
                this.mCloseGuard.warnIfOpen();
            }
        } finally {
            super.finalize();
        }
    }
}
