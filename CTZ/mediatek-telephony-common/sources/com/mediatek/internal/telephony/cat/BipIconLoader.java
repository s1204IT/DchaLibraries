package com.mediatek.internal.telephony.cat;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.telephony.TelephonyManager;
import com.android.internal.telephony.cat.ImageDescriptor;
import com.android.internal.telephony.uicc.IccFileHandler;
import com.android.internal.util.HexDump;
import com.mediatek.internal.telephony.ppl.PplMessageManager;
import java.util.HashMap;

class BipIconLoader extends Handler {
    private static final int CLUT_ENTRY_SIZE = 3;
    private static final int CLUT_LOCATION_OFFSET = 4;
    private static final int EVENT_READ_CLUT_DONE = 3;
    private static final int EVENT_READ_EF_IMG_RECOED_DONE = 1;
    private static final int EVENT_READ_ICON_DONE = 2;
    private static final int STATE_MULTI_ICONS = 2;
    private static final int STATE_SINGLE_ICON = 1;
    private static final String TAG = "Stk-BipIL";
    private Bitmap mCurrentIcon;
    private int mCurrentRecordIndex;
    private Message mEndMsg;
    private byte[] mIconData;
    private Bitmap[] mIcons;
    private HashMap<Integer, Bitmap> mIconsCache;
    private ImageDescriptor mId;
    private int mRecordNumber;
    private int[] mRecordNumbers;
    private IccFileHandler mSimFH;
    private int mSlotId;
    private int mState;
    private static BipIconLoader sLoader = null;
    private static HandlerThread[] sThread = null;
    private static int sSimCount = 0;

    private BipIconLoader(Looper looper, IccFileHandler iccFileHandler, int i) {
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
        this.mSlotId = i;
        this.mIconsCache = new HashMap<>(50);
    }

    static BipIconLoader getInstance(Handler handler, IccFileHandler iccFileHandler, int i) {
        if (sLoader != null) {
            return sLoader;
        }
        if (sThread == null) {
            sSimCount = TelephonyManager.getDefault().getSimCount();
            sThread = new HandlerThread[sSimCount];
            for (int i2 = 0; i2 < sSimCount; i2++) {
                sThread[i2] = null;
            }
        }
        if (iccFileHandler == null) {
            return null;
        }
        if (sThread[i] == null) {
            sThread[i] = new HandlerThread("BIP Icon Loader");
            sThread[i].start();
        }
        if (sThread[i].getLooper() != null) {
            return new BipIconLoader(sThread[i].getLooper(), iccFileHandler, i);
        }
        return null;
    }

    void loadIcons(int[] iArr, Message message) {
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

    void loadIcon(int i, Message message) {
        if (message == null) {
            return;
        }
        this.mEndMsg = message;
        this.mState = 1;
        startLoadingIcon(i);
    }

    private void startLoadingIcon(int i) {
        MtkCatLog.d(TAG, "call startLoadingIcon");
        this.mId = null;
        this.mIconData = null;
        this.mCurrentIcon = null;
        this.mRecordNumber = i;
        if (this.mIconsCache.containsKey(Integer.valueOf(i))) {
            MtkCatLog.d(TAG, "mIconsCache contains record " + i);
            this.mCurrentIcon = this.mIconsCache.get(Integer.valueOf(i));
            postIcon();
            return;
        }
        MtkCatLog.d(TAG, "to load icon from EFimg");
        readId();
    }

    @Override
    public void handleMessage(Message message) {
        try {
            switch (message.what) {
                case 1:
                    MtkCatLog.d(TAG, "load EFimg done");
                    if (message.obj == null) {
                        MtkCatLog.e(TAG, "msg.obj is null.");
                        return;
                    }
                    MtkCatLog.d(TAG, "msg.obj is " + message.obj.getClass().getName());
                    AsyncResult asyncResult = (AsyncResult) message.obj;
                    MtkCatLog.d(TAG, "EFimg raw data: " + HexDump.toHexString((byte[]) asyncResult.result));
                    if (handleImageDescriptor((byte[]) asyncResult.result)) {
                        readIconData();
                        return;
                    }
                    throw new Exception("Unable to parse image descriptor");
                case 2:
                    MtkCatLog.d(TAG, "load icon done");
                    byte[] bArr = (byte[]) ((AsyncResult) message.obj).result;
                    MtkCatLog.d(TAG, "icon raw data: " + HexDump.toHexString(bArr));
                    MtkCatLog.d(TAG, "load icon CODING_SCHEME = " + this.mId.mCodingScheme);
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
                        MtkCatLog.d(TAG, "else  /postIcon ");
                        postIcon();
                        return;
                    }
                case 3:
                    MtkCatLog.d(TAG, "load clut done");
                    this.mCurrentIcon = parseToRGB(this.mIconData, this.mIconData.length, false, (byte[]) ((AsyncResult) message.obj).result);
                    this.mIconsCache.put(Integer.valueOf(this.mRecordNumber), this.mCurrentIcon);
                    postIcon();
                    return;
                default:
                    return;
            }
        } catch (Exception e) {
            MtkCatLog.d(this, "Icon load failed");
            e.printStackTrace();
            postIcon();
        }
    }

    private boolean handleImageDescriptor(byte[] bArr) {
        MtkCatLog.d(TAG, "call handleImageDescriptor");
        this.mId = ImageDescriptor.parse(bArr, 1);
        if (this.mId == null) {
            MtkCatLog.d(TAG, "fail to parse image raw data");
            return false;
        }
        MtkCatLog.d(TAG, "success to parse image raw data");
        return true;
    }

    private void readClut() {
        this.mSimFH.loadEFImgTransparent(this.mId.mImageId, this.mIconData[4], this.mIconData[5], this.mIconData[3] * 3, obtainMessage(3));
    }

    private void readId() {
        MtkCatLog.d(TAG, "call readId");
        if (this.mRecordNumber < 0) {
            this.mCurrentIcon = null;
            postIcon();
        } else {
            this.mSimFH.loadEFImgLinearFixed(this.mRecordNumber, obtainMessage(1));
        }
    }

    private void readIconData() {
        MtkCatLog.d(TAG, "call readIconData");
        this.mSimFH.loadEFImgTransparent(this.mId.mImageId, 0, 0, this.mId.mLength, obtainMessage(2));
    }

    private void postIcon() {
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
            MtkCatLog.d("BipIconLoader", "parseToBnW; size error");
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
        int i6 = bArr[2] & PplMessageManager.Type.INVALID;
        int i7 = 3;
        int i8 = bArr[3] & PplMessageManager.Type.INVALID;
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
        this.mSimFH = null;
        if (sThread[this.mSlotId] != null) {
            sThread[this.mSlotId].quit();
            sThread[this.mSlotId] = null;
        }
        int i = 0;
        while (i < sSimCount && sThread[i] == null) {
            i++;
        }
        if (i == sSimCount) {
            sThread = null;
        }
        this.mIconsCache = null;
        sLoader = null;
    }
}
