package com.coremedia.iso.boxes;

import com.coremedia.iso.IsoFile;
import com.coremedia.iso.IsoTypeReader;
import com.coremedia.iso.IsoTypeWriter;
import com.coremedia.iso.Utf8;
import com.googlecode.mp4parser.AbstractFullBox;
import java.nio.ByteBuffer;

public class ClassificationBox extends AbstractFullBox {
    private String classificationEntity;
    private String classificationInfo;
    private int classificationTableIndex;
    private String language;

    public ClassificationBox() {
        super("clsf");
    }

    public String getLanguage() {
        return this.language;
    }

    public String getClassificationEntity() {
        return this.classificationEntity;
    }

    public int getClassificationTableIndex() {
        return this.classificationTableIndex;
    }

    public String getClassificationInfo() {
        return this.classificationInfo;
    }

    @Override
    protected long getContentSize() {
        return 8 + Utf8.utf8StringLengthInBytes(this.classificationInfo) + 1;
    }

    @Override
    public void _parseDetails(ByteBuffer byteBuffer) {
        parseVersionAndFlags(byteBuffer);
        byte[] bArr = new byte[4];
        byteBuffer.get(bArr);
        this.classificationEntity = IsoFile.bytesToFourCC(bArr);
        this.classificationTableIndex = IsoTypeReader.readUInt16(byteBuffer);
        this.language = IsoTypeReader.readIso639(byteBuffer);
        this.classificationInfo = IsoTypeReader.readString(byteBuffer);
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        byteBuffer.put(IsoFile.fourCCtoBytes(this.classificationEntity));
        IsoTypeWriter.writeUInt16(byteBuffer, this.classificationTableIndex);
        IsoTypeWriter.writeIso639(byteBuffer, this.language);
        byteBuffer.put(Utf8.convert(this.classificationInfo));
        byteBuffer.put((byte) 0);
    }

    public String toString() {
        return "ClassificationBox[language=" + getLanguage() + "classificationEntity=" + getClassificationEntity() + ";classificationTableIndex=" + getClassificationTableIndex() + ";language=" + getLanguage() + ";classificationInfo=" + getClassificationInfo() + "]";
    }
}
