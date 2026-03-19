package android.preference;

import android.R;
import android.app.Fragment;
import android.app.FragmentBreadCrumbs;
import android.app.FragmentTransaction;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.provider.Downloads;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.util.Xml;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import com.android.internal.util.XmlUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.xmlpull.v1.XmlPullParserException;

public abstract class PreferenceActivity extends ListActivity implements PreferenceManager.OnPreferenceTreeClickListener, PreferenceFragment.OnPreferenceStartFragmentCallback {
    private static final String BACK_STACK_PREFS = ":android:prefs";
    private static final String CUR_HEADER_TAG = ":android:cur_header";
    private static final boolean DBG = "eng".equals(Build.TYPE);
    public static final String EXTRA_NO_HEADERS = ":android:no_headers";
    private static final String EXTRA_PREFS_SET_BACK_TEXT = "extra_prefs_set_back_text";
    private static final String EXTRA_PREFS_SET_NEXT_TEXT = "extra_prefs_set_next_text";
    private static final String EXTRA_PREFS_SHOW_BUTTON_BAR = "extra_prefs_show_button_bar";
    private static final String EXTRA_PREFS_SHOW_SKIP = "extra_prefs_show_skip";
    public static final String EXTRA_SHOW_FRAGMENT = ":android:show_fragment";
    public static final String EXTRA_SHOW_FRAGMENT_ARGUMENTS = ":android:show_fragment_args";
    public static final String EXTRA_SHOW_FRAGMENT_SHORT_TITLE = ":android:show_fragment_short_title";
    public static final String EXTRA_SHOW_FRAGMENT_TITLE = ":android:show_fragment_title";
    private static final int FIRST_REQUEST_CODE = 100;
    private static final String HEADERS_TAG = ":android:headers";
    public static final long HEADER_ID_UNDEFINED = -1;
    private static final int MSG_BIND_PREFERENCES = 1;
    private static final int MSG_BUILD_HEADERS = 2;
    private static final String PREFERENCES_TAG = ":android:preferences";
    private static final String TAG = "PreferenceActivity";
    private Header mCurHeader;
    private FragmentBreadCrumbs mFragmentBreadCrumbs;
    private FrameLayout mListFooter;
    private Button mNextButton;
    private PreferenceManager mPreferenceManager;
    private ViewGroup mPrefsContainer;
    private Bundle mSavedInstanceState;
    private boolean mSinglePane;
    private final ArrayList<Header> mHeaders = new ArrayList<>();
    private int mPreferenceHeaderItemResId = 0;
    private boolean mPreferenceHeaderRemoveEmptyIcon = false;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Header mappedHeader;
            switch (msg.what) {
                case 1:
                    if (PreferenceActivity.DBG) {
                        Log.d(PreferenceActivity.TAG, "handleMessage, MSG_BIND_PREFERENCES");
                    }
                    PreferenceActivity.this.bindPreferences();
                    break;
                case 2:
                    if (PreferenceActivity.DBG) {
                        Log.d(PreferenceActivity.TAG, "handleMessage, MSG_BUILD_HEADERS");
                    }
                    ArrayList<Header> oldHeaders = new ArrayList<>(PreferenceActivity.this.mHeaders);
                    PreferenceActivity.this.mHeaders.clear();
                    PreferenceActivity.this.onBuildHeaders(PreferenceActivity.this.mHeaders);
                    if (PreferenceActivity.this.mAdapter instanceof BaseAdapter) {
                        ((BaseAdapter) PreferenceActivity.this.mAdapter).notifyDataSetChanged();
                    }
                    Header header = PreferenceActivity.this.onGetNewHeader();
                    if (header != null && header.fragment != null) {
                        Header mappedHeader2 = PreferenceActivity.this.findBestMatchingHeader(header, oldHeaders);
                        if (mappedHeader2 == null || PreferenceActivity.this.mCurHeader != mappedHeader2) {
                            PreferenceActivity.this.switchToHeader(header);
                        }
                        break;
                    } else if (PreferenceActivity.this.mCurHeader != null && (mappedHeader = PreferenceActivity.this.findBestMatchingHeader(PreferenceActivity.this.mCurHeader, PreferenceActivity.this.mHeaders)) != null) {
                        PreferenceActivity.this.setSelectedHeader(mappedHeader);
                        break;
                    }
                    break;
            }
        }
    };

    private static class HeaderAdapter extends ArrayAdapter<Header> {
        private LayoutInflater mInflater;
        private int mLayoutResId;
        private boolean mRemoveIconIfEmpty;

        private static class HeaderViewHolder {
            ImageView icon;
            TextView summary;
            TextView title;

            HeaderViewHolder(HeaderViewHolder headerViewHolder) {
                this();
            }

            private HeaderViewHolder() {
            }
        }

        public HeaderAdapter(Context context, List<Header> objects, int layoutResId, boolean removeIconBehavior) {
            super(context, 0, objects);
            this.mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            this.mLayoutResId = layoutResId;
            this.mRemoveIconIfEmpty = removeIconBehavior;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view;
            HeaderViewHolder holder;
            HeaderViewHolder headerViewHolder = null;
            if (convertView == null) {
                view = this.mInflater.inflate(this.mLayoutResId, parent, false);
                holder = new HeaderViewHolder(headerViewHolder);
                holder.icon = (ImageView) view.findViewById(R.id.icon);
                holder.title = (TextView) view.findViewById(R.id.title);
                holder.summary = (TextView) view.findViewById(R.id.summary);
                view.setTag(holder);
            } else {
                view = convertView;
                holder = (HeaderViewHolder) convertView.getTag();
            }
            Header header = getItem(position);
            if (this.mRemoveIconIfEmpty) {
                if (header.iconRes == 0) {
                    holder.icon.setVisibility(8);
                } else {
                    holder.icon.setVisibility(0);
                    holder.icon.setImageResource(header.iconRes);
                }
            } else {
                holder.icon.setImageResource(header.iconRes);
            }
            holder.title.setText(header.getTitle(getContext().getResources()));
            CharSequence summary = header.getSummary(getContext().getResources());
            if (!TextUtils.isEmpty(summary)) {
                holder.summary.setVisibility(0);
                holder.summary.setText(summary);
            } else {
                holder.summary.setVisibility(8);
            }
            return view;
        }
    }

    public static final class Header implements Parcelable {
        public static final Parcelable.Creator<Header> CREATOR = new Parcelable.Creator<Header>() {
            @Override
            public Header createFromParcel(Parcel source) {
                return new Header(source);
            }

            @Override
            public Header[] newArray(int size) {
                return new Header[size];
            }
        };
        public CharSequence breadCrumbShortTitle;
        public int breadCrumbShortTitleRes;
        public CharSequence breadCrumbTitle;
        public int breadCrumbTitleRes;
        public Bundle extras;
        public String fragment;
        public Bundle fragmentArguments;
        public int iconRes;
        public long id = -1;
        public Intent intent;
        public CharSequence summary;
        public int summaryRes;
        public CharSequence title;
        public int titleRes;

        public Header() {
        }

        public CharSequence getTitle(Resources res) {
            if (this.titleRes != 0) {
                return res.getText(this.titleRes);
            }
            return this.title;
        }

        public CharSequence getSummary(Resources res) {
            if (this.summaryRes != 0) {
                return res.getText(this.summaryRes);
            }
            return this.summary;
        }

        public CharSequence getBreadCrumbTitle(Resources res) {
            if (this.breadCrumbTitleRes != 0) {
                return res.getText(this.breadCrumbTitleRes);
            }
            return this.breadCrumbTitle;
        }

        public CharSequence getBreadCrumbShortTitle(Resources res) {
            if (this.breadCrumbShortTitleRes != 0) {
                return res.getText(this.breadCrumbShortTitleRes);
            }
            return this.breadCrumbShortTitle;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeLong(this.id);
            dest.writeInt(this.titleRes);
            TextUtils.writeToParcel(this.title, dest, flags);
            dest.writeInt(this.summaryRes);
            TextUtils.writeToParcel(this.summary, dest, flags);
            dest.writeInt(this.breadCrumbTitleRes);
            TextUtils.writeToParcel(this.breadCrumbTitle, dest, flags);
            dest.writeInt(this.breadCrumbShortTitleRes);
            TextUtils.writeToParcel(this.breadCrumbShortTitle, dest, flags);
            dest.writeInt(this.iconRes);
            dest.writeString(this.fragment);
            dest.writeBundle(this.fragmentArguments);
            if (this.intent != null) {
                dest.writeInt(1);
                this.intent.writeToParcel(dest, flags);
            } else {
                dest.writeInt(0);
            }
            dest.writeBundle(this.extras);
        }

        public void readFromParcel(Parcel in) {
            this.id = in.readLong();
            this.titleRes = in.readInt();
            this.title = (CharSequence) TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
            this.summaryRes = in.readInt();
            this.summary = (CharSequence) TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
            this.breadCrumbTitleRes = in.readInt();
            this.breadCrumbTitle = (CharSequence) TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
            this.breadCrumbShortTitleRes = in.readInt();
            this.breadCrumbShortTitle = (CharSequence) TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
            this.iconRes = in.readInt();
            this.fragment = in.readString();
            this.fragmentArguments = in.readBundle();
            if (in.readInt() != 0) {
                this.intent = Intent.CREATOR.createFromParcel(in);
            }
            this.extras = in.readBundle();
        }

        Header(Parcel in) {
            readFromParcel(in);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TypedArray sa = obtainStyledAttributes(null, com.android.internal.R.styleable.PreferenceActivity, 18219039, 0);
        int layoutResId = sa.getResourceId(0, 17367219);
        this.mPreferenceHeaderItemResId = sa.getResourceId(1, 17367213);
        this.mPreferenceHeaderRemoveEmptyIcon = sa.getBoolean(2, false);
        sa.recycle();
        setContentView(layoutResId);
        this.mListFooter = (FrameLayout) findViewById(16909263);
        this.mPrefsContainer = (ViewGroup) findViewById(16909264);
        boolean hidingHeaders = onIsHidingHeaders();
        this.mSinglePane = hidingHeaders || !onIsMultiPane();
        String initialFragment = getIntent().getStringExtra(EXTRA_SHOW_FRAGMENT);
        Bundle initialArguments = getIntent().getBundleExtra(EXTRA_SHOW_FRAGMENT_ARGUMENTS);
        int initialTitle = getIntent().getIntExtra(EXTRA_SHOW_FRAGMENT_TITLE, 0);
        int initialShortTitle = getIntent().getIntExtra(EXTRA_SHOW_FRAGMENT_SHORT_TITLE, 0);
        if (DBG) {
            Log.d(TAG, "onCreate, hidingHeaders = " + hidingHeaders + ", mSinglePane = " + this.mSinglePane);
        }
        if (savedInstanceState != null) {
            if (DBG) {
                Log.d(TAG, "    Restarts from a previous saved state.");
            }
            ArrayList<Header> headers = savedInstanceState.getParcelableArrayList(HEADERS_TAG);
            if (headers != null) {
                if (DBG) {
                    Log.d(TAG, "    Get previous headers from parcelable array list.");
                }
                this.mHeaders.addAll(headers);
                int curHeader = savedInstanceState.getInt(CUR_HEADER_TAG, -1);
                if (curHeader >= 0 && curHeader < this.mHeaders.size()) {
                    setSelectedHeader(this.mHeaders.get(curHeader));
                }
            }
        } else {
            if (DBG) {
                Log.d(TAG, "    Start a new activity.");
            }
            if (initialFragment != null && this.mSinglePane) {
                if (DBG) {
                    Log.d(TAG, "    Show a fragment from EXTRA_SHOW_FRAGMENT.");
                }
                switchToHeader(initialFragment, initialArguments);
                if (initialTitle != 0) {
                    CharSequence initialTitleStr = getText(initialTitle);
                    CharSequence initialShortTitleStr = initialShortTitle != 0 ? getText(initialShortTitle) : null;
                    showBreadCrumbs(initialTitleStr, initialShortTitleStr);
                }
            } else {
                onBuildHeaders(this.mHeaders);
                if (this.mHeaders.size() > 0) {
                    if (DBG) {
                        Log.d(TAG, "    Build headers successfully.");
                    }
                    if (!this.mSinglePane) {
                        if (initialFragment == null) {
                            Header h = onGetInitialHeader();
                            switchToHeader(h);
                        } else {
                            switchToHeader(initialFragment, initialArguments);
                        }
                    }
                }
            }
        }
        if (initialFragment != null && this.mSinglePane) {
            if (DBG) {
                Log.d(TAG, "    Single pane, showing just a prefs fragment.");
            }
            findViewById(16909262).setVisibility(8);
            this.mPrefsContainer.setVisibility(0);
            if (initialTitle != 0) {
                CharSequence initialTitleStr2 = getText(initialTitle);
                CharSequence initialShortTitleStr2 = initialShortTitle != 0 ? getText(initialShortTitle) : null;
                showBreadCrumbs(initialTitleStr2, initialShortTitleStr2);
            }
        } else if (this.mHeaders.size() > 0) {
            if (DBG) {
                Log.d(TAG, "    Set list adapter created from headers.");
            }
            setListAdapter(new HeaderAdapter(this, this.mHeaders, this.mPreferenceHeaderItemResId, this.mPreferenceHeaderRemoveEmptyIcon));
            if (!this.mSinglePane) {
                getListView().setChoiceMode(1);
                if (this.mCurHeader != null) {
                    setSelectedHeader(this.mCurHeader);
                }
                this.mPrefsContainer.setVisibility(0);
            }
        } else {
            if (DBG) {
                Log.d(TAG, "    In the old \"just show a screen of preferences\" mode.");
            }
            setContentView(17367221);
            this.mListFooter = (FrameLayout) findViewById(16909263);
            this.mPrefsContainer = (ViewGroup) findViewById(16909265);
            this.mPreferenceManager = new PreferenceManager(this, 100);
            this.mPreferenceManager.setOnPreferenceTreeClickListener(this);
        }
        Intent intent = getIntent();
        if (!intent.getBooleanExtra(EXTRA_PREFS_SHOW_BUTTON_BAR, false)) {
            return;
        }
        if (DBG) {
            Log.d(TAG, "    Initialize button bar.");
        }
        findViewById(16909266).setVisibility(0);
        Button backButton = (Button) findViewById(16909267);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PreferenceActivity.this.setResult(0);
                PreferenceActivity.this.finish();
            }
        });
        Button skipButton = (Button) findViewById(16909268);
        skipButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PreferenceActivity.this.setResult(-1);
                PreferenceActivity.this.finish();
            }
        });
        this.mNextButton = (Button) findViewById(16909269);
        this.mNextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PreferenceActivity.this.setResult(-1);
                PreferenceActivity.this.finish();
            }
        });
        if (intent.hasExtra(EXTRA_PREFS_SET_NEXT_TEXT)) {
            String buttonText = intent.getStringExtra(EXTRA_PREFS_SET_NEXT_TEXT);
            if (TextUtils.isEmpty(buttonText)) {
                this.mNextButton.setVisibility(8);
            } else {
                this.mNextButton.setText(buttonText);
            }
        }
        if (intent.hasExtra(EXTRA_PREFS_SET_BACK_TEXT)) {
            String buttonText2 = intent.getStringExtra(EXTRA_PREFS_SET_BACK_TEXT);
            if (TextUtils.isEmpty(buttonText2)) {
                backButton.setVisibility(8);
            } else {
                backButton.setText(buttonText2);
            }
        }
        if (!intent.getBooleanExtra(EXTRA_PREFS_SHOW_SKIP, false)) {
            return;
        }
        skipButton.setVisibility(0);
    }

    public boolean hasHeaders() {
        return getListView().getVisibility() == 0 && this.mPreferenceManager == null;
    }

    public List<Header> getHeaders() {
        return this.mHeaders;
    }

    public boolean isMultiPane() {
        return hasHeaders() && this.mPrefsContainer.getVisibility() == 0;
    }

    public boolean onIsMultiPane() {
        boolean preferMultiPane = getResources().getBoolean(17956869);
        return preferMultiPane;
    }

    public boolean onIsHidingHeaders() {
        return getIntent().getBooleanExtra(EXTRA_NO_HEADERS, false);
    }

    public Header onGetInitialHeader() {
        for (int i = 0; i < this.mHeaders.size(); i++) {
            Header h = this.mHeaders.get(i);
            if (h.fragment != null) {
                return h;
            }
        }
        throw new IllegalStateException("Must have at least one header with a fragment");
    }

    public Header onGetNewHeader() {
        return null;
    }

    public void onBuildHeaders(List<Header> target) {
    }

    public void invalidateHeaders() {
        if (this.mHandler.hasMessages(2)) {
            return;
        }
        this.mHandler.sendEmptyMessage(2);
    }

    public void loadHeadersFromResource(int resid, List<Header> target) {
        int type;
        if (DBG) {
            Log.d(TAG, "loadHeadersFromResource");
        }
        XmlResourceParser xmlResourceParser = null;
        try {
            try {
                XmlResourceParser parser = getResources().getXml(resid);
                AttributeSet attrs = Xml.asAttributeSet(parser);
                do {
                    type = parser.next();
                    if (type == 1) {
                        break;
                    }
                } while (type != 2);
                String nodeName = parser.getName();
                if (!"preference-headers".equals(nodeName)) {
                    throw new RuntimeException("XML document must start with <preference-headers> tag; found" + nodeName + " at " + parser.getPositionDescription());
                }
                Bundle curBundle = null;
                int outerDepth = parser.getDepth();
                while (true) {
                    int type2 = parser.next();
                    if (type2 == 1 || (type2 == 3 && parser.getDepth() <= outerDepth)) {
                        break;
                    }
                    if (type2 != 3 && type2 != 4) {
                        if (Downloads.Impl.RequestHeaders.COLUMN_HEADER.equals(parser.getName())) {
                            Header header = new Header();
                            TypedArray sa = obtainStyledAttributes(attrs, com.android.internal.R.styleable.PreferenceHeader);
                            header.id = sa.getResourceId(1, -1);
                            TypedValue tv = sa.peekValue(2);
                            if (tv != null && tv.type == 3) {
                                if (tv.resourceId != 0) {
                                    header.titleRes = tv.resourceId;
                                } else {
                                    header.title = tv.string;
                                }
                            }
                            TypedValue tv2 = sa.peekValue(3);
                            if (tv2 != null && tv2.type == 3) {
                                if (tv2.resourceId != 0) {
                                    header.summaryRes = tv2.resourceId;
                                } else {
                                    header.summary = tv2.string;
                                }
                            }
                            TypedValue tv3 = sa.peekValue(5);
                            if (tv3 != null && tv3.type == 3) {
                                if (tv3.resourceId != 0) {
                                    header.breadCrumbTitleRes = tv3.resourceId;
                                } else {
                                    header.breadCrumbTitle = tv3.string;
                                }
                            }
                            TypedValue tv4 = sa.peekValue(6);
                            if (tv4 != null && tv4.type == 3) {
                                if (tv4.resourceId != 0) {
                                    header.breadCrumbShortTitleRes = tv4.resourceId;
                                } else {
                                    header.breadCrumbShortTitle = tv4.string;
                                }
                            }
                            header.iconRes = sa.getResourceId(0, 0);
                            header.fragment = sa.getString(4);
                            sa.recycle();
                            if (curBundle == null) {
                                curBundle = new Bundle();
                            }
                            int innerDepth = parser.getDepth();
                            while (true) {
                                int type3 = parser.next();
                                if (type3 == 1 || (type3 == 3 && parser.getDepth() <= innerDepth)) {
                                    break;
                                }
                                if (type3 != 3 && type3 != 4) {
                                    String innerNodeName = parser.getName();
                                    if (innerNodeName.equals("extra")) {
                                        getResources().parseBundleExtra("extra", attrs, curBundle);
                                        XmlUtils.skipCurrentTag(parser);
                                    } else if (innerNodeName.equals("intent")) {
                                        header.intent = Intent.parseIntent(getResources(), parser, attrs);
                                    } else {
                                        XmlUtils.skipCurrentTag(parser);
                                    }
                                }
                            }
                            if (curBundle.size() > 0) {
                                header.fragmentArguments = curBundle;
                                curBundle = null;
                            }
                            target.add(header);
                        } else {
                            XmlUtils.skipCurrentTag(parser);
                        }
                    }
                }
                if (parser != null) {
                    parser.close();
                }
            } catch (IOException e) {
                throw new RuntimeException("Error parsing headers", e);
            } catch (XmlPullParserException e2) {
                throw new RuntimeException("Error parsing headers", e2);
            }
        } catch (Throwable th) {
            if (0 != 0) {
                xmlResourceParser.close();
            }
            throw th;
        }
    }

    protected boolean isValidFragment(String fragmentName) {
        if (getApplicationInfo().targetSdkVersion >= 19) {
            throw new RuntimeException("Subclasses of PreferenceActivity must override isValidFragment(String) to verify that the Fragment class is valid! " + getClass().getName() + " has not checked if fragment " + fragmentName + " is valid.");
        }
        return true;
    }

    public void setListFooter(View view) {
        this.mListFooter.removeAllViews();
        this.mListFooter.addView(view, new FrameLayout.LayoutParams(-1, -2));
    }

    @Override
    protected void onStop() {
        if (DBG) {
            Log.d(TAG, "onStop");
        }
        super.onStop();
        if (this.mPreferenceManager == null) {
            return;
        }
        this.mPreferenceManager.dispatchActivityStop();
    }

    @Override
    protected void onDestroy() {
        if (DBG) {
            Log.d(TAG, "onDestroy");
        }
        this.mHandler.removeMessages(1);
        this.mHandler.removeMessages(2);
        super.onDestroy();
        if (this.mPreferenceManager == null) {
            return;
        }
        this.mPreferenceManager.dispatchActivityDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        PreferenceScreen preferenceScreen;
        int index;
        super.onSaveInstanceState(outState);
        if (DBG) {
            Log.d(TAG, "onSaveInstanceState");
        }
        if (this.mHeaders.size() > 0) {
            outState.putParcelableArrayList(HEADERS_TAG, this.mHeaders);
            if (this.mCurHeader != null && (index = this.mHeaders.indexOf(this.mCurHeader)) >= 0) {
                outState.putInt(CUR_HEADER_TAG, index);
            }
        }
        if (this.mPreferenceManager == null || (preferenceScreen = getPreferenceScreen()) == null) {
            return;
        }
        Bundle container = new Bundle();
        preferenceScreen.saveHierarchyState(container);
        outState.putBundle(PREFERENCES_TAG, container);
    }

    @Override
    protected void onRestoreInstanceState(Bundle state) {
        Bundle container;
        PreferenceScreen preferenceScreen;
        if (DBG) {
            Log.d(TAG, "onRestoreInstanceState");
        }
        if (this.mPreferenceManager != null && (container = state.getBundle(PREFERENCES_TAG)) != null && (preferenceScreen = getPreferenceScreen()) != null) {
            preferenceScreen.restoreHierarchyState(container);
            this.mSavedInstanceState = state;
        } else {
            super.onRestoreInstanceState(state);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (this.mPreferenceManager == null) {
            return;
        }
        this.mPreferenceManager.dispatchActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onContentChanged() {
        super.onContentChanged();
        if (this.mPreferenceManager == null) {
            return;
        }
        postBindPreferences();
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        if (DBG) {
            Log.d(TAG, "onListItemClick, position = " + position + ", id = " + id);
        }
        if (!isResumed()) {
            return;
        }
        super.onListItemClick(l, v, position, id);
        if (this.mAdapter == null) {
            return;
        }
        Object item = this.mAdapter.getItem(position);
        if (item instanceof Header) {
            onHeaderClick((Header) item, position);
        }
    }

    public void onHeaderClick(Header header, int position) {
        if (header.fragment != null) {
            if (this.mSinglePane) {
                if (DBG) {
                    Log.d(TAG, "onHeaderClick, single pane and startWithFragment.");
                }
                int titleRes = header.breadCrumbTitleRes;
                int shortTitleRes = header.breadCrumbShortTitleRes;
                if (titleRes == 0) {
                    titleRes = header.titleRes;
                    shortTitleRes = 0;
                }
                startWithFragment(header.fragment, header.fragmentArguments, null, 0, titleRes, shortTitleRes);
                return;
            }
            if (DBG) {
                Log.d(TAG, "onHeaderClick, multiple pane and switchToHeader.");
            }
            switchToHeader(header);
            return;
        }
        if (header.intent == null) {
            return;
        }
        if (DBG) {
            Log.d(TAG, "onHeaderClick, start activity with header intent.");
        }
        startActivity(header.intent);
    }

    public Intent onBuildStartFragmentIntent(String fragmentName, Bundle args, int titleRes, int shortTitleRes) {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setClass(this, getClass());
        intent.putExtra(EXTRA_SHOW_FRAGMENT, fragmentName);
        intent.putExtra(EXTRA_SHOW_FRAGMENT_ARGUMENTS, args);
        intent.putExtra(EXTRA_SHOW_FRAGMENT_TITLE, titleRes);
        intent.putExtra(EXTRA_SHOW_FRAGMENT_SHORT_TITLE, shortTitleRes);
        intent.putExtra(EXTRA_NO_HEADERS, true);
        return intent;
    }

    public void startWithFragment(String fragmentName, Bundle args, Fragment resultTo, int resultRequestCode) {
        startWithFragment(fragmentName, args, resultTo, resultRequestCode, 0, 0);
    }

    public void startWithFragment(String fragmentName, Bundle args, Fragment resultTo, int resultRequestCode, int titleRes, int shortTitleRes) {
        Intent intent = onBuildStartFragmentIntent(fragmentName, args, titleRes, shortTitleRes);
        if (resultTo == null) {
            startActivity(intent);
        } else {
            resultTo.startActivityForResult(intent, resultRequestCode);
        }
    }

    public void showBreadCrumbs(CharSequence title, CharSequence shortTitle) {
        if (this.mFragmentBreadCrumbs == null) {
            View crumbs = findViewById(R.id.title);
            try {
                this.mFragmentBreadCrumbs = (FragmentBreadCrumbs) crumbs;
                if (this.mFragmentBreadCrumbs == null) {
                    if (title != null) {
                        setTitle(title);
                        return;
                    }
                    return;
                }
                if (this.mSinglePane) {
                    this.mFragmentBreadCrumbs.setVisibility(8);
                    View bcSection = findViewById(16909122);
                    if (bcSection != null) {
                        bcSection.setVisibility(8);
                    }
                    setTitle(title);
                }
                this.mFragmentBreadCrumbs.setMaxVisible(2);
                this.mFragmentBreadCrumbs.setActivity(this);
            } catch (ClassCastException e) {
                setTitle(title);
                return;
            }
        }
        if (this.mFragmentBreadCrumbs.getVisibility() != 0) {
            setTitle(title);
        } else {
            this.mFragmentBreadCrumbs.setTitle(title, shortTitle);
            this.mFragmentBreadCrumbs.setParentTitle(null, null, null);
        }
    }

    public void setParentTitle(CharSequence title, CharSequence shortTitle, View.OnClickListener listener) {
        if (this.mFragmentBreadCrumbs == null) {
            return;
        }
        this.mFragmentBreadCrumbs.setParentTitle(title, shortTitle, listener);
    }

    void setSelectedHeader(Header header) {
        this.mCurHeader = header;
        int index = this.mHeaders.indexOf(header);
        if (index >= 0) {
            getListView().setItemChecked(index, true);
        } else {
            getListView().clearChoices();
        }
        showBreadCrumbs(header);
    }

    void showBreadCrumbs(Header header) {
        if (header != null) {
            CharSequence title = header.getBreadCrumbTitle(getResources());
            if (title == null) {
                title = header.getTitle(getResources());
            }
            if (title == null) {
                title = getTitle();
            }
            showBreadCrumbs(title, header.getBreadCrumbShortTitle(getResources()));
            return;
        }
        showBreadCrumbs(getTitle(), null);
    }

    private void switchToHeaderInner(String fragmentName, Bundle args) {
        getFragmentManager().popBackStack(BACK_STACK_PREFS, 1);
        if (!isValidFragment(fragmentName)) {
            throw new IllegalArgumentException("Invalid fragment for this activity: " + fragmentName);
        }
        Fragment f = Fragment.instantiate(this, fragmentName, args);
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.setTransition(4099);
        transaction.replace(16909265, f);
        transaction.commitAllowingStateLoss();
    }

    public void switchToHeader(String fragmentName, Bundle args) {
        Header selectedHeader = null;
        int i = 0;
        while (true) {
            if (i >= this.mHeaders.size()) {
                break;
            }
            if (!fragmentName.equals(this.mHeaders.get(i).fragment)) {
                i++;
            } else {
                Header selectedHeader2 = this.mHeaders.get(i);
                selectedHeader = selectedHeader2;
                break;
            }
        }
        setSelectedHeader(selectedHeader);
        switchToHeaderInner(fragmentName, args);
    }

    public void switchToHeader(Header header) {
        if (this.mCurHeader == header) {
            getFragmentManager().popBackStack(BACK_STACK_PREFS, 1);
        } else {
            if (header.fragment == null) {
                throw new IllegalStateException("can't switch to header that has no fragment");
            }
            switchToHeaderInner(header.fragment, header.fragmentArguments);
            setSelectedHeader(header);
        }
    }

    Header findBestMatchingHeader(Header cur, ArrayList<Header> from) {
        ArrayList<Header> matches = new ArrayList<>();
        for (int j = 0; j < from.size(); j++) {
            Header oh = from.get(j);
            if (cur == oh || (cur.id != -1 && cur.id == oh.id)) {
                matches.clear();
                matches.add(oh);
                break;
            }
            if (cur.fragment != null) {
                if (cur.fragment.equals(oh.fragment)) {
                    matches.add(oh);
                }
            } else if (cur.intent != null) {
                if (cur.intent.equals(oh.intent)) {
                    matches.add(oh);
                }
            } else if (cur.title != null && cur.title.equals(oh.title)) {
                matches.add(oh);
            }
        }
        int NM = matches.size();
        if (NM == 1) {
            return matches.get(0);
        }
        if (NM > 1) {
            for (int j2 = 0; j2 < NM; j2++) {
                Header oh2 = matches.get(j2);
                if (cur.fragmentArguments != null && cur.fragmentArguments.equals(oh2.fragmentArguments)) {
                    return oh2;
                }
                if (cur.extras != null && cur.extras.equals(oh2.extras)) {
                    return oh2;
                }
                if (cur.title != null && cur.title.equals(oh2.title)) {
                    return oh2;
                }
            }
        }
        return null;
    }

    public void startPreferenceFragment(Fragment fragment, boolean push) {
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(16909265, fragment);
        if (push) {
            transaction.setTransition(4097);
            transaction.addToBackStack(BACK_STACK_PREFS);
        } else {
            transaction.setTransition(4099);
        }
        transaction.commitAllowingStateLoss();
    }

    public void startPreferencePanel(String fragmentClass, Bundle args, int titleRes, CharSequence titleText, Fragment resultTo, int resultRequestCode) {
        if (this.mSinglePane) {
            startWithFragment(fragmentClass, args, resultTo, resultRequestCode, titleRes, 0);
            return;
        }
        Fragment f = Fragment.instantiate(this, fragmentClass, args);
        if (resultTo != null) {
            f.setTargetFragment(resultTo, resultRequestCode);
        }
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(16909265, f);
        if (titleRes != 0) {
            transaction.setBreadCrumbTitle(titleRes);
        } else if (titleText != null) {
            transaction.setBreadCrumbTitle(titleText);
        }
        transaction.setTransition(4097);
        transaction.addToBackStack(BACK_STACK_PREFS);
        transaction.commitAllowingStateLoss();
    }

    public void finishPreferencePanel(Fragment caller, int resultCode, Intent resultData) {
        if (this.mSinglePane) {
            setResult(resultCode, resultData);
            finish();
            return;
        }
        onBackPressed();
        if (caller == null || caller.getTargetFragment() == null) {
            return;
        }
        caller.getTargetFragment().onActivityResult(caller.getTargetRequestCode(), resultCode, resultData);
    }

    @Override
    public boolean onPreferenceStartFragment(PreferenceFragment caller, Preference pref) {
        startPreferencePanel(pref.getFragment(), pref.getExtras(), pref.getTitleRes(), pref.getTitle(), null, 0);
        return true;
    }

    private void postBindPreferences() {
        if (this.mHandler.hasMessages(1)) {
            return;
        }
        this.mHandler.obtainMessage(1).sendToTarget();
    }

    private void bindPreferences() {
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        if (preferenceScreen == null) {
            return;
        }
        preferenceScreen.bind(getListView());
        if (this.mSavedInstanceState == null) {
            return;
        }
        super.onRestoreInstanceState(this.mSavedInstanceState);
        this.mSavedInstanceState = null;
    }

    @Deprecated
    public PreferenceManager getPreferenceManager() {
        return this.mPreferenceManager;
    }

    private void requirePreferenceManager() {
        if (this.mPreferenceManager != null) {
            return;
        }
        if (this.mAdapter == null) {
            throw new RuntimeException("This should be called after super.onCreate.");
        }
        throw new RuntimeException("Modern two-pane PreferenceActivity requires use of a PreferenceFragment");
    }

    @Deprecated
    public void setPreferenceScreen(PreferenceScreen preferenceScreen) {
        requirePreferenceManager();
        if (!this.mPreferenceManager.setPreferences(preferenceScreen) || preferenceScreen == null) {
            return;
        }
        postBindPreferences();
        CharSequence title = getPreferenceScreen().getTitle();
        if (title == null) {
            return;
        }
        setTitle(title);
    }

    @Deprecated
    public PreferenceScreen getPreferenceScreen() {
        if (this.mPreferenceManager != null) {
            return this.mPreferenceManager.getPreferenceScreen();
        }
        return null;
    }

    @Deprecated
    public void addPreferencesFromIntent(Intent intent) {
        if (DBG) {
            Log.d(TAG, "addPreferencesFromIntent, intent = " + intent);
        }
        requirePreferenceManager();
        setPreferenceScreen(this.mPreferenceManager.inflateFromIntent(intent, getPreferenceScreen()));
    }

    @Deprecated
    public void addPreferencesFromResource(int preferencesResId) {
        if (DBG) {
            Log.d(TAG, "addPreferencesFromResource, preferencesResId = " + preferencesResId);
        }
        requirePreferenceManager();
        setPreferenceScreen(this.mPreferenceManager.inflateFromResource(this, preferencesResId, getPreferenceScreen()));
    }

    @Override
    @Deprecated
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        return false;
    }

    @Deprecated
    public Preference findPreference(CharSequence key) {
        if (this.mPreferenceManager == null) {
            return null;
        }
        return this.mPreferenceManager.findPreference(key);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (this.mPreferenceManager == null) {
            return;
        }
        this.mPreferenceManager.dispatchNewIntent(intent);
    }

    protected boolean hasNextButton() {
        return this.mNextButton != null;
    }

    protected Button getNextButton() {
        return this.mNextButton;
    }
}
