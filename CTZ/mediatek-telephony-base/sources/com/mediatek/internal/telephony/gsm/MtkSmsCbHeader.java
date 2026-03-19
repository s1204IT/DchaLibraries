package com.mediatek.internal.telephony.gsm;

import android.os.Build;
import android.telephony.Rlog;
import android.telephony.SmsCbCmasInfo;
import android.telephony.SmsCbEtwsInfo;
import android.util.Xml;
import com.android.internal.telephony.gsm.SmsCbHeader;
import com.android.internal.util.XmlUtils;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import mediatek.telephony.MtkTelephony;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class MtkSmsCbHeader extends SmsCbHeader {
    private static final String LOG_TAG = "MtkSmsCbHeader";
    private static final String SPECIAL_PWS_CHANNEL_PATH = "/vendor/etc/special_pws_channel.xml";
    private boolean mIsEtwsPrimary;
    protected String mPlmn;
    private static final boolean ENG = "eng".equals(Build.TYPE);
    private static HashMap<String, String> mSpecialChannelList = null;
    private static final Object mListLock = new Object();

    public MtkSmsCbHeader(byte[] bArr, String str, boolean z) throws IllegalArgumentException {
        byte[] bArrCopyOfRange;
        this.mIsEtwsPrimary = false;
        if (bArr == null || bArr.length < 6) {
            throw new IllegalArgumentException("Illegal PDU");
        }
        this.mPlmn = str;
        this.mIsEtwsPrimary = z;
        int i = 1;
        if (bArr.length <= 88) {
            this.mGeographicalScope = (bArr[0] & 192) >>> 6;
            this.mSerialNumber = ((bArr[0] & 255) << 8) | (bArr[1] & 255);
            this.mMessageIdentifier = ((bArr[2] & 255) << 8) | (bArr[3] & 255);
            if (isEtwsMessage() && bArr.length <= 56 && this.mIsEtwsPrimary) {
                this.mFormat = 3;
                this.mDataCodingScheme = -1;
                this.mPageIndex = -1;
                this.mNrOfPages = -1;
                boolean z2 = (bArr[4] & 1) != 0;
                boolean z3 = (bArr[5] & 128) != 0;
                int i2 = (bArr[4] & 254) >>> 1;
                if (bArr.length > 6) {
                    bArrCopyOfRange = Arrays.copyOfRange(bArr, 6, bArr.length);
                } else {
                    bArrCopyOfRange = null;
                }
                this.mEtwsInfo = new SmsCbEtwsInfo(i2, z2, z3, true, bArrCopyOfRange);
                if (ENG) {
                    log("Create primary ETWS Info!");
                }
                this.mCmasInfo = null;
                return;
            }
            this.mFormat = 1;
            this.mDataCodingScheme = bArr[4] & 255;
            int i3 = (bArr[5] & 240) >>> 4;
            int i4 = bArr[5] & 15;
            if (i3 == 0 || i4 == 0 || i3 > i4) {
                i4 = 1;
            } else {
                i = i3;
            }
            this.mPageIndex = i;
            this.mNrOfPages = i4;
        } else {
            this.mFormat = 2;
            byte b = bArr[0];
            if (b != 1) {
                throw new IllegalArgumentException("Unsupported message type " + ((int) b));
            }
            this.mMessageIdentifier = ((bArr[1] & 255) << 8) | (bArr[2] & 255);
            this.mGeographicalScope = (bArr[3] & 192) >>> 6;
            this.mSerialNumber = ((bArr[3] & 255) << 8) | (bArr[4] & 255);
            this.mDataCodingScheme = bArr[5] & 255;
            this.mPageIndex = 1;
            this.mNrOfPages = 1;
        }
        if (isEtwsMessage()) {
            this.mEtwsInfo = new SmsCbEtwsInfo(getEtwsWarningType(), isEtwsEmergencyUserAlert(), isEtwsPopupAlert(), false, (byte[]) null);
            if (ENG) {
                log("Create non-primary ETWS Info!");
            }
            this.mCmasInfo = null;
        } else if (isCmasMessage()) {
            int cmasMessageClass = getCmasMessageClass();
            int cmasSeverity = getCmasSeverity();
            int cmasUrgency = getCmasUrgency();
            int cmasCertainty = getCmasCertainty();
            this.mEtwsInfo = null;
            this.mCmasInfo = new SmsCbCmasInfo(cmasMessageClass, -1, -1, cmasSeverity, cmasUrgency, cmasCertainty);
        } else {
            this.mEtwsInfo = null;
            this.mCmasInfo = null;
        }
        log("pdu length= " + bArr.length + ", " + this);
    }

    protected boolean isEmergencyMessage() {
        return (this.mMessageIdentifier >= 4352 && this.mMessageIdentifier <= 6399) || checkNationalEmergencyChannels();
    }

    protected boolean isCmasMessage() {
        return (this.mMessageIdentifier >= 4370 && this.mMessageIdentifier <= 4399) || checkNationalEmergencyChannels();
    }

    protected int getCmasMessageClass() {
        int i = this.mMessageIdentifier;
        if (i == 911 || i == 919 || i == 921) {
            return 0;
        }
        return super.getCmasMessageClass();
    }

    public String toString() {
        return "MtkSmsCbHeader{GS=" + this.mGeographicalScope + ", serialNumber=0x" + Integer.toHexString(this.mSerialNumber) + ", messageIdentifier=0x" + Integer.toHexString(this.mMessageIdentifier) + ", DCS=0x" + Integer.toHexString(this.mDataCodingScheme) + ", page " + this.mPageIndex + " of " + this.mNrOfPages + ", isEtwsPrimary=" + this.mIsEtwsPrimary + ", plmn " + this.mPlmn + '}';
    }

    private static void loadSpecialChannelList() {
        XmlPullParser xmlPullParserNewPullParser;
        synchronized (mListLock) {
            if (mSpecialChannelList == null) {
                log("load special_pws_channel.xml...");
                mSpecialChannelList = new HashMap<>();
                File file = new File(SPECIAL_PWS_CHANNEL_PATH);
                try {
                    FileReader fileReader = new FileReader(file);
                    try {
                        xmlPullParserNewPullParser = Xml.newPullParser();
                        xmlPullParserNewPullParser.setInput(fileReader);
                        XmlUtils.beginDocument(xmlPullParserNewPullParser, "SpecialPwsChannel");
                    } catch (IOException e) {
                        loge("Exception in parser " + e);
                    } catch (XmlPullParserException e2) {
                        loge("Exception in parser " + e2);
                    }
                    while (true) {
                        XmlUtils.nextElement(xmlPullParserNewPullParser);
                        if (!"SpecialPwsChannel".equals(xmlPullParserNewPullParser.getName())) {
                            break;
                        }
                        mSpecialChannelList.put(xmlPullParserNewPullParser.getAttributeValue(null, MtkTelephony.Carriers.MCC), xmlPullParserNewPullParser.getAttributeValue(null, "channels"));
                    }
                    fileReader.close();
                    log("Special channels list size=" + mSpecialChannelList.size());
                } catch (FileNotFoundException e3) {
                    Rlog.w(LOG_TAG, "Can not open " + file.getAbsolutePath());
                }
            } else {
                log("Special PWS channel list is already loaded");
            }
        }
    }

    private boolean checkNationalEmergencyChannels() {
        loadSpecialChannelList();
        if (mSpecialChannelList != null) {
            String strSubstring = (this.mPlmn == null || this.mPlmn.length() < 3) ? "" : this.mPlmn.substring(0, 3);
            String str = mSpecialChannelList.get(strSubstring);
            log("checkNationalEmergencyChannels, mPlmn " + this.mPlmn + ",mcc " + strSubstring + ", channels list " + str + ", header's channel " + this.mMessageIdentifier);
            if (str != null && str.length() > 0) {
                String[] strArrSplit = str.split(",");
                for (String str2 : strArrSplit) {
                    if (str2.equals(Integer.toString(this.mMessageIdentifier))) {
                        return true;
                    }
                }
            }
        } else {
            log("checkNationalEmergencyChannels, mSpecialChannelList is null!");
        }
        return false;
    }

    private static void log(String str) {
        if (ENG) {
            Rlog.d(LOG_TAG, str);
        }
    }

    private static void loge(String str) {
        if (ENG) {
            Rlog.e(LOG_TAG, str);
        }
    }
}
