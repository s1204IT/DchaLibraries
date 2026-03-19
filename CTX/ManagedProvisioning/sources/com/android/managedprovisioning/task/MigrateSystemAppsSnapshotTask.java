package com.android.managedprovisioning.task;

import android.content.Context;
import android.os.FileUtils;
import com.android.managedprovisioning.common.ProvisionLogger;
import com.android.managedprovisioning.task.AbstractProvisioningTask;
import com.android.managedprovisioning.task.nonrequiredapps.SystemAppsSnapshot;
import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MigrateSystemAppsSnapshotTask extends AbstractProvisioningTask {
    private static final Pattern XML_FILE_NAME_PATTERN = Pattern.compile("(\\d+)\\.xml");

    public MigrateSystemAppsSnapshotTask(Context context, AbstractProvisioningTask.Callback callback) {
        super(context, null, callback);
    }

    @Override
    public void run(int i) {
        migrateIfNecessary();
    }

    private void migrateIfNecessary() {
        File legacyFolder = SystemAppsSnapshot.getLegacyFolder(this.mContext);
        if (!legacyFolder.exists()) {
            return;
        }
        ProvisionLogger.logi("Found legacy system_apps folder, kick start migration.");
        SystemAppsSnapshot.getFolder(this.mContext).mkdirs();
        for (File file : legacyFolder.listFiles()) {
            String name = file.getName();
            Matcher matcher = XML_FILE_NAME_PATTERN.matcher(name);
            if (matcher.find()) {
                int i = Integer.parseInt(matcher.group(1));
                try {
                    File systemAppsFile = SystemAppsSnapshot.getSystemAppsFile(this.mContext, i);
                    ProvisionLogger.logi("Moving " + file.getAbsolutePath() + " to " + systemAppsFile.getAbsolutePath());
                    if (!file.renameTo(systemAppsFile)) {
                        ProvisionLogger.loge("Failed to migrate " + file.getAbsolutePath());
                    }
                } catch (IllegalArgumentException e) {
                    ProvisionLogger.logi("user " + i + " no longer exists, skip migrating its snapshot file");
                }
            } else {
                ProvisionLogger.logw("Found invalid file during migration: " + name);
            }
        }
        FileUtils.deleteContentsAndDir(legacyFolder);
    }

    @Override
    public int getStatusMsgId() {
        return 0;
    }
}
