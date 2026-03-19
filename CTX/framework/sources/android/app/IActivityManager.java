package android.app;

import android.app.ActivityManager;
import android.app.ApplicationErrorReport;
import android.app.IActivityController;
import android.app.IApplicationThread;
import android.app.IAssistDataReceiver;
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
import android.content.ComponentName;
import android.content.IIntentReceiver;
import android.content.IIntentSender;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.os.IInterface;
import android.os.IProgressListener;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.os.StrictMode;
import android.os.WorkSource;
import android.service.voice.IVoiceInteractionSession;
import android.text.TextUtils;
import android.view.IRecentsAnimationRunner;
import android.view.RemoteAnimationAdapter;
import android.view.RemoteAnimationDefinition;
import com.android.internal.app.IVoiceInteractor;
import com.android.internal.os.IResultReceiver;
import com.android.internal.policy.IKeyguardDismissCallback;
import java.util.List;

public interface IActivityManager extends IInterface {
    void activityDestroyed(IBinder iBinder) throws RemoteException;

    void activityIdle(IBinder iBinder, Configuration configuration, boolean z) throws RemoteException;

    void activityPaused(IBinder iBinder) throws RemoteException;

    void activityRelaunched(IBinder iBinder) throws RemoteException;

    void activityResumed(IBinder iBinder) throws RemoteException;

    void activitySlept(IBinder iBinder) throws RemoteException;

    void activityStopped(IBinder iBinder, Bundle bundle, PersistableBundle persistableBundle, CharSequence charSequence) throws RemoteException;

    int addAppTask(IBinder iBinder, Intent intent, ActivityManager.TaskDescription taskDescription, Bitmap bitmap) throws RemoteException;

    void addInstrumentationResults(IApplicationThread iApplicationThread, Bundle bundle) throws RemoteException;

    void addPackageDependency(String str) throws RemoteException;

    void alwaysShowUnsupportedCompileSdkWarning(ComponentName componentName) throws RemoteException;

    void appNotRespondingViaProvider(IBinder iBinder) throws RemoteException;

    void attachApplication(IApplicationThread iApplicationThread, long j) throws RemoteException;

    void backgroundWhitelistUid(int i) throws RemoteException;

    void backupAgentCreated(String str, IBinder iBinder) throws RemoteException;

    boolean bindBackupAgent(String str, int i, int i2) throws RemoteException;

    int bindService(IApplicationThread iApplicationThread, IBinder iBinder, Intent intent, String str, IServiceConnection iServiceConnection, int i, String str2, int i2) throws RemoteException;

    void bootAnimationComplete() throws RemoteException;

    int broadcastIntent(IApplicationThread iApplicationThread, Intent intent, String str, IIntentReceiver iIntentReceiver, int i, String str2, Bundle bundle, String[] strArr, int i2, Bundle bundle2, boolean z, boolean z2, int i3) throws RemoteException;

    void cancelIntentSender(IIntentSender iIntentSender) throws RemoteException;

    void cancelRecentsAnimation(boolean z) throws RemoteException;

    void cancelTaskWindowTransition(int i) throws RemoteException;

    int checkGrantUriPermission(int i, String str, Uri uri, int i2, int i3) throws RemoteException;

    int checkPermission(String str, int i, int i2) throws RemoteException;

    int checkPermissionWithToken(String str, int i, int i2, IBinder iBinder) throws RemoteException;

    int checkUriPermission(Uri uri, int i, int i2, int i3, int i4, IBinder iBinder) throws RemoteException;

    boolean clearApplicationUserData(String str, boolean z, IPackageDataObserver iPackageDataObserver, int i) throws RemoteException;

    void clearGrantedUriPermissions(String str, int i) throws RemoteException;

    void clearPendingBackup() throws RemoteException;

    void closeSystemDialogs(String str) throws RemoteException;

    boolean convertFromTranslucent(IBinder iBinder) throws RemoteException;

    boolean convertToTranslucent(IBinder iBinder, Bundle bundle) throws RemoteException;

    void crashApplication(int i, int i2, String str, int i3, String str2, boolean z) throws RemoteException;

    int createStackOnDisplay(int i) throws RemoteException;

    void dismissKeyguard(IBinder iBinder, IKeyguardDismissCallback iKeyguardDismissCallback, CharSequence charSequence) throws RemoteException;

    void dismissPip(boolean z, int i) throws RemoteException;

    void dismissSplitScreenMode(boolean z) throws RemoteException;

    boolean dumpHeap(String str, int i, boolean z, boolean z2, boolean z3, String str2, ParcelFileDescriptor parcelFileDescriptor) throws RemoteException;

    void dumpHeapFinished(String str) throws RemoteException;

    boolean enterPictureInPictureMode(IBinder iBinder, PictureInPictureParams pictureInPictureParams) throws RemoteException;

    void enterSafeMode() throws RemoteException;

    void exitFreeformMode(IBinder iBinder) throws RemoteException;

    boolean finishActivity(IBinder iBinder, int i, Intent intent, int i2) throws RemoteException;

    boolean finishActivityAffinity(IBinder iBinder) throws RemoteException;

    void finishHeavyWeightApp() throws RemoteException;

    void finishInstrumentation(IApplicationThread iApplicationThread, int i, Bundle bundle) throws RemoteException;

    void finishReceiver(IBinder iBinder, int i, String str, Bundle bundle, boolean z, int i2) throws RemoteException;

    void finishSubActivity(IBinder iBinder, String str, int i) throws RemoteException;

    void finishVoiceTask(IVoiceInteractionSession iVoiceInteractionSession) throws RemoteException;

    void forceStopPackage(String str, int i) throws RemoteException;

    ComponentName getActivityClassForToken(IBinder iBinder) throws RemoteException;

    int getActivityDisplayId(IBinder iBinder) throws RemoteException;

    Bundle getActivityOptions(IBinder iBinder) throws RemoteException;

    List<ActivityManager.StackInfo> getAllStackInfos() throws RemoteException;

    Point getAppTaskThumbnailSize() throws RemoteException;

    List<IBinder> getAppTasks(String str) throws RemoteException;

    Bundle getAssistContextExtras(int i) throws RemoteException;

    ComponentName getCallingActivity(IBinder iBinder) throws RemoteException;

    String getCallingPackage(IBinder iBinder) throws RemoteException;

    Configuration getConfiguration() throws RemoteException;

    ContentProviderHolder getContentProvider(IApplicationThread iApplicationThread, String str, int i, boolean z) throws RemoteException;

    ContentProviderHolder getContentProviderExternal(String str, int i, IBinder iBinder) throws RemoteException;

    UserInfo getCurrentUser() throws RemoteException;

    ConfigurationInfo getDeviceConfigurationInfo() throws RemoteException;

    List<ActivityManager.RunningTaskInfo> getFilteredTasks(int i, int i2, int i3) throws RemoteException;

    ActivityManager.StackInfo getFocusedStackInfo() throws RemoteException;

    int getFrontActivityScreenCompatMode() throws RemoteException;

    ParceledListSlice getGrantedUriPermissions(String str, int i) throws RemoteException;

    Intent getIntentForIntentSender(IIntentSender iIntentSender) throws RemoteException;

    IIntentSender getIntentSender(int i, String str, IBinder iBinder, String str2, int i2, Intent[] intentArr, String[] strArr, int i3, Bundle bundle, int i4) throws RemoteException;

    int getLastResumedActivityUserId() throws RemoteException;

    String getLaunchedFromPackage(IBinder iBinder) throws RemoteException;

    int getLaunchedFromUid(IBinder iBinder) throws RemoteException;

    int getLockTaskModeState() throws RemoteException;

    int getMaxNumPictureInPictureActions(IBinder iBinder) throws RemoteException;

    void getMemoryInfo(ActivityManager.MemoryInfo memoryInfo) throws RemoteException;

    int getMemoryTrimLevel() throws RemoteException;

    void getMyMemoryState(ActivityManager.RunningAppProcessInfo runningAppProcessInfo) throws RemoteException;

    boolean getPackageAskScreenCompat(String str) throws RemoteException;

    String getPackageForIntentSender(IIntentSender iIntentSender) throws RemoteException;

    String getPackageForToken(IBinder iBinder) throws RemoteException;

    int getPackageProcessState(String str, String str2) throws RemoteException;

    int getPackageScreenCompatMode(String str) throws RemoteException;

    ParceledListSlice getPersistedUriPermissions(String str, boolean z) throws RemoteException;

    int getProcessLimit() throws RemoteException;

    Debug.MemoryInfo[] getProcessMemoryInfo(int[] iArr) throws RemoteException;

    long[] getProcessPss(int[] iArr) throws RemoteException;

    List<ActivityManager.ProcessErrorStateInfo> getProcessesInErrorState() throws RemoteException;

    String getProviderMimeType(Uri uri, int i) throws RemoteException;

    ParceledListSlice getRecentTasks(int i, int i2, int i3) throws RemoteException;

    int getRequestedOrientation(IBinder iBinder) throws RemoteException;

    List<ActivityManager.RunningAppProcessInfo> getRunningAppProcesses() throws RemoteException;

    List<ApplicationInfo> getRunningExternalApplications() throws RemoteException;

    PendingIntent getRunningServiceControlPanel(ComponentName componentName) throws RemoteException;

    int[] getRunningUserIds() throws RemoteException;

    List<ActivityManager.RunningServiceInfo> getServices(int i, int i2) throws RemoteException;

    ActivityManager.StackInfo getStackInfo(int i, int i2) throws RemoteException;

    String getTagForIntentSender(IIntentSender iIntentSender, String str) throws RemoteException;

    Rect getTaskBounds(int i) throws RemoteException;

    ActivityManager.TaskDescription getTaskDescription(int i) throws RemoteException;

    Bitmap getTaskDescriptionIcon(String str, int i) throws RemoteException;

    int getTaskForActivity(IBinder iBinder, boolean z) throws RemoteException;

    ActivityManager.TaskSnapshot getTaskSnapshot(int i, boolean z) throws RemoteException;

    List<ActivityManager.RunningTaskInfo> getTasks(int i) throws RemoteException;

    int getUidForIntentSender(IIntentSender iIntentSender) throws RemoteException;

    int getUidProcessState(int i, String str) throws RemoteException;

    IBinder getUriPermissionOwnerForActivity(IBinder iBinder) throws RemoteException;

    void grantUriPermission(IApplicationThread iApplicationThread, String str, Uri uri, int i, int i2) throws RemoteException;

    void grantUriPermissionFromOwner(IBinder iBinder, int i, String str, Uri uri, int i2, int i3, int i4) throws RemoteException;

    void handleApplicationCrash(IBinder iBinder, ApplicationErrorReport.ParcelableCrashInfo parcelableCrashInfo) throws RemoteException;

    void handleApplicationStrictModeViolation(IBinder iBinder, int i, StrictMode.ViolationInfo violationInfo) throws RemoteException;

    boolean handleApplicationWtf(IBinder iBinder, String str, boolean z, ApplicationErrorReport.ParcelableCrashInfo parcelableCrashInfo) throws RemoteException;

    int handleIncomingUser(int i, int i2, int i3, boolean z, boolean z2, String str, String str2) throws RemoteException;

    void hang(IBinder iBinder, boolean z) throws RemoteException;

    long inputDispatchingTimedOut(int i, boolean z, String str) throws RemoteException;

    boolean isAppForeground(int i) throws RemoteException;

    boolean isAppStartModeDisabled(int i, String str) throws RemoteException;

    boolean isAssistDataAllowedOnCurrentActivity() throws RemoteException;

    boolean isBackgroundRestricted(String str) throws RemoteException;

    boolean isImmersive(IBinder iBinder) throws RemoteException;

    boolean isInLockTaskMode() throws RemoteException;

    boolean isInMultiWindowMode(IBinder iBinder) throws RemoteException;

    boolean isInPictureInPictureMode(IBinder iBinder) throws RemoteException;

    boolean isIntentSenderAForegroundService(IIntentSender iIntentSender) throws RemoteException;

    boolean isIntentSenderAnActivity(IIntentSender iIntentSender) throws RemoteException;

    boolean isIntentSenderTargetedToPackage(IIntentSender iIntentSender) throws RemoteException;

    boolean isRootVoiceInteraction(IBinder iBinder) throws RemoteException;

    boolean isTopActivityImmersive() throws RemoteException;

    boolean isTopOfTask(IBinder iBinder) throws RemoteException;

    boolean isUidActive(int i, String str) throws RemoteException;

    boolean isUserAMonkey() throws RemoteException;

    boolean isUserRunning(int i, int i2) throws RemoteException;

    boolean isVrModePackageEnabled(ComponentName componentName) throws RemoteException;

    void keyguardGoingAway(int i) throws RemoteException;

    void killAllBackgroundProcesses() throws RemoteException;

    void killApplication(String str, int i, int i2, String str2) throws RemoteException;

    void killApplicationProcess(String str, int i) throws RemoteException;

    void killBackgroundProcesses(String str, int i) throws RemoteException;

    void killPackageDependents(String str, int i) throws RemoteException;

    boolean killPids(int[] iArr, String str, boolean z) throws RemoteException;

    boolean killProcessesBelowForeground(String str) throws RemoteException;

    void killUid(int i, int i2, String str) throws RemoteException;

    boolean launchAssistIntent(Intent intent, int i, String str, int i2, Bundle bundle) throws RemoteException;

    void makePackageIdle(String str, int i) throws RemoteException;

    boolean moveActivityTaskToBack(IBinder iBinder, boolean z) throws RemoteException;

    void moveStackToDisplay(int i, int i2) throws RemoteException;

    void moveTaskBackwards(int i) throws RemoteException;

    void moveTaskToFront(int i, int i2, Bundle bundle) throws RemoteException;

    void moveTaskToStack(int i, int i2, boolean z) throws RemoteException;

    void moveTasksToFullscreenStack(int i, boolean z) throws RemoteException;

    boolean moveTopActivityToPinnedStack(int i, Rect rect) throws RemoteException;

    boolean navigateUpTo(IBinder iBinder, Intent intent, int i, Intent intent2) throws RemoteException;

    IBinder newUriPermissionOwner(String str) throws RemoteException;

    void noteAlarmFinish(IIntentSender iIntentSender, WorkSource workSource, int i, String str) throws RemoteException;

    void noteAlarmStart(IIntentSender iIntentSender, WorkSource workSource, int i, String str) throws RemoteException;

    void noteWakeupAlarm(IIntentSender iIntentSender, WorkSource workSource, int i, String str, String str2) throws RemoteException;

    void notifyActivityDrawn(IBinder iBinder) throws RemoteException;

    void notifyCleartextNetwork(int i, byte[] bArr) throws RemoteException;

    void notifyEnterAnimationComplete(IBinder iBinder) throws RemoteException;

    void notifyLaunchTaskBehindComplete(IBinder iBinder) throws RemoteException;

    void notifyLockedProfile(int i) throws RemoteException;

    void notifyPinnedStackAnimationEnded() throws RemoteException;

    void notifyPinnedStackAnimationStarted() throws RemoteException;

    ParcelFileDescriptor openContentUri(String str) throws RemoteException;

    void overridePendingTransition(IBinder iBinder, String str, int i, int i2) throws RemoteException;

    IBinder peekService(Intent intent, String str, String str2) throws RemoteException;

    void performIdleMaintenance() throws RemoteException;

    void positionTaskInStack(int i, int i2, int i3) throws RemoteException;

    boolean profileControl(String str, int i, boolean z, ProfilerInfo profilerInfo, int i2) throws RemoteException;

    void publishContentProviders(IApplicationThread iApplicationThread, List<ContentProviderHolder> list) throws RemoteException;

    void publishService(IBinder iBinder, Intent intent, IBinder iBinder2) throws RemoteException;

    boolean refContentProvider(IBinder iBinder, int i, int i2) throws RemoteException;

    void registerIntentSenderCancelListener(IIntentSender iIntentSender, IResultReceiver iResultReceiver) throws RemoteException;

    void registerProcessObserver(IProcessObserver iProcessObserver) throws RemoteException;

    Intent registerReceiver(IApplicationThread iApplicationThread, String str, IIntentReceiver iIntentReceiver, IntentFilter intentFilter, String str2, int i, int i2) throws RemoteException;

    void registerRemoteAnimationForNextActivityStart(String str, RemoteAnimationAdapter remoteAnimationAdapter) throws RemoteException;

    void registerRemoteAnimations(IBinder iBinder, RemoteAnimationDefinition remoteAnimationDefinition) throws RemoteException;

    void registerTaskStackListener(ITaskStackListener iTaskStackListener) throws RemoteException;

    void registerUidObserver(IUidObserver iUidObserver, int i, int i2, String str) throws RemoteException;

    void registerUserSwitchObserver(IUserSwitchObserver iUserSwitchObserver, String str) throws RemoteException;

    boolean releaseActivityInstance(IBinder iBinder) throws RemoteException;

    void releasePersistableUriPermission(Uri uri, int i, String str, int i2) throws RemoteException;

    void releaseSomeActivities(IApplicationThread iApplicationThread) throws RemoteException;

    void removeContentProvider(IBinder iBinder, boolean z) throws RemoteException;

    void removeContentProviderExternal(String str, IBinder iBinder) throws RemoteException;

    void removeStack(int i) throws RemoteException;

    void removeStacksInWindowingModes(int[] iArr) throws RemoteException;

    void removeStacksWithActivityTypes(int[] iArr) throws RemoteException;

    boolean removeTask(int i) throws RemoteException;

    void reportActivityFullyDrawn(IBinder iBinder, boolean z) throws RemoteException;

    void reportAssistContextExtras(IBinder iBinder, Bundle bundle, AssistStructure assistStructure, AssistContent assistContent, Uri uri) throws RemoteException;

    void reportSizeConfigurations(IBinder iBinder, int[] iArr, int[] iArr2, int[] iArr3) throws RemoteException;

    boolean requestAssistContextExtras(int i, IAssistDataReceiver iAssistDataReceiver, Bundle bundle, IBinder iBinder, boolean z, boolean z2) throws RemoteException;

    boolean requestAutofillData(IAssistDataReceiver iAssistDataReceiver, Bundle bundle, IBinder iBinder, int i) throws RemoteException;

    void requestBugReport(int i) throws RemoteException;

    void requestTelephonyBugReport(String str, String str2) throws RemoteException;

    void requestWifiBugReport(String str, String str2) throws RemoteException;

    void resizeDockedStack(Rect rect, Rect rect2, Rect rect3, Rect rect4, Rect rect5) throws RemoteException;

    void resizePinnedStack(Rect rect, Rect rect2) throws RemoteException;

    void resizeStack(int i, Rect rect, boolean z, boolean z2, boolean z3, int i2) throws RemoteException;

    void resizeTask(int i, Rect rect, int i2) throws RemoteException;

    void restart() throws RemoteException;

    int restartUserInBackground(int i) throws RemoteException;

    void resumeAppSwitches() throws RemoteException;

    void revokeUriPermission(IApplicationThread iApplicationThread, String str, Uri uri, int i, int i2) throws RemoteException;

    void revokeUriPermissionFromOwner(IBinder iBinder, Uri uri, int i, int i2) throws RemoteException;

    void scheduleApplicationInfoChanged(List<String> list, int i) throws RemoteException;

    void sendIdleJobTrigger() throws RemoteException;

    int sendIntentSender(IIntentSender iIntentSender, IBinder iBinder, int i, Intent intent, String str, IIntentReceiver iIntentReceiver, String str2, Bundle bundle) throws RemoteException;

    void serviceDoneExecuting(IBinder iBinder, int i, int i2, int i3) throws RemoteException;

    void setAalEnabled(boolean z) throws RemoteException;

    void setAalMode(int i) throws RemoteException;

    void setActivityController(IActivityController iActivityController, boolean z) throws RemoteException;

    void setAgentApp(String str, String str2) throws RemoteException;

    void setAlwaysFinish(boolean z) throws RemoteException;

    void setDebugApp(String str, boolean z, boolean z2) throws RemoteException;

    void setDisablePreviewScreenshots(IBinder iBinder, boolean z) throws RemoteException;

    void setDumpHeapDebugLimit(String str, int i, long j, String str2) throws RemoteException;

    void setFocusedStack(int i) throws RemoteException;

    void setFocusedTask(int i) throws RemoteException;

    void setFrontActivityScreenCompatMode(int i) throws RemoteException;

    void setHasTopUi(boolean z) throws RemoteException;

    void setImmersive(IBinder iBinder, boolean z) throws RemoteException;

    void setLockScreenShown(boolean z, boolean z2, int i) throws RemoteException;

    void setPackageAskScreenCompat(String str, boolean z) throws RemoteException;

    void setPackageScreenCompatMode(String str, int i) throws RemoteException;

    void setPersistentVrThread(int i) throws RemoteException;

    void setPictureInPictureParams(IBinder iBinder, PictureInPictureParams pictureInPictureParams) throws RemoteException;

    void setProcessImportant(IBinder iBinder, int i, boolean z, String str) throws RemoteException;

    void setProcessLimit(int i) throws RemoteException;

    boolean setProcessMemoryTrimLevel(String str, int i, int i2) throws RemoteException;

    void setRenderThread(int i) throws RemoteException;

    void setRequestedOrientation(IBinder iBinder, int i) throws RemoteException;

    void setServiceForeground(ComponentName componentName, IBinder iBinder, int i, Notification notification, int i2) throws RemoteException;

    void setShowWhenLocked(IBinder iBinder, boolean z) throws RemoteException;

    void setSplitScreenResizing(boolean z) throws RemoteException;

    void setTaskDescription(IBinder iBinder, ActivityManager.TaskDescription taskDescription) throws RemoteException;

    void setTaskResizeable(int i, int i2) throws RemoteException;

    void setTaskWindowingMode(int i, int i2, boolean z) throws RemoteException;

    boolean setTaskWindowingModeSplitScreenPrimary(int i, int i2, boolean z, boolean z2, Rect rect, boolean z3) throws RemoteException;

    void setTurnScreenOn(IBinder iBinder, boolean z) throws RemoteException;

    void setUserIsMonkey(boolean z) throws RemoteException;

    void setVoiceKeepAwake(IVoiceInteractionSession iVoiceInteractionSession, boolean z) throws RemoteException;

    int setVrMode(IBinder iBinder, boolean z, ComponentName componentName) throws RemoteException;

    void setVrThread(int i) throws RemoteException;

    boolean shouldUpRecreateTask(IBinder iBinder, String str) throws RemoteException;

    boolean showAssistFromActivity(IBinder iBinder, Bundle bundle) throws RemoteException;

    void showBootMessage(CharSequence charSequence, boolean z) throws RemoteException;

    void showLockTaskEscapeMessage(IBinder iBinder) throws RemoteException;

    void showWaitingForDebugger(IApplicationThread iApplicationThread, boolean z) throws RemoteException;

    boolean shutdown(int i) throws RemoteException;

    void signalPersistentProcesses(int i) throws RemoteException;

    int startActivities(IApplicationThread iApplicationThread, String str, Intent[] intentArr, String[] strArr, IBinder iBinder, Bundle bundle, int i) throws RemoteException;

    int startActivity(IApplicationThread iApplicationThread, String str, Intent intent, String str2, IBinder iBinder, String str3, int i, int i2, ProfilerInfo profilerInfo, Bundle bundle) throws RemoteException;

    WaitResult startActivityAndWait(IApplicationThread iApplicationThread, String str, Intent intent, String str2, IBinder iBinder, String str3, int i, int i2, ProfilerInfo profilerInfo, Bundle bundle, int i3) throws RemoteException;

    int startActivityAsCaller(IApplicationThread iApplicationThread, String str, Intent intent, String str2, IBinder iBinder, String str3, int i, int i2, ProfilerInfo profilerInfo, Bundle bundle, boolean z, int i3) throws RemoteException;

    int startActivityAsUser(IApplicationThread iApplicationThread, String str, Intent intent, String str2, IBinder iBinder, String str3, int i, int i2, ProfilerInfo profilerInfo, Bundle bundle, int i3) throws RemoteException;

    int startActivityFromRecents(int i, Bundle bundle) throws RemoteException;

    int startActivityIntentSender(IApplicationThread iApplicationThread, IIntentSender iIntentSender, IBinder iBinder, Intent intent, String str, IBinder iBinder2, String str2, int i, int i2, int i3, Bundle bundle) throws RemoteException;

    int startActivityWithConfig(IApplicationThread iApplicationThread, String str, Intent intent, String str2, IBinder iBinder, String str3, int i, int i2, Configuration configuration, Bundle bundle, int i3) throws RemoteException;

    int startAssistantActivity(String str, int i, int i2, Intent intent, String str2, Bundle bundle, int i3) throws RemoteException;

    boolean startBinderTracking() throws RemoteException;

    void startConfirmDeviceCredentialIntent(Intent intent, Bundle bundle) throws RemoteException;

    void startInPlaceAnimationOnFrontMostApplication(Bundle bundle) throws RemoteException;

    boolean startInstrumentation(ComponentName componentName, String str, int i, Bundle bundle, IInstrumentationWatcher iInstrumentationWatcher, IUiAutomationConnection iUiAutomationConnection, int i2, String str2) throws RemoteException;

    void startLocalVoiceInteraction(IBinder iBinder, Bundle bundle) throws RemoteException;

    void startLockTaskModeByToken(IBinder iBinder) throws RemoteException;

    boolean startNextMatchingActivity(IBinder iBinder, Intent intent, Bundle bundle) throws RemoteException;

    void startRecentsActivity(Intent intent, IAssistDataReceiver iAssistDataReceiver, IRecentsAnimationRunner iRecentsAnimationRunner) throws RemoteException;

    ComponentName startService(IApplicationThread iApplicationThread, Intent intent, String str, boolean z, String str2, int i) throws RemoteException;

    void startSystemLockTaskMode(int i) throws RemoteException;

    boolean startUserInBackground(int i) throws RemoteException;

    boolean startUserInBackgroundWithListener(int i, IProgressListener iProgressListener) throws RemoteException;

    int startVoiceActivity(String str, int i, int i2, Intent intent, String str2, IVoiceInteractionSession iVoiceInteractionSession, IVoiceInteractor iVoiceInteractor, int i3, ProfilerInfo profilerInfo, Bundle bundle, int i4) throws RemoteException;

    void stopAppSwitches() throws RemoteException;

    boolean stopBinderTrackingAndDump(ParcelFileDescriptor parcelFileDescriptor) throws RemoteException;

    void stopLocalVoiceInteraction(IBinder iBinder) throws RemoteException;

    void stopLockTaskModeByToken(IBinder iBinder) throws RemoteException;

    int stopService(IApplicationThread iApplicationThread, Intent intent, String str, int i) throws RemoteException;

    boolean stopServiceToken(ComponentName componentName, IBinder iBinder, int i) throws RemoteException;

    void stopSystemLockTaskMode() throws RemoteException;

    int stopUser(int i, boolean z, IStopUserCallback iStopUserCallback) throws RemoteException;

    boolean supportsLocalVoiceInteraction() throws RemoteException;

    void suppressResizeConfigChanges(boolean z) throws RemoteException;

    boolean switchUser(int i) throws RemoteException;

    void takePersistableUriPermission(Uri uri, int i, String str, int i2) throws RemoteException;

    void unbindBackupAgent(ApplicationInfo applicationInfo) throws RemoteException;

    void unbindFinished(IBinder iBinder, Intent intent, boolean z) throws RemoteException;

    boolean unbindService(IServiceConnection iServiceConnection) throws RemoteException;

    void unbroadcastIntent(IApplicationThread iApplicationThread, Intent intent, int i) throws RemoteException;

    void unhandledBack() throws RemoteException;

    boolean unlockUser(int i, byte[] bArr, byte[] bArr2, IProgressListener iProgressListener) throws RemoteException;

    void unregisterIntentSenderCancelListener(IIntentSender iIntentSender, IResultReceiver iResultReceiver) throws RemoteException;

    void unregisterProcessObserver(IProcessObserver iProcessObserver) throws RemoteException;

    void unregisterReceiver(IIntentReceiver iIntentReceiver) throws RemoteException;

    void unregisterTaskStackListener(ITaskStackListener iTaskStackListener) throws RemoteException;

    void unregisterUidObserver(IUidObserver iUidObserver) throws RemoteException;

    void unregisterUserSwitchObserver(IUserSwitchObserver iUserSwitchObserver) throws RemoteException;

    void unstableProviderDied(IBinder iBinder) throws RemoteException;

    boolean updateConfiguration(Configuration configuration) throws RemoteException;

    void updateDeviceOwner(String str) throws RemoteException;

    boolean updateDisplayOverrideConfiguration(Configuration configuration, int i) throws RemoteException;

    void updateLockTaskFeatures(int i, int i2) throws RemoteException;

    void updateLockTaskPackages(int i, String[] strArr) throws RemoteException;

    void updatePersistentConfiguration(Configuration configuration) throws RemoteException;

    void waitForNetworkStateUpdate(long j) throws RemoteException;

    boolean willActivityBeVisible(IBinder iBinder) throws RemoteException;

    public static abstract class Stub extends Binder implements IActivityManager {
        private static final String DESCRIPTOR = "android.app.IActivityManager";
        static final int TRANSACTION_activityDestroyed = 58;
        static final int TRANSACTION_activityIdle = 15;
        static final int TRANSACTION_activityPaused = 16;
        static final int TRANSACTION_activityRelaunched = 258;
        static final int TRANSACTION_activityResumed = 35;
        static final int TRANSACTION_activitySlept = 121;
        static final int TRANSACTION_activityStopped = 17;
        static final int TRANSACTION_addAppTask = 208;
        static final int TRANSACTION_addInstrumentationResults = 40;
        static final int TRANSACTION_addPackageDependency = 93;
        static final int TRANSACTION_alwaysShowUnsupportedCompileSdkWarning = 305;
        static final int TRANSACTION_appNotRespondingViaProvider = 184;
        static final int TRANSACTION_attachApplication = 14;
        static final int TRANSACTION_backgroundWhitelistUid = 298;
        static final int TRANSACTION_backupAgentCreated = 89;
        static final int TRANSACTION_bindBackupAgent = 88;
        static final int TRANSACTION_bindService = 32;
        static final int TRANSACTION_bootAnimationComplete = 212;
        static final int TRANSACTION_broadcastIntent = 11;
        static final int TRANSACTION_cancelIntentSender = 60;
        static final int TRANSACTION_cancelRecentsAnimation = 197;
        static final int TRANSACTION_cancelTaskWindowTransition = 291;
        static final int TRANSACTION_checkGrantUriPermission = 117;
        static final int TRANSACTION_checkPermission = 49;
        static final int TRANSACTION_checkPermissionWithToken = 216;
        static final int TRANSACTION_checkUriPermission = 50;
        static final int TRANSACTION_clearApplicationUserData = 76;
        static final int TRANSACTION_clearGrantedUriPermissions = 264;
        static final int TRANSACTION_clearPendingBackup = 160;
        static final int TRANSACTION_closeSystemDialogs = 95;
        static final int TRANSACTION_convertFromTranslucent = 175;
        static final int TRANSACTION_convertToTranslucent = 176;
        static final int TRANSACTION_crashApplication = 112;
        static final int TRANSACTION_createStackOnDisplay = 220;
        static final int TRANSACTION_dismissKeyguard = 289;
        static final int TRANSACTION_dismissPip = 246;
        static final int TRANSACTION_dismissSplitScreenMode = 245;
        static final int TRANSACTION_dumpHeap = 118;
        static final int TRANSACTION_dumpHeapFinished = 226;
        static final int TRANSACTION_enterPictureInPictureMode = 255;
        static final int TRANSACTION_enterSafeMode = 64;
        static final int TRANSACTION_exitFreeformMode = 242;
        static final int TRANSACTION_finishActivity = 8;
        static final int TRANSACTION_finishActivityAffinity = 146;
        static final int TRANSACTION_finishHeavyWeightApp = 107;
        static final int TRANSACTION_finishInstrumentation = 41;
        static final int TRANSACTION_finishReceiver = 13;
        static final int TRANSACTION_finishSubActivity = 28;
        static final int TRANSACTION_finishVoiceTask = 203;
        static final int TRANSACTION_forceStopPackage = 77;
        static final int TRANSACTION_getActivityClassForToken = 45;
        static final int TRANSACTION_getActivityDisplayId = 186;
        static final int TRANSACTION_getActivityOptions = 199;
        static final int TRANSACTION_getAllStackInfos = 171;
        static final int TRANSACTION_getAppTaskThumbnailSize = 209;
        static final int TRANSACTION_getAppTasks = 200;
        static final int TRANSACTION_getAssistContextExtras = 162;
        static final int TRANSACTION_getCallingActivity = 19;
        static final int TRANSACTION_getCallingPackage = 18;
        static final int TRANSACTION_getConfiguration = 42;
        static final int TRANSACTION_getContentProvider = 25;
        static final int TRANSACTION_getContentProviderExternal = 138;
        static final int TRANSACTION_getCurrentUser = 142;
        static final int TRANSACTION_getDeviceConfigurationInfo = 82;
        static final int TRANSACTION_getFilteredTasks = 21;
        static final int TRANSACTION_getFocusedStackInfo = 173;
        static final int TRANSACTION_getFrontActivityScreenCompatMode = 122;
        static final int TRANSACTION_getGrantedUriPermissions = 263;
        static final int TRANSACTION_getIntentForIntentSender = 161;
        static final int TRANSACTION_getIntentSender = 59;
        static final int TRANSACTION_getLastResumedActivityUserId = 297;
        static final int TRANSACTION_getLaunchedFromPackage = 164;
        static final int TRANSACTION_getLaunchedFromUid = 147;
        static final int TRANSACTION_getLockTaskModeState = 224;
        static final int TRANSACTION_getMaxNumPictureInPictureActions = 257;
        static final int TRANSACTION_getMemoryInfo = 74;
        static final int TRANSACTION_getMemoryTrimLevel = 275;
        static final int TRANSACTION_getMyMemoryState = 140;
        static final int TRANSACTION_getPackageAskScreenCompat = 126;
        static final int TRANSACTION_getPackageForIntentSender = 61;
        static final int TRANSACTION_getPackageForToken = 46;
        static final int TRANSACTION_getPackageProcessState = 231;
        static final int TRANSACTION_getPackageScreenCompatMode = 124;
        static final int TRANSACTION_getPersistedUriPermissions = 183;
        static final int TRANSACTION_getProcessLimit = 48;
        static final int TRANSACTION_getProcessMemoryInfo = 96;
        static final int TRANSACTION_getProcessPss = 135;
        static final int TRANSACTION_getProcessesInErrorState = 75;
        static final int TRANSACTION_getProviderMimeType = 113;
        static final int TRANSACTION_getRecentTasks = 56;
        static final int TRANSACTION_getRequestedOrientation = 69;
        static final int TRANSACTION_getRunningAppProcesses = 81;
        static final int TRANSACTION_getRunningExternalApplications = 106;
        static final int TRANSACTION_getRunningServiceControlPanel = 29;
        static final int TRANSACTION_getRunningUserIds = 155;
        static final int TRANSACTION_getServices = 79;
        static final int TRANSACTION_getStackInfo = 174;
        static final int TRANSACTION_getTagForIntentSender = 188;
        static final int TRANSACTION_getTaskBounds = 185;
        static final int TRANSACTION_getTaskDescription = 80;
        static final int TRANSACTION_getTaskDescriptionIcon = 213;
        static final int TRANSACTION_getTaskForActivity = 24;
        static final int TRANSACTION_getTaskSnapshot = 292;
        static final int TRANSACTION_getTasks = 20;
        static final int TRANSACTION_getUidForIntentSender = 91;
        static final int TRANSACTION_getUidProcessState = 235;
        static final int TRANSACTION_getUriPermissionOwnerForActivity = 259;
        static final int TRANSACTION_grantUriPermission = 51;
        static final int TRANSACTION_grantUriPermissionFromOwner = 115;
        static final int TRANSACTION_handleApplicationCrash = 5;
        static final int TRANSACTION_handleApplicationStrictModeViolation = 108;
        static final int TRANSACTION_handleApplicationWtf = 100;
        static final int TRANSACTION_handleIncomingUser = 92;
        static final int TRANSACTION_hang = 167;
        static final int TRANSACTION_inputDispatchingTimedOut = 159;
        static final int TRANSACTION_isAppForeground = 265;
        static final int TRANSACTION_isAppStartModeDisabled = 250;
        static final int TRANSACTION_isAssistDataAllowedOnCurrentActivity = 236;
        static final int TRANSACTION_isBackgroundRestricted = 282;
        static final int TRANSACTION_isImmersive = 109;
        static final int TRANSACTION_isInLockTaskMode = 192;
        static final int TRANSACTION_isInMultiWindowMode = 252;
        static final int TRANSACTION_isInPictureInPictureMode = 253;
        static final int TRANSACTION_isIntentSenderAForegroundService = 150;
        static final int TRANSACTION_isIntentSenderAnActivity = 149;
        static final int TRANSACTION_isIntentSenderTargetedToPackage = 133;
        static final int TRANSACTION_isRootVoiceInteraction = 238;
        static final int TRANSACTION_isTopActivityImmersive = 111;
        static final int TRANSACTION_isTopOfTask = 204;
        static final int TRANSACTION_isUidActive = 4;
        static final int TRANSACTION_isUserAMonkey = 102;
        static final int TRANSACTION_isUserRunning = 120;
        static final int TRANSACTION_isVrModePackageEnabled = 277;
        static final int TRANSACTION_keyguardGoingAway = 234;
        static final int TRANSACTION_killAllBackgroundProcesses = 137;
        static final int TRANSACTION_killApplication = 94;
        static final int TRANSACTION_killApplicationProcess = 97;
        static final int TRANSACTION_killBackgroundProcesses = 101;
        static final int TRANSACTION_killPackageDependents = 254;
        static final int TRANSACTION_killPids = 78;
        static final int TRANSACTION_killProcessesBelowForeground = 141;
        static final int TRANSACTION_killUid = 165;
        static final int TRANSACTION_launchAssistIntent = 214;
        static final int TRANSACTION_makePackageIdle = 274;
        static final int TRANSACTION_moveActivityTaskToBack = 73;
        static final int TRANSACTION_moveStackToDisplay = 287;
        static final int TRANSACTION_moveTaskBackwards = 23;
        static final int TRANSACTION_moveTaskToFront = 22;
        static final int TRANSACTION_moveTaskToStack = 169;
        static final int TRANSACTION_moveTasksToFullscreenStack = 248;
        static final int TRANSACTION_moveTopActivityToPinnedStack = 249;
        static final int TRANSACTION_navigateUpTo = 144;
        static final int TRANSACTION_newUriPermissionOwner = 114;
        static final int TRANSACTION_noteAlarmFinish = 230;
        static final int TRANSACTION_noteAlarmStart = 229;
        static final int TRANSACTION_noteWakeupAlarm = 66;
        static final int TRANSACTION_notifyActivityDrawn = 177;
        static final int TRANSACTION_notifyCleartextNetwork = 219;
        static final int TRANSACTION_notifyEnterAnimationComplete = 206;
        static final int TRANSACTION_notifyLaunchTaskBehindComplete = 205;
        static final int TRANSACTION_notifyLockedProfile = 278;
        static final int TRANSACTION_notifyPinnedStackAnimationEnded = 270;
        static final int TRANSACTION_notifyPinnedStackAnimationStarted = 269;
        static final int TRANSACTION_openContentUri = 1;
        static final int TRANSACTION_overridePendingTransition = 99;
        static final int TRANSACTION_peekService = 83;
        static final int TRANSACTION_performIdleMaintenance = 180;
        static final int TRANSACTION_positionTaskInStack = 241;
        static final int TRANSACTION_profileControl = 84;
        static final int TRANSACTION_publishContentProviders = 26;
        static final int TRANSACTION_publishService = 34;
        static final int TRANSACTION_refContentProvider = 27;
        static final int TRANSACTION_registerIntentSenderCancelListener = 62;
        static final int TRANSACTION_registerProcessObserver = 131;
        static final int TRANSACTION_registerReceiver = 9;
        static final int TRANSACTION_registerRemoteAnimationForNextActivityStart = 304;
        static final int TRANSACTION_registerRemoteAnimations = 303;
        static final int TRANSACTION_registerTaskStackListener = 217;
        static final int TRANSACTION_registerUidObserver = 2;
        static final int TRANSACTION_registerUserSwitchObserver = 153;
        static final int TRANSACTION_releaseActivityInstance = 210;
        static final int TRANSACTION_releasePersistableUriPermission = 182;
        static final int TRANSACTION_releaseSomeActivities = 211;
        static final int TRANSACTION_removeContentProvider = 67;
        static final int TRANSACTION_removeContentProviderExternal = 139;
        static final int TRANSACTION_removeStack = 271;
        static final int TRANSACTION_removeStacksInWindowingModes = 272;
        static final int TRANSACTION_removeStacksWithActivityTypes = 273;
        static final int TRANSACTION_removeTask = 130;
        static final int TRANSACTION_reportActivityFullyDrawn = 178;
        static final int TRANSACTION_reportAssistContextExtras = 163;
        static final int TRANSACTION_reportSizeConfigurations = 243;
        static final int TRANSACTION_requestAssistContextExtras = 222;
        static final int TRANSACTION_requestAutofillData = 288;
        static final int TRANSACTION_requestBugReport = 156;
        static final int TRANSACTION_requestTelephonyBugReport = 157;
        static final int TRANSACTION_requestWifiBugReport = 158;
        static final int TRANSACTION_resizeDockedStack = 260;
        static final int TRANSACTION_resizePinnedStack = 276;
        static final int TRANSACTION_resizeStack = 170;
        static final int TRANSACTION_resizeTask = 223;
        static final int TRANSACTION_restart = 179;
        static final int TRANSACTION_restartUserInBackground = 290;
        static final int TRANSACTION_resumeAppSwitches = 87;
        static final int TRANSACTION_revokeUriPermission = 52;
        static final int TRANSACTION_revokeUriPermissionFromOwner = 116;
        static final int TRANSACTION_scheduleApplicationInfoChanged = 293;
        static final int TRANSACTION_sendIdleJobTrigger = 280;
        static final int TRANSACTION_sendIntentSender = 281;
        static final int TRANSACTION_serviceDoneExecuting = 57;
        static final int TRANSACTION_setAalEnabled = 307;
        static final int TRANSACTION_setAalMode = 306;
        static final int TRANSACTION_setActivityController = 53;
        static final int TRANSACTION_setAgentApp = 37;
        static final int TRANSACTION_setAlwaysFinish = 38;
        static final int TRANSACTION_setDebugApp = 36;
        static final int TRANSACTION_setDisablePreviewScreenshots = 296;
        static final int TRANSACTION_setDumpHeapDebugLimit = 225;
        static final int TRANSACTION_setFocusedStack = 172;
        static final int TRANSACTION_setFocusedTask = 129;
        static final int TRANSACTION_setFrontActivityScreenCompatMode = 123;
        static final int TRANSACTION_setHasTopUi = 285;
        static final int TRANSACTION_setImmersive = 110;
        static final int TRANSACTION_setLockScreenShown = 145;
        static final int TRANSACTION_setPackageAskScreenCompat = 127;
        static final int TRANSACTION_setPackageScreenCompatMode = 125;
        static final int TRANSACTION_setPersistentVrThread = 294;
        static final int TRANSACTION_setPictureInPictureParams = 256;
        static final int TRANSACTION_setProcessImportant = 71;
        static final int TRANSACTION_setProcessLimit = 47;
        static final int TRANSACTION_setProcessMemoryTrimLevel = 187;
        static final int TRANSACTION_setRenderThread = 284;
        static final int TRANSACTION_setRequestedOrientation = 68;
        static final int TRANSACTION_setServiceForeground = 72;
        static final int TRANSACTION_setShowWhenLocked = 300;
        static final int TRANSACTION_setSplitScreenResizing = 261;
        static final int TRANSACTION_setTaskDescription = 193;
        static final int TRANSACTION_setTaskResizeable = 221;
        static final int TRANSACTION_setTaskWindowingMode = 168;
        static final int TRANSACTION_setTaskWindowingModeSplitScreenPrimary = 244;
        static final int TRANSACTION_setTurnScreenOn = 301;
        static final int TRANSACTION_setUserIsMonkey = 166;
        static final int TRANSACTION_setVoiceKeepAwake = 227;
        static final int TRANSACTION_setVrMode = 262;
        static final int TRANSACTION_setVrThread = 283;
        static final int TRANSACTION_shouldUpRecreateTask = 143;
        static final int TRANSACTION_showAssistFromActivity = 237;
        static final int TRANSACTION_showBootMessage = 136;
        static final int TRANSACTION_showLockTaskEscapeMessage = 232;
        static final int TRANSACTION_showWaitingForDebugger = 54;
        static final int TRANSACTION_shutdown = 85;
        static final int TRANSACTION_signalPersistentProcesses = 55;
        static final int TRANSACTION_startActivities = 119;
        static final int TRANSACTION_startActivity = 6;
        static final int TRANSACTION_startActivityAndWait = 103;
        static final int TRANSACTION_startActivityAsCaller = 207;
        static final int TRANSACTION_startActivityAsUser = 151;
        static final int TRANSACTION_startActivityFromRecents = 198;
        static final int TRANSACTION_startActivityIntentSender = 98;
        static final int TRANSACTION_startActivityWithConfig = 105;
        static final int TRANSACTION_startAssistantActivity = 195;
        static final int TRANSACTION_startBinderTracking = 239;
        static final int TRANSACTION_startConfirmDeviceCredentialIntent = 279;
        static final int TRANSACTION_startInPlaceAnimationOnFrontMostApplication = 215;
        static final int TRANSACTION_startInstrumentation = 39;
        static final int TRANSACTION_startLocalVoiceInteraction = 266;
        static final int TRANSACTION_startLockTaskModeByToken = 190;
        static final int TRANSACTION_startNextMatchingActivity = 65;
        static final int TRANSACTION_startRecentsActivity = 196;
        static final int TRANSACTION_startService = 30;
        static final int TRANSACTION_startSystemLockTaskMode = 201;
        static final int TRANSACTION_startUserInBackground = 189;
        static final int TRANSACTION_startUserInBackgroundWithListener = 302;
        static final int TRANSACTION_startVoiceActivity = 194;
        static final int TRANSACTION_stopAppSwitches = 86;
        static final int TRANSACTION_stopBinderTrackingAndDump = 240;
        static final int TRANSACTION_stopLocalVoiceInteraction = 267;
        static final int TRANSACTION_stopLockTaskModeByToken = 191;
        static final int TRANSACTION_stopService = 31;
        static final int TRANSACTION_stopServiceToken = 44;
        static final int TRANSACTION_stopSystemLockTaskMode = 202;
        static final int TRANSACTION_stopUser = 152;
        static final int TRANSACTION_supportsLocalVoiceInteraction = 268;
        static final int TRANSACTION_suppressResizeConfigChanges = 247;
        static final int TRANSACTION_switchUser = 128;
        static final int TRANSACTION_takePersistableUriPermission = 181;
        static final int TRANSACTION_unbindBackupAgent = 90;
        static final int TRANSACTION_unbindFinished = 70;
        static final int TRANSACTION_unbindService = 33;
        static final int TRANSACTION_unbroadcastIntent = 12;
        static final int TRANSACTION_unhandledBack = 7;
        static final int TRANSACTION_unlockUser = 251;
        static final int TRANSACTION_unregisterIntentSenderCancelListener = 63;
        static final int TRANSACTION_unregisterProcessObserver = 132;
        static final int TRANSACTION_unregisterReceiver = 10;
        static final int TRANSACTION_unregisterTaskStackListener = 218;
        static final int TRANSACTION_unregisterUidObserver = 3;
        static final int TRANSACTION_unregisterUserSwitchObserver = 154;
        static final int TRANSACTION_unstableProviderDied = 148;
        static final int TRANSACTION_updateConfiguration = 43;
        static final int TRANSACTION_updateDeviceOwner = 233;
        static final int TRANSACTION_updateDisplayOverrideConfiguration = 286;
        static final int TRANSACTION_updateLockTaskFeatures = 299;
        static final int TRANSACTION_updateLockTaskPackages = 228;
        static final int TRANSACTION_updatePersistentConfiguration = 134;
        static final int TRANSACTION_waitForNetworkStateUpdate = 295;
        static final int TRANSACTION_willActivityBeVisible = 104;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IActivityManager asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface(DESCRIPTOR);
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof IActivityManager)) {
                return (IActivityManager) iInterfaceQueryLocalInterface;
            }
            return new Proxy(iBinder);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            Bundle bundleCreateFromParcel;
            PersistableBundle persistableBundleCreateFromParcel;
            Intent intentCreateFromParcel;
            Intent intentCreateFromParcel2;
            Intent intentCreateFromParcel3;
            ActivityManager.TaskDescription taskDescriptionCreateFromParcel;
            Rect rectCreateFromParcel;
            Intent intentCreateFromParcel4;
            if (i == 1598968902) {
                parcel2.writeString(DESCRIPTOR);
                return true;
            }
            switch (i) {
                case 1:
                    parcel.enforceInterface(DESCRIPTOR);
                    ParcelFileDescriptor parcelFileDescriptorOpenContentUri = openContentUri(parcel.readString());
                    parcel2.writeNoException();
                    if (parcelFileDescriptorOpenContentUri != null) {
                        parcel2.writeInt(1);
                        parcelFileDescriptorOpenContentUri.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 2:
                    parcel.enforceInterface(DESCRIPTOR);
                    registerUidObserver(IUidObserver.Stub.asInterface(parcel.readStrongBinder()), parcel.readInt(), parcel.readInt(), parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case 3:
                    parcel.enforceInterface(DESCRIPTOR);
                    unregisterUidObserver(IUidObserver.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    return true;
                case 4:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zIsUidActive = isUidActive(parcel.readInt(), parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsUidActive ? 1 : 0);
                    return true;
                case 5:
                    parcel.enforceInterface(DESCRIPTOR);
                    handleApplicationCrash(parcel.readStrongBinder(), parcel.readInt() != 0 ? ApplicationErrorReport.ParcelableCrashInfo.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    return true;
                case 6:
                    return onTransact$startActivity$(parcel, parcel2);
                case 7:
                    parcel.enforceInterface(DESCRIPTOR);
                    unhandledBack();
                    parcel2.writeNoException();
                    return true;
                case 8:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zFinishActivity = finishActivity(parcel.readStrongBinder(), parcel.readInt(), parcel.readInt() != 0 ? Intent.CREATOR.createFromParcel(parcel) : null, parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(zFinishActivity ? 1 : 0);
                    return true;
                case 9:
                    return onTransact$registerReceiver$(parcel, parcel2);
                case 10:
                    parcel.enforceInterface(DESCRIPTOR);
                    unregisterReceiver(IIntentReceiver.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    return true;
                case 11:
                    return onTransact$broadcastIntent$(parcel, parcel2);
                case 12:
                    parcel.enforceInterface(DESCRIPTOR);
                    unbroadcastIntent(IApplicationThread.Stub.asInterface(parcel.readStrongBinder()), parcel.readInt() != 0 ? Intent.CREATOR.createFromParcel(parcel) : null, parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 13:
                    return onTransact$finishReceiver$(parcel, parcel2);
                case 14:
                    parcel.enforceInterface(DESCRIPTOR);
                    attachApplication(IApplicationThread.Stub.asInterface(parcel.readStrongBinder()), parcel.readLong());
                    parcel2.writeNoException();
                    return true;
                case 15:
                    parcel.enforceInterface(DESCRIPTOR);
                    activityIdle(parcel.readStrongBinder(), parcel.readInt() != 0 ? Configuration.CREATOR.createFromParcel(parcel) : null, parcel.readInt() != 0);
                    return true;
                case 16:
                    parcel.enforceInterface(DESCRIPTOR);
                    activityPaused(parcel.readStrongBinder());
                    parcel2.writeNoException();
                    return true;
                case 17:
                    parcel.enforceInterface(DESCRIPTOR);
                    IBinder strongBinder = parcel.readStrongBinder();
                    if (parcel.readInt() != 0) {
                        bundleCreateFromParcel = Bundle.CREATOR.createFromParcel(parcel);
                    } else {
                        bundleCreateFromParcel = null;
                    }
                    if (parcel.readInt() != 0) {
                        persistableBundleCreateFromParcel = PersistableBundle.CREATOR.createFromParcel(parcel);
                    } else {
                        persistableBundleCreateFromParcel = null;
                    }
                    activityStopped(strongBinder, bundleCreateFromParcel, persistableBundleCreateFromParcel, parcel.readInt() != 0 ? TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(parcel) : null);
                    return true;
                case 18:
                    parcel.enforceInterface(DESCRIPTOR);
                    String callingPackage = getCallingPackage(parcel.readStrongBinder());
                    parcel2.writeNoException();
                    parcel2.writeString(callingPackage);
                    return true;
                case 19:
                    parcel.enforceInterface(DESCRIPTOR);
                    ComponentName callingActivity = getCallingActivity(parcel.readStrongBinder());
                    parcel2.writeNoException();
                    if (callingActivity != null) {
                        parcel2.writeInt(1);
                        callingActivity.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 20:
                    parcel.enforceInterface(DESCRIPTOR);
                    List<ActivityManager.RunningTaskInfo> tasks = getTasks(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeTypedList(tasks);
                    return true;
                case 21:
                    parcel.enforceInterface(DESCRIPTOR);
                    List<ActivityManager.RunningTaskInfo> filteredTasks = getFilteredTasks(parcel.readInt(), parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeTypedList(filteredTasks);
                    return true;
                case 22:
                    parcel.enforceInterface(DESCRIPTOR);
                    moveTaskToFront(parcel.readInt(), parcel.readInt(), parcel.readInt() != 0 ? Bundle.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    return true;
                case 23:
                    parcel.enforceInterface(DESCRIPTOR);
                    moveTaskBackwards(parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 24:
                    parcel.enforceInterface(DESCRIPTOR);
                    int taskForActivity = getTaskForActivity(parcel.readStrongBinder(), parcel.readInt() != 0);
                    parcel2.writeNoException();
                    parcel2.writeInt(taskForActivity);
                    return true;
                case 25:
                    parcel.enforceInterface(DESCRIPTOR);
                    ContentProviderHolder contentProvider = getContentProvider(IApplicationThread.Stub.asInterface(parcel.readStrongBinder()), parcel.readString(), parcel.readInt(), parcel.readInt() != 0);
                    parcel2.writeNoException();
                    if (contentProvider != null) {
                        parcel2.writeInt(1);
                        contentProvider.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 26:
                    parcel.enforceInterface(DESCRIPTOR);
                    publishContentProviders(IApplicationThread.Stub.asInterface(parcel.readStrongBinder()), parcel.createTypedArrayList(ContentProviderHolder.CREATOR));
                    parcel2.writeNoException();
                    return true;
                case 27:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zRefContentProvider = refContentProvider(parcel.readStrongBinder(), parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(zRefContentProvider ? 1 : 0);
                    return true;
                case 28:
                    parcel.enforceInterface(DESCRIPTOR);
                    finishSubActivity(parcel.readStrongBinder(), parcel.readString(), parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 29:
                    parcel.enforceInterface(DESCRIPTOR);
                    PendingIntent runningServiceControlPanel = getRunningServiceControlPanel(parcel.readInt() != 0 ? ComponentName.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    if (runningServiceControlPanel != null) {
                        parcel2.writeInt(1);
                        runningServiceControlPanel.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 30:
                    return onTransact$startService$(parcel, parcel2);
                case 31:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iStopService = stopService(IApplicationThread.Stub.asInterface(parcel.readStrongBinder()), parcel.readInt() != 0 ? Intent.CREATOR.createFromParcel(parcel) : null, parcel.readString(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(iStopService);
                    return true;
                case 32:
                    return onTransact$bindService$(parcel, parcel2);
                case 33:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zUnbindService = unbindService(IServiceConnection.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    parcel2.writeInt(zUnbindService ? 1 : 0);
                    return true;
                case 34:
                    parcel.enforceInterface(DESCRIPTOR);
                    publishService(parcel.readStrongBinder(), parcel.readInt() != 0 ? Intent.CREATOR.createFromParcel(parcel) : null, parcel.readStrongBinder());
                    parcel2.writeNoException();
                    return true;
                case 35:
                    parcel.enforceInterface(DESCRIPTOR);
                    activityResumed(parcel.readStrongBinder());
                    parcel2.writeNoException();
                    return true;
                case 36:
                    parcel.enforceInterface(DESCRIPTOR);
                    setDebugApp(parcel.readString(), parcel.readInt() != 0, parcel.readInt() != 0);
                    parcel2.writeNoException();
                    return true;
                case 37:
                    parcel.enforceInterface(DESCRIPTOR);
                    setAgentApp(parcel.readString(), parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case 38:
                    parcel.enforceInterface(DESCRIPTOR);
                    setAlwaysFinish(parcel.readInt() != 0);
                    parcel2.writeNoException();
                    return true;
                case 39:
                    return onTransact$startInstrumentation$(parcel, parcel2);
                case 40:
                    parcel.enforceInterface(DESCRIPTOR);
                    addInstrumentationResults(IApplicationThread.Stub.asInterface(parcel.readStrongBinder()), parcel.readInt() != 0 ? Bundle.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    return true;
                case 41:
                    parcel.enforceInterface(DESCRIPTOR);
                    finishInstrumentation(IApplicationThread.Stub.asInterface(parcel.readStrongBinder()), parcel.readInt(), parcel.readInt() != 0 ? Bundle.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    return true;
                case 42:
                    parcel.enforceInterface(DESCRIPTOR);
                    Configuration configuration = getConfiguration();
                    parcel2.writeNoException();
                    if (configuration != null) {
                        parcel2.writeInt(1);
                        configuration.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 43:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zUpdateConfiguration = updateConfiguration(parcel.readInt() != 0 ? Configuration.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    parcel2.writeInt(zUpdateConfiguration ? 1 : 0);
                    return true;
                case 44:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zStopServiceToken = stopServiceToken(parcel.readInt() != 0 ? ComponentName.CREATOR.createFromParcel(parcel) : null, parcel.readStrongBinder(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(zStopServiceToken ? 1 : 0);
                    return true;
                case 45:
                    parcel.enforceInterface(DESCRIPTOR);
                    ComponentName activityClassForToken = getActivityClassForToken(parcel.readStrongBinder());
                    parcel2.writeNoException();
                    if (activityClassForToken != null) {
                        parcel2.writeInt(1);
                        activityClassForToken.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 46:
                    parcel.enforceInterface(DESCRIPTOR);
                    String packageForToken = getPackageForToken(parcel.readStrongBinder());
                    parcel2.writeNoException();
                    parcel2.writeString(packageForToken);
                    return true;
                case 47:
                    parcel.enforceInterface(DESCRIPTOR);
                    setProcessLimit(parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 48:
                    parcel.enforceInterface(DESCRIPTOR);
                    int processLimit = getProcessLimit();
                    parcel2.writeNoException();
                    parcel2.writeInt(processLimit);
                    return true;
                case 49:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iCheckPermission = checkPermission(parcel.readString(), parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(iCheckPermission);
                    return true;
                case 50:
                    return onTransact$checkUriPermission$(parcel, parcel2);
                case 51:
                    parcel.enforceInterface(DESCRIPTOR);
                    grantUriPermission(IApplicationThread.Stub.asInterface(parcel.readStrongBinder()), parcel.readString(), parcel.readInt() != 0 ? Uri.CREATOR.createFromParcel(parcel) : null, parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 52:
                    parcel.enforceInterface(DESCRIPTOR);
                    revokeUriPermission(IApplicationThread.Stub.asInterface(parcel.readStrongBinder()), parcel.readString(), parcel.readInt() != 0 ? Uri.CREATOR.createFromParcel(parcel) : null, parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 53:
                    parcel.enforceInterface(DESCRIPTOR);
                    setActivityController(IActivityController.Stub.asInterface(parcel.readStrongBinder()), parcel.readInt() != 0);
                    parcel2.writeNoException();
                    return true;
                case 54:
                    parcel.enforceInterface(DESCRIPTOR);
                    showWaitingForDebugger(IApplicationThread.Stub.asInterface(parcel.readStrongBinder()), parcel.readInt() != 0);
                    parcel2.writeNoException();
                    return true;
                case 55:
                    parcel.enforceInterface(DESCRIPTOR);
                    signalPersistentProcesses(parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 56:
                    parcel.enforceInterface(DESCRIPTOR);
                    ParceledListSlice recentTasks = getRecentTasks(parcel.readInt(), parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    if (recentTasks != null) {
                        parcel2.writeInt(1);
                        recentTasks.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 57:
                    parcel.enforceInterface(DESCRIPTOR);
                    serviceDoneExecuting(parcel.readStrongBinder(), parcel.readInt(), parcel.readInt(), parcel.readInt());
                    return true;
                case 58:
                    parcel.enforceInterface(DESCRIPTOR);
                    activityDestroyed(parcel.readStrongBinder());
                    return true;
                case 59:
                    return onTransact$getIntentSender$(parcel, parcel2);
                case 60:
                    parcel.enforceInterface(DESCRIPTOR);
                    cancelIntentSender(IIntentSender.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    return true;
                case 61:
                    parcel.enforceInterface(DESCRIPTOR);
                    String packageForIntentSender = getPackageForIntentSender(IIntentSender.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    parcel2.writeString(packageForIntentSender);
                    return true;
                case 62:
                    parcel.enforceInterface(DESCRIPTOR);
                    registerIntentSenderCancelListener(IIntentSender.Stub.asInterface(parcel.readStrongBinder()), IResultReceiver.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    return true;
                case 63:
                    parcel.enforceInterface(DESCRIPTOR);
                    unregisterIntentSenderCancelListener(IIntentSender.Stub.asInterface(parcel.readStrongBinder()), IResultReceiver.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    return true;
                case 64:
                    parcel.enforceInterface(DESCRIPTOR);
                    enterSafeMode();
                    parcel2.writeNoException();
                    return true;
                case 65:
                    parcel.enforceInterface(DESCRIPTOR);
                    IBinder strongBinder2 = parcel.readStrongBinder();
                    if (parcel.readInt() != 0) {
                        intentCreateFromParcel = Intent.CREATOR.createFromParcel(parcel);
                    } else {
                        intentCreateFromParcel = null;
                    }
                    boolean zStartNextMatchingActivity = startNextMatchingActivity(strongBinder2, intentCreateFromParcel, parcel.readInt() != 0 ? Bundle.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    parcel2.writeInt(zStartNextMatchingActivity ? 1 : 0);
                    return true;
                case 66:
                    return onTransact$noteWakeupAlarm$(parcel, parcel2);
                case 67:
                    parcel.enforceInterface(DESCRIPTOR);
                    removeContentProvider(parcel.readStrongBinder(), parcel.readInt() != 0);
                    parcel2.writeNoException();
                    return true;
                case 68:
                    parcel.enforceInterface(DESCRIPTOR);
                    setRequestedOrientation(parcel.readStrongBinder(), parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 69:
                    parcel.enforceInterface(DESCRIPTOR);
                    int requestedOrientation = getRequestedOrientation(parcel.readStrongBinder());
                    parcel2.writeNoException();
                    parcel2.writeInt(requestedOrientation);
                    return true;
                case 70:
                    parcel.enforceInterface(DESCRIPTOR);
                    unbindFinished(parcel.readStrongBinder(), parcel.readInt() != 0 ? Intent.CREATOR.createFromParcel(parcel) : null, parcel.readInt() != 0);
                    parcel2.writeNoException();
                    return true;
                case 71:
                    parcel.enforceInterface(DESCRIPTOR);
                    setProcessImportant(parcel.readStrongBinder(), parcel.readInt(), parcel.readInt() != 0, parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case 72:
                    return onTransact$setServiceForeground$(parcel, parcel2);
                case 73:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zMoveActivityTaskToBack = moveActivityTaskToBack(parcel.readStrongBinder(), parcel.readInt() != 0);
                    parcel2.writeNoException();
                    parcel2.writeInt(zMoveActivityTaskToBack ? 1 : 0);
                    return true;
                case 74:
                    parcel.enforceInterface(DESCRIPTOR);
                    ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
                    getMemoryInfo(memoryInfo);
                    parcel2.writeNoException();
                    parcel2.writeInt(1);
                    memoryInfo.writeToParcel(parcel2, 1);
                    return true;
                case 75:
                    parcel.enforceInterface(DESCRIPTOR);
                    List<ActivityManager.ProcessErrorStateInfo> processesInErrorState = getProcessesInErrorState();
                    parcel2.writeNoException();
                    parcel2.writeTypedList(processesInErrorState);
                    return true;
                case 76:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zClearApplicationUserData = clearApplicationUserData(parcel.readString(), parcel.readInt() != 0, IPackageDataObserver.Stub.asInterface(parcel.readStrongBinder()), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(zClearApplicationUserData ? 1 : 0);
                    return true;
                case 77:
                    parcel.enforceInterface(DESCRIPTOR);
                    forceStopPackage(parcel.readString(), parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 78:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zKillPids = killPids(parcel.createIntArray(), parcel.readString(), parcel.readInt() != 0);
                    parcel2.writeNoException();
                    parcel2.writeInt(zKillPids ? 1 : 0);
                    return true;
                case 79:
                    parcel.enforceInterface(DESCRIPTOR);
                    List<ActivityManager.RunningServiceInfo> services = getServices(parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeTypedList(services);
                    return true;
                case 80:
                    parcel.enforceInterface(DESCRIPTOR);
                    ActivityManager.TaskDescription taskDescription = getTaskDescription(parcel.readInt());
                    parcel2.writeNoException();
                    if (taskDescription != null) {
                        parcel2.writeInt(1);
                        taskDescription.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 81:
                    parcel.enforceInterface(DESCRIPTOR);
                    List<ActivityManager.RunningAppProcessInfo> runningAppProcesses = getRunningAppProcesses();
                    parcel2.writeNoException();
                    parcel2.writeTypedList(runningAppProcesses);
                    return true;
                case 82:
                    parcel.enforceInterface(DESCRIPTOR);
                    ConfigurationInfo deviceConfigurationInfo = getDeviceConfigurationInfo();
                    parcel2.writeNoException();
                    if (deviceConfigurationInfo != null) {
                        parcel2.writeInt(1);
                        deviceConfigurationInfo.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 83:
                    parcel.enforceInterface(DESCRIPTOR);
                    IBinder iBinderPeekService = peekService(parcel.readInt() != 0 ? Intent.CREATOR.createFromParcel(parcel) : null, parcel.readString(), parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeStrongBinder(iBinderPeekService);
                    return true;
                case 84:
                    return onTransact$profileControl$(parcel, parcel2);
                case 85:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zShutdown = shutdown(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(zShutdown ? 1 : 0);
                    return true;
                case 86:
                    parcel.enforceInterface(DESCRIPTOR);
                    stopAppSwitches();
                    parcel2.writeNoException();
                    return true;
                case 87:
                    parcel.enforceInterface(DESCRIPTOR);
                    resumeAppSwitches();
                    parcel2.writeNoException();
                    return true;
                case 88:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zBindBackupAgent = bindBackupAgent(parcel.readString(), parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(zBindBackupAgent ? 1 : 0);
                    return true;
                case 89:
                    parcel.enforceInterface(DESCRIPTOR);
                    backupAgentCreated(parcel.readString(), parcel.readStrongBinder());
                    parcel2.writeNoException();
                    return true;
                case 90:
                    parcel.enforceInterface(DESCRIPTOR);
                    unbindBackupAgent(parcel.readInt() != 0 ? ApplicationInfo.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    return true;
                case 91:
                    parcel.enforceInterface(DESCRIPTOR);
                    int uidForIntentSender = getUidForIntentSender(IIntentSender.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    parcel2.writeInt(uidForIntentSender);
                    return true;
                case 92:
                    return onTransact$handleIncomingUser$(parcel, parcel2);
                case 93:
                    parcel.enforceInterface(DESCRIPTOR);
                    addPackageDependency(parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case 94:
                    parcel.enforceInterface(DESCRIPTOR);
                    killApplication(parcel.readString(), parcel.readInt(), parcel.readInt(), parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case 95:
                    parcel.enforceInterface(DESCRIPTOR);
                    closeSystemDialogs(parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case 96:
                    parcel.enforceInterface(DESCRIPTOR);
                    Debug.MemoryInfo[] processMemoryInfo = getProcessMemoryInfo(parcel.createIntArray());
                    parcel2.writeNoException();
                    parcel2.writeTypedArray(processMemoryInfo, 1);
                    return true;
                case 97:
                    parcel.enforceInterface(DESCRIPTOR);
                    killApplicationProcess(parcel.readString(), parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 98:
                    return onTransact$startActivityIntentSender$(parcel, parcel2);
                case 99:
                    parcel.enforceInterface(DESCRIPTOR);
                    overridePendingTransition(parcel.readStrongBinder(), parcel.readString(), parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 100:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zHandleApplicationWtf = handleApplicationWtf(parcel.readStrongBinder(), parcel.readString(), parcel.readInt() != 0, parcel.readInt() != 0 ? ApplicationErrorReport.ParcelableCrashInfo.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    parcel2.writeInt(zHandleApplicationWtf ? 1 : 0);
                    return true;
                case 101:
                    parcel.enforceInterface(DESCRIPTOR);
                    killBackgroundProcesses(parcel.readString(), parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 102:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zIsUserAMonkey = isUserAMonkey();
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsUserAMonkey ? 1 : 0);
                    return true;
                case 103:
                    return onTransact$startActivityAndWait$(parcel, parcel2);
                case 104:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zWillActivityBeVisible = willActivityBeVisible(parcel.readStrongBinder());
                    parcel2.writeNoException();
                    parcel2.writeInt(zWillActivityBeVisible ? 1 : 0);
                    return true;
                case 105:
                    return onTransact$startActivityWithConfig$(parcel, parcel2);
                case 106:
                    parcel.enforceInterface(DESCRIPTOR);
                    List<ApplicationInfo> runningExternalApplications = getRunningExternalApplications();
                    parcel2.writeNoException();
                    parcel2.writeTypedList(runningExternalApplications);
                    return true;
                case 107:
                    parcel.enforceInterface(DESCRIPTOR);
                    finishHeavyWeightApp();
                    parcel2.writeNoException();
                    return true;
                case 108:
                    parcel.enforceInterface(DESCRIPTOR);
                    handleApplicationStrictModeViolation(parcel.readStrongBinder(), parcel.readInt(), parcel.readInt() != 0 ? StrictMode.ViolationInfo.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    return true;
                case 109:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zIsImmersive = isImmersive(parcel.readStrongBinder());
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsImmersive ? 1 : 0);
                    return true;
                case 110:
                    parcel.enforceInterface(DESCRIPTOR);
                    setImmersive(parcel.readStrongBinder(), parcel.readInt() != 0);
                    parcel2.writeNoException();
                    return true;
                case 111:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zIsTopActivityImmersive = isTopActivityImmersive();
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsTopActivityImmersive ? 1 : 0);
                    return true;
                case 112:
                    return onTransact$crashApplication$(parcel, parcel2);
                case 113:
                    parcel.enforceInterface(DESCRIPTOR);
                    String providerMimeType = getProviderMimeType(parcel.readInt() != 0 ? Uri.CREATOR.createFromParcel(parcel) : null, parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeString(providerMimeType);
                    return true;
                case 114:
                    parcel.enforceInterface(DESCRIPTOR);
                    IBinder iBinderNewUriPermissionOwner = newUriPermissionOwner(parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeStrongBinder(iBinderNewUriPermissionOwner);
                    return true;
                case 115:
                    return onTransact$grantUriPermissionFromOwner$(parcel, parcel2);
                case 116:
                    parcel.enforceInterface(DESCRIPTOR);
                    revokeUriPermissionFromOwner(parcel.readStrongBinder(), parcel.readInt() != 0 ? Uri.CREATOR.createFromParcel(parcel) : null, parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 117:
                    return onTransact$checkGrantUriPermission$(parcel, parcel2);
                case 118:
                    return onTransact$dumpHeap$(parcel, parcel2);
                case 119:
                    return onTransact$startActivities$(parcel, parcel2);
                case 120:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zIsUserRunning = isUserRunning(parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsUserRunning ? 1 : 0);
                    return true;
                case 121:
                    parcel.enforceInterface(DESCRIPTOR);
                    activitySlept(parcel.readStrongBinder());
                    return true;
                case 122:
                    parcel.enforceInterface(DESCRIPTOR);
                    int frontActivityScreenCompatMode = getFrontActivityScreenCompatMode();
                    parcel2.writeNoException();
                    parcel2.writeInt(frontActivityScreenCompatMode);
                    return true;
                case 123:
                    parcel.enforceInterface(DESCRIPTOR);
                    setFrontActivityScreenCompatMode(parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 124:
                    parcel.enforceInterface(DESCRIPTOR);
                    int packageScreenCompatMode = getPackageScreenCompatMode(parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeInt(packageScreenCompatMode);
                    return true;
                case 125:
                    parcel.enforceInterface(DESCRIPTOR);
                    setPackageScreenCompatMode(parcel.readString(), parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 126:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean packageAskScreenCompat = getPackageAskScreenCompat(parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeInt(packageAskScreenCompat ? 1 : 0);
                    return true;
                case 127:
                    parcel.enforceInterface(DESCRIPTOR);
                    setPackageAskScreenCompat(parcel.readString(), parcel.readInt() != 0);
                    parcel2.writeNoException();
                    return true;
                case 128:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zSwitchUser = switchUser(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(zSwitchUser ? 1 : 0);
                    return true;
                case 129:
                    parcel.enforceInterface(DESCRIPTOR);
                    setFocusedTask(parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 130:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zRemoveTask = removeTask(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(zRemoveTask ? 1 : 0);
                    return true;
                case 131:
                    parcel.enforceInterface(DESCRIPTOR);
                    registerProcessObserver(IProcessObserver.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    return true;
                case 132:
                    parcel.enforceInterface(DESCRIPTOR);
                    unregisterProcessObserver(IProcessObserver.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    return true;
                case 133:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zIsIntentSenderTargetedToPackage = isIntentSenderTargetedToPackage(IIntentSender.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsIntentSenderTargetedToPackage ? 1 : 0);
                    return true;
                case 134:
                    parcel.enforceInterface(DESCRIPTOR);
                    updatePersistentConfiguration(parcel.readInt() != 0 ? Configuration.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    return true;
                case 135:
                    parcel.enforceInterface(DESCRIPTOR);
                    long[] processPss = getProcessPss(parcel.createIntArray());
                    parcel2.writeNoException();
                    parcel2.writeLongArray(processPss);
                    return true;
                case 136:
                    parcel.enforceInterface(DESCRIPTOR);
                    showBootMessage(parcel.readInt() != 0 ? TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(parcel) : null, parcel.readInt() != 0);
                    parcel2.writeNoException();
                    return true;
                case 137:
                    parcel.enforceInterface(DESCRIPTOR);
                    killAllBackgroundProcesses();
                    parcel2.writeNoException();
                    return true;
                case 138:
                    parcel.enforceInterface(DESCRIPTOR);
                    ContentProviderHolder contentProviderExternal = getContentProviderExternal(parcel.readString(), parcel.readInt(), parcel.readStrongBinder());
                    parcel2.writeNoException();
                    if (contentProviderExternal != null) {
                        parcel2.writeInt(1);
                        contentProviderExternal.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 139:
                    parcel.enforceInterface(DESCRIPTOR);
                    removeContentProviderExternal(parcel.readString(), parcel.readStrongBinder());
                    parcel2.writeNoException();
                    return true;
                case 140:
                    parcel.enforceInterface(DESCRIPTOR);
                    ActivityManager.RunningAppProcessInfo runningAppProcessInfo = new ActivityManager.RunningAppProcessInfo();
                    getMyMemoryState(runningAppProcessInfo);
                    parcel2.writeNoException();
                    parcel2.writeInt(1);
                    runningAppProcessInfo.writeToParcel(parcel2, 1);
                    return true;
                case 141:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zKillProcessesBelowForeground = killProcessesBelowForeground(parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeInt(zKillProcessesBelowForeground ? 1 : 0);
                    return true;
                case 142:
                    parcel.enforceInterface(DESCRIPTOR);
                    UserInfo currentUser = getCurrentUser();
                    parcel2.writeNoException();
                    if (currentUser != null) {
                        parcel2.writeInt(1);
                        currentUser.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 143:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zShouldUpRecreateTask = shouldUpRecreateTask(parcel.readStrongBinder(), parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeInt(zShouldUpRecreateTask ? 1 : 0);
                    return true;
                case 144:
                    parcel.enforceInterface(DESCRIPTOR);
                    IBinder strongBinder3 = parcel.readStrongBinder();
                    if (parcel.readInt() != 0) {
                        intentCreateFromParcel2 = Intent.CREATOR.createFromParcel(parcel);
                    } else {
                        intentCreateFromParcel2 = null;
                    }
                    boolean zNavigateUpTo = navigateUpTo(strongBinder3, intentCreateFromParcel2, parcel.readInt(), parcel.readInt() != 0 ? Intent.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    parcel2.writeInt(zNavigateUpTo ? 1 : 0);
                    return true;
                case 145:
                    parcel.enforceInterface(DESCRIPTOR);
                    setLockScreenShown(parcel.readInt() != 0, parcel.readInt() != 0, parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 146:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zFinishActivityAffinity = finishActivityAffinity(parcel.readStrongBinder());
                    parcel2.writeNoException();
                    parcel2.writeInt(zFinishActivityAffinity ? 1 : 0);
                    return true;
                case 147:
                    parcel.enforceInterface(DESCRIPTOR);
                    int launchedFromUid = getLaunchedFromUid(parcel.readStrongBinder());
                    parcel2.writeNoException();
                    parcel2.writeInt(launchedFromUid);
                    return true;
                case 148:
                    parcel.enforceInterface(DESCRIPTOR);
                    unstableProviderDied(parcel.readStrongBinder());
                    parcel2.writeNoException();
                    return true;
                case 149:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zIsIntentSenderAnActivity = isIntentSenderAnActivity(IIntentSender.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsIntentSenderAnActivity ? 1 : 0);
                    return true;
                case 150:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zIsIntentSenderAForegroundService = isIntentSenderAForegroundService(IIntentSender.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsIntentSenderAForegroundService ? 1 : 0);
                    return true;
                case 151:
                    return onTransact$startActivityAsUser$(parcel, parcel2);
                case 152:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iStopUser = stopUser(parcel.readInt(), parcel.readInt() != 0, IStopUserCallback.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    parcel2.writeInt(iStopUser);
                    return true;
                case 153:
                    parcel.enforceInterface(DESCRIPTOR);
                    registerUserSwitchObserver(IUserSwitchObserver.Stub.asInterface(parcel.readStrongBinder()), parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case 154:
                    parcel.enforceInterface(DESCRIPTOR);
                    unregisterUserSwitchObserver(IUserSwitchObserver.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    return true;
                case 155:
                    parcel.enforceInterface(DESCRIPTOR);
                    int[] runningUserIds = getRunningUserIds();
                    parcel2.writeNoException();
                    parcel2.writeIntArray(runningUserIds);
                    return true;
                case 156:
                    parcel.enforceInterface(DESCRIPTOR);
                    requestBugReport(parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 157:
                    parcel.enforceInterface(DESCRIPTOR);
                    requestTelephonyBugReport(parcel.readString(), parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case 158:
                    parcel.enforceInterface(DESCRIPTOR);
                    requestWifiBugReport(parcel.readString(), parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case 159:
                    parcel.enforceInterface(DESCRIPTOR);
                    long jInputDispatchingTimedOut = inputDispatchingTimedOut(parcel.readInt(), parcel.readInt() != 0, parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeLong(jInputDispatchingTimedOut);
                    return true;
                case 160:
                    parcel.enforceInterface(DESCRIPTOR);
                    clearPendingBackup();
                    parcel2.writeNoException();
                    return true;
                case 161:
                    parcel.enforceInterface(DESCRIPTOR);
                    Intent intentForIntentSender = getIntentForIntentSender(IIntentSender.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    if (intentForIntentSender != null) {
                        parcel2.writeInt(1);
                        intentForIntentSender.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 162:
                    parcel.enforceInterface(DESCRIPTOR);
                    Bundle assistContextExtras = getAssistContextExtras(parcel.readInt());
                    parcel2.writeNoException();
                    if (assistContextExtras != null) {
                        parcel2.writeInt(1);
                        assistContextExtras.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 163:
                    return onTransact$reportAssistContextExtras$(parcel, parcel2);
                case 164:
                    parcel.enforceInterface(DESCRIPTOR);
                    String launchedFromPackage = getLaunchedFromPackage(parcel.readStrongBinder());
                    parcel2.writeNoException();
                    parcel2.writeString(launchedFromPackage);
                    return true;
                case 165:
                    parcel.enforceInterface(DESCRIPTOR);
                    killUid(parcel.readInt(), parcel.readInt(), parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case 166:
                    parcel.enforceInterface(DESCRIPTOR);
                    setUserIsMonkey(parcel.readInt() != 0);
                    parcel2.writeNoException();
                    return true;
                case 167:
                    parcel.enforceInterface(DESCRIPTOR);
                    hang(parcel.readStrongBinder(), parcel.readInt() != 0);
                    parcel2.writeNoException();
                    return true;
                case 168:
                    parcel.enforceInterface(DESCRIPTOR);
                    setTaskWindowingMode(parcel.readInt(), parcel.readInt(), parcel.readInt() != 0);
                    parcel2.writeNoException();
                    return true;
                case 169:
                    parcel.enforceInterface(DESCRIPTOR);
                    moveTaskToStack(parcel.readInt(), parcel.readInt(), parcel.readInt() != 0);
                    parcel2.writeNoException();
                    return true;
                case 170:
                    return onTransact$resizeStack$(parcel, parcel2);
                case 171:
                    parcel.enforceInterface(DESCRIPTOR);
                    List<ActivityManager.StackInfo> allStackInfos = getAllStackInfos();
                    parcel2.writeNoException();
                    parcel2.writeTypedList(allStackInfos);
                    return true;
                case 172:
                    parcel.enforceInterface(DESCRIPTOR);
                    setFocusedStack(parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 173:
                    parcel.enforceInterface(DESCRIPTOR);
                    ActivityManager.StackInfo focusedStackInfo = getFocusedStackInfo();
                    parcel2.writeNoException();
                    if (focusedStackInfo != null) {
                        parcel2.writeInt(1);
                        focusedStackInfo.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 174:
                    parcel.enforceInterface(DESCRIPTOR);
                    ActivityManager.StackInfo stackInfo = getStackInfo(parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    if (stackInfo != null) {
                        parcel2.writeInt(1);
                        stackInfo.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 175:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zConvertFromTranslucent = convertFromTranslucent(parcel.readStrongBinder());
                    parcel2.writeNoException();
                    parcel2.writeInt(zConvertFromTranslucent ? 1 : 0);
                    return true;
                case 176:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zConvertToTranslucent = convertToTranslucent(parcel.readStrongBinder(), parcel.readInt() != 0 ? Bundle.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    parcel2.writeInt(zConvertToTranslucent ? 1 : 0);
                    return true;
                case 177:
                    parcel.enforceInterface(DESCRIPTOR);
                    notifyActivityDrawn(parcel.readStrongBinder());
                    parcel2.writeNoException();
                    return true;
                case 178:
                    parcel.enforceInterface(DESCRIPTOR);
                    reportActivityFullyDrawn(parcel.readStrongBinder(), parcel.readInt() != 0);
                    parcel2.writeNoException();
                    return true;
                case 179:
                    parcel.enforceInterface(DESCRIPTOR);
                    restart();
                    parcel2.writeNoException();
                    return true;
                case 180:
                    parcel.enforceInterface(DESCRIPTOR);
                    performIdleMaintenance();
                    parcel2.writeNoException();
                    return true;
                case 181:
                    parcel.enforceInterface(DESCRIPTOR);
                    takePersistableUriPermission(parcel.readInt() != 0 ? Uri.CREATOR.createFromParcel(parcel) : null, parcel.readInt(), parcel.readString(), parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 182:
                    parcel.enforceInterface(DESCRIPTOR);
                    releasePersistableUriPermission(parcel.readInt() != 0 ? Uri.CREATOR.createFromParcel(parcel) : null, parcel.readInt(), parcel.readString(), parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 183:
                    parcel.enforceInterface(DESCRIPTOR);
                    ParceledListSlice persistedUriPermissions = getPersistedUriPermissions(parcel.readString(), parcel.readInt() != 0);
                    parcel2.writeNoException();
                    if (persistedUriPermissions != null) {
                        parcel2.writeInt(1);
                        persistedUriPermissions.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 184:
                    parcel.enforceInterface(DESCRIPTOR);
                    appNotRespondingViaProvider(parcel.readStrongBinder());
                    parcel2.writeNoException();
                    return true;
                case 185:
                    parcel.enforceInterface(DESCRIPTOR);
                    Rect taskBounds = getTaskBounds(parcel.readInt());
                    parcel2.writeNoException();
                    if (taskBounds != null) {
                        parcel2.writeInt(1);
                        taskBounds.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 186:
                    parcel.enforceInterface(DESCRIPTOR);
                    int activityDisplayId = getActivityDisplayId(parcel.readStrongBinder());
                    parcel2.writeNoException();
                    parcel2.writeInt(activityDisplayId);
                    return true;
                case 187:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean processMemoryTrimLevel = setProcessMemoryTrimLevel(parcel.readString(), parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(processMemoryTrimLevel ? 1 : 0);
                    return true;
                case 188:
                    parcel.enforceInterface(DESCRIPTOR);
                    String tagForIntentSender = getTagForIntentSender(IIntentSender.Stub.asInterface(parcel.readStrongBinder()), parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeString(tagForIntentSender);
                    return true;
                case 189:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zStartUserInBackground = startUserInBackground(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(zStartUserInBackground ? 1 : 0);
                    return true;
                case 190:
                    parcel.enforceInterface(DESCRIPTOR);
                    startLockTaskModeByToken(parcel.readStrongBinder());
                    parcel2.writeNoException();
                    return true;
                case 191:
                    parcel.enforceInterface(DESCRIPTOR);
                    stopLockTaskModeByToken(parcel.readStrongBinder());
                    parcel2.writeNoException();
                    return true;
                case 192:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zIsInLockTaskMode = isInLockTaskMode();
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsInLockTaskMode ? 1 : 0);
                    return true;
                case 193:
                    parcel.enforceInterface(DESCRIPTOR);
                    setTaskDescription(parcel.readStrongBinder(), parcel.readInt() != 0 ? ActivityManager.TaskDescription.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    return true;
                case 194:
                    return onTransact$startVoiceActivity$(parcel, parcel2);
                case 195:
                    return onTransact$startAssistantActivity$(parcel, parcel2);
                case 196:
                    parcel.enforceInterface(DESCRIPTOR);
                    startRecentsActivity(parcel.readInt() != 0 ? Intent.CREATOR.createFromParcel(parcel) : null, IAssistDataReceiver.Stub.asInterface(parcel.readStrongBinder()), IRecentsAnimationRunner.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    return true;
                case 197:
                    parcel.enforceInterface(DESCRIPTOR);
                    cancelRecentsAnimation(parcel.readInt() != 0);
                    parcel2.writeNoException();
                    return true;
                case 198:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iStartActivityFromRecents = startActivityFromRecents(parcel.readInt(), parcel.readInt() != 0 ? Bundle.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    parcel2.writeInt(iStartActivityFromRecents);
                    return true;
                case 199:
                    parcel.enforceInterface(DESCRIPTOR);
                    Bundle activityOptions = getActivityOptions(parcel.readStrongBinder());
                    parcel2.writeNoException();
                    if (activityOptions != null) {
                        parcel2.writeInt(1);
                        activityOptions.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 200:
                    parcel.enforceInterface(DESCRIPTOR);
                    List<IBinder> appTasks = getAppTasks(parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeBinderList(appTasks);
                    return true;
                case 201:
                    parcel.enforceInterface(DESCRIPTOR);
                    startSystemLockTaskMode(parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 202:
                    parcel.enforceInterface(DESCRIPTOR);
                    stopSystemLockTaskMode();
                    parcel2.writeNoException();
                    return true;
                case 203:
                    parcel.enforceInterface(DESCRIPTOR);
                    finishVoiceTask(IVoiceInteractionSession.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    return true;
                case 204:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zIsTopOfTask = isTopOfTask(parcel.readStrongBinder());
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsTopOfTask ? 1 : 0);
                    return true;
                case 205:
                    parcel.enforceInterface(DESCRIPTOR);
                    notifyLaunchTaskBehindComplete(parcel.readStrongBinder());
                    parcel2.writeNoException();
                    return true;
                case 206:
                    parcel.enforceInterface(DESCRIPTOR);
                    notifyEnterAnimationComplete(parcel.readStrongBinder());
                    parcel2.writeNoException();
                    return true;
                case 207:
                    return onTransact$startActivityAsCaller$(parcel, parcel2);
                case 208:
                    parcel.enforceInterface(DESCRIPTOR);
                    IBinder strongBinder4 = parcel.readStrongBinder();
                    if (parcel.readInt() != 0) {
                        intentCreateFromParcel3 = Intent.CREATOR.createFromParcel(parcel);
                    } else {
                        intentCreateFromParcel3 = null;
                    }
                    if (parcel.readInt() != 0) {
                        taskDescriptionCreateFromParcel = ActivityManager.TaskDescription.CREATOR.createFromParcel(parcel);
                    } else {
                        taskDescriptionCreateFromParcel = null;
                    }
                    int iAddAppTask = addAppTask(strongBinder4, intentCreateFromParcel3, taskDescriptionCreateFromParcel, parcel.readInt() != 0 ? Bitmap.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    parcel2.writeInt(iAddAppTask);
                    return true;
                case 209:
                    parcel.enforceInterface(DESCRIPTOR);
                    Point appTaskThumbnailSize = getAppTaskThumbnailSize();
                    parcel2.writeNoException();
                    if (appTaskThumbnailSize != null) {
                        parcel2.writeInt(1);
                        appTaskThumbnailSize.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 210:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zReleaseActivityInstance = releaseActivityInstance(parcel.readStrongBinder());
                    parcel2.writeNoException();
                    parcel2.writeInt(zReleaseActivityInstance ? 1 : 0);
                    return true;
                case 211:
                    parcel.enforceInterface(DESCRIPTOR);
                    releaseSomeActivities(IApplicationThread.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    return true;
                case 212:
                    parcel.enforceInterface(DESCRIPTOR);
                    bootAnimationComplete();
                    parcel2.writeNoException();
                    return true;
                case 213:
                    parcel.enforceInterface(DESCRIPTOR);
                    Bitmap taskDescriptionIcon = getTaskDescriptionIcon(parcel.readString(), parcel.readInt());
                    parcel2.writeNoException();
                    if (taskDescriptionIcon != null) {
                        parcel2.writeInt(1);
                        taskDescriptionIcon.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 214:
                    return onTransact$launchAssistIntent$(parcel, parcel2);
                case 215:
                    parcel.enforceInterface(DESCRIPTOR);
                    startInPlaceAnimationOnFrontMostApplication(parcel.readInt() != 0 ? Bundle.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    return true;
                case 216:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iCheckPermissionWithToken = checkPermissionWithToken(parcel.readString(), parcel.readInt(), parcel.readInt(), parcel.readStrongBinder());
                    parcel2.writeNoException();
                    parcel2.writeInt(iCheckPermissionWithToken);
                    return true;
                case 217:
                    parcel.enforceInterface(DESCRIPTOR);
                    registerTaskStackListener(ITaskStackListener.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    return true;
                case 218:
                    parcel.enforceInterface(DESCRIPTOR);
                    unregisterTaskStackListener(ITaskStackListener.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    return true;
                case 219:
                    parcel.enforceInterface(DESCRIPTOR);
                    notifyCleartextNetwork(parcel.readInt(), parcel.createByteArray());
                    parcel2.writeNoException();
                    return true;
                case 220:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iCreateStackOnDisplay = createStackOnDisplay(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(iCreateStackOnDisplay);
                    return true;
                case 221:
                    parcel.enforceInterface(DESCRIPTOR);
                    setTaskResizeable(parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 222:
                    return onTransact$requestAssistContextExtras$(parcel, parcel2);
                case 223:
                    parcel.enforceInterface(DESCRIPTOR);
                    resizeTask(parcel.readInt(), parcel.readInt() != 0 ? Rect.CREATOR.createFromParcel(parcel) : null, parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 224:
                    parcel.enforceInterface(DESCRIPTOR);
                    int lockTaskModeState = getLockTaskModeState();
                    parcel2.writeNoException();
                    parcel2.writeInt(lockTaskModeState);
                    return true;
                case 225:
                    parcel.enforceInterface(DESCRIPTOR);
                    setDumpHeapDebugLimit(parcel.readString(), parcel.readInt(), parcel.readLong(), parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case 226:
                    parcel.enforceInterface(DESCRIPTOR);
                    dumpHeapFinished(parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case 227:
                    parcel.enforceInterface(DESCRIPTOR);
                    setVoiceKeepAwake(IVoiceInteractionSession.Stub.asInterface(parcel.readStrongBinder()), parcel.readInt() != 0);
                    parcel2.writeNoException();
                    return true;
                case 228:
                    parcel.enforceInterface(DESCRIPTOR);
                    updateLockTaskPackages(parcel.readInt(), parcel.createStringArray());
                    parcel2.writeNoException();
                    return true;
                case 229:
                    parcel.enforceInterface(DESCRIPTOR);
                    noteAlarmStart(IIntentSender.Stub.asInterface(parcel.readStrongBinder()), parcel.readInt() != 0 ? WorkSource.CREATOR.createFromParcel(parcel) : null, parcel.readInt(), parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case 230:
                    parcel.enforceInterface(DESCRIPTOR);
                    noteAlarmFinish(IIntentSender.Stub.asInterface(parcel.readStrongBinder()), parcel.readInt() != 0 ? WorkSource.CREATOR.createFromParcel(parcel) : null, parcel.readInt(), parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case 231:
                    parcel.enforceInterface(DESCRIPTOR);
                    int packageProcessState = getPackageProcessState(parcel.readString(), parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeInt(packageProcessState);
                    return true;
                case 232:
                    parcel.enforceInterface(DESCRIPTOR);
                    showLockTaskEscapeMessage(parcel.readStrongBinder());
                    return true;
                case 233:
                    parcel.enforceInterface(DESCRIPTOR);
                    updateDeviceOwner(parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case 234:
                    parcel.enforceInterface(DESCRIPTOR);
                    keyguardGoingAway(parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 235:
                    parcel.enforceInterface(DESCRIPTOR);
                    int uidProcessState = getUidProcessState(parcel.readInt(), parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeInt(uidProcessState);
                    return true;
                case 236:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zIsAssistDataAllowedOnCurrentActivity = isAssistDataAllowedOnCurrentActivity();
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsAssistDataAllowedOnCurrentActivity ? 1 : 0);
                    return true;
                case 237:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zShowAssistFromActivity = showAssistFromActivity(parcel.readStrongBinder(), parcel.readInt() != 0 ? Bundle.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    parcel2.writeInt(zShowAssistFromActivity ? 1 : 0);
                    return true;
                case 238:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zIsRootVoiceInteraction = isRootVoiceInteraction(parcel.readStrongBinder());
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsRootVoiceInteraction ? 1 : 0);
                    return true;
                case 239:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zStartBinderTracking = startBinderTracking();
                    parcel2.writeNoException();
                    parcel2.writeInt(zStartBinderTracking ? 1 : 0);
                    return true;
                case 240:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zStopBinderTrackingAndDump = stopBinderTrackingAndDump(parcel.readInt() != 0 ? ParcelFileDescriptor.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    parcel2.writeInt(zStopBinderTrackingAndDump ? 1 : 0);
                    return true;
                case 241:
                    parcel.enforceInterface(DESCRIPTOR);
                    positionTaskInStack(parcel.readInt(), parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 242:
                    parcel.enforceInterface(DESCRIPTOR);
                    exitFreeformMode(parcel.readStrongBinder());
                    parcel2.writeNoException();
                    return true;
                case 243:
                    parcel.enforceInterface(DESCRIPTOR);
                    reportSizeConfigurations(parcel.readStrongBinder(), parcel.createIntArray(), parcel.createIntArray(), parcel.createIntArray());
                    parcel2.writeNoException();
                    return true;
                case 244:
                    return onTransact$setTaskWindowingModeSplitScreenPrimary$(parcel, parcel2);
                case 245:
                    parcel.enforceInterface(DESCRIPTOR);
                    dismissSplitScreenMode(parcel.readInt() != 0);
                    parcel2.writeNoException();
                    return true;
                case 246:
                    parcel.enforceInterface(DESCRIPTOR);
                    dismissPip(parcel.readInt() != 0, parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 247:
                    parcel.enforceInterface(DESCRIPTOR);
                    suppressResizeConfigChanges(parcel.readInt() != 0);
                    parcel2.writeNoException();
                    return true;
                case 248:
                    parcel.enforceInterface(DESCRIPTOR);
                    moveTasksToFullscreenStack(parcel.readInt(), parcel.readInt() != 0);
                    parcel2.writeNoException();
                    return true;
                case 249:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zMoveTopActivityToPinnedStack = moveTopActivityToPinnedStack(parcel.readInt(), parcel.readInt() != 0 ? Rect.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    parcel2.writeInt(zMoveTopActivityToPinnedStack ? 1 : 0);
                    return true;
                case 250:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zIsAppStartModeDisabled = isAppStartModeDisabled(parcel.readInt(), parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsAppStartModeDisabled ? 1 : 0);
                    return true;
                case 251:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zUnlockUser = unlockUser(parcel.readInt(), parcel.createByteArray(), parcel.createByteArray(), IProgressListener.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    parcel2.writeInt(zUnlockUser ? 1 : 0);
                    return true;
                case 252:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zIsInMultiWindowMode = isInMultiWindowMode(parcel.readStrongBinder());
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsInMultiWindowMode ? 1 : 0);
                    return true;
                case 253:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zIsInPictureInPictureMode = isInPictureInPictureMode(parcel.readStrongBinder());
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsInPictureInPictureMode ? 1 : 0);
                    return true;
                case 254:
                    parcel.enforceInterface(DESCRIPTOR);
                    killPackageDependents(parcel.readString(), parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 255:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zEnterPictureInPictureMode = enterPictureInPictureMode(parcel.readStrongBinder(), parcel.readInt() != 0 ? PictureInPictureParams.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    parcel2.writeInt(zEnterPictureInPictureMode ? 1 : 0);
                    return true;
                case 256:
                    parcel.enforceInterface(DESCRIPTOR);
                    setPictureInPictureParams(parcel.readStrongBinder(), parcel.readInt() != 0 ? PictureInPictureParams.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    return true;
                case 257:
                    parcel.enforceInterface(DESCRIPTOR);
                    int maxNumPictureInPictureActions = getMaxNumPictureInPictureActions(parcel.readStrongBinder());
                    parcel2.writeNoException();
                    parcel2.writeInt(maxNumPictureInPictureActions);
                    return true;
                case 258:
                    parcel.enforceInterface(DESCRIPTOR);
                    activityRelaunched(parcel.readStrongBinder());
                    parcel2.writeNoException();
                    return true;
                case 259:
                    parcel.enforceInterface(DESCRIPTOR);
                    IBinder uriPermissionOwnerForActivity = getUriPermissionOwnerForActivity(parcel.readStrongBinder());
                    parcel2.writeNoException();
                    parcel2.writeStrongBinder(uriPermissionOwnerForActivity);
                    return true;
                case 260:
                    return onTransact$resizeDockedStack$(parcel, parcel2);
                case 261:
                    parcel.enforceInterface(DESCRIPTOR);
                    setSplitScreenResizing(parcel.readInt() != 0);
                    parcel2.writeNoException();
                    return true;
                case 262:
                    parcel.enforceInterface(DESCRIPTOR);
                    int vrMode = setVrMode(parcel.readStrongBinder(), parcel.readInt() != 0, parcel.readInt() != 0 ? ComponentName.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    parcel2.writeInt(vrMode);
                    return true;
                case 263:
                    parcel.enforceInterface(DESCRIPTOR);
                    ParceledListSlice grantedUriPermissions = getGrantedUriPermissions(parcel.readString(), parcel.readInt());
                    parcel2.writeNoException();
                    if (grantedUriPermissions != null) {
                        parcel2.writeInt(1);
                        grantedUriPermissions.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 264:
                    parcel.enforceInterface(DESCRIPTOR);
                    clearGrantedUriPermissions(parcel.readString(), parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 265:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zIsAppForeground = isAppForeground(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsAppForeground ? 1 : 0);
                    return true;
                case 266:
                    parcel.enforceInterface(DESCRIPTOR);
                    startLocalVoiceInteraction(parcel.readStrongBinder(), parcel.readInt() != 0 ? Bundle.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    return true;
                case 267:
                    parcel.enforceInterface(DESCRIPTOR);
                    stopLocalVoiceInteraction(parcel.readStrongBinder());
                    parcel2.writeNoException();
                    return true;
                case 268:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zSupportsLocalVoiceInteraction = supportsLocalVoiceInteraction();
                    parcel2.writeNoException();
                    parcel2.writeInt(zSupportsLocalVoiceInteraction ? 1 : 0);
                    return true;
                case 269:
                    parcel.enforceInterface(DESCRIPTOR);
                    notifyPinnedStackAnimationStarted();
                    parcel2.writeNoException();
                    return true;
                case 270:
                    parcel.enforceInterface(DESCRIPTOR);
                    notifyPinnedStackAnimationEnded();
                    parcel2.writeNoException();
                    return true;
                case 271:
                    parcel.enforceInterface(DESCRIPTOR);
                    removeStack(parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 272:
                    parcel.enforceInterface(DESCRIPTOR);
                    removeStacksInWindowingModes(parcel.createIntArray());
                    parcel2.writeNoException();
                    return true;
                case 273:
                    parcel.enforceInterface(DESCRIPTOR);
                    removeStacksWithActivityTypes(parcel.createIntArray());
                    parcel2.writeNoException();
                    return true;
                case 274:
                    parcel.enforceInterface(DESCRIPTOR);
                    makePackageIdle(parcel.readString(), parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 275:
                    parcel.enforceInterface(DESCRIPTOR);
                    int memoryTrimLevel = getMemoryTrimLevel();
                    parcel2.writeNoException();
                    parcel2.writeInt(memoryTrimLevel);
                    return true;
                case 276:
                    parcel.enforceInterface(DESCRIPTOR);
                    if (parcel.readInt() != 0) {
                        rectCreateFromParcel = Rect.CREATOR.createFromParcel(parcel);
                    } else {
                        rectCreateFromParcel = null;
                    }
                    resizePinnedStack(rectCreateFromParcel, parcel.readInt() != 0 ? Rect.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    return true;
                case 277:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zIsVrModePackageEnabled = isVrModePackageEnabled(parcel.readInt() != 0 ? ComponentName.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsVrModePackageEnabled ? 1 : 0);
                    return true;
                case 278:
                    parcel.enforceInterface(DESCRIPTOR);
                    notifyLockedProfile(parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 279:
                    parcel.enforceInterface(DESCRIPTOR);
                    if (parcel.readInt() != 0) {
                        intentCreateFromParcel4 = Intent.CREATOR.createFromParcel(parcel);
                    } else {
                        intentCreateFromParcel4 = null;
                    }
                    startConfirmDeviceCredentialIntent(intentCreateFromParcel4, parcel.readInt() != 0 ? Bundle.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    return true;
                case 280:
                    parcel.enforceInterface(DESCRIPTOR);
                    sendIdleJobTrigger();
                    parcel2.writeNoException();
                    return true;
                case 281:
                    return onTransact$sendIntentSender$(parcel, parcel2);
                case 282:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zIsBackgroundRestricted = isBackgroundRestricted(parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsBackgroundRestricted ? 1 : 0);
                    return true;
                case 283:
                    parcel.enforceInterface(DESCRIPTOR);
                    setVrThread(parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 284:
                    parcel.enforceInterface(DESCRIPTOR);
                    setRenderThread(parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 285:
                    parcel.enforceInterface(DESCRIPTOR);
                    setHasTopUi(parcel.readInt() != 0);
                    parcel2.writeNoException();
                    return true;
                case 286:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zUpdateDisplayOverrideConfiguration = updateDisplayOverrideConfiguration(parcel.readInt() != 0 ? Configuration.CREATOR.createFromParcel(parcel) : null, parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(zUpdateDisplayOverrideConfiguration ? 1 : 0);
                    return true;
                case 287:
                    parcel.enforceInterface(DESCRIPTOR);
                    moveStackToDisplay(parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 288:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zRequestAutofillData = requestAutofillData(IAssistDataReceiver.Stub.asInterface(parcel.readStrongBinder()), parcel.readInt() != 0 ? Bundle.CREATOR.createFromParcel(parcel) : null, parcel.readStrongBinder(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(zRequestAutofillData ? 1 : 0);
                    return true;
                case 289:
                    parcel.enforceInterface(DESCRIPTOR);
                    dismissKeyguard(parcel.readStrongBinder(), IKeyguardDismissCallback.Stub.asInterface(parcel.readStrongBinder()), parcel.readInt() != 0 ? TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    return true;
                case 290:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iRestartUserInBackground = restartUserInBackground(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(iRestartUserInBackground);
                    return true;
                case 291:
                    parcel.enforceInterface(DESCRIPTOR);
                    cancelTaskWindowTransition(parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 292:
                    parcel.enforceInterface(DESCRIPTOR);
                    ActivityManager.TaskSnapshot taskSnapshot = getTaskSnapshot(parcel.readInt(), parcel.readInt() != 0);
                    parcel2.writeNoException();
                    if (taskSnapshot != null) {
                        parcel2.writeInt(1);
                        taskSnapshot.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 293:
                    parcel.enforceInterface(DESCRIPTOR);
                    scheduleApplicationInfoChanged(parcel.createStringArrayList(), parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 294:
                    parcel.enforceInterface(DESCRIPTOR);
                    setPersistentVrThread(parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 295:
                    parcel.enforceInterface(DESCRIPTOR);
                    waitForNetworkStateUpdate(parcel.readLong());
                    parcel2.writeNoException();
                    return true;
                case 296:
                    parcel.enforceInterface(DESCRIPTOR);
                    setDisablePreviewScreenshots(parcel.readStrongBinder(), parcel.readInt() != 0);
                    parcel2.writeNoException();
                    return true;
                case 297:
                    parcel.enforceInterface(DESCRIPTOR);
                    int lastResumedActivityUserId = getLastResumedActivityUserId();
                    parcel2.writeNoException();
                    parcel2.writeInt(lastResumedActivityUserId);
                    return true;
                case 298:
                    parcel.enforceInterface(DESCRIPTOR);
                    backgroundWhitelistUid(parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 299:
                    parcel.enforceInterface(DESCRIPTOR);
                    updateLockTaskFeatures(parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 300:
                    parcel.enforceInterface(DESCRIPTOR);
                    setShowWhenLocked(parcel.readStrongBinder(), parcel.readInt() != 0);
                    parcel2.writeNoException();
                    return true;
                case 301:
                    parcel.enforceInterface(DESCRIPTOR);
                    setTurnScreenOn(parcel.readStrongBinder(), parcel.readInt() != 0);
                    parcel2.writeNoException();
                    return true;
                case 302:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zStartUserInBackgroundWithListener = startUserInBackgroundWithListener(parcel.readInt(), IProgressListener.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    parcel2.writeInt(zStartUserInBackgroundWithListener ? 1 : 0);
                    return true;
                case 303:
                    parcel.enforceInterface(DESCRIPTOR);
                    registerRemoteAnimations(parcel.readStrongBinder(), parcel.readInt() != 0 ? RemoteAnimationDefinition.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    return true;
                case 304:
                    parcel.enforceInterface(DESCRIPTOR);
                    registerRemoteAnimationForNextActivityStart(parcel.readString(), parcel.readInt() != 0 ? RemoteAnimationAdapter.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    return true;
                case 305:
                    parcel.enforceInterface(DESCRIPTOR);
                    alwaysShowUnsupportedCompileSdkWarning(parcel.readInt() != 0 ? ComponentName.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    return true;
                case 306:
                    parcel.enforceInterface(DESCRIPTOR);
                    setAalMode(parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 307:
                    parcel.enforceInterface(DESCRIPTOR);
                    setAalEnabled(parcel.readInt() != 0);
                    parcel2.writeNoException();
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }

        private static class Proxy implements IActivityManager {
            private IBinder mRemote;

            Proxy(IBinder iBinder) {
                this.mRemote = iBinder;
            }

            @Override
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return Stub.DESCRIPTOR;
            }

            @Override
            public ParcelFileDescriptor openContentUri(String str) throws RemoteException {
                ParcelFileDescriptor parcelFileDescriptorCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(1, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        parcelFileDescriptorCreateFromParcel = ParcelFileDescriptor.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        parcelFileDescriptorCreateFromParcel = null;
                    }
                    return parcelFileDescriptorCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void registerUidObserver(IUidObserver iUidObserver, int i, int i2, String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iUidObserver != null ? iUidObserver.asBinder() : null);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(2, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void unregisterUidObserver(IUidObserver iUidObserver) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iUidObserver != null ? iUidObserver.asBinder() : null);
                    this.mRemote.transact(3, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isUidActive(int i, String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(4, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void handleApplicationCrash(IBinder iBinder, ApplicationErrorReport.ParcelableCrashInfo parcelableCrashInfo) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    if (parcelableCrashInfo != null) {
                        parcelObtain.writeInt(1);
                        parcelableCrashInfo.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(5, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int startActivity(IApplicationThread iApplicationThread, String str, Intent intent, String str2, IBinder iBinder, String str3, int i, int i2, ProfilerInfo profilerInfo, Bundle bundle) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iApplicationThread != null ? iApplicationThread.asBinder() : null);
                    parcelObtain.writeString(str);
                    if (intent != null) {
                        parcelObtain.writeInt(1);
                        intent.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeString(str2);
                    parcelObtain.writeStrongBinder(iBinder);
                    parcelObtain.writeString(str3);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    if (profilerInfo != null) {
                        parcelObtain.writeInt(1);
                        profilerInfo.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    if (bundle != null) {
                        parcelObtain.writeInt(1);
                        bundle.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(6, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void unhandledBack() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(7, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean finishActivity(IBinder iBinder, int i, Intent intent, int i2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    parcelObtain.writeInt(i);
                    boolean z = true;
                    if (intent != null) {
                        parcelObtain.writeInt(1);
                        intent.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(8, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() == 0) {
                        z = false;
                    }
                    return z;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public Intent registerReceiver(IApplicationThread iApplicationThread, String str, IIntentReceiver iIntentReceiver, IntentFilter intentFilter, String str2, int i, int i2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    Intent intentCreateFromParcel = null;
                    parcelObtain.writeStrongBinder(iApplicationThread != null ? iApplicationThread.asBinder() : null);
                    parcelObtain.writeString(str);
                    parcelObtain.writeStrongBinder(iIntentReceiver != null ? iIntentReceiver.asBinder() : null);
                    if (intentFilter != null) {
                        parcelObtain.writeInt(1);
                        intentFilter.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeString(str2);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(9, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        intentCreateFromParcel = Intent.CREATOR.createFromParcel(parcelObtain2);
                    }
                    return intentCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void unregisterReceiver(IIntentReceiver iIntentReceiver) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iIntentReceiver != null ? iIntentReceiver.asBinder() : null);
                    this.mRemote.transact(10, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int broadcastIntent(IApplicationThread iApplicationThread, Intent intent, String str, IIntentReceiver iIntentReceiver, int i, String str2, Bundle bundle, String[] strArr, int i2, Bundle bundle2, boolean z, boolean z2, int i3) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iApplicationThread != null ? iApplicationThread.asBinder() : null);
                    if (intent != null) {
                        parcelObtain.writeInt(1);
                        intent.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeString(str);
                    parcelObtain.writeStrongBinder(iIntentReceiver != null ? iIntentReceiver.asBinder() : null);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeString(str2);
                    if (bundle != null) {
                        parcelObtain.writeInt(1);
                        bundle.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeStringArray(strArr);
                    parcelObtain.writeInt(i2);
                    if (bundle2 != null) {
                        parcelObtain.writeInt(1);
                        bundle2.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeInt(z ? 1 : 0);
                    parcelObtain.writeInt(z2 ? 1 : 0);
                    parcelObtain.writeInt(i3);
                    this.mRemote.transact(11, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void unbroadcastIntent(IApplicationThread iApplicationThread, Intent intent, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iApplicationThread != null ? iApplicationThread.asBinder() : null);
                    if (intent != null) {
                        parcelObtain.writeInt(1);
                        intent.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(12, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void finishReceiver(IBinder iBinder, int i, String str, Bundle bundle, boolean z, int i2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeString(str);
                    if (bundle != null) {
                        parcelObtain.writeInt(1);
                        bundle.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeInt(z ? 1 : 0);
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(13, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void attachApplication(IApplicationThread iApplicationThread, long j) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iApplicationThread != null ? iApplicationThread.asBinder() : null);
                    parcelObtain.writeLong(j);
                    this.mRemote.transact(14, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void activityIdle(IBinder iBinder, Configuration configuration, boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    if (configuration != null) {
                        parcelObtain.writeInt(1);
                        configuration.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(15, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void activityPaused(IBinder iBinder) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    this.mRemote.transact(16, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void activityStopped(IBinder iBinder, Bundle bundle, PersistableBundle persistableBundle, CharSequence charSequence) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    if (bundle != null) {
                        parcelObtain.writeInt(1);
                        bundle.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    if (persistableBundle != null) {
                        parcelObtain.writeInt(1);
                        persistableBundle.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    if (charSequence != null) {
                        parcelObtain.writeInt(1);
                        TextUtils.writeToParcel(charSequence, parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(17, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public String getCallingPackage(IBinder iBinder) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    this.mRemote.transact(18, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readString();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public ComponentName getCallingActivity(IBinder iBinder) throws RemoteException {
                ComponentName componentNameCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    this.mRemote.transact(19, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        componentNameCreateFromParcel = ComponentName.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        componentNameCreateFromParcel = null;
                    }
                    return componentNameCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public List<ActivityManager.RunningTaskInfo> getTasks(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(20, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.createTypedArrayList(ActivityManager.RunningTaskInfo.CREATOR);
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public List<ActivityManager.RunningTaskInfo> getFilteredTasks(int i, int i2, int i3) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeInt(i3);
                    this.mRemote.transact(21, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.createTypedArrayList(ActivityManager.RunningTaskInfo.CREATOR);
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void moveTaskToFront(int i, int i2, Bundle bundle) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    if (bundle != null) {
                        parcelObtain.writeInt(1);
                        bundle.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(22, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void moveTaskBackwards(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(23, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int getTaskForActivity(IBinder iBinder, boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(24, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public ContentProviderHolder getContentProvider(IApplicationThread iApplicationThread, String str, int i, boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    ContentProviderHolder contentProviderHolderCreateFromParcel = null;
                    parcelObtain.writeStrongBinder(iApplicationThread != null ? iApplicationThread.asBinder() : null);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(25, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        contentProviderHolderCreateFromParcel = ContentProviderHolder.CREATOR.createFromParcel(parcelObtain2);
                    }
                    return contentProviderHolderCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void publishContentProviders(IApplicationThread iApplicationThread, List<ContentProviderHolder> list) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iApplicationThread != null ? iApplicationThread.asBinder() : null);
                    parcelObtain.writeTypedList(list);
                    this.mRemote.transact(26, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean refContentProvider(IBinder iBinder, int i, int i2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(27, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void finishSubActivity(IBinder iBinder, String str, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(28, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public PendingIntent getRunningServiceControlPanel(ComponentName componentName) throws RemoteException {
                PendingIntent pendingIntentCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (componentName != null) {
                        parcelObtain.writeInt(1);
                        componentName.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(29, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        pendingIntentCreateFromParcel = PendingIntent.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        pendingIntentCreateFromParcel = null;
                    }
                    return pendingIntentCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public ComponentName startService(IApplicationThread iApplicationThread, Intent intent, String str, boolean z, String str2, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    ComponentName componentNameCreateFromParcel = null;
                    parcelObtain.writeStrongBinder(iApplicationThread != null ? iApplicationThread.asBinder() : null);
                    if (intent != null) {
                        parcelObtain.writeInt(1);
                        intent.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(z ? 1 : 0);
                    parcelObtain.writeString(str2);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(30, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        componentNameCreateFromParcel = ComponentName.CREATOR.createFromParcel(parcelObtain2);
                    }
                    return componentNameCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int stopService(IApplicationThread iApplicationThread, Intent intent, String str, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iApplicationThread != null ? iApplicationThread.asBinder() : null);
                    if (intent != null) {
                        parcelObtain.writeInt(1);
                        intent.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(31, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int bindService(IApplicationThread iApplicationThread, IBinder iBinder, Intent intent, String str, IServiceConnection iServiceConnection, int i, String str2, int i2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iApplicationThread != null ? iApplicationThread.asBinder() : null);
                    parcelObtain.writeStrongBinder(iBinder);
                    if (intent != null) {
                        parcelObtain.writeInt(1);
                        intent.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeString(str);
                    parcelObtain.writeStrongBinder(iServiceConnection != null ? iServiceConnection.asBinder() : null);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeString(str2);
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(32, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean unbindService(IServiceConnection iServiceConnection) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iServiceConnection != null ? iServiceConnection.asBinder() : null);
                    this.mRemote.transact(33, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void publishService(IBinder iBinder, Intent intent, IBinder iBinder2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    if (intent != null) {
                        parcelObtain.writeInt(1);
                        intent.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeStrongBinder(iBinder2);
                    this.mRemote.transact(34, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void activityResumed(IBinder iBinder) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    this.mRemote.transact(35, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setDebugApp(String str, boolean z, boolean z2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(z ? 1 : 0);
                    parcelObtain.writeInt(z2 ? 1 : 0);
                    this.mRemote.transact(36, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setAgentApp(String str, String str2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeString(str2);
                    this.mRemote.transact(37, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setAlwaysFinish(boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(38, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean startInstrumentation(ComponentName componentName, String str, int i, Bundle bundle, IInstrumentationWatcher iInstrumentationWatcher, IUiAutomationConnection iUiAutomationConnection, int i2, String str2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = true;
                    if (componentName != null) {
                        parcelObtain.writeInt(1);
                        componentName.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    if (bundle != null) {
                        parcelObtain.writeInt(1);
                        bundle.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeStrongBinder(iInstrumentationWatcher != null ? iInstrumentationWatcher.asBinder() : null);
                    parcelObtain.writeStrongBinder(iUiAutomationConnection != null ? iUiAutomationConnection.asBinder() : null);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeString(str2);
                    this.mRemote.transact(39, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() == 0) {
                        z = false;
                    }
                    return z;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void addInstrumentationResults(IApplicationThread iApplicationThread, Bundle bundle) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iApplicationThread != null ? iApplicationThread.asBinder() : null);
                    if (bundle != null) {
                        parcelObtain.writeInt(1);
                        bundle.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(40, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void finishInstrumentation(IApplicationThread iApplicationThread, int i, Bundle bundle) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iApplicationThread != null ? iApplicationThread.asBinder() : null);
                    parcelObtain.writeInt(i);
                    if (bundle != null) {
                        parcelObtain.writeInt(1);
                        bundle.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(41, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public Configuration getConfiguration() throws RemoteException {
                Configuration configurationCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(42, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        configurationCreateFromParcel = Configuration.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        configurationCreateFromParcel = null;
                    }
                    return configurationCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean updateConfiguration(Configuration configuration) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = true;
                    if (configuration != null) {
                        parcelObtain.writeInt(1);
                        configuration.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(43, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() == 0) {
                        z = false;
                    }
                    return z;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean stopServiceToken(ComponentName componentName, IBinder iBinder, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = true;
                    if (componentName != null) {
                        parcelObtain.writeInt(1);
                        componentName.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeStrongBinder(iBinder);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(44, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() == 0) {
                        z = false;
                    }
                    return z;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public ComponentName getActivityClassForToken(IBinder iBinder) throws RemoteException {
                ComponentName componentNameCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    this.mRemote.transact(45, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        componentNameCreateFromParcel = ComponentName.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        componentNameCreateFromParcel = null;
                    }
                    return componentNameCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public String getPackageForToken(IBinder iBinder) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    this.mRemote.transact(46, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readString();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setProcessLimit(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(47, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int getProcessLimit() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(48, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int checkPermission(String str, int i, int i2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(49, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int checkUriPermission(Uri uri, int i, int i2, int i3, int i4, IBinder iBinder) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (uri != null) {
                        parcelObtain.writeInt(1);
                        uri.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeInt(i3);
                    parcelObtain.writeInt(i4);
                    parcelObtain.writeStrongBinder(iBinder);
                    this.mRemote.transact(50, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void grantUriPermission(IApplicationThread iApplicationThread, String str, Uri uri, int i, int i2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iApplicationThread != null ? iApplicationThread.asBinder() : null);
                    parcelObtain.writeString(str);
                    if (uri != null) {
                        parcelObtain.writeInt(1);
                        uri.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(51, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void revokeUriPermission(IApplicationThread iApplicationThread, String str, Uri uri, int i, int i2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iApplicationThread != null ? iApplicationThread.asBinder() : null);
                    parcelObtain.writeString(str);
                    if (uri != null) {
                        parcelObtain.writeInt(1);
                        uri.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(52, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setActivityController(IActivityController iActivityController, boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iActivityController != null ? iActivityController.asBinder() : null);
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(53, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void showWaitingForDebugger(IApplicationThread iApplicationThread, boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iApplicationThread != null ? iApplicationThread.asBinder() : null);
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(54, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void signalPersistentProcesses(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(55, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public ParceledListSlice getRecentTasks(int i, int i2, int i3) throws RemoteException {
                ParceledListSlice parceledListSliceCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeInt(i3);
                    this.mRemote.transact(56, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        parceledListSliceCreateFromParcel = ParceledListSlice.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        parceledListSliceCreateFromParcel = null;
                    }
                    return parceledListSliceCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void serviceDoneExecuting(IBinder iBinder, int i, int i2, int i3) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeInt(i3);
                    this.mRemote.transact(57, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void activityDestroyed(IBinder iBinder) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    this.mRemote.transact(58, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public IIntentSender getIntentSender(int i, String str, IBinder iBinder, String str2, int i2, Intent[] intentArr, String[] strArr, int i3, Bundle bundle, int i4) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeString(str);
                    parcelObtain.writeStrongBinder(iBinder);
                    parcelObtain.writeString(str2);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeTypedArray(intentArr, 0);
                    parcelObtain.writeStringArray(strArr);
                    parcelObtain.writeInt(i3);
                    if (bundle != null) {
                        parcelObtain.writeInt(1);
                        bundle.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeInt(i4);
                    this.mRemote.transact(59, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return IIntentSender.Stub.asInterface(parcelObtain2.readStrongBinder());
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void cancelIntentSender(IIntentSender iIntentSender) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iIntentSender != null ? iIntentSender.asBinder() : null);
                    this.mRemote.transact(60, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public String getPackageForIntentSender(IIntentSender iIntentSender) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iIntentSender != null ? iIntentSender.asBinder() : null);
                    this.mRemote.transact(61, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readString();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void registerIntentSenderCancelListener(IIntentSender iIntentSender, IResultReceiver iResultReceiver) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iIntentSender != null ? iIntentSender.asBinder() : null);
                    parcelObtain.writeStrongBinder(iResultReceiver != null ? iResultReceiver.asBinder() : null);
                    this.mRemote.transact(62, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void unregisterIntentSenderCancelListener(IIntentSender iIntentSender, IResultReceiver iResultReceiver) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iIntentSender != null ? iIntentSender.asBinder() : null);
                    parcelObtain.writeStrongBinder(iResultReceiver != null ? iResultReceiver.asBinder() : null);
                    this.mRemote.transact(63, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void enterSafeMode() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(64, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean startNextMatchingActivity(IBinder iBinder, Intent intent, Bundle bundle) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    boolean z = true;
                    if (intent != null) {
                        parcelObtain.writeInt(1);
                        intent.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    if (bundle != null) {
                        parcelObtain.writeInt(1);
                        bundle.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(65, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() == 0) {
                        z = false;
                    }
                    return z;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void noteWakeupAlarm(IIntentSender iIntentSender, WorkSource workSource, int i, String str, String str2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iIntentSender != null ? iIntentSender.asBinder() : null);
                    if (workSource != null) {
                        parcelObtain.writeInt(1);
                        workSource.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeInt(i);
                    parcelObtain.writeString(str);
                    parcelObtain.writeString(str2);
                    this.mRemote.transact(66, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void removeContentProvider(IBinder iBinder, boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(67, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setRequestedOrientation(IBinder iBinder, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(68, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int getRequestedOrientation(IBinder iBinder) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    this.mRemote.transact(69, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void unbindFinished(IBinder iBinder, Intent intent, boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    if (intent != null) {
                        parcelObtain.writeInt(1);
                        intent.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(70, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setProcessImportant(IBinder iBinder, int i, boolean z, String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(z ? 1 : 0);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(71, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setServiceForeground(ComponentName componentName, IBinder iBinder, int i, Notification notification, int i2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (componentName != null) {
                        parcelObtain.writeInt(1);
                        componentName.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeStrongBinder(iBinder);
                    parcelObtain.writeInt(i);
                    if (notification != null) {
                        parcelObtain.writeInt(1);
                        notification.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(72, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean moveActivityTaskToBack(IBinder iBinder, boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(73, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void getMemoryInfo(ActivityManager.MemoryInfo memoryInfo) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(74, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        memoryInfo.readFromParcel(parcelObtain2);
                    }
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public List<ActivityManager.ProcessErrorStateInfo> getProcessesInErrorState() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(75, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.createTypedArrayList(ActivityManager.ProcessErrorStateInfo.CREATOR);
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean clearApplicationUserData(String str, boolean z, IPackageDataObserver iPackageDataObserver, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(z ? 1 : 0);
                    parcelObtain.writeStrongBinder(iPackageDataObserver != null ? iPackageDataObserver.asBinder() : null);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(76, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void forceStopPackage(String str, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(77, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean killPids(int[] iArr, String str, boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeIntArray(iArr);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(78, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public List<ActivityManager.RunningServiceInfo> getServices(int i, int i2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(79, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.createTypedArrayList(ActivityManager.RunningServiceInfo.CREATOR);
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public ActivityManager.TaskDescription getTaskDescription(int i) throws RemoteException {
                ActivityManager.TaskDescription taskDescriptionCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(80, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        taskDescriptionCreateFromParcel = ActivityManager.TaskDescription.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        taskDescriptionCreateFromParcel = null;
                    }
                    return taskDescriptionCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public List<ActivityManager.RunningAppProcessInfo> getRunningAppProcesses() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(81, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.createTypedArrayList(ActivityManager.RunningAppProcessInfo.CREATOR);
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public ConfigurationInfo getDeviceConfigurationInfo() throws RemoteException {
                ConfigurationInfo configurationInfoCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(82, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        configurationInfoCreateFromParcel = ConfigurationInfo.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        configurationInfoCreateFromParcel = null;
                    }
                    return configurationInfoCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public IBinder peekService(Intent intent, String str, String str2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (intent != null) {
                        parcelObtain.writeInt(1);
                        intent.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeString(str);
                    parcelObtain.writeString(str2);
                    this.mRemote.transact(83, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readStrongBinder();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean profileControl(String str, int i, boolean z, ProfilerInfo profilerInfo, int i2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(z ? 1 : 0);
                    boolean z2 = true;
                    if (profilerInfo != null) {
                        parcelObtain.writeInt(1);
                        profilerInfo.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(84, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() == 0) {
                        z2 = false;
                    }
                    return z2;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean shutdown(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(85, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void stopAppSwitches() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(86, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void resumeAppSwitches() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(87, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean bindBackupAgent(String str, int i, int i2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(88, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void backupAgentCreated(String str, IBinder iBinder) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeStrongBinder(iBinder);
                    this.mRemote.transact(89, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void unbindBackupAgent(ApplicationInfo applicationInfo) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (applicationInfo != null) {
                        parcelObtain.writeInt(1);
                        applicationInfo.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(90, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int getUidForIntentSender(IIntentSender iIntentSender) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iIntentSender != null ? iIntentSender.asBinder() : null);
                    this.mRemote.transact(91, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int handleIncomingUser(int i, int i2, int i3, boolean z, boolean z2, String str, String str2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeInt(i3);
                    parcelObtain.writeInt(z ? 1 : 0);
                    parcelObtain.writeInt(z2 ? 1 : 0);
                    parcelObtain.writeString(str);
                    parcelObtain.writeString(str2);
                    this.mRemote.transact(92, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void addPackageDependency(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(93, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void killApplication(String str, int i, int i2, String str2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeString(str2);
                    this.mRemote.transact(94, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void closeSystemDialogs(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(95, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public Debug.MemoryInfo[] getProcessMemoryInfo(int[] iArr) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeIntArray(iArr);
                    this.mRemote.transact(96, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return (Debug.MemoryInfo[]) parcelObtain2.createTypedArray(Debug.MemoryInfo.CREATOR);
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void killApplicationProcess(String str, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(97, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int startActivityIntentSender(IApplicationThread iApplicationThread, IIntentSender iIntentSender, IBinder iBinder, Intent intent, String str, IBinder iBinder2, String str2, int i, int i2, int i3, Bundle bundle) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iApplicationThread != null ? iApplicationThread.asBinder() : null);
                    parcelObtain.writeStrongBinder(iIntentSender != null ? iIntentSender.asBinder() : null);
                    parcelObtain.writeStrongBinder(iBinder);
                    if (intent != null) {
                        parcelObtain.writeInt(1);
                        intent.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeString(str);
                    parcelObtain.writeStrongBinder(iBinder2);
                    parcelObtain.writeString(str2);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeInt(i3);
                    if (bundle != null) {
                        parcelObtain.writeInt(1);
                        bundle.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(98, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void overridePendingTransition(IBinder iBinder, String str, int i, int i2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(99, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean handleApplicationWtf(IBinder iBinder, String str, boolean z, ApplicationErrorReport.ParcelableCrashInfo parcelableCrashInfo) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(z ? 1 : 0);
                    boolean z2 = true;
                    if (parcelableCrashInfo != null) {
                        parcelObtain.writeInt(1);
                        parcelableCrashInfo.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(100, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() == 0) {
                        z2 = false;
                    }
                    return z2;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void killBackgroundProcesses(String str, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(101, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isUserAMonkey() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(102, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public WaitResult startActivityAndWait(IApplicationThread iApplicationThread, String str, Intent intent, String str2, IBinder iBinder, String str3, int i, int i2, ProfilerInfo profilerInfo, Bundle bundle, int i3) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    WaitResult waitResultCreateFromParcel = null;
                    parcelObtain.writeStrongBinder(iApplicationThread != null ? iApplicationThread.asBinder() : null);
                    parcelObtain.writeString(str);
                    if (intent != null) {
                        parcelObtain.writeInt(1);
                        intent.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeString(str2);
                    parcelObtain.writeStrongBinder(iBinder);
                    parcelObtain.writeString(str3);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    if (profilerInfo != null) {
                        parcelObtain.writeInt(1);
                        profilerInfo.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    if (bundle != null) {
                        parcelObtain.writeInt(1);
                        bundle.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeInt(i3);
                    this.mRemote.transact(103, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        waitResultCreateFromParcel = WaitResult.CREATOR.createFromParcel(parcelObtain2);
                    }
                    return waitResultCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean willActivityBeVisible(IBinder iBinder) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    this.mRemote.transact(104, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int startActivityWithConfig(IApplicationThread iApplicationThread, String str, Intent intent, String str2, IBinder iBinder, String str3, int i, int i2, Configuration configuration, Bundle bundle, int i3) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iApplicationThread != null ? iApplicationThread.asBinder() : null);
                    parcelObtain.writeString(str);
                    if (intent != null) {
                        parcelObtain.writeInt(1);
                        intent.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeString(str2);
                    parcelObtain.writeStrongBinder(iBinder);
                    parcelObtain.writeString(str3);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    if (configuration != null) {
                        parcelObtain.writeInt(1);
                        configuration.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    if (bundle != null) {
                        parcelObtain.writeInt(1);
                        bundle.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeInt(i3);
                    this.mRemote.transact(105, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public List<ApplicationInfo> getRunningExternalApplications() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(106, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.createTypedArrayList(ApplicationInfo.CREATOR);
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void finishHeavyWeightApp() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(107, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void handleApplicationStrictModeViolation(IBinder iBinder, int i, StrictMode.ViolationInfo violationInfo) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    parcelObtain.writeInt(i);
                    if (violationInfo != null) {
                        parcelObtain.writeInt(1);
                        violationInfo.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(108, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isImmersive(IBinder iBinder) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    this.mRemote.transact(109, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setImmersive(IBinder iBinder, boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(110, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isTopActivityImmersive() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(111, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void crashApplication(int i, int i2, String str, int i3, String str2, boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i3);
                    parcelObtain.writeString(str2);
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(112, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public String getProviderMimeType(Uri uri, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (uri != null) {
                        parcelObtain.writeInt(1);
                        uri.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(113, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readString();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public IBinder newUriPermissionOwner(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(114, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readStrongBinder();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void grantUriPermissionFromOwner(IBinder iBinder, int i, String str, Uri uri, int i2, int i3, int i4) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeString(str);
                    if (uri != null) {
                        parcelObtain.writeInt(1);
                        uri.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeInt(i3);
                    parcelObtain.writeInt(i4);
                    this.mRemote.transact(115, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void revokeUriPermissionFromOwner(IBinder iBinder, Uri uri, int i, int i2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    if (uri != null) {
                        parcelObtain.writeInt(1);
                        uri.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(116, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int checkGrantUriPermission(int i, String str, Uri uri, int i2, int i3) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeString(str);
                    if (uri != null) {
                        parcelObtain.writeInt(1);
                        uri.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeInt(i3);
                    this.mRemote.transact(117, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean dumpHeap(String str, int i, boolean z, boolean z2, boolean z3, String str2, ParcelFileDescriptor parcelFileDescriptor) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(z ? 1 : 0);
                    parcelObtain.writeInt(z2 ? 1 : 0);
                    parcelObtain.writeInt(z3 ? 1 : 0);
                    parcelObtain.writeString(str2);
                    boolean z4 = true;
                    if (parcelFileDescriptor != null) {
                        parcelObtain.writeInt(1);
                        parcelFileDescriptor.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(118, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() == 0) {
                        z4 = false;
                    }
                    return z4;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int startActivities(IApplicationThread iApplicationThread, String str, Intent[] intentArr, String[] strArr, IBinder iBinder, Bundle bundle, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iApplicationThread != null ? iApplicationThread.asBinder() : null);
                    parcelObtain.writeString(str);
                    parcelObtain.writeTypedArray(intentArr, 0);
                    parcelObtain.writeStringArray(strArr);
                    parcelObtain.writeStrongBinder(iBinder);
                    if (bundle != null) {
                        parcelObtain.writeInt(1);
                        bundle.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(119, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isUserRunning(int i, int i2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(120, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void activitySlept(IBinder iBinder) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    this.mRemote.transact(121, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public int getFrontActivityScreenCompatMode() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(122, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setFrontActivityScreenCompatMode(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(123, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int getPackageScreenCompatMode(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(124, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setPackageScreenCompatMode(String str, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(125, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean getPackageAskScreenCompat(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(126, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setPackageAskScreenCompat(String str, boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(127, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean switchUser(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(128, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setFocusedTask(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(129, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean removeTask(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(130, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void registerProcessObserver(IProcessObserver iProcessObserver) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iProcessObserver != null ? iProcessObserver.asBinder() : null);
                    this.mRemote.transact(131, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void unregisterProcessObserver(IProcessObserver iProcessObserver) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iProcessObserver != null ? iProcessObserver.asBinder() : null);
                    this.mRemote.transact(132, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isIntentSenderTargetedToPackage(IIntentSender iIntentSender) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iIntentSender != null ? iIntentSender.asBinder() : null);
                    this.mRemote.transact(133, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void updatePersistentConfiguration(Configuration configuration) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (configuration != null) {
                        parcelObtain.writeInt(1);
                        configuration.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(134, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public long[] getProcessPss(int[] iArr) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeIntArray(iArr);
                    this.mRemote.transact(135, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.createLongArray();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void showBootMessage(CharSequence charSequence, boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (charSequence != null) {
                        parcelObtain.writeInt(1);
                        TextUtils.writeToParcel(charSequence, parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(136, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void killAllBackgroundProcesses() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(137, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public ContentProviderHolder getContentProviderExternal(String str, int i, IBinder iBinder) throws RemoteException {
                ContentProviderHolder contentProviderHolderCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeStrongBinder(iBinder);
                    this.mRemote.transact(138, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        contentProviderHolderCreateFromParcel = ContentProviderHolder.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        contentProviderHolderCreateFromParcel = null;
                    }
                    return contentProviderHolderCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void removeContentProviderExternal(String str, IBinder iBinder) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeStrongBinder(iBinder);
                    this.mRemote.transact(139, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void getMyMemoryState(ActivityManager.RunningAppProcessInfo runningAppProcessInfo) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(140, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        runningAppProcessInfo.readFromParcel(parcelObtain2);
                    }
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean killProcessesBelowForeground(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(141, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public UserInfo getCurrentUser() throws RemoteException {
                UserInfo userInfoCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(142, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        userInfoCreateFromParcel = UserInfo.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        userInfoCreateFromParcel = null;
                    }
                    return userInfoCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean shouldUpRecreateTask(IBinder iBinder, String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(143, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean navigateUpTo(IBinder iBinder, Intent intent, int i, Intent intent2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    boolean z = true;
                    if (intent != null) {
                        parcelObtain.writeInt(1);
                        intent.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeInt(i);
                    if (intent2 != null) {
                        parcelObtain.writeInt(1);
                        intent2.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(144, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() == 0) {
                        z = false;
                    }
                    return z;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setLockScreenShown(boolean z, boolean z2, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(z ? 1 : 0);
                    parcelObtain.writeInt(z2 ? 1 : 0);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(145, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean finishActivityAffinity(IBinder iBinder) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    this.mRemote.transact(146, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int getLaunchedFromUid(IBinder iBinder) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    this.mRemote.transact(147, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void unstableProviderDied(IBinder iBinder) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    this.mRemote.transact(148, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isIntentSenderAnActivity(IIntentSender iIntentSender) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iIntentSender != null ? iIntentSender.asBinder() : null);
                    this.mRemote.transact(149, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isIntentSenderAForegroundService(IIntentSender iIntentSender) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iIntentSender != null ? iIntentSender.asBinder() : null);
                    this.mRemote.transact(150, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int startActivityAsUser(IApplicationThread iApplicationThread, String str, Intent intent, String str2, IBinder iBinder, String str3, int i, int i2, ProfilerInfo profilerInfo, Bundle bundle, int i3) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iApplicationThread != null ? iApplicationThread.asBinder() : null);
                    parcelObtain.writeString(str);
                    if (intent != null) {
                        parcelObtain.writeInt(1);
                        intent.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeString(str2);
                    parcelObtain.writeStrongBinder(iBinder);
                    parcelObtain.writeString(str3);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    if (profilerInfo != null) {
                        parcelObtain.writeInt(1);
                        profilerInfo.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    if (bundle != null) {
                        parcelObtain.writeInt(1);
                        bundle.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeInt(i3);
                    this.mRemote.transact(151, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int stopUser(int i, boolean z, IStopUserCallback iStopUserCallback) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(z ? 1 : 0);
                    parcelObtain.writeStrongBinder(iStopUserCallback != null ? iStopUserCallback.asBinder() : null);
                    this.mRemote.transact(152, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void registerUserSwitchObserver(IUserSwitchObserver iUserSwitchObserver, String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iUserSwitchObserver != null ? iUserSwitchObserver.asBinder() : null);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(153, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void unregisterUserSwitchObserver(IUserSwitchObserver iUserSwitchObserver) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iUserSwitchObserver != null ? iUserSwitchObserver.asBinder() : null);
                    this.mRemote.transact(154, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int[] getRunningUserIds() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(155, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.createIntArray();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void requestBugReport(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(156, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void requestTelephonyBugReport(String str, String str2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeString(str2);
                    this.mRemote.transact(157, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void requestWifiBugReport(String str, String str2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeString(str2);
                    this.mRemote.transact(158, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public long inputDispatchingTimedOut(int i, boolean z, String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(z ? 1 : 0);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(159, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readLong();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void clearPendingBackup() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(160, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public Intent getIntentForIntentSender(IIntentSender iIntentSender) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    Intent intentCreateFromParcel = null;
                    parcelObtain.writeStrongBinder(iIntentSender != null ? iIntentSender.asBinder() : null);
                    this.mRemote.transact(161, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        intentCreateFromParcel = Intent.CREATOR.createFromParcel(parcelObtain2);
                    }
                    return intentCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public Bundle getAssistContextExtras(int i) throws RemoteException {
                Bundle bundleCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(162, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        bundleCreateFromParcel = Bundle.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        bundleCreateFromParcel = null;
                    }
                    return bundleCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void reportAssistContextExtras(IBinder iBinder, Bundle bundle, AssistStructure assistStructure, AssistContent assistContent, Uri uri) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    if (bundle != null) {
                        parcelObtain.writeInt(1);
                        bundle.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    if (assistStructure != null) {
                        parcelObtain.writeInt(1);
                        assistStructure.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    if (assistContent != null) {
                        parcelObtain.writeInt(1);
                        assistContent.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    if (uri != null) {
                        parcelObtain.writeInt(1);
                        uri.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(163, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public String getLaunchedFromPackage(IBinder iBinder) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    this.mRemote.transact(164, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readString();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void killUid(int i, int i2, String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(165, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setUserIsMonkey(boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(166, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void hang(IBinder iBinder, boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(167, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setTaskWindowingMode(int i, int i2, boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(168, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void moveTaskToStack(int i, int i2, boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(169, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void resizeStack(int i, Rect rect, boolean z, boolean z2, boolean z3, int i2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    if (rect != null) {
                        parcelObtain.writeInt(1);
                        rect.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeInt(z ? 1 : 0);
                    parcelObtain.writeInt(z2 ? 1 : 0);
                    parcelObtain.writeInt(z3 ? 1 : 0);
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(170, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public List<ActivityManager.StackInfo> getAllStackInfos() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(171, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.createTypedArrayList(ActivityManager.StackInfo.CREATOR);
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setFocusedStack(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(172, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public ActivityManager.StackInfo getFocusedStackInfo() throws RemoteException {
                ActivityManager.StackInfo stackInfoCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(173, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        stackInfoCreateFromParcel = ActivityManager.StackInfo.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        stackInfoCreateFromParcel = null;
                    }
                    return stackInfoCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public ActivityManager.StackInfo getStackInfo(int i, int i2) throws RemoteException {
                ActivityManager.StackInfo stackInfoCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(174, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        stackInfoCreateFromParcel = ActivityManager.StackInfo.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        stackInfoCreateFromParcel = null;
                    }
                    return stackInfoCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean convertFromTranslucent(IBinder iBinder) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    this.mRemote.transact(175, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean convertToTranslucent(IBinder iBinder, Bundle bundle) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    boolean z = true;
                    if (bundle != null) {
                        parcelObtain.writeInt(1);
                        bundle.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(176, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() == 0) {
                        z = false;
                    }
                    return z;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void notifyActivityDrawn(IBinder iBinder) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    this.mRemote.transact(177, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void reportActivityFullyDrawn(IBinder iBinder, boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(178, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void restart() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(179, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void performIdleMaintenance() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(180, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void takePersistableUriPermission(Uri uri, int i, String str, int i2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (uri != null) {
                        parcelObtain.writeInt(1);
                        uri.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeInt(i);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(181, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void releasePersistableUriPermission(Uri uri, int i, String str, int i2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (uri != null) {
                        parcelObtain.writeInt(1);
                        uri.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeInt(i);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(182, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public ParceledListSlice getPersistedUriPermissions(String str, boolean z) throws RemoteException {
                ParceledListSlice parceledListSliceCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(183, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        parceledListSliceCreateFromParcel = ParceledListSlice.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        parceledListSliceCreateFromParcel = null;
                    }
                    return parceledListSliceCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void appNotRespondingViaProvider(IBinder iBinder) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    this.mRemote.transact(184, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public Rect getTaskBounds(int i) throws RemoteException {
                Rect rectCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(185, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        rectCreateFromParcel = Rect.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        rectCreateFromParcel = null;
                    }
                    return rectCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int getActivityDisplayId(IBinder iBinder) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    this.mRemote.transact(186, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean setProcessMemoryTrimLevel(String str, int i, int i2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(187, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public String getTagForIntentSender(IIntentSender iIntentSender, String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iIntentSender != null ? iIntentSender.asBinder() : null);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(188, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readString();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean startUserInBackground(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(189, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void startLockTaskModeByToken(IBinder iBinder) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    this.mRemote.transact(190, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void stopLockTaskModeByToken(IBinder iBinder) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    this.mRemote.transact(191, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isInLockTaskMode() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(192, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setTaskDescription(IBinder iBinder, ActivityManager.TaskDescription taskDescription) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    if (taskDescription != null) {
                        parcelObtain.writeInt(1);
                        taskDescription.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(193, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int startVoiceActivity(String str, int i, int i2, Intent intent, String str2, IVoiceInteractionSession iVoiceInteractionSession, IVoiceInteractor iVoiceInteractor, int i3, ProfilerInfo profilerInfo, Bundle bundle, int i4) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    if (intent != null) {
                        parcelObtain.writeInt(1);
                        intent.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeString(str2);
                    parcelObtain.writeStrongBinder(iVoiceInteractionSession != null ? iVoiceInteractionSession.asBinder() : null);
                    parcelObtain.writeStrongBinder(iVoiceInteractor != null ? iVoiceInteractor.asBinder() : null);
                    parcelObtain.writeInt(i3);
                    if (profilerInfo != null) {
                        parcelObtain.writeInt(1);
                        profilerInfo.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    if (bundle != null) {
                        parcelObtain.writeInt(1);
                        bundle.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeInt(i4);
                    this.mRemote.transact(194, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int startAssistantActivity(String str, int i, int i2, Intent intent, String str2, Bundle bundle, int i3) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    if (intent != null) {
                        parcelObtain.writeInt(1);
                        intent.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeString(str2);
                    if (bundle != null) {
                        parcelObtain.writeInt(1);
                        bundle.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeInt(i3);
                    this.mRemote.transact(195, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void startRecentsActivity(Intent intent, IAssistDataReceiver iAssistDataReceiver, IRecentsAnimationRunner iRecentsAnimationRunner) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (intent != null) {
                        parcelObtain.writeInt(1);
                        intent.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeStrongBinder(iAssistDataReceiver != null ? iAssistDataReceiver.asBinder() : null);
                    parcelObtain.writeStrongBinder(iRecentsAnimationRunner != null ? iRecentsAnimationRunner.asBinder() : null);
                    this.mRemote.transact(196, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void cancelRecentsAnimation(boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(197, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int startActivityFromRecents(int i, Bundle bundle) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    if (bundle != null) {
                        parcelObtain.writeInt(1);
                        bundle.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(198, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public Bundle getActivityOptions(IBinder iBinder) throws RemoteException {
                Bundle bundleCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    this.mRemote.transact(199, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        bundleCreateFromParcel = Bundle.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        bundleCreateFromParcel = null;
                    }
                    return bundleCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public List<IBinder> getAppTasks(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(200, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.createBinderArrayList();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void startSystemLockTaskMode(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(201, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void stopSystemLockTaskMode() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(202, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void finishVoiceTask(IVoiceInteractionSession iVoiceInteractionSession) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iVoiceInteractionSession != null ? iVoiceInteractionSession.asBinder() : null);
                    this.mRemote.transact(203, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isTopOfTask(IBinder iBinder) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    this.mRemote.transact(204, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void notifyLaunchTaskBehindComplete(IBinder iBinder) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    this.mRemote.transact(205, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void notifyEnterAnimationComplete(IBinder iBinder) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    this.mRemote.transact(206, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int startActivityAsCaller(IApplicationThread iApplicationThread, String str, Intent intent, String str2, IBinder iBinder, String str3, int i, int i2, ProfilerInfo profilerInfo, Bundle bundle, boolean z, int i3) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iApplicationThread != null ? iApplicationThread.asBinder() : null);
                    parcelObtain.writeString(str);
                    if (intent != null) {
                        parcelObtain.writeInt(1);
                        intent.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeString(str2);
                    parcelObtain.writeStrongBinder(iBinder);
                    parcelObtain.writeString(str3);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    if (profilerInfo != null) {
                        parcelObtain.writeInt(1);
                        profilerInfo.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    if (bundle != null) {
                        parcelObtain.writeInt(1);
                        bundle.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeInt(z ? 1 : 0);
                    parcelObtain.writeInt(i3);
                    this.mRemote.transact(207, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int addAppTask(IBinder iBinder, Intent intent, ActivityManager.TaskDescription taskDescription, Bitmap bitmap) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    if (intent != null) {
                        parcelObtain.writeInt(1);
                        intent.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    if (taskDescription != null) {
                        parcelObtain.writeInt(1);
                        taskDescription.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    if (bitmap != null) {
                        parcelObtain.writeInt(1);
                        bitmap.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(208, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public Point getAppTaskThumbnailSize() throws RemoteException {
                Point pointCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(209, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        pointCreateFromParcel = Point.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        pointCreateFromParcel = null;
                    }
                    return pointCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean releaseActivityInstance(IBinder iBinder) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    this.mRemote.transact(210, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void releaseSomeActivities(IApplicationThread iApplicationThread) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iApplicationThread != null ? iApplicationThread.asBinder() : null);
                    this.mRemote.transact(211, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void bootAnimationComplete() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(212, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public Bitmap getTaskDescriptionIcon(String str, int i) throws RemoteException {
                Bitmap bitmapCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(213, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        bitmapCreateFromParcel = Bitmap.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        bitmapCreateFromParcel = null;
                    }
                    return bitmapCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean launchAssistIntent(Intent intent, int i, String str, int i2, Bundle bundle) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = true;
                    if (intent != null) {
                        parcelObtain.writeInt(1);
                        intent.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeInt(i);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i2);
                    if (bundle != null) {
                        parcelObtain.writeInt(1);
                        bundle.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(214, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() == 0) {
                        z = false;
                    }
                    return z;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void startInPlaceAnimationOnFrontMostApplication(Bundle bundle) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (bundle != null) {
                        parcelObtain.writeInt(1);
                        bundle.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(215, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int checkPermissionWithToken(String str, int i, int i2, IBinder iBinder) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeStrongBinder(iBinder);
                    this.mRemote.transact(216, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void registerTaskStackListener(ITaskStackListener iTaskStackListener) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iTaskStackListener != null ? iTaskStackListener.asBinder() : null);
                    this.mRemote.transact(217, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void unregisterTaskStackListener(ITaskStackListener iTaskStackListener) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iTaskStackListener != null ? iTaskStackListener.asBinder() : null);
                    this.mRemote.transact(218, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void notifyCleartextNetwork(int i, byte[] bArr) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeByteArray(bArr);
                    this.mRemote.transact(219, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int createStackOnDisplay(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(220, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setTaskResizeable(int i, int i2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(221, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean requestAssistContextExtras(int i, IAssistDataReceiver iAssistDataReceiver, Bundle bundle, IBinder iBinder, boolean z, boolean z2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeStrongBinder(iAssistDataReceiver != null ? iAssistDataReceiver.asBinder() : null);
                    boolean z3 = true;
                    if (bundle != null) {
                        parcelObtain.writeInt(1);
                        bundle.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeStrongBinder(iBinder);
                    parcelObtain.writeInt(z ? 1 : 0);
                    parcelObtain.writeInt(z2 ? 1 : 0);
                    this.mRemote.transact(222, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() == 0) {
                        z3 = false;
                    }
                    return z3;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void resizeTask(int i, Rect rect, int i2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    if (rect != null) {
                        parcelObtain.writeInt(1);
                        rect.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(223, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int getLockTaskModeState() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(224, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setDumpHeapDebugLimit(String str, int i, long j, String str2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeLong(j);
                    parcelObtain.writeString(str2);
                    this.mRemote.transact(225, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void dumpHeapFinished(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(226, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setVoiceKeepAwake(IVoiceInteractionSession iVoiceInteractionSession, boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iVoiceInteractionSession != null ? iVoiceInteractionSession.asBinder() : null);
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(227, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void updateLockTaskPackages(int i, String[] strArr) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeStringArray(strArr);
                    this.mRemote.transact(228, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void noteAlarmStart(IIntentSender iIntentSender, WorkSource workSource, int i, String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iIntentSender != null ? iIntentSender.asBinder() : null);
                    if (workSource != null) {
                        parcelObtain.writeInt(1);
                        workSource.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeInt(i);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(229, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void noteAlarmFinish(IIntentSender iIntentSender, WorkSource workSource, int i, String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iIntentSender != null ? iIntentSender.asBinder() : null);
                    if (workSource != null) {
                        parcelObtain.writeInt(1);
                        workSource.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeInt(i);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(230, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int getPackageProcessState(String str, String str2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeString(str2);
                    this.mRemote.transact(231, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void showLockTaskEscapeMessage(IBinder iBinder) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    this.mRemote.transact(232, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void updateDeviceOwner(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(233, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void keyguardGoingAway(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(234, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int getUidProcessState(int i, String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(235, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isAssistDataAllowedOnCurrentActivity() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(236, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean showAssistFromActivity(IBinder iBinder, Bundle bundle) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    boolean z = true;
                    if (bundle != null) {
                        parcelObtain.writeInt(1);
                        bundle.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(237, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() == 0) {
                        z = false;
                    }
                    return z;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isRootVoiceInteraction(IBinder iBinder) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    this.mRemote.transact(238, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean startBinderTracking() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(239, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean stopBinderTrackingAndDump(ParcelFileDescriptor parcelFileDescriptor) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = true;
                    if (parcelFileDescriptor != null) {
                        parcelObtain.writeInt(1);
                        parcelFileDescriptor.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(240, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() == 0) {
                        z = false;
                    }
                    return z;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void positionTaskInStack(int i, int i2, int i3) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeInt(i3);
                    this.mRemote.transact(241, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void exitFreeformMode(IBinder iBinder) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    this.mRemote.transact(242, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void reportSizeConfigurations(IBinder iBinder, int[] iArr, int[] iArr2, int[] iArr3) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    parcelObtain.writeIntArray(iArr);
                    parcelObtain.writeIntArray(iArr2);
                    parcelObtain.writeIntArray(iArr3);
                    this.mRemote.transact(243, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean setTaskWindowingModeSplitScreenPrimary(int i, int i2, boolean z, boolean z2, Rect rect, boolean z3) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeInt(z ? 1 : 0);
                    parcelObtain.writeInt(z2 ? 1 : 0);
                    boolean z4 = true;
                    if (rect != null) {
                        parcelObtain.writeInt(1);
                        rect.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeInt(z3 ? 1 : 0);
                    this.mRemote.transact(244, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() == 0) {
                        z4 = false;
                    }
                    return z4;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void dismissSplitScreenMode(boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(245, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void dismissPip(boolean z, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(z ? 1 : 0);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(246, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void suppressResizeConfigChanges(boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(247, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void moveTasksToFullscreenStack(int i, boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(248, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean moveTopActivityToPinnedStack(int i, Rect rect) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    boolean z = true;
                    if (rect != null) {
                        parcelObtain.writeInt(1);
                        rect.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(249, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() == 0) {
                        z = false;
                    }
                    return z;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isAppStartModeDisabled(int i, String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(250, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean unlockUser(int i, byte[] bArr, byte[] bArr2, IProgressListener iProgressListener) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeByteArray(bArr);
                    parcelObtain.writeByteArray(bArr2);
                    parcelObtain.writeStrongBinder(iProgressListener != null ? iProgressListener.asBinder() : null);
                    this.mRemote.transact(251, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isInMultiWindowMode(IBinder iBinder) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    this.mRemote.transact(252, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isInPictureInPictureMode(IBinder iBinder) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    this.mRemote.transact(253, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void killPackageDependents(String str, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(254, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean enterPictureInPictureMode(IBinder iBinder, PictureInPictureParams pictureInPictureParams) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    boolean z = true;
                    if (pictureInPictureParams != null) {
                        parcelObtain.writeInt(1);
                        pictureInPictureParams.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(255, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() == 0) {
                        z = false;
                    }
                    return z;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setPictureInPictureParams(IBinder iBinder, PictureInPictureParams pictureInPictureParams) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    if (pictureInPictureParams != null) {
                        parcelObtain.writeInt(1);
                        pictureInPictureParams.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(256, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int getMaxNumPictureInPictureActions(IBinder iBinder) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    this.mRemote.transact(257, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void activityRelaunched(IBinder iBinder) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    this.mRemote.transact(258, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public IBinder getUriPermissionOwnerForActivity(IBinder iBinder) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    this.mRemote.transact(259, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readStrongBinder();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void resizeDockedStack(Rect rect, Rect rect2, Rect rect3, Rect rect4, Rect rect5) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (rect != null) {
                        parcelObtain.writeInt(1);
                        rect.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    if (rect2 != null) {
                        parcelObtain.writeInt(1);
                        rect2.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    if (rect3 != null) {
                        parcelObtain.writeInt(1);
                        rect3.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    if (rect4 != null) {
                        parcelObtain.writeInt(1);
                        rect4.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    if (rect5 != null) {
                        parcelObtain.writeInt(1);
                        rect5.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(260, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setSplitScreenResizing(boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(261, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int setVrMode(IBinder iBinder, boolean z, ComponentName componentName) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    parcelObtain.writeInt(z ? 1 : 0);
                    if (componentName != null) {
                        parcelObtain.writeInt(1);
                        componentName.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(262, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public ParceledListSlice getGrantedUriPermissions(String str, int i) throws RemoteException {
                ParceledListSlice parceledListSliceCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(263, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        parceledListSliceCreateFromParcel = ParceledListSlice.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        parceledListSliceCreateFromParcel = null;
                    }
                    return parceledListSliceCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void clearGrantedUriPermissions(String str, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(264, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isAppForeground(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(265, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void startLocalVoiceInteraction(IBinder iBinder, Bundle bundle) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    if (bundle != null) {
                        parcelObtain.writeInt(1);
                        bundle.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(266, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void stopLocalVoiceInteraction(IBinder iBinder) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    this.mRemote.transact(267, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean supportsLocalVoiceInteraction() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(268, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void notifyPinnedStackAnimationStarted() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(269, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void notifyPinnedStackAnimationEnded() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(270, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void removeStack(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(271, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void removeStacksInWindowingModes(int[] iArr) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeIntArray(iArr);
                    this.mRemote.transact(272, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void removeStacksWithActivityTypes(int[] iArr) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeIntArray(iArr);
                    this.mRemote.transact(273, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void makePackageIdle(String str, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(274, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int getMemoryTrimLevel() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(275, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void resizePinnedStack(Rect rect, Rect rect2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (rect != null) {
                        parcelObtain.writeInt(1);
                        rect.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    if (rect2 != null) {
                        parcelObtain.writeInt(1);
                        rect2.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(276, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isVrModePackageEnabled(ComponentName componentName) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = true;
                    if (componentName != null) {
                        parcelObtain.writeInt(1);
                        componentName.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(277, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() == 0) {
                        z = false;
                    }
                    return z;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void notifyLockedProfile(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(278, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void startConfirmDeviceCredentialIntent(Intent intent, Bundle bundle) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (intent != null) {
                        parcelObtain.writeInt(1);
                        intent.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    if (bundle != null) {
                        parcelObtain.writeInt(1);
                        bundle.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(279, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void sendIdleJobTrigger() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(280, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int sendIntentSender(IIntentSender iIntentSender, IBinder iBinder, int i, Intent intent, String str, IIntentReceiver iIntentReceiver, String str2, Bundle bundle) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iIntentSender != null ? iIntentSender.asBinder() : null);
                    parcelObtain.writeStrongBinder(iBinder);
                    parcelObtain.writeInt(i);
                    if (intent != null) {
                        parcelObtain.writeInt(1);
                        intent.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeString(str);
                    parcelObtain.writeStrongBinder(iIntentReceiver != null ? iIntentReceiver.asBinder() : null);
                    parcelObtain.writeString(str2);
                    if (bundle != null) {
                        parcelObtain.writeInt(1);
                        bundle.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(281, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isBackgroundRestricted(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(282, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setVrThread(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(283, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setRenderThread(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(284, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setHasTopUi(boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(285, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean updateDisplayOverrideConfiguration(Configuration configuration, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = true;
                    if (configuration != null) {
                        parcelObtain.writeInt(1);
                        configuration.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(286, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() == 0) {
                        z = false;
                    }
                    return z;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void moveStackToDisplay(int i, int i2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(287, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean requestAutofillData(IAssistDataReceiver iAssistDataReceiver, Bundle bundle, IBinder iBinder, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iAssistDataReceiver != null ? iAssistDataReceiver.asBinder() : null);
                    boolean z = true;
                    if (bundle != null) {
                        parcelObtain.writeInt(1);
                        bundle.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeStrongBinder(iBinder);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(288, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() == 0) {
                        z = false;
                    }
                    return z;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void dismissKeyguard(IBinder iBinder, IKeyguardDismissCallback iKeyguardDismissCallback, CharSequence charSequence) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    parcelObtain.writeStrongBinder(iKeyguardDismissCallback != null ? iKeyguardDismissCallback.asBinder() : null);
                    if (charSequence != null) {
                        parcelObtain.writeInt(1);
                        TextUtils.writeToParcel(charSequence, parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(289, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int restartUserInBackground(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(290, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void cancelTaskWindowTransition(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(291, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public ActivityManager.TaskSnapshot getTaskSnapshot(int i, boolean z) throws RemoteException {
                ActivityManager.TaskSnapshot taskSnapshotCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(292, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        taskSnapshotCreateFromParcel = ActivityManager.TaskSnapshot.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        taskSnapshotCreateFromParcel = null;
                    }
                    return taskSnapshotCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void scheduleApplicationInfoChanged(List<String> list, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStringList(list);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(293, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setPersistentVrThread(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(294, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void waitForNetworkStateUpdate(long j) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeLong(j);
                    this.mRemote.transact(295, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setDisablePreviewScreenshots(IBinder iBinder, boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(296, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int getLastResumedActivityUserId() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(297, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void backgroundWhitelistUid(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(298, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void updateLockTaskFeatures(int i, int i2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(299, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setShowWhenLocked(IBinder iBinder, boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(300, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setTurnScreenOn(IBinder iBinder, boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(301, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean startUserInBackgroundWithListener(int i, IProgressListener iProgressListener) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeStrongBinder(iProgressListener != null ? iProgressListener.asBinder() : null);
                    this.mRemote.transact(302, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void registerRemoteAnimations(IBinder iBinder, RemoteAnimationDefinition remoteAnimationDefinition) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    if (remoteAnimationDefinition != null) {
                        parcelObtain.writeInt(1);
                        remoteAnimationDefinition.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(303, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void registerRemoteAnimationForNextActivityStart(String str, RemoteAnimationAdapter remoteAnimationAdapter) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    if (remoteAnimationAdapter != null) {
                        parcelObtain.writeInt(1);
                        remoteAnimationAdapter.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(304, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void alwaysShowUnsupportedCompileSdkWarning(ComponentName componentName) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (componentName != null) {
                        parcelObtain.writeInt(1);
                        componentName.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(305, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setAalMode(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(306, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setAalEnabled(boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(307, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }
        }

        private boolean onTransact$startActivity$(Parcel parcel, Parcel parcel2) throws RemoteException {
            Intent intentCreateFromParcel;
            ProfilerInfo profilerInfoCreateFromParcel;
            Bundle bundleCreateFromParcel;
            parcel.enforceInterface(DESCRIPTOR);
            IApplicationThread iApplicationThreadAsInterface = IApplicationThread.Stub.asInterface(parcel.readStrongBinder());
            String string = parcel.readString();
            if (parcel.readInt() != 0) {
                intentCreateFromParcel = Intent.CREATOR.createFromParcel(parcel);
            } else {
                intentCreateFromParcel = null;
            }
            String string2 = parcel.readString();
            IBinder strongBinder = parcel.readStrongBinder();
            String string3 = parcel.readString();
            int i = parcel.readInt();
            int i2 = parcel.readInt();
            if (parcel.readInt() != 0) {
                profilerInfoCreateFromParcel = ProfilerInfo.CREATOR.createFromParcel(parcel);
            } else {
                profilerInfoCreateFromParcel = null;
            }
            if (parcel.readInt() != 0) {
                bundleCreateFromParcel = Bundle.CREATOR.createFromParcel(parcel);
            } else {
                bundleCreateFromParcel = null;
            }
            int iStartActivity = startActivity(iApplicationThreadAsInterface, string, intentCreateFromParcel, string2, strongBinder, string3, i, i2, profilerInfoCreateFromParcel, bundleCreateFromParcel);
            parcel2.writeNoException();
            parcel2.writeInt(iStartActivity);
            return true;
        }

        private boolean onTransact$registerReceiver$(Parcel parcel, Parcel parcel2) throws RemoteException {
            IntentFilter intentFilterCreateFromParcel;
            parcel.enforceInterface(DESCRIPTOR);
            IApplicationThread iApplicationThreadAsInterface = IApplicationThread.Stub.asInterface(parcel.readStrongBinder());
            String string = parcel.readString();
            IIntentReceiver iIntentReceiverAsInterface = IIntentReceiver.Stub.asInterface(parcel.readStrongBinder());
            if (parcel.readInt() != 0) {
                intentFilterCreateFromParcel = IntentFilter.CREATOR.createFromParcel(parcel);
            } else {
                intentFilterCreateFromParcel = null;
            }
            Intent intentRegisterReceiver = registerReceiver(iApplicationThreadAsInterface, string, iIntentReceiverAsInterface, intentFilterCreateFromParcel, parcel.readString(), parcel.readInt(), parcel.readInt());
            parcel2.writeNoException();
            if (intentRegisterReceiver != null) {
                parcel2.writeInt(1);
                intentRegisterReceiver.writeToParcel(parcel2, 1);
            } else {
                parcel2.writeInt(0);
            }
            return true;
        }

        private boolean onTransact$broadcastIntent$(Parcel parcel, Parcel parcel2) throws RemoteException {
            Intent intentCreateFromParcel;
            Bundle bundleCreateFromParcel;
            Bundle bundleCreateFromParcel2;
            parcel.enforceInterface(DESCRIPTOR);
            IApplicationThread iApplicationThreadAsInterface = IApplicationThread.Stub.asInterface(parcel.readStrongBinder());
            if (parcel.readInt() != 0) {
                intentCreateFromParcel = Intent.CREATOR.createFromParcel(parcel);
            } else {
                intentCreateFromParcel = null;
            }
            String string = parcel.readString();
            IIntentReceiver iIntentReceiverAsInterface = IIntentReceiver.Stub.asInterface(parcel.readStrongBinder());
            int i = parcel.readInt();
            String string2 = parcel.readString();
            if (parcel.readInt() != 0) {
                bundleCreateFromParcel = Bundle.CREATOR.createFromParcel(parcel);
            } else {
                bundleCreateFromParcel = null;
            }
            String[] strArrCreateStringArray = parcel.createStringArray();
            int i2 = parcel.readInt();
            if (parcel.readInt() != 0) {
                bundleCreateFromParcel2 = Bundle.CREATOR.createFromParcel(parcel);
            } else {
                bundleCreateFromParcel2 = null;
            }
            int iBroadcastIntent = broadcastIntent(iApplicationThreadAsInterface, intentCreateFromParcel, string, iIntentReceiverAsInterface, i, string2, bundleCreateFromParcel, strArrCreateStringArray, i2, bundleCreateFromParcel2, parcel.readInt() != 0, parcel.readInt() != 0, parcel.readInt());
            parcel2.writeNoException();
            parcel2.writeInt(iBroadcastIntent);
            return true;
        }

        private boolean onTransact$finishReceiver$(Parcel parcel, Parcel parcel2) throws RemoteException {
            Bundle bundleCreateFromParcel;
            parcel.enforceInterface(DESCRIPTOR);
            IBinder strongBinder = parcel.readStrongBinder();
            int i = parcel.readInt();
            String string = parcel.readString();
            if (parcel.readInt() != 0) {
                bundleCreateFromParcel = Bundle.CREATOR.createFromParcel(parcel);
            } else {
                bundleCreateFromParcel = null;
            }
            finishReceiver(strongBinder, i, string, bundleCreateFromParcel, parcel.readInt() != 0, parcel.readInt());
            return true;
        }

        private boolean onTransact$startService$(Parcel parcel, Parcel parcel2) throws RemoteException {
            Intent intentCreateFromParcel;
            parcel.enforceInterface(DESCRIPTOR);
            IApplicationThread iApplicationThreadAsInterface = IApplicationThread.Stub.asInterface(parcel.readStrongBinder());
            if (parcel.readInt() != 0) {
                intentCreateFromParcel = Intent.CREATOR.createFromParcel(parcel);
            } else {
                intentCreateFromParcel = null;
            }
            ComponentName componentNameStartService = startService(iApplicationThreadAsInterface, intentCreateFromParcel, parcel.readString(), parcel.readInt() != 0, parcel.readString(), parcel.readInt());
            parcel2.writeNoException();
            if (componentNameStartService != null) {
                parcel2.writeInt(1);
                componentNameStartService.writeToParcel(parcel2, 1);
            } else {
                parcel2.writeInt(0);
            }
            return true;
        }

        private boolean onTransact$bindService$(Parcel parcel, Parcel parcel2) throws RemoteException {
            Intent intentCreateFromParcel;
            parcel.enforceInterface(DESCRIPTOR);
            IApplicationThread iApplicationThreadAsInterface = IApplicationThread.Stub.asInterface(parcel.readStrongBinder());
            IBinder strongBinder = parcel.readStrongBinder();
            if (parcel.readInt() != 0) {
                intentCreateFromParcel = Intent.CREATOR.createFromParcel(parcel);
            } else {
                intentCreateFromParcel = null;
            }
            int iBindService = bindService(iApplicationThreadAsInterface, strongBinder, intentCreateFromParcel, parcel.readString(), IServiceConnection.Stub.asInterface(parcel.readStrongBinder()), parcel.readInt(), parcel.readString(), parcel.readInt());
            parcel2.writeNoException();
            parcel2.writeInt(iBindService);
            return true;
        }

        private boolean onTransact$startInstrumentation$(Parcel parcel, Parcel parcel2) throws RemoteException {
            ComponentName componentNameCreateFromParcel;
            parcel.enforceInterface(DESCRIPTOR);
            if (parcel.readInt() != 0) {
                componentNameCreateFromParcel = ComponentName.CREATOR.createFromParcel(parcel);
            } else {
                componentNameCreateFromParcel = null;
            }
            boolean zStartInstrumentation = startInstrumentation(componentNameCreateFromParcel, parcel.readString(), parcel.readInt(), parcel.readInt() != 0 ? Bundle.CREATOR.createFromParcel(parcel) : null, IInstrumentationWatcher.Stub.asInterface(parcel.readStrongBinder()), IUiAutomationConnection.Stub.asInterface(parcel.readStrongBinder()), parcel.readInt(), parcel.readString());
            parcel2.writeNoException();
            parcel2.writeInt(zStartInstrumentation ? 1 : 0);
            return true;
        }

        private boolean onTransact$checkUriPermission$(Parcel parcel, Parcel parcel2) throws RemoteException {
            Uri uriCreateFromParcel;
            parcel.enforceInterface(DESCRIPTOR);
            if (parcel.readInt() != 0) {
                uriCreateFromParcel = Uri.CREATOR.createFromParcel(parcel);
            } else {
                uriCreateFromParcel = null;
            }
            int iCheckUriPermission = checkUriPermission(uriCreateFromParcel, parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.readStrongBinder());
            parcel2.writeNoException();
            parcel2.writeInt(iCheckUriPermission);
            return true;
        }

        private boolean onTransact$getIntentSender$(Parcel parcel, Parcel parcel2) throws RemoteException {
            Bundle bundleCreateFromParcel;
            parcel.enforceInterface(DESCRIPTOR);
            int i = parcel.readInt();
            String string = parcel.readString();
            IBinder strongBinder = parcel.readStrongBinder();
            String string2 = parcel.readString();
            int i2 = parcel.readInt();
            Intent[] intentArr = (Intent[]) parcel.createTypedArray(Intent.CREATOR);
            String[] strArrCreateStringArray = parcel.createStringArray();
            int i3 = parcel.readInt();
            if (parcel.readInt() != 0) {
                bundleCreateFromParcel = Bundle.CREATOR.createFromParcel(parcel);
            } else {
                bundleCreateFromParcel = null;
            }
            IIntentSender intentSender = getIntentSender(i, string, strongBinder, string2, i2, intentArr, strArrCreateStringArray, i3, bundleCreateFromParcel, parcel.readInt());
            parcel2.writeNoException();
            parcel2.writeStrongBinder(intentSender != null ? intentSender.asBinder() : null);
            return true;
        }

        private boolean onTransact$noteWakeupAlarm$(Parcel parcel, Parcel parcel2) throws RemoteException {
            WorkSource workSourceCreateFromParcel;
            parcel.enforceInterface(DESCRIPTOR);
            IIntentSender iIntentSenderAsInterface = IIntentSender.Stub.asInterface(parcel.readStrongBinder());
            if (parcel.readInt() != 0) {
                workSourceCreateFromParcel = WorkSource.CREATOR.createFromParcel(parcel);
            } else {
                workSourceCreateFromParcel = null;
            }
            noteWakeupAlarm(iIntentSenderAsInterface, workSourceCreateFromParcel, parcel.readInt(), parcel.readString(), parcel.readString());
            parcel2.writeNoException();
            return true;
        }

        private boolean onTransact$setServiceForeground$(Parcel parcel, Parcel parcel2) throws RemoteException {
            ComponentName componentNameCreateFromParcel;
            parcel.enforceInterface(DESCRIPTOR);
            if (parcel.readInt() != 0) {
                componentNameCreateFromParcel = ComponentName.CREATOR.createFromParcel(parcel);
            } else {
                componentNameCreateFromParcel = null;
            }
            setServiceForeground(componentNameCreateFromParcel, parcel.readStrongBinder(), parcel.readInt(), parcel.readInt() != 0 ? Notification.CREATOR.createFromParcel(parcel) : null, parcel.readInt());
            parcel2.writeNoException();
            return true;
        }

        private boolean onTransact$profileControl$(Parcel parcel, Parcel parcel2) throws RemoteException {
            ProfilerInfo profilerInfoCreateFromParcel;
            parcel.enforceInterface(DESCRIPTOR);
            String string = parcel.readString();
            int i = parcel.readInt();
            boolean z = parcel.readInt() != 0;
            if (parcel.readInt() != 0) {
                profilerInfoCreateFromParcel = ProfilerInfo.CREATOR.createFromParcel(parcel);
            } else {
                profilerInfoCreateFromParcel = null;
            }
            boolean zProfileControl = profileControl(string, i, z, profilerInfoCreateFromParcel, parcel.readInt());
            parcel2.writeNoException();
            parcel2.writeInt(zProfileControl ? 1 : 0);
            return true;
        }

        private boolean onTransact$handleIncomingUser$(Parcel parcel, Parcel parcel2) throws RemoteException {
            parcel.enforceInterface(DESCRIPTOR);
            int iHandleIncomingUser = handleIncomingUser(parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.readInt() != 0, parcel.readInt() != 0, parcel.readString(), parcel.readString());
            parcel2.writeNoException();
            parcel2.writeInt(iHandleIncomingUser);
            return true;
        }

        private boolean onTransact$startActivityIntentSender$(Parcel parcel, Parcel parcel2) throws RemoteException {
            Intent intentCreateFromParcel;
            Bundle bundleCreateFromParcel;
            parcel.enforceInterface(DESCRIPTOR);
            IApplicationThread iApplicationThreadAsInterface = IApplicationThread.Stub.asInterface(parcel.readStrongBinder());
            IIntentSender iIntentSenderAsInterface = IIntentSender.Stub.asInterface(parcel.readStrongBinder());
            IBinder strongBinder = parcel.readStrongBinder();
            if (parcel.readInt() != 0) {
                intentCreateFromParcel = Intent.CREATOR.createFromParcel(parcel);
            } else {
                intentCreateFromParcel = null;
            }
            String string = parcel.readString();
            IBinder strongBinder2 = parcel.readStrongBinder();
            String string2 = parcel.readString();
            int i = parcel.readInt();
            int i2 = parcel.readInt();
            int i3 = parcel.readInt();
            if (parcel.readInt() != 0) {
                bundleCreateFromParcel = Bundle.CREATOR.createFromParcel(parcel);
            } else {
                bundleCreateFromParcel = null;
            }
            int iStartActivityIntentSender = startActivityIntentSender(iApplicationThreadAsInterface, iIntentSenderAsInterface, strongBinder, intentCreateFromParcel, string, strongBinder2, string2, i, i2, i3, bundleCreateFromParcel);
            parcel2.writeNoException();
            parcel2.writeInt(iStartActivityIntentSender);
            return true;
        }

        private boolean onTransact$startActivityAndWait$(Parcel parcel, Parcel parcel2) throws RemoteException {
            Intent intentCreateFromParcel;
            ProfilerInfo profilerInfoCreateFromParcel;
            Bundle bundleCreateFromParcel;
            parcel.enforceInterface(DESCRIPTOR);
            IApplicationThread iApplicationThreadAsInterface = IApplicationThread.Stub.asInterface(parcel.readStrongBinder());
            String string = parcel.readString();
            if (parcel.readInt() != 0) {
                intentCreateFromParcel = Intent.CREATOR.createFromParcel(parcel);
            } else {
                intentCreateFromParcel = null;
            }
            String string2 = parcel.readString();
            IBinder strongBinder = parcel.readStrongBinder();
            String string3 = parcel.readString();
            int i = parcel.readInt();
            int i2 = parcel.readInt();
            if (parcel.readInt() != 0) {
                profilerInfoCreateFromParcel = ProfilerInfo.CREATOR.createFromParcel(parcel);
            } else {
                profilerInfoCreateFromParcel = null;
            }
            if (parcel.readInt() != 0) {
                bundleCreateFromParcel = Bundle.CREATOR.createFromParcel(parcel);
            } else {
                bundleCreateFromParcel = null;
            }
            WaitResult waitResultStartActivityAndWait = startActivityAndWait(iApplicationThreadAsInterface, string, intentCreateFromParcel, string2, strongBinder, string3, i, i2, profilerInfoCreateFromParcel, bundleCreateFromParcel, parcel.readInt());
            parcel2.writeNoException();
            if (waitResultStartActivityAndWait != null) {
                parcel2.writeInt(1);
                waitResultStartActivityAndWait.writeToParcel(parcel2, 1);
            } else {
                parcel2.writeInt(0);
            }
            return true;
        }

        private boolean onTransact$startActivityWithConfig$(Parcel parcel, Parcel parcel2) throws RemoteException {
            Intent intentCreateFromParcel;
            Configuration configurationCreateFromParcel;
            Bundle bundleCreateFromParcel;
            parcel.enforceInterface(DESCRIPTOR);
            IApplicationThread iApplicationThreadAsInterface = IApplicationThread.Stub.asInterface(parcel.readStrongBinder());
            String string = parcel.readString();
            if (parcel.readInt() != 0) {
                intentCreateFromParcel = Intent.CREATOR.createFromParcel(parcel);
            } else {
                intentCreateFromParcel = null;
            }
            String string2 = parcel.readString();
            IBinder strongBinder = parcel.readStrongBinder();
            String string3 = parcel.readString();
            int i = parcel.readInt();
            int i2 = parcel.readInt();
            if (parcel.readInt() != 0) {
                configurationCreateFromParcel = Configuration.CREATOR.createFromParcel(parcel);
            } else {
                configurationCreateFromParcel = null;
            }
            if (parcel.readInt() != 0) {
                bundleCreateFromParcel = Bundle.CREATOR.createFromParcel(parcel);
            } else {
                bundleCreateFromParcel = null;
            }
            int iStartActivityWithConfig = startActivityWithConfig(iApplicationThreadAsInterface, string, intentCreateFromParcel, string2, strongBinder, string3, i, i2, configurationCreateFromParcel, bundleCreateFromParcel, parcel.readInt());
            parcel2.writeNoException();
            parcel2.writeInt(iStartActivityWithConfig);
            return true;
        }

        private boolean onTransact$crashApplication$(Parcel parcel, Parcel parcel2) throws RemoteException {
            parcel.enforceInterface(DESCRIPTOR);
            crashApplication(parcel.readInt(), parcel.readInt(), parcel.readString(), parcel.readInt(), parcel.readString(), parcel.readInt() != 0);
            parcel2.writeNoException();
            return true;
        }

        private boolean onTransact$grantUriPermissionFromOwner$(Parcel parcel, Parcel parcel2) throws RemoteException {
            Uri uriCreateFromParcel;
            parcel.enforceInterface(DESCRIPTOR);
            IBinder strongBinder = parcel.readStrongBinder();
            int i = parcel.readInt();
            String string = parcel.readString();
            if (parcel.readInt() != 0) {
                uriCreateFromParcel = Uri.CREATOR.createFromParcel(parcel);
            } else {
                uriCreateFromParcel = null;
            }
            grantUriPermissionFromOwner(strongBinder, i, string, uriCreateFromParcel, parcel.readInt(), parcel.readInt(), parcel.readInt());
            parcel2.writeNoException();
            return true;
        }

        private boolean onTransact$checkGrantUriPermission$(Parcel parcel, Parcel parcel2) throws RemoteException {
            Uri uriCreateFromParcel;
            parcel.enforceInterface(DESCRIPTOR);
            int i = parcel.readInt();
            String string = parcel.readString();
            if (parcel.readInt() != 0) {
                uriCreateFromParcel = Uri.CREATOR.createFromParcel(parcel);
            } else {
                uriCreateFromParcel = null;
            }
            int iCheckGrantUriPermission = checkGrantUriPermission(i, string, uriCreateFromParcel, parcel.readInt(), parcel.readInt());
            parcel2.writeNoException();
            parcel2.writeInt(iCheckGrantUriPermission);
            return true;
        }

        private boolean onTransact$dumpHeap$(Parcel parcel, Parcel parcel2) throws RemoteException {
            ParcelFileDescriptor parcelFileDescriptorCreateFromParcel;
            parcel.enforceInterface(DESCRIPTOR);
            String string = parcel.readString();
            int i = parcel.readInt();
            boolean z = parcel.readInt() != 0;
            boolean z2 = parcel.readInt() != 0;
            boolean z3 = parcel.readInt() != 0;
            String string2 = parcel.readString();
            if (parcel.readInt() != 0) {
                parcelFileDescriptorCreateFromParcel = ParcelFileDescriptor.CREATOR.createFromParcel(parcel);
            } else {
                parcelFileDescriptorCreateFromParcel = null;
            }
            boolean zDumpHeap = dumpHeap(string, i, z, z2, z3, string2, parcelFileDescriptorCreateFromParcel);
            parcel2.writeNoException();
            parcel2.writeInt(zDumpHeap ? 1 : 0);
            return true;
        }

        private boolean onTransact$startActivities$(Parcel parcel, Parcel parcel2) throws RemoteException {
            Bundle bundleCreateFromParcel;
            parcel.enforceInterface(DESCRIPTOR);
            IApplicationThread iApplicationThreadAsInterface = IApplicationThread.Stub.asInterface(parcel.readStrongBinder());
            String string = parcel.readString();
            Intent[] intentArr = (Intent[]) parcel.createTypedArray(Intent.CREATOR);
            String[] strArrCreateStringArray = parcel.createStringArray();
            IBinder strongBinder = parcel.readStrongBinder();
            if (parcel.readInt() != 0) {
                bundleCreateFromParcel = Bundle.CREATOR.createFromParcel(parcel);
            } else {
                bundleCreateFromParcel = null;
            }
            int iStartActivities = startActivities(iApplicationThreadAsInterface, string, intentArr, strArrCreateStringArray, strongBinder, bundleCreateFromParcel, parcel.readInt());
            parcel2.writeNoException();
            parcel2.writeInt(iStartActivities);
            return true;
        }

        private boolean onTransact$startActivityAsUser$(Parcel parcel, Parcel parcel2) throws RemoteException {
            Intent intentCreateFromParcel;
            ProfilerInfo profilerInfoCreateFromParcel;
            Bundle bundleCreateFromParcel;
            parcel.enforceInterface(DESCRIPTOR);
            IApplicationThread iApplicationThreadAsInterface = IApplicationThread.Stub.asInterface(parcel.readStrongBinder());
            String string = parcel.readString();
            if (parcel.readInt() != 0) {
                intentCreateFromParcel = Intent.CREATOR.createFromParcel(parcel);
            } else {
                intentCreateFromParcel = null;
            }
            String string2 = parcel.readString();
            IBinder strongBinder = parcel.readStrongBinder();
            String string3 = parcel.readString();
            int i = parcel.readInt();
            int i2 = parcel.readInt();
            if (parcel.readInt() != 0) {
                profilerInfoCreateFromParcel = ProfilerInfo.CREATOR.createFromParcel(parcel);
            } else {
                profilerInfoCreateFromParcel = null;
            }
            if (parcel.readInt() != 0) {
                bundleCreateFromParcel = Bundle.CREATOR.createFromParcel(parcel);
            } else {
                bundleCreateFromParcel = null;
            }
            int iStartActivityAsUser = startActivityAsUser(iApplicationThreadAsInterface, string, intentCreateFromParcel, string2, strongBinder, string3, i, i2, profilerInfoCreateFromParcel, bundleCreateFromParcel, parcel.readInt());
            parcel2.writeNoException();
            parcel2.writeInt(iStartActivityAsUser);
            return true;
        }

        private boolean onTransact$reportAssistContextExtras$(Parcel parcel, Parcel parcel2) throws RemoteException {
            Bundle bundleCreateFromParcel;
            AssistStructure assistStructureCreateFromParcel;
            AssistContent assistContentCreateFromParcel;
            Uri uriCreateFromParcel;
            parcel.enforceInterface(DESCRIPTOR);
            IBinder strongBinder = parcel.readStrongBinder();
            if (parcel.readInt() != 0) {
                bundleCreateFromParcel = Bundle.CREATOR.createFromParcel(parcel);
            } else {
                bundleCreateFromParcel = null;
            }
            if (parcel.readInt() != 0) {
                assistStructureCreateFromParcel = AssistStructure.CREATOR.createFromParcel(parcel);
            } else {
                assistStructureCreateFromParcel = null;
            }
            if (parcel.readInt() != 0) {
                assistContentCreateFromParcel = AssistContent.CREATOR.createFromParcel(parcel);
            } else {
                assistContentCreateFromParcel = null;
            }
            if (parcel.readInt() != 0) {
                uriCreateFromParcel = Uri.CREATOR.createFromParcel(parcel);
            } else {
                uriCreateFromParcel = null;
            }
            reportAssistContextExtras(strongBinder, bundleCreateFromParcel, assistStructureCreateFromParcel, assistContentCreateFromParcel, uriCreateFromParcel);
            parcel2.writeNoException();
            return true;
        }

        private boolean onTransact$resizeStack$(Parcel parcel, Parcel parcel2) throws RemoteException {
            Rect rectCreateFromParcel;
            parcel.enforceInterface(DESCRIPTOR);
            int i = parcel.readInt();
            if (parcel.readInt() != 0) {
                rectCreateFromParcel = Rect.CREATOR.createFromParcel(parcel);
            } else {
                rectCreateFromParcel = null;
            }
            resizeStack(i, rectCreateFromParcel, parcel.readInt() != 0, parcel.readInt() != 0, parcel.readInt() != 0, parcel.readInt());
            parcel2.writeNoException();
            return true;
        }

        private boolean onTransact$startVoiceActivity$(Parcel parcel, Parcel parcel2) throws RemoteException {
            Intent intentCreateFromParcel;
            ProfilerInfo profilerInfoCreateFromParcel;
            Bundle bundleCreateFromParcel;
            parcel.enforceInterface(DESCRIPTOR);
            String string = parcel.readString();
            int i = parcel.readInt();
            int i2 = parcel.readInt();
            if (parcel.readInt() != 0) {
                intentCreateFromParcel = Intent.CREATOR.createFromParcel(parcel);
            } else {
                intentCreateFromParcel = null;
            }
            String string2 = parcel.readString();
            IVoiceInteractionSession iVoiceInteractionSessionAsInterface = IVoiceInteractionSession.Stub.asInterface(parcel.readStrongBinder());
            IVoiceInteractor iVoiceInteractorAsInterface = IVoiceInteractor.Stub.asInterface(parcel.readStrongBinder());
            int i3 = parcel.readInt();
            if (parcel.readInt() != 0) {
                profilerInfoCreateFromParcel = ProfilerInfo.CREATOR.createFromParcel(parcel);
            } else {
                profilerInfoCreateFromParcel = null;
            }
            if (parcel.readInt() != 0) {
                bundleCreateFromParcel = Bundle.CREATOR.createFromParcel(parcel);
            } else {
                bundleCreateFromParcel = null;
            }
            int iStartVoiceActivity = startVoiceActivity(string, i, i2, intentCreateFromParcel, string2, iVoiceInteractionSessionAsInterface, iVoiceInteractorAsInterface, i3, profilerInfoCreateFromParcel, bundleCreateFromParcel, parcel.readInt());
            parcel2.writeNoException();
            parcel2.writeInt(iStartVoiceActivity);
            return true;
        }

        private boolean onTransact$startAssistantActivity$(Parcel parcel, Parcel parcel2) throws RemoteException {
            Intent intentCreateFromParcel;
            Bundle bundleCreateFromParcel;
            parcel.enforceInterface(DESCRIPTOR);
            String string = parcel.readString();
            int i = parcel.readInt();
            int i2 = parcel.readInt();
            if (parcel.readInt() != 0) {
                intentCreateFromParcel = Intent.CREATOR.createFromParcel(parcel);
            } else {
                intentCreateFromParcel = null;
            }
            String string2 = parcel.readString();
            if (parcel.readInt() != 0) {
                bundleCreateFromParcel = Bundle.CREATOR.createFromParcel(parcel);
            } else {
                bundleCreateFromParcel = null;
            }
            int iStartAssistantActivity = startAssistantActivity(string, i, i2, intentCreateFromParcel, string2, bundleCreateFromParcel, parcel.readInt());
            parcel2.writeNoException();
            parcel2.writeInt(iStartAssistantActivity);
            return true;
        }

        private boolean onTransact$startActivityAsCaller$(Parcel parcel, Parcel parcel2) throws RemoteException {
            Intent intentCreateFromParcel;
            ProfilerInfo profilerInfoCreateFromParcel;
            Bundle bundleCreateFromParcel;
            parcel.enforceInterface(DESCRIPTOR);
            IApplicationThread iApplicationThreadAsInterface = IApplicationThread.Stub.asInterface(parcel.readStrongBinder());
            String string = parcel.readString();
            if (parcel.readInt() != 0) {
                intentCreateFromParcel = Intent.CREATOR.createFromParcel(parcel);
            } else {
                intentCreateFromParcel = null;
            }
            String string2 = parcel.readString();
            IBinder strongBinder = parcel.readStrongBinder();
            String string3 = parcel.readString();
            int i = parcel.readInt();
            int i2 = parcel.readInt();
            if (parcel.readInt() != 0) {
                profilerInfoCreateFromParcel = ProfilerInfo.CREATOR.createFromParcel(parcel);
            } else {
                profilerInfoCreateFromParcel = null;
            }
            if (parcel.readInt() != 0) {
                bundleCreateFromParcel = Bundle.CREATOR.createFromParcel(parcel);
            } else {
                bundleCreateFromParcel = null;
            }
            int iStartActivityAsCaller = startActivityAsCaller(iApplicationThreadAsInterface, string, intentCreateFromParcel, string2, strongBinder, string3, i, i2, profilerInfoCreateFromParcel, bundleCreateFromParcel, parcel.readInt() != 0, parcel.readInt());
            parcel2.writeNoException();
            parcel2.writeInt(iStartActivityAsCaller);
            return true;
        }

        private boolean onTransact$launchAssistIntent$(Parcel parcel, Parcel parcel2) throws RemoteException {
            Intent intentCreateFromParcel;
            parcel.enforceInterface(DESCRIPTOR);
            if (parcel.readInt() != 0) {
                intentCreateFromParcel = Intent.CREATOR.createFromParcel(parcel);
            } else {
                intentCreateFromParcel = null;
            }
            boolean zLaunchAssistIntent = launchAssistIntent(intentCreateFromParcel, parcel.readInt(), parcel.readString(), parcel.readInt(), parcel.readInt() != 0 ? Bundle.CREATOR.createFromParcel(parcel) : null);
            parcel2.writeNoException();
            parcel2.writeInt(zLaunchAssistIntent ? 1 : 0);
            return true;
        }

        private boolean onTransact$requestAssistContextExtras$(Parcel parcel, Parcel parcel2) throws RemoteException {
            Bundle bundleCreateFromParcel;
            parcel.enforceInterface(DESCRIPTOR);
            int i = parcel.readInt();
            IAssistDataReceiver iAssistDataReceiverAsInterface = IAssistDataReceiver.Stub.asInterface(parcel.readStrongBinder());
            if (parcel.readInt() != 0) {
                bundleCreateFromParcel = Bundle.CREATOR.createFromParcel(parcel);
            } else {
                bundleCreateFromParcel = null;
            }
            boolean zRequestAssistContextExtras = requestAssistContextExtras(i, iAssistDataReceiverAsInterface, bundleCreateFromParcel, parcel.readStrongBinder(), parcel.readInt() != 0, parcel.readInt() != 0);
            parcel2.writeNoException();
            parcel2.writeInt(zRequestAssistContextExtras ? 1 : 0);
            return true;
        }

        private boolean onTransact$setTaskWindowingModeSplitScreenPrimary$(Parcel parcel, Parcel parcel2) throws RemoteException {
            Rect rectCreateFromParcel;
            parcel.enforceInterface(DESCRIPTOR);
            int i = parcel.readInt();
            int i2 = parcel.readInt();
            boolean z = parcel.readInt() != 0;
            boolean z2 = parcel.readInt() != 0;
            if (parcel.readInt() != 0) {
                rectCreateFromParcel = Rect.CREATOR.createFromParcel(parcel);
            } else {
                rectCreateFromParcel = null;
            }
            boolean taskWindowingModeSplitScreenPrimary = setTaskWindowingModeSplitScreenPrimary(i, i2, z, z2, rectCreateFromParcel, parcel.readInt() != 0);
            parcel2.writeNoException();
            parcel2.writeInt(taskWindowingModeSplitScreenPrimary ? 1 : 0);
            return true;
        }

        private boolean onTransact$resizeDockedStack$(Parcel parcel, Parcel parcel2) throws RemoteException {
            Rect rectCreateFromParcel;
            Rect rectCreateFromParcel2;
            Rect rectCreateFromParcel3;
            Rect rectCreateFromParcel4;
            parcel.enforceInterface(DESCRIPTOR);
            if (parcel.readInt() != 0) {
                rectCreateFromParcel = Rect.CREATOR.createFromParcel(parcel);
            } else {
                rectCreateFromParcel = null;
            }
            if (parcel.readInt() != 0) {
                rectCreateFromParcel2 = Rect.CREATOR.createFromParcel(parcel);
            } else {
                rectCreateFromParcel2 = null;
            }
            if (parcel.readInt() != 0) {
                rectCreateFromParcel3 = Rect.CREATOR.createFromParcel(parcel);
            } else {
                rectCreateFromParcel3 = null;
            }
            if (parcel.readInt() != 0) {
                rectCreateFromParcel4 = Rect.CREATOR.createFromParcel(parcel);
            } else {
                rectCreateFromParcel4 = null;
            }
            resizeDockedStack(rectCreateFromParcel, rectCreateFromParcel2, rectCreateFromParcel3, rectCreateFromParcel4, parcel.readInt() != 0 ? Rect.CREATOR.createFromParcel(parcel) : null);
            parcel2.writeNoException();
            return true;
        }

        private boolean onTransact$sendIntentSender$(Parcel parcel, Parcel parcel2) throws RemoteException {
            Intent intentCreateFromParcel;
            Bundle bundleCreateFromParcel;
            parcel.enforceInterface(DESCRIPTOR);
            IIntentSender iIntentSenderAsInterface = IIntentSender.Stub.asInterface(parcel.readStrongBinder());
            IBinder strongBinder = parcel.readStrongBinder();
            int i = parcel.readInt();
            if (parcel.readInt() != 0) {
                intentCreateFromParcel = Intent.CREATOR.createFromParcel(parcel);
            } else {
                intentCreateFromParcel = null;
            }
            String string = parcel.readString();
            IIntentReceiver iIntentReceiverAsInterface = IIntentReceiver.Stub.asInterface(parcel.readStrongBinder());
            String string2 = parcel.readString();
            if (parcel.readInt() != 0) {
                bundleCreateFromParcel = Bundle.CREATOR.createFromParcel(parcel);
            } else {
                bundleCreateFromParcel = null;
            }
            int iSendIntentSender = sendIntentSender(iIntentSenderAsInterface, strongBinder, i, intentCreateFromParcel, string, iIntentReceiverAsInterface, string2, bundleCreateFromParcel);
            parcel2.writeNoException();
            parcel2.writeInt(iSendIntentSender);
            return true;
        }
    }
}
