package com.coremedia.iso.boxes;

import com.coremedia.iso.IsoTypeReader;
import com.coremedia.iso.IsoTypeWriter;
import java.nio.ByteBuffer;

public class VideoMediaHeaderBox extends AbstractMediaHeaderBox {
    private int graphicsmode;
    private int[] opcolor;

    public VideoMediaHeaderBox() {
        super("vmhd");
        this.graphicsmode = 0;
        this.opcolor = new int[]{0, 0, 0};
        setFlags(1);
    }

    public int getGraphicsmode() {
        return this.graphicsmode;
    }

    public int[] getOpcolor() {
        return this.opcolor;
    }

    @Override
    protected long getContentSize() {
        return 12L;
    }

    @Override
    public void _parseDetails(ByteBuffer content) {
        parseVersionAndFlags(content);
        this.graphicsmode = IsoTypeReader.readUInt16(content);
        this.opcolor = new int[3];
        for (int i = 0; i < 3; i++) {
            this.opcolor[i] = IsoTypeReader.readUInt16(content);
        }
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        writeVersionAndFlags(byteBuffer);
        IsoTypeWriter.writeUInt16(byteBuffer, this.graphicsmode);
        int[] arr$ = this.opcolor;
        for (int anOpcolor : arr$) {
            IsoTypeWriter.writeUInt16(byteBuffer, anOpcolor);
        }
    }

    public String toString() {
        return "VideoMediaHeaderBox[graphicsmode=" + getGraphicsmode() + ";opcolor0=" + getOpcolor()[0] + ";opcolor1=" + getOpcolor()[1] + ";opcolor2=" + getOpcolor()[2] + "]";
    }
}
