package android.util.jar;

import android.util.jar.StrictJarManifest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Attributes;

class StrictJarManifestReader {
    private final byte[] buf;
    private final int endOfMainSection;
    private Attributes.Name name;
    private int pos;
    private String value;
    private final HashMap<String, Attributes.Name> attributeNameCache = new HashMap<>();
    private final ByteArrayOutputStream valueBuffer = new ByteArrayOutputStream(80);
    private int consecutiveLineBreaks = 0;

    public StrictJarManifestReader(byte[] bArr, Attributes attributes) throws IOException {
        this.buf = bArr;
        while (readHeader()) {
            attributes.put(this.name, this.value);
        }
        this.endOfMainSection = this.pos;
    }

    public void readEntries(Map<String, Attributes> map, Map<String, StrictJarManifest.Chunk> map2) throws IOException {
        int i = this.pos;
        while (readHeader()) {
            if (!Attributes.Name.NAME.equals(this.name)) {
                throw new IOException("Entry is not named");
            }
            String str = this.value;
            Attributes attributes = map.get(str);
            if (attributes == null) {
                attributes = new Attributes(12);
            }
            while (readHeader()) {
                attributes.put(this.name, this.value);
            }
            if (map2 != null) {
                if (map2.get(str) != null) {
                    throw new IOException("A jar verifier does not support more than one entry with the same name");
                }
                map2.put(str, new StrictJarManifest.Chunk(i, this.pos));
                i = this.pos;
            }
            map.put(str, attributes);
        }
    }

    public int getEndOfMainSection() {
        return this.endOfMainSection;
    }

    private boolean readHeader() throws IOException {
        if (this.consecutiveLineBreaks > 1) {
            this.consecutiveLineBreaks = 0;
            return false;
        }
        readName();
        this.consecutiveLineBreaks = 0;
        readValue();
        return this.consecutiveLineBreaks > 0;
    }

    private void readName() throws IOException {
        int i = this.pos;
        while (this.pos < this.buf.length) {
            byte[] bArr = this.buf;
            int i2 = this.pos;
            this.pos = i2 + 1;
            if (bArr[i2] == 58) {
                String str = new String(this.buf, i, (this.pos - i) - 1, StandardCharsets.US_ASCII);
                byte[] bArr2 = this.buf;
                int i3 = this.pos;
                this.pos = i3 + 1;
                if (bArr2[i3] != 32) {
                    throw new IOException(String.format("Invalid value for attribute '%s'", str));
                }
                try {
                    this.name = this.attributeNameCache.get(str);
                    if (this.name == null) {
                        this.name = new Attributes.Name(str);
                        this.attributeNameCache.put(str, this.name);
                        return;
                    }
                    return;
                } catch (IllegalArgumentException e) {
                    throw new IOException(e.getMessage());
                }
            }
        }
    }

    private void readValue() throws IOException {
        int i = this.pos;
        int i2 = this.pos;
        this.valueBuffer.reset();
        int i3 = i2;
        int i4 = i;
        loop0: while (true) {
            boolean z = false;
            while (true) {
                if (this.pos >= this.buf.length) {
                    break loop0;
                }
                byte[] bArr = this.buf;
                int i5 = this.pos;
                this.pos = i5 + 1;
                byte b = bArr[i5];
                if (b == 0) {
                    throw new IOException("NUL character in a manifest");
                }
                if (b != 10) {
                    if (b == 13) {
                        this.consecutiveLineBreaks++;
                        z = true;
                    } else if (b == 32 && this.consecutiveLineBreaks == 1) {
                        this.valueBuffer.write(this.buf, i4, i3 - i4);
                        i4 = this.pos;
                        this.consecutiveLineBreaks = 0;
                    } else {
                        if (this.consecutiveLineBreaks >= 1) {
                            this.pos--;
                            break loop0;
                        }
                        i3 = this.pos;
                    }
                } else if (z) {
                    break;
                } else {
                    this.consecutiveLineBreaks++;
                }
            }
        }
        this.valueBuffer.write(this.buf, i4, i3 - i4);
        this.value = this.valueBuffer.toString(StandardCharsets.UTF_8.name());
    }
}
