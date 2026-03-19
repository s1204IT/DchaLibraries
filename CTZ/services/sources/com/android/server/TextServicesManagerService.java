package com.android.server;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.pm.UserInfo;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Slog;
import android.util.SparseArray;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;
import android.view.textservice.SpellCheckerInfo;
import android.view.textservice.SpellCheckerSubtype;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.content.PackageMonitor;
import com.android.internal.inputmethod.InputMethodUtils;
import com.android.internal.textservice.ISpellCheckerService;
import com.android.internal.textservice.ISpellCheckerServiceCallback;
import com.android.internal.textservice.ISpellCheckerSession;
import com.android.internal.textservice.ISpellCheckerSessionListener;
import com.android.internal.textservice.ITextServicesManager;
import com.android.internal.textservice.ITextServicesSessionListener;
import com.android.internal.textservice.LazyIntToIntMap;
import com.android.internal.util.DumpUtils;
import com.android.server.TextServicesManagerService;
import com.android.server.backup.BackupManagerConstants;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.IntUnaryOperator;
import java.util.function.Predicate;
import org.xmlpull.v1.XmlPullParserException;

public class TextServicesManagerService extends ITextServicesManager.Stub {
    private static final boolean DBG = false;
    private static final String TAG = TextServicesManagerService.class.getSimpleName();
    private final Context mContext;
    private final UserManager mUserManager;
    private final SparseArray<TextServicesData> mUserData = new SparseArray<>();
    private final Object mLock = new Object();

    @GuardedBy("mLock")
    private final LazyIntToIntMap mSpellCheckerOwnerUserIdMap = new LazyIntToIntMap(new IntUnaryOperator() {
        @Override
        public final int applyAsInt(int i) {
            return TextServicesManagerService.lambda$new$0(this.f$0, i);
        }
    });
    private final TextServicesMonitor mMonitor = new TextServicesMonitor();

    private static class TextServicesData {
        private final Context mContext;
        private final ContentResolver mResolver;
        private final int mUserId;
        public int mUpdateCount = 0;
        private final HashMap<String, SpellCheckerInfo> mSpellCheckerMap = new HashMap<>();
        private final ArrayList<SpellCheckerInfo> mSpellCheckerList = new ArrayList<>();
        private final HashMap<String, SpellCheckerBindGroup> mSpellCheckerBindGroups = new HashMap<>();

        public TextServicesData(int i, Context context) {
            this.mUserId = i;
            this.mContext = context;
            this.mResolver = context.getContentResolver();
        }

        private void putString(String str, String str2) {
            Settings.Secure.putStringForUser(this.mResolver, str, str2, this.mUserId);
        }

        private String getString(String str, String str2) {
            String stringForUser = Settings.Secure.getStringForUser(this.mResolver, str, this.mUserId);
            return stringForUser != null ? stringForUser : str2;
        }

        private void putInt(String str, int i) {
            Settings.Secure.putIntForUser(this.mResolver, str, i, this.mUserId);
        }

        private int getInt(String str, int i) {
            return Settings.Secure.getIntForUser(this.mResolver, str, i, this.mUserId);
        }

        private boolean getBoolean(String str, boolean z) {
            return getInt(str, z ? 1 : 0) == 1;
        }

        private void putSelectedSpellChecker(String str) {
            putString("selected_spell_checker", str);
        }

        private void putSelectedSpellCheckerSubtype(int i) {
            putInt("selected_spell_checker_subtype", i);
        }

        private String getSelectedSpellChecker() {
            return getString("selected_spell_checker", BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        }

        public int getSelectedSpellCheckerSubtype(int i) {
            return getInt("selected_spell_checker_subtype", i);
        }

        public boolean isSpellCheckerEnabled() {
            return getBoolean("spell_checker_enabled", true);
        }

        public SpellCheckerInfo getCurrentSpellChecker() {
            String selectedSpellChecker = getSelectedSpellChecker();
            if (TextUtils.isEmpty(selectedSpellChecker)) {
                return null;
            }
            return this.mSpellCheckerMap.get(selectedSpellChecker);
        }

        public void setCurrentSpellChecker(SpellCheckerInfo spellCheckerInfo) {
            if (spellCheckerInfo != null) {
                putSelectedSpellChecker(spellCheckerInfo.getId());
            } else {
                putSelectedSpellChecker(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
            }
            putSelectedSpellCheckerSubtype(0);
        }

        private void initializeTextServicesData() {
            this.mSpellCheckerList.clear();
            this.mSpellCheckerMap.clear();
            this.mUpdateCount++;
            List listQueryIntentServicesAsUser = this.mContext.getPackageManager().queryIntentServicesAsUser(new Intent("android.service.textservice.SpellCheckerService"), 128, this.mUserId);
            int size = listQueryIntentServicesAsUser.size();
            for (int i = 0; i < size; i++) {
                ResolveInfo resolveInfo = (ResolveInfo) listQueryIntentServicesAsUser.get(i);
                ServiceInfo serviceInfo = resolveInfo.serviceInfo;
                ComponentName componentName = new ComponentName(serviceInfo.packageName, serviceInfo.name);
                if (!"android.permission.BIND_TEXT_SERVICE".equals(serviceInfo.permission)) {
                    Slog.w(TextServicesManagerService.TAG, "Skipping text service " + componentName + ": it does not require the permission android.permission.BIND_TEXT_SERVICE");
                } else {
                    try {
                        SpellCheckerInfo spellCheckerInfo = new SpellCheckerInfo(this.mContext, resolveInfo);
                        if (spellCheckerInfo.getSubtypeCount() <= 0) {
                            Slog.w(TextServicesManagerService.TAG, "Skipping text service " + componentName + ": it does not contain subtypes.");
                        } else {
                            this.mSpellCheckerList.add(spellCheckerInfo);
                            this.mSpellCheckerMap.put(spellCheckerInfo.getId(), spellCheckerInfo);
                        }
                    } catch (IOException e) {
                        Slog.w(TextServicesManagerService.TAG, "Unable to load the spell checker " + componentName, e);
                    } catch (XmlPullParserException e2) {
                        Slog.w(TextServicesManagerService.TAG, "Unable to load the spell checker " + componentName, e2);
                    }
                }
            }
        }

        private void dump(PrintWriter printWriter) {
            printWriter.println("  User #" + this.mUserId);
            printWriter.println("  Spell Checkers:");
            printWriter.println("  Spell Checkers: mUpdateCount=" + this.mUpdateCount);
            int i = 0;
            for (SpellCheckerInfo spellCheckerInfo : this.mSpellCheckerMap.values()) {
                printWriter.println("  Spell Checker #" + i);
                spellCheckerInfo.dump(printWriter, "    ");
                i++;
            }
            printWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
            printWriter.println("  Spell Checker Bind Groups:");
            for (Map.Entry<String, SpellCheckerBindGroup> entry : this.mSpellCheckerBindGroups.entrySet()) {
                SpellCheckerBindGroup value = entry.getValue();
                printWriter.println("    " + entry.getKey() + " " + value + ":");
                StringBuilder sb = new StringBuilder();
                sb.append("      mInternalConnection=");
                sb.append(value.mInternalConnection);
                printWriter.println(sb.toString());
                printWriter.println("      mSpellChecker=" + value.mSpellChecker);
                printWriter.println("      mUnbindCalled=" + value.mUnbindCalled);
                printWriter.println("      mConnected=" + value.mConnected);
                int size = value.mPendingSessionRequests.size();
                for (int i2 = 0; i2 < size; i2++) {
                    SessionRequest sessionRequest = (SessionRequest) value.mPendingSessionRequests.get(i2);
                    printWriter.println("      Pending Request #" + i2 + ":");
                    StringBuilder sb2 = new StringBuilder();
                    sb2.append("        mTsListener=");
                    sb2.append(sessionRequest.mTsListener);
                    printWriter.println(sb2.toString());
                    printWriter.println("        mScListener=" + sessionRequest.mScListener);
                    printWriter.println("        mScLocale=" + sessionRequest.mLocale + " mUid=" + sessionRequest.mUid);
                }
                int size2 = value.mOnGoingSessionRequests.size();
                for (int i3 = 0; i3 < size2; i3 = i3 + 1 + 1) {
                    SessionRequest sessionRequest2 = (SessionRequest) value.mOnGoingSessionRequests.get(i3);
                    printWriter.println("      On going Request #" + i3 + ":");
                    StringBuilder sb3 = new StringBuilder();
                    sb3.append("        mTsListener=");
                    sb3.append(sessionRequest2.mTsListener);
                    printWriter.println(sb3.toString());
                    printWriter.println("        mScListener=" + sessionRequest2.mScListener);
                    printWriter.println("        mScLocale=" + sessionRequest2.mLocale + " mUid=" + sessionRequest2.mUid);
                }
                int registeredCallbackCount = value.mListeners.getRegisteredCallbackCount();
                for (int i4 = 0; i4 < registeredCallbackCount; i4++) {
                    ISpellCheckerSessionListener registeredCallbackItem = value.mListeners.getRegisteredCallbackItem(i4);
                    printWriter.println("      Listener #" + i4 + ":");
                    StringBuilder sb4 = new StringBuilder();
                    sb4.append("        mScListener=");
                    sb4.append(registeredCallbackItem);
                    printWriter.println(sb4.toString());
                    printWriter.println("        mGroup=" + value);
                }
            }
        }
    }

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
        public void onStopUser(int i) {
            this.mService.onStopUser(i);
        }

        @Override
        public void onUnlockUser(int i) {
            this.mService.onUnlockUser(i);
        }
    }

    void onStopUser(int i) {
        synchronized (this.mLock) {
            this.mSpellCheckerOwnerUserIdMap.delete(i);
            TextServicesData textServicesData = this.mUserData.get(i);
            if (textServicesData == null) {
                return;
            }
            unbindServiceLocked(textServicesData);
            this.mUserData.remove(i);
        }
    }

    void onUnlockUser(int i) {
        synchronized (this.mLock) {
            initializeInternalStateLocked(i);
        }
    }

    public TextServicesManagerService(Context context) {
        this.mContext = context;
        this.mUserManager = (UserManager) this.mContext.getSystemService(UserManager.class);
        this.mMonitor.register(context, null, UserHandle.ALL, true);
    }

    public static int lambda$new$0(TextServicesManagerService textServicesManagerService, int i) {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            UserInfo profileParent = textServicesManagerService.mUserManager.getProfileParent(i);
            if (profileParent != null) {
                i = profileParent.id;
            }
            return i;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private void initializeInternalStateLocked(int i) {
        if (i != this.mSpellCheckerOwnerUserIdMap.get(i)) {
            return;
        }
        TextServicesData textServicesData = this.mUserData.get(i);
        if (textServicesData == null) {
            textServicesData = new TextServicesData(i, this.mContext);
            this.mUserData.put(i, textServicesData);
        }
        textServicesData.initializeTextServicesData();
        if (textServicesData.getCurrentSpellChecker() == null) {
            setCurrentSpellCheckerLocked(findAvailSystemSpellCheckerLocked(null, textServicesData), textServicesData);
        }
    }

    private final class TextServicesMonitor extends PackageMonitor {
        private TextServicesMonitor() {
        }

        public void onSomePackagesChanged() {
            SpellCheckerInfo spellCheckerInfoFindAvailSystemSpellCheckerLocked;
            int changingUserId = getChangingUserId();
            synchronized (TextServicesManagerService.this.mLock) {
                TextServicesData textServicesData = (TextServicesData) TextServicesManagerService.this.mUserData.get(changingUserId);
                if (textServicesData == null) {
                    return;
                }
                SpellCheckerInfo currentSpellChecker = textServicesData.getCurrentSpellChecker();
                textServicesData.initializeTextServicesData();
                if (textServicesData.isSpellCheckerEnabled()) {
                    if (currentSpellChecker == null) {
                        TextServicesManagerService.this.setCurrentSpellCheckerLocked(TextServicesManagerService.this.findAvailSystemSpellCheckerLocked(null, textServicesData), textServicesData);
                    } else {
                        String packageName = currentSpellChecker.getPackageName();
                        int iIsPackageDisappearing = isPackageDisappearing(packageName);
                        if ((iIsPackageDisappearing == 3 || iIsPackageDisappearing == 2) && ((spellCheckerInfoFindAvailSystemSpellCheckerLocked = TextServicesManagerService.this.findAvailSystemSpellCheckerLocked(packageName, textServicesData)) == null || (spellCheckerInfoFindAvailSystemSpellCheckerLocked != null && !spellCheckerInfoFindAvailSystemSpellCheckerLocked.getId().equals(currentSpellChecker.getId())))) {
                            TextServicesManagerService.this.setCurrentSpellCheckerLocked(spellCheckerInfoFindAvailSystemSpellCheckerLocked, textServicesData);
                        }
                    }
                }
            }
        }
    }

    private boolean bindCurrentSpellCheckerService(Intent intent, ServiceConnection serviceConnection, int i, int i2) {
        if (intent == null || serviceConnection == null) {
            Slog.e(TAG, "--- bind failed: service = " + intent + ", conn = " + serviceConnection + ", userId =" + i2);
            return false;
        }
        return this.mContext.bindServiceAsUser(intent, serviceConnection, i, UserHandle.of(i2));
    }

    private void unbindServiceLocked(TextServicesData textServicesData) {
        HashMap map = textServicesData.mSpellCheckerBindGroups;
        Iterator it = map.values().iterator();
        while (it.hasNext()) {
            ((SpellCheckerBindGroup) it.next()).removeAllLocked();
        }
        map.clear();
    }

    private SpellCheckerInfo findAvailSystemSpellCheckerLocked(String str, TextServicesData textServicesData) {
        ArrayList arrayList = new ArrayList();
        for (SpellCheckerInfo spellCheckerInfo : textServicesData.mSpellCheckerList) {
            if ((1 & spellCheckerInfo.getServiceInfo().applicationInfo.flags) != 0) {
                arrayList.add(spellCheckerInfo);
            }
        }
        int size = arrayList.size();
        if (size == 0) {
            Slog.w(TAG, "no available spell checker services found");
            return null;
        }
        if (str != null) {
            for (int i = 0; i < size; i++) {
                SpellCheckerInfo spellCheckerInfo2 = (SpellCheckerInfo) arrayList.get(i);
                if (str.equals(spellCheckerInfo2.getPackageName())) {
                    return spellCheckerInfo2;
                }
            }
        }
        ArrayList suitableLocalesForSpellChecker = InputMethodUtils.getSuitableLocalesForSpellChecker(this.mContext.getResources().getConfiguration().locale);
        int size2 = suitableLocalesForSpellChecker.size();
        for (int i2 = 0; i2 < size2; i2++) {
            Locale locale = (Locale) suitableLocalesForSpellChecker.get(i2);
            for (int i3 = 0; i3 < size; i3++) {
                SpellCheckerInfo spellCheckerInfo3 = (SpellCheckerInfo) arrayList.get(i3);
                int subtypeCount = spellCheckerInfo3.getSubtypeCount();
                for (int i4 = 0; i4 < subtypeCount; i4++) {
                    if (locale.equals(InputMethodUtils.constructLocaleFromString(spellCheckerInfo3.getSubtypeAt(i4).getLocale()))) {
                        return spellCheckerInfo3;
                    }
                }
            }
        }
        if (size > 1) {
            Slog.w(TAG, "more than one spell checker service found, picking first");
        }
        return (SpellCheckerInfo) arrayList.get(0);
    }

    public SpellCheckerInfo getCurrentSpellChecker(String str) {
        int callingUserId = UserHandle.getCallingUserId();
        synchronized (this.mLock) {
            TextServicesData dataFromCallingUserIdLocked = getDataFromCallingUserIdLocked(callingUserId);
            if (dataFromCallingUserIdLocked == null) {
                return null;
            }
            return dataFromCallingUserIdLocked.getCurrentSpellChecker();
        }
    }

    public SpellCheckerSubtype getCurrentSpellCheckerSubtype(String str, boolean z) {
        String string;
        InputMethodSubtype currentInputMethodSubtype;
        int callingUserId = UserHandle.getCallingUserId();
        synchronized (this.mLock) {
            TextServicesData dataFromCallingUserIdLocked = getDataFromCallingUserIdLocked(callingUserId);
            SpellCheckerSubtype spellCheckerSubtype = null;
            if (dataFromCallingUserIdLocked == null) {
                return null;
            }
            int selectedSpellCheckerSubtype = dataFromCallingUserIdLocked.getSelectedSpellCheckerSubtype(0);
            SpellCheckerInfo currentSpellChecker = dataFromCallingUserIdLocked.getCurrentSpellChecker();
            Locale locale = this.mContext.getResources().getConfiguration().locale;
            if (currentSpellChecker == null || currentSpellChecker.getSubtypeCount() == 0) {
                return null;
            }
            if (selectedSpellCheckerSubtype == 0 && !z) {
                return null;
            }
            if (selectedSpellCheckerSubtype == 0) {
                InputMethodManager inputMethodManager = (InputMethodManager) this.mContext.getSystemService(InputMethodManager.class);
                if (inputMethodManager != null && (currentInputMethodSubtype = inputMethodManager.getCurrentInputMethodSubtype()) != null) {
                    string = currentInputMethodSubtype.getLocale();
                    if (TextUtils.isEmpty(string)) {
                    }
                    if (string == null) {
                    }
                } else {
                    string = null;
                    if (string == null) {
                        string = locale.toString();
                    }
                }
            } else {
                string = null;
            }
            for (int i = 0; i < currentSpellChecker.getSubtypeCount(); i++) {
                SpellCheckerSubtype subtypeAt = currentSpellChecker.getSubtypeAt(i);
                if (selectedSpellCheckerSubtype == 0) {
                    String locale2 = subtypeAt.getLocale();
                    if (string.equals(locale2)) {
                        return subtypeAt;
                    }
                    if (spellCheckerSubtype != null || string.length() < 2 || locale2.length() < 2 || !string.startsWith(locale2)) {
                        subtypeAt = spellCheckerSubtype;
                    }
                    spellCheckerSubtype = subtypeAt;
                } else if (subtypeAt.hashCode() == selectedSpellCheckerSubtype) {
                    return subtypeAt;
                }
            }
            return spellCheckerSubtype;
        }
    }

    public void getSpellCheckerService(String str, String str2, ITextServicesSessionListener iTextServicesSessionListener, ISpellCheckerSessionListener iSpellCheckerSessionListener, Bundle bundle) {
        if (TextUtils.isEmpty(str) || iTextServicesSessionListener == null || iSpellCheckerSessionListener == null) {
            Slog.e(TAG, "getSpellCheckerService: Invalid input.");
            return;
        }
        int callingUserId = UserHandle.getCallingUserId();
        synchronized (this.mLock) {
            TextServicesData dataFromCallingUserIdLocked = getDataFromCallingUserIdLocked(callingUserId);
            if (dataFromCallingUserIdLocked == null) {
                return;
            }
            HashMap map = dataFromCallingUserIdLocked.mSpellCheckerMap;
            if (map.containsKey(str)) {
                SpellCheckerInfo spellCheckerInfo = (SpellCheckerInfo) map.get(str);
                SpellCheckerBindGroup spellCheckerBindGroupStartSpellCheckerServiceInnerLocked = (SpellCheckerBindGroup) dataFromCallingUserIdLocked.mSpellCheckerBindGroups.get(str);
                int callingUid = Binder.getCallingUid();
                if (spellCheckerBindGroupStartSpellCheckerServiceInnerLocked == null) {
                    long jClearCallingIdentity = Binder.clearCallingIdentity();
                    try {
                        spellCheckerBindGroupStartSpellCheckerServiceInnerLocked = startSpellCheckerServiceInnerLocked(spellCheckerInfo, dataFromCallingUserIdLocked);
                        if (spellCheckerBindGroupStartSpellCheckerServiceInnerLocked == null) {
                            return;
                        }
                    } finally {
                        Binder.restoreCallingIdentity(jClearCallingIdentity);
                    }
                }
                spellCheckerBindGroupStartSpellCheckerServiceInnerLocked.getISpellCheckerSessionOrQueueLocked(new SessionRequest(callingUid, str2, iTextServicesSessionListener, iSpellCheckerSessionListener, bundle));
            }
        }
    }

    public boolean isSpellCheckerEnabled() {
        int callingUserId = UserHandle.getCallingUserId();
        synchronized (this.mLock) {
            TextServicesData dataFromCallingUserIdLocked = getDataFromCallingUserIdLocked(callingUserId);
            if (dataFromCallingUserIdLocked == null) {
                return false;
            }
            return dataFromCallingUserIdLocked.isSpellCheckerEnabled();
        }
    }

    private SpellCheckerBindGroup startSpellCheckerServiceInnerLocked(SpellCheckerInfo spellCheckerInfo, TextServicesData textServicesData) {
        String id = spellCheckerInfo.getId();
        InternalServiceConnection internalServiceConnection = new InternalServiceConnection(id, textServicesData.mSpellCheckerBindGroups);
        Intent intent = new Intent("android.service.textservice.SpellCheckerService");
        intent.setComponent(spellCheckerInfo.getComponent());
        if (!bindCurrentSpellCheckerService(intent, internalServiceConnection, 8388609, textServicesData.mUserId)) {
            Slog.e(TAG, "Failed to get a spell checker service.");
            return null;
        }
        SpellCheckerBindGroup spellCheckerBindGroup = new SpellCheckerBindGroup(internalServiceConnection);
        textServicesData.mSpellCheckerBindGroups.put(id, spellCheckerBindGroup);
        return spellCheckerBindGroup;
    }

    public SpellCheckerInfo[] getEnabledSpellCheckers() {
        int callingUserId = UserHandle.getCallingUserId();
        synchronized (this.mLock) {
            TextServicesData dataFromCallingUserIdLocked = getDataFromCallingUserIdLocked(callingUserId);
            if (dataFromCallingUserIdLocked == null) {
                return null;
            }
            ArrayList arrayList = dataFromCallingUserIdLocked.mSpellCheckerList;
            return (SpellCheckerInfo[]) arrayList.toArray(new SpellCheckerInfo[arrayList.size()]);
        }
    }

    public void finishSpellCheckerService(ISpellCheckerSessionListener iSpellCheckerSessionListener) {
        int callingUserId = UserHandle.getCallingUserId();
        synchronized (this.mLock) {
            TextServicesData dataFromCallingUserIdLocked = getDataFromCallingUserIdLocked(callingUserId);
            if (dataFromCallingUserIdLocked == null) {
                return;
            }
            ArrayList arrayList = new ArrayList();
            for (SpellCheckerBindGroup spellCheckerBindGroup : dataFromCallingUserIdLocked.mSpellCheckerBindGroups.values()) {
                if (spellCheckerBindGroup != null) {
                    arrayList.add(spellCheckerBindGroup);
                }
            }
            int size = arrayList.size();
            for (int i = 0; i < size; i++) {
                ((SpellCheckerBindGroup) arrayList.get(i)).removeListener(iSpellCheckerSessionListener);
            }
        }
    }

    private void setCurrentSpellCheckerLocked(SpellCheckerInfo spellCheckerInfo, TextServicesData textServicesData) {
        if (spellCheckerInfo != null) {
            spellCheckerInfo.getId();
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            textServicesData.setCurrentSpellChecker(spellCheckerInfo);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    protected void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        if (DumpUtils.checkDumpPermission(this.mContext, TAG, printWriter)) {
            if (strArr.length == 0 || (strArr.length == 1 && strArr[0].equals("-a"))) {
                synchronized (this.mLock) {
                    printWriter.println("Current Text Services Manager state:");
                    printWriter.println("  Users:");
                    int size = this.mUserData.size();
                    for (int i = 0; i < size; i++) {
                        this.mUserData.valueAt(i).dump(printWriter);
                    }
                }
                return;
            }
            if (strArr.length != 2 || !strArr[0].equals("--user")) {
                printWriter.println("Invalid arguments to text services.");
                return;
            }
            int i2 = Integer.parseInt(strArr[1]);
            if (this.mUserManager.getUserInfo(i2) == null) {
                printWriter.println("Non-existent user.");
                return;
            }
            TextServicesData textServicesData = this.mUserData.get(i2);
            if (textServicesData == null) {
                printWriter.println("User needs to unlock first.");
                return;
            }
            synchronized (this.mLock) {
                printWriter.println("Current Text Services Manager state:");
                printWriter.println("  User " + i2 + ":");
                textServicesData.dump(printWriter);
            }
        }
    }

    private TextServicesData getDataFromCallingUserIdLocked(int i) {
        SpellCheckerInfo currentSpellChecker;
        int i2 = this.mSpellCheckerOwnerUserIdMap.get(i);
        TextServicesData textServicesData = this.mUserData.get(i2);
        if (i2 != i && (textServicesData == null || (currentSpellChecker = textServicesData.getCurrentSpellChecker()) == null || (currentSpellChecker.getServiceInfo().applicationInfo.flags & 1) == 0)) {
            return null;
        }
        return textServicesData;
    }

    private static final class SessionRequest {
        public final Bundle mBundle;
        public final String mLocale;
        public final ISpellCheckerSessionListener mScListener;
        public final ITextServicesSessionListener mTsListener;
        public final int mUid;

        SessionRequest(int i, String str, ITextServicesSessionListener iTextServicesSessionListener, ISpellCheckerSessionListener iSpellCheckerSessionListener, Bundle bundle) {
            this.mUid = i;
            this.mLocale = str;
            this.mTsListener = iTextServicesSessionListener;
            this.mScListener = iSpellCheckerSessionListener;
            this.mBundle = bundle;
        }
    }

    private final class SpellCheckerBindGroup {
        private boolean mConnected;
        private final InternalServiceConnection mInternalConnection;
        private ISpellCheckerService mSpellChecker;
        HashMap<String, SpellCheckerBindGroup> mSpellCheckerBindGroups;
        private boolean mUnbindCalled;
        private final String TAG = SpellCheckerBindGroup.class.getSimpleName();
        private final ArrayList<SessionRequest> mPendingSessionRequests = new ArrayList<>();
        private final ArrayList<SessionRequest> mOnGoingSessionRequests = new ArrayList<>();
        private final InternalDeathRecipients mListeners = new InternalDeathRecipients(this);

        public SpellCheckerBindGroup(InternalServiceConnection internalServiceConnection) {
            this.mInternalConnection = internalServiceConnection;
            this.mSpellCheckerBindGroups = internalServiceConnection.mSpellCheckerBindGroups;
        }

        public void onServiceConnectedLocked(ISpellCheckerService iSpellCheckerService) {
            if (this.mUnbindCalled) {
                return;
            }
            this.mSpellChecker = iSpellCheckerService;
            this.mConnected = true;
            try {
                int size = this.mPendingSessionRequests.size();
                for (int i = 0; i < size; i++) {
                    SessionRequest sessionRequest = this.mPendingSessionRequests.get(i);
                    this.mSpellChecker.getISpellCheckerSession(sessionRequest.mLocale, sessionRequest.mScListener, sessionRequest.mBundle, new ISpellCheckerServiceCallbackBinder(this, sessionRequest));
                    this.mOnGoingSessionRequests.add(sessionRequest);
                }
                this.mPendingSessionRequests.clear();
            } catch (RemoteException e) {
                removeAllLocked();
            }
            cleanLocked();
        }

        public void onServiceDisconnectedLocked() {
            this.mSpellChecker = null;
            this.mConnected = false;
        }

        public void removeListener(ISpellCheckerSessionListener iSpellCheckerSessionListener) {
            synchronized (TextServicesManagerService.this.mLock) {
                this.mListeners.unregister(iSpellCheckerSessionListener);
                final IBinder iBinderAsBinder = iSpellCheckerSessionListener.asBinder();
                Predicate<? super SessionRequest> predicate = new Predicate() {
                    @Override
                    public final boolean test(Object obj) {
                        return TextServicesManagerService.SpellCheckerBindGroup.lambda$removeListener$0(iBinderAsBinder, (TextServicesManagerService.SessionRequest) obj);
                    }
                };
                this.mPendingSessionRequests.removeIf(predicate);
                this.mOnGoingSessionRequests.removeIf(predicate);
                cleanLocked();
            }
        }

        static boolean lambda$removeListener$0(IBinder iBinder, SessionRequest sessionRequest) {
            return sessionRequest.mScListener.asBinder() == iBinder;
        }

        private void cleanLocked() {
            if (this.mUnbindCalled || this.mListeners.getRegisteredCallbackCount() > 0 || !this.mPendingSessionRequests.isEmpty() || !this.mOnGoingSessionRequests.isEmpty()) {
                return;
            }
            String str = this.mInternalConnection.mSciId;
            if (this.mSpellCheckerBindGroups.get(str) == this) {
                this.mSpellCheckerBindGroups.remove(str);
            }
            TextServicesManagerService.this.mContext.unbindService(this.mInternalConnection);
            this.mUnbindCalled = true;
        }

        public void removeAllLocked() {
            Slog.e(this.TAG, "Remove the spell checker bind unexpectedly.");
            for (int registeredCallbackCount = this.mListeners.getRegisteredCallbackCount() - 1; registeredCallbackCount >= 0; registeredCallbackCount--) {
                this.mListeners.unregister(this.mListeners.getRegisteredCallbackItem(registeredCallbackCount));
            }
            this.mPendingSessionRequests.clear();
            this.mOnGoingSessionRequests.clear();
            cleanLocked();
        }

        public void getISpellCheckerSessionOrQueueLocked(SessionRequest sessionRequest) {
            if (this.mUnbindCalled) {
                return;
            }
            this.mListeners.register(sessionRequest.mScListener);
            if (!this.mConnected) {
                this.mPendingSessionRequests.add(sessionRequest);
                return;
            }
            try {
                this.mSpellChecker.getISpellCheckerSession(sessionRequest.mLocale, sessionRequest.mScListener, sessionRequest.mBundle, new ISpellCheckerServiceCallbackBinder(this, sessionRequest));
                this.mOnGoingSessionRequests.add(sessionRequest);
            } catch (RemoteException e) {
                removeAllLocked();
            }
            cleanLocked();
        }

        void onSessionCreated(ISpellCheckerSession iSpellCheckerSession, SessionRequest sessionRequest) {
            synchronized (TextServicesManagerService.this.mLock) {
                if (this.mUnbindCalled) {
                    return;
                }
                if (this.mOnGoingSessionRequests.remove(sessionRequest)) {
                    try {
                        sessionRequest.mTsListener.onServiceConnected(iSpellCheckerSession);
                    } catch (RemoteException e) {
                    }
                }
                cleanLocked();
            }
        }
    }

    private final class InternalServiceConnection implements ServiceConnection {
        private final String mSciId;
        private final HashMap<String, SpellCheckerBindGroup> mSpellCheckerBindGroups;

        public InternalServiceConnection(String str, HashMap<String, SpellCheckerBindGroup> map) {
            this.mSciId = str;
            this.mSpellCheckerBindGroups = map;
        }

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            synchronized (TextServicesManagerService.this.mLock) {
                onServiceConnectedInnerLocked(componentName, iBinder);
            }
        }

        private void onServiceConnectedInnerLocked(ComponentName componentName, IBinder iBinder) {
            ISpellCheckerService iSpellCheckerServiceAsInterface = ISpellCheckerService.Stub.asInterface(iBinder);
            SpellCheckerBindGroup spellCheckerBindGroup = this.mSpellCheckerBindGroups.get(this.mSciId);
            if (spellCheckerBindGroup != null && this == spellCheckerBindGroup.mInternalConnection) {
                spellCheckerBindGroup.onServiceConnectedLocked(iSpellCheckerServiceAsInterface);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            synchronized (TextServicesManagerService.this.mLock) {
                onServiceDisconnectedInnerLocked(componentName);
            }
        }

        private void onServiceDisconnectedInnerLocked(ComponentName componentName) {
            SpellCheckerBindGroup spellCheckerBindGroup = this.mSpellCheckerBindGroups.get(this.mSciId);
            if (spellCheckerBindGroup != null && this == spellCheckerBindGroup.mInternalConnection) {
                spellCheckerBindGroup.onServiceDisconnectedLocked();
            }
        }
    }

    private static final class InternalDeathRecipients extends RemoteCallbackList<ISpellCheckerSessionListener> {
        private final SpellCheckerBindGroup mGroup;

        public InternalDeathRecipients(SpellCheckerBindGroup spellCheckerBindGroup) {
            this.mGroup = spellCheckerBindGroup;
        }

        @Override
        public void onCallbackDied(ISpellCheckerSessionListener iSpellCheckerSessionListener) {
            this.mGroup.removeListener(iSpellCheckerSessionListener);
        }
    }

    private static final class ISpellCheckerServiceCallbackBinder extends ISpellCheckerServiceCallback.Stub {
        private final SpellCheckerBindGroup mBindGroup;
        private final SessionRequest mRequest;

        ISpellCheckerServiceCallbackBinder(SpellCheckerBindGroup spellCheckerBindGroup, SessionRequest sessionRequest) {
            this.mBindGroup = spellCheckerBindGroup;
            this.mRequest = sessionRequest;
        }

        public void onSessionCreated(ISpellCheckerSession iSpellCheckerSession) {
            this.mBindGroup.onSessionCreated(iSpellCheckerSession, this.mRequest);
        }
    }
}
