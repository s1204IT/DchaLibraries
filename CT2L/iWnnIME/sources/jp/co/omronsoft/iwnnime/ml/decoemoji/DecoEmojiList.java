package jp.co.omronsoft.iwnnime.ml.decoemoji;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.util.Log;
import com.android.common.speech.LoggingEvents;
import java.util.ArrayList;
import java.util.Locale;
import jp.co.omronsoft.android.decoemojimanager.interfacedata.DecoEmojiCategoryInfo;
import jp.co.omronsoft.android.decoemojimanager.interfacedata.DecoEmojiContract;
import jp.co.omronsoft.android.emoji.EmojiAssist;

public class DecoEmojiList {
    private static final int MAX_ITEM_IN_PAGE = 500;
    private static final int NUMBER_CODE = 9312;
    private static final int PAGE_NUMBER = 10000;
    private static final String TAG = "iWnn";
    private int mCategoriesNextIndex;
    private int mCategoryId;
    private Context mContext;
    private int mDecoEmojiListNextIndex;
    private boolean mHasEnded;
    private boolean mIsJapanese;
    private EmojiAssist.DecoEmojiTextInfo mNextItem;
    private int mPageId;
    private ArrayList<DecoEmojiCategoryInfo> mCategories = new ArrayList<>();
    private ArrayList<EmojiAssist.DecoEmojiTextInfo> mDecoEmojiList = new ArrayList<>();
    private ArrayList<EmojiAssist.DecoEmojiTextInfo> mCreatedList = new ArrayList<>();

    public DecoEmojiList(Context context) {
        this.mContext = context;
    }

    public void initializeList() {
        this.mCreatedList.clear();
        this.mHasEnded = true;
        this.mIsJapanese = Locale.getDefault().getLanguage().equals(Locale.JAPANESE.toString());
        this.mCategories.clear();
        this.mCategoriesNextIndex = 0;
        int emojiType = DecoEmojiUtil.getEditorEmojiType();
        if (emojiType != -1) {
            Cursor cursor = null;
            Cursor tmpCursor = null;
            try {
                try {
                    Uri uri = DecoEmojiContract.CONTENT_DECOINFOLIST_URI.buildUpon().appendQueryParameter(DecoEmojiContract.QUERY_PARAM_DISTINCT, "true").build();
                    String[] selection = {DecoEmojiContract.DecoEmojiInfoColumns.CATEGORY_ID, DecoEmojiContract.DecoEmojiInfoColumns.CATEGORY_NAME_JPN, DecoEmojiContract.DecoEmojiInfoColumns.CATEGORY_NAME_ENG};
                    String emojiTypeColum = DecoEmojiContract.makeStringEmojiKind((byte) emojiType);
                    String where = "kind IN (" + emojiTypeColum + ")";
                    String total_contents = " (category_id = ?) and (kind IN (" + emojiTypeColum + "))and " + DecoEmojiContract.DecoEmojiInfoColumns.KIND;
                    String[] arg = {LoggingEvents.EXTRA_CALLING_APP_NAME};
                    int itemPage = 0;
                    cursor = this.mContext.getContentResolver().query(uri, selection, where, null, "category_preset_id asc");
                    if (cursor != null && cursor.getCount() > 0) {
                        while (cursor.moveToNext()) {
                            int categoryId = cursor.getInt(cursor.getColumnIndex(DecoEmojiContract.DecoEmojiInfoColumns.CATEGORY_ID));
                            arg[0] = String.valueOf(categoryId);
                            tmpCursor = this.mContext.getContentResolver().query(DecoEmojiContract.CONTENT_DECOINFOLIST_COUNT_URI, null, total_contents, arg, null);
                            if (tmpCursor != null) {
                                if (tmpCursor.getCount() > 0) {
                                    tmpCursor.moveToNext();
                                    itemPage = tmpCursor.getInt(0) / 500;
                                    if (tmpCursor.getInt(0) % 500 > 0) {
                                        itemPage++;
                                    }
                                }
                                tmpCursor.close();
                                for (int i = 0; i < itemPage; i++) {
                                    DecoEmojiCategoryInfo values = new DecoEmojiCategoryInfo();
                                    int categoryId2 = categoryId + (i * PAGE_NUMBER);
                                    String name_jpn = cursor.getString(cursor.getColumnIndex(DecoEmojiContract.DecoEmojiInfoColumns.CATEGORY_NAME_JPN));
                                    String name_eng = cursor.getString(cursor.getColumnIndex(DecoEmojiContract.DecoEmojiInfoColumns.CATEGORY_NAME_ENG));
                                    if (itemPage > 1) {
                                        int[] number = {i + NUMBER_CODE};
                                        String page_number = new String(number, 0, number.length);
                                        name_jpn = name_jpn + page_number;
                                        name_eng = name_eng + page_number;
                                    }
                                    values.setCategoryId(categoryId2);
                                    values.setCategoryName_jpn(name_jpn);
                                    values.setCategoryName_eng(name_eng);
                                    this.mCategories.add(values);
                                }
                            }
                        }
                    }
                } catch (SQLiteException e) {
                    e.printStackTrace();
                    if (cursor != null) {
                        cursor.close();
                    }
                    if (tmpCursor != null) {
                        tmpCursor.close();
                    }
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
                if (tmpCursor != null) {
                    tmpCursor.close();
                }
            }
        }
        this.mHasEnded = false;
        this.mNextItem = createNextCategory();
    }

    public EmojiAssist.DecoEmojiTextInfo getItem(int index) {
        if (index < this.mCreatedList.size()) {
            return this.mCreatedList.get(index);
        }
        if (index != this.mCreatedList.size()) {
            Log.e(TAG, "Request index is mismatch: " + index + "/" + this.mCreatedList.size());
        }
        if (this.mHasEnded) {
            return null;
        }
        EmojiAssist.DecoEmojiTextInfo ret = this.mNextItem;
        boolean nextCategory = false;
        if (this.mDecoEmojiList.size() <= this.mDecoEmojiListNextIndex) {
            createNextDecoEmojiList();
            if (this.mDecoEmojiList.size() == 0) {
                nextCategory = true;
            }
        }
        if (nextCategory) {
            this.mNextItem = createNextCategory();
        } else {
            this.mNextItem = this.mDecoEmojiList.get(this.mDecoEmojiListNextIndex);
            this.mDecoEmojiListNextIndex++;
        }
        this.mCreatedList.add(ret);
        return ret;
    }

    private EmojiAssist.DecoEmojiTextInfo createNextCategory() {
        String categoryText;
        if (this.mCategories.size() <= this.mCategoriesNextIndex) {
            this.mHasEnded = true;
            return null;
        }
        DecoEmojiCategoryInfo info = this.mCategories.get(this.mCategoriesNextIndex);
        this.mCategoriesNextIndex++;
        createDecoEmojiList(info.getCategoryId());
        if (this.mIsJapanese) {
            categoryText = info.getCategoryName_jpn();
        } else {
            categoryText = info.getCategoryName_eng();
        }
        EmojiAssist.DecoEmojiTextInfo ret = new EmojiAssist.DecoEmojiTextInfo();
        ret.setUri("[" + categoryText + "]");
        return ret;
    }

    private void createDecoEmojiList(int categoryId) {
        this.mCategoryId = categoryId;
        this.mPageId = -1;
        createNextDecoEmojiList();
    }

    private void createNextDecoEmojiList() {
        this.mPageId++;
        getDecoEmojiList();
    }

    private void getDecoEmojiList() {
        this.mDecoEmojiList.clear();
        this.mDecoEmojiListNextIndex = 0;
        int start = this.mPageId * 500;
        int emojiType = DecoEmojiUtil.getEditorEmojiType();
        if (emojiType == -1) {
            return;
        }
        String emojiTypeColum = DecoEmojiContract.makeStringEmojiKind((byte) emojiType);
        String where = " (category_id = ?) and (kind IN (" + emojiTypeColum + "))and " + DecoEmojiContract.DecoEmojiInfoColumns.KIND + " <> 4";
        String[] arg = {LoggingEvents.EXTRA_CALLING_APP_NAME};
        arg[0] = String.valueOf(this.mCategoryId);
        Cursor cursor = null;
        try {
            try {
                cursor = this.mContext.getContentResolver().query(DecoEmojiContract.CONTENT_DECOINFOLIST_COUNT_URI, null, where, arg, null);
                if (cursor == null) {
                    if (cursor != null) {
                        cursor.close();
                        return;
                    }
                    return;
                }
                if (cursor.getCount() <= 0) {
                    if (cursor != null) {
                        cursor.close();
                        return;
                    }
                    return;
                }
                cursor.moveToNext();
                int count = cursor.getInt(0);
                cursor.close();
                String[] selection = DecoEmojiUtil.SELECTION_DECOEMOJI_INFO;
                String where2 = " (category_id = ?) and (kind IN (" + emojiTypeColum + "))and " + DecoEmojiContract.DecoEmojiInfoColumns.KIND + " <> 4";
                String order = "last_use_cnt desc, uri asc limit 500 offset " + start;
                int emojiDCount = 0;
                if (start < count && (cursor = this.mContext.getContentResolver().query(DecoEmojiContract.CONTENT_DECOINFOLIST_URI, selection, where2, arg, order)) != null) {
                    if (cursor.getCount() > 0) {
                        while (cursor.moveToNext()) {
                            EmojiAssist.DecoEmojiTextInfo info = new EmojiAssist.DecoEmojiTextInfo();
                            DecoEmojiUtil.setDecoEmojiInfo(cursor, info);
                            this.mDecoEmojiList.add(info);
                            emojiDCount++;
                        }
                    }
                    cursor.close();
                }
                if (start + 500 > count) {
                    int offset = 0;
                    if (emojiDCount == 0 && (offset = start - count) < 0) {
                        offset = 0;
                    }
                    String where3 = " (category_id = ?) and (kind IN (" + emojiTypeColum + "))and " + DecoEmojiContract.DecoEmojiInfoColumns.KIND + " = 4";
                    String order2 = "last_use_cnt desc, uri asc limit " + (500 - emojiDCount) + " offset " + offset;
                    cursor = this.mContext.getContentResolver().query(DecoEmojiContract.CONTENT_DECOINFOLIST_URI, selection, where3, arg, order2);
                    if (cursor != null) {
                        if (cursor.getCount() > 0) {
                            while (cursor.moveToNext()) {
                                EmojiAssist.DecoEmojiTextInfo info2 = new EmojiAssist.DecoEmojiTextInfo();
                                DecoEmojiUtil.setDecoEmojiInfo(cursor, info2);
                                this.mDecoEmojiList.add(info2);
                            }
                        }
                        cursor.close();
                    }
                }
                if (cursor != null) {
                    cursor.close();
                }
            } catch (SQLiteException e) {
                e.printStackTrace();
                if (cursor != null) {
                    cursor.close();
                }
            }
        } catch (Throwable th) {
            if (cursor != null) {
                cursor.close();
            }
            throw th;
        }
    }

    public ArrayList<DecoEmojiCategoryInfo> getCategoryList() {
        return this.mCategories;
    }

    public ArrayList<EmojiAssist.DecoEmojiTextInfo> getDecoEmojiItemList(int category) {
        if (category > PAGE_NUMBER) {
            this.mCategoryId = category % PAGE_NUMBER;
            this.mPageId = category / PAGE_NUMBER;
        } else {
            this.mCategoryId = category;
            this.mPageId = 0;
        }
        getDecoEmojiList();
        return this.mDecoEmojiList;
    }
}
