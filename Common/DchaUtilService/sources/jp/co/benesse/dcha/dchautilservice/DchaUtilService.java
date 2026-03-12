package jp.co.benesse.dcha.dchautilservice;

import android.app.ActivityManager;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserManager;
import android.provider.Settings;
import android.text.TextUtils;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import jp.co.benesse.dcha.dchautilservice.IDchaUtilService;
import jp.co.benesse.dcha.util.Logger;
import jp.co.benesse.dcha.util.ReflectionUtils;
import jp.co.benesse.dcha.util.StorageManagerAdapter;
import jp.co.benesse.dcha.util.WindowManagerAdapter;

public class DchaUtilService extends Service {
    private static final String PACKAGE_SHO_HOME = "jp.co.benesse.touch.allgrade.b003.touchhomelauncher";
    public static final String TAG = DchaUtilService.class.getSimpleName();
    protected IDchaUtilService.Stub mDchaUtilServiceStub = new IDchaUtilService.Stub() {
        @Override
        public boolean setForcedDisplaySize(int width, int height) {
            int callPid = getCallingPid();
            String callPkg = getPackageNameFromPid(callPid);
            Logger.d(DchaUtilService.TAG, "setForcedDisplaySize callPid:", Integer.valueOf(callPid), ", callPkg:", callPkg);
            Logger.d(DchaUtilService.TAG, "setForcedDisplaySize 0001 width:", Integer.valueOf(width), " height:", Integer.valueOf(height));
            boolean result = WindowManagerAdapter.setForcedDisplaySize(DchaUtilService.this.getApplicationContext(), width, height);
            Logger.d(DchaUtilService.TAG, "setForcedDisplaySize 0002 result:", Boolean.valueOf(result));
            return result;
        }

        @Override
        public int[] getDisplaySize() {
            int callPid = getCallingPid();
            String callPkg = getPackageNameFromPid(callPid);
            Logger.d(DchaUtilService.TAG, "getDisplaySize callPid:", Integer.valueOf(callPid), ", callPkg:", callPkg);
            Logger.d(DchaUtilService.TAG, "getDisplaySize 0001");
            int[] size = WindowManagerAdapter.getDisplaySize(DchaUtilService.this.getApplicationContext());
            Logger.d(DchaUtilService.TAG, "getDisplaySize 0002 width:", Integer.valueOf(size[0]), " height:", Integer.valueOf(size[1]));
            return size;
        }

        @Override
        public int[] getLcdSize() {
            int callPid = getCallingPid();
            String callPkg = getPackageNameFromPid(callPid);
            Logger.d(DchaUtilService.TAG, "getLcdSize callPid:", Integer.valueOf(callPid), ", callPkg:", callPkg);
            Logger.d(DchaUtilService.TAG, "getLcdSize 0001");
            int[] size = {0, 0};
            if (DchaUtilService.this.existsPackage(DchaUtilService.PACKAGE_SHO_HOME)) {
                size[0] = 1280;
                size[1] = 800;
            } else {
                size = WindowManagerAdapter.getLcdSize(DchaUtilService.this.getApplicationContext());
            }
            Logger.d(DchaUtilService.TAG, "getLcdSize 0002 width:", Integer.valueOf(size[0]), " height:", Integer.valueOf(size[1]));
            return size;
        }

        @Override
        public void sdUnmount() throws RemoteException {
            int callPid = getCallingPid();
            String callPkg = getPackageNameFromPid(callPid);
            Logger.d(DchaUtilService.TAG, "sdUnmount callPid:", Integer.valueOf(callPid), ", callPkg:", callPkg);
            Logger.d(DchaUtilService.TAG, "sdUnmount 0001");
            DchaUtilService.this.unmountExternalStorage();
            Logger.d(DchaUtilService.TAG, "sdUnmount 0002");
        }

        @Override
        public boolean copyFile(String srcFilePath, String dstFilePath) throws Throwable {
            String extStoragePath;
            int callPid = getCallingPid();
            String callPkg = getPackageNameFromPid(callPid);
            Logger.d(DchaUtilService.TAG, "copyFile callPid:", Integer.valueOf(callPid), ", callPkg:", callPkg);
            Logger.d(DchaUtilService.TAG, "copyFile 0001 srcFilePath:", srcFilePath, " dstFilePath:", dstFilePath);
            boolean result = false;
            try {
                extStoragePath = DchaUtilService.this.getCanonicalPath(System.getenv("SECONDARY_STORAGE"));
            } catch (Exception e) {
                Logger.d(DchaUtilService.TAG, "copyFile 0006", e);
            }
            if (TextUtils.isEmpty(srcFilePath) || TextUtils.isEmpty(dstFilePath)) {
                Logger.d(DchaUtilService.TAG, "copyFile 0002");
                throw new IllegalArgumentException();
            }
            String srcCanonicalPath = DchaUtilService.this.getCanonicalPath(srcFilePath);
            String dstCanonicalPath = DchaUtilService.this.getCanonicalPath(dstFilePath);
            if (!srcCanonicalPath.startsWith(extStoragePath) && !dstCanonicalPath.startsWith(extStoragePath)) {
                Logger.d(DchaUtilService.TAG, "copyFile 0003");
                throw new SecurityException("The path is not a external storage.");
            }
            File srcFile = new File(srcCanonicalPath);
            File dstFile = new File(dstCanonicalPath);
            if (srcFile.isFile() && (dstFile.isDirectory() || dstFile.isFile() || (!dstFile.exists() && dstFile.getParentFile().isDirectory()))) {
                Logger.d(DchaUtilService.TAG, "copyFile 0004");
                result = fileCopy(srcFile, dstFile);
                Logger.d(DchaUtilService.TAG, "copyFile 0007: ", Boolean.valueOf(result));
                return result;
            }
            Logger.d(DchaUtilService.TAG, "copyFile 0005");
            throw new IllegalArgumentException();
        }

        @Override
        public boolean copyDirectory(String srcDirPath, String dstDirPath, boolean makeTopDir) {
            String extStoragePath;
            int callPid = getCallingPid();
            String callPkg = getPackageNameFromPid(callPid);
            Logger.d(DchaUtilService.TAG, "copyDirectory callPid:", Integer.valueOf(callPid), ", callPkg:", callPkg);
            Logger.d(DchaUtilService.TAG, "copyDirectory callPkg:", callPkg);
            Logger.d(DchaUtilService.TAG, "copyDirectory 0001 srcDirPath:", srcDirPath, " dstDirPath:", dstDirPath, " makeTopDir:", Boolean.valueOf(makeTopDir));
            boolean result = false;
            try {
                extStoragePath = DchaUtilService.this.getCanonicalPath(System.getenv("SECONDARY_STORAGE"));
            } catch (Exception e) {
                Logger.d(DchaUtilService.TAG, "copyDirectory 0006", e);
            }
            if (TextUtils.isEmpty(srcDirPath) || TextUtils.isEmpty(dstDirPath)) {
                Logger.d(DchaUtilService.TAG, "copyDirectory 0002");
                throw new IllegalArgumentException();
            }
            String srcCanonicalPath = DchaUtilService.this.getCanonicalPath(srcDirPath);
            String dstCanonicalPath = DchaUtilService.this.getCanonicalPath(dstDirPath);
            if (!srcCanonicalPath.startsWith(extStoragePath) && !dstCanonicalPath.startsWith(extStoragePath)) {
                Logger.d(DchaUtilService.TAG, "copyDirectory 0003");
                throw new SecurityException("The path is not a external storage.");
            }
            File srcFile = new File(srcCanonicalPath);
            File dstFile = new File(dstCanonicalPath);
            if (srcFile.isDirectory() && dstFile.isDirectory()) {
                Logger.d(DchaUtilService.TAG, "copyDirectory 0004");
                result = directoryCopy(srcFile, dstFile, makeTopDir);
                Logger.d(DchaUtilService.TAG, "copyDirectory 0007 result:", Boolean.valueOf(result));
                return result;
            }
            Logger.d(DchaUtilService.TAG, "copyDirectory 0005");
            throw new IllegalArgumentException();
        }

        protected boolean directoryCopy(File srcDir, File destDir, boolean makeTopDir) {
            File copyDir;
            Logger.d(DchaUtilService.TAG, "directoryCopy 0001");
            if (!srcDir.exists()) {
                Logger.d(DchaUtilService.TAG, "directoryCopy 0002");
                return false;
            }
            File[] fromFile = srcDir.listFiles();
            if (makeTopDir) {
                Logger.d(DchaUtilService.TAG, "directoryCopy 0003");
                copyDir = new File(destDir.getPath() + File.separator + srcDir.getName());
            } else {
                Logger.d(DchaUtilService.TAG, "directoryCopy 0004");
                copyDir = destDir;
            }
            if (!copyDir.exists()) {
                Logger.d(DchaUtilService.TAG, "directoryCopy 0005");
                if (!copyDir.mkdir()) {
                    Logger.d(DchaUtilService.TAG, "directoryCopy 0006");
                    return false;
                }
                copyDir.setReadable(true, false);
                copyDir.setWritable(true, false);
                copyDir.setExecutable(true, false);
            }
            if (fromFile != null) {
                Logger.d(DchaUtilService.TAG, "directoryCopy 0007");
                for (File f : fromFile) {
                    if (f.isFile()) {
                        Logger.d(DchaUtilService.TAG, "directoryCopy 0008");
                        if (!fileCopy(f, copyDir)) {
                            Logger.d(DchaUtilService.TAG, "directoryCopy 0009");
                            return false;
                        }
                    } else {
                        Logger.d(DchaUtilService.TAG, "directoryCopy 0010");
                        if (!directoryCopy(f, copyDir, true)) {
                            Logger.d(DchaUtilService.TAG, "directoryCopy 0011");
                            return false;
                        }
                    }
                }
            }
            Logger.d(DchaUtilService.TAG, "directoryCopy 0012");
            return true;
        }

        protected boolean fileCopy(File src, File dest) throws Throwable {
            File copyFile;
            FileInputStream inputStream;
            FileOutputStream outputStream;
            Logger.d(DchaUtilService.TAG, "fileCopy 0001");
            if (dest.isDirectory()) {
                Logger.d(DchaUtilService.TAG, "fileCopy 0002");
                copyFile = new File(dest.getPath() + File.separator + src.getName());
            } else {
                Logger.d(DchaUtilService.TAG, "fileCopy 0003");
                copyFile = dest;
            }
            Closeable inputStream2 = null;
            Closeable outputStream2 = null;
            FileChannel channelFrom = null;
            FileChannel channelTo = null;
            try {
                try {
                    copyFile.createNewFile();
                    inputStream = new FileInputStream(src);
                    try {
                        channelFrom = inputStream.getChannel();
                        outputStream = new FileOutputStream(copyFile);
                    } catch (Exception e) {
                        e = e;
                        inputStream2 = inputStream;
                    } catch (Throwable th) {
                        th = th;
                        inputStream2 = inputStream;
                    }
                } catch (Exception e2) {
                    e = e2;
                }
            } catch (Throwable th2) {
                th = th2;
            }
            try {
                channelTo = outputStream.getChannel();
                channelFrom.transferTo(0L, channelFrom.size(), channelTo);
                copyFile.setLastModified(src.lastModified());
                copyFile.setReadable(true, false);
                copyFile.setWritable(true, false);
                copyFile.setExecutable(true, false);
                Logger.d(DchaUtilService.TAG, "fileCopy 0004");
                Logger.d(DchaUtilService.TAG, "fileCopy 0006");
                close(channelFrom);
                close(channelTo);
                close(inputStream);
                close(outputStream);
                return true;
            } catch (Exception e3) {
                e = e3;
                outputStream2 = outputStream;
                inputStream2 = inputStream;
                Logger.d(DchaUtilService.TAG, "fileCopy 0005", e);
                Logger.d(DchaUtilService.TAG, "fileCopy 0006");
                close(channelFrom);
                close(channelTo);
                close(inputStream2);
                close(outputStream2);
                Logger.d(DchaUtilService.TAG, "fileCopy 0007");
                return false;
            } catch (Throwable th3) {
                th = th3;
                outputStream2 = outputStream;
                inputStream2 = inputStream;
                Logger.d(DchaUtilService.TAG, "fileCopy 0006");
                close(channelFrom);
                close(channelTo);
                close(inputStream2);
                close(outputStream2);
                throw th;
            }
        }

        protected void close(Closeable closeable) {
            Logger.d(DchaUtilService.TAG, "close 0001");
            if (closeable == null) {
                Logger.d(DchaUtilService.TAG, "close 0002");
                return;
            }
            try {
                closeable.close();
            } catch (IOException e) {
                Logger.d(DchaUtilService.TAG, "close 0003");
            }
            Logger.d(DchaUtilService.TAG, "close 0004");
        }

        @Override
        public boolean deleteFile(String path) {
            String extStoragePath;
            int callPid = getCallingPid();
            String callPkg = getPackageNameFromPid(callPid);
            Logger.d(DchaUtilService.TAG, "deleteFile callPid:", Integer.valueOf(callPid), ", callPkg:", callPkg);
            Logger.d(DchaUtilService.TAG, "deleteFile 0001 path:", path);
            boolean result = false;
            try {
                extStoragePath = DchaUtilService.this.getCanonicalPath(System.getenv("SECONDARY_STORAGE"));
            } catch (Exception e) {
                Logger.d(DchaUtilService.TAG, "deleteFile 0007", e);
            }
            if (TextUtils.isEmpty(path)) {
                Logger.d(DchaUtilService.TAG, "deleteFile 0002");
                throw new IllegalArgumentException();
            }
            String canonicalPath = DchaUtilService.this.getCanonicalPath(path);
            if (!canonicalPath.startsWith(extStoragePath)) {
                Logger.d(DchaUtilService.TAG, "deleteFile 0003");
                throw new SecurityException("The path is not a external storage.");
            }
            File file = new File(canonicalPath);
            if (file.isDirectory()) {
                Logger.d(DchaUtilService.TAG, "deleteFile 0004");
                File[] fromFile = file.listFiles();
                if (fromFile == null) {
                    Logger.d(DchaUtilService.TAG, "deleteFile 0005");
                    return false;
                }
                for (File f : fromFile) {
                    if (!deleteFile(f.getCanonicalPath())) {
                        Logger.d(DchaUtilService.TAG, "deleteFile 0006");
                        return false;
                    }
                }
            }
            result = file.delete();
            Logger.d(DchaUtilService.TAG, "deleteFile 0008 result:", Boolean.valueOf(result));
            return result;
        }

        @Override
        public boolean makeDir(String path, String dirname) {
            String extStoragePath;
            int callPid = getCallingPid();
            String callPkg = getPackageNameFromPid(callPid);
            Logger.d(DchaUtilService.TAG, "makeDir callPid:", Integer.valueOf(callPid), ", callPkg:", callPkg);
            Logger.d(DchaUtilService.TAG, "makeDir 0001 dirname:", dirname);
            boolean result = false;
            try {
                extStoragePath = DchaUtilService.this.getCanonicalPath(System.getenv("SECONDARY_STORAGE"));
            } catch (Exception e) {
                Logger.d(DchaUtilService.TAG, "makeDir 0005", e);
            }
            if (TextUtils.isEmpty(path)) {
                Logger.d(DchaUtilService.TAG, "makeDir 0002");
                throw new IllegalArgumentException();
            }
            String canonicalPath = DchaUtilService.this.getCanonicalPath(path);
            if (!canonicalPath.startsWith(extStoragePath)) {
                Logger.d(DchaUtilService.TAG, "makeDir 0003");
                throw new SecurityException("The path is not a external storage.");
            }
            File basePath = new File(canonicalPath);
            if (basePath.isDirectory()) {
                Logger.d(DchaUtilService.TAG, "makeDir 0004");
                File dir = new File(basePath, dirname);
                result = dir.mkdir();
                dir.setReadable(true, false);
                dir.setWritable(true, false);
                dir.setExecutable(true, false);
            }
            Logger.d(DchaUtilService.TAG, "makeDir 0006 result:", Boolean.valueOf(result));
            return result;
        }

        @Override
        public void hideNavigationBar(boolean hide) {
            int callPid = getCallingPid();
            String callPkg = getPackageNameFromPid(callPid);
            Logger.d(DchaUtilService.TAG, "hideNavigationBar callPid:", Integer.valueOf(callPid), ", callPkg:", callPkg);
            Logger.d(DchaUtilService.TAG, "hideNavigationBar 0001 hide:", Boolean.valueOf(hide));
            Settings.System.putInt(DchaUtilService.this.getContentResolver(), "hide_navigation_bar", hide ? 1 : 0);
            Logger.d(DchaUtilService.TAG, "hideNavigationBar 0002");
        }

        @Override
        public int getUserCount() {
            int callPid = getCallingPid();
            String callPkg = getPackageNameFromPid(callPid);
            Logger.d(DchaUtilService.TAG, "getUserCount callPid:", Integer.valueOf(callPid), ", callPkg:", callPkg);
            Logger.d(DchaUtilService.TAG, "getUserCount 0001");
            UserManager um = (UserManager) DchaUtilService.this.getSystemService("user");
            int count = um.getUserCount();
            Logger.d(DchaUtilService.TAG, "getUserCount return:", Integer.valueOf(count));
            return count;
        }

        @Override
        public void setDefaultPreferredHomeApp(String packageName) throws RemoteException {
            int callPid = getCallingPid();
            String callPkg = getPackageNameFromPid(callPid);
            Logger.d(DchaUtilService.TAG, "setDefaultPreferredHomeApp callPid:", Integer.valueOf(callPid), ", callPkg:", callPkg);
            try {
                Logger.d(DchaUtilService.TAG, "setDefalutPreferredHomeApp 0001 packageName:", packageName);
                PackageManager pm = DchaUtilService.this.getPackageManager();
                ComponentName defaultHomeComponentName = null;
                IntentFilter filter = new IntentFilter();
                filter.addAction("android.intent.action.MAIN");
                filter.addCategory("android.intent.category.HOME");
                filter.addCategory("android.intent.category.DEFAULT");
                Intent intent = new Intent("android.intent.action.MAIN");
                intent.addCategory("android.intent.category.HOME");
                intent.addCategory("android.intent.category.DEFAULT");
                List<ResolveInfo> homeApps = pm.queryIntentActivities(intent, 0);
                List<ComponentName> compNames = new ArrayList<>();
                for (ResolveInfo homeApp : homeApps) {
                    String packName = homeApp.activityInfo.applicationInfo.packageName;
                    String activityName = homeApp.activityInfo.name;
                    Logger.d(DchaUtilService.TAG, "setDefalutPreferredHomeApp packName:" + packName);
                    Logger.d(DchaUtilService.TAG, "setDefalutPreferredHomeApp activityName:" + activityName);
                    ComponentName compname = new ComponentName(packName, activityName);
                    compNames.add(compname);
                    if (packName.equalsIgnoreCase(packageName)) {
                        Logger.d(DchaUtilService.TAG, "setDefalutPreferredHomeApp 0002");
                        defaultHomeComponentName = compname;
                        Logger.d(DchaUtilService.TAG, "setDefalutPreferredHomeApp defaultHomeComponentName:" + defaultHomeComponentName);
                    }
                }
                ComponentName[] homeAppSet = (ComponentName[]) compNames.toArray(new ComponentName[compNames.size()]);
                if (defaultHomeComponentName != null) {
                    Logger.d(DchaUtilService.TAG, "setDefalutPreferredHomeApp 0003");
                    Method method = null;
                    try {
                        method = PackageManager.class.getDeclaredMethod("addPreferredActivity", IntentFilter.class, Integer.TYPE, ComponentName[].class, ComponentName.class, Integer.TYPE);
                    } catch (NoSuchMethodException e) {
                    }
                    if (method == null) {
                        method = PackageManager.class.getDeclaredMethod("addPreferredActivityAsUser", IntentFilter.class, Integer.TYPE, ComponentName[].class, ComponentName.class, Integer.TYPE);
                    }
                    method.setAccessible(true);
                    method.invoke(pm, filter, 1081344, homeAppSet, defaultHomeComponentName, 0);
                }
                Logger.d(DchaUtilService.TAG, "setDefalutPreferredHomeApp 0005");
            } catch (Exception e2) {
                Logger.e(DchaUtilService.TAG, "setDefalutPrefferredHomeApp 0004", e2);
                throw new RemoteException(e2.toString());
            }
        }

        @Override
        public void clearDefaultPreferredApp(String packageName) throws RemoteException {
            int callPid = getCallingPid();
            String callPkg = getPackageNameFromPid(callPid);
            Logger.d(DchaUtilService.TAG, "clearDefaultPreferredApp callPid:", Integer.valueOf(callPid), ", callPkg:", callPkg);
            Logger.d(DchaUtilService.TAG, "clearDefaultPreferredApp 0001 packageName:", packageName);
            try {
                PackageManager pm = DchaUtilService.this.getPackageManager();
                pm.clearPackagePreferredActivities(packageName);
                Logger.d(DchaUtilService.TAG, "clearDefaultPreferredApp 0003");
            } catch (Exception e) {
                Logger.e(DchaUtilService.TAG, "clearDefaultPreferredApp 0002", e);
                throw new RemoteException(e.toString());
            }
        }

        @Override
        public String getCanonicalExternalPath(String linkPath) throws RemoteException {
            int callPid = getCallingPid();
            String callPkg = getPackageNameFromPid(callPid);
            Logger.d(DchaUtilService.TAG, "getCanonicalExternalPath callPid:", Integer.valueOf(callPid), ", callPkg:", callPkg);
            Logger.d(DchaUtilService.TAG, "getCanonicalExternalPath 0001");
            if (TextUtils.isEmpty(linkPath)) {
                Logger.d(DchaUtilService.TAG, "getCanonicalExternalPath 0002");
                return linkPath;
            }
            String canonicalPath = linkPath;
            try {
                canonicalPath = new File(linkPath).getCanonicalPath();
            } catch (Exception e) {
                Logger.d(DchaUtilService.TAG, "getCanonicalExternalPath 0003", e);
            }
            if (Build.VERSION.SDK_INT >= 28) {
                Logger.d(DchaUtilService.TAG, "getCanonicalExternalPath 0004");
                String extStoragePath = StorageManagerAdapter.getPath(DchaUtilService.this.getApplicationContext());
                if (!TextUtils.isEmpty(extStoragePath)) {
                    Logger.d(DchaUtilService.TAG, "getCanonicalExternalPath 0005");
                    String secondaryStoragePath = System.getenv("SECONDARY_STORAGE");
                    if (canonicalPath.startsWith(secondaryStoragePath)) {
                        Logger.d(DchaUtilService.TAG, "getCanonicalExternalPath 0006");
                        canonicalPath = canonicalPath.replace(secondaryStoragePath, extStoragePath);
                    }
                }
            }
            Logger.d(DchaUtilService.TAG, "getCanonicalExternalPath 0007 return:", canonicalPath);
            return canonicalPath;
        }

        @Override
        public String[] listFiles(String path) {
            String extStoragePath;
            int callPid = getCallingPid();
            String callPkg = getPackageNameFromPid(callPid);
            Logger.d(DchaUtilService.TAG, "listFiles callPid:", Integer.valueOf(callPid), ", callPkg:", callPkg);
            Logger.d(DchaUtilService.TAG, "listFiles 0001 path:", path);
            String[] resultList = null;
            try {
                extStoragePath = getCanonicalExternalPath(System.getenv("SECONDARY_STORAGE"));
            } catch (Exception e) {
                Logger.d(DchaUtilService.TAG, "listFiles 0005", e);
            }
            if (TextUtils.isEmpty(path)) {
                Logger.d(DchaUtilService.TAG, "listFiles 0002");
                throw new IllegalArgumentException();
            }
            String canonicalPath = getCanonicalExternalPath(path);
            if (!canonicalPath.startsWith(extStoragePath)) {
                Logger.d(DchaUtilService.TAG, "listFiles 0003");
                throw new SecurityException("The path is not a external storage.");
            }
            File file = new File(canonicalPath);
            File[] listFiles = file.listFiles();
            if (listFiles != null) {
                Logger.d(DchaUtilService.TAG, "listFiles 0004");
                resultList = new String[listFiles.length];
                int len$ = listFiles.length;
                int i$ = 0;
                int count = 0;
                while (i$ < len$) {
                    File f = listFiles[i$];
                    int count2 = count + 1;
                    resultList[count] = f.getCanonicalPath();
                    i$++;
                    count = count2;
                }
            }
            Logger.d(DchaUtilService.TAG, "listFiles 0006");
            return resultList;
        }

        @Override
        public boolean existsFile(String path) {
            String extStoragePath;
            int callPid = getCallingPid();
            String callPkg = getPackageNameFromPid(callPid);
            Logger.d(DchaUtilService.TAG, "existsFile callPid:", Integer.valueOf(callPid), ", callPkg:", callPkg);
            Logger.d(DchaUtilService.TAG, "existsFile 0001 path:", path);
            boolean result = false;
            try {
                extStoragePath = getCanonicalExternalPath(System.getenv("SECONDARY_STORAGE"));
            } catch (Exception e) {
                Logger.d(DchaUtilService.TAG, "existsFile 0004", e);
            }
            if (TextUtils.isEmpty(path)) {
                Logger.d(DchaUtilService.TAG, "existsFile 0002");
                throw new IllegalArgumentException();
            }
            String canonicalPath = getCanonicalExternalPath(path);
            if (!canonicalPath.startsWith(extStoragePath)) {
                Logger.d(DchaUtilService.TAG, "existsFile 0003");
                throw new SecurityException("The path is not a external storage.");
            }
            File file = new File(canonicalPath);
            result = file.exists();
            Logger.d(DchaUtilService.TAG, "existsFile 0005 result:", Boolean.valueOf(result));
            return result;
        }

        @Override
        public int[] getOriginalLcdSize() {
            int callPid = getCallingPid();
            String callPkg = getPackageNameFromPid(callPid);
            Logger.d(DchaUtilService.TAG, "getOriginalLcdSize callPid:", Integer.valueOf(callPid), ", callPkg:", callPkg);
            Logger.d(DchaUtilService.TAG, "getOriginalLcdSize 0001");
            int[] iArr = {0, 0};
            int[] size = WindowManagerAdapter.getLcdSize(DchaUtilService.this.getApplicationContext());
            Logger.d(DchaUtilService.TAG, "getLcdSize 0002 width:", Integer.valueOf(size[0]), " height:", Integer.valueOf(size[1]));
            return size;
        }

        @Override
        public void gotoSleep() throws RemoteException {
            int callPid = getCallingPid();
            String callPkg = getPackageNameFromPid(callPid);
            Logger.d(DchaUtilService.TAG, "gotoSleep callPid:", Integer.valueOf(callPid), ", callPkg:", callPkg);
            Logger.d(DchaUtilService.TAG, "gotoSleep 0001");
            PowerManager pm = (PowerManager) DchaUtilService.this.getSystemService("power");
            try {
                pm.getClass().getMethod("goToSleep", Long.TYPE).invoke(pm, Long.valueOf(SystemClock.uptimeMillis()));
            } catch (Exception e) {
                Logger.d(DchaUtilService.TAG, "gotoSleep 0002");
            }
            Logger.d(DchaUtilService.TAG, "gotoSleep 0003");
        }

        @Override
        public void initializeDevice() {
            Logger.d(DchaUtilService.TAG, "initializeDevice 0001");
            Intent it = new Intent("android.intent.action.MASTER_CLEAR");
            it.addFlags(16777216);
            DchaUtilService.this.sendBroadcast(it);
            Logger.d(DchaUtilService.TAG, "initializeDevice 0002");
        }

        protected String getPackageNameFromPid(int pid) {
            String pkgName = "Unknown";
            try {
                ActivityManager manager = (ActivityManager) DchaUtilService.this.getSystemService("activity");
                List<ActivityManager.RunningAppProcessInfo> appList = manager.getRunningAppProcesses();
                for (ActivityManager.RunningAppProcessInfo info : appList) {
                    if (pid == info.pid) {
                        pkgName = info.processName;
                    }
                }
            } catch (Exception e) {
            }
            return pkgName;
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        Logger.d(TAG, "onBind 0001");
        return this.mDchaUtilServiceStub;
    }

    public void unmountExternalStorage() throws RemoteException {
        Logger.d(TAG, "unmountExternalStorage 0001");
        try {
            if (Build.VERSION.SDK_INT >= 28) {
                Logger.d(TAG, "unmountExternalStorage 0002");
                StorageManagerAdapter.unmount(getApplicationContext());
            } else {
                Logger.d(TAG, "unmountExternalStorage 0003");
                ClassLoader loader = ClassLoader.getSystemClassLoader();
                String extStoragePath = System.getenv("SECONDARY_STORAGE");
                Object mountObj = ReflectionUtils.invokeDeclaredMethod(loader.loadClass("android.os.ServiceManager"), null, "getService", new Class[]{String.class}, new Object[]{"mount"});
                Object imountObj = ReflectionUtils.invokeDeclaredMethod(loader.loadClass("android.os.storage.IMountService$Stub"), null, "asInterface", new Class[]{IBinder.class}, new Object[]{mountObj});
                Class[] types = {String.class, Boolean.TYPE, Boolean.TYPE};
                Object[] args = {extStoragePath, true, false};
                ReflectionUtils.invokeDeclaredMethod(loader.loadClass("android.os.storage.IMountService"), imountObj, "unmountVolume", types, args);
            }
            Logger.d(TAG, "unmountExternalStorage 0005");
        } catch (Exception e) {
            Logger.d(TAG, "unmountExternalStorage 0004", e);
            throw new RemoteException();
        }
    }

    public String getCanonicalPath(String linkPath) {
        Logger.d(TAG, "getCanonicalPath 0001");
        if (TextUtils.isEmpty(linkPath)) {
            Logger.d(TAG, "getCanonicalPath 0002");
            return linkPath;
        }
        String canonicalPath = linkPath;
        try {
            canonicalPath = new File(linkPath).getCanonicalPath();
        } catch (Exception e) {
            Logger.d(TAG, "getCanonicalPath 0003", e);
        }
        if (Build.VERSION.SDK_INT >= 28) {
            Logger.d(TAG, "getCanonicalPath 0004");
            String path = StorageManagerAdapter.getInternalPath(getApplicationContext());
            if (!TextUtils.isEmpty(path)) {
                Logger.d(TAG, "getCanonicalPath 0005");
                String secondaryStoragePath = System.getenv("SECONDARY_STORAGE");
                String storageManagerPath = StorageManagerAdapter.getPath(getApplicationContext());
                if (canonicalPath.startsWith(secondaryStoragePath)) {
                    Logger.d(TAG, "getCanonicalPath 0006");
                    canonicalPath = canonicalPath.replace(secondaryStoragePath, path);
                } else if (canonicalPath.startsWith(storageManagerPath)) {
                    Logger.d(TAG, "getCanonicalPath 0007");
                    canonicalPath = canonicalPath.replace(storageManagerPath, path);
                }
            }
        }
        Logger.d(TAG, "getCanonicalPath 0008 return:", canonicalPath);
        return canonicalPath;
    }

    public boolean existsPackage(String packageName) {
        try {
            PackageManager pm = getPackageManager();
            for (PackageInfo pkgInfo : pm.getInstalledPackages(8704)) {
                if (TextUtils.equals(packageName, pkgInfo.packageName)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            Logger.d(TAG, "existsPackage", e);
            return false;
        }
    }
}
