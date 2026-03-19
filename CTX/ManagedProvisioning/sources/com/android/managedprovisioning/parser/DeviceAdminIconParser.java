package com.android.managedprovisioning.parser;

import android.content.Context;
import android.net.Uri;
import com.android.managedprovisioning.common.StoreUtils;
import java.io.File;

public class DeviceAdminIconParser {
    private final Context mContext;
    private final File mFileIcon;

    public DeviceAdminIconParser(Context context, long j) {
        this.mContext = context;
        this.mFileIcon = new File(new File(this.mContext.getFilesDir(), "provisioning_params_file_cache"), "device_admin_icon_" + j);
    }

    public String parse(Uri uri) {
        if (uri == null) {
            return null;
        }
        this.mFileIcon.getParentFile().mkdirs();
        if (StoreUtils.copyUriIntoFile(this.mContext.getContentResolver(), uri, this.mFileIcon)) {
            return this.mFileIcon.getAbsolutePath();
        }
        return null;
    }
}
