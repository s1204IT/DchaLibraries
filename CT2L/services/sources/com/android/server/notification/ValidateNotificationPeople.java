package com.android.server.notification;

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
import android.util.Log;
import android.util.LruCache;
import android.util.Slog;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class ValidateNotificationPeople implements NotificationSignalExtractor {
    private static final boolean ENABLE_PEOPLE_VALIDATOR = true;
    private static final boolean INFO = true;
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
    private Map<Integer, Context> mUserToContextMap;
    private static final String TAG = "ValidateNoPeople";
    private static final boolean DEBUG = Log.isLoggable(TAG, 3);
    private static final String[] LOOKUP_PROJECTION = {"_id", "starred"};

    static int access$108(ValidateNotificationPeople x0) {
        int i = x0.mEvictionCount;
        x0.mEvictionCount = i + 1;
        return i;
    }

    @Override
    public void initialize(Context context) {
        if (DEBUG) {
            Slog.d(TAG, "Initializing  " + getClass().getSimpleName() + ".");
        }
        this.mUserToContextMap = new ArrayMap();
        this.mBaseContext = context;
        this.mPeopleCache = new LruCache<>(PEOPLE_CACHE_SIZE);
        this.mEnabled = 1 == Settings.Global.getInt(this.mBaseContext.getContentResolver(), SETTING_ENABLE_PEOPLE_VALIDATOR, 1);
        if (this.mEnabled) {
            this.mHandler = new Handler();
            this.mObserver = new ContentObserver(this.mHandler) {
                @Override
                public void onChange(boolean selfChange, Uri uri, int userId) {
                    super.onChange(selfChange, uri, userId);
                    if (ValidateNotificationPeople.DEBUG || ValidateNotificationPeople.this.mEvictionCount % 100 == 0) {
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
    public RankingReconsideration process(NotificationRecord record) {
        if (!this.mEnabled) {
            Slog.i(TAG, "disabled");
            return null;
        }
        if (record == null || record.getNotification() == null) {
            Slog.i(TAG, "skipping empty notification");
            return null;
        }
        if (record.getUserId() == -1) {
            Slog.i(TAG, "skipping global notification");
            return null;
        }
        Context context = getContextAsUser(record.getUser());
        if (context == null) {
            Slog.i(TAG, "skipping notification that lacks a context");
            return null;
        }
        return validatePeople(context, record);
    }

    @Override
    public void setConfig(RankingConfig config) {
    }

    public float getContactAffinity(UserHandle userHandle, Bundle extras, int timeoutMs, float timeoutAffinity) {
        if (DEBUG) {
            Slog.d(TAG, "checking affinity for " + userHandle);
        }
        if (extras == null) {
            return NONE;
        }
        String key = Long.toString(System.nanoTime());
        float[] affinityOut = new float[1];
        Context context = getContextAsUser(userHandle);
        if (context == null) {
            return NONE;
        }
        final PeopleRankingReconsideration prr = validatePeople(context, key, extras, affinityOut);
        float affinity = affinityOut[0];
        if (prr != null) {
            final Semaphore s = new Semaphore(0);
            AsyncTask.THREAD_POOL_EXECUTOR.execute(new Runnable() {
                @Override
                public void run() {
                    prr.work();
                    s.release();
                }
            });
            try {
                if (!s.tryAcquire(timeoutMs, TimeUnit.MILLISECONDS)) {
                    Slog.w(TAG, "Timeout while waiting for affinity: " + key + ". Returning timeoutAffinity=" + timeoutAffinity);
                    return timeoutAffinity;
                }
                return Math.max(prr.getContactAffinity(), affinity);
            } catch (InterruptedException e) {
                Slog.w(TAG, "InterruptedException while waiting for affinity: " + key + ". Returning affinity=" + affinity, e);
                return affinity;
            }
        }
        return affinity;
    }

    private Context getContextAsUser(UserHandle userHandle) {
        Context context = this.mUserToContextMap.get(Integer.valueOf(userHandle.getIdentifier()));
        if (context == null) {
            try {
                context = this.mBaseContext.createPackageContextAsUser("android", 0, userHandle);
                this.mUserToContextMap.put(Integer.valueOf(userHandle.getIdentifier()), context);
                return context;
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "failed to create package context for lookups", e);
                return context;
            }
        }
        return context;
    }

    private RankingReconsideration validatePeople(Context context, NotificationRecord record) {
        String key = record.getKey();
        Bundle extras = record.getNotification().extras;
        float[] affinityOut = new float[1];
        RankingReconsideration rr = validatePeople(context, key, extras, affinityOut);
        record.setContactAffinity(affinityOut[0]);
        return rr;
    }

    private PeopleRankingReconsideration validatePeople(Context context, String key, Bundle extras, float[] affinityOut) {
        String[] people;
        float affinity = NONE;
        if (extras == null || (people = getExtraPeople(extras)) == null || people.length == 0) {
            return null;
        }
        Slog.i(TAG, "Validating: " + key);
        LinkedList<String> pendingLookups = new LinkedList<>();
        for (int personIdx = 0; personIdx < people.length && personIdx < 10; personIdx++) {
            String handle = people[personIdx];
            if (!TextUtils.isEmpty(handle)) {
                synchronized (this.mPeopleCache) {
                    String cacheKey = getCacheKey(context.getUserId(), handle);
                    LookupResult lookupResult = this.mPeopleCache.get(cacheKey);
                    if (lookupResult == null || lookupResult.isExpired()) {
                        pendingLookups.add(handle);
                    } else if (DEBUG) {
                        Slog.d(TAG, "using cached lookupResult");
                    }
                    if (lookupResult != null) {
                        affinity = Math.max(affinity, lookupResult.getAffinity());
                    }
                }
            }
        }
        affinityOut[0] = affinity;
        if (pendingLookups.isEmpty()) {
            Slog.i(TAG, "final affinity: " + affinity);
            return null;
        }
        if (DEBUG) {
            Slog.d(TAG, "Pending: future work scheduled for: " + key);
        }
        return new PeopleRankingReconsideration(context, key, pendingLookups);
    }

    private String getCacheKey(int userId, String handle) {
        return Integer.toString(userId) + ":" + handle;
    }

    public static String[] getExtraPeople(Bundle extras) {
        Object people = extras.get("android.people");
        if (people instanceof String[]) {
            return (String[]) people;
        }
        if (people instanceof ArrayList) {
            ArrayList arrayList = (ArrayList) people;
            if (arrayList.isEmpty()) {
                return null;
            }
            if (arrayList.get(0) instanceof String) {
                return (String[]) arrayList.toArray(new String[arrayList.size()]);
            }
            if (!(arrayList.get(0) instanceof CharSequence)) {
                return null;
            }
            int N = arrayList.size();
            String[] array = new String[N];
            for (int i = 0; i < N; i++) {
                array[i] = ((CharSequence) arrayList.get(i)).toString();
            }
            return array;
        }
        if (people instanceof String) {
            return new String[]{(String) people};
        }
        if (people instanceof char[]) {
            return new String[]{new String((char[]) people)};
        }
        if (people instanceof CharSequence) {
            return new String[]{((CharSequence) people).toString()};
        }
        if (!(people instanceof CharSequence[])) {
            return null;
        }
        CharSequence[] charSeqArray = (CharSequence[]) people;
        int N2 = charSeqArray.length;
        String[] array2 = new String[N2];
        for (int i2 = 0; i2 < N2; i2++) {
            array2[i2] = charSeqArray[i2].toString();
        }
        return array2;
    }

    private LookupResult resolvePhoneContact(Context context, String number) {
        Uri phoneUri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
        return searchContacts(context, phoneUri);
    }

    private LookupResult resolveEmailContact(Context context, String email) {
        Uri numberUri = Uri.withAppendedPath(ContactsContract.CommonDataKinds.Email.CONTENT_LOOKUP_URI, Uri.encode(email));
        return searchContacts(context, numberUri);
    }

    private LookupResult searchContacts(Context context, Uri lookupUri) {
        LookupResult lookupResult = new LookupResult();
        Cursor c = null;
        try {
            try {
                c = context.getContentResolver().query(lookupUri, LOOKUP_PROJECTION, null, null, null);
                if (c == null) {
                    Slog.w(TAG, "Null cursor from contacts query.");
                } else {
                    while (c.moveToNext()) {
                        lookupResult.mergeContact(c);
                    }
                    if (c != null) {
                        c.close();
                    }
                }
            } catch (Throwable t) {
                Slog.w(TAG, "Problem getting content resolver or performing contacts query.", t);
                if (c != null) {
                    c.close();
                }
            }
            return lookupResult;
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    private static class LookupResult {
        private static final long CONTACT_REFRESH_MILLIS = 3600000;
        private float mAffinity = ValidateNotificationPeople.NONE;
        private final long mExpireMillis = System.currentTimeMillis() + CONTACT_REFRESH_MILLIS;

        public void mergeContact(Cursor cursor) {
            this.mAffinity = Math.max(this.mAffinity, ValidateNotificationPeople.VALID_CONTACT);
            int idIdx = cursor.getColumnIndex("_id");
            if (idIdx >= 0) {
                int id = cursor.getInt(idIdx);
                if (ValidateNotificationPeople.DEBUG) {
                    Slog.d(ValidateNotificationPeople.TAG, "contact _ID is: " + id);
                }
            } else {
                Slog.i(ValidateNotificationPeople.TAG, "invalid cursor: no _ID");
            }
            int starIdx = cursor.getColumnIndex("starred");
            if (starIdx < 0) {
                if (ValidateNotificationPeople.DEBUG) {
                    Slog.d(ValidateNotificationPeople.TAG, "invalid cursor: no STARRED");
                }
            } else {
                boolean isStarred = cursor.getInt(starIdx) != 0;
                if (isStarred) {
                    this.mAffinity = Math.max(this.mAffinity, ValidateNotificationPeople.STARRED_CONTACT);
                }
                if (ValidateNotificationPeople.DEBUG) {
                    Slog.d(ValidateNotificationPeople.TAG, "contact STARRED is: " + isStarred);
                }
            }
        }

        private boolean isExpired() {
            return this.mExpireMillis < System.currentTimeMillis();
        }

        private boolean isInvalid() {
            return this.mAffinity == ValidateNotificationPeople.NONE || isExpired();
        }

        public float getAffinity() {
            return isInvalid() ? ValidateNotificationPeople.NONE : this.mAffinity;
        }
    }

    private class PeopleRankingReconsideration extends RankingReconsideration {
        private float mContactAffinity;
        private final Context mContext;
        private final LinkedList<String> mPendingLookups;

        private PeopleRankingReconsideration(Context context, String key, LinkedList<String> pendingLookups) {
            super(key);
            this.mContactAffinity = ValidateNotificationPeople.NONE;
            this.mContext = context;
            this.mPendingLookups = pendingLookups;
        }

        @Override
        public void work() {
            LookupResult lookupResult;
            Slog.i(ValidateNotificationPeople.TAG, "Executing: validation for: " + this.mKey);
            long timeStartMs = System.currentTimeMillis();
            for (String handle : this.mPendingLookups) {
                Uri uri = Uri.parse(handle);
                if ("tel".equals(uri.getScheme())) {
                    if (ValidateNotificationPeople.DEBUG) {
                        Slog.d(ValidateNotificationPeople.TAG, "checking telephone URI: " + handle);
                    }
                    lookupResult = ValidateNotificationPeople.this.resolvePhoneContact(this.mContext, uri.getSchemeSpecificPart());
                } else if ("mailto".equals(uri.getScheme())) {
                    if (ValidateNotificationPeople.DEBUG) {
                        Slog.d(ValidateNotificationPeople.TAG, "checking mailto URI: " + handle);
                    }
                    lookupResult = ValidateNotificationPeople.this.resolveEmailContact(this.mContext, uri.getSchemeSpecificPart());
                } else if (handle.startsWith(ContactsContract.Contacts.CONTENT_LOOKUP_URI.toString())) {
                    if (ValidateNotificationPeople.DEBUG) {
                        Slog.d(ValidateNotificationPeople.TAG, "checking lookup URI: " + handle);
                    }
                    lookupResult = ValidateNotificationPeople.this.searchContacts(this.mContext, uri);
                } else {
                    lookupResult = new LookupResult();
                    Slog.w(ValidateNotificationPeople.TAG, "unsupported URI " + handle);
                }
                if (lookupResult != null) {
                    synchronized (ValidateNotificationPeople.this.mPeopleCache) {
                        String cacheKey = ValidateNotificationPeople.this.getCacheKey(this.mContext.getUserId(), handle);
                        ValidateNotificationPeople.this.mPeopleCache.put(cacheKey, lookupResult);
                    }
                    this.mContactAffinity = Math.max(this.mContactAffinity, lookupResult.getAffinity());
                }
            }
            if (ValidateNotificationPeople.DEBUG) {
                Slog.d(ValidateNotificationPeople.TAG, "Validation finished in " + (System.currentTimeMillis() - timeStartMs) + "ms");
            }
        }

        @Override
        public void applyChangesLocked(NotificationRecord operand) {
            float affinityBound = operand.getContactAffinity();
            operand.setContactAffinity(Math.max(this.mContactAffinity, affinityBound));
            Slog.i(ValidateNotificationPeople.TAG, "final affinity: " + operand.getContactAffinity());
        }

        public float getContactAffinity() {
            return this.mContactAffinity;
        }
    }
}
