package com.android.server.devicepolicy;

import android.content.ComponentName;
import android.os.Environment;
import android.text.TextUtils;
import android.util.AtomicFile;
import android.util.Slog;
import android.util.Xml;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.Preconditions;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

class TransferOwnershipMetadataManager {
    static final String ADMIN_TYPE_DEVICE_OWNER = "device-owner";
    static final String ADMIN_TYPE_PROFILE_OWNER = "profile-owner";
    public static final String OWNER_TRANSFER_METADATA_XML = "owner-transfer-metadata.xml";
    private static final String TAG = TransferOwnershipMetadataManager.class.getName();

    @VisibleForTesting
    static final String TAG_ADMIN_TYPE = "admin-type";

    @VisibleForTesting
    static final String TAG_SOURCE_COMPONENT = "source-component";

    @VisibleForTesting
    static final String TAG_TARGET_COMPONENT = "target-component";

    @VisibleForTesting
    static final String TAG_USER_ID = "user-id";
    private final Injector mInjector;

    TransferOwnershipMetadataManager() {
        this(new Injector());
    }

    @VisibleForTesting
    TransferOwnershipMetadataManager(Injector injector) {
        this.mInjector = injector;
    }

    boolean saveMetadataFile(Metadata metadata) {
        FileOutputStream fileOutputStreamStartWrite;
        File file = new File(this.mInjector.getOwnerTransferMetadataDir(), OWNER_TRANSFER_METADATA_XML);
        AtomicFile atomicFile = new AtomicFile(file);
        try {
            fileOutputStreamStartWrite = atomicFile.startWrite();
        } catch (IOException e) {
            e = e;
            fileOutputStreamStartWrite = null;
        }
        try {
            XmlSerializer fastXmlSerializer = new FastXmlSerializer();
            fastXmlSerializer.setOutput(fileOutputStreamStartWrite, StandardCharsets.UTF_8.name());
            fastXmlSerializer.startDocument(null, true);
            insertSimpleTag(fastXmlSerializer, TAG_USER_ID, Integer.toString(metadata.userId));
            insertSimpleTag(fastXmlSerializer, TAG_SOURCE_COMPONENT, metadata.sourceComponent.flattenToString());
            insertSimpleTag(fastXmlSerializer, TAG_TARGET_COMPONENT, metadata.targetComponent.flattenToString());
            insertSimpleTag(fastXmlSerializer, TAG_ADMIN_TYPE, metadata.adminType);
            fastXmlSerializer.endDocument();
            atomicFile.finishWrite(fileOutputStreamStartWrite);
            return true;
        } catch (IOException e2) {
            e = e2;
            Slog.e(TAG, "Caught exception while trying to save Owner Transfer Params to file " + file, e);
            file.delete();
            atomicFile.failWrite(fileOutputStreamStartWrite);
            return false;
        }
    }

    private void insertSimpleTag(XmlSerializer xmlSerializer, String str, String str2) throws IOException {
        xmlSerializer.startTag(null, str);
        xmlSerializer.text(str2);
        xmlSerializer.endTag(null, str);
    }

    Metadata loadMetadataFile() {
        Throwable th;
        Throwable th2;
        File file = new File(this.mInjector.getOwnerTransferMetadataDir(), OWNER_TRANSFER_METADATA_XML);
        if (!file.exists()) {
            return null;
        }
        Slog.d(TAG, "Loading TransferOwnershipMetadataManager from " + file);
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            try {
                XmlPullParser xmlPullParserNewPullParser = Xml.newPullParser();
                xmlPullParserNewPullParser.setInput(fileInputStream, null);
                Metadata metadataFile = parseMetadataFile(xmlPullParserNewPullParser);
                fileInputStream.close();
                return metadataFile;
            } catch (Throwable th3) {
                try {
                    throw th3;
                } catch (Throwable th4) {
                    th = th3;
                    th2 = th4;
                    if (th != null) {
                        fileInputStream.close();
                        throw th2;
                    }
                    try {
                        fileInputStream.close();
                        throw th2;
                    } catch (Throwable th5) {
                        th.addSuppressed(th5);
                        throw th2;
                    }
                }
            }
        } catch (IOException | IllegalArgumentException | XmlPullParserException e) {
            Slog.e(TAG, "Caught exception while trying to load the owner transfer params from file " + file, e);
            return null;
        }
    }

    private Metadata parseMetadataFile(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
        byte b;
        int depth = xmlPullParser.getDepth();
        String text = null;
        int i = 0;
        String text2 = null;
        String text3 = null;
        while (true) {
            int next = xmlPullParser.next();
            if (next != 1 && (next != 3 || xmlPullParser.getDepth() > depth)) {
                if (next != 3 && next != 4) {
                    String name = xmlPullParser.getName();
                    int iHashCode = name.hashCode();
                    if (iHashCode == -337219647) {
                        if (name.equals(TAG_TARGET_COMPONENT)) {
                            b = 1;
                        }
                        switch (b) {
                        }
                    } else if (iHashCode == -147180963) {
                        if (name.equals(TAG_USER_ID)) {
                            b = 0;
                        }
                        switch (b) {
                        }
                    } else if (iHashCode != 281362891) {
                        b = (iHashCode == 641951480 && name.equals(TAG_ADMIN_TYPE)) ? (byte) 3 : (byte) -1;
                        switch (b) {
                            case 0:
                                xmlPullParser.next();
                                i = Integer.parseInt(xmlPullParser.getText());
                                break;
                            case 1:
                                xmlPullParser.next();
                                text2 = xmlPullParser.getText();
                                break;
                            case 2:
                                xmlPullParser.next();
                                text = xmlPullParser.getText();
                                break;
                            case 3:
                                xmlPullParser.next();
                                text3 = xmlPullParser.getText();
                                break;
                        }
                    } else {
                        if (name.equals(TAG_SOURCE_COMPONENT)) {
                            b = 2;
                        }
                        switch (b) {
                        }
                    }
                }
            }
        }
        return new Metadata(text, text2, i, text3);
    }

    void deleteMetadataFile() {
        new File(this.mInjector.getOwnerTransferMetadataDir(), OWNER_TRANSFER_METADATA_XML).delete();
    }

    boolean metadataFileExists() {
        return new File(this.mInjector.getOwnerTransferMetadataDir(), OWNER_TRANSFER_METADATA_XML).exists();
    }

    static class Metadata {
        final String adminType;
        final ComponentName sourceComponent;
        final ComponentName targetComponent;
        final int userId;

        Metadata(ComponentName componentName, ComponentName componentName2, int i, String str) {
            this.sourceComponent = componentName;
            this.targetComponent = componentName2;
            Preconditions.checkNotNull(componentName);
            Preconditions.checkNotNull(componentName2);
            Preconditions.checkStringNotEmpty(str);
            this.userId = i;
            this.adminType = str;
        }

        Metadata(String str, String str2, int i, String str3) {
            this(unflattenComponentUnchecked(str), unflattenComponentUnchecked(str2), i, str3);
        }

        private static ComponentName unflattenComponentUnchecked(String str) {
            Preconditions.checkNotNull(str);
            return ComponentName.unflattenFromString(str);
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof Metadata)) {
                return false;
            }
            Metadata metadata = (Metadata) obj;
            return this.userId == metadata.userId && this.sourceComponent.equals(metadata.sourceComponent) && this.targetComponent.equals(metadata.targetComponent) && TextUtils.equals(this.adminType, metadata.adminType);
        }

        public int hashCode() {
            return (31 * (((((this.userId + 31) * 31) + this.sourceComponent.hashCode()) * 31) + this.targetComponent.hashCode())) + this.adminType.hashCode();
        }
    }

    @VisibleForTesting
    static class Injector {
        Injector() {
        }

        public File getOwnerTransferMetadataDir() {
            return Environment.getDataSystemDirectory();
        }
    }
}
