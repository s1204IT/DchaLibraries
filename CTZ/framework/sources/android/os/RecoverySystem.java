package android.os;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.SystemApi;
import android.app.PendingIntent;
import android.app.backup.FullBackup;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.IRecoverySystemProgressListener;
import android.provider.Settings;
import android.telephony.euicc.EuiccManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.WindowManager;
import com.android.internal.logging.MetricsLogger;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import libcore.io.Streams;
import sun.security.pkcs.PKCS7;
import sun.security.pkcs.SignerInfo;

public class RecoverySystem {
    private static final String ACTION_EUICC_FACTORY_RESET = "com.android.internal.action.EUICC_FACTORY_RESET";
    private static final long DEFAULT_EUICC_FACTORY_RESET_TIMEOUT_MILLIS = 30000;
    private static final String LAST_PREFIX = "last_";
    private static final int LOG_FILE_MAX_LENGTH = 65536;
    private static final long MAX_EUICC_FACTORY_RESET_TIMEOUT_MILLIS = 60000;
    private static final long MIN_EUICC_FACTORY_RESET_TIMEOUT_MILLIS = 5000;
    private static final String PACKAGE_NAME_WIPING_EUICC_DATA_CALLBACK = "android";
    private static final long PUBLISH_PROGRESS_INTERVAL_MS = 500;
    private static final String TAG = "RecoverySystem";
    private final IRecoverySystem mService;
    private static final File DEFAULT_KEYSTORE = new File("/system/etc/security/otacerts.zip");
    private static final File RECOVERY_DIR = new File("/cache/recovery");
    private static final File LOG_FILE = new File(RECOVERY_DIR, "log");
    private static final File LAST_INSTALL_FILE = new File(RECOVERY_DIR, "last_install");
    public static final File BLOCK_MAP_FILE = new File(RECOVERY_DIR, "block.map");
    public static final File UNCRYPT_PACKAGE_FILE = new File(RECOVERY_DIR, "uncrypt_file");
    public static final File UNCRYPT_STATUS_FILE = new File(RECOVERY_DIR, "uncrypt_status");
    private static final Object sRequestLock = new Object();

    public interface ProgressListener {
        void onProgress(int i);
    }

    private static HashSet<X509Certificate> getTrustedCerts(File file) throws GeneralSecurityException, IOException {
        HashSet<X509Certificate> hashSet = new HashSet<>();
        if (file == null) {
            file = DEFAULT_KEYSTORE;
        }
        ZipFile zipFile = new ZipFile(file);
        try {
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            Enumeration<? extends ZipEntry> enumerationEntries = zipFile.entries();
            while (enumerationEntries.hasMoreElements()) {
                InputStream inputStream = zipFile.getInputStream(enumerationEntries.nextElement());
                try {
                    hashSet.add((X509Certificate) certificateFactory.generateCertificate(inputStream));
                    inputStream.close();
                } finally {
                }
            }
            return hashSet;
        } finally {
            zipFile.close();
        }
    }

    public static void verifyPackage(File file, final ProgressListener progressListener, File file2) throws GeneralSecurityException, IOException {
        final long length = file.length();
        final RandomAccessFile randomAccessFile = new RandomAccessFile(file, FullBackup.ROOT_TREE_TOKEN);
        try {
            final long jCurrentTimeMillis = System.currentTimeMillis();
            if (progressListener != null) {
                progressListener.onProgress(0);
            }
            randomAccessFile.seek(length - 6);
            byte[] bArr = new byte[6];
            randomAccessFile.readFully(bArr);
            if (bArr[2] != -1 || bArr[3] != -1) {
                throw new SignatureException("no signature in file (no footer)");
            }
            final int i = (bArr[4] & 255) | ((bArr[5] & 255) << 8);
            boolean z = true;
            int i2 = ((bArr[1] & 255) << 8) | (bArr[0] & 255);
            int i3 = i + 22;
            byte[] bArr2 = new byte[i3];
            randomAccessFile.seek(length - ((long) i3));
            randomAccessFile.readFully(bArr2);
            byte b = 80;
            if (bArr2[0] != 80 || bArr2[1] != 75 || bArr2[2] != 5 || bArr2[3] != 6) {
                throw new SignatureException("no signature in file (bad footer)");
            }
            int i4 = 4;
            while (i4 < bArr2.length - 3) {
                if (bArr2[i4] == b && bArr2[i4 + 1] == 75 && bArr2[i4 + 2] == 5) {
                    if (bArr2[i4 + 3] == 6) {
                        throw new SignatureException("EOCD marker found after start of EOCD");
                    }
                }
                i4++;
                b = 80;
            }
            PKCS7 pkcs7 = new PKCS7(new ByteArrayInputStream(bArr2, i3 - i2, i2));
            X509Certificate[] certificates = pkcs7.getCertificates();
            if (certificates == null || certificates.length == 0) {
                throw new SignatureException("signature contains no certificates");
            }
            PublicKey publicKey = certificates[0].getPublicKey();
            SignerInfo[] signerInfos = pkcs7.getSignerInfos();
            if (signerInfos == null || signerInfos.length == 0) {
                throw new SignatureException("signature contains no signedData");
            }
            SignerInfo signerInfo = signerInfos[0];
            Iterator<X509Certificate> it = getTrustedCerts(file2 == null ? DEFAULT_KEYSTORE : file2).iterator();
            while (true) {
                if (it.hasNext()) {
                    if (it.next().getPublicKey().equals(publicKey)) {
                        break;
                    }
                } else {
                    z = false;
                    break;
                }
            }
            if (!z) {
                throw new SignatureException("signature doesn't match any trusted key");
            }
            randomAccessFile.seek(0L);
            SignerInfo signerInfoVerify = pkcs7.verify(signerInfo, new InputStream() {
                long lastPublishTime;
                long toRead;
                long soFar = 0;
                int lastPercent = 0;

                {
                    this.toRead = (length - ((long) i)) - 2;
                    this.lastPublishTime = jCurrentTimeMillis;
                }

                @Override
                public int read() throws IOException {
                    throw new UnsupportedOperationException();
                }

                @Override
                public int read(byte[] bArr3, int i5, int i6) throws IOException {
                    if (this.soFar >= this.toRead || Thread.currentThread().isInterrupted()) {
                        return -1;
                    }
                    if (this.soFar + ((long) i6) > this.toRead) {
                        i6 = (int) (this.toRead - this.soFar);
                    }
                    int i7 = randomAccessFile.read(bArr3, i5, i6);
                    this.soFar += (long) i7;
                    if (progressListener != null) {
                        long jCurrentTimeMillis2 = System.currentTimeMillis();
                        int i8 = (int) ((this.soFar * 100) / this.toRead);
                        if (i8 > this.lastPercent && jCurrentTimeMillis2 - this.lastPublishTime > RecoverySystem.PUBLISH_PROGRESS_INTERVAL_MS) {
                            this.lastPercent = i8;
                            this.lastPublishTime = jCurrentTimeMillis2;
                            progressListener.onProgress(this.lastPercent);
                        }
                    }
                    return i7;
                }
            });
            boolean zInterrupted = Thread.interrupted();
            if (progressListener != null) {
                progressListener.onProgress(100);
            }
            if (zInterrupted) {
                throw new SignatureException("verification was interrupted");
            }
            if (signerInfoVerify == null) {
                throw new SignatureException("signature digest verification failed");
            }
            randomAccessFile.close();
            if (!readAndVerifyPackageCompatibilityEntry(file)) {
                throw new SignatureException("package compatibility verification failed");
            }
        } catch (Throwable th) {
            randomAccessFile.close();
            throw th;
        }
    }

    private static boolean verifyPackageCompatibility(InputStream inputStream) throws IOException {
        long size;
        ArrayList arrayList = new ArrayList();
        ZipInputStream zipInputStream = new ZipInputStream(inputStream);
        while (true) {
            ZipEntry nextEntry = zipInputStream.getNextEntry();
            if (nextEntry != null) {
                size = nextEntry.getSize();
                if (size > 2147483647L || size < 0) {
                    break;
                }
                byte[] bArr = new byte[(int) size];
                Streams.readFully(zipInputStream, bArr);
                arrayList.add(new String(bArr, StandardCharsets.UTF_8));
            } else {
                if (arrayList.isEmpty()) {
                    throw new IOException("no entries found in the compatibility file");
                }
                return VintfObject.verify((String[]) arrayList.toArray(new String[arrayList.size()])) == 0;
            }
        }
        throw new IOException("invalid entry size (" + size + ") in the compatibility file");
    }

    private static boolean readAndVerifyPackageCompatibilityEntry(File file) throws Exception {
        ZipFile zipFile = new ZipFile(file);
        Throwable th = null;
        try {
            ZipEntry entry = zipFile.getEntry("compatibility.zip");
            if (entry == null) {
                return true;
            }
            return verifyPackageCompatibility(zipFile.getInputStream(entry));
        } finally {
            $closeResource(th, zipFile);
        }
        $closeResource(th, zipFile);
    }

    private static void $closeResource(Throwable th, AutoCloseable autoCloseable) throws Exception {
        if (th == null) {
            autoCloseable.close();
            return;
        }
        try {
            autoCloseable.close();
        } catch (Throwable th2) {
            th.addSuppressed(th2);
        }
    }

    @SystemApi
    @SuppressLint({"Doclava125"})
    public static boolean verifyPackageCompatibility(File file) throws Exception {
        FileInputStream fileInputStream = new FileInputStream(file);
        Throwable th = null;
        try {
            return verifyPackageCompatibility(fileInputStream);
        } finally {
            $closeResource(th, fileInputStream);
        }
    }

    @SystemApi
    public static void processPackage(Context context, File file, ProgressListener progressListener, Handler handler) throws IOException {
        String canonicalPath = file.getCanonicalPath();
        if (!canonicalPath.startsWith("/data/")) {
            return;
        }
        RecoverySystem recoverySystem = (RecoverySystem) context.getSystemService("recovery");
        AnonymousClass2 anonymousClass2 = null;
        if (progressListener != null) {
            if (handler == null) {
                handler = new Handler(context.getMainLooper());
            }
            anonymousClass2 = new AnonymousClass2(handler, progressListener);
        }
        if (!recoverySystem.uncrypt(canonicalPath, anonymousClass2)) {
            throw new IOException("process package failed");
        }
    }

    class AnonymousClass2 extends IRecoverySystemProgressListener.Stub {
        int lastProgress = 0;
        long lastPublishTime = System.currentTimeMillis();
        final ProgressListener val$listener;
        final Handler val$progressHandler;

        AnonymousClass2(Handler handler, ProgressListener progressListener) {
            this.val$progressHandler = handler;
            this.val$listener = progressListener;
        }

        @Override
        public void onProgress(final int i) {
            final long jCurrentTimeMillis = System.currentTimeMillis();
            this.val$progressHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (i > AnonymousClass2.this.lastProgress && jCurrentTimeMillis - AnonymousClass2.this.lastPublishTime > RecoverySystem.PUBLISH_PROGRESS_INTERVAL_MS) {
                        AnonymousClass2.this.lastProgress = i;
                        AnonymousClass2.this.lastPublishTime = jCurrentTimeMillis;
                        AnonymousClass2.this.val$listener.onProgress(i);
                    }
                }
            });
        }
    }

    @SystemApi
    public static void processPackage(Context context, File file, ProgressListener progressListener) throws IOException {
        processPackage(context, file, progressListener, null);
    }

    public static void installPackage(Context context, File file) throws IOException {
        installPackage(context, file, false);
    }

    @SystemApi
    public static void installPackage(Context context, File file, boolean z) throws IOException {
        synchronized (sRequestLock) {
            LOG_FILE.delete();
            UNCRYPT_PACKAGE_FILE.delete();
            String canonicalPath = file.getCanonicalPath();
            Log.w(TAG, "!!! REBOOTING TO INSTALL " + canonicalPath + " !!!");
            boolean zEndsWith = canonicalPath.endsWith("_s.zip");
            if (canonicalPath.startsWith("/data/")) {
                if (z) {
                    if (!BLOCK_MAP_FILE.exists()) {
                        Log.e(TAG, "Package claimed to have been processed but failed to find the block map file.");
                        throw new IOException("Failed to find block map file");
                    }
                } else {
                    FileWriter fileWriter = new FileWriter(UNCRYPT_PACKAGE_FILE);
                    try {
                        fileWriter.write(canonicalPath + "\n");
                        fileWriter.close();
                        if (!UNCRYPT_PACKAGE_FILE.setReadable(true, false) || !UNCRYPT_PACKAGE_FILE.setWritable(true, false)) {
                            Log.e(TAG, "Error setting permission for " + UNCRYPT_PACKAGE_FILE);
                        }
                        BLOCK_MAP_FILE.delete();
                    } catch (Throwable th) {
                        fileWriter.close();
                        throw th;
                    }
                }
                canonicalPath = "@/cache/recovery/block.map";
            }
            String str = ("--update_package=" + canonicalPath + "\n") + ("--locale=" + Locale.getDefault().toLanguageTag() + "\n");
            if (zEndsWith) {
                str = str + "--security\n";
            }
            if (!((RecoverySystem) context.getSystemService("recovery")).setupBcb(str)) {
                throw new IOException("Setup BCB failed");
            }
            PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            String str2 = PowerManager.REBOOT_RECOVERY_UPDATE;
            if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_LEANBACK) && ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getState() != 2) {
                str2 = PowerManager.REBOOT_RECOVERY_UPDATE + ",quiescent";
            }
            powerManager.reboot(str2);
            throw new IOException("Reboot failed (no permissions?)");
        }
    }

    @SystemApi
    public static void scheduleUpdateOnBoot(Context context, File file) throws IOException {
        String canonicalPath = file.getCanonicalPath();
        boolean zEndsWith = canonicalPath.endsWith("_s.zip");
        if (canonicalPath.startsWith("/data/")) {
            canonicalPath = "@/cache/recovery/block.map";
        }
        String str = ("--update_package=" + canonicalPath + "\n") + ("--locale=" + Locale.getDefault().toLanguageTag() + "\n");
        if (zEndsWith) {
            str = str + "--security\n";
        }
        if (!((RecoverySystem) context.getSystemService("recovery")).setupBcb(str)) {
            throw new IOException("schedule update on boot failed");
        }
    }

    @SystemApi
    public static void cancelScheduledUpdate(Context context) throws IOException {
        if (!((RecoverySystem) context.getSystemService("recovery")).clearBcb()) {
            throw new IOException("cancel scheduled update failed");
        }
    }

    public static void rebootWipeUserData(Context context) throws IOException {
        rebootWipeUserData(context, false, context.getPackageName(), false, false);
    }

    public static void rebootWipeUserData(Context context, String str) throws IOException {
        rebootWipeUserData(context, false, str, false, false);
    }

    public static void rebootWipeUserData(Context context, boolean z) throws IOException {
        rebootWipeUserData(context, z, context.getPackageName(), false, false);
    }

    public static void rebootWipeUserData(Context context, boolean z, String str, boolean z2) throws IOException {
        rebootWipeUserData(context, z, str, z2, false);
    }

    public static void rebootWipeUserData(Context context, boolean z, String str, boolean z2, boolean z3) throws IOException {
        String str2;
        UserManager userManager = (UserManager) context.getSystemService("user");
        if (!z2 && userManager.hasUserRestriction(UserManager.DISALLOW_FACTORY_RESET)) {
            throw new SecurityException("Wiping data is not allowed for this user.");
        }
        final ConditionVariable conditionVariable = new ConditionVariable();
        Intent intent = new Intent(Intent.ACTION_MASTER_CLEAR_NOTIFICATION);
        intent.addFlags(285212672);
        context.sendOrderedBroadcastAsUser(intent, UserHandle.SYSTEM, Manifest.permission.MASTER_CLEAR, new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent2) {
                conditionVariable.open();
            }
        }, null, 0, null, null);
        conditionVariable.block();
        if (z3) {
            wipeEuiccData(context, "android");
        }
        String str3 = null;
        if (z) {
            str2 = "--shutdown_after";
        } else {
            str2 = null;
        }
        if (!TextUtils.isEmpty(str)) {
            str3 = "--reason=" + sanitizeArg(str);
        }
        bootCommand(context, str2, "--wipe_data", str3, "--locale=" + Locale.getDefault().toLanguageTag());
    }

    public static boolean wipeEuiccData(Context context, String str) {
        if (Settings.Global.getInt(context.getContentResolver(), Settings.Global.EUICC_PROVISIONED, 0) == 0) {
            Log.d(TAG, "Skipping eUICC wipe/retain as it is not provisioned");
            return true;
        }
        EuiccManager euiccManager = (EuiccManager) context.getSystemService(Context.EUICC_SERVICE);
        if (euiccManager == null || !euiccManager.isEnabled()) {
            return false;
        }
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final AtomicBoolean atomicBoolean = new AtomicBoolean(false);
        BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                if (RecoverySystem.ACTION_EUICC_FACTORY_RESET.equals(intent.getAction())) {
                    if (getResultCode() != 0) {
                        Log.e(RecoverySystem.TAG, "Error wiping euicc data, Detailed code = " + intent.getIntExtra(EuiccManager.EXTRA_EMBEDDED_SUBSCRIPTION_DETAILED_CODE, 0));
                    } else {
                        Log.d(RecoverySystem.TAG, "Successfully wiped euicc data.");
                        atomicBoolean.set(true);
                    }
                    countDownLatch.countDown();
                }
            }
        };
        Intent intent = new Intent(ACTION_EUICC_FACTORY_RESET);
        intent.setPackage(str);
        PendingIntent broadcastAsUser = PendingIntent.getBroadcastAsUser(context, 0, intent, 134217728, UserHandle.SYSTEM);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_EUICC_FACTORY_RESET);
        HandlerThread handlerThread = new HandlerThread("euiccWipeFinishReceiverThread");
        handlerThread.start();
        context.getApplicationContext().registerReceiver(broadcastReceiver, intentFilter, null, new Handler(handlerThread.getLooper()));
        euiccManager.eraseSubscriptions(broadcastAsUser);
        try {
            long j = Settings.Global.getLong(context.getContentResolver(), Settings.Global.EUICC_FACTORY_RESET_TIMEOUT_MILLIS, 30000L);
            if (j < 5000) {
                j = 5000;
            } else if (j > 60000) {
                j = 60000;
            }
            if (countDownLatch.await(j, TimeUnit.MILLISECONDS)) {
                context.getApplicationContext().unregisterReceiver(broadcastReceiver);
                return atomicBoolean.get();
            }
            Log.e(TAG, "Timeout wiping eUICC data.");
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Log.e(TAG, "Wiping eUICC data interrupted", e);
            return false;
        } finally {
            context.getApplicationContext().unregisterReceiver(broadcastReceiver);
        }
        context.getApplicationContext().unregisterReceiver(broadcastReceiver);
    }

    public static void rebootPromptAndWipeUserData(Context context, String str) throws IOException {
        String str2;
        if (!TextUtils.isEmpty(str)) {
            str2 = "--reason=" + sanitizeArg(str);
        } else {
            str2 = null;
        }
        bootCommand(context, null, "--prompt_and_wipe_data", str2, "--locale=" + Locale.getDefault().toString());
    }

    public static void rebootWipeCache(Context context) throws IOException {
        rebootWipeCache(context, context.getPackageName());
    }

    public static void rebootWipeCache(Context context, String str) throws IOException {
        String str2;
        if (!TextUtils.isEmpty(str)) {
            str2 = "--reason=" + sanitizeArg(str);
        } else {
            str2 = null;
        }
        bootCommand(context, "--wipe_cache", str2, "--locale=" + Locale.getDefault().toLanguageTag());
    }

    @SystemApi
    public static void rebootWipeAb(Context context, File file, String str) throws IOException {
        String str2;
        if (!TextUtils.isEmpty(str)) {
            str2 = "--reason=" + sanitizeArg(str);
        } else {
            str2 = null;
        }
        bootCommand(context, "--wipe_ab", "--wipe_package=" + file.getCanonicalPath(), str2, "--locale=" + Locale.getDefault().toLanguageTag());
    }

    private static void bootCommand(Context context, String... strArr) throws IOException {
        LOG_FILE.delete();
        StringBuilder sb = new StringBuilder();
        for (String str : strArr) {
            if (!TextUtils.isEmpty(str)) {
                sb.append(str);
                sb.append("\n");
            }
        }
        ((RecoverySystem) context.getSystemService("recovery")).rebootRecoveryWithCommand(sb.toString());
        throw new IOException("Reboot failed (no permissions?)");
    }

    private static void parseLastInstallLog(Context context) throws Exception {
        Throwable th;
        int i;
        int i2;
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(LAST_INSTALL_FILE));
            int i3 = -1;
            int i4 = -1;
            int i5 = -1;
            int i6 = -1;
            int i7 = -1;
            int i8 = -1;
            int i9 = -1;
            int i10 = -1;
            int i11 = -1;
            int i12 = -1;
            int i13 = -1;
            while (true) {
                try {
                    String line = bufferedReader.readLine();
                    if (line == null) {
                        break;
                    }
                    int iIndexOf = line.indexOf(58);
                    if (iIndexOf == i3 || (i2 = iIndexOf + 1) >= line.length()) {
                        i = i4;
                    } else {
                        i = i4;
                        try {
                            long j = Long.parseLong(line.substring(i2).trim());
                            try {
                                int intExact = line.startsWith("bytes") ? Math.toIntExact(j / 1048576) : Math.toIntExact(j);
                                if (line.startsWith(DropBoxManager.EXTRA_TIME)) {
                                    i4 = intExact;
                                } else {
                                    if (line.startsWith("uncrypt_time")) {
                                        i5 = intExact;
                                    } else if (line.startsWith("source_build")) {
                                        i6 = intExact;
                                    } else if (line.startsWith("bytes_written")) {
                                        if (i7 != -1) {
                                            intExact += i7;
                                        }
                                        i7 = intExact;
                                    } else if (line.startsWith("bytes_stashed")) {
                                        if (i8 != -1) {
                                            intExact += i8;
                                        }
                                        i8 = intExact;
                                    } else if (line.startsWith("temperature_start")) {
                                        i9 = intExact;
                                    } else if (line.startsWith("temperature_end")) {
                                        i10 = intExact;
                                    } else if (line.startsWith("temperature_max")) {
                                        i11 = intExact;
                                    } else if (line.startsWith("error")) {
                                        i12 = intExact;
                                    } else if (line.startsWith("cause")) {
                                        i13 = intExact;
                                    }
                                    i4 = i;
                                }
                            } catch (ArithmeticException e) {
                                Log.e(TAG, "Number overflows in " + line);
                                i4 = i;
                            }
                        } catch (NumberFormatException e2) {
                            Log.e(TAG, "Failed to parse numbers in " + line);
                        }
                        i3 = -1;
                    }
                    i4 = i;
                    i3 = -1;
                } catch (Throwable th2) {
                    th = th2;
                    th = null;
                    $closeResource(th, bufferedReader);
                    throw th;
                }
            }
            int i14 = i4;
            if (i14 != -1) {
                MetricsLogger.histogram(context, "ota_time_total", i14);
            }
            if (i5 != -1) {
                MetricsLogger.histogram(context, "ota_uncrypt_time", i5);
            }
            if (i6 != -1) {
                MetricsLogger.histogram(context, "ota_source_version", i6);
            }
            if (i7 != -1) {
                MetricsLogger.histogram(context, "ota_written_in_MiBs", i7);
            }
            if (i8 != -1) {
                MetricsLogger.histogram(context, "ota_stashed_in_MiBs", i8);
            }
            if (i9 != -1) {
                MetricsLogger.histogram(context, "ota_temperature_start", i9);
            }
            if (i10 != -1) {
                MetricsLogger.histogram(context, "ota_temperature_end", i10);
            }
            if (i11 != -1) {
                MetricsLogger.histogram(context, "ota_temperature_max", i11);
            }
            if (i12 != -1) {
                MetricsLogger.histogram(context, "ota_non_ab_error_code", i12);
            }
            if (i13 != -1) {
                MetricsLogger.histogram(context, "ota_non_ab_cause_code", i13);
            }
            $closeResource(null, bufferedReader);
        } catch (IOException e3) {
            Log.e(TAG, "Failed to read lines in last_install", e3);
        }
    }

    public static String handleAftermath(Context context) throws Exception {
        String textFile;
        String textFile2 = null;
        try {
            textFile = FileUtils.readTextFile(LOG_FILE, -65536, "...\n");
        } catch (FileNotFoundException e) {
            Log.i(TAG, "No recovery log file");
            textFile = null;
        } catch (IOException e2) {
            Log.e(TAG, "Error reading recovery log", e2);
            textFile = null;
        }
        if (textFile != null) {
            parseLastInstallLog(context);
        }
        boolean zExists = BLOCK_MAP_FILE.exists();
        if (!zExists && UNCRYPT_PACKAGE_FILE.exists()) {
            try {
                textFile2 = FileUtils.readTextFile(UNCRYPT_PACKAGE_FILE, 0, null);
            } catch (IOException e3) {
                Log.e(TAG, "Error reading uncrypt file", e3);
            }
            if (textFile2 != null && textFile2.startsWith("/data")) {
                if (UNCRYPT_PACKAGE_FILE.delete()) {
                    Log.i(TAG, "Deleted: " + textFile2);
                } else {
                    Log.e(TAG, "Can't delete: " + textFile2);
                }
            }
        }
        String[] list = RECOVERY_DIR.list();
        for (int i = 0; list != null && i < list.length; i++) {
            if (!list[i].startsWith(LAST_PREFIX) && ((!zExists || !list[i].equals(BLOCK_MAP_FILE.getName())) && (!zExists || !list[i].equals(UNCRYPT_PACKAGE_FILE.getName())))) {
                recursiveDelete(new File(RECOVERY_DIR, list[i]));
            }
        }
        return textFile;
    }

    private static void recursiveDelete(File file) {
        if (file.isDirectory()) {
            String[] list = file.list();
            for (int i = 0; list != null && i < list.length; i++) {
                recursiveDelete(new File(file, list[i]));
            }
        }
        if (!file.delete()) {
            Log.e(TAG, "Can't delete: " + file);
            return;
        }
        Log.i(TAG, "Deleted: " + file);
    }

    private boolean uncrypt(String str, IRecoverySystemProgressListener iRecoverySystemProgressListener) {
        try {
            return this.mService.uncrypt(str, iRecoverySystemProgressListener);
        } catch (RemoteException e) {
            return false;
        }
    }

    private boolean setupBcb(String str) {
        try {
            return this.mService.setupBcb(str);
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

    private void rebootRecoveryWithCommand(String str) {
        try {
            this.mService.rebootRecoveryWithCommand(str);
        } catch (RemoteException e) {
        }
    }

    private static String sanitizeArg(String str) {
        return str.replace((char) 0, '?').replace('\n', '?');
    }

    public RecoverySystem() {
        this.mService = null;
    }

    public RecoverySystem(IRecoverySystem iRecoverySystem) {
        this.mService = iRecoverySystem;
    }
}
