package com.android.server.wifi.hotspot2.anqp;

import android.text.TextUtils;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.wifi.ByteBufferReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class IconInfo {
    private static final int LANGUAGE_CODE_LENGTH = 3;
    private final String mFileName;
    private final int mHeight;
    private final String mIconType;
    private final String mLanguage;
    private final int mWidth;

    @VisibleForTesting
    public IconInfo(int i, int i2, String str, String str2, String str3) {
        this.mWidth = i;
        this.mHeight = i2;
        this.mLanguage = str;
        this.mIconType = str2;
        this.mFileName = str3;
    }

    public static IconInfo parse(ByteBuffer byteBuffer) {
        return new IconInfo(((int) ByteBufferReader.readInteger(byteBuffer, ByteOrder.LITTLE_ENDIAN, 2)) & Constants.SHORT_MASK, ((int) ByteBufferReader.readInteger(byteBuffer, ByteOrder.LITTLE_ENDIAN, 2)) & Constants.SHORT_MASK, ByteBufferReader.readString(byteBuffer, 3, StandardCharsets.US_ASCII).trim(), ByteBufferReader.readStringWithByteLength(byteBuffer, StandardCharsets.US_ASCII), ByteBufferReader.readStringWithByteLength(byteBuffer, StandardCharsets.UTF_8));
    }

    public int getWidth() {
        return this.mWidth;
    }

    public int getHeight() {
        return this.mHeight;
    }

    public String getLanguage() {
        return this.mLanguage;
    }

    public String getIconType() {
        return this.mIconType;
    }

    public String getFileName() {
        return this.mFileName;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof IconInfo)) {
            return false;
        }
        IconInfo iconInfo = (IconInfo) obj;
        return this.mWidth == iconInfo.mWidth && this.mHeight == iconInfo.mHeight && TextUtils.equals(this.mLanguage, iconInfo.mLanguage) && TextUtils.equals(this.mIconType, iconInfo.mIconType) && TextUtils.equals(this.mFileName, iconInfo.mFileName);
    }

    public int hashCode() {
        return Objects.hash(Integer.valueOf(this.mWidth), Integer.valueOf(this.mHeight), this.mLanguage, this.mIconType, this.mFileName);
    }

    public String toString() {
        return "IconInfo{Width=" + this.mWidth + ", Height=" + this.mHeight + ", Language=" + this.mLanguage + ", IconType='" + this.mIconType + "', FileName='" + this.mFileName + "'}";
    }
}
