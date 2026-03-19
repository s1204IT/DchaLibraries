package com.coremedia.iso.boxes;

import com.googlecode.mp4parser.AbstractBox;
import java.nio.ByteBuffer;

public class UserBox extends AbstractBox {
    byte[] data;

    @Override
    protected long getContentSize() {
        return this.data.length;
    }

    public String toString() {
        return "UserBox[type=" + getType() + ";userType=" + new String(getUserType()) + ";contentLength=" + this.data.length + "]";
    }

    @Override
    public void _parseDetails(ByteBuffer byteBuffer) {
        this.data = new byte[byteBuffer.remaining()];
        byteBuffer.get(this.data);
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        byteBuffer.put(this.data);
    }
}
