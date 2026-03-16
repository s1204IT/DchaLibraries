package com.bumptech.glide.gifdecoder;

import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.MotionEventCompat;
import android.util.Log;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class GifHeaderParser {
    private static final int MAX_STACK_SIZE = 4096;
    public static final String TAG = "GifHeaderParser";
    protected boolean lctFlag;
    protected int lctSize;
    private byte[] pixelStack;
    private short[] prefix;
    private final ByteBuffer rawData;
    private byte[] suffix;
    private GifHeader header = new GifHeader();
    protected byte[] block = new byte[256];
    protected int blockSize = 0;

    public GifHeaderParser(byte[] data) {
        if (data != null) {
            this.rawData = ByteBuffer.wrap(data);
            this.rawData.rewind();
            this.rawData.order(ByteOrder.LITTLE_ENDIAN);
        } else {
            this.rawData = null;
            this.header.status = 2;
        }
    }

    public GifHeader parseHeader() {
        if (err()) {
            return this.header;
        }
        readHeader();
        if (!err()) {
            readContents();
            if (this.header.frameCount < 0) {
                this.header.status = 1;
            }
        }
        return this.header;
    }

    protected void readContents() {
        boolean done = false;
        while (!done && !err()) {
            int code = read();
            switch (code) {
                case 33:
                    int code2 = read();
                    switch (code2) {
                        case 1:
                            skip();
                            break;
                        case 249:
                            this.header.currentFrame = new GifFrame();
                            readGraphicControlExt();
                            break;
                        case 254:
                            skip();
                            break;
                        case MotionEventCompat.ACTION_MASK:
                            readBlock();
                            String app = "";
                            for (int i = 0; i < 11; i++) {
                                app = app + ((char) this.block[i]);
                            }
                            if (app.equals("NETSCAPE2.0")) {
                                readNetscapeExt();
                            } else {
                                skip();
                            }
                            break;
                        default:
                            skip();
                            break;
                    }
                    break;
                case 44:
                    readBitmap();
                    break;
                case 59:
                    done = true;
                    break;
                default:
                    this.header.status = 1;
                    break;
            }
        }
    }

    protected void readGraphicControlExt() {
        read();
        int packed = read();
        this.header.currentFrame.dispose = (packed & 28) >> 2;
        if (this.header.currentFrame.dispose == 0) {
            this.header.currentFrame.dispose = 1;
        }
        this.header.currentFrame.transparency = (packed & 1) != 0;
        this.header.isTransparent |= this.header.currentFrame.transparency;
        this.header.currentFrame.delay = readShort() * 10;
        this.header.currentFrame.transIndex = read();
        read();
    }

    protected void readBitmap() {
        this.header.currentFrame.ix = readShort();
        this.header.currentFrame.iy = readShort();
        this.header.currentFrame.iw = readShort();
        this.header.currentFrame.ih = readShort();
        int packed = read();
        this.lctFlag = (packed & 128) != 0;
        this.lctSize = (int) Math.pow(2.0d, (packed & 7) + 1);
        this.header.currentFrame.interlace = (packed & 64) != 0;
        if (this.lctFlag) {
            this.header.currentFrame.lct = readColorTable(this.lctSize);
        } else {
            this.header.currentFrame.lct = null;
        }
        this.header.currentFrame.bufferFrameStart = this.rawData.position();
        skipBitmapData();
        skip();
        if (!err()) {
            this.header.frameCount++;
            this.header.frames.add(this.header.currentFrame);
        }
    }

    protected void readNetscapeExt() {
        do {
            readBlock();
            if (this.block[0] == 1) {
                int b1 = this.block[1] & 255;
                int b2 = this.block[2] & 255;
                this.header.loopCount = (b2 << 8) | b1;
            }
            if (this.blockSize <= 0) {
                return;
            }
        } while (!err());
    }

    private void readHeader() {
        String id = "";
        for (int i = 0; i < 6; i++) {
            id = id + ((char) read());
        }
        if (!id.startsWith("GIF")) {
            this.header.status = 1;
            return;
        }
        readLSD();
        if (this.header.gctFlag && !err()) {
            this.header.gct = readColorTable(this.header.gctSize);
            this.header.bgColor = this.header.gct[this.header.bgIndex];
        }
    }

    protected void readLSD() {
        this.header.width = readShort();
        this.header.height = readShort();
        int packed = read();
        this.header.gctFlag = (packed & 128) != 0;
        this.header.gctSize = 2 << (packed & 7);
        this.header.bgIndex = read();
        this.header.pixelAspect = read();
    }

    protected int[] readColorTable(int ncolors) {
        int nbytes = ncolors * 3;
        int[] tab = null;
        byte[] c = new byte[nbytes];
        try {
            this.rawData.get(c);
            tab = new int[256];
            int j = 0;
            int i = 0;
            while (i < ncolors) {
                int j2 = j + 1;
                int r = c[j] & 255;
                int j3 = j2 + 1;
                int g = c[j2] & 255;
                int j4 = j3 + 1;
                int b = c[j3] & 255;
                int i2 = i + 1;
                tab[i] = (-16777216) | (r << 16) | (g << 8) | b;
                j = j4;
                i = i2;
            }
        } catch (BufferUnderflowException e) {
            Log.w(TAG, "Format Error Reading Color Table", e);
            this.header.status = 1;
        }
        return tab;
    }

    protected void skipBitmapData() {
        int i;
        int i2 = this.header.width * this.header.height;
        if (this.prefix == null) {
            this.prefix = new short[4096];
        }
        if (this.suffix == null) {
            this.suffix = new byte[4096];
        }
        if (this.pixelStack == null) {
            this.pixelStack = new byte[FragmentTransaction.TRANSIT_FRAGMENT_OPEN];
        }
        int i3 = read();
        int i4 = 1 << i3;
        int i5 = i4 + 1;
        int i6 = i4 + 2;
        byte b = -1;
        int i7 = i3 + 1;
        int i8 = (1 << i7) - 1;
        System.currentTimeMillis();
        for (int i9 = 0; i9 < i4; i9++) {
            this.prefix[i9] = 0;
            this.suffix[i9] = (byte) i9;
        }
        System.currentTimeMillis();
        int i10 = 0;
        int i11 = 0;
        int block = 0;
        int i12 = 0;
        int i13 = 0;
        int i14 = 0;
        int i15 = 0;
        int i16 = 0;
        while (i15 < i2) {
            i14++;
            if (i16 != 0) {
                i = i16;
                i11 = i11;
                b = b;
            } else if (i12 < i7) {
                if (block == 0) {
                    block = readBlock();
                    if (block > 0) {
                        i10 = 0;
                    } else {
                        return;
                    }
                }
                i13 += ((this.block[i10] & 255) == true ? 1 : 0) << i12;
                i12 += 8;
                i10++;
                block--;
            } else {
                int i17 = i13 & i8;
                i13 >>= i7;
                i12 -= i7;
                if (i17 > i6) {
                    break;
                }
                if (i17 != i5) {
                    if (i17 != i4) {
                        if ((b == true ? 1 : 0) == -1) {
                            this.pixelStack[i16] = this.suffix[i17 == true ? 1 : 0];
                            b = i17 == true ? 1 : 0;
                            i11 = i17 == true ? 1 : 0;
                            i16++;
                        } else {
                            short s = i17;
                            if (i17 == i6) {
                                this.pixelStack[i16] = i11 == true ? (byte) 1 : (byte) 0;
                                s = b == true ? 1 : 0;
                                i16++;
                            }
                            while (s > i4) {
                                this.pixelStack[i16] = this.suffix[s];
                                s = this.prefix[s];
                                i16++;
                            }
                            int i18 = this.suffix[s] & 255;
                            if (i6 < 4096) {
                                i = i16 + 1;
                                this.pixelStack[i16] = i18 == true ? (byte) 1 : (byte) 0;
                                this.prefix[i6] = b == true ? 1 : 0 ? (short) 1 : (short) 0;
                                this.suffix[i6] = i18 == true ? (byte) 1 : (byte) 0;
                                i6++;
                                if ((i6 & i8) == 0 && i6 < 4096) {
                                    i7++;
                                    i8 += i6;
                                }
                                b = i17 == true ? 1 : 0;
                                i11 = i18;
                            } else {
                                return;
                            }
                        }
                    } else {
                        i7 = i3 + 1;
                        i8 = (1 << i7) - 1;
                        i6 = i4 + 2;
                        b = -1;
                    }
                } else {
                    return;
                }
            }
            i15++;
            i16 = i - 1;
        }
    }

    protected void skip() {
        do {
            readBlock();
            if (this.blockSize <= 0) {
                return;
            }
        } while (!err());
    }

    protected int readBlock() {
        this.blockSize = read();
        int n = 0;
        if (this.blockSize > 0) {
            while (n < this.blockSize) {
                try {
                    int count = this.blockSize - n;
                    this.rawData.get(this.block, n, count);
                    n += count;
                } catch (Exception e) {
                    Log.w(TAG, "Error Reading Block", e);
                    this.header.status = 1;
                }
            }
        }
        return n;
    }

    private int read() {
        try {
            int curByte = this.rawData.get() & 255;
            return curByte;
        } catch (Exception e) {
            this.header.status = 1;
            return 0;
        }
    }

    protected int readShort() {
        return this.rawData.getShort();
    }

    private boolean err() {
        return this.header.status != 0;
    }
}
