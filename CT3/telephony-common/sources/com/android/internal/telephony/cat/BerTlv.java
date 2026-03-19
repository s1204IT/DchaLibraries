package com.android.internal.telephony.cat;

import com.mediatek.internal.telephony.ppl.PplMessageManager;
import java.util.List;

class BerTlv {
    public static final int BER_EVENT_DOWNLOAD_TAG = 214;
    public static final int BER_MENU_SELECTION_TAG = 211;
    public static final int BER_PROACTIVE_COMMAND_TAG = 208;
    public static final int BER_UNKNOWN_TAG = 0;
    private List<ComprehensionTlv> mCompTlvs;
    private boolean mLengthValid;
    private int mTag;

    private BerTlv(int tag, List<ComprehensionTlv> ctlvs, boolean lengthValid) {
        this.mTag = 0;
        this.mCompTlvs = null;
        this.mLengthValid = true;
        this.mTag = tag;
        this.mCompTlvs = ctlvs;
        this.mLengthValid = lengthValid;
    }

    public List<ComprehensionTlv> getComprehensionTlvs() {
        return this.mCompTlvs;
    }

    public int getTag() {
        return this.mTag;
    }

    public boolean isLengthValid() {
        return this.mLengthValid;
    }

    public static BerTlv decode(byte[] data) throws ResultException {
        int endIndex = data.length;
        int length = 0;
        boolean isLengthValid = true;
        int curIndex = 1;
        try {
            int tag = data[0] & PplMessageManager.Type.INVALID;
            if (tag == 208) {
                int curIndex2 = 1 + 1;
                try {
                    int temp = data[1] & PplMessageManager.Type.INVALID;
                    if (temp < 128) {
                        length = temp;
                        curIndex = curIndex2;
                    } else {
                        if (temp != 129) {
                            throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD, "Expected first byte to be length or a length tag and < 0x81 byte= " + Integer.toHexString(temp) + " curIndex=" + curIndex2 + " endIndex=" + endIndex);
                        }
                        curIndex = curIndex2 + 1;
                        int temp2 = data[curIndex2] & PplMessageManager.Type.INVALID;
                        if (temp2 < 128) {
                            throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD, "length < 0x80 length=" + Integer.toHexString(0) + " curIndex=" + curIndex + " endIndex=" + endIndex);
                        }
                        length = temp2;
                    }
                } catch (ResultException e) {
                    e = e;
                    throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD, e.explanation());
                } catch (IndexOutOfBoundsException e2) {
                    curIndex = curIndex2;
                    throw new ResultException(ResultCode.REQUIRED_VALUES_MISSING, "IndexOutOfBoundsException  curIndex=" + curIndex + " endIndex=" + endIndex);
                }
            } else if (ComprehensionTlvTag.COMMAND_DETAILS.value() == (tag & (-129))) {
                tag = 0;
                curIndex = 0;
            }
            if (endIndex - curIndex < length) {
            }
            List<ComprehensionTlv> ctlvs = ComprehensionTlv.decodeMany(data, curIndex);
            if (tag == 208) {
                int totalLength = 0;
                for (ComprehensionTlv item : ctlvs) {
                    int itemLength = item.getLength();
                    if (itemLength >= 128 && itemLength <= 255) {
                        totalLength += itemLength + 3;
                    } else {
                        if (itemLength < 0 || itemLength >= 128) {
                            isLengthValid = false;
                            break;
                        }
                        totalLength += itemLength + 2;
                    }
                }
                if (length != totalLength) {
                    isLengthValid = false;
                }
            }
            return new BerTlv(tag, ctlvs, isLengthValid);
        } catch (ResultException e3) {
            e = e3;
        } catch (IndexOutOfBoundsException e4) {
        }
    }
}
