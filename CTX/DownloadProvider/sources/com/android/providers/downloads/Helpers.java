package com.android.providers.downloads;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.FileUtils;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.MimeTypeMap;
import java.io.File;
import java.io.IOException;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Helpers {
    private static Handler sAsyncHandler;
    private static HandlerThread sAsyncHandlerThread;
    private static DownloadNotifier sNotifier;
    private static SystemFacade sSystemFacade;
    public static Random sRandom = new Random(SystemClock.uptimeMillis());
    private static final Pattern CONTENT_DISPOSITION_PATTERN = Pattern.compile("attachment;\\s*filename\\s*=\\s*\"([^\"]*)\"");
    private static final Object sUniqueLock = new Object();

    public static synchronized Handler getAsyncHandler() {
        if (sAsyncHandlerThread == null) {
            sAsyncHandlerThread = new HandlerThread("sAsyncHandlerThread", 10);
            sAsyncHandlerThread.start();
            sAsyncHandler = new Handler(sAsyncHandlerThread.getLooper());
        }
        return sAsyncHandler;
    }

    public static synchronized void setSystemFacade(SystemFacade systemFacade) {
        sSystemFacade = systemFacade;
    }

    public static synchronized SystemFacade getSystemFacade(Context context) {
        if (sSystemFacade == null) {
            sSystemFacade = new RealSystemFacade(context);
        }
        return sSystemFacade;
    }

    public static synchronized DownloadNotifier getDownloadNotifier(Context context) {
        if (sNotifier == null) {
            sNotifier = new DownloadNotifier(context);
        }
        return sNotifier;
    }

    public static String getString(Cursor cursor, String str) {
        return cursor.getString(cursor.getColumnIndexOrThrow(str));
    }

    public static int getInt(Cursor cursor, String str) {
        return cursor.getInt(cursor.getColumnIndexOrThrow(str));
    }

    public static void scheduleJob(Context context, long j) {
        if (!scheduleJob(context, DownloadInfo.queryDownloadInfo(context, j))) {
            getDownloadNotifier(context).update();
        }
    }

    public static boolean scheduleJob(Context context, DownloadInfo downloadInfo) {
        if (downloadInfo == null) {
            return false;
        }
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(JobScheduler.class);
        int i = (int) downloadInfo.mId;
        jobScheduler.cancel(i);
        if (!downloadInfo.isReadyToSchedule()) {
            return false;
        }
        JobInfo.Builder builder = new JobInfo.Builder(i, new ComponentName(context, (Class<?>) DownloadJobService.class));
        if (downloadInfo.isVisible()) {
            builder.setPriority(30);
            builder.setFlags(1);
        }
        long minimumLatency = downloadInfo.getMinimumLatency();
        if (minimumLatency > 0) {
            builder.setMinimumLatency(minimumLatency);
        }
        builder.setRequiredNetworkType(downloadInfo.getRequiredNetworkType(downloadInfo.mTotalBytes));
        if ((downloadInfo.mFlags & 1) != 0) {
            builder.setRequiresCharging(true);
        }
        if ((downloadInfo.mFlags & 2) != 0) {
            builder.setRequiresDeviceIdle(true);
        }
        if (downloadInfo.mTotalBytes > 0) {
            if (downloadInfo.mCurrentBytes > 0 && !TextUtils.isEmpty(downloadInfo.mETag)) {
                builder.setEstimatedNetworkBytes(downloadInfo.mTotalBytes - downloadInfo.mCurrentBytes);
            } else {
                builder.setEstimatedNetworkBytes(downloadInfo.mTotalBytes);
            }
        }
        String str = downloadInfo.mPackage;
        if (str == null) {
            str = context.getPackageManager().getPackagesForUid(downloadInfo.mUid)[0];
        }
        jobScheduler.scheduleAsPackage(builder.build(), str, UserHandle.myUserId(), "DownloadManager");
        return true;
    }

    private static String parseContentDisposition(String str) {
        try {
            Matcher matcher = CONTENT_DISPOSITION_PATTERN.matcher(str);
            if (matcher.find()) {
                return matcher.group(1);
            }
            return null;
        } catch (IllegalStateException e) {
            return null;
        }
    }

    static String generateSaveFile(Context context, String str, String str2, String str3, String str4, String str5, int i) throws IOException {
        String strChooseFilename;
        File absoluteFile;
        File[] fileArr;
        String strSubstring;
        String strChooseExtensionFromFilename;
        String absolutePath;
        if (i == 4) {
            File file = new File(Uri.parse(str2).getPath());
            absoluteFile = file.getParentFile().getAbsoluteFile();
            fileArr = new File[]{absoluteFile};
            strChooseFilename = file.getName();
        } else {
            File runningDestinationDirectory = getRunningDestinationDirectory(context, i);
            File[] fileArr2 = {runningDestinationDirectory, getSuccessDestinationDirectory(context, i)};
            strChooseFilename = chooseFilename(str, str2, str3, str4);
            absoluteFile = runningDestinationDirectory;
            fileArr = fileArr2;
        }
        for (File file2 : fileArr) {
            if (!file2.isDirectory() && !file2.mkdirs()) {
                throw new IOException("Failed to create parent for " + file2);
            }
        }
        if (DownloadDrmHelper.isDrmConvertNeeded(str5)) {
            strChooseFilename = DownloadDrmHelper.modifyDrmFwLockFileExtension(strChooseFilename);
        }
        int iLastIndexOf = strChooseFilename.lastIndexOf(46);
        boolean z = iLastIndexOf < 0;
        if (i == 4) {
            if (z) {
                strChooseExtensionFromFilename = "";
            } else {
                strSubstring = strChooseFilename.substring(0, iLastIndexOf);
                strChooseExtensionFromFilename = strChooseFilename.substring(iLastIndexOf);
                strChooseFilename = strSubstring;
            }
        } else if (z) {
            strChooseExtensionFromFilename = chooseExtensionFromMimeType(str5, true);
        } else {
            strSubstring = strChooseFilename.substring(0, iLastIndexOf);
            strChooseExtensionFromFilename = chooseExtensionFromFilename(str5, i, strChooseFilename, iLastIndexOf);
            strChooseFilename = strSubstring;
        }
        synchronized (sUniqueLock) {
            File file3 = new File(absoluteFile, generateAvailableFilenameLocked(fileArr, strChooseFilename, strChooseExtensionFromFilename));
            file3.createNewFile();
            absolutePath = file3.getAbsolutePath();
        }
        return absolutePath;
    }

    private static String chooseFilename(String str, String str2, String str3, String str4) {
        String strDecode;
        int iLastIndexOf;
        String strDecode2;
        if (str2 != null && !str2.endsWith("/")) {
            if (Constants.LOGVV) {
                Log.v("DownloadManager", "getting filename from hint");
            }
            int iLastIndexOf2 = str2.lastIndexOf(47) + 1;
            if (iLastIndexOf2 > 0) {
                str2 = str2.substring(iLastIndexOf2);
            }
        } else {
            str2 = null;
        }
        if (str2 == null && str3 != null && (str2 = parseContentDisposition(str3)) != null) {
            if (Constants.LOGVV) {
                Log.v("DownloadManager", "getting filename from content-disposition");
            }
            int iLastIndexOf3 = str2.lastIndexOf(47) + 1;
            if (iLastIndexOf3 > 0) {
                str2 = str2.substring(iLastIndexOf3);
            }
        }
        if (str2 == null && str4 != null && (strDecode2 = Uri.decode(str4)) != null && !strDecode2.endsWith("/") && strDecode2.indexOf(63) < 0) {
            if (Constants.LOGVV) {
                Log.v("DownloadManager", "getting filename from content-location");
            }
            int iLastIndexOf4 = strDecode2.lastIndexOf(47) + 1;
            if (iLastIndexOf4 > 0) {
                str2 = strDecode2.substring(iLastIndexOf4);
            } else {
                str2 = strDecode2;
            }
        }
        if (str2 == null && (strDecode = Uri.decode(str)) != null && !strDecode.endsWith("/") && strDecode.indexOf(63) < 0 && (iLastIndexOf = strDecode.lastIndexOf(47) + 1) > 0) {
            if (Constants.LOGVV) {
                Log.v("DownloadManager", "getting filename from uri");
            }
            str2 = strDecode.substring(iLastIndexOf);
        }
        if (str2 == null) {
            if (Constants.LOGVV) {
                Log.v("DownloadManager", "using default filename");
            }
            str2 = "downloadfile";
        }
        return FileUtils.buildValidFatFilename(str2);
    }

    private static String chooseExtensionFromMimeType(String str, boolean z) {
        String extensionFromMimeType;
        if (str != null) {
            extensionFromMimeType = MimeTypeMap.getSingleton().getExtensionFromMimeType(str);
            if (extensionFromMimeType != null) {
                if (Constants.LOGVV) {
                    Log.v("DownloadManager", "adding extension from type");
                }
                extensionFromMimeType = "." + extensionFromMimeType;
            } else if (Constants.LOGVV) {
                Log.v("DownloadManager", "couldn't find extension for " + str);
            }
        } else {
            extensionFromMimeType = null;
        }
        if (extensionFromMimeType == null) {
            if (str != null && str.toLowerCase().startsWith("text/")) {
                if (str.equalsIgnoreCase("text/html")) {
                    if (Constants.LOGVV) {
                        Log.v("DownloadManager", "adding default html extension");
                    }
                    return ".html";
                }
                if (z) {
                    if (Constants.LOGVV) {
                        Log.v("DownloadManager", "adding default text extension");
                    }
                    return ".txt";
                }
                return extensionFromMimeType;
            }
            if (z) {
                if (Constants.LOGVV) {
                    Log.v("DownloadManager", "adding default binary extension");
                }
                return ".bin";
            }
            return extensionFromMimeType;
        }
        return extensionFromMimeType;
    }

    private static String chooseExtensionFromFilename(String str, int i, String str2, int i2) {
        String strChooseExtensionFromMimeType;
        String mimeTypeFromExtension;
        if (str != null && ((mimeTypeFromExtension = MimeTypeMap.getSingleton().getMimeTypeFromExtension(str2.substring(i2 + 1))) == null || !mimeTypeFromExtension.equalsIgnoreCase(str))) {
            strChooseExtensionFromMimeType = chooseExtensionFromMimeType(str, false);
            if (strChooseExtensionFromMimeType != null) {
                if (Constants.LOGVV) {
                    Log.v("DownloadManager", "substituting extension from type");
                }
            } else if (Constants.LOGVV) {
                Log.v("DownloadManager", "couldn't find extension for " + str);
            }
        } else {
            strChooseExtensionFromMimeType = null;
        }
        if (strChooseExtensionFromMimeType == null) {
            if (Constants.LOGVV) {
                Log.v("DownloadManager", "keeping extension");
            }
            return str2.substring(i2);
        }
        return strChooseExtensionFromMimeType;
    }

    private static boolean isFilenameAvailableLocked(File[] fileArr, String str) {
        if ("recovery".equalsIgnoreCase(str)) {
            return false;
        }
        for (File file : fileArr) {
            if (new File(file, str).exists()) {
                return false;
            }
        }
        return true;
    }

    private static String generateAvailableFilenameLocked(File[] fileArr, String str, String str2) throws IOException {
        String str3 = str + str2;
        if (isFilenameAvailableLocked(fileArr, str3)) {
            return str3;
        }
        int iNextInt = 1;
        for (int i = 1; i < 1000000000; i *= 10) {
            for (int i2 = 0; i2 < 9; i2++) {
                String str4 = str + "-" + iNextInt + str2;
                if (!isFilenameAvailableLocked(fileArr, str4)) {
                    iNextInt += sRandom.nextInt(i) + 1;
                } else {
                    return str4;
                }
            }
        }
        throw new IOException("Failed to generate an available filename");
    }

    static boolean isFilenameValid(Context context, File file) {
        return isFilenameValid(context, file, true);
    }

    static boolean isFilenameValidInExternal(Context context, File file) {
        return isFilenameValid(context, file, false);
    }

    static boolean isFilenameValidInExternalPackage(Context context, File file, String str) {
        try {
            if (!containsCanonical(Environment.buildExternalStorageAppFilesDirs(str), file) && !containsCanonical(Environment.buildExternalStorageAppObbDirs(str), file) && !containsCanonical(Environment.buildExternalStorageAppCacheDirs(str), file)) {
                if (containsCanonical(Environment.buildExternalStorageAppMediaDirs(str), file)) {
                    return true;
                }
                Log.w("DownloadManager", "Path appears to be invalid: " + file);
                return false;
            }
            return true;
        } catch (IOException e) {
            Log.w("DownloadManager", "Failed to resolve canonical path: " + e);
            return false;
        }
    }

    static boolean isFilenameValid(Context context, File file, boolean z) {
        if (z) {
            try {
                if (containsCanonical(context.getFilesDir(), file) || containsCanonical(context.getCacheDir(), file) || containsCanonical(Environment.getDownloadCacheDirectory(), file)) {
                    return true;
                }
            } catch (IOException e) {
                Log.w("DownloadManager", "Failed to resolve canonical path: " + e);
                return false;
            }
        }
        for (StorageVolume storageVolume : StorageManager.getVolumeList(UserHandle.myUserId(), 256)) {
            if (containsCanonical(storageVolume.getPathFile(), file)) {
                return true;
            }
        }
        Log.w("DownloadManager", "Path appears to be invalid: " + file);
        return false;
    }

    private static boolean containsCanonical(File file, File file2) throws IOException {
        return FileUtils.contains(file.getCanonicalFile(), file2);
    }

    private static boolean containsCanonical(File[] fileArr, File file) throws IOException {
        for (File file2 : fileArr) {
            if (containsCanonical(file2, file)) {
                return true;
            }
        }
        return false;
    }

    public static File getRunningDestinationDirectory(Context context, int i) throws IOException {
        return getDestinationDirectory(context, i, true);
    }

    public static File getSuccessDestinationDirectory(Context context, int i) throws IOException {
        return getDestinationDirectory(context, i, false);
    }

    private static File getDestinationDirectory(Context context, int i, boolean z) throws IOException {
        switch (i) {
            case 0:
                File file = new File(Environment.getExternalStorageDirectory(), Environment.DIRECTORY_DOWNLOADS);
                if (!file.isDirectory() && file.mkdirs()) {
                    throw new IOException("unable to create external downloads directory");
                }
                return file;
            case 1:
            case 2:
            case 3:
                if (z) {
                    return context.getFilesDir();
                }
                return context.getCacheDir();
            default:
                throw new IllegalStateException("unexpected destination: " + i);
        }
    }
}
