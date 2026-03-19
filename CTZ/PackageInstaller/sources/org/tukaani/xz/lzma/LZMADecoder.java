package org.tukaani.xz.lzma;

import java.io.IOException;
import org.tukaani.xz.lz.LZDecoder;
import org.tukaani.xz.lzma.LZMACoder;
import org.tukaani.xz.rangecoder.RangeDecoder;

public final class LZMADecoder extends LZMACoder {
    private final LiteralDecoder literalDecoder;
    private final LZDecoder lz;
    private final LengthDecoder matchLenDecoder;
    private final RangeDecoder rc;
    private final LengthDecoder repLenDecoder;

    public LZMADecoder(LZDecoder lZDecoder, RangeDecoder rangeDecoder, int i, int i2, int i3) {
        super(i3);
        this.matchLenDecoder = new LengthDecoder();
        this.repLenDecoder = new LengthDecoder();
        this.lz = lZDecoder;
        this.rc = rangeDecoder;
        this.literalDecoder = new LiteralDecoder(i, i2);
        reset();
    }

    @Override
    public void reset() {
        super.reset();
        this.literalDecoder.reset();
        this.matchLenDecoder.reset();
        this.repLenDecoder.reset();
    }

    public boolean endMarkerDetected() {
        return this.reps[0] == -1;
    }

    public void decode() throws IOException {
        int iDecodeRepMatch;
        this.lz.repeatPending();
        while (this.lz.hasSpace()) {
            int pos = this.lz.getPos() & this.posMask;
            if (this.rc.decodeBit(this.isMatch[this.state.get()], pos) == 0) {
                this.literalDecoder.decode();
            } else {
                if (this.rc.decodeBit(this.isRep, this.state.get()) == 0) {
                    iDecodeRepMatch = decodeMatch(pos);
                } else {
                    iDecodeRepMatch = decodeRepMatch(pos);
                }
                this.lz.repeat(this.reps[0], iDecodeRepMatch);
            }
        }
        this.rc.normalize();
    }

    private int decodeMatch(int i) throws IOException {
        this.state.updateMatch();
        this.reps[3] = this.reps[2];
        this.reps[2] = this.reps[1];
        this.reps[1] = this.reps[0];
        int iDecode = this.matchLenDecoder.decode(i);
        int iDecodeBitTree = this.rc.decodeBitTree(this.distSlots[getDistState(iDecode)]);
        if (iDecodeBitTree < 4) {
            this.reps[0] = iDecodeBitTree;
        } else {
            int i2 = (iDecodeBitTree >> 1) - 1;
            this.reps[0] = (2 | (iDecodeBitTree & 1)) << i2;
            if (iDecodeBitTree < 14) {
                int[] iArr = this.reps;
                iArr[0] = this.rc.decodeReverseBitTree(this.distSpecial[iDecodeBitTree - 4]) | iArr[0];
            } else {
                int[] iArr2 = this.reps;
                iArr2[0] = (this.rc.decodeDirectBits(i2 - 4) << 4) | iArr2[0];
                int[] iArr3 = this.reps;
                iArr3[0] = iArr3[0] | this.rc.decodeReverseBitTree(this.distAlign);
            }
        }
        return iDecode;
    }

    private int decodeRepMatch(int i) throws IOException {
        int i2;
        if (this.rc.decodeBit(this.isRep0, this.state.get()) == 0) {
            if (this.rc.decodeBit(this.isRep0Long[this.state.get()], i) == 0) {
                this.state.updateShortRep();
                return 1;
            }
        } else {
            if (this.rc.decodeBit(this.isRep1, this.state.get()) == 0) {
                i2 = this.reps[1];
            } else {
                if (this.rc.decodeBit(this.isRep2, this.state.get()) == 0) {
                    i2 = this.reps[2];
                } else {
                    i2 = this.reps[3];
                    this.reps[3] = this.reps[2];
                }
                this.reps[2] = this.reps[1];
            }
            this.reps[1] = this.reps[0];
            this.reps[0] = i2;
        }
        this.state.updateLongRep();
        return this.repLenDecoder.decode(i);
    }

    private class LiteralDecoder extends LZMACoder.LiteralCoder {
        private final LiteralSubdecoder[] subdecoders;

        LiteralDecoder(int i, int i2) {
            super(i, i2);
            this.subdecoders = new LiteralSubdecoder[1 << (i + i2)];
            for (int i3 = 0; i3 < this.subdecoders.length; i3++) {
                this.subdecoders[i3] = new LiteralSubdecoder();
            }
        }

        void reset() {
            for (int i = 0; i < this.subdecoders.length; i++) {
                this.subdecoders[i].reset();
            }
        }

        void decode() throws IOException {
            this.subdecoders[getSubcoderIndex(LZMADecoder.this.lz.getByte(0), LZMADecoder.this.lz.getPos())].decode();
        }

        private class LiteralSubdecoder extends LZMACoder.LiteralCoder.LiteralSubcoder {
            private LiteralSubdecoder() {
                super();
            }

            void decode() throws IOException {
                int iDecodeBit = 1;
                if (!LZMADecoder.this.state.isLiteral()) {
                    int i = LZMADecoder.this.lz.getByte(LZMADecoder.this.reps[0]);
                    int i2 = 256;
                    int i3 = 1;
                    do {
                        i <<= 1;
                        int i4 = i & i2;
                        int iDecodeBit2 = LZMADecoder.this.rc.decodeBit(this.probs, i2 + i4 + i3);
                        i3 = (i3 << 1) | iDecodeBit2;
                        i2 &= (~i4) ^ (0 - iDecodeBit2);
                    } while (i3 < 256);
                    iDecodeBit = i3;
                } else {
                    do {
                        iDecodeBit = LZMADecoder.this.rc.decodeBit(this.probs, iDecodeBit) | (iDecodeBit << 1);
                    } while (iDecodeBit < 256);
                }
                LZMADecoder.this.lz.putByte((byte) iDecodeBit);
                LZMADecoder.this.state.updateLiteral();
            }
        }
    }

    private class LengthDecoder extends LZMACoder.LengthCoder {
        private LengthDecoder() {
            super();
        }

        int decode(int i) throws IOException {
            if (LZMADecoder.this.rc.decodeBit(this.choice, 0) == 0) {
                return LZMADecoder.this.rc.decodeBitTree(this.low[i]) + 2;
            }
            return LZMADecoder.this.rc.decodeBit(this.choice, 1) == 0 ? LZMADecoder.this.rc.decodeBitTree(this.mid[i]) + 2 + 8 : LZMADecoder.this.rc.decodeBitTree(this.high) + 2 + 8 + 8;
        }
    }
}
