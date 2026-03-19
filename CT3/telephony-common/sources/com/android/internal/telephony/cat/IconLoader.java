package com.android.internal.telephony.cat;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.telephony.TelephonyManager;
import com.android.internal.telephony.CallFailCause;
import com.android.internal.telephony.uicc.IccFileHandler;
import com.android.internal.util.HexDump;
import com.mediatek.internal.telephony.ppl.PplMessageManager;
import java.util.HashMap;

class IconLoader extends Handler {
    private static final int CLUT_ENTRY_SIZE = 3;
    private static final int CLUT_LOCATION_OFFSET = 4;
    private static final int EVENT_READ_CLUT_DONE = 3;
    private static final int EVENT_READ_EF_IMG_RECOED_DONE = 1;
    private static final int EVENT_READ_ICON_DONE = 2;
    private static final int STATE_MULTI_ICONS = 2;
    private static final int STATE_SINGLE_ICON = 1;
    private static final String TAG = "Stk-IL";
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
    private static IconLoader sLoader = null;
    private static HandlerThread[] sThread = null;
    private static int sSimCount = 0;

    private IconLoader(Looper looper, IccFileHandler fh, int slotId) {
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
        this.mSimFH = fh;
        this.mSlotId = slotId;
        this.mIconsCache = new HashMap<>(50);
    }

    static IconLoader getInstance(Handler caller, IccFileHandler fh, int slotId) {
        if (sLoader != null) {
            return sLoader;
        }
        if (sThread == null) {
            sSimCount = TelephonyManager.getDefault().getSimCount();
            sThread = new HandlerThread[sSimCount];
            for (int i = 0; i < sSimCount; i++) {
                sThread[i] = null;
            }
        }
        if (fh == null) {
            return null;
        }
        if (sThread[slotId] == null) {
            sThread[slotId] = new HandlerThread("Cat Icon Loader");
            sThread[slotId].start();
        }
        if (sThread[slotId].getLooper() != null) {
            return new IconLoader(sThread[slotId].getLooper(), fh, slotId);
        }
        return null;
    }

    void loadIcons(int[] recordNumbers, Message msg) {
        if (recordNumbers == null || recordNumbers.length == 0 || msg == null) {
            return;
        }
        this.mEndMsg = msg;
        this.mIcons = new Bitmap[recordNumbers.length];
        this.mRecordNumbers = recordNumbers;
        this.mCurrentRecordIndex = 0;
        this.mState = 2;
        startLoadingIcon(recordNumbers[0]);
    }

    void loadIcon(int recordNumber, Message msg) {
        if (msg == null) {
            return;
        }
        this.mEndMsg = msg;
        this.mState = 1;
        startLoadingIcon(recordNumber);
    }

    private void startLoadingIcon(int recordNumber) {
        CatLog.d(TAG, "call startLoadingIcon");
        this.mId = null;
        this.mIconData = null;
        this.mCurrentIcon = null;
        this.mRecordNumber = recordNumber;
        if (this.mIconsCache.containsKey(Integer.valueOf(recordNumber))) {
            CatLog.d(TAG, "mIconsCache contains record " + recordNumber);
            this.mCurrentIcon = this.mIconsCache.get(Integer.valueOf(recordNumber));
            postIcon();
        } else {
            CatLog.d(TAG, "to load icon from EFimg");
            readId();
        }
    }

    @Override
    public void handleMessage(Message msg) {
        try {
            switch (msg.what) {
                case 1:
                    CatLog.d(TAG, "load EFimg done");
                    if (msg.obj == null) {
                        CatLog.e(TAG, "msg.obj is null.");
                        return;
                    }
                    CatLog.d(TAG, "msg.obj is " + msg.obj.getClass().getName());
                    AsyncResult ar = (AsyncResult) msg.obj;
                    CatLog.d(TAG, "EFimg raw data: " + HexDump.toHexString((byte[]) ar.result));
                    if (handleImageDescriptor((byte[]) ar.result)) {
                        readIconData();
                        return;
                    }
                    throw new Exception("Unable to parse image descriptor");
                case 2:
                    CatLog.d(TAG, "load icon done");
                    AsyncResult ar2 = (AsyncResult) msg.obj;
                    byte[] rawData = (byte[]) ar2.result;
                    CatLog.d(TAG, "icon raw data: " + HexDump.toHexString(rawData));
                    CatLog.d(TAG, "load icon CODING_SCHEME = " + this.mId.mCodingScheme);
                    if (this.mId.mCodingScheme == 17) {
                        this.mCurrentIcon = parseToBnW(rawData, rawData.length);
                        this.mIconsCache.put(Integer.valueOf(this.mRecordNumber), this.mCurrentIcon);
                        postIcon();
                        return;
                    } else if (this.mId.mCodingScheme == 33) {
                        this.mIconData = rawData;
                        readClut();
                        return;
                    } else {
                        CatLog.d(TAG, "else  /postIcon ");
                        postIcon();
                        return;
                    }
                case 3:
                    CatLog.d(TAG, "load clut done");
                    AsyncResult ar3 = (AsyncResult) msg.obj;
                    byte[] clut = (byte[]) ar3.result;
                    this.mCurrentIcon = parseToRGB(this.mIconData, this.mIconData.length, false, clut);
                    this.mIconsCache.put(Integer.valueOf(this.mRecordNumber), this.mCurrentIcon);
                    postIcon();
                    return;
                default:
                    return;
            }
        } catch (Exception e) {
            CatLog.d(this, "Icon load failed");
            e.printStackTrace();
            postIcon();
        }
    }

    private boolean handleImageDescriptor(byte[] rawData) {
        CatLog.d(TAG, "call handleImageDescriptor");
        this.mId = ImageDescriptor.parse(rawData, 1);
        if (this.mId == null) {
            CatLog.d(TAG, "fail to parse image raw data");
            return false;
        }
        CatLog.d(TAG, "success to parse image raw data");
        return true;
    }

    private void readClut() {
        int length = this.mIconData[3] * 3;
        Message msg = obtainMessage(3);
        this.mSimFH.loadEFImgTransparent(this.mId.mImageId, this.mIconData[4], this.mIconData[5], length, msg);
    }

    private void readId() {
        CatLog.d(TAG, "call readId");
        if (this.mRecordNumber < 0) {
            this.mCurrentIcon = null;
            postIcon();
        } else {
            Message msg = obtainMessage(1);
            this.mSimFH.loadEFImgLinearFixed(this.mRecordNumber, msg);
        }
    }

    private void readIconData() {
        CatLog.d(TAG, "call readIconData");
        Message msg = obtainMessage(2);
        this.mSimFH.loadEFImgTransparent(this.mId.mImageId, 0, 0, this.mId.mLength, msg);
    }

    private void postIcon() {
        if (this.mState == 1) {
            this.mEndMsg.obj = this.mCurrentIcon;
            this.mEndMsg.sendToTarget();
        } else {
            if (this.mState != 2) {
                return;
            }
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

    public static Bitmap parseToBnW(byte[] data, int length) {
        int valueIndex;
        int width = data[0] & 255;
        int valueIndex2 = 1 + 1;
        int height = data[1] & 255;
        int numOfPixels = width * height;
        int[] pixels = new int[numOfPixels];
        int bitIndex = 7;
        byte currentByte = 0;
        int pixelIndex = 0;
        while (pixelIndex < numOfPixels) {
            if (pixelIndex % 8 == 0) {
                valueIndex = valueIndex2 + 1;
                currentByte = data[valueIndex2];
                bitIndex = 7;
            } else {
                valueIndex = valueIndex2;
            }
            pixels[pixelIndex] = bitToBnW((currentByte >> bitIndex) & 1);
            bitIndex--;
            pixelIndex++;
            valueIndex2 = valueIndex;
        }
        if (pixelIndex != numOfPixels) {
            CatLog.d("IconLoader", "parseToBnW; size error");
        }
        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888);
    }

    private static int bitToBnW(int bit) {
        if (bit == 1) {
            return -1;
        }
        return -16777216;
    }

    public static Bitmap parseToRGB(byte[] data, int length, boolean transparency, byte[] clut) {
        boolean bitsOverlaps;
        int pixelIndex;
        int valueIndex;
        int valueIndex2;
        int width = data[0] & 255;
        int valueIndex3 = 1 + 1;
        int height = data[1] & 255;
        int valueIndex4 = valueIndex3 + 1;
        int bitsPerImg = data[valueIndex3] & PplMessageManager.Type.INVALID;
        int i = valueIndex4 + 1;
        int numOfClutEntries = data[valueIndex4] & PplMessageManager.Type.INVALID;
        if (transparency) {
            clut[numOfClutEntries - 1] = 0;
        }
        int numOfPixels = width * height;
        int[] pixels = new int[numOfPixels];
        int bitsStartOffset = 8 - bitsPerImg;
        int bitIndex = bitsStartOffset;
        byte currentByte = data[6];
        int mask = getMask(bitsPerImg);
        if (8 % bitsPerImg == 0) {
            bitsOverlaps = true;
            pixelIndex = 0;
            valueIndex = 7;
        } else {
            bitsOverlaps = false;
            pixelIndex = 0;
            valueIndex = 7;
        }
        while (pixelIndex < numOfPixels) {
            if (bitIndex < 0) {
                valueIndex2 = valueIndex + 1;
                currentByte = data[valueIndex];
                bitIndex = bitsOverlaps ? bitsStartOffset : bitIndex * (-1);
            } else {
                valueIndex2 = valueIndex;
            }
            int clutEntry = (currentByte >> bitIndex) & mask;
            int clutIndex = clutEntry * 3;
            pixels[pixelIndex] = Color.rgb((int) clut[clutIndex], (int) clut[clutIndex + 1], (int) clut[clutIndex + 2]);
            bitIndex -= bitsPerImg;
            pixelIndex++;
            valueIndex = valueIndex2;
        }
        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888);
    }

    private static int getMask(int numOfBits) {
        switch (numOfBits) {
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
                return CallFailCause.INTERWORKING_UNSPECIFIED;
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
