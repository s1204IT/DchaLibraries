package com.googlecode.mp4parser.boxes.ultraviolet;

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
    public void _parseDetails(ByteBuffer byteBuffer) {
        parseVersionAndFlags(byteBuffer);
        this.baseLocation = IsoTypeReader.readString(byteBuffer);
        byteBuffer.get(new byte[(256 - Utf8.utf8StringLengthInBytes(this.baseLocation)) - 1]);
        this.purchaseLocation = IsoTypeReader.readString(byteBuffer);
        byteBuffer.get(new byte[(256 - Utf8.utf8StringLengthInBytes(this.purchaseLocation)) - 1]);
        byteBuffer.get(new byte[512]);
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        writeVersionAndFlags(byteBuffer);
        byteBuffer.put(Utf8.convert(this.baseLocation));
        byteBuffer.put(new byte[256 - Utf8.utf8StringLengthInBytes(this.baseLocation)]);
        byteBuffer.put(Utf8.convert(this.purchaseLocation));
        byteBuffer.put(new byte[256 - Utf8.utf8StringLengthInBytes(this.purchaseLocation)]);
        byteBuffer.put(new byte[512]);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        BaseLocationBox baseLocationBox = (BaseLocationBox) obj;
        if (this.baseLocation == null ? baseLocationBox.baseLocation != null : !this.baseLocation.equals(baseLocationBox.baseLocation)) {
            return false;
        }
        if (this.purchaseLocation == null ? baseLocationBox.purchaseLocation == null : this.purchaseLocation.equals(baseLocationBox.purchaseLocation)) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return (31 * (this.baseLocation != null ? this.baseLocation.hashCode() : 0)) + (this.purchaseLocation != null ? this.purchaseLocation.hashCode() : 0);
    }
}
