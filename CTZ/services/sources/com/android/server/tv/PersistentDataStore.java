package com.android.server.tv;

import android.content.Context;
import android.content.Intent;
import android.media.tv.TvContentRating;
import android.os.Environment;
import android.os.Handler;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.AtomicFile;
import android.util.Slog;
import android.util.Xml;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.XmlUtils;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

final class PersistentDataStore {
    private static final String ATTR_ENABLED = "enabled";
    private static final String ATTR_STRING = "string";
    private static final String TAG = "TvInputManagerService";
    private static final String TAG_BLOCKED_RATINGS = "blocked-ratings";
    private static final String TAG_PARENTAL_CONTROLS = "parental-controls";
    private static final String TAG_RATING = "rating";
    private static final String TAG_TV_INPUT_MANAGER_STATE = "tv-input-manager-state";
    private final AtomicFile mAtomicFile;
    private boolean mBlockedRatingsChanged;
    private final Context mContext;
    private boolean mLoaded;
    private boolean mParentalControlsEnabled;
    private boolean mParentalControlsEnabledChanged;
    private final Handler mHandler = new Handler();
    private final List<TvContentRating> mBlockedRatings = Collections.synchronizedList(new ArrayList());
    private final Runnable mSaveRunnable = new Runnable() {
        @Override
        public void run() {
            PersistentDataStore.this.save();
        }
    };

    public PersistentDataStore(Context context, int i) {
        this.mContext = context;
        File userSystemDirectory = Environment.getUserSystemDirectory(i);
        if (!userSystemDirectory.exists() && !userSystemDirectory.mkdirs()) {
            throw new IllegalStateException("User dir cannot be created: " + userSystemDirectory);
        }
        this.mAtomicFile = new AtomicFile(new File(userSystemDirectory, "tv-input-manager-state.xml"), "tv-input-state");
    }

    public boolean isParentalControlsEnabled() {
        loadIfNeeded();
        return this.mParentalControlsEnabled;
    }

    public void setParentalControlsEnabled(boolean z) {
        loadIfNeeded();
        if (this.mParentalControlsEnabled != z) {
            this.mParentalControlsEnabled = z;
            this.mParentalControlsEnabledChanged = true;
            postSave();
        }
    }

    public boolean isRatingBlocked(TvContentRating tvContentRating) {
        loadIfNeeded();
        synchronized (this.mBlockedRatings) {
            Iterator<TvContentRating> it = this.mBlockedRatings.iterator();
            while (it.hasNext()) {
                if (tvContentRating.contains(it.next())) {
                    return true;
                }
            }
            return false;
        }
    }

    public TvContentRating[] getBlockedRatings() {
        loadIfNeeded();
        return (TvContentRating[]) this.mBlockedRatings.toArray(new TvContentRating[this.mBlockedRatings.size()]);
    }

    public void addBlockedRating(TvContentRating tvContentRating) {
        loadIfNeeded();
        if (tvContentRating != null && !this.mBlockedRatings.contains(tvContentRating)) {
            this.mBlockedRatings.add(tvContentRating);
            this.mBlockedRatingsChanged = true;
            postSave();
        }
    }

    public void removeBlockedRating(TvContentRating tvContentRating) {
        loadIfNeeded();
        if (tvContentRating != null && this.mBlockedRatings.contains(tvContentRating)) {
            this.mBlockedRatings.remove(tvContentRating);
            this.mBlockedRatingsChanged = true;
            postSave();
        }
    }

    private void loadIfNeeded() {
        if (!this.mLoaded) {
            load();
            this.mLoaded = true;
        }
    }

    private void clearState() {
        this.mBlockedRatings.clear();
        this.mParentalControlsEnabled = false;
    }

    private void load() {
        clearState();
        try {
            FileInputStream fileInputStreamOpenRead = this.mAtomicFile.openRead();
            try {
                try {
                    XmlPullParser xmlPullParserNewPullParser = Xml.newPullParser();
                    xmlPullParserNewPullParser.setInput(new BufferedInputStream(fileInputStreamOpenRead), StandardCharsets.UTF_8.name());
                    loadFromXml(xmlPullParserNewPullParser);
                } catch (IOException | XmlPullParserException e) {
                    Slog.w(TAG, "Failed to load tv input manager persistent store data.", e);
                    clearState();
                }
            } finally {
                IoUtils.closeQuietly(fileInputStreamOpenRead);
            }
        } catch (FileNotFoundException e2) {
        }
    }

    private void postSave() {
        this.mHandler.removeCallbacks(this.mSaveRunnable);
        this.mHandler.post(this.mSaveRunnable);
    }

    private void save() {
        try {
            FileOutputStream fileOutputStreamStartWrite = this.mAtomicFile.startWrite();
            try {
                FastXmlSerializer fastXmlSerializer = new FastXmlSerializer();
                fastXmlSerializer.setOutput(new BufferedOutputStream(fileOutputStreamStartWrite), StandardCharsets.UTF_8.name());
                saveToXml(fastXmlSerializer);
                fastXmlSerializer.flush();
                this.mAtomicFile.finishWrite(fileOutputStreamStartWrite);
                broadcastChangesIfNeeded();
            } catch (Throwable th) {
                this.mAtomicFile.failWrite(fileOutputStreamStartWrite);
                throw th;
            }
        } catch (IOException e) {
            Slog.w(TAG, "Failed to save tv input manager persistent store data.", e);
        }
    }

    private void broadcastChangesIfNeeded() {
        if (this.mParentalControlsEnabledChanged) {
            this.mParentalControlsEnabledChanged = false;
            this.mContext.sendBroadcastAsUser(new Intent("android.media.tv.action.PARENTAL_CONTROLS_ENABLED_CHANGED"), UserHandle.ALL);
        }
        if (this.mBlockedRatingsChanged) {
            this.mBlockedRatingsChanged = false;
            this.mContext.sendBroadcastAsUser(new Intent("android.media.tv.action.BLOCKED_RATINGS_CHANGED"), UserHandle.ALL);
        }
    }

    private void loadFromXml(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
        XmlUtils.beginDocument(xmlPullParser, TAG_TV_INPUT_MANAGER_STATE);
        int depth = xmlPullParser.getDepth();
        while (XmlUtils.nextElementWithin(xmlPullParser, depth)) {
            if (xmlPullParser.getName().equals(TAG_BLOCKED_RATINGS)) {
                loadBlockedRatingsFromXml(xmlPullParser);
            } else if (xmlPullParser.getName().equals(TAG_PARENTAL_CONTROLS)) {
                String attributeValue = xmlPullParser.getAttributeValue(null, ATTR_ENABLED);
                if (TextUtils.isEmpty(attributeValue)) {
                    throw new XmlPullParserException("Missing enabled attribute on parental-controls");
                }
                this.mParentalControlsEnabled = Boolean.parseBoolean(attributeValue);
            } else {
                continue;
            }
        }
    }

    private void loadBlockedRatingsFromXml(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
        int depth = xmlPullParser.getDepth();
        while (XmlUtils.nextElementWithin(xmlPullParser, depth)) {
            if (xmlPullParser.getName().equals(TAG_RATING)) {
                String attributeValue = xmlPullParser.getAttributeValue(null, ATTR_STRING);
                if (TextUtils.isEmpty(attributeValue)) {
                    throw new XmlPullParserException("Missing string attribute on rating");
                }
                this.mBlockedRatings.add(TvContentRating.unflattenFromString(attributeValue));
            }
        }
    }

    private void saveToXml(XmlSerializer xmlSerializer) throws IOException {
        xmlSerializer.startDocument(null, true);
        xmlSerializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
        xmlSerializer.startTag(null, TAG_TV_INPUT_MANAGER_STATE);
        xmlSerializer.startTag(null, TAG_BLOCKED_RATINGS);
        synchronized (this.mBlockedRatings) {
            for (TvContentRating tvContentRating : this.mBlockedRatings) {
                xmlSerializer.startTag(null, TAG_RATING);
                xmlSerializer.attribute(null, ATTR_STRING, tvContentRating.flattenToString());
                xmlSerializer.endTag(null, TAG_RATING);
            }
        }
        xmlSerializer.endTag(null, TAG_BLOCKED_RATINGS);
        xmlSerializer.startTag(null, TAG_PARENTAL_CONTROLS);
        xmlSerializer.attribute(null, ATTR_ENABLED, Boolean.toString(this.mParentalControlsEnabled));
        xmlSerializer.endTag(null, TAG_PARENTAL_CONTROLS);
        xmlSerializer.endTag(null, TAG_TV_INPUT_MANAGER_STATE);
        xmlSerializer.endDocument();
    }
}
