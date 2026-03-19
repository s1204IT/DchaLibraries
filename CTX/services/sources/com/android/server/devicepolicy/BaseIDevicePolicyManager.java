package com.android.server.devicepolicy;

import android.app.admin.IDevicePolicyManager;
import android.content.ComponentName;
import android.os.PersistableBundle;
import android.security.keymaster.KeymasterCertificateChain;
import android.security.keystore.ParcelableKeyGenParameterSpec;
import android.telephony.data.ApnSetting;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

abstract class BaseIDevicePolicyManager extends IDevicePolicyManager.Stub {
    abstract void handleStartUser(int i);

    abstract void handleStopUser(int i);

    abstract void handleUnlockUser(int i);

    abstract void systemReady(int i);

    BaseIDevicePolicyManager() {
    }

    public void setSystemSetting(ComponentName componentName, String str, String str2) {
    }

    public void transferOwnership(ComponentName componentName, ComponentName componentName2, PersistableBundle persistableBundle) {
    }

    public PersistableBundle getTransferOwnershipBundle() {
        return null;
    }

    public boolean generateKeyPair(ComponentName componentName, String str, String str2, ParcelableKeyGenParameterSpec parcelableKeyGenParameterSpec, int i, KeymasterCertificateChain keymasterCertificateChain) {
        return false;
    }

    public boolean isUsingUnifiedPassword(ComponentName componentName) {
        return true;
    }

    public boolean setKeyPairCertificate(ComponentName componentName, String str, String str2, byte[] bArr, byte[] bArr2, boolean z) {
        return false;
    }

    public void setStartUserSessionMessage(ComponentName componentName, CharSequence charSequence) {
    }

    public void setEndUserSessionMessage(ComponentName componentName, CharSequence charSequence) {
    }

    @Override
    public String getStartUserSessionMessage(ComponentName componentName) {
        return null;
    }

    @Override
    public String getEndUserSessionMessage(ComponentName componentName) {
        return null;
    }

    public List<String> setMeteredDataDisabledPackages(ComponentName componentName, List<String> list) {
        return list;
    }

    public List<String> getMeteredDataDisabledPackages(ComponentName componentName) {
        return new ArrayList();
    }

    public int addOverrideApn(ComponentName componentName, ApnSetting apnSetting) {
        return -1;
    }

    public boolean updateOverrideApn(ComponentName componentName, int i, ApnSetting apnSetting) {
        return false;
    }

    public boolean removeOverrideApn(ComponentName componentName, int i) {
        return false;
    }

    public List<ApnSetting> getOverrideApns(ComponentName componentName) {
        return Collections.emptyList();
    }

    public void setOverrideApnsEnabled(ComponentName componentName, boolean z) {
    }

    public boolean isOverrideApnEnabled(ComponentName componentName) {
        return false;
    }

    public void clearSystemUpdatePolicyFreezePeriodRecord() {
    }

    public boolean isMeteredDataDisabledPackageForUser(ComponentName componentName, String str, int i) {
        return false;
    }

    public long forceSecurityLogs() {
        return 0L;
    }

    public void setDefaultSmsApplication(ComponentName componentName, String str) {
    }
}
