package com.android.server;

import android.app.ActivityManagerNative;
import android.app.IUserSwitchObserver;
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
import android.content.pm.UserInfo;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IRemoteCallback;
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
import com.android.internal.textservice.ISpellCheckerService;
import com.android.internal.textservice.ISpellCheckerSession;
import com.android.internal.textservice.ISpellCheckerSessionListener;
import com.android.internal.textservice.ITextServicesManager;
import com.android.internal.textservice.ITextServicesSessionListener;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.xmlpull.v1.XmlPullParserException;

public class TextServicesManagerService extends ITextServicesManager.Stub {
    private static final boolean DBG = false;
    private static final String TAG = TextServicesManagerService.class.getSimpleName();
    private final Context mContext;
    private final TextServicesMonitor mMonitor;
    private final TextServicesSettings mSettings;
    private final HashMap<String, SpellCheckerInfo> mSpellCheckerMap = new HashMap<>();
    private final ArrayList<SpellCheckerInfo> mSpellCheckerList = new ArrayList<>();
    private final HashMap<String, SpellCheckerBindGroup> mSpellCheckerBindGroups = new HashMap<>();
    private boolean mSystemReady = false;

    public void systemRunning() {
        if (!this.mSystemReady) {
            this.mSystemReady = true;
        }
    }

    public TextServicesManagerService(Context context) {
        this.mContext = context;
        IntentFilter broadcastFilter = new IntentFilter();
        broadcastFilter.addAction("android.intent.action.USER_ADDED");
        broadcastFilter.addAction("android.intent.action.USER_REMOVED");
        this.mContext.registerReceiver(new TextServicesBroadcastReceiver(), broadcastFilter);
        int userId = 0;
        try {
            ActivityManagerNative.getDefault().registerUserSwitchObserver(new IUserSwitchObserver.Stub() {
                public void onUserSwitching(int newUserId, IRemoteCallback reply) {
                    synchronized (TextServicesManagerService.this.mSpellCheckerMap) {
                        TextServicesManagerService.this.switchUserLocked(newUserId);
                    }
                    if (reply != null) {
                        try {
                            reply.sendResult((Bundle) null);
                        } catch (RemoteException e) {
                        }
                    }
                }

                public void onUserSwitchComplete(int newUserId) throws RemoteException {
                }
            });
            userId = ActivityManagerNative.getDefault().getCurrentUser().id;
        } catch (RemoteException e) {
            Slog.w(TAG, "Couldn't get current user ID; guessing it's 0", e);
        }
        this.mMonitor = new TextServicesMonitor();
        this.mMonitor.register(context, null, true);
        this.mSettings = new TextServicesSettings(context.getContentResolver(), userId);
        switchUserLocked(userId);
    }

    private void switchUserLocked(int userId) {
        SpellCheckerInfo sci;
        this.mSettings.setCurrentUserId(userId);
        updateCurrentProfileIds();
        unbindServiceLocked();
        buildSpellCheckerMapLocked(this.mContext, this.mSpellCheckerList, this.mSpellCheckerMap, this.mSettings);
        if (getCurrentSpellChecker(null) == null && (sci = findAvailSpellCheckerLocked(null, null)) != null) {
            setCurrentSpellCheckerLocked(sci.getId());
        }
    }

    void updateCurrentProfileIds() {
        List<UserInfo> profiles = UserManager.get(this.mContext).getProfiles(this.mSettings.getCurrentUserId());
        int[] currentProfileIds = new int[profiles.size()];
        for (int i = 0; i < currentProfileIds.length; i++) {
            currentProfileIds[i] = profiles.get(i).id;
        }
        this.mSettings.setCurrentProfileIds(currentProfileIds);
    }

    private class TextServicesMonitor extends PackageMonitor {
        private TextServicesMonitor() {
        }

        private boolean isChangingPackagesOfCurrentUser() {
            int userId = getChangingUserId();
            return userId == TextServicesManagerService.this.mSettings.getCurrentUserId();
        }

        public void onSomePackagesChanged() {
            SpellCheckerInfo sci;
            if (isChangingPackagesOfCurrentUser()) {
                synchronized (TextServicesManagerService.this.mSpellCheckerMap) {
                    TextServicesManagerService.buildSpellCheckerMapLocked(TextServicesManagerService.this.mContext, TextServicesManagerService.this.mSpellCheckerList, TextServicesManagerService.this.mSpellCheckerMap, TextServicesManagerService.this.mSettings);
                    SpellCheckerInfo sci2 = TextServicesManagerService.this.getCurrentSpellChecker(null);
                    if (sci2 != null) {
                        String packageName = sci2.getPackageName();
                        int change = isPackageDisappearing(packageName);
                        if ((change == 3 || change == 2 || isPackageModified(packageName)) && (sci = TextServicesManagerService.this.findAvailSpellCheckerLocked(null, packageName)) != null) {
                            TextServicesManagerService.this.setCurrentSpellCheckerLocked(sci.getId());
                        }
                    }
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
            if (!"android.intent.action.USER_ADDED".equals(action) && !"android.intent.action.USER_REMOVED".equals(action)) {
                Slog.w(TextServicesManagerService.TAG, "Unexpected intent " + intent);
            } else {
                TextServicesManagerService.this.updateCurrentProfileIds();
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
        if (service != null && conn != null) {
            return this.mContext.bindServiceAsUser(service, conn, flags, new UserHandle(this.mSettings.getCurrentUserId()));
        }
        Slog.e(TAG, "--- bind failed: service = " + service + ", conn = " + conn);
        return false;
    }

    private void unbindServiceLocked() {
        for (SpellCheckerBindGroup scbg : this.mSpellCheckerBindGroups.values()) {
            scbg.removeAll();
        }
        this.mSpellCheckerBindGroups.clear();
    }

    private SpellCheckerInfo findAvailSpellCheckerLocked(String locale, String prefPackage) {
        int spellCheckersCount = this.mSpellCheckerList.size();
        if (spellCheckersCount == 0) {
            Slog.w(TAG, "no available spell checker services found");
            return null;
        }
        if (prefPackage != null) {
            for (int i = 0; i < spellCheckersCount; i++) {
                SpellCheckerInfo sci = this.mSpellCheckerList.get(i);
                if (prefPackage.equals(sci.getPackageName())) {
                    return sci;
                }
            }
        }
        if (spellCheckersCount > 1) {
            Slog.w(TAG, "more than one spell checker service found, picking first");
        }
        return this.mSpellCheckerList.get(0);
    }

    public SpellCheckerInfo getCurrentSpellChecker(String locale) {
        if (calledFromValidUser()) {
            return getCurrentSpellCheckerWithoutVerification();
        }
        return null;
    }

    private SpellCheckerInfo getCurrentSpellCheckerWithoutVerification() {
        SpellCheckerInfo spellCheckerInfo;
        synchronized (this.mSpellCheckerMap) {
            String curSpellCheckerId = this.mSettings.getSelectedSpellChecker();
            spellCheckerInfo = TextUtils.isEmpty(curSpellCheckerId) ? null : this.mSpellCheckerMap.get(curSpellCheckerId);
        }
        return spellCheckerInfo;
    }

    public SpellCheckerSubtype getCurrentSpellCheckerSubtype(String locale, boolean allowImplicitlySelectedSubtype) {
        int hashCode;
        InputMethodSubtype currentInputMethodSubtype;
        if (!calledFromValidUser()) {
            return null;
        }
        synchronized (this.mSpellCheckerMap) {
            String subtypeHashCodeStr = this.mSettings.getSelectedSpellCheckerSubtype();
            SpellCheckerInfo sci = getCurrentSpellChecker(null);
            if (sci == null || sci.getSubtypeCount() == 0) {
                return null;
            }
            if (!TextUtils.isEmpty(subtypeHashCodeStr)) {
                hashCode = Integer.valueOf(subtypeHashCodeStr).intValue();
            } else {
                hashCode = 0;
            }
            if (hashCode == 0 && !allowImplicitlySelectedSubtype) {
                return null;
            }
            String candidateLocale = null;
            if (hashCode == 0) {
                InputMethodManager imm = (InputMethodManager) this.mContext.getSystemService("input_method");
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
                if (hashCode == 0) {
                    String scsLocale = scs.getLocale();
                    if (!candidateLocale.equals(scsLocale)) {
                        if (candidate == null && candidateLocale.length() >= 2 && scsLocale.length() >= 2 && candidateLocale.startsWith(scsLocale)) {
                            candidate = scs;
                        }
                    } else {
                        return scs;
                    }
                } else if (scs.hashCode() == hashCode) {
                    return scs;
                }
            }
            return candidate;
        }
    }

    public void getSpellCheckerService(String sciId, String locale, ITextServicesSessionListener tsListener, ISpellCheckerSessionListener scListener, Bundle bundle) {
        SpellCheckerBindGroup bindGroup;
        if (calledFromValidUser() && this.mSystemReady) {
            if (TextUtils.isEmpty(sciId) || tsListener == null || scListener == null) {
                Slog.e(TAG, "getSpellCheckerService: Invalid input.");
                return;
            }
            synchronized (this.mSpellCheckerMap) {
                if (this.mSpellCheckerMap.containsKey(sciId)) {
                    SpellCheckerInfo sci = this.mSpellCheckerMap.get(sciId);
                    int uid = Binder.getCallingUid();
                    if (this.mSpellCheckerBindGroups.containsKey(sciId) && (bindGroup = this.mSpellCheckerBindGroups.get(sciId)) != null) {
                        InternalDeathRecipient recipient = this.mSpellCheckerBindGroups.get(sciId).addListener(tsListener, locale, scListener, uid, bundle);
                        if (recipient != null) {
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
        if (!bindCurrentSpellCheckerService(serviceIntent, connection, 1)) {
            Slog.e(TAG, "Failed to get a spell checker service.");
        } else {
            SpellCheckerBindGroup group = new SpellCheckerBindGroup(connection, tsListener, locale, scListener, uid, bundle);
            this.mSpellCheckerBindGroups.put(sciId, group);
        }
    }

    public SpellCheckerInfo[] getEnabledSpellCheckers() {
        if (calledFromValidUser()) {
            return (SpellCheckerInfo[]) this.mSpellCheckerList.toArray(new SpellCheckerInfo[this.mSpellCheckerList.size()]);
        }
        return null;
    }

    public void finishSpellCheckerService(ISpellCheckerSessionListener listener) {
        if (calledFromValidUser()) {
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
    }

    public void setCurrentSpellChecker(String locale, String sciId) {
        if (calledFromValidUser()) {
            synchronized (this.mSpellCheckerMap) {
                if (this.mContext.checkCallingOrSelfPermission("android.permission.WRITE_SECURE_SETTINGS") != 0) {
                    throw new SecurityException("Requires permission android.permission.WRITE_SECURE_SETTINGS");
                }
                setCurrentSpellCheckerLocked(sciId);
            }
        }
    }

    public void setCurrentSpellCheckerSubtype(String locale, int hashCode) {
        if (calledFromValidUser()) {
            synchronized (this.mSpellCheckerMap) {
                if (this.mContext.checkCallingOrSelfPermission("android.permission.WRITE_SECURE_SETTINGS") != 0) {
                    throw new SecurityException("Requires permission android.permission.WRITE_SECURE_SETTINGS");
                }
                setCurrentSpellCheckerSubtypeLocked(hashCode);
            }
        }
    }

    public void setSpellCheckerEnabled(boolean enabled) {
        if (calledFromValidUser()) {
            synchronized (this.mSpellCheckerMap) {
                if (this.mContext.checkCallingOrSelfPermission("android.permission.WRITE_SECURE_SETTINGS") != 0) {
                    throw new SecurityException("Requires permission android.permission.WRITE_SECURE_SETTINGS");
                }
                setSpellCheckerEnabledLocked(enabled);
            }
        }
    }

    private void setCurrentSpellCheckerLocked(String sciId) {
        if (!TextUtils.isEmpty(sciId) && this.mSpellCheckerMap.containsKey(sciId)) {
            SpellCheckerInfo currentSci = getCurrentSpellChecker(null);
            if (currentSci == null || !currentSci.getId().equals(sciId)) {
                long ident = Binder.clearCallingIdentity();
                try {
                    this.mSettings.putSelectedSpellChecker(sciId);
                    setCurrentSpellCheckerSubtypeLocked(0);
                } finally {
                    Binder.restoreCallingIdentity(ident);
                }
            }
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
            pw.println("  Spell Checker Map:");
            for (Map.Entry<String, SpellCheckerInfo> ent : this.mSpellCheckerMap.entrySet()) {
                pw.print("    ");
                pw.print(ent.getKey());
                pw.println(":");
                SpellCheckerInfo info = ent.getValue();
                pw.print("      ");
                pw.print("id=");
                pw.println(info.getId());
                pw.print("      ");
                pw.print("comp=");
                pw.println(info.getComponent().toShortString());
                int NS = info.getSubtypeCount();
                for (int i = 0; i < NS; i++) {
                    SpellCheckerSubtype st = info.getSubtypeAt(i);
                    pw.print("      ");
                    pw.print("Subtype #");
                    pw.print(i);
                    pw.println(":");
                    pw.print("        ");
                    pw.print("locale=");
                    pw.println(st.getLocale());
                    pw.print("        ");
                    pw.print("extraValue=");
                    pw.println(st.getExtraValue());
                }
            }
            pw.println("");
            pw.println("  Spell Checker Bind Groups:");
            for (Map.Entry<String, SpellCheckerBindGroup> ent2 : this.mSpellCheckerBindGroups.entrySet()) {
                SpellCheckerBindGroup grp = ent2.getValue();
                pw.print("    ");
                pw.print(ent2.getKey());
                pw.print(" ");
                pw.print(grp);
                pw.println(":");
                pw.print("      ");
                pw.print("mInternalConnection=");
                pw.println(grp.mInternalConnection);
                pw.print("      ");
                pw.print("mSpellChecker=");
                pw.println(grp.mSpellChecker);
                pw.print("      ");
                pw.print("mBound=");
                pw.print(grp.mBound);
                pw.print(" mConnected=");
                pw.println(grp.mConnected);
                int NL = grp.mListeners.size();
                for (int i2 = 0; i2 < NL; i2++) {
                    InternalDeathRecipient listener = (InternalDeathRecipient) grp.mListeners.get(i2);
                    pw.print("      ");
                    pw.print("Listener #");
                    pw.print(i2);
                    pw.println(":");
                    pw.print("        ");
                    pw.print("mTsListener=");
                    pw.println(listener.mTsListener);
                    pw.print("        ");
                    pw.print("mScListener=");
                    pw.println(listener.mScListener);
                    pw.print("        ");
                    pw.print("mGroup=");
                    pw.println(listener.mGroup);
                    pw.print("        ");
                    pw.print("mScLocale=");
                    pw.print(listener.mScLocale);
                    pw.print(" mUid=");
                    pw.println(listener.mUid);
                }
            }
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
                    try {
                        try {
                            int size = this.mListeners.size();
                            for (int i = 0; i < size; i++) {
                                if (this.mListeners.get(i).hasSpellCheckerListener(scListener)) {
                                    return null;
                                }
                            }
                            recipient = TextServicesManagerService.this.new InternalDeathRecipient(this, tsListener, locale, scListener, uid, bundle);
                            try {
                                scListener.asBinder().linkToDeath(recipient, 0);
                                this.mListeners.add(recipient);
                            } catch (RemoteException e) {
                            }
                        } catch (RemoteException e2) {
                            recipient = null;
                        }
                        cleanLocked();
                        return recipient;
                    } catch (Throwable th) {
                        th = th;
                        throw th;
                    }
                } catch (Throwable th2) {
                    th = th2;
                }
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
            if (this.mBound && this.mListeners.isEmpty()) {
                this.mBound = false;
                String sciId = this.mInternalConnection.mSciId;
                SpellCheckerBindGroup cur = (SpellCheckerBindGroup) TextServicesManagerService.this.mSpellCheckerBindGroups.get(sciId);
                if (cur == this) {
                    TextServicesManagerService.this.mSpellCheckerBindGroups.remove(sciId);
                }
                TextServicesManagerService.this.mContext.unbindService(this.mInternalConnection);
            }
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
            if (group != null && this == group.mInternalConnection) {
                group.onServiceConnected(spellChecker);
            }
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

        public TextServicesSettings(ContentResolver resolver, int userId) {
            this.mResolver = resolver;
            this.mCurrentUserId = userId;
        }

        public void setCurrentUserId(int userId) {
            this.mCurrentUserId = userId;
        }

        public void setCurrentProfileIds(int[] currentProfileIds) {
            synchronized (this.mLock) {
                this.mCurrentProfileIds = currentProfileIds;
            }
        }

        public boolean isCurrentProfile(int userId) {
            boolean z = true;
            synchronized (this.mLock) {
                if (userId != this.mCurrentUserId) {
                    int i = 0;
                    while (true) {
                        if (i < this.mCurrentProfileIds.length) {
                            if (userId == this.mCurrentProfileIds[i]) {
                                break;
                            }
                            i++;
                        } else {
                            z = false;
                            break;
                        }
                    }
                }
            }
            return z;
        }

        public int getCurrentUserId() {
            return this.mCurrentUserId;
        }

        public void putSelectedSpellChecker(String sciId) {
            Settings.Secure.putStringForUser(this.mResolver, "selected_spell_checker", sciId, this.mCurrentUserId);
        }

        public void putSelectedSpellCheckerSubtype(int hashCode) {
            Settings.Secure.putStringForUser(this.mResolver, "selected_spell_checker_subtype", String.valueOf(hashCode), this.mCurrentUserId);
        }

        public void setSpellCheckerEnabled(boolean enabled) {
            Settings.Secure.putIntForUser(this.mResolver, "spell_checker_enabled", enabled ? 1 : 0, this.mCurrentUserId);
        }

        public String getSelectedSpellChecker() {
            return Settings.Secure.getStringForUser(this.mResolver, "selected_spell_checker", this.mCurrentUserId);
        }

        public String getSelectedSpellCheckerSubtype() {
            return Settings.Secure.getStringForUser(this.mResolver, "selected_spell_checker_subtype", this.mCurrentUserId);
        }

        public boolean isSpellCheckerEnabled() {
            return Settings.Secure.getIntForUser(this.mResolver, "spell_checker_enabled", 1, this.mCurrentUserId) == 1;
        }
    }

    private static String getStackTrace() {
        StringBuilder sb = new StringBuilder();
        try {
            throw new RuntimeException();
        } catch (RuntimeException e) {
            StackTraceElement[] frames = e.getStackTrace();
            for (int j = 1; j < frames.length; j++) {
                sb.append(frames[j].toString() + "\n");
            }
            return sb.toString();
        }
    }
}
