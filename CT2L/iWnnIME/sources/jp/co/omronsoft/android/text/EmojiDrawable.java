package jp.co.omronsoft.android.text;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Movie;
import android.graphics.Paint;
import android.os.Handler;
import android.text.TextPaint;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import jp.co.omronsoft.android.emoji.AnimatedEmojiFactory;

public class EmojiDrawable {
    public static final int ANIMATION_INFINITE = -999;
    public static final int ANIMATION_OFF = 0;
    private static final int BASE_CACHE_SIZE = 1;
    private static final int CALLBACK_MESSAGE = 999;
    private static final boolean DEBUG_EMOJI = false;
    public static final int DECOME_POP_FLAG_FALSE = 0;
    public static final int DECOME_POP_FLAG_TRUE = 1;
    public static final int DECO_EMOJI_BASE_PIXEL = 20;
    public static final int DECO_EMOJI_TYPE_PICTURE = 4;
    public static final int DECO_EMOJI_TYPE_RECT = 3;
    public static final int DECO_EMOJI_TYPE_SQ = 2;
    public static final int DECO_EMOJI_TYPE_SQ20 = 1;
    private static final String EMOJI_LIBRARY_PATH = "/data/data/jp.co.omronsoft.android.decoemojimanager/lib/";
    private static final int FILE_MAXSIZE = 2097152;
    private static final int GIF_CHECK_NUM = 4;
    private static final int JPG_CHECK_NUM = 2;
    private static final float MAX_FONT_SIZE = 65535.0f;
    private static final int MAX_MSG_COUNT = 9999;
    private static final int PNG_CHECK_NUM = 8;
    private static final int POP_ALPHA = 230;
    private static final String TAG = "EmojiDrawable";
    private static boolean sHasLoadedLibrary;
    private Context mContext;
    private static final char JPG_START_CHAR = 255;
    private static final char[] JPG_HEADER = {JPG_START_CHAR, 216};
    private static final char GIF_START_CHAR = 'G';
    private static final char[] GIF_HEADER = {GIF_START_CHAR, 'I', 'F', '8'};
    private static final char[] PNG_HEADER = {137, 'P', 'N', GIF_START_CHAR, '\r', '\n', 26, '\n'};
    private static CustomLinkedHashMap<EmojiCacheKey, WeakReference<Movie>> sWeakEmojiCache_ics = null;
    private static CustomLinkedHashMap<EmojiCacheKey, WeakReference<jp.co.omronsoft.android.emoji.Movie>> sWeakEmojiCache_gb = null;
    private static CustomLinkedHashMap<EmojiCacheKey, Bitmap> sWeakBitmapCache = null;
    private static CustomLinkedHashMap<EmojiCacheKey, WeakReference<Object>> sWeakDataCache = null;
    private static final AnimatedEmojiFactory sFactory = null;
    private static int sDuration = 600;
    private static int[] sEmojiArray = null;
    private static int[][] sFontSizeConvArray = null;
    private static EmojiCacheKey searchKey = new EmojiCacheKey();
    private static Paint sTmpPaint = new Paint();
    private static TextPaint sTmpTextPaint = new TextPaint();
    private static Paint.FontMetricsInt sTmpFontMetricsInt = new Paint.FontMetricsInt();
    private static String sEmojiLibraryPath = null;
    private int mCode = -1;
    private Canvas mCanvas = null;
    private Handler mHandler = null;
    private int mWhat = 0;
    private float mWidth = 0.0f;
    private boolean mIsStartedCallback = DEBUG_EMOJI;
    private long mSavedCurrentTime = 0;
    private String mText = null;
    private int mMaxMsgCount = 0;
    private int mSentMsgCounter = 0;
    private int mLoopCount = ANIMATION_INFINITE;
    private HashMap<Integer, Integer> mLastFrame = new HashMap<>();
    private boolean mDisplayPop = DEBUG_EMOJI;
    private boolean mPictureScale = DEBUG_EMOJI;
    private Bitmap mPopBitmapScale = null;
    private Handler handler = null;

    public static final native String getEmojiLibraryPath();

    private static class CustomLinkedHashMap<K, V> extends LinkedHashMap<K, V> {
        public int mCacheSize = 800;

        @Override
        protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            return EmojiDrawable.DEBUG_EMOJI;
        }
    }

    public EmojiDrawable() {
    }

    public EmojiDrawable(Context context) {
    }

    public EmojiDrawable(Canvas canvas) {
    }

    public static boolean isEmoji(int code) {
        return DEBUG_EMOJI;
    }

    public static boolean isEmoji(CharSequence text) {
        return DEBUG_EMOJI;
    }

    public static float getEmojiWidth(int code, Paint paint) {
        return 0.0f;
    }

    public static int getTextWidths(CharSequence text, TextPaint paint, float[] widths) {
        return 0;
    }

    public static boolean isAnimatedEmoji(int code) {
        return DEBUG_EMOJI;
    }

    public void setCallbackDrawing(Handler handler, int what) {
    }

    public void startCallbackDrawing() {
    }

    public void stopCallbackDrawing() {
    }

    public void drawEmoji(float x, float y, Paint paint) {
    }

    public void drawEmoji(float x, float y, float textSize, float textScaleX, Paint.FontMetricsInt fm) {
    }

    public int drawString(float x, float y, Paint paint, float maxWidth) {
        return 0;
    }

    private float drawCharInString(float x, float y, float nextX, char c, float maxWidth, Canvas canvas, Paint paint) {
        return 0.0f;
    }

    private float drawEmojiInString(float x, float y, float nextX, int code, float maxWidth, Canvas canvas, float textSize, float textScaleX, Paint.FontMetricsInt fm) {
        return 0.0f;
    }

    public void setCanvas(Canvas canvas) {
    }

    public void setAnimationLoopCount(int cnt) {
    }

    public void setEmoji(int code) {
    }

    public void setString(String text) {
    }

    public float getDrawnWidth() {
        return 0.0f;
    }

    public void setEmojiCacheSize(int size) {
    }

    public void drawDecoEmoji(float x, float y, String uri, Canvas canvas, Paint paint, Context context, int width, int height, int kind, Bitmap popImage) {
    }

    private void drawPop(Canvas canvas, Paint paint, Bitmap popImage, int width, int height, int x, int y, float scaleX, float scaleY) {
    }

    public static boolean isLoadedLibrary() {
        return DEBUG_EMOJI;
    }

    public static byte[] getEmojiGifData(int code, float textsize) {
        return sFactory.getImageBinary(0, 0.0f);
    }

    public void loadDisplayPop(boolean display) {
        this.mDisplayPop = display;
    }

    public void setPictureScale(boolean scale) {
        this.mPictureScale = scale;
    }

    public boolean getPictureScale() {
        return this.mPictureScale;
    }
}
