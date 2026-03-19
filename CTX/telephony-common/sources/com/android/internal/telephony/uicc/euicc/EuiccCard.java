package com.android.internal.telephony.uicc.euicc;

import android.R;
import android.content.Context;
import android.content.res.Resources;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Registrant;
import android.os.RegistrantList;
import android.service.carrier.CarrierIdentifier;
import android.service.euicc.EuiccProfileInfo;
import android.telephony.Rlog;
import android.telephony.SubscriptionInfo;
import android.telephony.UiccAccessRule;
import android.telephony.euicc.EuiccNotification;
import android.telephony.euicc.EuiccRulesAuthTable;
import android.text.TextUtils;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.uicc.IccCardStatus;
import com.android.internal.telephony.uicc.IccUtils;
import com.android.internal.telephony.uicc.UiccCard;
import com.android.internal.telephony.uicc.asn1.Asn1Decoder;
import com.android.internal.telephony.uicc.asn1.Asn1Node;
import com.android.internal.telephony.uicc.asn1.InvalidAsn1DataException;
import com.android.internal.telephony.uicc.asn1.TagNotFoundException;
import com.android.internal.telephony.uicc.euicc.apdu.ApduException;
import com.android.internal.telephony.uicc.euicc.apdu.ApduSender;
import com.android.internal.telephony.uicc.euicc.apdu.RequestBuilder;
import com.android.internal.telephony.uicc.euicc.apdu.RequestProvider;
import com.android.internal.telephony.uicc.euicc.async.AsyncResultCallback;
import com.android.internal.telephony.uicc.euicc.async.AsyncResultHelper;
import com.google.android.mms.pdu.PduHeaders;
import java.util.Arrays;
import java.util.List;

public class EuiccCard extends UiccCard {
    private static final int APDU_ERROR_SIM_REFRESH = 28416;
    private static final int CODE_NOTHING_TO_DELETE = 1;
    private static final int CODE_NO_RESULT_AVAILABLE = 1;
    private static final int CODE_OK = 0;
    private static final int CODE_PROFILE_NOT_IN_EXPECTED_STATE = 2;
    private static final boolean DBG = true;
    private static final String DEV_CAP_CDMA_1X = "cdma1x";
    private static final String DEV_CAP_CRL = "crl";
    private static final String DEV_CAP_EHRPD = "ehrpd";
    private static final String DEV_CAP_EUTRAN = "eutran";
    private static final String DEV_CAP_GSM = "gsm";
    private static final String DEV_CAP_HRPD = "hrpd";
    private static final String DEV_CAP_NFC = "nfc";
    private static final String DEV_CAP_UTRAN = "utran";
    private static final int ICCID_LENGTH = 20;
    private static final String ISD_R_AID = "A0000005591010FFFFFFFF8900000100";
    private static final String LOG_TAG = "EuiccCard";
    private static final EuiccSpecVersion SGP_2_0 = new EuiccSpecVersion(2, 0, 0);
    private final ApduSender mApduSender;
    private volatile String mEid;
    private RegistrantList mEidReadyRegistrants;
    private final Object mLock;
    private EuiccSpecVersion mSpecVersion;

    private interface ApduExceptionHandler {
        void handleException(Throwable th);
    }

    private interface ApduRequestBuilder {
        void build(RequestBuilder requestBuilder) throws TagNotFoundException, InvalidAsn1DataException, EuiccCardException;
    }

    private interface ApduResponseHandler<T> {
        T handleResult(byte[] bArr) throws TagNotFoundException, InvalidAsn1DataException, EuiccCardException;
    }

    public EuiccCard(Context context, CommandsInterface commandsInterface, IccCardStatus iccCardStatus, int i, Object obj) {
        super(context, commandsInterface, iccCardStatus, i, obj);
        this.mLock = new Object();
        this.mApduSender = new ApduSender(commandsInterface, ISD_R_AID, false);
        loadEidAndNotifyRegistrants();
    }

    public void registerForEidReady(Handler handler, int i, Object obj) {
        Registrant registrant = new Registrant(handler, i, obj);
        if (this.mEid != null) {
            registrant.notifyRegistrant(new AsyncResult((Object) null, (Object) null, (Throwable) null));
            return;
        }
        if (this.mEidReadyRegistrants == null) {
            this.mEidReadyRegistrants = new RegistrantList();
        }
        this.mEidReadyRegistrants.add(registrant);
    }

    public void unregisterForEidReady(Handler handler) {
        if (this.mEidReadyRegistrants != null) {
            this.mEidReadyRegistrants.remove(handler);
        }
    }

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PRIVATE)
    protected void loadEidAndNotifyRegistrants() {
        getEid(new AsyncResultCallback<String>() {
            @Override
            public void onResult(String str) {
                if (EuiccCard.this.mEidReadyRegistrants != null) {
                    EuiccCard.this.mEidReadyRegistrants.notifyRegistrants(new AsyncResult((Object) null, (Object) null, (Throwable) null));
                }
            }

            @Override
            public void onException(Throwable th) {
                if (EuiccCard.this.mEidReadyRegistrants != null) {
                    EuiccCard.this.mEidReadyRegistrants.notifyRegistrants(new AsyncResult((Object) null, (Object) null, (Throwable) null));
                }
                EuiccCard.this.mEid = "";
                EuiccCard.this.mCardId = "";
                Rlog.e(EuiccCard.LOG_TAG, "Failed loading eid", th);
            }
        }, new Handler());
    }

    public void getSpecVersion(AsyncResultCallback<EuiccSpecVersion> asyncResultCallback, Handler handler) {
        if (this.mSpecVersion != null) {
            AsyncResultHelper.returnResult(this.mSpecVersion, asyncResultCallback, handler);
        } else {
            sendApdu(newRequestProvider(new ApduRequestBuilder() {
                @Override
                public final void build(RequestBuilder requestBuilder) throws TagNotFoundException, InvalidAsn1DataException, EuiccCardException {
                    EuiccCard.lambda$getSpecVersion$0(requestBuilder);
                }
            }), new ApduResponseHandler() {
                @Override
                public final Object handleResult(byte[] bArr) {
                    return this.f$0.mSpecVersion;
                }
            }, asyncResultCallback, handler);
        }
    }

    static void lambda$getSpecVersion$0(RequestBuilder requestBuilder) throws TagNotFoundException, InvalidAsn1DataException, EuiccCardException {
    }

    @Override
    protected void updateCardId() {
        if (TextUtils.isEmpty(this.mEid)) {
            super.updateCardId();
        } else {
            this.mCardId = this.mEid;
        }
    }

    public void getAllProfiles(AsyncResultCallback<EuiccProfileInfo[]> asyncResultCallback, Handler handler) {
        sendApdu(newRequestProvider(new ApduRequestBuilder() {
            @Override
            public final void build(RequestBuilder requestBuilder) {
                requestBuilder.addStoreData(Asn1Node.newBuilder(48941).addChildAsBytes(92, Tags.EUICC_PROFILE_TAGS).build().toHex());
            }
        }), new ApduResponseHandler() {
            @Override
            public final Object handleResult(byte[] bArr) {
                return EuiccCard.lambda$getAllProfiles$3(bArr);
            }
        }, asyncResultCallback, handler);
    }

    static EuiccProfileInfo[] lambda$getAllProfiles$3(byte[] bArr) throws TagNotFoundException, InvalidAsn1DataException, EuiccCardException {
        List children = new Asn1Decoder(bArr).nextNode().getChild(160, new int[0]).getChildren(227);
        int size = children.size();
        EuiccProfileInfo[] euiccProfileInfoArr = new EuiccProfileInfo[size];
        int i = 0;
        for (int i2 = 0; i2 < size; i2++) {
            Asn1Node asn1Node = (Asn1Node) children.get(i2);
            if (!asn1Node.hasChild(90, new int[0])) {
                loge("Profile must have an ICCID.");
            } else {
                EuiccProfileInfo.Builder builder = new EuiccProfileInfo.Builder(stripTrailingFs(asn1Node.getChild(90, new int[0]).asBytes()));
                buildProfile(asn1Node, builder);
                euiccProfileInfoArr[i] = builder.build();
                i++;
            }
        }
        return euiccProfileInfoArr;
    }

    public final void getProfile(final String str, AsyncResultCallback<EuiccProfileInfo> asyncResultCallback, Handler handler) {
        sendApdu(newRequestProvider(new ApduRequestBuilder() {
            @Override
            public final void build(RequestBuilder requestBuilder) {
                requestBuilder.addStoreData(Asn1Node.newBuilder(48941).addChild(Asn1Node.newBuilder(160).addChildAsBytes(90, IccUtils.bcdToBytes(EuiccCard.padTrailingFs(str))).build()).addChildAsBytes(92, Tags.EUICC_PROFILE_TAGS).build().toHex());
            }
        }), new ApduResponseHandler() {
            @Override
            public final Object handleResult(byte[] bArr) {
                return EuiccCard.lambda$getProfile$5(bArr);
            }
        }, asyncResultCallback, handler);
    }

    static EuiccProfileInfo lambda$getProfile$5(byte[] bArr) throws TagNotFoundException, InvalidAsn1DataException, EuiccCardException {
        List children = new Asn1Decoder(bArr).nextNode().getChild(160, new int[0]).getChildren(227);
        if (children.isEmpty()) {
            return null;
        }
        Asn1Node asn1Node = (Asn1Node) children.get(0);
        EuiccProfileInfo.Builder builder = new EuiccProfileInfo.Builder(stripTrailingFs(asn1Node.getChild(90, new int[0]).asBytes()));
        buildProfile(asn1Node, builder);
        return builder.build();
    }

    public void disableProfile(final String str, final boolean z, AsyncResultCallback<Void> asyncResultCallback, Handler handler) {
        sendApduWithSimResetErrorWorkaround(newRequestProvider(new ApduRequestBuilder() {
            @Override
            public final void build(RequestBuilder requestBuilder) {
                requestBuilder.addStoreData(Asn1Node.newBuilder(48946).addChild(Asn1Node.newBuilder(160).addChildAsBytes(90, IccUtils.bcdToBytes(EuiccCard.padTrailingFs(str)))).addChildAsBoolean(129, z).build().toHex());
            }
        }), new ApduResponseHandler() {
            @Override
            public final Object handleResult(byte[] bArr) {
                return EuiccCard.lambda$disableProfile$7(str, bArr);
            }
        }, asyncResultCallback, handler);
    }

    static Void lambda$disableProfile$7(String str, byte[] bArr) throws TagNotFoundException, InvalidAsn1DataException, EuiccCardException {
        int simpleResult = parseSimpleResult(bArr);
        if (simpleResult == 0) {
            return null;
        }
        if (simpleResult == 2) {
            logd("Profile is already disabled, iccid: " + SubscriptionInfo.givePrintableIccid(str));
            return null;
        }
        throw new EuiccCardErrorException(11, simpleResult);
    }

    public void switchToProfile(final String str, final boolean z, AsyncResultCallback<Void> asyncResultCallback, Handler handler) {
        sendApduWithSimResetErrorWorkaround(newRequestProvider(new ApduRequestBuilder() {
            @Override
            public final void build(RequestBuilder requestBuilder) {
                requestBuilder.addStoreData(Asn1Node.newBuilder(48945).addChild(Asn1Node.newBuilder(160).addChildAsBytes(90, IccUtils.bcdToBytes(EuiccCard.padTrailingFs(str)))).addChildAsBoolean(129, z).build().toHex());
            }
        }), new ApduResponseHandler() {
            @Override
            public final Object handleResult(byte[] bArr) {
                return EuiccCard.lambda$switchToProfile$9(str, bArr);
            }
        }, asyncResultCallback, handler);
    }

    static Void lambda$switchToProfile$9(String str, byte[] bArr) throws TagNotFoundException, InvalidAsn1DataException, EuiccCardException {
        int simpleResult = parseSimpleResult(bArr);
        if (simpleResult == 0) {
            return null;
        }
        if (simpleResult == 2) {
            logd("Profile is already enabled, iccid: " + SubscriptionInfo.givePrintableIccid(str));
            return null;
        }
        throw new EuiccCardErrorException(10, simpleResult);
    }

    public String getEid() {
        return this.mEid;
    }

    public void getEid(AsyncResultCallback<String> asyncResultCallback, Handler handler) {
        if (this.mEid != null) {
            AsyncResultHelper.returnResult(this.mEid, asyncResultCallback, handler);
        } else {
            sendApdu(newRequestProvider(new ApduRequestBuilder() {
                @Override
                public final void build(RequestBuilder requestBuilder) {
                    requestBuilder.addStoreData(Asn1Node.newBuilder(48958).addChildAsBytes(92, new byte[]{90}).build().toHex());
                }
            }), new ApduResponseHandler() {
                @Override
                public final Object handleResult(byte[] bArr) {
                    return EuiccCard.lambda$getEid$11(this.f$0, bArr);
                }
            }, asyncResultCallback, handler);
        }
    }

    public static String lambda$getEid$11(EuiccCard euiccCard, byte[] bArr) throws TagNotFoundException, InvalidAsn1DataException, EuiccCardException {
        String strBytesToHexString = IccUtils.bytesToHexString(parseResponse(bArr).getChild(90, new int[0]).asBytes());
        synchronized (euiccCard.mLock) {
            euiccCard.mEid = strBytesToHexString;
            euiccCard.mCardId = strBytesToHexString;
        }
        return strBytesToHexString;
    }

    public void setNickname(final String str, final String str2, AsyncResultCallback<Void> asyncResultCallback, Handler handler) {
        sendApdu(newRequestProvider(new ApduRequestBuilder() {
            @Override
            public final void build(RequestBuilder requestBuilder) {
                requestBuilder.addStoreData(Asn1Node.newBuilder(48937).addChildAsBytes(90, IccUtils.bcdToBytes(EuiccCard.padTrailingFs(str))).addChildAsString(144, str2).build().toHex());
            }
        }), new ApduResponseHandler() {
            @Override
            public final Object handleResult(byte[] bArr) {
                return EuiccCard.lambda$setNickname$13(bArr);
            }
        }, asyncResultCallback, handler);
    }

    static Void lambda$setNickname$13(byte[] bArr) throws TagNotFoundException, InvalidAsn1DataException, EuiccCardException {
        int simpleResult = parseSimpleResult(bArr);
        if (simpleResult != 0) {
            throw new EuiccCardErrorException(7, simpleResult);
        }
        return null;
    }

    public void deleteProfile(final String str, AsyncResultCallback<Void> asyncResultCallback, Handler handler) {
        sendApdu(newRequestProvider(new ApduRequestBuilder() {
            @Override
            public final void build(RequestBuilder requestBuilder) {
                requestBuilder.addStoreData(Asn1Node.newBuilder(48947).addChildAsBytes(90, IccUtils.bcdToBytes(EuiccCard.padTrailingFs(str))).build().toHex());
            }
        }), new ApduResponseHandler() {
            @Override
            public final Object handleResult(byte[] bArr) {
                return EuiccCard.lambda$deleteProfile$15(bArr);
            }
        }, asyncResultCallback, handler);
    }

    static Void lambda$deleteProfile$15(byte[] bArr) throws TagNotFoundException, InvalidAsn1DataException, EuiccCardException {
        int simpleResult = parseSimpleResult(bArr);
        if (simpleResult != 0) {
            throw new EuiccCardErrorException(12, simpleResult);
        }
        return null;
    }

    public void resetMemory(final int i, AsyncResultCallback<Void> asyncResultCallback, Handler handler) {
        sendApduWithSimResetErrorWorkaround(newRequestProvider(new ApduRequestBuilder() {
            @Override
            public final void build(RequestBuilder requestBuilder) {
                requestBuilder.addStoreData(Asn1Node.newBuilder(48948).addChildAsBits(130, i).build().toHex());
            }
        }), new ApduResponseHandler() {
            @Override
            public final Object handleResult(byte[] bArr) {
                return EuiccCard.lambda$resetMemory$17(bArr);
            }
        }, asyncResultCallback, handler);
    }

    static Void lambda$resetMemory$17(byte[] bArr) throws TagNotFoundException, InvalidAsn1DataException, EuiccCardException {
        int simpleResult = parseSimpleResult(bArr);
        if (simpleResult != 0 && simpleResult != 1) {
            throw new EuiccCardErrorException(13, simpleResult);
        }
        return null;
    }

    public void getDefaultSmdpAddress(AsyncResultCallback<String> asyncResultCallback, Handler handler) {
        sendApdu(newRequestProvider(new ApduRequestBuilder() {
            @Override
            public final void build(RequestBuilder requestBuilder) {
                requestBuilder.addStoreData(Asn1Node.newBuilder(48956).build().toHex());
            }
        }), new ApduResponseHandler() {
            @Override
            public final Object handleResult(byte[] bArr) {
                return EuiccCard.parseResponse(bArr).getChild(128, new int[0]).asString();
            }
        }, asyncResultCallback, handler);
    }

    public void getSmdsAddress(AsyncResultCallback<String> asyncResultCallback, Handler handler) {
        sendApdu(newRequestProvider(new ApduRequestBuilder() {
            @Override
            public final void build(RequestBuilder requestBuilder) {
                requestBuilder.addStoreData(Asn1Node.newBuilder(48956).build().toHex());
            }
        }), new ApduResponseHandler() {
            @Override
            public final Object handleResult(byte[] bArr) {
                return EuiccCard.parseResponse(bArr).getChild(129, new int[0]).asString();
            }
        }, asyncResultCallback, handler);
    }

    public void setDefaultSmdpAddress(final String str, AsyncResultCallback<Void> asyncResultCallback, Handler handler) {
        sendApdu(newRequestProvider(new ApduRequestBuilder() {
            @Override
            public final void build(RequestBuilder requestBuilder) {
                requestBuilder.addStoreData(Asn1Node.newBuilder(48959).addChildAsString(128, str).build().toHex());
            }
        }), new ApduResponseHandler() {
            @Override
            public final Object handleResult(byte[] bArr) {
                return EuiccCard.lambda$setDefaultSmdpAddress$23(bArr);
            }
        }, asyncResultCallback, handler);
    }

    static Void lambda$setDefaultSmdpAddress$23(byte[] bArr) throws TagNotFoundException, InvalidAsn1DataException, EuiccCardException {
        int simpleResult = parseSimpleResult(bArr);
        if (simpleResult != 0) {
            throw new EuiccCardErrorException(14, simpleResult);
        }
        return null;
    }

    public void getRulesAuthTable(AsyncResultCallback<EuiccRulesAuthTable> asyncResultCallback, Handler handler) {
        sendApdu(newRequestProvider(new ApduRequestBuilder() {
            @Override
            public final void build(RequestBuilder requestBuilder) {
                requestBuilder.addStoreData(Asn1Node.newBuilder(48963).build().toHex());
            }
        }), new ApduResponseHandler() {
            @Override
            public final Object handleResult(byte[] bArr) {
                return EuiccCard.lambda$getRulesAuthTable$25(bArr);
            }
        }, asyncResultCallback, handler);
    }

    static EuiccRulesAuthTable lambda$getRulesAuthTable$25(byte[] bArr) throws TagNotFoundException, InvalidAsn1DataException, EuiccCardException {
        List children = parseResponse(bArr).getChildren(160);
        EuiccRulesAuthTable.Builder builder = new EuiccRulesAuthTable.Builder(children.size());
        int size = children.size();
        for (int i = 0; i < size; i++) {
            Asn1Node asn1Node = (Asn1Node) children.get(i);
            List children2 = asn1Node.getChild(PduHeaders.PREVIOUSLY_SENT_DATE, new int[0]).getChildren();
            int size2 = children2.size();
            CarrierIdentifier[] carrierIdentifierArr = new CarrierIdentifier[size2];
            for (int i2 = 0; i2 < size2; i2++) {
                carrierIdentifierArr[i2] = buildCarrierIdentifier((Asn1Node) children2.get(i2));
            }
            builder.add(asn1Node.getChild(128, new int[0]).asBits(), Arrays.asList(carrierIdentifierArr), asn1Node.getChild(130, new int[0]).asBits());
        }
        return builder.build();
    }

    public void getEuiccChallenge(AsyncResultCallback<byte[]> asyncResultCallback, Handler handler) {
        sendApdu(newRequestProvider(new ApduRequestBuilder() {
            @Override
            public final void build(RequestBuilder requestBuilder) {
                requestBuilder.addStoreData(Asn1Node.newBuilder(48942).build().toHex());
            }
        }), new ApduResponseHandler() {
            @Override
            public final Object handleResult(byte[] bArr) {
                return EuiccCard.parseResponse(bArr).getChild(128, new int[0]).asBytes();
            }
        }, asyncResultCallback, handler);
    }

    public void getEuiccInfo1(AsyncResultCallback<byte[]> asyncResultCallback, Handler handler) {
        sendApdu(newRequestProvider(new ApduRequestBuilder() {
            @Override
            public final void build(RequestBuilder requestBuilder) {
                requestBuilder.addStoreData(Asn1Node.newBuilder(48928).build().toHex());
            }
        }), new ApduResponseHandler() {
            @Override
            public final Object handleResult(byte[] bArr) {
                return EuiccCard.lambda$getEuiccInfo1$29(bArr);
            }
        }, asyncResultCallback, handler);
    }

    static byte[] lambda$getEuiccInfo1$29(byte[] bArr) throws TagNotFoundException, InvalidAsn1DataException, EuiccCardException {
        return bArr;
    }

    public void getEuiccInfo2(AsyncResultCallback<byte[]> asyncResultCallback, Handler handler) {
        sendApdu(newRequestProvider(new ApduRequestBuilder() {
            @Override
            public final void build(RequestBuilder requestBuilder) {
                requestBuilder.addStoreData(Asn1Node.newBuilder(48930).build().toHex());
            }
        }), new ApduResponseHandler() {
            @Override
            public final Object handleResult(byte[] bArr) {
                return EuiccCard.lambda$getEuiccInfo2$31(bArr);
            }
        }, asyncResultCallback, handler);
    }

    static byte[] lambda$getEuiccInfo2$31(byte[] bArr) throws TagNotFoundException, InvalidAsn1DataException, EuiccCardException {
        return bArr;
    }

    public void authenticateServer(final String str, final byte[] bArr, final byte[] bArr2, final byte[] bArr3, final byte[] bArr4, AsyncResultCallback<byte[]> asyncResultCallback, Handler handler) {
        sendApdu(newRequestProvider(new ApduRequestBuilder() {
            @Override
            public final void build(RequestBuilder requestBuilder) throws TagNotFoundException, InvalidAsn1DataException, EuiccCardException {
                EuiccCard.lambda$authenticateServer$32(this.f$0, str, bArr, bArr2, bArr3, bArr4, requestBuilder);
            }
        }), new ApduResponseHandler() {
            @Override
            public final Object handleResult(byte[] bArr5) {
                return EuiccCard.parseResponseAndCheckSimpleError(bArr5, 3).toBytes();
            }
        }, asyncResultCallback, handler);
    }

    public static void lambda$authenticateServer$32(EuiccCard euiccCard, String str, byte[] bArr, byte[] bArr2, byte[] bArr3, byte[] bArr4, RequestBuilder requestBuilder) throws TagNotFoundException, InvalidAsn1DataException, EuiccCardException {
        byte[] deviceId = euiccCard.getDeviceId();
        byte[] bArr5 = new byte[4];
        System.arraycopy(deviceId, 0, bArr5, 0, 4);
        Asn1Node.Builder builderNewBuilder = Asn1Node.newBuilder(PduHeaders.PREVIOUSLY_SENT_DATE);
        String[] stringArray = euiccCard.getResources().getStringArray(R.array.config_deviceStatesOnWhichToSleep);
        if (stringArray != null) {
            for (String str2 : stringArray) {
                euiccCard.addDeviceCapability(builderNewBuilder, str2);
            }
        } else {
            logd("No device capabilities set.");
        }
        requestBuilder.addStoreData(Asn1Node.newBuilder(48952).addChild(new Asn1Decoder(bArr).nextNode()).addChild(new Asn1Decoder(bArr2).nextNode()).addChild(new Asn1Decoder(bArr3).nextNode()).addChild(new Asn1Decoder(bArr4).nextNode()).addChild(Asn1Node.newBuilder(160).addChildAsString(128, str).addChild(Asn1Node.newBuilder(PduHeaders.PREVIOUSLY_SENT_DATE).addChildAsBytes(128, bArr5).addChild(builderNewBuilder).addChildAsBytes(130, deviceId))).build().toHex());
    }

    public void prepareDownload(final byte[] bArr, final byte[] bArr2, final byte[] bArr3, final byte[] bArr4, AsyncResultCallback<byte[]> asyncResultCallback, Handler handler) {
        sendApdu(newRequestProvider(new ApduRequestBuilder() {
            @Override
            public final void build(RequestBuilder requestBuilder) throws TagNotFoundException, InvalidAsn1DataException, EuiccCardException {
                EuiccCard.lambda$prepareDownload$34(bArr2, bArr3, bArr, bArr4, requestBuilder);
            }
        }), new ApduResponseHandler() {
            @Override
            public final Object handleResult(byte[] bArr5) {
                return EuiccCard.lambda$prepareDownload$35(bArr5);
            }
        }, asyncResultCallback, handler);
    }

    static void lambda$prepareDownload$34(byte[] bArr, byte[] bArr2, byte[] bArr3, byte[] bArr4, RequestBuilder requestBuilder) throws TagNotFoundException, InvalidAsn1DataException, EuiccCardException {
        Asn1Node.Builder builderAddChild = Asn1Node.newBuilder(48929).addChild(new Asn1Decoder(bArr).nextNode()).addChild(new Asn1Decoder(bArr2).nextNode());
        if (bArr3 != null) {
            builderAddChild.addChildAsBytes(4, bArr3);
        }
        requestBuilder.addStoreData(builderAddChild.addChild(new Asn1Decoder(bArr4).nextNode()).build().toHex());
    }

    static byte[] lambda$prepareDownload$35(byte[] bArr) throws TagNotFoundException, InvalidAsn1DataException, EuiccCardException {
        Asn1Node response = parseResponse(bArr);
        if (response.hasChild(PduHeaders.PREVIOUSLY_SENT_DATE, new int[]{2})) {
            throw new EuiccCardErrorException(2, response.getChild(PduHeaders.PREVIOUSLY_SENT_DATE, new int[]{2}).asInteger());
        }
        return response.toBytes();
    }

    public void loadBoundProfilePackage(final byte[] bArr, AsyncResultCallback<byte[]> asyncResultCallback, Handler handler) {
        sendApdu(newRequestProvider(new ApduRequestBuilder() {
            @Override
            public final void build(RequestBuilder requestBuilder) throws TagNotFoundException, InvalidAsn1DataException, EuiccCardException {
                EuiccCard.lambda$loadBoundProfilePackage$36(bArr, requestBuilder);
            }
        }), new ApduResponseHandler() {
            @Override
            public final Object handleResult(byte[] bArr2) {
                return EuiccCard.lambda$loadBoundProfilePackage$37(bArr2);
            }
        }, asyncResultCallback, handler);
    }

    static void lambda$loadBoundProfilePackage$36(byte[] bArr, RequestBuilder requestBuilder) throws TagNotFoundException, InvalidAsn1DataException, EuiccCardException {
        Asn1Node asn1NodeNextNode = new Asn1Decoder(bArr).nextNode();
        Asn1Node child = asn1NodeNextNode.getChild(48931, new int[0]);
        Asn1Node child2 = asn1NodeNextNode.getChild(160, new int[0]);
        Asn1Node child3 = asn1NodeNextNode.getChild(PduHeaders.PREVIOUSLY_SENT_DATE, new int[0]);
        List children = child3.getChildren(136);
        Asn1Node child4 = asn1NodeNextNode.getChild(PduHeaders.MM_STATE, new int[0]);
        List children2 = child4.getChildren(134);
        requestBuilder.addStoreData(asn1NodeNextNode.getHeadAsHex() + child.toHex());
        requestBuilder.addStoreData(child2.toHex());
        requestBuilder.addStoreData(child3.getHeadAsHex());
        int size = children.size();
        for (int i = 0; i < size; i++) {
            requestBuilder.addStoreData(((Asn1Node) children.get(i)).toHex());
        }
        if (asn1NodeNextNode.hasChild(PduHeaders.STORE, new int[0])) {
            requestBuilder.addStoreData(asn1NodeNextNode.getChild(PduHeaders.STORE, new int[0]).toHex());
        }
        requestBuilder.addStoreData(child4.getHeadAsHex());
        int size2 = children2.size();
        for (int i2 = 0; i2 < size2; i2++) {
            requestBuilder.addStoreData(((Asn1Node) children2.get(i2)).toHex());
        }
    }

    static byte[] lambda$loadBoundProfilePackage$37(byte[] bArr) throws TagNotFoundException, InvalidAsn1DataException, EuiccCardException {
        Asn1Node response = parseResponse(bArr);
        if (response.hasChild(48935, new int[]{PduHeaders.STORE, PduHeaders.PREVIOUSLY_SENT_DATE, 129})) {
            Asn1Node child = response.getChild(48935, new int[]{PduHeaders.STORE, PduHeaders.PREVIOUSLY_SENT_DATE, 129});
            throw new EuiccCardErrorException(5, child.asInteger(), child);
        }
        return response.toBytes();
    }

    public void cancelSession(final byte[] bArr, final int i, AsyncResultCallback<byte[]> asyncResultCallback, Handler handler) {
        sendApdu(newRequestProvider(new ApduRequestBuilder() {
            @Override
            public final void build(RequestBuilder requestBuilder) {
                requestBuilder.addStoreData(Asn1Node.newBuilder(48961).addChildAsBytes(128, bArr).addChildAsInteger(129, i).build().toHex());
            }
        }), new ApduResponseHandler() {
            @Override
            public final Object handleResult(byte[] bArr2) {
                return EuiccCard.parseResponseAndCheckSimpleError(bArr2, 4).toBytes();
            }
        }, asyncResultCallback, handler);
    }

    public void listNotifications(final int i, AsyncResultCallback<EuiccNotification[]> asyncResultCallback, Handler handler) {
        sendApdu(newRequestProvider(new ApduRequestBuilder() {
            @Override
            public final void build(RequestBuilder requestBuilder) {
                requestBuilder.addStoreData(Asn1Node.newBuilder(48936).addChildAsBits(129, i).build().toHex());
            }
        }), new ApduResponseHandler() {
            @Override
            public final Object handleResult(byte[] bArr) {
                return EuiccCard.lambda$listNotifications$41(bArr);
            }
        }, asyncResultCallback, handler);
    }

    static EuiccNotification[] lambda$listNotifications$41(byte[] bArr) throws TagNotFoundException, InvalidAsn1DataException, EuiccCardException {
        List children = parseResponseAndCheckSimpleError(bArr, 6).getChild(160, new int[0]).getChildren();
        EuiccNotification[] euiccNotificationArr = new EuiccNotification[children.size()];
        for (int i = 0; i < euiccNotificationArr.length; i++) {
            euiccNotificationArr[i] = createNotification((Asn1Node) children.get(i));
        }
        return euiccNotificationArr;
    }

    public void retrieveNotificationList(final int i, AsyncResultCallback<EuiccNotification[]> asyncResultCallback, Handler handler) {
        sendApdu(newRequestProvider(new ApduRequestBuilder() {
            @Override
            public final void build(RequestBuilder requestBuilder) {
                requestBuilder.addStoreData(Asn1Node.newBuilder(48939).addChild(Asn1Node.newBuilder(160).addChildAsBits(129, i)).build().toHex());
            }
        }), new ApduResponseHandler() {
            @Override
            public final Object handleResult(byte[] bArr) {
                return EuiccCard.lambda$retrieveNotificationList$43(bArr);
            }
        }, asyncResultCallback, handler);
    }

    static EuiccNotification[] lambda$retrieveNotificationList$43(byte[] bArr) throws TagNotFoundException, InvalidAsn1DataException, EuiccCardException {
        Asn1Node response = parseResponse(bArr);
        if (response.hasChild(129, new int[0])) {
            int iAsInteger = response.getChild(129, new int[0]).asInteger();
            if (iAsInteger == 1) {
                return new EuiccNotification[0];
            }
            throw new EuiccCardErrorException(8, iAsInteger);
        }
        List children = response.getChild(160, new int[0]).getChildren();
        EuiccNotification[] euiccNotificationArr = new EuiccNotification[children.size()];
        for (int i = 0; i < euiccNotificationArr.length; i++) {
            euiccNotificationArr[i] = createNotification((Asn1Node) children.get(i));
        }
        return euiccNotificationArr;
    }

    public void retrieveNotification(final int i, AsyncResultCallback<EuiccNotification> asyncResultCallback, Handler handler) {
        sendApdu(newRequestProvider(new ApduRequestBuilder() {
            @Override
            public final void build(RequestBuilder requestBuilder) {
                requestBuilder.addStoreData(Asn1Node.newBuilder(48939).addChild(Asn1Node.newBuilder(160).addChildAsInteger(128, i)).build().toHex());
            }
        }), new ApduResponseHandler() {
            @Override
            public final Object handleResult(byte[] bArr) {
                return EuiccCard.lambda$retrieveNotification$45(bArr);
            }
        }, asyncResultCallback, handler);
    }

    static EuiccNotification lambda$retrieveNotification$45(byte[] bArr) throws TagNotFoundException, InvalidAsn1DataException, EuiccCardException {
        List children = parseResponseAndCheckSimpleError(bArr, 8).getChild(160, new int[0]).getChildren();
        if (children.size() > 0) {
            return createNotification((Asn1Node) children.get(0));
        }
        return null;
    }

    public void removeNotificationFromList(final int i, AsyncResultCallback<Void> asyncResultCallback, Handler handler) {
        sendApdu(newRequestProvider(new ApduRequestBuilder() {
            @Override
            public final void build(RequestBuilder requestBuilder) {
                requestBuilder.addStoreData(Asn1Node.newBuilder(48944).addChildAsInteger(128, i).build().toHex());
            }
        }), new ApduResponseHandler() {
            @Override
            public final Object handleResult(byte[] bArr) {
                return EuiccCard.lambda$removeNotificationFromList$47(bArr);
            }
        }, asyncResultCallback, handler);
    }

    static Void lambda$removeNotificationFromList$47(byte[] bArr) throws TagNotFoundException, InvalidAsn1DataException, EuiccCardException {
        int simpleResult = parseSimpleResult(bArr);
        if (simpleResult != 0 && simpleResult != 1) {
            throw new EuiccCardErrorException(9, simpleResult);
        }
        return null;
    }

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PRIVATE)
    public void addDeviceCapability(Asn1Node.Builder builder, String str) {
        byte[] bArr;
        String[] strArrSplit = str.split(",");
        if (strArrSplit.length != 2) {
            loge("Invalid device capability item: " + Arrays.toString(strArrSplit));
        }
        String strTrim = strArrSplit[0].trim();
        try {
            bArr = new byte[]{Integer.valueOf(Integer.parseInt(strArrSplit[1].trim())).byteValue(), 0, 0};
            switch (strTrim) {
                case "gsm":
                    builder.addChildAsBytes(128, bArr);
                    break;
                case "utran":
                    builder.addChildAsBytes(129, bArr);
                    break;
                case "cdma1x":
                    builder.addChildAsBytes(130, bArr);
                    break;
                case "hrpd":
                    builder.addChildAsBytes(131, bArr);
                    break;
                case "ehrpd":
                    builder.addChildAsBytes(132, bArr);
                    break;
                case "eutran":
                    builder.addChildAsBytes(133, bArr);
                    break;
                case "nfc":
                    builder.addChildAsBytes(134, bArr);
                    break;
                case "crl":
                    builder.addChildAsBytes(135, bArr);
                    break;
                default:
                    loge("Invalid device capability name: " + strTrim);
                    break;
            }
        } catch (NumberFormatException e) {
            loge("Invalid device capability version number.", e);
        }
    }

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PRIVATE)
    protected byte[] getDeviceId() {
        byte[] bArr = new byte[8];
        Phone phone = PhoneFactory.getPhone(getPhoneId());
        if (phone != null) {
            IccUtils.bcdToBytes(phone.getDeviceId(), bArr);
        }
        return bArr;
    }

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PRIVATE)
    protected Resources getResources() {
        return Resources.getSystem();
    }

    private RequestProvider newRequestProvider(final ApduRequestBuilder apduRequestBuilder) {
        return new RequestProvider() {
            @Override
            public final void buildRequest(byte[] bArr, RequestBuilder requestBuilder) throws Throwable {
                EuiccCard.lambda$newRequestProvider$48(this.f$0, apduRequestBuilder, bArr, requestBuilder);
            }
        };
    }

    public static void lambda$newRequestProvider$48(EuiccCard euiccCard, ApduRequestBuilder apduRequestBuilder, byte[] bArr, RequestBuilder requestBuilder) throws Throwable {
        EuiccSpecVersion orExtractSpecVersion = euiccCard.getOrExtractSpecVersion(bArr);
        if (orExtractSpecVersion == null) {
            throw new EuiccCardException("Cannot get eUICC spec version.");
        }
        try {
            if (orExtractSpecVersion.compareTo(SGP_2_0) < 0) {
                throw new EuiccCardException("eUICC spec version is unsupported: " + orExtractSpecVersion);
            }
            apduRequestBuilder.build(requestBuilder);
        } catch (InvalidAsn1DataException | TagNotFoundException e) {
            throw new EuiccCardException("Cannot parse ASN1 to build request.", e);
        }
    }

    private EuiccSpecVersion getOrExtractSpecVersion(byte[] bArr) {
        if (this.mSpecVersion != null) {
            return this.mSpecVersion;
        }
        EuiccSpecVersion euiccSpecVersionFromOpenChannelResponse = EuiccSpecVersion.fromOpenChannelResponse(bArr);
        if (euiccSpecVersionFromOpenChannelResponse != null) {
            synchronized (this.mLock) {
                if (this.mSpecVersion == null) {
                    this.mSpecVersion = euiccSpecVersionFromOpenChannelResponse;
                }
            }
        }
        return euiccSpecVersionFromOpenChannelResponse;
    }

    private <T> void sendApdu(RequestProvider requestProvider, ApduResponseHandler<T> apduResponseHandler, final AsyncResultCallback<T> asyncResultCallback, Handler handler) {
        sendApdu(requestProvider, apduResponseHandler, new ApduExceptionHandler() {
            @Override
            public final void handleException(Throwable th) {
                asyncResultCallback.onException(new EuiccCardException("Cannot send APDU.", th));
            }
        }, asyncResultCallback, handler);
    }

    private void sendApduWithSimResetErrorWorkaround(RequestProvider requestProvider, ApduResponseHandler<Void> apduResponseHandler, final AsyncResultCallback<Void> asyncResultCallback, Handler handler) {
        sendApdu(requestProvider, apduResponseHandler, new ApduExceptionHandler() {
            @Override
            public final void handleException(Throwable th) {
                EuiccCard.lambda$sendApduWithSimResetErrorWorkaround$50(asyncResultCallback, th);
            }
        }, asyncResultCallback, handler);
    }

    static void lambda$sendApduWithSimResetErrorWorkaround$50(AsyncResultCallback asyncResultCallback, Throwable th) {
        if ((th instanceof ApduException) && ((ApduException) th).getApduStatus() == APDU_ERROR_SIM_REFRESH) {
            logi("Sim is refreshed after disabling profile, no response got.");
            asyncResultCallback.onResult(null);
        } else {
            asyncResultCallback.onException(new EuiccCardException("Cannot send APDU.", th));
        }
    }

    private <T> void sendApdu(RequestProvider requestProvider, final ApduResponseHandler<T> apduResponseHandler, final ApduExceptionHandler apduExceptionHandler, final AsyncResultCallback<T> asyncResultCallback, Handler handler) {
        this.mApduSender.send(requestProvider, new AsyncResultCallback<byte[]>() {
            @Override
            public void onResult(byte[] bArr) {
                try {
                    asyncResultCallback.onResult(apduResponseHandler.handleResult(bArr));
                } catch (InvalidAsn1DataException | TagNotFoundException e) {
                    asyncResultCallback.onException(new EuiccCardException("Cannot parse response: " + IccUtils.bytesToHexString(bArr), e));
                } catch (EuiccCardException e2) {
                    asyncResultCallback.onException(e2);
                }
            }

            @Override
            public void onException(Throwable th) {
                apduExceptionHandler.handleException(th);
            }
        }, handler);
    }

    private static void buildProfile(Asn1Node asn1Node, EuiccProfileInfo.Builder builder) throws TagNotFoundException, InvalidAsn1DataException {
        if (asn1Node.hasChild(144, new int[0])) {
            builder.setNickname(asn1Node.getChild(144, new int[0]).asString());
        }
        if (asn1Node.hasChild(145, new int[0])) {
            builder.setServiceProviderName(asn1Node.getChild(145, new int[0]).asString());
        }
        if (asn1Node.hasChild(146, new int[0])) {
            builder.setProfileName(asn1Node.getChild(146, new int[0]).asString());
        }
        if (asn1Node.hasChild(PduHeaders.APPLIC_ID, new int[0])) {
            builder.setCarrierIdentifier(buildCarrierIdentifier(asn1Node.getChild(PduHeaders.APPLIC_ID, new int[0])));
        }
        if (asn1Node.hasChild(40816, new int[0])) {
            builder.setState(asn1Node.getChild(40816, new int[0]).asInteger());
        } else {
            builder.setState(0);
        }
        if (asn1Node.hasChild(149, new int[0])) {
            builder.setProfileClass(asn1Node.getChild(149, new int[0]).asInteger());
        } else {
            builder.setProfileClass(2);
        }
        if (asn1Node.hasChild(153, new int[0])) {
            builder.setPolicyRules(asn1Node.getChild(153, new int[0]).asBits());
        }
        if (asn1Node.hasChild(49014, new int[0])) {
            UiccAccessRule[] uiccAccessRuleArrBuildUiccAccessRule = buildUiccAccessRule(asn1Node.getChild(49014, new int[0]).getChildren(226));
            List listAsList = null;
            if (uiccAccessRuleArrBuildUiccAccessRule != null) {
                listAsList = Arrays.asList(uiccAccessRuleArrBuildUiccAccessRule);
            }
            builder.setUiccAccessRule(listAsList);
        }
    }

    private static CarrierIdentifier buildCarrierIdentifier(Asn1Node asn1Node) throws TagNotFoundException, InvalidAsn1DataException {
        String strBytesToHexString;
        if (asn1Node.hasChild(129, new int[0])) {
            strBytesToHexString = IccUtils.bytesToHexString(asn1Node.getChild(129, new int[0]).asBytes());
        } else {
            strBytesToHexString = null;
        }
        return new CarrierIdentifier(asn1Node.getChild(128, new int[0]).asBytes(), strBytesToHexString, asn1Node.hasChild(130, new int[0]) ? IccUtils.bytesToHexString(asn1Node.getChild(130, new int[0]).asBytes()) : null);
    }

    private static UiccAccessRule[] buildUiccAccessRule(List<Asn1Node> list) throws TagNotFoundException, InvalidAsn1DataException {
        String strAsString;
        if (list.isEmpty()) {
            return null;
        }
        int size = list.size();
        UiccAccessRule[] uiccAccessRuleArr = new UiccAccessRule[size];
        for (int i = 0; i < size; i++) {
            Asn1Node asn1Node = list.get(i);
            Asn1Node child = asn1Node.getChild(225, new int[0]);
            byte[] bArrAsBytes = child.getChild(193, new int[0]).asBytes();
            if (child.hasChild(202, new int[0])) {
                strAsString = child.getChild(202, new int[0]).asString();
            } else {
                strAsString = null;
            }
            long jAsRawLong = 0;
            if (asn1Node.hasChild(227, new int[]{219})) {
                jAsRawLong = asn1Node.getChild(227, new int[]{219}).asRawLong();
            }
            uiccAccessRuleArr[i] = new UiccAccessRule(bArrAsBytes, strAsString, jAsRawLong);
        }
        return uiccAccessRuleArr;
    }

    private static EuiccNotification createNotification(Asn1Node asn1Node) throws TagNotFoundException, InvalidAsn1DataException {
        Asn1Node child;
        if (asn1Node.getTag() != 48943) {
            if (asn1Node.getTag() == 48951) {
                child = asn1Node.getChild(48935, new int[]{48943});
            } else {
                child = asn1Node.getChild(48943, new int[0]);
            }
        } else {
            child = asn1Node;
        }
        return new EuiccNotification(child.getChild(128, new int[0]).asInteger(), child.getChild(12, new int[0]).asString(), child.getChild(129, new int[0]).asBits(), asn1Node.getTag() == 48943 ? null : asn1Node.toBytes());
    }

    private static int parseSimpleResult(byte[] bArr) throws TagNotFoundException, InvalidAsn1DataException, EuiccCardException {
        return parseResponse(bArr).getChild(128, new int[0]).asInteger();
    }

    private static Asn1Node parseResponse(byte[] bArr) throws InvalidAsn1DataException, EuiccCardException {
        Asn1Decoder asn1Decoder = new Asn1Decoder(bArr);
        if (!asn1Decoder.hasNextNode()) {
            throw new EuiccCardException("Empty response", null);
        }
        return asn1Decoder.nextNode();
    }

    private static Asn1Node parseResponseAndCheckSimpleError(byte[] bArr, int i) throws TagNotFoundException, InvalidAsn1DataException, EuiccCardException {
        Asn1Node response = parseResponse(bArr);
        if (response.hasChild(129, new int[0])) {
            throw new EuiccCardErrorException(i, response.getChild(129, new int[0]).asInteger());
        }
        return response;
    }

    private static String stripTrailingFs(byte[] bArr) {
        return IccUtils.stripTrailingFs(IccUtils.bchToString(bArr, 0, bArr.length));
    }

    private static String padTrailingFs(String str) {
        if (!TextUtils.isEmpty(str) && str.length() < 20) {
            return str + new String(new char[20 - str.length()]).replace((char) 0, 'F');
        }
        return str;
    }

    private static void loge(String str) {
        Rlog.e(LOG_TAG, str);
    }

    private static void loge(String str, Throwable th) {
        Rlog.e(LOG_TAG, str, th);
    }

    private static void logi(String str) {
        Rlog.i(LOG_TAG, str);
    }

    private static void logd(String str) {
        Rlog.d(LOG_TAG, str);
    }
}
