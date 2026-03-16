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
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            if (cursor != null) {
                Activity activity = CalendarColorPickerDialog.this.getActivity();
                if (activity == null || activity.isFinishing()) {
                    cursor.close();
                    return;
                }
                switch (token) {
                    case 2:
                        if (cursor.moveToFirst()) {
                            CalendarColorPickerDialog.this.mSelectedColor = Utils.getDisplayColorFromColor(cursor.getInt(2));
                            Uri uri = CalendarContract.Colors.CONTENT_URI;
                            String[] args = {cursor.getString(0), cursor.getString(1)};
                            cursor.close();
                            startQuery(4, null, uri, CalendarColorPickerDialog.COLORS_PROJECTION, "account_name=? AND account_type=? AND color_type=0", args, null);
                        } else {
                            cursor.close();
                            CalendarColorPickerDialog.this.dismiss();
                        }
                        break;
                    case 4:
                        if (cursor.moveToFirst()) {
                            CalendarColorPickerDialog.this.mColorKeyMap.clear();
                            ArrayList<Integer> colors = new ArrayList<>();
                            do {
                                int colorKey = cursor.getInt(1);
                                int rawColor = cursor.getInt(0);
                                int displayColor = Utils.getDisplayColorFromColor(rawColor);
                                CalendarColorPickerDialog.this.mColorKeyMap.put(displayColor, colorKey);
                                colors.add(Integer.valueOf(displayColor));
                            } while (cursor.moveToNext());
                            Integer[] colorsToSort = (Integer[]) colors.toArray(new Integer[colors.size()]);
                            Arrays.sort(colorsToSort, new HsvColorComparator());
                            CalendarColorPickerDialog.this.mColors = new int[colorsToSort.length];
                            for (int i = 0; i < CalendarColorPickerDialog.this.mColors.length; i++) {
                                CalendarColorPickerDialog.this.mColors[i] = colorsToSort[i].intValue();
                            }
                            CalendarColorPickerDialog.this.showPaletteView();
                            cursor.close();
                        } else {
                            cursor.close();
                            CalendarColorPickerDialog.this.dismiss();
                        }
                        break;
                }
            }
        }
    }

    private class OnCalendarColorSelectedListener implements ColorPickerSwatch.OnColorSelectedListener {
        private OnCalendarColorSelectedListener() {
        }

        @Override
        public void onColorSelected(int color) {
            if (color != CalendarColorPickerDialog.this.mSelectedColor && CalendarColorPickerDialog.this.mService != null) {
                ContentValues values = new ContentValues();
                values.put("calendar_color_index", Integer.valueOf(CalendarColorPickerDialog.this.mColorKeyMap.get(color)));
                CalendarColorPickerDialog.this.mService.startUpdate(CalendarColorPickerDialog.this.mService.getNextToken(), null, ContentUris.withAppendedId(CalendarContract.Calendars.CONTENT_URI, CalendarColorPickerDialog.this.mCalendarId), values, null, null, 0L);
            }
        }
    }

    public static CalendarColorPickerDialog newInstance(long calendarId, boolean isTablet) {
        CalendarColorPickerDialog ret = new CalendarColorPickerDialog();
        ret.setArguments(R.string.calendar_color_picker_dialog_title, 4, isTablet ? 1 : 2);
        ret.setCalendarId(calendarId);
        return ret;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong("calendar_id", this.mCalendarId);
        saveColorKeys(outState);
    }

    private void saveColorKeys(Bundle outState) {
        if (this.mColors != null) {
            int[] colorKeys = new int[this.mColors.length];
            for (int i = 0; i < this.mColors.length; i++) {
                colorKeys[i] = this.mColorKeyMap.get(this.mColors[i]);
            }
            outState.putIntArray("color_keys", colorKeys);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            this.mCalendarId = savedInstanceState.getLong("calendar_id");
            retrieveColorKeys(savedInstanceState);
        }
        setOnColorSelectedListener(new OnCalendarColorSelectedListener());
    }

    private void retrieveColorKeys(Bundle savedInstanceState) {
        int[] colorKeys = savedInstanceState.getIntArray("color_keys");
        if (this.mColors != null && colorKeys != null) {
            for (int i = 0; i < this.mColors.length; i++) {
                this.mColorKeyMap.put(this.mColors[i], colorKeys[i]);
            }
        }
    }

    @Override
    public void setColors(int[] colors, int selectedColor) {
        throw new IllegalStateException("Must call setCalendarId() to update calendar colors");
    }

    public void setCalendarId(long calendarId) {
        if (calendarId != this.mCalendarId) {
            this.mCalendarId = calendarId;
            startQuery();
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        this.mService = new QueryService(getActivity());
        if (this.mColors == null) {
            startQuery();
        }
        return dialog;
    }

    private void startQuery() {
        if (this.mService != null) {
            showProgressBarView();
            this.mService.startQuery(2, null, ContentUris.withAppendedId(CalendarContract.Calendars.CONTENT_URI, this.mCalendarId), CALENDARS_PROJECTION, null, null, null);
        }
    }
}
