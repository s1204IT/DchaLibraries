package com.googlecode.mp4parser.boxes.ultraviolet;

import android.support.v4.app.NotificationCompat;
import com.coremedia.iso.IsoTypeReader;
import com.coremedia.iso.Utf8;
import com.googlecode.mp4parser.AbstractFullBox;
import java.nio.ByteBuffer;

public class BaseLocationBox extends AbstractFullBox {
    String baseLocation;
    String purchaseLocation;

    public BaseLocationBox() {
        super("bloc");
        this.baseLocation = "";
        this.purchaseLocation = "";
    }

    @Override
    protected long getContentSize() {
        return 1028L;
    }

    @Override
    public void _parseDetails(ByteBuffer content) {
        parseVersionAndFlags(content);
        this.baseLocation = IsoTypeReader.readString(content);
        content.get(new byte[(256 - Utf8.utf8StringLengthInBytes(this.baseLocation)) - 1]);
        this.purchaseLocation = IsoTypeReader.readString(content);
        content.get(new byte[(256 - Utf8.utf8StringLengthInBytes(this.purchaseLocation)) - 1]);
        content.get(new byte[NotificationCompat.FLAG_GROUP_SUMMARY]);
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        writeVersionAndFlags(byteBuffer);
        byteBuffer.put(Utf8.convert(this.baseLocation));
        byteBuffer.put(new byte[256 - Utf8.utf8StringLengthInBytes(this.baseLocation)]);
        byteBuffer.put(Utf8.convert(this.purchaseLocation));
        byteBuffer.put(new byte[256 - Utf8.utf8StringLengthInBytes(this.purchaseLocation)]);
        byteBuffer.put(new byte[NotificationCompat.FLAG_GROUP_SUMMARY]);
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BaseLocationBox that = (BaseLocationBox) o;
        if (this.baseLocation == null ? that.baseLocation != null : !this.baseLocation.equals(that.baseLocation)) {
            return false;
        }
        if (this.purchaseLocation != null) {
            if (this.purchaseLocation.equals(that.purchaseLocation)) {
                return true;
            }
        } else if (that.purchaseLocation == null) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        int result = this.baseLocation != null ? this.baseLocation.hashCode() : 0;
        return (result * 31) + (this.purchaseLocation != null ? this.purchaseLocation.hashCode() : 0);
    }
}
