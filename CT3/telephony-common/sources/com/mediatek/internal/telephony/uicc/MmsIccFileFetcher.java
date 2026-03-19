package com.mediatek.internal.telephony.uicc;

import android.content.Context;
import com.android.internal.telephony.Phone;
import com.mediatek.internal.telephony.MmsConfigInfo;
import com.mediatek.internal.telephony.MmsIcpInfo;
import com.mediatek.internal.telephony.ppl.PplMessageManager;
import java.util.ArrayList;

public final class MmsIccFileFetcher extends IccFileFetcherBase {
    private static final String MMS_CONFIG_INFO = "ef_mms_config_info";
    private static final int MMS_ICP_AI_TAG = 133;
    private static final int MMS_ICP_AM_TAG = 132;
    private static final int MMS_ICP_CP_TAG = 171;
    private static final int MMS_ICP_G_TAG = 131;
    private static final int MMS_ICP_ICBI_TAG = 130;
    private static final String MMS_ICP_INFO = "ef_mms_icp_info";
    private static final int MMS_ICP_INVALID_TAG = 255;
    private static final int MMS_ICP_I_TAG = 128;
    private static final int MMS_ICP_RS_TAG = 129;
    private static final String TAG = "MmsIccFileFetcher";
    ArrayList<String> mFileList;

    public MmsIccFileFetcher(Context c, Phone phone) {
        super(c, phone);
        this.mFileList = new ArrayList<>();
        this.mFileList.add(MMS_ICP_INFO);
        this.mFileList.add(MMS_CONFIG_INFO);
    }

    @Override
    public ArrayList<String> onGetKeys() {
        return this.mFileList;
    }

    @Override
    public IccFileRequest onGetFilePara(String key) {
        if (!key.equals(MMS_ICP_INFO)) {
            if (key.equals(MMS_CONFIG_INFO)) {
                return new IccFileRequest(28542, 1, 0, "3F007F25", null, -1, null);
            }
            return null;
        }
        return new IccFileRequest(28519, 1, 0, "3F007F25", null, -1, null);
    }

    String dumpBytes(byte[] data) {
        String ret = UsimPBMemInfo.STRING_NOT_SET;
        for (int i = 0; i < data.length; i++) {
            ret = (ret + Integer.toHexString((data[i] & 240) >> 4)) + Integer.toHexString(data[i] & 15);
        }
        return ret;
    }

    void decodeGateWay(MmsIcpInfo info, byte[] data, int start, int len) {
        if (info == null) {
            return;
        }
        int pos = start;
        while (pos < start + len && (data[pos] & PplMessageManager.Type.INVALID) != 0) {
            pos++;
        }
        String gateWay = new String(data, start, pos - start);
        log("parseMmsIcpInfo decodeGateWay gateWay = " + gateWay.trim());
        info.mDomainName = gateWay.trim();
    }

    void decodeMmsImplementation(MmsIcpInfo info, byte[] data, int start, int len) {
        if (info == null) {
            return;
        }
        int type = data[start] & 255;
        if ((type & 1) == 1) {
            info.mImplementation = "WAP";
        } else if ((type & 2) == 2) {
            info.mImplementation = "M-IMAP";
        } else if ((type & 4) == 4) {
            info.mImplementation = "SIP";
        } else {
            info.mImplementation = "UNKNOWN";
        }
        log("parseMmsIcpInfo decodeMmsImplementation imp = " + info.mImplementation);
    }

    boolean isValideIcpInfo(MmsIcpInfo info) {
        if (info.mImplementation == null || info.mImplementation.isEmpty()) {
            log("parseMmsIcpInfo isValide = false");
            return false;
        }
        log("parseMmsIcpInfo isValide = true");
        return true;
    }

    MmsIcpInfo parseMmsIcpInfo(byte[] data) {
        if (data == null) {
            return null;
        }
        log("parseMmsIcpInfo data = " + dumpBytes(data));
        int pos = 0;
        MmsIcpInfo icpInfo = new MmsIcpInfo();
        while (pos < data.length) {
            int tagParam = data[pos] & PplMessageManager.Type.INVALID;
            int paramLen = data[pos + 1] & PplMessageManager.Type.INVALID;
            if (tagParam == 255 || tagParam == 0) {
                log("parseMmsIcpInfo invalid tagParam: " + tagParam);
                if (isValideIcpInfo(icpInfo)) {
                    return null;
                }
                return icpInfo;
            }
            switch (tagParam) {
                case 128:
                    decodeMmsImplementation(icpInfo, data, pos + 2, 1);
                    pos += 3;
                    break;
                case 129:
                    String tempStr = new String(data, pos + 2, paramLen);
                    icpInfo.mRelayOrServerAddress = tempStr;
                    pos = pos + 2 + paramLen;
                    log("parseMmsIcpInfo, MMS_ICP_RS_TAG value = " + tempStr);
                    break;
                case 130:
                    String tempStr2 = new String(data, pos + 2, paramLen);
                    pos = pos + 2 + paramLen;
                    log("parseMmsIcpInfo, MMS_ICP_ICBI_TAG value = " + tempStr2);
                    break;
                case 131:
                    decodeGateWay(icpInfo, data, pos + 2, paramLen);
                    pos = pos + 2 + paramLen;
                    break;
                case 132:
                    String tempStr3 = new String(data, pos + 2, paramLen);
                    pos = pos + 2 + paramLen;
                    log("parseMmsIcpInfo, MMS_ICP_AM_TAG value = " + tempStr3);
                    break;
                case 133:
                    String tempStr4 = new String(data, pos + 2, paramLen);
                    pos = pos + 2 + paramLen;
                    log("parseMmsIcpInfo, MMS_ICP_AI_TAG value = " + tempStr4);
                    break;
                case 171:
                    pos += 2;
                    break;
                default:
                    log("unkonwn tag.");
                    break;
            }
        }
        if (isValideIcpInfo(icpInfo)) {
        }
    }

    MmsConfigInfo parseMmsConfigInfo(byte[] data) {
        if (data == null || data.length < 8) {
            return null;
        }
        log("parseMmsConfigInfo data = " + dumpBytes(data));
        MmsConfigInfo info = new MmsConfigInfo();
        info.mMessageMaxSize = ((data[0] & PplMessageManager.Type.INVALID) << 24) | ((data[1] & PplMessageManager.Type.INVALID) << 16) | ((data[2] & PplMessageManager.Type.INVALID) << 8) | (data[3] & PplMessageManager.Type.INVALID);
        info.mRetryTimes = data[4] & PplMessageManager.Type.INVALID;
        info.mRetryInterval = data[5] & PplMessageManager.Type.INVALID;
        info.mCenterTimeout = ((data[6] & PplMessageManager.Type.INVALID) << 8) | (data[7] & PplMessageManager.Type.INVALID);
        log("parseMmsConfigInfo: mMessageMaxSize = " + info.mMessageMaxSize + " mRetryTimes = " + info.mRetryTimes + " mRetryInterval = " + info.mRetryInterval + " mCenterTimeout = " + info.mCenterTimeout);
        return info;
    }

    @Override
    public void onParseResult(String key, byte[] transparent, ArrayList<byte[]> linearfixed) {
        log("KEY = " + key + " transparent = " + transparent + " linearfixed = " + linearfixed);
        if (!key.equals(MMS_ICP_INFO)) {
            if (key.equals(MMS_CONFIG_INFO)) {
                this.mData.put(MMS_CONFIG_INFO, parseMmsConfigInfo(transparent));
                return;
            } else {
                loge("unknown key type.");
                return;
            }
        }
        this.mData.put(MMS_ICP_INFO, parseMmsIcpInfo(transparent));
    }

    protected MmsConfigInfo getMmsConfigInfo() {
        if (this.mData.containsKey(MMS_CONFIG_INFO)) {
            return (MmsConfigInfo) this.mData.get(MMS_CONFIG_INFO);
        }
        return null;
    }

    protected MmsIcpInfo getMmsIcpInfo() {
        if (this.mData.containsKey(MMS_ICP_INFO)) {
            return (MmsIcpInfo) this.mData.get(MMS_ICP_INFO);
        }
        return null;
    }
}
