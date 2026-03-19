package com.mediatek.internal.telephony.uicc;

import android.content.Context;
import android.os.SystemProperties;
import android.telephony.Rlog;
import com.android.internal.telephony.CallFailCause;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.uicc.IccConstants;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class SmsIccFileFetcher extends IccFileFetcherBase {
    private static final String SMSP = "ef_smsp";
    private static final String SMSS = "ef_smss";
    private static final String TAG = "SmsIccFileFetcher";
    ArrayList<String> mFileList;

    public SmsIccFileFetcher(Context c, Phone phone) {
        super(c, phone);
        this.mFileList = new ArrayList<>();
        this.mFileList.add(SMSS);
        this.mFileList.add(SMSP);
    }

    @Override
    public ArrayList<String> onGetKeys() {
        return this.mFileList;
    }

    @Override
    public IccFileRequest onGetFilePara(String key) {
        if (!key.equals(SMSS)) {
            if (key.equals(SMSP)) {
                return new IccFileRequest(IccConstants.EF_GID1, 0, 2, "3F007F25", null, -1, null);
            }
            return null;
        }
        return new IccFileRequest(28477, 1, 2, "3F007F25", null, -1, null);
    }

    @Override
    public void onParseResult(String key, byte[] transparent, ArrayList<byte[]> linearfixed) {
        if (SMSS.equals(key)) {
            Rlog.d(TAG, "SMSS = " + Arrays.toString(transparent));
            this.mData.put(SMSS, transparent);
            return;
        }
        if (!SMSP.equals(key)) {
            return;
        }
        Rlog.d(TAG, "SMSP = " + linearfixed);
        this.mData.put(SMSP, linearfixed);
        if (linearfixed == null || linearfixed.size() <= 0) {
            return;
        }
        for (byte[] item : linearfixed) {
            Rlog.d(TAG, "SMSP = " + Arrays.toString(item));
        }
    }

    public synchronized int getNextMessageId() {
        int nextMsgId;
        Rlog.d(TAG, "getNextMessageId");
        byte[] bytes = (byte[]) this.mData.get(SMSS);
        nextMsgId = -1;
        if (bytes != null && bytes.length > 4) {
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            DataInputStream dis = new DataInputStream(bais);
            try {
                int msgId = dis.readUnsignedShort();
                dis.close();
                nextMsgId = (msgId % CallFailCause.ERROR_UNSPECIFIED) + 1;
                String str = Integer.toString(nextMsgId);
                SystemProperties.set("persist.radio.cdma.msgid", str);
            } catch (IOException e) {
                Rlog.e(TAG, "getNextMessageId IOException");
            }
            Rlog.d(TAG, "getmWapMsgId nextMsgId = " + nextMsgId);
            byte[] bs = ByteBuffer.allocate(4).putInt(nextMsgId).array();
            bytes[0] = bs[2];
            bytes[1] = bs[3];
            IccFileRequest simInfo = new IccFileRequest(IccConstants.EF_GID1, 1, 2, "3F007F25", bytes, -1, null);
            updateSimInfo(simInfo);
        }
        return nextMsgId;
    }

    public synchronized int getWapMsgId() {
        int mWapMsgId;
        Rlog.d(TAG, "getmWapMsgId");
        byte[] bytes = (byte[]) this.mData.get(SMSS);
        mWapMsgId = -1;
        if (bytes != null && bytes.length > 4) {
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            DataInputStream dis = new DataInputStream(bais);
            try {
                dis.readUnsignedShort();
                int msgId = dis.readUnsignedShort();
                dis.close();
                mWapMsgId = (msgId % CallFailCause.ERROR_UNSPECIFIED) + 1;
            } catch (IOException e) {
                Rlog.e(TAG, "getmWapMsgId IOException");
            }
            Rlog.d(TAG, "getmWapMsgId mWapMsgId = " + mWapMsgId);
            byte[] bs = ByteBuffer.allocate(4).putInt(mWapMsgId).array();
            bytes[2] = bs[2];
            bytes[3] = bs[3];
            IccFileRequest simInfo = new IccFileRequest(IccConstants.EF_GID1, 1, 2, "3F007F25", bytes, -1, null);
            updateSimInfo(simInfo);
        }
        return mWapMsgId;
    }

    public List<byte[]> getSmsPara() {
        Rlog.d(TAG, "getSmsPara");
        return (ArrayList) this.mData.get(SMSP);
    }
}
