package com.android.settings.wrapper;

import android.content.om.IOverlayManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import java.util.ArrayList;
import java.util.List;

/* loaded from: classes.dex */
public class OverlayManagerWrapper {
    private final IOverlayManager mOverlayManager;

    public OverlayManagerWrapper(IOverlayManager iOverlayManager) {
        this.mOverlayManager = iOverlayManager;
    }

    public OverlayManagerWrapper() {
        this(IOverlayManager.Stub.asInterface(ServiceManager.getService("overlay")));
    }

    public List<OverlayInfo> getOverlayInfosForTarget(String str, int i) {
        if (this.mOverlayManager == null) {
            return new ArrayList();
        }
        try {
            List overlayInfosForTarget = this.mOverlayManager.getOverlayInfosForTarget(str, i);
            ArrayList arrayList = new ArrayList(overlayInfosForTarget.size());
            for (int i2 = 0; i2 < overlayInfosForTarget.size(); i2++) {
                arrayList.add(new OverlayInfo((android.content.om.OverlayInfo) overlayInfosForTarget.get(i2)));
            }
            return arrayList;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean setEnabled(String str, boolean z, int i) {
        if (this.mOverlayManager == null) {
            return false;
        }
        try {
            return this.mOverlayManager.setEnabled(str, z, i);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean setEnabledExclusiveInCategory(String str, int i) {
        if (this.mOverlayManager == null) {
            return false;
        }
        try {
            return this.mOverlayManager.setEnabledExclusiveInCategory(str, i);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public static class OverlayInfo {
        public final String category;
        private final boolean mEnabled;
        public final String packageName;

        public OverlayInfo(android.content.om.OverlayInfo overlayInfo) {
            this.mEnabled = overlayInfo.isEnabled();
            this.category = overlayInfo.category;
            this.packageName = overlayInfo.packageName;
        }

        public boolean isEnabled() {
            return this.mEnabled;
        }
    }
}
