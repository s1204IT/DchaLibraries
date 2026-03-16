package com.android.server.pm;

import android.content.pm.PackageParser;
import android.content.pm.Signature;
import android.os.Environment;
import android.util.Slog;
import android.util.Xml;
import com.android.internal.util.XmlUtils;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public final class SELinuxMMAC {
    private static final String BASE_SEAPP_CONTEXTS = "/seapp_contexts";
    private static final String BASE_VERSION_FILE = "/selinux_version";
    private static final String DATA_SEAPP_CONTEXTS;
    private static final boolean DEBUG_POLICY = false;
    private static final boolean DEBUG_POLICY_INSTALL = false;
    private static final String MAC_PERMISSIONS;
    private static final String SEAPP_CONTEXTS;
    private static final String SEAPP_HASH_FILE;
    private static final String TAG = "SELinuxMMAC";
    private static HashMap<Signature, Policy> sSigSeinfo = new HashMap<>();
    private static String sDefaultSeinfo = null;
    private static final String DATA_VERSION_FILE = Environment.getDataDirectory() + "/security/current/selinux_version";
    private static final boolean USE_OVERRIDE_POLICY = useOverridePolicy();
    private static final String DATA_MAC_PERMISSIONS = Environment.getDataDirectory() + "/security/current/mac_permissions.xml";
    private static final String BASE_MAC_PERMISSIONS = Environment.getRootDirectory() + "/etc/security/mac_permissions.xml";

    static {
        MAC_PERMISSIONS = USE_OVERRIDE_POLICY ? DATA_MAC_PERMISSIONS : BASE_MAC_PERMISSIONS;
        DATA_SEAPP_CONTEXTS = Environment.getDataDirectory() + "/security/current/seapp_contexts";
        SEAPP_CONTEXTS = USE_OVERRIDE_POLICY ? DATA_SEAPP_CONTEXTS : BASE_SEAPP_CONTEXTS;
        SEAPP_HASH_FILE = Environment.getDataDirectory().toString() + "/system/seapp_hash";
    }

    static class Policy {
        private String seinfo = null;
        private final HashMap<String, String> pkgMap = new HashMap<>();

        Policy() {
        }

        void putSeinfo(String seinfoValue) {
            this.seinfo = seinfoValue;
        }

        void putPkg(String pkg, String seinfoValue) {
            this.pkgMap.put(pkg, seinfoValue);
        }

        boolean isValid() {
            return (this.seinfo == null && this.pkgMap.isEmpty()) ? false : true;
        }

        String checkPolicy(String pkgName) {
            String seinfoValue = this.pkgMap.get(pkgName);
            return seinfoValue != null ? seinfoValue : this.seinfo;
        }
    }

    private static void flushInstallPolicy() {
        sSigSeinfo.clear();
        sDefaultSeinfo = null;
    }

    public static boolean readInstallPolicy() throws Throwable {
        boolean z;
        FileReader policyFile;
        HashMap<Signature, Policy> sigSeinfo = new HashMap<>();
        String defaultSeinfo = null;
        FileReader policyFile2 = null;
        try {
            try {
                policyFile = new FileReader(MAC_PERMISSIONS);
            } catch (Throwable th) {
                th = th;
            }
        } catch (IOException e) {
            ioe = e;
        } catch (XmlPullParserException e2) {
            xpe = e2;
        }
        try {
            Slog.d(TAG, "Using policy file " + MAC_PERMISSIONS);
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(policyFile);
            XmlUtils.beginDocument(parser, "policy");
            while (true) {
                XmlUtils.nextElement(parser);
                if (parser.getEventType() == 1) {
                    break;
                }
                String tagName = parser.getName();
                if ("signer".equals(tagName)) {
                    String cert = parser.getAttributeValue(null, "signature");
                    if (cert == null) {
                        Slog.w(TAG, "<signer> without signature at " + parser.getPositionDescription());
                        XmlUtils.skipCurrentTag(parser);
                    } else {
                        try {
                            Signature signature = new Signature(cert);
                            Policy policy = readPolicyTags(parser);
                            if (policy.isValid()) {
                                sigSeinfo.put(signature, policy);
                            }
                        } catch (IllegalArgumentException e3) {
                            Slog.w(TAG, "<signer> with bad signature at " + parser.getPositionDescription(), e3);
                            XmlUtils.skipCurrentTag(parser);
                        }
                    }
                } else if ("default".equals(tagName)) {
                    defaultSeinfo = readSeinfoTag(parser);
                } else {
                    XmlUtils.skipCurrentTag(parser);
                }
            }
            IoUtils.closeQuietly(policyFile);
            flushInstallPolicy();
            sSigSeinfo = sigSeinfo;
            sDefaultSeinfo = defaultSeinfo;
            z = true;
            policyFile2 = policyFile;
        } catch (IOException e4) {
            ioe = e4;
            policyFile2 = policyFile;
            Slog.w(TAG, "Got exception parsing " + MAC_PERMISSIONS, ioe);
            z = false;
            IoUtils.closeQuietly(policyFile2);
        } catch (XmlPullParserException e5) {
            xpe = e5;
            policyFile2 = policyFile;
            Slog.w(TAG, "Got exception parsing " + MAC_PERMISSIONS, xpe);
            z = false;
            IoUtils.closeQuietly(policyFile2);
        } catch (Throwable th2) {
            th = th2;
            policyFile2 = policyFile;
            IoUtils.closeQuietly(policyFile2);
            throw th;
        }
        return z;
    }

    private static Policy readPolicyTags(XmlPullParser parser) throws XmlPullParserException, IOException {
        int outerDepth = parser.getDepth();
        Policy policy = new Policy();
        while (true) {
            int type = parser.next();
            if (type == 1 || (type == 3 && parser.getDepth() <= outerDepth)) {
                break;
            }
            if (type != 3 && type != 4) {
                String tagName = parser.getName();
                if ("seinfo".equals(tagName)) {
                    String seinfo = parseSeinfo(parser);
                    if (seinfo != null) {
                        policy.putSeinfo(seinfo);
                    }
                    XmlUtils.skipCurrentTag(parser);
                } else if ("package".equals(tagName)) {
                    String pkg = parser.getAttributeValue(null, "name");
                    if (!validatePackageName(pkg)) {
                        Slog.w(TAG, "<package> without valid name at " + parser.getPositionDescription());
                        XmlUtils.skipCurrentTag(parser);
                    } else {
                        String seinfo2 = readSeinfoTag(parser);
                        if (seinfo2 != null) {
                            policy.putPkg(pkg, seinfo2);
                        }
                    }
                } else {
                    XmlUtils.skipCurrentTag(parser);
                }
            }
        }
        return policy;
    }

    private static String readSeinfoTag(XmlPullParser parser) throws XmlPullParserException, IOException {
        int outerDepth = parser.getDepth();
        String seinfo = null;
        while (true) {
            int type = parser.next();
            if (type == 1 || (type == 3 && parser.getDepth() <= outerDepth)) {
                break;
            }
            if (type != 3 && type != 4) {
                String tagName = parser.getName();
                if ("seinfo".equals(tagName)) {
                    seinfo = parseSeinfo(parser);
                }
                XmlUtils.skipCurrentTag(parser);
            }
        }
        return seinfo;
    }

    private static String parseSeinfo(XmlPullParser parser) {
        String seinfoValue = parser.getAttributeValue(null, "value");
        if (!validateValue(seinfoValue)) {
            Slog.w(TAG, "<seinfo> without valid value at " + parser.getPositionDescription());
            return null;
        }
        return seinfoValue;
    }

    private static boolean validatePackageName(String name) {
        if (name == null) {
            return false;
        }
        int N = name.length();
        boolean hasSep = false;
        boolean front = true;
        for (int i = 0; i < N; i++) {
            char c = name.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
                front = false;
            } else if (front || ((c < '0' || c > '9') && c != '_')) {
                if (c != '.') {
                    return false;
                }
                hasSep = true;
                front = true;
            }
        }
        return hasSep;
    }

    private static boolean validateValue(String name) {
        int N;
        if (name == null || (N = name.length()) == 0) {
            return false;
        }
        for (int i = 0; i < N; i++) {
            char c = name.charAt(i);
            if ((c < 'a' || c > 'z') && ((c < 'A' || c > 'Z') && c != '_')) {
                return false;
            }
        }
        return true;
    }

    public static boolean assignSeinfoValue(PackageParser.Package pkg) {
        Policy policy;
        String seinfo;
        Signature[] arr$ = pkg.mSignatures;
        for (Signature s : arr$) {
            if (s != null && (policy = sSigSeinfo.get(s)) != null && (seinfo = policy.checkPolicy(pkg.packageName)) != null) {
                pkg.applicationInfo.seinfo = seinfo;
                return true;
            }
        }
        pkg.applicationInfo.seinfo = sDefaultSeinfo;
        return sDefaultSeinfo != null;
    }

    public static boolean shouldRestorecon() {
        try {
            byte[] currentHash = returnHash(SEAPP_CONTEXTS);
            byte[] storedHash = null;
            try {
                storedHash = IoUtils.readFileAsByteArray(SEAPP_HASH_FILE);
            } catch (IOException e) {
                Slog.w(TAG, "Error opening " + SEAPP_HASH_FILE + ". Assuming first boot.");
            }
            return storedHash == null || !MessageDigest.isEqual(storedHash, currentHash);
        } catch (IOException ioe) {
            Slog.e(TAG, "Error with hashing seapp_contexts.", ioe);
            return false;
        }
    }

    public static void setRestoreconDone() throws Throwable {
        try {
            byte[] currentHash = returnHash(SEAPP_CONTEXTS);
            dumpHash(new File(SEAPP_HASH_FILE), currentHash);
        } catch (IOException ioe) {
            Slog.e(TAG, "Error with saving hash to " + SEAPP_HASH_FILE, ioe);
        }
    }

    private static void dumpHash(File file, byte[] content) throws Throwable {
        FileOutputStream fos = null;
        File tmp = null;
        try {
            tmp = File.createTempFile("seapp_hash", ".journal", file.getParentFile());
            tmp.setReadable(true);
            FileOutputStream fos2 = new FileOutputStream(tmp);
            try {
                fos2.write(content);
                fos2.getFD().sync();
                if (!tmp.renameTo(file)) {
                    throw new IOException("Failure renaming " + file.getCanonicalPath());
                }
                if (tmp != null) {
                    tmp.delete();
                }
                IoUtils.closeQuietly(fos2);
            } catch (Throwable th) {
                th = th;
                fos = fos2;
                if (tmp != null) {
                    tmp.delete();
                }
                IoUtils.closeQuietly(fos);
                throw th;
            }
        } catch (Throwable th2) {
            th = th2;
        }
    }

    private static byte[] returnHash(String file) throws IOException {
        try {
            byte[] contents = IoUtils.readFileAsByteArray(file);
            return MessageDigest.getInstance("SHA-1").digest(contents);
        } catch (NoSuchAlgorithmException nsae) {
            throw new RuntimeException(nsae);
        }
    }

    private static boolean useOverridePolicy() {
        try {
            String overrideVersion = IoUtils.readFileAsString(DATA_VERSION_FILE);
            String baseVersion = IoUtils.readFileAsString(BASE_VERSION_FILE);
            if (overrideVersion.equals(baseVersion)) {
                return true;
            }
            Slog.e(TAG, "Override policy version '" + overrideVersion + "' doesn't match base version '" + baseVersion + "'. Skipping override policy files.");
        } catch (FileNotFoundException e) {
        } catch (IOException ioe) {
            Slog.w(TAG, "Skipping override policy files.", ioe);
        }
        return false;
    }
}
