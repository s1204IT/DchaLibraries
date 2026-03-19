package android.app;

import android.R;
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
import android.app.IUidObserver;
import android.app.IUserSwitchObserver;
import android.app.assist.AssistContent;
import android.app.assist.AssistStructure;
import android.bluetooth.BluetoothClass;
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
import android.media.MediaFile;
import android.net.Uri;
import android.net.wifi.AnqpInformationElement;
import android.os.Binder;
import android.os.Bundle;
import android.os.Debug;
import android.os.IBinder;
import android.os.IProgressListener;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.StrictMode;
import android.provider.Downloads;
import android.service.voice.IVoiceInteractionSession;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Singleton;
import com.android.internal.app.IVoiceInteractor;
import com.android.internal.os.IResultReceiver;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class ActivityManagerNative extends Binder implements IActivityManager {
    static volatile boolean sSystemReady = false;
    private static final Singleton<IActivityManager> gDefault = new Singleton<IActivityManager>() {
        protected IActivityManager m92create() {
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
        if (in != null) {
            return in;
        }
        return new ActivityManagerProxy(obj);
    }

    public static IActivityManager getDefault() {
        return (IActivityManager) gDefault.get();
    }

    public static boolean isSystemReady() {
        if (!sSystemReady) {
            sSystemReady = getDefault().testIsSystemReady();
        }
        return sSystemReady;
    }

    public static void broadcastStickyIntent(Intent intent, String permission, int userId) {
        broadcastStickyIntent(intent, permission, -1, userId);
    }

    public static void broadcastStickyIntent(Intent intent, String permission, int appOp, int userId) {
        try {
            getDefault().broadcastIntent(null, intent, null, null, -1, null, null, null, appOp, null, false, true, userId);
        } catch (RemoteException e) {
        }
    }

    public static void noteWakeupAlarm(PendingIntent ps, int sourceUid, String sourcePkg, String tag) {
        try {
            getDefault().noteWakeupAlarm(ps != null ? ps.getTarget() : null, sourceUid, sourcePkg, tag);
        } catch (RemoteException e) {
        }
    }

    public static void noteAlarmStart(PendingIntent ps, int sourceUid, String tag) {
        try {
            getDefault().noteAlarmStart(ps != null ? ps.getTarget() : null, sourceUid, tag);
        } catch (RemoteException e) {
        }
    }

    public static void noteAlarmFinish(PendingIntent ps, int sourceUid, String tag) {
        try {
            getDefault().noteAlarmFinish(ps != null ? ps.getTarget() : null, sourceUid, tag);
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
        String[] strArrCreateStringArray;
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
                ProfilerInfo profilerInfoCreateFromParcel = data.readInt() != 0 ? ProfilerInfo.CREATOR.createFromParcel(data) : null;
                Bundle options = data.readInt() != 0 ? Bundle.CREATOR.createFromParcel(data) : null;
                int result = startActivity(app2, callingPackage, intent, resolvedType, resultTo, resultWho, requestCode, startFlags, profilerInfoCreateFromParcel, options);
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
                    return true;
                }
                reply.writeInt(0);
                return true;
            case 6:
            case 7:
            case 8:
            case 9:
            case 10:
            case 25:
            case 28:
            case 40:
            case 41:
            case 139:
            case 160:
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
            case R.styleable.Theme_searchViewStyle:
            case 245:
            case R.styleable.Theme_buttonBarNeutralButtonStyle:
            case R.styleable.Theme_buttonBarNegativeButtonStyle:
            case R.styleable.Theme_actionBarPopupTheme:
            case R.styleable.Theme_timePickerStyle:
            case 250:
            case R.styleable.Theme_toolbarStyle:
            case 252:
            case R.styleable.Theme_windowReturnTransition:
            case 254:
            case 255:
            case 256:
            case 257:
            case 258:
            case 259:
            case 260:
            case 261:
            case 262:
            case 263:
            case 264:
            case 265:
            case 266:
            case 267:
            case 268:
            case 269:
            case 270:
            case AnqpInformationElement.ANQP_EMERGENCY_NAI:
            case 272:
            case 273:
            case 274:
            case 275:
            case BluetoothClass.Device.COMPUTER_PALM_SIZE_PC_PDA:
            case 277:
            case 278:
            case 279:
            case BluetoothClass.Device.COMPUTER_WEARABLE:
            case MediaFile.FILE_TYPE_3GPP:
            case MediaFile.FILE_TYPE_3GPP2:
            case MediaFile.FILE_TYPE_WMV:
            case MediaFile.FILE_TYPE_ASF:
            case MediaFile.FILE_TYPE_MKV:
            case MediaFile.FILE_TYPE_MP2TS:
            case MediaFile.FILE_TYPE_AVI:
            case MediaFile.FILE_TYPE_WEBM:
            case 311:
            case 312:
            case 313:
            case 314:
            case 315:
            case 316:
            case 317:
            case 318:
            case 319:
            case 320:
            case 321:
            case 322:
            case 323:
            case 324:
            case 325:
            case 326:
            case 327:
            case 328:
            case 329:
            case 330:
            case 331:
            case 332:
            case 333:
            case 334:
            case 335:
            case 336:
            case 337:
            case 338:
            case 339:
            case 340:
            case 378:
            case 379:
            case 380:
            case 381:
            case 382:
            case 383:
            case 384:
            case 385:
            case 386:
            case 387:
            case 388:
            case 389:
            case 390:
            case 391:
            case 392:
            case MediaFile.FILE_TYPE_MP2PS:
            case MediaFile.FILE_TYPE_OGM:
            case MediaFile.FILE_TYPE_RV:
            case MediaFile.FILE_TYPE_RMVB:
            case MediaFile.FILE_TYPE_QUICKTIME_VIDEO:
            case MediaFile.FILE_TYPE_FLV:
            case MediaFile.FILE_TYPE_RM:
            case 400:
            case 401:
            case MediaFile.FILE_TYPE_GIF:
            case 403:
            case 404:
            case 405:
            case 407:
            case 408:
            case 409:
            case 410:
            case Downloads.Impl.STATUS_LENGTH_REQUIRED:
            case Downloads.Impl.STATUS_PRECONDITION_FAILED:
            case 413:
            case 414:
            case 415:
            case 416:
            case 417:
            case 418:
            case 419:
            case 420:
            case 421:
            case 422:
            case 423:
            case 424:
            case 425:
            case 426:
            case 427:
            case 428:
            case 429:
            case 430:
            case 431:
            case 432:
            case 433:
            case 434:
            case 435:
            case 436:
            case 437:
            case 438:
            case 439:
            case 440:
            case 441:
            case 442:
            case 443:
            case 444:
            case 445:
            case 446:
            case 447:
            case 448:
            case 449:
            case 450:
            case 451:
            case 452:
            case 453:
            case 454:
            case 455:
            case 456:
            case 457:
            case 458:
            case 459:
            case 460:
            case 461:
            case 462:
            case 463:
            case 464:
            case 465:
            case 466:
            case 467:
            case 468:
            case 469:
            case 470:
            case 471:
            case 472:
            case 473:
            case 474:
            case 475:
            case 476:
            case 477:
            case 478:
            case 479:
            case 480:
            case 481:
            case 482:
            case 483:
            case 484:
            case 485:
            case 486:
            case 487:
            case 488:
            case Downloads.Impl.STATUS_CANNOT_RESUME:
            case 490:
            case 491:
            case 492:
            case Downloads.Impl.STATUS_UNHANDLED_REDIRECT:
            case Downloads.Impl.STATUS_UNHANDLED_HTTP_CODE:
            case Downloads.Impl.STATUS_HTTP_DATA_ERROR:
            case Downloads.Impl.STATUS_HTTP_EXCEPTION:
            case Downloads.Impl.STATUS_TOO_MANY_REDIRECTS:
            case Downloads.Impl.STATUS_BLOCKED:
            case MediaFile.FILE_TYPE_MPO:
            case 500:
            default:
                return super.onTransact(code, data, reply, flags);
            case 11:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder token = data.readStrongBinder();
                Intent intent2 = null;
                int resultCode = data.readInt();
                if (data.readInt() != 0) {
                    Intent resultData = Intent.CREATOR.createFromParcel(data);
                    intent2 = resultData;
                }
                int finishTask = data.readInt();
                boolean res = finishActivity(token, resultCode, intent2, finishTask);
                reply.writeNoException();
                reply.writeInt(res ? 1 : 0);
                return true;
            case 12:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder b2 = data.readStrongBinder();
                IApplicationThread iApplicationThreadAsInterface = b2 != null ? ApplicationThreadNative.asInterface(b2) : null;
                String packageName = data.readString();
                IBinder b3 = data.readStrongBinder();
                IIntentReceiver iIntentReceiverAsInterface = b3 != null ? IIntentReceiver.Stub.asInterface(b3) : null;
                IntentFilter filter = IntentFilter.CREATOR.createFromParcel(data);
                String perm = data.readString();
                int userId = data.readInt();
                Intent intent3 = registerReceiver(iApplicationThreadAsInterface, packageName, iIntentReceiverAsInterface, filter, perm, userId);
                reply.writeNoException();
                if (intent3 != null) {
                    reply.writeInt(1);
                    intent3.writeToParcel(reply, 0);
                    return true;
                }
                reply.writeInt(0);
                return true;
            case 13:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder b4 = data.readStrongBinder();
                if (b4 == null) {
                    return true;
                }
                IIntentReceiver rec = IIntentReceiver.Stub.asInterface(b4);
                unregisterReceiver(rec);
                reply.writeNoException();
                return true;
            case 14:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder b5 = data.readStrongBinder();
                IApplicationThread iApplicationThreadAsInterface2 = b5 != null ? ApplicationThreadNative.asInterface(b5) : null;
                Intent intent4 = Intent.CREATOR.createFromParcel(data);
                String resolvedType2 = data.readString();
                IBinder b6 = data.readStrongBinder();
                IIntentReceiver iIntentReceiverAsInterface2 = b6 != null ? IIntentReceiver.Stub.asInterface(b6) : null;
                int resultCode2 = data.readInt();
                String resultData2 = data.readString();
                Bundle resultExtras = data.readBundle();
                String[] perms = data.readStringArray();
                int appOp = data.readInt();
                Bundle options2 = data.readBundle();
                boolean serialized = data.readInt() != 0;
                boolean sticky = data.readInt() != 0;
                int userId2 = data.readInt();
                int res2 = broadcastIntent(iApplicationThreadAsInterface2, intent4, resolvedType2, iIntentReceiverAsInterface2, resultCode2, resultData2, resultExtras, perms, appOp, options2, serialized, sticky, userId2);
                reply.writeNoException();
                reply.writeInt(res2);
                return true;
            case 15:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder b7 = data.readStrongBinder();
                IApplicationThread iApplicationThreadAsInterface3 = b7 != null ? ApplicationThreadNative.asInterface(b7) : null;
                Intent intent5 = Intent.CREATOR.createFromParcel(data);
                int userId3 = data.readInt();
                unbroadcastIntent(iApplicationThreadAsInterface3, intent5, userId3);
                reply.writeNoException();
                return true;
            case 16:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder who = data.readStrongBinder();
                int resultCode3 = data.readInt();
                String resultData3 = data.readString();
                Bundle resultExtras2 = data.readBundle();
                boolean resultAbort = data.readInt() != 0;
                int intentFlags = data.readInt();
                if (who != null) {
                    finishReceiver(who, resultCode3, resultData3, resultExtras2, resultAbort, intentFlags);
                }
                reply.writeNoException();
                return true;
            case 17:
                data.enforceInterface(IActivityManager.descriptor);
                IApplicationThread app3 = ApplicationThreadNative.asInterface(data.readStrongBinder());
                if (app3 != null) {
                    attachApplication(app3);
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
                CharSequence description = (CharSequence) TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(data);
                activityStopped(token4, map, persistentState, description);
                reply.writeNoException();
                return true;
            case 21:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder token5 = data.readStrongBinder();
                String callingPackage2 = token5 != null ? getCallingPackage(token5) : null;
                reply.writeNoException();
                reply.writeString(callingPackage2);
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
                Bundle options3 = data.readInt() != 0 ? Bundle.CREATOR.createFromParcel(data) : null;
                moveTaskToFront(task, fl2, options3);
                reply.writeNoException();
                return true;
            case 26:
                data.enforceInterface(IActivityManager.descriptor);
                int task2 = data.readInt();
                moveTaskBackwards(task2);
                reply.writeNoException();
                return true;
            case 27:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder token7 = data.readStrongBinder();
                boolean onlyRoot = data.readInt() != 0;
                int res3 = token7 != null ? getTaskForActivity(token7, onlyRoot) : -1;
                reply.writeNoException();
                reply.writeInt(res3);
                return true;
            case 29:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder b8 = data.readStrongBinder();
                IApplicationThread app4 = ApplicationThreadNative.asInterface(b8);
                String name = data.readString();
                int userId4 = data.readInt();
                boolean stable = data.readInt() != 0;
                IActivityManager.ContentProviderHolder cph = getContentProvider(app4, name, userId4, stable);
                reply.writeNoException();
                if (cph != null) {
                    reply.writeInt(1);
                    cph.writeToParcel(reply, 0);
                    return true;
                }
                reply.writeInt(0);
                return true;
            case 30:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder b9 = data.readStrongBinder();
                IApplicationThread app5 = ApplicationThreadNative.asInterface(b9);
                ArrayList<IActivityManager.ContentProviderHolder> providers = data.createTypedArrayList(IActivityManager.ContentProviderHolder.CREATOR);
                publishContentProviders(app5, providers);
                reply.writeNoException();
                return true;
            case 31:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder b10 = data.readStrongBinder();
                int stable2 = data.readInt();
                int unstable = data.readInt();
                boolean res4 = refContentProvider(b10, stable2, unstable);
                reply.writeNoException();
                reply.writeInt(res4 ? 1 : 0);
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
                IApplicationThread app6 = ApplicationThreadNative.asInterface(b11);
                Intent service = Intent.CREATOR.createFromParcel(data);
                String resolvedType3 = data.readString();
                String callingPackage3 = data.readString();
                int userId5 = data.readInt();
                ComponentName cn2 = startService(app6, service, resolvedType3, callingPackage3, userId5);
                reply.writeNoException();
                ComponentName.writeToParcel(cn2, reply);
                return true;
            case 35:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder b12 = data.readStrongBinder();
                IApplicationThread app7 = ApplicationThreadNative.asInterface(b12);
                Intent service2 = Intent.CREATOR.createFromParcel(data);
                String resolvedType4 = data.readString();
                int userId6 = data.readInt();
                int res5 = stopService(app7, service2, resolvedType4, userId6);
                reply.writeNoException();
                reply.writeInt(res5);
                return true;
            case 36:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder b13 = data.readStrongBinder();
                IApplicationThread app8 = ApplicationThreadNative.asInterface(b13);
                IBinder token9 = data.readStrongBinder();
                Intent service3 = Intent.CREATOR.createFromParcel(data);
                String resolvedType5 = data.readString();
                IBinder b14 = data.readStrongBinder();
                int fl3 = data.readInt();
                String callingPackage4 = data.readString();
                int userId7 = data.readInt();
                IServiceConnection conn = IServiceConnection.Stub.asInterface(b14);
                int res6 = bindService(app8, token9, service3, resolvedType5, conn, fl3, callingPackage4, userId7);
                reply.writeNoException();
                reply.writeInt(res6);
                return true;
            case 37:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder b15 = data.readStrongBinder();
                IServiceConnection conn2 = IServiceConnection.Stub.asInterface(b15);
                boolean res7 = unbindService(conn2);
                reply.writeNoException();
                reply.writeInt(res7 ? 1 : 0);
                return true;
            case 38:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder token10 = data.readStrongBinder();
                Intent intent6 = Intent.CREATOR.createFromParcel(data);
                IBinder service4 = data.readStrongBinder();
                publishService(token10, intent6, service4);
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
                boolean res8 = startInstrumentation(className, profileFile, fl4, arguments, w, c, userId8, abiOverride);
                reply.writeNoException();
                reply.writeInt(res8 ? 1 : 0);
                return true;
            case 45:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder b18 = data.readStrongBinder();
                IApplicationThread app9 = ApplicationThreadNative.asInterface(b18);
                int resultCode4 = data.readInt();
                Bundle results = data.readBundle();
                finishInstrumentation(app9, resultCode4, results);
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
                boolean res9 = stopServiceToken(className2, token12, startId);
                reply.writeNoException();
                reply.writeInt(res9 ? 1 : 0);
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
                String perm2 = data.readString();
                int pid = data.readInt();
                int uid = data.readInt();
                int res10 = checkPermission(perm2, pid, uid);
                reply.writeNoException();
                reply.writeInt(res10);
                return true;
            case 54:
                data.enforceInterface(IActivityManager.descriptor);
                Uri uri2 = Uri.CREATOR.createFromParcel(data);
                int pid2 = data.readInt();
                int uid2 = data.readInt();
                int mode = data.readInt();
                int userId9 = data.readInt();
                IBinder callerToken = data.readStrongBinder();
                int res11 = checkUriPermission(uri2, pid2, uid2, mode, userId9, callerToken);
                reply.writeNoException();
                reply.writeInt(res11);
                return true;
            case 55:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder b19 = data.readStrongBinder();
                IApplicationThread app10 = ApplicationThreadNative.asInterface(b19);
                String targetPkg = data.readString();
                Uri uri3 = Uri.CREATOR.createFromParcel(data);
                int mode2 = data.readInt();
                int userId10 = data.readInt();
                grantUriPermission(app10, targetPkg, uri3, mode2, userId10);
                reply.writeNoException();
                return true;
            case 56:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder b20 = data.readStrongBinder();
                IApplicationThread app11 = ApplicationThreadNative.asInterface(b20);
                Uri uri4 = Uri.CREATOR.createFromParcel(data);
                int mode3 = data.readInt();
                int userId11 = data.readInt();
                revokeUriPermission(app11, uri4, mode3, userId11);
                reply.writeNoException();
                return true;
            case 57:
                data.enforceInterface(IActivityManager.descriptor);
                IActivityController watcher = IActivityController.Stub.asInterface(data.readStrongBinder());
                boolean imAMonkey = data.readInt() != 0;
                setActivityController(watcher, imAMonkey);
                reply.writeNoException();
                return true;
            case 58:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder b21 = data.readStrongBinder();
                IApplicationThread app12 = ApplicationThreadNative.asInterface(b21);
                boolean waiting = data.readInt() != 0;
                showWaitingForDebugger(app12, waiting);
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
                ParceledListSlice<ActivityManager.RecentTaskInfo> list2 = getRecentTasks(maxNum2, fl5, userId12);
                reply.writeNoException();
                list2.writeToParcel(reply, 1);
                return true;
            case 61:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder token15 = data.readStrongBinder();
                int type = data.readInt();
                int startId2 = data.readInt();
                int res12 = data.readInt();
                serviceDoneExecuting(token15, type, startId2, res12);
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
                    strArrCreateStringArray = data.createStringArray();
                } else {
                    requestIntents = null;
                    strArrCreateStringArray = null;
                }
                int fl6 = data.readInt();
                Bundle bundleCreateFromParcel = data.readInt() != 0 ? Bundle.CREATOR.createFromParcel(data) : null;
                int userId13 = data.readInt();
                IIntentSender res13 = getIntentSender(type2, packageName2, token17, resultWho3, requestCode3, requestIntents, strArrCreateStringArray, fl6, bundleCreateFromParcel, userId13);
                reply.writeNoException();
                reply.writeStrongBinder(res13 != null ? res13.asBinder() : null);
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
                String res14 = getPackageForIntentSender(r2);
                reply.writeNoException();
                reply.writeString(res14);
                return true;
            case 66:
                data.enforceInterface(IActivityManager.descriptor);
                enterSafeMode();
                reply.writeNoException();
                return true;
            case 67:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder callingActivity = data.readStrongBinder();
                Intent intent7 = Intent.CREATOR.createFromParcel(data);
                Bundle options4 = data.readInt() != 0 ? Bundle.CREATOR.createFromParcel(data) : null;
                boolean result2 = startNextMatchingActivity(callingActivity, intent7, options4);
                reply.writeNoException();
                reply.writeInt(result2 ? 1 : 0);
                return true;
            case 68:
                data.enforceInterface(IActivityManager.descriptor);
                IIntentSender is = IIntentSender.Stub.asInterface(data.readStrongBinder());
                int sourceUid = data.readInt();
                String sourcePkg = data.readString();
                String tag = data.readString();
                noteWakeupAlarm(is, sourceUid, sourcePkg, tag);
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
                Intent intent8 = Intent.CREATOR.createFromParcel(data);
                boolean doRebind = data.readInt() != 0;
                unbindFinished(token20, intent8, doRebind);
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
                int sflags = data.readInt();
                setServiceForeground(className3, token22, id, notification, sflags);
                reply.writeNoException();
                return true;
            case 75:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder token23 = data.readStrongBinder();
                boolean nonRoot = data.readInt() != 0;
                boolean res15 = moveActivityTaskToBack(token23, nonRoot);
                reply.writeNoException();
                reply.writeInt(res15 ? 1 : 0);
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
                boolean res16 = clearApplicationUserData(packageName3, observer, userId14);
                reply.writeNoException();
                reply.writeInt(res16 ? 1 : 0);
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
                boolean res17 = killPids(pids, reason, secure);
                reply.writeNoException();
                reply.writeInt(res17 ? 1 : 0);
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
                    return true;
                }
                reply.writeInt(0);
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
                String callingPackage5 = data.readString();
                IBinder binder = peekService(service5, resolvedType6, callingPackage5);
                reply.writeNoException();
                reply.writeStrongBinder(binder);
                return true;
            case 86:
                data.enforceInterface(IActivityManager.descriptor);
                String process = data.readString();
                int userId16 = data.readInt();
                boolean start = data.readInt() != 0;
                int profileType = data.readInt();
                ProfilerInfo profilerInfo = data.readInt() != 0 ? ProfilerInfo.CREATOR.createFromParcel(data) : null;
                boolean res18 = profileControl(process, userId16, start, profilerInfo, profileType);
                reply.writeNoException();
                reply.writeInt(res18 ? 1 : 0);
                return true;
            case 87:
                data.enforceInterface(IActivityManager.descriptor);
                boolean res19 = shutdown(data.readInt());
                reply.writeNoException();
                reply.writeInt(res19 ? 1 : 0);
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
                int res20 = getUidForIntentSender(r3);
                reply.writeNoException();
                reply.writeInt(res20);
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
                int res21 = handleIncomingUser(callingPid, callingUid, userId18, allowAll, requireFull, name2, callerPackage);
                reply.writeNoException();
                reply.writeInt(res21);
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
                int appId = data.readInt();
                int userId19 = data.readInt();
                String reason2 = data.readString();
                killApplication(pkg, appId, userId19, reason2);
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
                Debug.MemoryInfo[] res22 = getProcessMemoryInfo(pids2);
                reply.writeNoException();
                reply.writeTypedArray(res22, 1);
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
                IApplicationThread app13 = ApplicationThreadNative.asInterface(b23);
                IntentSender intent9 = IntentSender.CREATOR.createFromParcel(data);
                Intent fillInIntent = null;
                if (data.readInt() != 0) {
                    Intent fillInIntent2 = Intent.CREATOR.createFromParcel(data);
                    fillInIntent = fillInIntent2;
                }
                String resolvedType7 = data.readString();
                IBinder resultTo2 = data.readStrongBinder();
                String resultWho4 = data.readString();
                int requestCode4 = data.readInt();
                int flagsMask = data.readInt();
                int flagsValues = data.readInt();
                Bundle options5 = data.readInt() != 0 ? Bundle.CREATOR.createFromParcel(data) : null;
                int result3 = startActivityIntentSender(app13, intent9, fillInIntent, resolvedType7, resultTo2, resultWho4, requestCode4, flagsMask, flagsValues, options5);
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
                IBinder app14 = data.readStrongBinder();
                String tag2 = data.readString();
                boolean system = data.readInt() != 0;
                ApplicationErrorReport.CrashInfo ci2 = new ApplicationErrorReport.CrashInfo(data);
                boolean res23 = handleApplicationWtf(app14, tag2, system, ci2);
                reply.writeNoException();
                reply.writeInt(res23 ? 1 : 0);
                return true;
            case 103:
                data.enforceInterface(IActivityManager.descriptor);
                String packageName9 = data.readString();
                int userId20 = data.readInt();
                killBackgroundProcesses(packageName9, userId20);
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
                IApplicationThread app15 = ApplicationThreadNative.asInterface(b24);
                String callingPackage6 = data.readString();
                Intent intent10 = Intent.CREATOR.createFromParcel(data);
                String resolvedType8 = data.readString();
                IBinder resultTo3 = data.readStrongBinder();
                String resultWho5 = data.readString();
                int requestCode5 = data.readInt();
                int startFlags2 = data.readInt();
                ProfilerInfo profilerInfoCreateFromParcel2 = data.readInt() != 0 ? ProfilerInfo.CREATOR.createFromParcel(data) : null;
                Bundle bundleCreateFromParcel2 = data.readInt() != 0 ? Bundle.CREATOR.createFromParcel(data) : null;
                int userId21 = data.readInt();
                IActivityManager.WaitResult result4 = startActivityAndWait(app15, callingPackage6, intent10, resolvedType8, resultTo3, resultWho5, requestCode5, startFlags2, profilerInfoCreateFromParcel2, bundleCreateFromParcel2, userId21);
                reply.writeNoException();
                result4.writeToParcel(reply, 0);
                return true;
            case 106:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder token25 = data.readStrongBinder();
                boolean res24 = willActivityBeVisible(token25);
                reply.writeNoException();
                reply.writeInt(res24 ? 1 : 0);
                return true;
            case 107:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder b25 = data.readStrongBinder();
                IApplicationThread app16 = ApplicationThreadNative.asInterface(b25);
                String callingPackage7 = data.readString();
                Intent intent11 = Intent.CREATOR.createFromParcel(data);
                String resolvedType9 = data.readString();
                IBinder resultTo4 = data.readStrongBinder();
                String resultWho6 = data.readString();
                int requestCode6 = data.readInt();
                int startFlags3 = data.readInt();
                Configuration config6 = Configuration.CREATOR.createFromParcel(data);
                Bundle bundleCreateFromParcel3 = data.readInt() != 0 ? Bundle.CREATOR.createFromParcel(data) : null;
                int userId22 = data.readInt();
                int result5 = startActivityWithConfig(app16, callingPackage7, intent11, resolvedType9, resultTo4, resultWho6, requestCode6, startFlags3, config6, bundleCreateFromParcel3, userId22);
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
                IBinder app17 = data.readStrongBinder();
                int violationMask = data.readInt();
                handleApplicationStrictModeViolation(app17, violationMask, new StrictMode.ViolationInfo(data));
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
                int userId23 = data.readInt();
                String type3 = getProviderMimeType(uri5, userId23);
                reply.writeNoException();
                reply.writeString(type3);
                return true;
            case 116:
                data.enforceInterface(IActivityManager.descriptor);
                String name3 = data.readString();
                IBinder perm3 = newUriPermissionOwner(name3);
                reply.writeNoException();
                reply.writeStrongBinder(perm3);
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
                int userId24 = data.readInt();
                revokeUriPermissionFromOwner(owner2, uri7, mode5, userId24);
                reply.writeNoException();
                return true;
            case 119:
                data.enforceInterface(IActivityManager.descriptor);
                int callingUid2 = data.readInt();
                String targetPkg3 = data.readString();
                Uri uri9 = Uri.CREATOR.createFromParcel(data);
                int modeFlags = data.readInt();
                int userId25 = data.readInt();
                int res25 = checkGrantUriPermission(callingUid2, targetPkg3, uri9, modeFlags, userId25);
                reply.writeNoException();
                reply.writeInt(res25);
                return true;
            case 120:
                data.enforceInterface(IActivityManager.descriptor);
                String process2 = data.readString();
                int userId26 = data.readInt();
                boolean managed = data.readInt() != 0;
                String path = data.readString();
                ParcelFileDescriptor fd = data.readInt() != 0 ? ParcelFileDescriptor.CREATOR.createFromParcel(data) : null;
                boolean res26 = dumpHeap(process2, userId26, managed, path, fd);
                reply.writeNoException();
                reply.writeInt(res26 ? 1 : 0);
                return true;
            case 121:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder b26 = data.readStrongBinder();
                IApplicationThread app18 = ApplicationThreadNative.asInterface(b26);
                String callingPackage8 = data.readString();
                Intent[] intents = (Intent[]) data.createTypedArray(Intent.CREATOR);
                String[] resolvedTypes = data.createStringArray();
                IBinder resultTo5 = data.readStrongBinder();
                Bundle bundleCreateFromParcel4 = data.readInt() != 0 ? Bundle.CREATOR.createFromParcel(data) : null;
                int userId27 = data.readInt();
                int result6 = startActivities(app18, callingPackage8, intents, resolvedTypes, resultTo5, bundleCreateFromParcel4, userId27);
                reply.writeNoException();
                reply.writeInt(result6);
                return true;
            case 122:
                data.enforceInterface(IActivityManager.descriptor);
                int userid = data.readInt();
                int _flags = data.readInt();
                boolean result7 = isUserRunning(userid, _flags);
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
            case 131:
                data.enforceInterface(IActivityManager.descriptor);
                int taskId = data.readInt();
                setFocusedTask(taskId);
                reply.writeNoException();
                return true;
            case 132:
                data.enforceInterface(IActivityManager.descriptor);
                int taskId2 = data.readInt();
                boolean result9 = removeTask(taskId2);
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
                boolean res27 = isIntentSenderTargetedToPackage(r4);
                reply.writeNoException();
                reply.writeInt(res27 ? 1 : 0);
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
                CharSequence msg = (CharSequence) TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(data);
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
                int userId28 = data.readInt();
                IBinder token29 = data.readStrongBinder();
                IActivityManager.ContentProviderHolder cph2 = getContentProviderExternal(name4, userId28, token29);
                reply.writeNoException();
                if (cph2 != null) {
                    reply.writeInt(1);
                    cph2.writeToParcel(reply, 0);
                    return true;
                }
                reply.writeInt(0);
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
                boolean res28 = killProcessesBelowForeground(reason4);
                reply.writeNoException();
                reply.writeInt(res28 ? 1 : 0);
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
                boolean res29 = shouldUpRecreateTask(token31, destAffinity);
                reply.writeNoException();
                reply.writeInt(res29 ? 1 : 0);
                return true;
            case 147:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder token32 = data.readStrongBinder();
                Intent target = Intent.CREATOR.createFromParcel(data);
                int resultCode5 = data.readInt();
                Intent intent12 = null;
                if (data.readInt() != 0) {
                    Intent resultData4 = Intent.CREATOR.createFromParcel(data);
                    intent12 = resultData4;
                }
                boolean res30 = navigateUpTo(token32, target, resultCode5, intent12);
                reply.writeNoException();
                reply.writeInt(res30 ? 1 : 0);
                return true;
            case 148:
                data.enforceInterface(IActivityManager.descriptor);
                boolean showing = data.readInt() != 0;
                boolean occluded = data.readInt() != 0;
                setLockScreenShown(showing, occluded);
                reply.writeNoException();
                return true;
            case 149:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder token33 = data.readStrongBinder();
                boolean res31 = finishActivityAffinity(token33);
                reply.writeNoException();
                reply.writeInt(res31 ? 1 : 0);
                return true;
            case 150:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder token34 = data.readStrongBinder();
                int res32 = getLaunchedFromUid(token34);
                reply.writeNoException();
                reply.writeInt(res32);
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
                boolean res33 = isIntentSenderAnActivity(r5);
                reply.writeNoException();
                reply.writeInt(res33 ? 1 : 0);
                return true;
            case 153:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder b28 = data.readStrongBinder();
                IApplicationThread app19 = ApplicationThreadNative.asInterface(b28);
                String callingPackage9 = data.readString();
                Intent intent13 = Intent.CREATOR.createFromParcel(data);
                String resolvedType10 = data.readString();
                IBinder resultTo6 = data.readStrongBinder();
                String resultWho7 = data.readString();
                int requestCode7 = data.readInt();
                int startFlags4 = data.readInt();
                ProfilerInfo profilerInfoCreateFromParcel3 = data.readInt() != 0 ? ProfilerInfo.CREATOR.createFromParcel(data) : null;
                Bundle bundleCreateFromParcel5 = data.readInt() != 0 ? Bundle.CREATOR.createFromParcel(data) : null;
                int userId29 = data.readInt();
                int result10 = startActivityAsUser(app19, callingPackage9, intent13, resolvedType10, resultTo6, resultWho7, requestCode7, startFlags4, profilerInfoCreateFromParcel3, bundleCreateFromParcel5, userId29);
                reply.writeNoException();
                reply.writeInt(result10);
                return true;
            case 154:
                data.enforceInterface(IActivityManager.descriptor);
                int userid3 = data.readInt();
                boolean force = data.readInt() != 0;
                IStopUserCallback callback = IStopUserCallback.Stub.asInterface(data.readStrongBinder());
                int result11 = stopUser(userid3, force, callback);
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
                int bugreportType = data.readInt();
                requestBugReport(bugreportType);
                reply.writeNoException();
                return true;
            case 159:
                data.enforceInterface(IActivityManager.descriptor);
                int pid4 = data.readInt();
                boolean aboveSystem = data.readInt() != 0;
                String reason5 = data.readString();
                long res34 = inputDispatchingTimedOut(pid4, aboveSystem, reason5);
                reply.writeNoException();
                reply.writeLong(res34);
                return true;
            case 161:
                data.enforceInterface(IActivityManager.descriptor);
                IIntentSender r6 = IIntentSender.Stub.asInterface(data.readStrongBinder());
                Intent intent14 = getIntentForIntentSender(r6);
                reply.writeNoException();
                if (intent14 != null) {
                    reply.writeInt(1);
                    intent14.writeToParcel(reply, 1);
                    return true;
                }
                reply.writeInt(0);
                return true;
            case 162:
                data.enforceInterface(IActivityManager.descriptor);
                int requestType = data.readInt();
                Bundle res35 = getAssistContextExtras(requestType);
                reply.writeNoException();
                reply.writeBundle(res35);
                return true;
            case 163:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder token35 = data.readStrongBinder();
                Bundle extras = data.readBundle();
                AssistStructure structure = AssistStructure.CREATOR.createFromParcel(data);
                AssistContent content = AssistContent.CREATOR.createFromParcel(data);
                Uri referrer = data.readInt() != 0 ? Uri.CREATOR.createFromParcel(data) : null;
                reportAssistContextExtras(token35, extras, structure, content, referrer);
                reply.writeNoException();
                return true;
            case 164:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder token36 = data.readStrongBinder();
                String res36 = getLaunchedFromPackage(token36);
                reply.writeNoException();
                reply.writeString(res36);
                return true;
            case 165:
                data.enforceInterface(IActivityManager.descriptor);
                int appId2 = data.readInt();
                int userId30 = data.readInt();
                String reason6 = data.readString();
                killUid(appId2, userId30, reason6);
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
                IActivityContainer activityContainer = createVirtualActivityContainer(parentActivityToken, callback2);
                reply.writeNoException();
                if (activityContainer != null) {
                    reply.writeInt(1);
                    reply.writeStrongBinder(activityContainer.asBinder());
                    return true;
                }
                reply.writeInt(0);
                return true;
            case 169:
                data.enforceInterface(IActivityManager.descriptor);
                int taskId3 = data.readInt();
                int stackId = data.readInt();
                boolean toTop = data.readInt() != 0;
                moveTaskToStack(taskId3, stackId, toTop);
                reply.writeNoException();
                return true;
            case 170:
                data.enforceInterface(IActivityManager.descriptor);
                int stackId2 = data.readInt();
                boolean hasRect = data.readInt() != 0;
                Rect r7 = null;
                if (hasRect) {
                    Rect r8 = Rect.CREATOR.createFromParcel(data);
                    r7 = r8;
                }
                boolean allowResizeInDockedMode = data.readInt() == 1;
                boolean preserveWindows = data.readInt() == 1;
                boolean animate = data.readInt() == 1;
                int animationDuration = data.readInt();
                resizeStack(stackId2, r7, allowResizeInDockedMode, preserveWindows, animate, animationDuration);
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
                    return true;
                }
                reply.writeInt(0);
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
                ActivityOptions options6 = ActivityOptions.fromBundle(bundle2);
                boolean converted2 = convertToTranslucent(token38, options6);
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
                int userId31 = data.readInt();
                takePersistableUriPermission(uri10, mode10, userId31);
                reply.writeNoException();
                return true;
            case 181:
                data.enforceInterface(IActivityManager.descriptor);
                Uri uri11 = Uri.CREATOR.createFromParcel(data);
                int mode11 = data.readInt();
                int userId32 = data.readInt();
                releasePersistableUriPermission(uri11, mode11, userId32);
                reply.writeNoException();
                return true;
            case 182:
                data.enforceInterface(IActivityManager.descriptor);
                String packageName11 = data.readString();
                boolean incoming = data.readInt() != 0;
                ParceledListSlice<UriPermission> perms2 = getPersistedUriPermissions(packageName11, incoming);
                reply.writeNoException();
                perms2.writeToParcel(reply, 1);
                return true;
            case 183:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder b29 = data.readStrongBinder();
                appNotRespondingViaProvider(b29);
                reply.writeNoException();
                return true;
            case 184:
                data.enforceInterface(IActivityManager.descriptor);
                int taskId4 = data.readInt();
                Rect r9 = getTaskBounds(taskId4);
                reply.writeNoException();
                r9.writeToParcel(reply, 0);
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
            case 187:
                data.enforceInterface(IActivityManager.descriptor);
                String process3 = data.readString();
                int userId33 = data.readInt();
                int level = data.readInt();
                boolean res37 = setProcessMemoryTrimLevel(process3, userId33, level);
                reply.writeNoException();
                reply.writeInt(res37 ? 1 : 0);
                return true;
            case 211:
                data.enforceInterface(IActivityManager.descriptor);
                IIntentSender r10 = IIntentSender.Stub.asInterface(data.readStrongBinder());
                String prefix = data.readString();
                String tag3 = getTagForIntentSender(r10, prefix);
                reply.writeNoException();
                reply.writeString(tag3);
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
                int taskId5 = data.readInt();
                boolean isInHomeStack = isInHomeStack(taskId5);
                reply.writeNoException();
                reply.writeInt(isInHomeStack ? 1 : 0);
                return true;
            case 214:
                data.enforceInterface(IActivityManager.descriptor);
                int taskId6 = data.readInt();
                startLockTaskMode(taskId6);
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
                String callingPackage10 = data.readString();
                int callingPid2 = data.readInt();
                int callingUid3 = data.readInt();
                Intent intent15 = Intent.CREATOR.createFromParcel(data);
                String resolvedType11 = data.readString();
                IVoiceInteractionSession session = IVoiceInteractionSession.Stub.asInterface(data.readStrongBinder());
                IVoiceInteractor interactor = IVoiceInteractor.Stub.asInterface(data.readStrongBinder());
                int startFlags5 = data.readInt();
                ProfilerInfo profilerInfoCreateFromParcel4 = data.readInt() != 0 ? ProfilerInfo.CREATOR.createFromParcel(data) : null;
                Bundle bundleCreateFromParcel6 = data.readInt() != 0 ? Bundle.CREATOR.createFromParcel(data) : null;
                int userId34 = data.readInt();
                int result14 = startVoiceActivity(callingPackage10, callingPid2, callingUid3, intent15, resolvedType11, session, interactor, startFlags5, profilerInfoCreateFromParcel4, bundleCreateFromParcel6, userId34);
                reply.writeNoException();
                reply.writeInt(result14);
                return true;
            case 220:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder token43 = data.readStrongBinder();
                ActivityOptions options7 = getActivityOptions(token43);
                reply.writeNoException();
                reply.writeBundle(options7 == null ? null : options7.toBundle());
                return true;
            case 221:
                data.enforceInterface(IActivityManager.descriptor);
                String callingPackage11 = data.readString();
                List<IAppTask> list8 = getAppTasks(callingPackage11);
                reply.writeNoException();
                int N3 = list8 != null ? list8.size() : -1;
                reply.writeInt(N3);
                for (int i3 = 0; i3 < N3; i3++) {
                    IAppTask task3 = list8.get(i3);
                    reply.writeStrongBinder(task3.asBinder());
                }
                return true;
            case 222:
                data.enforceInterface(IActivityManager.descriptor);
                int taskId7 = data.readInt();
                startSystemLockTaskMode(taskId7);
                reply.writeNoException();
                return true;
            case 223:
                data.enforceInterface(IActivityManager.descriptor);
                stopSystemLockTaskMode();
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
                int taskId8 = data.readInt();
                Bundle options8 = data.readInt() == 0 ? null : Bundle.CREATOR.createFromParcel(data);
                int result15 = startActivityFromRecents(taskId8, options8);
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
                IApplicationThread app20 = ApplicationThreadNative.asInterface(b30);
                String callingPackage12 = data.readString();
                Intent intent16 = Intent.CREATOR.createFromParcel(data);
                String resolvedType12 = data.readString();
                IBinder resultTo7 = data.readStrongBinder();
                String resultWho8 = data.readString();
                int requestCode8 = data.readInt();
                int startFlags6 = data.readInt();
                ProfilerInfo profilerInfoCreateFromParcel5 = data.readInt() != 0 ? ProfilerInfo.CREATOR.createFromParcel(data) : null;
                Bundle bundleCreateFromParcel7 = data.readInt() != 0 ? Bundle.CREATOR.createFromParcel(data) : null;
                boolean ignoreTargetSecurity = data.readInt() != 0;
                int userId35 = data.readInt();
                int result16 = startActivityAsCaller(app20, callingPackage12, intent16, resolvedType12, resultTo7, resultWho8, requestCode8, startFlags6, profilerInfoCreateFromParcel5, bundleCreateFromParcel7, ignoreTargetSecurity, userId35);
                reply.writeNoException();
                reply.writeInt(result16);
                return true;
            case 234:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder activityToken2 = data.readStrongBinder();
                Intent intent17 = Intent.CREATOR.createFromParcel(data);
                ActivityManager.TaskDescription descr = ActivityManager.TaskDescription.CREATOR.createFromParcel(data);
                Bitmap thumbnail = Bitmap.CREATOR.createFromParcel(data);
                int res38 = addAppTask(activityToken2, intent17, descr, thumbnail);
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
                IApplicationThread app21 = ApplicationThreadNative.asInterface(data.readStrongBinder());
                releaseSomeActivities(app21);
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
                int userId36 = data.readInt();
                Bitmap icon = getTaskDescriptionIcon(filename, userId36);
                reply.writeNoException();
                if (icon == null) {
                    reply.writeInt(0);
                    return true;
                }
                reply.writeInt(1);
                icon.writeToParcel(reply, 0);
                return true;
            case 240:
                data.enforceInterface(IActivityManager.descriptor);
                Intent intent18 = Intent.CREATOR.createFromParcel(data);
                int requestType2 = data.readInt();
                String hint = data.readString();
                int userHandle = data.readInt();
                Bundle args = data.readBundle();
                boolean res40 = launchAssistIntent(intent18, requestType2, hint, userHandle, args);
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
                ActivityOptions options9 = ActivityOptions.fromBundle(bundle);
                startInPlaceAnimationOnFrontMostApplication(options9);
                reply.writeNoException();
                return true;
            case 242:
                data.enforceInterface(IActivityManager.descriptor);
                String perm4 = data.readString();
                int pid5 = data.readInt();
                int uid5 = data.readInt();
                IBinder token51 = data.readStrongBinder();
                int res41 = checkPermissionWithToken(perm4, pid5, uid5, token51);
                reply.writeNoException();
                reply.writeInt(res41);
                return true;
            case 243:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder token52 = data.readStrongBinder();
                registerTaskStackListener(ITaskStackListener.Stub.asInterface(token52));
                reply.writeNoException();
                return true;
            case IActivityManager.NOTIFY_CLEARTEXT_NETWORK_TRANSACTION:
                data.enforceInterface(IActivityManager.descriptor);
                int uid6 = data.readInt();
                byte[] firstPacket = data.createByteArray();
                notifyCleartextNetwork(uid6, firstPacket);
                reply.writeNoException();
                return true;
            case IActivityManager.CREATE_STACK_ON_DISPLAY:
                data.enforceInterface(IActivityManager.descriptor);
                int displayId2 = data.readInt();
                IActivityContainer activityContainer2 = createStackOnDisplay(displayId2);
                reply.writeNoException();
                if (activityContainer2 != null) {
                    reply.writeInt(1);
                    reply.writeStrongBinder(activityContainer2.asBinder());
                    return true;
                }
                reply.writeInt(0);
                return true;
            case IActivityManager.GET_FOCUSED_STACK_ID_TRANSACTION:
                data.enforceInterface(IActivityManager.descriptor);
                int focusedStackId = getFocusedStackId();
                reply.writeNoException();
                reply.writeInt(focusedStackId);
                return true;
            case IActivityManager.SET_TASK_RESIZEABLE_TRANSACTION:
                data.enforceInterface(IActivityManager.descriptor);
                int taskId9 = data.readInt();
                int resizeableMode = data.readInt();
                setTaskResizeable(taskId9, resizeableMode);
                reply.writeNoException();
                return true;
            case IActivityManager.REQUEST_ASSIST_CONTEXT_EXTRAS_TRANSACTION:
                data.enforceInterface(IActivityManager.descriptor);
                int requestType3 = data.readInt();
                IResultReceiver receiver = IResultReceiver.Stub.asInterface(data.readStrongBinder());
                Bundle receiverExtras = data.readBundle();
                IBinder activityToken3 = data.readStrongBinder();
                boolean focused = data.readInt() == 1;
                boolean newSessionId = data.readInt() == 1;
                boolean res42 = requestAssistContextExtras(requestType3, receiver, receiverExtras, activityToken3, focused, newSessionId);
                reply.writeNoException();
                reply.writeInt(res42 ? 1 : 0);
                return true;
            case IActivityManager.RESIZE_TASK_TRANSACTION:
                data.enforceInterface(IActivityManager.descriptor);
                int taskId10 = data.readInt();
                int resizeMode = data.readInt();
                Rect r11 = Rect.CREATOR.createFromParcel(data);
                resizeTask(taskId10, r11, resizeMode);
                reply.writeNoException();
                return true;
            case IActivityManager.GET_LOCK_TASK_MODE_STATE_TRANSACTION:
                data.enforceInterface(IActivityManager.descriptor);
                int lockTaskModeState = getLockTaskModeState();
                reply.writeNoException();
                reply.writeInt(lockTaskModeState);
                return true;
            case IActivityManager.SET_DUMP_HEAP_DEBUG_LIMIT_TRANSACTION:
                data.enforceInterface(IActivityManager.descriptor);
                String procName = data.readString();
                int uid7 = data.readInt();
                long maxMemSize = data.readLong();
                String reportPackage = data.readString();
                setDumpHeapDebugLimit(procName, uid7, maxMemSize, reportPackage);
                reply.writeNoException();
                return true;
            case IActivityManager.DUMP_HEAP_FINISHED_TRANSACTION:
                data.enforceInterface(IActivityManager.descriptor);
                String path2 = data.readString();
                dumpHeapFinished(path2);
                reply.writeNoException();
                return true;
            case IActivityManager.SET_VOICE_KEEP_AWAKE_TRANSACTION:
                data.enforceInterface(IActivityManager.descriptor);
                IVoiceInteractionSession session3 = IVoiceInteractionSession.Stub.asInterface(data.readStrongBinder());
                boolean keepAwake = data.readInt() != 0;
                setVoiceKeepAwake(session3, keepAwake);
                reply.writeNoException();
                return true;
            case IActivityManager.UPDATE_LOCK_TASK_PACKAGES_TRANSACTION:
                data.enforceInterface(IActivityManager.descriptor);
                int userId37 = data.readInt();
                String[] packages = data.readStringArray();
                updateLockTaskPackages(userId37, packages);
                reply.writeNoException();
                return true;
            case IActivityManager.NOTE_ALARM_START_TRANSACTION:
                data.enforceInterface(IActivityManager.descriptor);
                IIntentSender is2 = IIntentSender.Stub.asInterface(data.readStrongBinder());
                int sourceUid2 = data.readInt();
                String tag4 = data.readString();
                noteAlarmStart(is2, sourceUid2, tag4);
                reply.writeNoException();
                return true;
            case IActivityManager.NOTE_ALARM_FINISH_TRANSACTION:
                data.enforceInterface(IActivityManager.descriptor);
                IIntentSender is3 = IIntentSender.Stub.asInterface(data.readStrongBinder());
                int sourceUid3 = data.readInt();
                String tag5 = data.readString();
                noteAlarmFinish(is3, sourceUid3, tag5);
                reply.writeNoException();
                return true;
            case IActivityManager.GET_PACKAGE_PROCESS_STATE_TRANSACTION:
                data.enforceInterface(IActivityManager.descriptor);
                String pkg6 = data.readString();
                String callingPackage13 = data.readString();
                int res43 = getPackageProcessState(pkg6, callingPackage13);
                reply.writeNoException();
                reply.writeInt(res43);
                return true;
            case IActivityManager.SHOW_LOCK_TASK_ESCAPE_MESSAGE_TRANSACTION:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder token53 = data.readStrongBinder();
                showLockTaskEscapeMessage(token53);
                reply.writeNoException();
                return true;
            case IActivityManager.UPDATE_DEVICE_OWNER_TRANSACTION:
                data.enforceInterface(IActivityManager.descriptor);
                String packageName12 = data.readString();
                updateDeviceOwner(packageName12);
                reply.writeNoException();
                return true;
            case IActivityManager.KEYGUARD_GOING_AWAY_TRANSACTION:
                data.enforceInterface(IActivityManager.descriptor);
                keyguardGoingAway(data.readInt());
                reply.writeNoException();
                return true;
            case IActivityManager.REGISTER_UID_OBSERVER_TRANSACTION:
                data.enforceInterface(IActivityManager.descriptor);
                IUidObserver observer6 = IUidObserver.Stub.asInterface(data.readStrongBinder());
                int which = data.readInt();
                registerUidObserver(observer6, which);
                return true;
            case IActivityManager.UNREGISTER_UID_OBSERVER_TRANSACTION:
                data.enforceInterface(IActivityManager.descriptor);
                IUidObserver observer7 = IUidObserver.Stub.asInterface(data.readStrongBinder());
                unregisterUidObserver(observer7);
                return true;
            case 300:
                data.enforceInterface(IActivityManager.descriptor);
                boolean res44 = isAssistDataAllowedOnCurrentActivity();
                reply.writeNoException();
                reply.writeInt(res44 ? 1 : 0);
                return true;
            case 301:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder token54 = data.readStrongBinder();
                Bundle args2 = data.readBundle();
                boolean res45 = showAssistFromActivity(token54, args2);
                reply.writeNoException();
                reply.writeInt(res45 ? 1 : 0);
                return true;
            case 302:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder token55 = data.readStrongBinder();
                boolean res46 = isRootVoiceInteraction(token55);
                reply.writeNoException();
                reply.writeInt(res46 ? 1 : 0);
                return true;
            case IActivityManager.START_BINDER_TRACKING_TRANSACTION:
                data.enforceInterface(IActivityManager.descriptor);
                boolean res47 = startBinderTracking();
                reply.writeNoException();
                reply.writeInt(res47 ? 1 : 0);
                return true;
            case IActivityManager.STOP_BINDER_TRACKING_AND_DUMP_TRANSACTION:
                data.enforceInterface(IActivityManager.descriptor);
                ParcelFileDescriptor fd2 = data.readInt() != 0 ? ParcelFileDescriptor.CREATOR.createFromParcel(data) : null;
                boolean res48 = stopBinderTrackingAndDump(fd2);
                reply.writeNoException();
                reply.writeInt(res48 ? 1 : 0);
                return true;
            case IActivityManager.POSITION_TASK_IN_STACK_TRANSACTION:
                data.enforceInterface(IActivityManager.descriptor);
                int taskId11 = data.readInt();
                int stackId5 = data.readInt();
                int position = data.readInt();
                positionTaskInStack(taskId11, stackId5, position);
                reply.writeNoException();
                return true;
            case IActivityManager.GET_ACTIVITY_STACK_ID_TRANSACTION:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder token56 = data.readStrongBinder();
                int stackId6 = getActivityStackId(token56);
                reply.writeNoException();
                reply.writeInt(stackId6);
                return true;
            case IActivityManager.EXIT_FREEFORM_MODE_TRANSACTION:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder token57 = data.readStrongBinder();
                exitFreeformMode(token57);
                reply.writeNoException();
                return true;
            case IActivityManager.REPORT_SIZE_CONFIGURATIONS:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder token58 = data.readStrongBinder();
                int[] horizontal = readIntArray(data);
                int[] vertical = readIntArray(data);
                int[] smallest = readIntArray(data);
                reportSizeConfigurations(token58, horizontal, vertical, smallest);
                return true;
            case IActivityManager.MOVE_TASK_TO_DOCKED_STACK_TRANSACTION:
                data.enforceInterface(IActivityManager.descriptor);
                int taskId12 = data.readInt();
                int createMode = data.readInt();
                boolean toTop2 = data.readInt() != 0;
                boolean animate2 = data.readInt() != 0;
                Rect bounds = null;
                boolean hasBounds = data.readInt() != 0;
                if (hasBounds) {
                    Rect bounds2 = Rect.CREATOR.createFromParcel(data);
                    bounds = bounds2;
                }
                boolean moveHomeStackFront = data.readInt() != 0;
                boolean res49 = moveTaskToDockedStack(taskId12, createMode, toTop2, animate2, bounds, moveHomeStackFront);
                reply.writeNoException();
                reply.writeInt(res49 ? 1 : 0);
                return true;
            case IActivityManager.SUPPRESS_RESIZE_CONFIG_CHANGES_TRANSACTION:
                data.enforceInterface(IActivityManager.descriptor);
                boolean suppress = data.readInt() == 1;
                suppressResizeConfigChanges(suppress);
                reply.writeNoException();
                return true;
            case IActivityManager.MOVE_TASKS_TO_FULLSCREEN_STACK_TRANSACTION:
                data.enforceInterface(IActivityManager.descriptor);
                int stackId7 = data.readInt();
                boolean onTop = data.readInt() == 1;
                moveTasksToFullscreenStack(stackId7, onTop);
                reply.writeNoException();
                return true;
            case IActivityManager.MOVE_TOP_ACTIVITY_TO_PINNED_STACK_TRANSACTION:
                data.enforceInterface(IActivityManager.descriptor);
                int stackId8 = data.readInt();
                Rect r12 = Rect.CREATOR.createFromParcel(data);
                boolean res50 = moveTopActivityToPinnedStack(stackId8, r12);
                reply.writeNoException();
                reply.writeInt(res50 ? 1 : 0);
                return true;
            case IActivityManager.GET_APP_START_MODE_TRANSACTION:
                data.enforceInterface(IActivityManager.descriptor);
                int uid8 = data.readInt();
                String pkg7 = data.readString();
                int res51 = getAppStartMode(uid8, pkg7);
                reply.writeNoException();
                reply.writeInt(res51);
                return true;
            case IActivityManager.UNLOCK_USER_TRANSACTION:
                data.enforceInterface(IActivityManager.descriptor);
                int userId38 = data.readInt();
                byte[] token59 = data.createByteArray();
                byte[] secret = data.createByteArray();
                IProgressListener listener = IProgressListener.Stub.asInterface(data.readStrongBinder());
                boolean result17 = unlockUser(userId38, token59, secret, listener);
                reply.writeNoException();
                reply.writeInt(result17 ? 1 : 0);
                return true;
            case IActivityManager.IN_MULTI_WINDOW_TRANSACTION:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder token60 = data.readStrongBinder();
                boolean inMultiWindow = isInMultiWindowMode(token60);
                reply.writeNoException();
                reply.writeInt(inMultiWindow ? 1 : 0);
                return true;
            case IActivityManager.IN_PICTURE_IN_PICTURE_TRANSACTION:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder token61 = data.readStrongBinder();
                boolean inPip = isInPictureInPictureMode(token61);
                reply.writeNoException();
                reply.writeInt(inPip ? 1 : 0);
                return true;
            case IActivityManager.KILL_PACKAGE_DEPENDENTS_TRANSACTION:
                data.enforceInterface(IActivityManager.descriptor);
                String packageName13 = data.readString();
                int userId39 = data.readInt();
                killPackageDependents(packageName13, userId39);
                reply.writeNoException();
                return true;
            case IActivityManager.ENTER_PICTURE_IN_PICTURE_TRANSACTION:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder token62 = data.readStrongBinder();
                enterPictureInPictureMode(token62);
                reply.writeNoException();
                return true;
            case IActivityManager.ACTIVITY_RELAUNCHED_TRANSACTION:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder token63 = data.readStrongBinder();
                activityRelaunched(token63);
                reply.writeNoException();
                return true;
            case IActivityManager.GET_URI_PERMISSION_OWNER_FOR_ACTIVITY_TRANSACTION:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder activityToken4 = data.readStrongBinder();
                IBinder perm5 = getUriPermissionOwnerForActivity(activityToken4);
                reply.writeNoException();
                reply.writeStrongBinder(perm5);
                return true;
            case IActivityManager.RESIZE_DOCKED_STACK_TRANSACTION:
                data.enforceInterface(IActivityManager.descriptor);
                boolean hasBounds2 = data.readInt() != 0;
                Rect bounds3 = null;
                if (hasBounds2) {
                    Rect bounds4 = Rect.CREATOR.createFromParcel(data);
                    bounds3 = bounds4;
                }
                boolean hasTempDockedTaskBounds = data.readInt() != 0;
                Rect tempDockedTaskBounds = null;
                if (hasTempDockedTaskBounds) {
                    Rect tempDockedTaskBounds2 = Rect.CREATOR.createFromParcel(data);
                    tempDockedTaskBounds = tempDockedTaskBounds2;
                }
                boolean hasTempDockedTaskInsetBounds = data.readInt() != 0;
                Rect tempDockedTaskInsetBounds = null;
                if (hasTempDockedTaskInsetBounds) {
                    Rect tempDockedTaskInsetBounds2 = Rect.CREATOR.createFromParcel(data);
                    tempDockedTaskInsetBounds = tempDockedTaskInsetBounds2;
                }
                boolean hasTempOtherTaskBounds = data.readInt() != 0;
                Rect tempOtherTaskBounds = null;
                if (hasTempOtherTaskBounds) {
                    Rect tempOtherTaskBounds2 = Rect.CREATOR.createFromParcel(data);
                    tempOtherTaskBounds = tempOtherTaskBounds2;
                }
                boolean hasTempOtherTaskInsetBounds = data.readInt() != 0;
                Rect tempOtherTaskInsetBounds = null;
                if (hasTempOtherTaskInsetBounds) {
                    Rect tempOtherTaskInsetBounds2 = Rect.CREATOR.createFromParcel(data);
                    tempOtherTaskInsetBounds = tempOtherTaskInsetBounds2;
                }
                resizeDockedStack(bounds3, tempDockedTaskBounds, tempDockedTaskInsetBounds, tempOtherTaskBounds, tempOtherTaskInsetBounds);
                reply.writeNoException();
                return true;
            case IActivityManager.SET_VR_MODE_TRANSACTION:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder token64 = data.readStrongBinder();
                boolean enable2 = data.readInt() == 1;
                ComponentName packageName14 = ComponentName.CREATOR.createFromParcel(data);
                int res52 = setVrMode(token64, enable2, packageName14);
                reply.writeNoException();
                reply.writeInt(res52);
                return true;
            case IActivityManager.GET_GRANTED_URI_PERMISSIONS_TRANSACTION:
                data.enforceInterface(IActivityManager.descriptor);
                String packageName15 = data.readString();
                int userId40 = data.readInt();
                ParceledListSlice<UriPermission> perms3 = getGrantedUriPermissions(packageName15, userId40);
                reply.writeNoException();
                perms3.writeToParcel(reply, 1);
                return true;
            case IActivityManager.CLEAR_GRANTED_URI_PERMISSIONS_TRANSACTION:
                data.enforceInterface(IActivityManager.descriptor);
                String packageName16 = data.readString();
                int userId41 = data.readInt();
                clearGrantedUriPermissions(packageName16, userId41);
                reply.writeNoException();
                return true;
            case IActivityManager.IS_APP_FOREGROUND_TRANSACTION:
                data.enforceInterface(IActivityManager.descriptor);
                int userHandle2 = data.readInt();
                boolean isForeground2 = isAppForeground(userHandle2);
                reply.writeNoException();
                reply.writeInt(isForeground2 ? 1 : 0);
                return true;
            case IActivityManager.START_LOCAL_VOICE_INTERACTION_TRANSACTION:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder token65 = data.readStrongBinder();
                Bundle options10 = data.readBundle();
                startLocalVoiceInteraction(token65, options10);
                reply.writeNoException();
                return true;
            case IActivityManager.STOP_LOCAL_VOICE_INTERACTION_TRANSACTION:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder token66 = data.readStrongBinder();
                stopLocalVoiceInteraction(token66);
                reply.writeNoException();
                return true;
            case IActivityManager.SUPPORTS_LOCAL_VOICE_INTERACTION_TRANSACTION:
                data.enforceInterface(IActivityManager.descriptor);
                boolean result18 = supportsLocalVoiceInteraction();
                reply.writeNoException();
                reply.writeInt(result18 ? 1 : 0);
                return true;
            case IActivityManager.NOTIFY_PINNED_STACK_ANIMATION_ENDED_TRANSACTION:
                data.enforceInterface(IActivityManager.descriptor);
                reply.writeNoException();
                return true;
            case IActivityManager.REMOVE_STACK:
                data.enforceInterface(IActivityManager.descriptor);
                int stackId9 = data.readInt();
                removeStack(stackId9);
                reply.writeNoException();
                return true;
            case IActivityManager.SET_LENIENT_BACKGROUND_CHECK_TRANSACTION:
                data.enforceInterface(IActivityManager.descriptor);
                boolean enabled3 = data.readInt() != 0;
                setLenientBackgroundCheck(enabled3);
                reply.writeNoException();
                return true;
            case IActivityManager.GET_MEMORY_TRIM_LEVEL_TRANSACTION:
                data.enforceInterface(IActivityManager.descriptor);
                int level2 = getMemoryTrimLevel();
                reply.writeNoException();
                reply.writeInt(level2);
                return true;
            case IActivityManager.RESIZE_PINNED_STACK_TRANSACTION:
                data.enforceInterface(IActivityManager.descriptor);
                boolean hasBounds3 = data.readInt() != 0;
                Rect bounds5 = null;
                if (hasBounds3) {
                    Rect bounds6 = Rect.CREATOR.createFromParcel(data);
                    bounds5 = bounds6;
                }
                boolean hasTempPinnedTaskBounds = data.readInt() != 0;
                Rect rect = null;
                if (hasTempPinnedTaskBounds) {
                    Rect tempPinnedTaskBounds = Rect.CREATOR.createFromParcel(data);
                    rect = tempPinnedTaskBounds;
                }
                resizePinnedStack(bounds5, rect);
                return true;
            case IActivityManager.IS_VR_PACKAGE_ENABLED_TRANSACTION:
                data.enforceInterface(IActivityManager.descriptor);
                ComponentName packageName17 = ComponentName.CREATOR.createFromParcel(data);
                boolean res53 = isVrModePackageEnabled(packageName17);
                reply.writeNoException();
                reply.writeInt(res53 ? 1 : 0);
                return true;
            case IActivityManager.SWAP_DOCKED_AND_FULLSCREEN_STACK:
                data.enforceInterface(IActivityManager.descriptor);
                swapDockedAndFullscreenStack();
                reply.writeNoException();
                return true;
            case IActivityManager.NOTIFY_LOCKED_PROFILE:
                data.enforceInterface(IActivityManager.descriptor);
                int userId42 = data.readInt();
                notifyLockedProfile(userId42);
                reply.writeNoException();
                return true;
            case IActivityManager.START_CONFIRM_DEVICE_CREDENTIAL_INTENT:
                data.enforceInterface(IActivityManager.descriptor);
                Intent intent19 = Intent.CREATOR.createFromParcel(data);
                startConfirmDeviceCredentialIntent(intent19);
                reply.writeNoException();
                return true;
            case IActivityManager.SEND_IDLE_JOB_TRIGGER_TRANSACTION:
                data.enforceInterface(IActivityManager.descriptor);
                sendIdleJobTrigger();
                reply.writeNoException();
                return true;
            case IActivityManager.SEND_INTENT_SENDER_TRANSACTION:
                data.enforceInterface(IActivityManager.descriptor);
                IIntentSender sender = IIntentSender.Stub.asInterface(data.readStrongBinder());
                int scode = data.readInt();
                Intent intentCreateFromParcel = data.readInt() != 0 ? Intent.CREATOR.createFromParcel(data) : null;
                String resolvedType13 = data.readString();
                IIntentReceiver finishedReceiver = IIntentReceiver.Stub.asInterface(data.readStrongBinder());
                String requiredPermission = data.readString();
                Bundle options11 = data.readInt() != 0 ? Bundle.CREATOR.createFromParcel(data) : null;
                int result19 = sendIntentSender(sender, scode, intentCreateFromParcel, resolvedType13, finishedReceiver, requiredPermission, options11);
                reply.writeNoException();
                reply.writeInt(result19);
                return true;
            case 406:
                data.enforceInterface(IActivityManager.descriptor);
                int[] pids4 = data.createIntArray();
                long[] pss2 = getProcessPswap(pids4);
                reply.writeNoException();
                reply.writeLongArray(pss2);
                return true;
            case 501:
                data.enforceInterface(IActivityManager.descriptor);
                String packageName18 = data.readString();
                int userId43 = data.readInt();
                String reason7 = data.readString();
                forceKillPackage(packageName18, userId43, reason7);
                reply.writeNoException();
                return true;
            case 502:
                data.enforceInterface(IActivityManager.descriptor);
                ComponentName className4 = ComponentName.readFromParcel(data);
                setWallpaperProcess(className4);
                reply.writeNoException();
                return true;
            case 503:
                data.enforceInterface(IActivityManager.descriptor);
                boolean isForeground3 = data.readInt() != 0;
                updateWallpaperState(isForeground3);
                reply.writeNoException();
                return true;
            case 504:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder token67 = data.readStrongBinder();
                boolean isSticky = data.readInt() != 0;
                stickWindow(token67, isSticky);
                reply.writeNoException();
                return true;
            case IActivityManager.GET_STICKY_WINDOW_TRANSACTION:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder token68 = data.readStrongBinder();
                boolean isSticky2 = isStickyByMtk(token68);
                reply.writeNoException();
                reply.writeInt(isSticky2 ? 1 : 0);
                return true;
            case IActivityManager.RESTORE_WINDOW_TRANSACTION:
                data.enforceInterface(IActivityManager.descriptor);
                restoreWindow();
                reply.writeNoException();
                return true;
            case IActivityManager.GET_INFO_PACKAGE_LIST_FROM_PID_TRANSACTION:
                data.enforceInterface(IActivityManager.descriptor);
                int pid6 = data.readInt();
                String[] result20 = getPackageListFromPid(pid6);
                reply.writeNoException();
                reply.writeStringArray(result20);
                return true;
            case IActivityManager.GET_INFO_PROCESSES_WITH_ADJ_TRANSACTION:
                data.enforceInterface(IActivityManager.descriptor);
                ArrayMap<Integer, ArrayList<Integer>> result21 = getProcessesWithAdj();
                reply.writeNoException();
                int size2 = result21.size();
                reply.writeInt(size2);
                for (Map.Entry<Integer, ArrayList<Integer>> e : result21.entrySet()) {
                    reply.writeInt(e.getKey().intValue());
                    ArrayList<Integer> entryList = e.getValue();
                    int listSize = entryList.size();
                    reply.writeInt(listSize);
                    int i4 = 0;
                    while (true) {
                        int j = i4;
                        if (j < listSize) {
                            reply.writeInt(entryList.get(j).intValue());
                            i4 = j + 1;
                        }
                    }
                }
                return true;
            case IActivityManager.READY_TO_GET_CONTENT_PROVIDER_TRANSACTION:
                data.enforceInterface(IActivityManager.descriptor);
                IBinder b31 = data.readStrongBinder();
                IApplicationThread app22 = ApplicationThreadNative.asInterface(b31);
                String name6 = data.readString();
                int userId44 = data.readInt();
                int result22 = readyToGetContentProvider(app22, name6, userId44);
                reply.writeNoException();
                reply.writeInt(result22);
                return true;
            case IActivityManager.AAL_SET_AAL_MODE:
                data.enforceInterface(IActivityManager.descriptor);
                int mode12 = data.readInt();
                setAalMode(mode12);
                reply.writeNoException();
                return true;
            case IActivityManager.AAL_SET_AAL_ENABLED:
                data.enforceInterface(IActivityManager.descriptor);
                boolean enabled4 = data.readInt() != 0;
                setAalEnabled(enabled4);
                reply.writeNoException();
                return true;
        }
    }

    private int[] readIntArray(Parcel data) {
        int smallestSize = data.readInt();
        if (smallestSize <= 0) {
            return null;
        }
        int[] smallest = new int[smallestSize];
        data.readIntArray(smallest);
        return smallest;
    }

    @Override
    public IBinder asBinder() {
        return this;
    }
}
