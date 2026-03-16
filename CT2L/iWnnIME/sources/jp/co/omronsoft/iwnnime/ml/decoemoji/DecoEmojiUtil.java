package jp.co.omronsoft.iwnnime.ml.decoemoji;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.util.Log;
import com.android.common.speech.LoggingEvents;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import jp.co.omronsoft.android.decoemojimanager.interfacedata.DecoEmojiAttrInfo;
import jp.co.omronsoft.android.decoemojimanager.interfacedata.DecoEmojiContract;
import jp.co.omronsoft.android.decoemojimanager.interfacedata.IDecoEmojiConstant;
import jp.co.omronsoft.android.emoji.EmojiAssist;
import jp.co.omronsoft.iwnnime.ml.ComposingText;
import jp.co.omronsoft.iwnnime.ml.IWnnIME;
import jp.co.omronsoft.iwnnime.ml.WnnWord;
import jp.co.omronsoft.iwnnime.ml.iwnn.iWnnEngine;

public class DecoEmojiUtil {
    public static final String DECOEMOJIMANAGER_CLASSNAME = "jp.co.omronsoft.android.decoemojimanager.DecoEmojiManager";
    public static final String DECOEMOJIMANAGER_PACKAGENAME = "jp.co.omronsoft.android.decoemojimanager";
    public static final int DECOEMOJI_ID_LENGTH = 5;
    public static final int DECOEMOJI_ID_PREFIX = 27;
    private static final int DECOEMOJI_INFO_CNT = 100;
    public static final int EMOJITYPE_INVALID = -1;
    public static final String[] SELECTION_DECOEMOJI_INFO = {"uri", DecoEmojiContract.DecoEmojiInfoColumns.WIDTH, DecoEmojiContract.DecoEmojiInfoColumns.HEIGHT, DecoEmojiContract.DecoEmojiInfoColumns.KIND, DecoEmojiContract.DecoEmojiInfoColumns.DECOME_POP_FLAG};
    private static final int STATE_ERROR_DISABLE_DECOEMOJI = -100;
    private static final int STATE_ERROR_INVALID_ID = -101;
    private static final int STATE_ERROR_NOT_BIND_MANAGER = -102;
    private static final int STATE_ERROR_NOT_EMOJI_TYPE = -105;
    private static final int STATE_ERROR_NOT_FOUND_FILE = -104;
    private static final int STATE_ERROR_REMOTE_EXCEPTION_MANAGER = -103;
    private static final int STATE_NOT_DECOEMOJI = 0;
    private static final int STATE_SPANNED_DECOEMOJI = 1;
    private static final String TAG = "iWnn";
    private static boolean sEnableDecoEmoji;

    public static void setConvertFunctionEnabled(boolean enable) {
        sEnableDecoEmoji = enable;
    }

    public static CharSequence getSpannedCandidate(WnnWord word) {
        Context context = IWnnIME.getContext();
        if (context == null) {
            return LoggingEvents.EXTRA_CALLING_APP_NAME;
        }
        if (sEnableDecoEmoji && (word.attribute & iWnnEngine.WNNWORD_ATTRIBUTE_DECOEMOJI) != 0) {
            EmojiAssist.DecoEmojiTextInfo decoEmojiTextInfo = new EmojiAssist.DecoEmojiTextInfo();
            if (word.decoEmojiInfo != null) {
                int width = word.decoEmojiInfo.getWidth();
                int height = word.decoEmojiInfo.getHeight();
                if (width > 0 && height > 0) {
                    decoEmojiTextInfo.setWidth(width);
                    decoEmojiTextInfo.setHeight(height);
                    decoEmojiTextInfo.setKind(word.decoEmojiInfo.getKind());
                }
            }
            decoEmojiTextInfo.setUri("file://" + word.candidate);
            decoEmojiTextInfo.setEmojiType(IWnnIME.getEmojiType());
            decoEmojiTextInfo.setContext(context);
            return EmojiAssist.getInstance().getDecoEmojiText(decoEmojiTextInfo);
        }
        return getSpannedText(word.candidate);
    }

    public static CharSequence getSpannedText(CharSequence text) {
        return getSpannedText(text, new int[1]);
    }

    public static CharSequence getSpannedText(CharSequence text, int[] status) {
        status[0] = 0;
        if (text.toString().indexOf(27) >= 0) {
            if (!sEnableDecoEmoji) {
                status[0] = -100;
                if (IWnnIME.getCurrentIme() == null) {
                    return LoggingEvents.EXTRA_CALLING_APP_NAME;
                }
                return text;
            }
            Context context = IWnnIME.getContext();
            if (context == null) {
                return LoggingEvents.EXTRA_CALLING_APP_NAME;
            }
            try {
                byte[] charArray = text.toString().getBytes(IWnnIME.CHARSET_NAME_UTF8);
                int left = 0;
                int last = charArray.length;
                SpannableStringBuilder builder = new SpannableStringBuilder();
                int emojiType = getEditorEmojiType();
                if (emojiType == -1) {
                    emojiType = 0;
                }
                int i = 0;
                while (i < last) {
                    if (charArray[i] == 27) {
                        int count = i - left;
                        if (count > 0) {
                            builder.append((CharSequence) new String(charArray, left, count, IWnnIME.CHARSET_NAME_UTF8));
                        }
                        EmojiAssist.DecoEmojiTextInfo info = getDecoEmojiInfo(convertToEmojiIdInt(charArray, i), status);
                        File file = new File(info.getUri());
                        if (!file.exists()) {
                            if (status[0] == 0) {
                                status[0] = STATE_ERROR_NOT_FOUND_FILE;
                            }
                            return LoggingEvents.EXTRA_CALLING_APP_NAME;
                        }
                        info.setUri("file://" + info.getUri());
                        info.setEmojiType(IWnnIME.getEmojiType());
                        info.setContext(context);
                        CharSequence decoEmoji = EmojiAssist.getInstance().getDecoEmojiText(info);
                        int convEmojiType = DecoEmojiContract.convertEmojiType(info.getKind());
                        if ((convEmojiType & emojiType) == 0) {
                            status[0] = STATE_ERROR_NOT_EMOJI_TYPE;
                            return LoggingEvents.EXTRA_CALLING_APP_NAME;
                        }
                        builder.append(decoEmoji);
                        i += 5;
                        left = i;
                    } else {
                        i++;
                    }
                }
                if (builder.length() > 0) {
                    if (left < last) {
                        builder.append((CharSequence) new String(charArray, left, last - left, IWnnIME.CHARSET_NAME_UTF8));
                    }
                    status[0] = 1;
                    return builder;
                }
                return text;
            } catch (UnsupportedEncodingException e) {
                Log.e(TAG, "DecoEmojiUtil#getSpannedText() Exception", e);
                return text;
            }
        }
        return text;
    }

    public static int convertToEmojiIdInt(byte[] charArray, int offset) {
        if (charArray.length < offset + 5) {
            return -1;
        }
        int id = 0;
        int last = offset + 5;
        for (int i = offset + 1; i < last; i++) {
            int digit = charArray[i] - 48;
            if (digit < 0) {
                Log.e(TAG, "digit = " + digit);
                return -1;
            }
            id = (id + digit) * 10;
        }
        return id / 10;
    }

    public static boolean setDecoEmojiInfo(Cursor cursor, EmojiAssist.DecoEmojiTextInfo decoEmojiInfo) {
        if (cursor == null || decoEmojiInfo == null) {
            return false;
        }
        int uriIndex = cursor.getColumnIndex("uri");
        int widthIndex = cursor.getColumnIndex(DecoEmojiContract.DecoEmojiInfoColumns.WIDTH);
        int heightIndex = cursor.getColumnIndex(DecoEmojiContract.DecoEmojiInfoColumns.HEIGHT);
        int kindIndex = cursor.getColumnIndex(DecoEmojiContract.DecoEmojiInfoColumns.KIND);
        int popIndex = cursor.getColumnIndex(DecoEmojiContract.DecoEmojiInfoColumns.DECOME_POP_FLAG);
        if (uriIndex < 0 || widthIndex < 0 || heightIndex < 0 || kindIndex < 0 || popIndex < 0) {
            return false;
        }
        decoEmojiInfo.setUri(cursor.getString(uriIndex));
        decoEmojiInfo.setWidth(cursor.getInt(widthIndex));
        decoEmojiInfo.setHeight(cursor.getInt(heightIndex));
        decoEmojiInfo.setKind(cursor.getInt(kindIndex));
        return true;
    }

    private static EmojiAssist.DecoEmojiTextInfo getDecoEmojiInfo(int decoEmojiId, int[] status) {
        EmojiAssist.DecoEmojiTextInfo info = new EmojiAssist.DecoEmojiTextInfo();
        if (decoEmojiId < 0) {
            status[0] = -101;
        } else {
            Context context = IWnnIME.getContext();
            if (context != null) {
                Cursor cursor = null;
                try {
                    try {
                        Cursor cursor2 = context.getContentResolver().query(ContentUris.withAppendedId(DecoEmojiContract.CONTENT_DECOINFOLIST_URI, decoEmojiId), SELECTION_DECOEMOJI_INFO, null, null, null);
                        if (cursor2 != null && cursor2.getCount() > 0) {
                            cursor2.moveToFirst();
                            setDecoEmojiInfo(cursor2, info);
                        } else {
                            status[0] = 0;
                        }
                        if (cursor2 != null) {
                            cursor2.close();
                        }
                    } catch (SQLiteException e) {
                        e.printStackTrace();
                        if (0 != 0) {
                            cursor.close();
                        }
                    }
                } catch (Throwable th) {
                    if (0 != 0) {
                        cursor.close();
                    }
                    throw th;
                }
            }
        }
        return info;
    }

    public static CharSequence convertComposingToCommitText(ComposingText composingText, int targetLayer, int from, int to) {
        CharSequence composingString = composingText.toString(targetLayer, from, to);
        if (targetLayer == 2) {
            return getSpannedText(composingString);
        }
        return composingString;
    }

    public static boolean isDecoEmoji(CharSequence text) {
        if (text.toString().indexOf(27) >= 0) {
            return true;
        }
        return false;
    }

    public static void getDecoEmojiDicInfo(int decoemoji_id, String componentName) {
        iWnnEngine engine;
        Context context = IWnnIME.getContext();
        if (context != null) {
            if (IWnnIME.getCurrentIme() == null) {
                engine = iWnnEngine.getEngineForService(componentName);
            } else {
                engine = iWnnEngine.getEngine();
            }
            if (engine == null || engine.checkDecoemojiDicset() >= 0) {
                int timestamp = 0;
                Cursor tCursor = null;
                Cursor dDursor = null;
                Cursor cursor = null;
                if (decoemoji_id > -1) {
                    try {
                        try {
                            String[] tColumn = {"timestamp"};
                            String tSelection = "decoemoji_id = " + decoemoji_id;
                            tCursor = context.getContentResolver().query(DecoEmojiContract.CONTENT_DECOINFOLIST_URI, tColumn, tSelection, null, null);
                            if (tCursor != null && tCursor.getCount() > 0) {
                                tCursor.moveToNext();
                                timestamp = tCursor.getInt(tCursor.getColumnIndex("timestamp"));
                            }
                        } catch (SQLiteException e) {
                            e.printStackTrace();
                            if (tCursor != null) {
                                tCursor.close();
                            }
                            if (dDursor != null) {
                                dDursor.close();
                            }
                            if (cursor != null) {
                                cursor.close();
                                return;
                            }
                            return;
                        }
                    } catch (Throwable th) {
                        if (tCursor != null) {
                            tCursor.close();
                        }
                        if (dDursor != null) {
                            dDursor.close();
                        }
                        if (cursor != null) {
                            cursor.close();
                        }
                        throw th;
                    }
                }
                String[] dColumn = {"decoemoji_id"};
                String dSelection = "( decoemoji_id > " + decoemoji_id + " or timestamp > " + timestamp + " )";
                dDursor = context.getContentResolver().query(DecoEmojiContract.CONTENT_DECOINFOLIST_URI, dColumn, dSelection, null, "decoemoji_id");
                if (dDursor != null && dDursor.getCount() > 0) {
                    dDursor.moveToFirst();
                    int startId = dDursor.getInt(dDursor.getColumnIndex("decoemoji_id"));
                    dDursor.moveToLast();
                    int endId = dDursor.getInt(dDursor.getColumnIndex("decoemoji_id"));
                    String[] column = {"decoemoji_id", DecoEmojiContract.DecoEmojiDicColumns.DECOEMOJI_NAME, DecoEmojiContract.DecoEmojiDicColumns.DECOEMOJI_PART, DecoEmojiContract.DecoEmojiDicColumns.DECOEMOJI_NOTE};
                    String selection = "decoemoji_id >= " + startId + " and decoemoji_id <= " + endId;
                    cursor = context.getContentResolver().query(DecoEmojiContract.CONTENT_DECODICLIST_URI, column, selection, null, "decoemoji_id ASC");
                    if (cursor != null && cursor.getCount() > 0) {
                        List<DecoEmojiAttrInfo> decoList = new ArrayList<>();
                        int preId = -1;
                        int dicCnt = 0;
                        DecoEmojiAttrInfo attrinfo = null;
                        while (cursor.moveToNext()) {
                            int id = cursor.getInt(cursor.getColumnIndex("decoemoji_id"));
                            String name = cursor.getString(cursor.getColumnIndex(DecoEmojiContract.DecoEmojiDicColumns.DECOEMOJI_NAME));
                            int partId = cursor.getInt(cursor.getColumnIndex(DecoEmojiContract.DecoEmojiDicColumns.DECOEMOJI_PART));
                            String note = cursor.getString(cursor.getColumnIndex(DecoEmojiContract.DecoEmojiDicColumns.DECOEMOJI_NOTE));
                            if (id != preId) {
                                if (decoList.size() >= 99) {
                                    break;
                                }
                                if (attrinfo != null) {
                                    decoList.add(attrinfo);
                                }
                                attrinfo = new DecoEmojiAttrInfo();
                                dicCnt = 0;
                            } else {
                                dicCnt++;
                            }
                            if (dicCnt < 10) {
                                if (attrinfo != null) {
                                    attrinfo.setID(id);
                                    attrinfo.setName(dicCnt, name);
                                    Integer part = new Integer(partId);
                                    attrinfo.setPart(dicCnt, part.byteValue());
                                    attrinfo.setNote(dicCnt, note);
                                }
                                preId = id;
                            }
                        }
                        if (attrinfo != null) {
                            decoList.add(attrinfo);
                        }
                        Intent intent = new Intent(context.getPackageName());
                        Bundle bundle = new Bundle();
                        bundle.putParcelableArrayList(IDecoEmojiConstant.BROADCAST_DATA_TAG, (ArrayList) decoList);
                        bundle.putInt(IDecoEmojiConstant.BROADCAST_TYPE_TAG, 0);
                        intent.putExtras(bundle);
                        context.sendBroadcast(intent);
                    }
                }
                if (tCursor != null) {
                    tCursor.close();
                }
                if (dDursor != null) {
                    dDursor.close();
                }
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
    }

    public static int getEmojiType(int decoEmojiId) {
        Context context;
        int type = 0;
        if (decoEmojiId >= 0 && (context = IWnnIME.getContext()) != null) {
            Cursor cursor = null;
            type = 0;
            try {
                try {
                    cursor = context.getContentResolver().query(ContentUris.withAppendedId(DecoEmojiContract.CONTENT_DECOINFOLIST_URI, decoEmojiId), null, null, null, null);
                    if (cursor != null && cursor.getCount() > 0) {
                        cursor.moveToFirst();
                        type = cursor.getInt(cursor.getColumnIndex(DecoEmojiContract.DecoEmojiInfoColumns.KIND));
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
        return type;
    }

    public static int getEditorEmojiType() {
        int emojiType = IWnnIME.getEmojiType();
        if (emojiType == 0 || emojiType > 15) {
            return 1;
        }
        int emojiType2 = emojiType & 15;
        if (emojiType2 == 0) {
            return -1;
        }
        return emojiType2;
    }
}
