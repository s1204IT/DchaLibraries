package com.android.server.am;

import android.app.ContentProviderHolder;
import android.content.ComponentName;
import android.content.IContentProvider;
import android.content.pm.ApplicationInfo;
import android.content.pm.ProviderInfo;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Slog;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;

public final class ContentProviderRecord implements ComponentName.WithComponentName {
    final ApplicationInfo appInfo;
    final ArrayList<ContentProviderConnection> connections = new ArrayList<>();
    int externalProcessNoHandleCount;
    HashMap<IBinder, ExternalProcessHandle> externalProcessTokenToHandle;
    public final ProviderInfo info;
    public ProcessRecord launchingApp;
    final ComponentName name;
    public boolean noReleaseNeeded;
    ProcessRecord proc;
    public IContentProvider provider;
    final ActivityManagerService service;
    String shortStringName;
    final boolean singleton;
    String stringName;
    final int uid;

    public ContentProviderRecord(ActivityManagerService activityManagerService, ProviderInfo providerInfo, ApplicationInfo applicationInfo, ComponentName componentName, boolean z) {
        this.service = activityManagerService;
        this.info = providerInfo;
        this.uid = applicationInfo.uid;
        this.appInfo = applicationInfo;
        this.name = componentName;
        this.singleton = z;
        this.noReleaseNeeded = this.uid == 0 || this.uid == 1000;
    }

    public ContentProviderRecord(ContentProviderRecord contentProviderRecord) {
        this.service = contentProviderRecord.service;
        this.info = contentProviderRecord.info;
        this.uid = contentProviderRecord.uid;
        this.appInfo = contentProviderRecord.appInfo;
        this.name = contentProviderRecord.name;
        this.singleton = contentProviderRecord.singleton;
        this.noReleaseNeeded = contentProviderRecord.noReleaseNeeded;
    }

    public ContentProviderHolder newHolder(ContentProviderConnection contentProviderConnection) {
        ContentProviderHolder contentProviderHolder = new ContentProviderHolder(this.info);
        contentProviderHolder.provider = this.provider;
        contentProviderHolder.noReleaseNeeded = this.noReleaseNeeded;
        contentProviderHolder.connection = contentProviderConnection;
        return contentProviderHolder;
    }

    public boolean canRunHere(ProcessRecord processRecord) {
        return (this.info.multiprocess || this.info.processName.equals(processRecord.processName)) && this.uid == processRecord.info.uid;
    }

    public void addExternalProcessHandleLocked(IBinder iBinder) {
        if (iBinder == null) {
            this.externalProcessNoHandleCount++;
            return;
        }
        if (this.externalProcessTokenToHandle == null) {
            this.externalProcessTokenToHandle = new HashMap<>();
        }
        ExternalProcessHandle externalProcessHandle = this.externalProcessTokenToHandle.get(iBinder);
        if (externalProcessHandle == null) {
            externalProcessHandle = new ExternalProcessHandle(iBinder);
            this.externalProcessTokenToHandle.put(iBinder, externalProcessHandle);
        }
        ExternalProcessHandle.access$008(externalProcessHandle);
    }

    public boolean removeExternalProcessHandleLocked(IBinder iBinder) {
        boolean z;
        ExternalProcessHandle externalProcessHandle;
        if (hasExternalProcessHandles()) {
            if (this.externalProcessTokenToHandle == null || (externalProcessHandle = this.externalProcessTokenToHandle.get(iBinder)) == null) {
                z = false;
            } else {
                ExternalProcessHandle.access$010(externalProcessHandle);
                if (externalProcessHandle.mAcquisitionCount == 0) {
                    removeExternalProcessHandleInternalLocked(iBinder);
                    return true;
                }
                z = true;
            }
            if (!z) {
                this.externalProcessNoHandleCount--;
                return true;
            }
        }
        return false;
    }

    private void removeExternalProcessHandleInternalLocked(IBinder iBinder) {
        this.externalProcessTokenToHandle.get(iBinder).unlinkFromOwnDeathLocked();
        this.externalProcessTokenToHandle.remove(iBinder);
        if (this.externalProcessTokenToHandle.size() == 0) {
            this.externalProcessTokenToHandle = null;
        }
    }

    public boolean hasExternalProcessHandles() {
        return this.externalProcessTokenToHandle != null || this.externalProcessNoHandleCount > 0;
    }

    public boolean hasConnectionOrHandle() {
        return !this.connections.isEmpty() || hasExternalProcessHandles();
    }

    void dump(PrintWriter printWriter, String str, boolean z) {
        if (z) {
            printWriter.print(str);
            printWriter.print("package=");
            printWriter.print(this.info.applicationInfo.packageName);
            printWriter.print(" process=");
            printWriter.println(this.info.processName);
        }
        printWriter.print(str);
        printWriter.print("proc=");
        printWriter.println(this.proc);
        if (this.launchingApp != null) {
            printWriter.print(str);
            printWriter.print("launchingApp=");
            printWriter.println(this.launchingApp);
        }
        if (z) {
            printWriter.print(str);
            printWriter.print("uid=");
            printWriter.print(this.uid);
            printWriter.print(" provider=");
            printWriter.println(this.provider);
        }
        if (this.singleton) {
            printWriter.print(str);
            printWriter.print("singleton=");
            printWriter.println(this.singleton);
        }
        printWriter.print(str);
        printWriter.print("authority=");
        printWriter.println(this.info.authority);
        if (z && (this.info.isSyncable || this.info.multiprocess || this.info.initOrder != 0)) {
            printWriter.print(str);
            printWriter.print("isSyncable=");
            printWriter.print(this.info.isSyncable);
            printWriter.print(" multiprocess=");
            printWriter.print(this.info.multiprocess);
            printWriter.print(" initOrder=");
            printWriter.println(this.info.initOrder);
        }
        if (z) {
            if (hasExternalProcessHandles()) {
                printWriter.print(str);
                printWriter.print("externals:");
                if (this.externalProcessTokenToHandle != null) {
                    printWriter.print(" w/token=");
                    printWriter.print(this.externalProcessTokenToHandle.size());
                }
                if (this.externalProcessNoHandleCount > 0) {
                    printWriter.print(" notoken=");
                    printWriter.print(this.externalProcessNoHandleCount);
                }
                printWriter.println();
            }
        } else if (this.connections.size() > 0 || this.externalProcessNoHandleCount > 0) {
            printWriter.print(str);
            printWriter.print(this.connections.size());
            printWriter.print(" connections, ");
            printWriter.print(this.externalProcessNoHandleCount);
            printWriter.println(" external handles");
        }
        if (this.connections.size() > 0) {
            if (z) {
                printWriter.print(str);
                printWriter.println("Connections:");
            }
            for (int i = 0; i < this.connections.size(); i++) {
                ContentProviderConnection contentProviderConnection = this.connections.get(i);
                printWriter.print(str);
                printWriter.print("  -> ");
                printWriter.println(contentProviderConnection.toClientString());
                if (contentProviderConnection.provider != this) {
                    printWriter.print(str);
                    printWriter.print("    *** WRONG PROVIDER: ");
                    printWriter.println(contentProviderConnection.provider);
                }
            }
        }
    }

    public String toString() {
        if (this.stringName != null) {
            return this.stringName;
        }
        StringBuilder sb = new StringBuilder(128);
        sb.append("ContentProviderRecord{");
        sb.append(Integer.toHexString(System.identityHashCode(this)));
        sb.append(" u");
        sb.append(UserHandle.getUserId(this.uid));
        sb.append(' ');
        sb.append(this.name.flattenToShortString());
        sb.append('}');
        String string = sb.toString();
        this.stringName = string;
        return string;
    }

    public String toShortString() {
        if (this.shortStringName != null) {
            return this.shortStringName;
        }
        StringBuilder sb = new StringBuilder(128);
        sb.append(Integer.toHexString(System.identityHashCode(this)));
        sb.append('/');
        sb.append(this.name.flattenToShortString());
        String string = sb.toString();
        this.shortStringName = string;
        return string;
    }

    private class ExternalProcessHandle implements IBinder.DeathRecipient {
        private static final String LOG_TAG = "ExternalProcessHanldle";
        private int mAcquisitionCount;
        private final IBinder mToken;

        static int access$008(ExternalProcessHandle externalProcessHandle) {
            int i = externalProcessHandle.mAcquisitionCount;
            externalProcessHandle.mAcquisitionCount = i + 1;
            return i;
        }

        static int access$010(ExternalProcessHandle externalProcessHandle) {
            int i = externalProcessHandle.mAcquisitionCount;
            externalProcessHandle.mAcquisitionCount = i - 1;
            return i;
        }

        public ExternalProcessHandle(IBinder iBinder) {
            this.mToken = iBinder;
            try {
                iBinder.linkToDeath(this, 0);
            } catch (RemoteException e) {
                Slog.e(LOG_TAG, "Couldn't register for death for token: " + this.mToken, e);
            }
        }

        public void unlinkFromOwnDeathLocked() {
            this.mToken.unlinkToDeath(this, 0);
        }

        @Override
        public void binderDied() {
            synchronized (ContentProviderRecord.this.service) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    if (ContentProviderRecord.this.hasExternalProcessHandles() && ContentProviderRecord.this.externalProcessTokenToHandle.get(this.mToken) != null) {
                        ContentProviderRecord.this.removeExternalProcessHandleInternalLocked(this.mToken);
                    }
                } catch (Throwable th) {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            ActivityManagerService.resetPriorityAfterLockedSection();
        }
    }

    public ComponentName getComponentName() {
        return this.name;
    }
}
