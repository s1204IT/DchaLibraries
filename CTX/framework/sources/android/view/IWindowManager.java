package android.view;

import android.app.IAssistDataReceiver;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.GraphicBuffer;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Region;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.IRemoteCallback;
import android.os.Parcel;
import android.os.RemoteException;
import android.text.TextUtils;
import android.view.IAppTransitionAnimationSpecsFuture;
import android.view.IDockedStackListener;
import android.view.IOnKeyguardExitResult;
import android.view.IPinnedStackListener;
import android.view.IRotationWatcher;
import android.view.IWallpaperVisibilityListener;
import android.view.IWindowSession;
import android.view.IWindowSessionCallback;
import com.android.internal.os.IResultReceiver;
import com.android.internal.policy.IKeyguardDismissCallback;
import com.android.internal.policy.IShortcutService;
import com.android.internal.view.IInputContext;
import com.android.internal.view.IInputMethodClient;

public interface IWindowManager extends IInterface {
    void addWindowToken(IBinder iBinder, int i, int i2) throws RemoteException;

    void clearForcedDisplayDensityForUser(int i, int i2) throws RemoteException;

    void clearForcedDisplaySize(int i) throws RemoteException;

    boolean clearWindowContentFrameStats(IBinder iBinder) throws RemoteException;

    void closeSystemDialogs(String str) throws RemoteException;

    void createInputConsumer(IBinder iBinder, String str, InputChannel inputChannel) throws RemoteException;

    boolean destroyInputConsumer(String str) throws RemoteException;

    void disableKeyguard(IBinder iBinder, String str) throws RemoteException;

    void dismissKeyguard(IKeyguardDismissCallback iKeyguardDismissCallback, CharSequence charSequence) throws RemoteException;

    void dontOverrideDisplayInfo(int i) throws RemoteException;

    void enableScreenIfNeeded() throws RemoteException;

    void endProlongedAnimations() throws RemoteException;

    void executeAppTransition() throws RemoteException;

    void exitKeyguardSecurely(IOnKeyguardExitResult iOnKeyguardExitResult) throws RemoteException;

    void freezeRotation(int i) throws RemoteException;

    float getAnimationScale(int i) throws RemoteException;

    float[] getAnimationScales() throws RemoteException;

    int getBaseDisplayDensity(int i) throws RemoteException;

    void getBaseDisplaySize(int i, Point point) throws RemoteException;

    float getCurrentAnimatorScale() throws RemoteException;

    Region getCurrentImeTouchRegion() throws RemoteException;

    int getDefaultDisplayRotation() throws RemoteException;

    int getDockedStackSide() throws RemoteException;

    int getInitialDisplayDensity(int i) throws RemoteException;

    void getInitialDisplaySize(int i, Point point) throws RemoteException;

    int getNavBarPosition() throws RemoteException;

    int getPendingAppTransition() throws RemoteException;

    int getPreferredOptionsPanelGravity() throws RemoteException;

    void getStableInsets(int i, Rect rect) throws RemoteException;

    WindowContentFrameStats getWindowContentFrameStats(IBinder iBinder) throws RemoteException;

    boolean hasNavigationBar() throws RemoteException;

    boolean inputMethodClientHasFocus(IInputMethodClient iInputMethodClient) throws RemoteException;

    boolean isKeyguardLocked() throws RemoteException;

    boolean isKeyguardSecure() throws RemoteException;

    boolean isRotationFrozen() throws RemoteException;

    boolean isSafeModeEnabled() throws RemoteException;

    boolean isViewServerRunning() throws RemoteException;

    boolean isWindowTraceEnabled() throws RemoteException;

    void lockNow(Bundle bundle) throws RemoteException;

    IWindowSession openSession(IWindowSessionCallback iWindowSessionCallback, IInputMethodClient iInputMethodClient, IInputContext iInputContext) throws RemoteException;

    void overridePendingAppTransition(String str, int i, int i2, IRemoteCallback iRemoteCallback) throws RemoteException;

    void overridePendingAppTransitionAspectScaledThumb(GraphicBuffer graphicBuffer, int i, int i2, int i3, int i4, IRemoteCallback iRemoteCallback, boolean z) throws RemoteException;

    void overridePendingAppTransitionClipReveal(int i, int i2, int i3, int i4) throws RemoteException;

    void overridePendingAppTransitionInPlace(String str, int i) throws RemoteException;

    void overridePendingAppTransitionMultiThumb(AppTransitionAnimationSpec[] appTransitionAnimationSpecArr, IRemoteCallback iRemoteCallback, IRemoteCallback iRemoteCallback2, boolean z) throws RemoteException;

    void overridePendingAppTransitionMultiThumbFuture(IAppTransitionAnimationSpecsFuture iAppTransitionAnimationSpecsFuture, IRemoteCallback iRemoteCallback, boolean z) throws RemoteException;

    void overridePendingAppTransitionRemote(RemoteAnimationAdapter remoteAnimationAdapter) throws RemoteException;

    void overridePendingAppTransitionScaleUp(int i, int i2, int i3, int i4) throws RemoteException;

    void overridePendingAppTransitionThumb(GraphicBuffer graphicBuffer, int i, int i2, IRemoteCallback iRemoteCallback, boolean z) throws RemoteException;

    void prepareAppTransition(int i, boolean z) throws RemoteException;

    void reenableKeyguard(IBinder iBinder) throws RemoteException;

    void refreshScreenCaptureDisabled(int i) throws RemoteException;

    void registerDockedStackListener(IDockedStackListener iDockedStackListener) throws RemoteException;

    void registerPinnedStackListener(int i, IPinnedStackListener iPinnedStackListener) throws RemoteException;

    void registerShortcutKey(long j, IShortcutService iShortcutService) throws RemoteException;

    boolean registerWallpaperVisibilityListener(IWallpaperVisibilityListener iWallpaperVisibilityListener, int i) throws RemoteException;

    void removeRotationWatcher(IRotationWatcher iRotationWatcher) throws RemoteException;

    void removeWindowToken(IBinder iBinder, int i) throws RemoteException;

    void requestAppKeyboardShortcuts(IResultReceiver iResultReceiver, int i) throws RemoteException;

    boolean requestAssistScreenshot(IAssistDataReceiver iAssistDataReceiver) throws RemoteException;

    void requestUserActivityNotification() throws RemoteException;

    Bitmap screenshotWallpaper() throws RemoteException;

    void setAnimationScale(int i, float f) throws RemoteException;

    void setAnimationScales(float[] fArr) throws RemoteException;

    void setDockedStackDividerTouchRegion(Rect rect) throws RemoteException;

    void setEventDispatching(boolean z) throws RemoteException;

    void setFocusedApp(IBinder iBinder, boolean z) throws RemoteException;

    void setForcedDisplayDensityForUser(int i, int i2, int i3) throws RemoteException;

    void setForcedDisplayScalingMode(int i, int i2) throws RemoteException;

    void setForcedDisplaySize(int i, int i2, int i3) throws RemoteException;

    void setInTouchMode(boolean z) throws RemoteException;

    void setNavBarVirtualKeyHapticFeedbackEnabled(boolean z) throws RemoteException;

    int[] setNewDisplayOverrideConfiguration(Configuration configuration, int i) throws RemoteException;

    void setOverscan(int i, int i2, int i3, int i4, int i5) throws RemoteException;

    void setPipVisibility(boolean z) throws RemoteException;

    void setRecentsVisibility(boolean z) throws RemoteException;

    void setResizeDimLayer(boolean z, int i, float f) throws RemoteException;

    void setShelfHeight(boolean z, int i) throws RemoteException;

    void setStrictModeVisualIndicatorPreference(String str) throws RemoteException;

    void setSwitchingUser(boolean z) throws RemoteException;

    void showStrictModeViolation(boolean z) throws RemoteException;

    void startFreezingScreen(int i, int i2) throws RemoteException;

    boolean startViewServer(int i) throws RemoteException;

    void startWindowTrace() throws RemoteException;

    void statusBarVisibilityChanged(int i) throws RemoteException;

    void stopFreezingScreen() throws RemoteException;

    boolean stopViewServer() throws RemoteException;

    void stopWindowTrace() throws RemoteException;

    void thawRotation() throws RemoteException;

    void unregisterWallpaperVisibilityListener(IWallpaperVisibilityListener iWallpaperVisibilityListener, int i) throws RemoteException;

    Configuration updateOrientationFromAppTokens(Configuration configuration, IBinder iBinder, int i) throws RemoteException;

    void updateRotation(boolean z, boolean z2) throws RemoteException;

    int watchRotation(IRotationWatcher iRotationWatcher, int i) throws RemoteException;

    public static abstract class Stub extends Binder implements IWindowManager {
        private static final String DESCRIPTOR = "android.view.IWindowManager";
        static final int TRANSACTION_addWindowToken = 17;
        static final int TRANSACTION_clearForcedDisplayDensityForUser = 13;
        static final int TRANSACTION_clearForcedDisplaySize = 9;
        static final int TRANSACTION_clearWindowContentFrameStats = 76;
        static final int TRANSACTION_closeSystemDialogs = 44;
        static final int TRANSACTION_createInputConsumer = 86;
        static final int TRANSACTION_destroyInputConsumer = 87;
        static final int TRANSACTION_disableKeyguard = 37;
        static final int TRANSACTION_dismissKeyguard = 42;
        static final int TRANSACTION_dontOverrideDisplayInfo = 93;
        static final int TRANSACTION_enableScreenIfNeeded = 75;
        static final int TRANSACTION_endProlongedAnimations = 32;
        static final int TRANSACTION_executeAppTransition = 31;
        static final int TRANSACTION_exitKeyguardSecurely = 39;
        static final int TRANSACTION_freezeRotation = 59;
        static final int TRANSACTION_getAnimationScale = 45;
        static final int TRANSACTION_getAnimationScales = 46;
        static final int TRANSACTION_getBaseDisplayDensity = 11;
        static final int TRANSACTION_getBaseDisplaySize = 7;
        static final int TRANSACTION_getCurrentAnimatorScale = 49;
        static final int TRANSACTION_getCurrentImeTouchRegion = 88;
        static final int TRANSACTION_getDefaultDisplayRotation = 55;
        static final int TRANSACTION_getDockedStackSide = 78;
        static final int TRANSACTION_getInitialDisplayDensity = 10;
        static final int TRANSACTION_getInitialDisplaySize = 6;
        static final int TRANSACTION_getNavBarPosition = 72;
        static final int TRANSACTION_getPendingAppTransition = 21;
        static final int TRANSACTION_getPreferredOptionsPanelGravity = 58;
        static final int TRANSACTION_getStableInsets = 84;
        static final int TRANSACTION_getWindowContentFrameStats = 77;
        static final int TRANSACTION_hasNavigationBar = 71;
        static final int TRANSACTION_inputMethodClientHasFocus = 5;
        static final int TRANSACTION_isKeyguardLocked = 40;
        static final int TRANSACTION_isKeyguardSecure = 41;
        static final int TRANSACTION_isRotationFrozen = 61;
        static final int TRANSACTION_isSafeModeEnabled = 74;
        static final int TRANSACTION_isViewServerRunning = 3;
        static final int TRANSACTION_isWindowTraceEnabled = 91;
        static final int TRANSACTION_lockNow = 73;
        static final int TRANSACTION_openSession = 4;
        static final int TRANSACTION_overridePendingAppTransition = 22;
        static final int TRANSACTION_overridePendingAppTransitionAspectScaledThumb = 26;
        static final int TRANSACTION_overridePendingAppTransitionClipReveal = 24;
        static final int TRANSACTION_overridePendingAppTransitionInPlace = 28;
        static final int TRANSACTION_overridePendingAppTransitionMultiThumb = 27;
        static final int TRANSACTION_overridePendingAppTransitionMultiThumbFuture = 29;
        static final int TRANSACTION_overridePendingAppTransitionRemote = 30;
        static final int TRANSACTION_overridePendingAppTransitionScaleUp = 23;
        static final int TRANSACTION_overridePendingAppTransitionThumb = 25;
        static final int TRANSACTION_prepareAppTransition = 20;
        static final int TRANSACTION_reenableKeyguard = 38;
        static final int TRANSACTION_refreshScreenCaptureDisabled = 53;
        static final int TRANSACTION_registerDockedStackListener = 80;
        static final int TRANSACTION_registerPinnedStackListener = 81;
        static final int TRANSACTION_registerShortcutKey = 85;
        static final int TRANSACTION_registerWallpaperVisibilityListener = 63;
        static final int TRANSACTION_removeRotationWatcher = 57;
        static final int TRANSACTION_removeWindowToken = 18;
        static final int TRANSACTION_requestAppKeyboardShortcuts = 83;
        static final int TRANSACTION_requestAssistScreenshot = 65;
        static final int TRANSACTION_requestUserActivityNotification = 92;
        static final int TRANSACTION_screenshotWallpaper = 62;
        static final int TRANSACTION_setAnimationScale = 47;
        static final int TRANSACTION_setAnimationScales = 48;
        static final int TRANSACTION_setDockedStackDividerTouchRegion = 79;
        static final int TRANSACTION_setEventDispatching = 16;
        static final int TRANSACTION_setFocusedApp = 19;
        static final int TRANSACTION_setForcedDisplayDensityForUser = 12;
        static final int TRANSACTION_setForcedDisplayScalingMode = 14;
        static final int TRANSACTION_setForcedDisplaySize = 8;
        static final int TRANSACTION_setInTouchMode = 50;
        static final int TRANSACTION_setNavBarVirtualKeyHapticFeedbackEnabled = 70;
        static final int TRANSACTION_setNewDisplayOverrideConfiguration = 34;
        static final int TRANSACTION_setOverscan = 15;
        static final int TRANSACTION_setPipVisibility = 68;
        static final int TRANSACTION_setRecentsVisibility = 67;
        static final int TRANSACTION_setResizeDimLayer = 82;
        static final int TRANSACTION_setShelfHeight = 69;
        static final int TRANSACTION_setStrictModeVisualIndicatorPreference = 52;
        static final int TRANSACTION_setSwitchingUser = 43;
        static final int TRANSACTION_showStrictModeViolation = 51;
        static final int TRANSACTION_startFreezingScreen = 35;
        static final int TRANSACTION_startViewServer = 1;
        static final int TRANSACTION_startWindowTrace = 89;
        static final int TRANSACTION_statusBarVisibilityChanged = 66;
        static final int TRANSACTION_stopFreezingScreen = 36;
        static final int TRANSACTION_stopViewServer = 2;
        static final int TRANSACTION_stopWindowTrace = 90;
        static final int TRANSACTION_thawRotation = 60;
        static final int TRANSACTION_unregisterWallpaperVisibilityListener = 64;
        static final int TRANSACTION_updateOrientationFromAppTokens = 33;
        static final int TRANSACTION_updateRotation = 54;
        static final int TRANSACTION_watchRotation = 56;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IWindowManager asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface(DESCRIPTOR);
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof IWindowManager)) {
                return (IWindowManager) iInterfaceQueryLocalInterface;
            }
            return new Proxy(iBinder);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            GraphicBuffer graphicBufferCreateFromParcel;
            GraphicBuffer graphicBufferCreateFromParcel2;
            if (i == 1598968902) {
                parcel2.writeString(DESCRIPTOR);
                return true;
            }
            switch (i) {
                case 1:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zStartViewServer = startViewServer(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(zStartViewServer ? 1 : 0);
                    return true;
                case 2:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zStopViewServer = stopViewServer();
                    parcel2.writeNoException();
                    parcel2.writeInt(zStopViewServer ? 1 : 0);
                    return true;
                case 3:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zIsViewServerRunning = isViewServerRunning();
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsViewServerRunning ? 1 : 0);
                    return true;
                case 4:
                    parcel.enforceInterface(DESCRIPTOR);
                    IWindowSession iWindowSessionOpenSession = openSession(IWindowSessionCallback.Stub.asInterface(parcel.readStrongBinder()), IInputMethodClient.Stub.asInterface(parcel.readStrongBinder()), IInputContext.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    parcel2.writeStrongBinder(iWindowSessionOpenSession != null ? iWindowSessionOpenSession.asBinder() : null);
                    return true;
                case 5:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zInputMethodClientHasFocus = inputMethodClientHasFocus(IInputMethodClient.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    parcel2.writeInt(zInputMethodClientHasFocus ? 1 : 0);
                    return true;
                case 6:
                    parcel.enforceInterface(DESCRIPTOR);
                    int i3 = parcel.readInt();
                    Point point = new Point();
                    getInitialDisplaySize(i3, point);
                    parcel2.writeNoException();
                    parcel2.writeInt(1);
                    point.writeToParcel(parcel2, 1);
                    return true;
                case 7:
                    parcel.enforceInterface(DESCRIPTOR);
                    int i4 = parcel.readInt();
                    Point point2 = new Point();
                    getBaseDisplaySize(i4, point2);
                    parcel2.writeNoException();
                    parcel2.writeInt(1);
                    point2.writeToParcel(parcel2, 1);
                    return true;
                case 8:
                    parcel.enforceInterface(DESCRIPTOR);
                    setForcedDisplaySize(parcel.readInt(), parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 9:
                    parcel.enforceInterface(DESCRIPTOR);
                    clearForcedDisplaySize(parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 10:
                    parcel.enforceInterface(DESCRIPTOR);
                    int initialDisplayDensity = getInitialDisplayDensity(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(initialDisplayDensity);
                    return true;
                case 11:
                    parcel.enforceInterface(DESCRIPTOR);
                    int baseDisplayDensity = getBaseDisplayDensity(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(baseDisplayDensity);
                    return true;
                case 12:
                    parcel.enforceInterface(DESCRIPTOR);
                    setForcedDisplayDensityForUser(parcel.readInt(), parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 13:
                    parcel.enforceInterface(DESCRIPTOR);
                    clearForcedDisplayDensityForUser(parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 14:
                    parcel.enforceInterface(DESCRIPTOR);
                    setForcedDisplayScalingMode(parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 15:
                    parcel.enforceInterface(DESCRIPTOR);
                    setOverscan(parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 16:
                    parcel.enforceInterface(DESCRIPTOR);
                    setEventDispatching(parcel.readInt() != 0);
                    parcel2.writeNoException();
                    return true;
                case 17:
                    parcel.enforceInterface(DESCRIPTOR);
                    addWindowToken(parcel.readStrongBinder(), parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 18:
                    parcel.enforceInterface(DESCRIPTOR);
                    removeWindowToken(parcel.readStrongBinder(), parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 19:
                    parcel.enforceInterface(DESCRIPTOR);
                    setFocusedApp(parcel.readStrongBinder(), parcel.readInt() != 0);
                    parcel2.writeNoException();
                    return true;
                case 20:
                    parcel.enforceInterface(DESCRIPTOR);
                    prepareAppTransition(parcel.readInt(), parcel.readInt() != 0);
                    parcel2.writeNoException();
                    return true;
                case 21:
                    parcel.enforceInterface(DESCRIPTOR);
                    int pendingAppTransition = getPendingAppTransition();
                    parcel2.writeNoException();
                    parcel2.writeInt(pendingAppTransition);
                    return true;
                case 22:
                    parcel.enforceInterface(DESCRIPTOR);
                    overridePendingAppTransition(parcel.readString(), parcel.readInt(), parcel.readInt(), IRemoteCallback.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    return true;
                case 23:
                    parcel.enforceInterface(DESCRIPTOR);
                    overridePendingAppTransitionScaleUp(parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 24:
                    parcel.enforceInterface(DESCRIPTOR);
                    overridePendingAppTransitionClipReveal(parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 25:
                    parcel.enforceInterface(DESCRIPTOR);
                    if (parcel.readInt() != 0) {
                        graphicBufferCreateFromParcel = GraphicBuffer.CREATOR.createFromParcel(parcel);
                    } else {
                        graphicBufferCreateFromParcel = null;
                    }
                    overridePendingAppTransitionThumb(graphicBufferCreateFromParcel, parcel.readInt(), parcel.readInt(), IRemoteCallback.Stub.asInterface(parcel.readStrongBinder()), parcel.readInt() != 0);
                    parcel2.writeNoException();
                    return true;
                case 26:
                    parcel.enforceInterface(DESCRIPTOR);
                    if (parcel.readInt() != 0) {
                        graphicBufferCreateFromParcel2 = GraphicBuffer.CREATOR.createFromParcel(parcel);
                    } else {
                        graphicBufferCreateFromParcel2 = null;
                    }
                    overridePendingAppTransitionAspectScaledThumb(graphicBufferCreateFromParcel2, parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.readInt(), IRemoteCallback.Stub.asInterface(parcel.readStrongBinder()), parcel.readInt() != 0);
                    parcel2.writeNoException();
                    return true;
                case 27:
                    parcel.enforceInterface(DESCRIPTOR);
                    overridePendingAppTransitionMultiThumb((AppTransitionAnimationSpec[]) parcel.createTypedArray(AppTransitionAnimationSpec.CREATOR), IRemoteCallback.Stub.asInterface(parcel.readStrongBinder()), IRemoteCallback.Stub.asInterface(parcel.readStrongBinder()), parcel.readInt() != 0);
                    parcel2.writeNoException();
                    return true;
                case 28:
                    parcel.enforceInterface(DESCRIPTOR);
                    overridePendingAppTransitionInPlace(parcel.readString(), parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 29:
                    parcel.enforceInterface(DESCRIPTOR);
                    overridePendingAppTransitionMultiThumbFuture(IAppTransitionAnimationSpecsFuture.Stub.asInterface(parcel.readStrongBinder()), IRemoteCallback.Stub.asInterface(parcel.readStrongBinder()), parcel.readInt() != 0);
                    parcel2.writeNoException();
                    return true;
                case 30:
                    parcel.enforceInterface(DESCRIPTOR);
                    overridePendingAppTransitionRemote(parcel.readInt() != 0 ? RemoteAnimationAdapter.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    return true;
                case 31:
                    parcel.enforceInterface(DESCRIPTOR);
                    executeAppTransition();
                    parcel2.writeNoException();
                    return true;
                case 32:
                    parcel.enforceInterface(DESCRIPTOR);
                    endProlongedAnimations();
                    parcel2.writeNoException();
                    return true;
                case 33:
                    parcel.enforceInterface(DESCRIPTOR);
                    Configuration configurationUpdateOrientationFromAppTokens = updateOrientationFromAppTokens(parcel.readInt() != 0 ? Configuration.CREATOR.createFromParcel(parcel) : null, parcel.readStrongBinder(), parcel.readInt());
                    parcel2.writeNoException();
                    if (configurationUpdateOrientationFromAppTokens != null) {
                        parcel2.writeInt(1);
                        configurationUpdateOrientationFromAppTokens.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 34:
                    parcel.enforceInterface(DESCRIPTOR);
                    int[] newDisplayOverrideConfiguration = setNewDisplayOverrideConfiguration(parcel.readInt() != 0 ? Configuration.CREATOR.createFromParcel(parcel) : null, parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeIntArray(newDisplayOverrideConfiguration);
                    return true;
                case 35:
                    parcel.enforceInterface(DESCRIPTOR);
                    startFreezingScreen(parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 36:
                    parcel.enforceInterface(DESCRIPTOR);
                    stopFreezingScreen();
                    parcel2.writeNoException();
                    return true;
                case 37:
                    parcel.enforceInterface(DESCRIPTOR);
                    disableKeyguard(parcel.readStrongBinder(), parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case 38:
                    parcel.enforceInterface(DESCRIPTOR);
                    reenableKeyguard(parcel.readStrongBinder());
                    parcel2.writeNoException();
                    return true;
                case 39:
                    parcel.enforceInterface(DESCRIPTOR);
                    exitKeyguardSecurely(IOnKeyguardExitResult.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    return true;
                case 40:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zIsKeyguardLocked = isKeyguardLocked();
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsKeyguardLocked ? 1 : 0);
                    return true;
                case 41:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zIsKeyguardSecure = isKeyguardSecure();
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsKeyguardSecure ? 1 : 0);
                    return true;
                case 42:
                    parcel.enforceInterface(DESCRIPTOR);
                    dismissKeyguard(IKeyguardDismissCallback.Stub.asInterface(parcel.readStrongBinder()), parcel.readInt() != 0 ? TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    return true;
                case 43:
                    parcel.enforceInterface(DESCRIPTOR);
                    setSwitchingUser(parcel.readInt() != 0);
                    parcel2.writeNoException();
                    return true;
                case 44:
                    parcel.enforceInterface(DESCRIPTOR);
                    closeSystemDialogs(parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case 45:
                    parcel.enforceInterface(DESCRIPTOR);
                    float animationScale = getAnimationScale(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeFloat(animationScale);
                    return true;
                case 46:
                    parcel.enforceInterface(DESCRIPTOR);
                    float[] animationScales = getAnimationScales();
                    parcel2.writeNoException();
                    parcel2.writeFloatArray(animationScales);
                    return true;
                case 47:
                    parcel.enforceInterface(DESCRIPTOR);
                    setAnimationScale(parcel.readInt(), parcel.readFloat());
                    parcel2.writeNoException();
                    return true;
                case 48:
                    parcel.enforceInterface(DESCRIPTOR);
                    setAnimationScales(parcel.createFloatArray());
                    parcel2.writeNoException();
                    return true;
                case 49:
                    parcel.enforceInterface(DESCRIPTOR);
                    float currentAnimatorScale = getCurrentAnimatorScale();
                    parcel2.writeNoException();
                    parcel2.writeFloat(currentAnimatorScale);
                    return true;
                case 50:
                    parcel.enforceInterface(DESCRIPTOR);
                    setInTouchMode(parcel.readInt() != 0);
                    parcel2.writeNoException();
                    return true;
                case 51:
                    parcel.enforceInterface(DESCRIPTOR);
                    showStrictModeViolation(parcel.readInt() != 0);
                    parcel2.writeNoException();
                    return true;
                case 52:
                    parcel.enforceInterface(DESCRIPTOR);
                    setStrictModeVisualIndicatorPreference(parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case 53:
                    parcel.enforceInterface(DESCRIPTOR);
                    refreshScreenCaptureDisabled(parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 54:
                    parcel.enforceInterface(DESCRIPTOR);
                    updateRotation(parcel.readInt() != 0, parcel.readInt() != 0);
                    parcel2.writeNoException();
                    return true;
                case 55:
                    parcel.enforceInterface(DESCRIPTOR);
                    int defaultDisplayRotation = getDefaultDisplayRotation();
                    parcel2.writeNoException();
                    parcel2.writeInt(defaultDisplayRotation);
                    return true;
                case 56:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iWatchRotation = watchRotation(IRotationWatcher.Stub.asInterface(parcel.readStrongBinder()), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(iWatchRotation);
                    return true;
                case 57:
                    parcel.enforceInterface(DESCRIPTOR);
                    removeRotationWatcher(IRotationWatcher.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    return true;
                case 58:
                    parcel.enforceInterface(DESCRIPTOR);
                    int preferredOptionsPanelGravity = getPreferredOptionsPanelGravity();
                    parcel2.writeNoException();
                    parcel2.writeInt(preferredOptionsPanelGravity);
                    return true;
                case 59:
                    parcel.enforceInterface(DESCRIPTOR);
                    freezeRotation(parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 60:
                    parcel.enforceInterface(DESCRIPTOR);
                    thawRotation();
                    parcel2.writeNoException();
                    return true;
                case 61:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zIsRotationFrozen = isRotationFrozen();
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsRotationFrozen ? 1 : 0);
                    return true;
                case 62:
                    parcel.enforceInterface(DESCRIPTOR);
                    Bitmap bitmapScreenshotWallpaper = screenshotWallpaper();
                    parcel2.writeNoException();
                    if (bitmapScreenshotWallpaper != null) {
                        parcel2.writeInt(1);
                        bitmapScreenshotWallpaper.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 63:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zRegisterWallpaperVisibilityListener = registerWallpaperVisibilityListener(IWallpaperVisibilityListener.Stub.asInterface(parcel.readStrongBinder()), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(zRegisterWallpaperVisibilityListener ? 1 : 0);
                    return true;
                case 64:
                    parcel.enforceInterface(DESCRIPTOR);
                    unregisterWallpaperVisibilityListener(IWallpaperVisibilityListener.Stub.asInterface(parcel.readStrongBinder()), parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 65:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zRequestAssistScreenshot = requestAssistScreenshot(IAssistDataReceiver.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    parcel2.writeInt(zRequestAssistScreenshot ? 1 : 0);
                    return true;
                case 66:
                    parcel.enforceInterface(DESCRIPTOR);
                    statusBarVisibilityChanged(parcel.readInt());
                    return true;
                case 67:
                    parcel.enforceInterface(DESCRIPTOR);
                    setRecentsVisibility(parcel.readInt() != 0);
                    return true;
                case 68:
                    parcel.enforceInterface(DESCRIPTOR);
                    setPipVisibility(parcel.readInt() != 0);
                    return true;
                case 69:
                    parcel.enforceInterface(DESCRIPTOR);
                    setShelfHeight(parcel.readInt() != 0, parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 70:
                    parcel.enforceInterface(DESCRIPTOR);
                    setNavBarVirtualKeyHapticFeedbackEnabled(parcel.readInt() != 0);
                    parcel2.writeNoException();
                    return true;
                case 71:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zHasNavigationBar = hasNavigationBar();
                    parcel2.writeNoException();
                    parcel2.writeInt(zHasNavigationBar ? 1 : 0);
                    return true;
                case 72:
                    parcel.enforceInterface(DESCRIPTOR);
                    int navBarPosition = getNavBarPosition();
                    parcel2.writeNoException();
                    parcel2.writeInt(navBarPosition);
                    return true;
                case 73:
                    parcel.enforceInterface(DESCRIPTOR);
                    lockNow(parcel.readInt() != 0 ? Bundle.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    return true;
                case 74:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zIsSafeModeEnabled = isSafeModeEnabled();
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsSafeModeEnabled ? 1 : 0);
                    return true;
                case 75:
                    parcel.enforceInterface(DESCRIPTOR);
                    enableScreenIfNeeded();
                    parcel2.writeNoException();
                    return true;
                case 76:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zClearWindowContentFrameStats = clearWindowContentFrameStats(parcel.readStrongBinder());
                    parcel2.writeNoException();
                    parcel2.writeInt(zClearWindowContentFrameStats ? 1 : 0);
                    return true;
                case 77:
                    parcel.enforceInterface(DESCRIPTOR);
                    WindowContentFrameStats windowContentFrameStats = getWindowContentFrameStats(parcel.readStrongBinder());
                    parcel2.writeNoException();
                    if (windowContentFrameStats != null) {
                        parcel2.writeInt(1);
                        windowContentFrameStats.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 78:
                    parcel.enforceInterface(DESCRIPTOR);
                    int dockedStackSide = getDockedStackSide();
                    parcel2.writeNoException();
                    parcel2.writeInt(dockedStackSide);
                    return true;
                case 79:
                    parcel.enforceInterface(DESCRIPTOR);
                    setDockedStackDividerTouchRegion(parcel.readInt() != 0 ? Rect.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    return true;
                case 80:
                    parcel.enforceInterface(DESCRIPTOR);
                    registerDockedStackListener(IDockedStackListener.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    return true;
                case 81:
                    parcel.enforceInterface(DESCRIPTOR);
                    registerPinnedStackListener(parcel.readInt(), IPinnedStackListener.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    return true;
                case 82:
                    parcel.enforceInterface(DESCRIPTOR);
                    setResizeDimLayer(parcel.readInt() != 0, parcel.readInt(), parcel.readFloat());
                    parcel2.writeNoException();
                    return true;
                case 83:
                    parcel.enforceInterface(DESCRIPTOR);
                    requestAppKeyboardShortcuts(IResultReceiver.Stub.asInterface(parcel.readStrongBinder()), parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 84:
                    parcel.enforceInterface(DESCRIPTOR);
                    int i5 = parcel.readInt();
                    Rect rect = new Rect();
                    getStableInsets(i5, rect);
                    parcel2.writeNoException();
                    parcel2.writeInt(1);
                    rect.writeToParcel(parcel2, 1);
                    return true;
                case 85:
                    parcel.enforceInterface(DESCRIPTOR);
                    registerShortcutKey(parcel.readLong(), IShortcutService.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    return true;
                case 86:
                    parcel.enforceInterface(DESCRIPTOR);
                    IBinder strongBinder = parcel.readStrongBinder();
                    String string = parcel.readString();
                    InputChannel inputChannel = new InputChannel();
                    createInputConsumer(strongBinder, string, inputChannel);
                    parcel2.writeNoException();
                    parcel2.writeInt(1);
                    inputChannel.writeToParcel(parcel2, 1);
                    return true;
                case 87:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zDestroyInputConsumer = destroyInputConsumer(parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeInt(zDestroyInputConsumer ? 1 : 0);
                    return true;
                case 88:
                    parcel.enforceInterface(DESCRIPTOR);
                    Region currentImeTouchRegion = getCurrentImeTouchRegion();
                    parcel2.writeNoException();
                    if (currentImeTouchRegion != null) {
                        parcel2.writeInt(1);
                        currentImeTouchRegion.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 89:
                    parcel.enforceInterface(DESCRIPTOR);
                    startWindowTrace();
                    parcel2.writeNoException();
                    return true;
                case 90:
                    parcel.enforceInterface(DESCRIPTOR);
                    stopWindowTrace();
                    parcel2.writeNoException();
                    return true;
                case 91:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zIsWindowTraceEnabled = isWindowTraceEnabled();
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsWindowTraceEnabled ? 1 : 0);
                    return true;
                case 92:
                    parcel.enforceInterface(DESCRIPTOR);
                    requestUserActivityNotification();
                    parcel2.writeNoException();
                    return true;
                case 93:
                    parcel.enforceInterface(DESCRIPTOR);
                    dontOverrideDisplayInfo(parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }

        private static class Proxy implements IWindowManager {
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
            public boolean startViewServer(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(1, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean stopViewServer() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(2, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isViewServerRunning() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(3, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public IWindowSession openSession(IWindowSessionCallback iWindowSessionCallback, IInputMethodClient iInputMethodClient, IInputContext iInputContext) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iWindowSessionCallback != null ? iWindowSessionCallback.asBinder() : null);
                    parcelObtain.writeStrongBinder(iInputMethodClient != null ? iInputMethodClient.asBinder() : null);
                    parcelObtain.writeStrongBinder(iInputContext != null ? iInputContext.asBinder() : null);
                    this.mRemote.transact(4, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return IWindowSession.Stub.asInterface(parcelObtain2.readStrongBinder());
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean inputMethodClientHasFocus(IInputMethodClient iInputMethodClient) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iInputMethodClient != null ? iInputMethodClient.asBinder() : null);
                    this.mRemote.transact(5, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void getInitialDisplaySize(int i, Point point) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(6, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        point.readFromParcel(parcelObtain2);
                    }
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void getBaseDisplaySize(int i, Point point) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(7, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        point.readFromParcel(parcelObtain2);
                    }
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setForcedDisplaySize(int i, int i2, int i3) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeInt(i3);
                    this.mRemote.transact(8, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void clearForcedDisplaySize(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(9, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int getInitialDisplayDensity(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(10, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int getBaseDisplayDensity(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(11, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setForcedDisplayDensityForUser(int i, int i2, int i3) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeInt(i3);
                    this.mRemote.transact(12, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void clearForcedDisplayDensityForUser(int i, int i2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(13, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setForcedDisplayScalingMode(int i, int i2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(14, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setOverscan(int i, int i2, int i3, int i4, int i5) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeInt(i3);
                    parcelObtain.writeInt(i4);
                    parcelObtain.writeInt(i5);
                    this.mRemote.transact(15, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setEventDispatching(boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(16, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void addWindowToken(IBinder iBinder, int i, int i2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(17, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void removeWindowToken(IBinder iBinder, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(18, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setFocusedApp(IBinder iBinder, boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(19, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void prepareAppTransition(int i, boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(20, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int getPendingAppTransition() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(21, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void overridePendingAppTransition(String str, int i, int i2, IRemoteCallback iRemoteCallback) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeStrongBinder(iRemoteCallback != null ? iRemoteCallback.asBinder() : null);
                    this.mRemote.transact(22, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void overridePendingAppTransitionScaleUp(int i, int i2, int i3, int i4) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeInt(i3);
                    parcelObtain.writeInt(i4);
                    this.mRemote.transact(23, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void overridePendingAppTransitionClipReveal(int i, int i2, int i3, int i4) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeInt(i3);
                    parcelObtain.writeInt(i4);
                    this.mRemote.transact(24, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void overridePendingAppTransitionThumb(GraphicBuffer graphicBuffer, int i, int i2, IRemoteCallback iRemoteCallback, boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (graphicBuffer != null) {
                        parcelObtain.writeInt(1);
                        graphicBuffer.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeStrongBinder(iRemoteCallback != null ? iRemoteCallback.asBinder() : null);
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(25, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void overridePendingAppTransitionAspectScaledThumb(GraphicBuffer graphicBuffer, int i, int i2, int i3, int i4, IRemoteCallback iRemoteCallback, boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (graphicBuffer != null) {
                        parcelObtain.writeInt(1);
                        graphicBuffer.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeInt(i3);
                    parcelObtain.writeInt(i4);
                    parcelObtain.writeStrongBinder(iRemoteCallback != null ? iRemoteCallback.asBinder() : null);
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(26, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void overridePendingAppTransitionMultiThumb(AppTransitionAnimationSpec[] appTransitionAnimationSpecArr, IRemoteCallback iRemoteCallback, IRemoteCallback iRemoteCallback2, boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeTypedArray(appTransitionAnimationSpecArr, 0);
                    parcelObtain.writeStrongBinder(iRemoteCallback != null ? iRemoteCallback.asBinder() : null);
                    parcelObtain.writeStrongBinder(iRemoteCallback2 != null ? iRemoteCallback2.asBinder() : null);
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(27, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void overridePendingAppTransitionInPlace(String str, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
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
            public void overridePendingAppTransitionMultiThumbFuture(IAppTransitionAnimationSpecsFuture iAppTransitionAnimationSpecsFuture, IRemoteCallback iRemoteCallback, boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iAppTransitionAnimationSpecsFuture != null ? iAppTransitionAnimationSpecsFuture.asBinder() : null);
                    parcelObtain.writeStrongBinder(iRemoteCallback != null ? iRemoteCallback.asBinder() : null);
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(29, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void overridePendingAppTransitionRemote(RemoteAnimationAdapter remoteAnimationAdapter) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (remoteAnimationAdapter != null) {
                        parcelObtain.writeInt(1);
                        remoteAnimationAdapter.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(30, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void executeAppTransition() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(31, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void endProlongedAnimations() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(32, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public Configuration updateOrientationFromAppTokens(Configuration configuration, IBinder iBinder, int i) throws RemoteException {
                Configuration configurationCreateFromParcel;
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
                    parcelObtain.writeStrongBinder(iBinder);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(33, parcelObtain, parcelObtain2, 0);
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
            public int[] setNewDisplayOverrideConfiguration(Configuration configuration, int i) throws RemoteException {
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
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(34, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.createIntArray();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void startFreezingScreen(int i, int i2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(35, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void stopFreezingScreen() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(36, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void disableKeyguard(IBinder iBinder, String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(37, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void reenableKeyguard(IBinder iBinder) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    this.mRemote.transact(38, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void exitKeyguardSecurely(IOnKeyguardExitResult iOnKeyguardExitResult) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iOnKeyguardExitResult != null ? iOnKeyguardExitResult.asBinder() : null);
                    this.mRemote.transact(39, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isKeyguardLocked() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(40, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isKeyguardSecure() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(41, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void dismissKeyguard(IKeyguardDismissCallback iKeyguardDismissCallback, CharSequence charSequence) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iKeyguardDismissCallback != null ? iKeyguardDismissCallback.asBinder() : null);
                    if (charSequence != null) {
                        parcelObtain.writeInt(1);
                        TextUtils.writeToParcel(charSequence, parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(42, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setSwitchingUser(boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(43, parcelObtain, parcelObtain2, 0);
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
                    this.mRemote.transact(44, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public float getAnimationScale(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(45, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readFloat();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public float[] getAnimationScales() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(46, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.createFloatArray();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setAnimationScale(int i, float f) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeFloat(f);
                    this.mRemote.transact(47, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setAnimationScales(float[] fArr) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeFloatArray(fArr);
                    this.mRemote.transact(48, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public float getCurrentAnimatorScale() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(49, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readFloat();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setInTouchMode(boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(50, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void showStrictModeViolation(boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(51, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setStrictModeVisualIndicatorPreference(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(52, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void refreshScreenCaptureDisabled(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(53, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void updateRotation(boolean z, boolean z2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(z ? 1 : 0);
                    parcelObtain.writeInt(z2 ? 1 : 0);
                    this.mRemote.transact(54, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int getDefaultDisplayRotation() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(55, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int watchRotation(IRotationWatcher iRotationWatcher, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iRotationWatcher != null ? iRotationWatcher.asBinder() : null);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(56, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void removeRotationWatcher(IRotationWatcher iRotationWatcher) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iRotationWatcher != null ? iRotationWatcher.asBinder() : null);
                    this.mRemote.transact(57, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int getPreferredOptionsPanelGravity() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(58, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void freezeRotation(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(59, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void thawRotation() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(60, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isRotationFrozen() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(61, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public Bitmap screenshotWallpaper() throws RemoteException {
                Bitmap bitmapCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(62, parcelObtain, parcelObtain2, 0);
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
            public boolean registerWallpaperVisibilityListener(IWallpaperVisibilityListener iWallpaperVisibilityListener, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iWallpaperVisibilityListener != null ? iWallpaperVisibilityListener.asBinder() : null);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(63, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void unregisterWallpaperVisibilityListener(IWallpaperVisibilityListener iWallpaperVisibilityListener, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iWallpaperVisibilityListener != null ? iWallpaperVisibilityListener.asBinder() : null);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(64, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean requestAssistScreenshot(IAssistDataReceiver iAssistDataReceiver) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iAssistDataReceiver != null ? iAssistDataReceiver.asBinder() : null);
                    this.mRemote.transact(65, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void statusBarVisibilityChanged(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(66, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setRecentsVisibility(boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(67, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setPipVisibility(boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(68, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setShelfHeight(boolean z, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(z ? 1 : 0);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(69, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setNavBarVirtualKeyHapticFeedbackEnabled(boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(70, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean hasNavigationBar() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(71, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int getNavBarPosition() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(72, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void lockNow(Bundle bundle) throws RemoteException {
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
                    this.mRemote.transact(73, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isSafeModeEnabled() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(74, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void enableScreenIfNeeded() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(75, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean clearWindowContentFrameStats(IBinder iBinder) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    this.mRemote.transact(76, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public WindowContentFrameStats getWindowContentFrameStats(IBinder iBinder) throws RemoteException {
                WindowContentFrameStats windowContentFrameStatsCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    this.mRemote.transact(77, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        windowContentFrameStatsCreateFromParcel = WindowContentFrameStats.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        windowContentFrameStatsCreateFromParcel = null;
                    }
                    return windowContentFrameStatsCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int getDockedStackSide() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(78, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setDockedStackDividerTouchRegion(Rect rect) throws RemoteException {
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
                    this.mRemote.transact(79, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void registerDockedStackListener(IDockedStackListener iDockedStackListener) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iDockedStackListener != null ? iDockedStackListener.asBinder() : null);
                    this.mRemote.transact(80, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void registerPinnedStackListener(int i, IPinnedStackListener iPinnedStackListener) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeStrongBinder(iPinnedStackListener != null ? iPinnedStackListener.asBinder() : null);
                    this.mRemote.transact(81, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setResizeDimLayer(boolean z, int i, float f) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(z ? 1 : 0);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeFloat(f);
                    this.mRemote.transact(82, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void requestAppKeyboardShortcuts(IResultReceiver iResultReceiver, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iResultReceiver != null ? iResultReceiver.asBinder() : null);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(83, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void getStableInsets(int i, Rect rect) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(84, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        rect.readFromParcel(parcelObtain2);
                    }
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void registerShortcutKey(long j, IShortcutService iShortcutService) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeLong(j);
                    parcelObtain.writeStrongBinder(iShortcutService != null ? iShortcutService.asBinder() : null);
                    this.mRemote.transact(85, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void createInputConsumer(IBinder iBinder, String str, InputChannel inputChannel) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(86, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        inputChannel.readFromParcel(parcelObtain2);
                    }
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean destroyInputConsumer(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(87, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public Region getCurrentImeTouchRegion() throws RemoteException {
                Region regionCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(88, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        regionCreateFromParcel = Region.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        regionCreateFromParcel = null;
                    }
                    return regionCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void startWindowTrace() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(89, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void stopWindowTrace() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(90, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isWindowTraceEnabled() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(91, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void requestUserActivityNotification() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(92, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void dontOverrideDisplayInfo(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(93, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }
        }
    }
}
