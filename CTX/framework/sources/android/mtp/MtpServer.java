package android.mtp;

import com.android.internal.util.Preconditions;
import java.io.FileDescriptor;

public class MtpServer implements Runnable {
    private final MtpDatabase mDatabase;
    private long mNativeContext;
    private final Runnable mOnTerminate;

    private final native void native_add_storage(MtpStorage mtpStorage);

    private final native void native_cleanup();

    public static final native void native_configure(boolean z);

    private final native void native_remove_storage(int i);

    private final native void native_run();

    private final native void native_send_device_property_changed(int i);

    private final native void native_send_object_added(int i);

    private final native void native_send_object_removed(int i);

    private final native void native_setup(MtpDatabase mtpDatabase, FileDescriptor fileDescriptor, boolean z, String str, String str2, String str3, String str4);

    static {
        System.loadLibrary("media_jni");
    }

    public MtpServer(MtpDatabase mtpDatabase, FileDescriptor fileDescriptor, boolean z, Runnable runnable, String str, String str2, String str3, String str4) {
        this.mDatabase = (MtpDatabase) Preconditions.checkNotNull(mtpDatabase);
        this.mOnTerminate = (Runnable) Preconditions.checkNotNull(runnable);
        native_setup(mtpDatabase, fileDescriptor, z, str, str2, str3, str4);
        mtpDatabase.setServer(this);
    }

    public void start() {
        new Thread(this, "MtpServer").start();
    }

    @Override
    public void run() {
        native_run();
        native_cleanup();
        this.mDatabase.close();
        this.mOnTerminate.run();
    }

    public void sendObjectAdded(int i) {
        native_send_object_added(i);
    }

    public void sendObjectRemoved(int i) {
        native_send_object_removed(i);
    }

    public void sendDevicePropertyChanged(int i) {
        native_send_device_property_changed(i);
    }

    public void addStorage(MtpStorage mtpStorage) {
        native_add_storage(mtpStorage);
    }

    public void removeStorage(MtpStorage mtpStorage) {
        native_remove_storage(mtpStorage.getStorageId());
    }

    public static void configure(boolean z) {
        native_configure(z);
    }
}
