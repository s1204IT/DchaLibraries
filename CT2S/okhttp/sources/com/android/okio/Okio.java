package com.android.okio;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class Okio {
    private Okio() {
    }

    public static BufferedSource buffer(Source source) {
        return new RealBufferedSource(source);
    }

    public static BufferedSink buffer(Sink sink) {
        return new RealBufferedSink(sink);
    }

    public static void copy(OkBuffer source, long offset, long byteCount, OutputStream sink) throws IOException {
        Util.checkOffsetAndCount(source.size, offset, byteCount);
        Segment s = source.head;
        while (offset >= s.limit - s.pos) {
            offset -= (long) (s.limit - s.pos);
            s = s.next;
        }
        while (byteCount > 0) {
            int pos = (int) (((long) s.pos) + offset);
            int toWrite = (int) Math.min(s.limit - pos, byteCount);
            sink.write(s.data, pos, toWrite);
            byteCount -= (long) toWrite;
            offset = 0;
        }
    }

    public static Sink sink(final OutputStream out) {
        return new Sink() {
            private Deadline deadline = Deadline.NONE;

            @Override
            public void write(OkBuffer source, long byteCount) throws IOException {
                Util.checkOffsetAndCount(source.size, 0L, byteCount);
                while (byteCount > 0) {
                    this.deadline.throwIfReached();
                    Segment head = source.head;
                    int toCopy = (int) Math.min(byteCount, head.limit - head.pos);
                    out.write(head.data, head.pos, toCopy);
                    head.pos += toCopy;
                    byteCount -= (long) toCopy;
                    source.size -= (long) toCopy;
                    if (head.pos == head.limit) {
                        source.head = head.pop();
                        SegmentPool.INSTANCE.recycle(head);
                    }
                }
            }

            @Override
            public void flush() throws IOException {
                out.flush();
            }

            @Override
            public void close() throws IOException {
                out.close();
            }

            @Override
            public Sink mo2deadline(Deadline deadline) {
                if (deadline == null) {
                    throw new IllegalArgumentException("deadline == null");
                }
                this.deadline = deadline;
                return this;
            }

            public String toString() {
                return "sink(" + out + ")";
            }
        };
    }

    public static Source source(final InputStream in) {
        return new Source() {
            private Deadline deadline = Deadline.NONE;

            @Override
            public long read(OkBuffer sink, long byteCount) throws IOException {
                if (byteCount < 0) {
                    throw new IllegalArgumentException("byteCount < 0: " + byteCount);
                }
                this.deadline.throwIfReached();
                Segment tail = sink.writableSegment(1);
                int maxToCopy = (int) Math.min(byteCount, 2048 - tail.limit);
                int bytesRead = in.read(tail.data, tail.limit, maxToCopy);
                if (bytesRead == -1) {
                    return -1L;
                }
                tail.limit += bytesRead;
                sink.size += (long) bytesRead;
                return bytesRead;
            }

            @Override
            public void close() throws IOException {
                in.close();
            }

            @Override
            public Source mo2deadline(Deadline deadline) {
                if (deadline == null) {
                    throw new IllegalArgumentException("deadline == null");
                }
                this.deadline = deadline;
                return this;
            }

            public String toString() {
                return "source(" + in + ")";
            }
        };
    }
}
