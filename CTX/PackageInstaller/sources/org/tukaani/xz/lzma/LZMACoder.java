package org.tukaani.xz.lzma;

import java.lang.reflect.Array;
import org.tukaani.xz.rangecoder.RangeCoder;

abstract class LZMACoder {
    final int posMask;
    final int[] reps = new int[4];
    final State state = new State();
    final short[][] isMatch = (short[][]) Array.newInstance((Class<?>) short.class, 12, 16);
    final short[] isRep = new short[12];
    final short[] isRep0 = new short[12];
    final short[] isRep1 = new short[12];
    final short[] isRep2 = new short[12];
    final short[][] isRep0Long = (short[][]) Array.newInstance((Class<?>) short.class, 12, 16);
    final short[][] distSlots = (short[][]) Array.newInstance((Class<?>) short.class, 4, 64);
    final short[][] distSpecial = {new short[2], new short[2], new short[4], new short[4], new short[8], new short[8], new short[16], new short[16], new short[32], new short[32]};
    final short[] distAlign = new short[16];

    static final int getDistState(int i) {
        if (i < 6) {
            return i - 2;
        }
        return 3;
    }

    LZMACoder(int i) {
        this.posMask = (1 << i) - 1;
    }

    void reset() {
        this.reps[0] = 0;
        this.reps[1] = 0;
        this.reps[2] = 0;
        this.reps[3] = 0;
        this.state.reset();
        for (int i = 0; i < this.isMatch.length; i++) {
            RangeCoder.initProbs(this.isMatch[i]);
        }
        RangeCoder.initProbs(this.isRep);
        RangeCoder.initProbs(this.isRep0);
        RangeCoder.initProbs(this.isRep1);
        RangeCoder.initProbs(this.isRep2);
        for (int i2 = 0; i2 < this.isRep0Long.length; i2++) {
            RangeCoder.initProbs(this.isRep0Long[i2]);
        }
        for (int i3 = 0; i3 < this.distSlots.length; i3++) {
            RangeCoder.initProbs(this.distSlots[i3]);
        }
        for (int i4 = 0; i4 < this.distSpecial.length; i4++) {
            RangeCoder.initProbs(this.distSpecial[i4]);
        }
        RangeCoder.initProbs(this.distAlign);
    }

    abstract class LiteralCoder {
        private final int lc;
        private final int literalPosMask;

        LiteralCoder(int i, int i2) {
            this.lc = i;
            this.literalPosMask = (1 << i2) - 1;
        }

        final int getSubcoderIndex(int i, int i2) {
            return (i >> (8 - this.lc)) + ((i2 & this.literalPosMask) << this.lc);
        }

        abstract class LiteralSubcoder {
            final short[] probs = new short[768];

            LiteralSubcoder() {
            }

            void reset() {
                RangeCoder.initProbs(this.probs);
            }
        }
    }

    abstract class LengthCoder {
        final short[] choice = new short[2];
        final short[][] low = (short[][]) Array.newInstance((Class<?>) short.class, 16, 8);
        final short[][] mid = (short[][]) Array.newInstance((Class<?>) short.class, 16, 8);
        final short[] high = new short[256];

        LengthCoder() {
        }

        void reset() {
            RangeCoder.initProbs(this.choice);
            for (int i = 0; i < this.low.length; i++) {
                RangeCoder.initProbs(this.low[i]);
            }
            for (int i2 = 0; i2 < this.low.length; i2++) {
                RangeCoder.initProbs(this.mid[i2]);
            }
            RangeCoder.initProbs(this.high);
        }
    }
}
