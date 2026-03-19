package com.android.server.locksettings.recoverablekeystore.serialization;

import android.security.keystore.recovery.KeyChainProtectionParams;
import android.security.keystore.recovery.KeyChainSnapshot;
import android.security.keystore.recovery.KeyDerivationParams;
import android.security.keystore.recovery.WrappedApplicationKey;
import android.util.Base64;
import android.util.Xml;
import com.android.server.backup.BackupManagerConstants;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.CertPath;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class KeyChainSnapshotDeserializer {
    public static KeyChainSnapshot deserialize(InputStream inputStream) throws KeyChainSnapshotParserException, IOException {
        try {
            return deserializeInternal(inputStream);
        } catch (XmlPullParserException e) {
            throw new KeyChainSnapshotParserException("Malformed KeyChainSnapshot XML", e);
        }
    }

    private static KeyChainSnapshot deserializeInternal(InputStream inputStream) throws XmlPullParserException, KeyChainSnapshotParserException, IOException {
        String name;
        XmlPullParser xmlPullParserNewPullParser = Xml.newPullParser();
        xmlPullParserNewPullParser.setInput(inputStream, "UTF-8");
        xmlPullParserNewPullParser.nextTag();
        xmlPullParserNewPullParser.require(2, KeyChainSnapshotSchema.NAMESPACE, "keyChainSnapshot");
        KeyChainSnapshot.Builder builder = new KeyChainSnapshot.Builder();
        while (true) {
            if (xmlPullParserNewPullParser.next() != 3) {
                if (xmlPullParserNewPullParser.getEventType() == 2) {
                    name = xmlPullParserNewPullParser.getName();
                    switch (name) {
                        case "snapshotVersion":
                            builder.setSnapshotVersion(readIntTag(xmlPullParserNewPullParser, "snapshotVersion"));
                            break;
                        case "recoveryKeyMaterial":
                            builder.setEncryptedRecoveryKeyBlob(readBlobTag(xmlPullParserNewPullParser, "recoveryKeyMaterial"));
                            break;
                        case "counterId":
                            builder.setCounterId(readLongTag(xmlPullParserNewPullParser, "counterId"));
                            break;
                        case "serverParams":
                            builder.setServerParams(readBlobTag(xmlPullParserNewPullParser, "serverParams"));
                            break;
                        case "maxAttempts":
                            builder.setMaxAttempts(readIntTag(xmlPullParserNewPullParser, "maxAttempts"));
                            break;
                        case "thmCertPath":
                            try {
                                builder.setTrustedHardwareCertPath(readCertPathTag(xmlPullParserNewPullParser, "thmCertPath"));
                                break;
                            } catch (CertificateException e) {
                                throw new KeyChainSnapshotParserException("Could not set trustedHardwareCertPath", e);
                            }
                            break;
                        case "backendPublicKey":
                            break;
                        case "keyChainProtectionParamsList":
                            builder.setKeyChainProtectionParams(readKeyChainProtectionParamsList(xmlPullParserNewPullParser));
                            break;
                        case "applicationKeysList":
                            builder.setWrappedApplicationKeys(readWrappedApplicationKeys(xmlPullParserNewPullParser));
                            break;
                        default:
                            throw new KeyChainSnapshotParserException(String.format(Locale.US, "Unexpected tag %s in keyChainSnapshot", name));
                    }
                }
            } else {
                xmlPullParserNewPullParser.require(3, KeyChainSnapshotSchema.NAMESPACE, "keyChainSnapshot");
                try {
                    return builder.build();
                } catch (NullPointerException e2) {
                    throw new KeyChainSnapshotParserException("Failed to build KeyChainSnapshot", e2);
                }
            }
        }
    }

    private static List<WrappedApplicationKey> readWrappedApplicationKeys(XmlPullParser xmlPullParser) throws XmlPullParserException, KeyChainSnapshotParserException, IOException {
        xmlPullParser.require(2, KeyChainSnapshotSchema.NAMESPACE, "applicationKeysList");
        ArrayList arrayList = new ArrayList();
        while (xmlPullParser.next() != 3) {
            if (xmlPullParser.getEventType() == 2) {
                arrayList.add(readWrappedApplicationKey(xmlPullParser));
            }
        }
        xmlPullParser.require(3, KeyChainSnapshotSchema.NAMESPACE, "applicationKeysList");
        return arrayList;
    }

    private static WrappedApplicationKey readWrappedApplicationKey(XmlPullParser xmlPullParser) throws XmlPullParserException, KeyChainSnapshotParserException, IOException {
        xmlPullParser.require(2, KeyChainSnapshotSchema.NAMESPACE, "applicationKey");
        WrappedApplicationKey.Builder builder = new WrappedApplicationKey.Builder();
        while (xmlPullParser.next() != 3) {
            if (xmlPullParser.getEventType() == 2) {
                String name = xmlPullParser.getName();
                byte b = -1;
                int iHashCode = name.hashCode();
                if (iHashCode != -963209050) {
                    if (iHashCode == 92902992 && name.equals("alias")) {
                        b = 0;
                    }
                } else if (name.equals("keyMaterial")) {
                    b = 1;
                }
                switch (b) {
                    case 0:
                        builder.setAlias(readStringTag(xmlPullParser, "alias"));
                        break;
                    case 1:
                        builder.setEncryptedKeyMaterial(readBlobTag(xmlPullParser, "keyMaterial"));
                        break;
                    default:
                        throw new KeyChainSnapshotParserException(String.format(Locale.US, "Unexpected tag %s in wrappedApplicationKey", name));
                }
            }
        }
        xmlPullParser.require(3, KeyChainSnapshotSchema.NAMESPACE, "applicationKey");
        try {
            return builder.build();
        } catch (NullPointerException e) {
            throw new KeyChainSnapshotParserException("Failed to build WrappedApplicationKey", e);
        }
    }

    private static List<KeyChainProtectionParams> readKeyChainProtectionParamsList(XmlPullParser xmlPullParser) throws XmlPullParserException, KeyChainSnapshotParserException, IOException {
        xmlPullParser.require(2, KeyChainSnapshotSchema.NAMESPACE, "keyChainProtectionParamsList");
        ArrayList arrayList = new ArrayList();
        while (xmlPullParser.next() != 3) {
            if (xmlPullParser.getEventType() == 2) {
                arrayList.add(readKeyChainProtectionParams(xmlPullParser));
            }
        }
        xmlPullParser.require(3, KeyChainSnapshotSchema.NAMESPACE, "keyChainProtectionParamsList");
        return arrayList;
    }

    private static KeyChainProtectionParams readKeyChainProtectionParams(XmlPullParser xmlPullParser) throws XmlPullParserException, KeyChainSnapshotParserException, IOException {
        xmlPullParser.require(2, KeyChainSnapshotSchema.NAMESPACE, "keyChainProtectionParams");
        KeyChainProtectionParams.Builder builder = new KeyChainProtectionParams.Builder();
        while (xmlPullParser.next() != 3) {
            if (xmlPullParser.getEventType() == 2) {
                String name = xmlPullParser.getName();
                byte b = -1;
                int iHashCode = name.hashCode();
                if (iHashCode != -776797115) {
                    if (iHashCode != -696958923) {
                        if (iHashCode == 912448924 && name.equals("keyDerivationParams")) {
                            b = 2;
                        }
                    } else if (name.equals("userSecretType")) {
                        b = 1;
                    }
                } else if (name.equals("lockScreenUiType")) {
                    b = 0;
                }
                switch (b) {
                    case 0:
                        builder.setLockScreenUiFormat(readIntTag(xmlPullParser, "lockScreenUiType"));
                        break;
                    case 1:
                        builder.setUserSecretType(readIntTag(xmlPullParser, "userSecretType"));
                        break;
                    case 2:
                        builder.setKeyDerivationParams(readKeyDerivationParams(xmlPullParser));
                        break;
                    default:
                        throw new KeyChainSnapshotParserException(String.format(Locale.US, "Unexpected tag %s in keyChainProtectionParams", name));
                }
            }
        }
        xmlPullParser.require(3, KeyChainSnapshotSchema.NAMESPACE, "keyChainProtectionParams");
        try {
            return builder.build();
        } catch (NullPointerException e) {
            throw new KeyChainSnapshotParserException("Failed to build KeyChainProtectionParams", e);
        }
    }

    private static KeyDerivationParams readKeyDerivationParams(XmlPullParser xmlPullParser) throws XmlPullParserException, KeyChainSnapshotParserException, IOException {
        KeyDerivationParams keyDerivationParamsCreateSha256Params;
        byte b;
        xmlPullParser.require(2, KeyChainSnapshotSchema.NAMESPACE, "keyDerivationParams");
        byte[] blobTag = null;
        int intTag = -1;
        int intTag2 = -1;
        while (xmlPullParser.next() != 3) {
            if (xmlPullParser.getEventType() == 2) {
                String name = xmlPullParser.getName();
                int iHashCode = name.hashCode();
                if (iHashCode == -973274212) {
                    if (name.equals("memoryDifficulty")) {
                        b = 0;
                    }
                    switch (b) {
                    }
                } else if (iHashCode != 3522646) {
                    b = (iHashCode == 225490031 && name.equals("algorithm")) ? (byte) 1 : (byte) -1;
                    switch (b) {
                        case 0:
                            intTag2 = readIntTag(xmlPullParser, "memoryDifficulty");
                            break;
                        case 1:
                            intTag = readIntTag(xmlPullParser, "algorithm");
                            break;
                        case 2:
                            blobTag = readBlobTag(xmlPullParser, "salt");
                            break;
                        default:
                            throw new KeyChainSnapshotParserException(String.format(Locale.US, "Unexpected tag %s in keyDerivationParams", name));
                    }
                } else {
                    if (name.equals("salt")) {
                        b = 2;
                    }
                    switch (b) {
                    }
                }
            }
        }
        if (blobTag == null) {
            throw new KeyChainSnapshotParserException("salt was not set in keyDerivationParams");
        }
        switch (intTag) {
            case 1:
                keyDerivationParamsCreateSha256Params = KeyDerivationParams.createSha256Params(blobTag);
                break;
            case 2:
                keyDerivationParamsCreateSha256Params = KeyDerivationParams.createScryptParams(blobTag, intTag2);
                break;
            default:
                throw new KeyChainSnapshotParserException("Unknown algorithm in keyDerivationParams");
        }
        xmlPullParser.require(3, KeyChainSnapshotSchema.NAMESPACE, "keyDerivationParams");
        return keyDerivationParamsCreateSha256Params;
    }

    private static int readIntTag(XmlPullParser xmlPullParser, String str) throws XmlPullParserException, KeyChainSnapshotParserException, IOException {
        xmlPullParser.require(2, KeyChainSnapshotSchema.NAMESPACE, str);
        String text = readText(xmlPullParser);
        xmlPullParser.require(3, KeyChainSnapshotSchema.NAMESPACE, str);
        try {
            return Integer.valueOf(text).intValue();
        } catch (NumberFormatException e) {
            throw new KeyChainSnapshotParserException(String.format(Locale.US, "%s expected int but got '%s'", str, text), e);
        }
    }

    private static long readLongTag(XmlPullParser xmlPullParser, String str) throws XmlPullParserException, KeyChainSnapshotParserException, IOException {
        xmlPullParser.require(2, KeyChainSnapshotSchema.NAMESPACE, str);
        String text = readText(xmlPullParser);
        xmlPullParser.require(3, KeyChainSnapshotSchema.NAMESPACE, str);
        try {
            return Long.valueOf(text).longValue();
        } catch (NumberFormatException e) {
            throw new KeyChainSnapshotParserException(String.format(Locale.US, "%s expected long but got '%s'", str, text), e);
        }
    }

    private static String readStringTag(XmlPullParser xmlPullParser, String str) throws XmlPullParserException, IOException {
        xmlPullParser.require(2, KeyChainSnapshotSchema.NAMESPACE, str);
        String text = readText(xmlPullParser);
        xmlPullParser.require(3, KeyChainSnapshotSchema.NAMESPACE, str);
        return text;
    }

    private static byte[] readBlobTag(XmlPullParser xmlPullParser, String str) throws XmlPullParserException, KeyChainSnapshotParserException, IOException {
        xmlPullParser.require(2, KeyChainSnapshotSchema.NAMESPACE, str);
        String text = readText(xmlPullParser);
        xmlPullParser.require(3, KeyChainSnapshotSchema.NAMESPACE, str);
        try {
            return Base64.decode(text, 0);
        } catch (IllegalArgumentException e) {
            throw new KeyChainSnapshotParserException(String.format(Locale.US, "%s expected base64 encoded bytes but got '%s'", str, text), e);
        }
    }

    private static CertPath readCertPathTag(XmlPullParser xmlPullParser, String str) throws XmlPullParserException, KeyChainSnapshotParserException, IOException {
        try {
            return CertificateFactory.getInstance("X.509").generateCertPath(new ByteArrayInputStream(readBlobTag(xmlPullParser, str)));
        } catch (CertificateException e) {
            throw new KeyChainSnapshotParserException("Could not parse CertPath in tag " + str, e);
        }
    }

    private static String readText(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
        if (xmlPullParser.next() != 4) {
            return BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        }
        String text = xmlPullParser.getText();
        xmlPullParser.nextTag();
        return text;
    }

    private KeyChainSnapshotDeserializer() {
    }
}
