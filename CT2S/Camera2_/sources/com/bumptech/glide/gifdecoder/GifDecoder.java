package com.bumptech.glide.gifdecoder;

import android.graphics.Bitmap;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.MotionEventCompat;
import android.util.Log;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class GifDecoder {
    private static final int DISPOSAL_BACKGROUND = 2;
    private static final int DISPOSAL_NONE = 1;
    private static final int DISPOSAL_PREVIOUS = 3;
    private static final int DISPOSAL_UNSPECIFIED = 0;
    private static final int MAX_STACK_SIZE = 4096;
    public static final int STATUS_FORMAT_ERROR = 1;
    public static final int STATUS_OK = 0;
    public static final int STATUS_OPEN_ERROR = 2;
    private static final String TAG = GifDecoder.class.getSimpleName();
    private int[] act;
    private BitmapProvider bitmapProvider;
    private byte[] data;
    private String id;
    private byte[] mainPixels;
    private int[] mainScratch;
    private byte[] pixelStack;
    private short[] prefix;
    private ByteBuffer rawData;
    private byte[] suffix;
    private byte[] block = new byte[256];
    private int framePointer = -1;
    private GifHeader header = new GifHeader();

    public interface BitmapProvider {
        Bitmap obtain(int i, int i2, Bitmap.Config config);
    }

    public GifDecoder(BitmapProvider provider) {
        this.bitmapProvider = provider;
    }

    public int getWidth() {
        return this.header.width;
    }

    public int getHeight() {
        return this.header.height;
    }

    public boolean isTransparent() {
        return this.header.isTransparent;
    }

    public int getGifByteSize() {
        return this.data.length;
    }

    public byte[] getData() {
        return this.data;
    }

    public int getDecodedFramesByteSizeSum() {
        return (this.header.isTransparent ? 4 : 2) * this.header.height * this.header.frameCount * this.header.width;
    }

    public void advance() {
        this.framePointer = (this.framePointer + 1) % this.header.frameCount;
    }

    public int getDelay(int n) {
        if (n < 0 || n >= this.header.frameCount) {
            return -1;
        }
        int delay = this.header.frames.get(n).delay;
        return delay;
    }

    public int getNextDelay() {
        if (this.header.frameCount <= 0 || this.framePointer < 0) {
            return -1;
        }
        return getDelay(this.framePointer);
    }

    public int getFrameCount() {
        return this.header.frameCount;
    }

    public int getCurrentFrameIndex() {
        return this.framePointer;
    }

    public int getLoopCount() {
        return this.header.loopCount;
    }

    public String getId() {
        return this.id;
    }

    public Bitmap getNextFrame() {
        Bitmap pixels = null;
        if (this.header.frameCount > 0 && this.framePointer >= 0) {
            GifFrame frame = this.header.frames.get(this.framePointer);
            if (frame.lct == null) {
                this.act = this.header.gct;
            } else {
                this.act = frame.lct;
                if (this.header.bgIndex == frame.transIndex) {
                    this.header.bgColor = 0;
                }
            }
            int save = 0;
            if (frame.transparency) {
                save = this.act[frame.transIndex];
                this.act[frame.transIndex] = 0;
            }
            if (this.act == null) {
                Log.w(TAG, "No Valid Color Table");
                this.header.status = 1;
            } else {
                pixels = setPixels(this.framePointer);
                if (frame.transparency) {
                    this.act[frame.transIndex] = save;
                }
            }
        }
        return pixels;
    }

    public int read(InputStream is, int contentLength) {
        if (is != null) {
            int capacity = contentLength > 0 ? contentLength + 4096 : 16384;
            try {
                ByteArrayOutputStream buffer = new ByteArrayOutputStream(capacity);
                byte[] data = new byte[16384];
                while (true) {
                    int nRead = is.read(data, 0, data.length);
                    if (nRead == -1) {
                        break;
                    }
                    buffer.write(data, 0, nRead);
                }
                buffer.flush();
                read(buffer.toByteArray());
            } catch (IOException e) {
                Log.w(TAG, "Error reading data from stream", e);
            }
        } else {
            this.header.status = 2;
        }
        if (is != null) {
            try {
                is.close();
            } catch (IOException e2) {
                Log.w(TAG, "Error closing stream", e2);
            }
        }
        return this.header.status;
    }

    public void setData(String id, GifHeader header, byte[] data) {
        this.id = id;
        this.header = header;
        this.data = data;
        this.rawData = ByteBuffer.wrap(data);
        this.rawData.rewind();
        this.rawData.order(ByteOrder.LITTLE_ENDIAN);
        this.mainPixels = new byte[header.width * header.height];
        this.mainScratch = new int[header.width * header.height];
    }

    public int read(byte[] data) {
        this.data = data;
        this.header = new GifHeaderParser(data).parseHeader();
        if (data != null) {
            this.rawData = ByteBuffer.wrap(data);
            this.rawData.rewind();
            this.rawData.order(ByteOrder.LITTLE_ENDIAN);
            this.mainPixels = new byte[this.header.width * this.header.height];
            this.mainScratch = new int[this.header.width * this.header.height];
        }
        return this.header.status;
    }

    private Bitmap setPixels(int frameIndex) {
        GifFrame currentFrame = this.header.frames.get(frameIndex);
        GifFrame previousFrame = null;
        int previousIndex = frameIndex - 1;
        if (previousIndex >= 0) {
            GifFrame previousFrame2 = this.header.frames.get(previousIndex);
            previousFrame = previousFrame2;
        }
        int[] dest = this.mainScratch;
        if (previousFrame != null && previousFrame.dispose > 0) {
            if (previousFrame.dispose == 2) {
                int c = 0;
                if (!currentFrame.transparency) {
                    c = this.header.bgColor;
                }
                for (int i = 0; i < previousFrame.ih; i++) {
                    int n1 = ((previousFrame.iy + i) * this.header.width) + previousFrame.ix;
                    int n2 = n1 + previousFrame.iw;
                    for (int k = n1; k < n2; k++) {
                        dest[k] = c;
                    }
                }
            }
        } else {
            int c2 = 0;
            if (!currentFrame.transparency) {
                c2 = this.header.bgColor;
            }
            for (int i2 = 0; i2 < dest.length; i2++) {
                dest[i2] = c2;
            }
        }
        decodeBitmapData(currentFrame, this.mainPixels);
        int pass = 1;
        int inc = 8;
        int iline = 0;
        for (int i3 = 0; i3 < currentFrame.ih; i3++) {
            int line = i3;
            if (currentFrame.interlace) {
                if (iline >= currentFrame.ih) {
                    pass++;
                    switch (pass) {
                        case 2:
                            iline = 4;
                            break;
                        case 3:
                            iline = 2;
                            inc = 4;
                            break;
                        case 4:
                            iline = 1;
                            inc = 2;
                            break;
                    }
                }
                line = iline;
                iline += inc;
            }
            int line2 = line + currentFrame.iy;
            if (line2 < this.header.height) {
                int k2 = line2 * this.header.width;
                int dx = k2 + currentFrame.ix;
                int dlim = dx + currentFrame.iw;
                if (this.header.width + k2 < dlim) {
                    dlim = k2 + this.header.width;
                }
                int sx = i3 * currentFrame.iw;
                int sx2 = sx;
                while (dx < dlim) {
                    int sx3 = sx2 + 1;
                    int index = this.mainPixels[sx2] & MotionEventCompat.ACTION_MASK;
                    int c3 = this.act[index];
                    if (c3 != 0) {
                        dest[dx] = c3;
                    }
                    dx++;
                    sx2 = sx3;
                }
            }
        }
        Bitmap result = getNextBitmap();
        result.setPixels(dest, 0, this.header.width, 0, 0, this.header.width, this.header.height);
        return result;
    }

    private void decodeBitmapData(GifFrame gifFrame, byte[] bArr) {
        int i;
        if (gifFrame != null) {
            this.rawData.position(gifFrame.bufferFrameStart);
        }
        int i2 = gifFrame == null ? this.header.width * this.header.height : gifFrame.iw * gifFrame.ih;
        if (bArr == null || bArr.length < i2) {
            bArr = new byte[i2];
        }
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
        for (int i9 = 0; i9 < i4; i9++) {
            this.prefix[i9] = 0;
            this.suffix[i9] = (byte) i9;
        }
        int i10 = 0;
        int i11 = 0;
        int block = 0;
        int i12 = 0;
        int i13 = 0;
        int i14 = 0;
        int i15 = 0;
        int i16 = 0;
        while (i14 < i2) {
            if (i16 != 0) {
                i = i16;
                i11 = i11;
                b = b;
            } else if (i12 < i7) {
                if (block == 0) {
                    block = readBlock();
                    if (block <= 0) {
                        break;
                    } else {
                        i10 = 0;
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
                if (i17 == i5) {
                    break;
                }
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
                        if (i6 >= 4096) {
                            break;
                        }
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
                    }
                } else {
                    i7 = i3 + 1;
                    i8 = (1 << i7) - 1;
                    i6 = i4 + 2;
                    b = -1;
                }
            }
            int i19 = i - 1;
            bArr[i15] = this.pixelStack[i19];
            i14++;
            i15++;
            i16 = i19;
        }
        for (int i20 = i15; i20 < i2; i20++) {
            bArr[i20] = 0;
        }
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

    private int readBlock() {
        int blockSize = read();
        int n = 0;
        if (blockSize > 0) {
            while (n < blockSize) {
                int count = blockSize - n;
                try {
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

    private Bitmap getNextBitmap() {
        Bitmap.Config targetConfig = this.header.isTransparent ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565;
        Bitmap result = this.bitmapProvider.obtain(this.header.width, this.header.height, targetConfig);
        if (result == null) {
            return Bitmap.createBitmap(this.header.width, this.header.height, targetConfig);
        }
        result.eraseColor(0);
        return result;
    }
}
