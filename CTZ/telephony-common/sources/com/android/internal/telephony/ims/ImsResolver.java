package com.android.internal.telephony.ims;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.os.UserHandle;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionManager;
import android.telephony.ims.aidl.IImsConfig;
import android.telephony.ims.aidl.IImsMmTelFeature;
import android.telephony.ims.aidl.IImsRcsFeature;
import android.telephony.ims.aidl.IImsRegistration;
import android.telephony.ims.stub.ImsFeatureConfiguration;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import com.android.ims.internal.IImsServiceFeatureCallback;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.os.SomeArgs;
import com.android.internal.telephony.TelephonyComponentFactory;
import com.android.internal.telephony.ims.ImsResolver;
import com.android.internal.telephony.ims.ImsServiceController;
import com.android.internal.telephony.ims.ImsServiceFeatureQueryManager;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ImsResolver implements ImsServiceController.ImsServiceControllerCallbacks {
    private static final int DELAY_DYNAMIC_QUERY_MS = 5000;
    private static final int HANDLER_ADD_PACKAGE = 0;
    private static final int HANDLER_CONFIG_CHANGED = 2;
    private static final int HANDLER_DYNAMIC_FEATURE_CHANGE = 4;
    private static final int HANDLER_OVERRIDE_IMS_SERVICE_CONFIG = 5;
    private static final int HANDLER_REMOVE_PACKAGE = 1;
    private static final int HANDLER_START_DYNAMIC_FEATURE_QUERY = 3;
    public static final String METADATA_EMERGENCY_MMTEL_FEATURE = "android.telephony.ims.EMERGENCY_MMTEL_FEATURE";
    public static final String METADATA_MMTEL_FEATURE = "android.telephony.ims.MMTEL_FEATURE";
    private static final String METADATA_OVERRIDE_PERM_CHECK = "override_bind_check";
    public static final String METADATA_RCS_FEATURE = "android.telephony.ims.RCS_FEATURE";
    private static final String TAG = "ImsResolver";
    private List<SparseArray<ImsServiceController>> mBoundImsServicesByFeature;
    private final CarrierConfigManager mCarrierConfigManager;
    private String[] mCarrierServices;
    private final Context mContext;
    private String mDeviceService;
    private ImsServiceFeatureQueryManager mFeatureQueryManager;
    private final boolean mIsDynamicBinding;
    private final int mNumSlots;
    private final ComponentName mStaticComponent;
    private BroadcastReceiver mAppChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            byte b;
            String action = intent.getAction();
            String schemeSpecificPart = intent.getData().getSchemeSpecificPart();
            int iHashCode = action.hashCode();
            if (iHashCode != -810471698) {
                if (iHashCode != 172491798) {
                    if (iHashCode != 525384130) {
                        b = (iHashCode == 1544582882 && action.equals("android.intent.action.PACKAGE_ADDED")) ? (byte) 0 : (byte) -1;
                    } else if (action.equals("android.intent.action.PACKAGE_REMOVED")) {
                        b = 3;
                    }
                } else if (action.equals("android.intent.action.PACKAGE_CHANGED")) {
                    b = 2;
                }
            } else if (action.equals("android.intent.action.PACKAGE_REPLACED")) {
                b = 1;
            }
            switch (b) {
                case 0:
                case 1:
                case 2:
                    ImsResolver.this.mHandler.obtainMessage(0, schemeSpecificPart).sendToTarget();
                    break;
                case 3:
                    ImsResolver.this.mHandler.obtainMessage(1, schemeSpecificPart).sendToTarget();
                    break;
            }
        }
    };
    private BroadcastReceiver mConfigChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int intExtra = intent.getIntExtra("android.telephony.extra.SLOT_INDEX", -1);
            if (intExtra == -1) {
                Log.i(ImsResolver.TAG, "Received SIM change for invalid slot id.");
                return;
            }
            Log.i(ImsResolver.TAG, "Received Carrier Config Changed for SlotId: " + intExtra);
            ImsResolver.this.mHandler.obtainMessage(2, Integer.valueOf(intExtra)).sendToTarget();
        }
    };
    private SubscriptionManagerProxy mSubscriptionManagerProxy = new SubscriptionManagerProxy() {
        @Override
        public int getSubId(int i) {
            int[] subId = SubscriptionManager.getSubId(i);
            if (subId != null) {
                return subId[0];
            }
            return -1;
        }

        @Override
        public int getSlotIndex(int i) {
            return SubscriptionManager.getSlotIndex(i);
        }
    };
    private ImsServiceControllerFactory mImsServiceControllerFactory = new ImsServiceControllerFactory() {
        @Override
        public String getServiceInterface() {
            return "android.telephony.ims.ImsService";
        }

        @Override
        public ImsServiceController create(Context context, ComponentName componentName, ImsServiceController.ImsServiceControllerCallbacks imsServiceControllerCallbacks) {
            return new ImsServiceController(context, componentName, imsServiceControllerCallbacks);
        }
    };
    private ImsServiceControllerFactory mImsServiceControllerFactoryCompat = new ImsServiceControllerFactory() {
        @Override
        public String getServiceInterface() {
            return "android.telephony.ims.compat.ImsService";
        }

        @Override
        public ImsServiceController create(Context context, ComponentName componentName, ImsServiceController.ImsServiceControllerCallbacks imsServiceControllerCallbacks) {
            return new ImsServiceControllerCompat(context, componentName, imsServiceControllerCallbacks);
        }
    };
    private ImsServiceControllerFactory mImsServiceControllerFactoryStaticBindingCompat = new ImsServiceControllerFactory() {
        @Override
        public String getServiceInterface() {
            return null;
        }

        @Override
        public ImsServiceController create(Context context, ComponentName componentName, ImsServiceController.ImsServiceControllerCallbacks imsServiceControllerCallbacks) {
            return TelephonyComponentFactory.getInstance().makeStaticImsServiceController(context, componentName, imsServiceControllerCallbacks);
        }
    };
    private ImsDynamicQueryManagerFactory mDynamicQueryManagerFactory = new ImsDynamicQueryManagerFactory() {
        @Override
        public final ImsServiceFeatureQueryManager create(Context context, ImsServiceFeatureQueryManager.Listener listener) {
            return new ImsServiceFeatureQueryManager(context, listener);
        }
    };
    private final Object mBoundServicesLock = new Object();
    private Handler mHandler = new Handler(Looper.getMainLooper(), new Handler.Callback() {
        @Override
        public final boolean handleMessage(Message message) {
            return ImsResolver.lambda$new$0(this.f$0, message);
        }
    });
    private ImsServiceFeatureQueryManager.Listener mDynamicQueryListener = new ImsServiceFeatureQueryManager.Listener() {
        @Override
        public void onComplete(ComponentName componentName, Set<ImsFeatureConfiguration.FeatureSlotPair> set) {
            Log.d(ImsResolver.TAG, "onComplete called for name: " + componentName + "features:" + ImsResolver.this.printFeatures(set));
            ImsResolver.this.handleFeaturesChanged(componentName, set);
        }

        @Override
        public void onError(ComponentName componentName) {
            Log.w(ImsResolver.TAG, "onError: " + componentName + "returned with an error result");
            ImsResolver.this.scheduleQueryForFeatures(componentName, ImsResolver.DELAY_DYNAMIC_QUERY_MS);
        }
    };
    private Map<ComponentName, ImsServiceInfo> mInstalledServicesCache = new HashMap();
    private Map<ComponentName, ImsServiceController> mActiveControllers = new HashMap();

    @VisibleForTesting
    public interface ImsDynamicQueryManagerFactory {
        ImsServiceFeatureQueryManager create(Context context, ImsServiceFeatureQueryManager.Listener listener);
    }

    @VisibleForTesting
    public interface ImsServiceControllerFactory {
        ImsServiceController create(Context context, ComponentName componentName, ImsServiceController.ImsServiceControllerCallbacks imsServiceControllerCallbacks);

        String getServiceInterface();
    }

    @VisibleForTesting
    public interface SubscriptionManagerProxy {
        int getSlotIndex(int i);

        int getSubId(int i);
    }

    public static SparseArray lambda$WVd6ghNMbVDukmkxia3ZwNeZzEY() {
        return new SparseArray();
    }

    @VisibleForTesting
    public static class ImsServiceInfo {
        public ImsServiceControllerFactory controllerFactory;
        private final int mNumSlots;
        public ComponentName name;
        public boolean featureFromMetadata = true;
        private final HashSet<ImsFeatureConfiguration.FeatureSlotPair> mSupportedFeatures = new HashSet<>();

        public ImsServiceInfo(int i) {
            this.mNumSlots = i;
        }

        void addFeatureForAllSlots(int i) {
            for (int i2 = 0; i2 < this.mNumSlots; i2++) {
                this.mSupportedFeatures.add(new ImsFeatureConfiguration.FeatureSlotPair(i2, i));
            }
        }

        void replaceFeatures(Set<ImsFeatureConfiguration.FeatureSlotPair> set) {
            this.mSupportedFeatures.clear();
            this.mSupportedFeatures.addAll(set);
        }

        @VisibleForTesting
        public HashSet<ImsFeatureConfiguration.FeatureSlotPair> getSupportedFeatures() {
            return this.mSupportedFeatures;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            ImsServiceInfo imsServiceInfo = (ImsServiceInfo) obj;
            if (this.name == null ? imsServiceInfo.name != null : !this.name.equals(imsServiceInfo.name)) {
                return false;
            }
            if (!this.mSupportedFeatures.equals(imsServiceInfo.mSupportedFeatures)) {
                return false;
            }
            if (this.controllerFactory != null) {
                return this.controllerFactory.equals(imsServiceInfo.controllerFactory);
            }
            if (imsServiceInfo.controllerFactory == null) {
                return true;
            }
            return false;
        }

        public int hashCode() {
            return (31 * (this.name != null ? this.name.hashCode() : 0)) + (this.controllerFactory != null ? this.controllerFactory.hashCode() : 0);
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("[ImsServiceInfo] name=");
            sb.append(this.name);
            sb.append(", supportedFeatures=[ ");
            for (ImsFeatureConfiguration.FeatureSlotPair featureSlotPair : this.mSupportedFeatures) {
                sb.append("(");
                sb.append(featureSlotPair.slotId);
                sb.append(",");
                sb.append(featureSlotPair.featureType);
                sb.append(") ");
            }
            return sb.toString();
        }
    }

    public static boolean lambda$new$0(ImsResolver imsResolver, Message message) {
        switch (message.what) {
            case 0:
                imsResolver.maybeAddedImsService((String) message.obj);
                return true;
            case 1:
                imsResolver.maybeRemovedImsService((String) message.obj);
                return true;
            case 2:
                imsResolver.carrierConfigChanged(((Integer) message.obj).intValue());
                return true;
            case 3:
                imsResolver.startDynamicQuery((ImsServiceInfo) message.obj);
                return true;
            case 4:
                SomeArgs someArgs = (SomeArgs) message.obj;
                ComponentName componentName = (ComponentName) someArgs.arg1;
                Set<ImsFeatureConfiguration.FeatureSlotPair> set = (Set) someArgs.arg2;
                someArgs.recycle();
                imsResolver.dynamicQueryComplete(componentName, set);
                return true;
            case 5:
                int i = message.arg1;
                boolean z = message.arg2 == 1;
                String str = (String) message.obj;
                if (z) {
                    Log.i(TAG, "overriding carrier ImsService - slot=" + i + " packageName=" + str);
                    imsResolver.maybeRebindService(i, str);
                } else {
                    Log.i(TAG, "overriding device ImsService -  packageName=" + str);
                    if (str == null || str.isEmpty()) {
                        imsResolver.unbindImsService(imsResolver.getImsServiceInfoFromCache(imsResolver.mDeviceService));
                    }
                    imsResolver.mDeviceService = str;
                    ImsServiceInfo imsServiceInfoFromCache = imsResolver.getImsServiceInfoFromCache(imsResolver.mDeviceService);
                    if (imsServiceInfoFromCache != null) {
                        if (imsServiceInfoFromCache.featureFromMetadata) {
                            imsResolver.bindImsService(imsServiceInfoFromCache);
                        } else {
                            imsResolver.scheduleQueryForFeatures(imsServiceInfoFromCache);
                        }
                    }
                }
                return true;
            default:
                return false;
        }
    }

    public ImsResolver(Context context, String str, int i, boolean z) {
        this.mContext = context;
        this.mDeviceService = str;
        this.mNumSlots = i;
        this.mIsDynamicBinding = z;
        this.mStaticComponent = new ComponentName(this.mContext, (Class<?>) ImsResolver.class);
        if (!this.mIsDynamicBinding) {
            Log.i(TAG, "ImsResolver initialized with static binding.");
            this.mDeviceService = this.mStaticComponent.getPackageName();
        }
        this.mCarrierConfigManager = (CarrierConfigManager) this.mContext.getSystemService("carrier_config");
        this.mCarrierServices = new String[i];
        this.mBoundImsServicesByFeature = (List) Stream.generate(new Supplier() {
            @Override
            public final Object get() {
                return ImsResolver.lambda$WVd6ghNMbVDukmkxia3ZwNeZzEY();
            }
        }).limit(this.mNumSlots).collect(Collectors.toList());
        if (this.mIsDynamicBinding) {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.intent.action.PACKAGE_CHANGED");
            intentFilter.addAction("android.intent.action.PACKAGE_REMOVED");
            intentFilter.addAction("android.intent.action.PACKAGE_ADDED");
            intentFilter.addDataScheme("package");
            context.registerReceiverAsUser(this.mAppChangedReceiver, UserHandle.ALL, intentFilter, null, null);
            context.registerReceiver(this.mConfigChangedReceiver, new IntentFilter("android.telephony.action.CARRIER_CONFIG_CHANGED"));
        }
    }

    @VisibleForTesting
    public void setSubscriptionManagerProxy(SubscriptionManagerProxy subscriptionManagerProxy) {
        this.mSubscriptionManagerProxy = subscriptionManagerProxy;
    }

    @VisibleForTesting
    public void setImsServiceControllerFactory(ImsServiceControllerFactory imsServiceControllerFactory) {
        this.mImsServiceControllerFactory = imsServiceControllerFactory;
    }

    @VisibleForTesting
    public Handler getHandler() {
        return this.mHandler;
    }

    @VisibleForTesting
    public void setImsDynamicQueryManagerFactory(ImsDynamicQueryManagerFactory imsDynamicQueryManagerFactory) {
        this.mDynamicQueryManagerFactory = imsDynamicQueryManagerFactory;
    }

    public void initPopulateCacheAndStartBind() {
        Log.i(TAG, "Initializing cache and binding.");
        this.mFeatureQueryManager = this.mDynamicQueryManagerFactory.create(this.mContext, this.mDynamicQueryListener);
        this.mHandler.obtainMessage(2, -1).sendToTarget();
        this.mHandler.obtainMessage(0, null).sendToTarget();
    }

    public void enableIms(int i) {
        SparseArray<ImsServiceController> imsServiceControllers = getImsServiceControllers(i);
        if (imsServiceControllers != null) {
            for (int i2 = 0; i2 < imsServiceControllers.size(); i2++) {
                imsServiceControllers.get(imsServiceControllers.keyAt(i2)).enableIms(i);
            }
        }
    }

    public void disableIms(int i) {
        SparseArray<ImsServiceController> imsServiceControllers = getImsServiceControllers(i);
        if (imsServiceControllers != null) {
            for (int i2 = 0; i2 < imsServiceControllers.size(); i2++) {
                imsServiceControllers.get(imsServiceControllers.keyAt(i2)).disableIms(i);
            }
        }
    }

    public IImsMmTelFeature getMmTelFeatureAndListen(int i, IImsServiceFeatureCallback iImsServiceFeatureCallback) {
        ImsServiceController imsServiceControllerAndListen = getImsServiceControllerAndListen(i, 1, iImsServiceFeatureCallback);
        if (imsServiceControllerAndListen != null) {
            return imsServiceControllerAndListen.getMmTelFeature(i);
        }
        return null;
    }

    public IImsRcsFeature getRcsFeatureAndListen(int i, IImsServiceFeatureCallback iImsServiceFeatureCallback) {
        ImsServiceController imsServiceControllerAndListen = getImsServiceControllerAndListen(i, 2, iImsServiceFeatureCallback);
        if (imsServiceControllerAndListen != null) {
            return imsServiceControllerAndListen.getRcsFeature(i);
        }
        return null;
    }

    public IImsRegistration getImsRegistration(int i, int i2) throws RemoteException {
        ImsServiceController imsServiceController = getImsServiceController(i, i2);
        if (imsServiceController != null) {
            return imsServiceController.getRegistration(i);
        }
        return null;
    }

    public IImsConfig getImsConfig(int i, int i2) throws RemoteException {
        ImsServiceController imsServiceController = getImsServiceController(i, i2);
        if (imsServiceController != null) {
            return imsServiceController.getConfig(i);
        }
        return null;
    }

    @VisibleForTesting
    public ImsServiceController getImsServiceController(int i, int i2) {
        if (i < 0 || i >= this.mNumSlots) {
            return null;
        }
        synchronized (this.mBoundServicesLock) {
            SparseArray<ImsServiceController> sparseArray = this.mBoundImsServicesByFeature.get(i);
            if (sparseArray == null) {
                return null;
            }
            return sparseArray.get(i2);
        }
    }

    private SparseArray<ImsServiceController> getImsServiceControllers(int i) {
        if (i < 0 || i >= this.mNumSlots) {
            return null;
        }
        synchronized (this.mBoundServicesLock) {
            SparseArray<ImsServiceController> sparseArray = this.mBoundImsServicesByFeature.get(i);
            if (sparseArray == null) {
                return null;
            }
            return sparseArray;
        }
    }

    @VisibleForTesting
    public ImsServiceController getImsServiceControllerAndListen(int i, int i2, IImsServiceFeatureCallback iImsServiceFeatureCallback) {
        ImsServiceController imsServiceController = getImsServiceController(i, i2);
        if (imsServiceController != null) {
            imsServiceController.addImsServiceFeatureCallback(iImsServiceFeatureCallback);
            return imsServiceController;
        }
        return null;
    }

    public boolean overrideImsServiceConfiguration(int i, boolean z, String str) {
        if (i < 0 || i >= this.mNumSlots) {
            Log.w(TAG, "overrideImsServiceConfiguration: invalid slotId!");
            return false;
        }
        if (str == null) {
            Log.w(TAG, "overrideImsServiceConfiguration: null packageName!");
            return false;
        }
        Message.obtain(this.mHandler, 5, i, z ? 1 : 0, str).sendToTarget();
        return true;
    }

    public String getImsServiceConfiguration(int i, boolean z) {
        if (i >= 0 && i < this.mNumSlots) {
            return z ? this.mCarrierServices[i] : this.mDeviceService;
        }
        Log.w(TAG, "getImsServiceConfiguration: invalid slotId!");
        return "";
    }

    private void putImsController(int i, int i2, ImsServiceController imsServiceController) {
        if (i < 0 || i >= this.mNumSlots || i2 <= -1 || i2 >= 3) {
            Log.w(TAG, "putImsController received invalid parameters - slot: " + i + ", feature: " + i2);
            return;
        }
        synchronized (this.mBoundServicesLock) {
            SparseArray<ImsServiceController> sparseArray = this.mBoundImsServicesByFeature.get(i);
            if (sparseArray == null) {
                sparseArray = new SparseArray<>();
                this.mBoundImsServicesByFeature.add(i, sparseArray);
            }
            Log.i(TAG, "ImsServiceController added on slot: " + i + " with feature: " + i2 + " using package: " + imsServiceController.getComponentName());
            sparseArray.put(i2, imsServiceController);
        }
    }

    private ImsServiceController removeImsController(int i, int i2) {
        if (i < 0 || i >= this.mNumSlots || i2 <= -1 || i2 >= 3) {
            Log.w(TAG, "removeImsController received invalid parameters - slot: " + i + ", feature: " + i2);
            return null;
        }
        synchronized (this.mBoundServicesLock) {
            SparseArray<ImsServiceController> sparseArray = this.mBoundImsServicesByFeature.get(i);
            if (sparseArray == null) {
                return null;
            }
            ImsServiceController imsServiceController = sparseArray.get(i2, null);
            if (imsServiceController != null) {
                Log.i(TAG, "ImsServiceController removed on slot: " + i + " with feature: " + i2 + " using package: " + imsServiceController.getComponentName());
                sparseArray.remove(i2);
            }
            return imsServiceController;
        }
    }

    private void maybeAddedImsService(String str) {
        Log.d(TAG, "maybeAddedImsService, packageName: " + str);
        List<ImsServiceInfo> imsServiceInfo = getImsServiceInfo(str);
        ArrayList<ImsServiceInfo> arrayList = new ArrayList();
        for (ImsServiceInfo imsServiceInfo2 : imsServiceInfo) {
            ImsServiceInfo infoByComponentName = getInfoByComponentName(this.mInstalledServicesCache, imsServiceInfo2.name);
            if (infoByComponentName != null) {
                if (imsServiceInfo2.featureFromMetadata) {
                    Log.i(TAG, "Updating features in cached ImsService: " + imsServiceInfo2.name);
                    Log.d(TAG, "Updating features - Old features: " + infoByComponentName + " new features: " + imsServiceInfo2);
                    infoByComponentName.replaceFeatures(imsServiceInfo2.getSupportedFeatures());
                    updateImsServiceFeatures(imsServiceInfo2);
                } else {
                    scheduleQueryForFeatures(imsServiceInfo2);
                }
            } else {
                Log.i(TAG, "Adding newly added ImsService to cache: " + imsServiceInfo2.name);
                this.mInstalledServicesCache.put(imsServiceInfo2.name, imsServiceInfo2);
                if (imsServiceInfo2.featureFromMetadata) {
                    arrayList.add(imsServiceInfo2);
                } else {
                    scheduleQueryForFeatures(imsServiceInfo2);
                }
            }
        }
        for (ImsServiceInfo imsServiceInfo3 : arrayList) {
            if (isActiveCarrierService(imsServiceInfo3)) {
                bindImsService(imsServiceInfo3);
                updateImsServiceFeatures(getImsServiceInfoFromCache(this.mDeviceService));
            } else if (isDeviceService(imsServiceInfo3)) {
                bindImsService(imsServiceInfo3);
            }
        }
    }

    private boolean maybeRemovedImsService(String str) {
        ImsServiceInfo infoByPackageName = getInfoByPackageName(this.mInstalledServicesCache, str);
        if (infoByPackageName != null) {
            this.mInstalledServicesCache.remove(infoByPackageName.name);
            Log.i(TAG, "Removing ImsService: " + infoByPackageName.name);
            unbindImsService(infoByPackageName);
            updateImsServiceFeatures(getImsServiceInfoFromCache(this.mDeviceService));
            return true;
        }
        return false;
    }

    private boolean isActiveCarrierService(ImsServiceInfo imsServiceInfo) {
        for (int i = 0; i < this.mNumSlots; i++) {
            if (TextUtils.equals(this.mCarrierServices[i], imsServiceInfo.name.getPackageName())) {
                return true;
            }
        }
        return false;
    }

    private boolean isDeviceService(ImsServiceInfo imsServiceInfo) {
        return TextUtils.equals(this.mDeviceService, imsServiceInfo.name.getPackageName());
    }

    private int getSlotForActiveCarrierService(ImsServiceInfo imsServiceInfo) {
        for (int i = 0; i < this.mNumSlots; i++) {
            if (TextUtils.equals(this.mCarrierServices[i], imsServiceInfo.name.getPackageName())) {
                return i;
            }
        }
        return -1;
    }

    private ImsServiceController getControllerByServiceInfo(Map<ComponentName, ImsServiceController> map, final ImsServiceInfo imsServiceInfo) {
        return map.values().stream().filter(new Predicate() {
            @Override
            public final boolean test(Object obj) {
                return Objects.equals(((ImsServiceController) obj).getComponentName(), imsServiceInfo.name);
            }
        }).findFirst().orElse(null);
    }

    private ImsServiceInfo getInfoByPackageName(Map<ComponentName, ImsServiceInfo> map, final String str) {
        return map.values().stream().filter(new Predicate() {
            @Override
            public final boolean test(Object obj) {
                return Objects.equals(((ImsResolver.ImsServiceInfo) obj).name.getPackageName(), str);
            }
        }).findFirst().orElse(null);
    }

    private ImsServiceInfo getInfoByComponentName(Map<ComponentName, ImsServiceInfo> map, ComponentName componentName) {
        return map.get(componentName);
    }

    private void updateImsServiceFeatures(ImsServiceInfo imsServiceInfo) {
        if (imsServiceInfo == null) {
            return;
        }
        ImsServiceController controllerByServiceInfo = getControllerByServiceInfo(this.mActiveControllers, imsServiceInfo);
        HashSet<ImsFeatureConfiguration.FeatureSlotPair> hashSetCalculateFeaturesToCreate = calculateFeaturesToCreate(imsServiceInfo);
        if (shouldFeaturesCauseBind(hashSetCalculateFeaturesToCreate)) {
            try {
                if (controllerByServiceInfo != null) {
                    Log.i(TAG, "Updating features for ImsService: " + controllerByServiceInfo.getComponentName());
                    Log.d(TAG, "Updating Features - New Features: " + hashSetCalculateFeaturesToCreate);
                    controllerByServiceInfo.changeImsServiceFeatures(hashSetCalculateFeaturesToCreate);
                } else {
                    Log.i(TAG, "updateImsServiceFeatures: unbound with active features, rebinding");
                    bindImsServiceWithFeatures(imsServiceInfo, hashSetCalculateFeaturesToCreate);
                }
                if (isActiveCarrierService(imsServiceInfo) && !TextUtils.equals(imsServiceInfo.name.getPackageName(), this.mDeviceService)) {
                    Log.i(TAG, "Updating device default");
                    updateImsServiceFeatures(getImsServiceInfoFromCache(this.mDeviceService));
                    return;
                }
                return;
            } catch (RemoteException e) {
                Log.e(TAG, "updateImsServiceFeatures: Remote Exception: " + e.getMessage());
                return;
            }
        }
        if (controllerByServiceInfo != null) {
            Log.i(TAG, "Unbinding: features = 0 for ImsService: " + controllerByServiceInfo.getComponentName());
            unbindImsService(imsServiceInfo);
        }
    }

    private void bindImsService(ImsServiceInfo imsServiceInfo) {
        if (imsServiceInfo == null) {
            return;
        }
        bindImsServiceWithFeatures(imsServiceInfo, calculateFeaturesToCreate(imsServiceInfo));
    }

    private void bindImsServiceWithFeatures(ImsServiceInfo imsServiceInfo, HashSet<ImsFeatureConfiguration.FeatureSlotPair> hashSet) {
        if (shouldFeaturesCauseBind(hashSet)) {
            ImsServiceController controllerByServiceInfo = getControllerByServiceInfo(this.mActiveControllers, imsServiceInfo);
            if (controllerByServiceInfo != null) {
                Log.i(TAG, "ImsService connection exists, updating features " + hashSet);
                try {
                    controllerByServiceInfo.changeImsServiceFeatures(hashSet);
                } catch (RemoteException e) {
                    Log.w(TAG, "bindImsService: error=" + e.getMessage());
                }
            } else {
                controllerByServiceInfo = imsServiceInfo.controllerFactory.create(this.mContext, imsServiceInfo.name, this);
                Log.i(TAG, "Binding ImsService: " + controllerByServiceInfo.getComponentName() + " with features: " + hashSet);
                controllerByServiceInfo.bind(hashSet);
            }
            this.mActiveControllers.put(imsServiceInfo.name, controllerByServiceInfo);
        }
    }

    private void unbindImsService(ImsServiceInfo imsServiceInfo) {
        ImsServiceController controllerByServiceInfo;
        if (imsServiceInfo != null && (controllerByServiceInfo = getControllerByServiceInfo(this.mActiveControllers, imsServiceInfo)) != null) {
            try {
                Log.i(TAG, "Unbinding ImsService: " + controllerByServiceInfo.getComponentName());
                controllerByServiceInfo.unbind();
            } catch (RemoteException e) {
                Log.e(TAG, "unbindImsService: Remote Exception: " + e.getMessage());
            }
            this.mActiveControllers.remove(imsServiceInfo.name);
        }
    }

    private HashSet<ImsFeatureConfiguration.FeatureSlotPair> calculateFeaturesToCreate(ImsServiceInfo imsServiceInfo) {
        HashSet<ImsFeatureConfiguration.FeatureSlotPair> hashSet = new HashSet<>();
        final int slotForActiveCarrierService = getSlotForActiveCarrierService(imsServiceInfo);
        if (slotForActiveCarrierService != -1) {
            hashSet.addAll((Collection) imsServiceInfo.getSupportedFeatures().stream().filter(new Predicate() {
                @Override
                public final boolean test(Object obj) {
                    return ImsResolver.lambda$calculateFeaturesToCreate$3(slotForActiveCarrierService, (ImsFeatureConfiguration.FeatureSlotPair) obj);
                }
            }).collect(Collectors.toList()));
        } else if (isDeviceService(imsServiceInfo)) {
            for (final int i = 0; i < this.mNumSlots; i++) {
                ImsServiceInfo imsServiceInfoFromCache = getImsServiceInfoFromCache(this.mCarrierServices[i]);
                if (imsServiceInfoFromCache == null) {
                    hashSet.addAll((Collection) imsServiceInfo.getSupportedFeatures().stream().filter(new Predicate() {
                        @Override
                        public final boolean test(Object obj) {
                            return ImsResolver.lambda$calculateFeaturesToCreate$4(i, (ImsFeatureConfiguration.FeatureSlotPair) obj);
                        }
                    }).collect(Collectors.toList()));
                } else {
                    HashSet hashSet2 = new HashSet(imsServiceInfo.getSupportedFeatures());
                    hashSet2.removeAll(imsServiceInfoFromCache.getSupportedFeatures());
                    hashSet.addAll((Collection) hashSet2.stream().filter(new Predicate() {
                        @Override
                        public final boolean test(Object obj) {
                            return ImsResolver.lambda$calculateFeaturesToCreate$5(i, (ImsFeatureConfiguration.FeatureSlotPair) obj);
                        }
                    }).collect(Collectors.toList()));
                }
            }
        }
        return hashSet;
    }

    static boolean lambda$calculateFeaturesToCreate$3(int i, ImsFeatureConfiguration.FeatureSlotPair featureSlotPair) {
        return i == featureSlotPair.slotId;
    }

    static boolean lambda$calculateFeaturesToCreate$4(int i, ImsFeatureConfiguration.FeatureSlotPair featureSlotPair) {
        return i == featureSlotPair.slotId;
    }

    static boolean lambda$calculateFeaturesToCreate$5(int i, ImsFeatureConfiguration.FeatureSlotPair featureSlotPair) {
        return i == featureSlotPair.slotId;
    }

    @Override
    public void imsServiceFeatureCreated(int i, int i2, ImsServiceController imsServiceController) {
        putImsController(i, i2, imsServiceController);
    }

    @Override
    public void imsServiceFeatureRemoved(int i, int i2, ImsServiceController imsServiceController) {
        removeImsController(i, i2);
    }

    @Override
    public void imsServiceFeaturesChanged(ImsFeatureConfiguration imsFeatureConfiguration, ImsServiceController imsServiceController) {
        if (imsServiceController == null || imsFeatureConfiguration == null) {
            return;
        }
        Log.i(TAG, "imsServiceFeaturesChanged: config=" + imsFeatureConfiguration.getServiceFeatures() + ", ComponentName=" + imsServiceController.getComponentName());
        handleFeaturesChanged(imsServiceController.getComponentName(), imsFeatureConfiguration.getServiceFeatures());
    }

    private boolean shouldFeaturesCauseBind(HashSet<ImsFeatureConfiguration.FeatureSlotPair> hashSet) {
        return hashSet.stream().filter(new Predicate() {
            @Override
            public final boolean test(Object obj) {
                return ImsResolver.lambda$shouldFeaturesCauseBind$6((ImsFeatureConfiguration.FeatureSlotPair) obj);
            }
        }).count() > 0;
    }

    static boolean lambda$shouldFeaturesCauseBind$6(ImsFeatureConfiguration.FeatureSlotPair featureSlotPair) {
        return featureSlotPair.featureType != 0;
    }

    private void maybeRebindService(int i, String str) {
        if (i <= -1) {
            for (int i2 = 0; i2 < this.mNumSlots; i2++) {
                updateBoundCarrierServices(i2, str);
            }
            return;
        }
        updateBoundCarrierServices(i, str);
    }

    private void carrierConfigChanged(int i) {
        PersistableBundle configForSubId = this.mCarrierConfigManager.getConfigForSubId(this.mSubscriptionManagerProxy.getSubId(i));
        if (configForSubId != null) {
            maybeRebindService(i, configForSubId.getString("config_ims_package_override_string", null));
        } else {
            Log.w(TAG, "carrierConfigChanged: CarrierConfig is null!");
        }
    }

    private void updateBoundCarrierServices(int i, String str) {
        if (i > -1 && i < this.mNumSlots) {
            String str2 = this.mCarrierServices[i];
            this.mCarrierServices[i] = str;
            if (!TextUtils.equals(str, str2)) {
                Log.i(TAG, "Carrier Config updated, binding new ImsService");
                unbindImsService(getImsServiceInfoFromCache(str2));
                ImsServiceInfo imsServiceInfoFromCache = getImsServiceInfoFromCache(str);
                if (imsServiceInfoFromCache == null || imsServiceInfoFromCache.featureFromMetadata) {
                    bindImsService(imsServiceInfoFromCache);
                    updateImsServiceFeatures(getImsServiceInfoFromCache(this.mDeviceService));
                } else {
                    scheduleQueryForFeatures(imsServiceInfoFromCache);
                }
            }
        }
    }

    private void scheduleQueryForFeatures(ImsServiceInfo imsServiceInfo, int i) {
        if (!isDeviceService(imsServiceInfo) && getSlotForActiveCarrierService(imsServiceInfo) == -1) {
            Log.i(TAG, "scheduleQueryForFeatures: skipping query for ImsService that is not set as carrier/device ImsService.");
            return;
        }
        Message messageObtain = Message.obtain(this.mHandler, 3, imsServiceInfo);
        if (this.mHandler.hasMessages(3, imsServiceInfo)) {
            Log.d(TAG, "scheduleQueryForFeatures: dynamic query for " + imsServiceInfo.name + " already scheduled");
            return;
        }
        Log.d(TAG, "scheduleQueryForFeatures: starting dynamic query for " + imsServiceInfo.name + " in " + i + "ms.");
        this.mHandler.sendMessageDelayed(messageObtain, (long) i);
    }

    private void scheduleQueryForFeatures(ComponentName componentName, int i) {
        ImsServiceInfo imsServiceInfoFromCache = getImsServiceInfoFromCache(componentName.getPackageName());
        if (imsServiceInfoFromCache == null) {
            Log.w(TAG, "scheduleQueryForFeatures: Couldn't find cached info for name: " + componentName);
            return;
        }
        scheduleQueryForFeatures(imsServiceInfoFromCache, i);
    }

    private void scheduleQueryForFeatures(ImsServiceInfo imsServiceInfo) {
        scheduleQueryForFeatures(imsServiceInfo, 0);
    }

    private void handleFeaturesChanged(ComponentName componentName, Set<ImsFeatureConfiguration.FeatureSlotPair> set) {
        SomeArgs someArgsObtain = SomeArgs.obtain();
        someArgsObtain.arg1 = componentName;
        someArgsObtain.arg2 = set;
        this.mHandler.obtainMessage(4, someArgsObtain).sendToTarget();
    }

    private void startDynamicQuery(ImsServiceInfo imsServiceInfo) {
        if (!this.mFeatureQueryManager.startQuery(imsServiceInfo.name, imsServiceInfo.controllerFactory.getServiceInterface())) {
            Log.w(TAG, "startDynamicQuery: service could not connect. Retrying after delay.");
            scheduleQueryForFeatures(imsServiceInfo, DELAY_DYNAMIC_QUERY_MS);
        } else {
            Log.d(TAG, "startDynamicQuery: Service queried, waiting for response.");
        }
    }

    private void dynamicQueryComplete(ComponentName componentName, Set<ImsFeatureConfiguration.FeatureSlotPair> set) {
        ImsServiceInfo imsServiceInfoFromCache = getImsServiceInfoFromCache(componentName.getPackageName());
        if (imsServiceInfoFromCache == null) {
            Log.w(TAG, "handleFeaturesChanged: Couldn't find cached info for name: " + componentName);
            return;
        }
        imsServiceInfoFromCache.replaceFeatures(set);
        if (isActiveCarrierService(imsServiceInfoFromCache)) {
            bindImsService(imsServiceInfoFromCache);
            updateImsServiceFeatures(getImsServiceInfoFromCache(this.mDeviceService));
        } else if (isDeviceService(imsServiceInfoFromCache)) {
            bindImsService(imsServiceInfoFromCache);
        }
    }

    public boolean isResolvingBinding() {
        return this.mHandler.hasMessages(3) || this.mHandler.hasMessages(4) || this.mFeatureQueryManager.isQueryInProgress();
    }

    private String printFeatures(Set<ImsFeatureConfiguration.FeatureSlotPair> set) {
        StringBuilder sb = new StringBuilder();
        sb.append("features: [");
        if (set != null) {
            for (ImsFeatureConfiguration.FeatureSlotPair featureSlotPair : set) {
                sb.append("{");
                sb.append(featureSlotPair.slotId);
                sb.append(",");
                sb.append(featureSlotPair.featureType);
                sb.append("} ");
            }
            sb.append("]");
        }
        return sb.toString();
    }

    @VisibleForTesting
    public ImsServiceInfo getImsServiceInfoFromCache(String str) {
        ImsServiceInfo infoByPackageName;
        if (TextUtils.isEmpty(str) || (infoByPackageName = getInfoByPackageName(this.mInstalledServicesCache, str)) == null) {
            return null;
        }
        return infoByPackageName;
    }

    private List<ImsServiceInfo> getImsServiceInfo(String str) {
        ArrayList arrayList = new ArrayList();
        if (!this.mIsDynamicBinding) {
            arrayList.addAll(getStaticImsService());
        } else {
            arrayList.addAll(searchForImsServices(str, this.mImsServiceControllerFactory));
            arrayList.addAll(searchForImsServices(str, this.mImsServiceControllerFactoryCompat));
        }
        return arrayList;
    }

    private List<ImsServiceInfo> getStaticImsService() {
        ArrayList arrayList = new ArrayList();
        ImsServiceInfo imsServiceInfo = new ImsServiceInfo(this.mNumSlots);
        imsServiceInfo.name = this.mStaticComponent;
        imsServiceInfo.controllerFactory = this.mImsServiceControllerFactoryStaticBindingCompat;
        imsServiceInfo.addFeatureForAllSlots(0);
        imsServiceInfo.addFeatureForAllSlots(1);
        arrayList.add(imsServiceInfo);
        return arrayList;
    }

    private List<ImsServiceInfo> searchForImsServices(String str, ImsServiceControllerFactory imsServiceControllerFactory) {
        ArrayList arrayList = new ArrayList();
        Intent intent = new Intent(imsServiceControllerFactory.getServiceInterface());
        intent.setPackage(str);
        Iterator it = this.mContext.getPackageManager().queryIntentServicesAsUser(intent, 128, this.mContext.getUserId()).iterator();
        while (it.hasNext()) {
            ServiceInfo serviceInfo = ((ResolveInfo) it.next()).serviceInfo;
            if (serviceInfo != null) {
                ImsServiceInfo imsServiceInfo = new ImsServiceInfo(this.mNumSlots);
                imsServiceInfo.name = new ComponentName(serviceInfo.packageName, serviceInfo.name);
                imsServiceInfo.controllerFactory = imsServiceControllerFactory;
                if (isDeviceService(imsServiceInfo) || this.mImsServiceControllerFactoryCompat == imsServiceControllerFactory) {
                    if (serviceInfo.metaData != null) {
                        if (serviceInfo.metaData.getBoolean(METADATA_EMERGENCY_MMTEL_FEATURE, false)) {
                            imsServiceInfo.addFeatureForAllSlots(0);
                        }
                        if (serviceInfo.metaData.getBoolean(METADATA_MMTEL_FEATURE, false)) {
                            imsServiceInfo.addFeatureForAllSlots(1);
                        }
                        if (serviceInfo.metaData.getBoolean(METADATA_RCS_FEATURE, false)) {
                            imsServiceInfo.addFeatureForAllSlots(2);
                        }
                    }
                    if (this.mImsServiceControllerFactoryCompat != imsServiceControllerFactory && imsServiceInfo.getSupportedFeatures().isEmpty()) {
                        imsServiceInfo.featureFromMetadata = false;
                    }
                } else {
                    imsServiceInfo.featureFromMetadata = false;
                }
                Log.i(TAG, "service name: " + imsServiceInfo.name + ", manifest query: " + imsServiceInfo.featureFromMetadata);
                if (TextUtils.equals(serviceInfo.permission, "android.permission.BIND_IMS_SERVICE") || serviceInfo.metaData.getBoolean(METADATA_OVERRIDE_PERM_CHECK, false)) {
                    arrayList.add(imsServiceInfo);
                } else {
                    Log.w(TAG, "ImsService is not protected with BIND_IMS_SERVICE permission: " + imsServiceInfo.name);
                }
            }
        }
        return arrayList;
    }
}
