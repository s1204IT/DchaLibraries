package com.android.server.wifi.hotspot2.anqp;

import android.net.Uri;
import android.text.TextUtils;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.wifi.ByteBufferReader;
import java.net.ProtocolException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class OsuProviderInfo {
    private static final int MAXIMUM_I18N_STRING_LENGTH = 252;

    @VisibleForTesting
    public static final int MINIMUM_LENGTH = 9;
    private final List<I18Name> mFriendlyNames;
    private final List<IconInfo> mIconInfoList;
    private final List<Integer> mMethodList;
    private final String mNetworkAccessIdentifier;
    private final Uri mServerUri;
    private final List<I18Name> mServiceDescriptions;

    @VisibleForTesting
    public OsuProviderInfo(List<I18Name> list, Uri uri, List<Integer> list2, List<IconInfo> list3, String str, List<I18Name> list4) {
        this.mFriendlyNames = list;
        this.mServerUri = uri;
        this.mMethodList = list2;
        this.mIconInfoList = list3;
        this.mNetworkAccessIdentifier = str;
        this.mServiceDescriptions = list4;
    }

    public static OsuProviderInfo parse(ByteBuffer byteBuffer) throws ProtocolException {
        int integer = ((int) ByteBufferReader.readInteger(byteBuffer, ByteOrder.LITTLE_ENDIAN, 2)) & Constants.SHORT_MASK;
        if (integer < 9) {
            throw new ProtocolException("Invalid length value: " + integer);
        }
        List<I18Name> i18Names = parseI18Names(getSubBuffer(byteBuffer, ((int) ByteBufferReader.readInteger(byteBuffer, ByteOrder.LITTLE_ENDIAN, 2)) & Constants.SHORT_MASK));
        Uri uri = Uri.parse(ByteBufferReader.readStringWithByteLength(byteBuffer, StandardCharsets.UTF_8));
        ArrayList arrayList = new ArrayList();
        for (int i = byteBuffer.get() & 255; i > 0; i--) {
            arrayList.add(Integer.valueOf(byteBuffer.get() & 255));
        }
        ByteBuffer subBuffer = getSubBuffer(byteBuffer, ((int) ByteBufferReader.readInteger(byteBuffer, ByteOrder.LITTLE_ENDIAN, 2)) & Constants.SHORT_MASK);
        ArrayList arrayList2 = new ArrayList();
        while (subBuffer.hasRemaining()) {
            arrayList2.add(IconInfo.parse(subBuffer));
        }
        return new OsuProviderInfo(i18Names, uri, arrayList, arrayList2, ByteBufferReader.readStringWithByteLength(byteBuffer, StandardCharsets.UTF_8), parseI18Names(getSubBuffer(byteBuffer, ((int) ByteBufferReader.readInteger(byteBuffer, ByteOrder.LITTLE_ENDIAN, 2)) & Constants.SHORT_MASK)));
    }

    public List<I18Name> getFriendlyNames() {
        return Collections.unmodifiableList(this.mFriendlyNames);
    }

    public Uri getServerUri() {
        return this.mServerUri;
    }

    public List<Integer> getMethodList() {
        return Collections.unmodifiableList(this.mMethodList);
    }

    public List<IconInfo> getIconInfoList() {
        return Collections.unmodifiableList(this.mIconInfoList);
    }

    public String getNetworkAccessIdentifier() {
        return this.mNetworkAccessIdentifier;
    }

    public List<I18Name> getServiceDescriptions() {
        return Collections.unmodifiableList(this.mServiceDescriptions);
    }

    public String getFriendlyName() {
        return getI18String(this.mFriendlyNames);
    }

    public String getServiceDescription() {
        return getI18String(this.mServiceDescriptions);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof OsuProviderInfo)) {
            return false;
        }
        OsuProviderInfo osuProviderInfo = (OsuProviderInfo) obj;
        if (this.mFriendlyNames != null ? this.mFriendlyNames.equals(osuProviderInfo.mFriendlyNames) : osuProviderInfo.mFriendlyNames == null) {
            if (this.mServerUri != null ? this.mServerUri.equals(osuProviderInfo.mServerUri) : osuProviderInfo.mServerUri == null) {
                if (this.mMethodList != null ? this.mMethodList.equals(osuProviderInfo.mMethodList) : osuProviderInfo.mMethodList == null) {
                    if (this.mIconInfoList != null ? this.mIconInfoList.equals(osuProviderInfo.mIconInfoList) : osuProviderInfo.mIconInfoList == null) {
                        if (TextUtils.equals(this.mNetworkAccessIdentifier, osuProviderInfo.mNetworkAccessIdentifier)) {
                            if (this.mServiceDescriptions == null) {
                                if (osuProviderInfo.mServiceDescriptions == null) {
                                    return true;
                                }
                            } else if (this.mServiceDescriptions.equals(osuProviderInfo.mServiceDescriptions)) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    public int hashCode() {
        return Objects.hash(this.mFriendlyNames, this.mServerUri, this.mMethodList, this.mIconInfoList, this.mNetworkAccessIdentifier, this.mServiceDescriptions);
    }

    public String toString() {
        return "OsuProviderInfo{mFriendlyNames=" + this.mFriendlyNames + ", mServerUri=" + this.mServerUri + ", mMethodList=" + this.mMethodList + ", mIconInfoList=" + this.mIconInfoList + ", mNetworkAccessIdentifier=" + this.mNetworkAccessIdentifier + ", mServiceDescriptions=" + this.mServiceDescriptions + "}";
    }

    private static List<I18Name> parseI18Names(ByteBuffer byteBuffer) throws ProtocolException {
        ArrayList arrayList = new ArrayList();
        while (byteBuffer.hasRemaining()) {
            I18Name i18Name = I18Name.parse(byteBuffer);
            int length = i18Name.getText().getBytes(StandardCharsets.UTF_8).length;
            if (length > 252) {
                throw new ProtocolException("I18Name string exceeds the maximum allowed " + length);
            }
            arrayList.add(i18Name);
        }
        return arrayList;
    }

    private static ByteBuffer getSubBuffer(ByteBuffer byteBuffer, int i) {
        if (byteBuffer.remaining() < i) {
            throw new BufferUnderflowException();
        }
        ByteBuffer byteBufferSlice = byteBuffer.slice();
        byteBufferSlice.limit(i);
        byteBuffer.position(byteBuffer.position() + i);
        return byteBufferSlice;
    }

    private static String getI18String(List<I18Name> list) {
        for (I18Name i18Name : list) {
            if (i18Name.getLanguage().equals(Locale.getDefault().getLanguage())) {
                return i18Name.getText();
            }
        }
        if (list.size() > 0) {
            return list.get(0).getText();
        }
        return null;
    }
}
