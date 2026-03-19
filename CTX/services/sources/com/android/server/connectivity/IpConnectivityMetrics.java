package com.android.server.connectivity;

import android.content.Context;
import android.net.ConnectivityMetricsEvent;
import android.net.IIpConnectivityMetrics;
import android.net.INetdEventCallback;
import android.net.ip.IpClient;
import android.net.metrics.ApfProgramEvent;
import android.os.Binder;
import android.os.IBinder;
import android.provider.Settings;
import android.util.ArrayMap;
import android.util.Base64;
import android.util.Log;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.RingBuffer;
import com.android.internal.util.TokenBucket;
import com.android.server.LocalServices;
import com.android.server.SystemService;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.connectivity.metrics.nano.IpConnectivityLogClass;
import dalvik.system.PathClassLoader;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.function.ToIntFunction;

public final class IpConnectivityMetrics extends SystemService {
    private static final boolean DBG = false;
    private static final int DEFAULT_BUFFER_SIZE = 2000;
    private static final int DEFAULT_LOG_SIZE = 500;
    private static final int ERROR_RATE_LIMITED = -1;
    private static final int MAXIMUM_BUFFER_SIZE = 20000;
    private static final int MAXIMUM_CONNECT_LATENCY_RECORDS = 20000;
    private static final int NYC = 0;
    private static final int NYC_MR1 = 1;
    private static final int NYC_MR2 = 2;
    private static final String SERVICE_NAME = "connmetrics";
    public static final int VERSION = 2;

    @VisibleForTesting
    public final Impl impl;
    private IBinder mBinder;

    @GuardedBy("mLock")
    private final ArrayMap<Class<?>, TokenBucket> mBuckets;

    @GuardedBy("mLock")
    private ArrayList<ConnectivityMetricsEvent> mBuffer;

    @GuardedBy("mLock")
    private int mCapacity;
    private final ToIntFunction<Context> mCapacityGetter;

    @VisibleForTesting
    final DefaultNetworkMetrics mDefaultNetworkMetrics;

    @GuardedBy("mLock")
    private int mDropped;

    @GuardedBy("mLock")
    private final RingBuffer<ConnectivityMetricsEvent> mEventLog;
    private final Object mLock;

    @VisibleForTesting
    NetdEventListenerService mNetdListener;
    private static final String TAG = IpConnectivityMetrics.class.getSimpleName();
    private static final ToIntFunction<Context> READ_BUFFER_SIZE = new ToIntFunction() {
        @Override
        public final int applyAsInt(Object obj) {
            return IpConnectivityMetrics.lambda$static$0((Context) obj);
        }
    };

    public interface Logger {
        DefaultNetworkMetrics defaultNetworkMetrics();
    }

    public IpConnectivityMetrics(Context context, ToIntFunction<Context> toIntFunction) {
        super(context);
        this.mLock = new Object();
        this.impl = new Impl();
        this.mEventLog = new RingBuffer<>(ConnectivityMetricsEvent.class, 500);
        this.mBuckets = makeRateLimitingBuckets();
        this.mDefaultNetworkMetrics = new DefaultNetworkMetrics();
        this.mCapacityGetter = toIntFunction;
        initBuffer();
    }

    public IpConnectivityMetrics(Context context) {
        this(context, READ_BUFFER_SIZE);
    }

    @Override
    public void onStart() {
    }

    @Override
    public void onBootPhase(int i) {
        if (i == 500) {
            this.mNetdListener = new NetdEventListenerService(getContext());
            publishBinderService(SERVICE_NAME, this.impl);
            NetdEventListenerService netdEventListenerService = this.mNetdListener;
            publishBinderService(NetdEventListenerService.SERVICE_NAME, this.mNetdListener);
            try {
                Class clsLoadClass = new PathClassLoader("/system/framework/mediatek-framework-net.jar", getContext().getClassLoader()).loadClass("com.mediatek.net.connectivity.MtkIpConnectivityMetrics");
                Constructor constructor = clsLoadClass.getConstructor(Context.class, NetdEventListenerService.class);
                constructor.setAccessible(true);
                Object objNewInstance = constructor.newInstance(getContext(), this.mNetdListener);
                Method declaredMethod = clsLoadClass.getDeclaredMethod("getMtkIpConnSrv", new Class[0]);
                declaredMethod.setAccessible(true);
                this.mBinder = (IBinder) declaredMethod.invoke(objNewInstance, new Object[0]);
            } catch (Exception e) {
                Log.d(TAG, "No MtkIpConnectivityMetrics:" + e);
            }
            if (this.mBinder == null) {
                Log.e(TAG, "mtkIpConnClass is null");
            } else {
                publishBinderService("mtkconnmetrics", this.mBinder);
                LocalServices.addService(Logger.class, new LoggerImpl());
            }
        }
    }

    @VisibleForTesting
    public int bufferCapacity() {
        return this.mCapacityGetter.applyAsInt(getContext());
    }

    private void initBuffer() {
        synchronized (this.mLock) {
            this.mDropped = 0;
            this.mCapacity = bufferCapacity();
            this.mBuffer = new ArrayList<>(this.mCapacity);
        }
    }

    private int append(ConnectivityMetricsEvent connectivityMetricsEvent) {
        synchronized (this.mLock) {
            this.mEventLog.append(connectivityMetricsEvent);
            int size = this.mCapacity - this.mBuffer.size();
            if (connectivityMetricsEvent == null) {
                return size;
            }
            if (isRateLimited(connectivityMetricsEvent)) {
                return -1;
            }
            if (size == 0) {
                this.mDropped++;
                return 0;
            }
            this.mBuffer.add(connectivityMetricsEvent);
            return size - 1;
        }
    }

    private boolean isRateLimited(ConnectivityMetricsEvent connectivityMetricsEvent) {
        TokenBucket tokenBucket = this.mBuckets.get(connectivityMetricsEvent.data.getClass());
        return (tokenBucket == null || tokenBucket.get()) ? false : true;
    }

    private String flushEncodedOutput() {
        ArrayList<ConnectivityMetricsEvent> arrayList;
        int i;
        synchronized (this.mLock) {
            arrayList = this.mBuffer;
            i = this.mDropped;
            initBuffer();
        }
        List<IpConnectivityLogClass.IpConnectivityEvent> proto = IpConnectivityEventBuilder.toProto(arrayList);
        this.mDefaultNetworkMetrics.flushEvents(proto);
        if (this.mNetdListener != null) {
            this.mNetdListener.flushStatistics(proto);
        }
        try {
            return Base64.encodeToString(IpConnectivityEventBuilder.serialize(i, proto), 0);
        } catch (IOException e) {
            Log.e(TAG, "could not serialize events", e);
            return BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        }
    }

    private void cmdFlush(PrintWriter printWriter) {
        printWriter.print(flushEncodedOutput());
    }

    private void cmdList(PrintWriter printWriter) {
        printWriter.println("metrics events:");
        Iterator<ConnectivityMetricsEvent> it = getEvents().iterator();
        while (it.hasNext()) {
            printWriter.println(it.next().toString());
        }
        printWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        if (this.mNetdListener != null) {
            this.mNetdListener.list(printWriter);
        }
        printWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        this.mDefaultNetworkMetrics.listEvents(printWriter);
    }

    private void cmdListAsProto(PrintWriter printWriter) {
        Iterator<IpConnectivityLogClass.IpConnectivityEvent> it = IpConnectivityEventBuilder.toProto(getEvents()).iterator();
        while (it.hasNext()) {
            printWriter.print(it.next().toString());
        }
        if (this.mNetdListener != null) {
            this.mNetdListener.listAsProtos(printWriter);
        }
        this.mDefaultNetworkMetrics.listEventsAsProto(printWriter);
    }

    private List<ConnectivityMetricsEvent> getEvents() {
        List<ConnectivityMetricsEvent> listAsList;
        synchronized (this.mLock) {
            listAsList = Arrays.asList((ConnectivityMetricsEvent[]) this.mEventLog.toArray());
        }
        return listAsList;
    }

    public final class Impl extends IIpConnectivityMetrics.Stub {
        static final String CMD_DEFAULT = "";
        static final String CMD_FLUSH = "flush";
        static final String CMD_IPCLIENT = "ipclient";
        static final String CMD_LIST = "list";
        static final String CMD_PROTO = "proto";

        public Impl() {
        }

        public int logEvent(ConnectivityMetricsEvent connectivityMetricsEvent) {
            enforceConnectivityInternalPermission();
            return IpConnectivityMetrics.this.append(connectivityMetricsEvent);
        }

        public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
            enforceDumpPermission();
            byte b = 0;
            String str = strArr.length > 0 ? strArr[0] : "";
            int iHashCode = str.hashCode();
            if (iHashCode != 3322014) {
                if (iHashCode != 97532676) {
                    if (iHashCode != 106940904) {
                        b = (iHashCode == 1864910770 && str.equals("ipclient")) ? (byte) 2 : (byte) -1;
                    } else if (str.equals(CMD_PROTO)) {
                        b = 1;
                    }
                } else if (!str.equals(CMD_FLUSH)) {
                }
            } else if (str.equals(CMD_LIST)) {
                b = 3;
            }
            String[] strArr2 = null;
            switch (b) {
                case 0:
                    IpConnectivityMetrics.this.cmdFlush(printWriter);
                    break;
                case 1:
                    IpConnectivityMetrics.this.cmdListAsProto(printWriter);
                    break;
                case 2:
                    if (strArr != null && strArr.length > 1) {
                        strArr2 = (String[]) Arrays.copyOfRange(strArr, 1, strArr.length);
                    }
                    IpClient.dumpAllLogs(printWriter, strArr2);
                    break;
                case 3:
                    IpConnectivityMetrics.this.cmdList(printWriter);
                    break;
                default:
                    IpConnectivityMetrics.this.cmdList(printWriter);
                    printWriter.println("");
                    IpClient.dumpAllLogs(printWriter, null);
                    break;
            }
        }

        private void enforceConnectivityInternalPermission() {
            enforcePermission("android.permission.CONNECTIVITY_INTERNAL");
        }

        private void enforceDumpPermission() {
            enforcePermission("android.permission.DUMP");
        }

        private void enforcePermission(String str) {
            IpConnectivityMetrics.this.getContext().enforceCallingOrSelfPermission(str, "IpConnectivityMetrics");
        }

        private void enforceNetdEventListeningPermission() {
            int callingUid = Binder.getCallingUid();
            if (callingUid != 1000) {
                throw new SecurityException(String.format("Uid %d has no permission to listen for netd events.", Integer.valueOf(callingUid)));
            }
        }

        public boolean addNetdEventCallback(int i, INetdEventCallback iNetdEventCallback) {
            enforceNetdEventListeningPermission();
            if (IpConnectivityMetrics.this.mNetdListener == null) {
                return false;
            }
            return IpConnectivityMetrics.this.mNetdListener.addNetdEventCallback(i, iNetdEventCallback);
        }

        public boolean removeNetdEventCallback(int i) {
            enforceNetdEventListeningPermission();
            if (IpConnectivityMetrics.this.mNetdListener == null) {
                return true;
            }
            return IpConnectivityMetrics.this.mNetdListener.removeNetdEventCallback(i);
        }
    }

    static int lambda$static$0(Context context) {
        int i = Settings.Global.getInt(context.getContentResolver(), "connectivity_metrics_buffer_size", 2000);
        if (i <= 0) {
            return 2000;
        }
        return Math.min(i, 20000);
    }

    private static ArrayMap<Class<?>, TokenBucket> makeRateLimitingBuckets() {
        ArrayMap<Class<?>, TokenBucket> arrayMap = new ArrayMap<>();
        arrayMap.put(ApfProgramEvent.class, new TokenBucket(60000, 50));
        return arrayMap;
    }

    private class LoggerImpl implements Logger {
        private LoggerImpl() {
        }

        @Override
        public DefaultNetworkMetrics defaultNetworkMetrics() {
            return IpConnectivityMetrics.this.mDefaultNetworkMetrics;
        }
    }
}
