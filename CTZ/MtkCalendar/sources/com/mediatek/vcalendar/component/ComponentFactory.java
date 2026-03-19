package com.mediatek.vcalendar.component;

import com.mediatek.vcalendar.utils.LogUtil;

public final class ComponentFactory {
    public static Component createComponent(String str, Component component) {
        StringBuilder sb = new StringBuilder();
        sb.append("createComponent(): name: ");
        sb.append(str);
        sb.append(", parent: ");
        sb.append(component != null ? component.mName : null);
        LogUtil.i("ComponentFactory", sb.toString());
        if ("VEVENT".equals(str)) {
            return new VEvent();
        }
        if ("VALARM".equals(str)) {
            return new VAlarm(component);
        }
        if ("VTIMEZONE".equals(str)) {
            return new VTimezone();
        }
        return new Component(str, component);
    }
}
