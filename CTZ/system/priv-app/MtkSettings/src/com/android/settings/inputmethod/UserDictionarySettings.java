package com.android.settings.inputmethod;

import android.app.ActionBar;
import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.UserDictionary;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AlphabetIndexer;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SectionIndexer;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import com.android.settings.R;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.Instrumentable;
import com.android.settingslib.core.instrumentation.VisibilityLoggerMixin;

/* loaded from: classes.dex */
public class UserDictionarySettings extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor>, Instrumentable {
    private Cursor mCursor;
    private String mLocale;
    private VisibilityLoggerMixin mVisibilityLoggerMixin;

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 514;
    }

    @Override // android.app.Fragment
    public void onCreate(Bundle bundle) {
        String stringExtra;
        String string;
        super.onCreate(bundle);
        this.mVisibilityLoggerMixin = new VisibilityLoggerMixin(getMetricsCategory(), FeatureFactory.getFactory(getContext()).getMetricsFeatureProvider());
        Intent intent = getActivity().getIntent();
        if (intent != null) {
            stringExtra = intent.getStringExtra("locale");
        } else {
            stringExtra = null;
        }
        Bundle arguments = getArguments();
        if (arguments != null) {
            string = arguments.getString("locale");
        } else {
            string = null;
        }
        if (string == null) {
            if (stringExtra == null) {
                stringExtra = null;
            }
        } else {
            stringExtra = string;
        }
        this.mLocale = stringExtra;
        setHasOptionsMenu(true);
        getLoaderManager().initLoader(1, null, this);
    }

    @Override // android.app.ListFragment, android.app.Fragment
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        ActionBar actionBar = getActivity().getActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.user_dict_settings_title);
            actionBar.setSubtitle(UserDictionarySettingsUtils.getLocaleDisplayName(getActivity(), this.mLocale));
        }
        return layoutInflater.inflate(android.R.layout.notification_template_material_call, viewGroup, false);
    }

    @Override // android.app.ListFragment, android.app.Fragment
    public void onViewCreated(View view, Bundle bundle) {
        super.onViewCreated(view, bundle);
        TextView textView = (TextView) getView().findViewById(android.R.id.empty);
        textView.setText(R.string.user_dict_settings_empty_text);
        ListView listView = getListView();
        listView.setFastScrollEnabled(true);
        listView.setEmptyView(textView);
    }

    @Override // android.app.Fragment
    public void onResume() {
        super.onResume();
        this.mVisibilityLoggerMixin.onResume();
        getLoaderManager().restartLoader(1, null, this);
    }

    private ListAdapter createAdapter() {
        return new MyAdapter(getActivity(), R.layout.user_dictionary_item, this.mCursor, new String[]{"word", "shortcut"}, new int[]{android.R.id.text1, android.R.id.text2});
    }

    @Override // android.app.ListFragment
    public void onListItemClick(ListView listView, View view, int i, long j) {
        String word = getWord(i);
        String shortcut = getShortcut(i);
        if (word != null) {
            showAddOrEditDialog(word, shortcut);
        }
    }

    @Override // android.app.Fragment
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        menu.add(0, 1, 0, R.string.user_dict_settings_add_menu_title).setIcon(R.drawable.ic_menu_add_white).setShowAsAction(5);
    }

    @Override // android.app.Fragment
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == 1) {
            showAddOrEditDialog(null, null);
            return true;
        }
        return false;
    }

    @Override // android.app.Fragment
    public void onPause() {
        super.onPause();
        this.mVisibilityLoggerMixin.onPause();
    }

    private void showAddOrEditDialog(String str, String str2) {
        int i;
        Bundle bundle = new Bundle();
        if (str == null) {
            i = 1;
        } else {
            i = 0;
        }
        bundle.putInt("mode", i);
        bundle.putString("word", str);
        bundle.putString("shortcut", str2);
        bundle.putString("locale", this.mLocale);
        new SubSettingLauncher(getContext()).setDestination(UserDictionaryAddWordFragment.class.getName()).setArguments(bundle).setTitle(R.string.user_dict_settings_add_dialog_title).setSourceMetricsCategory(getMetricsCategory()).launch();
    }

    private String getWord(int i) {
        if (this.mCursor == null) {
            return null;
        }
        this.mCursor.moveToPosition(i);
        if (this.mCursor.isAfterLast()) {
            return null;
        }
        return this.mCursor.getString(this.mCursor.getColumnIndexOrThrow("word"));
    }

    private String getShortcut(int i) {
        if (this.mCursor == null) {
            return null;
        }
        this.mCursor.moveToPosition(i);
        if (this.mCursor.isAfterLast()) {
            return null;
        }
        return this.mCursor.getString(this.mCursor.getColumnIndexOrThrow("shortcut"));
    }

    public static void deleteWord(String str, String str2, ContentResolver contentResolver) {
        if (TextUtils.isEmpty(str2)) {
            contentResolver.delete(UserDictionary.Words.CONTENT_URI, "word=? AND shortcut is null OR shortcut=''", new String[]{str});
        } else {
            contentResolver.delete(UserDictionary.Words.CONTENT_URI, "word=? AND shortcut=?", new String[]{str, str2});
        }
    }

    @Override // android.app.LoaderManager.LoaderCallbacks
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new UserDictionaryCursorLoader(getContext(), this.mLocale);
    }

    /* JADX DEBUG: Method merged with bridge method: onLoadFinished(Landroid/content/Loader;Ljava/lang/Object;)V */
    @Override // android.app.LoaderManager.LoaderCallbacks
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        this.mCursor = cursor;
        getListView().setAdapter(createAdapter());
    }

    @Override // android.app.LoaderManager.LoaderCallbacks
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    private static class MyAdapter extends SimpleCursorAdapter implements SectionIndexer {
        private AlphabetIndexer mIndexer;
        private final SimpleCursorAdapter.ViewBinder mViewBinder;

        public MyAdapter(Context context, int i, Cursor cursor, String[] strArr, int[] iArr) {
            super(context, i, cursor, strArr, iArr);
            this.mViewBinder = new SimpleCursorAdapter.ViewBinder() { // from class: com.android.settings.inputmethod.UserDictionarySettings.MyAdapter.1
                @Override // android.widget.SimpleCursorAdapter.ViewBinder
                public boolean setViewValue(View view, Cursor cursor2, int i2) {
                    if (i2 != 2) {
                        return false;
                    }
                    String string = cursor2.getString(2);
                    if (TextUtils.isEmpty(string)) {
                        view.setVisibility(8);
                    } else {
                        ((TextView) view).setText(string);
                        view.setVisibility(0);
                    }
                    view.invalidate();
                    return true;
                }
            };
            if (cursor != null) {
                this.mIndexer = new AlphabetIndexer(cursor, cursor.getColumnIndexOrThrow("word"), context.getString(android.R.string.config_defaultAccessibilityService));
            }
            setViewBinder(this.mViewBinder);
        }

        @Override // android.widget.SectionIndexer
        public int getPositionForSection(int i) {
            if (this.mIndexer == null) {
                return 0;
            }
            return this.mIndexer.getPositionForSection(i);
        }

        @Override // android.widget.SectionIndexer
        public int getSectionForPosition(int i) {
            if (this.mIndexer == null) {
                return 0;
            }
            return this.mIndexer.getSectionForPosition(i);
        }

        @Override // android.widget.SectionIndexer
        public Object[] getSections() {
            if (this.mIndexer == null) {
                return null;
            }
            return this.mIndexer.getSections();
        }
    }
}
