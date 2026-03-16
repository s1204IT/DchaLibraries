package com.android.dex;

import com.android.dex.Dex;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.SocketOptions;
import java.util.Arrays;

public final class TableOfContents {
    public int checksum;
    public int dataOff;
    public int dataSize;
    public int fileSize;
    public int linkOff;
    public int linkSize;
    public final Section header = new Section(0);
    public final Section stringIds = new Section(1);
    public final Section typeIds = new Section(2);
    public final Section protoIds = new Section(3);
    public final Section fieldIds = new Section(4);
    public final Section methodIds = new Section(5);
    public final Section classDefs = new Section(6);
    public final Section mapList = new Section(4096);
    public final Section typeLists = new Section(SocketOptions.SO_SNDBUF);
    public final Section annotationSetRefLists = new Section(SocketOptions.SO_RCVBUF);
    public final Section annotationSets = new Section(SocketOptions.SO_OOBINLINE);
    public final Section classDatas = new Section(8192);
    public final Section codes = new Section(8193);
    public final Section stringDatas = new Section(8194);
    public final Section debugInfos = new Section(8195);
    public final Section annotations = new Section(8196);
    public final Section encodedArrays = new Section(8197);
    public final Section annotationsDirectories = new Section(8198);
    public final Section[] sections = {this.header, this.stringIds, this.typeIds, this.protoIds, this.fieldIds, this.methodIds, this.classDefs, this.mapList, this.typeLists, this.annotationSetRefLists, this.annotationSets, this.classDatas, this.codes, this.stringDatas, this.debugInfos, this.annotations, this.encodedArrays, this.annotationsDirectories};
    public byte[] signature = new byte[20];

    public void readFrom(Dex dex) throws IOException {
        readHeader(dex.open(0));
        readMap(dex.open(this.mapList.off));
        computeSizesFromOffsets();
    }

    private void readHeader(Dex.Section headerIn) throws UnsupportedEncodingException {
        byte[] magic = headerIn.readByteArray(8);
        int apiTarget = DexFormat.magicToApi(magic);
        if (apiTarget != 13) {
            throw new DexException("Unexpected magic: " + Arrays.toString(magic));
        }
        this.checksum = headerIn.readInt();
        this.signature = headerIn.readByteArray(20);
        this.fileSize = headerIn.readInt();
        int headerSize = headerIn.readInt();
        if (headerSize != 112) {
            throw new DexException("Unexpected header: 0x" + Integer.toHexString(headerSize));
        }
        int endianTag = headerIn.readInt();
        if (endianTag != 305419896) {
            throw new DexException("Unexpected endian tag: 0x" + Integer.toHexString(endianTag));
        }
        this.linkSize = headerIn.readInt();
        this.linkOff = headerIn.readInt();
        this.mapList.off = headerIn.readInt();
        if (this.mapList.off == 0) {
            throw new DexException("Cannot merge dex files that do not contain a map");
        }
        this.stringIds.size = headerIn.readInt();
        this.stringIds.off = headerIn.readInt();
        this.typeIds.size = headerIn.readInt();
        this.typeIds.off = headerIn.readInt();
        this.protoIds.size = headerIn.readInt();
        this.protoIds.off = headerIn.readInt();
        this.fieldIds.size = headerIn.readInt();
        this.fieldIds.off = headerIn.readInt();
        this.methodIds.size = headerIn.readInt();
        this.methodIds.off = headerIn.readInt();
        this.classDefs.size = headerIn.readInt();
        this.classDefs.off = headerIn.readInt();
        this.dataSize = headerIn.readInt();
        this.dataOff = headerIn.readInt();
    }

    private void readMap(Dex.Section in) throws IOException {
        int mapSize = in.readInt();
        Section previous = null;
        for (int i = 0; i < mapSize; i++) {
            short type = in.readShort();
            in.readShort();
            Section section = getSection(type);
            int size = in.readInt();
            int offset = in.readInt();
            if ((section.size != 0 && section.size != size) || (section.off != -1 && section.off != offset)) {
                throw new DexException("Unexpected map value for 0x" + Integer.toHexString(type));
            }
            section.size = size;
            section.off = offset;
            if (previous != null && previous.off > section.off) {
                throw new DexException("Map is unsorted at " + previous + ", " + section);
            }
            previous = section;
        }
        Arrays.sort(this.sections);
    }

    public void computeSizesFromOffsets() {
        int end = this.dataOff + this.dataSize;
        for (int i = this.sections.length - 1; i >= 0; i--) {
            Section section = this.sections[i];
            if (section.off != -1) {
                if (section.off > end) {
                    throw new DexException("Map is unsorted at " + section);
                }
                section.byteCount = end - section.off;
                end = section.off;
            }
        }
    }

    private Section getSection(short type) {
        Section[] arr$ = this.sections;
        for (Section section : arr$) {
            if (section.type == type) {
                return section;
            }
        }
        throw new IllegalArgumentException("No such map item: " + ((int) type));
    }

    public void writeHeader(Dex.Section out) throws IOException {
        out.write(DexFormat.apiToMagic(13).getBytes("UTF-8"));
        out.writeInt(this.checksum);
        out.write(this.signature);
        out.writeInt(this.fileSize);
        out.writeInt(112);
        out.writeInt(DexFormat.ENDIAN_TAG);
        out.writeInt(this.linkSize);
        out.writeInt(this.linkOff);
        out.writeInt(this.mapList.off);
        out.writeInt(this.stringIds.size);
        out.writeInt(this.stringIds.off);
        out.writeInt(this.typeIds.size);
        out.writeInt(this.typeIds.off);
        out.writeInt(this.protoIds.size);
        out.writeInt(this.protoIds.off);
        out.writeInt(this.fieldIds.size);
        out.writeInt(this.fieldIds.off);
        out.writeInt(this.methodIds.size);
        out.writeInt(this.methodIds.off);
        out.writeInt(this.classDefs.size);
        out.writeInt(this.classDefs.off);
        out.writeInt(this.dataSize);
        out.writeInt(this.dataOff);
    }

    public void writeMap(Dex.Section out) throws IOException {
        int count = 0;
        for (Section section : this.sections) {
            if (section.exists()) {
                count++;
            }
        }
        out.writeInt(count);
        Section[] arr$ = this.sections;
        for (Section section2 : arr$) {
            if (section2.exists()) {
                out.writeShort(section2.type);
                out.writeShort((short) 0);
                out.writeInt(section2.size);
                out.writeInt(section2.off);
            }
        }
    }

    public static class Section implements Comparable<Section> {
        public final short type;
        public int size = 0;
        public int off = -1;
        public int byteCount = 0;

        public Section(int type) {
            this.type = (short) type;
        }

        public boolean exists() {
            return this.size > 0;
        }

        @Override
        public int compareTo(Section section) {
            if (this.off != section.off) {
                return this.off < section.off ? -1 : 1;
            }
            return 0;
        }

        public String toString() {
            return String.format("Section[type=%#x,off=%#x,size=%#x]", Short.valueOf(this.type), Integer.valueOf(this.off), Integer.valueOf(this.size));
        }
    }
}
