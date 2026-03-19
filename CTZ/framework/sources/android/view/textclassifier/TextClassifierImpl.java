package android.view.textclassifier;

import android.app.PendingIntent;
import android.app.RemoteAction;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.BenesseExtension;
import android.os.Bundle;
import android.os.DropBoxManager;
import android.os.LocaleList;
import android.os.ParcelFileDescriptor;
import android.os.UserManager;
import android.provider.Browser;
import android.provider.CalendarContract;
import android.provider.ContactsContract;
import android.service.notification.ZenModeConfig;
import android.view.textclassifier.TextClassification;
import android.view.textclassifier.TextClassifier;
import android.view.textclassifier.TextClassifierImplNative;
import android.view.textclassifier.TextLinks;
import android.view.textclassifier.TextSelection;
import com.android.internal.R;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.util.Preconditions;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public final class TextClassifierImpl implements TextClassifier {
    private static final String LOG_TAG = "androidtc";
    private static final String MODEL_DIR = "/etc/textclassifier/";
    private static final String MODEL_FILE_REGEX = "textclassifier\\.(.*)\\.model";
    private static final String UPDATED_MODEL_FILE_PATH = "/data/misc/textclassifier/textclassifier.model";

    @GuardedBy("mLock")
    private List<ModelFile> mAllModelFiles;
    private final Context mContext;
    private final TextClassifier mFallback;
    private final GenerateLinksLogger mGenerateLinksLogger;
    private final Object mLock;
    private final Object mLoggerLock;

    @GuardedBy("mLock")
    private ModelFile mModel;

    @GuardedBy("mLock")
    private TextClassifierImplNative mNative;

    @GuardedBy("mLoggerLock")
    private SelectionSessionLogger mSessionLogger;
    private final TextClassificationConstants mSettings;

    public TextClassifierImpl(Context context, TextClassificationConstants textClassificationConstants, TextClassifier textClassifier) {
        this.mLock = new Object();
        this.mLoggerLock = new Object();
        this.mContext = (Context) Preconditions.checkNotNull(context);
        this.mFallback = (TextClassifier) Preconditions.checkNotNull(textClassifier);
        this.mSettings = (TextClassificationConstants) Preconditions.checkNotNull(textClassificationConstants);
        this.mGenerateLinksLogger = new GenerateLinksLogger(this.mSettings.getGenerateLinksLogSampleRate());
    }

    public TextClassifierImpl(Context context, TextClassificationConstants textClassificationConstants) {
        this(context, textClassificationConstants, TextClassifier.NO_OP);
    }

    @Override
    public TextSelection suggestSelection(TextSelection.Request request) {
        int endIndex;
        int startIndex;
        Preconditions.checkNotNull(request);
        TextClassifier.Utils.checkMainThread();
        try {
            int endIndex2 = request.getEndIndex() - request.getStartIndex();
            String string = request.getText().toString();
            if (string.length() > 0 && endIndex2 <= this.mSettings.getSuggestSelectionMaxRangeLength()) {
                String strConcatenateLocales = concatenateLocales(request.getDefaultLocales());
                ZonedDateTime zonedDateTimeNow = ZonedDateTime.now();
                TextClassifierImplNative textClassifierImplNative = getNative(request.getDefaultLocales());
                if (this.mSettings.isModelDarkLaunchEnabled() && !request.isDarkLaunchAllowed()) {
                    startIndex = request.getStartIndex();
                    endIndex = request.getEndIndex();
                } else {
                    int[] iArrSuggestSelection = textClassifierImplNative.suggestSelection(string, request.getStartIndex(), request.getEndIndex(), new TextClassifierImplNative.SelectionOptions(strConcatenateLocales));
                    int i = iArrSuggestSelection[0];
                    endIndex = iArrSuggestSelection[1];
                    startIndex = i;
                }
                if (startIndex < endIndex && startIndex >= 0 && endIndex <= string.length() && startIndex <= request.getStartIndex() && endIndex >= request.getEndIndex()) {
                    TextSelection.Builder builder = new TextSelection.Builder(startIndex, endIndex);
                    TextClassifierImplNative.ClassificationResult[] classificationResultArrClassifyText = textClassifierImplNative.classifyText(string, startIndex, endIndex, new TextClassifierImplNative.ClassificationOptions(zonedDateTimeNow.toInstant().toEpochMilli(), zonedDateTimeNow.getZone().getId(), strConcatenateLocales));
                    int length = classificationResultArrClassifyText.length;
                    for (int i2 = 0; i2 < length; i2++) {
                        builder.setEntityType(classificationResultArrClassifyText[i2].getCollection(), classificationResultArrClassifyText[i2].getScore());
                    }
                    return builder.setId(createId(string, request.getStartIndex(), request.getEndIndex())).build();
                }
                Log.d("androidtc", "Got bad indices for input text. Ignoring result.");
            }
        } catch (Throwable th) {
            Log.e("androidtc", "Error suggesting selection for text. No changes to selection suggested.", th);
        }
        return this.mFallback.suggestSelection(request);
    }

    @Override
    public TextClassification classifyText(TextClassification.Request request) {
        Preconditions.checkNotNull(request);
        TextClassifier.Utils.checkMainThread();
        try {
            int endIndex = request.getEndIndex() - request.getStartIndex();
            String string = request.getText().toString();
            if (string.length() > 0 && endIndex <= this.mSettings.getClassifyTextMaxRangeLength()) {
                String strConcatenateLocales = concatenateLocales(request.getDefaultLocales());
                ZonedDateTime referenceTime = request.getReferenceTime() != null ? request.getReferenceTime() : ZonedDateTime.now();
                TextClassifierImplNative.ClassificationResult[] classificationResultArrClassifyText = getNative(request.getDefaultLocales()).classifyText(string, request.getStartIndex(), request.getEndIndex(), new TextClassifierImplNative.ClassificationOptions(referenceTime.toInstant().toEpochMilli(), referenceTime.getZone().getId(), strConcatenateLocales));
                if (classificationResultArrClassifyText.length > 0) {
                    return createClassificationResult(classificationResultArrClassifyText, string, request.getStartIndex(), request.getEndIndex(), referenceTime.toInstant());
                }
            }
        } catch (Throwable th) {
            Log.e("androidtc", "Error getting text classification info.", th);
        }
        return this.mFallback.classifyText(request);
    }

    @Override
    public TextLinks generateLinks(TextLinks.Request request) {
        Collection<String> entityListDefault;
        String callingPackageName;
        Preconditions.checkNotNull(request);
        TextClassifier.Utils.checkTextLength(request.getText(), getMaxGenerateLinksTextLength());
        TextClassifier.Utils.checkMainThread();
        if (!this.mSettings.isSmartLinkifyEnabled() && request.isLegacyFallback()) {
            return TextClassifier.Utils.generateLegacyLinks(request);
        }
        String string = request.getText().toString();
        TextLinks.Builder builder = new TextLinks.Builder(string);
        try {
            long jCurrentTimeMillis = System.currentTimeMillis();
            ZonedDateTime zonedDateTimeNow = ZonedDateTime.now();
            if (request.getEntityConfig() != null) {
                entityListDefault = request.getEntityConfig().resolveEntityListModifications(getEntitiesForHints(request.getEntityConfig().getHints()));
            } else {
                entityListDefault = this.mSettings.getEntityListDefault();
            }
            for (TextClassifierImplNative.AnnotatedSpan annotatedSpan : getNative(request.getDefaultLocales()).annotate(string, new TextClassifierImplNative.AnnotationOptions(zonedDateTimeNow.toInstant().toEpochMilli(), zonedDateTimeNow.getZone().getId(), concatenateLocales(request.getDefaultLocales())))) {
                TextClassifierImplNative.ClassificationResult[] classification = annotatedSpan.getClassification();
                if (classification.length != 0 && entityListDefault.contains(classification[0].getCollection())) {
                    HashMap map = new HashMap();
                    for (int i = 0; i < classification.length; i++) {
                        map.put(classification[i].getCollection(), Float.valueOf(classification[i].getScore()));
                    }
                    builder.addLink(annotatedSpan.getStartIndex(), annotatedSpan.getEndIndex(), map);
                }
            }
            TextLinks textLinksBuild = builder.build();
            long jCurrentTimeMillis2 = System.currentTimeMillis();
            if (request.getCallingPackageName() == null) {
                callingPackageName = this.mContext.getPackageName();
            } else {
                callingPackageName = request.getCallingPackageName();
            }
            this.mGenerateLinksLogger.logGenerateLinks(request.getText(), textLinksBuild, callingPackageName, jCurrentTimeMillis2 - jCurrentTimeMillis);
            return textLinksBuild;
        } catch (Throwable th) {
            Log.e("androidtc", "Error getting links info.", th);
            return this.mFallback.generateLinks(request);
        }
    }

    @Override
    public int getMaxGenerateLinksTextLength() {
        return this.mSettings.getGenerateLinksMaxTextLength();
    }

    private Collection<String> getEntitiesForHints(Collection<String> collection) {
        boolean zContains = collection.contains(TextClassifier.HINT_TEXT_IS_EDITABLE);
        if (zContains == collection.contains(TextClassifier.HINT_TEXT_IS_NOT_EDITABLE)) {
            return this.mSettings.getEntityListDefault();
        }
        if (zContains) {
            return this.mSettings.getEntityListEditable();
        }
        return this.mSettings.getEntityListNotEditable();
    }

    @Override
    public void onSelectionEvent(SelectionEvent selectionEvent) {
        Preconditions.checkNotNull(selectionEvent);
        synchronized (this.mLoggerLock) {
            if (this.mSessionLogger == null) {
                this.mSessionLogger = new SelectionSessionLogger();
            }
            this.mSessionLogger.writeEvent(selectionEvent);
        }
    }

    private TextClassifierImplNative getNative(LocaleList localeList) throws FileNotFoundException {
        TextClassifierImplNative textClassifierImplNative;
        synchronized (this.mLock) {
            if (localeList == null) {
                try {
                    localeList = LocaleList.getEmptyLocaleList();
                } catch (Throwable th) {
                    throw th;
                }
            }
            ModelFile modelFileFindBestModelLocked = findBestModelLocked(localeList);
            if (modelFileFindBestModelLocked == null) {
                throw new FileNotFoundException("No model for " + localeList.toLanguageTags());
            }
            if (this.mNative == null || !Objects.equals(this.mModel, modelFileFindBestModelLocked)) {
                Log.d("androidtc", "Loading " + modelFileFindBestModelLocked);
                destroyNativeIfExistsLocked();
                ParcelFileDescriptor parcelFileDescriptorOpen = ParcelFileDescriptor.open(new File(modelFileFindBestModelLocked.getPath()), 268435456);
                this.mNative = new TextClassifierImplNative(parcelFileDescriptorOpen.getFd());
                closeAndLogError(parcelFileDescriptorOpen);
                this.mModel = modelFileFindBestModelLocked;
            }
            textClassifierImplNative = this.mNative;
        }
        return textClassifierImplNative;
    }

    private String createId(String str, int i, int i2) {
        String strCreateId;
        synchronized (this.mLock) {
            strCreateId = SelectionSessionLogger.createId(str, i, i2, this.mContext, this.mModel.getVersion(), this.mModel.getSupportedLocales());
        }
        return strCreateId;
    }

    @GuardedBy("mLock")
    private void destroyNativeIfExistsLocked() {
        if (this.mNative != null) {
            this.mNative.close();
            this.mNative = null;
        }
    }

    private static String concatenateLocales(LocaleList localeList) {
        return localeList == null ? "" : localeList.toLanguageTags();
    }

    @GuardedBy("mLock")
    private ModelFile findBestModelLocked(LocaleList localeList) {
        List<Locale.LanguageRange> list = Locale.LanguageRange.parse(localeList.isEmpty() ? LocaleList.getDefault().toLanguageTags() : localeList.toLanguageTags() + "," + LocaleList.getDefault().toLanguageTags());
        ModelFile modelFile = null;
        for (ModelFile modelFile2 : listAllModelsLocked()) {
            if (modelFile2.isAnyLanguageSupported(list) && modelFile2.isPreferredTo(modelFile)) {
                modelFile = modelFile2;
            }
        }
        return modelFile;
    }

    @GuardedBy("mLock")
    private List<ModelFile> listAllModelsLocked() {
        ModelFile modelFileFromPath;
        ModelFile modelFileFromPath2;
        if (this.mAllModelFiles == null) {
            ArrayList arrayList = new ArrayList();
            if (new File(UPDATED_MODEL_FILE_PATH).exists() && (modelFileFromPath2 = ModelFile.fromPath(UPDATED_MODEL_FILE_PATH)) != null) {
                arrayList.add(modelFileFromPath2);
            }
            File file = new File(MODEL_DIR);
            if (file.exists() && file.isDirectory()) {
                File[] fileArrListFiles = file.listFiles();
                Pattern patternCompile = Pattern.compile(MODEL_FILE_REGEX);
                for (File file2 : fileArrListFiles) {
                    if (patternCompile.matcher(file2.getName()).matches() && file2.isFile() && (modelFileFromPath = ModelFile.fromPath(file2.getAbsolutePath())) != null) {
                        arrayList.add(modelFileFromPath);
                    }
                }
            }
            this.mAllModelFiles = arrayList;
        }
        return this.mAllModelFiles;
    }

    private TextClassification createClassificationResult(TextClassifierImplNative.ClassificationResult[] classificationResultArr, String str, int i, int i2, Instant instant) {
        String strSubstring = str.substring(i, i2);
        TextClassification.Builder text = new TextClassification.Builder().setText(strSubstring);
        int length = classificationResultArr.length;
        float score = Float.MIN_VALUE;
        TextClassifierImplNative.ClassificationResult classificationResult = null;
        for (int i3 = 0; i3 < length; i3++) {
            text.setEntityType(classificationResultArr[i3].getCollection(), classificationResultArr[i3].getScore());
            if (classificationResultArr[i3].getScore() > score) {
                classificationResult = classificationResultArr[i3];
                score = classificationResultArr[i3].getScore();
            }
        }
        boolean z = true;
        for (LabeledIntent labeledIntent : IntentFactory.create(this.mContext, instant, classificationResult, strSubstring)) {
            RemoteAction remoteActionAsRemoteAction = labeledIntent.asRemoteAction(this.mContext);
            if (remoteActionAsRemoteAction != null) {
                if (z) {
                    text.setIcon(remoteActionAsRemoteAction.getIcon().loadDrawable(this.mContext));
                    text.setLabel(remoteActionAsRemoteAction.getTitle().toString());
                    text.setIntent(labeledIntent.getIntent());
                    text.setOnClickListener(TextClassification.createIntentOnClickListener(TextClassification.createPendingIntent(this.mContext, labeledIntent.getIntent(), labeledIntent.getRequestCode())));
                    z = false;
                }
                text.addAction(remoteActionAsRemoteAction);
            }
        }
        return text.setId(createId(str, i, i2)).build();
    }

    private static void closeAndLogError(ParcelFileDescriptor parcelFileDescriptor) {
        try {
            parcelFileDescriptor.close();
        } catch (IOException e) {
            Log.e("androidtc", "Error closing file.", e);
        }
    }

    private static final class ModelFile {
        private final boolean mLanguageIndependent;
        private final String mName;
        private final String mPath;
        private final List<Locale> mSupportedLocales;
        private final int mVersion;

        static ModelFile fromPath(String str) {
            File file = new File(str);
            try {
                ParcelFileDescriptor parcelFileDescriptorOpen = ParcelFileDescriptor.open(file, 268435456);
                int version = TextClassifierImplNative.getVersion(parcelFileDescriptorOpen.getFd());
                String locales = TextClassifierImplNative.getLocales(parcelFileDescriptorOpen.getFd());
                if (locales.isEmpty()) {
                    Log.d("androidtc", "Ignoring " + file.getAbsolutePath());
                    return null;
                }
                boolean zEquals = locales.equals(PhoneConstants.APN_TYPE_ALL);
                ArrayList arrayList = new ArrayList();
                for (String str2 : locales.split(",")) {
                    arrayList.add(Locale.forLanguageTag(str2));
                }
                TextClassifierImpl.closeAndLogError(parcelFileDescriptorOpen);
                return new ModelFile(str, file.getName(), version, arrayList, zEquals);
            } catch (FileNotFoundException e) {
                Log.e("androidtc", "Failed to peek " + file.getAbsolutePath(), e);
                return null;
            }
        }

        String getPath() {
            return this.mPath;
        }

        String getName() {
            return this.mName;
        }

        int getVersion() {
            return this.mVersion;
        }

        boolean isAnyLanguageSupported(List<Locale.LanguageRange> list) {
            return this.mLanguageIndependent || Locale.lookup(list, this.mSupportedLocales) != null;
        }

        List<Locale> getSupportedLocales() {
            return Collections.unmodifiableList(this.mSupportedLocales);
        }

        public boolean isPreferredTo(ModelFile modelFile) {
            if (modelFile == null) {
                return true;
            }
            if ((!this.mLanguageIndependent && modelFile.mLanguageIndependent) || getVersion() > modelFile.getVersion()) {
                return true;
            }
            return false;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || !ModelFile.class.isAssignableFrom(obj.getClass())) {
                return false;
            }
            return this.mPath.equals(((ModelFile) obj).mPath);
        }

        public String toString() {
            StringJoiner stringJoiner = new StringJoiner(",");
            Iterator<Locale> it = this.mSupportedLocales.iterator();
            while (it.hasNext()) {
                stringJoiner.add(it.next().toLanguageTag());
            }
            return String.format(Locale.US, "ModelFile { path=%s name=%s version=%d locales=%s }", this.mPath, this.mName, Integer.valueOf(this.mVersion), stringJoiner.toString());
        }

        private ModelFile(String str, String str2, int i, List<Locale> list, boolean z) {
            this.mPath = str;
            this.mName = str2;
            this.mVersion = i;
            this.mSupportedLocales = list;
            this.mLanguageIndependent = z;
        }
    }

    private static final class LabeledIntent {
        static final int DEFAULT_REQUEST_CODE = 0;
        private final String mDescription;
        private final Intent mIntent;
        private final int mRequestCode;
        private final String mTitle;

        LabeledIntent(String str, String str2, Intent intent, int i) {
            this.mTitle = str;
            this.mDescription = str2;
            this.mIntent = intent;
            this.mRequestCode = i;
        }

        String getTitle() {
            return this.mTitle;
        }

        String getDescription() {
            return this.mDescription;
        }

        Intent getIntent() {
            return this.mIntent;
        }

        int getRequestCode() {
            return this.mRequestCode;
        }

        RemoteAction asRemoteAction(Context context) {
            String str;
            Icon iconCreateWithResource;
            boolean z = false;
            ResolveInfo resolveInfoResolveActivity = context.getPackageManager().resolveActivity(this.mIntent, 0);
            if (resolveInfoResolveActivity != null && resolveInfoResolveActivity.activityInfo != null) {
                str = resolveInfoResolveActivity.activityInfo.packageName;
            } else {
                str = null;
            }
            if (str != null && !ZenModeConfig.SYSTEM_AUTHORITY.equals(str)) {
                this.mIntent.setComponent(new ComponentName(str, resolveInfoResolveActivity.activityInfo.name));
                if (resolveInfoResolveActivity.activityInfo.getIconResource() != 0) {
                    iconCreateWithResource = Icon.createWithResource(str, resolveInfoResolveActivity.activityInfo.getIconResource());
                    z = true;
                }
            } else {
                iconCreateWithResource = null;
            }
            if (iconCreateWithResource == null) {
                iconCreateWithResource = Icon.createWithResource(ZenModeConfig.SYSTEM_AUTHORITY, R.drawable.ic_more_items);
            }
            PendingIntent pendingIntentCreatePendingIntent = TextClassification.createPendingIntent(context, this.mIntent, this.mRequestCode);
            if (pendingIntentCreatePendingIntent == null) {
                return null;
            }
            RemoteAction remoteAction = new RemoteAction(iconCreateWithResource, this.mTitle, this.mDescription, pendingIntentCreatePendingIntent);
            remoteAction.setShouldShowIcon(z);
            return remoteAction;
        }
    }

    static final class IntentFactory {
        private static final long MIN_EVENT_FUTURE_MILLIS = TimeUnit.MINUTES.toMillis(5);
        private static final long DEFAULT_EVENT_DURATION = TimeUnit.HOURS.toMillis(1);

        private IntentFactory() {
        }

        public static List<LabeledIntent> create(Context context, Instant instant, TextClassifierImplNative.ClassificationResult classificationResult, String str) {
            if (BenesseExtension.getDchaState() != 0) {
                return new ArrayList();
            }
            String lowerCase = classificationResult.getCollection().trim().toLowerCase(Locale.ENGLISH);
            String strTrim = str.trim();
            switch (lowerCase) {
                case "date":
                case "datetime":
                    if (classificationResult.getDatetimeResult() == null) {
                        break;
                    } else {
                        break;
                    }
                    break;
            }
            return new ArrayList();
        }

        private static List<LabeledIntent> createForEmail(Context context, String str) {
            return Arrays.asList(new LabeledIntent(context.getString(R.string.email), context.getString(R.string.email_desc), new Intent(Intent.ACTION_SENDTO).setData(Uri.parse(String.format("mailto:%s", str))), 0), new LabeledIntent(context.getString(R.string.add_contact), context.getString(R.string.add_contact_desc), new Intent(Intent.ACTION_INSERT_OR_EDIT).setType(ContactsContract.Contacts.CONTENT_ITEM_TYPE).putExtra("email", str), str.hashCode()));
        }

        private static List<LabeledIntent> createForPhone(Context context, String str) {
            ArrayList arrayList = new ArrayList();
            UserManager userManager = (UserManager) context.getSystemService(UserManager.class);
            Bundle userRestrictions = userManager != null ? userManager.getUserRestrictions() : new Bundle();
            if (!userRestrictions.getBoolean(UserManager.DISALLOW_OUTGOING_CALLS, false)) {
                arrayList.add(new LabeledIntent(context.getString(R.string.dial), context.getString(R.string.dial_desc), new Intent(Intent.ACTION_DIAL).setData(Uri.parse(String.format("tel:%s", str))), 0));
            }
            arrayList.add(new LabeledIntent(context.getString(R.string.add_contact), context.getString(R.string.add_contact_desc), new Intent(Intent.ACTION_INSERT_OR_EDIT).setType(ContactsContract.Contacts.CONTENT_ITEM_TYPE).putExtra("phone", str), str.hashCode()));
            if (!userRestrictions.getBoolean(UserManager.DISALLOW_SMS, false)) {
                arrayList.add(new LabeledIntent(context.getString(R.string.sms), context.getString(R.string.sms_desc), new Intent(Intent.ACTION_SENDTO).setData(Uri.parse(String.format("smsto:%s", str))), 0));
            }
            return arrayList;
        }

        private static List<LabeledIntent> createForAddress(Context context, String str) {
            ArrayList arrayList = new ArrayList();
            try {
                arrayList.add(new LabeledIntent(context.getString(R.string.map), context.getString(R.string.map_desc), new Intent("android.intent.action.VIEW").setData(Uri.parse(String.format("geo:0,0?q=%s", URLEncoder.encode(str, "UTF-8")))), 0));
            } catch (UnsupportedEncodingException e) {
                Log.e("androidtc", "Could not encode address", e);
            }
            return arrayList;
        }

        private static List<LabeledIntent> createForUrl(Context context, String str) {
            if (Uri.parse(str).getScheme() == null) {
                str = "http://" + str;
            }
            return Arrays.asList(new LabeledIntent(context.getString(R.string.browse), context.getString(R.string.browse_desc), new Intent("android.intent.action.VIEW", Uri.parse(str)).putExtra(Browser.EXTRA_APPLICATION_ID, context.getPackageName()), 0));
        }

        private static List<LabeledIntent> createForDatetime(Context context, String str, Instant instant, Instant instant2) {
            if (instant == null) {
                instant = Instant.now();
            }
            ArrayList arrayList = new ArrayList();
            arrayList.add(createCalendarViewIntent(context, instant2));
            if (instant.until(instant2, ChronoUnit.MILLIS) > MIN_EVENT_FUTURE_MILLIS) {
                arrayList.add(createCalendarCreateEventIntent(context, instant2, str));
            }
            return arrayList;
        }

        private static List<LabeledIntent> createForFlight(Context context, String str) {
            if (BenesseExtension.getDchaState() != 0) {
                return new ArrayList();
            }
            return Arrays.asList(new LabeledIntent(context.getString(R.string.view_flight), context.getString(R.string.view_flight_desc), new Intent(Intent.ACTION_WEB_SEARCH).putExtra("query", str), str.hashCode()));
        }

        private static LabeledIntent createCalendarViewIntent(Context context, Instant instant) {
            Uri.Builder builderBuildUpon = CalendarContract.CONTENT_URI.buildUpon();
            builderBuildUpon.appendPath(DropBoxManager.EXTRA_TIME);
            ContentUris.appendId(builderBuildUpon, instant.toEpochMilli());
            return new LabeledIntent(context.getString(R.string.view_calendar), context.getString(R.string.view_calendar_desc), new Intent("android.intent.action.VIEW").setData(builderBuildUpon.build()), 0);
        }

        private static LabeledIntent createCalendarCreateEventIntent(Context context, Instant instant, String str) {
            return new LabeledIntent(context.getString(R.string.add_calendar_event), context.getString(R.string.add_calendar_event_desc), new Intent("android.intent.action.INSERT").setData(CalendarContract.Events.CONTENT_URI).putExtra("allDay", "date".equals(str)).putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, instant.toEpochMilli()).putExtra(CalendarContract.EXTRA_EVENT_END_TIME, instant.toEpochMilli() + DEFAULT_EVENT_DURATION), instant.hashCode());
        }
    }
}
