package android.drm;

import android.net.ProxyInfo;
import android.net.wifi.WifiEnterpriseConfig;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;

public class DrmUtils {
    static byte[] readBytes(String path) throws IOException {
        File file = new File(path);
        return readBytes(file);
    }

    static byte[] readBytes(File file) throws IOException {
        FileInputStream inputStream = new FileInputStream(file);
        BufferedInputStream bufferedStream = new BufferedInputStream(inputStream);
        byte[] data = null;
        try {
            int length = bufferedStream.available();
            if (length > 0) {
                data = new byte[length];
                bufferedStream.read(data);
            }
            return data;
        } finally {
            quietlyDispose(bufferedStream);
            quietlyDispose(inputStream);
        }
    }

    static void writeToFile(String path, byte[] data) throws Throwable {
        FileOutputStream outputStream;
        FileOutputStream outputStream2 = null;
        if (path == null || data == null) {
            return;
        }
        try {
            outputStream = new FileOutputStream(path);
        } catch (Throwable th) {
            th = th;
        }
        try {
            outputStream.write(data);
            quietlyDispose(outputStream);
        } catch (Throwable th2) {
            th = th2;
            outputStream2 = outputStream;
            quietlyDispose(outputStream2);
            throw th;
        }
    }

    static void removeFile(String path) throws IOException {
        File file = new File(path);
        file.delete();
    }

    private static void quietlyDispose(InputStream stream) {
        if (stream == null) {
            return;
        }
        try {
            stream.close();
        } catch (IOException e) {
        }
    }

    private static void quietlyDispose(OutputStream stream) {
        if (stream == null) {
            return;
        }
        try {
            stream.close();
        } catch (IOException e) {
        }
    }

    public static ExtendedMetadataParser getExtendedMetadataParser(byte[] extendedMetadata) {
        return new ExtendedMetadataParser(extendedMetadata, null);
    }

    public static class ExtendedMetadataParser {
        HashMap<String, String> mMap;

        ExtendedMetadataParser(byte[] constraintData, ExtendedMetadataParser extendedMetadataParser) {
            this(constraintData);
        }

        private int readByte(byte[] constraintData, int arrayIndex) {
            return constraintData[arrayIndex];
        }

        private String readMultipleBytes(byte[] constraintData, int numberOfBytes, int arrayIndex) {
            byte[] returnBytes = new byte[numberOfBytes];
            int j = arrayIndex;
            int i = 0;
            while (j < arrayIndex + numberOfBytes) {
                returnBytes[i] = constraintData[j];
                j++;
                i++;
            }
            return new String(returnBytes);
        }

        private ExtendedMetadataParser(byte[] constraintData) {
            this.mMap = new HashMap<>();
            int index = 0;
            while (index < constraintData.length) {
                int keyLength = readByte(constraintData, index);
                int index2 = index + 1;
                int valueLength = readByte(constraintData, index2);
                int index3 = index2 + 1;
                String strKey = readMultipleBytes(constraintData, keyLength, index3);
                int index4 = index3 + keyLength;
                String strValue = readMultipleBytes(constraintData, valueLength, index4);
                if (strValue.equals(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER)) {
                    strValue = ProxyInfo.LOCAL_EXCL_LIST;
                }
                index = index4 + valueLength;
                this.mMap.put(strKey, strValue);
            }
        }

        public Iterator<String> iterator() {
            return this.mMap.values().iterator();
        }

        public Iterator<String> keyIterator() {
            return this.mMap.keySet().iterator();
        }

        public String get(String key) {
            return this.mMap.get(key);
        }
    }
}
