package com.bumptech.glide.gifdecoder;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.os.Build;
import android.util.Log;
import com.mediatek.gallerybasic.base.Generator;
import com.mediatek.gallerybasic.util.Utils;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Iterator;

public class GifDecoder {
    private int[] act;
    private BitmapProvider bitmapProvider;
    private byte[] data;
    private int framePointer;
    private byte[] mainPixels;
    private int[] mainScratch;
    private byte[] pixelStack;
    private short[] prefix;
    private Bitmap previousImage;
    private ByteBuffer rawData;
    private boolean savePrevious;
    private int status;
    private byte[] suffix;
    private static final String TAG = GifDecoder.class.getSimpleName();
    private static final Bitmap.Config BITMAP_CONFIG = Bitmap.Config.ARGB_8888;
    private final byte[] block = new byte[256];
    private GifHeader header = new GifHeader();

    public interface BitmapProvider {
        Bitmap obtain(int i, int i2, Bitmap.Config config);

        void release(Bitmap bitmap);
    }

    public GifDecoder(BitmapProvider bitmapProvider) {
        this.bitmapProvider = bitmapProvider;
    }

    public int getWidth() {
        return this.header.width;
    }

    public int getHeight() {
        return this.header.height;
    }

    public void advance() {
        if (this.header.frameCount >= 1) {
            this.framePointer = (this.framePointer + 1) % this.header.frameCount;
        }
    }

    public int getDelay(int i) {
        if (i >= 0 && i < this.header.frameCount) {
            return this.header.frames.get(i).delay;
        }
        return -1;
    }

    public int getFrameCount() {
        return this.header.frameCount;
    }

    public synchronized Bitmap getNextFrame() {
        GifFrame gifFrame;
        if (this.header.frameCount <= 0 || this.framePointer < 0) {
            if (Log.isLoggable(TAG, 3)) {
                Log.d(TAG, "unable to decode frame, frameCount=" + this.header.frameCount + " framePointer=" + this.framePointer);
            }
            this.status = 1;
        }
        if (this.status != 1 && this.status != 2) {
            int i = 0;
            this.status = 0;
            GifFrame gifFrame2 = this.header.frames.get(this.framePointer);
            int i2 = this.framePointer - 1;
            if (i2 >= 0) {
                gifFrame = this.header.frames.get(i2);
            } else {
                gifFrame = null;
            }
            if (gifFrame2.lct == null) {
                this.act = this.header.gct;
            } else {
                this.act = gifFrame2.lct;
                if (this.header.bgIndex == gifFrame2.transIndex) {
                    this.header.bgColor = 0;
                }
            }
            if (gifFrame2.transparency) {
                int i3 = this.act[gifFrame2.transIndex];
                this.act[gifFrame2.transIndex] = 0;
                i = i3;
            }
            if (this.act == null) {
                if (Log.isLoggable(TAG, 3)) {
                    Log.d(TAG, "No Valid Color Table");
                }
                this.status = 1;
                return null;
            }
            Bitmap pixels = setPixels(gifFrame2, gifFrame);
            if (gifFrame2.transparency) {
                this.act[gifFrame2.transIndex] = i;
            }
            return pixels;
        }
        if (Log.isLoggable(TAG, 3)) {
            Log.d(TAG, "Unable to decode frame, status=" + this.status);
        }
        return null;
    }

    public void clear() {
        this.header = null;
        this.data = null;
        this.mainPixels = null;
        this.mainScratch = null;
        if (this.previousImage != null) {
            this.bitmapProvider.release(this.previousImage);
        }
        this.previousImage = null;
    }

    public void setData(GifHeader gifHeader, byte[] bArr) {
        this.header = gifHeader;
        this.data = bArr;
        this.status = 0;
        this.framePointer = -1;
        this.rawData = ByteBuffer.wrap(bArr);
        this.rawData.rewind();
        this.rawData.order(ByteOrder.LITTLE_ENDIAN);
        this.savePrevious = false;
        Iterator<GifFrame> it = gifHeader.frames.iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            } else if (it.next().dispose == 3) {
                this.savePrevious = true;
                break;
            }
        }
        this.mainPixels = new byte[gifHeader.width * gifHeader.height];
        this.mainScratch = new int[gifHeader.width * gifHeader.height];
    }

    private Bitmap setPixels(GifFrame gifFrame, GifFrame gifFrame2) {
        int i;
        int i2;
        int i3;
        int i4 = this.header.width;
        int i5 = this.header.height;
        int[] iArr = this.mainScratch;
        int i6 = 0;
        if (gifFrame2 != null && gifFrame2.dispose > 0) {
            if (gifFrame2.dispose == 2) {
                if (!gifFrame.transparency) {
                    i3 = this.header.bgColor;
                } else {
                    i3 = 0;
                }
                Arrays.fill(iArr, i3);
            } else if (gifFrame2.dispose == 3 && this.previousImage != null) {
                this.previousImage.getPixels(iArr, 0, i4, 0, 0, i4, i5);
            }
        }
        if (gifFrame2 == null && this.framePointer == 0) {
            if (!gifFrame.transparency) {
                i2 = this.header.bgColor;
            } else {
                i2 = 0;
            }
            Arrays.fill(iArr, i2);
        }
        decodeBitmapData(gifFrame);
        int i7 = 8;
        int i8 = 1;
        int i9 = 0;
        while (i6 < gifFrame.ih) {
            if (gifFrame.interlace) {
                if (i9 >= gifFrame.ih) {
                    i8++;
                    switch (i8) {
                        case 2:
                            i9 = 4;
                            break;
                        case Generator.STATE_GENERATED_FAIL:
                            i7 = 4;
                            i9 = 2;
                            break;
                        case 4:
                            i9 = 1;
                            i7 = 2;
                            break;
                    }
                }
                i = i9 + i7;
            } else {
                i = i9;
                i9 = i6;
            }
            int i10 = i9 + gifFrame.iy;
            if (i10 < this.header.height) {
                int i11 = i10 * this.header.width;
                int i12 = gifFrame.ix + i11;
                int i13 = gifFrame.iw + i12;
                if (this.header.width + i11 < i13) {
                    i13 = this.header.width + i11;
                }
                int i14 = gifFrame.iw * i6;
                while (i12 < i13) {
                    int i15 = i14 + 1;
                    int i16 = this.act[this.mainPixels[i14] & 255];
                    if (i16 != 0) {
                        iArr[i12] = i16;
                    }
                    i12++;
                    i14 = i15;
                }
            }
            i6++;
            i9 = i;
        }
        if ((this.savePrevious && gifFrame.dispose == 0) || gifFrame.dispose == 1) {
            if (this.previousImage == null) {
                this.previousImage = getNextBitmap();
            }
            this.previousImage.setPixels(iArr, 0, i4, 0, 0, i4, i5);
        }
        Bitmap nextBitmap = getNextBitmap();
        nextBitmap.setPixels(iArr, 0, i4, 0, 0, i4, i5);
        return nextBitmap;
    }

    private void decodeBitmapData(GifFrame gifFrame) {
        int i;
        short s;
        if (gifFrame != null) {
            this.rawData.position(gifFrame.bufferFrameStart);
        }
        int i2 = gifFrame == null ? this.header.width * this.header.height : gifFrame.ih * gifFrame.iw;
        if (this.mainPixels == null || this.mainPixels.length < i2) {
            this.mainPixels = new byte[i2];
        }
        if (this.prefix == null) {
            this.prefix = new short[4096];
        }
        if (this.suffix == null) {
            this.suffix = new byte[4096];
        }
        if (this.pixelStack == null) {
            this.pixelStack = new byte[4097];
        }
        int i3 = read();
        int i4 = 1;
        int i5 = 1 << i3;
        int i6 = i5 + 1;
        int i7 = i5 + 2;
        int i8 = i3 + 1;
        int i9 = (1 << i8) - 1;
        for (int i10 = 0; i10 < i5; i10++) {
            this.prefix[i10] = 0;
            this.suffix[i10] = (byte) i10;
        }
        int i11 = -1;
        int i12 = i8;
        int i13 = i7;
        int i14 = i9;
        int i15 = 0;
        int block = 0;
        int i16 = 0;
        int i17 = 0;
        int i18 = 0;
        int i19 = 0;
        int i20 = 0;
        int i21 = 0;
        int i22 = -1;
        while (true) {
            if (i15 >= i2) {
                break;
            }
            int i23 = 3;
            if (block == 0) {
                block = readBlock();
                if (block <= 0) {
                    this.status = 3;
                    break;
                }
                i18 = 0;
            }
            i17 += (this.block[i18] & 255) << i19;
            i18 += i4;
            block += i11;
            int i24 = i19 + 8;
            int i25 = i20;
            int i26 = i22;
            int i27 = i15;
            int i28 = i16;
            int i29 = i13;
            int i30 = i12;
            while (i24 >= i30) {
                int i31 = i17 & i14;
                i17 >>= i30;
                i24 -= i30;
                if (i31 != i5) {
                    if (i31 > i29) {
                        this.status = i23;
                    } else if (i31 != i6) {
                        if (i26 == -1) {
                            this.pixelStack[i21] = this.suffix[i31];
                            i26 = i31;
                            i25 = i26;
                            i21++;
                        } else {
                            if (i31 >= i29) {
                                i = i8;
                                this.pixelStack[i21] = (byte) i25;
                                s = i26;
                                i21++;
                            } else {
                                i = i8;
                                s = i31;
                            }
                            while (s >= i5) {
                                this.pixelStack[i21] = this.suffix[s];
                                s = this.prefix[s];
                                i21++;
                                i24 = i24;
                            }
                            int i32 = i24;
                            int i33 = this.suffix[s] & 255;
                            int i34 = i21 + 1;
                            int i35 = i5;
                            byte b = (byte) i33;
                            this.pixelStack[i21] = b;
                            if (i29 < 4096) {
                                this.prefix[i29] = (short) i26;
                                this.suffix[i29] = b;
                                i29++;
                                if ((i29 & i14) == 0 && i29 < 4096) {
                                    i30++;
                                    i14 += i29;
                                }
                            }
                            i21 = i34;
                            int i36 = i28;
                            while (i21 > 0 && i36 < i2) {
                                i21--;
                                this.mainPixels[i36] = this.pixelStack[i21];
                                i27++;
                                i36++;
                            }
                            i25 = i33;
                            i28 = i36;
                            i26 = i31;
                            i8 = i;
                            i24 = i32;
                            i5 = i35;
                        }
                        i23 = 3;
                    }
                    i22 = i26;
                    i12 = i30;
                    i13 = i29;
                    i15 = i27;
                    i16 = i28;
                    i20 = i25;
                    i4 = 1;
                    i11 = -1;
                    i19 = i24;
                    break;
                }
                i30 = i8;
                i29 = i7;
                i14 = i9;
                i26 = -1;
                i11 = -1;
            }
            i22 = i26;
            i12 = i30;
            i13 = i29;
            i15 = i27;
            i16 = i28;
            i4 = 1;
            i20 = i25;
            i19 = i24;
            i8 = i8;
        }
        while (i16 < i2) {
            this.mainPixels[i16] = 0;
            i16++;
        }
    }

    private int read() {
        try {
            return this.rawData.get() & 255;
        } catch (Exception e) {
            this.status = 1;
            return 0;
        }
    }

    private int readBlock() {
        int i = read();
        int i2 = 0;
        if (i > 0) {
            while (i2 < i) {
                int i3 = i - i2;
                try {
                    this.rawData.get(this.block, i2, i3);
                    i2 += i3;
                } catch (Exception e) {
                    Log.w(TAG, "Error Reading Block", e);
                    this.status = 1;
                }
            }
        }
        return i2;
    }

    private Bitmap getNextBitmap() {
        Bitmap bitmapObtain = this.bitmapProvider.obtain(this.header.width, this.header.height, BITMAP_CONFIG);
        if (bitmapObtain == null) {
            bitmapObtain = Bitmap.createBitmap(this.header.width, this.header.height, BITMAP_CONFIG);
        }
        setAlpha(bitmapObtain);
        return bitmapObtain;
    }

    @TargetApi(Utils.VERSION_CODES.HONEYCOMB_MR1)
    private static void setAlpha(Bitmap bitmap) {
        if (Build.VERSION.SDK_INT >= 12) {
            bitmap.setHasAlpha(true);
        }
    }
}
