package com.android.commands.sm;

import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.storage.DiskInfo;
import android.os.storage.IMountService;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.util.Log;

public final class Sm {
    private static final String TAG = "Sm";
    private String[] mArgs;
    private String mCurArgData;
    private int mNextArg;
    IMountService mSm;

    public static void main(String[] args) {
        boolean success = false;
        try {
            new Sm().run(args);
            success = true;
        } catch (Exception e) {
            if (e instanceof IllegalArgumentException) {
                showUsage();
                System.exit(1);
            }
            Log.e(TAG, "Error", e);
            System.err.println("Error: " + e);
        }
        System.exit(success ? 0 : 1);
    }

    public void run(String[] args) throws Exception {
        if (args.length < 1) {
            throw new IllegalArgumentException();
        }
        this.mSm = IMountService.Stub.asInterface(ServiceManager.getService("mount"));
        if (this.mSm == null) {
            throw new RemoteException("Failed to find running mount service");
        }
        this.mArgs = args;
        String op = args[0];
        this.mNextArg = 1;
        if ("list-disks".equals(op)) {
            runListDisks();
            return;
        }
        if ("list-volumes".equals(op)) {
            runListVolumes();
            return;
        }
        if ("has-adoptable".equals(op)) {
            runHasAdoptable();
            return;
        }
        if ("get-primary-storage-uuid".equals(op)) {
            runGetPrimaryStorageUuid();
            return;
        }
        if ("set-force-adoptable".equals(op)) {
            runSetForceAdoptable();
            return;
        }
        if ("set-sdcardfs".equals(op)) {
            runSetSdcardfs();
            return;
        }
        if ("partition".equals(op)) {
            runPartition();
            return;
        }
        if ("mount".equals(op)) {
            runMount();
            return;
        }
        if ("unmount".equals(op)) {
            runUnmount();
            return;
        }
        if ("format".equals(op)) {
            runFormat();
            return;
        }
        if ("benchmark".equals(op)) {
            runBenchmark();
            return;
        }
        if ("forget".equals(op)) {
            runForget();
        } else if ("set-emulate-fbe".equals(op)) {
            runSetEmulateFbe();
        } else {
            if ("get-fbe-mode".equals(op)) {
                runGetFbeMode();
                return;
            }
            throw new IllegalArgumentException();
        }
    }

    public void runListDisks() throws RemoteException {
        boolean onlyAdoptable = "adoptable".equals(nextArg());
        DiskInfo[] disks = this.mSm.getDisks();
        for (DiskInfo disk : disks) {
            if (!onlyAdoptable || disk.isAdoptable()) {
                System.out.println(disk.getId());
            }
        }
    }

    public void runListVolumes() throws RemoteException {
        int filterType;
        String filter = nextArg();
        if ("public".equals(filter)) {
            filterType = 0;
        } else if ("private".equals(filter)) {
            filterType = 1;
        } else if ("emulated".equals(filter)) {
            filterType = 2;
        } else {
            filterType = -1;
        }
        VolumeInfo[] vols = this.mSm.getVolumes(0);
        for (VolumeInfo vol : vols) {
            if (filterType == -1 || filterType == vol.getType()) {
                String envState = VolumeInfo.getEnvironmentForState(vol.getState());
                System.out.println(vol.getId() + " " + envState + " " + vol.getFsUuid());
            }
        }
    }

    public void runHasAdoptable() {
        System.out.println(SystemProperties.getBoolean("vold.has_adoptable", false));
    }

    public void runGetPrimaryStorageUuid() throws RemoteException {
        System.out.println(this.mSm.getPrimaryStorageUuid());
    }

    public void runSetForceAdoptable() throws RemoteException {
        boolean forceAdoptable = Boolean.parseBoolean(nextArg());
        this.mSm.setDebugFlags(forceAdoptable ? 1 : 0, 1);
    }

    public void runSetSdcardfs() throws RemoteException {
        String strNextArg = nextArg();
        if (!strNextArg.equals("on")) {
            if (!strNextArg.equals("off")) {
                if (!strNextArg.equals("default")) {
                    return;
                }
                this.mSm.setDebugFlags(0, 12);
                return;
            }
            this.mSm.setDebugFlags(8, 12);
            return;
        }
        this.mSm.setDebugFlags(4, 12);
    }

    public void runSetEmulateFbe() throws RemoteException {
        boolean emulateFbe = Boolean.parseBoolean(nextArg());
        this.mSm.setDebugFlags(emulateFbe ? 2 : 0, 2);
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
        String diskId = nextArg();
        String type = nextArg();
        if ("public".equals(type)) {
            this.mSm.partitionPublic(diskId);
            return;
        }
        if ("private".equals(type)) {
            this.mSm.partitionPrivate(diskId);
        } else {
            if ("mixed".equals(type)) {
                int ratio = Integer.parseInt(nextArg());
                this.mSm.partitionMixed(diskId, ratio);
                return;
            }
            throw new IllegalArgumentException("Unsupported partition type " + type);
        }
    }

    public void runMount() throws RemoteException {
        String volId = nextArg();
        this.mSm.mount(volId);
    }

    public void runUnmount() throws RemoteException {
        String volId = nextArg();
        this.mSm.unmount(volId);
    }

    public void runFormat() throws RemoteException {
        String volId = nextArg();
        this.mSm.format(volId);
    }

    public void runBenchmark() throws RemoteException {
        String volId = nextArg();
        this.mSm.benchmark(volId);
    }

    public void runForget() throws RemoteException {
        String fsUuid = nextArg();
        if ("all".equals(fsUuid)) {
            this.mSm.forgetAllVolumes();
        } else {
            this.mSm.forgetVolume(fsUuid);
        }
    }

    private String nextArg() {
        if (this.mNextArg >= this.mArgs.length) {
            return null;
        }
        String arg = this.mArgs[this.mNextArg];
        this.mNextArg++;
        return arg;
    }

    private static int showUsage() {
        System.err.println("usage: sm list-disks [adoptable]");
        System.err.println("       sm list-volumes [public|private|emulated|all]");
        System.err.println("       sm has-adoptable");
        System.err.println("       sm get-primary-storage-uuid");
        System.err.println("       sm set-force-adoptable [true|false]");
        System.err.println("");
        System.err.println("       sm partition DISK [public|private|mixed] [ratio]");
        System.err.println("       sm mount VOLUME");
        System.err.println("       sm unmount VOLUME");
        System.err.println("       sm format VOLUME");
        System.err.println("       sm benchmark VOLUME");
        System.err.println("");
        System.err.println("       sm forget [UUID|all]");
        System.err.println("");
        System.err.println("       sm set-emulate-fbe [true|false]");
        System.err.println("");
        return 1;
    }
}
