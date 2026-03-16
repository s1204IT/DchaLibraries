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
    public static String constructReminderLabel(Context context, int minutes, boolean abbrev) {
        int value;
        int resId;
        Resources resources = context.getResources();
        if (minutes % 60 != 0) {
            value = minutes;
            if (abbrev) {
                resId = R.plurals.Nmins;
            } else {
                resId = R.plurals.Nminutes;
            }
        } else if (minutes % 1440 != 0) {
            value = minutes / 60;
            resId = R.plurals.Nhours;
        } else {
            value = minutes / 1440;
            resId = R.plurals.Ndays;
        }
        String format = resources.getQuantityString(resId, value);
        return String.format(format, Integer.valueOf(value));
    }

    public static int findMinutesInReminderList(ArrayList<Integer> values, int minutes) {
        int index = values.indexOf(Integer.valueOf(minutes));
        if (index == -1) {
            Log.e("EventViewUtils", "Cannot find minutes (" + minutes + ") in list");
            return 0;
        }
        return index;
    }

    public static int findMethodInReminderList(ArrayList<Integer> values, int method) {
        int index = values.indexOf(Integer.valueOf(method));
        if (index == -1) {
            return 0;
        }
        return index;
    }

    public static ArrayList<CalendarEventModel.ReminderEntry> reminderItemsToReminders(ArrayList<LinearLayout> reminderItems, ArrayList<Integer> reminderMinuteValues, ArrayList<Integer> reminderMethodValues) {
        int len = reminderItems.size();
        ArrayList<CalendarEventModel.ReminderEntry> reminders = new ArrayList<>(len);
        for (int index = 0; index < len; index++) {
            LinearLayout layout = reminderItems.get(index);
            Spinner minuteSpinner = (Spinner) layout.findViewById(R.id.reminder_minutes_value);
            Spinner methodSpinner = (Spinner) layout.findViewById(R.id.reminder_method_value);
            int minutes = reminderMinuteValues.get(minuteSpinner.getSelectedItemPosition()).intValue();
            int method = reminderMethodValues.get(methodSpinner.getSelectedItemPosition()).intValue();
            reminders.add(CalendarEventModel.ReminderEntry.valueOf(minutes, method));
        }
        return reminders;
    }

    public static void addMinutesToList(Context context, ArrayList<Integer> values, ArrayList<String> labels, int minutes) {
        int index = values.indexOf(Integer.valueOf(minutes));
        if (index == -1) {
            String label = constructReminderLabel(context, minutes, false);
            int len = values.size();
            for (int i = 0; i < len; i++) {
                if (minutes < values.get(i).intValue()) {
                    values.add(i, Integer.valueOf(minutes));
                    labels.add(i, label);
                    return;
                }
            }
            values.add(Integer.valueOf(minutes));
            labels.add(len, label);
        }
    }

    public static void reduceMethodList(ArrayList<Integer> values, ArrayList<String> labels, String allowedMethods) {
        String[] allowedStrings = allowedMethods.split(",");
        int[] allowedValues = new int[allowedStrings.length];
        for (int i = 0; i < allowedValues.length; i++) {
            try {
                allowedValues[i] = Integer.parseInt(allowedStrings[i], 10);
            } catch (NumberFormatException e) {
                Log.w("EventViewUtils", "Bad allowed-strings list: '" + allowedStrings[i] + "' in '" + allowedMethods + "'");
                return;
            }
        }
        for (int i2 = values.size() - 1; i2 >= 0; i2--) {
            int val = values.get(i2).intValue();
            int j = allowedValues.length - 1;
            while (j >= 0 && val != allowedValues[j]) {
                j--;
            }
            if (j < 0) {
                values.remove(i2);
                labels.remove(i2);
            }
        }
    }

    private static void setReminderSpinnerLabels(Activity activity, Spinner spinner, ArrayList<String> labels) {
        Resources res = activity.getResources();
        spinner.setPrompt(res.getString(R.string.reminders_label));
        ArrayAdapter<String> adapter = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter((SpinnerAdapter) adapter);
    }

    public static boolean addReminder(Activity activity, View view, View.OnClickListener listener, ArrayList<LinearLayout> items, ArrayList<Integer> minuteValues, ArrayList<String> minuteLabels, ArrayList<Integer> methodValues, ArrayList<String> methodLabels, CalendarEventModel.ReminderEntry newReminder, int maxReminders, AdapterView.OnItemSelectedListener onItemSelected) {
        if (items.size() >= maxReminders) {
            return false;
        }
        LayoutInflater inflater = activity.getLayoutInflater();
        LinearLayout parent = (LinearLayout) view.findViewById(R.id.reminder_items_container);
        LinearLayout reminderItem = (LinearLayout) inflater.inflate(R.layout.edit_reminder_item, (ViewGroup) null);
        parent.addView(reminderItem);
        ImageButton reminderRemoveButton = (ImageButton) reminderItem.findViewById(R.id.reminder_remove);
        reminderRemoveButton.setOnClickListener(listener);
        Spinner spinner = (Spinner) reminderItem.findViewById(R.id.reminder_minutes_value);
        setReminderSpinnerLabels(activity, spinner, minuteLabels);
        int index = findMinutesInReminderList(minuteValues, newReminder.getMinutes());
        spinner.setSelection(index);
        if (onItemSelected != null) {
            spinner.setTag(Integer.valueOf(index));
            spinner.setOnItemSelectedListener(onItemSelected);
        }
        Spinner spinner2 = (Spinner) reminderItem.findViewById(R.id.reminder_method_value);
        setReminderSpinnerLabels(activity, spinner2, methodLabels);
        int index2 = findMethodInReminderList(methodValues, newReminder.getMethod());
        spinner2.setSelection(index2);
        if (onItemSelected != null) {
            spinner2.setTag(Integer.valueOf(index2));
            spinner2.setOnItemSelectedListener(onItemSelected);
        }
        items.add(reminderItem);
        return true;
    }

    public static void updateAddReminderButton(View view, ArrayList<LinearLayout> reminders, int maxReminders) {
        View reminderAddButton = view.findViewById(R.id.reminder_add);
        if (reminderAddButton != null) {
            if (reminders.size() >= maxReminders) {
                reminderAddButton.setEnabled(false);
                reminderAddButton.setVisibility(8);
            } else {
                reminderAddButton.setEnabled(true);
                reminderAddButton.setVisibility(0);
            }
        }
    }
}
