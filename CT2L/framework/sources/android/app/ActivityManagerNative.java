package android.app;

import android.app.ActivityManager;
import android.app.ApplicationErrorReport;
import android.app.IActivityContainer;
import android.app.IActivityContainerCallback;
import android.app.IActivityController;
import android.app.IActivityManager;
import android.app.IInstrumentationWatcher;
import android.app.IProcessObserver;
import android.app.IServiceConnection;
import android.app.IStopUserCallback;
import android.app.ITaskStackListener;
import android.app.IUiAutomationConnection;
import android.app.IUserSwitchObserver;
import android.content.ComponentName;
import android.content.Context;
import android.content.IIntentReceiver;
import android.content.IIntentSender;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.UriPermission;
import android.content.pm.ApplicationInfo;
import android.content.pm.ConfigurationInfo;
import android.content.pm.IPackageDataObserver;
import android.content.pm.ParceledListSlice;
import android.content.pm.UserInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Debug;
import android.os.IBinder;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.StrictMode;
import android.service.voice.IVoiceInteractionSession;
import android.text.TextUtils;
import android.util.Singleton;
import com.android.internal.app.IVoiceInteractor;
import java.util.ArrayList;
import java.util.List;

public abstract class ActivityManagerNative extends Binder implements IActivityManager {
    static boolean sSystemReady = false;
    private static final Singleton<IActivityManager> gDefault = new Singleton<IActivityManager>() {
        @Override
        protected IActivityManager create() {
            IBinder b = ServiceManager.getService(Context.ACTIVITY_SERVICE);
            IActivityManager am = ActivityManagerNative.asInterface(b);
            return am;
        }
    };

    public static IActivityManager asInterface(IBinder obj) {
        if (obj == null) {
            return null;
        }
        IActivityManager in = (IActivityManager) obj.queryLocalInterface(IActivityManager.descriptor);
        return in == null ? new ActivityManagerProxy(obj) : in;
    }

    public static IActivityManager getDefault() {
        return gDefault.get();
    }

    public static boolean isSystemReady() {
        if (!sSystemReady) {
            sSystemReady = getDefault().testIsSystemReady();
        }
        return sSystemReady;
    }

    public static void broadcastStickyIntent(Intent intent, String permission, int userId) {
        try {
            getDefault().broadcastIntent(null, intent, null, null, -1, null, null, null, -1, false, true, userId);
        } catch (RemoteException e) {
        }
    }

    public static void noteWakeupAlarm(PendingIntent ps, int sourceUid, String sourcePkg) {
        try {
            getDefault().noteWakeupAlarm(ps.getTarget(), sourceUid, sourcePkg);
        } catch (RemoteException e) {
        }
    }

    public ActivityManagerNative() {
        attachInterface(this, IActivityManager.descriptor);
    }

    @Override
    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        Bundle bundle;
        Bundle bundle2;
        Intent[] requestIntents;
        String[] requestResolvedTypes;
        switch (code) {
            case 2:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder app = data.readStrongBinder();
                ApplicationErrorReport.CrashInfo ci = new ApplicationErrorReport.CrashInfo(data);
                handleApplicationCrash(app, ci);
                reply.writeNoException();
                return true;
            case 3:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder b = data.readStrongBinder();
                IApplicationThread app2 = ApplicationThreadNative.asInterface(b);
                String callingPackage = data.readString();
                Intent intent = Intent.CREATOR.createFromParcel(data);
                String resolvedType = data.readString();
                IBinder resultTo = data.readStrongBinder();
                String resultWho = data.readString();
                int requestCode = data.readInt();
                int startFlags = data.readInt();
                ProfilerInfo profilerInfo = data.readInt() != 0 ? ProfilerInfo.CREATOR.createFromParcel(data) : null;
                Bundle options = data.readInt() != 0 ? Bundle.CREATOR.createFromParcel(data) : null;
                int result = startActivity(app2, callingPackage, intent, resolvedType, resultTo, resultWho, requestCode, startFlags, profilerInfo, options);
                reply.writeNoException();
                reply.writeInt(result);
                return true;
            case 4:
                data.enforceInterface(IActivityManager.descriptor);
                unhandledBack();
                reply.writeNoException();
                return true;
            case 5:
                data.enforceInterface(IActivityManager.descriptor);
                Uri uri = Uri.parse(data.readString());
                ParcelFileDescriptor pfd = openContentUri(uri);
                reply.writeNoException();
                if (pfd != null) {
                    reply.writeInt(1);
                    pfd.writeToParcel(reply, 1);
                } else {
                    reply.writeInt(0);
                }
                return true;
            case 6:
            case 7:
            case 8:
            case 9:
            case 10:
            case 28:
            case 40:
            case 41:
            case 131:
            case 139:
            case 160:
            case 187:
            case 188:
            case 189:
            case 190:
            case 191:
            case 192:
            case 193:
            case 194:
            case 195:
            case 196:
            case 197:
            case 198:
            case 199:
            case 200:
            case 201:
            case 202:
            case 203:
            case 204:
            case 205:
            case 206:
            case 207:
            case 208:
            case 209:
            case 210:
            default:
                return super.onTransact(code, data, reply, flags);
            case 11:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder token = data.readStrongBinder();
                Intent resultData = null;
                int resultCode = data.readInt();
                if (data.readInt() != 0) {
                    Intent resultData2 = Intent.CREATOR.createFromParcel(data);
                    resultData = resultData2;
                }
                boolean finishTask = data.readInt() != 0;
                boolean res = finishActivity(token, resultCode, resultData, finishTask);
                reply.writeNoException();
                reply.writeInt(res ? 1 : 0);
                return true;
            case 12:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder b2 = data.readStrongBinder();
                IApplicationThread app3 = b2 != null ? ApplicationThreadNative.asInterface(b2) : null;
                String packageName = data.readString();
                IBinder b3 = data.readStrongBinder();
                IIntentReceiver rec = b3 != null ? IIntentReceiver.Stub.asInterface(b3) : null;
                IntentFilter filter = IntentFilter.CREATOR.createFromParcel(data);
                String perm = data.readString();
                int userId = data.readInt();
                Intent intent2 = registerReceiver(app3, packageName, rec, filter, perm, userId);
                reply.writeNoException();
                if (intent2 != null) {
                    reply.writeInt(1);
                    intent2.writeToParcel(reply, 0);
                } else {
                    reply.writeInt(0);
                }
                return true;
            case 13:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder b4 = data.readStrongBinder();
                if (b4 == null) {
                    return true;
                }
                IIntentReceiver rec2 = IIntentReceiver.Stub.asInterface(b4);
                unregisterReceiver(rec2);
                reply.writeNoException();
                return true;
            case 14:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder b5 = data.readStrongBinder();
                IApplicationThread app4 = b5 != null ? ApplicationThreadNative.asInterface(b5) : null;
                Intent intent3 = Intent.CREATOR.createFromParcel(data);
                String resolvedType2 = data.readString();
                IBinder b6 = data.readStrongBinder();
                IIntentReceiver resultTo2 = b6 != null ? IIntentReceiver.Stub.asInterface(b6) : null;
                int resultCode2 = data.readInt();
                String resultData3 = data.readString();
                Bundle resultExtras = data.readBundle();
                String perm2 = data.readString();
                int appOp = data.readInt();
                boolean serialized = data.readInt() != 0;
                boolean sticky = data.readInt() != 0;
                int userId2 = data.readInt();
                int res2 = broadcastIntent(app4, intent3, resolvedType2, resultTo2, resultCode2, resultData3, resultExtras, perm2, appOp, serialized, sticky, userId2);
                reply.writeNoException();
                reply.writeInt(res2);
                return true;
            case 15:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder b7 = data.readStrongBinder();
                IApplicationThread app5 = b7 != null ? ApplicationThreadNative.asInterface(b7) : null;
                Intent intent4 = Intent.CREATOR.createFromParcel(data);
                int userId3 = data.readInt();
                unbroadcastIntent(app5, intent4, userId3);
                reply.writeNoException();
                return true;
            case 16:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder who = data.readStrongBinder();
                int resultCode3 = data.readInt();
                String resultData4 = data.readString();
                Bundle resultExtras2 = data.readBundle();
                boolean resultAbort = data.readInt() != 0;
                if (who != null) {
                    finishReceiver(who, resultCode3, resultData4, resultExtras2, resultAbort);
                }
                reply.writeNoException();
                return true;
            case 17:
                data.enforceInterface(IActivityManager.descriptor);
                IApplicationThread app6 = ApplicationThreadNative.asInterface(data.readStrongBinder());
                if (app6 != null) {
                    attachApplication(app6);
                }
                reply.writeNoException();
                return true;
            case 18:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder token2 = data.readStrongBinder();
                Configuration config = null;
                if (data.readInt() != 0) {
                    Configuration config2 = Configuration.CREATOR.createFromParcel(data);
                    config = config2;
                }
                boolean stopProfiling = data.readInt() != 0;
                if (token2 != null) {
                    activityIdle(token2, config, stopProfiling);
                }
                reply.writeNoException();
                return true;
            case 19:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder token3 = data.readStrongBinder();
                activityPaused(token3);
                reply.writeNoException();
                return true;
            case 20:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder token4 = data.readStrongBinder();
                Bundle map = data.readBundle();
                PersistableBundle persistentState = data.readPersistableBundle();
                CharSequence description = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(data);
                activityStopped(token4, map, persistentState, description);
                reply.writeNoException();
                return true;
            case 21:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder token5 = data.readStrongBinder();
                String res3 = token5 != null ? getCallingPackage(token5) : null;
                reply.writeNoException();
                reply.writeString(res3);
                return true;
            case 22:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder token6 = data.readStrongBinder();
                ComponentName cn = getCallingActivity(token6);
                reply.writeNoException();
                ComponentName.writeToParcel(cn, reply);
                return true;
            case 23:
                data.enforceInterface(IActivityManager.descriptor);
                int maxNum = data.readInt();
                int fl = data.readInt();
                List<ActivityManager.RunningTaskInfo> list = getTasks(maxNum, fl);
                reply.writeNoException();
                int N = list != null ? list.size() : -1;
                reply.writeInt(N);
                for (int i = 0; i < N; i++) {
                    list.get(i).writeToParcel(reply, 0);
                }
                return true;
            case 24:
                data.enforceInterface(IActivityManager.descriptor);
                int task = data.readInt();
                int fl2 = data.readInt();
                Bundle options2 = data.readInt() != 0 ? Bundle.CREATOR.createFromParcel(data) : null;
                moveTaskToFront(task, fl2, options2);
                reply.writeNoException();
                return true;
            case 25:
                data.enforceInterface(IActivityManager.descriptor);
                int task2 = data.readInt();
                moveTaskToBack(task2);
                reply.writeNoException();
                return true;
            case 26:
                data.enforceInterface(IActivityManager.descriptor);
                int task3 = data.readInt();
                moveTaskBackwards(task3);
                reply.writeNoException();
                return true;
            case 27:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder token7 = data.readStrongBinder();
                boolean onlyRoot = data.readInt() != 0;
                int res4 = token7 != null ? getTaskForActivity(token7, onlyRoot) : -1;
                reply.writeNoException();
                reply.writeInt(res4);
                return true;
            case 29:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder b8 = data.readStrongBinder();
                IApplicationThread app7 = ApplicationThreadNative.asInterface(b8);
                String name = data.readString();
                int userId4 = data.readInt();
                boolean stable = data.readInt() != 0;
                IActivityManager.ContentProviderHolder cph = getContentProvider(app7, name, userId4, stable);
                reply.writeNoException();
                if (cph != null) {
                    reply.writeInt(1);
                    cph.writeToParcel(reply, 0);
                } else {
                    reply.writeInt(0);
                }
                return true;
            case 30:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder b9 = data.readStrongBinder();
                IApplicationThread app8 = ApplicationThreadNative.asInterface(b9);
                ArrayList<IActivityManager.ContentProviderHolder> providers = data.createTypedArrayList(IActivityManager.ContentProviderHolder.CREATOR);
                publishContentProviders(app8, providers);
                reply.writeNoException();
                return true;
            case 31:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder b10 = data.readStrongBinder();
                int stable2 = data.readInt();
                int unstable = data.readInt();
                boolean res5 = refContentProvider(b10, stable2, unstable);
                reply.writeNoException();
                reply.writeInt(res5 ? 1 : 0);
                return true;
            case 32:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder token8 = data.readStrongBinder();
                String resultWho2 = data.readString();
                int requestCode2 = data.readInt();
                finishSubActivity(token8, resultWho2, requestCode2);
                reply.writeNoException();
                return true;
            case 33:
                data.enforceInterface(IActivityManager.descriptor);
                ComponentName comp = ComponentName.CREATOR.createFromParcel(data);
                PendingIntent pi = getRunningServiceControlPanel(comp);
                reply.writeNoException();
                PendingIntent.writePendingIntentOrNullToParcel(pi, reply);
                return true;
            case 34:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder b11 = data.readStrongBinder();
                IApplicationThread app9 = ApplicationThreadNative.asInterface(b11);
                Intent service = Intent.CREATOR.createFromParcel(data);
                String resolvedType3 = data.readString();
                int userId5 = data.readInt();
                ComponentName cn2 = startService(app9, service, resolvedType3, userId5);
                reply.writeNoException();
                ComponentName.writeToParcel(cn2, reply);
                return true;
            case 35:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder b12 = data.readStrongBinder();
                IApplicationThread app10 = ApplicationThreadNative.asInterface(b12);
                Intent service2 = Intent.CREATOR.createFromParcel(data);
                String resolvedType4 = data.readString();
                int userId6 = data.readInt();
                int res6 = stopService(app10, service2, resolvedType4, userId6);
                reply.writeNoException();
                reply.writeInt(res6);
                return true;
            case 36:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder b13 = data.readStrongBinder();
                IApplicationThread app11 = ApplicationThreadNative.asInterface(b13);
                IBinder token9 = data.readStrongBinder();
                Intent service3 = Intent.CREATOR.createFromParcel(data);
                String resolvedType5 = data.readString();
                IBinder b14 = data.readStrongBinder();
                int fl3 = data.readInt();
                int userId7 = data.readInt();
                IServiceConnection conn = IServiceConnection.Stub.asInterface(b14);
                int res7 = bindService(app11, token9, service3, resolvedType5, conn, fl3, userId7);
                reply.writeNoException();
                reply.writeInt(res7);
                return true;
            case 37:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder b15 = data.readStrongBinder();
                IServiceConnection conn2 = IServiceConnection.Stub.asInterface(b15);
                boolean res8 = unbindService(conn2);
                reply.writeNoException();
                reply.writeInt(res8 ? 1 : 0);
                return true;
            case 38:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder token10 = data.readStrongBinder();
                Intent intent5 = Intent.CREATOR.createFromParcel(data);
                IBinder service4 = data.readStrongBinder();
                publishService(token10, intent5, service4);
                reply.writeNoException();
                return true;
            case 39:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder token11 = data.readStrongBinder();
                activityResumed(token11);
                reply.writeNoException();
                return true;
            case 42:
                data.enforceInterface(IActivityManager.descriptor);
                String pn = data.readString();
                boolean wfd = data.readInt() != 0;
                boolean per = data.readInt() != 0;
                setDebugApp(pn, wfd, per);
                reply.writeNoException();
                return true;
            case 43:
                data.enforceInterface(IActivityManager.descriptor);
                boolean enabled = data.readInt() != 0;
                setAlwaysFinish(enabled);
                reply.writeNoException();
                return true;
            case 44:
                data.enforceInterface(IActivityManager.descriptor);
                ComponentName className = ComponentName.readFromParcel(data);
                String profileFile = data.readString();
                int fl4 = data.readInt();
                Bundle arguments = data.readBundle();
                IBinder b16 = data.readStrongBinder();
                IInstrumentationWatcher w = IInstrumentationWatcher.Stub.asInterface(b16);
                IBinder b17 = data.readStrongBinder();
                IUiAutomationConnection c = IUiAutomationConnection.Stub.asInterface(b17);
                int userId8 = data.readInt();
                String abiOverride = data.readString();
                boolean res9 = startInstrumentation(className, profileFile, fl4, arguments, w, c, userId8, abiOverride);
                reply.writeNoException();
                reply.writeInt(res9 ? 1 : 0);
                return true;
            case 45:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder b18 = data.readStrongBinder();
                IApplicationThread app12 = ApplicationThreadNative.asInterface(b18);
                int resultCode4 = data.readInt();
                Bundle results = data.readBundle();
                finishInstrumentation(app12, resultCode4, results);
                reply.writeNoException();
                return true;
            case 46:
                data.enforceInterface(IActivityManager.descriptor);
                Configuration config3 = getConfiguration();
                reply.writeNoException();
                config3.writeToParcel(reply, 0);
                return true;
            case 47:
                data.enforceInterface(IActivityManager.descriptor);
                Configuration config4 = Configuration.CREATOR.createFromParcel(data);
                updateConfiguration(config4);
                reply.writeNoException();
                return true;
            case 48:
                data.enforceInterface(IActivityManager.descriptor);
                ComponentName className2 = ComponentName.readFromParcel(data);
                IBinder token12 = data.readStrongBinder();
                int startId = data.readInt();
                boolean res10 = stopServiceToken(className2, token12, startId);
                reply.writeNoException();
                reply.writeInt(res10 ? 1 : 0);
                return true;
            case 49:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder token13 = data.readStrongBinder();
                ComponentName cn3 = getActivityClassForToken(token13);
                reply.writeNoException();
                ComponentName.writeToParcel(cn3, reply);
                return true;
            case 50:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder token14 = data.readStrongBinder();
                reply.writeNoException();
                reply.writeString(getPackageForToken(token14));
                return true;
            case 51:
                data.enforceInterface(IActivityManager.descriptor);
                int max = data.readInt();
                setProcessLimit(max);
                reply.writeNoException();
                return true;
            case 52:
                data.enforceInterface(IActivityManager.descriptor);
                int limit = getProcessLimit();
                reply.writeNoException();
                reply.writeInt(limit);
                return true;
            case 53:
                data.enforceInterface(IActivityManager.descriptor);
                String perm3 = data.readString();
                int pid = data.readInt();
                int uid = data.readInt();
                int res11 = checkPermission(perm3, pid, uid);
                reply.writeNoException();
                reply.writeInt(res11);
                return true;
            case 54:
                data.enforceInterface(IActivityManager.descriptor);
                Uri uri2 = Uri.CREATOR.createFromParcel(data);
                int pid2 = data.readInt();
                int uid2 = data.readInt();
                int mode = data.readInt();
                int userId9 = data.readInt();
                IBinder callerToken = data.readStrongBinder();
                int res12 = checkUriPermission(uri2, pid2, uid2, mode, userId9, callerToken);
                reply.writeNoException();
                reply.writeInt(res12);
                return true;
            case 55:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder b19 = data.readStrongBinder();
                IApplicationThread app13 = ApplicationThreadNative.asInterface(b19);
                String targetPkg = data.readString();
                Uri uri3 = Uri.CREATOR.createFromParcel(data);
                int mode2 = data.readInt();
                int userId10 = data.readInt();
                grantUriPermission(app13, targetPkg, uri3, mode2, userId10);
                reply.writeNoException();
                return true;
            case 56:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder b20 = data.readStrongBinder();
                IApplicationThread app14 = ApplicationThreadNative.asInterface(b20);
                Uri uri4 = Uri.CREATOR.createFromParcel(data);
                int mode3 = data.readInt();
                int userId11 = data.readInt();
                revokeUriPermission(app14, uri4, mode3, userId11);
                reply.writeNoException();
                return true;
            case 57:
                data.enforceInterface(IActivityManager.descriptor);
                IActivityController watcher = IActivityController.Stub.asInterface(data.readStrongBinder());
                setActivityController(watcher);
                reply.writeNoException();
                return true;
            case 58:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder b21 = data.readStrongBinder();
                IApplicationThread app15 = ApplicationThreadNative.asInterface(b21);
                boolean waiting = data.readInt() != 0;
                showWaitingForDebugger(app15, waiting);
                reply.writeNoException();
                return true;
            case 59:
                data.enforceInterface(IActivityManager.descriptor);
                int sig = data.readInt();
                signalPersistentProcesses(sig);
                reply.writeNoException();
                return true;
            case 60:
                data.enforceInterface(IActivityManager.descriptor);
                int maxNum2 = data.readInt();
                int fl5 = data.readInt();
                int userId12 = data.readInt();
                List<ActivityManager.RecentTaskInfo> list2 = getRecentTasks(maxNum2, fl5, userId12);
                reply.writeNoException();
                reply.writeTypedList(list2);
                return true;
            case 61:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder token15 = data.readStrongBinder();
                int type = data.readInt();
                int startId2 = data.readInt();
                int res13 = data.readInt();
                serviceDoneExecuting(token15, type, startId2, res13);
                reply.writeNoException();
                return true;
            case 62:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder token16 = data.readStrongBinder();
                activityDestroyed(token16);
                reply.writeNoException();
                return true;
            case 63:
                data.enforceInterface(IActivityManager.descriptor);
                int type2 = data.readInt();
                String packageName2 = data.readString();
                IBinder token17 = data.readStrongBinder();
                String resultWho3 = data.readString();
                int requestCode3 = data.readInt();
                if (data.readInt() != 0) {
                    requestIntents = (Intent[]) data.createTypedArray(Intent.CREATOR);
                    requestResolvedTypes = data.createStringArray();
                } else {
                    requestIntents = null;
                    requestResolvedTypes = null;
                }
                int fl6 = data.readInt();
                Bundle options3 = data.readInt() != 0 ? Bundle.CREATOR.createFromParcel(data) : null;
                int userId13 = data.readInt();
                IIntentSender res14 = getIntentSender(type2, packageName2, token17, resultWho3, requestCode3, requestIntents, requestResolvedTypes, fl6, options3, userId13);
                reply.writeNoException();
                reply.writeStrongBinder(res14 != null ? res14.asBinder() : null);
                return true;
            case 64:
                data.enforceInterface(IActivityManager.descriptor);
                IIntentSender r = IIntentSender.Stub.asInterface(data.readStrongBinder());
                cancelIntentSender(r);
                reply.writeNoException();
                return true;
            case 65:
                data.enforceInterface(IActivityManager.descriptor);
                IIntentSender r2 = IIntentSender.Stub.asInterface(data.readStrongBinder());
                String res15 = getPackageForIntentSender(r2);
                reply.writeNoException();
                reply.writeString(res15);
                return true;
            case 66:
                data.enforceInterface(IActivityManager.descriptor);
                enterSafeMode();
                reply.writeNoException();
                return true;
            case 67:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder callingActivity = data.readStrongBinder();
                Intent intent6 = Intent.CREATOR.createFromParcel(data);
                Bundle options4 = data.readInt() != 0 ? Bundle.CREATOR.createFromParcel(data) : null;
                boolean result2 = startNextMatchingActivity(callingActivity, intent6, options4);
                reply.writeNoException();
                reply.writeInt(result2 ? 1 : 0);
                return true;
            case 68:
                data.enforceInterface(IActivityManager.descriptor);
                IIntentSender is = IIntentSender.Stub.asInterface(data.readStrongBinder());
                int sourceUid = data.readInt();
                String sourcePkg = data.readString();
                noteWakeupAlarm(is, sourceUid, sourcePkg);
                reply.writeNoException();
                return true;
            case 69:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder b22 = data.readStrongBinder();
                boolean stable3 = data.readInt() != 0;
                removeContentProvider(b22, stable3);
                reply.writeNoException();
                return true;
            case 70:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder token18 = data.readStrongBinder();
                int requestedOrientation = data.readInt();
                setRequestedOrientation(token18, requestedOrientation);
                reply.writeNoException();
                return true;
            case 71:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder token19 = data.readStrongBinder();
                int req = getRequestedOrientation(token19);
                reply.writeNoException();
                reply.writeInt(req);
                return true;
            case 72:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder token20 = data.readStrongBinder();
                Intent intent7 = Intent.CREATOR.createFromParcel(data);
                boolean doRebind = data.readInt() != 0;
                unbindFinished(token20, intent7, doRebind);
                reply.writeNoException();
                return true;
            case 73:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder token21 = data.readStrongBinder();
                int pid3 = data.readInt();
                boolean isForeground = data.readInt() != 0;
                setProcessForeground(token21, pid3, isForeground);
                reply.writeNoException();
                return true;
            case 74:
                data.enforceInterface(IActivityManager.descriptor);
                ComponentName className3 = ComponentName.readFromParcel(data);
                IBinder token22 = data.readStrongBinder();
                int id = data.readInt();
                Notification notification = null;
                if (data.readInt() != 0) {
                    Notification notification2 = Notification.CREATOR.createFromParcel(data);
                    notification = notification2;
                }
                boolean removeNotification = data.readInt() != 0;
                setServiceForeground(className3, token22, id, notification, removeNotification);
                reply.writeNoException();
                return true;
            case 75:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder token23 = data.readStrongBinder();
                boolean nonRoot = data.readInt() != 0;
                boolean res16 = moveActivityTaskToBack(token23, nonRoot);
                reply.writeNoException();
                reply.writeInt(res16 ? 1 : 0);
                return true;
            case 76:
                data.enforceInterface(IActivityManager.descriptor);
                ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
                getMemoryInfo(mi);
                reply.writeNoException();
                mi.writeToParcel(reply, 0);
                return true;
            case 77:
                data.enforceInterface(IActivityManager.descriptor);
                List<ActivityManager.ProcessErrorStateInfo> list3 = getProcessesInErrorState();
                reply.writeNoException();
                reply.writeTypedList(list3);
                return true;
            case 78:
                data.enforceInterface(IActivityManager.descriptor);
                String packageName3 = data.readString();
                IPackageDataObserver observer = IPackageDataObserver.Stub.asInterface(data.readStrongBinder());
                int userId14 = data.readInt();
                boolean res17 = clearApplicationUserData(packageName3, observer, userId14);
                reply.writeNoException();
                reply.writeInt(res17 ? 1 : 0);
                return true;
            case 79:
                data.enforceInterface(IActivityManager.descriptor);
                String packageName4 = data.readString();
                int userId15 = data.readInt();
                forceStopPackage(packageName4, userId15);
                reply.writeNoException();
                return true;
            case 80:
                data.enforceInterface(IActivityManager.descriptor);
                int[] pids = data.createIntArray();
                String reason = data.readString();
                boolean secure = data.readInt() != 0;
                boolean res18 = killPids(pids, reason, secure);
                reply.writeNoException();
                reply.writeInt(res18 ? 1 : 0);
                return true;
            case 81:
                data.enforceInterface(IActivityManager.descriptor);
                int maxNum3 = data.readInt();
                int fl7 = data.readInt();
                List<ActivityManager.RunningServiceInfo> list4 = getServices(maxNum3, fl7);
                reply.writeNoException();
                int N2 = list4 != null ? list4.size() : -1;
                reply.writeInt(N2);
                for (int i2 = 0; i2 < N2; i2++) {
                    list4.get(i2).writeToParcel(reply, 0);
                }
                return true;
            case 82:
                data.enforceInterface(IActivityManager.descriptor);
                int id2 = data.readInt();
                ActivityManager.TaskThumbnail taskThumbnail = getTaskThumbnail(id2);
                reply.writeNoException();
                if (taskThumbnail != null) {
                    reply.writeInt(1);
                    taskThumbnail.writeToParcel(reply, 1);
                } else {
                    reply.writeInt(0);
                }
                return true;
            case 83:
                data.enforceInterface(IActivityManager.descriptor);
                List<ActivityManager.RunningAppProcessInfo> list5 = getRunningAppProcesses();
                reply.writeNoException();
                reply.writeTypedList(list5);
                return true;
            case 84:
                data.enforceInterface(IActivityManager.descriptor);
                ConfigurationInfo config5 = getDeviceConfigurationInfo();
                reply.writeNoException();
                config5.writeToParcel(reply, 0);
                return true;
            case 85:
                data.enforceInterface(IActivityManager.descriptor);
                Intent service5 = Intent.CREATOR.createFromParcel(data);
                String resolvedType6 = data.readString();
                IBinder binder = peekService(service5, resolvedType6);
                reply.writeNoException();
                reply.writeStrongBinder(binder);
                return true;
            case 86:
                data.enforceInterface(IActivityManager.descriptor);
                String process = data.readString();
                int userId16 = data.readInt();
                boolean start = data.readInt() != 0;
                int profileType = data.readInt();
                ProfilerInfo profilerInfo2 = data.readInt() != 0 ? ProfilerInfo.CREATOR.createFromParcel(data) : null;
                boolean res19 = profileControl(process, userId16, start, profilerInfo2, profileType);
                reply.writeNoException();
                reply.writeInt(res19 ? 1 : 0);
                return true;
            case 87:
                data.enforceInterface(IActivityManager.descriptor);
                boolean res20 = shutdown(data.readInt());
                reply.writeNoException();
                reply.writeInt(res20 ? 1 : 0);
                return true;
            case 88:
                data.enforceInterface(IActivityManager.descriptor);
                stopAppSwitches();
                reply.writeNoException();
                return true;
            case 89:
                data.enforceInterface(IActivityManager.descriptor);
                resumeAppSwitches();
                reply.writeNoException();
                return true;
            case 90:
                data.enforceInterface(IActivityManager.descriptor);
                String packageName5 = data.readString();
                int backupRestoreMode = data.readInt();
                int userId17 = data.readInt();
                boolean success = bindBackupAgent(packageName5, backupRestoreMode, userId17);
                reply.writeNoException();
                reply.writeInt(success ? 1 : 0);
                return true;
            case 91:
                data.enforceInterface(IActivityManager.descriptor);
                String packageName6 = data.readString();
                IBinder agent = data.readStrongBinder();
                backupAgentCreated(packageName6, agent);
                reply.writeNoException();
                return true;
            case 92:
                data.enforceInterface(IActivityManager.descriptor);
                unbindBackupAgent(ApplicationInfo.CREATOR.createFromParcel(data));
                reply.writeNoException();
                return true;
            case 93:
                data.enforceInterface(IActivityManager.descriptor);
                IIntentSender r3 = IIntentSender.Stub.asInterface(data.readStrongBinder());
                int res21 = getUidForIntentSender(r3);
                reply.writeNoException();
                reply.writeInt(res21);
                return true;
            case 94:
                data.enforceInterface(IActivityManager.descriptor);
                int callingPid = data.readInt();
                int callingUid = data.readInt();
                int userId18 = data.readInt();
                boolean allowAll = data.readInt() != 0;
                boolean requireFull = data.readInt() != 0;
                String name2 = data.readString();
                String callerPackage = data.readString();
                int res22 = handleIncomingUser(callingPid, callingUid, userId18, allowAll, requireFull, name2, callerPackage);
                reply.writeNoException();
                reply.writeInt(res22);
                return true;
            case 95:
                data.enforceInterface(IActivityManager.descriptor);
                String packageName7 = data.readString();
                addPackageDependency(packageName7);
                reply.writeNoException();
                return true;
            case 96:
                data.enforceInterface(IActivityManager.descriptor);
                String pkg = data.readString();
                int appid = data.readInt();
                String reason2 = data.readString();
                killApplicationWithAppId(pkg, appid, reason2);
                reply.writeNoException();
                return true;
            case 97:
                data.enforceInterface(IActivityManager.descriptor);
                String reason3 = data.readString();
                closeSystemDialogs(reason3);
                reply.writeNoException();
                return true;
            case 98:
                data.enforceInterface(IActivityManager.descriptor);
                int[] pids2 = data.createIntArray();
                Debug.MemoryInfo[] res23 = getProcessMemoryInfo(pids2);
                reply.writeNoException();
                reply.writeTypedArray(res23, 1);
                return true;
            case 99:
                data.enforceInterface(IActivityManager.descriptor);
                String processName = data.readString();
                int uid3 = data.readInt();
                killApplicationProcess(processName, uid3);
                reply.writeNoException();
                return true;
            case 100:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder b23 = data.readStrongBinder();
                IApplicationThread app16 = ApplicationThreadNative.asInterface(b23);
                IntentSender intent8 = IntentSender.CREATOR.createFromParcel(data);
                Intent fillInIntent = null;
                if (data.readInt() != 0) {
                    Intent fillInIntent2 = Intent.CREATOR.createFromParcel(data);
                    fillInIntent = fillInIntent2;
                }
                String resolvedType7 = data.readString();
                IBinder resultTo3 = data.readStrongBinder();
                String resultWho4 = data.readString();
                int requestCode4 = data.readInt();
                int flagsMask = data.readInt();
                int flagsValues = data.readInt();
                Bundle options5 = data.readInt() != 0 ? Bundle.CREATOR.createFromParcel(data) : null;
                int result3 = startActivityIntentSender(app16, intent8, fillInIntent, resolvedType7, resultTo3, resultWho4, requestCode4, flagsMask, flagsValues, options5);
                reply.writeNoException();
                reply.writeInt(result3);
                return true;
            case 101:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder token24 = data.readStrongBinder();
                String packageName8 = data.readString();
                int enterAnim = data.readInt();
                int exitAnim = data.readInt();
                overridePendingTransition(token24, packageName8, enterAnim, exitAnim);
                reply.writeNoException();
                return true;
            case 102:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder app17 = data.readStrongBinder();
                String tag = data.readString();
                boolean system = data.readInt() != 0;
                ApplicationErrorReport.CrashInfo ci2 = new ApplicationErrorReport.CrashInfo(data);
                boolean res24 = handleApplicationWtf(app17, tag, system, ci2);
                reply.writeNoException();
                reply.writeInt(res24 ? 1 : 0);
                return true;
            case 103:
                data.enforceInterface(IActivityManager.descriptor);
                String packageName9 = data.readString();
                int userId19 = data.readInt();
                killBackgroundProcesses(packageName9, userId19);
                reply.writeNoException();
                return true;
            case 104:
                data.enforceInterface(IActivityManager.descriptor);
                boolean areThey = isUserAMonkey();
                reply.writeNoException();
                reply.writeInt(areThey ? 1 : 0);
                return true;
            case 105:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder b24 = data.readStrongBinder();
                IApplicationThread app18 = ApplicationThreadNative.asInterface(b24);
                String callingPackage2 = data.readString();
                Intent intent9 = Intent.CREATOR.createFromParcel(data);
                String resolvedType8 = data.readString();
                IBinder resultTo4 = data.readStrongBinder();
                String resultWho5 = data.readString();
                int requestCode5 = data.readInt();
                int startFlags2 = data.readInt();
                ProfilerInfo profilerInfo3 = data.readInt() != 0 ? ProfilerInfo.CREATOR.createFromParcel(data) : null;
                Bundle options6 = data.readInt() != 0 ? Bundle.CREATOR.createFromParcel(data) : null;
                int userId20 = data.readInt();
                IActivityManager.WaitResult result4 = startActivityAndWait(app18, callingPackage2, intent9, resolvedType8, resultTo4, resultWho5, requestCode5, startFlags2, profilerInfo3, options6, userId20);
                reply.writeNoException();
                result4.writeToParcel(reply, 0);
                return true;
            case 106:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder token25 = data.readStrongBinder();
                boolean res25 = willActivityBeVisible(token25);
                reply.writeNoException();
                reply.writeInt(res25 ? 1 : 0);
                return true;
            case 107:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder b25 = data.readStrongBinder();
                IApplicationThread app19 = ApplicationThreadNative.asInterface(b25);
                String callingPackage3 = data.readString();
                Intent intent10 = Intent.CREATOR.createFromParcel(data);
                String resolvedType9 = data.readString();
                IBinder resultTo5 = data.readStrongBinder();
                String resultWho6 = data.readString();
                int requestCode6 = data.readInt();
                int startFlags3 = data.readInt();
                Configuration config6 = Configuration.CREATOR.createFromParcel(data);
                Bundle options7 = data.readInt() != 0 ? Bundle.CREATOR.createFromParcel(data) : null;
                int userId21 = data.readInt();
                int result5 = startActivityWithConfig(app19, callingPackage3, intent10, resolvedType9, resultTo5, resultWho6, requestCode6, startFlags3, config6, options7, userId21);
                reply.writeNoException();
                reply.writeInt(result5);
                return true;
            case 108:
                data.enforceInterface(IActivityManager.descriptor);
                List<ApplicationInfo> list6 = getRunningExternalApplications();
                reply.writeNoException();
                reply.writeTypedList(list6);
                return true;
            case 109:
                data.enforceInterface(IActivityManager.descriptor);
                finishHeavyWeightApp();
                reply.writeNoException();
                return true;
            case 110:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder app20 = data.readStrongBinder();
                int violationMask = data.readInt();
                handleApplicationStrictModeViolation(app20, violationMask, new StrictMode.ViolationInfo(data));
                reply.writeNoException();
                return true;
            case 111:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder token26 = data.readStrongBinder();
                boolean isit = isImmersive(token26);
                reply.writeNoException();
                reply.writeInt(isit ? 1 : 0);
                return true;
            case 112:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder token27 = data.readStrongBinder();
                boolean imm = data.readInt() == 1;
                setImmersive(token27, imm);
                reply.writeNoException();
                return true;
            case 113:
                data.enforceInterface(IActivityManager.descriptor);
                boolean isit2 = isTopActivityImmersive();
                reply.writeNoException();
                reply.writeInt(isit2 ? 1 : 0);
                return true;
            case 114:
                data.enforceInterface(IActivityManager.descriptor);
                int uid4 = data.readInt();
                int initialPid = data.readInt();
                String packageName10 = data.readString();
                String message = data.readString();
                crashApplication(uid4, initialPid, packageName10, message);
                reply.writeNoException();
                return true;
            case 115:
                data.enforceInterface(IActivityManager.descriptor);
                Uri uri5 = Uri.CREATOR.createFromParcel(data);
                int userId22 = data.readInt();
                String type3 = getProviderMimeType(uri5, userId22);
                reply.writeNoException();
                reply.writeString(type3);
                return true;
            case 116:
                data.enforceInterface(IActivityManager.descriptor);
                String name3 = data.readString();
                IBinder perm4 = newUriPermissionOwner(name3);
                reply.writeNoException();
                reply.writeStrongBinder(perm4);
                return true;
            case 117:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder owner = data.readStrongBinder();
                int fromUid = data.readInt();
                String targetPkg2 = data.readString();
                Uri uri6 = Uri.CREATOR.createFromParcel(data);
                int mode4 = data.readInt();
                int sourceUserId = data.readInt();
                int targetUserId = data.readInt();
                grantUriPermissionFromOwner(owner, fromUid, targetPkg2, uri6, mode4, sourceUserId, targetUserId);
                reply.writeNoException();
                return true;
            case 118:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder owner2 = data.readStrongBinder();
                Uri uri7 = null;
                if (data.readInt() != 0) {
                    Uri uri8 = Uri.CREATOR.createFromParcel(data);
                    uri7 = uri8;
                }
                int mode5 = data.readInt();
                int userId23 = data.readInt();
                revokeUriPermissionFromOwner(owner2, uri7, mode5, userId23);
                reply.writeNoException();
                return true;
            case 119:
                data.enforceInterface(IActivityManager.descriptor);
                int callingUid2 = data.readInt();
                String targetPkg3 = data.readString();
                Uri uri9 = Uri.CREATOR.createFromParcel(data);
                int modeFlags = data.readInt();
                int userId24 = data.readInt();
                int res26 = checkGrantUriPermission(callingUid2, targetPkg3, uri9, modeFlags, userId24);
                reply.writeNoException();
                reply.writeInt(res26);
                return true;
            case 120:
                data.enforceInterface(IActivityManager.descriptor);
                String process2 = data.readString();
                int userId25 = data.readInt();
                boolean managed = data.readInt() != 0;
                String path = data.readString();
                ParcelFileDescriptor fd = data.readInt() != 0 ? ParcelFileDescriptor.CREATOR.createFromParcel(data) : null;
                boolean res27 = dumpHeap(process2, userId25, managed, path, fd);
                reply.writeNoException();
                reply.writeInt(res27 ? 1 : 0);
                return true;
            case 121:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder b26 = data.readStrongBinder();
                IApplicationThread app21 = ApplicationThreadNative.asInterface(b26);
                String callingPackage4 = data.readString();
                Intent[] intents = (Intent[]) data.createTypedArray(Intent.CREATOR);
                String[] resolvedTypes = data.createStringArray();
                IBinder resultTo6 = data.readStrongBinder();
                Bundle options8 = data.readInt() != 0 ? Bundle.CREATOR.createFromParcel(data) : null;
                int userId26 = data.readInt();
                int result6 = startActivities(app21, callingPackage4, intents, resolvedTypes, resultTo6, options8, userId26);
                reply.writeNoException();
                reply.writeInt(result6);
                return true;
            case 122:
                data.enforceInterface(IActivityManager.descriptor);
                int userid = data.readInt();
                boolean orStopping = data.readInt() != 0;
                boolean result7 = isUserRunning(userid, orStopping);
                reply.writeNoException();
                reply.writeInt(result7 ? 1 : 0);
                return true;
            case 123:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder token28 = data.readStrongBinder();
                activitySlept(token28);
                reply.writeNoException();
                return true;
            case 124:
                data.enforceInterface(IActivityManager.descriptor);
                int mode6 = getFrontActivityScreenCompatMode();
                reply.writeNoException();
                reply.writeInt(mode6);
                return true;
            case 125:
                data.enforceInterface(IActivityManager.descriptor);
                int mode7 = data.readInt();
                setFrontActivityScreenCompatMode(mode7);
                reply.writeNoException();
                reply.writeInt(mode7);
                return true;
            case 126:
                data.enforceInterface(IActivityManager.descriptor);
                String pkg2 = data.readString();
                int mode8 = getPackageScreenCompatMode(pkg2);
                reply.writeNoException();
                reply.writeInt(mode8);
                return true;
            case 127:
                data.enforceInterface(IActivityManager.descriptor);
                String pkg3 = data.readString();
                int mode9 = data.readInt();
                setPackageScreenCompatMode(pkg3, mode9);
                reply.writeNoException();
                return true;
            case 128:
                data.enforceInterface(IActivityManager.descriptor);
                String pkg4 = data.readString();
                boolean ask = getPackageAskScreenCompat(pkg4);
                reply.writeNoException();
                reply.writeInt(ask ? 1 : 0);
                return true;
            case 129:
                data.enforceInterface(IActivityManager.descriptor);
                String pkg5 = data.readString();
                boolean ask2 = data.readInt() != 0;
                setPackageAskScreenCompat(pkg5, ask2);
                reply.writeNoException();
                return true;
            case 130:
                data.enforceInterface(IActivityManager.descriptor);
                int userid2 = data.readInt();
                boolean result8 = switchUser(userid2);
                reply.writeNoException();
                reply.writeInt(result8 ? 1 : 0);
                return true;
            case 132:
                data.enforceInterface(IActivityManager.descriptor);
                int taskId = data.readInt();
                boolean result9 = removeTask(taskId);
                reply.writeNoException();
                reply.writeInt(result9 ? 1 : 0);
                return true;
            case 133:
                data.enforceInterface(IActivityManager.descriptor);
                IProcessObserver observer2 = IProcessObserver.Stub.asInterface(data.readStrongBinder());
                registerProcessObserver(observer2);
                return true;
            case 134:
                data.enforceInterface(IActivityManager.descriptor);
                IProcessObserver observer3 = IProcessObserver.Stub.asInterface(data.readStrongBinder());
                unregisterProcessObserver(observer3);
                return true;
            case 135:
                data.enforceInterface(IActivityManager.descriptor);
                IIntentSender r4 = IIntentSender.Stub.asInterface(data.readStrongBinder());
                boolean res28 = isIntentSenderTargetedToPackage(r4);
                reply.writeNoException();
                reply.writeInt(res28 ? 1 : 0);
                return true;
            case 136:
                data.enforceInterface(IActivityManager.descriptor);
                Configuration config7 = Configuration.CREATOR.createFromParcel(data);
                updatePersistentConfiguration(config7);
                reply.writeNoException();
                return true;
            case 137:
                data.enforceInterface(IActivityManager.descriptor);
                int[] pids3 = data.createIntArray();
                long[] pss = getProcessPss(pids3);
                reply.writeNoException();
                reply.writeLongArray(pss);
                return true;
            case 138:
                data.enforceInterface(IActivityManager.descriptor);
                CharSequence msg = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(data);
                boolean always = data.readInt() != 0;
                showBootMessage(msg, always);
                reply.writeNoException();
                return true;
            case 140:
                data.enforceInterface(IActivityManager.descriptor);
                killAllBackgroundProcesses();
                reply.writeNoException();
                return true;
            case 141:
                data.enforceInterface(IActivityManager.descriptor);
                String name4 = data.readString();
                int userId27 = data.readInt();
                IBinder token29 = data.readStrongBinder();
                IActivityManager.ContentProviderHolder cph2 = getContentProviderExternal(name4, userId27, token29);
                reply.writeNoException();
                if (cph2 != null) {
                    reply.writeInt(1);
                    cph2.writeToParcel(reply, 0);
                } else {
                    reply.writeInt(0);
                }
                return true;
            case 142:
                data.enforceInterface(IActivityManager.descriptor);
                String name5 = data.readString();
                IBinder token30 = data.readStrongBinder();
                removeContentProviderExternal(name5, token30);
                reply.writeNoException();
                return true;
            case 143:
                data.enforceInterface(IActivityManager.descriptor);
                ActivityManager.RunningAppProcessInfo info = new ActivityManager.RunningAppProcessInfo();
                getMyMemoryState(info);
                reply.writeNoException();
                info.writeToParcel(reply, 0);
                return true;
            case 144:
                data.enforceInterface(IActivityManager.descriptor);
                String reason4 = data.readString();
                boolean res29 = killProcessesBelowForeground(reason4);
                reply.writeNoException();
                reply.writeInt(res29 ? 1 : 0);
                return true;
            case 145:
                data.enforceInterface(IActivityManager.descriptor);
                UserInfo userInfo = getCurrentUser();
                reply.writeNoException();
                userInfo.writeToParcel(reply, 0);
                return true;
            case 146:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder token31 = data.readStrongBinder();
                String destAffinity = data.readString();
                boolean res30 = shouldUpRecreateTask(token31, destAffinity);
                reply.writeNoException();
                reply.writeInt(res30 ? 1 : 0);
                return true;
            case 147:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder token32 = data.readStrongBinder();
                Intent target = Intent.CREATOR.createFromParcel(data);
                int resultCode5 = data.readInt();
                Intent resultData5 = null;
                if (data.readInt() != 0) {
                    Intent resultData6 = Intent.CREATOR.createFromParcel(data);
                    resultData5 = resultData6;
                }
                boolean res31 = navigateUpTo(token32, target, resultCode5, resultData5);
                reply.writeNoException();
                reply.writeInt(res31 ? 1 : 0);
                return true;
            case 148:
                data.enforceInterface(IActivityManager.descriptor);
                setLockScreenShown(data.readInt() != 0);
                reply.writeNoException();
                return true;
            case 149:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder token33 = data.readStrongBinder();
                boolean res32 = finishActivityAffinity(token33);
                reply.writeNoException();
                reply.writeInt(res32 ? 1 : 0);
                return true;
            case 150:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder token34 = data.readStrongBinder();
                int res33 = getLaunchedFromUid(token34);
                reply.writeNoException();
                reply.writeInt(res33);
                return true;
            case 151:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder b27 = data.readStrongBinder();
                unstableProviderDied(b27);
                reply.writeNoException();
                return true;
            case 152:
                data.enforceInterface(IActivityManager.descriptor);
                IIntentSender r5 = IIntentSender.Stub.asInterface(data.readStrongBinder());
                boolean res34 = isIntentSenderAnActivity(r5);
                reply.writeNoException();
                reply.writeInt(res34 ? 1 : 0);
                return true;
            case 153:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder b28 = data.readStrongBinder();
                IApplicationThread app22 = ApplicationThreadNative.asInterface(b28);
                String callingPackage5 = data.readString();
                Intent intent11 = Intent.CREATOR.createFromParcel(data);
                String resolvedType10 = data.readString();
                IBinder resultTo7 = data.readStrongBinder();
                String resultWho7 = data.readString();
                int requestCode7 = data.readInt();
                int startFlags4 = data.readInt();
                ProfilerInfo profilerInfo4 = data.readInt() != 0 ? ProfilerInfo.CREATOR.createFromParcel(data) : null;
                Bundle options9 = data.readInt() != 0 ? Bundle.CREATOR.createFromParcel(data) : null;
                int userId28 = data.readInt();
                int result10 = startActivityAsUser(app22, callingPackage5, intent11, resolvedType10, resultTo7, resultWho7, requestCode7, startFlags4, profilerInfo4, options9, userId28);
                reply.writeNoException();
                reply.writeInt(result10);
                return true;
            case 154:
                data.enforceInterface(IActivityManager.descriptor);
                int userid3 = data.readInt();
                IStopUserCallback callback = IStopUserCallback.Stub.asInterface(data.readStrongBinder());
                int result11 = stopUser(userid3, callback);
                reply.writeNoException();
                reply.writeInt(result11);
                return true;
            case 155:
                data.enforceInterface(IActivityManager.descriptor);
                IUserSwitchObserver observer4 = IUserSwitchObserver.Stub.asInterface(data.readStrongBinder());
                registerUserSwitchObserver(observer4);
                reply.writeNoException();
                return true;
            case 156:
                data.enforceInterface(IActivityManager.descriptor);
                IUserSwitchObserver observer5 = IUserSwitchObserver.Stub.asInterface(data.readStrongBinder());
                unregisterUserSwitchObserver(observer5);
                reply.writeNoException();
                return true;
            case 157:
                data.enforceInterface(IActivityManager.descriptor);
                int[] result12 = getRunningUserIds();
                reply.writeNoException();
                reply.writeIntArray(result12);
                return true;
            case 158:
                data.enforceInterface(IActivityManager.descriptor);
                requestBugReport();
                reply.writeNoException();
                return true;
            case 159:
                data.enforceInterface(IActivityManager.descriptor);
                int pid4 = data.readInt();
                boolean aboveSystem = data.readInt() != 0;
                String reason5 = data.readString();
                long res35 = inputDispatchingTimedOut(pid4, aboveSystem, reason5);
                reply.writeNoException();
                reply.writeLong(res35);
                return true;
            case 161:
                data.enforceInterface(IActivityManager.descriptor);
                IIntentSender r6 = IIntentSender.Stub.asInterface(data.readStrongBinder());
                Intent intent12 = getIntentForIntentSender(r6);
                reply.writeNoException();
                if (intent12 != null) {
                    reply.writeInt(1);
                    intent12.writeToParcel(reply, 1);
                } else {
                    reply.writeInt(0);
                }
                return true;
            case 162:
                data.enforceInterface(IActivityManager.descriptor);
                int requestType = data.readInt();
                Bundle res36 = getAssistContextExtras(requestType);
                reply.writeNoException();
                reply.writeBundle(res36);
                return true;
            case 163:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder token35 = data.readStrongBinder();
                Bundle extras = data.readBundle();
                reportAssistContextExtras(token35, extras);
                reply.writeNoException();
                return true;
            case 164:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder token36 = data.readStrongBinder();
                String res37 = getLaunchedFromPackage(token36);
                reply.writeNoException();
                reply.writeString(res37);
                return true;
            case 165:
                data.enforceInterface(IActivityManager.descriptor);
                int uid5 = data.readInt();
                String reason6 = data.readString();
                killUid(uid5, reason6);
                reply.writeNoException();
                return true;
            case 166:
                data.enforceInterface(IActivityManager.descriptor);
                boolean monkey = data.readInt() == 1;
                setUserIsMonkey(monkey);
                reply.writeNoException();
                return true;
            case 167:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder who2 = data.readStrongBinder();
                boolean allowRestart = data.readInt() != 0;
                hang(who2, allowRestart);
                reply.writeNoException();
                return true;
            case 168:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder parentActivityToken = data.readStrongBinder();
                IActivityContainerCallback callback2 = IActivityContainerCallback.Stub.asInterface(data.readStrongBinder());
                IActivityContainer activityContainer = createActivityContainer(parentActivityToken, callback2);
                reply.writeNoException();
                if (activityContainer != null) {
                    reply.writeInt(1);
                    reply.writeStrongBinder(activityContainer.asBinder());
                } else {
                    reply.writeInt(0);
                }
                return true;
            case 169:
                data.enforceInterface(IActivityManager.descriptor);
                int taskId2 = data.readInt();
                int stackId = data.readInt();
                boolean toTop = data.readInt() != 0;
                moveTaskToStack(taskId2, stackId, toTop);
                reply.writeNoException();
                return true;
            case 170:
                data.enforceInterface(IActivityManager.descriptor);
                int stackId2 = data.readInt();
                data.readFloat();
                Rect r7 = Rect.CREATOR.createFromParcel(data);
                resizeStack(stackId2, r7);
                reply.writeNoException();
                return true;
            case 171:
                data.enforceInterface(IActivityManager.descriptor);
                List<ActivityManager.StackInfo> list7 = getAllStackInfos();
                reply.writeNoException();
                reply.writeTypedList(list7);
                return true;
            case 172:
                data.enforceInterface(IActivityManager.descriptor);
                int stackId3 = data.readInt();
                setFocusedStack(stackId3);
                reply.writeNoException();
                return true;
            case 173:
                data.enforceInterface(IActivityManager.descriptor);
                int stackId4 = data.readInt();
                ActivityManager.StackInfo info2 = getStackInfo(stackId4);
                reply.writeNoException();
                if (info2 != null) {
                    reply.writeInt(1);
                    info2.writeToParcel(reply, 0);
                } else {
                    reply.writeInt(0);
                }
                return true;
            case 174:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder token37 = data.readStrongBinder();
                boolean converted = convertFromTranslucent(token37);
                reply.writeNoException();
                reply.writeInt(converted ? 1 : 0);
                return true;
            case 175:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder token38 = data.readStrongBinder();
                if (data.readInt() == 0) {
                    bundle2 = null;
                } else {
                    bundle2 = data.readBundle();
                }
                ActivityOptions options10 = bundle2 == null ? null : new ActivityOptions(bundle2);
                boolean converted2 = convertToTranslucent(token38, options10);
                reply.writeNoException();
                reply.writeInt(converted2 ? 1 : 0);
                return true;
            case 176:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder token39 = data.readStrongBinder();
                notifyActivityDrawn(token39);
                reply.writeNoException();
                return true;
            case 177:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder token40 = data.readStrongBinder();
                reportActivityFullyDrawn(token40);
                reply.writeNoException();
                return true;
            case 178:
                data.enforceInterface(IActivityManager.descriptor);
                restart();
                reply.writeNoException();
                return true;
            case 179:
                data.enforceInterface(IActivityManager.descriptor);
                performIdleMaintenance();
                reply.writeNoException();
                return true;
            case 180:
                data.enforceInterface(IActivityManager.descriptor);
                Uri uri10 = Uri.CREATOR.createFromParcel(data);
                int mode10 = data.readInt();
                int userId29 = data.readInt();
                takePersistableUriPermission(uri10, mode10, userId29);
                reply.writeNoException();
                return true;
            case 181:
                data.enforceInterface(IActivityManager.descriptor);
                Uri uri11 = Uri.CREATOR.createFromParcel(data);
                int mode11 = data.readInt();
                int userId30 = data.readInt();
                releasePersistableUriPermission(uri11, mode11, userId30);
                reply.writeNoException();
                return true;
            case 182:
                data.enforceInterface(IActivityManager.descriptor);
                String packageName11 = data.readString();
                boolean incoming = data.readInt() != 0;
                ParceledListSlice<UriPermission> perms = getPersistedUriPermissions(packageName11, incoming);
                reply.writeNoException();
                perms.writeToParcel(reply, 1);
                return true;
            case 183:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder b29 = data.readStrongBinder();
                appNotRespondingViaProvider(b29);
                reply.writeNoException();
                return true;
            case 184:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder homeActivityToken = getHomeActivityToken();
                reply.writeNoException();
                reply.writeStrongBinder(homeActivityToken);
                return true;
            case 185:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder activityToken = data.readStrongBinder();
                int displayId = getActivityDisplayId(activityToken);
                reply.writeNoException();
                reply.writeInt(displayId);
                return true;
            case 186:
                data.enforceInterface(IActivityManager.descriptor);
                deleteActivityContainer(IActivityContainer.Stub.asInterface(data.readStrongBinder()));
                reply.writeNoException();
                return true;
            case 211:
                data.enforceInterface(IActivityManager.descriptor);
                IIntentSender r8 = IIntentSender.Stub.asInterface(data.readStrongBinder());
                String prefix = data.readString();
                String tag2 = getTagForIntentSender(r8, prefix);
                reply.writeNoException();
                reply.writeString(tag2);
                return true;
            case 212:
                data.enforceInterface(IActivityManager.descriptor);
                int userid4 = data.readInt();
                boolean result13 = startUserInBackground(userid4);
                reply.writeNoException();
                reply.writeInt(result13 ? 1 : 0);
                return true;
            case 213:
                data.enforceInterface(IActivityManager.descriptor);
                int taskId3 = data.readInt();
                boolean isInHomeStack = isInHomeStack(taskId3);
                reply.writeNoException();
                reply.writeInt(isInHomeStack ? 1 : 0);
                return true;
            case 214:
                data.enforceInterface(IActivityManager.descriptor);
                int taskId4 = data.readInt();
                startLockTaskMode(taskId4);
                reply.writeNoException();
                return true;
            case 215:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder token41 = data.readStrongBinder();
                startLockTaskMode(token41);
                reply.writeNoException();
                return true;
            case 216:
                data.enforceInterface(IActivityManager.descriptor);
                stopLockTaskMode();
                reply.writeNoException();
                return true;
            case 217:
                data.enforceInterface(IActivityManager.descriptor);
                boolean isInLockTaskMode = isInLockTaskMode();
                reply.writeNoException();
                reply.writeInt(isInLockTaskMode ? 1 : 0);
                return true;
            case 218:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder token42 = data.readStrongBinder();
                ActivityManager.TaskDescription values = ActivityManager.TaskDescription.CREATOR.createFromParcel(data);
                setTaskDescription(token42, values);
                reply.writeNoException();
                return true;
            case 219:
                data.enforceInterface(IActivityManager.descriptor);
                String callingPackage6 = data.readString();
                int callingPid2 = data.readInt();
                int callingUid3 = data.readInt();
                Intent intent13 = Intent.CREATOR.createFromParcel(data);
                String resolvedType11 = data.readString();
                IVoiceInteractionSession session = IVoiceInteractionSession.Stub.asInterface(data.readStrongBinder());
                IVoiceInteractor interactor = IVoiceInteractor.Stub.asInterface(data.readStrongBinder());
                int startFlags5 = data.readInt();
                ProfilerInfo profilerInfo5 = data.readInt() != 0 ? ProfilerInfo.CREATOR.createFromParcel(data) : null;
                Bundle options11 = data.readInt() != 0 ? Bundle.CREATOR.createFromParcel(data) : null;
                int userId31 = data.readInt();
                int result14 = startVoiceActivity(callingPackage6, callingPid2, callingUid3, intent13, resolvedType11, session, interactor, startFlags5, profilerInfo5, options11, userId31);
                reply.writeNoException();
                reply.writeInt(result14);
                return true;
            case 220:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder token43 = data.readStrongBinder();
                ActivityOptions options12 = getActivityOptions(token43);
                reply.writeNoException();
                reply.writeBundle(options12 == null ? null : options12.toBundle());
                return true;
            case 221:
                data.enforceInterface(IActivityManager.descriptor);
                String callingPackage7 = data.readString();
                List<IAppTask> list8 = getAppTasks(callingPackage7);
                reply.writeNoException();
                int N3 = list8 != null ? list8.size() : -1;
                reply.writeInt(N3);
                for (int i3 = 0; i3 < N3; i3++) {
                    IAppTask task4 = list8.get(i3);
                    reply.writeStrongBinder(task4.asBinder());
                }
                return true;
            case 222:
                data.enforceInterface(IActivityManager.descriptor);
                startLockTaskModeOnCurrent();
                reply.writeNoException();
                return true;
            case 223:
                data.enforceInterface(IActivityManager.descriptor);
                stopLockTaskModeOnCurrent();
                reply.writeNoException();
                return true;
            case 224:
                data.enforceInterface(IActivityManager.descriptor);
                IVoiceInteractionSession session2 = IVoiceInteractionSession.Stub.asInterface(data.readStrongBinder());
                finishVoiceTask(session2);
                reply.writeNoException();
                return true;
            case 225:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder token44 = data.readStrongBinder();
                boolean isTopOfTask = isTopOfTask(token44);
                reply.writeNoException();
                reply.writeInt(isTopOfTask ? 1 : 0);
                return true;
            case 226:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder token45 = data.readStrongBinder();
                boolean enable = data.readInt() > 0;
                boolean success2 = requestVisibleBehind(token45, enable);
                reply.writeNoException();
                reply.writeInt(success2 ? 1 : 0);
                return true;
            case 227:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder token46 = data.readStrongBinder();
                boolean enabled2 = isBackgroundVisibleBehind(token46);
                reply.writeNoException();
                reply.writeInt(enabled2 ? 1 : 0);
                return true;
            case 228:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder token47 = data.readStrongBinder();
                backgroundResourcesReleased(token47);
                reply.writeNoException();
                return true;
            case 229:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder token48 = data.readStrongBinder();
                notifyLaunchTaskBehindComplete(token48);
                reply.writeNoException();
                return true;
            case 230:
                data.enforceInterface(IActivityManager.descriptor);
                int taskId5 = data.readInt();
                Bundle options13 = data.readInt() == 0 ? null : Bundle.CREATOR.createFromParcel(data);
                int result15 = startActivityFromRecents(taskId5, options13);
                reply.writeNoException();
                reply.writeInt(result15);
                return true;
            case 231:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder token49 = data.readStrongBinder();
                notifyEnterAnimationComplete(token49);
                reply.writeNoException();
                return true;
            case 232:
                data.enforceInterface(IActivityManager.descriptor);
                keyguardWaitingForActivityDrawn();
                reply.writeNoException();
                return true;
            case 233:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder b30 = data.readStrongBinder();
                IApplicationThread app23 = ApplicationThreadNative.asInterface(b30);
                String callingPackage8 = data.readString();
                Intent intent14 = Intent.CREATOR.createFromParcel(data);
                String resolvedType12 = data.readString();
                IBinder resultTo8 = data.readStrongBinder();
                String resultWho8 = data.readString();
                int requestCode8 = data.readInt();
                int startFlags6 = data.readInt();
                ProfilerInfo profilerInfo6 = data.readInt() != 0 ? ProfilerInfo.CREATOR.createFromParcel(data) : null;
                Bundle options14 = data.readInt() != 0 ? Bundle.CREATOR.createFromParcel(data) : null;
                int userId32 = data.readInt();
                int result16 = startActivityAsCaller(app23, callingPackage8, intent14, resolvedType12, resultTo8, resultWho8, requestCode8, startFlags6, profilerInfo6, options14, userId32);
                reply.writeNoException();
                reply.writeInt(result16);
                return true;
            case 234:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder activityToken2 = data.readStrongBinder();
                Intent intent15 = Intent.CREATOR.createFromParcel(data);
                ActivityManager.TaskDescription descr = ActivityManager.TaskDescription.CREATOR.createFromParcel(data);
                Bitmap thumbnail = Bitmap.CREATOR.createFromParcel(data);
                int res38 = addAppTask(activityToken2, intent15, descr, thumbnail);
                reply.writeNoException();
                reply.writeInt(res38);
                return true;
            case 235:
                data.enforceInterface(IActivityManager.descriptor);
                Point size = getAppTaskThumbnailSize();
                reply.writeNoException();
                size.writeToParcel(reply, 0);
                return true;
            case 236:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder token50 = data.readStrongBinder();
                boolean res39 = releaseActivityInstance(token50);
                reply.writeNoException();
                reply.writeInt(res39 ? 1 : 0);
                return true;
            case 237:
                data.enforceInterface(IActivityManager.descriptor);
                IApplicationThread app24 = ApplicationThreadNative.asInterface(data.readStrongBinder());
                releaseSomeActivities(app24);
                reply.writeNoException();
                return true;
            case 238:
                data.enforceInterface(IActivityManager.descriptor);
                bootAnimationComplete();
                reply.writeNoException();
                return true;
            case 239:
                data.enforceInterface(IActivityManager.descriptor);
                String filename = data.readString();
                Bitmap icon = getTaskDescriptionIcon(filename);
                reply.writeNoException();
                if (icon == null) {
                    reply.writeInt(0);
                } else {
                    reply.writeInt(1);
                    icon.writeToParcel(reply, 0);
                }
                return true;
            case 240:
                data.enforceInterface(IActivityManager.descriptor);
                Intent intent16 = Intent.CREATOR.createFromParcel(data);
                int requestType2 = data.readInt();
                String hint = data.readString();
                int userHandle = data.readInt();
                boolean res40 = launchAssistIntent(intent16, requestType2, hint, userHandle);
                reply.writeNoException();
                reply.writeInt(res40 ? 1 : 0);
                return true;
            case 241:
                data.enforceInterface(IActivityManager.descriptor);
                if (data.readInt() == 0) {
                    bundle = null;
                } else {
                    bundle = data.readBundle();
                }
                ActivityOptions options15 = bundle == null ? null : new ActivityOptions(bundle);
                startInPlaceAnimationOnFrontMostApplication(options15);
                reply.writeNoException();
                return true;
            case 242:
                data.enforceInterface(IActivityManager.descriptor);
                String perm5 = data.readString();
                int pid5 = data.readInt();
                int uid6 = data.readInt();
                IBinder token51 = data.readStrongBinder();
                int res41 = checkPermissionWithToken(perm5, pid5, uid6, token51);
                reply.writeNoException();
                reply.writeInt(res41);
                return true;
            case 243:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder token52 = data.readStrongBinder();
                registerTaskStackListener(ITaskStackListener.Stub.asInterface(token52));
                reply.writeNoException();
                return true;
            case 244:
                data.enforceInterface(IActivityManager.descriptor);
                systemBackupRestored();
                reply.writeNoException();
                return true;
        }
    }

    @Override
    public IBinder asBinder() {
        return this;
    }
}
