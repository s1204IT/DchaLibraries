package com.android.calendar.event;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import com.android.calendar.CalendarEventModel;
import com.android.calendar.R;
import java.util.ArrayList;

public class EventViewUtils {
    public static String constructReminderLabel(Context context, int i, boolean z) {
        int i2;
        Resources resources = context.getResources();
        if (i % 60 != 0) {
            if (z) {
                i2 = R.plurals.Nmins;
            } else {
                i2 = R.plurals.Nminutes;
            }
        } else if (i % 1440 != 0) {
            i /= 60;
            i2 = R.plurals.Nhours;
        } else {
            i /= 1440;
            i2 = R.plurals.Ndays;
        }
        return String.format(resources.getQuantityString(i2, i), Integer.valueOf(i));
    }

    public static int findMinutesInReminderList(ArrayList<Integer> arrayList, int i) {
        int iIndexOf = arrayList.indexOf(Integer.valueOf(i));
        if (iIndexOf == -1) {
            Log.e("EventViewUtils", "Cannot find minutes (" + i + ") in list");
            return 0;
        }
        return iIndexOf;
    }

    public static int findMethodInReminderList(ArrayList<Integer> arrayList, int i) {
        int iIndexOf = arrayList.indexOf(Integer.valueOf(i));
        if (iIndexOf == -1) {
            return 0;
        }
        return iIndexOf;
    }

    public static ArrayList<CalendarEventModel.ReminderEntry> reminderItemsToReminders(ArrayList<LinearLayout> arrayList, ArrayList<Integer> arrayList2, ArrayList<Integer> arrayList3) {
        int size = arrayList.size();
        ArrayList<CalendarEventModel.ReminderEntry> arrayList4 = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            LinearLayout linearLayout = arrayList.get(i);
            arrayList4.add(CalendarEventModel.ReminderEntry.valueOf(arrayList2.get(((Spinner) linearLayout.findViewById(R.id.reminder_minutes_value)).getSelectedItemPosition()).intValue(), arrayList3.get(((Spinner) linearLayout.findViewById(R.id.reminder_method_value)).getSelectedItemPosition()).intValue()));
        }
        return arrayList4;
    }

    public static void addMinutesToList(Context context, ArrayList<Integer> arrayList, ArrayList<String> arrayList2, int i) {
        if (arrayList.indexOf(Integer.valueOf(i)) != -1) {
            return;
        }
        String strConstructReminderLabel = constructReminderLabel(context, i, false);
        int size = arrayList.size();
        for (int i2 = 0; i2 < size; i2++) {
            if (i < arrayList.get(i2).intValue()) {
                arrayList.add(i2, Integer.valueOf(i));
                arrayList2.add(i2, strConstructReminderLabel);
                return;
            }
        }
        arrayList.add(Integer.valueOf(i));
        arrayList2.add(size, strConstructReminderLabel);
    }

    public static void reduceMethodList(ArrayList<Integer> arrayList, ArrayList<String> arrayList2, String str) {
        String[] strArrSplit = str.split(",");
        int[] iArr = new int[strArrSplit.length];
        for (int i = 0; i < iArr.length; i++) {
            try {
                iArr[i] = Integer.parseInt(strArrSplit[i], 10);
            } catch (NumberFormatException e) {
                Log.w("EventViewUtils", "Bad allowed-strings list: '" + strArrSplit[i] + "' in '" + str + "'");
                return;
            }
        }
        for (int size = arrayList.size() - 1; size >= 0; size--) {
            int iIntValue = arrayList.get(size).intValue();
            int length = iArr.length - 1;
            while (length >= 0 && iIntValue != iArr[length]) {
                length--;
            }
            if (length < 0) {
                arrayList.remove(size);
                arrayList2.remove(size);
            }
        }
    }

    private static void setReminderSpinnerLabels(Activity activity, Spinner spinner, ArrayList<String> arrayList) {
        spinner.setPrompt(activity.getResources().getString(R.string.reminders_label));
        ArrayAdapter arrayAdapter = new ArrayAdapter(activity, android.R.layout.simple_spinner_item, arrayList);
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter((SpinnerAdapter) arrayAdapter);
    }

    public static boolean addReminder(Activity activity, View view, View.OnClickListener onClickListener, ArrayList<LinearLayout> arrayList, ArrayList<Integer> arrayList2, ArrayList<String> arrayList3, ArrayList<Integer> arrayList4, ArrayList<String> arrayList5, CalendarEventModel.ReminderEntry reminderEntry, int i, AdapterView.OnItemSelectedListener onItemSelectedListener) {
        if (arrayList.size() >= i) {
            return false;
        }
        LayoutInflater layoutInflater = activity.getLayoutInflater();
        LinearLayout linearLayout = (LinearLayout) view.findViewById(R.id.reminder_items_container);
        LinearLayout linearLayout2 = (LinearLayout) layoutInflater.inflate(R.layout.edit_reminder_item, (ViewGroup) null);
        linearLayout.addView(linearLayout2);
        ((ImageButton) linearLayout2.findViewById(R.id.reminder_remove)).setOnClickListener(onClickListener);
        Spinner spinner = (Spinner) linearLayout2.findViewById(R.id.reminder_minutes_value);
        setReminderSpinnerLabels(activity, spinner, arrayList3);
        int iFindMinutesInReminderList = findMinutesInReminderList(arrayList2, reminderEntry.getMinutes());
        spinner.setSelection(iFindMinutesInReminderList);
        if (onItemSelectedListener != null) {
            spinner.setTag(Integer.valueOf(iFindMinutesInReminderList));
            spinner.setOnItemSelectedListener(onItemSelectedListener);
        }
        Spinner spinner2 = (Spinner) linearLayout2.findViewById(R.id.reminder_method_value);
        setReminderSpinnerLabels(activity, spinner2, arrayList5);
        if (arrayList5.size() <= 1) {
            spinner2.setVisibility(8);
        }
        int iFindMethodInReminderList = findMethodInReminderList(arrayList4, reminderEntry.getMethod());
        spinner2.setSelection(iFindMethodInReminderList);
        if (onItemSelectedListener != null) {
            spinner2.setTag(Integer.valueOf(iFindMethodInReminderList));
            spinner2.setOnItemSelectedListener(onItemSelectedListener);
        }
        arrayList.add(linearLayout2);
        return true;
    }

    public static void updateAddReminderButton(View view, ArrayList<LinearLayout> arrayList, int i) {
        View viewFindViewById = view.findViewById(R.id.reminder_add);
        if (viewFindViewById != null) {
            if (arrayList.size() >= i) {
                viewFindViewById.setEnabled(false);
                viewFindViewById.setVisibility(8);
            } else {
                viewFindViewById.setEnabled(true);
                viewFindViewById.setVisibility(0);
            }
        }
    }
}
