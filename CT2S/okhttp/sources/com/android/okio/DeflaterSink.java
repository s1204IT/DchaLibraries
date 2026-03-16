package com.android.okio;

import java.io.IOException;
import java.util.zip.Deflater;

public final class DeflaterSink implements Sink {
    private boolean closed;
    private final Deflater deflater;
    private final BufferedSink sink;

    public DeflaterSink(Sink sink, Deflater deflater) {
        this.sink = Okio.buffer(sink);
        this.deflater = deflater;
    }

    @Override
    public void write(OkBuffer source, long byteCount) throws IOException {
        Util.checkOffsetAndCount(source.size, 0L, byteCount);
        while (byteCount > 0) {
            Segment head = source.head;
            int toDeflate = (int) Math.min(byteCount, head.limit - head.pos);
            this.deflater.setInput(head.data, head.pos, toDeflate);
            deflate(false);
            source.size -= (long) toDeflate;
            head.pos += toDeflate;
            if (head.pos == head.limit) {
                source.head = head.pop();
                SegmentPool.INSTANCE.recycle(head);
            }
            byteCount -= (long) toDeflate;
        }
    }

    private void deflate(boolean syncFlush) throws IOException {
        OkBuffer buffer = this.sink.buffer();
        while (true) {
            Segment s = buffer.writableSegment(1);
            int deflated = syncFlush ? this.deflater.deflate(s.data, s.limit, 2048 - s.limit, 2) : this.deflater.deflate(s.data, s.limit, 2048 - s.limit);
            if (deflated > 0) {
                s.limit += deflated;
                buffer.size += (long) deflated;
                this.sink.emitCompleteSegments();
            } else if (this.deflater.needsInput()) {
                return;
            }
        }
    }

    @Override
    public void flush() throws IOException {
        deflate(true);
        this.sink.flush();
    }

    @Override
    public void close() throws Throwable {
        if (!this.closed) {
            Throwable thrown = null;
            try {
                this.deflater.finish();
                deflate(false);
            } catch (Throwable e) {
                thrown = e;
            }
            try {
                this.deflater.end();
            } catch (Throwable e2) {
                if (thrown == null) {
                    thrown = e2;
                }
            }
            try {
                this.sink.close();
            } catch (Throwable e3) {
                if (thrown == null) {
                    thrown = e3;
                }
            }
            this.closed = true;
            if (thrown != null) {
                Util.sneakyRethrow(thrown);
            }
        }
    }

    @Override
    public Sink mo2deadline(Deadline deadline) {
        this.sink.mo2deadline(deadline);
        return this;
    }

    public String toString() {
        return "DeflaterSink(" + this.sink + ")";
    }
}
