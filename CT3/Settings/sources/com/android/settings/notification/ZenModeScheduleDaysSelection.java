package com.android.settings.notification;

import android.content.Context;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import com.android.settings.R;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;

public class ZenModeScheduleDaysSelection extends ScrollView {
    public static final int[] DAYS = {1, 2, 3, 4, 5, 6, 7};
    private final SimpleDateFormat mDayFormat;
    private final SparseBooleanArray mDays;
    private final LinearLayout mLayout;

    public ZenModeScheduleDaysSelection(Context context, int[] days) {
        super(context);
        this.mDayFormat = new SimpleDateFormat("EEEE");
        this.mDays = new SparseBooleanArray();
        this.mLayout = new LinearLayout(this.mContext);
        int hPad = context.getResources().getDimensionPixelSize(R.dimen.zen_schedule_day_margin);
        this.mLayout.setPadding(hPad, 0, hPad, 0);
        addView(this.mLayout);
        if (days != null) {
            for (int i : days) {
                this.mDays.put(i, true);
            }
        }
        this.mLayout.setOrientation(1);
        Calendar c = Calendar.getInstance();
        LayoutInflater inflater = LayoutInflater.from(context);
        for (int i2 = 0; i2 < DAYS.length; i2++) {
            final int day = DAYS[i2];
            CheckBox checkBox = (CheckBox) inflater.inflate(R.layout.zen_schedule_rule_day, (ViewGroup) this, false);
            c.set(7, day);
            checkBox.setText(this.mDayFormat.format(c.getTime()));
            checkBox.setChecked(this.mDays.get(day));
            checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    ZenModeScheduleDaysSelection.this.mDays.put(day, isChecked);
                    ZenModeScheduleDaysSelection.this.onChanged(ZenModeScheduleDaysSelection.this.getDays());
                }
            });
            this.mLayout.addView(checkBox);
        }
    }

    public int[] getDays() {
        SparseBooleanArray rt = new SparseBooleanArray(this.mDays.size());
        for (int i = 0; i < this.mDays.size(); i++) {
            int day = this.mDays.keyAt(i);
            if (this.mDays.valueAt(i)) {
                rt.put(day, true);
            }
        }
        int[] rta = new int[rt.size()];
        for (int i2 = 0; i2 < rta.length; i2++) {
            rta[i2] = rt.keyAt(i2);
        }
        Arrays.sort(rta);
        return rta;
    }

    protected void onChanged(int[] days) {
    }
}
