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
import com.mediatek.calendar.LogUtil;

public class SelectCalendarsSimpleAdapter extends BaseAdapter implements ListAdapter, CalendarColorCache.OnCalendarColorsLoadedListener {
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
    private static int SELECTED_COLOR_CHIP_SIZE = 0;
    private static int UNSELECTED_COLOR_CHIP_SIZE = 0;
    private static int COLOR_CHIP_LEFT_MARGIN = 0;
    private static int COLOR_CHIP_RIGHT_MARGIN = 0;
    private static int COLOR_CHIP_TOP_OFFSET = 0;
    private static int BOTTOM_ITEM_HEIGHT = 0;
    private static int NORMAL_ITEM_HEIGHT = 0;
    private static int ORG_SELECTED_COLOR_CHIP_SIZE = 16;
    private static int ORG_UNSELECTED_COLOR_CHIP_SIZE = 10;
    private static int ORG_COLOR_CHIP_LEFT_MARGIN = 20;
    private static int ORG_COLOR_CHIP_RIGHT_MARGIN = 8;
    private static int ORG_COLOR_CHIP_TOP_OFFSET = 5;
    private static int ORG_BOTTOM_ITEM_HEIGHT = 64;
    private static int ORG_NORMAL_ITEM_HEIGHT = 48;
    private static float mScale = 0.0f;

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

    public SelectCalendarsSimpleAdapter(Context context, int i, Cursor cursor, FragmentManager fragmentManager) {
        this.mLayout = i;
        this.mOrientation = context.getResources().getConfiguration().orientation;
        initData(cursor);
        this.mInflater = (LayoutInflater) context.getSystemService("layout_inflater");
        this.mRes = context.getResources();
        this.mColorCalendarVisible = this.mRes.getColor(R.color.calendar_visible);
        this.mColorCalendarHidden = this.mRes.getColor(R.color.calendar_hidden);
        this.mColorCalendarSecondaryVisible = this.mRes.getColor(R.color.calendar_secondary_visible);
        this.mColorCalendarSecondaryHidden = this.mRes.getColor(R.color.calendar_secondary_hidden);
        if (mScale == 0.0f) {
            mScale = this.mRes.getDisplayMetrics().density;
            if (SELECTED_COLOR_CHIP_SIZE == 0) {
                SELECTED_COLOR_CHIP_SIZE = (int) (ORG_SELECTED_COLOR_CHIP_SIZE * mScale);
            }
            if (UNSELECTED_COLOR_CHIP_SIZE == 0) {
                UNSELECTED_COLOR_CHIP_SIZE = (int) (ORG_UNSELECTED_COLOR_CHIP_SIZE * mScale);
            }
            if (COLOR_CHIP_LEFT_MARGIN == 0) {
                COLOR_CHIP_LEFT_MARGIN = (int) (ORG_COLOR_CHIP_LEFT_MARGIN * mScale);
            }
            if (COLOR_CHIP_RIGHT_MARGIN == 0) {
                COLOR_CHIP_RIGHT_MARGIN = (int) (ORG_COLOR_CHIP_RIGHT_MARGIN * mScale);
            }
            if (COLOR_CHIP_TOP_OFFSET == 0) {
                COLOR_CHIP_TOP_OFFSET = (int) (ORG_COLOR_CHIP_TOP_OFFSET * mScale);
            }
            if (BOTTOM_ITEM_HEIGHT == 0) {
                BOTTOM_ITEM_HEIGHT = (int) (ORG_BOTTOM_ITEM_HEIGHT * mScale);
            }
            if (NORMAL_ITEM_HEIGHT == 0) {
                NORMAL_ITEM_HEIGHT = (int) (ORG_NORMAL_ITEM_HEIGHT * mScale);
            }
        }
        this.mCache = new CalendarColorCache(context, this);
        this.mFragmentManager = fragmentManager;
        this.mColorPickerDialog = (CalendarColorPickerDialog) fragmentManager.findFragmentByTag("ColorPickerDialog");
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

    private void initData(Cursor cursor) {
        if (this.mCursor != null && cursor != this.mCursor) {
            this.mCursor.close();
        }
        if (cursor == null) {
            this.mCursor = cursor;
            this.mRowCount = 0;
            this.mData = null;
            return;
        }
        this.mCursor = cursor;
        int columnIndexOrThrow = cursor.getColumnIndexOrThrow("_id");
        int columnIndexOrThrow2 = cursor.getColumnIndexOrThrow("calendar_displayName");
        int columnIndexOrThrow3 = cursor.getColumnIndexOrThrow("calendar_color");
        int columnIndexOrThrow4 = cursor.getColumnIndexOrThrow("visible");
        int columnIndexOrThrow5 = cursor.getColumnIndexOrThrow("ownerAccount");
        if (columnIndexOrThrow < 0 || columnIndexOrThrow2 < 0 || columnIndexOrThrow3 < 0 || columnIndexOrThrow4 < 0 || columnIndexOrThrow5 < 0) {
            LogUtil.w("SelectCalendarsAdapter", "getColumIndex failed, return like cursor == null");
            this.mRowCount = 0;
            this.mData = null;
            return;
        }
        this.mIdColumn = columnIndexOrThrow;
        this.mNameColumn = columnIndexOrThrow2;
        this.mColorColumn = columnIndexOrThrow3;
        this.mVisibleColumn = columnIndexOrThrow4;
        this.mOwnerAccountColumn = columnIndexOrThrow5;
        this.mAccountNameColumn = cursor.getColumnIndexOrThrow("account_name");
        this.mAccountTypeColumn = cursor.getColumnIndexOrThrow("account_type");
        this.mRowCount = cursor.getCount();
        this.mData = new CalendarRow[cursor.getCount()];
        cursor.moveToPosition(-1);
        int i = 0;
        while (cursor.moveToNext()) {
            this.mData[i] = new CalendarRow();
            this.mData[i].id = cursor.getLong(this.mIdColumn);
            this.mData[i].displayName = cursor.getString(this.mNameColumn);
            this.mData[i].color = cursor.getInt(this.mColorColumn);
            this.mData[i].selected = cursor.getInt(this.mVisibleColumn) != 0;
            this.mData[i].ownerAccount = cursor.getString(this.mOwnerAccountColumn);
            this.mData[i].accountName = cursor.getString(this.mAccountNameColumn);
            this.mData[i].accountType = cursor.getString(this.mAccountTypeColumn);
            i++;
        }
    }

    public void changeCursor(Cursor cursor) {
        initData(cursor);
        notifyDataSetChanged();
    }

    @Override
    public View getView(final int i, View view, ViewGroup viewGroup) {
        int i2;
        int i3;
        if (i >= this.mRowCount) {
            return null;
        }
        String str = this.mData[i].displayName;
        boolean z = this.mData[i].selected;
        int displayColorFromColor = Utils.getDisplayColorFromColor(this.mData[i].color);
        boolean z2 = false;
        if (view == null) {
            view = this.mInflater.inflate(this.mLayout, viewGroup, false);
            final View viewFindViewById = view.findViewById(R.id.color);
            final View view2 = (View) viewFindViewById.getParent();
            view2.post(new Runnable() {
                @Override
                public void run() {
                    Rect rect = new Rect();
                    viewFindViewById.getHitRect(rect);
                    rect.top -= SelectCalendarsSimpleAdapter.this.mColorViewTouchAreaIncrease;
                    rect.bottom += SelectCalendarsSimpleAdapter.this.mColorViewTouchAreaIncrease;
                    rect.left -= SelectCalendarsSimpleAdapter.this.mColorViewTouchAreaIncrease;
                    rect.right += SelectCalendarsSimpleAdapter.this.mColorViewTouchAreaIncrease;
                    view2.setTouchDelegate(new TouchDelegate(rect, viewFindViewById));
                }
            });
        }
        TextView textView = (TextView) view.findViewById(R.id.calendar);
        textView.setText(str);
        View viewFindViewById2 = view.findViewById(R.id.color);
        viewFindViewById2.setBackgroundColor(displayColorFromColor);
        viewFindViewById2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view3) {
                if (SelectCalendarsSimpleAdapter.this.hasMoreColors(i)) {
                    if (SelectCalendarsSimpleAdapter.this.mColorPickerDialog != null) {
                        SelectCalendarsSimpleAdapter.this.mColorPickerDialog.setCalendarId(SelectCalendarsSimpleAdapter.this.mData[i].id);
                    } else {
                        SelectCalendarsSimpleAdapter.this.mColorPickerDialog = CalendarColorPickerDialog.newInstance(SelectCalendarsSimpleAdapter.this.mData[i].id, SelectCalendarsSimpleAdapter.this.mIsTablet);
                    }
                    SelectCalendarsSimpleAdapter.this.mFragmentManager.executePendingTransactions();
                    if (!SelectCalendarsSimpleAdapter.this.mColorPickerDialog.isAdded()) {
                        SelectCalendarsSimpleAdapter.this.mColorPickerDialog.show(SelectCalendarsSimpleAdapter.this.mFragmentManager, "ColorPickerDialog");
                    }
                }
            }
        });
        if (z) {
            i2 = this.mColorCalendarVisible;
        } else {
            i2 = this.mColorCalendarHidden;
        }
        textView.setTextColor(i2);
        CheckBox checkBox = (CheckBox) view.findViewById(R.id.sync);
        if (checkBox != null) {
            checkBox.setChecked(z);
            viewFindViewById2.setEnabled(hasMoreColors(i));
            ViewGroup.LayoutParams layoutParams = textView.getLayoutParams();
            TextView textView2 = (TextView) view.findViewById(R.id.status);
            if (!TextUtils.isEmpty(this.mData[i].ownerAccount) && !this.mData[i].ownerAccount.equals(str) && !this.mData[i].ownerAccount.endsWith("calendar.google.com")) {
                if (z) {
                    i3 = this.mColorCalendarSecondaryVisible;
                } else {
                    i3 = this.mColorCalendarSecondaryHidden;
                }
                textView2.setText(this.mData[i].ownerAccount);
                textView2.setTextColor(i3);
                textView2.setVisibility(0);
                layoutParams.height = -2;
            } else {
                textView2.setVisibility(8);
                layoutParams.height = -1;
            }
            textView.setLayoutParams(layoutParams);
        } else {
            View viewFindViewById3 = view.findViewById(R.id.color);
            if (z && hasMoreColors(i)) {
                z2 = true;
            }
            viewFindViewById3.setEnabled(z2);
            view.setBackgroundDrawable(getBackground(i, z));
            ViewGroup.LayoutParams layoutParams2 = view.getLayoutParams();
            if (i == this.mData.length - 1) {
                layoutParams2.height = BOTTOM_ITEM_HEIGHT;
            } else {
                layoutParams2.height = NORMAL_ITEM_HEIGHT;
            }
            view.setLayoutParams(layoutParams2);
            CheckBox checkBox2 = (CheckBox) view.findViewById(R.id.visible_check_box);
            if (checkBox2 != null) {
                checkBox2.setChecked(z);
            }
        }
        view.invalidate();
        return view;
    }

    private boolean hasMoreColors(int i) {
        return this.mCache.hasColors(this.mData[i].accountName, this.mData[i].accountType);
    }

    protected Drawable getBackground(int i, boolean z) {
        char c = 0;
        int i2 = (z ? 1 : 0) | ((i == 0 && this.mOrientation == 2) ? (char) 2 : (char) 0) | (i == this.mData.length + (-1) ? 4 : 0);
        if (i > 0 && this.mData[i - 1].selected) {
            c = '\b';
        }
        return this.mRes.getDrawable(TabletCalendarItemBackgrounds.getBackgrounds()[i2 | c]);
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

    public void setVisible(int i, int i2) {
        this.mData[i].selected = i2 != 0;
        notifyDataSetChanged();
    }

    public int getVisible(int i) {
        return this.mData[i].selected ? 1 : 0;
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
