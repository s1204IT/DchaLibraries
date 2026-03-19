package com.android.gallery3d.gadget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import com.android.gallery3d.R;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.gadget.WidgetDatabaseHelper;
import com.android.gallery3d.util.ThreadPool;
import com.mediatek.gallery3d.util.Log;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class WidgetUtils {
    public static Context sContext;
    private static int sStackPhotoWidth = 220;
    private static int sStackPhotoHeight = 170;

    public static void initialize(Context context) {
        sContext = context;
        Resources resources = context.getResources();
        sStackPhotoWidth = resources.getDimensionPixelSize(R.dimen.stack_photo_width);
        sStackPhotoHeight = resources.getDimensionPixelSize(R.dimen.stack_photo_height);
    }

    public static Bitmap createWidgetBitmap(MediaItem mediaItem) {
        if (mediaItem == null) {
            Log.d("Gallery2/WidgetUtils", "<createWidgetBitmap> image == null, return null");
            return null;
        }
        Log.d("Gallery2/WidgetUtils", "<createWidgetBitmap> decode image path = " + mediaItem.getFilePath());
        Bitmap bitmapRun = mediaItem.requestImage(1).run(ThreadPool.JOB_CONTEXT_STUB);
        if (bitmapRun == null) {
            Log.w("Gallery2/WidgetUtils", "fail to get image of " + mediaItem.toString());
            return null;
        }
        return createWidgetBitmap(bitmapRun, mediaItem.getRotation());
    }

    public static Bitmap createWidgetBitmap(Bitmap bitmap, int i) {
        float fMax;
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        if (((i / 90) & 1) == 0) {
            fMax = Math.max(sStackPhotoWidth / width, sStackPhotoHeight / height);
        } else {
            fMax = Math.max(sStackPhotoWidth / height, sStackPhotoHeight / width);
        }
        Bitmap bitmapCreateBitmap = Bitmap.createBitmap(sStackPhotoWidth, sStackPhotoHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmapCreateBitmap);
        canvas.translate(sStackPhotoWidth / 2, sStackPhotoHeight / 2);
        canvas.rotate(i);
        canvas.scale(fMax, fMax);
        canvas.drawBitmap(bitmap, (-width) / 2, (-height) / 2, new Paint(6));
        return bitmapCreateBitmap;
    }

    public static void notifyAllWidgetViewChanged() {
        Iterator<Integer> it = getAllWidgetId().iterator();
        while (it.hasNext()) {
            AppWidgetManager.getInstance(sContext).notifyAppWidgetViewDataChanged(it.next().intValue(), R.id.appwidget_stack_view);
        }
    }

    private static List<Integer> getAllWidgetId() throws Throwable {
        WidgetDatabaseHelper widgetDatabaseHelper = new WidgetDatabaseHelper(sContext);
        ArrayList arrayList = new ArrayList();
        List<WidgetDatabaseHelper.Entry> entries = widgetDatabaseHelper.getEntries(0);
        List<WidgetDatabaseHelper.Entry> entries2 = widgetDatabaseHelper.getEntries(1);
        List<WidgetDatabaseHelper.Entry> entries3 = widgetDatabaseHelper.getEntries(2);
        putIdsToList(entries, arrayList);
        putIdsToList(entries2, arrayList);
        putIdsToList(entries3, arrayList);
        return arrayList;
    }

    private static void putIdsToList(List<WidgetDatabaseHelper.Entry> list, List<Integer> list2) {
        Iterator<WidgetDatabaseHelper.Entry> it = list.iterator();
        while (it.hasNext()) {
            list2.add(Integer.valueOf(it.next().widgetId));
        }
    }
}
