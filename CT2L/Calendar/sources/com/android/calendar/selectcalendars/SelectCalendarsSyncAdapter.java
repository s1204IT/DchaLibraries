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
import java.util.HashMap;

public class SelectCalendarsSyncAdapter extends BaseAdapter implements AdapterView.OnItemClickListener, ListAdapter, CalendarColorCache.OnCalendarColorsLoadedListener {
    private static int COLOR_CHIP_SIZE = 30;
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

    public SelectCalendarsSyncAdapter(Context context, Cursor c, FragmentManager manager) {
        initData(c);
        this.mCache = new CalendarColorCache(context, this);
        this.mFragmentManager = manager;
        this.mColorPickerDialog = (CalendarColorPickerDialog) manager.findFragmentByTag("ColorPickerDialog");
        this.mColorViewTouchAreaIncrease = context.getResources().getDimensionPixelSize(R.dimen.color_view_touch_area_increase);
        this.mIsTablet = Utils.getConfigBool(context, R.bool.tablet_config);
        this.mInflater = (LayoutInflater) context.getSystemService("layout_inflater");
        COLOR_CHIP_SIZE = (int) (COLOR_CHIP_SIZE * context.getResources().getDisplayMetrics().density);
        this.r.resize(COLOR_CHIP_SIZE, COLOR_CHIP_SIZE);
        Resources res = context.getResources();
        this.mSyncedString = res.getString(R.string.synced);
        this.mNotSyncedString = res.getString(R.string.not_synced);
    }

    private void initData(Cursor c) {
        if (c == null) {
            this.mRowCount = 0;
            this.mData = null;
            return;
        }
        this.mIdColumn = c.getColumnIndexOrThrow("_id");
        this.mNameColumn = c.getColumnIndexOrThrow("calendar_displayName");
        this.mColorColumn = c.getColumnIndexOrThrow("calendar_color");
        this.mSyncedColumn = c.getColumnIndexOrThrow("sync_events");
        this.mAccountNameColumn = c.getColumnIndexOrThrow("account_name");
        this.mAccountTypeColumn = c.getColumnIndexOrThrow("account_type");
        this.mRowCount = c.getCount();
        this.mData = new CalendarRow[this.mRowCount];
        c.moveToPosition(-1);
        int p = 0;
        while (c.moveToNext()) {
            long id = c.getLong(this.mIdColumn);
            this.mData[p] = new CalendarRow();
            this.mData[p].id = id;
            this.mData[p].displayName = c.getString(this.mNameColumn);
            this.mData[p].color = c.getInt(this.mColorColumn);
            this.mData[p].originalSynced = c.getInt(this.mSyncedColumn) != 0;
            this.mData[p].accountName = c.getString(this.mAccountNameColumn);
            this.mData[p].accountType = c.getString(this.mAccountTypeColumn);
            if (this.mChanges.containsKey(Long.valueOf(id))) {
                this.mData[p].synced = this.mChanges.get(Long.valueOf(id)).synced;
            } else {
                this.mData[p].synced = this.mData[p].originalSynced;
            }
            p++;
        }
    }

    public void changeCursor(Cursor c) {
        initData(c);
        notifyDataSetChanged();
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View view;
        if (position >= this.mRowCount) {
            return null;
        }
        String name = this.mData[position].displayName;
        boolean selected = this.mData[position].synced;
        int color = Utils.getDisplayColorFromColor(this.mData[position].color);
        if (convertView == null) {
            view = this.mInflater.inflate(R.layout.calendar_sync_item, parent, false);
            final View delegate = view.findViewById(R.id.color);
            final View delegateParent = (View) delegate.getParent();
            delegateParent.post(new Runnable() {
                @Override
                public void run() {
                    Rect r = new Rect();
                    delegate.getHitRect(r);
                    r.top -= SelectCalendarsSyncAdapter.this.mColorViewTouchAreaIncrease;
                    r.bottom += SelectCalendarsSyncAdapter.this.mColorViewTouchAreaIncrease;
                    r.left -= SelectCalendarsSyncAdapter.this.mColorViewTouchAreaIncrease;
                    r.right += SelectCalendarsSyncAdapter.this.mColorViewTouchAreaIncrease;
                    delegateParent.setTouchDelegate(new TouchDelegate(r, delegate));
                }
            });
        } else {
            view = convertView;
        }
        view.setTag(this.mData[position]);
        CheckBox cb = (CheckBox) view.findViewById(R.id.sync);
        cb.setChecked(selected);
        if (selected) {
            setText(view, R.id.status, this.mSyncedString);
        } else {
            setText(view, R.id.status, this.mNotSyncedString);
        }
        View colorView = view.findViewById(R.id.color);
        colorView.setEnabled(hasMoreColors(position));
        colorView.setBackgroundColor(color);
        colorView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (SelectCalendarsSyncAdapter.this.hasMoreColors(position)) {
                    if (SelectCalendarsSyncAdapter.this.mColorPickerDialog != null) {
                        SelectCalendarsSyncAdapter.this.mColorPickerDialog.setCalendarId(SelectCalendarsSyncAdapter.this.mData[position].id);
                    } else {
                        SelectCalendarsSyncAdapter.this.mColorPickerDialog = CalendarColorPickerDialog.newInstance(SelectCalendarsSyncAdapter.this.mData[position].id, SelectCalendarsSyncAdapter.this.mIsTablet);
                    }
                    SelectCalendarsSyncAdapter.this.mFragmentManager.executePendingTransactions();
                    if (!SelectCalendarsSyncAdapter.this.mColorPickerDialog.isAdded()) {
                        SelectCalendarsSyncAdapter.this.mColorPickerDialog.show(SelectCalendarsSyncAdapter.this.mFragmentManager, "ColorPickerDialog");
                    }
                }
            }
        });
        setText(view, R.id.calendar, name);
        return view;
    }

    private boolean hasMoreColors(int position) {
        return this.mCache.hasColors(this.mData[position].accountName, this.mData[position].accountType);
    }

    private static void setText(View view, int id, String text) {
        if (!TextUtils.isEmpty(text)) {
            TextView textView = (TextView) view.findViewById(id);
            textView.setText(text);
        }
    }

    @Override
    public int getCount() {
        return this.mRowCount;
    }

    @Override
    public Object getItem(int position) {
        if (position >= this.mRowCount) {
            return null;
        }
        return this.mData[position];
    }

    @Override
    public long getItemId(int position) {
        if (position >= this.mRowCount) {
            return 0L;
        }
        return this.mData[position].id;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        String status;
        CalendarRow row = (CalendarRow) view.getTag();
        row.synced = !row.synced;
        if (row.synced) {
            status = this.mSyncedString;
        } else {
            status = this.mNotSyncedString;
        }
        setText(view, R.id.status, status);
        CheckBox cb = (CheckBox) view.findViewById(R.id.sync);
        cb.setChecked(row.synced);
        this.mChanges.put(Long.valueOf(row.id), row);
    }

    public HashMap<Long, CalendarRow> getChanges() {
        return this.mChanges;
    }

    @Override
    public void onCalendarColorsLoaded() {
        notifyDataSetChanged();
    }
}
