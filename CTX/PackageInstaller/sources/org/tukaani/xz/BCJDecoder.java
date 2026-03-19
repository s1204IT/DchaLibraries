package org.tukaani.xz;

import java.io.InputStream;
import org.tukaani.xz.simple.ARM;
import org.tukaani.xz.simple.ARMThumb;
import org.tukaani.xz.simple.IA64;
import org.tukaani.xz.simple.PowerPC;
import org.tukaani.xz.simple.SPARC;
import org.tukaani.xz.simple.SimpleFilter;
import org.tukaani.xz.simple.X86;

class BCJDecoder extends BCJCoder implements FilterDecoder {
    static final boolean $assertionsDisabled = false;
    private final long filterID;
    private final int startOffset;

    BCJDecoder(long j, byte[] bArr) throws UnsupportedOptionsException {
        this.filterID = j;
        if (bArr.length == 0) {
            this.startOffset = 0;
            return;
        }
        if (bArr.length == 4) {
            int i = 0;
            for (int i2 = 0; i2 < 4; i2++) {
                i |= (bArr[i2] & 255) << (i2 * 8);
            }
            this.startOffset = i;
            return;
        }
        throw new UnsupportedOptionsException("Unsupported BCJ filter properties");
    }

    @Override
    public int getMemoryUsage() {
        return SimpleInputStream.getMemoryUsage();
    }

    @Override
    public InputStream getInputStream(InputStream inputStream) {
        SimpleFilter sparc;
        if (this.filterID == 4) {
            sparc = new X86(false, this.startOffset);
        } else if (this.filterID == 5) {
            sparc = new PowerPC(false, this.startOffset);
        } else if (this.filterID == 6) {
            sparc = new IA64(false, this.startOffset);
        } else if (this.filterID == 7) {
            sparc = new ARM(false, this.startOffset);
        } else if (this.filterID == 8) {
            sparc = new ARMThumb(false, this.startOffset);
        } else if (this.filterID == 9) {
            sparc = new SPARC(false, this.startOffset);
        } else {
            sparc = null;
        }
        return new SimpleInputStream(inputStream, sparc);
    }
}
