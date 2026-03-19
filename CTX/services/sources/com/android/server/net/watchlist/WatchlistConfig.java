package com.android.server.net.watchlist;

import android.os.FileUtils;
import android.util.Log;
import android.util.Slog;
import android.util.Xml;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.HexDump;
import com.android.internal.util.XmlUtils;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.CRC32;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

class WatchlistConfig {
    private static final String NETWORK_WATCHLIST_DB_FOR_TEST_PATH = "/data/misc/network_watchlist/network_watchlist_for_test.xml";
    private static final String NETWORK_WATCHLIST_DB_PATH = "/data/misc/network_watchlist/network_watchlist.xml";
    private static final String TAG = "WatchlistConfig";
    private static final WatchlistConfig sInstance = new WatchlistConfig();
    private volatile CrcShaDigests mDomainDigests;
    private volatile CrcShaDigests mIpDigests;
    private boolean mIsSecureConfig;
    private File mXmlFile;

    private static class XmlTags {
        private static final String CRC32_DOMAIN = "crc32-domain";
        private static final String CRC32_IP = "crc32-ip";
        private static final String HASH = "hash";
        private static final String SHA256_DOMAIN = "sha256-domain";
        private static final String SHA256_IP = "sha256-ip";
        private static final String WATCHLIST_CONFIG = "watchlist-config";

        private XmlTags() {
        }
    }

    private static class CrcShaDigests {
        final HarmfulDigests crc32Digests;
        final HarmfulDigests sha256Digests;

        public CrcShaDigests(HarmfulDigests harmfulDigests, HarmfulDigests harmfulDigests2) {
            this.crc32Digests = harmfulDigests;
            this.sha256Digests = harmfulDigests2;
        }
    }

    public static WatchlistConfig getInstance() {
        return sInstance;
    }

    private WatchlistConfig() {
        this(new File(NETWORK_WATCHLIST_DB_PATH));
    }

    @VisibleForTesting
    protected WatchlistConfig(File file) {
        this.mIsSecureConfig = true;
        this.mXmlFile = file;
        reloadConfig();
    }

    public void reloadConfig() {
        if (!this.mXmlFile.exists()) {
            return;
        }
        try {
            FileInputStream fileInputStream = new FileInputStream(this.mXmlFile);
            Throwable th = null;
            try {
                List<byte[]> arrayList = new ArrayList<>();
                List<byte[]> arrayList2 = new ArrayList<>();
                List<byte[]> arrayList3 = new ArrayList<>();
                List<byte[]> arrayList4 = new ArrayList<>();
                XmlPullParser xmlPullParserNewPullParser = Xml.newPullParser();
                xmlPullParserNewPullParser.setInput(fileInputStream, StandardCharsets.UTF_8.name());
                xmlPullParserNewPullParser.nextTag();
                xmlPullParserNewPullParser.require(2, null, "watchlist-config");
                while (true) {
                    byte b = 3;
                    if (xmlPullParserNewPullParser.nextTag() != 2) {
                        xmlPullParserNewPullParser.require(3, null, "watchlist-config");
                        this.mDomainDigests = new CrcShaDigests(new HarmfulDigests(arrayList), new HarmfulDigests(arrayList2));
                        this.mIpDigests = new CrcShaDigests(new HarmfulDigests(arrayList3), new HarmfulDigests(arrayList4));
                        Log.i(TAG, "Reload watchlist done");
                        fileInputStream.close();
                        return;
                    }
                    String name = xmlPullParserNewPullParser.getName();
                    int iHashCode = name.hashCode();
                    if (iHashCode != -1862636386) {
                        if (iHashCode != -14835926) {
                            if (iHashCode != 835385997) {
                                b = (iHashCode == 1718657537 && name.equals("crc32-ip")) ? (byte) 1 : (byte) -1;
                            } else if (name.equals("sha256-ip")) {
                            }
                        } else if (name.equals("sha256-domain")) {
                            b = 2;
                        }
                    } else if (name.equals("crc32-domain")) {
                        b = 0;
                    }
                    switch (b) {
                        case 0:
                            parseHashes(xmlPullParserNewPullParser, name, arrayList);
                            break;
                        case 1:
                            parseHashes(xmlPullParserNewPullParser, name, arrayList3);
                            break;
                        case 2:
                            parseHashes(xmlPullParserNewPullParser, name, arrayList2);
                            break;
                        case 3:
                            parseHashes(xmlPullParserNewPullParser, name, arrayList4);
                            break;
                        default:
                            Log.w(TAG, "Unknown element: " + xmlPullParserNewPullParser.getName());
                            XmlUtils.skipCurrentTag(xmlPullParserNewPullParser);
                            break;
                    }
                }
            } catch (Throwable th2) {
                if (0 != 0) {
                    try {
                        fileInputStream.close();
                    } catch (Throwable th3) {
                        th.addSuppressed(th3);
                    }
                } else {
                    fileInputStream.close();
                }
                throw th2;
            }
        } catch (IOException | IllegalStateException | IndexOutOfBoundsException | NullPointerException | NumberFormatException | XmlPullParserException e) {
            Slog.e(TAG, "Failed parsing xml", e);
        }
    }

    private void parseHashes(XmlPullParser xmlPullParser, String str, List<byte[]> list) throws XmlPullParserException, IOException {
        xmlPullParser.require(2, null, str);
        while (xmlPullParser.nextTag() == 2) {
            xmlPullParser.require(2, null, "hash");
            byte[] bArrHexStringToByteArray = HexDump.hexStringToByteArray(xmlPullParser.nextText());
            xmlPullParser.require(3, null, "hash");
            list.add(bArrHexStringToByteArray);
        }
        xmlPullParser.require(3, null, str);
    }

    public boolean containsDomain(String str) {
        CrcShaDigests crcShaDigests = this.mDomainDigests;
        if (crcShaDigests == null) {
            return false;
        }
        if (!crcShaDigests.crc32Digests.contains(getCrc32(str))) {
            return false;
        }
        return crcShaDigests.sha256Digests.contains(getSha256(str));
    }

    public boolean containsIp(String str) {
        CrcShaDigests crcShaDigests = this.mIpDigests;
        if (crcShaDigests == null) {
            return false;
        }
        if (!crcShaDigests.crc32Digests.contains(getCrc32(str))) {
            return false;
        }
        return crcShaDigests.sha256Digests.contains(getSha256(str));
    }

    private byte[] getCrc32(String str) {
        CRC32 crc32 = new CRC32();
        crc32.update(str.getBytes());
        long value = crc32.getValue();
        return new byte[]{(byte) ((value >> 24) & 255), (byte) ((value >> 16) & 255), (byte) ((value >> 8) & 255), (byte) (value & 255)};
    }

    private byte[] getSha256(String str) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA256");
            messageDigest.update(str.getBytes());
            return messageDigest.digest();
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    public boolean isConfigSecure() {
        return this.mIsSecureConfig;
    }

    public byte[] getWatchlistConfigHash() {
        if (!this.mXmlFile.exists()) {
            return null;
        }
        try {
            return DigestUtils.getSha256Hash(this.mXmlFile);
        } catch (IOException | NoSuchAlgorithmException e) {
            Log.e(TAG, "Unable to get watchlist config hash", e);
            return null;
        }
    }

    public void setTestMode(InputStream inputStream) throws IOException {
        Log.i(TAG, "Setting watchlist testing config");
        FileUtils.copyToFileOrThrow(inputStream, new File(NETWORK_WATCHLIST_DB_FOR_TEST_PATH));
        this.mIsSecureConfig = false;
        this.mXmlFile = new File(NETWORK_WATCHLIST_DB_FOR_TEST_PATH);
        reloadConfig();
    }

    public void removeTestModeConfig() {
        try {
            File file = new File(NETWORK_WATCHLIST_DB_FOR_TEST_PATH);
            if (file.exists()) {
                file.delete();
            }
        } catch (Exception e) {
            Log.e(TAG, "Unable to delete test config");
        }
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        byte[] watchlistConfigHash = getWatchlistConfigHash();
        StringBuilder sb = new StringBuilder();
        sb.append("Watchlist config hash: ");
        sb.append(watchlistConfigHash != null ? HexDump.toHexString(watchlistConfigHash) : null);
        printWriter.println(sb.toString());
        printWriter.println("Domain CRC32 digest list:");
        if (this.mDomainDigests != null) {
            this.mDomainDigests.crc32Digests.dump(fileDescriptor, printWriter, strArr);
        }
        printWriter.println("Domain SHA256 digest list:");
        if (this.mDomainDigests != null) {
            this.mDomainDigests.sha256Digests.dump(fileDescriptor, printWriter, strArr);
        }
        printWriter.println("Ip CRC32 digest list:");
        if (this.mIpDigests != null) {
            this.mIpDigests.crc32Digests.dump(fileDescriptor, printWriter, strArr);
        }
        printWriter.println("Ip SHA256 digest list:");
        if (this.mIpDigests != null) {
            this.mIpDigests.sha256Digests.dump(fileDescriptor, printWriter, strArr);
        }
    }
}
