package com.android.server;

import android.app.AppOpsManager;
import android.content.Context;
import android.net.IIpSecService;
import android.net.INetd;
import android.net.IpSecAlgorithm;
import android.net.IpSecConfig;
import android.net.IpSecSpiResponse;
import android.net.IpSecTransformResponse;
import android.net.IpSecTunnelInterfaceResponse;
import android.net.IpSecUdpEncapResponse;
import android.net.LinkAddress;
import android.net.Network;
import android.net.NetworkUtils;
import android.net.TrafficStats;
import android.net.util.NetdService;
import android.os.Binder;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.ServiceSpecificException;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.text.TextUtils;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.Preconditions;
import com.android.server.backup.BackupManagerConstants;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import libcore.io.IoUtils;

public class IpSecService extends IIpSecService.Stub {
    static final int FREE_PORT_MIN = 1024;
    private static final InetAddress INADDR_ANY;
    private static final int MAX_PORT_BIND_ATTEMPTS = 10;
    private static final int NETD_FETCH_TIMEOUT_MS = 5000;
    private static final String NETD_SERVICE_NAME = "netd";
    static final int PORT_MAX = 65535;

    @VisibleForTesting
    static final int TUN_INTF_NETID_RANGE = 1024;

    @VisibleForTesting
    static final int TUN_INTF_NETID_START = 64512;
    private final Context mContext;

    @GuardedBy("IpSecService.this")
    private int mNextResourceId;
    private int mNextTunnelNetIdIndex;
    private final IpSecServiceConfiguration mSrvConfig;
    private final SparseBooleanArray mTunnelNetIds;
    final UidFdTagger mUidFdTagger;

    @VisibleForTesting
    final UserResourceTracker mUserResourceTracker;
    private static final String TAG = "IpSecService";
    private static final boolean DBG = Log.isLoggable(TAG, 3);
    private static final int[] DIRECTIONS = {1, 0};
    private static final String[] WILDCARD_ADDRESSES = {"0.0.0.0", "::"};

    @VisibleForTesting
    public interface IResource {
        void freeUnderlyingResources() throws RemoteException;

        void invalidate() throws RemoteException;
    }

    interface IpSecServiceConfiguration {
        public static final IpSecServiceConfiguration GETSRVINSTANCE = new IpSecServiceConfiguration() {
            @Override
            public INetd getNetdInstance() throws RemoteException {
                INetd netdService = NetdService.getInstance();
                if (netdService == null) {
                    throw new RemoteException("Failed to Get Netd Instance");
                }
                return netdService;
            }
        };

        INetd getNetdInstance() throws RemoteException;
    }

    @VisibleForTesting
    public interface UidFdTagger {
        void tag(FileDescriptor fileDescriptor, int i) throws IOException;
    }

    static {
        try {
            INADDR_ANY = InetAddress.getByAddress(new byte[]{0, 0, 0, 0});
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    @VisibleForTesting
    public class RefcountedResource<T extends IResource> implements IBinder.DeathRecipient {
        IBinder mBinder;
        private final List<RefcountedResource> mChildren;
        int mRefCount = 1;
        private final T mResource;

        RefcountedResource(T t, IBinder iBinder, RefcountedResource... refcountedResourceArr) {
            synchronized (IpSecService.this) {
                this.mResource = t;
                this.mChildren = new ArrayList(refcountedResourceArr.length);
                this.mBinder = iBinder;
                for (RefcountedResource refcountedResource : refcountedResourceArr) {
                    this.mChildren.add(refcountedResource);
                    refcountedResource.mRefCount++;
                }
                try {
                    this.mBinder.linkToDeath(this, 0);
                } catch (RemoteException e) {
                    binderDied();
                }
            }
        }

        @Override
        public void binderDied() {
            synchronized (IpSecService.this) {
                try {
                    userRelease();
                } catch (Exception e) {
                    Log.e(IpSecService.TAG, "Failed to release resource: " + e);
                }
            }
        }

        public T getResource() {
            return this.mResource;
        }

        @GuardedBy("IpSecService.this")
        public void userRelease() throws RemoteException {
            if (this.mBinder == null) {
                return;
            }
            this.mBinder.unlinkToDeath(this, 0);
            this.mBinder = null;
            this.mResource.invalidate();
            releaseReference();
        }

        @GuardedBy("IpSecService.this")
        @VisibleForTesting
        public void releaseReference() throws RemoteException {
            this.mRefCount--;
            if (this.mRefCount > 0) {
                return;
            }
            if (this.mRefCount < 0) {
                throw new IllegalStateException("Invalid operation - resource has already been released.");
            }
            this.mResource.freeUnderlyingResources();
            Iterator<RefcountedResource> it = this.mChildren.iterator();
            while (it.hasNext()) {
                it.next().releaseReference();
            }
            this.mRefCount--;
        }

        public String toString() {
            return "{mResource=" + this.mResource + ", mRefCount=" + this.mRefCount + ", mChildren=" + this.mChildren + "}";
        }
    }

    @VisibleForTesting
    static class ResourceTracker {
        int mCurrent = 0;
        private final int mMax;

        ResourceTracker(int i) {
            this.mMax = i;
        }

        boolean isAvailable() {
            return this.mCurrent < this.mMax;
        }

        void take() {
            if (!isAvailable()) {
                Log.wtf(IpSecService.TAG, "Too many resources allocated!");
            }
            this.mCurrent++;
        }

        void give() {
            if (this.mCurrent <= 0) {
                Log.wtf(IpSecService.TAG, "We've released this resource too many times");
            }
            this.mCurrent--;
        }

        public String toString() {
            return "{mCurrent=" + this.mCurrent + ", mMax=" + this.mMax + "}";
        }
    }

    @VisibleForTesting
    static final class UserRecord {
        public static final int MAX_NUM_ENCAP_SOCKETS = 2;
        public static final int MAX_NUM_SPIS = 8;
        public static final int MAX_NUM_TRANSFORMS = 4;
        public static final int MAX_NUM_TUNNEL_INTERFACES = 2;
        final RefcountedResourceArray<SpiRecord> mSpiRecords = new RefcountedResourceArray<>(SpiRecord.class.getSimpleName());
        final RefcountedResourceArray<TransformRecord> mTransformRecords = new RefcountedResourceArray<>(TransformRecord.class.getSimpleName());
        final RefcountedResourceArray<EncapSocketRecord> mEncapSocketRecords = new RefcountedResourceArray<>(EncapSocketRecord.class.getSimpleName());
        final RefcountedResourceArray<TunnelInterfaceRecord> mTunnelInterfaceRecords = new RefcountedResourceArray<>(TunnelInterfaceRecord.class.getSimpleName());
        final ResourceTracker mSpiQuotaTracker = new ResourceTracker(8);
        final ResourceTracker mTransformQuotaTracker = new ResourceTracker(4);
        final ResourceTracker mSocketQuotaTracker = new ResourceTracker(2);
        final ResourceTracker mTunnelQuotaTracker = new ResourceTracker(2);

        UserRecord() {
        }

        void removeSpiRecord(int i) {
            this.mSpiRecords.remove(i);
        }

        void removeTransformRecord(int i) {
            this.mTransformRecords.remove(i);
        }

        void removeTunnelInterfaceRecord(int i) {
            this.mTunnelInterfaceRecords.remove(i);
        }

        void removeEncapSocketRecord(int i) {
            this.mEncapSocketRecords.remove(i);
        }

        public String toString() {
            return "{mSpiQuotaTracker=" + this.mSpiQuotaTracker + ", mTransformQuotaTracker=" + this.mTransformQuotaTracker + ", mSocketQuotaTracker=" + this.mSocketQuotaTracker + ", mTunnelQuotaTracker=" + this.mTunnelQuotaTracker + ", mSpiRecords=" + this.mSpiRecords + ", mTransformRecords=" + this.mTransformRecords + ", mEncapSocketRecords=" + this.mEncapSocketRecords + ", mTunnelInterfaceRecords=" + this.mTunnelInterfaceRecords + "}";
        }
    }

    @VisibleForTesting
    static final class UserResourceTracker {
        private final SparseArray<UserRecord> mUserRecords = new SparseArray<>();

        UserResourceTracker() {
        }

        public UserRecord getUserRecord(int i) {
            checkCallerUid(i);
            UserRecord userRecord = this.mUserRecords.get(i);
            if (userRecord == null) {
                UserRecord userRecord2 = new UserRecord();
                this.mUserRecords.put(i, userRecord2);
                return userRecord2;
            }
            return userRecord;
        }

        private void checkCallerUid(int i) {
            if (i != Binder.getCallingUid() && 1000 != Binder.getCallingUid()) {
                throw new SecurityException("Attempted access of unowned resources");
            }
        }

        public String toString() {
            return this.mUserRecords.toString();
        }
    }

    private abstract class OwnedResourceRecord implements IResource {
        protected final int mResourceId;
        final int pid;
        final int uid;

        @Override
        public abstract void freeUnderlyingResources() throws RemoteException;

        protected abstract ResourceTracker getResourceTracker();

        @Override
        public abstract void invalidate() throws RemoteException;

        OwnedResourceRecord(int i) {
            if (i == -1) {
                throw new IllegalArgumentException("Resource ID must not be INVALID_RESOURCE_ID");
            }
            this.mResourceId = i;
            this.pid = Binder.getCallingPid();
            this.uid = Binder.getCallingUid();
            getResourceTracker().take();
        }

        protected UserRecord getUserRecord() {
            return IpSecService.this.mUserResourceTracker.getUserRecord(this.uid);
        }

        public String toString() {
            return "{mResourceId=" + this.mResourceId + ", pid=" + this.pid + ", uid=" + this.uid + "}";
        }
    }

    static class RefcountedResourceArray<T extends IResource> {
        SparseArray<RefcountedResource<T>> mArray = new SparseArray<>();
        private final String mTypeName;

        public RefcountedResourceArray(String str) {
            this.mTypeName = str;
        }

        T getResourceOrThrow(int i) {
            return (T) getRefcountedResourceOrThrow(i).getResource();
        }

        RefcountedResource<T> getRefcountedResourceOrThrow(int i) {
            RefcountedResource<T> refcountedResource = this.mArray.get(i);
            if (refcountedResource == null) {
                throw new IllegalArgumentException(String.format("No such %s found for given id: %d", this.mTypeName, Integer.valueOf(i)));
            }
            return refcountedResource;
        }

        void put(int i, RefcountedResource<T> refcountedResource) {
            Preconditions.checkNotNull(refcountedResource, "Null resources cannot be added");
            this.mArray.put(i, refcountedResource);
        }

        void remove(int i) {
            this.mArray.remove(i);
        }

        public String toString() {
            return this.mArray.toString();
        }
    }

    private final class TransformRecord extends OwnedResourceRecord {
        private final IpSecConfig mConfig;
        private final EncapSocketRecord mSocket;
        private final SpiRecord mSpi;

        TransformRecord(int i, IpSecConfig ipSecConfig, SpiRecord spiRecord, EncapSocketRecord encapSocketRecord) {
            super(i);
            this.mConfig = ipSecConfig;
            this.mSpi = spiRecord;
            this.mSocket = encapSocketRecord;
            spiRecord.setOwnedByTransform();
        }

        public IpSecConfig getConfig() {
            return this.mConfig;
        }

        public SpiRecord getSpiRecord() {
            return this.mSpi;
        }

        public EncapSocketRecord getSocketRecord() {
            return this.mSocket;
        }

        @Override
        public void freeUnderlyingResources() {
            try {
                IpSecService.this.mSrvConfig.getNetdInstance().ipSecDeleteSecurityAssociation(this.mResourceId, this.mConfig.getSourceAddress(), this.mConfig.getDestinationAddress(), this.mSpi.getSpi(), this.mConfig.getMarkValue(), this.mConfig.getMarkMask());
            } catch (RemoteException | ServiceSpecificException e) {
                Log.e(IpSecService.TAG, "Failed to delete SA with ID: " + this.mResourceId, e);
            }
            getResourceTracker().give();
        }

        @Override
        public void invalidate() throws RemoteException {
            getUserRecord().removeTransformRecord(this.mResourceId);
        }

        @Override
        protected ResourceTracker getResourceTracker() {
            return getUserRecord().mTransformQuotaTracker;
        }

        @Override
        public String toString() {
            return "{super=" + super.toString() + ", mSocket=" + this.mSocket + ", mSpi.mResourceId=" + this.mSpi.mResourceId + ", mConfig=" + this.mConfig + "}";
        }
    }

    private final class SpiRecord extends OwnedResourceRecord {
        private final String mDestinationAddress;
        private boolean mOwnedByTransform;
        private final String mSourceAddress;
        private int mSpi;

        SpiRecord(int i, String str, String str2, int i2) {
            super(i);
            this.mOwnedByTransform = false;
            this.mSourceAddress = str;
            this.mDestinationAddress = str2;
            this.mSpi = i2;
        }

        @Override
        public void freeUnderlyingResources() {
            try {
                if (!this.mOwnedByTransform) {
                    IpSecService.this.mSrvConfig.getNetdInstance().ipSecDeleteSecurityAssociation(this.mResourceId, this.mSourceAddress, this.mDestinationAddress, this.mSpi, 0, 0);
                }
            } catch (ServiceSpecificException | RemoteException e) {
                Log.e(IpSecService.TAG, "Failed to delete SPI reservation with ID: " + this.mResourceId, e);
            }
            this.mSpi = 0;
            getResourceTracker().give();
        }

        public int getSpi() {
            return this.mSpi;
        }

        public String getDestinationAddress() {
            return this.mDestinationAddress;
        }

        public void setOwnedByTransform() {
            if (this.mOwnedByTransform) {
                throw new IllegalStateException("Cannot own an SPI twice!");
            }
            this.mOwnedByTransform = true;
        }

        public boolean getOwnedByTransform() {
            return this.mOwnedByTransform;
        }

        @Override
        public void invalidate() throws RemoteException {
            getUserRecord().removeSpiRecord(this.mResourceId);
        }

        @Override
        protected ResourceTracker getResourceTracker() {
            return getUserRecord().mSpiQuotaTracker;
        }

        @Override
        public String toString() {
            return "{super=" + super.toString() + ", mSpi=" + this.mSpi + ", mSourceAddress=" + this.mSourceAddress + ", mDestinationAddress=" + this.mDestinationAddress + ", mOwnedByTransform=" + this.mOwnedByTransform + "}";
        }
    }

    @VisibleForTesting
    int reserveNetId() {
        synchronized (this.mTunnelNetIds) {
            for (int i = 0; i < 1024; i++) {
                try {
                    int i2 = this.mNextTunnelNetIdIndex + TUN_INTF_NETID_START;
                    int i3 = this.mNextTunnelNetIdIndex + 1;
                    this.mNextTunnelNetIdIndex = i3;
                    if (i3 >= 1024) {
                        this.mNextTunnelNetIdIndex = 0;
                    }
                    if (!this.mTunnelNetIds.get(i2)) {
                        this.mTunnelNetIds.put(i2, true);
                        return i2;
                    }
                } catch (Throwable th) {
                    throw th;
                }
            }
            throw new IllegalStateException("No free netIds to allocate");
        }
    }

    @VisibleForTesting
    void releaseNetId(int i) {
        synchronized (this.mTunnelNetIds) {
            this.mTunnelNetIds.delete(i);
        }
    }

    private final class TunnelInterfaceRecord extends OwnedResourceRecord {
        private final int mIkey;
        private final String mInterfaceName;
        private final String mLocalAddress;
        private final int mOkey;
        private final String mRemoteAddress;
        private final Network mUnderlyingNetwork;

        TunnelInterfaceRecord(int i, String str, Network network, String str2, String str3, int i2, int i3) {
            super(i);
            this.mInterfaceName = str;
            this.mUnderlyingNetwork = network;
            this.mLocalAddress = str2;
            this.mRemoteAddress = str3;
            this.mIkey = i2;
            this.mOkey = i3;
        }

        @Override
        public void freeUnderlyingResources() {
            try {
                IpSecService.this.mSrvConfig.getNetdInstance().removeVirtualTunnelInterface(this.mInterfaceName);
                for (String str : IpSecService.WILDCARD_ADDRESSES) {
                    int[] iArr = IpSecService.DIRECTIONS;
                    int length = iArr.length;
                    for (int i = 0; i < length; i++) {
                        int i2 = iArr[i];
                        IpSecService.this.mSrvConfig.getNetdInstance().ipSecDeleteSecurityPolicy(0, i2, str, str, i2 == 0 ? this.mIkey : this.mOkey, -1);
                    }
                }
            } catch (ServiceSpecificException | RemoteException e) {
                Log.e(IpSecService.TAG, "Failed to delete VTI with interface name: " + this.mInterfaceName + " and id: " + this.mResourceId, e);
            }
            getResourceTracker().give();
            IpSecService.this.releaseNetId(this.mIkey);
            IpSecService.this.releaseNetId(this.mOkey);
        }

        public String getInterfaceName() {
            return this.mInterfaceName;
        }

        public Network getUnderlyingNetwork() {
            return this.mUnderlyingNetwork;
        }

        public String getLocalAddress() {
            return this.mLocalAddress;
        }

        public String getRemoteAddress() {
            return this.mRemoteAddress;
        }

        public int getIkey() {
            return this.mIkey;
        }

        public int getOkey() {
            return this.mOkey;
        }

        @Override
        protected ResourceTracker getResourceTracker() {
            return getUserRecord().mTunnelQuotaTracker;
        }

        @Override
        public void invalidate() {
            getUserRecord().removeTunnelInterfaceRecord(this.mResourceId);
        }

        @Override
        public String toString() {
            return "{super=" + super.toString() + ", mInterfaceName=" + this.mInterfaceName + ", mUnderlyingNetwork=" + this.mUnderlyingNetwork + ", mLocalAddress=" + this.mLocalAddress + ", mRemoteAddress=" + this.mRemoteAddress + ", mIkey=" + this.mIkey + ", mOkey=" + this.mOkey + "}";
        }
    }

    private final class EncapSocketRecord extends OwnedResourceRecord {
        private final int mPort;
        private FileDescriptor mSocket;

        EncapSocketRecord(int i, FileDescriptor fileDescriptor, int i2) {
            super(i);
            this.mSocket = fileDescriptor;
            this.mPort = i2;
        }

        @Override
        public void freeUnderlyingResources() {
            Log.d(IpSecService.TAG, "Closing port " + this.mPort);
            IoUtils.closeQuietly(this.mSocket);
            this.mSocket = null;
            getResourceTracker().give();
        }

        public int getPort() {
            return this.mPort;
        }

        public FileDescriptor getFileDescriptor() {
            return this.mSocket;
        }

        @Override
        protected ResourceTracker getResourceTracker() {
            return getUserRecord().mSocketQuotaTracker;
        }

        @Override
        public void invalidate() {
            getUserRecord().removeEncapSocketRecord(this.mResourceId);
        }

        @Override
        public String toString() {
            return "{super=" + super.toString() + ", mSocket=" + this.mSocket + ", mPort=" + this.mPort + "}";
        }
    }

    private IpSecService(Context context) {
        this(context, IpSecServiceConfiguration.GETSRVINSTANCE);
    }

    static IpSecService create(Context context) throws InterruptedException {
        IpSecService ipSecService = new IpSecService(context);
        ipSecService.connectNativeNetdService();
        return ipSecService;
    }

    private AppOpsManager getAppOpsManager() {
        AppOpsManager appOpsManager = (AppOpsManager) this.mContext.getSystemService("appops");
        if (appOpsManager == null) {
            throw new RuntimeException("System Server couldn't get AppOps");
        }
        return appOpsManager;
    }

    @VisibleForTesting
    public IpSecService(Context context, IpSecServiceConfiguration ipSecServiceConfiguration) {
        this(context, ipSecServiceConfiguration, new UidFdTagger() {
            @Override
            public final void tag(FileDescriptor fileDescriptor, int i) throws IOException {
                IpSecService.lambda$new$0(fileDescriptor, i);
            }
        });
    }

    static void lambda$new$0(FileDescriptor fileDescriptor, int i) throws IOException {
        try {
            TrafficStats.setThreadStatsUid(i);
            TrafficStats.tagFileDescriptor(fileDescriptor);
        } finally {
            TrafficStats.clearThreadStatsUid();
        }
    }

    @VisibleForTesting
    public IpSecService(Context context, IpSecServiceConfiguration ipSecServiceConfiguration, UidFdTagger uidFdTagger) {
        this.mNextResourceId = 1;
        this.mUserResourceTracker = new UserResourceTracker();
        this.mTunnelNetIds = new SparseBooleanArray();
        this.mNextTunnelNetIdIndex = 0;
        this.mContext = context;
        this.mSrvConfig = ipSecServiceConfiguration;
        this.mUidFdTagger = uidFdTagger;
    }

    public void systemReady() {
        if (isNetdAlive()) {
            Slog.d(TAG, "IpSecService is ready");
        } else {
            Slog.wtf(TAG, "IpSecService not ready: failed to connect to NetD Native Service!");
        }
    }

    private void connectNativeNetdService() {
        new Thread() {
            @Override
            public void run() {
                synchronized (IpSecService.this) {
                    NetdService.get(5000L);
                }
            }
        }.start();
    }

    synchronized boolean isNetdAlive() {
        try {
            INetd netdInstance = this.mSrvConfig.getNetdInstance();
            if (netdInstance == null) {
                return false;
            }
            return netdInstance.isAlive();
        } catch (RemoteException e) {
            return false;
        }
    }

    private static void checkInetAddress(String str) {
        if (TextUtils.isEmpty(str)) {
            throw new IllegalArgumentException("Unspecified address");
        }
        if (NetworkUtils.numericToInetAddress(str).isAnyLocalAddress()) {
            throw new IllegalArgumentException("Inappropriate wildcard address: " + str);
        }
    }

    private static void checkDirection(int i) {
        switch (i) {
            case 0:
            case 1:
                return;
            default:
                throw new IllegalArgumentException("Invalid Direction: " + i);
        }
    }

    public synchronized IpSecSpiResponse allocateSecurityParameterIndex(String str, int i, IBinder iBinder) throws RemoteException {
        int iIpSecAllocateSpi;
        checkInetAddress(str);
        if (i > 0 && i < 256) {
            throw new IllegalArgumentException("ESP SPI must not be in the range of 0-255.");
        }
        Preconditions.checkNotNull(iBinder, "Null Binder passed to allocateSecurityParameterIndex");
        UserRecord userRecord = this.mUserResourceTracker.getUserRecord(Binder.getCallingUid());
        int i2 = this.mNextResourceId;
        this.mNextResourceId = i2 + 1;
        try {
            try {
                if (!userRecord.mSpiQuotaTracker.isAvailable()) {
                    return new IpSecSpiResponse(1, -1, 0);
                }
                iIpSecAllocateSpi = this.mSrvConfig.getNetdInstance().ipSecAllocateSpi(i2, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, str, i);
                try {
                    Log.d(TAG, "Allocated SPI " + iIpSecAllocateSpi);
                    userRecord.mSpiRecords.put(i2, new RefcountedResource<>(new SpiRecord(i2, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, str, iIpSecAllocateSpi), iBinder, new RefcountedResource[0]));
                    return new IpSecSpiResponse(0, i2, iIpSecAllocateSpi);
                } catch (ServiceSpecificException e) {
                    e = e;
                    if (e.errorCode == OsConstants.ENOENT) {
                        return new IpSecSpiResponse(2, -1, iIpSecAllocateSpi);
                    }
                    throw e;
                }
            } catch (ServiceSpecificException e2) {
                e = e2;
                iIpSecAllocateSpi = 0;
            }
        } catch (RemoteException e3) {
            throw e3.rethrowFromSystemServer();
        }
    }

    private void releaseResource(RefcountedResourceArray refcountedResourceArray, int i) throws RemoteException {
        refcountedResourceArray.getRefcountedResourceOrThrow(i).userRelease();
    }

    public synchronized void releaseSecurityParameterIndex(int i) throws RemoteException {
        releaseResource(this.mUserResourceTracker.getUserRecord(Binder.getCallingUid()).mSpiRecords, i);
    }

    private int bindToRandomPort(FileDescriptor fileDescriptor) throws IOException {
        for (int i = 10; i > 0; i--) {
            try {
                FileDescriptor fileDescriptorSocket = Os.socket(OsConstants.AF_INET, OsConstants.SOCK_DGRAM, OsConstants.IPPROTO_UDP);
                Os.bind(fileDescriptorSocket, INADDR_ANY, 0);
                int port = ((InetSocketAddress) Os.getsockname(fileDescriptorSocket)).getPort();
                Os.close(fileDescriptorSocket);
                Log.v(TAG, "Binding to port " + port);
                Os.bind(fileDescriptor, INADDR_ANY, port);
                return port;
            } catch (ErrnoException e) {
                if (e.errno != OsConstants.EADDRINUSE) {
                    throw e.rethrowAsIOException();
                }
            }
        }
        throw new IOException("Failed 10 attempts to bind to a port");
    }

    public synchronized IpSecUdpEncapResponse openUdpEncapsulationSocket(int i, IBinder iBinder) throws RemoteException {
        FileDescriptor fileDescriptorSocket;
        if (i != 0 && (i < 1024 || i > 65535)) {
            throw new IllegalArgumentException("Specified port number must be a valid non-reserved UDP port");
        }
        Preconditions.checkNotNull(iBinder, "Null Binder passed to openUdpEncapsulationSocket");
        int callingUid = Binder.getCallingUid();
        UserRecord userRecord = this.mUserResourceTracker.getUserRecord(callingUid);
        int i2 = this.mNextResourceId;
        this.mNextResourceId = i2 + 1;
        try {
            if (!userRecord.mSocketQuotaTracker.isAvailable()) {
                return new IpSecUdpEncapResponse(1);
            }
            fileDescriptorSocket = Os.socket(OsConstants.AF_INET, OsConstants.SOCK_DGRAM, OsConstants.IPPROTO_UDP);
            try {
                this.mUidFdTagger.tag(fileDescriptorSocket, callingUid);
                Os.setsockoptInt(fileDescriptorSocket, OsConstants.IPPROTO_UDP, OsConstants.UDP_ENCAP, OsConstants.UDP_ENCAP_ESPINUDP);
                this.mSrvConfig.getNetdInstance().ipSecSetEncapSocketOwner(fileDescriptorSocket, callingUid);
                if (i != 0) {
                    Log.v(TAG, "Binding to port " + i);
                    Os.bind(fileDescriptorSocket, INADDR_ANY, i);
                } else {
                    i = bindToRandomPort(fileDescriptorSocket);
                }
                userRecord.mEncapSocketRecords.put(i2, new RefcountedResource<>(new EncapSocketRecord(i2, fileDescriptorSocket, i), iBinder, new RefcountedResource[0]));
                return new IpSecUdpEncapResponse(0, i2, i, fileDescriptorSocket);
            } catch (ErrnoException | IOException e) {
                IoUtils.closeQuietly(fileDescriptorSocket);
                return new IpSecUdpEncapResponse(1);
            }
        } catch (ErrnoException | IOException e2) {
            fileDescriptorSocket = null;
        }
    }

    public synchronized void closeUdpEncapsulationSocket(int i) throws RemoteException {
        releaseResource(this.mUserResourceTracker.getUserRecord(Binder.getCallingUid()).mEncapSocketRecords, i);
    }

    public synchronized IpSecTunnelInterfaceResponse createTunnelInterface(String str, String str2, Network network, IBinder iBinder, String str3) {
        int i;
        int i2;
        int i3;
        int i4;
        enforceTunnelPermissions(str3);
        Preconditions.checkNotNull(iBinder, "Null Binder passed to createTunnelInterface");
        Preconditions.checkNotNull(network, "No underlying network was specified");
        checkInetAddress(str);
        checkInetAddress(str2);
        UserRecord userRecord = this.mUserResourceTracker.getUserRecord(Binder.getCallingUid());
        int i5 = 1;
        if (!userRecord.mTunnelQuotaTracker.isAvailable()) {
            return new IpSecTunnelInterfaceResponse(1);
        }
        int i6 = this.mNextResourceId;
        this.mNextResourceId = i6 + 1;
        int iReserveNetId = reserveNetId();
        int iReserveNetId2 = reserveNetId();
        int i7 = 0;
        String str4 = String.format("%s%d", INetd.IPSEC_INTERFACE_PREFIX, Integer.valueOf(i6));
        try {
            this.mSrvConfig.getNetdInstance().addVirtualTunnelInterface(str4, str, str2, iReserveNetId, iReserveNetId2);
            String[] strArr = WILDCARD_ADDRESSES;
            int length = strArr.length;
            int i8 = 0;
            while (i8 < length) {
                String str5 = strArr[i8];
                int[] iArr = DIRECTIONS;
                int length2 = iArr.length;
                int i9 = i7;
                while (i9 < length2) {
                    int i10 = iArr[i9];
                    this.mSrvConfig.getNetdInstance().ipSecAddSecurityPolicy(0, i10, str5, str5, 0, i10 == i5 ? iReserveNetId2 : iReserveNetId, -1);
                    i9++;
                    length2 = length2;
                    iArr = iArr;
                    i5 = 1;
                }
                i8++;
                i5 = 1;
                i7 = 0;
            }
            i = iReserveNetId2;
            i2 = iReserveNetId;
        } catch (RemoteException e) {
            e = e;
            i3 = iReserveNetId2;
            i4 = iReserveNetId;
        } catch (Throwable th) {
            th = th;
            i = iReserveNetId2;
            i2 = iReserveNetId;
        }
        try {
            userRecord.mTunnelInterfaceRecords.put(i6, new RefcountedResource<>(new TunnelInterfaceRecord(i6, str4, network, str, str2, iReserveNetId, i), iBinder, new RefcountedResource[0]));
            return new IpSecTunnelInterfaceResponse(0, i6, str4);
        } catch (RemoteException e2) {
            e = e2;
            i3 = i;
            i4 = i2;
            releaseNetId(i4);
            releaseNetId(i3);
            throw e.rethrowFromSystemServer();
        } catch (Throwable th2) {
            th = th2;
            releaseNetId(i2);
            releaseNetId(i);
            throw th;
        }
    }

    public synchronized void addAddressToTunnelInterface(int i, LinkAddress linkAddress, String str) {
        enforceTunnelPermissions(str);
        try {
            this.mSrvConfig.getNetdInstance().interfaceAddAddress(((TunnelInterfaceRecord) this.mUserResourceTracker.getUserRecord(Binder.getCallingUid()).mTunnelInterfaceRecords.getResourceOrThrow(i)).mInterfaceName, linkAddress.getAddress().getHostAddress(), linkAddress.getPrefixLength());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public synchronized void removeAddressFromTunnelInterface(int i, LinkAddress linkAddress, String str) {
        enforceTunnelPermissions(str);
        try {
            this.mSrvConfig.getNetdInstance().interfaceDelAddress(((TunnelInterfaceRecord) this.mUserResourceTracker.getUserRecord(Binder.getCallingUid()).mTunnelInterfaceRecords.getResourceOrThrow(i)).mInterfaceName, linkAddress.getAddress().getHostAddress(), linkAddress.getPrefixLength());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public synchronized void deleteTunnelInterface(int i, String str) throws RemoteException {
        enforceTunnelPermissions(str);
        releaseResource(this.mUserResourceTracker.getUserRecord(Binder.getCallingUid()).mTunnelInterfaceRecords, i);
    }

    @VisibleForTesting
    void validateAlgorithms(IpSecConfig ipSecConfig) throws IllegalArgumentException {
        IpSecAlgorithm authentication = ipSecConfig.getAuthentication();
        IpSecAlgorithm encryption = ipSecConfig.getEncryption();
        IpSecAlgorithm authenticatedEncryption = ipSecConfig.getAuthenticatedEncryption();
        boolean z = true;
        Preconditions.checkArgument((authenticatedEncryption == null && encryption == null && authentication == null) ? false : true, "No Encryption or Authentication algorithms specified");
        Preconditions.checkArgument(authentication == null || authentication.isAuthentication(), "Unsupported algorithm for Authentication");
        Preconditions.checkArgument(encryption == null || encryption.isEncryption(), "Unsupported algorithm for Encryption");
        Preconditions.checkArgument(authenticatedEncryption == null || authenticatedEncryption.isAead(), "Unsupported algorithm for Authenticated Encryption");
        if (authenticatedEncryption != null && (authentication != null || encryption != null)) {
            z = false;
        }
        Preconditions.checkArgument(z, "Authenticated Encryption is mutually exclusive with other Authentication or Encryption algorithms");
    }

    private void checkIpSecConfig(IpSecConfig ipSecConfig) {
        UserRecord userRecord = this.mUserResourceTracker.getUserRecord(Binder.getCallingUid());
        switch (ipSecConfig.getEncapType()) {
            case 0:
                break;
            case 1:
            case 2:
                userRecord.mEncapSocketRecords.getResourceOrThrow(ipSecConfig.getEncapSocketResourceId());
                int encapRemotePort = ipSecConfig.getEncapRemotePort();
                if (encapRemotePort <= 0 || encapRemotePort > 65535) {
                    throw new IllegalArgumentException("Invalid remote UDP port: " + encapRemotePort);
                }
                break;
            default:
                throw new IllegalArgumentException("Invalid Encap Type: " + ipSecConfig.getEncapType());
        }
        validateAlgorithms(ipSecConfig);
        SpiRecord spiRecord = (SpiRecord) userRecord.mSpiRecords.getResourceOrThrow(ipSecConfig.getSpiResourceId());
        if (spiRecord.getOwnedByTransform()) {
            throw new IllegalStateException("SPI already in use; cannot be used in new Transforms");
        }
        if (TextUtils.isEmpty(ipSecConfig.getDestinationAddress())) {
            ipSecConfig.setDestinationAddress(spiRecord.getDestinationAddress());
        }
        if (!ipSecConfig.getDestinationAddress().equals(spiRecord.getDestinationAddress())) {
            throw new IllegalArgumentException("Mismatched remote addresseses.");
        }
        checkInetAddress(ipSecConfig.getDestinationAddress());
        checkInetAddress(ipSecConfig.getSourceAddress());
        switch (ipSecConfig.getMode()) {
            case 0:
            case 1:
                return;
            default:
                throw new IllegalArgumentException("Invalid IpSecTransform.mode: " + ipSecConfig.getMode());
        }
    }

    private void enforceTunnelPermissions(String str) {
        Preconditions.checkNotNull(str, "Null calling package cannot create IpSec tunnels");
        int iNoteOp = getAppOpsManager().noteOp(75, Binder.getCallingUid(), str);
        if (iNoteOp != 0) {
            if (iNoteOp == 3) {
                this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_IPSEC_TUNNELS", TAG);
                return;
            }
            throw new SecurityException("Request to ignore AppOps for non-legacy API");
        }
    }

    private void createOrUpdateTransform(IpSecConfig ipSecConfig, int i, SpiRecord spiRecord, EncapSocketRecord encapSocketRecord) throws RemoteException {
        int port;
        int encapRemotePort;
        String name;
        int encapType = ipSecConfig.getEncapType();
        if (encapType == 0) {
            port = 0;
            encapRemotePort = 0;
        } else {
            port = encapSocketRecord.getPort();
            encapRemotePort = ipSecConfig.getEncapRemotePort();
        }
        IpSecAlgorithm authentication = ipSecConfig.getAuthentication();
        IpSecAlgorithm encryption = ipSecConfig.getEncryption();
        IpSecAlgorithm authenticatedEncryption = ipSecConfig.getAuthenticatedEncryption();
        if (encryption == null) {
            name = authenticatedEncryption == null ? "ecb(cipher_null)" : BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        } else {
            name = encryption.getName();
        }
        this.mSrvConfig.getNetdInstance().ipSecAddSecurityAssociation(i, ipSecConfig.getMode(), ipSecConfig.getSourceAddress(), ipSecConfig.getDestinationAddress(), ipSecConfig.getNetwork() != null ? ipSecConfig.getNetwork().netId : 0, spiRecord.getSpi(), ipSecConfig.getMarkValue(), ipSecConfig.getMarkMask(), authentication != null ? authentication.getName() : BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, authentication != null ? authentication.getKey() : new byte[0], authentication != null ? authentication.getTruncationLengthBits() : 0, name, encryption != null ? encryption.getKey() : new byte[0], encryption != null ? encryption.getTruncationLengthBits() : 0, authenticatedEncryption != null ? authenticatedEncryption.getName() : BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, authenticatedEncryption != null ? authenticatedEncryption.getKey() : new byte[0], authenticatedEncryption != null ? authenticatedEncryption.getTruncationLengthBits() : 0, encapType, port, encapRemotePort);
    }

    public synchronized IpSecTransformResponse createTransform(IpSecConfig ipSecConfig, IBinder iBinder, String str) throws RemoteException {
        Preconditions.checkNotNull(ipSecConfig);
        if (ipSecConfig.getMode() == 1) {
            enforceTunnelPermissions(str);
        }
        checkIpSecConfig(ipSecConfig);
        Preconditions.checkNotNull(iBinder, "Null Binder passed to createTransform");
        int i = this.mNextResourceId;
        this.mNextResourceId = i + 1;
        UserRecord userRecord = this.mUserResourceTracker.getUserRecord(Binder.getCallingUid());
        ArrayList arrayList = new ArrayList();
        if (!userRecord.mTransformQuotaTracker.isAvailable()) {
            return new IpSecTransformResponse(1);
        }
        EncapSocketRecord encapSocketRecord = null;
        if (ipSecConfig.getEncapType() != 0) {
            RefcountedResource<T> refcountedResourceOrThrow = userRecord.mEncapSocketRecords.getRefcountedResourceOrThrow(ipSecConfig.getEncapSocketResourceId());
            arrayList.add(refcountedResourceOrThrow);
            encapSocketRecord = (EncapSocketRecord) refcountedResourceOrThrow.getResource();
        }
        EncapSocketRecord encapSocketRecord2 = encapSocketRecord;
        RefcountedResource<T> refcountedResourceOrThrow2 = userRecord.mSpiRecords.getRefcountedResourceOrThrow(ipSecConfig.getSpiResourceId());
        arrayList.add(refcountedResourceOrThrow2);
        SpiRecord spiRecord = (SpiRecord) refcountedResourceOrThrow2.getResource();
        createOrUpdateTransform(ipSecConfig, i, spiRecord, encapSocketRecord2);
        userRecord.mTransformRecords.put(i, new RefcountedResource<>(new TransformRecord(i, ipSecConfig, spiRecord, encapSocketRecord2), iBinder, (RefcountedResource[]) arrayList.toArray(new RefcountedResource[arrayList.size()])));
        return new IpSecTransformResponse(0, i);
    }

    public synchronized void deleteTransform(int i) throws RemoteException {
        releaseResource(this.mUserResourceTracker.getUserRecord(Binder.getCallingUid()).mTransformRecords, i);
    }

    public synchronized void applyTransportModeTransform(ParcelFileDescriptor parcelFileDescriptor, int i, int i2) throws RemoteException {
        UserRecord userRecord = this.mUserResourceTracker.getUserRecord(Binder.getCallingUid());
        checkDirection(i);
        TransformRecord transformRecord = (TransformRecord) userRecord.mTransformRecords.getResourceOrThrow(i2);
        if (transformRecord.pid != getCallingPid() || transformRecord.uid != getCallingUid()) {
            throw new SecurityException("Only the owner of an IpSec Transform may apply it!");
        }
        IpSecConfig config = transformRecord.getConfig();
        Preconditions.checkArgument(config.getMode() == 0, "Transform mode was not Transport mode; cannot be applied to a socket");
        this.mSrvConfig.getNetdInstance().ipSecApplyTransportModeTransform(parcelFileDescriptor.getFileDescriptor(), i2, i, config.getSourceAddress(), config.getDestinationAddress(), transformRecord.getSpiRecord().getSpi());
    }

    public synchronized void removeTransportModeTransforms(ParcelFileDescriptor parcelFileDescriptor) throws RemoteException {
        this.mSrvConfig.getNetdInstance().ipSecRemoveTransportModeTransform(parcelFileDescriptor.getFileDescriptor());
    }

    public synchronized void applyTunnelModeTransform(int i, int i2, int i3, String str) throws RemoteException {
        int okey;
        enforceTunnelPermissions(str);
        checkDirection(i2);
        UserRecord userRecord = this.mUserResourceTracker.getUserRecord(Binder.getCallingUid());
        TransformRecord transformRecord = (TransformRecord) userRecord.mTransformRecords.getResourceOrThrow(i3);
        TunnelInterfaceRecord tunnelInterfaceRecord = (TunnelInterfaceRecord) userRecord.mTunnelInterfaceRecords.getResourceOrThrow(i);
        IpSecConfig config = transformRecord.getConfig();
        Preconditions.checkArgument(config.getMode() == 1, "Transform mode was not Tunnel mode; cannot be applied to a tunnel interface");
        EncapSocketRecord encapSocketRecord = null;
        if (config.getEncapType() != 0) {
            encapSocketRecord = (EncapSocketRecord) userRecord.mEncapSocketRecords.getResourceOrThrow(config.getEncapSocketResourceId());
        }
        EncapSocketRecord encapSocketRecord2 = encapSocketRecord;
        SpiRecord spiRecord = (SpiRecord) userRecord.mSpiRecords.getResourceOrThrow(config.getSpiResourceId());
        if (i2 == 0) {
            okey = tunnelInterfaceRecord.getIkey();
        } else {
            okey = tunnelInterfaceRecord.getOkey();
        }
        int i4 = okey;
        try {
            config.setMarkValue(i4);
            config.setMarkMask(-1);
            if (i2 == 1) {
                config.setNetwork(tunnelInterfaceRecord.getUnderlyingNetwork());
                String[] strArr = WILDCARD_ADDRESSES;
                int length = strArr.length;
                int i5 = 0;
                while (i5 < length) {
                    String str2 = strArr[i5];
                    this.mSrvConfig.getNetdInstance().ipSecUpdateSecurityPolicy(0, i2, str2, str2, transformRecord.getSpiRecord().getSpi(), i4, -1);
                    i5++;
                    length = length;
                    strArr = strArr;
                }
            }
            createOrUpdateTransform(config, i3, spiRecord, encapSocketRecord2);
        } catch (ServiceSpecificException e) {
            if (e.errorCode == OsConstants.EINVAL) {
                throw new IllegalArgumentException(e.toString());
            }
            throw e;
        }
    }

    protected synchronized void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.DUMP", TAG);
        printWriter.println("IpSecService dump:");
        StringBuilder sb = new StringBuilder();
        sb.append("NetdNativeService Connection: ");
        sb.append(isNetdAlive() ? "alive" : "dead");
        printWriter.println(sb.toString());
        printWriter.println();
        printWriter.println("mUserResourceTracker:");
        printWriter.println(this.mUserResourceTracker);
    }
}
