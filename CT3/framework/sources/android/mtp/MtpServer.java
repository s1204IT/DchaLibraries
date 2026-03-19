package android.mtp;

public class MtpServer implements Runnable {
    private final MtpDatabase mDatabase;
    private long mNativeContext;
    private boolean mServerEndup;

    private final native void native_add_storage(MtpStorage mtpStorage);

    private final native void native_cleanup();

    private final native void native_remove_storage(int i);

    private final native void native_run();

    private final native void native_send_device_property_changed(int i);

    private final native void native_send_object_added(int i);

    private final native void native_send_object_infoChanged(int i);

    private final native void native_send_object_removed(int i);

    private final native void native_send_storage_infoChanged(int i);

    private final native void native_setup(MtpDatabase mtpDatabase, boolean z);

    private final native void native_update_storage(MtpStorage mtpStorage);

    static {
        System.loadLibrary("media_jni");
    }

    public MtpServer(MtpDatabase database, boolean usePtp) {
        this.mServerEndup = false;
        this.mServerEndup = false;
        this.mDatabase = database;
        native_setup(database, usePtp);
        database.setServer(this);
    }

    public void start() {
        this.mServerEndup = false;
        Thread thread = new Thread(this, "MtpServer");
        thread.start();
    }

    @Override
    public void run() {
        native_run();
        native_cleanup();
        this.mServerEndup = true;
        this.mDatabase.close();
    }

    public void sendObjectAdded(int handle) {
        native_send_object_added(handle);
    }

    public void sendObjectRemoved(int handle) {
        native_send_object_removed(handle);
    }

    public void sendDevicePropertyChanged(int property) {
        native_send_device_property_changed(property);
    }

    public void addStorage(MtpStorage storage) {
        native_add_storage(storage);
    }

    public void removeStorage(MtpStorage storage) {
        native_remove_storage(storage.getStorageId());
    }

    public void updateStorage(MtpStorage storage) {
        native_update_storage(storage);
    }

    public void sendStorageInfoChanged(MtpStorage storage) {
        native_send_storage_infoChanged(storage.getStorageId());
    }

    public void endSession() {
    }

    public boolean getStatus() {
        return this.mServerEndup;
    }

    public void sendObjectInfoChanged(int handle) {
        native_send_object_infoChanged(handle);
    }
}
