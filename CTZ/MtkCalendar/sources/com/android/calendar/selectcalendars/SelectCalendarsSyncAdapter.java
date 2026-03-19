package com.android.calendar.selectcalendars;

import android.app.FragmentManager;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Rect;
import android.graphics.drawable.shapes.RectShape;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.TouchDelegate;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ListAdapter;
import android.widget.TextView;
import com.android.calendar.CalendarColorPickerDialog;
import com.android.calendar.R;
import com.android.calendar.Utils;
import com.android.calendar.selectcalendars.CalendarColorCache;
import com.mediatek.calendar.LogUtil;
import java.util.HashMap;

public class SelectCalendarsSyncAdapter extends BaseAdapter implements AdapterView.OnItemClickListener, ListAdapter, CalendarColorCache.OnCalendarColorsLoadedListener {
    private static int COLOR_CHIP_SIZE = 30;
    private static HashMap<Long, Boolean> mCheckBoxStatus = new HashMap<>();
    private int mAccountNameColumn;
    private int mAccountTypeColumn;
    private CalendarColorCache mCache;
    private int mColorColumn;
    private CalendarColorPickerDialog mColorPickerDialog;
    private int mColorViewTouchAreaIncrease;
    private CalendarRow[] mData;
    private FragmentManager mFragmentManager;
    private int mIdColumn;
    private LayoutInflater mInflater;
    private boolean mIsTablet;
    private int mNameColumn;
    private final String mNotSyncedString;
    private int mSyncedColumn;
    private final String mSyncedString;
    private RectShape r = new RectShape();
    private HashMap<Long, CalendarRow> mChanges = new HashMap<>();
    private int mRowCount = 0;

    public class CalendarRow {
        String accountName;
        String accountType;
        int color;
        String displayName;
        long id;
        boolean originalSynced;
        boolean synced;

        public CalendarRow() {
        }
    }

    public SelectCalendarsSyncAdapter(Context context, Cursor cursor, FragmentManager fragmentManager) {
        initData(cursor);
        this.mCache = new CalendarColorCache(context, this);
        this.mFragmentManager = fragmentManager;
        this.mColorPickerDialog = (CalendarColorPickerDialog) fragmentManager.findFragmentByTag("ColorPickerDialog");
        this.mColorViewTouchAreaIncrease = context.getResources().getDimensionPixelSize(R.dimen.color_view_touch_area_increase);
        this.mIsTablet = Utils.getConfigBool(context, R.bool.tablet_config);
        this.mInflater = (LayoutInflater) context.getSystemService("layout_inflater");
        COLOR_CHIP_SIZE = (int) (COLOR_CHIP_SIZE * context.getResources().getDisplayMetrics().density);
        this.r.resize(COLOR_CHIP_SIZE, COLOR_CHIP_SIZE);
        Resources resources = context.getResources();
        this.mSyncedString = resources.getString(R.string.synced);
        this.mNotSyncedString = resources.getString(R.string.not_synced);
    }

    private void initData(Cursor cursor) {
        if (cursor == null) {
            this.mRowCount = 0;
            this.mData = null;
            return;
        }
        int columnIndexOrThrow = cursor.getColumnIndexOrThrow("_id");
        int columnIndexOrThrow2 = cursor.getColumnIndexOrThrow("calendar_displayName");
        int columnIndexOrThrow3 = cursor.getColumnIndexOrThrow("calendar_color");
        int columnIndexOrThrow4 = cursor.getColumnIndexOrThrow("sync_events");
        if (columnIndexOrThrow < 0 || columnIndexOrThrow2 < 0 || columnIndexOrThrow3 < 0 || columnIndexOrThrow4 < 0) {
            LogUtil.w("SelCalsAdapter", "getColumnIndex failed, return as cursor is null");
            this.mRowCount = 0;
            this.mData = null;
            return;
        }
        this.mIdColumn = columnIndexOrThrow;
        this.mNameColumn = columnIndexOrThrow2;
        this.mColorColumn = columnIndexOrThrow3;
        this.mSyncedColumn = columnIndexOrThrow4;
        this.mAccountNameColumn = cursor.getColumnIndexOrThrow("account_name");
        this.mAccountTypeColumn = cursor.getColumnIndexOrThrow("account_type");
        this.mRowCount = cursor.getCount();
        this.mData = new CalendarRow[this.mRowCount];
        cursor.moveToPosition(-1);
        int i = 0;
        while (cursor.moveToNext()) {
            long j = cursor.getLong(this.mIdColumn);
            this.mData[i] = new CalendarRow();
            this.mData[i].id = j;
            this.mData[i].displayName = cursor.getString(this.mNameColumn);
            this.mData[i].color = cursor.getInt(this.mColorColumn);
            this.mData[i].originalSynced = cursor.getInt(this.mSyncedColumn) != 0;
            this.mData[i].accountName = cursor.getString(this.mAccountNameColumn);
            this.mData[i].accountType = cursor.getString(this.mAccountTypeColumn);
            if (mCheckBoxStatus.containsKey(Long.valueOf(j))) {
                this.mData[i].synced = mCheckBoxStatus.get(Long.valueOf(j)).booleanValue();
            } else {
                this.mData[i].synced = this.mData[i].originalSynced;
            }
            i++;
        }
    }

    public void changeCursor(Cursor cursor) {
        initData(cursor);
        notifyDataSetChanged();
    }

    @Override
    public View getView(final int i, View view, ViewGroup viewGroup) {
        if (i >= this.mRowCount) {
            return null;
        }
        String str = this.mData[i].displayName;
        boolean z = this.mData[i].synced;
        int displayColorFromColor = Utils.getDisplayColorFromColor(this.mData[i].color);
        if (view == null) {
            view = this.mInflater.inflate(R.layout.calendar_sync_item, viewGroup, false);
            final View viewFindViewById = view.findViewById(R.id.color);
            final View view2 = (View) viewFindViewById.getParent();
            view2.post(new Runnable() {
                @Override
                public void run() {
                    Rect rect = new Rect();
                    viewFindViewById.getHitRect(rect);
                    rect.top -= SelectCalendarsSyncAdapter.this.mColorViewTouchAreaIncrease;
                    rect.bottom += SelectCalendarsSyncAdapter.this.mColorViewTouchAreaIncrease;
                    rect.left -= SelectCalendarsSyncAdapter.this.mColorViewTouchAreaIncrease;
                    rect.right += SelectCalendarsSyncAdapter.this.mColorViewTouchAreaIncrease;
                    view2.setTouchDelegate(new TouchDelegate(rect, viewFindViewById));
                }
            });
        }
        view.setTag(this.mData[i]);
        ((CheckBox) view.findViewById(R.id.sync)).setChecked(z);
        if (z) {
            setText(view, R.id.status, this.mSyncedString);
        } else {
            setText(view, R.id.status, this.mNotSyncedString);
        }
        View viewFindViewById2 = view.findViewById(R.id.color);
        viewFindViewById2.setEnabled(hasMoreColors(i));
        viewFindViewById2.setBackgroundColor(displayColorFromColor);
        viewFindViewById2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view3) {
                if (SelectCalendarsSyncAdapter.this.hasMoreColors(i)) {
                    if (SelectCalendarsSyncAdapter.this.mColorPickerDialog != null) {
                        SelectCalendarsSyncAdapter.this.mColorPickerDialog.setCalendarId(SelectCalendarsSyncAdapter.this.mData[i].id);
                    } else {
                        SelectCalendarsSyncAdapter.this.mColorPickerDialog = CalendarColorPickerDialog.newInstance(SelectCalendarsSyncAdapter.this.mData[i].id, SelectCalendarsSyncAdapter.this.mIsTablet);
                    }
                    SelectCalendarsSyncAdapter.this.mFragmentManager.executePendingTransactions();
                    if (!SelectCalendarsSyncAdapter.this.mColorPickerDialog.isAdded()) {
                        SelectCalendarsSyncAdapter.this.mColorPickerDialog.show(SelectCalendarsSyncAdapter.this.mFragmentManager, "ColorPickerDialog");
                    }
                }
            }
        });
        setText(view, R.id.calendar, str);
        return view;
    }

    private boolean hasMoreColors(int i) {
        return this.mCache.hasColors(this.mData[i].accountName, this.mData[i].accountType);
    }

    private static void setText(View view, int i, String str) {
        if (TextUtils.isEmpty(str)) {
            return;
        }
        ((TextView) view.findViewById(i)).setText(str);
    }

    @Override
    public int getCount() {
        return this.mRowCount;
    }

    @Override
    public Object getItem(int i) {
        if (i >= this.mRowCount) {
            return null;
        }
        return this.mData[i];
    }

    @Override
    public long getItemId(int i) {
        if (i >= this.mRowCount) {
            return 0L;
        }
        return this.mData[i].id;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long j) {
        String str;
        CalendarRow calendarRow = (CalendarRow) view.getTag();
        calendarRow.synced = !calendarRow.synced;
        if (calendarRow.synced) {
            str = this.mSyncedString;
        } else {
            str = this.mNotSyncedString;
        }
        setText(view, R.id.status, str);
        ((CheckBox) view.findViewById(R.id.sync)).setChecked(calendarRow.synced);
        this.mChanges.put(Long.valueOf(calendarRow.id), calendarRow);
        mCheckBoxStatus.put(Long.valueOf(calendarRow.id), Boolean.valueOf(calendarRow.synced));
    }

    public HashMap<Long, CalendarRow> getChanges() {
        return this.mChanges;
    }

    public static void clearCheckBoxStatus() {
        mCheckBoxStatus.clear();
    }

    @Override
    public void onCalendarColorsLoaded() {
        notifyDataSetChanged();
    }
}
