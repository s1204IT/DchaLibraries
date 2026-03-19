package com.android.calendar.selectcalendars;

import android.accounts.AccountManager;
import android.accounts.AuthenticatorDescription;
import android.app.FragmentManager;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.Rect;
import android.net.Uri;
import android.provider.CalendarContract;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.TouchDelegate;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CursorTreeAdapter;
import android.widget.TextView;
import com.android.calendar.CalendarColorPickerDialog;
import com.android.calendar.R;
import com.android.calendar.Utils;
import com.android.calendar.selectcalendars.CalendarColorCache;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class SelectSyncedCalendarsMultiAccountAdapter extends CursorTreeAdapter implements View.OnClickListener, CalendarColorCache.OnCalendarColorsLoadedListener {
    private static String mNotSyncedText;
    private static String mSyncedText;
    private final SelectSyncedCalendarsMultiAccountActivity mActivity;
    protected AuthenticatorDescription[] mAuthDescs;
    private CalendarColorCache mCache;
    private Map<Long, Boolean> mCalendarChanges;
    private Map<Long, Boolean> mCalendarInitialStates;
    private AsyncCalendarsUpdater mCalendarsUpdater;
    private Map<String, Cursor> mChildrenCursors;
    private boolean mClosedCursorsFlag;
    private CalendarColorPickerDialog mColorPickerDialog;
    private int mColorViewTouchAreaIncrease;
    private final FragmentManager mFragmentManager;
    private final LayoutInflater mInflater;
    private final boolean mIsTablet;
    private final ContentResolver mResolver;
    private Map<String, AuthenticatorDescription> mTypeToAuthDescription;
    private final View mView;
    private static final Runnable mStopRefreshing = new Runnable() {
        @Override
        public void run() {
            boolean unused = SelectSyncedCalendarsMultiAccountAdapter.mRefresh = false;
        }
    };
    private static int mUpdateToken = 1000;
    private static boolean mRefresh = true;
    private static HashMap<String, Boolean> mIsDuplicateName = new HashMap<>();
    private static final String[] PROJECTION = {"_id", "account_name", "ownerAccount", "calendar_displayName", "calendar_color", "visible", "sync_events", "(account_name=ownerAccount) AS \"primary\"", "account_type"};

    private class AsyncCalendarsUpdater extends AsyncQueryHandler {
        public AsyncCalendarsUpdater(ContentResolver contentResolver) {
            super(contentResolver);
        }

        @Override
        protected void onQueryComplete(int i, Object obj, Cursor cursor) {
            if (cursor != null) {
                synchronized (SelectSyncedCalendarsMultiAccountAdapter.this.mChildrenCursors) {
                    if (!SelectSyncedCalendarsMultiAccountAdapter.this.mClosedCursorsFlag && (SelectSyncedCalendarsMultiAccountAdapter.this.mActivity == null || !SelectSyncedCalendarsMultiAccountAdapter.this.mActivity.isFinishing())) {
                        Cursor cursor2 = (Cursor) SelectSyncedCalendarsMultiAccountAdapter.this.mChildrenCursors.get(obj);
                        if (cursor2 != null && Utils.compareCursors(cursor2, cursor)) {
                            cursor.close();
                            return;
                        }
                        MatrixCursor matrixCursorMatrixCursorFromCursor = Utils.matrixCursorFromCursor(cursor);
                        cursor.close();
                        Utils.checkForDuplicateNames(SelectSyncedCalendarsMultiAccountAdapter.mIsDuplicateName, matrixCursorMatrixCursorFromCursor, 3);
                        SelectSyncedCalendarsMultiAccountAdapter.this.mChildrenCursors.put((String) obj, matrixCursorMatrixCursorFromCursor);
                        try {
                            SelectSyncedCalendarsMultiAccountAdapter.this.setChildrenCursor(i, matrixCursorMatrixCursorFromCursor);
                        } catch (NullPointerException e) {
                            Log.w("Calendar", "Adapter expired, try again on the next query: " + e);
                        }
                        if (cursor2 != null) {
                            cursor2.close();
                            return;
                        }
                        return;
                    }
                    cursor.close();
                }
            }
        }
    }

    @Override
    public void onClick(View view) {
        long jLongValue = ((Long) view.getTag(R.id.calendar)).longValue();
        boolean zBooleanValue = this.mCalendarInitialStates.get(Long.valueOf(jLongValue)).booleanValue();
        boolean z = this.mCalendarChanges.containsKey(Long.valueOf(jLongValue)) ? !this.mCalendarChanges.get(Long.valueOf(jLongValue)).booleanValue() : !zBooleanValue;
        if (z == zBooleanValue) {
            this.mCalendarChanges.remove(Long.valueOf(jLongValue));
        } else {
            this.mCalendarChanges.put(Long.valueOf(jLongValue), Boolean.valueOf(z));
        }
        ((CheckBox) view.getTag(R.id.sync)).setChecked(z);
        setText(view, R.id.status, z ? mSyncedText : mNotSyncedText);
    }

    public SelectSyncedCalendarsMultiAccountAdapter(Context context, Cursor cursor, SelectSyncedCalendarsMultiAccountActivity selectSyncedCalendarsMultiAccountActivity) {
        super(cursor, context);
        this.mTypeToAuthDescription = new HashMap();
        this.mCalendarChanges = new HashMap();
        this.mCalendarInitialStates = new HashMap();
        this.mChildrenCursors = new HashMap();
        mSyncedText = context.getString(R.string.synced);
        mNotSyncedText = context.getString(R.string.not_synced);
        this.mCache = new CalendarColorCache(context, this);
        this.mInflater = (LayoutInflater) context.getSystemService("layout_inflater");
        this.mResolver = context.getContentResolver();
        this.mActivity = selectSyncedCalendarsMultiAccountActivity;
        this.mFragmentManager = selectSyncedCalendarsMultiAccountActivity.getFragmentManager();
        this.mColorPickerDialog = (CalendarColorPickerDialog) this.mFragmentManager.findFragmentByTag("ColorPickerDialog");
        this.mIsTablet = Utils.getConfigBool(context, R.bool.tablet_config);
        if (this.mCalendarsUpdater == null) {
            this.mCalendarsUpdater = new AsyncCalendarsUpdater(this.mResolver);
        }
        if (cursor == null || cursor.getCount() == 0) {
            Log.i("Calendar", "SelectCalendarsAdapter: No accounts were returned!");
        }
        this.mAuthDescs = AccountManager.get(context).getAuthenticatorTypes();
        for (int i = 0; i < this.mAuthDescs.length; i++) {
            this.mTypeToAuthDescription.put(this.mAuthDescs[i].type, this.mAuthDescs[i]);
        }
        this.mView = this.mActivity.getExpandableListView();
        mRefresh = true;
        this.mClosedCursorsFlag = false;
        this.mColorViewTouchAreaIncrease = context.getResources().getDimensionPixelSize(R.dimen.color_view_touch_area_increase);
    }

    public void startRefreshStopDelay() {
        mRefresh = true;
        this.mView.postDelayed(mStopRefreshing, 60000L);
    }

    public void cancelRefreshStopDelay() {
        this.mView.removeCallbacks(mStopRefreshing);
    }

    public void doSaveAction() {
        this.mCalendarsUpdater.cancelOperation(mUpdateToken);
        mUpdateToken++;
        if (mUpdateToken < 1000) {
            mUpdateToken = 1000;
        }
        Iterator<Long> it = this.mCalendarChanges.keySet().iterator();
        while (it.hasNext()) {
            long jLongValue = it.next().longValue();
            boolean zBooleanValue = this.mCalendarChanges.get(Long.valueOf(jLongValue)).booleanValue();
            Uri uriWithAppendedId = ContentUris.withAppendedId(CalendarContract.Calendars.CONTENT_URI, jLongValue);
            ContentValues contentValues = new ContentValues();
            contentValues.put("visible", Integer.valueOf(zBooleanValue ? 1 : 0));
            contentValues.put("sync_events", Integer.valueOf(zBooleanValue ? 1 : 0));
            this.mCalendarsUpdater.startUpdate(mUpdateToken, Long.valueOf(jLongValue), uriWithAppendedId, contentValues, null, null);
        }
    }

    private static void setText(View view, int i, String str) {
        if (TextUtils.isEmpty(str)) {
            return;
        }
        ((TextView) view.findViewById(i)).setText(str);
    }

    protected CharSequence getLabelForType(String str) {
        if (this.mTypeToAuthDescription.containsKey(str)) {
            try {
                AuthenticatorDescription authenticatorDescription = this.mTypeToAuthDescription.get(str);
                return this.mActivity.createPackageContext(authenticatorDescription.packageName, 0).getResources().getText(authenticatorDescription.labelId);
            } catch (PackageManager.NameNotFoundException e) {
                Log.w("Calendar", "No label for account type , type " + str);
            }
        }
        return null;
    }

    @Override
    protected void bindChildView(View view, Context context, Cursor cursor, boolean z) {
        final long j = cursor.getLong(0);
        String string = cursor.getString(3);
        String string2 = cursor.getString(2);
        final String string3 = cursor.getString(1);
        final String string4 = cursor.getString(8);
        int displayColorFromColor = Utils.getDisplayColorFromColor(cursor.getInt(4));
        final View viewFindViewById = view.findViewById(R.id.color);
        viewFindViewById.setEnabled(this.mCache.hasColors(string3, string4));
        viewFindViewById.setBackgroundColor(displayColorFromColor);
        final View view2 = (View) viewFindViewById.getParent();
        view2.post(new Runnable() {
            @Override
            public void run() {
                Rect rect = new Rect();
                viewFindViewById.getHitRect(rect);
                rect.top -= SelectSyncedCalendarsMultiAccountAdapter.this.mColorViewTouchAreaIncrease;
                rect.bottom += SelectSyncedCalendarsMultiAccountAdapter.this.mColorViewTouchAreaIncrease;
                rect.left -= SelectSyncedCalendarsMultiAccountAdapter.this.mColorViewTouchAreaIncrease;
                rect.right += SelectSyncedCalendarsMultiAccountAdapter.this.mColorViewTouchAreaIncrease;
                view2.setTouchDelegate(new TouchDelegate(rect, viewFindViewById));
            }
        });
        viewFindViewById.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view3) {
                if (SelectSyncedCalendarsMultiAccountAdapter.this.mCache.hasColors(string3, string4)) {
                    if (SelectSyncedCalendarsMultiAccountAdapter.this.mColorPickerDialog != null) {
                        SelectSyncedCalendarsMultiAccountAdapter.this.mColorPickerDialog.setCalendarId(j);
                    } else {
                        SelectSyncedCalendarsMultiAccountAdapter.this.mColorPickerDialog = CalendarColorPickerDialog.newInstance(j, SelectSyncedCalendarsMultiAccountAdapter.this.mIsTablet);
                    }
                    SelectSyncedCalendarsMultiAccountAdapter.this.mFragmentManager.executePendingTransactions();
                    if (!SelectSyncedCalendarsMultiAccountAdapter.this.mColorPickerDialog.isAdded()) {
                        SelectSyncedCalendarsMultiAccountAdapter.this.mColorPickerDialog.show(SelectSyncedCalendarsMultiAccountAdapter.this.mFragmentManager, "ColorPickerDialog");
                    }
                }
            }
        });
        if (mIsDuplicateName.containsKey(string) && mIsDuplicateName.get(string).booleanValue() && !string.equalsIgnoreCase(string2)) {
            string = string + " <" + string2 + ">";
        }
        setText(view, R.id.calendar, string);
        Boolean boolValueOf = this.mCalendarChanges.get(Long.valueOf(j));
        if (boolValueOf == null) {
            boolValueOf = Boolean.valueOf(cursor.getInt(6) == 1);
            this.mCalendarInitialStates.put(Long.valueOf(j), boolValueOf);
        }
        CheckBox checkBox = (CheckBox) view.findViewById(R.id.sync);
        checkBox.setChecked(boolValueOf.booleanValue());
        setText(view, R.id.status, boolValueOf.booleanValue() ? mSyncedText : mNotSyncedText);
        view.setTag(R.id.calendar, Long.valueOf(j));
        view.setTag(R.id.sync, checkBox);
        view.setOnClickListener(this);
    }

    @Override
    protected void bindGroupView(View view, Context context, Cursor cursor, boolean z) {
        int columnIndexOrThrow = cursor.getColumnIndexOrThrow("account_name");
        int columnIndexOrThrow2 = cursor.getColumnIndexOrThrow("account_type");
        String string = cursor.getString(columnIndexOrThrow);
        CharSequence labelForType = getLabelForType(cursor.getString(columnIndexOrThrow2));
        setText(view, R.id.account, string);
        if (labelForType != null) {
            setText(view, R.id.account_type, labelForType.toString());
        }
    }

    @Override
    protected Cursor getChildrenCursor(Cursor cursor) {
        int columnIndexOrThrow = cursor.getColumnIndexOrThrow("account_name");
        int columnIndexOrThrow2 = cursor.getColumnIndexOrThrow("account_type");
        String string = cursor.getString(columnIndexOrThrow);
        String string2 = cursor.getString(columnIndexOrThrow2);
        Cursor cursor2 = this.mChildrenCursors.get(string2 + "#" + string);
        new RefreshCalendars(cursor.getPosition(), string, string2).run();
        return cursor2;
    }

    @Override
    protected View newChildView(Context context, Cursor cursor, boolean z, ViewGroup viewGroup) {
        return this.mInflater.inflate(R.layout.calendar_sync_item, viewGroup, false);
    }

    @Override
    protected View newGroupView(Context context, Cursor cursor, boolean z, ViewGroup viewGroup) {
        return this.mInflater.inflate(R.layout.account_item, viewGroup, false);
    }

    public void closeChildrenCursors() {
        synchronized (this.mChildrenCursors) {
            Iterator<String> it = this.mChildrenCursors.keySet().iterator();
            while (it.hasNext()) {
                Cursor cursor = this.mChildrenCursors.get(it.next());
                if (!cursor.isClosed()) {
                    cursor.close();
                }
            }
            this.mChildrenCursors.clear();
            this.mClosedCursorsFlag = true;
        }
    }

    private class RefreshCalendars implements Runnable {
        String mAccount;
        String mAccountType;
        int mToken;

        public RefreshCalendars(int i, String str, String str2) {
            this.mToken = i;
            this.mAccount = str;
            this.mAccountType = str2;
        }

        @Override
        public void run() {
            SelectSyncedCalendarsMultiAccountAdapter.this.mCalendarsUpdater.cancelOperation(this.mToken);
            if (SelectSyncedCalendarsMultiAccountAdapter.mRefresh) {
                SelectSyncedCalendarsMultiAccountAdapter.this.mView.postDelayed(SelectSyncedCalendarsMultiAccountAdapter.this.new RefreshCalendars(this.mToken, this.mAccount, this.mAccountType), 5000L);
            }
            SelectSyncedCalendarsMultiAccountAdapter.this.mCalendarsUpdater.startQuery(this.mToken, this.mAccountType + "#" + this.mAccount, CalendarContract.Calendars.CONTENT_URI, SelectSyncedCalendarsMultiAccountAdapter.PROJECTION, "account_name=? AND account_type=?", new String[]{this.mAccount, this.mAccountType}, "\"primary\" DESC,calendar_displayName COLLATE NOCASE");
        }
    }

    @Override
    public void onCalendarColorsLoaded() {
        notifyDataSetChanged();
    }
}
