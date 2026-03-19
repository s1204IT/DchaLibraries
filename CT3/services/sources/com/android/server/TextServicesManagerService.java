package com.android.server;

import android.app.ActivityManagerNative;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Slog;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;
import android.view.textservice.SpellCheckerInfo;
import android.view.textservice.SpellCheckerSubtype;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.content.PackageMonitor;
import com.android.internal.inputmethod.InputMethodUtils;
import com.android.internal.textservice.ISpellCheckerService;
import com.android.internal.textservice.ISpellCheckerSession;
import com.android.internal.textservice.ISpellCheckerSessionListener;
import com.android.internal.textservice.ITextServicesManager;
import com.android.internal.textservice.ITextServicesSessionListener;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.xmlpull.v1.XmlPullParserException;

public class TextServicesManagerService extends ITextServicesManager.Stub {
    private static final boolean DBG = false;
    private static final String TAG = TextServicesManagerService.class.getSimpleName();
    private final Context mContext;
    private final TextServicesMonitor mMonitor;
    private final TextServicesSettings mSettings;
    private final UserManager mUserManager;
    private final HashMap<String, SpellCheckerInfo> mSpellCheckerMap = new HashMap<>();
    private final ArrayList<SpellCheckerInfo> mSpellCheckerList = new ArrayList<>();
    private final HashMap<String, SpellCheckerBindGroup> mSpellCheckerBindGroups = new HashMap<>();
    private boolean mSystemReady = false;

    public static final class Lifecycle extends SystemService {
        private TextServicesManagerService mService;

        public Lifecycle(Context context) {
            super(context);
            this.mService = new TextServicesManagerService(context);
        }

        @Override
        public void onStart() {
            publishBinderService("textservices", this.mService);
        }

        @Override
        public void onSwitchUser(int userHandle) {
            this.mService.onSwitchUser(userHandle);
        }

        @Override
        public void onBootPhase(int phase) {
            if (phase != 550) {
                return;
            }
            this.mService.systemRunning();
        }

        @Override
        public void onUnlockUser(int userHandle) {
            this.mService.onUnlockUser(userHandle);
        }
    }

    void systemRunning() {
        synchronized (this.mSpellCheckerMap) {
            if (!this.mSystemReady) {
                this.mSystemReady = true;
                resetInternalState(this.mSettings.getCurrentUserId());
            }
        }
    }

    void onSwitchUser(int userId) {
        synchronized (this.mSpellCheckerMap) {
            resetInternalState(userId);
        }
    }

    void onUnlockUser(int userId) {
        synchronized (this.mSpellCheckerMap) {
            int currentUserId = this.mSettings.getCurrentUserId();
            if (userId != currentUserId) {
                return;
            }
            resetInternalState(currentUserId);
        }
    }

    public TextServicesManagerService(Context context) {
        TextServicesMonitor textServicesMonitor = null;
        this.mContext = context;
        this.mUserManager = (UserManager) this.mContext.getSystemService(UserManager.class);
        IntentFilter broadcastFilter = new IntentFilter();
        broadcastFilter.addAction("android.intent.action.USER_ADDED");
        broadcastFilter.addAction("android.intent.action.USER_REMOVED");
        this.mContext.registerReceiver(new TextServicesBroadcastReceiver(), broadcastFilter);
        int userId = 0;
        try {
            userId = ActivityManagerNative.getDefault().getCurrentUser().id;
        } catch (RemoteException e) {
            Slog.w(TAG, "Couldn't get current user ID; guessing it's 0", e);
        }
        this.mMonitor = new TextServicesMonitor(this, textServicesMonitor);
        this.mMonitor.register(context, null, true);
        boolean useCopyOnWriteSettings = (this.mSystemReady && this.mUserManager.isUserUnlockingOrUnlocked(userId)) ? false : true;
        this.mSettings = new TextServicesSettings(context.getContentResolver(), userId, useCopyOnWriteSettings);
        resetInternalState(userId);
    }

    private void resetInternalState(int userId) {
        SpellCheckerInfo sci;
        boolean useCopyOnWriteSettings = (this.mSystemReady && this.mUserManager.isUserUnlockingOrUnlocked(userId)) ? false : true;
        this.mSettings.switchCurrentUser(userId, useCopyOnWriteSettings);
        updateCurrentProfileIds();
        unbindServiceLocked();
        buildSpellCheckerMapLocked(this.mContext, this.mSpellCheckerList, this.mSpellCheckerMap, this.mSettings);
        if (getCurrentSpellChecker(null) != null || (sci = findAvailSystemSpellCheckerLocked(null)) == null) {
            return;
        }
        setCurrentSpellCheckerLocked(sci.getId());
    }

    void updateCurrentProfileIds() {
        this.mSettings.setCurrentProfileIds(this.mUserManager.getProfileIdsWithDisabled(this.mSettings.getCurrentUserId()));
    }

    private class TextServicesMonitor extends PackageMonitor {
        TextServicesMonitor(TextServicesManagerService this$0, TextServicesMonitor textServicesMonitor) {
            this();
        }

        private TextServicesMonitor() {
        }

        private boolean isChangingPackagesOfCurrentUser() {
            int userId = getChangingUserId();
            return userId == TextServicesManagerService.this.mSettings.getCurrentUserId();
        }

        public void onSomePackagesChanged() {
            SpellCheckerInfo sci;
            if (!isChangingPackagesOfCurrentUser()) {
                return;
            }
            synchronized (TextServicesManagerService.this.mSpellCheckerMap) {
                TextServicesManagerService.buildSpellCheckerMapLocked(TextServicesManagerService.this.mContext, TextServicesManagerService.this.mSpellCheckerList, TextServicesManagerService.this.mSpellCheckerMap, TextServicesManagerService.this.mSettings);
                SpellCheckerInfo sci2 = TextServicesManagerService.this.getCurrentSpellChecker(null);
                if (sci2 == null) {
                    return;
                }
                String packageName = sci2.getPackageName();
                int change = isPackageDisappearing(packageName);
                if ((change == 3 || change == 2 || isPackageModified(packageName)) && (sci = TextServicesManagerService.this.findAvailSystemSpellCheckerLocked(packageName)) != null) {
                    TextServicesManagerService.this.setCurrentSpellCheckerLocked(sci.getId());
                }
            }
        }
    }

    class TextServicesBroadcastReceiver extends BroadcastReceiver {
        TextServicesBroadcastReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.intent.action.USER_ADDED".equals(action) || "android.intent.action.USER_REMOVED".equals(action)) {
                TextServicesManagerService.this.updateCurrentProfileIds();
            } else {
                Slog.w(TextServicesManagerService.TAG, "Unexpected intent " + intent);
            }
        }
    }

    private static void buildSpellCheckerMapLocked(Context context, ArrayList<SpellCheckerInfo> list, HashMap<String, SpellCheckerInfo> map, TextServicesSettings settings) {
        list.clear();
        map.clear();
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> services = pm.queryIntentServicesAsUser(new Intent("android.service.textservice.SpellCheckerService"), 128, settings.getCurrentUserId());
        int N = services.size();
        for (int i = 0; i < N; i++) {
            ResolveInfo ri = services.get(i);
            ServiceInfo si = ri.serviceInfo;
            ComponentName compName = new ComponentName(si.packageName, si.name);
            if (!"android.permission.BIND_TEXT_SERVICE".equals(si.permission)) {
                Slog.w(TAG, "Skipping text service " + compName + ": it does not require the permission android.permission.BIND_TEXT_SERVICE");
            } else {
                try {
                    SpellCheckerInfo sci = new SpellCheckerInfo(context, ri);
                    if (sci.getSubtypeCount() <= 0) {
                        Slog.w(TAG, "Skipping text service " + compName + ": it does not contain subtypes.");
                    } else {
                        list.add(sci);
                        map.put(sci.getId(), sci);
                    }
                } catch (IOException e) {
                    Slog.w(TAG, "Unable to load the spell checker " + compName, e);
                } catch (XmlPullParserException e2) {
                    Slog.w(TAG, "Unable to load the spell checker " + compName, e2);
                }
            }
        }
    }

    private boolean calledFromValidUser() {
        SpellCheckerInfo spellCheckerInfo;
        int uid = Binder.getCallingUid();
        int userId = UserHandle.getUserId(uid);
        if (uid == 1000 || userId == this.mSettings.getCurrentUserId()) {
            return true;
        }
        this.mSettings.isCurrentProfile(userId);
        if (this.mSettings.isCurrentProfile(userId) && (spellCheckerInfo = getCurrentSpellCheckerWithoutVerification()) != null) {
            ServiceInfo serviceInfo = spellCheckerInfo.getServiceInfo();
            boolean isSystemSpellChecker = (serviceInfo.applicationInfo.flags & 1) != 0;
            if (isSystemSpellChecker) {
                return true;
            }
        }
        return false;
    }

    private boolean bindCurrentSpellCheckerService(Intent service, ServiceConnection conn, int flags) {
        if (service == null || conn == null) {
            Slog.e(TAG, "--- bind failed: service = " + service + ", conn = " + conn);
            return false;
        }
        return this.mContext.bindServiceAsUser(service, conn, flags, new UserHandle(this.mSettings.getCurrentUserId()));
    }

    private void unbindServiceLocked() {
        for (SpellCheckerBindGroup scbg : this.mSpellCheckerBindGroups.values()) {
            scbg.removeAll();
        }
        this.mSpellCheckerBindGroups.clear();
    }

    private SpellCheckerInfo findAvailSystemSpellCheckerLocked(String prefPackage) {
        ArrayList<SpellCheckerInfo> spellCheckerList = new ArrayList<>();
        for (SpellCheckerInfo sci : this.mSpellCheckerList) {
            if ((sci.getServiceInfo().applicationInfo.flags & 1) != 0) {
                spellCheckerList.add(sci);
            }
        }
        int spellCheckersCount = spellCheckerList.size();
        if (spellCheckersCount == 0) {
            Slog.w(TAG, "no available spell checker services found");
            return null;
        }
        if (prefPackage != null) {
            for (int i = 0; i < spellCheckersCount; i++) {
                SpellCheckerInfo sci2 = spellCheckerList.get(i);
                if (prefPackage.equals(sci2.getPackageName())) {
                    return sci2;
                }
            }
        }
        Locale systemLocal = this.mContext.getResources().getConfiguration().locale;
        ArrayList<Locale> suitableLocales = InputMethodUtils.getSuitableLocalesForSpellChecker(systemLocal);
        int localeCount = suitableLocales.size();
        for (int localeIndex = 0; localeIndex < localeCount; localeIndex++) {
            Locale locale = suitableLocales.get(localeIndex);
            for (int spellCheckersIndex = 0; spellCheckersIndex < spellCheckersCount; spellCheckersIndex++) {
                SpellCheckerInfo info = spellCheckerList.get(spellCheckersIndex);
                int subtypeCount = info.getSubtypeCount();
                for (int subtypeIndex = 0; subtypeIndex < subtypeCount; subtypeIndex++) {
                    SpellCheckerSubtype subtype = info.getSubtypeAt(subtypeIndex);
                    Locale subtypeLocale = InputMethodUtils.constructLocaleFromString(subtype.getLocale());
                    if (locale.equals(subtypeLocale)) {
                        return info;
                    }
                }
            }
        }
        if (spellCheckersCount > 1) {
            Slog.w(TAG, "more than one spell checker service found, picking first");
        }
        return spellCheckerList.get(0);
    }

    public SpellCheckerInfo getCurrentSpellChecker(String locale) {
        if (!calledFromValidUser()) {
            return null;
        }
        return getCurrentSpellCheckerWithoutVerification();
    }

    private SpellCheckerInfo getCurrentSpellCheckerWithoutVerification() {
        synchronized (this.mSpellCheckerMap) {
            String curSpellCheckerId = this.mSettings.getSelectedSpellChecker();
            if (TextUtils.isEmpty(curSpellCheckerId)) {
                return null;
            }
            return this.mSpellCheckerMap.get(curSpellCheckerId);
        }
    }

    public SpellCheckerSubtype getCurrentSpellCheckerSubtype(String locale, boolean allowImplicitlySelectedSubtype) {
        InputMethodSubtype currentInputMethodSubtype;
        if (!calledFromValidUser()) {
            return null;
        }
        synchronized (this.mSpellCheckerMap) {
            int subtypeHashCode = this.mSettings.getSelectedSpellCheckerSubtype(0);
            SpellCheckerInfo sci = getCurrentSpellChecker(null);
            if (sci == null || sci.getSubtypeCount() == 0) {
                return null;
            }
            if (subtypeHashCode == 0 && !allowImplicitlySelectedSubtype) {
                return null;
            }
            String candidateLocale = null;
            if (subtypeHashCode == 0) {
                InputMethodManager imm = (InputMethodManager) this.mContext.getSystemService(InputMethodManager.class);
                if (imm != null && (currentInputMethodSubtype = imm.getCurrentInputMethodSubtype()) != null) {
                    String localeString = currentInputMethodSubtype.getLocale();
                    if (!TextUtils.isEmpty(localeString)) {
                        candidateLocale = localeString;
                    }
                }
                if (candidateLocale == null) {
                    candidateLocale = this.mContext.getResources().getConfiguration().locale.toString();
                }
            }
            SpellCheckerSubtype candidate = null;
            for (int i = 0; i < sci.getSubtypeCount(); i++) {
                SpellCheckerSubtype scs = sci.getSubtypeAt(i);
                if (subtypeHashCode == 0) {
                    String scsLocale = scs.getLocale();
                    if (candidateLocale.equals(scsLocale)) {
                        return scs;
                    }
                    if (candidate == null && candidateLocale.length() >= 2 && scsLocale.length() >= 2 && candidateLocale.startsWith(scsLocale)) {
                        candidate = scs;
                    }
                } else if (scs.hashCode() == subtypeHashCode) {
                    return scs;
                }
            }
            return candidate;
        }
    }

    public void getSpellCheckerService(String sciId, String locale, ITextServicesSessionListener tsListener, ISpellCheckerSessionListener scListener, Bundle bundle) {
        SpellCheckerBindGroup bindGroup;
        if (!calledFromValidUser() || !this.mSystemReady) {
            return;
        }
        if (TextUtils.isEmpty(sciId) || tsListener == null || scListener == null) {
            Slog.e(TAG, "getSpellCheckerService: Invalid input.");
            return;
        }
        synchronized (this.mSpellCheckerMap) {
            if (!this.mSpellCheckerMap.containsKey(sciId)) {
                return;
            }
            SpellCheckerInfo sci = this.mSpellCheckerMap.get(sciId);
            int uid = Binder.getCallingUid();
            if (this.mSpellCheckerBindGroups.containsKey(sciId) && (bindGroup = this.mSpellCheckerBindGroups.get(sciId)) != null) {
                InternalDeathRecipient recipient = this.mSpellCheckerBindGroups.get(sciId).addListener(tsListener, locale, scListener, uid, bundle);
                if (recipient == null) {
                    return;
                }
                if ((bindGroup.mSpellChecker == null) & bindGroup.mConnected) {
                    Slog.e(TAG, "The state of the spell checker bind group is illegal.");
                    bindGroup.removeAll();
                } else if (bindGroup.mSpellChecker != null) {
                    try {
                        ISpellCheckerSession session = bindGroup.mSpellChecker.getISpellCheckerSession(recipient.mScLocale, recipient.mScListener, bundle);
                        if (session != null) {
                            tsListener.onServiceConnected(session);
                            return;
                        }
                        bindGroup.removeAll();
                    } catch (RemoteException e) {
                        Slog.e(TAG, "Exception in getting spell checker session: " + e);
                        bindGroup.removeAll();
                    }
                }
                long ident = Binder.clearCallingIdentity();
                startSpellCheckerServiceInnerLocked(sci, locale, tsListener, scListener, uid, bundle);
                return;
            }
            long ident2 = Binder.clearCallingIdentity();
            try {
                startSpellCheckerServiceInnerLocked(sci, locale, tsListener, scListener, uid, bundle);
                return;
            } finally {
                Binder.restoreCallingIdentity(ident2);
            }
        }
    }

    public boolean isSpellCheckerEnabled() {
        boolean zIsSpellCheckerEnabledLocked;
        if (!calledFromValidUser()) {
            return false;
        }
        synchronized (this.mSpellCheckerMap) {
            zIsSpellCheckerEnabledLocked = isSpellCheckerEnabledLocked();
        }
        return zIsSpellCheckerEnabledLocked;
    }

    private void startSpellCheckerServiceInnerLocked(SpellCheckerInfo info, String locale, ITextServicesSessionListener tsListener, ISpellCheckerSessionListener scListener, int uid, Bundle bundle) {
        String sciId = info.getId();
        InternalServiceConnection connection = new InternalServiceConnection(sciId, locale, bundle);
        Intent serviceIntent = new Intent("android.service.textservice.SpellCheckerService");
        serviceIntent.setComponent(info.getComponent());
        if (!bindCurrentSpellCheckerService(serviceIntent, connection, 33554433)) {
            Slog.e(TAG, "Failed to get a spell checker service.");
        } else {
            SpellCheckerBindGroup group = new SpellCheckerBindGroup(connection, tsListener, locale, scListener, uid, bundle);
            this.mSpellCheckerBindGroups.put(sciId, group);
        }
    }

    public SpellCheckerInfo[] getEnabledSpellCheckers() {
        if (!calledFromValidUser()) {
            return null;
        }
        return (SpellCheckerInfo[]) this.mSpellCheckerList.toArray(new SpellCheckerInfo[this.mSpellCheckerList.size()]);
    }

    public void finishSpellCheckerService(ISpellCheckerSessionListener listener) {
        if (!calledFromValidUser()) {
            return;
        }
        synchronized (this.mSpellCheckerMap) {
            ArrayList<SpellCheckerBindGroup> removeList = new ArrayList<>();
            for (SpellCheckerBindGroup group : this.mSpellCheckerBindGroups.values()) {
                if (group != null) {
                    removeList.add(group);
                }
            }
            int removeSize = removeList.size();
            for (int i = 0; i < removeSize; i++) {
                removeList.get(i).removeListener(listener);
            }
        }
    }

    public void setCurrentSpellChecker(String locale, String sciId) {
        if (!calledFromValidUser()) {
            return;
        }
        synchronized (this.mSpellCheckerMap) {
            if (this.mContext.checkCallingOrSelfPermission("android.permission.WRITE_SECURE_SETTINGS") != 0) {
                throw new SecurityException("Requires permission android.permission.WRITE_SECURE_SETTINGS");
            }
            setCurrentSpellCheckerLocked(sciId);
        }
    }

    public void setCurrentSpellCheckerSubtype(String locale, int hashCode) {
        if (!calledFromValidUser()) {
            return;
        }
        synchronized (this.mSpellCheckerMap) {
            if (this.mContext.checkCallingOrSelfPermission("android.permission.WRITE_SECURE_SETTINGS") != 0) {
                throw new SecurityException("Requires permission android.permission.WRITE_SECURE_SETTINGS");
            }
            setCurrentSpellCheckerSubtypeLocked(hashCode);
        }
    }

    public void setSpellCheckerEnabled(boolean enabled) {
        if (!calledFromValidUser()) {
            return;
        }
        synchronized (this.mSpellCheckerMap) {
            if (this.mContext.checkCallingOrSelfPermission("android.permission.WRITE_SECURE_SETTINGS") != 0) {
                throw new SecurityException("Requires permission android.permission.WRITE_SECURE_SETTINGS");
            }
            setSpellCheckerEnabledLocked(enabled);
        }
    }

    private void setCurrentSpellCheckerLocked(String sciId) {
        if (TextUtils.isEmpty(sciId) || !this.mSpellCheckerMap.containsKey(sciId)) {
            return;
        }
        SpellCheckerInfo currentSci = getCurrentSpellChecker(null);
        if (currentSci != null && currentSci.getId().equals(sciId)) {
            return;
        }
        long ident = Binder.clearCallingIdentity();
        try {
            this.mSettings.putSelectedSpellChecker(sciId);
            setCurrentSpellCheckerSubtypeLocked(0);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private void setCurrentSpellCheckerSubtypeLocked(int hashCode) {
        SpellCheckerInfo sci = getCurrentSpellChecker(null);
        int tempHashCode = 0;
        int i = 0;
        while (true) {
            if (sci == null || i >= sci.getSubtypeCount()) {
                break;
            }
            if (sci.getSubtypeAt(i).hashCode() != hashCode) {
                i++;
            } else {
                tempHashCode = hashCode;
                break;
            }
        }
        long ident = Binder.clearCallingIdentity();
        try {
            this.mSettings.putSelectedSpellCheckerSubtype(tempHashCode);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private void setSpellCheckerEnabledLocked(boolean enabled) {
        long ident = Binder.clearCallingIdentity();
        try {
            this.mSettings.setSpellCheckerEnabled(enabled);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private boolean isSpellCheckerEnabledLocked() {
        long ident = Binder.clearCallingIdentity();
        try {
            boolean retval = this.mSettings.isSpellCheckerEnabled();
            return retval;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.DUMP") != 0) {
            pw.println("Permission Denial: can't dump TextServicesManagerService from from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid());
            return;
        }
        synchronized (this.mSpellCheckerMap) {
            pw.println("Current Text Services Manager state:");
            pw.println("  Spell Checkers:");
            int spellCheckerIndex = 0;
            for (SpellCheckerInfo info : this.mSpellCheckerMap.values()) {
                pw.println("  Spell Checker #" + spellCheckerIndex);
                info.dump(pw, "    ");
                spellCheckerIndex++;
            }
            pw.println("");
            pw.println("  Spell Checker Bind Groups:");
            for (Map.Entry<String, SpellCheckerBindGroup> ent : this.mSpellCheckerBindGroups.entrySet()) {
                SpellCheckerBindGroup grp = ent.getValue();
                pw.println("    " + ent.getKey() + " " + grp + ":");
                pw.println("      mInternalConnection=" + grp.mInternalConnection);
                pw.println("      mSpellChecker=" + grp.mSpellChecker);
                pw.println("      mBound=" + grp.mBound + " mConnected=" + grp.mConnected);
                int N = grp.mListeners.size();
                for (int i = 0; i < N; i++) {
                    InternalDeathRecipient listener = (InternalDeathRecipient) grp.mListeners.get(i);
                    pw.println("      Listener #" + i + ":");
                    pw.println("        mTsListener=" + listener.mTsListener);
                    pw.println("        mScListener=" + listener.mScListener);
                    pw.println("        mGroup=" + listener.mGroup);
                    pw.println("        mScLocale=" + listener.mScLocale + " mUid=" + listener.mUid);
                }
            }
            pw.println("");
            pw.println("  mSettings:");
            this.mSettings.dumpLocked(pw, "    ");
        }
    }

    private class SpellCheckerBindGroup {
        private final InternalServiceConnection mInternalConnection;
        public ISpellCheckerService mSpellChecker;
        private final String TAG = SpellCheckerBindGroup.class.getSimpleName();
        private final CopyOnWriteArrayList<InternalDeathRecipient> mListeners = new CopyOnWriteArrayList<>();
        public boolean mBound = true;
        public boolean mConnected = false;

        public SpellCheckerBindGroup(InternalServiceConnection connection, ITextServicesSessionListener listener, String locale, ISpellCheckerSessionListener scListener, int uid, Bundle bundle) throws Throwable {
            this.mInternalConnection = connection;
            addListener(listener, locale, scListener, uid, bundle);
        }

        public void onServiceConnected(ISpellCheckerService spellChecker) {
            for (InternalDeathRecipient listener : this.mListeners) {
                try {
                    ISpellCheckerSession session = spellChecker.getISpellCheckerSession(listener.mScLocale, listener.mScListener, listener.mBundle);
                    synchronized (TextServicesManagerService.this.mSpellCheckerMap) {
                        if (this.mListeners.contains(listener)) {
                            listener.mTsListener.onServiceConnected(session);
                        }
                    }
                } catch (RemoteException e) {
                    Slog.e(this.TAG, "Exception in getting the spell checker session.Reconnect to the spellchecker. ", e);
                    removeAll();
                    return;
                }
            }
            synchronized (TextServicesManagerService.this.mSpellCheckerMap) {
                this.mSpellChecker = spellChecker;
                this.mConnected = true;
            }
        }

        public InternalDeathRecipient addListener(ITextServicesSessionListener tsListener, String locale, ISpellCheckerSessionListener scListener, int uid, Bundle bundle) throws Throwable {
            InternalDeathRecipient recipient;
            synchronized (TextServicesManagerService.this.mSpellCheckerMap) {
                try {
                    int size = this.mListeners.size();
                    for (int i = 0; i < size; i++) {
                        if (this.mListeners.get(i).hasSpellCheckerListener(scListener)) {
                            return null;
                        }
                    }
                    recipient = TextServicesManagerService.this.new InternalDeathRecipient(this, tsListener, locale, scListener, uid, bundle);
                    try {
                        try {
                            scListener.asBinder().linkToDeath(recipient, 0);
                            this.mListeners.add(recipient);
                        } catch (RemoteException e) {
                        }
                    } catch (Throwable th) {
                        th = th;
                        throw th;
                    }
                } catch (RemoteException e2) {
                    recipient = null;
                } catch (Throwable th2) {
                    th = th2;
                    throw th;
                }
                cleanLocked();
                return recipient;
            }
        }

        public void removeListener(ISpellCheckerSessionListener listener) {
            synchronized (TextServicesManagerService.this.mSpellCheckerMap) {
                int size = this.mListeners.size();
                ArrayList<InternalDeathRecipient> removeList = new ArrayList<>();
                for (int i = 0; i < size; i++) {
                    InternalDeathRecipient tempRecipient = this.mListeners.get(i);
                    if (tempRecipient.hasSpellCheckerListener(listener)) {
                        removeList.add(tempRecipient);
                    }
                }
                int removeSize = removeList.size();
                for (int i2 = 0; i2 < removeSize; i2++) {
                    InternalDeathRecipient idr = removeList.get(i2);
                    idr.mScListener.asBinder().unlinkToDeath(idr, 0);
                    this.mListeners.remove(idr);
                }
                cleanLocked();
            }
        }

        private void cleanLocked() {
            if (!this.mBound || !this.mListeners.isEmpty()) {
                return;
            }
            this.mBound = false;
            String sciId = this.mInternalConnection.mSciId;
            SpellCheckerBindGroup cur = (SpellCheckerBindGroup) TextServicesManagerService.this.mSpellCheckerBindGroups.get(sciId);
            if (cur == this) {
                TextServicesManagerService.this.mSpellCheckerBindGroups.remove(sciId);
            }
            TextServicesManagerService.this.mContext.unbindService(this.mInternalConnection);
        }

        public void removeAll() {
            Slog.e(this.TAG, "Remove the spell checker bind unexpectedly.");
            synchronized (TextServicesManagerService.this.mSpellCheckerMap) {
                int size = this.mListeners.size();
                for (int i = 0; i < size; i++) {
                    InternalDeathRecipient idr = this.mListeners.get(i);
                    idr.mScListener.asBinder().unlinkToDeath(idr, 0);
                }
                this.mListeners.clear();
                cleanLocked();
            }
        }
    }

    private class InternalServiceConnection implements ServiceConnection {
        private final Bundle mBundle;
        private final String mLocale;
        private final String mSciId;

        public InternalServiceConnection(String id, String locale, Bundle bundle) {
            this.mSciId = id;
            this.mLocale = locale;
            this.mBundle = bundle;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            synchronized (TextServicesManagerService.this.mSpellCheckerMap) {
                onServiceConnectedInnerLocked(name, service);
            }
        }

        private void onServiceConnectedInnerLocked(ComponentName name, IBinder service) {
            ISpellCheckerService spellChecker = ISpellCheckerService.Stub.asInterface(service);
            SpellCheckerBindGroup group = (SpellCheckerBindGroup) TextServicesManagerService.this.mSpellCheckerBindGroups.get(this.mSciId);
            if (group == null || this != group.mInternalConnection) {
                return;
            }
            group.onServiceConnected(spellChecker);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            synchronized (TextServicesManagerService.this.mSpellCheckerMap) {
                SpellCheckerBindGroup group = (SpellCheckerBindGroup) TextServicesManagerService.this.mSpellCheckerBindGroups.get(this.mSciId);
                if (group != null && this == group.mInternalConnection) {
                    TextServicesManagerService.this.mSpellCheckerBindGroups.remove(this.mSciId);
                }
            }
        }
    }

    private class InternalDeathRecipient implements IBinder.DeathRecipient {
        public final Bundle mBundle;
        private final SpellCheckerBindGroup mGroup;
        public final ISpellCheckerSessionListener mScListener;
        public final String mScLocale;
        public final ITextServicesSessionListener mTsListener;
        public final int mUid;

        public InternalDeathRecipient(SpellCheckerBindGroup group, ITextServicesSessionListener tsListener, String scLocale, ISpellCheckerSessionListener scListener, int uid, Bundle bundle) {
            this.mTsListener = tsListener;
            this.mScListener = scListener;
            this.mScLocale = scLocale;
            this.mGroup = group;
            this.mUid = uid;
            this.mBundle = bundle;
        }

        public boolean hasSpellCheckerListener(ISpellCheckerSessionListener listener) {
            return listener.asBinder().equals(this.mScListener.asBinder());
        }

        @Override
        public void binderDied() {
            this.mGroup.removeListener(this.mScListener);
        }
    }

    private static class TextServicesSettings {
        private int mCurrentUserId;
        private final ContentResolver mResolver;

        @GuardedBy("mLock")
        private int[] mCurrentProfileIds = new int[0];
        private Object mLock = new Object();
        private final HashMap<String, String> mCopyOnWriteDataStore = new HashMap<>();
        private boolean mCopyOnWrite = false;

        public TextServicesSettings(ContentResolver resolver, int userId, boolean copyOnWrite) {
            this.mResolver = resolver;
            switchCurrentUser(userId, copyOnWrite);
        }

        public void switchCurrentUser(int userId, boolean copyOnWrite) {
            if (this.mCurrentUserId != userId || this.mCopyOnWrite != copyOnWrite) {
                this.mCopyOnWriteDataStore.clear();
            }
            this.mCurrentUserId = userId;
            this.mCopyOnWrite = copyOnWrite;
        }

        private void putString(String key, String str) {
            if (this.mCopyOnWrite) {
                this.mCopyOnWriteDataStore.put(key, str);
            } else {
                Settings.Secure.putStringForUser(this.mResolver, key, str, this.mCurrentUserId);
            }
        }

        private String getString(String key, String defaultValue) {
            String result;
            if (this.mCopyOnWrite && this.mCopyOnWriteDataStore.containsKey(key)) {
                result = this.mCopyOnWriteDataStore.get(key);
            } else {
                result = Settings.Secure.getStringForUser(this.mResolver, key, this.mCurrentUserId);
            }
            return result != null ? result : defaultValue;
        }

        private void putInt(String key, int value) {
            if (this.mCopyOnWrite) {
                this.mCopyOnWriteDataStore.put(key, String.valueOf(value));
            } else {
                Settings.Secure.putIntForUser(this.mResolver, key, value, this.mCurrentUserId);
            }
        }

        private int getInt(String key, int defaultValue) {
            if (this.mCopyOnWrite && this.mCopyOnWriteDataStore.containsKey(key)) {
                String result = this.mCopyOnWriteDataStore.get(key);
                if (result != null) {
                    return Integer.parseInt(result);
                }
                return 0;
            }
            return Settings.Secure.getIntForUser(this.mResolver, key, defaultValue, this.mCurrentUserId);
        }

        private void putBoolean(String key, boolean value) {
            putInt(key, value ? 1 : 0);
        }

        private boolean getBoolean(String key, boolean defaultValue) {
            return getInt(key, defaultValue ? 1 : 0) == 1;
        }

        public void setCurrentProfileIds(int[] currentProfileIds) {
            synchronized (this.mLock) {
                this.mCurrentProfileIds = currentProfileIds;
            }
        }

        public boolean isCurrentProfile(int userId) {
            synchronized (this.mLock) {
                if (userId == this.mCurrentUserId) {
                    return true;
                }
                for (int i = 0; i < this.mCurrentProfileIds.length; i++) {
                    if (userId == this.mCurrentProfileIds[i]) {
                        return true;
                    }
                }
                return false;
            }
        }

        public int getCurrentUserId() {
            return this.mCurrentUserId;
        }

        public void putSelectedSpellChecker(String sciId) {
            if (TextUtils.isEmpty(sciId)) {
                putString("selected_spell_checker", null);
            } else {
                putString("selected_spell_checker", sciId);
            }
        }

        public void putSelectedSpellCheckerSubtype(int hashCode) {
            putInt("selected_spell_checker_subtype", hashCode);
        }

        public void setSpellCheckerEnabled(boolean enabled) {
            putBoolean("spell_checker_enabled", enabled);
        }

        public String getSelectedSpellChecker() {
            return getString("selected_spell_checker", "");
        }

        public int getSelectedSpellCheckerSubtype(int defaultValue) {
            return getInt("selected_spell_checker_subtype", defaultValue);
        }

        public boolean isSpellCheckerEnabled() {
            return getBoolean("spell_checker_enabled", true);
        }

        public void dumpLocked(PrintWriter pw, String prefix) {
            pw.println(prefix + "mCurrentUserId=" + this.mCurrentUserId);
            pw.println(prefix + "mCurrentProfileIds=" + Arrays.toString(this.mCurrentProfileIds));
            pw.println(prefix + "mCopyOnWrite=" + this.mCopyOnWrite);
        }
    }

    private static String getStackTrace() {
        StringBuilder sb = new StringBuilder();
        try {
            throw new RuntimeException();
        } catch (RuntimeException e) {
            StackTraceElement[] frames = e.getStackTrace();
            for (int j = 1; j < frames.length; j++) {
                sb.append(frames[j].toString()).append("\n");
            }
            return sb.toString();
        }
    }
}
