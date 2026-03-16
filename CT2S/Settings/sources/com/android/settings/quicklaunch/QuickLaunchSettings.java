package com.android.settings.quicklaunch;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import java.net.URISyntaxException;

public class QuickLaunchSettings extends SettingsPreferenceFragment implements DialogInterface.OnClickListener, AdapterView.OnItemLongClickListener {
    private static final String[] sProjection = {"shortcut", "title", "intent"};
    private SparseBooleanArray mBookmarkedShortcuts;
    private Cursor mBookmarksCursor;
    private BookmarksObserver mBookmarksObserver;
    private CharSequence mClearDialogBookmarkTitle;
    private char mClearDialogShortcut;
    private PreferenceGroup mShortcutGroup;
    private SparseArray<ShortcutPreference> mShortcutToPreference;
    private Handler mUiHandler = new Handler();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.quick_launch_settings);
        this.mShortcutGroup = (PreferenceGroup) findPreference("shortcut_category");
        this.mShortcutToPreference = new SparseArray<>();
        this.mBookmarksObserver = new BookmarksObserver(this.mUiHandler);
        initShortcutPreferences();
        this.mBookmarksCursor = getActivity().getContentResolver().query(Settings.Bookmarks.CONTENT_URI, sProjection, null, null, null);
    }

    @Override
    public void onResume() {
        super.onResume();
        this.mBookmarksCursor = getActivity().getContentResolver().query(Settings.Bookmarks.CONTENT_URI, sProjection, null, null, null);
        getContentResolver().registerContentObserver(Settings.Bookmarks.CONTENT_URI, true, this.mBookmarksObserver);
        refreshShortcuts();
    }

    @Override
    public void onPause() {
        super.onPause();
        getContentResolver().unregisterContentObserver(this.mBookmarksObserver);
    }

    @Override
    public void onStop() {
        super.onStop();
        this.mBookmarksCursor.close();
    }

    @Override
    public void onActivityCreated(Bundle state) {
        super.onActivityCreated(state);
        getListView().setOnItemLongClickListener(this);
        if (state != null) {
            this.mClearDialogBookmarkTitle = state.getString("CLEAR_DIALOG_BOOKMARK_TITLE");
            this.mClearDialogShortcut = (char) state.getInt("CLEAR_DIALOG_SHORTCUT", 0);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putCharSequence("CLEAR_DIALOG_BOOKMARK_TITLE", this.mClearDialogBookmarkTitle);
        outState.putInt("CLEAR_DIALOG_SHORTCUT", this.mClearDialogShortcut);
    }

    @Override
    public Dialog onCreateDialog(int id) {
        switch (id) {
            case 0:
                return new AlertDialog.Builder(getActivity()).setTitle(getString(R.string.quick_launch_clear_dialog_title)).setMessage(getString(R.string.quick_launch_clear_dialog_message, new Object[]{Character.valueOf(this.mClearDialogShortcut), this.mClearDialogBookmarkTitle})).setPositiveButton(R.string.quick_launch_clear_ok_button, this).setNegativeButton(R.string.quick_launch_clear_cancel_button, this).create();
            default:
                return super.onCreateDialog(id);
        }
    }

    private void showClearDialog(ShortcutPreference pref) {
        if (pref.hasBookmark()) {
            this.mClearDialogBookmarkTitle = pref.getTitle();
            this.mClearDialogShortcut = pref.getShortcut();
            showDialog(0);
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (this.mClearDialogShortcut > 0 && which == -1) {
            clearShortcut(this.mClearDialogShortcut);
        }
        this.mClearDialogBookmarkTitle = null;
        this.mClearDialogShortcut = (char) 0;
    }

    private void clearShortcut(char shortcut) {
        getContentResolver().delete(Settings.Bookmarks.CONTENT_URI, "shortcut=?", new String[]{String.valueOf((int) shortcut)});
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (!(preference instanceof ShortcutPreference)) {
            return false;
        }
        ShortcutPreference pref = (ShortcutPreference) preference;
        Intent intent = new Intent(getActivity(), (Class<?>) BookmarkPicker.class);
        intent.putExtra("com.android.settings.quicklaunch.SHORTCUT", pref.getShortcut());
        startActivityForResult(intent, 1);
        return true;
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        Preference pref = (Preference) getPreferenceScreen().getRootAdapter().getItem(position);
        if (!(pref instanceof ShortcutPreference)) {
            return false;
        }
        showClearDialog((ShortcutPreference) pref);
        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == -1) {
            if (requestCode == 1) {
                if (data == null) {
                    Log.w("QuickLaunchSettings", "Result from bookmark picker does not have an intent.");
                    return;
                } else {
                    char shortcut = data.getCharExtra("com.android.settings.quicklaunch.SHORTCUT", (char) 0);
                    updateShortcut(shortcut, data);
                    return;
                }
            }
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void updateShortcut(char shortcut, Intent intent) {
        Settings.Bookmarks.add(getContentResolver(), intent, "", "@quicklaunch", shortcut, 0);
    }

    private ShortcutPreference getOrCreatePreference(char shortcut) {
        ShortcutPreference pref = this.mShortcutToPreference.get(shortcut);
        if (pref == null) {
            Log.w("QuickLaunchSettings", "Unknown shortcut '" + shortcut + "', creating preference anyway");
            return createPreference(shortcut);
        }
        return pref;
    }

    private ShortcutPreference createPreference(char shortcut) {
        ShortcutPreference pref = new ShortcutPreference(getActivity(), shortcut);
        this.mShortcutGroup.addPreference(pref);
        this.mShortcutToPreference.put(shortcut, pref);
        return pref;
    }

    private void initShortcutPreferences() {
        SparseBooleanArray shortcutSeen = new SparseBooleanArray();
        KeyCharacterMap keyMap = KeyCharacterMap.load(-1);
        for (int keyCode = KeyEvent.getMaxKeyCode() - 1; keyCode >= 0; keyCode--) {
            char shortcut = Character.toLowerCase(keyMap.getDisplayLabel(keyCode));
            if (shortcut != 0 && !shortcutSeen.get(shortcut, false) && Character.isLetterOrDigit(shortcut)) {
                shortcutSeen.put(shortcut, true);
                createPreference(shortcut);
            }
        }
    }

    private synchronized void refreshShortcuts() {
        ShortcutPreference pref;
        Cursor c = this.mBookmarksCursor;
        if (c != null) {
            if (!c.requery()) {
                Log.e("QuickLaunchSettings", "Could not requery cursor when refreshing shortcuts.");
            } else {
                SparseBooleanArray noLongerBookmarkedShortcuts = this.mBookmarkedShortcuts;
                SparseBooleanArray newBookmarkedShortcuts = new SparseBooleanArray();
                while (c.moveToNext()) {
                    char shortcut = Character.toLowerCase((char) c.getInt(0));
                    if (shortcut != 0) {
                        ShortcutPreference pref2 = getOrCreatePreference(shortcut);
                        CharSequence title = Settings.Bookmarks.getTitle(getActivity(), c);
                        int intentColumn = c.getColumnIndex("intent");
                        String intentUri = c.getString(intentColumn);
                        PackageManager packageManager = getPackageManager();
                        try {
                            Intent intent = Intent.parseUri(intentUri, 0);
                            ResolveInfo info = packageManager.resolveActivity(intent, 0);
                            if (info != null) {
                                title = info.loadLabel(packageManager);
                            }
                        } catch (URISyntaxException e) {
                        }
                        pref2.setTitle(title);
                        pref2.setSummary(getString(R.string.quick_launch_shortcut, new Object[]{String.valueOf(shortcut)}));
                        pref2.setHasBookmark(true);
                        newBookmarkedShortcuts.put(shortcut, true);
                        if (noLongerBookmarkedShortcuts != null) {
                            noLongerBookmarkedShortcuts.put(shortcut, false);
                        }
                    }
                }
                if (noLongerBookmarkedShortcuts != null) {
                    for (int i = noLongerBookmarkedShortcuts.size() - 1; i >= 0; i--) {
                        if (noLongerBookmarkedShortcuts.valueAt(i) && (pref = this.mShortcutToPreference.get((char) noLongerBookmarkedShortcuts.keyAt(i))) != null) {
                            pref.setHasBookmark(false);
                        }
                    }
                }
                this.mBookmarkedShortcuts = newBookmarkedShortcuts;
                c.deactivate();
            }
        }
    }

    private class BookmarksObserver extends ContentObserver {
        public BookmarksObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            QuickLaunchSettings.this.refreshShortcuts();
        }
    }
}
