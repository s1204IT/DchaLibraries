package com.mediatek.internal.telephony;

import android.content.ComponentName;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.NetworkRequest;
import android.os.Handler;
import android.os.Looper;
import android.telephony.Rlog;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import com.android.internal.telephony.CallManager;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.DefaultPhoneNotifier;
import com.android.internal.telephony.GsmCdmaCallTracker;
import com.android.internal.telephony.GsmCdmaPhone;
import com.android.internal.telephony.ITelephonyRegistry;
import com.android.internal.telephony.IccInternalInterface;
import com.android.internal.telephony.IccPhoneBookInterfaceManager;
import com.android.internal.telephony.IccSmsInterfaceManager;
import com.android.internal.telephony.ImsSmsDispatcher;
import com.android.internal.telephony.InboundSmsTracker;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneNotifier;
import com.android.internal.telephony.PhoneSwitcher;
import com.android.internal.telephony.ProxyController;
import com.android.internal.telephony.RIL;
import com.android.internal.telephony.RetryManager;
import com.android.internal.telephony.ServiceStateTracker;
import com.android.internal.telephony.SmsDispatchersController;
import com.android.internal.telephony.SmsStorageMonitor;
import com.android.internal.telephony.SmsUsageMonitor;
import com.android.internal.telephony.SubscriptionController;
import com.android.internal.telephony.SubscriptionInfoUpdater;
import com.android.internal.telephony.SubscriptionMonitor;
import com.android.internal.telephony.TelephonyComponentFactory;
import com.android.internal.telephony.WapPushOverSms;
import com.android.internal.telephony.WspTypeDecoder;
import com.android.internal.telephony.cat.CatService;
import com.android.internal.telephony.cat.CommandParamsFactory;
import com.android.internal.telephony.cat.IconLoader;
import com.android.internal.telephony.cat.RilMessageDecoder;
import com.android.internal.telephony.cdma.CdmaInboundSmsHandler;
import com.android.internal.telephony.cdma.CdmaSMSDispatcher;
import com.android.internal.telephony.cdma.CdmaSubscriptionSourceManager;
import com.android.internal.telephony.dataconnection.DataConnection;
import com.android.internal.telephony.dataconnection.DataServiceManager;
import com.android.internal.telephony.dataconnection.DcAsyncChannel;
import com.android.internal.telephony.dataconnection.DcController;
import com.android.internal.telephony.dataconnection.DcRequest;
import com.android.internal.telephony.dataconnection.DcTesterFailBringUpAll;
import com.android.internal.telephony.dataconnection.DcTracker;
import com.android.internal.telephony.dataconnection.TelephonyNetworkFactory;
import com.android.internal.telephony.gsm.GsmInboundSmsHandler;
import com.android.internal.telephony.gsm.GsmSMSDispatcher;
import com.android.internal.telephony.ims.ImsServiceController;
import com.android.internal.telephony.imsphone.ImsPhone;
import com.android.internal.telephony.imsphone.ImsPhoneCallTracker;
import com.android.internal.telephony.uicc.IccCardStatus;
import com.android.internal.telephony.uicc.IccFileHandler;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.telephony.uicc.UiccCard;
import com.android.internal.telephony.uicc.UiccCardApplication;
import com.android.internal.telephony.uicc.UiccController;
import com.android.internal.telephony.uicc.UiccProfile;
import com.mediatek.internal.telephony.carrierexpress.CarrierExpressFwkHandler;
import com.mediatek.internal.telephony.cat.MtkCatLog;
import com.mediatek.internal.telephony.cat.MtkCatService;
import com.mediatek.internal.telephony.cat.MtkCommandParamsFactory;
import com.mediatek.internal.telephony.cat.MtkIconLoader;
import com.mediatek.internal.telephony.cat.MtkRilMessageDecoder;
import com.mediatek.internal.telephony.cdma.MtkCdmaInboundSmsHandler;
import com.mediatek.internal.telephony.cdma.MtkCdmaSMSDispatcher;
import com.mediatek.internal.telephony.cdma.MtkCdmaSubscriptionSourceManager;
import com.mediatek.internal.telephony.dataconnection.MtkDataConnection;
import com.mediatek.internal.telephony.dataconnection.MtkDcAsyncChannel;
import com.mediatek.internal.telephony.dataconnection.MtkDcController;
import com.mediatek.internal.telephony.dataconnection.MtkDcHelper;
import com.mediatek.internal.telephony.dataconnection.MtkDcRequest;
import com.mediatek.internal.telephony.dataconnection.MtkDcTracker;
import com.mediatek.internal.telephony.dataconnection.MtkTelephonyNetworkFactory;
import com.mediatek.internal.telephony.datasub.DataSubSelector;
import com.mediatek.internal.telephony.datasub.SmartDataSwitchAssistant;
import com.mediatek.internal.telephony.gsm.MtkGsmCellBroadcastHandler;
import com.mediatek.internal.telephony.gsm.MtkGsmInboundSmsHandler;
import com.mediatek.internal.telephony.gsm.MtkGsmSMSDispatcher;
import com.mediatek.internal.telephony.ims.MtkImsServiceControllerCompat;
import com.mediatek.internal.telephony.imsphone.MtkImsPhone;
import com.mediatek.internal.telephony.imsphone.MtkImsPhoneCallTracker;
import com.mediatek.internal.telephony.phb.MtkIccPhoneBookInterfaceManager;
import com.mediatek.internal.telephony.phb.MtkIccProvider;
import com.mediatek.internal.telephony.uicc.MtkUiccController;
import com.mediatek.internal.telephony.uicc.MtkUiccProfile;
import com.mediatek.internal.telephony.worldphone.WorldPhoneUtil;

public class MtkTelephonyComponentFactory extends TelephonyComponentFactory {
    private static MtkTelephonyComponentFactory sInstance;

    public static MtkTelephonyComponentFactory getInstance() {
        if (sInstance == null) {
            sInstance = new MtkTelephonyComponentFactory();
        }
        return sInstance;
    }

    public GsmCdmaPhone makePhone(Context context, CommandsInterface commandsInterface, PhoneNotifier phoneNotifier, int i, int i2, TelephonyComponentFactory telephonyComponentFactory) {
        return new MtkGsmCdmaPhone(context, commandsInterface, phoneNotifier, i, i2, telephonyComponentFactory);
    }

    public RIL makeRil(Context context, int i, int i2, Integer num) {
        return new MtkRIL(context, i, i2, num);
    }

    public ServiceStateTracker makeServiceStateTracker(GsmCdmaPhone gsmCdmaPhone, CommandsInterface commandsInterface) {
        return new MtkServiceStateTracker(gsmCdmaPhone, commandsInterface);
    }

    public SubscriptionController makeSubscriptionController(Phone phone) {
        return MtkSubscriptionController.mtkInit(phone);
    }

    public SubscriptionController makeSubscriptionController(Context context, CommandsInterface[] commandsInterfaceArr) {
        return MtkSubscriptionController.mtkInit(context, commandsInterfaceArr);
    }

    public GsmCdmaCallTracker makeGsmCdmaCallTracker(GsmCdmaPhone gsmCdmaPhone) {
        return new MtkGsmCdmaCallTracker(gsmCdmaPhone);
    }

    public SubscriptionInfoUpdater makeSubscriptionInfoUpdater(Looper looper, Context context, Phone[] phoneArr, CommandsInterface[] commandsInterfaceArr) {
        return new MtkSubscriptionInfoUpdater(looper, context, phoneArr, commandsInterfaceArr);
    }

    public CdmaSubscriptionSourceManager makeCdmaSubscriptionSourceManager(Context context, CommandsInterface commandsInterface, Handler handler, int i, Object obj) {
        return new MtkCdmaSubscriptionSourceManager(context, commandsInterface);
    }

    public DefaultPhoneNotifier makeDefaultPhoneNotifier() {
        Rlog.d("TelephonyComponentFactory", "makeDefaultPhoneNotifier mtk");
        return new MtkPhoneNotifier();
    }

    public UiccController makeUiccController(Context context, CommandsInterface[] commandsInterfaceArr) {
        Rlog.d("TelephonyComponentFactory", "makeUiccController mtk");
        return new MtkUiccController(context, commandsInterfaceArr);
    }

    public UiccProfile makeUiccProfile(Context context, CommandsInterface commandsInterface, IccCardStatus iccCardStatus, int i, UiccCard uiccCard, Object obj) {
        return new MtkUiccProfile(context, commandsInterface, iccCardStatus, i, uiccCard, obj);
    }

    public void initRadioManager(Context context, int i, CommandsInterface[] commandsInterfaceArr) {
        RadioManager.init(context, i, commandsInterfaceArr);
    }

    public CatService makeCatService(CommandsInterface commandsInterface, UiccCardApplication uiccCardApplication, IccRecords iccRecords, Context context, IccFileHandler iccFileHandler, UiccProfile uiccProfile, int i) {
        int currentPhoneType;
        int[] subId = SubscriptionManager.getSubId(i);
        if (subId != null) {
            currentPhoneType = TelephonyManager.getDefault().getCurrentPhoneType(subId[0]);
            MtkCatLog.d("MtkCatService", "makeCatService phoneType : " + currentPhoneType + " slotId: " + i + " subId[0]:" + subId[0]);
        } else {
            currentPhoneType = 1;
        }
        if (uiccProfile != null) {
            if (currentPhoneType == 2) {
                uiccCardApplication = uiccProfile.getApplication(2);
            } else {
                uiccCardApplication = uiccProfile.getApplicationIndex(0);
            }
        }
        UiccCardApplication uiccCardApplication2 = uiccCardApplication;
        MtkCatLog.v("MtkCatService", "makeCatService  ca = " + uiccCardApplication2);
        if (commandsInterface == null || uiccCardApplication2 == null || iccRecords == null || context == null || iccFileHandler == null || uiccProfile == null) {
            MtkCatLog.e("MtkCatService", "makeCatService exception, will not create MtkCatservice!!!!");
            return null;
        }
        return new MtkCatService(commandsInterface, uiccCardApplication2, iccRecords, context, iccFileHandler, uiccProfile, i);
    }

    public RilMessageDecoder makeRilMessageDecoder(Handler handler, IccFileHandler iccFileHandler, int i) {
        return new MtkRilMessageDecoder(handler, iccFileHandler, i);
    }

    public CommandParamsFactory makeCommandParamsFactory(RilMessageDecoder rilMessageDecoder, IccFileHandler iccFileHandler) {
        return new MtkCommandParamsFactory(rilMessageDecoder, iccFileHandler);
    }

    public IconLoader makeIconLoader(Looper looper, IccFileHandler iccFileHandler) {
        return new MtkIconLoader(looper, iccFileHandler);
    }

    public DcTracker makeDcTracker(Phone phone) {
        return new MtkDcTracker(phone, 1);
    }

    public TelephonyNetworkFactory makeTelephonyNetworkFactories(PhoneSwitcher phoneSwitcher, SubscriptionController subscriptionController, SubscriptionMonitor subscriptionMonitor, Looper looper, Context context, int i, DcTracker dcTracker) {
        return new MtkTelephonyNetworkFactory(phoneSwitcher, subscriptionController, subscriptionMonitor, looper, context, i, dcTracker);
    }

    public void makeDcHelper(Context context, Phone[] phoneArr) {
        MtkDcHelper.makeMtkDcHelper(context, phoneArr);
    }

    public void makeDataSubSelector(Context context, int i) {
        DataSubSelector.makeDataSubSelector(context, i);
    }

    public void makeSmartDataSwitchAssistant(Context context, Phone[] phoneArr) {
        SmartDataSwitchAssistant.makeSmartDataSwitchAssistant(context, phoneArr);
    }

    public void makeSuppServManager(Context context, Phone[] phoneArr) {
        MtkSuppServManager.makeSuppServManager(context, phoneArr).init();
    }

    public PhoneSwitcher makePhoneSwitcher(int i, int i2, Context context, SubscriptionController subscriptionController, Looper looper, ITelephonyRegistry iTelephonyRegistry, CommandsInterface[] commandsInterfaceArr, Phone[] phoneArr) {
        return new MtkPhoneSwitcher(i, i2, context, subscriptionController, looper, iTelephonyRegistry, commandsInterfaceArr, phoneArr);
    }

    public SmsStorageMonitor makeSmsStorageMonitor(Phone phone) {
        return new MtkSmsStorageMonitor(phone);
    }

    public SmsUsageMonitor makeSmsUsageMonitor(Context context) {
        return new MtkSmsUsageMonitor(context);
    }

    public IccSmsInterfaceManager makeIccSmsInterfaceManager(Phone phone) {
        return new MtkIccSmsInterfaceManager(phone);
    }

    public ImsSmsDispatcher makeImsSmsDispatcher(Phone phone, SmsDispatchersController smsDispatchersController) {
        return new MtkImsSmsDispatcher(phone, smsDispatchersController);
    }

    public GsmSMSDispatcher makeGsmSMSDispatcher(Phone phone, SmsDispatchersController smsDispatchersController, GsmInboundSmsHandler gsmInboundSmsHandler) {
        return new MtkGsmSMSDispatcher(phone, smsDispatchersController, gsmInboundSmsHandler);
    }

    public InboundSmsTracker makeInboundSmsTracker(byte[] bArr, long j, int i, boolean z, boolean z2, String str, String str2, String str3) {
        return new MtkInboundSmsTracker(bArr, j, i, z, z2, str, str2, str3);
    }

    public InboundSmsTracker makeInboundSmsTracker(byte[] bArr, long j, int i, boolean z, String str, String str2, int i2, int i3, int i4, boolean z2, String str3) {
        return new MtkInboundSmsTracker(bArr, j, i, z, str, str2, i2, i3, i4, z2, str3);
    }

    public InboundSmsTracker makeInboundSmsTracker(Cursor cursor, boolean z) {
        return new MtkInboundSmsTracker(cursor, z);
    }

    public void makeSmsBroadcastUndelivered(Context context, GsmInboundSmsHandler gsmInboundSmsHandler, CdmaInboundSmsHandler cdmaInboundSmsHandler) {
        MtkSmsBroadcastUndelivered.initialize(context, gsmInboundSmsHandler, cdmaInboundSmsHandler);
    }

    public WspTypeDecoder makeWspTypeDecoder(byte[] bArr) {
        return new MtkWspTypeDecoder(bArr);
    }

    public WapPushOverSms makeWapPushOverSms(Context context) {
        return new MtkWapPushOverSms(context);
    }

    public GsmInboundSmsHandler makeGsmInboundSmsHandler(Context context, SmsStorageMonitor smsStorageMonitor, Phone phone) {
        return MtkGsmInboundSmsHandler.makeInboundSmsHandler(context, smsStorageMonitor, phone);
    }

    public MtkSmsHeader makeSmsHeader() {
        return new MtkSmsHeader();
    }

    public MtkGsmCellBroadcastHandler makeGsmCellBroadcastHandler(Context context, Phone phone) {
        return MtkGsmCellBroadcastHandler.makeGsmCellBroadcastHandler(context, phone);
    }

    public SmsDispatchersController makeSmsDispatchersController(Phone phone, SmsStorageMonitor smsStorageMonitor, SmsUsageMonitor smsUsageMonitor) {
        return new MtkSmsDispatchersController(phone, phone.mSmsStorageMonitor, phone.mSmsUsageMonitor);
    }

    public ProxyController makeProxyController(Context context, Phone[] phoneArr, UiccController uiccController, CommandsInterface[] commandsInterfaceArr, PhoneSwitcher phoneSwitcher) {
        return new MtkProxyController(context, phoneArr, uiccController, commandsInterfaceArr, phoneSwitcher);
    }

    public CdmaInboundSmsHandler makeCdmaInboundSmsHandler(Context context, SmsStorageMonitor smsStorageMonitor, Phone phone, CdmaSMSDispatcher cdmaSMSDispatcher) {
        return new MtkCdmaInboundSmsHandler(context, smsStorageMonitor, phone, cdmaSMSDispatcher);
    }

    public CdmaSMSDispatcher makeCdmaSMSDispatcher(Phone phone, SmsDispatchersController smsDispatchersController) {
        return new MtkCdmaSMSDispatcher(phone, smsDispatchersController);
    }

    public void initEmbmsAdaptor(Context context, CommandsInterface[] commandsInterfaceArr) {
        MtkEmbmsAdaptor.getDefault(context, commandsInterfaceArr);
    }

    public void makeWorldPhoneManager() {
        WorldPhoneUtil.makeWorldPhoneManager();
    }

    public IccPhoneBookInterfaceManager makeIccPhoneBookInterfaceManager(Phone phone) {
        Rlog.d("TelephonyComponentFactory", "makeIccPhoneBookInterfaceManager mtk");
        return new MtkIccPhoneBookInterfaceManager(phone);
    }

    public IccInternalInterface makeIccProvider(UriMatcher uriMatcher, Context context) {
        Rlog.d("TelephonyComponentFactory", "makeIccProvider mtk");
        return new MtkIccProvider(uriMatcher, context);
    }

    public ImsPhoneCallTracker makeImsPhoneCallTracker(ImsPhone imsPhone) {
        return new MtkImsPhoneCallTracker(imsPhone);
    }

    public ImsPhone makeImsPhone(Context context, PhoneNotifier phoneNotifier, Phone phone) {
        try {
            return new MtkImsPhone(context, phoneNotifier, phone);
        } catch (Exception e) {
            Rlog.e("TelephonyComponentFactoryEx", "makeImsPhoneExt", e);
            return null;
        }
    }

    public CallManager makeCallManager() {
        return new MtkCallManager();
    }

    public RetryManager makeRetryManager(Phone phone, String str) {
        return new MtkRetryManager(phone, str);
    }

    public DataConnection makeDataConnection(Phone phone, String str, int i, DcTracker dcTracker, DataServiceManager dataServiceManager, DcTesterFailBringUpAll dcTesterFailBringUpAll, DcController dcController) {
        MtkDataConnection mtkDataConnection = new MtkDataConnection(phone, "Mtk" + str, i, dcTracker, dataServiceManager, dcTesterFailBringUpAll, dcController);
        DataConnection.TCP_BUFFER_SIZES_LTE = "2097152,4194304,8388608,262144,524288,1048576";
        return mtkDataConnection;
    }

    public DcAsyncChannel makeDcAsyncChannel(DataConnection dataConnection, String str) {
        return new MtkDcAsyncChannel(dataConnection, str);
    }

    public DcController makeDcController(String str, Phone phone, DcTracker dcTracker, DataServiceManager dataServiceManager, Handler handler) {
        return new MtkDcController("Mtk" + str, phone, dcTracker, dataServiceManager, handler);
    }

    public DcRequest makeDcRequest(NetworkRequest networkRequest, Context context) {
        return new MtkDcRequest(networkRequest, context);
    }

    public ComponentName makeConnectionServiceName() {
        Rlog.d("TelephonyComponentFactory", "makeConnectionServiceName mtk");
        return new ComponentName("com.android.phone", "com.mediatek.services.telephony.MtkTelephonyConnectionService");
    }

    public void makeNetworkStatusUpdater(Phone[] phoneArr, int i) {
        Rlog.d("TelephonyComponentFactory", "Creating NetworkStatusUpdater");
        MtkNetworkStatusUpdater.init(phoneArr, i);
    }

    public ImsServiceController makeStaticImsServiceController(Context context, ComponentName componentName, ImsServiceController.ImsServiceControllerCallbacks imsServiceControllerCallbacks) {
        return new MtkImsServiceControllerCompat(context, componentName, imsServiceControllerCallbacks);
    }

    public void initCarrierExpress() {
        Rlog.d("TelephonyComponentFactory", "Creating CarrierExpress");
        CarrierExpressFwkHandler.init();
    }
}
