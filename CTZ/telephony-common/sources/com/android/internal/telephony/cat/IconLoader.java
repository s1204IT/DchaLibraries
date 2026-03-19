package com.android.internal.telephony.cat;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import com.android.internal.telephony.TelephonyComponentFactory;
import com.android.internal.telephony.uicc.IccFileHandler;
import java.util.HashMap;

public class IconLoader extends Handler {
    protected static final int CLUT_ENTRY_SIZE = 3;
    protected static final int CLUT_LOCATION_OFFSET = 4;
    protected static final int EVENT_READ_CLUT_DONE = 3;
    protected static final int EVENT_READ_EF_IMG_RECOED_DONE = 1;
    protected static final int EVENT_READ_ICON_DONE = 2;
    protected static final int STATE_MULTI_ICONS = 2;
    protected static final int STATE_SINGLE_ICON = 1;
    private static HashMap<Object, IconLoader> sLoader = null;
    private static HashMap<Object, HandlerThread> sThread = null;
    protected Bitmap mCurrentIcon;
    protected int mCurrentRecordIndex;
    protected Message mEndMsg;
    protected byte[] mIconData;
    protected Bitmap[] mIcons;
    protected HashMap<Integer, Bitmap> mIconsCache;
    protected ImageDescriptor mId;
    protected int mRecordNumber;
    protected int[] mRecordNumbers;
    protected IccFileHandler mSimFH;
    protected int mState;

    public IconLoader(Looper looper, IccFileHandler iccFileHandler) {
        super(looper);
        this.mState = 1;
        this.mId = null;
        this.mCurrentIcon = null;
        this.mSimFH = null;
        this.mEndMsg = null;
        this.mIconData = null;
        this.mRecordNumbers = null;
        this.mCurrentRecordIndex = 0;
        this.mIcons = null;
        this.mIconsCache = null;
        this.mSimFH = iccFileHandler;
        this.mIconsCache = new HashMap<>(50);
    }

    static IconLoader getInstance(Handler handler, IccFileHandler iccFileHandler) {
        if (sLoader != null && sLoader.containsKey(iccFileHandler)) {
            return sLoader.get(iccFileHandler);
        }
        if (iccFileHandler != null) {
            if (sThread == null) {
                sThread = new HashMap<>(4);
            }
            HandlerThread handlerThread = new HandlerThread("Cat Icon Loader");
            sThread.put(iccFileHandler, handlerThread);
            handlerThread.start();
            if (sLoader == null) {
                sLoader = new HashMap<>(4);
            }
            IconLoader iconLoaderMakeIconLoader = TelephonyComponentFactory.getInstance().makeIconLoader(handlerThread.getLooper(), iccFileHandler);
            sLoader.put(iccFileHandler, iconLoaderMakeIconLoader);
            return iconLoaderMakeIconLoader;
        }
        return null;
    }

    public void loadIcons(int[] iArr, Message message) {
        if (iArr == null || iArr.length == 0 || message == null) {
            return;
        }
        this.mEndMsg = message;
        this.mIcons = new Bitmap[iArr.length];
        this.mRecordNumbers = iArr;
        this.mCurrentRecordIndex = 0;
        this.mState = 2;
        startLoadingIcon(iArr[0]);
    }

    public void loadIcon(int i, Message message) {
        if (message == null) {
            return;
        }
        this.mEndMsg = message;
        this.mState = 1;
        startLoadingIcon(i);
    }

    protected void startLoadingIcon(int i) {
        this.mId = null;
        this.mIconData = null;
        this.mCurrentIcon = null;
        this.mRecordNumber = i;
        if (this.mIconsCache.containsKey(Integer.valueOf(i))) {
            this.mCurrentIcon = this.mIconsCache.get(Integer.valueOf(i));
            postIcon();
        } else {
            readId();
        }
    }

    @Override
    public void handleMessage(Message message) {
        try {
            switch (message.what) {
                case 1:
                    if (handleImageDescriptor((byte[]) ((AsyncResult) message.obj).result)) {
                        readIconData();
                        return;
                    }
                    throw new Exception("Unable to parse image descriptor");
                case 2:
                    CatLog.d(this, "load icon done");
                    byte[] bArr = (byte[]) ((AsyncResult) message.obj).result;
                    if (this.mId.mCodingScheme == 17) {
                        this.mCurrentIcon = parseToBnW(bArr, bArr.length);
                        this.mIconsCache.put(Integer.valueOf(this.mRecordNumber), this.mCurrentIcon);
                        postIcon();
                        return;
                    } else if (this.mId.mCodingScheme == 33) {
                        this.mIconData = bArr;
                        readClut();
                        return;
                    } else {
                        CatLog.d(this, "else  /postIcon ");
                        postIcon();
                        return;
                    }
                case 3:
                    this.mCurrentIcon = parseToRGB(this.mIconData, this.mIconData.length, false, (byte[]) ((AsyncResult) message.obj).result);
                    this.mIconsCache.put(Integer.valueOf(this.mRecordNumber), this.mCurrentIcon);
                    postIcon();
                    return;
                default:
                    return;
            }
        } catch (Exception e) {
            CatLog.d(this, "Icon load failed");
            postIcon();
        }
    }

    protected boolean handleImageDescriptor(byte[] bArr) {
        this.mId = ImageDescriptor.parse(bArr, 1);
        if (this.mId != null) {
            return true;
        }
        return false;
    }

    protected void readClut() {
        this.mSimFH.loadEFImgTransparent(this.mId.mImageId, this.mIconData[4], this.mIconData[5], this.mIconData[3] * 3, obtainMessage(3));
    }

    protected void readId() {
        if (this.mRecordNumber < 0) {
            this.mCurrentIcon = null;
            postIcon();
        } else {
            this.mSimFH.loadEFImgLinearFixed(this.mRecordNumber, obtainMessage(1));
        }
    }

    protected void readIconData() {
        this.mSimFH.loadEFImgTransparent(this.mId.mImageId, 0, 0, this.mId.mLength, obtainMessage(2));
    }

    protected void postIcon() {
        if (this.mState == 1) {
            this.mEndMsg.obj = this.mCurrentIcon;
            this.mEndMsg.sendToTarget();
        } else if (this.mState == 2) {
            Bitmap[] bitmapArr = this.mIcons;
            int i = this.mCurrentRecordIndex;
            this.mCurrentRecordIndex = i + 1;
            bitmapArr[i] = this.mCurrentIcon;
            if (this.mCurrentRecordIndex < this.mRecordNumbers.length) {
                startLoadingIcon(this.mRecordNumbers[this.mCurrentRecordIndex]);
                return;
            }
            this.mEndMsg.obj = this.mIcons;
            this.mEndMsg.sendToTarget();
        }
    }

    public static Bitmap parseToBnW(byte[] bArr, int i) {
        int i2 = 0;
        int i3 = bArr[0] & 255;
        int i4 = bArr[1] & 255;
        int i5 = i3 * i4;
        int[] iArr = new int[i5];
        int i6 = 2;
        byte b = 0;
        int i7 = 7;
        while (i2 < i5) {
            if (i2 % 8 == 0) {
                i7 = 7;
                b = bArr[i6];
                i6++;
            }
            iArr[i2] = bitToBnW((b >> i7) & 1);
            i2++;
            i7--;
        }
        if (i2 != i5) {
            CatLog.d("IconLoader", "parseToBnW; size error");
        }
        return Bitmap.createBitmap(iArr, i3, i4, Bitmap.Config.ARGB_8888);
    }

    private static int bitToBnW(int i) {
        if (i == 1) {
            return -1;
        }
        return -16777216;
    }

    public static Bitmap parseToRGB(byte[] bArr, int i, boolean z, byte[] bArr2) {
        int i2 = 0;
        int i3 = bArr[0] & 255;
        boolean z2 = true;
        int i4 = bArr[1] & 255;
        int i5 = 2;
        int i6 = bArr[2] & 255;
        int i7 = 3;
        int i8 = bArr[3] & 255;
        if (true == z) {
            bArr2[i8 - 1] = 0;
        }
        int i9 = i3 * i4;
        int[] iArr = new int[i9];
        int i10 = 8 - i6;
        int i11 = 7;
        byte b = bArr[6];
        int mask = getMask(i6);
        if (8 % i6 != 0) {
            z2 = false;
        }
        byte b2 = b;
        int i12 = i10;
        while (i2 < i9) {
            if (i12 < 0) {
                int i13 = i11 + 1;
                byte b3 = bArr[i11];
                if (!z2) {
                    i12 *= -1;
                } else {
                    i12 = i10;
                }
                i11 = i13;
                b2 = b3;
            }
            int i14 = ((b2 >> i12) & mask) * i7;
            iArr[i2] = Color.rgb((int) bArr2[i14], (int) bArr2[i14 + 1], (int) bArr2[i14 + i5]);
            i12 -= i6;
            i2++;
            i5 = 2;
            i7 = 3;
        }
        return Bitmap.createBitmap(iArr, i3, i4, Bitmap.Config.ARGB_8888);
    }

    private static int getMask(int i) {
        switch (i) {
            case 1:
                return 1;
            case 2:
                return 3;
            case 3:
                return 7;
            case 4:
                return 15;
            case 5:
                return 31;
            case 6:
                return 63;
            case 7:
                return 127;
            case 8:
                return 255;
            default:
                return 0;
        }
    }

    public void dispose() {
        if (sThread != null && sThread.containsKey(this.mSimFH)) {
            HandlerThread handlerThread = sThread.get(this.mSimFH);
            if (handlerThread != null) {
                handlerThread.quit();
            }
            sThread.remove(this.mSimFH);
        }
        this.mIconsCache = null;
        if (sLoader != null && sLoader.containsKey(this.mSimFH)) {
            sLoader.get(this.mSimFH);
            sLoader.remove(this.mSimFH);
        }
        this.mSimFH = null;
    }
}
