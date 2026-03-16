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
        public AsyncCalendarsUpdater(ContentResolver cr) {
            super(cr);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            if (cursor != null) {
                synchronized (SelectSyncedCalendarsMultiAccountAdapter.this.mChildrenCursors) {
                    if (!SelectSyncedCalendarsMultiAccountAdapter.this.mClosedCursorsFlag && (SelectSyncedCalendarsMultiAccountAdapter.this.mActivity == null || !SelectSyncedCalendarsMultiAccountAdapter.this.mActivity.isFinishing())) {
                        Cursor currentCursor = (Cursor) SelectSyncedCalendarsMultiAccountAdapter.this.mChildrenCursors.get(cookie);
                        if (currentCursor != null && Utils.compareCursors(currentCursor, cursor)) {
                            cursor.close();
                            return;
                        }
                        MatrixCursor newCursor = Utils.matrixCursorFromCursor(cursor);
                        cursor.close();
                        Utils.checkForDuplicateNames(SelectSyncedCalendarsMultiAccountAdapter.mIsDuplicateName, newCursor, 3);
                        SelectSyncedCalendarsMultiAccountAdapter.this.mChildrenCursors.put((String) cookie, newCursor);
                        try {
                            SelectSyncedCalendarsMultiAccountAdapter.this.setChildrenCursor(token, newCursor);
                        } catch (NullPointerException e) {
                            Log.w("Calendar", "Adapter expired, try again on the next query: " + e);
                        }
                        if (currentCursor != null) {
                            currentCursor.close();
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
    public void onClick(View v) {
        boolean newState = true;
        long id = ((Long) v.getTag(R.id.calendar)).longValue();
        boolean initialState = this.mCalendarInitialStates.get(Long.valueOf(id)).booleanValue();
        if (this.mCalendarChanges.containsKey(Long.valueOf(id))) {
            if (this.mCalendarChanges.get(Long.valueOf(id)).booleanValue()) {
                newState = false;
            }
        } else if (initialState) {
            newState = false;
        }
        if (newState == initialState) {
            this.mCalendarChanges.remove(Long.valueOf(id));
        } else {
            this.mCalendarChanges.put(Long.valueOf(id), Boolean.valueOf(newState));
        }
        ((CheckBox) v.getTag(R.id.sync)).setChecked(newState);
        setText(v, R.id.status, newState ? mSyncedText : mNotSyncedText);
    }

    public SelectSyncedCalendarsMultiAccountAdapter(Context context, Cursor acctsCursor, SelectSyncedCalendarsMultiAccountActivity act) {
        super(acctsCursor, context);
        this.mTypeToAuthDescription = new HashMap();
        this.mCalendarChanges = new HashMap();
        this.mCalendarInitialStates = new HashMap();
        this.mChildrenCursors = new HashMap();
        mSyncedText = context.getString(R.string.synced);
        mNotSyncedText = context.getString(R.string.not_synced);
        this.mCache = new CalendarColorCache(context, this);
        this.mInflater = (LayoutInflater) context.getSystemService("layout_inflater");
        this.mResolver = context.getContentResolver();
        this.mActivity = act;
        this.mFragmentManager = act.getFragmentManager();
        this.mColorPickerDialog = (CalendarColorPickerDialog) this.mFragmentManager.findFragmentByTag("ColorPickerDialog");
        this.mIsTablet = Utils.getConfigBool(context, R.bool.tablet_config);
        if (this.mCalendarsUpdater == null) {
            this.mCalendarsUpdater = new AsyncCalendarsUpdater(this.mResolver);
        }
        if (acctsCursor == null || acctsCursor.getCount() == 0) {
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
        Iterator<Long> changeKeys = this.mCalendarChanges.keySet().iterator();
        while (changeKeys.hasNext()) {
            long id = changeKeys.next().longValue();
            boolean newSynced = this.mCalendarChanges.get(Long.valueOf(id)).booleanValue();
            Uri uri = ContentUris.withAppendedId(CalendarContract.Calendars.CONTENT_URI, id);
            ContentValues values = new ContentValues();
            values.put("visible", Integer.valueOf(newSynced ? 1 : 0));
            values.put("sync_events", Integer.valueOf(newSynced ? 1 : 0));
            this.mCalendarsUpdater.startUpdate(mUpdateToken, Long.valueOf(id), uri, values, null, null);
        }
    }

    private static void setText(View view, int id, String text) {
        if (!TextUtils.isEmpty(text)) {
            TextView textView = (TextView) view.findViewById(id);
            textView.setText(text);
        }
    }

    protected CharSequence getLabelForType(String accountType) {
        if (!this.mTypeToAuthDescription.containsKey(accountType)) {
            return null;
        }
        try {
            AuthenticatorDescription desc = this.mTypeToAuthDescription.get(accountType);
            Context authContext = this.mActivity.createPackageContext(desc.packageName, 0);
            CharSequence label = authContext.getResources().getText(desc.labelId);
            return label;
        } catch (PackageManager.NameNotFoundException e) {
            Log.w("Calendar", "No label for account type , type " + accountType);
            return null;
        }
    }

    @Override
    protected void bindChildView(View view, Context context, Cursor cursor, boolean isLastChild) {
        final long id = cursor.getLong(0);
        String name = cursor.getString(3);
        String owner = cursor.getString(2);
        final String accountName = cursor.getString(1);
        final String accountType = cursor.getString(8);
        int color = Utils.getDisplayColorFromColor(cursor.getInt(4));
        final View colorSquare = view.findViewById(R.id.color);
        colorSquare.setEnabled(this.mCache.hasColors(accountName, accountType));
        colorSquare.setBackgroundColor(color);
        final View delegateParent = (View) colorSquare.getParent();
        delegateParent.post(new Runnable() {
            @Override
            public void run() {
                Rect r = new Rect();
                colorSquare.getHitRect(r);
                r.top -= SelectSyncedCalendarsMultiAccountAdapter.this.mColorViewTouchAreaIncrease;
                r.bottom += SelectSyncedCalendarsMultiAccountAdapter.this.mColorViewTouchAreaIncrease;
                r.left -= SelectSyncedCalendarsMultiAccountAdapter.this.mColorViewTouchAreaIncrease;
                r.right += SelectSyncedCalendarsMultiAccountAdapter.this.mColorViewTouchAreaIncrease;
                delegateParent.setTouchDelegate(new TouchDelegate(r, colorSquare));
            }
        });
        colorSquare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (SelectSyncedCalendarsMultiAccountAdapter.this.mCache.hasColors(accountName, accountType)) {
                    if (SelectSyncedCalendarsMultiAccountAdapter.this.mColorPickerDialog != null) {
                        SelectSyncedCalendarsMultiAccountAdapter.this.mColorPickerDialog.setCalendarId(id);
                    } else {
                        SelectSyncedCalendarsMultiAccountAdapter.this.mColorPickerDialog = CalendarColorPickerDialog.newInstance(id, SelectSyncedCalendarsMultiAccountAdapter.this.mIsTablet);
                    }
                    SelectSyncedCalendarsMultiAccountAdapter.this.mFragmentManager.executePendingTransactions();
                    if (!SelectSyncedCalendarsMultiAccountAdapter.this.mColorPickerDialog.isAdded()) {
                        SelectSyncedCalendarsMultiAccountAdapter.this.mColorPickerDialog.show(SelectSyncedCalendarsMultiAccountAdapter.this.mFragmentManager, "ColorPickerDialog");
                    }
                }
            }
        });
        if (mIsDuplicateName.containsKey(name) && mIsDuplicateName.get(name).booleanValue() && !name.equalsIgnoreCase(owner)) {
            name = name + " <" + owner + ">";
        }
        setText(view, R.id.calendar, name);
        Boolean sync = this.mCalendarChanges.get(Long.valueOf(id));
        if (sync == null) {
            sync = Boolean.valueOf(cursor.getInt(6) == 1);
            this.mCalendarInitialStates.put(Long.valueOf(id), sync);
        }
        CheckBox button = (CheckBox) view.findViewById(R.id.sync);
        button.setChecked(sync.booleanValue());
        setText(view, R.id.status, sync.booleanValue() ? mSyncedText : mNotSyncedText);
        view.setTag(R.id.calendar, Long.valueOf(id));
        view.setTag(R.id.sync, button);
        view.setOnClickListener(this);
    }

    @Override
    protected void bindGroupView(View view, Context context, Cursor cursor, boolean isExpanded) {
        int accountColumn = cursor.getColumnIndexOrThrow("account_name");
        int accountTypeColumn = cursor.getColumnIndexOrThrow("account_type");
        String account = cursor.getString(accountColumn);
        String accountType = cursor.getString(accountTypeColumn);
        CharSequence accountLabel = getLabelForType(accountType);
        setText(view, R.id.account, account);
        if (accountLabel != null) {
            setText(view, R.id.account_type, accountLabel.toString());
        }
    }

    @Override
    protected Cursor getChildrenCursor(Cursor groupCursor) {
        int accountColumn = groupCursor.getColumnIndexOrThrow("account_name");
        int accountTypeColumn = groupCursor.getColumnIndexOrThrow("account_type");
        String account = groupCursor.getString(accountColumn);
        String accountType = groupCursor.getString(accountTypeColumn);
        Cursor childCursor = this.mChildrenCursors.get(accountType + "#" + account);
        new RefreshCalendars(groupCursor.getPosition(), account, accountType).run();
        return childCursor;
    }

    @Override
    protected View newChildView(Context context, Cursor cursor, boolean isLastChild, ViewGroup parent) {
        return this.mInflater.inflate(R.layout.calendar_sync_item, parent, false);
    }

    @Override
    protected View newGroupView(Context context, Cursor cursor, boolean isExpanded, ViewGroup parent) {
        return this.mInflater.inflate(R.layout.account_item, parent, false);
    }

    public void closeChildrenCursors() {
        synchronized (this.mChildrenCursors) {
            for (String key : this.mChildrenCursors.keySet()) {
                Cursor cursor = this.mChildrenCursors.get(key);
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

        public RefreshCalendars(int token, String account, String accountType) {
            this.mToken = token;
            this.mAccount = account;
            this.mAccountType = accountType;
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
