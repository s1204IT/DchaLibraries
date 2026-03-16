package com.coremedia.iso.boxes;

import com.coremedia.iso.IsoTypeReader;
import com.coremedia.iso.IsoTypeWriter;
import com.googlecode.mp4parser.AbstractFullBox;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class ProgressiveDownloadInformationBox extends AbstractFullBox {
    List<Entry> entries;

    public ProgressiveDownloadInformationBox() {
        super("pdin");
        this.entries = Collections.emptyList();
    }

    @Override
    protected long getContentSize() {
        return (this.entries.size() * 8) + 4;
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        writeVersionAndFlags(byteBuffer);
        for (Entry entry : this.entries) {
            IsoTypeWriter.writeUInt32(byteBuffer, entry.getRate());
            IsoTypeWriter.writeUInt32(byteBuffer, entry.getInitialDelay());
        }
    }

    @Override
    public void _parseDetails(ByteBuffer content) {
        parseVersionAndFlags(content);
        this.entries = new LinkedList();
        while (content.remaining() >= 8) {
            Entry entry = new Entry(IsoTypeReader.readUInt32(content), IsoTypeReader.readUInt32(content));
            this.entries.add(entry);
        }
    }

    public static class Entry {
        long initialDelay;
        long rate;

        public Entry(long rate, long initialDelay) {
            this.rate = rate;
            this.initialDelay = initialDelay;
        }

        public long getRate() {
            return this.rate;
        }

        public long getInitialDelay() {
            return this.initialDelay;
        }

        public String toString() {
            return "Entry{rate=" + this.rate + ", initialDelay=" + this.initialDelay + '}';
        }
    }

    public String toString() {
        return "ProgressiveDownloadInfoBox{entries=" + this.entries + '}';
    }
}
