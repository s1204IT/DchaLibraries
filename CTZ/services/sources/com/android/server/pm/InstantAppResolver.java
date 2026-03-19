package com.android.server.pm;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.ActivityInfo;
import android.content.pm.AuxiliaryResolveInfo;
import android.content.pm.InstantAppIntentFilter;
import android.content.pm.InstantAppRequest;
import android.content.pm.InstantAppResolveInfo;
import android.metrics.LogMaker;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.util.Slog;
import com.android.internal.logging.MetricsLogger;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.pm.InstantAppResolverConnection;
import com.android.server.pm.PackageManagerService;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public abstract class InstantAppResolver {
    private static final boolean DEBUG_INSTANT = Build.IS_DEBUGGABLE;
    private static final int RESOLUTION_BIND_TIMEOUT = 2;
    private static final int RESOLUTION_CALL_TIMEOUT = 3;
    private static final int RESOLUTION_FAILURE = 1;
    private static final int RESOLUTION_SUCCESS = 0;
    private static final String TAG = "PackageManager";
    private static MetricsLogger sMetricsLogger;

    @Retention(RetentionPolicy.SOURCE)
    public @interface ResolutionStatus {
    }

    private static MetricsLogger getLogger() {
        if (sMetricsLogger == null) {
            sMetricsLogger = new MetricsLogger();
        }
        return sMetricsLogger;
    }

    public static Intent sanitizeIntent(Intent intent) {
        Uri uriFromParts;
        Intent intent2 = new Intent(intent.getAction());
        Set<String> categories = intent.getCategories();
        if (categories != null) {
            Iterator<String> it = categories.iterator();
            while (it.hasNext()) {
                intent2.addCategory(it.next());
            }
        }
        if (intent.getData() == null) {
            uriFromParts = null;
        } else {
            uriFromParts = Uri.fromParts(intent.getScheme(), BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        }
        intent2.setDataAndType(uriFromParts, intent.getType());
        intent2.addFlags(intent.getFlags());
        intent2.setPackage(intent.getPackage());
        return intent2;
    }

    public static AuxiliaryResolveInfo doInstantAppResolutionPhaseOne(InstantAppResolverConnection instantAppResolverConnection, InstantAppRequest instantAppRequest) {
        int i;
        AuxiliaryResolveInfo auxiliaryResolveInfoFilterInstantAppIntent;
        long jCurrentTimeMillis = System.currentTimeMillis();
        String string = UUID.randomUUID().toString();
        if (DEBUG_INSTANT) {
            Log.d(TAG, "[" + string + "] Phase1; resolving");
        }
        Intent intent = instantAppRequest.origIntent;
        int i2 = 1;
        try {
            List<InstantAppResolveInfo> instantAppResolveInfoList = instantAppResolverConnection.getInstantAppResolveInfoList(sanitizeIntent(intent), instantAppRequest.digest.getDigestPrefixSecure(), string);
            if (instantAppResolveInfoList == null || instantAppResolveInfoList.size() <= 0) {
                i = 2;
                auxiliaryResolveInfoFilterInstantAppIntent = null;
            } else {
                i = 2;
                try {
                    auxiliaryResolveInfoFilterInstantAppIntent = filterInstantAppIntent(instantAppResolveInfoList, intent, instantAppRequest.resolvedType, instantAppRequest.userId, intent.getPackage(), instantAppRequest.digest, string);
                } catch (InstantAppResolverConnection.ConnectionException e) {
                    e = e;
                    if (e.failure == 1) {
                        i2 = i;
                    } else if (e.failure == i) {
                        i2 = 3;
                    }
                    auxiliaryResolveInfoFilterInstantAppIntent = null;
                }
            }
            i2 = 0;
        } catch (InstantAppResolverConnection.ConnectionException e2) {
            e = e2;
            i = 2;
        }
        if (instantAppRequest.resolveForStart && i2 == 0) {
            logMetrics(899, jCurrentTimeMillis, string, i2);
        }
        if (DEBUG_INSTANT && auxiliaryResolveInfoFilterInstantAppIntent == null) {
            if (i2 == i) {
                Log.d(TAG, "[" + string + "] Phase1; bind timed out");
            } else if (i2 == 3) {
                Log.d(TAG, "[" + string + "] Phase1; call timed out");
            } else if (i2 != 0) {
                Log.d(TAG, "[" + string + "] Phase1; service connection error");
            } else {
                Log.d(TAG, "[" + string + "] Phase1; No results matched");
            }
        }
        return (auxiliaryResolveInfoFilterInstantAppIntent != null || (intent.getFlags() & 2048) == 0) ? auxiliaryResolveInfoFilterInstantAppIntent : new AuxiliaryResolveInfo(string, false, createFailureIntent(intent, string), (List) null);
    }

    public static void doInstantAppResolutionPhaseTwo(final Context context, InstantAppResolverConnection instantAppResolverConnection, final InstantAppRequest instantAppRequest, final ActivityInfo activityInfo, Handler handler) {
        long jCurrentTimeMillis = System.currentTimeMillis();
        final String str = instantAppRequest.responseObj.token;
        if (DEBUG_INSTANT) {
            Log.d(TAG, "[" + str + "] Phase2; resolving");
        }
        final Intent intent = instantAppRequest.origIntent;
        final Intent intentSanitizeIntent = sanitizeIntent(intent);
        try {
            instantAppResolverConnection.getInstantAppIntentFilterList(intentSanitizeIntent, instantAppRequest.digest.getDigestPrefixSecure(), str, new InstantAppResolverConnection.PhaseTwoCallback() {
                @Override
                void onPhaseTwoResolved(List<InstantAppResolveInfo> list, long j) {
                    AuxiliaryResolveInfo auxiliaryResolveInfoFilterInstantAppIntent;
                    Intent intent2 = null;
                    if (list != null && list.size() > 0 && (auxiliaryResolveInfoFilterInstantAppIntent = InstantAppResolver.filterInstantAppIntent(list, intent, null, 0, intent.getPackage(), instantAppRequest.digest, str)) != null) {
                        intent2 = auxiliaryResolveInfoFilterInstantAppIntent.failureIntent;
                    }
                    Intent intentBuildEphemeralInstallerIntent = InstantAppResolver.buildEphemeralInstallerIntent(instantAppRequest.origIntent, intentSanitizeIntent, intent2, instantAppRequest.callingPackage, instantAppRequest.verificationBundle, instantAppRequest.resolvedType, instantAppRequest.userId, instantAppRequest.responseObj.installFailureActivity, str, false, instantAppRequest.responseObj.filters);
                    intentBuildEphemeralInstallerIntent.setComponent(new ComponentName(activityInfo.packageName, activityInfo.name));
                    InstantAppResolver.logMetrics(900, j, str, instantAppRequest.responseObj.filters != null ? 0 : 1);
                    context.startActivity(intentBuildEphemeralInstallerIntent);
                }
            }, handler, jCurrentTimeMillis);
        } catch (InstantAppResolverConnection.ConnectionException e) {
            int i = e.failure == 1 ? 2 : 1;
            logMetrics(900, jCurrentTimeMillis, str, i);
            if (DEBUG_INSTANT) {
                if (i == 2) {
                    Log.d(TAG, "[" + str + "] Phase2; bind timed out");
                    return;
                }
                Log.d(TAG, "[" + str + "] Phase2; service connection error");
            }
        }
    }

    public static Intent buildEphemeralInstallerIntent(Intent intent, Intent intent2, Intent intent3, String str, Bundle bundle, String str2, int i, ComponentName componentName, String str3, boolean z, List<AuxiliaryResolveInfo.AuxiliaryFilter> list) {
        Intent intent4;
        int flags = intent.getFlags();
        Intent intent5 = new Intent();
        intent5.setFlags(flags | 1073741824 | DumpState.DUMP_VOLUMES);
        if (str3 != null) {
            intent5.putExtra("android.intent.extra.EPHEMERAL_TOKEN", str3);
            intent5.putExtra("android.intent.extra.INSTANT_APP_TOKEN", str3);
        }
        if (intent.getData() != null) {
            intent5.putExtra("android.intent.extra.EPHEMERAL_HOSTNAME", intent.getData().getHost());
            intent5.putExtra("android.intent.extra.INSTANT_APP_HOSTNAME", intent.getData().getHost());
        }
        intent5.putExtra("android.intent.extra.INSTANT_APP_ACTION", intent.getAction());
        intent5.putExtra("android.intent.extra.INTENT", intent2);
        if (z) {
            intent5.setAction("android.intent.action.RESOLVE_INSTANT_APP_PACKAGE");
        } else {
            if (intent3 != null || componentName != null) {
                if (componentName != null) {
                    try {
                        intent4 = new Intent();
                        intent4.setComponent(componentName);
                        if (list != null && list.size() == 1) {
                            intent4.putExtra("android.intent.extra.SPLIT_NAME", list.get(0).splitName);
                        }
                        intent4.putExtra("android.intent.extra.INTENT", intent);
                    } catch (RemoteException e) {
                    }
                } else {
                    intent4 = intent3;
                }
                IntentSender intentSender = new IntentSender(ActivityManager.getService().getIntentSender(2, str, (IBinder) null, (String) null, 1, new Intent[]{intent4}, new String[]{str2}, 1409286144, (Bundle) null, i));
                intent5.putExtra("android.intent.extra.EPHEMERAL_FAILURE", intentSender);
                intent5.putExtra("android.intent.extra.INSTANT_APP_FAILURE", intentSender);
            }
            Intent intent6 = new Intent(intent);
            intent6.setLaunchToken(str3);
            try {
                IntentSender intentSender2 = new IntentSender(ActivityManager.getService().getIntentSender(2, str, (IBinder) null, (String) null, 0, new Intent[]{intent6}, new String[]{str2}, 1409286144, (Bundle) null, i));
                intent5.putExtra("android.intent.extra.EPHEMERAL_SUCCESS", intentSender2);
                intent5.putExtra("android.intent.extra.INSTANT_APP_SUCCESS", intentSender2);
            } catch (RemoteException e2) {
            }
            if (bundle != null) {
                intent5.putExtra("android.intent.extra.VERIFICATION_BUNDLE", bundle);
            }
            intent5.putExtra("android.intent.extra.CALLING_PACKAGE", str);
            if (list != null) {
                Bundle[] bundleArr = new Bundle[list.size()];
                int size = list.size();
                for (int i2 = 0; i2 < size; i2++) {
                    Bundle bundle2 = new Bundle();
                    AuxiliaryResolveInfo.AuxiliaryFilter auxiliaryFilter = list.get(i2);
                    bundle2.putBoolean("android.intent.extra.UNKNOWN_INSTANT_APP", auxiliaryFilter.resolveInfo != null && auxiliaryFilter.resolveInfo.shouldLetInstallerDecide());
                    bundle2.putString("android.intent.extra.PACKAGE_NAME", auxiliaryFilter.packageName);
                    bundle2.putString("android.intent.extra.SPLIT_NAME", auxiliaryFilter.splitName);
                    bundle2.putLong("android.intent.extra.LONG_VERSION_CODE", auxiliaryFilter.versionCode);
                    bundle2.putBundle("android.intent.extra.INSTANT_APP_EXTRAS", auxiliaryFilter.extras);
                    bundleArr[i2] = bundle2;
                    if (i2 == 0) {
                        intent5.putExtras(bundle2);
                        intent5.putExtra("android.intent.extra.VERSION_CODE", (int) auxiliaryFilter.versionCode);
                    }
                }
                intent5.putExtra("android.intent.extra.INSTANT_APP_BUNDLES", bundleArr);
            }
            intent5.setAction("android.intent.action.INSTALL_INSTANT_APP_PACKAGE");
        }
        return intent5;
    }

    private static AuxiliaryResolveInfo filterInstantAppIntent(List<InstantAppResolveInfo> list, Intent intent, String str, int i, String str2, InstantAppResolveInfo.InstantAppDigest instantAppDigest, String str3) {
        boolean z;
        int[] digestPrefix = instantAppDigest.getDigestPrefix();
        byte[][] digestBytes = instantAppDigest.getDigestBytes();
        boolean z2 = intent.isWebIntent() || (digestPrefix.length > 0 && (intent.getFlags() & 2048) == 0);
        boolean z3 = false;
        ArrayList arrayList = null;
        for (InstantAppResolveInfo instantAppResolveInfo : list) {
            if (z2 && instantAppResolveInfo.shouldLetInstallerDecide()) {
                Slog.d(TAG, "InstantAppResolveInfo with mShouldLetInstallerDecide=true when digest required; ignoring");
            } else {
                byte[] digestBytes2 = instantAppResolveInfo.getDigestBytes();
                if (digestPrefix.length > 0 && (z2 || digestBytes2.length > 0)) {
                    int length = digestPrefix.length - 1;
                    while (true) {
                        if (length >= 0) {
                            if (Arrays.equals(digestBytes[length], digestBytes2)) {
                                z = true;
                                break;
                            }
                            length--;
                        } else {
                            z = false;
                            break;
                        }
                    }
                    if (!z) {
                    }
                }
                List<AuxiliaryResolveInfo.AuxiliaryFilter> listComputeResolveFilters = computeResolveFilters(intent, str, i, str2, str3, instantAppResolveInfo);
                if (listComputeResolveFilters != null) {
                    if (listComputeResolveFilters.isEmpty()) {
                        z3 = true;
                    }
                    if (arrayList == null) {
                        arrayList = new ArrayList(listComputeResolveFilters);
                    } else {
                        arrayList.addAll(listComputeResolveFilters);
                    }
                }
            }
        }
        if (arrayList == null || arrayList.isEmpty()) {
            return null;
        }
        return new AuxiliaryResolveInfo(str3, z3, createFailureIntent(intent, str3), arrayList);
    }

    private static Intent createFailureIntent(Intent intent, String str) {
        Intent intent2 = new Intent(intent);
        intent2.setFlags(intent2.getFlags() | 512);
        intent2.setFlags(intent2.getFlags() & (-2049));
        intent2.setLaunchToken(str);
        return intent2;
    }

    private static List<AuxiliaryResolveInfo.AuxiliaryFilter> computeResolveFilters(Intent intent, String str, int i, String str2, String str3, InstantAppResolveInfo instantAppResolveInfo) {
        if (instantAppResolveInfo.shouldLetInstallerDecide()) {
            return Collections.singletonList(new AuxiliaryResolveInfo.AuxiliaryFilter(instantAppResolveInfo, (String) null, instantAppResolveInfo.getExtras()));
        }
        if (str2 != null && !str2.equals(instantAppResolveInfo.getPackageName())) {
            return null;
        }
        List intentFilters = instantAppResolveInfo.getIntentFilters();
        if (intentFilters == null || intentFilters.isEmpty()) {
            if (intent.isWebIntent()) {
                return null;
            }
            if (DEBUG_INSTANT) {
                Log.d(TAG, "No app filters; go to phase 2");
            }
            return Collections.emptyList();
        }
        PackageManagerService.InstantAppIntentResolver instantAppIntentResolver = new PackageManagerService.InstantAppIntentResolver();
        for (int size = intentFilters.size() - 1; size >= 0; size--) {
            InstantAppIntentFilter instantAppIntentFilter = (InstantAppIntentFilter) intentFilters.get(size);
            List filters = instantAppIntentFilter.getFilters();
            if (filters != null && !filters.isEmpty()) {
                for (int size2 = filters.size() - 1; size2 >= 0; size2--) {
                    IntentFilter intentFilter = (IntentFilter) filters.get(size2);
                    Iterator<IntentFilter.AuthorityEntry> itAuthoritiesIterator = intentFilter.authoritiesIterator();
                    if ((itAuthoritiesIterator != null && itAuthoritiesIterator.hasNext()) || ((!intentFilter.hasDataScheme("http") && !intentFilter.hasDataScheme("https")) || !intentFilter.hasAction("android.intent.action.VIEW") || !intentFilter.hasCategory("android.intent.category.BROWSABLE"))) {
                        instantAppIntentResolver.addFilter(new AuxiliaryResolveInfo.AuxiliaryFilter(intentFilter, instantAppResolveInfo, instantAppIntentFilter.getSplitName(), instantAppResolveInfo.getExtras()));
                    }
                }
            }
        }
        List<AuxiliaryResolveInfo.AuxiliaryFilter> listQueryIntent = instantAppIntentResolver.queryIntent(intent, str, false, i);
        if (!listQueryIntent.isEmpty()) {
            if (DEBUG_INSTANT) {
                Log.d(TAG, "[" + str3 + "] Found match(es); " + listQueryIntent);
            }
            return listQueryIntent;
        }
        if (DEBUG_INSTANT) {
            Log.d(TAG, "[" + str3 + "] No matches found package: " + instantAppResolveInfo.getPackageName() + ", versionCode: " + instantAppResolveInfo.getVersionCode());
        }
        return null;
    }

    private static void logMetrics(int i, long j, String str, int i2) {
        getLogger().write(new LogMaker(i).setType(4).addTaggedData(901, new Long(System.currentTimeMillis() - j)).addTaggedData(903, str).addTaggedData(902, new Integer(i2)));
    }
}
