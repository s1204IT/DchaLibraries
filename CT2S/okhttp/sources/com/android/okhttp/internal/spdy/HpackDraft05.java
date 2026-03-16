package com.android.okhttp.internal.spdy;

import com.android.okhttp.internal.BitArray;
import com.android.okhttp.internal.spdy.Huffman;
import com.android.okio.BufferedSource;
import com.android.okio.ByteString;
import com.android.okio.OkBuffer;
import com.android.okio.Okio;
import com.android.okio.Source;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class HpackDraft05 {
    private static final int PREFIX_6_BITS = 63;
    private static final int PREFIX_7_BITS = 127;
    private static final Header[] STATIC_HEADER_TABLE = {new Header(Header.TARGET_AUTHORITY, ""), new Header(Header.TARGET_METHOD, "GET"), new Header(Header.TARGET_METHOD, "POST"), new Header(Header.TARGET_PATH, "/"), new Header(Header.TARGET_PATH, "/index.html"), new Header(Header.TARGET_SCHEME, "http"), new Header(Header.TARGET_SCHEME, "https"), new Header(Header.RESPONSE_STATUS, "200"), new Header(Header.RESPONSE_STATUS, "500"), new Header(Header.RESPONSE_STATUS, "404"), new Header(Header.RESPONSE_STATUS, "403"), new Header(Header.RESPONSE_STATUS, "400"), new Header(Header.RESPONSE_STATUS, "401"), new Header("accept-charset", ""), new Header("accept-encoding", ""), new Header("accept-language", ""), new Header("accept-ranges", ""), new Header("accept", ""), new Header("access-control-allow-origin", ""), new Header("age", ""), new Header("allow", ""), new Header("authorization", ""), new Header("cache-control", ""), new Header("content-disposition", ""), new Header("content-encoding", ""), new Header("content-language", ""), new Header("content-length", ""), new Header("content-location", ""), new Header("content-range", ""), new Header("content-type", ""), new Header("cookie", ""), new Header("date", ""), new Header("etag", ""), new Header("expect", ""), new Header("expires", ""), new Header("from", ""), new Header("host", ""), new Header("if-match", ""), new Header("if-modified-since", ""), new Header("if-none-match", ""), new Header("if-range", ""), new Header("if-unmodified-since", ""), new Header("last-modified", ""), new Header("link", ""), new Header("location", ""), new Header("max-forwards", ""), new Header("proxy-authenticate", ""), new Header("proxy-authorization", ""), new Header("range", ""), new Header("referer", ""), new Header("refresh", ""), new Header("retry-after", ""), new Header("server", ""), new Header("set-cookie", ""), new Header("strict-transport-security", ""), new Header("transfer-encoding", ""), new Header("user-agent", ""), new Header("vary", ""), new Header("via", ""), new Header("www-authenticate", "")};
    private static final Map<ByteString, Integer> NAME_TO_FIRST_INDEX = nameToFirstIndex();

    private HpackDraft05() {
    }

    static final class Reader {
        private final Huffman.Codec huffmanCodec;
        private int maxHeaderTableByteCount;
        private final BufferedSource source;
        private final List<Header> emittedHeaders = new ArrayList();
        Header[] headerTable = new Header[8];
        int nextHeaderIndex = this.headerTable.length - 1;
        int headerCount = 0;
        BitArray referencedHeaders = new BitArray.FixedCapacity();
        BitArray emittedReferencedHeaders = new BitArray.FixedCapacity();
        int headerTableByteCount = 0;

        Reader(boolean client, int maxHeaderTableByteCount, Source source) {
            this.huffmanCodec = client ? Huffman.Codec.RESPONSE : Huffman.Codec.REQUEST;
            this.maxHeaderTableByteCount = maxHeaderTableByteCount;
            this.source = Okio.buffer(source);
        }

        int maxHeaderTableByteCount() {
            return this.maxHeaderTableByteCount;
        }

        void maxHeaderTableByteCount(int newMaxHeaderTableByteCount) {
            this.maxHeaderTableByteCount = newMaxHeaderTableByteCount;
            if (this.maxHeaderTableByteCount < this.headerTableByteCount) {
                if (this.maxHeaderTableByteCount == 0) {
                    clearHeaderTable();
                } else {
                    evictToRecoverBytes(this.headerTableByteCount - this.maxHeaderTableByteCount);
                }
            }
        }

        private void clearHeaderTable() {
            clearReferenceSet();
            Arrays.fill(this.headerTable, (Object) null);
            this.nextHeaderIndex = this.headerTable.length - 1;
            this.headerCount = 0;
            this.headerTableByteCount = 0;
        }

        private int evictToRecoverBytes(int bytesToRecover) {
            int entriesToEvict = 0;
            if (bytesToRecover > 0) {
                for (int j = this.headerTable.length - 1; j >= this.nextHeaderIndex && bytesToRecover > 0; j--) {
                    bytesToRecover -= this.headerTable[j].hpackSize;
                    this.headerTableByteCount -= this.headerTable[j].hpackSize;
                    this.headerCount--;
                    entriesToEvict++;
                }
                this.referencedHeaders.shiftLeft(entriesToEvict);
                this.emittedReferencedHeaders.shiftLeft(entriesToEvict);
                System.arraycopy(this.headerTable, this.nextHeaderIndex + 1, this.headerTable, this.nextHeaderIndex + 1 + entriesToEvict, this.headerCount);
                this.nextHeaderIndex += entriesToEvict;
            }
            return entriesToEvict;
        }

        void readHeaders() throws IOException {
            while (!this.source.exhausted()) {
                int b = this.source.readByte() & 255;
                if (b == 128) {
                    clearReferenceSet();
                } else if ((b & 128) == 128) {
                    int index = readInt(b, HpackDraft05.PREFIX_7_BITS);
                    readIndexedHeader(index - 1);
                } else if (b == 64) {
                    readLiteralHeaderWithoutIndexingNewName();
                } else if ((b & 64) == 64) {
                    int index2 = readInt(b, HpackDraft05.PREFIX_6_BITS);
                    readLiteralHeaderWithoutIndexingIndexedName(index2 - 1);
                } else if (b == 0) {
                    readLiteralHeaderWithIncrementalIndexingNewName();
                } else if ((b & 192) == 0) {
                    int index3 = readInt(b, HpackDraft05.PREFIX_6_BITS);
                    readLiteralHeaderWithIncrementalIndexingIndexedName(index3 - 1);
                } else {
                    throw new AssertionError("unhandled byte: " + Integer.toBinaryString(b));
                }
            }
        }

        private void clearReferenceSet() {
            this.referencedHeaders.clear();
            this.emittedReferencedHeaders.clear();
        }

        void emitReferenceSet() {
            for (int i = this.headerTable.length - 1; i != this.nextHeaderIndex; i--) {
                if (this.referencedHeaders.get(i) && !this.emittedReferencedHeaders.get(i)) {
                    this.emittedHeaders.add(this.headerTable[i]);
                }
            }
        }

        List<Header> getAndReset() {
            List<Header> result = new ArrayList<>(this.emittedHeaders);
            this.emittedHeaders.clear();
            this.emittedReferencedHeaders.clear();
            return result;
        }

        private void readIndexedHeader(int index) {
            if (isStaticHeader(index)) {
                Header staticEntry = HpackDraft05.STATIC_HEADER_TABLE[index - this.headerCount];
                if (this.maxHeaderTableByteCount == 0) {
                    this.emittedHeaders.add(staticEntry);
                    return;
                } else {
                    insertIntoHeaderTable(-1, staticEntry);
                    return;
                }
            }
            int headerTableIndex = headerTableIndex(index);
            if (!this.referencedHeaders.get(headerTableIndex)) {
                this.emittedHeaders.add(this.headerTable[headerTableIndex]);
                this.emittedReferencedHeaders.set(headerTableIndex);
            }
            this.referencedHeaders.toggle(headerTableIndex);
        }

        private int headerTableIndex(int index) {
            return this.nextHeaderIndex + 1 + index;
        }

        private void readLiteralHeaderWithoutIndexingIndexedName(int index) throws IOException {
            ByteString name = getName(index);
            ByteString value = readByteString(false);
            this.emittedHeaders.add(new Header(name, value));
        }

        private void readLiteralHeaderWithoutIndexingNewName() throws IOException {
            ByteString name = readByteString(true);
            ByteString value = readByteString(false);
            this.emittedHeaders.add(new Header(name, value));
        }

        private void readLiteralHeaderWithIncrementalIndexingIndexedName(int nameIndex) throws IOException {
            ByteString name = getName(nameIndex);
            ByteString value = readByteString(false);
            insertIntoHeaderTable(-1, new Header(name, value));
        }

        private void readLiteralHeaderWithIncrementalIndexingNewName() throws IOException {
            ByteString name = readByteString(true);
            ByteString value = readByteString(false);
            insertIntoHeaderTable(-1, new Header(name, value));
        }

        private ByteString getName(int index) {
            return isStaticHeader(index) ? HpackDraft05.STATIC_HEADER_TABLE[index - this.headerCount].name : this.headerTable[headerTableIndex(index)].name;
        }

        private boolean isStaticHeader(int index) {
            return index >= this.headerCount;
        }

        private void insertIntoHeaderTable(int index, Header entry) {
            int delta = entry.hpackSize;
            if (index != -1) {
                delta -= this.headerTable[headerTableIndex(index)].hpackSize;
            }
            if (delta > this.maxHeaderTableByteCount) {
                clearHeaderTable();
                this.emittedHeaders.add(entry);
                return;
            }
            int bytesToRecover = (this.headerTableByteCount + delta) - this.maxHeaderTableByteCount;
            int entriesEvicted = evictToRecoverBytes(bytesToRecover);
            if (index == -1) {
                if (this.headerCount + 1 > this.headerTable.length) {
                    Header[] doubled = new Header[this.headerTable.length * 2];
                    System.arraycopy(this.headerTable, 0, doubled, this.headerTable.length, this.headerTable.length);
                    if (doubled.length == 64) {
                        this.referencedHeaders = ((BitArray.FixedCapacity) this.referencedHeaders).toVariableCapacity();
                        this.emittedReferencedHeaders = ((BitArray.FixedCapacity) this.emittedReferencedHeaders).toVariableCapacity();
                    }
                    this.referencedHeaders.shiftLeft(this.headerTable.length);
                    this.emittedReferencedHeaders.shiftLeft(this.headerTable.length);
                    this.nextHeaderIndex = this.headerTable.length - 1;
                    this.headerTable = doubled;
                }
                int index2 = this.nextHeaderIndex;
                this.nextHeaderIndex = index2 - 1;
                this.referencedHeaders.set(index2);
                this.headerTable[index2] = entry;
                this.headerCount++;
            } else {
                int index3 = index + headerTableIndex(index) + entriesEvicted;
                this.referencedHeaders.set(index3);
                this.headerTable[index3] = entry;
            }
            this.headerTableByteCount += delta;
        }

        private int readByte() throws IOException {
            return this.source.readByte() & 255;
        }

        int readInt(int firstByte, int prefixMask) throws IOException {
            int prefix = firstByte & prefixMask;
            if (prefix >= prefixMask) {
                int result = prefixMask;
                int shift = 0;
                while (true) {
                    int b = readByte();
                    if ((b & 128) != 0) {
                        result += (b & HpackDraft05.PREFIX_7_BITS) << shift;
                        shift += 7;
                    } else {
                        return result + (b << shift);
                    }
                }
            } else {
                return prefix;
            }
        }

        ByteString readByteString(boolean asciiLowercase) throws IOException {
            int firstByte = readByte();
            boolean huffmanDecode = (firstByte & 128) == 128;
            int length = readInt(firstByte, HpackDraft05.PREFIX_7_BITS);
            ByteString byteString = this.source.readByteString(length);
            if (huffmanDecode) {
                byteString = this.huffmanCodec.decode(byteString);
            }
            if (asciiLowercase) {
                return byteString.toAsciiLowercase();
            }
            return byteString;
        }
    }

    private static Map<ByteString, Integer> nameToFirstIndex() {
        Map<ByteString, Integer> result = new LinkedHashMap<>(STATIC_HEADER_TABLE.length);
        for (int i = 0; i < STATIC_HEADER_TABLE.length; i++) {
            if (!result.containsKey(STATIC_HEADER_TABLE[i].name)) {
                result.put(STATIC_HEADER_TABLE[i].name, Integer.valueOf(i));
            }
        }
        return Collections.unmodifiableMap(result);
    }

    static final class Writer {
        private final OkBuffer out;

        Writer(OkBuffer out) {
            this.out = out;
        }

        void writeHeaders(List<Header> headerBlock) throws IOException {
            int size = headerBlock.size();
            for (int i = 0; i < size; i++) {
                ByteString name = headerBlock.get(i).name;
                Integer staticIndex = (Integer) HpackDraft05.NAME_TO_FIRST_INDEX.get(name);
                if (staticIndex != null) {
                    writeInt(staticIndex.intValue() + 1, HpackDraft05.PREFIX_6_BITS, 64);
                    writeByteString(headerBlock.get(i).value);
                } else {
                    this.out.writeByte(64);
                    writeByteString(name);
                    writeByteString(headerBlock.get(i).value);
                }
            }
        }

        void writeInt(int value, int prefixMask, int bits) throws IOException {
            if (value < prefixMask) {
                this.out.writeByte(bits | value);
                return;
            }
            this.out.writeByte(bits | prefixMask);
            int value2 = value - prefixMask;
            while (value2 >= 128) {
                int b = value2 & HpackDraft05.PREFIX_7_BITS;
                this.out.writeByte(b | 128);
                value2 >>>= 7;
            }
            this.out.writeByte(value2);
        }

        void writeByteString(ByteString data) throws IOException {
            writeInt(data.size(), HpackDraft05.PREFIX_7_BITS, 0);
            this.out.write(data);
        }
    }
}
