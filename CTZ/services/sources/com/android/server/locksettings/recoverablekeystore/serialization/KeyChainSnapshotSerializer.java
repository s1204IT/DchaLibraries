package com.android.server.locksettings.recoverablekeystore.serialization;

import android.security.keystore.recovery.KeyChainProtectionParams;
import android.security.keystore.recovery.KeyChainSnapshot;
import android.security.keystore.recovery.KeyDerivationParams;
import android.security.keystore.recovery.WrappedApplicationKey;
import android.util.Base64;
import android.util.Xml;
import java.io.IOException;
import java.io.OutputStream;
import java.security.cert.CertPath;
import java.security.cert.CertificateEncodingException;
import java.util.List;
import org.xmlpull.v1.XmlSerializer;

public class KeyChainSnapshotSerializer {
    public static void serialize(KeyChainSnapshot keyChainSnapshot, OutputStream outputStream) throws IOException, CertificateEncodingException {
        XmlSerializer xmlSerializerNewSerializer = Xml.newSerializer();
        xmlSerializerNewSerializer.setOutput(outputStream, "UTF-8");
        xmlSerializerNewSerializer.startDocument(null, null);
        xmlSerializerNewSerializer.startTag(KeyChainSnapshotSchema.NAMESPACE, "keyChainSnapshot");
        writeKeyChainSnapshotProperties(xmlSerializerNewSerializer, keyChainSnapshot);
        writeKeyChainProtectionParams(xmlSerializerNewSerializer, keyChainSnapshot.getKeyChainProtectionParams());
        writeApplicationKeys(xmlSerializerNewSerializer, keyChainSnapshot.getWrappedApplicationKeys());
        xmlSerializerNewSerializer.endTag(KeyChainSnapshotSchema.NAMESPACE, "keyChainSnapshot");
        xmlSerializerNewSerializer.endDocument();
    }

    private static void writeApplicationKeys(XmlSerializer xmlSerializer, List<WrappedApplicationKey> list) throws IOException {
        xmlSerializer.startTag(KeyChainSnapshotSchema.NAMESPACE, "applicationKeysList");
        for (WrappedApplicationKey wrappedApplicationKey : list) {
            xmlSerializer.startTag(KeyChainSnapshotSchema.NAMESPACE, "applicationKey");
            writeApplicationKeyProperties(xmlSerializer, wrappedApplicationKey);
            xmlSerializer.endTag(KeyChainSnapshotSchema.NAMESPACE, "applicationKey");
        }
        xmlSerializer.endTag(KeyChainSnapshotSchema.NAMESPACE, "applicationKeysList");
    }

    private static void writeApplicationKeyProperties(XmlSerializer xmlSerializer, WrappedApplicationKey wrappedApplicationKey) throws IOException {
        writePropertyTag(xmlSerializer, "alias", wrappedApplicationKey.getAlias());
        writePropertyTag(xmlSerializer, "keyMaterial", wrappedApplicationKey.getEncryptedKeyMaterial());
    }

    private static void writeKeyChainProtectionParams(XmlSerializer xmlSerializer, List<KeyChainProtectionParams> list) throws IOException {
        xmlSerializer.startTag(KeyChainSnapshotSchema.NAMESPACE, "keyChainProtectionParamsList");
        for (KeyChainProtectionParams keyChainProtectionParams : list) {
            xmlSerializer.startTag(KeyChainSnapshotSchema.NAMESPACE, "keyChainProtectionParams");
            writeKeyChainProtectionParamsProperties(xmlSerializer, keyChainProtectionParams);
            xmlSerializer.endTag(KeyChainSnapshotSchema.NAMESPACE, "keyChainProtectionParams");
        }
        xmlSerializer.endTag(KeyChainSnapshotSchema.NAMESPACE, "keyChainProtectionParamsList");
    }

    private static void writeKeyChainProtectionParamsProperties(XmlSerializer xmlSerializer, KeyChainProtectionParams keyChainProtectionParams) throws IOException {
        writePropertyTag(xmlSerializer, "userSecretType", keyChainProtectionParams.getUserSecretType());
        writePropertyTag(xmlSerializer, "lockScreenUiType", keyChainProtectionParams.getLockScreenUiFormat());
        writeKeyDerivationParams(xmlSerializer, keyChainProtectionParams.getKeyDerivationParams());
    }

    private static void writeKeyDerivationParams(XmlSerializer xmlSerializer, KeyDerivationParams keyDerivationParams) throws IOException {
        xmlSerializer.startTag(KeyChainSnapshotSchema.NAMESPACE, "keyDerivationParams");
        writeKeyDerivationParamsProperties(xmlSerializer, keyDerivationParams);
        xmlSerializer.endTag(KeyChainSnapshotSchema.NAMESPACE, "keyDerivationParams");
    }

    private static void writeKeyDerivationParamsProperties(XmlSerializer xmlSerializer, KeyDerivationParams keyDerivationParams) throws IOException {
        writePropertyTag(xmlSerializer, "algorithm", keyDerivationParams.getAlgorithm());
        writePropertyTag(xmlSerializer, "salt", keyDerivationParams.getSalt());
        writePropertyTag(xmlSerializer, "memoryDifficulty", keyDerivationParams.getMemoryDifficulty());
    }

    private static void writeKeyChainSnapshotProperties(XmlSerializer xmlSerializer, KeyChainSnapshot keyChainSnapshot) throws IOException, CertificateEncodingException {
        writePropertyTag(xmlSerializer, "snapshotVersion", keyChainSnapshot.getSnapshotVersion());
        writePropertyTag(xmlSerializer, "maxAttempts", keyChainSnapshot.getMaxAttempts());
        writePropertyTag(xmlSerializer, "counterId", keyChainSnapshot.getCounterId());
        writePropertyTag(xmlSerializer, "recoveryKeyMaterial", keyChainSnapshot.getEncryptedRecoveryKeyBlob());
        writePropertyTag(xmlSerializer, "serverParams", keyChainSnapshot.getServerParams());
        writePropertyTag(xmlSerializer, "thmCertPath", keyChainSnapshot.getTrustedHardwareCertPath());
    }

    private static void writePropertyTag(XmlSerializer xmlSerializer, String str, long j) throws IOException {
        xmlSerializer.startTag(KeyChainSnapshotSchema.NAMESPACE, str);
        xmlSerializer.text(Long.toString(j));
        xmlSerializer.endTag(KeyChainSnapshotSchema.NAMESPACE, str);
    }

    private static void writePropertyTag(XmlSerializer xmlSerializer, String str, String str2) throws IOException {
        xmlSerializer.startTag(KeyChainSnapshotSchema.NAMESPACE, str);
        xmlSerializer.text(str2);
        xmlSerializer.endTag(KeyChainSnapshotSchema.NAMESPACE, str);
    }

    private static void writePropertyTag(XmlSerializer xmlSerializer, String str, byte[] bArr) throws IOException {
        xmlSerializer.startTag(KeyChainSnapshotSchema.NAMESPACE, str);
        xmlSerializer.text(Base64.encodeToString(bArr, 0));
        xmlSerializer.endTag(KeyChainSnapshotSchema.NAMESPACE, str);
    }

    private static void writePropertyTag(XmlSerializer xmlSerializer, String str, CertPath certPath) throws IOException, CertificateEncodingException {
        writePropertyTag(xmlSerializer, str, certPath.getEncoded("PkiPath"));
    }

    private KeyChainSnapshotSerializer() {
    }
}
