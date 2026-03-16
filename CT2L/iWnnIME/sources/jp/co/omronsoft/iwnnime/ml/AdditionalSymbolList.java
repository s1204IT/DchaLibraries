package jp.co.omronsoft.iwnnime.ml;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;
import com.android.common.speech.LoggingEvents;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import jp.co.omronsoft.android.emoji.EmojiAssist;
import jp.co.omronsoft.iwnnime.ml.candidate.CandidatesManager;

public class AdditionalSymbolList {
    private static final String ADD_SYMBOL_LIST_ACTION = "jp.co.omronsoft.iwnnime.ml.ADD_SYMBOL";
    private static final String ADD_SYMBOL_LIST_KEY = "opt_add_symbol_list";
    private static final String ADD_SYMBOL_LIST_LONGPRESS_ACTION = "jp.co.omronsoft.iwnnime.ml.ADD_SYMBOL_LONGPRESS";
    private static final String ADD_SYMBOL_LIST_LONGPRESS_WORD_KEY = "longpressword";
    public static final int ADD_SYMBOL_LIST_SYMBOLTYPE_DECOEMOJI = 2;
    public static final int ADD_SYMBOL_LIST_SYMBOLTYPE_EMOJI = 1;
    public static final int ADD_SYMBOL_LIST_SYMBOLTYPE_INIT = -1;
    public static final int ADD_SYMBOL_LIST_SYMBOLTYPE_NORMAL = 0;
    private static final String TAG = "iWnn";
    private Context mContext;
    private int mMaxCandidates;
    private int mMaxCategories;

    public AdditionalSymbolList(Context context) {
        this.mMaxCandidates = 0;
        this.mMaxCategories = 0;
        this.mContext = context;
        Resources res = this.mContext.getResources();
        this.mMaxCandidates = res.getInteger(R.integer.additional_symbol_list_candidates_max);
        this.mMaxCategories = res.getInteger(R.integer.additional_symbol_list_categories_max);
    }

    public static List<ResolveInfo> getAdditionalSymbolListInfo(Context context) {
        PackageManager pm = context.getPackageManager();
        Intent intent = new Intent(ADD_SYMBOL_LIST_ACTION);
        List<ResolveInfo> tempResolveInfo = pm.queryIntentActivities(intent, 0);
        int infoFullSize = tempResolveInfo.size();
        ArrayList<ResolveInfo> resolveInfo = new ArrayList<>();
        for (int count = 0; count < infoFullSize; count++) {
            ResolveInfo checkResInfo = tempResolveInfo.get(count);
            String packageName = checkResInfo.activityInfo.packageName;
            int symbolType = getSymbolType(context, packageName);
            if (symbolType == 0 || symbolType == 1 || symbolType == 2) {
                resolveInfo.add(checkResInfo);
            } else {
                Log.e(TAG, "AdditionalSymbolList::getAdditionalSymbolListInfo() Illegal additional symbol app!! package name[" + packageName + "]");
            }
        }
        Collections.sort(resolveInfo, new ResolveInfo.DisplayNameComparator(pm));
        return resolveInfo;
    }

    public static String[] getSelectAdditionalSymbolList(Context context, boolean enableEmoji, boolean enableDecoEmoji) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        Set<String> values = sharedPref.getStringSet("opt_add_symbol_list", null);
        if (values == null || values.isEmpty()) {
            return null;
        }
        String[] tempStr = (String[]) values.toArray(new String[values.size()]);
        ArrayList<String> tempArrayStr = new ArrayList<>();
        for (int count = 0; count < tempStr.length; count++) {
            int symbolType = getSymbolType(context, tempStr[count]);
            if (symbolType == 0 || ((enableEmoji && symbolType == 1) || (enableDecoEmoji && symbolType == 2))) {
                tempArrayStr.add(tempStr[count]);
            } else {
                Log.e(TAG, "AdditionalSymbolList::getSelectAdditionalSymbolList() Not Disp Tab!!");
                Log.e(TAG, "package name[" + tempStr[count] + "] enableEmoji[" + enableEmoji + "] enableDecoEmoji[" + enableDecoEmoji + "] symbolType[" + symbolType + "]");
            }
        }
        if (tempArrayStr.isEmpty()) {
            Log.e(TAG, "AdditionalSymbolList::getSelectAdditionalSymbolList() Additinal symbol list can be displayed in the current field is not!");
            return null;
        }
        return (String[]) tempArrayStr.toArray(new String[tempArrayStr.size()]);
    }

    public String getTabName(String packageName) {
        Uri uri = Uri.parse("content://" + packageName + "/tab");
        Cursor cursor = this.mContext.getContentResolver().query(uri, null, null, null, null);
        if (cursor != null) {
            String tabName = null;
            if (cursor.moveToFirst()) {
                tabName = cursor.getString(0);
            } else {
                Log.e(TAG, "AdditionalSymbolList::getTabName() cursor is empty!! package name[" + packageName + "]");
            }
            cursor.close();
            return tabName;
        }
        Log.e(TAG, "AdditionalSymbolList::getTabName() cursor is null!! package name[" + packageName + "]");
        return null;
    }

    public void updateHistories(String packageName, String[] histories) {
        if (this.mContext != null && packageName != null && histories != null) {
            ContentValues args = new ContentValues();
            for (int i = 0; i < histories.length; i++) {
                if (histories[i] != null) {
                    args.put("history_" + i, histories[i]);
                } else {
                    args.put("history_" + i, LoggingEvents.EXTRA_CALLING_APP_NAME);
                }
            }
            Uri uri = Uri.parse("content://" + packageName + "/history");
            this.mContext.getContentResolver().update(uri, args, null, null);
        }
    }

    public void deleteHistories() {
        String[] packageNames = getSelectAdditionalSymbolList(this.mContext, true, true);
        if (packageNames != null) {
            for (String packageName : packageNames) {
                Uri uri = Uri.parse("content://" + packageName + "/history");
                this.mContext.getContentResolver().delete(uri, null, null);
            }
        }
    }

    public String[] getCandidates(String packageName, String category) {
        int max;
        Uri uri = Uri.parse("content://" + packageName + "/candidates-" + category);
        Cursor cursor = this.mContext.getContentResolver().query(uri, null, null, null, null);
        if (category.equals(CandidatesManager.CATEGORY_NAME_HISTORY)) {
            max = 50;
        } else {
            max = this.mMaxCandidates;
        }
        if (cursor != null) {
            ArrayList<String> candidates = new ArrayList<>();
            int count = 0;
            while (cursor.moveToNext()) {
                candidates.add(cursor.getString(0));
                count++;
                if (count >= max) {
                    break;
                }
            }
            cursor.close();
            return (String[]) candidates.toArray(new String[candidates.size()]);
        }
        Log.e(TAG, "AdditionalSymbolList::getCandidates() cursor is null!! package name[" + packageName + "]");
        return null;
    }

    public String[] getCategoryList(String packageName) {
        Uri uri = Uri.parse("content://" + packageName + "/categorylist");
        Cursor cursor = this.mContext.getContentResolver().query(uri, null, null, null, null);
        if (cursor != null) {
            ArrayList<String> candidates = new ArrayList<>();
            int count = 0;
            while (cursor.moveToNext()) {
                candidates.add(cursor.getString(0));
                count++;
                if (count >= this.mMaxCategories) {
                    break;
                }
            }
            cursor.close();
            return (String[]) candidates.toArray(new String[candidates.size()]);
        }
        Log.e(TAG, "AdditionalSymbolList::getCategoryList() cursor is null!! package name[" + packageName + "]");
        return null;
    }

    public List<EmojiAssist.DecoEmojiTextInfo> getDecoEmojiCandidates(String packageName, String category) {
        int max;
        Uri uri = Uri.parse("content://" + packageName + "/candidates-" + category);
        Cursor cursor = this.mContext.getContentResolver().query(uri, null, null, null, null);
        if (category.equals(CandidatesManager.CATEGORY_NAME_HISTORY)) {
            max = 50;
        } else {
            max = this.mMaxCandidates;
        }
        if (cursor != null) {
            ArrayList<EmojiAssist.DecoEmojiTextInfo> candidates = new ArrayList<>();
            int count = 0;
            while (cursor.moveToNext()) {
                String textInfo = cursor.getString(0);
                if (textInfo.indexOf(EmojiAssist.SPLIT_KEY) > 0) {
                    EmojiAssist.DecoEmojiTextInfo decoEmojiTextInfo = new EmojiAssist.DecoEmojiTextInfo();
                    String[] split = textInfo.split(EmojiAssist.SPLIT_KEY, 0);
                    try {
                        String path = split[0];
                        int width = Integer.parseInt(split[1]);
                        int height = Integer.parseInt(split[2]);
                        int kind = Integer.parseInt(split[3]);
                        decoEmojiTextInfo.setUri(path);
                        decoEmojiTextInfo.setWidth(width);
                        decoEmojiTextInfo.setHeight(height);
                        decoEmojiTextInfo.setKind(kind);
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "AdditionalSymbolList::getDecoEmojiCandidates() Exception[" + e + "]");
                    }
                    candidates.add(decoEmojiTextInfo);
                    count++;
                    if (count >= max) {
                        break;
                    }
                }
            }
            cursor.close();
            return candidates;
        }
        Log.e(TAG, "AdditionalSymbolList::getDecoEmojiCandidates() cursor is null!! package name[" + packageName + "]");
        return null;
    }

    public static int getSymbolType(Context context, String packageName) {
        Uri uri = Uri.parse("content://" + packageName + "/symboltype");
        Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
        if (cursor != null) {
            int symbolType = -1;
            if (cursor.moveToFirst()) {
                symbolType = cursor.getInt(0);
                if (symbolType < 0 || symbolType > 2) {
                    Log.e(TAG, "AdditionalSymbolList::getSymbolType() Illegal symbol type!! package name[" + packageName + "] get symbol type[" + symbolType + "]");
                    symbolType = -1;
                }
            } else {
                Log.e(TAG, "AdditionalSymbolList::getSymbolType() Fails to get symbol type(cursor is empty)!! package name[" + packageName + "]");
            }
            cursor.close();
            return symbolType;
        }
        Log.e(TAG, "AdditionalSymbolList::getSymbolType() Fails to get symbol type(cursor is null)!! package name[" + packageName + "]");
        return -1;
    }

    public boolean startLongPressActionAdditionalSymbol(String packageName, String candidate) {
        Uri uri = Uri.parse("content://" + packageName + "/longpressstartclass");
        Cursor cursor = this.mContext.getContentResolver().query(uri, null, candidate, null, null);
        if (cursor != null) {
            boolean ret = false;
            if (cursor.moveToFirst()) {
                String startClass = cursor.getString(0);
                Intent intent = new Intent(ADD_SYMBOL_LIST_LONGPRESS_ACTION);
                intent.setClassName(packageName, startClass);
                intent.putExtra(ADD_SYMBOL_LIST_LONGPRESS_WORD_KEY, candidate);
                this.mContext.sendBroadcast(intent);
                ret = true;
            } else {
                Log.e(TAG, "AdditionalSymbolList::startLongPressActionAdditionalSymbol() Fails to get start class(cursor is empty)!! package name[" + packageName + "]");
            }
            cursor.close();
            return ret;
        }
        Log.e(TAG, "AdditionalSymbolList::startLongPressActionAdditionalSymbol() Fails to get start class(cursor is null)!! package name[" + packageName + "]");
        return false;
    }
}
