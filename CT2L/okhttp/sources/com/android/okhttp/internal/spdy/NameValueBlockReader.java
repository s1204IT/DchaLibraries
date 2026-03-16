package com.android.okhttp.internal.spdy;

import com.android.okio.BufferedSource;
import com.android.okio.ByteString;
import com.android.okio.Deadline;
import com.android.okio.InflaterSource;
import com.android.okio.OkBuffer;
import com.android.okio.Okio;
import com.android.okio.Source;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

class NameValueBlockReader {
    private int compressedLimit;
    private final InflaterSource inflaterSource;
    private final BufferedSource source;

    static int access$022(NameValueBlockReader x0, long x1) {
        int i = (int) (((long) x0.compressedLimit) - x1);
        x0.compressedLimit = i;
        return i;
    }

    public NameValueBlockReader(final BufferedSource source) {
        Source throttleSource = new Source() {
            @Override
            public long read(OkBuffer sink, long byteCount) throws IOException {
                if (NameValueBlockReader.this.compressedLimit == 0) {
                    return -1L;
                }
                long read = source.read(sink, Math.min(byteCount, NameValueBlockReader.this.compressedLimit));
                if (read == -1) {
                    return -1L;
                }
                NameValueBlockReader.access$022(NameValueBlockReader.this, read);
                return read;
            }

            @Override
            public void close() throws IOException {
                source.close();
            }

            @Override
            public Source mo2deadline(Deadline deadline) {
                source.mo2deadline(deadline);
                return this;
            }
        };
        Inflater inflater = new Inflater() {
            @Override
            public int inflate(byte[] buffer, int offset, int count) throws DataFormatException {
                int result = super.inflate(buffer, offset, count);
                if (result == 0 && needsDictionary()) {
                    setDictionary(Spdy3.DICTIONARY);
                    return super.inflate(buffer, offset, count);
                }
                return result;
            }
        };
        this.inflaterSource = new InflaterSource(throttleSource, inflater);
        this.source = Okio.buffer(this.inflaterSource);
    }

    public List<Header> readNameValueBlock(int length) throws IOException {
        this.compressedLimit += length;
        int numberOfPairs = this.source.readInt();
        if (numberOfPairs < 0) {
            throw new IOException("numberOfPairs < 0: " + numberOfPairs);
        }
        if (numberOfPairs > 1024) {
            throw new IOException("numberOfPairs > 1024: " + numberOfPairs);
        }
        List<Header> entries = new ArrayList<>(numberOfPairs);
        for (int i = 0; i < numberOfPairs; i++) {
            ByteString name = readByteString().toAsciiLowercase();
            ByteString values = readByteString();
            if (name.size() == 0) {
                throw new IOException("name.size == 0");
            }
            entries.add(new Header(name, values));
        }
        doneReading();
        return entries;
    }

    private ByteString readByteString() throws IOException {
        int length = this.source.readInt();
        return this.source.readByteString(length);
    }

    private void doneReading() throws IOException {
        if (this.compressedLimit > 0) {
            this.inflaterSource.refill();
            if (this.compressedLimit != 0) {
                throw new IOException("compressedLimit > 0: " + this.compressedLimit);
            }
        }
    }

    public void close() throws IOException {
        this.source.close();
    }
}
