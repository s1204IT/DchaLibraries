package com.android.server.pm;

import android.content.pm.PackageParser;
import android.content.pm.Signature;
import com.android.internal.util.XmlUtils;
import com.android.server.am.AssistDataRequester;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

class PackageSignatures {
    PackageParser.SigningDetails mSigningDetails;

    PackageSignatures(PackageSignatures packageSignatures) {
        if (packageSignatures != null && packageSignatures.mSigningDetails != PackageParser.SigningDetails.UNKNOWN) {
            this.mSigningDetails = new PackageParser.SigningDetails(packageSignatures.mSigningDetails);
        } else {
            this.mSigningDetails = PackageParser.SigningDetails.UNKNOWN;
        }
    }

    PackageSignatures(PackageParser.SigningDetails signingDetails) {
        this.mSigningDetails = signingDetails;
    }

    PackageSignatures() {
        this.mSigningDetails = PackageParser.SigningDetails.UNKNOWN;
    }

    void writeXml(XmlSerializer xmlSerializer, String str, ArrayList<Signature> arrayList) throws IOException {
        if (this.mSigningDetails.signatures == null) {
            return;
        }
        xmlSerializer.startTag(null, str);
        xmlSerializer.attribute(null, AssistDataRequester.KEY_RECEIVER_EXTRA_COUNT, Integer.toString(this.mSigningDetails.signatures.length));
        xmlSerializer.attribute(null, "schemeVersion", Integer.toString(this.mSigningDetails.signatureSchemeVersion));
        writeCertsListXml(xmlSerializer, arrayList, this.mSigningDetails.signatures, null);
        if (this.mSigningDetails.pastSigningCertificates != null) {
            xmlSerializer.startTag(null, "pastSigs");
            xmlSerializer.attribute(null, AssistDataRequester.KEY_RECEIVER_EXTRA_COUNT, Integer.toString(this.mSigningDetails.pastSigningCertificates.length));
            writeCertsListXml(xmlSerializer, arrayList, this.mSigningDetails.pastSigningCertificates, this.mSigningDetails.pastSigningCertificatesFlags);
            xmlSerializer.endTag(null, "pastSigs");
        }
        xmlSerializer.endTag(null, str);
    }

    private void writeCertsListXml(XmlSerializer xmlSerializer, ArrayList<Signature> arrayList, Signature[] signatureArr, int[] iArr) throws IOException {
        for (int i = 0; i < signatureArr.length; i++) {
            xmlSerializer.startTag(null, "cert");
            Signature signature = signatureArr[i];
            int iHashCode = signature.hashCode();
            int size = arrayList.size();
            int i2 = 0;
            while (true) {
                if (i2 >= size) {
                    break;
                }
                Signature signature2 = arrayList.get(i2);
                if (signature2.hashCode() != iHashCode || !signature2.equals(signature)) {
                    i2++;
                } else {
                    xmlSerializer.attribute(null, AssistDataRequester.KEY_RECEIVER_EXTRA_INDEX, Integer.toString(i2));
                    break;
                }
            }
            if (i2 >= size) {
                arrayList.add(signature);
                xmlSerializer.attribute(null, AssistDataRequester.KEY_RECEIVER_EXTRA_INDEX, Integer.toString(size));
                xmlSerializer.attribute(null, "key", signature.toCharsString());
            }
            if (iArr != null) {
                xmlSerializer.attribute(null, "flags", Integer.toString(iArr[i]));
            }
            xmlSerializer.endTag(null, "cert");
        }
    }

    void readXml(XmlPullParser xmlPullParser, ArrayList<Signature> arrayList) throws XmlPullParserException, IOException {
        int i;
        PackageParser.SigningDetails.Builder builder = new PackageParser.SigningDetails.Builder();
        String attributeValue = xmlPullParser.getAttributeValue(null, AssistDataRequester.KEY_RECEIVER_EXTRA_COUNT);
        if (attributeValue == null) {
            PackageManagerService.reportSettingsProblem(5, "Error in package manager settings: <sigs> has no count at " + xmlPullParser.getPositionDescription());
            XmlUtils.skipCurrentTag(xmlPullParser);
        }
        int i2 = Integer.parseInt(attributeValue);
        String attributeValue2 = xmlPullParser.getAttributeValue(null, "schemeVersion");
        if (attributeValue2 == null) {
            PackageManagerService.reportSettingsProblem(5, "Error in package manager settings: <sigs> has no schemeVersion at " + xmlPullParser.getPositionDescription());
            i = 0;
        } else {
            i = Integer.parseInt(attributeValue2);
        }
        builder.setSignatureSchemeVersion(i);
        Signature[] signatureArr = new Signature[i2];
        int certsListXml = readCertsListXml(xmlPullParser, arrayList, signatureArr, null, builder);
        builder.setSignatures(signatureArr);
        if (certsListXml < i2) {
            Signature[] signatureArr2 = new Signature[certsListXml];
            System.arraycopy(signatureArr, 0, signatureArr2, 0, certsListXml);
            builder = builder.setSignatures(signatureArr2);
            PackageManagerService.reportSettingsProblem(5, "Error in package manager settings: <sigs> count does not match number of  <cert> entries" + xmlPullParser.getPositionDescription());
        }
        try {
            this.mSigningDetails = builder.build();
        } catch (CertificateException e) {
            PackageManagerService.reportSettingsProblem(5, "Error in package manager settings: <sigs> unable to convert certificate(s) to public key(s).");
            this.mSigningDetails = PackageParser.SigningDetails.UNKNOWN;
        }
    }

    private int readCertsListXml(XmlPullParser xmlPullParser, ArrayList<Signature> arrayList, Signature[] signatureArr, int[] iArr, PackageParser.SigningDetails.Builder builder) throws XmlPullParserException, IOException {
        String str;
        int i;
        int length = signatureArr.length;
        int depth = xmlPullParser.getDepth();
        PackageParser.SigningDetails.Builder pastSigningCertificatesFlags = builder;
        int i2 = 0;
        while (true) {
            int next = xmlPullParser.next();
            if (next == 1 || (next == 3 && xmlPullParser.getDepth() <= depth)) {
                break;
            }
            if (next != 3 && next != 4) {
                String name = xmlPullParser.getName();
                if (name.equals("cert")) {
                    if (i2 < length) {
                        String attributeValue = xmlPullParser.getAttributeValue(null, AssistDataRequester.KEY_RECEIVER_EXTRA_INDEX);
                        if (attributeValue != null) {
                            try {
                                int i3 = Integer.parseInt(attributeValue);
                                String attributeValue2 = xmlPullParser.getAttributeValue(null, "key");
                                if (attributeValue2 == null) {
                                    if (i3 >= 0 && i3 < arrayList.size()) {
                                        if (arrayList.get(i3) != null) {
                                            signatureArr[i2] = arrayList.get(i3);
                                        } else {
                                            PackageManagerService.reportSettingsProblem(5, "Error in package manager settings: <cert> index " + attributeValue + " is not defined at " + xmlPullParser.getPositionDescription());
                                        }
                                    } else {
                                        PackageManagerService.reportSettingsProblem(5, "Error in package manager settings: <cert> index " + attributeValue + " is out of bounds at " + xmlPullParser.getPositionDescription());
                                    }
                                } else {
                                    while (arrayList.size() <= i3) {
                                        arrayList.add(null);
                                    }
                                    Signature signature = new Signature(attributeValue2);
                                    arrayList.set(i3, signature);
                                    signatureArr[i2] = signature;
                                }
                            } catch (NumberFormatException e) {
                                PackageManagerService.reportSettingsProblem(5, "Error in package manager settings: <cert> index " + attributeValue + " is not a number at " + xmlPullParser.getPositionDescription());
                            } catch (IllegalArgumentException e2) {
                                PackageManagerService.reportSettingsProblem(5, "Error in package manager settings: <cert> index " + attributeValue + " has an invalid signature at " + xmlPullParser.getPositionDescription() + ": " + e2.getMessage());
                            }
                            if (iArr != null) {
                                String attributeValue3 = xmlPullParser.getAttributeValue(null, "flags");
                                if (attributeValue3 != null) {
                                    try {
                                        iArr[i2] = Integer.parseInt(attributeValue3);
                                    } catch (NumberFormatException e3) {
                                        PackageManagerService.reportSettingsProblem(5, "Error in package manager settings: <cert> flags " + attributeValue3 + " is not a number at " + xmlPullParser.getPositionDescription());
                                    }
                                } else {
                                    PackageManagerService.reportSettingsProblem(5, "Error in package manager settings: <cert> has no flags at " + xmlPullParser.getPositionDescription());
                                }
                            }
                        } else {
                            PackageManagerService.reportSettingsProblem(5, "Error in package manager settings: <cert> has no index at " + xmlPullParser.getPositionDescription());
                        }
                    } else {
                        PackageManagerService.reportSettingsProblem(5, "Error in package manager settings: too many <cert> tags, expected " + length + " at " + xmlPullParser.getPositionDescription());
                    }
                    i2++;
                    XmlUtils.skipCurrentTag(xmlPullParser);
                } else if (name.equals("pastSigs")) {
                    if (iArr == null) {
                        String attributeValue4 = xmlPullParser.getAttributeValue(null, AssistDataRequester.KEY_RECEIVER_EXTRA_COUNT);
                        if (attributeValue4 == null) {
                            PackageManagerService.reportSettingsProblem(5, "Error in package manager settings: <pastSigs> has no count at " + xmlPullParser.getPositionDescription());
                            XmlUtils.skipCurrentTag(xmlPullParser);
                        }
                        try {
                            int i4 = Integer.parseInt(attributeValue4);
                            Signature[] signatureArr2 = new Signature[i4];
                            int[] iArr2 = new int[i4];
                            str = attributeValue4;
                            try {
                                int certsListXml = readCertsListXml(xmlPullParser, arrayList, signatureArr2, iArr2, pastSigningCertificatesFlags);
                                PackageParser.SigningDetails.Builder pastSigningCertificatesFlags2 = pastSigningCertificatesFlags.setPastSigningCertificates(signatureArr2).setPastSigningCertificatesFlags(iArr2);
                                if (certsListXml < i4) {
                                    try {
                                        Signature[] signatureArr3 = new Signature[certsListXml];
                                        System.arraycopy(signatureArr2, 0, signatureArr3, 0, certsListXml);
                                        int[] iArr3 = new int[certsListXml];
                                        System.arraycopy(iArr2, 0, iArr3, 0, certsListXml);
                                        pastSigningCertificatesFlags = pastSigningCertificatesFlags2.setPastSigningCertificates(signatureArr3).setPastSigningCertificatesFlags(iArr3);
                                        i = 5;
                                        try {
                                            PackageManagerService.reportSettingsProblem(5, "Error in package manager settings: <pastSigs> count does not match number of <cert> entries " + xmlPullParser.getPositionDescription());
                                        } catch (NumberFormatException e4) {
                                            PackageManagerService.reportSettingsProblem(i, "Error in package manager settings: <pastSigs> count " + str + " is not a number at " + xmlPullParser.getPositionDescription());
                                        }
                                    } catch (NumberFormatException e5) {
                                        i = 5;
                                        pastSigningCertificatesFlags = pastSigningCertificatesFlags2;
                                    }
                                } else {
                                    pastSigningCertificatesFlags = pastSigningCertificatesFlags2;
                                }
                            } catch (NumberFormatException e6) {
                                i = 5;
                            }
                        } catch (NumberFormatException e7) {
                            str = attributeValue4;
                            i = 5;
                        }
                    } else {
                        PackageManagerService.reportSettingsProblem(5, "<pastSigs> encountered multiple times under the same <sigs> at " + xmlPullParser.getPositionDescription());
                        XmlUtils.skipCurrentTag(xmlPullParser);
                    }
                } else {
                    PackageManagerService.reportSettingsProblem(5, "Unknown element under <sigs>: " + xmlPullParser.getName());
                    XmlUtils.skipCurrentTag(xmlPullParser);
                }
            }
        }
        return i2;
    }

    public String toString() {
        StringBuffer stringBuffer = new StringBuffer(128);
        stringBuffer.append("PackageSignatures{");
        stringBuffer.append(Integer.toHexString(System.identityHashCode(this)));
        stringBuffer.append(" version:");
        stringBuffer.append(this.mSigningDetails.signatureSchemeVersion);
        stringBuffer.append(", signatures:[");
        if (this.mSigningDetails.signatures != null) {
            for (int i = 0; i < this.mSigningDetails.signatures.length; i++) {
                if (i > 0) {
                    stringBuffer.append(", ");
                }
                stringBuffer.append(Integer.toHexString(this.mSigningDetails.signatures[i].hashCode()));
            }
        }
        stringBuffer.append("]");
        stringBuffer.append(", past signatures:[");
        if (this.mSigningDetails.pastSigningCertificates != null) {
            for (int i2 = 0; i2 < this.mSigningDetails.pastSigningCertificates.length; i2++) {
                if (i2 > 0) {
                    stringBuffer.append(", ");
                }
                stringBuffer.append(Integer.toHexString(this.mSigningDetails.pastSigningCertificates[i2].hashCode()));
                stringBuffer.append(" flags: ");
                stringBuffer.append(Integer.toHexString(this.mSigningDetails.pastSigningCertificatesFlags[i2]));
            }
        }
        stringBuffer.append("]}");
        return stringBuffer.toString();
    }
}
