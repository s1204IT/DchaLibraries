package com.coremedia.iso.boxes;

import com.coremedia.iso.IsoTypeReader;
import com.coremedia.iso.IsoTypeWriter;
import com.coremedia.iso.Utf8;
import com.googlecode.mp4parser.AbstractFullBox;
import java.nio.ByteBuffer;

public class KeywordsBox extends AbstractFullBox {
    private String[] keywords;
    private String language;

    public KeywordsBox() {
        super("kywd");
    }

    public String getLanguage() {
        return this.language;
    }

    @Override
    protected long getContentSize() {
        long contentSize = 7;
        String[] arr$ = this.keywords;
        for (String keyword : arr$) {
            contentSize += (long) (Utf8.utf8StringLengthInBytes(keyword) + 1 + 1);
        }
        return contentSize;
    }

    @Override
    public void _parseDetails(ByteBuffer content) {
        parseVersionAndFlags(content);
        this.language = IsoTypeReader.readIso639(content);
        int keywordCount = IsoTypeReader.readUInt8(content);
        this.keywords = new String[keywordCount];
        for (int i = 0; i < keywordCount; i++) {
            IsoTypeReader.readUInt8(content);
            this.keywords[i] = IsoTypeReader.readString(content);
        }
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        writeVersionAndFlags(byteBuffer);
        IsoTypeWriter.writeIso639(byteBuffer, this.language);
        IsoTypeWriter.writeUInt8(byteBuffer, this.keywords.length);
        String[] arr$ = this.keywords;
        for (String keyword : arr$) {
            IsoTypeWriter.writeUInt8(byteBuffer, Utf8.utf8StringLengthInBytes(keyword) + 1);
            byteBuffer.put(Utf8.convert(keyword));
        }
    }

    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("KeywordsBox[language=").append(getLanguage());
        for (int i = 0; i < this.keywords.length; i++) {
            buffer.append(";keyword").append(i).append("=").append(this.keywords[i]);
        }
        buffer.append("]");
        return buffer.toString();
    }
}
