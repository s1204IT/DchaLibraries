package com.googlecode.mp4parser.boxes.mp4.objectdescriptors;

import com.coremedia.iso.Hex;
import java.io.IOException;
import java.nio.ByteBuffer;
import mf.org.apache.xerces.impl.xpath.XPath;

@Descriptor(tags = {XPath.Tokens.EXPRTOKEN_OPERATOR_DIV})
public class ExtensionProfileLevelDescriptor extends BaseDescriptor {
    byte[] bytes;

    @Override
    public void parseDetail(ByteBuffer byteBuffer) throws IOException {
        if (getSize() > 0) {
            this.bytes = new byte[getSize()];
            byteBuffer.get(this.bytes);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ExtensionDescriptor");
        sb.append("{bytes=");
        sb.append(this.bytes == null ? "null" : Hex.encodeHex(this.bytes));
        sb.append('}');
        return sb.toString();
    }
}
