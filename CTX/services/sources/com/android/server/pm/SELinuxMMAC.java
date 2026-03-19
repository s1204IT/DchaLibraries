package com.android.server.pm;

import android.content.pm.PackageParser;
import android.os.Environment;
import android.util.Slog;
import android.util.Xml;
import com.android.server.pm.Policy;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public final class SELinuxMMAC {
    private static final boolean DEBUG_POLICY = false;
    private static final boolean DEBUG_POLICY_INSTALL = false;
    private static final boolean DEBUG_POLICY_ORDER = false;
    private static final String DEFAULT_SEINFO = "default";
    private static final String PRIVILEGED_APP_STR = ":privapp";
    private static final String SANDBOX_V2_STR = ":v2";
    static final String TAG = "SELinuxMMAC";
    private static final String TARGETSDKVERSION_STR = ":targetSdkVersion=";
    private static boolean sPolicyRead;
    private static List<Policy> sPolicies = new ArrayList();
    private static List<File> sMacPermissions = new ArrayList();

    static {
        sMacPermissions.add(new File(Environment.getRootDirectory(), "/etc/selinux/plat_mac_permissions.xml"));
        File file = new File(Environment.getVendorDirectory(), "/etc/selinux/vendor_mac_permissions.xml");
        if (file.exists()) {
            sMacPermissions.add(file);
        } else {
            sMacPermissions.add(new File(Environment.getVendorDirectory(), "/etc/selinux/nonplat_mac_permissions.xml"));
        }
        File file2 = new File(Environment.getOdmDirectory(), "/etc/selinux/odm_mac_permissions.xml");
        if (file2.exists()) {
            sMacPermissions.add(file2);
        }
    }

    public static boolean readInstallPolicy() throws Throwable {
        FileReader fileReader;
        synchronized (sPolicies) {
            if (sPolicyRead) {
                return true;
            }
            ArrayList arrayList = new ArrayList();
            XmlPullParser xmlPullParserNewPullParser = Xml.newPullParser();
            int size = sMacPermissions.size();
            FileReader fileReader2 = null;
            int i = 0;
            while (i < size) {
                File file = sMacPermissions.get(i);
                try {
                    try {
                        fileReader = new FileReader(file);
                    } catch (Throwable th) {
                        th = th;
                    }
                } catch (IOException e) {
                    e = e;
                } catch (IllegalArgumentException | IllegalStateException | XmlPullParserException e2) {
                    e = e2;
                }
                try {
                    Slog.d(TAG, "Using policy file " + file);
                    xmlPullParserNewPullParser.setInput(fileReader);
                    xmlPullParserNewPullParser.nextTag();
                    xmlPullParserNewPullParser.require(2, null, "policy");
                    while (xmlPullParserNewPullParser.next() != 3) {
                        if (xmlPullParserNewPullParser.getEventType() == 2) {
                            String name = xmlPullParserNewPullParser.getName();
                            byte b = -1;
                            if (name.hashCode() == -902467798 && name.equals("signer")) {
                                b = 0;
                            }
                            if (b != 0) {
                                skip(xmlPullParserNewPullParser);
                            } else {
                                arrayList.add(readSignerOrThrow(xmlPullParserNewPullParser));
                            }
                        }
                    }
                    IoUtils.closeQuietly(fileReader);
                    i++;
                    fileReader2 = fileReader;
                } catch (IOException e3) {
                    e = e3;
                    fileReader2 = fileReader;
                    Slog.w(TAG, "Exception parsing " + file, e);
                    IoUtils.closeQuietly(fileReader2);
                    return false;
                } catch (IllegalArgumentException | IllegalStateException | XmlPullParserException e4) {
                    e = e4;
                    fileReader2 = fileReader;
                    Slog.w(TAG, "Exception @" + xmlPullParserNewPullParser.getPositionDescription() + " while parsing " + file + ":" + e);
                    IoUtils.closeQuietly(fileReader2);
                    return false;
                } catch (Throwable th2) {
                    th = th2;
                    fileReader2 = fileReader;
                    IoUtils.closeQuietly(fileReader2);
                    throw th;
                }
            }
            PolicyComparator policyComparator = new PolicyComparator();
            Collections.sort(arrayList, policyComparator);
            if (policyComparator.foundDuplicate()) {
                Slog.w(TAG, "ERROR! Duplicate entries found parsing mac_permissions.xml files");
                return false;
            }
            synchronized (sPolicies) {
                sPolicies.clear();
                sPolicies.addAll(arrayList);
                sPolicyRead = true;
            }
            return true;
        }
    }

    private static Policy readSignerOrThrow(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
        xmlPullParser.require(2, null, "signer");
        Policy.PolicyBuilder policyBuilder = new Policy.PolicyBuilder();
        String attributeValue = xmlPullParser.getAttributeValue(null, "signature");
        if (attributeValue != null) {
            policyBuilder.addSignature(attributeValue);
        }
        while (xmlPullParser.next() != 3) {
            if (xmlPullParser.getEventType() == 2) {
                String name = xmlPullParser.getName();
                if ("seinfo".equals(name)) {
                    policyBuilder.setGlobalSeinfoOrThrow(xmlPullParser.getAttributeValue(null, "value"));
                    readSeinfo(xmlPullParser);
                } else if (Settings.ATTR_PACKAGE.equals(name)) {
                    readPackageOrThrow(xmlPullParser, policyBuilder);
                } else if ("cert".equals(name)) {
                    policyBuilder.addSignature(xmlPullParser.getAttributeValue(null, "signature"));
                    readCert(xmlPullParser);
                } else {
                    skip(xmlPullParser);
                }
            }
        }
        return policyBuilder.build();
    }

    private static void readPackageOrThrow(XmlPullParser xmlPullParser, Policy.PolicyBuilder policyBuilder) throws XmlPullParserException, IOException {
        xmlPullParser.require(2, null, Settings.ATTR_PACKAGE);
        String attributeValue = xmlPullParser.getAttributeValue(null, Settings.ATTR_NAME);
        while (xmlPullParser.next() != 3) {
            if (xmlPullParser.getEventType() == 2) {
                if ("seinfo".equals(xmlPullParser.getName())) {
                    policyBuilder.addInnerPackageMapOrThrow(attributeValue, xmlPullParser.getAttributeValue(null, "value"));
                    readSeinfo(xmlPullParser);
                } else {
                    skip(xmlPullParser);
                }
            }
        }
    }

    private static void readCert(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
        xmlPullParser.require(2, null, "cert");
        xmlPullParser.nextTag();
    }

    private static void readSeinfo(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
        xmlPullParser.require(2, null, "seinfo");
        xmlPullParser.nextTag();
    }

    private static void skip(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
        if (xmlPullParser.getEventType() != 2) {
            throw new IllegalStateException();
        }
        int i = 1;
        while (i != 0) {
            switch (xmlPullParser.next()) {
                case 2:
                    i++;
                    break;
                case 3:
                    i--;
                    break;
            }
        }
    }

    public static String getSeInfo(PackageParser.Package r4, boolean z, int i, int i2) {
        String matchedSeInfo;
        synchronized (sPolicies) {
            matchedSeInfo = null;
            if (sPolicyRead) {
                Iterator<Policy> it = sPolicies.iterator();
                while (it.hasNext() && (matchedSeInfo = it.next().getMatchedSeInfo(r4)) == null) {
                }
            }
        }
        if (matchedSeInfo == null) {
            matchedSeInfo = "default";
        }
        if (i == 2) {
            matchedSeInfo = matchedSeInfo + SANDBOX_V2_STR;
        }
        if (z) {
            matchedSeInfo = matchedSeInfo + PRIVILEGED_APP_STR;
        }
        return matchedSeInfo + TARGETSDKVERSION_STR + i2;
    }
}
