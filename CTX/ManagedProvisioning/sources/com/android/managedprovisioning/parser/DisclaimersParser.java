package com.android.managedprovisioning.parser;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextUtils;
import com.android.managedprovisioning.common.ProvisionLogger;
import com.android.managedprovisioning.common.StoreUtils;
import com.android.managedprovisioning.model.DisclaimersParam;
import java.io.File;
import java.util.ArrayList;

public class DisclaimersParser {
    private final Context mContext;
    private final File mDisclaimerDir;
    private final long mProvisioningId;

    public DisclaimersParser(Context context, long j) {
        this.mContext = context;
        this.mProvisioningId = j;
        this.mDisclaimerDir = new File(this.mContext.getFilesDir(), "provisioning_params_file_cache");
    }

    public DisclaimersParam parse(Parcelable[] parcelableArr) throws ClassCastException {
        if (parcelableArr == null) {
            return null;
        }
        ArrayList arrayList = new ArrayList(3);
        for (int i = 0; i < parcelableArr.length && arrayList.size() < 3; i++) {
            Bundle bundle = (Bundle) parcelableArr[i];
            String string = bundle.getString("android.app.extra.PROVISIONING_DISCLAIMER_HEADER");
            Uri uri = (Uri) bundle.getParcelable("android.app.extra.PROVISIONING_DISCLAIMER_CONTENT");
            if (TextUtils.isEmpty(string)) {
                ProvisionLogger.logw("Empty disclaimer header in " + i + " element");
            } else if (uri == null) {
                ProvisionLogger.logw("Null disclaimer content uri in " + i + " element");
            } else {
                File fileSaveDisclaimerContentIntoFile = saveDisclaimerContentIntoFile(uri, i);
                if (fileSaveDisclaimerContentIntoFile == null) {
                    ProvisionLogger.logw("Failed to copy disclaimer uri in " + i + " element");
                } else {
                    arrayList.add(new DisclaimersParam.Disclaimer(string, fileSaveDisclaimerContentIntoFile.getPath()));
                }
            }
        }
        if (arrayList.isEmpty()) {
            return null;
        }
        return new DisclaimersParam.Builder().setDisclaimers((DisclaimersParam.Disclaimer[]) arrayList.toArray(new DisclaimersParam.Disclaimer[arrayList.size()])).build();
    }

    private File saveDisclaimerContentIntoFile(Uri uri, int i) {
        if (!this.mDisclaimerDir.exists()) {
            this.mDisclaimerDir.mkdirs();
        }
        File file = new File(this.mDisclaimerDir, "disclaimer_content_" + this.mProvisioningId + "_" + i + ".txt");
        if (StoreUtils.copyUriIntoFile(this.mContext.getContentResolver(), uri, file)) {
            return file;
        }
        return null;
    }
}
