package com.android.server.pm;

import android.content.pm.PackageParser;
import android.os.Environment;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.util.Slog;
import android.util.Xml;
import com.android.server.pm.Policy;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public final class SELinuxMMAC {
    private static final String AUTOPLAY_APP_STR = ":autoplayapp";
    private static final boolean DEBUG_POLICY = false;
    private static final boolean DEBUG_POLICY_INSTALL = false;
    private static final boolean DEBUG_POLICY_ORDER = false;
    private static final String PRIVILEGED_APP_STR = ":privapp";
    static final String TAG = "SELinuxMMAC";
    private static final String XATTR_SEAPP_HASH = "user.seapp_hash";
    private static List<Policy> sPolicies = new ArrayList();
    private static final File VERSION_FILE = new File("/selinux_version");
    private static final File MAC_PERMISSIONS = new File(Environment.getRootDirectory(), "/etc/security/mac_permissions.xml");
    private static final File SEAPP_CONTEXTS = new File("/seapp_contexts");
    private static final byte[] SEAPP_CONTEXTS_HASH = returnHash(SEAPP_CONTEXTS);

    public static boolean readInstallPolicy() throws Throwable {
        FileReader policyFile;
        List<Policy> policies = new ArrayList<>();
        FileReader fileReader = null;
        XmlPullParser parser = Xml.newPullParser();
        try {
            try {
                policyFile = new FileReader(MAC_PERMISSIONS);
            } catch (Throwable th) {
                th = th;
            }
        } catch (IOException e) {
            ioe = e;
        } catch (IllegalArgumentException | IllegalStateException | XmlPullParserException e2) {
            ex = e2;
        }
        try {
            try {
                Slog.d(TAG, "Using policy file " + MAC_PERMISSIONS);
                parser.setInput(policyFile);
                parser.nextTag();
                parser.require(2, null, "policy");
                while (parser.next() != 3) {
                    if (parser.getEventType() == 2) {
                        if (parser.getName().equals("signer")) {
                            policies.add(readSignerOrThrow(parser));
                        } else {
                            skip(parser);
                        }
                    }
                }
                IoUtils.closeQuietly(policyFile);
                PolicyComparator policySort = new PolicyComparator();
                Collections.sort(policies, policySort);
                if (policySort.foundDuplicate()) {
                    Slog.w(TAG, "ERROR! Duplicate entries found parsing " + MAC_PERMISSIONS);
                    return false;
                }
                synchronized (sPolicies) {
                    sPolicies = policies;
                }
                return true;
            } catch (IllegalArgumentException | IllegalStateException | XmlPullParserException e3) {
                ex = e3;
                fileReader = policyFile;
                Slog.w(TAG, "Exception @" + parser.getPositionDescription() + " while parsing " + MAC_PERMISSIONS + ":" + ex);
                IoUtils.closeQuietly(fileReader);
                return false;
            }
        } catch (IOException e4) {
            ioe = e4;
            fileReader = policyFile;
            Slog.w(TAG, "Exception parsing " + MAC_PERMISSIONS, ioe);
            IoUtils.closeQuietly(fileReader);
            return false;
        } catch (Throwable th2) {
            th = th2;
            fileReader = policyFile;
            IoUtils.closeQuietly(fileReader);
            throw th;
        }
    }

    private static Policy readSignerOrThrow(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(2, null, "signer");
        Policy.PolicyBuilder pb = new Policy.PolicyBuilder();
        String cert = parser.getAttributeValue(null, "signature");
        if (cert != null) {
            pb.addSignature(cert);
        }
        while (parser.next() != 3) {
            if (parser.getEventType() == 2) {
                String tagName = parser.getName();
                if ("seinfo".equals(tagName)) {
                    String seinfo = parser.getAttributeValue(null, "value");
                    pb.setGlobalSeinfoOrThrow(seinfo);
                    readSeinfo(parser);
                } else if ("package".equals(tagName)) {
                    readPackageOrThrow(parser, pb);
                } else if ("cert".equals(tagName)) {
                    String sig = parser.getAttributeValue(null, "signature");
                    pb.addSignature(sig);
                    readCert(parser);
                } else {
                    skip(parser);
                }
            }
        }
        return pb.build();
    }

    private static void readPackageOrThrow(XmlPullParser parser, Policy.PolicyBuilder pb) throws XmlPullParserException, IOException {
        parser.require(2, null, "package");
        String pkgName = parser.getAttributeValue(null, "name");
        while (parser.next() != 3) {
            if (parser.getEventType() == 2) {
                String tagName = parser.getName();
                if ("seinfo".equals(tagName)) {
                    String seinfo = parser.getAttributeValue(null, "value");
                    pb.addInnerPackageMapOrThrow(pkgName, seinfo);
                    readSeinfo(parser);
                } else {
                    skip(parser);
                }
            }
        }
    }

    private static void readCert(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(2, null, "cert");
        parser.nextTag();
    }

    private static void readSeinfo(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(2, null, "seinfo");
        parser.nextTag();
    }

    private static void skip(XmlPullParser p) throws XmlPullParserException, IOException {
        if (p.getEventType() != 2) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (p.next()) {
                case 2:
                    depth++;
                    break;
                case 3:
                    depth--;
                    break;
            }
        }
    }

    public static void assignSeinfoValue(PackageParser.Package pkg) {
        synchronized (sPolicies) {
            Iterator policy$iterator = sPolicies.iterator();
            while (true) {
                if (!policy$iterator.hasNext()) {
                    break;
                }
                Policy policy = (Policy) policy$iterator.next();
                String seinfo = policy.getMatchedSeinfo(pkg);
                if (seinfo != null) {
                    break;
                }
            }
        }
        if (pkg.applicationInfo.isAutoPlayApp()) {
            pkg.applicationInfo.seinfo += AUTOPLAY_APP_STR;
        }
        if (!pkg.applicationInfo.isPrivilegedApp()) {
            return;
        }
        pkg.applicationInfo.seinfo += PRIVILEGED_APP_STR;
    }

    public static boolean isRestoreconNeeded(File file) {
        try {
            byte[] buf = new byte[20];
            int len = Os.getxattr(file.getAbsolutePath(), XATTR_SEAPP_HASH, buf);
            if (len != 20) {
                return true;
            }
            if (Arrays.equals(SEAPP_CONTEXTS_HASH, buf)) {
                return false;
            }
            return true;
        } catch (ErrnoException e) {
            if (e.errno != OsConstants.ENODATA) {
                Slog.e(TAG, "Failed to read seapp hash for " + file, e);
                return true;
            }
            return true;
        }
    }

    public static void setRestoreconDone(File file) {
        try {
            Os.setxattr(file.getAbsolutePath(), XATTR_SEAPP_HASH, SEAPP_CONTEXTS_HASH, 0);
        } catch (ErrnoException e) {
            Slog.e(TAG, "Failed to persist seapp hash in " + file, e);
        }
    }

    private static byte[] returnHash(File file) {
        try {
            byte[] contents = IoUtils.readFileAsByteArray(file.getAbsolutePath());
            return MessageDigest.getInstance("SHA-1").digest(contents);
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
