package com.coremedia.iso;

import com.coremedia.iso.boxes.Box;
import com.coremedia.iso.boxes.MovieBox;
import com.googlecode.mp4parser.AbstractContainerBox;
import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Iterator;

public class IsoFile extends AbstractContainerBox implements Closeable {
    static final boolean $assertionsDisabled = false;
    protected BoxParser boxParser;
    ReadableByteChannel byteChannel;

    public IsoFile() {
        super("");
        this.boxParser = new PropertyBoxParserImpl(new String[0]);
    }

    public IsoFile(ReadableByteChannel readableByteChannel) throws IOException {
        super("");
        this.boxParser = new PropertyBoxParserImpl(new String[0]);
        this.byteChannel = readableByteChannel;
        this.boxParser = createBoxParser();
        parse();
    }

    protected BoxParser createBoxParser() {
        return new PropertyBoxParserImpl(new String[0]);
    }

    @Override
    public void _parseDetails(ByteBuffer byteBuffer) {
    }

    private void parse() throws IOException {
        boolean z = false;
        while (!z) {
            try {
                Box box = this.boxParser.parseBox(this.byteChannel, this);
                if (box != null) {
                    this.boxes.add(box);
                } else {
                    z = true;
                }
            } catch (EOFException e) {
                z = true;
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("IsoFile[");
        if (this.boxes == null) {
            sb.append("unparsed");
        } else {
            for (int i = 0; i < this.boxes.size(); i++) {
                if (i > 0) {
                    sb.append(";");
                }
                sb.append(this.boxes.get(i).toString());
            }
        }
        sb.append("]");
        return sb.toString();
    }

    public static byte[] fourCCtoBytes(String str) {
        byte[] bArr = new byte[4];
        if (str != null) {
            for (int i = 0; i < Math.min(4, str.length()); i++) {
                bArr[i] = (byte) str.charAt(i);
            }
        }
        return bArr;
    }

    public static String bytesToFourCC(byte[] bArr) {
        byte[] bArr2 = {0, 0, 0, 0};
        if (bArr != null) {
            System.arraycopy(bArr, 0, bArr2, 0, Math.min(bArr.length, 4));
        }
        try {
            return new String(bArr2, "ISO-8859-1");
        } catch (UnsupportedEncodingException e) {
            throw new Error("Required character encoding is missing", e);
        }
    }

    @Override
    public long getSize() {
        Iterator<Box> it = this.boxes.iterator();
        long size = 0;
        while (it.hasNext()) {
            size += it.next().getSize();
        }
        return size;
    }

    @Override
    public IsoFile getIsoFile() {
        return this;
    }

    public MovieBox getMovieBox() {
        for (Box box : this.boxes) {
            if (box instanceof MovieBox) {
                return box;
            }
        }
        return null;
    }

    @Override
    public void getBox(WritableByteChannel writableByteChannel) throws IOException {
        for (Box box : this.boxes) {
            if (writableByteChannel instanceof FileChannel) {
                writableByteChannel.position();
                box.getBox(writableByteChannel);
                writableByteChannel.position();
            } else {
                box.getBox(writableByteChannel);
            }
        }
    }

    @Override
    public void close() throws IOException {
        this.byteChannel.close();
    }
}
