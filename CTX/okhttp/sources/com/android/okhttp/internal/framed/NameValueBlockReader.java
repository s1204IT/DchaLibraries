package com.android.okhttp.internal.framed;

import com.android.okhttp.okio.Buffer;
import com.android.okhttp.okio.BufferedSource;
import com.android.okhttp.okio.ByteString;
import com.android.okhttp.okio.ForwardingSource;
import com.android.okhttp.okio.InflaterSource;
import com.android.okhttp.okio.Okio;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

class NameValueBlockReader {
    private int compressedLimit;
    private final InflaterSource inflaterSource;
    private final BufferedSource source;

    static int access$022(NameValueBlockReader nameValueBlockReader, long j) {
        int i = (int) (((long) nameValueBlockReader.compressedLimit) - j);
        nameValueBlockReader.compressedLimit = i;
        return i;
    }

    public NameValueBlockReader(BufferedSource bufferedSource) {
        this.inflaterSource = new InflaterSource(new ForwardingSource(bufferedSource) {
            @Override
            public long read(Buffer buffer, long j) throws IOException {
                if (NameValueBlockReader.this.compressedLimit == 0) {
                    return -1L;
                }
                long j2 = super.read(buffer, Math.min(j, NameValueBlockReader.this.compressedLimit));
                if (j2 == -1) {
                    return -1L;
                }
                NameValueBlockReader.access$022(NameValueBlockReader.this, j2);
                return j2;
            }
        }, new Inflater() {
            @Override
            public int inflate(byte[] bArr, int i, int i2) throws DataFormatException {
                int iInflate = super.inflate(bArr, i, i2);
                if (iInflate == 0 && needsDictionary()) {
                    setDictionary(Spdy3.DICTIONARY);
                    return super.inflate(bArr, i, i2);
                }
                return iInflate;
            }
        });
        this.source = Okio.buffer(this.inflaterSource);
    }

    public List<Header> readNameValueBlock(int i) throws IOException {
        this.compressedLimit += i;
        int i2 = this.source.readInt();
        if (i2 < 0) {
            throw new IOException("numberOfPairs < 0: " + i2);
        }
        if (i2 > 1024) {
            throw new IOException("numberOfPairs > 1024: " + i2);
        }
        ArrayList arrayList = new ArrayList(i2);
        for (int i3 = 0; i3 < i2; i3++) {
            ByteString asciiLowercase = readByteString().toAsciiLowercase();
            ByteString byteString = readByteString();
            if (asciiLowercase.size() == 0) {
                throw new IOException("name.size == 0");
            }
            arrayList.add(new Header(asciiLowercase, byteString));
        }
        doneReading();
        return arrayList;
    }

    private ByteString readByteString() throws IOException {
        return this.source.readByteString(this.source.readInt());
    }

    private void doneReading() throws IOException {
        if (this.compressedLimit > 0) {
            this.inflaterSource.refill();
            if (this.compressedLimit == 0) {
                return;
            }
            throw new IOException("compressedLimit > 0: " + this.compressedLimit);
        }
    }

    public void close() throws IOException {
        this.source.close();
    }
}
