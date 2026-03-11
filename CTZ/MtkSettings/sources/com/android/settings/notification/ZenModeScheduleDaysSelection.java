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
    private final SimpleDateFormat mDayFormat;
    private final SparseBooleanArray mDays;
    private final LinearLayout mLayout;

    public ZenModeScheduleDaysSelection(Context context, int[] iArr) {
        super(context);
        this.mDayFormat = new SimpleDateFormat("EEEE");
        this.mDays = new SparseBooleanArray();
        this.mLayout = new LinearLayout(this.mContext);
        int dimensionPixelSize = context.getResources().getDimensionPixelSize(R.dimen.zen_schedule_day_margin);
        this.mLayout.setPadding(dimensionPixelSize, 0, dimensionPixelSize, 0);
        addView(this.mLayout);
        if (iArr != null) {
            for (int i : iArr) {
                this.mDays.put(i, true);
            }
        }
        this.mLayout.setOrientation(1);
        Calendar calendar = Calendar.getInstance();
        int[] daysOfWeekForLocale = getDaysOfWeekForLocale(calendar);
        LayoutInflater layoutInflaterFrom = LayoutInflater.from(context);
        for (final int i2 : daysOfWeekForLocale) {
            CheckBox checkBox = (CheckBox) layoutInflaterFrom.inflate(R.layout.zen_schedule_rule_day, (ViewGroup) this, false);
            calendar.set(7, i2);
            checkBox.setText(this.mDayFormat.format(calendar.getTime()));
            checkBox.setChecked(this.mDays.get(i2));
            checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean z) {
                    ZenModeScheduleDaysSelection.this.mDays.put(i2, z);
                    ZenModeScheduleDaysSelection.this.onChanged(ZenModeScheduleDaysSelection.this.getDays());
                }
            });
            this.mLayout.addView(checkBox);
        }
    }

    public int[] getDays() {
        SparseBooleanArray sparseBooleanArray = new SparseBooleanArray(this.mDays.size());
        for (int i = 0; i < this.mDays.size(); i++) {
            int iKeyAt = this.mDays.keyAt(i);
            if (this.mDays.valueAt(i)) {
                sparseBooleanArray.put(iKeyAt, true);
            }
        }
        int[] iArr = new int[sparseBooleanArray.size()];
        for (int i2 = 0; i2 < iArr.length; i2++) {
            iArr[i2] = sparseBooleanArray.keyAt(i2);
        }
        Arrays.sort(iArr);
        return iArr;
    }

    protected static int[] getDaysOfWeekForLocale(Calendar calendar) {
        int[] iArr = new int[7];
        int firstDayOfWeek = calendar.getFirstDayOfWeek();
        for (int i = 0; i < iArr.length; i++) {
            if (firstDayOfWeek > 7) {
                firstDayOfWeek = 1;
            }
            iArr[i] = firstDayOfWeek;
            firstDayOfWeek++;
        }
        return iArr;
    }

    protected void onChanged(int[] iArr) {
    }
}
