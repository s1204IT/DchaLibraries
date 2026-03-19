package android.ext.services.notification;

import android.app.INotificationManager;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.ext.services.R;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.storage.StorageManager;
import android.provider.Settings;
import android.service.notification.Adjustment;
import android.service.notification.NotificationAssistantService;
import android.service.notification.NotificationListenerService;
import android.service.notification.NotificationStats;
import android.service.notification.StatusBarNotification;
import android.util.ArrayMap;
import android.util.AtomicFile;
import android.util.Log;
import android.util.Slog;
import android.util.Xml;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.XmlUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class Assistant extends NotificationAssistantService {
    private static final boolean DEBUG = Log.isLoggable("ExtAssistant", 3);
    private static final ArrayList<Integer> PREJUDICAL_DISMISSALS = new ArrayList<>();
    private float mDismissToViewRatioLimit;
    private int mStreakLimit;
    final ArrayMap<String, ChannelImpressions> mkeyToImpressions = new ArrayMap<>();
    ArrayMap<String, String> mLiveNotifications = new ArrayMap<>();
    private NotificationListenerService.Ranking mFakeRanking = null;
    private AtomicFile mFile = null;

    static {
        PREJUDICAL_DISMISSALS.add(2);
        PREJUDICAL_DISMISSALS.add(10);
    }

    public void onCreate() {
        super.onCreate();
        new SettingsObserver(this.mHandler);
    }

    private void loadFile() {
        if (DEBUG) {
            Slog.d("ExtAssistant", "loadFile");
        }
        AsyncTask.execute(new Runnable() {
            @Override
            public final void run() throws Throwable {
                Assistant.lambda$loadFile$0(this.f$0);
            }
        });
    }

    public static void lambda$loadFile$0(Assistant assistant) throws Throwable {
        FileInputStream fileInputStreamOpenRead;
        FileInputStream fileInputStream = null;
        try {
            try {
                fileInputStreamOpenRead = assistant.mFile.openRead();
            } catch (Throwable th) {
                th = th;
            }
        } catch (FileNotFoundException e) {
        } catch (IOException e2) {
            e = e2;
        } catch (NumberFormatException | XmlPullParserException e3) {
            e = e3;
        }
        try {
            assistant.readXml(fileInputStreamOpenRead);
            IoUtils.closeQuietly(fileInputStreamOpenRead);
        } catch (FileNotFoundException e4) {
            fileInputStream = fileInputStreamOpenRead;
            Log.d("ExtAssistant", "File doesn't exist or isn't readable yet");
            IoUtils.closeQuietly(fileInputStream);
        } catch (IOException e5) {
            e = e5;
            fileInputStream = fileInputStreamOpenRead;
            Log.e("ExtAssistant", "Unable to read channel impressions", e);
            IoUtils.closeQuietly(fileInputStream);
        } catch (NumberFormatException | XmlPullParserException e6) {
            e = e6;
            fileInputStream = fileInputStreamOpenRead;
            Log.e("ExtAssistant", "Unable to parse channel impressions", e);
            IoUtils.closeQuietly(fileInputStream);
        } catch (Throwable th2) {
            th = th2;
            fileInputStream = fileInputStreamOpenRead;
            IoUtils.closeQuietly(fileInputStream);
            throw th;
        }
    }

    protected void readXml(InputStream inputStream) throws XmlPullParserException, NumberFormatException, IOException {
        XmlPullParser xmlPullParserNewPullParser = Xml.newPullParser();
        xmlPullParserNewPullParser.setInput(inputStream, StandardCharsets.UTF_8.name());
        int depth = xmlPullParserNewPullParser.getDepth();
        while (XmlUtils.nextElementWithin(xmlPullParserNewPullParser, depth)) {
            if ("assistant".equals(xmlPullParserNewPullParser.getName())) {
                int depth2 = xmlPullParserNewPullParser.getDepth();
                while (XmlUtils.nextElementWithin(xmlPullParserNewPullParser, depth2)) {
                    if ("impression-set".equals(xmlPullParserNewPullParser.getName())) {
                        String attributeValue = xmlPullParserNewPullParser.getAttributeValue(null, "key");
                        ChannelImpressions channelImpressionsCreateChannelImpressionsWithThresholds = createChannelImpressionsWithThresholds();
                        channelImpressionsCreateChannelImpressionsWithThresholds.populateFromXml(xmlPullParserNewPullParser);
                        synchronized (this.mkeyToImpressions) {
                            channelImpressionsCreateChannelImpressionsWithThresholds.append(this.mkeyToImpressions.get(attributeValue));
                            this.mkeyToImpressions.put(attributeValue, channelImpressionsCreateChannelImpressionsWithThresholds);
                        }
                    }
                }
            }
        }
    }

    private void saveFile() throws IOException {
        AsyncTask.execute(new Runnable() {
            @Override
            public final void run() {
                Assistant.lambda$saveFile$1(this.f$0);
            }
        });
    }

    public static void lambda$saveFile$1(Assistant assistant) {
        try {
            FileOutputStream fileOutputStreamStartWrite = assistant.mFile.startWrite();
            try {
                XmlSerializer fastXmlSerializer = new FastXmlSerializer();
                fastXmlSerializer.setOutput(fileOutputStreamStartWrite, StandardCharsets.UTF_8.name());
                assistant.writeXml(fastXmlSerializer);
                assistant.mFile.finishWrite(fileOutputStreamStartWrite);
            } catch (IOException e) {
                Slog.w("ExtAssistant", "Failed to save impressions file, restoring backup", e);
                assistant.mFile.failWrite(fileOutputStreamStartWrite);
            }
        } catch (IOException e2) {
            Slog.w("ExtAssistant", "Failed to save policy file", e2);
        }
    }

    protected void writeXml(XmlSerializer xmlSerializer) throws IOException {
        xmlSerializer.startDocument(null, true);
        xmlSerializer.startTag(null, "assistant");
        xmlSerializer.attribute(null, "version", Integer.toString(1));
        synchronized (this.mkeyToImpressions) {
            for (Map.Entry<String, ChannelImpressions> entry : this.mkeyToImpressions.entrySet()) {
                xmlSerializer.startTag(null, "impression-set");
                xmlSerializer.attribute(null, "key", entry.getKey());
                entry.getValue().writeXml(xmlSerializer);
                xmlSerializer.endTag(null, "impression-set");
            }
        }
        xmlSerializer.endTag(null, "assistant");
        xmlSerializer.endDocument();
    }

    public Adjustment onNotificationEnqueued(StatusBarNotification statusBarNotification) {
        if (DEBUG) {
            Log.i("ExtAssistant", "ENQUEUED " + statusBarNotification.getKey());
            return null;
        }
        return null;
    }

    public void onNotificationPosted(StatusBarNotification statusBarNotification, NotificationListenerService.RankingMap rankingMap) {
        if (DEBUG) {
            Log.i("ExtAssistant", "POSTED " + statusBarNotification.getKey());
        }
        try {
            NotificationListenerService.Ranking ranking = getRanking(statusBarNotification.getKey(), rankingMap);
            if (ranking != null && ranking.getChannel() != null) {
                String key = getKey(statusBarNotification.getPackageName(), statusBarNotification.getUserId(), ranking.getChannel().getId());
                ChannelImpressions orDefault = this.mkeyToImpressions.getOrDefault(key, createChannelImpressionsWithThresholds());
                if (ranking.getImportance() > 1 && orDefault.shouldTriggerBlock()) {
                    adjustNotification(createNegativeAdjustment(statusBarNotification.getPackageName(), statusBarNotification.getKey(), statusBarNotification.getUserId()));
                }
                this.mkeyToImpressions.put(key, orDefault);
                this.mLiveNotifications.put(statusBarNotification.getKey(), ranking.getChannel().getId());
            }
        } catch (Throwable th) {
            Log.e("ExtAssistant", "Error occurred processing post", th);
        }
    }

    public void onNotificationRemoved(StatusBarNotification statusBarNotification, NotificationListenerService.RankingMap rankingMap, NotificationStats notificationStats, int i) {
        boolean z;
        boolean z2 = false;
        try {
            String key = getKey(statusBarNotification.getPackageName(), statusBarNotification.getUserId(), this.mLiveNotifications.remove(statusBarNotification.getKey()));
            synchronized (this.mkeyToImpressions) {
                ChannelImpressions orDefault = this.mkeyToImpressions.getOrDefault(key, createChannelImpressionsWithThresholds());
                z = true;
                if (notificationStats.hasSeen()) {
                    orDefault.incrementViews();
                    z2 = true;
                }
                if (PREJUDICAL_DISMISSALS.contains(Integer.valueOf(i))) {
                    if ((!statusBarNotification.isAppGroup() || statusBarNotification.getNotification().isGroupChild()) && !notificationStats.hasInteracted() && notificationStats.getDismissalSurface() != 2 && notificationStats.getDismissalSurface() != 1 && notificationStats.getDismissalSurface() != 0) {
                        if (DEBUG) {
                            Log.i("ExtAssistant", "increment dismissals " + key);
                        }
                        orDefault.incrementDismissals();
                    } else {
                        if (DEBUG) {
                            Slog.i("ExtAssistant", "reset streak " + key);
                        }
                        if (orDefault.getStreak() <= 0) {
                            z = z2;
                        }
                        orDefault.resetStreak();
                    }
                } else {
                    z = z2;
                }
                this.mkeyToImpressions.put(key, orDefault);
            }
            if (z) {
                saveFile();
            }
        } catch (Throwable th) {
            Slog.e("ExtAssistant", "Error occurred processing removal", th);
        }
    }

    public void onNotificationSnoozedUntilContext(StatusBarNotification statusBarNotification, String str) {
    }

    public void onListenerConnected() {
        if (DEBUG) {
            Log.i("ExtAssistant", "CONNECTED");
        }
        try {
            this.mFile = new AtomicFile(new File(new File(Environment.getDataUserCePackageDirectory(StorageManager.UUID_PRIVATE_INTERNAL, getUserId(), getPackageName()), "assistant"), "blocking_helper_stats.xml"));
            loadFile();
            for (StatusBarNotification statusBarNotification : getActiveNotifications()) {
                onNotificationPosted(statusBarNotification);
            }
        } catch (Throwable th) {
            Log.e("ExtAssistant", "Error occurred on connection", th);
        }
    }

    protected String getKey(String str, int i, String str2) {
        return str + "|" + i + "|" + str2;
    }

    private NotificationListenerService.Ranking getRanking(String str, NotificationListenerService.RankingMap rankingMap) {
        if (this.mFakeRanking != null) {
            return this.mFakeRanking;
        }
        NotificationListenerService.Ranking ranking = new NotificationListenerService.Ranking();
        rankingMap.getRanking(str, ranking);
        return ranking;
    }

    private Adjustment createNegativeAdjustment(String str, String str2, int i) {
        if (DEBUG) {
            Log.d("ExtAssistant", "User probably doesn't want " + str2);
        }
        Bundle bundle = new Bundle();
        bundle.putInt("key_user_sentiment", -1);
        return new Adjustment(str, str2, bundle, getContext().getString(R.string.prompt_block_reason), i);
    }

    protected void setFile(AtomicFile atomicFile) {
        this.mFile = atomicFile;
    }

    protected void setFakeRanking(NotificationListenerService.Ranking ranking) {
        this.mFakeRanking = ranking;
    }

    protected void setNoMan(INotificationManager iNotificationManager) {
        this.mNoMan = iNotificationManager;
    }

    protected void setContext(Context context) {
        this.mSystemContext = context;
    }

    protected ChannelImpressions getImpressions(String str) {
        ChannelImpressions channelImpressions;
        synchronized (this.mkeyToImpressions) {
            channelImpressions = this.mkeyToImpressions.get(str);
        }
        return channelImpressions;
    }

    protected void insertImpressions(String str, ChannelImpressions channelImpressions) {
        synchronized (this.mkeyToImpressions) {
            this.mkeyToImpressions.put(str, channelImpressions);
        }
    }

    private ChannelImpressions createChannelImpressionsWithThresholds() {
        ChannelImpressions channelImpressions = new ChannelImpressions();
        channelImpressions.updateThresholds(this.mDismissToViewRatioLimit, this.mStreakLimit);
        return channelImpressions;
    }

    private final class SettingsObserver extends ContentObserver {
        private final Uri DISMISS_TO_VIEW_RATIO_LIMIT_URI;
        private final Uri STREAK_LIMIT_URI;

        public SettingsObserver(Handler handler) {
            super(handler);
            this.STREAK_LIMIT_URI = Settings.Global.getUriFor("blocking_helper_streak_limit");
            this.DISMISS_TO_VIEW_RATIO_LIMIT_URI = Settings.Global.getUriFor("blocking_helper_dismiss_to_view_ratio");
            ContentResolver contentResolver = Assistant.this.getApplicationContext().getContentResolver();
            contentResolver.registerContentObserver(this.DISMISS_TO_VIEW_RATIO_LIMIT_URI, false, this, Assistant.this.getUserId());
            contentResolver.registerContentObserver(this.STREAK_LIMIT_URI, false, this, Assistant.this.getUserId());
            update(null);
        }

        @Override
        public void onChange(boolean z, Uri uri) {
            update(uri);
        }

        private void update(Uri uri) {
            ContentResolver contentResolver = Assistant.this.getApplicationContext().getContentResolver();
            if (uri == null || this.DISMISS_TO_VIEW_RATIO_LIMIT_URI.equals(uri)) {
                Assistant.this.mDismissToViewRatioLimit = Settings.Global.getFloat(contentResolver, "blocking_helper_dismiss_to_view_ratio", 0.8f);
            }
            if (uri == null || this.STREAK_LIMIT_URI.equals(uri)) {
                Assistant.this.mStreakLimit = Settings.Global.getInt(contentResolver, "blocking_helper_streak_limit", 2);
            }
            synchronized (Assistant.this.mkeyToImpressions) {
                Iterator<ChannelImpressions> it = Assistant.this.mkeyToImpressions.values().iterator();
                while (it.hasNext()) {
                    it.next().updateThresholds(Assistant.this.mDismissToViewRatioLimit, Assistant.this.mStreakLimit);
                }
            }
        }
    }
}
