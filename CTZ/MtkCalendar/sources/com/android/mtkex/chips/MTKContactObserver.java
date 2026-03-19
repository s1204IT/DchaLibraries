package com.android.mtkex.chips;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.ContactsContract;
import android.util.Log;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

@SuppressLint({"NewApi"})
public class MTKContactObserver {
    private Context mContext;
    private Cursor mCursor;
    private static int _IDIndex = 0;
    private static int CONTACT_IDIndex = 0;
    private static int DIRTYIndex = 0;
    private static int DELETEDIndex = 0;
    private static int VERSIONIndex = 0;
    private static final String[] sProjection = {"_id", "contact_id", "dirty", "deleted", "version"};
    private static final String[] sSelectionArgs = {String.valueOf(1), String.valueOf(1)};
    private static HashMap<Long, DirtyContactEvent> sPreDirtyContactMap = new HashMap<>();
    private boolean mStateReady = false;
    private Set mPreDirtyContactSet = new HashSet();
    private Set mDirtyContactSet = new HashSet();
    private HandlerThread mQueryThread = null;
    private Handler mQueryHandler = null;
    private ContentObserver mObserver = null;
    private ContentResolver mResolver = null;
    private ArrayList<ContactListener> mListeners = new ArrayList<>();
    private Runnable mInitRunnable = new Runnable() {
        @Override
        public void run() {
            MTKContactObserver.this.initState();
        }
    };
    private Runnable mDeInitRunnable = new Runnable() {
        @Override
        public void run() {
            MTKContactObserver.this.deInitState();
        }
    };

    public interface ContactListener {
        void onContactChange(Set set);
    }

    public void addContactListener(ContactListener contactListener) {
        Log.d("MTKRecipContactObserver", "addContactListener = " + contactListener);
        synchronized (this.mListeners) {
            this.mListeners.add(contactListener);
            if (this.mListeners.size() == 1) {
                if (this.mQueryHandler == null) {
                    this.mQueryThread = new HandlerThread("MTKRecipContactObserver");
                    this.mQueryThread.start();
                    this.mQueryHandler = new Handler(this.mQueryThread.getLooper());
                }
                this.mQueryHandler.removeCallbacks(this.mDeInitRunnable);
                this.mQueryHandler.post(this.mInitRunnable);
            }
        }
    }

    public void removeContactListener(ContactListener contactListener) {
        Log.d("MTKRecipContactObserver", "removeContactListener = " + contactListener);
        synchronized (this.mListeners) {
            this.mListeners.remove(contactListener);
            if (this.mListeners.size() == 0 && this.mQueryHandler != null) {
                this.mQueryHandler.postDelayed(this.mDeInitRunnable, 3000L);
            }
        }
    }

    public static final class DirtyContactEvent {
        static final String[] action = {"delete", "update", "add"};
        long CID;
        long _ID;
        int delete;
        int dirty;
        int eventType;
        int version;

        public DirtyContactEvent(long j, long j2, int i, int i2, int i3) {
            this._ID = j;
            this.CID = j2;
            this.dirty = i;
            this.delete = i2;
            this.version = i3;
            if (i2 == 1) {
                this.eventType = 0;
                if (MTKContactObserver.sPreDirtyContactMap.containsKey(Long.valueOf(this._ID))) {
                    this.CID = ((DirtyContactEvent) MTKContactObserver.sPreDirtyContactMap.get(Long.valueOf(this._ID))).CID;
                    return;
                }
                return;
            }
            this.eventType = 1;
            if (!MTKContactObserver.sPreDirtyContactMap.containsKey(Long.valueOf(this._ID))) {
                this.eventType = 2;
            }
        }

        public void update(int i, int i2, int i3, int i4) {
            this.eventType = i;
            this.dirty = i2;
            this.delete = i3;
            this.version = i4;
        }

        public boolean equals(Object obj) {
            return hashCode() == obj.hashCode();
        }

        public int hashCode() {
            return (((int) this._ID) << 12) | this.version | (this.dirty << 11) | (this.delete << 10);
        }

        public String toString() {
            return action[this.eventType] + "_ID" + this._ID + "CID" + this.CID + "dt" + this.dirty + "dl" + this.delete + "v" + this.version;
        }
    }

    void deInitState() {
        Log.d("MTKRecipContactObserver", "deInitState");
        this.mStateReady = false;
        this.mResolver.unregisterContentObserver(this.mObserver);
        this.mObserver = null;
        this.mResolver = null;
        this.mCursor = null;
        this.mPreDirtyContactSet.clear();
        sPreDirtyContactMap.clear();
        this.mDirtyContactSet.clear();
    }

    void initState() {
        synchronized (this.mListeners) {
            if (this.mObserver == null) {
                this.mObserver = new ContentObserver(this.mQueryHandler) {
                    @Override
                    public void onChange(boolean z, Uri uri) {
                        super.onChange(z, uri);
                        MTKContactObserver.this.contactChange(z, uri);
                    }
                };
                this.mResolver = this.mContext.getContentResolver();
                this.mResolver.registerContentObserver(ContactsContract.RawContacts.CONTENT_URI, true, this.mObserver);
            }
        }
        this.mCursor = queryDirtyRawContact(null, null);
        if (this.mCursor != null) {
            initCursorIndex();
            while (this.mCursor.moveToNext()) {
                try {
                    DirtyContactEvent dirtyContactItem = getDirtyContactItem(this.mCursor);
                    this.mPreDirtyContactSet.add(dirtyContactItem);
                    sPreDirtyContactMap.put(Long.valueOf(dirtyContactItem._ID), dirtyContactItem);
                } catch (Throwable th) {
                    this.mCursor.close();
                    throw th;
                }
            }
            this.mCursor.close();
            this.mStateReady = true;
            Log.d("MTKRecipContactObserver", "initState sucess");
            return;
        }
        Log.d("MTKRecipContactObserver", "initState fail cause of queryDirtyRawContact return null");
    }

    void initCursorIndex() {
        _IDIndex = this.mCursor.getColumnIndex("_id");
        CONTACT_IDIndex = this.mCursor.getColumnIndex("contact_id");
        DELETEDIndex = this.mCursor.getColumnIndex("deleted");
        DIRTYIndex = this.mCursor.getColumnIndex("dirty");
        VERSIONIndex = this.mCursor.getColumnIndex("version");
    }

    Cursor queryDirtyRawContact(String str, String[] strArr) {
        Cursor cursorQuery = this.mResolver.query(ContactsContract.RawContacts.CONTENT_URI, sProjection, str, strArr, null);
        if (cursorQuery != null) {
            Log.d("MTKRecipContactObserver", "queryDirtyRawContact cursor count = " + cursorQuery.getCount());
        }
        return cursorQuery;
    }

    private void contactChange(boolean z, Uri uri) {
        if (!this.mStateReady) {
            return;
        }
        Log.d("MTKRecipContactObserver", "selfChange = " + z + ", uri = " + uri);
        loadCurrentDirtyContact();
    }

    private void loadCurrentDirtyContact() {
        ArrayList arrayList;
        ArrayList arrayList2;
        this.mCursor = queryDirtyRawContact(null, null);
        if (this.mCursor == null) {
            Log.d("MTKRecipContactObserver", "loadCurrentDirtyContact mCursor is null!");
            return;
        }
        try {
            if (this.mCursor.getCount() < sPreDirtyContactMap.size()) {
                Log.d("MTKRecipContactObserver", "loadCurrentDirtyContact1+++");
                HashSet<DirtyContactEvent> hashSet = new HashSet();
                this.mDirtyContactSet.clear();
                this.mPreDirtyContactSet.clear();
                while (this.mCursor.moveToNext()) {
                    DirtyContactEvent dirtyContactItem = getDirtyContactItem(this.mCursor);
                    hashSet.add(dirtyContactItem);
                    this.mPreDirtyContactSet.add(dirtyContactItem);
                }
                for (DirtyContactEvent dirtyContactEvent : sPreDirtyContactMap.values()) {
                    if (!hashSet.contains(dirtyContactEvent)) {
                        dirtyContactEvent.update(0, 1, 1, dirtyContactEvent.version);
                        this.mDirtyContactSet.add(dirtyContactEvent);
                        Log.d("MTKRecipContactObserver", dirtyContactEvent.toString());
                    }
                }
                Log.d("MTKRecipContactObserver", "loadCurrentDirtyContact1---");
                synchronized (this.mListeners) {
                    arrayList2 = (ArrayList) this.mListeners.clone();
                }
                Iterator it = arrayList2.iterator();
                while (it.hasNext()) {
                    ((ContactListener) it.next()).onContactChange(this.mDirtyContactSet);
                }
                sPreDirtyContactMap.clear();
                for (DirtyContactEvent dirtyContactEvent2 : hashSet) {
                    sPreDirtyContactMap.put(Long.valueOf(dirtyContactEvent2._ID), dirtyContactEvent2);
                }
                Log.d("MTKRecipContactObserver", "cursor position = " + this.mCursor.getPosition());
                this.mCursor.close();
                return;
            }
            this.mDirtyContactSet.clear();
            Log.d("MTKRecipContactObserver", "loadCurrentDirtyContact2+++");
            while (this.mCursor.moveToNext()) {
                DirtyContactEvent dirtyContactItem2 = getDirtyContactItem(this.mCursor);
                if (!this.mPreDirtyContactSet.contains(dirtyContactItem2)) {
                    this.mPreDirtyContactSet.add(dirtyContactItem2);
                    this.mDirtyContactSet.add(dirtyContactItem2);
                    sPreDirtyContactMap.put(Long.valueOf(dirtyContactItem2._ID), dirtyContactItem2);
                    Log.d("MTKRecipContactObserver", dirtyContactItem2.toString());
                }
            }
            Log.d("MTKRecipContactObserver", "loadCurrentDirtyContact2---");
            synchronized (this.mListeners) {
                arrayList = (ArrayList) this.mListeners.clone();
            }
            Iterator it2 = arrayList.iterator();
            while (it2.hasNext()) {
                ((ContactListener) it2.next()).onContactChange(this.mDirtyContactSet);
            }
            Log.d("MTKRecipContactObserver", "cursor position = " + this.mCursor.getPosition());
            this.mCursor.close();
            return;
        } catch (Throwable th) {
            Log.d("MTKRecipContactObserver", "cursor position = " + this.mCursor.getPosition());
            this.mCursor.close();
            throw th;
        }
        Log.d("MTKRecipContactObserver", "cursor position = " + this.mCursor.getPosition());
        this.mCursor.close();
        throw th;
    }

    public MTKContactObserver(Context context) {
        this.mContext = null;
        this.mContext = context;
    }

    DirtyContactEvent getDirtyContactItem(Cursor cursor) {
        return new DirtyContactEvent(cursor.getLong(_IDIndex), cursor.getLong(CONTACT_IDIndex), cursor.getInt(DIRTYIndex), cursor.getInt(DELETEDIndex), cursor.getInt(VERSIONIndex));
    }
}
