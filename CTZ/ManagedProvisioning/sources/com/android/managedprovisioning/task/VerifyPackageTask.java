package com.android.managedprovisioning.task;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.text.TextUtils;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.Preconditions;
import com.android.managedprovisioning.R;
import com.android.managedprovisioning.common.ProvisionLogger;
import com.android.managedprovisioning.common.StoreUtils;
import com.android.managedprovisioning.common.Utils;
import com.android.managedprovisioning.model.PackageDownloadInfo;
import com.android.managedprovisioning.model.ProvisioningParams;
import com.android.managedprovisioning.task.AbstractProvisioningTask;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class VerifyPackageTask extends AbstractProvisioningTask {
    private final PackageDownloadInfo mDownloadInfo;
    private final DownloadPackageTask mDownloadPackageTask;
    private final PackageManager mPackageManager;
    private final Utils mUtils;

    public VerifyPackageTask(DownloadPackageTask downloadPackageTask, Context context, ProvisioningParams provisioningParams, AbstractProvisioningTask.Callback callback) {
        this(new Utils(), downloadPackageTask, context, provisioningParams, callback);
    }

    @VisibleForTesting
    VerifyPackageTask(Utils utils, DownloadPackageTask downloadPackageTask, Context context, ProvisioningParams provisioningParams, AbstractProvisioningTask.Callback callback) {
        super(context, provisioningParams, callback);
        this.mUtils = (Utils) Preconditions.checkNotNull(utils);
        this.mDownloadPackageTask = (DownloadPackageTask) Preconditions.checkNotNull(downloadPackageTask);
        this.mPackageManager = this.mContext.getPackageManager();
        this.mDownloadInfo = (PackageDownloadInfo) Preconditions.checkNotNull(provisioningParams.deviceAdminDownloadInfo);
    }

    @Override
    public void run(int i) {
        String downloadedPackageLocation = this.mDownloadPackageTask.getDownloadedPackageLocation();
        if (TextUtils.isEmpty(downloadedPackageLocation)) {
            ProvisionLogger.logw("VerifyPackageTask invoked, but download location is null");
            success();
            return;
        }
        PackageInfo packageArchiveInfo = this.mPackageManager.getPackageArchiveInfo(downloadedPackageLocation, 66);
        String strInferDeviceAdminPackageName = this.mProvisioningParams.inferDeviceAdminPackageName();
        if (packageArchiveInfo == null || strInferDeviceAdminPackageName == null) {
            ProvisionLogger.loge("Device admin package info or name is null");
            error(1);
            return;
        }
        if (this.mUtils.findDeviceAdminInPackageInfo(strInferDeviceAdminPackageName, this.mProvisioningParams.deviceAdminComponentName, packageArchiveInfo) == null) {
            error(1);
            return;
        }
        if (this.mDownloadInfo.packageChecksum.length > 0) {
            if (!doesPackageHashMatch(downloadedPackageLocation, this.mDownloadInfo.packageChecksum, this.mDownloadInfo.packageChecksumSupportsSha1)) {
                error(0);
                return;
            }
        } else if (!doesASignatureHashMatch(packageArchiveInfo, this.mDownloadInfo.signatureChecksum)) {
            error(0);
            return;
        }
        success();
    }

    @Override
    public int getStatusMsgId() {
        return R.string.progress_install;
    }

    private List<byte[]> computeHashesOfAllSignatures(Signature[] signatureArr) {
        if (signatureArr == null) {
            return null;
        }
        LinkedList linkedList = new LinkedList();
        for (Signature signature : signatureArr) {
            linkedList.add(this.mUtils.computeHashOfByteArray(signature.toByteArray()));
        }
        return linkedList;
    }

    private boolean doesASignatureHashMatch(PackageInfo packageInfo, byte[] bArr) {
        ProvisionLogger.logd("Checking SHA-256-hashes of all signatures of downloaded package.");
        List<byte[]> listComputeHashesOfAllSignatures = computeHashesOfAllSignatures(packageInfo.signatures);
        if (listComputeHashesOfAllSignatures == null || listComputeHashesOfAllSignatures.isEmpty()) {
            ProvisionLogger.loge("Downloaded package does not have any signatures.");
            return false;
        }
        Iterator<byte[]> it = listComputeHashesOfAllSignatures.iterator();
        while (it.hasNext()) {
            if (Arrays.equals(it.next(), bArr)) {
                return true;
            }
        }
        ProvisionLogger.loge("Provided hash does not match any signature hash.");
        ProvisionLogger.loge("Hash provided by programmer: " + StoreUtils.byteArrayToString(bArr));
        ProvisionLogger.loge("Hashes computed from package signatures: ");
        for (byte[] bArr2 : listComputeHashesOfAllSignatures) {
            if (bArr2 != null) {
                ProvisionLogger.loge(StoreUtils.byteArrayToString(bArr2));
            }
        }
        return false;
    }

    private boolean doesPackageHashMatch(String str, byte[] bArr, boolean z) throws Throwable {
        byte[] bArrComputeHashOfFile;
        ProvisionLogger.logd("Checking file hash of entire apk file.");
        byte[] bArrComputeHashOfFile2 = this.mUtils.computeHashOfFile(str, "SHA-256");
        if (Arrays.equals(bArr, bArrComputeHashOfFile2)) {
            return true;
        }
        if (z) {
            bArrComputeHashOfFile = this.mUtils.computeHashOfFile(str, "SHA-1");
            if (Arrays.equals(bArr, bArrComputeHashOfFile)) {
                return true;
            }
        } else {
            bArrComputeHashOfFile = null;
        }
        ProvisionLogger.loge("Provided hash does not match file hash.");
        ProvisionLogger.loge("Hash provided by programmer: " + StoreUtils.byteArrayToString(bArr));
        if (bArrComputeHashOfFile2 != null) {
            ProvisionLogger.loge("SHA-256 Hash computed from file: " + StoreUtils.byteArrayToString(bArrComputeHashOfFile2));
        }
        if (bArrComputeHashOfFile != null) {
            ProvisionLogger.loge("SHA-1 Hash computed from file: " + StoreUtils.byteArrayToString(bArrComputeHashOfFile));
            return false;
        }
        return false;
    }
}
