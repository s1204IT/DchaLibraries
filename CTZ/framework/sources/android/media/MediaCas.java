package android.media;

import android.hardware.cas.V1_0.HidlCasPluginDescriptor;
import android.hardware.cas.V1_0.ICas;
import android.hardware.cas.V1_0.ICasListener;
import android.hardware.cas.V1_0.IMediaCasService;
import android.media.MediaCasException;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IHwBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.util.Singleton;
import java.util.ArrayList;

public final class MediaCas implements AutoCloseable {
    private static final String TAG = "MediaCas";
    private static final Singleton<IMediaCasService> gDefault = new Singleton<IMediaCasService>() {
        @Override
        protected IMediaCasService create() {
            try {
                return IMediaCasService.getService();
            } catch (RemoteException e) {
                return null;
            }
        }
    };
    private final ICasListener.Stub mBinder = new ICasListener.Stub() {
        @Override
        public void onEvent(int i, int i2, ArrayList<Byte> arrayList) throws RemoteException {
            MediaCas.this.mEventHandler.sendMessage(MediaCas.this.mEventHandler.obtainMessage(0, i, i2, arrayList));
        }
    };
    private EventHandler mEventHandler;
    private HandlerThread mHandlerThread;
    private ICas mICas;
    private EventListener mListener;

    public interface EventListener {
        void onEvent(MediaCas mediaCas, int i, int i2, byte[] bArr);
    }

    static IMediaCasService getService() {
        return gDefault.get();
    }

    private void validateInternalStates() {
        if (this.mICas == null) {
            throw new IllegalStateException();
        }
    }

    private void cleanupAndRethrowIllegalState() {
        this.mICas = null;
        throw new IllegalStateException();
    }

    private class EventHandler extends Handler {
        private static final int MSG_CAS_EVENT = 0;

        public EventHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            if (message.what == 0) {
                MediaCas.this.mListener.onEvent(MediaCas.this, message.arg1, message.arg2, MediaCas.this.toBytes((ArrayList) message.obj));
            }
        }
    }

    public static class PluginDescriptor {
        private final int mCASystemId;
        private final String mName;

        private PluginDescriptor() {
            this.mCASystemId = 65535;
            this.mName = null;
        }

        PluginDescriptor(HidlCasPluginDescriptor hidlCasPluginDescriptor) {
            this.mCASystemId = hidlCasPluginDescriptor.caSystemId;
            this.mName = hidlCasPluginDescriptor.name;
        }

        public int getSystemId() {
            return this.mCASystemId;
        }

        public String getName() {
            return this.mName;
        }

        public String toString() {
            return "PluginDescriptor {" + this.mCASystemId + ", " + this.mName + "}";
        }
    }

    private ArrayList<Byte> toByteArray(byte[] bArr, int i, int i2) {
        ArrayList<Byte> arrayList = new ArrayList<>(i2);
        for (int i3 = 0; i3 < i2; i3++) {
            arrayList.add(Byte.valueOf(bArr[i + i3]));
        }
        return arrayList;
    }

    private ArrayList<Byte> toByteArray(byte[] bArr) {
        if (bArr == null) {
            return new ArrayList<>();
        }
        return toByteArray(bArr, 0, bArr.length);
    }

    private byte[] toBytes(ArrayList<Byte> arrayList) {
        if (arrayList != null) {
            byte[] bArr = new byte[arrayList.size()];
            for (int i = 0; i < bArr.length; i++) {
                bArr[i] = arrayList.get(i).byteValue();
            }
            return bArr;
        }
        return null;
    }

    public final class Session implements AutoCloseable {
        final ArrayList<Byte> mSessionId;

        Session(ArrayList<Byte> arrayList) {
            this.mSessionId = arrayList;
        }

        public void setPrivateData(byte[] bArr) throws MediaCasException {
            MediaCas.this.validateInternalStates();
            try {
                MediaCasException.throwExceptionIfNeeded(MediaCas.this.mICas.setSessionPrivateData(this.mSessionId, MediaCas.this.toByteArray(bArr, 0, bArr.length)));
            } catch (RemoteException e) {
                MediaCas.this.cleanupAndRethrowIllegalState();
            }
        }

        public void processEcm(byte[] bArr, int i, int i2) throws MediaCasException {
            MediaCas.this.validateInternalStates();
            try {
                MediaCasException.throwExceptionIfNeeded(MediaCas.this.mICas.processEcm(this.mSessionId, MediaCas.this.toByteArray(bArr, i, i2)));
            } catch (RemoteException e) {
                MediaCas.this.cleanupAndRethrowIllegalState();
            }
        }

        public void processEcm(byte[] bArr) throws MediaCasException {
            processEcm(bArr, 0, bArr.length);
        }

        @Override
        public void close() {
            MediaCas.this.validateInternalStates();
            try {
                MediaCasStateException.throwExceptionIfNeeded(MediaCas.this.mICas.closeSession(this.mSessionId));
            } catch (RemoteException e) {
                MediaCas.this.cleanupAndRethrowIllegalState();
            }
        }
    }

    Session createFromSessionId(ArrayList<Byte> arrayList) {
        if (arrayList == null || arrayList.size() == 0) {
            return null;
        }
        return new Session(arrayList);
    }

    public static boolean isSystemIdSupported(int i) {
        IMediaCasService service = getService();
        if (service != null) {
            try {
                return service.isSystemIdSupported(i);
            } catch (RemoteException e) {
                return false;
            }
        }
        return false;
    }

    public static PluginDescriptor[] enumeratePlugins() {
        IMediaCasService service = getService();
        if (service != null) {
            try {
                ArrayList<HidlCasPluginDescriptor> arrayListEnumeratePlugins = service.enumeratePlugins();
                if (arrayListEnumeratePlugins.size() == 0) {
                    return null;
                }
                PluginDescriptor[] pluginDescriptorArr = new PluginDescriptor[arrayListEnumeratePlugins.size()];
                for (int i = 0; i < pluginDescriptorArr.length; i++) {
                    pluginDescriptorArr[i] = new PluginDescriptor(arrayListEnumeratePlugins.get(i));
                }
                return pluginDescriptorArr;
            } catch (RemoteException e) {
            }
        }
        return null;
    }

    public MediaCas(int i) throws MediaCasException.UnsupportedCasException {
        try {
            try {
                this.mICas = getService().createPlugin(i, this.mBinder);
                if (this.mICas == null) {
                    throw new MediaCasException.UnsupportedCasException("Unsupported CA_system_id " + i);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to create plugin: " + e);
                this.mICas = null;
                if (this.mICas == null) {
                    throw new MediaCasException.UnsupportedCasException("Unsupported CA_system_id " + i);
                }
            }
        } catch (Throwable th) {
            if (this.mICas != null) {
                throw th;
            }
            throw new MediaCasException.UnsupportedCasException("Unsupported CA_system_id " + i);
        }
    }

    IHwBinder getBinder() {
        validateInternalStates();
        return this.mICas.asBinder();
    }

    public void setEventListener(EventListener eventListener, Handler handler) {
        this.mListener = eventListener;
        if (this.mListener == null) {
            this.mEventHandler = null;
            return;
        }
        Looper looper = handler != null ? handler.getLooper() : null;
        if (looper == null && (looper = Looper.myLooper()) == null && (looper = Looper.getMainLooper()) == null) {
            if (this.mHandlerThread == null || !this.mHandlerThread.isAlive()) {
                this.mHandlerThread = new HandlerThread("MediaCasEventThread", -2);
                this.mHandlerThread.start();
            }
            looper = this.mHandlerThread.getLooper();
        }
        this.mEventHandler = new EventHandler(looper);
    }

    public void setPrivateData(byte[] bArr) throws MediaCasException {
        validateInternalStates();
        try {
            MediaCasException.throwExceptionIfNeeded(this.mICas.setPrivateData(toByteArray(bArr, 0, bArr.length)));
        } catch (RemoteException e) {
            cleanupAndRethrowIllegalState();
        }
    }

    private class OpenSessionCallback implements ICas.openSessionCallback {
        public Session mSession;
        public int mStatus;

        private OpenSessionCallback() {
        }

        @Override
        public void onValues(int i, ArrayList<Byte> arrayList) {
            this.mStatus = i;
            this.mSession = MediaCas.this.createFromSessionId(arrayList);
        }
    }

    public Session openSession() throws MediaCasException {
        validateInternalStates();
        try {
            OpenSessionCallback openSessionCallback = new OpenSessionCallback();
            this.mICas.openSession(openSessionCallback);
            MediaCasException.throwExceptionIfNeeded(openSessionCallback.mStatus);
            return openSessionCallback.mSession;
        } catch (RemoteException e) {
            cleanupAndRethrowIllegalState();
            return null;
        }
    }

    public void processEmm(byte[] bArr, int i, int i2) throws MediaCasException {
        validateInternalStates();
        try {
            MediaCasException.throwExceptionIfNeeded(this.mICas.processEmm(toByteArray(bArr, i, i2)));
        } catch (RemoteException e) {
            cleanupAndRethrowIllegalState();
        }
    }

    public void processEmm(byte[] bArr) throws MediaCasException {
        processEmm(bArr, 0, bArr.length);
    }

    public void sendEvent(int i, int i2, byte[] bArr) throws MediaCasException {
        validateInternalStates();
        try {
            MediaCasException.throwExceptionIfNeeded(this.mICas.sendEvent(i, i2, toByteArray(bArr)));
        } catch (RemoteException e) {
            cleanupAndRethrowIllegalState();
        }
    }

    public void provision(String str) throws MediaCasException {
        validateInternalStates();
        try {
            MediaCasException.throwExceptionIfNeeded(this.mICas.provision(str));
        } catch (RemoteException e) {
            cleanupAndRethrowIllegalState();
        }
    }

    public void refreshEntitlements(int i, byte[] bArr) throws MediaCasException {
        validateInternalStates();
        try {
            MediaCasException.throwExceptionIfNeeded(this.mICas.refreshEntitlements(i, toByteArray(bArr)));
        } catch (RemoteException e) {
            cleanupAndRethrowIllegalState();
        }
    }

    @Override
    public void close() {
        if (this.mICas != null) {
            try {
                this.mICas.release();
            } catch (RemoteException e) {
            } catch (Throwable th) {
                this.mICas = null;
                throw th;
            }
            this.mICas = null;
        }
    }

    protected void finalize() {
        close();
    }
}
