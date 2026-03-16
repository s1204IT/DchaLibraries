package android.os;

import android.Manifest;
import android.app.backup.FullBackup;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.apache.harmony.security.asn1.BerInputStream;
import org.apache.harmony.security.pkcs7.ContentInfo;
import org.apache.harmony.security.pkcs7.SignedData;
import org.apache.harmony.security.pkcs7.SignerInfo;
import org.apache.harmony.security.x509.Certificate;

public class RecoverySystem {
    private static final long PUBLISH_PROGRESS_INTERVAL_MS = 500;
    private static final String TAG = "RecoverySystem";
    private static final File DEFAULT_KEYSTORE = new File("/system/etc/security/otacerts.zip");
    private static File RECOVERY_DIR = new File("/cache/recovery");
    private static File COMMAND_FILE = new File(RECOVERY_DIR, "command");
    private static File LOG_FILE = new File(RECOVERY_DIR, "log");
    private static String LAST_PREFIX = "last_";
    private static int LOG_FILE_MAX_LENGTH = 65536;

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

    public static void verifyPackage(File packageFile, ProgressListener listener, File deviceCertsZipFile) throws GeneralSecurityException, IOException {
        String alg;
        long fileLen = packageFile.length();
        RandomAccessFile raf = new RandomAccessFile(packageFile, FullBackup.ROOT_TREE_TOKEN);
        int lastPercent = 0;
        try {
            long lastPublishTime = System.currentTimeMillis();
            if (listener != null) {
                listener.onProgress(0);
            }
            raf.seek(fileLen - 6);
            byte[] footer = new byte[6];
            raf.readFully(footer);
            if (footer[2] != -1 || footer[3] != -1) {
                throw new SignatureException("no signature in file (no footer)");
            }
            int commentSize = (footer[4] & 255) | ((footer[5] & 255) << 8);
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
            BerInputStream bis = new BerInputStream(new ByteArrayInputStream(eocd, (commentSize + 22) - signatureStart, signatureStart));
            ContentInfo info = (ContentInfo) ContentInfo.ASN1.decode(bis);
            SignedData signedData = info.getSignedData();
            if (signedData == null) {
                throw new IOException("signedData is null");
            }
            List<Certificate> encCerts = signedData.getCertificates();
            if (encCerts.isEmpty()) {
                throw new IOException("encCerts is empty");
            }
            Iterator<Certificate> it = encCerts.iterator();
            if (it.hasNext()) {
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                InputStream is = new ByteArrayInputStream(it.next().getEncoded());
                X509Certificate cert = (X509Certificate) cf.generateCertificate(is);
                List<SignerInfo> sigInfos = signedData.getSignerInfos();
                if (!sigInfos.isEmpty()) {
                    SignerInfo sigInfo = sigInfos.get(0);
                    if (deviceCertsZipFile == null) {
                        deviceCertsZipFile = DEFAULT_KEYSTORE;
                    }
                    HashSet<X509Certificate> trusted = getTrustedCerts(deviceCertsZipFile);
                    PublicKey signatureKey = cert.getPublicKey();
                    boolean verified = false;
                    Iterator<X509Certificate> it2 = trusted.iterator();
                    while (true) {
                        if (!it2.hasNext()) {
                            break;
                        }
                        X509Certificate c = it2.next();
                        if (c.getPublicKey().equals(signatureKey)) {
                            verified = true;
                            break;
                        }
                    }
                    if (!verified) {
                        throw new SignatureException("signature doesn't match any trusted key");
                    }
                    String da = sigInfo.getDigestAlgorithm();
                    String dea = sigInfo.getDigestEncryptionAlgorithm();
                    if (da == null || dea == null) {
                        alg = cert.getSigAlgName();
                    } else {
                        alg = da + "with" + dea;
                    }
                    Signature sig = Signature.getInstance(alg);
                    sig.initVerify(cert);
                    long toRead = (fileLen - ((long) commentSize)) - 2;
                    long soFar = 0;
                    raf.seek(0L);
                    byte[] buffer = new byte[4096];
                    boolean interrupted = false;
                    while (soFar < toRead && !(interrupted = Thread.interrupted())) {
                        int size = buffer.length;
                        if (((long) size) + soFar > toRead) {
                            size = (int) (toRead - soFar);
                        }
                        int read = raf.read(buffer, 0, size);
                        sig.update(buffer, 0, read);
                        soFar += (long) read;
                        if (listener != null) {
                            long now = System.currentTimeMillis();
                            int p = (int) ((100 * soFar) / toRead);
                            if (p > lastPercent && now - lastPublishTime > PUBLISH_PROGRESS_INTERVAL_MS) {
                                lastPercent = p;
                                lastPublishTime = now;
                                listener.onProgress(lastPercent);
                            }
                        }
                    }
                    if (listener != null) {
                        listener.onProgress(100);
                    }
                    if (interrupted) {
                        throw new SignatureException("verification was interrupted");
                    }
                    if (!sig.verify(sigInfo.getEncryptedDigest())) {
                        throw new SignatureException("signature digest verification failed");
                    }
                    return;
                }
                throw new IOException("no signer infos!");
            }
            throw new SignatureException("signature contains no certificates");
        } finally {
            raf.close();
        }
    }

    public static void installPackage(Context context, File packageFile) throws IOException {
        String filename = packageFile.getCanonicalPath();
        Log.w(TAG, "!!! REBOOTING TO INSTALL " + filename + " !!!");
        String filenameArg = "--update_package=" + filename;
        String localeArg = "--locale=" + Locale.getDefault().toString();
        bootCommand(context, filenameArg, localeArg);
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
        UserManager um = (UserManager) context.getSystemService("user");
        if (um.hasUserRestriction(UserManager.DISALLOW_FACTORY_RESET)) {
            throw new SecurityException("Wiping data is not allowed for this user.");
        }
        final ConditionVariable condition = new ConditionVariable();
        Intent intent = new Intent("android.intent.action.MASTER_CLEAR_NOTIFICATION");
        intent.addFlags(268435456);
        context.sendOrderedBroadcastAsUser(intent, UserHandle.OWNER, Manifest.permission.MASTER_CLEAR, new BroadcastReceiver() {
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

    public static void rebootTerminateTarminal(Context context) throws IOException {
        final ConditionVariable condition = new ConditionVariable();
        Intent intent = new Intent("android.intent.action.MASTER_CLEAR_NOTIFICATION");
        context.sendOrderedBroadcastAsUser(intent, UserHandle.OWNER, Manifest.permission.MASTER_CLEAR, new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent2) {
                condition.open();
            }
        }, null, 0, null, null);
        condition.block();
        bootCommand(context, "--terminate=sY4r50Og");
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
        RECOVERY_DIR.mkdirs();
        COMMAND_FILE.delete();
        LOG_FILE.delete();
        FileWriter command = new FileWriter(COMMAND_FILE);
        try {
            for (String arg : args) {
                if (!TextUtils.isEmpty(arg)) {
                    command.write(arg);
                    command.write("\n");
                }
            }
            command.close();
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            pm.reboot(PowerManager.REBOOT_RECOVERY);
            throw new IOException("Reboot failed (no permissions?)");
        } catch (Throwable th) {
            command.close();
            throw th;
        }
    }

    public static String handleAftermath() {
        String log = null;
        try {
            log = FileUtils.readTextFile(LOG_FILE, -LOG_FILE_MAX_LENGTH, "...\n");
        } catch (FileNotFoundException e) {
            Log.i(TAG, "No recovery log file");
        } catch (IOException e2) {
            Log.e(TAG, "Error reading recovery log", e2);
        }
        String[] names = RECOVERY_DIR.list();
        for (int i = 0; names != null && i < names.length; i++) {
            if (!names[i].startsWith(LAST_PREFIX)) {
                File f = new File(RECOVERY_DIR, names[i]);
                if (!f.delete()) {
                    Log.e(TAG, "Can't delete: " + f);
                } else {
                    Log.i(TAG, "Deleted: " + f);
                }
            }
        }
        return log;
    }

    private static String sanitizeArg(String arg) {
        return arg.replace((char) 0, '?').replace('\n', '?');
    }

    private void RecoverySystem() {
    }
}
