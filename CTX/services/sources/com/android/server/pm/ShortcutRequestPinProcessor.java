package com.android.server.pm;

import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.IPinItemRequest;
import android.content.pm.LauncherApps;
import android.content.pm.ShortcutInfo;
import android.os.Bundle;
import android.os.UserHandle;
import android.util.Log;
import android.util.Pair;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.Preconditions;

class ShortcutRequestPinProcessor {
    private static final boolean DEBUG = false;
    private static final String TAG = "ShortcutService";
    private final Object mLock;
    private final ShortcutService mService;

    private static abstract class PinItemRequestInner extends IPinItemRequest.Stub {

        @GuardedBy("this")
        private boolean mAccepted;
        private final int mLauncherUid;
        protected final ShortcutRequestPinProcessor mProcessor;
        private final IntentSender mResultIntent;

        private PinItemRequestInner(ShortcutRequestPinProcessor shortcutRequestPinProcessor, IntentSender intentSender, int i) {
            this.mProcessor = shortcutRequestPinProcessor;
            this.mResultIntent = intentSender;
            this.mLauncherUid = i;
        }

        public ShortcutInfo getShortcutInfo() {
            return null;
        }

        public AppWidgetProviderInfo getAppWidgetProviderInfo() {
            return null;
        }

        public Bundle getExtras() {
            return null;
        }

        private boolean isCallerValid() {
            return this.mProcessor.isCallerUid(this.mLauncherUid);
        }

        public boolean isValid() {
            boolean z;
            if (!isCallerValid()) {
                return false;
            }
            synchronized (this) {
                z = !this.mAccepted;
            }
            return z;
        }

        public boolean accept(Bundle bundle) {
            if (!isCallerValid()) {
                throw new SecurityException("Calling uid mismatch");
            }
            Intent intentPutExtras = null;
            if (bundle != null) {
                try {
                    bundle.size();
                    intentPutExtras = new Intent().putExtras(bundle);
                } catch (RuntimeException e) {
                    throw new IllegalArgumentException("options cannot be unparceled", e);
                }
            }
            synchronized (this) {
                if (this.mAccepted) {
                    throw new IllegalStateException("accept() called already");
                }
                this.mAccepted = true;
            }
            if (tryAccept()) {
                this.mProcessor.sendResultIntent(this.mResultIntent, intentPutExtras);
                return true;
            }
            return false;
        }

        protected boolean tryAccept() {
            return true;
        }
    }

    private static class PinAppWidgetRequestInner extends PinItemRequestInner {
        final AppWidgetProviderInfo mAppWidgetProviderInfo;
        final Bundle mExtras;

        private PinAppWidgetRequestInner(ShortcutRequestPinProcessor shortcutRequestPinProcessor, IntentSender intentSender, int i, AppWidgetProviderInfo appWidgetProviderInfo, Bundle bundle) {
            super(intentSender, i);
            this.mAppWidgetProviderInfo = appWidgetProviderInfo;
            this.mExtras = bundle;
        }

        @Override
        public AppWidgetProviderInfo getAppWidgetProviderInfo() {
            return this.mAppWidgetProviderInfo;
        }

        @Override
        public Bundle getExtras() {
            return this.mExtras;
        }
    }

    private static class PinShortcutRequestInner extends PinItemRequestInner {
        public final String launcherPackage;
        public final int launcherUserId;
        public final boolean preExisting;
        public final ShortcutInfo shortcutForLauncher;
        public final ShortcutInfo shortcutOriginal;

        private PinShortcutRequestInner(ShortcutRequestPinProcessor shortcutRequestPinProcessor, ShortcutInfo shortcutInfo, ShortcutInfo shortcutInfo2, IntentSender intentSender, String str, int i, int i2, boolean z) {
            super(intentSender, i2);
            this.shortcutOriginal = shortcutInfo;
            this.shortcutForLauncher = shortcutInfo2;
            this.launcherPackage = str;
            this.launcherUserId = i;
            this.preExisting = z;
        }

        @Override
        public ShortcutInfo getShortcutInfo() {
            return this.shortcutForLauncher;
        }

        @Override
        protected boolean tryAccept() {
            return this.mProcessor.directPinShortcut(this);
        }
    }

    public ShortcutRequestPinProcessor(ShortcutService shortcutService, Object obj) {
        this.mService = shortcutService;
        this.mLock = obj;
    }

    public boolean isRequestPinItemSupported(int i, int i2) {
        return getRequestPinConfirmationActivity(i, i2) != null;
    }

    public boolean requestPinItemLocked(ShortcutInfo shortcutInfo, AppWidgetProviderInfo appWidgetProviderInfo, Bundle bundle, int i, IntentSender intentSender) {
        LauncherApps.PinItemRequest pinItemRequest;
        int i2 = shortcutInfo != null ? 1 : 2;
        Pair<ComponentName, Integer> requestPinConfirmationActivity = getRequestPinConfirmationActivity(i, i2);
        if (requestPinConfirmationActivity == null) {
            Log.w(TAG, "Launcher doesn't support requestPinnedShortcut(). Shortcut not created.");
            return false;
        }
        int iIntValue = ((Integer) requestPinConfirmationActivity.second).intValue();
        this.mService.throwIfUserLockedL(iIntValue);
        if (shortcutInfo != null) {
            pinItemRequest = requestPinShortcutLocked(shortcutInfo, intentSender, requestPinConfirmationActivity);
        } else {
            pinItemRequest = new LauncherApps.PinItemRequest(new PinAppWidgetRequestInner(intentSender, this.mService.injectGetPackageUid(((ComponentName) requestPinConfirmationActivity.first).getPackageName(), iIntValue), appWidgetProviderInfo, bundle), 2);
        }
        return startRequestConfirmActivity((ComponentName) requestPinConfirmationActivity.first, iIntValue, pinItemRequest, i2);
    }

    public Intent createShortcutResultIntent(ShortcutInfo shortcutInfo, int i) {
        int parentOrSelfUserId = this.mService.getParentOrSelfUserId(i);
        ComponentName defaultLauncher = this.mService.getDefaultLauncher(parentOrSelfUserId);
        if (defaultLauncher == null) {
            Log.e(TAG, "Default launcher not found.");
            return null;
        }
        this.mService.throwIfUserLockedL(parentOrSelfUserId);
        return new Intent().putExtra("android.content.pm.extra.PIN_ITEM_REQUEST", requestPinShortcutLocked(shortcutInfo, null, Pair.create(defaultLauncher, Integer.valueOf(parentOrSelfUserId))));
    }

    private LauncherApps.PinItemRequest requestPinShortcutLocked(ShortcutInfo shortcutInfo, IntentSender intentSender, Pair<ComponentName, Integer> pair) {
        IntentSender intentSender2;
        ShortcutInfo shortcutInfoClone;
        ShortcutInfo shortcutInfoFindShortcutById = this.mService.getPackageShortcutsForPublisherLocked(shortcutInfo.getPackage(), shortcutInfo.getUserId()).findShortcutById(shortcutInfo.getId());
        boolean z = shortcutInfoFindShortcutById != null;
        if (z) {
            shortcutInfoFindShortcutById.isVisibleToPublisher();
        }
        String packageName = ((ComponentName) pair.first).getPackageName();
        int iIntValue = ((Integer) pair.second).intValue();
        if (z) {
            validateExistingShortcut(shortcutInfoFindShortcutById);
            boolean zHasPinned = this.mService.getLauncherShortcutsLocked(packageName, shortcutInfoFindShortcutById.getUserId(), iIntValue).hasPinned(shortcutInfoFindShortcutById);
            intentSender2 = null;
            if (zHasPinned) {
                sendResultIntent(intentSender, null);
            } else {
                intentSender2 = intentSender;
            }
            shortcutInfoClone = shortcutInfoFindShortcutById.clone(11);
            if (!zHasPinned) {
                shortcutInfoClone.clearFlags(2);
            }
        } else {
            if (shortcutInfo.getActivity() == null) {
                shortcutInfo.setActivity(this.mService.injectGetDefaultMainActivity(shortcutInfo.getPackage(), shortcutInfo.getUserId()));
            }
            this.mService.validateShortcutForPinRequest(shortcutInfo);
            shortcutInfo.resolveResourceStrings(this.mService.injectGetResourcesForApplicationAsUser(shortcutInfo.getPackage(), shortcutInfo.getUserId()));
            intentSender2 = intentSender;
            shortcutInfoClone = shortcutInfo.clone(10);
        }
        return new LauncherApps.PinItemRequest(new PinShortcutRequestInner(shortcutInfo, shortcutInfoClone, intentSender2, packageName, iIntValue, this.mService.injectGetPackageUid(packageName, iIntValue), z), 1);
    }

    private void validateExistingShortcut(ShortcutInfo shortcutInfo) {
        Preconditions.checkArgument(shortcutInfo.isEnabled(), "Shortcut ID=" + shortcutInfo + " already exists but disabled.");
    }

    private boolean startRequestConfirmActivity(ComponentName componentName, int i, LauncherApps.PinItemRequest pinItemRequest, int i2) {
        Intent intent = new Intent(i2 == 1 ? "android.content.pm.action.CONFIRM_PIN_SHORTCUT" : "android.content.pm.action.CONFIRM_PIN_APPWIDGET");
        intent.setComponent(componentName);
        intent.putExtra("android.content.pm.extra.PIN_ITEM_REQUEST", pinItemRequest);
        intent.addFlags(268468224);
        long jInjectClearCallingIdentity = this.mService.injectClearCallingIdentity();
        try {
            this.mService.mContext.startActivityAsUser(intent, UserHandle.of(i));
            return true;
        } catch (RuntimeException e) {
            Log.e(TAG, "Unable to start activity " + componentName, e);
            return false;
        } finally {
            this.mService.injectRestoreCallingIdentity(jInjectClearCallingIdentity);
        }
    }

    @VisibleForTesting
    Pair<ComponentName, Integer> getRequestPinConfirmationActivity(int i, int i2) {
        int parentOrSelfUserId = this.mService.getParentOrSelfUserId(i);
        ComponentName defaultLauncher = this.mService.getDefaultLauncher(parentOrSelfUserId);
        if (defaultLauncher == null) {
            Log.e(TAG, "Default launcher not found.");
            return null;
        }
        ComponentName componentNameInjectGetPinConfirmationActivity = this.mService.injectGetPinConfirmationActivity(defaultLauncher.getPackageName(), parentOrSelfUserId, i2);
        if (componentNameInjectGetPinConfirmationActivity == null) {
            return null;
        }
        return Pair.create(componentNameInjectGetPinConfirmationActivity, Integer.valueOf(parentOrSelfUserId));
    }

    public void sendResultIntent(IntentSender intentSender, Intent intent) {
        this.mService.injectSendIntentSender(intentSender, intent);
    }

    public boolean isCallerUid(int i) {
        return i == this.mService.injectBinderCallingUid();
    }

    public boolean directPinShortcut(PinShortcutRequestInner pinShortcutRequestInner) {
        ShortcutInfo shortcutInfo = pinShortcutRequestInner.shortcutOriginal;
        int userId = shortcutInfo.getUserId();
        String str = shortcutInfo.getPackage();
        int i = pinShortcutRequestInner.launcherUserId;
        String str2 = pinShortcutRequestInner.launcherPackage;
        String id = shortcutInfo.getId();
        synchronized (this.mLock) {
            if (this.mService.isUserUnlockedL(userId) && this.mService.isUserUnlockedL(pinShortcutRequestInner.launcherUserId)) {
                ShortcutLauncher launcherShortcutsLocked = this.mService.getLauncherShortcutsLocked(str2, userId, i);
                launcherShortcutsLocked.attemptToRestoreIfNeededAndSave();
                if (launcherShortcutsLocked.hasPinned(shortcutInfo)) {
                    return true;
                }
                ShortcutPackage packageShortcutsForPublisherLocked = this.mService.getPackageShortcutsForPublisherLocked(str, userId);
                ShortcutInfo shortcutInfoFindShortcutById = packageShortcutsForPublisherLocked.findShortcutById(id);
                try {
                    if (shortcutInfoFindShortcutById == null) {
                        this.mService.validateShortcutForPinRequest(shortcutInfo);
                    } else {
                        validateExistingShortcut(shortcutInfoFindShortcutById);
                    }
                    if (shortcutInfoFindShortcutById == null) {
                        if (shortcutInfo.getActivity() == null) {
                            shortcutInfo.setActivity(this.mService.getDummyMainActivity(str));
                        }
                        packageShortcutsForPublisherLocked.addOrReplaceDynamicShortcut(shortcutInfo);
                    }
                    launcherShortcutsLocked.addPinnedShortcut(str, userId, id, true);
                    if (shortcutInfoFindShortcutById == null) {
                        packageShortcutsForPublisherLocked.deleteDynamicWithId(id, false);
                    }
                    packageShortcutsForPublisherLocked.adjustRanks();
                    this.mService.verifyStates();
                    this.mService.packageShortcutsChanged(str, userId);
                    return true;
                } catch (RuntimeException e) {
                    Log.w(TAG, "Unable to pin shortcut: " + e.getMessage());
                    return false;
                }
            }
            Log.w(TAG, "User is locked now.");
            return false;
        }
    }
}
