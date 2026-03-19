package android.drm;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

public class DrmInfo {
    private final HashMap<String, Object> mAttributes = new HashMap<>();
    private byte[] mData;
    private final int mInfoType;
    private final String mMimeType;

    public DrmInfo(int i, byte[] bArr, String str) {
        this.mInfoType = i;
        this.mMimeType = str;
        this.mData = bArr;
        if (!isValid()) {
            throw new IllegalArgumentException("infoType: " + i + ",mimeType: " + str + ",data: " + Arrays.toString(bArr));
        }
    }

    public DrmInfo(int i, String str, String str2) {
        this.mInfoType = i;
        this.mMimeType = str2;
        try {
            this.mData = DrmUtils.readBytes(str);
        } catch (IOException e) {
            this.mData = null;
        }
        if (!isValid()) {
            String str3 = "infoType: " + i + ",mimeType: " + str2 + ",data: " + Arrays.toString(this.mData);
            throw new IllegalArgumentException();
        }
    }

    public void put(String str, Object obj) {
        this.mAttributes.put(str, obj);
    }

    public Object get(String str) {
        return this.mAttributes.get(str);
    }

    public Iterator<String> keyIterator() {
        return this.mAttributes.keySet().iterator();
    }

    public Iterator<Object> iterator() {
        return this.mAttributes.values().iterator();
    }

    public byte[] getData() {
        return this.mData;
    }

    public String getMimeType() {
        return this.mMimeType;
    }

    public int getInfoType() {
        return this.mInfoType;
    }

    boolean isValid() {
        return (this.mMimeType == null || this.mMimeType.equals("") || this.mData == null || this.mData.length <= 0 || !DrmInfoRequest.isValidType(this.mInfoType)) ? false : true;
    }
}
