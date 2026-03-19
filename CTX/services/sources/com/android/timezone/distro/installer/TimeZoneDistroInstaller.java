package com.android.timezone.distro.installer;

import android.util.Slog;
import com.android.timezone.distro.DistroException;
import com.android.timezone.distro.DistroVersion;
import com.android.timezone.distro.FileUtils;
import com.android.timezone.distro.StagedDistroOperation;
import com.android.timezone.distro.TimeZoneDistro;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import libcore.util.TimeZoneFinder;
import libcore.util.ZoneInfoDB;

public class TimeZoneDistroInstaller {
    private static final String CURRENT_TZ_DATA_DIR_NAME = "current";
    public static final int INSTALL_FAIL_BAD_DISTRO_FORMAT_VERSION = 2;
    public static final int INSTALL_FAIL_BAD_DISTRO_STRUCTURE = 1;
    public static final int INSTALL_FAIL_RULES_TOO_OLD = 3;
    public static final int INSTALL_FAIL_VALIDATION_ERROR = 4;
    public static final int INSTALL_SUCCESS = 0;
    private static final String OLD_TZ_DATA_DIR_NAME = "old";
    private static final String STAGED_TZ_DATA_DIR_NAME = "staged";
    public static final int UNINSTALL_FAIL = 2;
    public static final int UNINSTALL_NOTHING_INSTALLED = 1;
    public static final int UNINSTALL_SUCCESS = 0;
    public static final String UNINSTALL_TOMBSTONE_FILE_NAME = "STAGED_UNINSTALL_TOMBSTONE";
    private static final String WORKING_DIR_NAME = "working";
    private final File currentTzDataDir;
    private final String logTag;
    private final File oldStagedDataDir;
    private final File stagedTzDataDir;
    private final File systemTzDataFile;
    private final File workingDir;

    @Retention(RetentionPolicy.SOURCE)
    private @interface InstallResultType {
    }

    @Retention(RetentionPolicy.SOURCE)
    private @interface UninstallResultType {
    }

    public TimeZoneDistroInstaller(String str, File file, File file2) {
        this.logTag = str;
        this.systemTzDataFile = file;
        this.oldStagedDataDir = new File(file2, OLD_TZ_DATA_DIR_NAME);
        this.stagedTzDataDir = new File(file2, STAGED_TZ_DATA_DIR_NAME);
        this.currentTzDataDir = new File(file2, CURRENT_TZ_DATA_DIR_NAME);
        this.workingDir = new File(file2, WORKING_DIR_NAME);
    }

    File getOldStagedDataDir() {
        return this.oldStagedDataDir;
    }

    File getStagedTzDataDir() {
        return this.stagedTzDataDir;
    }

    File getCurrentTzDataDir() {
        return this.currentTzDataDir;
    }

    File getWorkingDir() {
        return this.workingDir;
    }

    public int stageInstallWithErrorCode(TimeZoneDistro timeZoneDistro) throws IOException {
        if (this.oldStagedDataDir.exists()) {
            FileUtils.deleteRecursive(this.oldStagedDataDir);
        }
        if (this.workingDir.exists()) {
            FileUtils.deleteRecursive(this.workingDir);
        }
        Slog.i(this.logTag, "Unpacking / verifying time zone update");
        try {
            unpackDistro(timeZoneDistro, this.workingDir);
            try {
                DistroVersion distroVersion = readDistroVersion(this.workingDir);
                if (distroVersion == null) {
                    Slog.i(this.logTag, "Update not applied: Distro version could not be loaded");
                    deleteBestEffort(this.oldStagedDataDir);
                    deleteBestEffort(this.workingDir);
                    return 1;
                }
                if (!DistroVersion.isCompatibleWithThisDevice(distroVersion)) {
                    Slog.i(this.logTag, "Update not applied: Distro format version check failed: " + distroVersion);
                    deleteBestEffort(this.oldStagedDataDir);
                    deleteBestEffort(this.workingDir);
                    return 2;
                }
                if (!checkDistroDataFilesExist(this.workingDir)) {
                    Slog.i(this.logTag, "Update not applied: Distro is missing required data file(s)");
                    deleteBestEffort(this.oldStagedDataDir);
                    deleteBestEffort(this.workingDir);
                    return 1;
                }
                if (!checkDistroRulesNewerThanSystem(this.systemTzDataFile, distroVersion)) {
                    Slog.i(this.logTag, "Update not applied: Distro rules version check failed");
                    deleteBestEffort(this.oldStagedDataDir);
                    deleteBestEffort(this.workingDir);
                    return 3;
                }
                File file = new File(this.workingDir, TimeZoneDistro.TZDATA_FILE_NAME);
                ZoneInfoDB.TzData tzDataLoadTzData = ZoneInfoDB.TzData.loadTzData(file.getPath());
                if (tzDataLoadTzData == null) {
                    Slog.i(this.logTag, "Update not applied: " + file + " could not be loaded");
                    deleteBestEffort(this.oldStagedDataDir);
                    deleteBestEffort(this.workingDir);
                    return 4;
                }
                try {
                    try {
                        tzDataLoadTzData.validate();
                        tzDataLoadTzData.close();
                        File file2 = new File(this.workingDir, TimeZoneDistro.TZLOOKUP_FILE_NAME);
                        if (!file2.exists()) {
                            Slog.i(this.logTag, "Update not applied: " + file2 + " does not exist");
                            deleteBestEffort(this.oldStagedDataDir);
                            deleteBestEffort(this.workingDir);
                            return 1;
                        }
                        try {
                            TimeZoneFinder.createInstance(file2.getPath()).validate();
                            Slog.i(this.logTag, "Applying time zone update");
                            FileUtils.makeDirectoryWorldAccessible(this.workingDir);
                            if (this.stagedTzDataDir.exists()) {
                                Slog.i(this.logTag, "Moving " + this.stagedTzDataDir + " to " + this.oldStagedDataDir);
                                FileUtils.rename(this.stagedTzDataDir, this.oldStagedDataDir);
                            } else {
                                Slog.i(this.logTag, "Nothing to unstage at " + this.stagedTzDataDir);
                            }
                            Slog.i(this.logTag, "Moving " + this.workingDir + " to " + this.stagedTzDataDir);
                            FileUtils.rename(this.workingDir, this.stagedTzDataDir);
                            Slog.i(this.logTag, "Install staged: " + this.stagedTzDataDir + " successfully created");
                            deleteBestEffort(this.oldStagedDataDir);
                            deleteBestEffort(this.workingDir);
                            return 0;
                        } catch (IOException e) {
                            Slog.i(this.logTag, "Update not applied: " + file2 + " failed validation", e);
                            deleteBestEffort(this.oldStagedDataDir);
                            deleteBestEffort(this.workingDir);
                            return 4;
                        }
                    } catch (IOException e2) {
                        Slog.i(this.logTag, "Update not applied: " + file + " failed validation", e2);
                        deleteBestEffort(this.oldStagedDataDir);
                        deleteBestEffort(this.workingDir);
                        return 4;
                    }
                } finally {
                    tzDataLoadTzData.close();
                }
            } catch (DistroException e3) {
                Slog.i(this.logTag, "Invalid distro version: " + e3.getMessage());
                deleteBestEffort(this.oldStagedDataDir);
                deleteBestEffort(this.workingDir);
                return 1;
            }
        } catch (Throwable th) {
            deleteBestEffort(this.oldStagedDataDir);
            deleteBestEffort(this.workingDir);
            throw th;
        }
    }

    public int stageUninstall() throws IOException {
        Slog.i(this.logTag, "Uninstalling time zone update");
        if (this.oldStagedDataDir.exists()) {
            FileUtils.deleteRecursive(this.oldStagedDataDir);
        }
        if (this.workingDir.exists()) {
            FileUtils.deleteRecursive(this.workingDir);
        }
        try {
            if (!this.stagedTzDataDir.exists()) {
                Slog.i(this.logTag, "Nothing to unstage at " + this.stagedTzDataDir);
            } else {
                Slog.i(this.logTag, "Moving " + this.stagedTzDataDir + " to " + this.oldStagedDataDir);
                FileUtils.rename(this.stagedTzDataDir, this.oldStagedDataDir);
            }
            if (!this.currentTzDataDir.exists()) {
                Slog.i(this.logTag, "Nothing to uninstall at " + this.currentTzDataDir);
                return 1;
            }
            FileUtils.ensureDirectoriesExist(this.workingDir, true);
            FileUtils.createEmptyFile(new File(this.workingDir, UNINSTALL_TOMBSTONE_FILE_NAME));
            Slog.i(this.logTag, "Moving " + this.workingDir + " to " + this.stagedTzDataDir);
            FileUtils.rename(this.workingDir, this.stagedTzDataDir);
            Slog.i(this.logTag, "Uninstall staged: " + this.stagedTzDataDir + " successfully created");
            return 0;
        } finally {
            deleteBestEffort(this.oldStagedDataDir);
            deleteBestEffort(this.workingDir);
        }
    }

    public DistroVersion getInstalledDistroVersion() throws DistroException, IOException {
        if (!this.currentTzDataDir.exists()) {
            return null;
        }
        return readDistroVersion(this.currentTzDataDir);
    }

    public StagedDistroOperation getStagedDistroOperation() throws DistroException, IOException {
        if (!this.stagedTzDataDir.exists()) {
            return null;
        }
        if (new File(this.stagedTzDataDir, UNINSTALL_TOMBSTONE_FILE_NAME).exists()) {
            return StagedDistroOperation.uninstall();
        }
        return StagedDistroOperation.install(readDistroVersion(this.stagedTzDataDir));
    }

    public String getSystemRulesVersion() throws IOException {
        return readSystemRulesVersion(this.systemTzDataFile);
    }

    private void deleteBestEffort(File file) {
        if (file.exists()) {
            Slog.i(this.logTag, "Deleting " + file);
            try {
                FileUtils.deleteRecursive(file);
            } catch (IOException e) {
                Slog.w(this.logTag, "Unable to delete " + file, e);
            }
        }
    }

    private void unpackDistro(TimeZoneDistro timeZoneDistro, File file) throws Exception {
        Slog.i(this.logTag, "Unpacking update content to: " + file);
        timeZoneDistro.extractTo(file);
    }

    private boolean checkDistroDataFilesExist(File file) throws IOException {
        Slog.i(this.logTag, "Verifying distro contents");
        return FileUtils.filesExist(file, TimeZoneDistro.TZDATA_FILE_NAME, TimeZoneDistro.ICU_DATA_FILE_NAME);
    }

    private DistroVersion readDistroVersion(File file) throws DistroException, IOException {
        Slog.d(this.logTag, "Reading distro format version: " + file);
        File file2 = new File(file, TimeZoneDistro.DISTRO_VERSION_FILE_NAME);
        if (!file2.exists()) {
            throw new DistroException("No distro version file found: " + file2);
        }
        return DistroVersion.fromBytes(FileUtils.readBytes(file2, DistroVersion.DISTRO_VERSION_FILE_LENGTH));
    }

    private boolean checkDistroRulesNewerThanSystem(File file, DistroVersion distroVersion) throws IOException {
        Slog.i(this.logTag, "Reading /system rules version");
        String systemRulesVersion = readSystemRulesVersion(file);
        String str = distroVersion.rulesVersion;
        boolean z = str.compareTo(systemRulesVersion) >= 0;
        if (!z) {
            Slog.i(this.logTag, "Failed rules version check: distroRulesVersion=" + str + ", systemRulesVersion=" + systemRulesVersion);
        } else {
            Slog.i(this.logTag, "Passed rules version check: distroRulesVersion=" + str + ", systemRulesVersion=" + systemRulesVersion);
        }
        return z;
    }

    private String readSystemRulesVersion(File file) throws IOException {
        if (!file.exists()) {
            Slog.i(this.logTag, "tzdata file cannot be found in /system");
            throw new FileNotFoundException("system tzdata does not exist: " + file);
        }
        return ZoneInfoDB.TzData.getRulesVersion(file);
    }
}
