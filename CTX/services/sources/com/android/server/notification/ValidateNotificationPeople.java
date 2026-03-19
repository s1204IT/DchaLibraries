package com.android.server.notification;

import android.app.Person;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;
import android.util.LruCache;
import android.util.Slog;
import com.android.server.pm.PackageManagerService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class ValidateNotificationPeople implements NotificationSignalExtractor {
    private static final boolean ENABLE_PEOPLE_VALIDATOR = true;
    private static final int MAX_PEOPLE = 10;
    static final float NONE = 0.0f;
    private static final int PEOPLE_CACHE_SIZE = 200;
    private static final String SETTING_ENABLE_PEOPLE_VALIDATOR = "validate_notification_people_enabled";
    static final float STARRED_CONTACT = 1.0f;
    static final float VALID_CONTACT = 0.5f;
    private Context mBaseContext;
    protected boolean mEnabled;
    private int mEvictionCount;
    private Handler mHandler;
    private ContentObserver mObserver;
    private LruCache<String, LookupResult> mPeopleCache;
    private NotificationUsageStats mUsageStats;
    private Map<Integer, Context> mUserToContextMap;
    private static final String TAG = "ValidateNoPeople";
    private static final boolean VERBOSE = Log.isLoggable(TAG, 2);
    private static final boolean DEBUG = Log.isLoggable(TAG, 3);
    private static final String[] LOOKUP_PROJECTION = {"_id", "starred"};

    static int access$108(ValidateNotificationPeople validateNotificationPeople) {
        int i = validateNotificationPeople.mEvictionCount;
        validateNotificationPeople.mEvictionCount = i + 1;
        return i;
    }

    @Override
    public void initialize(Context context, NotificationUsageStats notificationUsageStats) {
        if (DEBUG) {
            Slog.d(TAG, "Initializing  " + getClass().getSimpleName() + ".");
        }
        this.mUserToContextMap = new ArrayMap();
        this.mBaseContext = context;
        this.mUsageStats = notificationUsageStats;
        this.mPeopleCache = new LruCache<>(200);
        this.mEnabled = 1 == Settings.Global.getInt(this.mBaseContext.getContentResolver(), SETTING_ENABLE_PEOPLE_VALIDATOR, 1);
        if (this.mEnabled) {
            this.mHandler = new Handler();
            this.mObserver = new ContentObserver(this.mHandler) {
                @Override
                public void onChange(boolean z, Uri uri, int i) {
                    super.onChange(z, uri, i);
                    if ((ValidateNotificationPeople.DEBUG || ValidateNotificationPeople.this.mEvictionCount % 100 == 0) && ValidateNotificationPeople.VERBOSE) {
                        Slog.i(ValidateNotificationPeople.TAG, "mEvictionCount: " + ValidateNotificationPeople.this.mEvictionCount);
                    }
                    ValidateNotificationPeople.this.mPeopleCache.evictAll();
                    ValidateNotificationPeople.access$108(ValidateNotificationPeople.this);
                }
            };
            this.mBaseContext.getContentResolver().registerContentObserver(ContactsContract.Contacts.CONTENT_URI, true, this.mObserver, -1);
        }
    }

    @Override
    public RankingReconsideration process(NotificationRecord notificationRecord) {
        if (!this.mEnabled) {
            if (VERBOSE) {
                Slog.i(TAG, "disabled");
            }
            return null;
        }
        if (notificationRecord == null || notificationRecord.getNotification() == null) {
            if (VERBOSE) {
                Slog.i(TAG, "skipping empty notification");
            }
            return null;
        }
        if (notificationRecord.getUserId() == -1) {
            if (VERBOSE) {
                Slog.i(TAG, "skipping global notification");
            }
            return null;
        }
        Context contextAsUser = getContextAsUser(notificationRecord.getUser());
        if (contextAsUser == null) {
            if (VERBOSE) {
                Slog.i(TAG, "skipping notification that lacks a context");
            }
            return null;
        }
        return validatePeople(contextAsUser, notificationRecord);
    }

    @Override
    public void setConfig(RankingConfig rankingConfig) {
    }

    @Override
    public void setZenHelper(ZenModeHelper zenModeHelper) {
    }

    public float getContactAffinity(UserHandle userHandle, Bundle bundle, int i, float f) {
        if (DEBUG) {
            Slog.d(TAG, "checking affinity for " + userHandle);
        }
        if (bundle == null) {
            return NONE;
        }
        String string = Long.toString(System.nanoTime());
        float[] fArr = new float[1];
        Context contextAsUser = getContextAsUser(userHandle);
        if (contextAsUser == null) {
            return NONE;
        }
        final PeopleRankingReconsideration peopleRankingReconsiderationValidatePeople = validatePeople(contextAsUser, string, bundle, null, fArr);
        float f2 = fArr[0];
        if (peopleRankingReconsiderationValidatePeople != null) {
            final Semaphore semaphore = new Semaphore(0);
            AsyncTask.THREAD_POOL_EXECUTOR.execute(new Runnable() {
                @Override
                public void run() throws Throwable {
                    peopleRankingReconsiderationValidatePeople.work();
                    semaphore.release();
                }
            });
            try {
                if (!semaphore.tryAcquire(i, TimeUnit.MILLISECONDS)) {
                    Slog.w(TAG, "Timeout while waiting for affinity: " + string + ". Returning timeoutAffinity=" + f);
                    return f;
                }
                return Math.max(peopleRankingReconsiderationValidatePeople.getContactAffinity(), f2);
            } catch (InterruptedException e) {
                Slog.w(TAG, "InterruptedException while waiting for affinity: " + string + ". Returning affinity=" + f2, e);
                return f2;
            }
        }
        return f2;
    }

    private Context getContextAsUser(UserHandle userHandle) {
        Context contextCreatePackageContextAsUser;
        Context context = this.mUserToContextMap.get(Integer.valueOf(userHandle.getIdentifier()));
        if (context == null) {
            try {
                contextCreatePackageContextAsUser = this.mBaseContext.createPackageContextAsUser(PackageManagerService.PLATFORM_PACKAGE_NAME, 0, userHandle);
            } catch (PackageManager.NameNotFoundException e) {
                e = e;
            }
            try {
                this.mUserToContextMap.put(Integer.valueOf(userHandle.getIdentifier()), contextCreatePackageContextAsUser);
                return contextCreatePackageContextAsUser;
            } catch (PackageManager.NameNotFoundException e2) {
                e = e2;
                context = contextCreatePackageContextAsUser;
                Log.e(TAG, "failed to create package context for lookups", e);
                return context;
            }
        }
        return context;
    }

    private RankingReconsideration validatePeople(Context context, NotificationRecord notificationRecord) {
        boolean z;
        float[] fArr = new float[1];
        PeopleRankingReconsideration peopleRankingReconsiderationValidatePeople = validatePeople(context, notificationRecord.getKey(), notificationRecord.getNotification().extras, notificationRecord.getPeopleOverride(), fArr);
        boolean z2 = false;
        float f = fArr[0];
        notificationRecord.setContactAffinity(f);
        if (peopleRankingReconsiderationValidatePeople == null) {
            NotificationUsageStats notificationUsageStats = this.mUsageStats;
            if (f > NONE) {
                z = true;
            } else {
                z = false;
            }
            if (f == 1.0f) {
                z2 = true;
            }
            notificationUsageStats.registerPeopleAffinity(notificationRecord, z, z2, true);
        } else {
            peopleRankingReconsiderationValidatePeople.setRecord(notificationRecord);
        }
        return peopleRankingReconsiderationValidatePeople;
    }

    private PeopleRankingReconsideration validatePeople(Context context, String str, Bundle bundle, List<String> list, float[] fArr) {
        if (bundle == null) {
            return null;
        }
        ArraySet<String> arraySet = new ArraySet(list);
        String[] extraPeople = getExtraPeople(bundle);
        if (extraPeople != null) {
            arraySet.addAll(Arrays.asList(extraPeople));
        }
        if (VERBOSE) {
            Slog.i(TAG, "Validating: " + str + " for " + context.getUserId());
        }
        LinkedList linkedList = new LinkedList();
        float fMax = NONE;
        int i = 0;
        for (String str2 : arraySet) {
            if (!TextUtils.isEmpty(str2)) {
                synchronized (this.mPeopleCache) {
                    LookupResult lookupResult = this.mPeopleCache.get(getCacheKey(context.getUserId(), str2));
                    if (lookupResult == null || lookupResult.isExpired()) {
                        linkedList.add(str2);
                    } else if (DEBUG) {
                        Slog.d(TAG, "using cached lookupResult");
                    }
                    if (lookupResult != null) {
                        fMax = Math.max(fMax, lookupResult.getAffinity());
                    }
                }
                i++;
                if (i == 10) {
                    break;
                }
            }
        }
        fArr[0] = fMax;
        if (linkedList.isEmpty()) {
            if (VERBOSE) {
                Slog.i(TAG, "final affinity: " + fMax);
            }
            return null;
        }
        if (DEBUG) {
            Slog.d(TAG, "Pending: future work scheduled for: " + str);
        }
        return new PeopleRankingReconsideration(context, str, linkedList);
    }

    private String getCacheKey(int i, String str) {
        return Integer.toString(i) + ":" + str;
    }

    public static String[] getExtraPeople(Bundle bundle) {
        return combineLists(getExtraPeopleForKey(bundle, "android.people"), getExtraPeopleForKey(bundle, "android.people.list"));
    }

    private static String[] combineLists(String[] strArr, String[] strArr2) {
        if (strArr == null) {
            return strArr2;
        }
        if (strArr2 == null) {
            return strArr;
        }
        ArraySet arraySet = new ArraySet(strArr.length + strArr2.length);
        for (String str : strArr) {
            arraySet.add(str);
        }
        for (String str2 : strArr2) {
            arraySet.add(str2);
        }
        return (String[]) arraySet.toArray();
    }

    private static String[] getExtraPeopleForKey(Bundle bundle, String str) {
        Object obj = bundle.get(str);
        if (obj instanceof String[]) {
            return (String[]) obj;
        }
        int i = 0;
        if (obj instanceof ArrayList) {
            ArrayList arrayList = (ArrayList) obj;
            if (arrayList.isEmpty()) {
                return null;
            }
            if (arrayList.get(0) instanceof String) {
                return (String[]) arrayList.toArray(new String[arrayList.size()]);
            }
            if (arrayList.get(0) instanceof CharSequence) {
                int size = arrayList.size();
                String[] strArr = new String[size];
                while (i < size) {
                    strArr[i] = ((CharSequence) arrayList.get(i)).toString();
                    i++;
                }
                return strArr;
            }
            if (!(arrayList.get(0) instanceof Person)) {
                return null;
            }
            int size2 = arrayList.size();
            String[] strArr2 = new String[size2];
            while (i < size2) {
                strArr2[i] = ((Person) arrayList.get(i)).resolveToLegacyUri();
                i++;
            }
            return strArr2;
        }
        if (obj instanceof String) {
            return new String[]{(String) obj};
        }
        if (obj instanceof char[]) {
            return new String[]{new String((char[]) obj)};
        }
        if (obj instanceof CharSequence) {
            return new String[]{((CharSequence) obj).toString()};
        }
        if (!(obj instanceof CharSequence[])) {
            return null;
        }
        CharSequence[] charSequenceArr = (CharSequence[]) obj;
        int length = charSequenceArr.length;
        String[] strArr3 = new String[length];
        while (i < length) {
            strArr3[i] = charSequenceArr[i].toString();
            i++;
        }
        return strArr3;
    }

    private LookupResult resolvePhoneContact(Context context, String str) {
        return searchContacts(context, Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(str)));
    }

    private LookupResult resolveEmailContact(Context context, String str) {
        return searchContacts(context, Uri.withAppendedPath(ContactsContract.CommonDataKinds.Email.CONTENT_LOOKUP_URI, Uri.encode(str)));
    }

    private LookupResult searchContacts(Context context, Uri uri) throws Throwable {
        Cursor cursorQuery;
        LookupResult lookupResult = new LookupResult();
        Cursor cursor = null;
        try {
            try {
                cursorQuery = context.getContentResolver().query(uri, LOOKUP_PROJECTION, null, null, null);
            } catch (Throwable th) {
                th = th;
            }
        } catch (Throwable th2) {
            th = th2;
        }
        try {
        } catch (Throwable th3) {
            th = th3;
            cursor = cursorQuery;
            Slog.w(TAG, "Problem getting content resolver or performing contacts query.", th);
            if (cursor != null) {
                cursor.close();
            }
        }
        if (cursorQuery == null) {
            Slog.w(TAG, "Null cursor from contacts query.");
            if (cursorQuery != null) {
                cursorQuery.close();
            }
            return lookupResult;
        }
        while (cursorQuery.moveToNext()) {
            lookupResult.mergeContact(cursorQuery);
        }
        if (cursorQuery != null) {
            cursorQuery.close();
        }
        return lookupResult;
    }

    private static class LookupResult {
        private static final long CONTACT_REFRESH_MILLIS = 3600000;
        private float mAffinity = ValidateNotificationPeople.NONE;
        private final long mExpireMillis = System.currentTimeMillis() + 3600000;

        public void mergeContact(Cursor cursor) {
            this.mAffinity = Math.max(this.mAffinity, 0.5f);
            int columnIndex = cursor.getColumnIndex("_id");
            if (columnIndex >= 0) {
                int i = cursor.getInt(columnIndex);
                if (ValidateNotificationPeople.DEBUG) {
                    Slog.d(ValidateNotificationPeople.TAG, "contact _ID is: " + i);
                }
            } else {
                Slog.i(ValidateNotificationPeople.TAG, "invalid cursor: no _ID");
            }
            int columnIndex2 = cursor.getColumnIndex("starred");
            if (columnIndex2 < 0) {
                if (ValidateNotificationPeople.DEBUG) {
                    Slog.d(ValidateNotificationPeople.TAG, "invalid cursor: no STARRED");
                    return;
                }
                return;
            }
            boolean z = cursor.getInt(columnIndex2) != 0;
            if (z) {
                this.mAffinity = Math.max(this.mAffinity, 1.0f);
            }
            if (ValidateNotificationPeople.DEBUG) {
                Slog.d(ValidateNotificationPeople.TAG, "contact STARRED is: " + z);
            }
        }

        private boolean isExpired() {
            return this.mExpireMillis < System.currentTimeMillis();
        }

        private boolean isInvalid() {
            return this.mAffinity == ValidateNotificationPeople.NONE || isExpired();
        }

        public float getAffinity() {
            if (isInvalid()) {
                return ValidateNotificationPeople.NONE;
            }
            return this.mAffinity;
        }
    }

    private class PeopleRankingReconsideration extends RankingReconsideration {
        private static final long LOOKUP_TIME = 1000;
        private float mContactAffinity;
        private final Context mContext;
        private final LinkedList<String> mPendingLookups;
        private NotificationRecord mRecord;

        private PeopleRankingReconsideration(Context context, String str, LinkedList<String> linkedList) {
            super(str, 1000L);
            this.mContactAffinity = ValidateNotificationPeople.NONE;
            this.mContext = context;
            this.mPendingLookups = linkedList;
        }

        @Override
        public void work() throws Throwable {
            LookupResult lookupResultSearchContacts;
            if (ValidateNotificationPeople.VERBOSE) {
                Slog.i(ValidateNotificationPeople.TAG, "Executing: validation for: " + this.mKey);
            }
            long jCurrentTimeMillis = System.currentTimeMillis();
            for (String str : this.mPendingLookups) {
                Uri uri = Uri.parse(str);
                if ("tel".equals(uri.getScheme())) {
                    if (ValidateNotificationPeople.DEBUG) {
                        Slog.d(ValidateNotificationPeople.TAG, "checking telephone URI: " + str);
                    }
                    lookupResultSearchContacts = ValidateNotificationPeople.this.resolvePhoneContact(this.mContext, uri.getSchemeSpecificPart());
                } else if ("mailto".equals(uri.getScheme())) {
                    if (ValidateNotificationPeople.DEBUG) {
                        Slog.d(ValidateNotificationPeople.TAG, "checking mailto URI: " + str);
                    }
                    lookupResultSearchContacts = ValidateNotificationPeople.this.resolveEmailContact(this.mContext, uri.getSchemeSpecificPart());
                } else if (str.startsWith(ContactsContract.Contacts.CONTENT_LOOKUP_URI.toString())) {
                    if (ValidateNotificationPeople.DEBUG) {
                        Slog.d(ValidateNotificationPeople.TAG, "checking lookup URI: " + str);
                    }
                    lookupResultSearchContacts = ValidateNotificationPeople.this.searchContacts(this.mContext, uri);
                } else {
                    LookupResult lookupResult = new LookupResult();
                    if (!com.android.server.pm.Settings.ATTR_NAME.equals(uri.getScheme())) {
                        Slog.w(ValidateNotificationPeople.TAG, "unsupported URI " + str);
                    }
                    lookupResultSearchContacts = lookupResult;
                }
                if (lookupResultSearchContacts != null) {
                    synchronized (ValidateNotificationPeople.this.mPeopleCache) {
                        ValidateNotificationPeople.this.mPeopleCache.put(ValidateNotificationPeople.this.getCacheKey(this.mContext.getUserId(), str), lookupResultSearchContacts);
                    }
                    if (ValidateNotificationPeople.DEBUG) {
                        Slog.d(ValidateNotificationPeople.TAG, "lookup contactAffinity is " + lookupResultSearchContacts.getAffinity());
                    }
                    this.mContactAffinity = Math.max(this.mContactAffinity, lookupResultSearchContacts.getAffinity());
                } else if (ValidateNotificationPeople.DEBUG) {
                    Slog.d(ValidateNotificationPeople.TAG, "lookupResult is null");
                }
            }
            if (ValidateNotificationPeople.DEBUG) {
                Slog.d(ValidateNotificationPeople.TAG, "Validation finished in " + (System.currentTimeMillis() - jCurrentTimeMillis) + "ms");
            }
            if (this.mRecord != null) {
                ValidateNotificationPeople.this.mUsageStats.registerPeopleAffinity(this.mRecord, this.mContactAffinity > ValidateNotificationPeople.NONE, this.mContactAffinity == 1.0f, false);
            }
        }

        @Override
        public void applyChangesLocked(NotificationRecord notificationRecord) {
            notificationRecord.setContactAffinity(Math.max(this.mContactAffinity, notificationRecord.getContactAffinity()));
            if (ValidateNotificationPeople.VERBOSE) {
                Slog.i(ValidateNotificationPeople.TAG, "final affinity: " + notificationRecord.getContactAffinity());
            }
        }

        public float getContactAffinity() {
            return this.mContactAffinity;
        }

        public void setRecord(NotificationRecord notificationRecord) {
            this.mRecord = notificationRecord;
        }
    }
}
