package com.mediatek.appworkingset;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Debug;
import android.os.Process;
import android.os.SystemProperties;
import android.os.Trace;
import android.os.UserHandle;
import android.util.ArrayMap;
import android.util.Log;
import android.util.SparseArray;
import com.android.internal.app.ProcessMap;
import com.android.internal.util.MemInfoReader;
import com.mediatek.am.AMEventHookData;
import com.mediatek.am.IAWSProcessRecord;
import com.mediatek.am.IAWSStoreRecord;
import com.mediatek.am.ProcessADJ;
import com.mediatek.apm.frc.FocusRelationshipChainPolicy;
import com.mediatek.apm.suppression.SuppressionPolicy;
import com.mediatek.aws.CustomProtectProcess;
import com.mediatek.datashaping.DataShapingUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;

public final class AWSManager {
    static final int APPLICATION_ACTIVITY_TYPE = 0;
    static final boolean CONFIG_ACCURATE = true;
    static final boolean DEBUG = false;
    static final boolean DEBUG_DB = false;
    static final boolean DEBUG_KILL = false;
    static final boolean DEBUG_PRIORITY = false;
    static final boolean DEBUG_RECORD = false;
    static final boolean DEBUG_SUPPRESS = false;
    static final boolean DEBUG_SWITCH = false;
    static final int HOME_ACTIVITY_TYPE = 1;
    static final int MAX_SUPPRESSION_TIME = 5000;
    private static final int MEM_BACK_TO_HOME = 2;
    private static final int MEM_IN_COMING_CALL = 3;
    private static final int MEM_LAUNCHED_PROCESS = 1;
    private static final int MEM_SAMPLE_PROCESS = 0;
    static final int MIN_DB_COMMIT_TIME = 180000;
    static final int MPO_SUPPRESS_ACTION = 545;
    static final int MaxPkgPriorityNode = 100;
    public static final int PROCESS_STATE_BOUND_FOREGROUND_SERVICE = 3;
    public static final int PROCESS_STATE_TOP = 2;
    static final int RECENTS_ACTIVITY_TYPE = 2;
    private static final String TAG = "AWSManager";
    static final int defaultAppNeeded = 30720;
    static boolean mIsReady;
    private static AWSManager sInstance;
    private AWSDBHelper db;
    private Timer dbTimer;
    ActivityManager mAm;
    Context mContext;
    private String mLaunchingPkgName;
    int mNumPkgPriorityNode;
    protected PkgPriorityNode mPDirty;
    static final String[] nativeRecordProcess = {"cameraserver", "mediaserver"};
    private static int PERCEPTIBLE_APP_ADJ = ProcessADJ.PERCEPTIBLE_APP_ADJ;
    ArrayMap<String, Integer> mNativePids = new ArrayMap<>();
    ArrayMap<String, Integer> mNativeUids = new ArrayMap<>();
    final ProcessMap<ProcessRecordStore> mProcessNames = new ProcessMap<>();
    final ArrayMap<String, PkgPriorityNode> mPackagesProcessMap = new ArrayMap<>();
    protected PkgPriorityNode mPHead = new PkgPriorityNode();
    protected PkgPriorityNode mPTail = new PkgPriorityNode();
    String foregroundApp = null;
    boolean frcFlag = false;
    private FocusRelationshipChainPolicy mFrcPolicy = FocusRelationshipChainPolicy.getInstance();
    private SuppressionPolicy mSuppressPolicy = SuppressionPolicy.getInstance();
    private Timer suppressTimer = null;
    private TimerTask suppressTimerTask = null;

    public AWSManager(Context context) {
        if (context == null) {
            Log.e(TAG, "Initializing with context's wrong:" + context);
            return;
        }
        mIsReady = false;
        this.mNumPkgPriorityNode = 0;
        this.mPDirty = null;
        this.mContext = context;
        this.mAm = (ActivityManager) this.mContext.getSystemService("activity");
        if (Build.VERSION.SDK_INT > 23) {
            Log.v(TAG, "Version > 23, perceptible adj = " + PERCEPTIBLE_APP_ADJ);
        }
        this.dbTimer = null;
        this.db = new AWSDBHelper(context, this);
        if (this.db == null) {
            Log.e(TAG, "Initializing with something's wrong:" + context + "," + this.db);
            return;
        }
        registerForPackageRemoval(context);
        registerForScreenOnOff(context);
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (AWSManager.this.db) {
                    AWSManager.this.db.readDB();
                }
                AWSManager.mIsReady = true;
                Log.v(AWSManager.TAG, "Initialized done");
            }
        }).start();
    }

    private void registerForPackageRemoval(Context context) {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.PACKAGE_REMOVED");
        if (context == null) {
            Log.e(TAG, "context = null");
        } else {
            context.registerReceiverAsUser(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context2, Intent intent) {
                    switch (intent.getAction()) {
                        case "android.intent.action.PACKAGE_REMOVED":
                            if (!intent.getBooleanExtra("android.intent.extra.REPLACING", false)) {
                                int intExtra = intent.getIntExtra("android.intent.extra.UID", -1);
                                Uri data = intent.getData();
                                if (intExtra != -1 && data != null) {
                                    String schemeSpecificPart = data.getSchemeSpecificPart();
                                    Log.v(AWSManager.TAG, "Pkg removal:" + schemeSpecificPart);
                                    AWSManager.this.clearProcList(schemeSpecificPart);
                                    AWSManager.this.clearLaunchProcList(schemeSpecificPart);
                                }
                                break;
                            }
                            break;
                    }
                }
            }, UserHandle.ALL, intentFilter, null, null);
        }
    }

    private void registerForScreenOnOff(Context context) {
        if (context == null) {
            Log.e(TAG, "context = null");
            return;
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.SCREEN_OFF");
        intentFilter.addAction("android.intent.action.SCREEN_ON");
        context.registerReceiverAsUser(new AnonymousClass3(), UserHandle.ALL, intentFilter, null, null);
    }

    class AnonymousClass3 extends BroadcastReceiver {
        AnonymousClass3() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case "android.intent.action.SCREEN_OFF":
                    if (AWSManager.mIsReady) {
                        if (AWSManager.this.dbTimer == null) {
                            AWSManager.this.dbTimer = new Timer();
                        }
                        AWSManager.this.dbTimer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Log.v(AWSManager.TAG, "Updating DB");
                                        synchronized (AWSManager.this.db) {
                                            AWSManager.this.db.updateDB();
                                        }
                                        if (AWSManager.this.dbTimer != null) {
                                            AWSManager.this.dbTimer.cancel();
                                            AWSManager.this.dbTimer = null;
                                        }
                                    }
                                }).start();
                            }
                        }, 180000L);
                        break;
                    }
                    break;
                case "android.intent.action.SCREEN_ON":
                    if (AWSManager.this.dbTimer != null) {
                        AWSManager.this.dbTimer.cancel();
                        AWSManager.this.dbTimer = null;
                        break;
                    }
                    break;
            }
        }
    }

    public static AWSManager getInstance(AMEventHookData.SystemReady systemReady) {
        if (!SystemProperties.get("ro.mtk_aws_support").equals("1")) {
            Log.d(TAG, "AWSManager not enabled");
            return null;
        }
        if (sInstance == null) {
            sInstance = new AWSManager((Context) systemReady.get(AMEventHookData.SystemReady.Index.context));
        }
        return sInstance;
    }

    public static AWSManager getInstance() {
        if (!SystemProperties.get("ro.mtk_aws_support").equals("1")) {
            Log.e(TAG, "AWSManager not enabled");
            return null;
        }
        if (sInstance != null) {
            return sInstance;
        }
        Log.v(TAG, "AWSManager get null instance, system not ready?");
        return null;
    }

    public int onBeforeActivitySwitch(AMEventHookData.BeforeActivitySwitch beforeActivitySwitch) {
        if (!IsSystemAndReady("onBeforeActivitySwitch")) {
            return 0;
        }
        boolean z = beforeActivitySwitch.getBoolean(AMEventHookData.BeforeActivitySwitch.Index.isNeedToPauseActivityFirst);
        String string = beforeActivitySwitch.getString(AMEventHookData.BeforeActivitySwitch.Index.lastResumedPackageName);
        String string2 = beforeActivitySwitch.getString(AMEventHookData.BeforeActivitySwitch.Index.nextResumedPackageName);
        int i = beforeActivitySwitch.getInt(AMEventHookData.BeforeActivitySwitch.Index.lastResumedActivityType);
        int i2 = beforeActivitySwitch.getInt(AMEventHookData.BeforeActivitySwitch.Index.nextResumedActivityType);
        if (z) {
            Trace.traceBegin(64L, "MPO_Suppress1");
            if (this.mFrcPolicy == null || this.mSuppressPolicy == null) {
                Log.e(TAG, "[onBeforeActivitySwitch] getInstance = null");
            } else if (i != 0 && i2 == 0 && !this.frcFlag) {
                this.mFrcPolicy.startFrc("MPO", 2, null);
                this.frcFlag = true;
            }
            Trace.traceEnd(64L);
            return 0;
        }
        Trace.traceBegin(64L, "MPO_Suppress2");
        if (this.mFrcPolicy == null || this.mSuppressPolicy == null) {
            Log.e(TAG, "[onBeforeActivitySwitch] getInstance = null");
        } else if (i != 0 && i2 == 0) {
            if (!this.frcFlag) {
                this.mFrcPolicy.startFrc("MPO", 2, null);
                this.frcFlag = true;
            }
            if (this.foregroundApp == null) {
                this.mSuppressPolicy.startSuppression("MPO", 5, MPO_SUPPRESS_ACTION, "MPO", null);
                this.foregroundApp = string2;
                if (this.suppressTimer == null) {
                    this.suppressTimer = new Timer();
                    this.suppressTimerTask = new TimerTask() {
                        @Override
                        public void run() {
                            if (AWSManager.this.mSuppressPolicy == null) {
                                SuppressionPolicy.getInstance();
                            }
                            if (AWSManager.this.mSuppressPolicy != null) {
                                AWSManager.this.mSuppressPolicy.stopSuppression("MPO");
                            } else {
                                Log.e(AWSManager.TAG, "[onBeforeActivitySwitch] mSuppressPolicy = null");
                            }
                        }
                    };
                    this.suppressTimer.schedule(this.suppressTimerTask, DataShapingUtils.CLOSING_DELAY_BUFFER_FOR_MUSIC);
                }
            }
        } else if (i == 0 && i2 != 0) {
            if (this.frcFlag) {
                this.mFrcPolicy.stopFrc("MPO");
                this.frcFlag = false;
                if (this.foregroundApp != null) {
                    this.mSuppressPolicy.stopSuppression("MPO");
                    this.foregroundApp = null;
                    if (this.suppressTimer != null) {
                        this.suppressTimer.cancel();
                        this.suppressTimer = null;
                    }
                }
            }
        } else if (this.frcFlag && this.foregroundApp != null) {
            this.mSuppressPolicy.stopSuppression("MPO");
            this.foregroundApp = null;
            if (this.suppressTimer != null) {
                this.suppressTimer.cancel();
                this.suppressTimer = null;
            }
        }
        Trace.traceEnd(64L);
        Trace.traceBegin(64L, "AWS_switch");
        final AWSLaunchRecord aWSLaunchRecord = new AWSLaunchRecord(string, string2, i, i2, beforeActivitySwitch.getInt(AMEventHookData.BeforeActivitySwitch.Index.waitProcessPid), (ArrayList) beforeActivitySwitch.get(AMEventHookData.BeforeActivitySwitch.Index.runningProcRecords));
        String prevPkgName = aWSLaunchRecord.getPrevPkgName();
        String nextPkgName = aWSLaunchRecord.getNextPkgName();
        if (prevPkgName != null && nextPkgName != null && !nextPkgName.equalsIgnoreCase(prevPkgName)) {
            boolean zContains = nextPkgName.contains("com.android.packageinstaller");
            if (!aWSLaunchRecord.isLaunchingToRecentApp() && !zContains) {
                this.mLaunchingPkgName = nextPkgName;
                try {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            AWSManager.this.ensureWorkingSet(aWSLaunchRecord);
                        }
                    }).start();
                } catch (Exception e) {
                    Log.e(TAG, "Exception thrown during ensureWorkingSet:", e);
                }
            } else {
                Trace.traceEnd(64L);
                return 0;
            }
        }
        Trace.traceEnd(64L);
        return 0;
    }

    public int recordST(int i, int i2, String str) {
        if (IsSystemAndReady("recordST") && str != null && Arrays.asList(nativeRecordProcess).contains(str)) {
            this.mNativePids.put(str, Integer.valueOf(i));
            this.mNativeUids.put(str, Integer.valueOf(i2));
        }
        return 0;
    }

    public int storeRecord(IAWSStoreRecord iAWSStoreRecord) {
        if (!IsSystemAndReady("storeRecord")) {
            return 0;
        }
        if (iAWSStoreRecord == null) {
            Log.e(TAG, "storeRecord, record is null");
            return -1;
        }
        switch ((int) (iAWSStoreRecord.getExtraVal() * (-1))) {
            case 1:
            case 2:
            case 3:
                recordLaunchMemory(iAWSStoreRecord);
                break;
            default:
                recordProcMemory(iAWSStoreRecord);
                break;
        }
        return 0;
    }

    private int recordProcMemory(IAWSStoreRecord iAWSStoreRecord) {
        updateProcessNames(iAWSStoreRecord.getRecord()).updateSampledMem(iAWSStoreRecord.getExtraVal());
        return 0;
    }

    private int recordLaunchMemory(IAWSStoreRecord iAWSStoreRecord) {
        String topPkgName = iAWSStoreRecord.getTopPkgName();
        if (topPkgName == null) {
            Log.e(TAG, "When recoring launch Mem, Top Pkg null");
            return -1;
        }
        if (!topPkgName.equals(this.mLaunchingPkgName)) {
            return 0;
        }
        clearLaunchProcList(topPkgName);
        ArrayList<String> arrayList = new ArrayList();
        for (IAWSProcessRecord iAWSProcessRecord : new AWSStoreRecord(iAWSStoreRecord).getRecords()) {
            ArrayMap arrayMap = new ArrayMap(iAWSProcessRecord.getpkgList());
            if (arrayMap.size() != 0) {
                for (int i = 0; i < arrayMap.size(); i++) {
                    ProcessRecordStore processRecordStoreUpdateProcessNames = updateProcessNames(iAWSProcessRecord);
                    String str = (String) arrayMap.keyAt(i);
                    if (topPkgName.equals(str) || isDepenPkg(topPkgName, str)) {
                        long pss = getPss(iAWSProcessRecord.getPid());
                        processRecordStoreUpdateProcessNames.updateSampledMem(pss);
                        updateLaunchProcList(topPkgName, processRecordStoreUpdateProcessNames);
                        processRecordStoreUpdateProcessNames.updateLaunchMem(topPkgName, pss);
                        ArrayList<String> depNaitveProcs = getDepNaitveProcs(iAWSProcessRecord);
                        if (depNaitveProcs != null) {
                            for (String str2 : depNaitveProcs) {
                                if (!arrayList.contains(str2)) {
                                    arrayList.add(str2);
                                }
                            }
                        }
                    }
                }
            }
        }
        for (String str3 : arrayList) {
            Integer num = this.mNativePids.get(str3);
            Integer num2 = this.mNativeUids.get(str3);
            if (num != null && num2 != null) {
                long pss2 = getPss(num.intValue());
                ProcessRecordStore processRecordStore = (ProcessRecordStore) this.mProcessNames.get(str3, num2.intValue());
                if (processRecordStore == null) {
                    processRecordStore = new ProcessRecordStore(str3, num2.intValue(), num.intValue());
                    updateProcessNames(processRecordStore);
                }
                processRecordStore.updateSampledMem(pss2);
                updateLaunchProcList(topPkgName, processRecordStore);
                processRecordStore.updateLaunchMem(topPkgName, pss2);
            }
        }
        return 0;
    }

    private boolean isDepenPkg(String str, String str2) {
        return false;
    }

    private ArrayList<String> getDepNaitveProcs(IAWSProcessRecord iAWSProcessRecord) {
        ArrayList<String> arrayList = new ArrayList<>();
        boolean zContains = iAWSProcessRecord.getProcName().contains("com.android.camera");
        boolean zContains2 = iAWSProcessRecord.getProcName().contains("com.mediatek.camera");
        if (!zContains && !zContains2) {
            return null;
        }
        for (String str : nativeRecordProcess) {
            arrayList.add(str);
        }
        return arrayList;
    }

    protected ProcessRecordStore updateProcessNames(IAWSProcessRecord iAWSProcessRecord) {
        ProcessRecordStore processRecordStore;
        synchronized (this.mProcessNames) {
            processRecordStore = (ProcessRecordStore) this.mProcessNames.get(iAWSProcessRecord.getProcName(), iAWSProcessRecord.getUid());
            if (processRecordStore == null) {
                processRecordStore = new ProcessRecordStore(iAWSProcessRecord);
                this.mProcessNames.put(iAWSProcessRecord.getProcName(), iAWSProcessRecord.getUid(), processRecordStore);
            } else {
                processRecordStore.update(iAWSProcessRecord);
            }
        }
        return processRecordStore;
    }

    protected void updateLaunchProcList(String str, ProcessRecordStore processRecordStore) {
        synchronized (this.mPackagesProcessMap) {
            PkgPriorityNode pkgPriorityNode = this.mPackagesProcessMap.get(str);
            if (pkgPriorityNode == null) {
                pkgPriorityNode = new PkgPriorityNode(str, processRecordStore);
                this.mPackagesProcessMap.put(str, pkgPriorityNode);
                addPkgPriorityNodeToHead(pkgPriorityNode);
            } else {
                updatePkgPriorityNode(pkgPriorityNode);
            }
            ArrayList<ProcessRecordStore> arrayList = pkgPriorityNode.procList;
            if (arrayList != null) {
                if (!arrayList.contains(processRecordStore)) {
                    arrayList.add(processRecordStore);
                }
            } else {
                Log.e(TAG, "Adding PRS to non existing prsList at pkg" + str);
            }
        }
    }

    private int ensureWorkingSet(AWSLaunchRecord aWSLaunchRecord) {
        aWSLaunchRecord.getNextPkgName();
        long jDiffFromHistory = diffFromHistory(aWSLaunchRecord);
        MemInfoReader memInfoReader = new MemInfoReader();
        memInfoReader.readMemInfo();
        long[] rawInfo = memInfoReader.getRawInfo();
        long j = jDiffFromHistory - ((rawInfo[1] + rawInfo[3]) - rawInfo[9]);
        if (j <= 0) {
            return 0;
        }
        long j2 = rawInfo[0] / 2;
        if (j <= j2) {
            j2 = j;
        }
        int iReserveMemory = reserveMemory(aWSLaunchRecord, j2);
        if (iReserveMemory == 0) {
        }
        return iReserveMemory;
    }

    private long diffFromHistory(AWSLaunchRecord aWSLaunchRecord) {
        PkgPriorityNode pkgPriorityNode;
        long j;
        String nextPkgName = aWSLaunchRecord.getNextPkgName();
        ArrayList<IAWSProcessRecord> runningProcessesRecords = aWSLaunchRecord.getRunningProcessesRecords();
        synchronized (this.mPackagesProcessMap) {
            pkgPriorityNode = this.mPackagesProcessMap.get(nextPkgName);
        }
        if (pkgPriorityNode == null) {
            return 30720L;
        }
        synchronized (this.mPackagesProcessMap) {
            j = 0;
            for (ProcessRecordStore processRecordStore : pkgPriorityNode.procList) {
                long sampledMem = 0;
                long launchMem = processRecordStore.getLaunchMem(nextPkgName);
                for (IAWSProcessRecord iAWSProcessRecord : runningProcessesRecords) {
                    if (processRecordStore.getProcName().equals(iAWSProcessRecord.getProcName())) {
                        processRecordStore.getSampledMem();
                        sampledMem = getPss(iAWSProcessRecord.getPid());
                        processRecordStore.updateSampledMem(sampledMem);
                    }
                }
                if (Arrays.asList(nativeRecordProcess).contains(processRecordStore.getProcName())) {
                    sampledMem = processRecordStore.getSampledMem();
                    Integer num = this.mNativePids.get(processRecordStore.getProcName());
                    if (num != null) {
                        sampledMem = getPss(num.intValue());
                        processRecordStore.updateSampledMem(sampledMem);
                    }
                }
                j = (launchMem - sampledMem) + j;
            }
        }
        return j;
    }

    private int reserveMemory(AWSLaunchRecord aWSLaunchRecord, long j) {
        boolean z;
        ArrayList<ProcessRecordStore> killingCandidates = getKillingCandidates(aWSLaunchRecord);
        ArrayList<ProcessRecordStore> arrayList = new ArrayList();
        SparseArray sparseArray = new SparseArray();
        int i = -1;
        ArrayList arrayList2 = null;
        for (ProcessRecordStore processRecordStore : killingCandidates) {
            int adj = processRecordStore.getAdj();
            if (adj != i) {
                arrayList2 = new ArrayList();
                if (sparseArray.get(adj) == null) {
                    sparseArray.put(adj, arrayList2);
                    i = adj;
                } else {
                    Log.e(TAG, "Adj " + adj + " added before");
                    return 0;
                }
            }
            if (arrayList2 != null) {
                arrayList2.add(processRecordStore);
            } else {
                Log.e(TAG, "Adj" + i + "add fail");
                return 0;
            }
        }
        long j2 = 0;
        boolean z2 = false;
        int size = sparseArray.size() - 1;
        while (true) {
            if (size < 0) {
                break;
            }
            ArrayList arrayList3 = (ArrayList) sparseArray.get(sparseArray.keyAt(size));
            if (arrayList3.size() == 1) {
                ProcessRecordStore processRecordStore2 = (ProcessRecordStore) arrayList3.get(0);
                if (processRecordStore2 != null) {
                    long sampledMem = processRecordStore2.getSampledMem();
                    if (sampledMem == 0) {
                        sampledMem = getPss(processRecordStore2.getPid());
                    }
                    processRecordStore2.updateSampledMem(sampledMem);
                } else {
                    Log.e(TAG, "ArrayList size= 1 but element does not exsit");
                }
            } else {
                Collections.sort(arrayList3, new Comparator<ProcessRecordStore>() {
                    @Override
                    public int compare(ProcessRecordStore processRecordStore3, ProcessRecordStore processRecordStore4) {
                        long j3;
                        long j4;
                        ProcessRecordStore processRecordStore5 = (ProcessRecordStore) AWSManager.this.mProcessNames.get(processRecordStore3.getProcName(), processRecordStore3.getUid());
                        ProcessRecordStore processRecordStore6 = (ProcessRecordStore) AWSManager.this.mProcessNames.get(processRecordStore4.getProcName(), processRecordStore4.getUid());
                        if (processRecordStore5 == null) {
                            j3 = 0;
                        } else {
                            long sampledMem2 = processRecordStore5.getSampledMem();
                            if (sampledMem2 == 0) {
                                long pss = AWSManager.this.getPss(processRecordStore5.getPid());
                                processRecordStore5.updateSampledMem(pss);
                                j3 = pss;
                            } else {
                                j3 = sampledMem2;
                            }
                        }
                        if (processRecordStore6 == null) {
                            j4 = 0;
                        } else {
                            long sampledMem3 = processRecordStore6.getSampledMem();
                            if (sampledMem3 == 0) {
                                long pss2 = AWSManager.this.getPss(processRecordStore6.getPid());
                                processRecordStore6.updateSampledMem(pss2);
                                j4 = pss2;
                            } else {
                                j4 = sampledMem3;
                            }
                        }
                        return !(((j4 - j3) > 0L ? 1 : ((j4 - j3) == 0L ? 0 : -1)) <= 0) ? 1 : -1;
                    }
                });
            }
            Iterator it = arrayList3.iterator();
            long sampledMem2 = j2;
            while (true) {
                if (!it.hasNext()) {
                    z = z2;
                    break;
                }
                ProcessRecordStore processRecordStore3 = (ProcessRecordStore) it.next();
                if (!(sampledMem2 <= j)) {
                    z = true;
                    break;
                }
                arrayList.add(processRecordStore3);
                sampledMem2 = processRecordStore3.getSampledMem() + sampledMem2;
            }
            if (z) {
                j2 = sampledMem2;
                break;
            }
            size--;
            z2 = z;
            j2 = sampledMem2;
        }
        for (ProcessRecordStore processRecordStore4 : arrayList) {
            if (matchAdj(processRecordStore4.getAdj(), processRecordStore4.getPid())) {
                kill(processRecordStore4.getPid(), processRecordStore4.getProcName(), processRecordStore4.getAdj());
            }
        }
        return !(((j - j2) > 0L ? 1 : ((j - j2) == 0L ? 0 : -1)) <= 0) ? -1 : 0;
    }

    private boolean matchAdj(int i, int i2) {
        if (this.mAm == null) {
            if (this.mContext == null) {
                Log.e(TAG, "Context is null, nothing to be kill");
                return false;
            }
            this.mAm = (ActivityManager) this.mContext.getSystemService("activity");
            if (this.mAm == null) {
                Log.e(TAG, "AM is null, nothing to be kill");
                return false;
            }
        }
        ArrayMap processesWithAdj = this.mAm.getProcessesWithAdj();
        if (processesWithAdj == null) {
            Log.e(TAG, "processMap is null, nothing to be kill");
            return false;
        }
        ArrayList arrayList = (ArrayList) processesWithAdj.get(Integer.valueOf(i));
        if (arrayList == null) {
            Log.e(TAG, "pidList is null, nothing to be kill");
            return false;
        }
        Iterator it = arrayList.iterator();
        while (it.hasNext()) {
            if (i2 == ((Integer) it.next()).intValue()) {
                return true;
            }
        }
        return false;
    }

    private ArrayList<ProcessRecordStore> getKillingCandidates(AWSLaunchRecord aWSLaunchRecord) {
        String nextPkgName = aWSLaunchRecord.getNextPkgName();
        ArrayList<IAWSProcessRecord> runningProcessesRecords = aWSLaunchRecord.getRunningProcessesRecords();
        ArrayList<ProcessRecordStore> arrayList = new ArrayList<>();
        for (IAWSProcessRecord iAWSProcessRecord : runningProcessesRecords) {
            if (iAWSProcessRecord.getAdj() > PERCEPTIBLE_APP_ADJ) {
                ArrayMap arrayMap = new ArrayMap(iAWSProcessRecord.getpkgList());
                if (arrayMap.size() != 0) {
                    for (int i = 0; i < arrayMap.size(); i++) {
                        if (!((String) arrayMap.keyAt(i)).equals(nextPkgName)) {
                        }
                    }
                    if (!collectDepedencyList(aWSLaunchRecord).contains(Integer.valueOf(iAWSProcessRecord.getPid())) && !isWhitelistToKeep(iAWSProcessRecord.getProcName()) && !Arrays.asList(CustomProtectProcess.PROTECT_PROCESS_LIST).contains(iAWSProcessRecord.getProcName())) {
                        arrayList.add(updateProcessNames(iAWSProcessRecord));
                    }
                }
            }
        }
        Collections.sort(arrayList, new Comparator<ProcessRecordStore>() {
            @Override
            public int compare(ProcessRecordStore processRecordStore, ProcessRecordStore processRecordStore2) {
                if (processRecordStore.getAdj() >= processRecordStore2.getAdj()) {
                    return processRecordStore.getAdj() <= processRecordStore2.getAdj() ? 0 : -1;
                }
                return 1;
            }
        });
        return arrayList;
    }

    private ArrayList<Integer> collectDepedencyList(AWSLaunchRecord aWSLaunchRecord) {
        ArrayList<Integer> arrayList = new ArrayList<>();
        int waitProcessPID = aWSLaunchRecord.getWaitProcessPID();
        if (waitProcessPID != -1) {
            arrayList.add(Integer.valueOf(waitProcessPID));
        }
        return arrayList;
    }

    private boolean isWhitelistToKeep(String str) {
        return str.matches("android.process.media") || str.matches("android.process.acore");
    }

    private void kill(int i, String str, int i2) {
        Log.v(TAG, String.format(" Killing process:pid: %d,(%s),adj=%d", Integer.valueOf(i), str, Integer.valueOf(i2)));
        try {
            Process.killProcessQuiet(i);
        } catch (Exception e) {
            Log.e(TAG, "Exception thrown during kill:", e);
        }
    }

    private void clearProcList(String str) {
        synchronized (this.mProcessNames) {
            int size = this.mProcessNames.getMap().size();
            for (int i = 0; i < size; i++) {
                SparseArray sparseArray = (SparseArray) this.mProcessNames.getMap().valueAt(i);
                int size2 = sparseArray.size();
                for (int i2 = 0; i2 < size2; i2++) {
                    ProcessRecordStore processRecordStore = (ProcessRecordStore) sparseArray.valueAt(i2);
                    if (processRecordStore.getPkgName().equals(str)) {
                        this.mProcessNames.remove(processRecordStore.getPkgName(), processRecordStore.getUid());
                    }
                }
            }
        }
    }

    private int clearLaunchProcList(String str) {
        synchronized (this.mPackagesProcessMap) {
            PkgPriorityNode pkgPriorityNode = this.mPackagesProcessMap.get(str);
            if (pkgPriorityNode != null) {
                removePkgPriorityNode(pkgPriorityNode);
                this.mPackagesProcessMap.remove(str);
            }
        }
        return 0;
    }

    private PkgPriorityNode removePkgPriorityNode(PkgPriorityNode pkgPriorityNode) {
        if (this.mPHead.next == pkgPriorityNode && this.mPTail.prev == pkgPriorityNode) {
            this.mPHead.next = null;
            this.mPTail.prev = null;
            this.mPDirty = null;
        } else {
            pkgPriorityNode.next.prev = pkgPriorityNode.prev;
            pkgPriorityNode.prev.next = pkgPriorityNode.next;
            if (this.mPDirty == pkgPriorityNode) {
                this.mPDirty = pkgPriorityNode.prev;
            }
        }
        pkgPriorityNode.next = null;
        pkgPriorityNode.prev = null;
        this.mNumPkgPriorityNode--;
        return pkgPriorityNode;
    }

    private int addPkgPriorityNodeToHead(PkgPriorityNode pkgPriorityNode) {
        if (this.mPHead.next == null) {
            this.mPHead.next = pkgPriorityNode;
            this.mPTail.prev = pkgPriorityNode;
            this.mPDirty = pkgPriorityNode;
            pkgPriorityNode.next = this.mPTail;
            pkgPriorityNode.prev = this.mPHead;
        } else {
            this.mPHead.next.prev = pkgPriorityNode;
            pkgPriorityNode.next = this.mPHead.next;
            this.mPHead.next = pkgPriorityNode;
            pkgPriorityNode.prev = this.mPHead;
        }
        this.mNumPkgPriorityNode++;
        if (this.mNumPkgPriorityNode <= 100) {
            return 0;
        }
        clearLaunchProcList(this.mPTail.prev.pkgName);
        return 0;
    }

    private int updatePkgPriorityNode(PkgPriorityNode pkgPriorityNode) {
        if (this.mPHead.next == pkgPriorityNode) {
            return 0;
        }
        addPkgPriorityNodeToHead(removePkgPriorityNode(pkgPriorityNode));
        return 0;
    }

    private void dumpAllPriority() {
        Log.v(TAG, "[PkgPriority]dumpAllPriority------");
        PkgPriorityNode pkgPriorityNode = this.mPHead.next;
        int i = 0;
        if (pkgPriorityNode == null) {
            Log.v(TAG, "[PkgPriority]: currently empty now");
            return;
        }
        do {
            Log.v(TAG, "[PkgPriority]: index" + i + ":" + pkgPriorityNode.pkgName);
            i++;
            pkgPriorityNode = pkgPriorityNode.next;
        } while (pkgPriorityNode.next != this.mPTail.next);
        Log.v(TAG, "[PkgPriority]: Iterate to Tail-----");
    }

    private long getPss(int i) {
        long[] jArr = new long[2];
        return jArr[1] + Debug.getPss(i, jArr, null);
    }

    private boolean IsSystemAndReady(String str) {
        if (mIsReady) {
            int callingUid = Binder.getCallingUid();
            if (callingUid == 0 || callingUid == 1000) {
                return true;
            }
            throw new SecurityException(str + " called from non-system process");
        }
        Log.v(TAG, "not ready");
        return false;
    }
}
