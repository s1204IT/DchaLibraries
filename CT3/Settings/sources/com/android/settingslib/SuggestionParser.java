package com.android.settingslib;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Pair;
import android.util.Xml;
import android.view.InflateException;
import com.android.settingslib.drawer.Tile;
import com.android.settingslib.drawer.TileUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class SuggestionParser {
    private final ArrayMap<Pair<String, String>, Tile> addCache = new ArrayMap<>();
    private final Context mContext;
    private final SharedPreferences mSharedPrefs;
    private final List<SuggestionCategory> mSuggestionList;

    public SuggestionParser(Context context, SharedPreferences sharedPrefs, int orderXml) {
        this.mContext = context;
        this.mSuggestionList = (List) new SuggestionOrderInflater(this.mContext).parse(orderXml);
        this.mSharedPrefs = sharedPrefs;
    }

    public List<Tile> getSuggestions() {
        List<Tile> suggestions = new ArrayList<>();
        int N = this.mSuggestionList.size();
        for (int i = 0; i < N; i++) {
            readSuggestions(this.mSuggestionList.get(i), suggestions);
        }
        return suggestions;
    }

    public boolean dismissSuggestion(Tile suggestion) {
        String keyBase = suggestion.intent.getComponent().flattenToShortString();
        int index = this.mSharedPrefs.getInt(keyBase + "_dismiss_index", 0);
        String dismissControl = suggestion.metaData.getString("com.android.settings.dismiss");
        if (dismissControl == null || parseDismissString(dismissControl).length == index) {
            return true;
        }
        this.mSharedPrefs.edit().putBoolean(keyBase + "_is_dismissed", true).commit();
        return false;
    }

    private void readSuggestions(SuggestionCategory category, List<Tile> suggestions) {
        int countBefore = suggestions.size();
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.addCategory(category.category);
        if (category.pkg != null) {
            intent.setPackage(category.pkg);
        }
        TileUtils.getTilesForIntent(this.mContext, new UserHandle(UserHandle.myUserId()), intent, this.addCache, null, suggestions, true, false);
        int i = countBefore;
        while (i < suggestions.size()) {
            if (!isAvailable(suggestions.get(i)) || !isSupported(suggestions.get(i)) || !satisfiesRequiredAccount(suggestions.get(i)) || isDismissed(suggestions.get(i))) {
                suggestions.remove(i);
                i--;
            }
            i++;
        }
        if (category.multiple || suggestions.size() <= countBefore + 1) {
            return;
        }
        Tile item = suggestions.remove(suggestions.size() - 1);
        while (suggestions.size() > countBefore) {
            Tile last = suggestions.remove(suggestions.size() - 1);
            if (last.priority > item.priority) {
                item = last;
            }
        }
        if (isCategoryDone(category.category)) {
            return;
        }
        suggestions.add(item);
    }

    private boolean isAvailable(Tile suggestion) {
        String featureRequired = suggestion.metaData.getString("com.android.settings.require_feature");
        if (featureRequired != null) {
            return this.mContext.getPackageManager().hasSystemFeature(featureRequired);
        }
        return true;
    }

    public boolean satisfiesRequiredAccount(Tile suggestion) {
        String requiredAccountType = suggestion.metaData.getString("com.android.settings.require_account");
        if (requiredAccountType == null) {
            return true;
        }
        AccountManager accountManager = AccountManager.get(this.mContext);
        Account[] accounts = accountManager.getAccountsByType(requiredAccountType);
        return accounts.length > 0;
    }

    public boolean isSupported(Tile suggestion) {
        int isSupportedResource = suggestion.metaData.getInt("com.android.settings.is_supported");
        try {
            if (suggestion.intent == null) {
                return false;
            }
            Resources res = this.mContext.getPackageManager().getResourcesForActivity(suggestion.intent.getComponent());
            if (isSupportedResource != 0) {
                return res.getBoolean(isSupportedResource);
            }
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            Log.w("SuggestionParser", "Cannot find resources for " + suggestion.intent.getComponent());
            return false;
        } catch (Resources.NotFoundException e2) {
            Log.w("SuggestionParser", "Cannot find resources for " + suggestion.intent.getComponent(), e2);
            return false;
        }
    }

    public boolean isCategoryDone(String category) {
        String name = "suggested.completed_category." + category;
        return Settings.Secure.getInt(this.mContext.getContentResolver(), name, 0) != 0;
    }

    public void markCategoryDone(String category) {
        String name = "suggested.completed_category." + category;
        Settings.Secure.putInt(this.mContext.getContentResolver(), name, 1);
    }

    private boolean isDismissed(Tile suggestion) {
        Object dismissObj = suggestion.metaData.get("com.android.settings.dismiss");
        if (dismissObj == null) {
            return false;
        }
        String dismissControl = String.valueOf(dismissObj);
        String keyBase = suggestion.intent.getComponent().flattenToShortString();
        if (!this.mSharedPrefs.contains(keyBase + "_setup_time")) {
            this.mSharedPrefs.edit().putLong(keyBase + "_setup_time", System.currentTimeMillis()).commit();
        }
        if (!this.mSharedPrefs.getBoolean(keyBase + "_is_dismissed", true)) {
            return false;
        }
        int index = this.mSharedPrefs.getInt(keyBase + "_dismiss_index", 0);
        int currentDismiss = parseDismissString(dismissControl)[index];
        long time = getEndTime(this.mSharedPrefs.getLong(keyBase + "_setup_time", 0L), currentDismiss);
        if (System.currentTimeMillis() < time) {
            return true;
        }
        this.mSharedPrefs.edit().putBoolean(keyBase + "_is_dismissed", false).putInt(keyBase + "_dismiss_index", index + 1).commit();
        return false;
    }

    private long getEndTime(long startTime, int daysDelay) {
        long days = ((long) daysDelay) * 86400000;
        return startTime + days;
    }

    private int[] parseDismissString(String dismissControl) {
        String[] dismissStrs = dismissControl.split(",");
        int[] dismisses = new int[dismissStrs.length];
        for (int i = 0; i < dismissStrs.length; i++) {
            dismisses[i] = Integer.parseInt(dismissStrs[i]);
        }
        return dismisses;
    }

    private static class SuggestionCategory {
        public String category;
        public boolean multiple;
        public String pkg;

        SuggestionCategory(SuggestionCategory suggestionCategory) {
            this();
        }

        private SuggestionCategory() {
        }
    }

    private static class SuggestionOrderInflater {
        private final Context mContext;

        public SuggestionOrderInflater(Context context) {
            this.mContext = context;
        }

        public Object parse(int resource) {
            int type;
            XmlPullParser parser = this.mContext.getResources().getXml(resource);
            AttributeSet attrs = Xml.asAttributeSet(parser);
            do {
                try {
                    type = parser.next();
                    if (type == 2) {
                        break;
                    }
                } catch (IOException | XmlPullParserException e) {
                    Log.w("SuggestionParser", "Problem parser resource " + resource, e);
                    return null;
                }
            } while (type != 1);
            if (type != 2) {
                throw new InflateException(parser.getPositionDescription() + ": No start tag found!");
            }
            Object xmlRoot = onCreateItem(parser.getName(), attrs);
            rParse(parser, xmlRoot, attrs);
            return xmlRoot;
        }

        private void rParse(XmlPullParser parser, Object parent, AttributeSet attrs) throws XmlPullParserException, IOException {
            int depth = parser.getDepth();
            while (true) {
                int type = parser.next();
                if ((type == 3 && parser.getDepth() <= depth) || type == 1) {
                    return;
                }
                if (type == 2) {
                    String name = parser.getName();
                    Object item = onCreateItem(name, attrs);
                    onAddChildItem(parent, item);
                    rParse(parser, item, attrs);
                }
            }
        }

        protected void onAddChildItem(Object parent, Object child) {
            if ((parent instanceof List) && (child instanceof SuggestionCategory)) {
                ((List) parent).add((SuggestionCategory) child);
                return;
            }
            throw new IllegalArgumentException("Parent was not a list");
        }

        protected Object onCreateItem(String name, AttributeSet attrs) {
            SuggestionCategory suggestionCategory = null;
            if (name.equals("optional-steps")) {
                return new ArrayList();
            }
            if (name.equals("step")) {
                SuggestionCategory category = new SuggestionCategory(suggestionCategory);
                category.category = attrs.getAttributeValue(null, "category");
                category.pkg = attrs.getAttributeValue(null, "package");
                String multiple = attrs.getAttributeValue(null, "multiple");
                category.multiple = !TextUtils.isEmpty(multiple) ? Boolean.parseBoolean(multiple) : false;
                return category;
            }
            throw new IllegalArgumentException("Unknown item " + name);
        }
    }
}
