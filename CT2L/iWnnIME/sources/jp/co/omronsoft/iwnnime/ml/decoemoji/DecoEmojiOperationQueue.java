package jp.co.omronsoft.iwnnime.ml.decoemoji;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.android.common.speech.LoggingEvents;
import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import jp.co.omronsoft.android.decoemojimanager.interfacedata.DecoEmojiAttrInfo;
import jp.co.omronsoft.iwnnime.ml.DecoEmojiListener;
import jp.co.omronsoft.iwnnime.ml.IWnnIME;
import jp.co.omronsoft.iwnnime.ml.iwnn.iWnnEngine;

public class DecoEmojiOperationQueue {
    private static final int ATTRINFO_LOOP_CNT = 10;
    public static final String DECO_ID_FORMAT = "%04d";
    public static final char ESC_CODE = 27;
    private static final int MAX_OPERATION = 20000;
    private static final int MAX_YOMI_LEN = 24;
    private static final int MIN_YOMI_LEN = 1;
    private static final byte PART_CATEGORY_1 = 1;
    private static final byte PART_NONE = 0;
    private static final String TAG = "DecoEmojiOperationQueue";
    private LinkedList<DecoEmojiOperation> mOperationQueue = new LinkedList<>();
    private static boolean DEBUG = false;
    private static DecoEmojiOperationQueue sInstance = new DecoEmojiOperationQueue();
    private static String mComponentName = LoggingEvents.EXTRA_CALLING_APP_NAME;

    private DecoEmojiOperationQueue() {
    }

    public static synchronized DecoEmojiOperationQueue getInstance() {
        return sInstance;
    }

    public synchronized void enqueueOperation(DecoEmojiAttrInfo[] decoemojiattrinfo, int type, Context context, boolean isUpdatePreferenceId) throws Throwable {
        try {
            try {
                if (decoemojiattrinfo == null) {
                    DecoEmojiOperation operation = new DecoEmojiOperation(null, type, isUpdatePreferenceId);
                    updateOperationEventCache(operation, context);
                    this.mOperationQueue.add(operation);
                    iWnnEngine engine = iWnnEngine.getEngine();
                    if (engine != null) {
                        engine.enqueueOperation(operation, context);
                    }
                    for (int index = 0; index < iWnnEngine.getNumEngineForService(); index++) {
                        iWnnEngine engine2 = iWnnEngine.getEngineForService(index);
                        if (engine2 != null) {
                            engine2.enqueueOperation(operation, context);
                        }
                    }
                } else {
                    int limit = Math.min(20000 - this.mOperationQueue.size(), decoemojiattrinfo.length);
                    int i = 0;
                    DecoEmojiOperation operation2 = null;
                    while (i < limit) {
                        DecoEmojiOperation operation3 = new DecoEmojiOperation(decoemojiattrinfo[i], type, isUpdatePreferenceId);
                        updateOperationEventCache(operation3, context);
                        this.mOperationQueue.add(operation3);
                        iWnnEngine engine3 = iWnnEngine.getEngine();
                        if (engine3 != null) {
                            engine3.enqueueOperation(operation3, context);
                        }
                        for (int index2 = 0; index2 < iWnnEngine.getNumEngineForService(); index2++) {
                            iWnnEngine engine4 = iWnnEngine.getEngineForService(index2);
                            if (engine4 != null) {
                                engine4.enqueueOperation(operation3, context);
                            }
                        }
                        i++;
                        operation2 = operation3;
                    }
                }
            } catch (Throwable th) {
                th = th;
                throw th;
            }
        } catch (Throwable th2) {
            th = th2;
            throw th;
        }
    }

    public synchronized boolean executeOperation(String componentName, Context context) {
        DecoEmojiOperation operation;
        boolean z = false;
        synchronized (this) {
            mComponentName = componentName;
            iWnnEngine engine = getEngine();
            if (engine != null && engine.checkDecoemojiDicset() >= 0 && (operation = this.mOperationQueue.poll()) != null) {
                switchOperate(operation, context);
                z = true;
            }
        }
        return z;
    }

    public synchronized void clearOperation() {
        this.mOperationQueue.clear();
    }

    private static void switchOperate(DecoEmojiOperation operation, Context context) {
        if (DEBUG) {
            Log.d(TAG, "switchOperate() Start");
        }
        switch (operation.getType()) {
            case 0:
                if (DEBUG) {
                    Log.d(TAG, "switchOperate()::insertDictionary Start");
                }
                insertDictionary(operation.getDecoEmojiAttrInfo(), operation.isUpdatePreferenceId(), context);
                if (DEBUG) {
                    Log.d(TAG, "switchOperate()::insertDictionary End");
                }
                break;
            case 2:
                if (DEBUG) {
                    Log.d(TAG, "switchOperate()::deleteDictionary Start");
                }
                deleteDictionary(operation.getDecoEmojiAttrInfo());
                if (DEBUG) {
                    Log.d(TAG, "switchOperate()::deleteDictionary End");
                }
                break;
            case 4:
                if (DEBUG) {
                    Log.d(TAG, "switchOperate()::resetDictionary Start");
                }
                resetDictionary();
                if (DEBUG) {
                    Log.d(TAG, "switchOperate()::resetDictionary End");
                }
                break;
        }
        if (DEBUG) {
            Log.d(TAG, "switchOperate() End");
        }
    }

    private static void insertDictionary(DecoEmojiAttrInfo[] decoemojiattrinfo, boolean isUpdatePreferenceId, Context context) {
        SharedPreferences pref;
        if (DEBUG) {
            Log.d(TAG, "insertDictionary() Start");
        }
        if (checkUpdate()) {
            controlDecoEmojiDictionary(decoemojiattrinfo, 0);
            if (isUpdatePreferenceId) {
                int length = decoemojiattrinfo.length;
                if (length > 0 && decoemojiattrinfo[length - 1] != null) {
                    IWnnIME currentIme = IWnnIME.getCurrentIme();
                    if (currentIme != null) {
                        pref = currentIme.getSharedPreferences(IWnnIME.FILENAME_NOT_RESET_SETTINGS_PREFERENCE, 0);
                    } else if (context != null) {
                        pref = context.getSharedPreferences(IWnnIME.FILENAME_NOT_RESET_SETTINGS_PREFERENCE, 0);
                    } else {
                        return;
                    }
                    if (pref != null) {
                        SharedPreferences.Editor editor = pref.edit();
                        editor.putInt(DecoEmojiListener.PREF_KEY, decoemojiattrinfo[length - 1].getId());
                        editor.commit();
                    }
                }
            } else {
                return;
            }
        }
        if (DEBUG) {
            Log.d(TAG, "insertDictionary() End");
        }
    }

    private static void deleteDictionary(DecoEmojiAttrInfo[] decoemojiattrinfo) {
        if (DEBUG) {
            Log.d(TAG, "deleteDictionary() Start");
        }
        if (checkUpdate()) {
            controlDecoEmojiDictionary(decoemojiattrinfo, 2);
        }
        if (DEBUG) {
            Log.d(TAG, "deleteDictionary() End");
        }
    }

    private static void resetDictionary() {
        if (DEBUG) {
            Log.d(TAG, "resetDictionary() Start");
        }
        if (checkUpdate()) {
            resetDecoEmojiDictionary();
        }
        if (DEBUG) {
            Log.d(TAG, "resetDictionary() End");
        }
    }

    private static int resetDecoEmojiDictionary() {
        if (DEBUG) {
            Log.d(TAG, "resetDecoEmojiDictionary() Start");
        }
        iWnnEngine engine = getEngine();
        if (engine == null) {
            return -1;
        }
        int iResetDecoEmojiDictionary = engine.resetDecoEmojiDictionary();
        if (DEBUG) {
            Log.d(TAG, "resetDecoEmojiDictionary() End");
            return iResetDecoEmojiDictionary;
        }
        return iResetDecoEmojiDictionary;
    }

    private static void controlDecoEmojiDictionary(DecoEmojiAttrInfo[] decoemojiattrinfo, int control_flag) {
        if (DEBUG) {
            Log.d(TAG, "controlDecoEmojiDictionary() Start");
        }
        iWnnEngine engine = getEngine();
        if (engine != null) {
            for (int i = 0; i < decoemojiattrinfo.length; i++) {
                String id = ESC_CODE + String.format(DECO_ID_FORMAT, Integer.valueOf(decoemojiattrinfo[i].getId()));
                int emojiType = 0;
                try {
                    byte[] charArray = id.getBytes(IWnnIME.CHARSET_NAME_UTF8);
                    emojiType = DecoEmojiUtil.getEmojiType(DecoEmojiUtil.convertToEmojiIdInt(charArray, 0));
                } catch (Exception e) {
                    Log.e(TAG, "controlDecoEmojiDictionary() Exception", e);
                }
                if (emojiType == 1 || emojiType == 2 || emojiType == 3 || control_flag == 2) {
                    for (int j = 0; j < 10; j++) {
                        if (!isDecoEmojiAttrInfo(decoemojiattrinfo[i].getName(j), control_flag)) {
                            byte part = repreceDecoEmojiAttrInfo(decoemojiattrinfo[i].getPart(j));
                            if (DEBUG) {
                                Log.d(TAG, "id = " + id + " decoemojiattrinfo[i].getName(j) = " + decoemojiattrinfo[i].getName(j) + " decoemojiattrinfo[i].getPart(j) = " + ((int) part) + " control_flag = " + control_flag);
                            }
                            engine.controlDecoEmojiDictionary(id, decoemojiattrinfo[i].getName(j), part, control_flag);
                        }
                    }
                }
            }
            if (DEBUG) {
                Log.d(TAG, "controlDecoEmojiDictionary() End");
            }
        }
    }

    private static boolean isDecoEmojiAttrInfo(String name, int control_flag) {
        if (DEBUG) {
            Log.d(TAG, "isDecoEmojiAttrInfo() Start");
        }
        boolean ret = false;
        if (control_flag == 0) {
            if (name == null || name.length() > 24 || name.length() < 1) {
                ret = true;
            }
        } else if (control_flag == 2 && name != null && (name.length() > 24 || name.length() < 1)) {
            ret = true;
        }
        if (DEBUG) {
            Log.d(TAG, "isDecoEmojiAttrInfo() End");
        }
        return ret;
    }

    private static byte repreceDecoEmojiAttrInfo(byte part) {
        if (DEBUG) {
            Log.d(TAG, "repreceDecoEmojiAttrInfo() Start");
        }
        if (part == 0) {
            if (DEBUG) {
                Log.d(TAG, "repreceDecoEmojiAttrInfo() 0 to 1");
            }
            part = PART_CATEGORY_1;
        }
        if (DEBUG) {
            Log.d(TAG, "repreceDecoEmojiAttrInfo() End");
        }
        return part;
    }

    private static boolean checkUpdate() {
        boolean result;
        if (DEBUG) {
            Log.d(TAG, "checkUpdate() Start");
        }
        iWnnEngine engine = getEngine();
        if (engine == null) {
            return false;
        }
        int ret = engine.checkDecoEmojiDictionary();
        if (ret >= 0) {
            result = true;
        } else {
            result = false;
        }
        if (DEBUG) {
            Log.d(TAG, "checkUpdate() End");
            return result;
        }
        return result;
    }

    private static iWnnEngine getEngine() {
        return IWnnIME.getCurrentIme() == null ? iWnnEngine.getEngineForService(mComponentName) : iWnnEngine.getEngine();
    }

    private void updateOperationEventCache(DecoEmojiOperation operation, Context context) throws Throwable {
        PrintWriter writer;
        if (operation == null || context == null) {
            Log.e(TAG, "updateOperationEventCache() parameter error");
            return;
        }
        StringBuffer writeStrBuf = new StringBuffer();
        SharedPreferences pref = context.getSharedPreferences(iWnnEngine.FILENAME_DECO_OPERATION_PROCESSED_INDEX_CACHE, 0);
        SharedPreferences.Editor editor = pref.edit();
        int type = operation.getType();
        switch (type) {
            case 2:
                writeStrBuf.append(String.valueOf(type));
                writeStrBuf.append(iWnnEngine.DECO_OPERATION_SEPARATOR);
                DecoEmojiAttrInfo[] attr = operation.getDecoEmojiAttrInfo();
                if (attr.length > 0 && attr[0] != null) {
                    writeStrBuf.append(attr[0].getId());
                } else {
                    writeStrBuf.append(String.valueOf(iWnnEngine.OPERATION_ID_INIT));
                }
                break;
            case 3:
            default:
                return;
            case 4:
                writeStrBuf.append(String.valueOf(type));
                writeStrBuf.append(iWnnEngine.DECO_OPERATION_SEPARATOR);
                writeStrBuf.append(String.valueOf(iWnnEngine.OPERATION_ID_INIT));
                context.deleteFile(iWnnEngine.FILENAME_DECO_OPERATION_EVENT_CACHE);
                editor.clear();
                editor.commit();
                break;
        }
        PrintWriter writer2 = null;
        try {
            try {
                OutputStream out = context.openFileOutput(iWnnEngine.FILENAME_DECO_OPERATION_EVENT_CACHE, 32768);
                writer = new PrintWriter(new OutputStreamWriter(out, IWnnIME.CHARSET_NAME_UTF8));
            } catch (Throwable th) {
                th = th;
            }
        } catch (FileNotFoundException e) {
            e = e;
        } catch (UnsupportedEncodingException e2) {
            e = e2;
        }
        try {
            writer.println(writeStrBuf.toString());
            long count = pref.getLong(iWnnEngine.KEYNAME_DECO_OPERATION_EVENT_COUNT, 0L);
            editor.putLong(iWnnEngine.KEYNAME_DECO_OPERATION_EVENT_COUNT, count + 1);
            editor.commit();
            if (writer != null) {
                writer.close();
                writer2 = writer;
            } else {
                writer2 = writer;
            }
        } catch (FileNotFoundException e3) {
            e = e3;
            writer2 = writer;
            Log.e(TAG, "updateOperationEventCache() Exception1[" + e + "]");
            if (writer2 != null) {
                writer2.close();
            }
        } catch (UnsupportedEncodingException e4) {
            e = e4;
            writer2 = writer;
            Log.e(TAG, "updateOperationEventCache() Exception2[" + e + "]");
            if (writer2 != null) {
                writer2.close();
            }
        } catch (Throwable th2) {
            th = th2;
            writer2 = writer;
            if (writer2 != null) {
                writer2.close();
            }
            throw th;
        }
    }
}
