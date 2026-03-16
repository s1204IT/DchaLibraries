package com.android.calendar.selectcalendars;

import android.app.FragmentManager;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.TouchDelegate;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ListAdapter;
import android.widget.TextView;
import com.android.calendar.CalendarColorPickerDialog;
import com.android.calendar.R;
import com.android.calendar.Utils;
import com.android.calendar.selectcalendars.CalendarColorCache;

public class SelectCalendarsSimpleAdapter extends BaseAdapter implements ListAdapter, CalendarColorCache.OnCalendarColorsLoadedListener {
    private static int BOTTOM_ITEM_HEIGHT = 64;
    private static int NORMAL_ITEM_HEIGHT = 48;
    private static float mScale = 0.0f;
    private int mAccountNameColumn;
    private int mAccountTypeColumn;
    private CalendarColorCache mCache;
    private int mColorCalendarHidden;
    private int mColorCalendarSecondaryHidden;
    private int mColorCalendarSecondaryVisible;
    private int mColorCalendarVisible;
    private int mColorColumn;
    private CalendarColorPickerDialog mColorPickerDialog;
    private int mColorViewTouchAreaIncrease;
    private Cursor mCursor;
    private CalendarRow[] mData;
    private FragmentManager mFragmentManager;
    private int mIdColumn;
    private LayoutInflater mInflater;
    private boolean mIsTablet;
    private int mLayout;
    private int mNameColumn;
    private int mOrientation;
    private int mOwnerAccountColumn;
    Resources mRes;
    private int mRowCount = 0;
    private int mVisibleColumn;

    private class CalendarRow {
        String accountName;
        String accountType;
        int color;
        String displayName;
        long id;
        String ownerAccount;
        boolean selected;

        private CalendarRow() {
        }
    }

    public SelectCalendarsSimpleAdapter(Context context, int layout, Cursor c, FragmentManager fm) {
        this.mLayout = layout;
        this.mOrientation = context.getResources().getConfiguration().orientation;
        initData(c);
        this.mInflater = (LayoutInflater) context.getSystemService("layout_inflater");
        this.mRes = context.getResources();
        this.mColorCalendarVisible = this.mRes.getColor(R.color.calendar_visible);
        this.mColorCalendarHidden = this.mRes.getColor(R.color.calendar_hidden);
        this.mColorCalendarSecondaryVisible = this.mRes.getColor(R.color.calendar_secondary_visible);
        this.mColorCalendarSecondaryHidden = this.mRes.getColor(R.color.calendar_secondary_hidden);
        if (mScale == 0.0f) {
            mScale = this.mRes.getDisplayMetrics().density;
            BOTTOM_ITEM_HEIGHT = (int) (BOTTOM_ITEM_HEIGHT * mScale);
            NORMAL_ITEM_HEIGHT = (int) (NORMAL_ITEM_HEIGHT * mScale);
        }
        this.mCache = new CalendarColorCache(context, this);
        this.mFragmentManager = fm;
        this.mColorPickerDialog = (CalendarColorPickerDialog) fm.findFragmentByTag("ColorPickerDialog");
        this.mIsTablet = Utils.getConfigBool(context, R.bool.tablet_config);
        this.mColorViewTouchAreaIncrease = context.getResources().getDimensionPixelSize(R.dimen.color_view_touch_area_increase);
    }

    private static class TabletCalendarItemBackgrounds {
        private static int[] mBackgrounds = null;

        static int[] getBackgrounds() {
            if (mBackgrounds != null) {
                return mBackgrounds;
            }
            mBackgrounds = new int[16];
            mBackgrounds[0] = R.drawable.calname_unselected;
            mBackgrounds[1] = R.drawable.calname_select_underunselected;
            mBackgrounds[5] = R.drawable.calname_bottom_select_underunselected;
            mBackgrounds[13] = R.drawable.calname_bottom_select_underselect;
            mBackgrounds[15] = mBackgrounds[13];
            mBackgrounds[7] = mBackgrounds[13];
            mBackgrounds[9] = R.drawable.calname_select_underselect;
            mBackgrounds[11] = mBackgrounds[9];
            mBackgrounds[3] = mBackgrounds[9];
            mBackgrounds[4] = R.drawable.calname_bottom_unselected;
            mBackgrounds[12] = R.drawable.calname_bottom_unselected_underselect;
            mBackgrounds[14] = mBackgrounds[12];
            mBackgrounds[6] = mBackgrounds[12];
            mBackgrounds[8] = R.drawable.calname_unselected_underselect;
            mBackgrounds[10] = mBackgrounds[8];
            mBackgrounds[2] = mBackgrounds[8];
            return mBackgrounds;
        }
    }

    private void initData(Cursor c) {
        if (this.mCursor != null && c != this.mCursor) {
            this.mCursor.close();
        }
        if (c == null) {
            this.mCursor = c;
            this.mRowCount = 0;
            this.mData = null;
            return;
        }
        this.mCursor = c;
        this.mIdColumn = c.getColumnIndexOrThrow("_id");
        this.mNameColumn = c.getColumnIndexOrThrow("calendar_displayName");
        this.mColorColumn = c.getColumnIndexOrThrow("calendar_color");
        this.mVisibleColumn = c.getColumnIndexOrThrow("visible");
        this.mOwnerAccountColumn = c.getColumnIndexOrThrow("ownerAccount");
        this.mAccountNameColumn = c.getColumnIndexOrThrow("account_name");
        this.mAccountTypeColumn = c.getColumnIndexOrThrow("account_type");
        this.mRowCount = c.getCount();
        this.mData = new CalendarRow[c.getCount()];
        c.moveToPosition(-1);
        int p = 0;
        while (c.moveToNext()) {
            this.mData[p] = new CalendarRow();
            this.mData[p].id = c.getLong(this.mIdColumn);
            this.mData[p].displayName = c.getString(this.mNameColumn);
            this.mData[p].color = c.getInt(this.mColorColumn);
            this.mData[p].selected = c.getInt(this.mVisibleColumn) != 0;
            this.mData[p].ownerAccount = c.getString(this.mOwnerAccountColumn);
            this.mData[p].accountName = c.getString(this.mAccountNameColumn);
            this.mData[p].accountType = c.getString(this.mAccountTypeColumn);
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
        int textColor;
        int secondaryColor;
        if (position >= this.mRowCount) {
            return null;
        }
        String name = this.mData[position].displayName;
        boolean selected = this.mData[position].selected;
        int color = Utils.getDisplayColorFromColor(this.mData[position].color);
        if (convertView == null) {
            view = this.mInflater.inflate(this.mLayout, parent, false);
            final View delegate = view.findViewById(R.id.color);
            final View delegateParent = (View) delegate.getParent();
            delegateParent.post(new Runnable() {
                @Override
                public void run() {
                    Rect r = new Rect();
                    delegate.getHitRect(r);
                    r.top -= SelectCalendarsSimpleAdapter.this.mColorViewTouchAreaIncrease;
                    r.bottom += SelectCalendarsSimpleAdapter.this.mColorViewTouchAreaIncrease;
                    r.left -= SelectCalendarsSimpleAdapter.this.mColorViewTouchAreaIncrease;
                    r.right += SelectCalendarsSimpleAdapter.this.mColorViewTouchAreaIncrease;
                    delegateParent.setTouchDelegate(new TouchDelegate(r, delegate));
                }
            });
        } else {
            view = convertView;
        }
        TextView calendarName = (TextView) view.findViewById(R.id.calendar);
        calendarName.setText(name);
        View colorView = view.findViewById(R.id.color);
        colorView.setBackgroundColor(color);
        colorView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (SelectCalendarsSimpleAdapter.this.hasMoreColors(position)) {
                    if (SelectCalendarsSimpleAdapter.this.mColorPickerDialog != null) {
                        SelectCalendarsSimpleAdapter.this.mColorPickerDialog.setCalendarId(SelectCalendarsSimpleAdapter.this.mData[position].id);
                    } else {
                        SelectCalendarsSimpleAdapter.this.mColorPickerDialog = CalendarColorPickerDialog.newInstance(SelectCalendarsSimpleAdapter.this.mData[position].id, SelectCalendarsSimpleAdapter.this.mIsTablet);
                    }
                    SelectCalendarsSimpleAdapter.this.mFragmentManager.executePendingTransactions();
                    if (!SelectCalendarsSimpleAdapter.this.mColorPickerDialog.isAdded()) {
                        SelectCalendarsSimpleAdapter.this.mColorPickerDialog.show(SelectCalendarsSimpleAdapter.this.mFragmentManager, "ColorPickerDialog");
                    }
                }
            }
        });
        if (selected) {
            textColor = this.mColorCalendarVisible;
        } else {
            textColor = this.mColorCalendarHidden;
        }
        calendarName.setTextColor(textColor);
        CheckBox syncCheckBox = (CheckBox) view.findViewById(R.id.sync);
        if (syncCheckBox != null) {
            syncCheckBox.setChecked(selected);
            colorView.setEnabled(hasMoreColors(position));
            ViewGroup.LayoutParams layoutParam = calendarName.getLayoutParams();
            TextView secondaryText = (TextView) view.findViewById(R.id.status);
            if (!TextUtils.isEmpty(this.mData[position].ownerAccount) && !this.mData[position].ownerAccount.equals(name) && !this.mData[position].ownerAccount.endsWith("calendar.google.com")) {
                if (selected) {
                    secondaryColor = this.mColorCalendarSecondaryVisible;
                } else {
                    secondaryColor = this.mColorCalendarSecondaryHidden;
                }
                secondaryText.setText(this.mData[position].ownerAccount);
                secondaryText.setTextColor(secondaryColor);
                secondaryText.setVisibility(0);
                layoutParam.height = -2;
            } else {
                secondaryText.setVisibility(8);
                layoutParam.height = -1;
            }
            calendarName.setLayoutParams(layoutParam);
        } else {
            view.findViewById(R.id.color).setEnabled(selected && hasMoreColors(position));
            view.setBackgroundDrawable(getBackground(position, selected));
            ViewGroup.LayoutParams newParams = view.getLayoutParams();
            if (position == this.mData.length - 1) {
                newParams.height = BOTTOM_ITEM_HEIGHT;
            } else {
                newParams.height = NORMAL_ITEM_HEIGHT;
            }
            view.setLayoutParams(newParams);
            CheckBox visibleCheckBox = (CheckBox) view.findViewById(R.id.visible_check_box);
            if (visibleCheckBox != null) {
                visibleCheckBox.setChecked(selected);
            }
        }
        view.invalidate();
        return view;
    }

    private boolean hasMoreColors(int position) {
        return this.mCache.hasColors(this.mData[position].accountName, this.mData[position].accountType);
    }

    protected Drawable getBackground(int position, boolean selected) {
        int i = 0;
        int bg = selected ? 1 : 0;
        int bg2 = bg | ((position == 0 && this.mOrientation == 2) ? 2 : 0) | (position == this.mData.length + (-1) ? 4 : 0);
        if (position > 0 && this.mData[position - 1].selected) {
            i = 8;
        }
        return this.mRes.getDrawable(TabletCalendarItemBackgrounds.getBackgrounds()[bg2 | i]);
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

    public void setVisible(int position, int visible) {
        this.mData[position].selected = visible != 0;
        notifyDataSetChanged();
    }

    public int getVisible(int position) {
        return this.mData[position].selected ? 1 : 0;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public void onCalendarColorsLoaded() {
        notifyDataSetChanged();
    }
}
