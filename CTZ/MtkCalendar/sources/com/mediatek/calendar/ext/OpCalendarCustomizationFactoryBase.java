package com.mediatek.calendar.ext;

import android.content.Context;
import com.mediatek.common.util.OperatorCustomizationFactoryLoader;
import java.util.ArrayList;
import java.util.List;

public class OpCalendarCustomizationFactoryBase {
    static OpCalendarCustomizationFactoryBase sFactory;
    private static final List<OperatorCustomizationFactoryLoader.OperatorFactoryInfo> sOpFactoryInfoList = new ArrayList();

    public ILunarExt makeLunarCalendar(Context context) {
        return new DefaultLunarExtension();
    }

    public IEditEventViewExt makeEditEventCalendar(Context context) {
        return new DefaultEditEventViewExt();
    }

    static {
        sOpFactoryInfoList.add(new OperatorCustomizationFactoryLoader.OperatorFactoryInfo("OP01Calendar.apk", "com.mediatek.op01.calendar.Op01CalendarCustomizationFactory", "com.mediatek.op01.calendar", "OP01"));
        sOpFactoryInfoList.add(new OperatorCustomizationFactoryLoader.OperatorFactoryInfo("OP02Calendar.apk", "com.mediatek.op02.calendar.Op02CalendarCustomizationFactory", "com.mediatek.op02.calendar", "OP02"));
        sFactory = null;
    }

    public static synchronized OpCalendarCustomizationFactoryBase getOpFactory(Context context) {
        if (sFactory == null) {
            sFactory = (OpCalendarCustomizationFactoryBase) OperatorCustomizationFactoryLoader.loadFactory(context, sOpFactoryInfoList);
            if (sFactory == null) {
                sFactory = new OpCalendarCustomizationFactoryBase();
            }
        }
        return sFactory;
    }
}
