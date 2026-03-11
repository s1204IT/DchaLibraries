package com.android.systemui.statusbar;

import android.R;
import android.app.AlertDialog;
import android.app.AppGlobals;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.hardware.input.InputManager;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;
import android.util.SparseArray;
import android.view.ContextThemeWrapper;
import android.view.InputDevice;
import android.view.KeyCharacterMap;
import android.view.KeyboardShortcutGroup;
import android.view.KeyboardShortcutInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.android.internal.app.AssistUtils;
import com.android.systemui.recents.Recents;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class KeyboardShortcuts {
    private static KeyboardShortcuts sInstance;
    private static boolean sIsShowing;
    private final Context mContext;
    private KeyCharacterMap mKeyCharacterMap;
    private Dialog mKeyboardShortcutsDialog;
    private static final String TAG = KeyboardShortcuts.class.getSimpleName();
    private static final Object sLock = new Object();
    private final SparseArray<String> mSpecialCharacterNames = new SparseArray<>();
    private final SparseArray<String> mModifierNames = new SparseArray<>();
    private final SparseArray<Drawable> mSpecialCharacterDrawables = new SparseArray<>();
    private final SparseArray<Drawable> mModifierDrawables = new SparseArray<>();
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final DialogInterface.OnClickListener mDialogCloseListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int id) {
            KeyboardShortcuts.this.dismissKeyboardShortcuts();
        }
    };
    private final Comparator<KeyboardShortcutInfo> mApplicationItemsComparator = new Comparator<KeyboardShortcutInfo>() {
        @Override
        public int compare(KeyboardShortcutInfo ksh1, KeyboardShortcutInfo ksh2) {
            boolean zIsEmpty;
            boolean zIsEmpty2;
            if (ksh1.getLabel() == null) {
                zIsEmpty = true;
            } else {
                zIsEmpty = ksh1.getLabel().toString().isEmpty();
            }
            if (ksh2.getLabel() == null) {
                zIsEmpty2 = true;
            } else {
                zIsEmpty2 = ksh2.getLabel().toString().isEmpty();
            }
            if (zIsEmpty && zIsEmpty2) {
                return 0;
            }
            if (zIsEmpty) {
                return 1;
            }
            if (zIsEmpty2) {
                return -1;
            }
            return ksh1.getLabel().toString().compareToIgnoreCase(ksh2.getLabel().toString());
        }
    };
    private final IPackageManager mPackageManager = AppGlobals.getPackageManager();

    private KeyboardShortcuts(Context context) {
        this.mContext = new ContextThemeWrapper(context, R.style.Theme.Material.Light);
        loadResources(context);
    }

    private static KeyboardShortcuts getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new KeyboardShortcuts(context);
        }
        return sInstance;
    }

    public static void show(Context context, int deviceId) {
        synchronized (sLock) {
            if (sInstance != null && !sInstance.mContext.equals(context)) {
                dismiss();
            }
            getInstance(context).showKeyboardShortcuts(deviceId);
            sIsShowing = true;
        }
    }

    public static void toggle(Context context, int deviceId) {
        synchronized (sLock) {
            if (sIsShowing) {
                dismiss();
            } else {
                show(context, deviceId);
            }
        }
    }

    public static void dismiss() {
        synchronized (sLock) {
            if (sInstance != null) {
                sInstance.dismissKeyboardShortcuts();
                sInstance = null;
            }
            sIsShowing = false;
        }
    }

    private void loadResources(Context context) {
        this.mSpecialCharacterNames.put(3, context.getString(com.android.systemui.R.string.keyboard_key_home));
        this.mSpecialCharacterNames.put(4, context.getString(com.android.systemui.R.string.keyboard_key_back));
        this.mSpecialCharacterNames.put(19, context.getString(com.android.systemui.R.string.keyboard_key_dpad_up));
        this.mSpecialCharacterNames.put(20, context.getString(com.android.systemui.R.string.keyboard_key_dpad_down));
        this.mSpecialCharacterNames.put(21, context.getString(com.android.systemui.R.string.keyboard_key_dpad_left));
        this.mSpecialCharacterNames.put(22, context.getString(com.android.systemui.R.string.keyboard_key_dpad_right));
        this.mSpecialCharacterNames.put(23, context.getString(com.android.systemui.R.string.keyboard_key_dpad_center));
        this.mSpecialCharacterNames.put(56, ".");
        this.mSpecialCharacterNames.put(61, context.getString(com.android.systemui.R.string.keyboard_key_tab));
        this.mSpecialCharacterNames.put(62, context.getString(com.android.systemui.R.string.keyboard_key_space));
        this.mSpecialCharacterNames.put(66, context.getString(com.android.systemui.R.string.keyboard_key_enter));
        this.mSpecialCharacterNames.put(67, context.getString(com.android.systemui.R.string.keyboard_key_backspace));
        this.mSpecialCharacterNames.put(85, context.getString(com.android.systemui.R.string.keyboard_key_media_play_pause));
        this.mSpecialCharacterNames.put(86, context.getString(com.android.systemui.R.string.keyboard_key_media_stop));
        this.mSpecialCharacterNames.put(87, context.getString(com.android.systemui.R.string.keyboard_key_media_next));
        this.mSpecialCharacterNames.put(88, context.getString(com.android.systemui.R.string.keyboard_key_media_previous));
        this.mSpecialCharacterNames.put(89, context.getString(com.android.systemui.R.string.keyboard_key_media_rewind));
        this.mSpecialCharacterNames.put(90, context.getString(com.android.systemui.R.string.keyboard_key_media_fast_forward));
        this.mSpecialCharacterNames.put(92, context.getString(com.android.systemui.R.string.keyboard_key_page_up));
        this.mSpecialCharacterNames.put(93, context.getString(com.android.systemui.R.string.keyboard_key_page_down));
        this.mSpecialCharacterNames.put(96, context.getString(com.android.systemui.R.string.keyboard_key_button_template, "A"));
        this.mSpecialCharacterNames.put(97, context.getString(com.android.systemui.R.string.keyboard_key_button_template, "B"));
        this.mSpecialCharacterNames.put(98, context.getString(com.android.systemui.R.string.keyboard_key_button_template, "C"));
        this.mSpecialCharacterNames.put(99, context.getString(com.android.systemui.R.string.keyboard_key_button_template, "X"));
        this.mSpecialCharacterNames.put(100, context.getString(com.android.systemui.R.string.keyboard_key_button_template, "Y"));
        this.mSpecialCharacterNames.put(101, context.getString(com.android.systemui.R.string.keyboard_key_button_template, "Z"));
        this.mSpecialCharacterNames.put(102, context.getString(com.android.systemui.R.string.keyboard_key_button_template, "L1"));
        this.mSpecialCharacterNames.put(103, context.getString(com.android.systemui.R.string.keyboard_key_button_template, "R1"));
        this.mSpecialCharacterNames.put(104, context.getString(com.android.systemui.R.string.keyboard_key_button_template, "L2"));
        this.mSpecialCharacterNames.put(105, context.getString(com.android.systemui.R.string.keyboard_key_button_template, "R2"));
        this.mSpecialCharacterNames.put(108, context.getString(com.android.systemui.R.string.keyboard_key_button_template, "Start"));
        this.mSpecialCharacterNames.put(109, context.getString(com.android.systemui.R.string.keyboard_key_button_template, "Select"));
        this.mSpecialCharacterNames.put(110, context.getString(com.android.systemui.R.string.keyboard_key_button_template, "Mode"));
        this.mSpecialCharacterNames.put(112, context.getString(com.android.systemui.R.string.keyboard_key_forward_del));
        this.mSpecialCharacterNames.put(111, "Esc");
        this.mSpecialCharacterNames.put(120, "SysRq");
        this.mSpecialCharacterNames.put(121, "Break");
        this.mSpecialCharacterNames.put(116, "Scroll Lock");
        this.mSpecialCharacterNames.put(122, context.getString(com.android.systemui.R.string.keyboard_key_move_home));
        this.mSpecialCharacterNames.put(123, context.getString(com.android.systemui.R.string.keyboard_key_move_end));
        this.mSpecialCharacterNames.put(124, context.getString(com.android.systemui.R.string.keyboard_key_insert));
        this.mSpecialCharacterNames.put(131, "F1");
        this.mSpecialCharacterNames.put(132, "F2");
        this.mSpecialCharacterNames.put(133, "F3");
        this.mSpecialCharacterNames.put(134, "F4");
        this.mSpecialCharacterNames.put(135, "F5");
        this.mSpecialCharacterNames.put(136, "F6");
        this.mSpecialCharacterNames.put(137, "F7");
        this.mSpecialCharacterNames.put(138, "F8");
        this.mSpecialCharacterNames.put(139, "F9");
        this.mSpecialCharacterNames.put(140, "F10");
        this.mSpecialCharacterNames.put(141, "F11");
        this.mSpecialCharacterNames.put(142, "F12");
        this.mSpecialCharacterNames.put(143, context.getString(com.android.systemui.R.string.keyboard_key_num_lock));
        this.mSpecialCharacterNames.put(144, context.getString(com.android.systemui.R.string.keyboard_key_numpad_template, "0"));
        this.mSpecialCharacterNames.put(145, context.getString(com.android.systemui.R.string.keyboard_key_numpad_template, "1"));
        this.mSpecialCharacterNames.put(146, context.getString(com.android.systemui.R.string.keyboard_key_numpad_template, "2"));
        this.mSpecialCharacterNames.put(147, context.getString(com.android.systemui.R.string.keyboard_key_numpad_template, "3"));
        this.mSpecialCharacterNames.put(148, context.getString(com.android.systemui.R.string.keyboard_key_numpad_template, "4"));
        this.mSpecialCharacterNames.put(149, context.getString(com.android.systemui.R.string.keyboard_key_numpad_template, "5"));
        this.mSpecialCharacterNames.put(150, context.getString(com.android.systemui.R.string.keyboard_key_numpad_template, "6"));
        this.mSpecialCharacterNames.put(151, context.getString(com.android.systemui.R.string.keyboard_key_numpad_template, "7"));
        this.mSpecialCharacterNames.put(152, context.getString(com.android.systemui.R.string.keyboard_key_numpad_template, "8"));
        this.mSpecialCharacterNames.put(153, context.getString(com.android.systemui.R.string.keyboard_key_numpad_template, "9"));
        this.mSpecialCharacterNames.put(154, context.getString(com.android.systemui.R.string.keyboard_key_numpad_template, "/"));
        this.mSpecialCharacterNames.put(155, context.getString(com.android.systemui.R.string.keyboard_key_numpad_template, "*"));
        this.mSpecialCharacterNames.put(156, context.getString(com.android.systemui.R.string.keyboard_key_numpad_template, "-"));
        this.mSpecialCharacterNames.put(157, context.getString(com.android.systemui.R.string.keyboard_key_numpad_template, "+"));
        this.mSpecialCharacterNames.put(158, context.getString(com.android.systemui.R.string.keyboard_key_numpad_template, "."));
        this.mSpecialCharacterNames.put(159, context.getString(com.android.systemui.R.string.keyboard_key_numpad_template, ","));
        this.mSpecialCharacterNames.put(160, context.getString(com.android.systemui.R.string.keyboard_key_numpad_template, context.getString(com.android.systemui.R.string.keyboard_key_enter)));
        this.mSpecialCharacterNames.put(161, context.getString(com.android.systemui.R.string.keyboard_key_numpad_template, "="));
        this.mSpecialCharacterNames.put(162, context.getString(com.android.systemui.R.string.keyboard_key_numpad_template, "("));
        this.mSpecialCharacterNames.put(163, context.getString(com.android.systemui.R.string.keyboard_key_numpad_template, ")"));
        this.mSpecialCharacterNames.put(211, "半角/全角");
        this.mSpecialCharacterNames.put(212, "英数");
        this.mSpecialCharacterNames.put(213, "無変換");
        this.mSpecialCharacterNames.put(214, "変換");
        this.mSpecialCharacterNames.put(215, "かな");
        this.mModifierNames.put(65536, "Meta");
        this.mModifierNames.put(4096, "Ctrl");
        this.mModifierNames.put(2, "Alt");
        this.mModifierNames.put(1, "Shift");
        this.mModifierNames.put(4, "Sym");
        this.mModifierNames.put(8, "Fn");
        this.mSpecialCharacterDrawables.put(67, context.getDrawable(com.android.systemui.R.drawable.ic_ksh_key_backspace));
        this.mSpecialCharacterDrawables.put(66, context.getDrawable(com.android.systemui.R.drawable.ic_ksh_key_enter));
        this.mSpecialCharacterDrawables.put(19, context.getDrawable(com.android.systemui.R.drawable.ic_ksh_key_up));
        this.mSpecialCharacterDrawables.put(22, context.getDrawable(com.android.systemui.R.drawable.ic_ksh_key_right));
        this.mSpecialCharacterDrawables.put(20, context.getDrawable(com.android.systemui.R.drawable.ic_ksh_key_down));
        this.mSpecialCharacterDrawables.put(21, context.getDrawable(com.android.systemui.R.drawable.ic_ksh_key_left));
        this.mModifierDrawables.put(65536, context.getDrawable(com.android.systemui.R.drawable.ic_ksh_key_meta));
    }

    private void retrieveKeyCharacterMap(int deviceId) {
        InputDevice inputDevice;
        InputManager inputManager = InputManager.getInstance();
        if (deviceId != -1 && (inputDevice = inputManager.getInputDevice(deviceId)) != null) {
            this.mKeyCharacterMap = inputDevice.getKeyCharacterMap();
            return;
        }
        int[] deviceIds = inputManager.getInputDeviceIds();
        for (int i : deviceIds) {
            InputDevice inputDevice2 = inputManager.getInputDevice(i);
            if (inputDevice2.getId() != -1 && inputDevice2.isFullKeyboard()) {
                this.mKeyCharacterMap = inputDevice2.getKeyCharacterMap();
                return;
            }
        }
        this.mKeyCharacterMap = inputManager.getInputDevice(-1).getKeyCharacterMap();
    }

    private void showKeyboardShortcuts(int deviceId) {
        retrieveKeyCharacterMap(deviceId);
        Recents.getSystemServices().requestKeyboardShortcuts(this.mContext, new WindowManager.KeyboardShortcutsReceiver() {
            public void onKeyboardShortcutsReceived(List<KeyboardShortcutGroup> result) {
                result.add(KeyboardShortcuts.this.getSystemShortcuts());
                KeyboardShortcutGroup appShortcuts = KeyboardShortcuts.this.getDefaultApplicationShortcuts();
                if (appShortcuts != null) {
                    result.add(appShortcuts);
                }
                KeyboardShortcuts.this.showKeyboardShortcutsDialog(result);
            }
        }, deviceId);
    }

    public void dismissKeyboardShortcuts() {
        if (this.mKeyboardShortcutsDialog == null) {
            return;
        }
        this.mKeyboardShortcutsDialog.dismiss();
        this.mKeyboardShortcutsDialog = null;
    }

    public KeyboardShortcutGroup getSystemShortcuts() {
        KeyboardShortcutGroup systemGroup = new KeyboardShortcutGroup((CharSequence) this.mContext.getString(com.android.systemui.R.string.keyboard_shortcut_group_system), true);
        systemGroup.addItem(new KeyboardShortcutInfo(this.mContext.getString(com.android.systemui.R.string.keyboard_shortcut_group_system_home), 66, 65536));
        systemGroup.addItem(new KeyboardShortcutInfo(this.mContext.getString(com.android.systemui.R.string.keyboard_shortcut_group_system_back), 67, 65536));
        systemGroup.addItem(new KeyboardShortcutInfo(this.mContext.getString(com.android.systemui.R.string.keyboard_shortcut_group_system_recents), 61, 2));
        systemGroup.addItem(new KeyboardShortcutInfo(this.mContext.getString(com.android.systemui.R.string.keyboard_shortcut_group_system_notifications), 42, 65536));
        systemGroup.addItem(new KeyboardShortcutInfo(this.mContext.getString(com.android.systemui.R.string.keyboard_shortcut_group_system_shortcuts_helper), 76, 65536));
        systemGroup.addItem(new KeyboardShortcutInfo(this.mContext.getString(com.android.systemui.R.string.keyboard_shortcut_group_system_switch_input), 62, 65536));
        return systemGroup;
    }

    public KeyboardShortcutGroup getDefaultApplicationShortcuts() {
        int userId = this.mContext.getUserId();
        List<KeyboardShortcutInfo> keyboardShortcutInfoAppItems = new ArrayList<>();
        AssistUtils assistUtils = new AssistUtils(this.mContext);
        ComponentName assistComponent = assistUtils.getAssistComponentForUser(userId);
        PackageInfo assistPackageInfo = null;
        try {
            assistPackageInfo = this.mPackageManager.getPackageInfo(assistComponent.getPackageName(), 0, userId);
        } catch (RemoteException e) {
            Log.e(TAG, "PackageManagerService is dead");
        }
        if (assistPackageInfo != null) {
            Icon assistIcon = Icon.createWithResource(assistPackageInfo.applicationInfo.packageName, assistPackageInfo.applicationInfo.icon);
            keyboardShortcutInfoAppItems.add(new KeyboardShortcutInfo(this.mContext.getString(com.android.systemui.R.string.keyboard_shortcut_group_applications_assist), assistIcon, 0, 65536));
        }
        Icon browserIcon = getIconForIntentCategory("android.intent.category.APP_BROWSER", userId);
        if (browserIcon != null) {
            keyboardShortcutInfoAppItems.add(new KeyboardShortcutInfo(this.mContext.getString(com.android.systemui.R.string.keyboard_shortcut_group_applications_browser), browserIcon, 30, 65536));
        }
        Icon contactsIcon = getIconForIntentCategory("android.intent.category.APP_CONTACTS", userId);
        if (contactsIcon != null) {
            keyboardShortcutInfoAppItems.add(new KeyboardShortcutInfo(this.mContext.getString(com.android.systemui.R.string.keyboard_shortcut_group_applications_contacts), contactsIcon, 31, 65536));
        }
        Icon emailIcon = getIconForIntentCategory("android.intent.category.APP_EMAIL", userId);
        if (emailIcon != null) {
            keyboardShortcutInfoAppItems.add(new KeyboardShortcutInfo(this.mContext.getString(com.android.systemui.R.string.keyboard_shortcut_group_applications_email), emailIcon, 33, 65536));
        }
        Icon messagingIcon = getIconForIntentCategory("android.intent.category.APP_MESSAGING", userId);
        if (messagingIcon != null) {
            keyboardShortcutInfoAppItems.add(new KeyboardShortcutInfo(this.mContext.getString(com.android.systemui.R.string.keyboard_shortcut_group_applications_im), messagingIcon, 48, 65536));
        }
        Icon musicIcon = getIconForIntentCategory("android.intent.category.APP_MUSIC", userId);
        if (musicIcon != null) {
            keyboardShortcutInfoAppItems.add(new KeyboardShortcutInfo(this.mContext.getString(com.android.systemui.R.string.keyboard_shortcut_group_applications_music), musicIcon, 44, 65536));
        }
        Icon calendarIcon = getIconForIntentCategory("android.intent.category.APP_CALENDAR", userId);
        if (calendarIcon != null) {
            keyboardShortcutInfoAppItems.add(new KeyboardShortcutInfo(this.mContext.getString(com.android.systemui.R.string.keyboard_shortcut_group_applications_calendar), calendarIcon, 40, 65536));
        }
        int itemsSize = keyboardShortcutInfoAppItems.size();
        if (itemsSize == 0) {
            return null;
        }
        Collections.sort(keyboardShortcutInfoAppItems, this.mApplicationItemsComparator);
        return new KeyboardShortcutGroup(this.mContext.getString(com.android.systemui.R.string.keyboard_shortcut_group_applications), keyboardShortcutInfoAppItems, true);
    }

    private Icon getIconForIntentCategory(String intentCategory, int userId) {
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.addCategory(intentCategory);
        PackageInfo packageInfo = getPackageInfoForIntent(intent, userId);
        if (packageInfo == null || packageInfo.applicationInfo.icon == 0) {
            return null;
        }
        return Icon.createWithResource(packageInfo.applicationInfo.packageName, packageInfo.applicationInfo.icon);
    }

    private PackageInfo getPackageInfoForIntent(Intent intent, int userId) {
        try {
            ResolveInfo handler = this.mPackageManager.resolveIntent(intent, intent.resolveTypeIfNeeded(this.mContext.getContentResolver()), 0, userId);
            if (handler == null || handler.activityInfo == null) {
                return null;
            }
            return this.mPackageManager.getPackageInfo(handler.activityInfo.packageName, 0, userId);
        } catch (RemoteException e) {
            Log.e(TAG, "PackageManagerService is dead", e);
            return null;
        }
    }

    public void showKeyboardShortcutsDialog(final List<KeyboardShortcutGroup> keyboardShortcutGroups) {
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                KeyboardShortcuts.this.handleShowKeyboardShortcuts(keyboardShortcutGroups);
            }
        });
    }

    public void handleShowKeyboardShortcuts(List<KeyboardShortcutGroup> keyboardShortcutGroups) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this.mContext);
        LayoutInflater inflater = (LayoutInflater) this.mContext.getSystemService("layout_inflater");
        View keyboardShortcutsView = inflater.inflate(com.android.systemui.R.layout.keyboard_shortcuts_view, (ViewGroup) null);
        populateKeyboardShortcuts((LinearLayout) keyboardShortcutsView.findViewById(com.android.systemui.R.id.keyboard_shortcuts_container), keyboardShortcutGroups);
        dialogBuilder.setView(keyboardShortcutsView);
        dialogBuilder.setPositiveButton(com.android.systemui.R.string.quick_settings_done, this.mDialogCloseListener);
        this.mKeyboardShortcutsDialog = dialogBuilder.create();
        this.mKeyboardShortcutsDialog.setCanceledOnTouchOutside(true);
        Window keyboardShortcutsWindow = this.mKeyboardShortcutsDialog.getWindow();
        keyboardShortcutsWindow.setType(2008);
        this.mKeyboardShortcutsDialog.show();
    }

    private void populateKeyboardShortcuts(LinearLayout keyboardShortcutsLayout, List<KeyboardShortcutGroup> keyboardShortcutGroups) {
        int color;
        LayoutInflater inflater = LayoutInflater.from(this.mContext);
        int keyboardShortcutGroupsSize = keyboardShortcutGroups.size();
        TextView shortcutsKeyView = (TextView) inflater.inflate(com.android.systemui.R.layout.keyboard_shortcuts_key_view, (ViewGroup) null, false);
        shortcutsKeyView.measure(0, 0);
        int shortcutKeyTextItemMinWidth = shortcutsKeyView.getMeasuredHeight();
        int shortcutKeyIconItemHeightWidth = (shortcutsKeyView.getMeasuredHeight() - shortcutsKeyView.getPaddingTop()) - shortcutsKeyView.getPaddingBottom();
        for (int i = 0; i < keyboardShortcutGroupsSize; i++) {
            KeyboardShortcutGroup group = keyboardShortcutGroups.get(i);
            TextView categoryTitle = (TextView) inflater.inflate(com.android.systemui.R.layout.keyboard_shortcuts_category_title, (ViewGroup) keyboardShortcutsLayout, false);
            categoryTitle.setText(group.getLabel());
            if (group.isSystemGroup()) {
                color = this.mContext.getColor(com.android.systemui.R.color.ksh_system_group_color);
            } else {
                color = this.mContext.getColor(com.android.systemui.R.color.ksh_application_group_color);
            }
            categoryTitle.setTextColor(color);
            keyboardShortcutsLayout.addView(categoryTitle);
            LinearLayout shortcutContainer = (LinearLayout) inflater.inflate(com.android.systemui.R.layout.keyboard_shortcuts_container, (ViewGroup) keyboardShortcutsLayout, false);
            int itemsSize = group.getItems().size();
            for (int j = 0; j < itemsSize; j++) {
                KeyboardShortcutInfo info = group.getItems().get(j);
                List<StringOrDrawable> shortcutKeys = getHumanReadableShortcutKeys(info);
                if (shortcutKeys == null) {
                    Log.w(TAG, "Keyboard Shortcut contains unsupported keys, skipping.");
                } else {
                    View shortcutView = inflater.inflate(com.android.systemui.R.layout.keyboard_shortcut_app_item, (ViewGroup) shortcutContainer, false);
                    if (info.getIcon() != null) {
                        ImageView shortcutIcon = (ImageView) shortcutView.findViewById(com.android.systemui.R.id.keyboard_shortcuts_icon);
                        shortcutIcon.setImageIcon(info.getIcon());
                        shortcutIcon.setVisibility(0);
                    }
                    TextView shortcutKeyword = (TextView) shortcutView.findViewById(com.android.systemui.R.id.keyboard_shortcuts_keyword);
                    shortcutKeyword.setText(info.getLabel());
                    if (info.getIcon() != null) {
                        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) shortcutKeyword.getLayoutParams();
                        lp.removeRule(20);
                        shortcutKeyword.setLayoutParams(lp);
                    }
                    ViewGroup shortcutItemsContainer = (ViewGroup) shortcutView.findViewById(com.android.systemui.R.id.keyboard_shortcuts_item_container);
                    int shortcutKeysSize = shortcutKeys.size();
                    for (int k = 0; k < shortcutKeysSize; k++) {
                        StringOrDrawable shortcutRepresentation = shortcutKeys.get(k);
                        if (shortcutRepresentation.drawable != null) {
                            ImageView shortcutKeyIconView = (ImageView) inflater.inflate(com.android.systemui.R.layout.keyboard_shortcuts_key_icon_view, shortcutItemsContainer, false);
                            Bitmap bitmap = Bitmap.createBitmap(shortcutKeyIconItemHeightWidth, shortcutKeyIconItemHeightWidth, Bitmap.Config.ARGB_8888);
                            Canvas canvas = new Canvas(bitmap);
                            shortcutRepresentation.drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                            shortcutRepresentation.drawable.draw(canvas);
                            shortcutKeyIconView.setImageBitmap(bitmap);
                            shortcutItemsContainer.addView(shortcutKeyIconView);
                        } else if (shortcutRepresentation.string != null) {
                            TextView shortcutKeyTextView = (TextView) inflater.inflate(com.android.systemui.R.layout.keyboard_shortcuts_key_view, shortcutItemsContainer, false);
                            shortcutKeyTextView.setMinimumWidth(shortcutKeyTextItemMinWidth);
                            shortcutKeyTextView.setText(shortcutRepresentation.string);
                            shortcutItemsContainer.addView(shortcutKeyTextView);
                        }
                    }
                    shortcutContainer.addView(shortcutView);
                }
            }
            keyboardShortcutsLayout.addView(shortcutContainer);
            if (i < keyboardShortcutGroupsSize - 1) {
                View separator = inflater.inflate(com.android.systemui.R.layout.keyboard_shortcuts_category_separator, (ViewGroup) keyboardShortcutsLayout, false);
                keyboardShortcutsLayout.addView(separator);
            }
        }
    }

    private List<StringOrDrawable> getHumanReadableShortcutKeys(KeyboardShortcutInfo info) {
        List<StringOrDrawable> shortcutKeys = getHumanReadableModifiers(info);
        if (shortcutKeys == null) {
            return null;
        }
        String displayLabelString = null;
        Drawable displayLabelDrawable = null;
        if (info.getBaseCharacter() > 0) {
            displayLabelString = String.valueOf(info.getBaseCharacter());
        } else if (this.mSpecialCharacterDrawables.get(info.getKeycode()) != null) {
            displayLabelDrawable = this.mSpecialCharacterDrawables.get(info.getKeycode());
        } else if (this.mSpecialCharacterNames.get(info.getKeycode()) != null) {
            String displayLabelString2 = this.mSpecialCharacterNames.get(info.getKeycode());
            displayLabelString = displayLabelString2;
        } else {
            if (info.getKeycode() == 0) {
                return shortcutKeys;
            }
            char displayLabel = this.mKeyCharacterMap.getDisplayLabel(info.getKeycode());
            if (displayLabel == 0) {
                return null;
            }
            displayLabelString = String.valueOf(displayLabel);
        }
        if (displayLabelDrawable != null) {
            shortcutKeys.add(new StringOrDrawable(displayLabelDrawable));
        } else if (displayLabelString != null) {
            shortcutKeys.add(new StringOrDrawable(displayLabelString.toUpperCase()));
        }
        return shortcutKeys;
    }

    private List<StringOrDrawable> getHumanReadableModifiers(KeyboardShortcutInfo info) {
        List<StringOrDrawable> shortcutKeys = new ArrayList<>();
        int modifiers = info.getModifiers();
        if (modifiers == 0) {
            return shortcutKeys;
        }
        for (int i = 0; i < this.mModifierNames.size(); i++) {
            int supportedModifier = this.mModifierNames.keyAt(i);
            if ((modifiers & supportedModifier) != 0) {
                if (this.mModifierDrawables.get(supportedModifier) != null) {
                    shortcutKeys.add(new StringOrDrawable(this.mModifierDrawables.get(supportedModifier)));
                } else {
                    shortcutKeys.add(new StringOrDrawable(this.mModifierNames.get(supportedModifier).toUpperCase()));
                }
                modifiers &= ~supportedModifier;
            }
        }
        if (modifiers != 0) {
            return null;
        }
        return shortcutKeys;
    }

    private static final class StringOrDrawable {
        public Drawable drawable;
        public String string;

        public StringOrDrawable(String string) {
            this.string = string;
        }

        public StringOrDrawable(Drawable drawable) {
            this.drawable = drawable;
        }
    }
}
