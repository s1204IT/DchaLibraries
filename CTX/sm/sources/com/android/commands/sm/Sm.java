package com.android.commands.sm;

import android.os.IVoldTaskListener;
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.storage.DiskInfo;
import android.os.storage.IStorageManager;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.util.Log;
import java.util.concurrent.CompletableFuture;

public final class Sm {
    private static final String TAG = "Sm";
    private String[] mArgs;
    private String mCurArgData;
    private int mNextArg;
    IStorageManager mSm;

    public static void main(String[] strArr) {
        int i;
        try {
            new Sm().run(strArr);
            i = 1;
        } catch (Exception e) {
            if (e instanceof IllegalArgumentException) {
                showUsage();
                System.exit(1);
            }
            Log.e(TAG, "Error", e);
            System.err.println("Error: " + e);
            i = 0;
        }
        System.exit(i ^ 1);
    }

    public void run(String[] strArr) throws Exception {
        if (strArr.length < 1) {
            throw new IllegalArgumentException();
        }
        this.mSm = IStorageManager.Stub.asInterface(ServiceManager.getService("mount"));
        if (this.mSm == null) {
            throw new RemoteException("Failed to find running mount service");
        }
        this.mArgs = strArr;
        String str = strArr[0];
        this.mNextArg = 1;
        if ("list-disks".equals(str)) {
            runListDisks();
            return;
        }
        if ("list-volumes".equals(str)) {
            runListVolumes();
            return;
        }
        if ("has-adoptable".equals(str)) {
            runHasAdoptable();
            return;
        }
        if ("get-primary-storage-uuid".equals(str)) {
            runGetPrimaryStorageUuid();
            return;
        }
        if ("set-force-adoptable".equals(str)) {
            runSetForceAdoptable();
            return;
        }
        if ("set-sdcardfs".equals(str)) {
            runSetSdcardfs();
            return;
        }
        if ("partition".equals(str)) {
            runPartition();
            return;
        }
        if ("mount".equals(str)) {
            runMount();
            return;
        }
        if ("unmount".equals(str)) {
            runUnmount();
            return;
        }
        if ("format".equals(str)) {
            runFormat();
            return;
        }
        if ("benchmark".equals(str)) {
            runBenchmark();
            return;
        }
        if ("forget".equals(str)) {
            runForget();
            return;
        }
        if ("set-emulate-fbe".equals(str)) {
            runSetEmulateFbe();
            return;
        }
        if ("get-fbe-mode".equals(str)) {
            runGetFbeMode();
            return;
        }
        if ("idle-maint".equals(str)) {
            runIdleMaint();
        } else if ("fstrim".equals(str)) {
            runFstrim();
        } else {
            if ("set-virtual-disk".equals(str)) {
                runSetVirtualDisk();
                return;
            }
            throw new IllegalArgumentException();
        }
    }

    public void runListDisks() throws RemoteException {
        boolean zEquals = "adoptable".equals(nextArg());
        for (DiskInfo diskInfo : this.mSm.getDisks()) {
            if (!zEquals || diskInfo.isAdoptable()) {
                System.out.println(diskInfo.getId());
            }
        }
    }

    public void runListVolumes() throws RemoteException {
        String strNextArg = nextArg();
        int i = "public".equals(strNextArg) ? 0 : "private".equals(strNextArg) ? 1 : "emulated".equals(strNextArg) ? 2 : -1;
        for (VolumeInfo volumeInfo : this.mSm.getVolumes(0)) {
            if (i == -1 || i == volumeInfo.getType()) {
                System.out.println(volumeInfo.getId() + " " + VolumeInfo.getEnvironmentForState(volumeInfo.getState()) + " " + volumeInfo.getFsUuid());
            }
        }
    }

    public void runHasAdoptable() {
        System.out.println(StorageManager.hasAdoptable());
    }

    public void runGetPrimaryStorageUuid() throws RemoteException {
        System.out.println(this.mSm.getPrimaryStorageUuid());
    }

    public void runSetForceAdoptable() throws RemoteException {
        byte b;
        String strNextArg = nextArg();
        int iHashCode = strNextArg.hashCode();
        if (iHashCode != 3551) {
            if (iHashCode != 109935) {
                if (iHashCode != 3569038) {
                    if (iHashCode != 97196323) {
                        b = (iHashCode == 1544803905 && strNextArg.equals("default")) ? (byte) 3 : (byte) -1;
                    } else if (strNextArg.equals("false")) {
                        b = 4;
                    }
                } else if (strNextArg.equals("true")) {
                    b = 1;
                }
            } else if (strNextArg.equals("off")) {
                b = 2;
            }
        } else if (strNextArg.equals("on")) {
            b = 0;
        }
        switch (b) {
            case 0:
            case 1:
                this.mSm.setDebugFlags(1, 3);
                break;
            case 2:
                this.mSm.setDebugFlags(2, 3);
                break;
            case 3:
            case 4:
                this.mSm.setDebugFlags(0, 3);
                break;
        }
    }

    public void runSetSdcardfs() throws RemoteException {
        byte b;
        String strNextArg = nextArg();
        int iHashCode = strNextArg.hashCode();
        if (iHashCode != 3551) {
            if (iHashCode != 109935) {
                b = (iHashCode == 1544803905 && strNextArg.equals("default")) ? (byte) 2 : (byte) -1;
            } else if (strNextArg.equals("off")) {
                b = 1;
            }
        } else if (strNextArg.equals("on")) {
            b = 0;
        }
        switch (b) {
            case 0:
                this.mSm.setDebugFlags(8, 24);
                break;
            case 1:
                this.mSm.setDebugFlags(16, 24);
                break;
            case 2:
                this.mSm.setDebugFlags(0, 24);
                break;
        }
    }

    public void runSetEmulateFbe() throws RemoteException {
        this.mSm.setDebugFlags(Boolean.parseBoolean(nextArg()) ? 4 : 0, 4);
    }

    public void runGetFbeMode() {
        if (StorageManager.isFileEncryptedNativeOnly()) {
            System.out.println("native");
        } else if (StorageManager.isFileEncryptedEmulatedOnly()) {
            System.out.println("emulated");
        } else {
            System.out.println("none");
        }
    }

    public void runPartition() throws RemoteException {
        String strNextArg = nextArg();
        String strNextArg2 = nextArg();
        if ("public".equals(strNextArg2)) {
            this.mSm.partitionPublic(strNextArg);
            return;
        }
        if ("private".equals(strNextArg2)) {
            this.mSm.partitionPrivate(strNextArg);
            return;
        }
        if ("mixed".equals(strNextArg2)) {
            this.mSm.partitionMixed(strNextArg, Integer.parseInt(nextArg()));
        } else {
            throw new IllegalArgumentException("Unsupported partition type " + strNextArg2);
        }
    }

    public void runMount() throws RemoteException {
        this.mSm.mount(nextArg());
    }

    public void runUnmount() throws RemoteException {
        this.mSm.unmount(nextArg());
    }

    public void runFormat() throws RemoteException {
        this.mSm.format(nextArg());
    }

    public void runBenchmark() throws Exception {
        String strNextArg = nextArg();
        final CompletableFuture completableFuture = new CompletableFuture();
        this.mSm.benchmark(strNextArg, new IVoldTaskListener.Stub() {
            public void onStatus(int i, PersistableBundle persistableBundle) {
            }

            public void onFinished(int i, PersistableBundle persistableBundle) {
                persistableBundle.size();
                completableFuture.complete(persistableBundle);
            }
        });
        System.out.println(completableFuture.get());
    }

    public void runForget() throws RemoteException {
        String strNextArg = nextArg();
        if ("all".equals(strNextArg)) {
            this.mSm.forgetAllVolumes();
        } else {
            this.mSm.forgetVolume(strNextArg);
        }
    }

    public void runFstrim() throws Exception {
        final CompletableFuture completableFuture = new CompletableFuture();
        this.mSm.fstrim(0, new IVoldTaskListener.Stub() {
            public void onStatus(int i, PersistableBundle persistableBundle) {
            }

            public void onFinished(int i, PersistableBundle persistableBundle) {
                persistableBundle.size();
                completableFuture.complete(persistableBundle);
            }
        });
        System.out.println(completableFuture.get());
    }

    public void runSetVirtualDisk() throws RemoteException {
        this.mSm.setDebugFlags(Boolean.parseBoolean(nextArg()) ? 32 : 0, 32);
    }

    public void runIdleMaint() throws RemoteException {
        if ("run".equals(nextArg())) {
            this.mSm.runIdleMaintenance();
        } else {
            this.mSm.abortIdleMaintenance();
        }
    }

    private String nextArg() {
        if (this.mNextArg >= this.mArgs.length) {
            return null;
        }
        String str = this.mArgs[this.mNextArg];
        this.mNextArg++;
        return str;
    }

    private static int showUsage() {
        System.err.println("usage: sm list-disks [adoptable]");
        System.err.println("       sm list-volumes [public|private|emulated|all]");
        System.err.println("       sm has-adoptable");
        System.err.println("       sm get-primary-storage-uuid");
        System.err.println("       sm set-force-adoptable [on|off|default]");
        System.err.println("       sm set-virtual-disk [true|false]");
        System.err.println("");
        System.err.println("       sm partition DISK [public|private|mixed] [ratio]");
        System.err.println("       sm mount VOLUME");
        System.err.println("       sm unmount VOLUME");
        System.err.println("       sm format VOLUME");
        System.err.println("       sm benchmark VOLUME");
        System.err.println("       sm idle-maint [run|abort]");
        System.err.println("       sm fstrim");
        System.err.println("");
        System.err.println("       sm forget [UUID|all]");
        System.err.println("");
        System.err.println("       sm set-emulate-fbe [true|false]");
        System.err.println("");
        return 1;
    }
}
