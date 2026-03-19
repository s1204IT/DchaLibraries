package com.android.internal.telephony;

import android.content.ComponentName;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.NetworkRequest;
import android.os.Handler;
import android.os.IDeviceIdleController;
import android.os.Looper;
import android.os.ServiceManager;
import android.telephony.Rlog;
import com.android.internal.telephony.cat.CatService;
import com.android.internal.telephony.cat.CommandParamsFactory;
import com.android.internal.telephony.cat.IconLoader;
import com.android.internal.telephony.cat.RilMessageDecoder;
import com.android.internal.telephony.cdma.CdmaInboundSmsHandler;
import com.android.internal.telephony.cdma.CdmaSMSDispatcher;
import com.android.internal.telephony.cdma.CdmaSubscriptionSourceManager;
import com.android.internal.telephony.cdma.EriManager;
import com.android.internal.telephony.dataconnection.DataConnection;
import com.android.internal.telephony.dataconnection.DataServiceManager;
import com.android.internal.telephony.dataconnection.DcAsyncChannel;
import com.android.internal.telephony.dataconnection.DcController;
import com.android.internal.telephony.dataconnection.DcRequest;
import com.android.internal.telephony.dataconnection.DcTesterFailBringUpAll;
import com.android.internal.telephony.dataconnection.DcTracker;
import com.android.internal.telephony.dataconnection.TelephonyNetworkFactory;
import com.android.internal.telephony.gsm.GsmCellBroadcastHandler;
import com.android.internal.telephony.gsm.GsmInboundSmsHandler;
import com.android.internal.telephony.gsm.GsmSMSDispatcher;
import com.android.internal.telephony.ims.ImsServiceController;
import com.android.internal.telephony.ims.ImsServiceControllerStaticCompat;
import com.android.internal.telephony.imsphone.ImsExternalCallTracker;
import com.android.internal.telephony.imsphone.ImsPhone;
import com.android.internal.telephony.imsphone.ImsPhoneCallTracker;
import com.android.internal.telephony.uicc.IccCardStatus;
import com.android.internal.telephony.uicc.IccFileHandler;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.telephony.uicc.UiccCard;
import com.android.internal.telephony.uicc.UiccCardApplication;
import com.android.internal.telephony.uicc.UiccController;
import com.android.internal.telephony.uicc.UiccProfile;
import java.lang.reflect.Constructor;

public class TelephonyComponentFactory {
    public static final String LOG_TAG = "TelephonyComponentFactory";
    private static TelephonyComponentFactory sInstance;

    public static TelephonyComponentFactory getInstance() {
        if (sInstance == null) {
            try {
                Class<?> cls = Class.forName("com.mediatek.internal.telephony.MtkTelephonyComponentFactory", false, ClassLoader.getSystemClassLoader());
                Rlog.d(LOG_TAG, "class = " + cls);
                Constructor<?> constructor = cls.getConstructor(new Class[0]);
                Rlog.d(LOG_TAG, "constructor function = " + constructor);
                sInstance = (TelephonyComponentFactory) constructor.newInstance(new Object[0]);
            } catch (Exception e) {
                Rlog.e(LOG_TAG, "No MtkTelephonyComponentFactory! Used AOSP for instead!");
                sInstance = new TelephonyComponentFactory();
            }
        }
        return sInstance;
    }

    public GsmCdmaCallTracker makeGsmCdmaCallTracker(GsmCdmaPhone gsmCdmaPhone) {
        return new GsmCdmaCallTracker(gsmCdmaPhone);
    }

    public CallManager makeCallManager() {
        return new CallManager();
    }

    public SmsStorageMonitor makeSmsStorageMonitor(Phone phone) {
        return new SmsStorageMonitor(phone);
    }

    public SmsUsageMonitor makeSmsUsageMonitor(Context context) {
        return new SmsUsageMonitor(context);
    }

    public ServiceStateTracker makeServiceStateTracker(GsmCdmaPhone gsmCdmaPhone, CommandsInterface commandsInterface) {
        return new ServiceStateTracker(gsmCdmaPhone, commandsInterface);
    }

    public NitzStateMachine makeNitzStateMachine(GsmCdmaPhone gsmCdmaPhone) {
        return new NitzStateMachine(gsmCdmaPhone);
    }

    public SimActivationTracker makeSimActivationTracker(Phone phone) {
        return new SimActivationTracker(phone);
    }

    public DcTracker makeDcTracker(Phone phone) {
        return new DcTracker(phone, 1);
    }

    public CarrierSignalAgent makeCarrierSignalAgent(Phone phone) {
        return new CarrierSignalAgent(phone);
    }

    public CarrierActionAgent makeCarrierActionAgent(Phone phone) {
        return new CarrierActionAgent(phone);
    }

    public CarrierIdentifier makeCarrierIdentifier(Phone phone) {
        return new CarrierIdentifier(phone);
    }

    public IccPhoneBookInterfaceManager makeIccPhoneBookInterfaceManager(Phone phone) {
        return new IccPhoneBookInterfaceManager(phone);
    }

    public IccSmsInterfaceManager makeIccSmsInterfaceManager(Phone phone) {
        return new IccSmsInterfaceManager(phone);
    }

    public SubscriptionController makeSubscriptionController(Phone phone) {
        return new SubscriptionController(phone);
    }

    public SubscriptionController makeSubscriptionController(Context context, CommandsInterface[] commandsInterfaceArr) {
        return new SubscriptionController(context);
    }

    public SubscriptionInfoUpdater makeSubscriptionInfoUpdater(Looper looper, Context context, Phone[] phoneArr, CommandsInterface[] commandsInterfaceArr) {
        return new SubscriptionInfoUpdater(looper, context, phoneArr, commandsInterfaceArr);
    }

    public UiccController makeUiccController(Context context, CommandsInterface[] commandsInterfaceArr) {
        return new UiccController(context, commandsInterfaceArr);
    }

    public UiccProfile makeUiccProfile(Context context, CommandsInterface commandsInterface, IccCardStatus iccCardStatus, int i, UiccCard uiccCard, Object obj) {
        return new UiccProfile(context, commandsInterface, iccCardStatus, i, uiccCard, obj);
    }

    public EriManager makeEriManager(Phone phone, Context context, int i) {
        return new EriManager(phone, context, i);
    }

    public WspTypeDecoder makeWspTypeDecoder(byte[] bArr) {
        return new WspTypeDecoder(bArr);
    }

    public InboundSmsTracker makeInboundSmsTracker(byte[] bArr, long j, int i, boolean z, boolean z2, String str, String str2, String str3) {
        return new InboundSmsTracker(bArr, j, i, z, z2, str, str2, str3);
    }

    public InboundSmsTracker makeInboundSmsTracker(byte[] bArr, long j, int i, boolean z, String str, String str2, int i2, int i3, int i4, boolean z2, String str3) {
        return new InboundSmsTracker(bArr, j, i, z, str, str2, i2, i3, i4, z2, str3);
    }

    public InboundSmsTracker makeInboundSmsTracker(Cursor cursor, boolean z) {
        return new InboundSmsTracker(cursor, z);
    }

    public ImsPhoneCallTracker makeImsPhoneCallTracker(ImsPhone imsPhone) {
        return new ImsPhoneCallTracker(imsPhone);
    }

    public ImsExternalCallTracker makeImsExternalCallTracker(ImsPhone imsPhone) {
        return new ImsExternalCallTracker(imsPhone);
    }

    public AppSmsManager makeAppSmsManager(Context context) {
        return new AppSmsManager(context);
    }

    public DeviceStateMonitor makeDeviceStateMonitor(Phone phone) {
        return new DeviceStateMonitor(phone);
    }

    public CdmaSubscriptionSourceManager getCdmaSubscriptionSourceManagerInstance(Context context, CommandsInterface commandsInterface, Handler handler, int i, Object obj) {
        return CdmaSubscriptionSourceManager.getInstance(context, commandsInterface, handler, i, obj);
    }

    public CdmaSubscriptionSourceManager makeCdmaSubscriptionSourceManager(Context context, CommandsInterface commandsInterface, Handler handler, int i, Object obj) {
        return new CdmaSubscriptionSourceManager(context, commandsInterface);
    }

    public void initEmbmsAdaptor(Context context, CommandsInterface[] commandsInterfaceArr) {
    }

    public IDeviceIdleController getIDeviceIdleController() {
        return IDeviceIdleController.Stub.asInterface(ServiceManager.getService("deviceidle"));
    }

    public SmsHeader makeSmsHeader() {
        return new SmsHeader();
    }

    public void makeSuppServManager(Context context, Phone[] phoneArr) {
    }

    public ImsSmsDispatcher makeImsSmsDispatcher(Phone phone, SmsDispatchersController smsDispatchersController) {
        return new ImsSmsDispatcher(phone, smsDispatchersController);
    }

    public CdmaSMSDispatcher makeCdmaSMSDispatcher(Phone phone, SmsDispatchersController smsDispatchersController) {
        return new CdmaSMSDispatcher(phone, smsDispatchersController);
    }

    public GsmSMSDispatcher makeGsmSMSDispatcher(Phone phone, SmsDispatchersController smsDispatchersController, GsmInboundSmsHandler gsmInboundSmsHandler) {
        return new GsmSMSDispatcher(phone, smsDispatchersController, gsmInboundSmsHandler);
    }

    public void makeSmsBroadcastUndelivered(Context context, GsmInboundSmsHandler gsmInboundSmsHandler, CdmaInboundSmsHandler cdmaInboundSmsHandler) {
        SmsBroadcastUndelivered.initialize(context, gsmInboundSmsHandler, cdmaInboundSmsHandler);
    }

    public WapPushOverSms makeWapPushOverSms(Context context) {
        return new WapPushOverSms(context);
    }

    public GsmInboundSmsHandler makeGsmInboundSmsHandler(Context context, SmsStorageMonitor smsStorageMonitor, Phone phone) {
        return GsmInboundSmsHandler.makeInboundSmsHandler(context, smsStorageMonitor, phone);
    }

    public GsmCellBroadcastHandler makeGsmCellBroadcastHandler(Context context, Phone phone) {
        return GsmCellBroadcastHandler.makeGsmCellBroadcastHandler(context, phone);
    }

    public void initRadioManager(Context context, int i, CommandsInterface[] commandsInterfaceArr) {
    }

    public SmsDispatchersController makeSmsDispatchersController(Phone phone, SmsStorageMonitor smsStorageMonitor, SmsUsageMonitor smsUsageMonitor) {
        return new SmsDispatchersController(phone, phone.mSmsStorageMonitor, phone.mSmsUsageMonitor);
    }

    public CatService makeCatService(CommandsInterface commandsInterface, UiccCardApplication uiccCardApplication, IccRecords iccRecords, Context context, IccFileHandler iccFileHandler, UiccProfile uiccProfile, int i) {
        return new CatService(commandsInterface, uiccCardApplication, iccRecords, context, iccFileHandler, uiccProfile, i);
    }

    public RilMessageDecoder makeRilMessageDecoder(Handler handler, IccFileHandler iccFileHandler, int i) {
        return new RilMessageDecoder(handler, iccFileHandler);
    }

    public CommandParamsFactory makeCommandParamsFactory(RilMessageDecoder rilMessageDecoder, IccFileHandler iccFileHandler) {
        return new CommandParamsFactory(rilMessageDecoder, iccFileHandler);
    }

    public IconLoader makeIconLoader(Looper looper, IccFileHandler iccFileHandler) {
        return new IconLoader(looper, iccFileHandler);
    }

    public TelephonyNetworkFactory makeTelephonyNetworkFactories(PhoneSwitcher phoneSwitcher, SubscriptionController subscriptionController, SubscriptionMonitor subscriptionMonitor, Looper looper, Context context, int i, DcTracker dcTracker) {
        return new TelephonyNetworkFactory(phoneSwitcher, subscriptionController, subscriptionMonitor, looper, context, i, dcTracker);
    }

    public PhoneSwitcher makePhoneSwitcher(int i, int i2, Context context, SubscriptionController subscriptionController, Looper looper, ITelephonyRegistry iTelephonyRegistry, CommandsInterface[] commandsInterfaceArr, Phone[] phoneArr) {
        return new PhoneSwitcher(i, i2, context, subscriptionController, looper, iTelephonyRegistry, commandsInterfaceArr, phoneArr);
    }

    public CdmaInboundSmsHandler makeCdmaInboundSmsHandler(Context context, SmsStorageMonitor smsStorageMonitor, Phone phone, CdmaSMSDispatcher cdmaSMSDispatcher) {
        return new CdmaInboundSmsHandler(context, smsStorageMonitor, phone, cdmaSMSDispatcher);
    }

    public void makeDataSubSelector(Context context, int i) {
    }

    public void makeSmartDataSwitchAssistant(Context context, Phone[] phoneArr) {
    }

    public RetryManager makeRetryManager(Phone phone, String str) {
        return new RetryManager(phone, str);
    }

    public DataConnection makeDataConnection(Phone phone, String str, int i, DcTracker dcTracker, DataServiceManager dataServiceManager, DcTesterFailBringUpAll dcTesterFailBringUpAll, DcController dcController) {
        return new DataConnection(phone, str, i, dcTracker, dataServiceManager, dcTesterFailBringUpAll, dcController);
    }

    public DcAsyncChannel makeDcAsyncChannel(DataConnection dataConnection, String str) {
        return new DcAsyncChannel(dataConnection, str);
    }

    public DcController makeDcController(String str, Phone phone, DcTracker dcTracker, DataServiceManager dataServiceManager, Handler handler) {
        return new DcController(str, phone, dcTracker, dataServiceManager, handler);
    }

    public DcRequest makeDcRequest(NetworkRequest networkRequest, Context context) {
        return new DcRequest(networkRequest, context);
    }

    public IccInternalInterface makeIccProvider(UriMatcher uriMatcher, Context context) {
        Rlog.d(LOG_TAG, "makeIccProvider aosp");
        return null;
    }

    public ProxyController makeProxyController(Context context, Phone[] phoneArr, UiccController uiccController, CommandsInterface[] commandsInterfaceArr, PhoneSwitcher phoneSwitcher) {
        return new ProxyController(context, phoneArr, uiccController, commandsInterfaceArr, phoneSwitcher);
    }

    public void makeWorldPhoneManager() {
    }

    public ImsPhone makeImsPhone(Context context, PhoneNotifier phoneNotifier, Phone phone) {
        return new ImsPhone(context, phoneNotifier, phone);
    }

    public void makeDcHelper(Context context, Phone[] phoneArr) {
    }

    public GsmCdmaPhone makePhone(Context context, CommandsInterface commandsInterface, PhoneNotifier phoneNotifier, int i, int i2, TelephonyComponentFactory telephonyComponentFactory) {
        return new GsmCdmaPhone(context, commandsInterface, phoneNotifier, i, i2, telephonyComponentFactory);
    }

    public RIL makeRil(Context context, int i, int i2, Integer num) {
        return new RIL(context, i, i2, num);
    }

    public DefaultPhoneNotifier makeDefaultPhoneNotifier() {
        Rlog.d(LOG_TAG, "makeDefaultPhoneNotifier aosp");
        return new DefaultPhoneNotifier();
    }

    public void makeNetworkStatusUpdater(Phone[] phoneArr, int i) {
    }

    public ImsServiceController makeStaticImsServiceController(Context context, ComponentName componentName, ImsServiceController.ImsServiceControllerCallbacks imsServiceControllerCallbacks) {
        return new ImsServiceControllerStaticCompat(context, componentName, imsServiceControllerCallbacks);
    }

    public LocaleTracker makeLocaleTracker(Phone phone, Looper looper) {
        return new LocaleTracker(phone, looper);
    }

    public void initCarrierExpress() {
    }
}
