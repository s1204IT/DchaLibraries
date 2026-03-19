package android.os;

import android.Manifest;
import android.app.backup.FullBackup;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.IRecoverySystemProgressListener;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.logging.MetricsLogger;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import sun.security.pkcs.PKCS7;
import sun.security.pkcs.SignerInfo;

public class RecoverySystem {
    private static final String LAST_PREFIX = "last_";
    private static final int LOG_FILE_MAX_LENGTH = 65536;
    private static final long PUBLISH_PROGRESS_INTERVAL_MS = 500;
    private static final String TAG = "RecoverySystem";
    private final IRecoverySystem mService;
    private static final File DEFAULT_KEYSTORE = new File("/system/etc/security/otacerts.zip");
    private static final File RECOVERY_DIR = new File("/cache/recovery");
    private static final File LOG_FILE = new File(RECOVERY_DIR, "log");
    private static final File LAST_INSTALL_FILE = new File(RECOVERY_DIR, "last_install");
    public static final File BLOCK_MAP_FILE = new File(RECOVERY_DIR, "block.map");
    public static final File UNCRYPT_PACKAGE_FILE = new File(RECOVERY_DIR, "uncrypt_file");
    private static final Object sRequestLock = new Object();

    public interface ProgressListener {
        void onProgress(int i);
    }

    private static HashSet<X509Certificate> getTrustedCerts(File keystore) throws GeneralSecurityException, IOException {
        HashSet<X509Certificate> trusted = new HashSet<>();
        if (keystore == null) {
            keystore = DEFAULT_KEYSTORE;
        }
        ZipFile zip = new ZipFile(keystore);
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                InputStream is = zip.getInputStream(entry);
                try {
                    trusted.add((X509Certificate) cf.generateCertificate(is));
                } finally {
                }
            }
            return trusted;
        } finally {
            zip.close();
        }
    }

    public static void verifyPackage(File packageFile, final ProgressListener listener, File deviceCertsZipFile) throws GeneralSecurityException, IOException {
        final long fileLen = packageFile.length();
        final RandomAccessFile raf = new RandomAccessFile(packageFile, FullBackup.ROOT_TREE_TOKEN);
        try {
            final long startTimeMillis = System.currentTimeMillis();
            if (listener != null) {
                listener.onProgress(0);
            }
            raf.seek(fileLen - 6);
            byte[] footer = new byte[6];
            raf.readFully(footer);
            if (footer[2] != -1 || footer[3] != -1) {
                throw new SignatureException("no signature in file (no footer)");
            }
            final int commentSize = (footer[4] & 255) | ((footer[5] & 255) << 8);
            int signatureStart = (footer[0] & 255) | ((footer[1] & 255) << 8);
            byte[] eocd = new byte[commentSize + 22];
            raf.seek(fileLen - ((long) (commentSize + 22)));
            raf.readFully(eocd);
            if (eocd[0] != 80 || eocd[1] != 75 || eocd[2] != 5 || eocd[3] != 6) {
                throw new SignatureException("no signature in file (bad footer)");
            }
            for (int i = 4; i < eocd.length - 3; i++) {
                if (eocd[i] == 80 && eocd[i + 1] == 75 && eocd[i + 2] == 5 && eocd[i + 3] == 6) {
                    throw new SignatureException("EOCD marker found after start of EOCD");
                }
            }
            PKCS7 block = new PKCS7(new ByteArrayInputStream(eocd, (commentSize + 22) - signatureStart, signatureStart));
            X509Certificate[] certificates = block.getCertificates();
            if (certificates == null || certificates.length == 0) {
                throw new SignatureException("signature contains no certificates");
            }
            X509Certificate cert = certificates[0];
            PublicKey signatureKey = cert.getPublicKey();
            SignerInfo[] signerInfos = block.getSignerInfos();
            if (signerInfos == null || signerInfos.length == 0) {
                throw new SignatureException("signature contains no signedData");
            }
            SignerInfo signerInfo = signerInfos[0];
            boolean verified = false;
            if (deviceCertsZipFile == null) {
                deviceCertsZipFile = DEFAULT_KEYSTORE;
            }
            HashSet<X509Certificate> trusted = getTrustedCerts(deviceCertsZipFile);
            Iterator c$iterator = trusted.iterator();
            while (true) {
                if (!c$iterator.hasNext()) {
                    break;
                }
                X509Certificate c = (X509Certificate) c$iterator.next();
                if (c.getPublicKey().equals(signatureKey)) {
                    verified = true;
                    break;
                }
            }
            if (!verified) {
                throw new SignatureException("signature doesn't match any trusted key");
            }
            raf.seek(0L);
            SignerInfo verifyResult = block.verify(signerInfo, new InputStream() {
                long lastPublishTime;
                long toRead;
                long soFar = 0;
                int lastPercent = 0;

                {
                    this.toRead = (fileLen - ((long) commentSize)) - 2;
                    this.lastPublishTime = startTimeMillis;
                }

                @Override
                public int read() throws IOException {
                    throw new UnsupportedOperationException();
                }

                @Override
                public int read(byte[] b, int off, int len) throws IOException {
                    if (this.soFar >= this.toRead || Thread.currentThread().isInterrupted()) {
                        return -1;
                    }
                    int size = len;
                    if (this.soFar + ((long) len) > this.toRead) {
                        size = (int) (this.toRead - this.soFar);
                    }
                    int read = raf.read(b, off, size);
                    this.soFar += (long) read;
                    if (listener != null) {
                        long now = System.currentTimeMillis();
                        int p = (int) ((this.soFar * 100) / this.toRead);
                        if (p > this.lastPercent && now - this.lastPublishTime > RecoverySystem.PUBLISH_PROGRESS_INTERVAL_MS) {
                            this.lastPercent = p;
                            this.lastPublishTime = now;
                            listener.onProgress(this.lastPercent);
                        }
                    }
                    return read;
                }
            });
            boolean interrupted = Thread.interrupted();
            if (listener != null) {
                listener.onProgress(100);
            }
            if (interrupted) {
                throw new SignatureException("verification was interrupted");
            }
            if (verifyResult == null) {
                throw new SignatureException("signature digest verification failed");
            }
        } finally {
            raf.close();
        }
    }

    public static void processPackage(Context context, File packageFile, ProgressListener listener, Handler handler) throws IOException {
        Handler progressHandler;
        String filename = packageFile.getCanonicalPath();
        if (!filename.startsWith("/data/")) {
            return;
        }
        RecoverySystem rs = (RecoverySystem) context.getSystemService("recovery");
        IRecoverySystemProgressListener progressListener = null;
        if (listener != null) {
            if (handler != null) {
                progressHandler = handler;
            } else {
                progressHandler = new Handler(context.getMainLooper());
            }
            progressListener = new AnonymousClass2(progressHandler, listener);
        }
        if (rs.uncrypt(filename, progressListener)) {
        } else {
            throw new IOException("process package failed");
        }
    }

    static class AnonymousClass2 extends IRecoverySystemProgressListener.Stub {
        int lastProgress = 0;
        long lastPublishTime = System.currentTimeMillis();
        final ProgressListener val$listener;
        final Handler val$progressHandler;

        AnonymousClass2(Handler val$progressHandler, ProgressListener val$listener) {
            this.val$progressHandler = val$progressHandler;
            this.val$listener = val$listener;
        }

        @Override
        public void onProgress(final int progress) {
            final long now = System.currentTimeMillis();
            Handler handler = this.val$progressHandler;
            final ProgressListener progressListener = this.val$listener;
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (progress <= AnonymousClass2.this.lastProgress || now - AnonymousClass2.this.lastPublishTime <= RecoverySystem.PUBLISH_PROGRESS_INTERVAL_MS) {
                        return;
                    }
                    AnonymousClass2.this.lastProgress = progress;
                    AnonymousClass2.this.lastPublishTime = now;
                    progressListener.onProgress(progress);
                }
            });
        }
    }

    public static void processPackage(Context context, File packageFile, ProgressListener listener) throws IOException {
        processPackage(context, packageFile, listener, null);
    }

    public static void installPackage(Context context, File packageFile) throws IOException {
        installPackage(context, packageFile, false);
    }

    public static void installPackage(Context context, File packageFile, boolean processed) throws IOException {
        synchronized (sRequestLock) {
            LOG_FILE.delete();
            UNCRYPT_PACKAGE_FILE.delete();
            String filename = packageFile.getCanonicalPath();
            Log.w(TAG, "!!! REBOOTING TO INSTALL " + filename + " !!!");
            boolean securityUpdate = filename.endsWith("_s.zip");
            if (filename.startsWith("/data/")) {
                if (processed) {
                    if (!BLOCK_MAP_FILE.exists()) {
                        Log.e(TAG, "Package claimed to have been processed but failed to find the block map file.");
                        throw new IOException("Failed to find block map file");
                    }
                } else {
                    FileWriter uncryptFile = new FileWriter(UNCRYPT_PACKAGE_FILE);
                    try {
                        uncryptFile.write(filename + "\n");
                        uncryptFile.close();
                        if (!UNCRYPT_PACKAGE_FILE.setReadable(true, false) || !UNCRYPT_PACKAGE_FILE.setWritable(true, false)) {
                            Log.e(TAG, "Error setting permission for " + UNCRYPT_PACKAGE_FILE);
                        }
                        BLOCK_MAP_FILE.delete();
                    } catch (Throwable th) {
                        uncryptFile.close();
                        throw th;
                    }
                }
                filename = "@/cache/recovery/block.map";
            }
            String filenameArg = "--update_package=" + filename + "\n";
            String localeArg = "--locale=" + Locale.getDefault().toString() + "\n";
            String command = filenameArg + localeArg;
            if (securityUpdate) {
                command = command + "--security\n";
            }
            RecoverySystem rs = (RecoverySystem) context.getSystemService("recovery");
            if (!rs.setupBcb(command)) {
                throw new IOException("Setup BCB failed");
            }
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            pm.reboot(PowerManager.REBOOT_RECOVERY_UPDATE);
            throw new IOException("Reboot failed (no permissions?)");
        }
    }

    public static void scheduleUpdateOnBoot(Context context, File packageFile) throws IOException {
        String filename = packageFile.getCanonicalPath();
        boolean securityUpdate = filename.endsWith("_s.zip");
        if (filename.startsWith("/data/")) {
            filename = "@/cache/recovery/block.map";
        }
        String filenameArg = "--update_package=" + filename + "\n";
        String localeArg = "--locale=" + Locale.getDefault().toString() + "\n";
        String command = filenameArg + localeArg;
        if (securityUpdate) {
            command = command + "--security\n";
        }
        RecoverySystem rs = (RecoverySystem) context.getSystemService("recovery");
        if (rs.setupBcb(command)) {
        } else {
            throw new IOException("schedule update on boot failed");
        }
    }

    public static void cancelScheduledUpdate(Context context) throws IOException {
        RecoverySystem rs = (RecoverySystem) context.getSystemService("recovery");
        if (rs.clearBcb()) {
        } else {
            throw new IOException("cancel scheduled update failed");
        }
    }

    public static void rebootWipeUserData(Context context) throws IOException {
        rebootWipeUserData(context, false, context.getPackageName());
    }

    public static void rebootWipeUserData(Context context, String reason) throws IOException {
        rebootWipeUserData(context, false, reason);
    }

    public static void rebootWipeUserData(Context context, boolean shutdown) throws IOException {
        rebootWipeUserData(context, shutdown, context.getPackageName());
    }

    public static void rebootWipeUserData(Context context, boolean shutdown, String reason) throws IOException {
        UserManager um = (UserManager) context.getSystemService(Context.USER_SERVICE);
        if (um.hasUserRestriction(UserManager.DISALLOW_FACTORY_RESET)) {
            throw new SecurityException("Wiping data is not allowed for this user.");
        }
        final ConditionVariable condition = new ConditionVariable();
        Intent intent = new Intent("android.intent.action.MASTER_CLEAR_NOTIFICATION");
        intent.addFlags(268435456);
        context.sendOrderedBroadcastAsUser(intent, UserHandle.SYSTEM, Manifest.permission.MASTER_CLEAR, new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent2) {
                condition.open();
            }
        }, null, 0, null, null);
        condition.block();
        String shutdownArg = null;
        if (shutdown) {
            shutdownArg = "--shutdown_after";
        }
        String reasonArg = null;
        if (!TextUtils.isEmpty(reason)) {
            reasonArg = "--reason=" + sanitizeArg(reason);
        }
        String localeArg = "--locale=" + Locale.getDefault().toString();
        bootCommand(context, shutdownArg, "--wipe_data", reasonArg, localeArg);
    }

    public static void rebootWipeCache(Context context) throws IOException {
        rebootWipeCache(context, context.getPackageName());
    }

    public static void rebootWipeCache(Context context, String reason) throws IOException {
        String reasonArg = null;
        if (!TextUtils.isEmpty(reason)) {
            reasonArg = "--reason=" + sanitizeArg(reason);
        }
        String localeArg = "--locale=" + Locale.getDefault().toString();
        bootCommand(context, "--wipe_cache", reasonArg, localeArg);
    }

    private static void bootCommand(Context context, String... args) throws IOException {
        synchronized (sRequestLock) {
            LOG_FILE.delete();
            StringBuilder command = new StringBuilder();
            for (String arg : args) {
                if (!TextUtils.isEmpty(arg)) {
                    command.append(arg);
                    command.append("\n");
                }
            }
            RecoverySystem rs = (RecoverySystem) context.getSystemService("recovery");
            rs.setupBcb(command.toString());
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            pm.reboot("recovery");
            throw new IOException("Reboot failed (no permissions?)");
        }
    }

    private static void parseLastInstallLog(Context context) throws Throwable {
        Throwable th = null;
        BufferedReader bufferedReader = null;
        try {
            BufferedReader in = new BufferedReader(new FileReader(LAST_INSTALL_FILE));
            int bytesWrittenInMiB = -1;
            int bytesStashedInMiB = -1;
            int timeTotal = -1;
            int sourceVersion = -1;
            while (true) {
                try {
                    String line = in.readLine();
                    if (line == null) {
                        break;
                    }
                    int numIndex = line.indexOf(58);
                    if (numIndex != -1 && numIndex + 1 < line.length()) {
                        String numString = line.substring(numIndex + 1).trim();
                        try {
                            long parsedNum = Long.parseLong(numString);
                            try {
                                int scaled = line.startsWith("bytes") ? Math.toIntExact(parsedNum / 1048576) : Math.toIntExact(parsedNum);
                                if (line.startsWith(DropBoxManager.EXTRA_TIME)) {
                                    timeTotal = scaled;
                                } else if (line.startsWith("source_build")) {
                                    sourceVersion = scaled;
                                } else if (line.startsWith("bytes_written")) {
                                    bytesWrittenInMiB = bytesWrittenInMiB == -1 ? scaled : bytesWrittenInMiB + scaled;
                                } else if (line.startsWith("bytes_stashed")) {
                                    bytesStashedInMiB = bytesStashedInMiB == -1 ? scaled : bytesStashedInMiB + scaled;
                                }
                            } catch (ArithmeticException e) {
                                Log.e(TAG, "Number overflows in " + line);
                            }
                        } catch (NumberFormatException e2) {
                            Log.e(TAG, "Failed to parse numbers in " + line);
                        }
                    }
                } catch (Throwable th2) {
                    th = th2;
                    bufferedReader = in;
                    if (bufferedReader != null) {
                    }
                    if (th != null) {
                    }
                }
            }
            if (timeTotal != -1) {
                MetricsLogger.histogram(context, "ota_time_total", timeTotal);
            }
            if (sourceVersion != -1) {
                MetricsLogger.histogram(context, "ota_source_version", sourceVersion);
            }
            if (bytesWrittenInMiB != -1) {
                MetricsLogger.histogram(context, "ota_written_in_MiBs", bytesWrittenInMiB);
            }
            if (bytesStashedInMiB != -1) {
                MetricsLogger.histogram(context, "ota_stashed_in_MiBs", bytesStashedInMiB);
            }
            if (in != null) {
                try {
                    try {
                        in.close();
                    } catch (Throwable th3) {
                        th = th3;
                    }
                } catch (IOException e3) {
                    e = e3;
                    Log.e(TAG, "Failed to read lines in last_install", e);
                    return;
                }
            }
            if (th != null) {
                throw th;
            }
        } catch (Throwable th4) {
            th = th4;
        }
    }

    public static String handleAftermath(Context context) throws Throwable {
        String log = null;
        try {
            log = FileUtils.readTextFile(LOG_FILE, Color.RED, "...\n");
        } catch (FileNotFoundException e) {
            Log.i(TAG, "No recovery log file");
        } catch (IOException e2) {
            Log.e(TAG, "Error reading recovery log", e2);
        }
        if (log != null) {
            parseLastInstallLog(context);
        }
        boolean reservePackage = BLOCK_MAP_FILE.exists();
        if (!reservePackage && UNCRYPT_PACKAGE_FILE.exists()) {
            String filename = null;
            try {
                filename = FileUtils.readTextFile(UNCRYPT_PACKAGE_FILE, 0, null);
            } catch (IOException e3) {
                Log.e(TAG, "Error reading uncrypt file", e3);
            }
            if (filename != null && filename.startsWith("/data")) {
                if (UNCRYPT_PACKAGE_FILE.delete()) {
                    Log.i(TAG, "Deleted: " + filename);
                } else {
                    Log.e(TAG, "Can't delete: " + filename);
                }
            }
        }
        String[] names = RECOVERY_DIR.list();
        for (int i = 0; names != null && i < names.length; i++) {
            if (!names[i].startsWith(LAST_PREFIX) && ((!reservePackage || !names[i].equals(BLOCK_MAP_FILE.getName())) && (!reservePackage || !names[i].equals(UNCRYPT_PACKAGE_FILE.getName())))) {
                recursiveDelete(new File(RECOVERY_DIR, names[i]));
            }
        }
        return log;
    }

    private static void recursiveDelete(File name) {
        if (name.isDirectory()) {
            String[] files = name.list();
            for (int i = 0; files != null && i < files.length; i++) {
                File f = new File(name, files[i]);
                recursiveDelete(f);
            }
        }
        if (!name.delete()) {
            Log.e(TAG, "Can't delete: " + name);
        } else {
            Log.i(TAG, "Deleted: " + name);
        }
    }

    private boolean uncrypt(String packageFile, IRecoverySystemProgressListener listener) {
        try {
            return this.mService.uncrypt(packageFile, listener);
        } catch (RemoteException e) {
            return false;
        }
    }

    private boolean setupBcb(String command) {
        try {
            return this.mService.setupBcb(command);
        } catch (RemoteException e) {
            return false;
        }
    }

    private boolean clearBcb() {
        try {
            return this.mService.clearBcb();
        } catch (RemoteException e) {
            return false;
        }
    }

    private static String sanitizeArg(String arg) {
        return arg.replace((char) 0, '?').replace('\n', '?');
    }

    public RecoverySystem() {
        this.mService = null;
    }

    public RecoverySystem(IRecoverySystem service) {
        this.mService = service;
    }
}
