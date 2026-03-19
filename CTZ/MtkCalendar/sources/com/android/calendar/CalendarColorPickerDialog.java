package com.android.calendar;

import android.app.Activity;
import android.app.Dialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.util.SparseIntArray;
import com.android.colorpicker.ColorPickerDialog;
import com.android.colorpicker.ColorPickerSwatch;
import com.android.colorpicker.HsvColorComparator;
import java.util.ArrayList;
import java.util.Arrays;

public class CalendarColorPickerDialog extends ColorPickerDialog {
    static final String[] CALENDARS_PROJECTION = {"account_name", "account_type", "calendar_color"};
    static final String[] COLORS_PROJECTION = {"color", "color_index"};
    private long mCalendarId;
    private SparseIntArray mColorKeyMap = new SparseIntArray();
    private QueryService mService;

    private class QueryService extends AsyncQueryService {
        private QueryService(Context context) {
            super(context);
        }

        @Override
        protected void onQueryComplete(int i, Object obj, Cursor cursor) {
            if (cursor == null) {
                return;
            }
            Activity activity = CalendarColorPickerDialog.this.getActivity();
            if (activity == null || activity.isFinishing()) {
                cursor.close();
                return;
            }
            if (i == 2) {
                if (cursor.moveToFirst()) {
                    ((ColorPickerDialog) CalendarColorPickerDialog.this).mSelectedColor = Utils.getDisplayColorFromColor(cursor.getInt(2));
                    Uri uri = CalendarContract.Colors.CONTENT_URI;
                    String[] strArr = {cursor.getString(0), cursor.getString(1)};
                    cursor.close();
                    startQuery(4, null, uri, CalendarColorPickerDialog.COLORS_PROJECTION, "account_name=? AND account_type=? AND color_type=0", strArr, null);
                    return;
                }
                cursor.close();
                CalendarColorPickerDialog.this.dismiss();
                return;
            }
            if (i == 4) {
                if (cursor.moveToFirst()) {
                    CalendarColorPickerDialog.this.mColorKeyMap.clear();
                    ArrayList arrayList = new ArrayList();
                    do {
                        int i2 = cursor.getInt(1);
                        int displayColorFromColor = Utils.getDisplayColorFromColor(cursor.getInt(0));
                        CalendarColorPickerDialog.this.mColorKeyMap.put(displayColorFromColor, i2);
                        arrayList.add(Integer.valueOf(displayColorFromColor));
                    } while (cursor.moveToNext());
                    Integer[] numArr = (Integer[]) arrayList.toArray(new Integer[arrayList.size()]);
                    Arrays.sort(numArr, new HsvColorComparator());
                    ((ColorPickerDialog) CalendarColorPickerDialog.this).mColors = new int[numArr.length];
                    for (int i3 = 0; i3 < CalendarColorPickerDialog.this.mColors.length; i3++) {
                        CalendarColorPickerDialog.this.mColors[i3] = numArr[i3].intValue();
                    }
                    CalendarColorPickerDialog.this.showPaletteView();
                    cursor.close();
                    return;
                }
                cursor.close();
                CalendarColorPickerDialog.this.dismiss();
            }
        }
    }

    private class OnCalendarColorSelectedListener implements ColorPickerSwatch.OnColorSelectedListener {
        private OnCalendarColorSelectedListener() {
        }

        @Override
        public void onColorSelected(int i) {
            if (i == ((ColorPickerDialog) CalendarColorPickerDialog.this).mSelectedColor || CalendarColorPickerDialog.this.mService == null) {
                return;
            }
            ContentValues contentValues = new ContentValues();
            contentValues.put("calendar_color_index", Integer.valueOf(CalendarColorPickerDialog.this.mColorKeyMap.get(i)));
            CalendarColorPickerDialog.this.mService.startUpdate(CalendarColorPickerDialog.this.mService.getNextToken(), null, ContentUris.withAppendedId(CalendarContract.Calendars.CONTENT_URI, CalendarColorPickerDialog.this.mCalendarId), contentValues, null, null, 0L);
        }
    }

    public static CalendarColorPickerDialog newInstance(long j, boolean z) {
        CalendarColorPickerDialog calendarColorPickerDialog = new CalendarColorPickerDialog();
        calendarColorPickerDialog.setArguments(R.string.calendar_color_picker_dialog_title, 4, z ? 1 : 2);
        calendarColorPickerDialog.setCalendarId(j);
        return calendarColorPickerDialog;
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putLong("calendar_id", this.mCalendarId);
        saveColorKeys(bundle);
    }

    private void saveColorKeys(Bundle bundle) {
        if (this.mColors == null) {
            return;
        }
        int[] iArr = new int[this.mColors.length];
        for (int i = 0; i < this.mColors.length; i++) {
            iArr[i] = this.mColorKeyMap.get(this.mColors[i]);
        }
        bundle.putIntArray("color_keys", iArr);
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        if (bundle != null) {
            this.mCalendarId = bundle.getLong("calendar_id");
            retrieveColorKeys(bundle);
        }
        setOnColorSelectedListener(new OnCalendarColorSelectedListener());
    }

    private void retrieveColorKeys(Bundle bundle) {
        int[] intArray = bundle.getIntArray("color_keys");
        if (this.mColors != null && intArray != null) {
            for (int i = 0; i < this.mColors.length; i++) {
                this.mColorKeyMap.put(this.mColors[i], intArray[i]);
            }
        }
    }

    @Override
    public void setColors(int[] iArr, int i) {
        throw new IllegalStateException("Must call setCalendarId() to update calendar colors");
    }

    public void setCalendarId(long j) {
        if (j != this.mCalendarId) {
            this.mCalendarId = j;
            startQuery();
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle bundle) {
        Dialog dialogOnCreateDialog = super.onCreateDialog(bundle);
        this.mService = new QueryService(getActivity());
        if (this.mColors == null) {
            startQuery();
        }
        return dialogOnCreateDialog;
    }

    private void startQuery() {
        if (this.mService != null) {
            showProgressBarView();
            this.mService.startQuery(2, null, ContentUris.withAppendedId(CalendarContract.Calendars.CONTENT_URI, this.mCalendarId), CALENDARS_PROJECTION, null, null, null);
        }
    }
}
