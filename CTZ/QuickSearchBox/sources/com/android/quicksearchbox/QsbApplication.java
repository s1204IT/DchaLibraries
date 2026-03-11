package com.android.quicksearchbox;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.view.ContextThemeWrapper;
import com.android.quicksearchbox.google.GoogleSource;
import com.android.quicksearchbox.google.GoogleSuggestClient;
import com.android.quicksearchbox.google.SearchBaseUrlHelper;
import com.android.quicksearchbox.ui.DefaultSuggestionViewFactory;
import com.android.quicksearchbox.ui.SuggestionViewFactory;
import com.android.quicksearchbox.util.HttpHelper;
import com.android.quicksearchbox.util.JavaNetHttpHelper;
import com.android.quicksearchbox.util.NamedTaskExecutor;
import com.android.quicksearchbox.util.PerNameExecutor;
import com.android.quicksearchbox.util.PriorityThreadFactory;
import com.android.quicksearchbox.util.SingleThreadNamedTaskExecutor;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.concurrent.ThreadFactory;

public class QsbApplication {
    private Config mConfig;
    private final Context mContext;
    private GoogleSource mGoogleSource;
    private HttpHelper mHttpHelper;
    private NamedTaskExecutor mIconLoaderExecutor;
    private Logger mLogger;
    private ThreadFactory mQueryThreadFactory;
    private SearchBaseUrlHelper mSearchBaseUrlHelper;
    private SearchSettings mSettings;
    private NamedTaskExecutor mSourceTaskExecutor;
    private SuggestionFormatter mSuggestionFormatter;
    private SuggestionViewFactory mSuggestionViewFactory;
    private SuggestionsProvider mSuggestionsProvider;
    private TextAppearanceFactory mTextAppearanceFactory;
    private Handler mUiThreadHandler;
    private int mVersionCode;
    private VoiceSearch mVoiceSearch;

    public QsbApplication(Context context) {
        this.mContext = new ContextThemeWrapper(context, 2131558410);
    }

    public static QsbApplication get(Context context) {
        return ((QsbApplicationWrapper) context.getApplicationContext()).getApp();
    }

    protected void checkThread() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            return;
        }
        throw new IllegalStateException("Accessed Application object from thread " + Thread.currentThread().getName());
    }

    protected void close() {
        checkThread();
        if (this.mConfig != null) {
            this.mConfig.close();
            this.mConfig = null;
        }
        if (this.mSuggestionsProvider != null) {
            this.mSuggestionsProvider.close();
            this.mSuggestionsProvider = null;
        }
    }

    protected Config createConfig() {
        return new Config(getContext());
    }

    protected GoogleSource createGoogleSource() {
        return new GoogleSuggestClient(getContext(), getMainThreadHandler(), getIconLoaderExecutor(), getConfig());
    }

    protected HttpHelper createHttpHelper() {
        return new JavaNetHttpHelper(new JavaNetHttpHelper.PassThroughRewriter(), getConfig().getUserAgent());
    }

    protected NamedTaskExecutor createIconLoaderExecutor() {
        return new PerNameExecutor(SingleThreadNamedTaskExecutor.factory(new PriorityThreadFactory(10)));
    }

    protected Logger createLogger() {
        return new EventLogLogger(getContext(), getConfig());
    }

    protected ThreadFactory createQueryThreadFactory() {
        return new ThreadFactoryBuilder().setNameFormat("QSB #%d").setThreadFactory(new PriorityThreadFactory(getConfig().getQueryThreadPriority())).build();
    }

    protected SearchBaseUrlHelper createSearchBaseUrlHelper() {
        return new SearchBaseUrlHelper(getContext(), getHttpHelper(), getSettings(), ((SearchSettingsImpl) getSettings()).getSearchPreferences());
    }

    protected SearchSettings createSettings() {
        return new SearchSettingsImpl(getContext(), getConfig());
    }

    protected NamedTaskExecutor createSourceTaskExecutor() {
        return new PerNameExecutor(SingleThreadNamedTaskExecutor.factory(getQueryThreadFactory()));
    }

    protected SuggestionFormatter createSuggestionFormatter() {
        return new LevenshteinSuggestionFormatter(getTextAppearanceFactory());
    }

    protected SuggestionViewFactory createSuggestionViewFactory() {
        return new DefaultSuggestionViewFactory(getContext());
    }

    protected SuggestionsProvider createSuggestionsProvider() {
        return new SuggestionsProviderImpl(getConfig(), getSourceTaskExecutor(), getMainThreadHandler(), getLogger());
    }

    protected TextAppearanceFactory createTextAppearanceFactory() {
        return new TextAppearanceFactory(getContext());
    }

    protected VoiceSearch createVoiceSearch() {
        return new VoiceSearch(getContext());
    }

    public Config getConfig() {
        Config config;
        synchronized (this) {
            if (this.mConfig == null) {
                this.mConfig = createConfig();
            }
            config = this.mConfig;
        }
        return config;
    }

    protected Context getContext() {
        return this.mContext;
    }

    public GoogleSource getGoogleSource() {
        checkThread();
        if (this.mGoogleSource == null) {
            this.mGoogleSource = createGoogleSource();
        }
        return this.mGoogleSource;
    }

    public Help getHelp() {
        return new Help(getContext(), getConfig());
    }

    public HttpHelper getHttpHelper() {
        HttpHelper httpHelper;
        synchronized (this) {
            if (this.mHttpHelper == null) {
                this.mHttpHelper = createHttpHelper();
            }
            httpHelper = this.mHttpHelper;
        }
        return httpHelper;
    }

    public NamedTaskExecutor getIconLoaderExecutor() {
        NamedTaskExecutor namedTaskExecutor;
        synchronized (this) {
            if (this.mIconLoaderExecutor == null) {
                this.mIconLoaderExecutor = createIconLoaderExecutor();
            }
            namedTaskExecutor = this.mIconLoaderExecutor;
        }
        return namedTaskExecutor;
    }

    public Logger getLogger() {
        checkThread();
        if (this.mLogger == null) {
            this.mLogger = createLogger();
        }
        return this.mLogger;
    }

    public Handler getMainThreadHandler() {
        Handler handler;
        synchronized (this) {
            if (this.mUiThreadHandler == null) {
                this.mUiThreadHandler = new Handler(Looper.getMainLooper());
            }
            handler = this.mUiThreadHandler;
        }
        return handler;
    }

    protected ThreadFactory getQueryThreadFactory() {
        checkThread();
        if (this.mQueryThreadFactory == null) {
            this.mQueryThreadFactory = createQueryThreadFactory();
        }
        return this.mQueryThreadFactory;
    }

    public SearchBaseUrlHelper getSearchBaseUrlHelper() {
        SearchBaseUrlHelper searchBaseUrlHelper;
        synchronized (this) {
            if (this.mSearchBaseUrlHelper == null) {
                this.mSearchBaseUrlHelper = createSearchBaseUrlHelper();
            }
            searchBaseUrlHelper = this.mSearchBaseUrlHelper;
        }
        return searchBaseUrlHelper;
    }

    public SearchSettings getSettings() {
        SearchSettings searchSettings;
        synchronized (this) {
            if (this.mSettings == null) {
                this.mSettings = createSettings();
                this.mSettings.upgradeSettingsIfNeeded();
            }
            searchSettings = this.mSettings;
        }
        return searchSettings;
    }

    public NamedTaskExecutor getSourceTaskExecutor() {
        checkThread();
        if (this.mSourceTaskExecutor == null) {
            this.mSourceTaskExecutor = createSourceTaskExecutor();
        }
        return this.mSourceTaskExecutor;
    }

    public SuggestionFormatter getSuggestionFormatter() {
        if (this.mSuggestionFormatter == null) {
            this.mSuggestionFormatter = createSuggestionFormatter();
        }
        return this.mSuggestionFormatter;
    }

    public SuggestionViewFactory getSuggestionViewFactory() {
        checkThread();
        if (this.mSuggestionViewFactory == null) {
            this.mSuggestionViewFactory = createSuggestionViewFactory();
        }
        return this.mSuggestionViewFactory;
    }

    protected SuggestionsProvider getSuggestionsProvider() {
        checkThread();
        if (this.mSuggestionsProvider == null) {
            this.mSuggestionsProvider = createSuggestionsProvider();
        }
        return this.mSuggestionsProvider;
    }

    public TextAppearanceFactory getTextAppearanceFactory() {
        if (this.mTextAppearanceFactory == null) {
            this.mTextAppearanceFactory = createTextAppearanceFactory();
        }
        return this.mTextAppearanceFactory;
    }

    public int getVersionCode() {
        if (this.mVersionCode == 0) {
            try {
                this.mVersionCode = getContext().getPackageManager().getPackageInfo(getContext().getPackageName(), 0).versionCode;
            } catch (PackageManager.NameNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        return this.mVersionCode;
    }

    public VoiceSearch getVoiceSearch() {
        checkThread();
        if (this.mVoiceSearch == null) {
            this.mVoiceSearch = createVoiceSearch();
        }
        return this.mVoiceSearch;
    }

    public void onStartupComplete() {
    }
}
