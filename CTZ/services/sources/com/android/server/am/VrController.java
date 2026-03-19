package com.android.server.am;

import android.content.ComponentName;
import android.os.Process;
import android.service.vr.IPersistentVrStateCallbacks;
import android.util.Slog;
import android.util.proto.ProtoOutputStream;
import android.util.proto.ProtoUtils;
import com.android.server.LocalServices;
import com.android.server.vr.VrManagerInternal;

final class VrController {
    private static final int FLAG_NON_VR_MODE = 0;
    private static final int FLAG_PERSISTENT_VR_MODE = 2;
    private static final int FLAG_VR_MODE = 1;
    private static int[] ORIG_ENUMS = {0, 1, 2};
    private static int[] PROTO_ENUMS = {0, 1, 2};
    private static final String TAG = "VrController";
    private final Object mGlobalAmLock;
    private int mVrState = 0;
    private int mVrRenderThreadTid = 0;
    private final IPersistentVrStateCallbacks mPersistentVrModeListener = new IPersistentVrStateCallbacks.Stub() {
        public void onPersistentVrStateChanged(boolean z) {
            synchronized (VrController.this.mGlobalAmLock) {
                try {
                    if (z) {
                        VrController.this.setVrRenderThreadLocked(0, 3, true);
                        VrController.access$276(VrController.this, 2);
                    } else {
                        VrController.this.setPersistentVrRenderThreadLocked(0, true);
                        VrController.access$272(VrController.this, -3);
                    }
                } catch (Throwable th) {
                    throw th;
                }
            }
        }
    };

    static int access$272(VrController vrController, int i) {
        int i2 = i & vrController.mVrState;
        vrController.mVrState = i2;
        return i2;
    }

    static int access$276(VrController vrController, int i) {
        int i2 = i | vrController.mVrState;
        vrController.mVrState = i2;
        return i2;
    }

    public VrController(Object obj) {
        this.mGlobalAmLock = obj;
    }

    public void onSystemReady() {
        VrManagerInternal vrManagerInternal = (VrManagerInternal) LocalServices.getService(VrManagerInternal.class);
        if (vrManagerInternal != null) {
            vrManagerInternal.addPersistentVrModeStateListener(this.mPersistentVrModeListener);
        }
    }

    public void onTopProcChangedLocked(ProcessRecord processRecord) {
        if (processRecord.curSchedGroup == 3) {
            setVrRenderThreadLocked(processRecord.vrThreadTid, processRecord.curSchedGroup, true);
        } else if (processRecord.vrThreadTid == this.mVrRenderThreadTid) {
            clearVrRenderThreadLocked(true);
        }
    }

    public boolean onVrModeChanged(ActivityRecord activityRecord) {
        boolean z;
        ComponentName componentName;
        int i;
        ComponentName componentName2;
        boolean zChangeVrModeLocked;
        int i2;
        VrManagerInternal vrManagerInternal = (VrManagerInternal) LocalServices.getService(VrManagerInternal.class);
        if (vrManagerInternal == null) {
            return false;
        }
        synchronized (this.mGlobalAmLock) {
            z = activityRecord.requestedVrComponent != null;
            componentName = activityRecord.requestedVrComponent;
            i = activityRecord.userId;
            componentName2 = activityRecord.info.getComponentName();
            zChangeVrModeLocked = changeVrModeLocked(z, activityRecord.app);
            if (activityRecord.app != null) {
                i2 = activityRecord.app.pid;
            } else {
                i2 = -1;
            }
        }
        vrManagerInternal.setVrMode(z, componentName, i, i2, componentName2);
        return zChangeVrModeLocked;
    }

    public void setVrThreadLocked(int i, int i2, ProcessRecord processRecord) {
        if (hasPersistentVrFlagSet()) {
            Slog.w(TAG, "VR thread cannot be set in persistent VR mode!");
            return;
        }
        if (processRecord == null) {
            Slog.w(TAG, "Persistent VR thread not set, calling process doesn't exist!");
            return;
        }
        if (i != 0) {
            enforceThreadInProcess(i, i2);
        }
        if (!inVrMode()) {
            Slog.w(TAG, "VR thread cannot be set when not in VR mode!");
        } else {
            setVrRenderThreadLocked(i, processRecord.curSchedGroup, false);
        }
        if (i <= 0) {
            i = 0;
        }
        processRecord.vrThreadTid = i;
    }

    public void setPersistentVrThreadLocked(int i, int i2, ProcessRecord processRecord) {
        if (!hasPersistentVrFlagSet()) {
            Slog.w(TAG, "Persistent VR thread may only be set in persistent VR mode!");
        } else {
            if (processRecord == null) {
                Slog.w(TAG, "Persistent VR thread not set, calling process doesn't exist!");
                return;
            }
            if (i != 0) {
                enforceThreadInProcess(i, i2);
            }
            setPersistentVrRenderThreadLocked(i, false);
        }
    }

    public boolean shouldDisableNonVrUiLocked() {
        return this.mVrState != 0;
    }

    private boolean changeVrModeLocked(boolean z, ProcessRecord processRecord) {
        int i = this.mVrState;
        boolean z2 = true;
        if (z) {
            this.mVrState |= 1;
        } else {
            this.mVrState &= -2;
        }
        if (i == this.mVrState) {
            z2 = false;
        }
        if (z2) {
            if (processRecord != null) {
                if (processRecord.vrThreadTid > 0) {
                    setVrRenderThreadLocked(processRecord.vrThreadTid, processRecord.curSchedGroup, false);
                }
            } else {
                clearVrRenderThreadLocked(false);
            }
        }
        return z2;
    }

    private int updateVrRenderThreadLocked(int i, boolean z) {
        if (this.mVrRenderThreadTid == i) {
            return this.mVrRenderThreadTid;
        }
        if (this.mVrRenderThreadTid > 0) {
            ActivityManagerService.scheduleAsRegularPriority(this.mVrRenderThreadTid, z);
            this.mVrRenderThreadTid = 0;
        }
        if (i > 0) {
            this.mVrRenderThreadTid = i;
            ActivityManagerService.scheduleAsFifoPriority(this.mVrRenderThreadTid, z);
        }
        return this.mVrRenderThreadTid;
    }

    private int setPersistentVrRenderThreadLocked(int i, boolean z) {
        if (!hasPersistentVrFlagSet()) {
            if (!z) {
                Slog.w(TAG, "Failed to set persistent VR thread, system not in persistent VR mode.");
            }
            return this.mVrRenderThreadTid;
        }
        return updateVrRenderThreadLocked(i, z);
    }

    private int setVrRenderThreadLocked(int i, int i2, boolean z) {
        boolean zInVrMode = inVrMode();
        boolean zHasPersistentVrFlagSet = hasPersistentVrFlagSet();
        if (!zInVrMode || zHasPersistentVrFlagSet || i2 != 3) {
            if (!z) {
                String str = "caller is not the current top application.";
                if (!zInVrMode) {
                    str = "system not in VR mode.";
                } else if (zHasPersistentVrFlagSet) {
                    str = "system in persistent VR mode.";
                }
                Slog.w(TAG, "Failed to set VR thread, " + str);
            }
            return this.mVrRenderThreadTid;
        }
        return updateVrRenderThreadLocked(i, z);
    }

    private void clearVrRenderThreadLocked(boolean z) {
        updateVrRenderThreadLocked(0, z);
    }

    private void enforceThreadInProcess(int i, int i2) {
        if (!Process.isThreadInProcess(i2, i)) {
            throw new IllegalArgumentException("VR thread does not belong to process");
        }
    }

    private boolean inVrMode() {
        return (this.mVrState & 1) != 0;
    }

    private boolean hasPersistentVrFlagSet() {
        return (this.mVrState & 2) != 0;
    }

    public String toString() {
        return String.format("[VrState=0x%x,VrRenderThreadTid=%d]", Integer.valueOf(this.mVrState), Integer.valueOf(this.mVrRenderThreadTid));
    }

    void writeToProto(ProtoOutputStream protoOutputStream, long j) {
        long jStart = protoOutputStream.start(j);
        ProtoUtils.writeBitWiseFlagsToProtoEnum(protoOutputStream, 2259152797697L, this.mVrState, ORIG_ENUMS, PROTO_ENUMS);
        protoOutputStream.write(1120986464258L, this.mVrRenderThreadTid);
        protoOutputStream.end(jStart);
    }
}
