package com.coremedia.iso.boxes.apple;

import com.coremedia.iso.IsoTypeReader;
import com.coremedia.iso.Utf8;
import com.googlecode.mp4parser.AbstractFullBox;
import com.mediatek.plugin.builder.PluginDescriptorBuilder;
import java.nio.ByteBuffer;

public final class AppleNameBox extends AbstractFullBox {
    private String name;

    public AppleNameBox() {
        super(PluginDescriptorBuilder.VALUE_NAME);
    }

    @Override
    protected long getContentSize() {
        return 4 + Utf8.convert(this.name).length;
    }

    @Override
    public void _parseDetails(ByteBuffer byteBuffer) {
        parseVersionAndFlags(byteBuffer);
        this.name = IsoTypeReader.readString(byteBuffer, byteBuffer.remaining());
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        writeVersionAndFlags(byteBuffer);
        byteBuffer.put(Utf8.convert(this.name));
    }
}
