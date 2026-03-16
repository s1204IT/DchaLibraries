package android.app.backup;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import com.android.internal.backup.IBackupTransport;

public class BackupTransport {
    public static final int AGENT_ERROR = -1003;
    public static final int AGENT_UNKNOWN = -1004;
    public static final int NO_MORE_DATA = -1;
    public static final int TRANSPORT_ERROR = -1000;
    public static final int TRANSPORT_NOT_INITIALIZED = -1001;
    public static final int TRANSPORT_OK = 0;
    public static final int TRANSPORT_PACKAGE_REJECTED = -1002;
    IBackupTransport mBinderImpl = new TransportImpl();

    public IBinder getBinder() {
        return this.mBinderImpl.asBinder();
    }

    public String name() {
        throw new UnsupportedOperationException("Transport name() not implemented");
    }

    public Intent configurationIntent() {
        return null;
    }

    public String currentDestinationString() {
        throw new UnsupportedOperationException("Transport currentDestinationString() not implemented");
    }

    public Intent dataManagementIntent() {
        return null;
    }

    public String dataManagementLabel() {
        throw new UnsupportedOperationException("Transport dataManagementLabel() not implemented");
    }

    public String transportDirName() {
        throw new UnsupportedOperationException("Transport transportDirName() not implemented");
    }

    public int initializeDevice() {
        return -1000;
    }

    public int clearBackupData(PackageInfo packageInfo) {
        return -1000;
    }

    public int finishBackup() {
        return -1000;
    }

    public long requestBackupTime() {
        return 0L;
    }

    public int performBackup(PackageInfo packageInfo, ParcelFileDescriptor inFd) {
        return -1000;
    }

    public RestoreSet[] getAvailableRestoreSets() {
        return null;
    }

    public long getCurrentRestoreSet() {
        return 0L;
    }

    public int startRestore(long token, PackageInfo[] packages) {
        return -1000;
    }

    public RestoreDescription nextRestorePackage() {
        return null;
    }

    public int getRestoreData(ParcelFileDescriptor outFd) {
        return -1000;
    }

    public void finishRestore() {
        throw new UnsupportedOperationException("Transport finishRestore() not implemented");
    }

    public long requestFullBackupTime() {
        return 0L;
    }

    public int performFullBackup(PackageInfo targetPackage, ParcelFileDescriptor socket) {
        return TRANSPORT_PACKAGE_REJECTED;
    }

    public int sendBackupData(int numBytes) {
        return -1000;
    }

    public void cancelFullBackup() {
        throw new UnsupportedOperationException("Transport cancelFullBackup() not implemented");
    }

    public int getNextFullRestoreDataChunk(ParcelFileDescriptor socket) {
        return 0;
    }

    public int abortFullRestore() {
        return 0;
    }

    class TransportImpl extends IBackupTransport.Stub {
        TransportImpl() {
        }

        @Override
        public String name() throws RemoteException {
            return BackupTransport.this.name();
        }

        @Override
        public Intent configurationIntent() throws RemoteException {
            return BackupTransport.this.configurationIntent();
        }

        @Override
        public String currentDestinationString() throws RemoteException {
            return BackupTransport.this.currentDestinationString();
        }

        @Override
        public Intent dataManagementIntent() {
            return BackupTransport.this.dataManagementIntent();
        }

        @Override
        public String dataManagementLabel() {
            return BackupTransport.this.dataManagementLabel();
        }

        @Override
        public String transportDirName() throws RemoteException {
            return BackupTransport.this.transportDirName();
        }

        @Override
        public long requestBackupTime() throws RemoteException {
            return BackupTransport.this.requestBackupTime();
        }

        @Override
        public int initializeDevice() throws RemoteException {
            return BackupTransport.this.initializeDevice();
        }

        @Override
        public int performBackup(PackageInfo packageInfo, ParcelFileDescriptor inFd) throws RemoteException {
            return BackupTransport.this.performBackup(packageInfo, inFd);
        }

        @Override
        public int clearBackupData(PackageInfo packageInfo) throws RemoteException {
            return BackupTransport.this.clearBackupData(packageInfo);
        }

        @Override
        public int finishBackup() throws RemoteException {
            return BackupTransport.this.finishBackup();
        }

        @Override
        public RestoreSet[] getAvailableRestoreSets() throws RemoteException {
            return BackupTransport.this.getAvailableRestoreSets();
        }

        @Override
        public long getCurrentRestoreSet() throws RemoteException {
            return BackupTransport.this.getCurrentRestoreSet();
        }

        @Override
        public int startRestore(long token, PackageInfo[] packages) throws RemoteException {
            return BackupTransport.this.startRestore(token, packages);
        }

        @Override
        public RestoreDescription nextRestorePackage() throws RemoteException {
            return BackupTransport.this.nextRestorePackage();
        }

        @Override
        public int getRestoreData(ParcelFileDescriptor outFd) throws RemoteException {
            return BackupTransport.this.getRestoreData(outFd);
        }

        @Override
        public void finishRestore() throws RemoteException {
            BackupTransport.this.finishRestore();
        }

        @Override
        public long requestFullBackupTime() throws RemoteException {
            return BackupTransport.this.requestFullBackupTime();
        }

        @Override
        public int performFullBackup(PackageInfo targetPackage, ParcelFileDescriptor socket) throws RemoteException {
            return BackupTransport.this.performFullBackup(targetPackage, socket);
        }

        @Override
        public int sendBackupData(int numBytes) throws RemoteException {
            return BackupTransport.this.sendBackupData(numBytes);
        }

        @Override
        public void cancelFullBackup() throws RemoteException {
            BackupTransport.this.cancelFullBackup();
        }

        @Override
        public int getNextFullRestoreDataChunk(ParcelFileDescriptor socket) {
            return BackupTransport.this.getNextFullRestoreDataChunk(socket);
        }

        @Override
        public int abortFullRestore() {
            return BackupTransport.this.abortFullRestore();
        }
    }
}
