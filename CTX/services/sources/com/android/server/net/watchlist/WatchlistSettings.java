package com.android.server.net.watchlist;

import android.os.Environment;
import android.util.AtomicFile;
import android.util.Log;
import android.util.Slog;
import android.util.Xml;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.HexDump;
import com.android.internal.util.XmlUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

class WatchlistSettings {
    private static final String FILE_NAME = "watchlist_settings.xml";
    private static final int SECRET_KEY_LENGTH = 48;
    private static final String TAG = "WatchlistSettings";
    private static final WatchlistSettings sInstance = new WatchlistSettings();
    private byte[] mPrivacySecretKey;
    private final AtomicFile mXmlFile;

    public static WatchlistSettings getInstance() {
        return sInstance;
    }

    private WatchlistSettings() {
        this(getSystemWatchlistFile());
    }

    static File getSystemWatchlistFile() {
        return new File(Environment.getDataSystemDirectory(), FILE_NAME);
    }

    @VisibleForTesting
    protected WatchlistSettings(File file) {
        this.mPrivacySecretKey = null;
        this.mXmlFile = new AtomicFile(file, "net-watchlist");
        reloadSettings();
        if (this.mPrivacySecretKey == null) {
            this.mPrivacySecretKey = generatePrivacySecretKey();
            saveSettings();
        }
    }

    private void reloadSettings() {
        if (!this.mXmlFile.exists()) {
            return;
        }
        try {
            FileInputStream fileInputStreamOpenRead = this.mXmlFile.openRead();
            Throwable th = null;
            try {
                XmlPullParser xmlPullParserNewPullParser = Xml.newPullParser();
                xmlPullParserNewPullParser.setInput(fileInputStreamOpenRead, StandardCharsets.UTF_8.name());
                XmlUtils.beginDocument(xmlPullParserNewPullParser, "network-watchlist-settings");
                int depth = xmlPullParserNewPullParser.getDepth();
                while (XmlUtils.nextElementWithin(xmlPullParserNewPullParser, depth)) {
                    if (xmlPullParserNewPullParser.getName().equals("secret-key")) {
                        this.mPrivacySecretKey = parseSecretKey(xmlPullParserNewPullParser);
                    }
                }
                Slog.i(TAG, "Reload watchlist settings done");
                if (fileInputStreamOpenRead != null) {
                    fileInputStreamOpenRead.close();
                }
            } catch (Throwable th2) {
                if (fileInputStreamOpenRead != null) {
                    if (th != null) {
                        try {
                            fileInputStreamOpenRead.close();
                        } catch (Throwable th3) {
                            th.addSuppressed(th3);
                        }
                    } else {
                        fileInputStreamOpenRead.close();
                    }
                }
                throw th2;
            }
        } catch (IOException | IllegalStateException | IndexOutOfBoundsException | NullPointerException | NumberFormatException | XmlPullParserException e) {
            Slog.e(TAG, "Failed parsing xml", e);
        }
    }

    private byte[] parseSecretKey(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
        xmlPullParser.require(2, null, "secret-key");
        byte[] bArrHexStringToByteArray = HexDump.hexStringToByteArray(xmlPullParser.nextText());
        xmlPullParser.require(3, null, "secret-key");
        if (bArrHexStringToByteArray == null || bArrHexStringToByteArray.length != 48) {
            Log.e(TAG, "Unable to parse secret key");
            return null;
        }
        return bArrHexStringToByteArray;
    }

    synchronized byte[] getPrivacySecretKey() {
        byte[] bArr;
        bArr = new byte[48];
        System.arraycopy(this.mPrivacySecretKey, 0, bArr, 0, 48);
        return bArr;
    }

    private byte[] generatePrivacySecretKey() {
        byte[] bArr = new byte[48];
        new SecureRandom().nextBytes(bArr);
        return bArr;
    }

    private void saveSettings() {
        try {
            FileOutputStream fileOutputStreamStartWrite = this.mXmlFile.startWrite();
            try {
                FastXmlSerializer fastXmlSerializer = new FastXmlSerializer();
                fastXmlSerializer.setOutput(fileOutputStreamStartWrite, StandardCharsets.UTF_8.name());
                fastXmlSerializer.startDocument(null, true);
                fastXmlSerializer.startTag(null, "network-watchlist-settings");
                fastXmlSerializer.startTag(null, "secret-key");
                fastXmlSerializer.text(HexDump.toHexString(this.mPrivacySecretKey));
                fastXmlSerializer.endTag(null, "secret-key");
                fastXmlSerializer.endTag(null, "network-watchlist-settings");
                fastXmlSerializer.endDocument();
                this.mXmlFile.finishWrite(fileOutputStreamStartWrite);
            } catch (IOException e) {
                Log.w(TAG, "Failed to write display settings, restoring backup.", e);
                this.mXmlFile.failWrite(fileOutputStreamStartWrite);
            }
        } catch (IOException e2) {
            Log.w(TAG, "Failed to write display settings: " + e2);
        }
    }
}
