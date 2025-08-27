package com.android.systemui.plugins.statusbar;

import android.content.Context;
import android.service.notification.StatusBarNotification;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import com.android.systemui.plugins.Plugin;
import com.android.systemui.plugins.annotations.Dependencies;
import com.android.systemui.plugins.annotations.DependsOn;
import com.android.systemui.plugins.annotations.ProvidesInterface;
import com.android.systemui.plugins.statusbar.NotificationSwipeActionHelper;
import java.util.ArrayList;

@Dependencies({@DependsOn(target = OnMenuEventListener.class), @DependsOn(target = MenuItem.class), @DependsOn(target = NotificationSwipeActionHelper.class), @DependsOn(target = NotificationSwipeActionHelper.SnoozeOption.class)})
@ProvidesInterface(action = NotificationMenuRowPlugin.ACTION, version = 4)
/* loaded from: classes.dex */
public interface NotificationMenuRowPlugin extends Plugin {
    public static final String ACTION = "com.android.systemui.action.PLUGIN_NOTIFICATION_MENU_ROW";
    public static final int VERSION = 4;

    @ProvidesInterface(version = 1)
    public interface MenuItem {
        public static final int VERSION = 1;

        String getContentDescription();

        View getGutsView();

        View getMenuView();
    }

    @ProvidesInterface(version = 1)
    public interface OnMenuEventListener {
        public static final int VERSION = 1;

        void onMenuClicked(View view, int i, int i2, MenuItem menuItem);

        void onMenuReset(View view);

        void onMenuShown(View view);
    }

    void createMenu(ViewGroup viewGroup, StatusBarNotification statusBarNotification);

    MenuItem getAppOpsMenuItem(Context context);

    MenuItem getLongpressMenuItem(Context context);

    ArrayList<MenuItem> getMenuItems(Context context);

    View getMenuView();

    MenuItem getSnoozeMenuItem(Context context);

    boolean isMenuVisible();

    void onHeightUpdate();

    void onNotificationUpdated(StatusBarNotification statusBarNotification);

    boolean onTouchEvent(View view, MotionEvent motionEvent, float f);

    void onTranslationUpdate(float f);

    void resetMenu();

    void setAppName(String str);

    void setMenuClickListener(OnMenuEventListener onMenuEventListener);

    void setMenuItems(ArrayList<MenuItem> arrayList);

    void setSwipeActionHelper(NotificationSwipeActionHelper notificationSwipeActionHelper);

    default boolean onInterceptTouchEvent(View view, MotionEvent motionEvent) {
        return false;
    }

    default boolean useDefaultMenuItems() {
        return false;
    }

    default void onConfigurationChanged() {
    }
}
