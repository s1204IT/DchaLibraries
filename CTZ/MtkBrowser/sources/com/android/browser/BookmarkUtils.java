package com.android.browser;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.graphics.drawable.PaintDrawable;
import android.net.Uri;
import android.os.Message;
import android.util.Log;
import com.android.browser.provider.BrowserContract;

public class BookmarkUtils {

    enum BookmarkIconType {
        ICON_INSTALLABLE_WEB_APP,
        ICON_HOME_SHORTCUT,
        ICON_WIDGET
    }

    static Bitmap createIcon(Context context, Bitmap bitmap, Bitmap bitmap2, BookmarkIconType bookmarkIconType) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService("activity");
        return createIcon(context, bitmap, bitmap2, bookmarkIconType, activityManager.getLauncherLargeIconSize(), activityManager.getLauncherLargeIconDensity());
    }

    static Drawable createListFaviconBackground(Context context) {
        PaintDrawable paintDrawable = new PaintDrawable();
        Resources resources = context.getResources();
        int dimensionPixelSize = resources.getDimensionPixelSize(R.dimen.list_favicon_padding);
        paintDrawable.setPadding(dimensionPixelSize, dimensionPixelSize, dimensionPixelSize, dimensionPixelSize);
        paintDrawable.getPaint().setColor(context.getResources().getColor(R.color.bookmarkListFaviconBackground));
        paintDrawable.setCornerRadius(resources.getDimension(R.dimen.list_favicon_corner_radius));
        return paintDrawable;
    }

    private static Bitmap createIcon(Context context, Bitmap bitmap, Bitmap bitmap2, BookmarkIconType bookmarkIconType, int i, int i2) {
        Bitmap bitmapCreateBitmap = Bitmap.createBitmap(i, i, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmapCreateBitmap);
        Rect rect = new Rect(0, 0, bitmapCreateBitmap.getWidth(), bitmapCreateBitmap.getHeight());
        if (bitmap != null) {
            drawTouchIconToCanvas(bitmap, canvas, rect);
        } else {
            Bitmap iconBackground = getIconBackground(context, bookmarkIconType, i2);
            if (iconBackground != null) {
                canvas.drawBitmap(iconBackground, (Rect) null, rect, new Paint(3));
            }
            if (bitmap2 != null) {
                drawFaviconToCanvas(context, bitmap2, canvas, rect, bookmarkIconType);
            }
        }
        canvas.setBitmap(null);
        return bitmapCreateBitmap;
    }

    static Intent createAddToHomeIntent(Context context, String str, String str2, Bitmap bitmap, Bitmap bitmap2) {
        Intent intent = new Intent("com.android.launcher.action.INSTALL_SHORTCUT");
        intent.putExtra("android.intent.extra.shortcut.INTENT", createShortcutIntent(str));
        intent.putExtra("android.intent.extra.shortcut.NAME", str2);
        intent.putExtra("android.intent.extra.shortcut.ICON", createIcon(context, bitmap, bitmap2, BookmarkIconType.ICON_HOME_SHORTCUT));
        intent.putExtra("duplicate", false);
        return intent;
    }

    static void createShortcutToHome(Context context, String str, String str2, Bitmap bitmap, Bitmap bitmap2) {
        ShortcutManager shortcutManager = (ShortcutManager) context.getSystemService(ShortcutManager.class);
        if (shortcutManager.isRequestPinShortcutSupported()) {
            Log.d("TestShortcut", "isRequestPinShortcutSupported true." + shortcutManager.requestPinShortcut(new ShortcutInfo.Builder(context, "bookmark" + str.hashCode()).setShortLabel(str2).setIcon(Icon.createWithBitmap(createIcon(context, bitmap, bitmap2, BookmarkIconType.ICON_HOME_SHORTCUT))).setIntent(createShortcutIntent(str)).build(), null));
            return;
        }
        Log.d("TestShortcut", "isRequestPinShortcutSupported false.");
    }

    static Intent createShortcutIntent(String str) {
        Intent intent = new Intent("android.intent.action.VIEW", Uri.parse(str));
        intent.putExtra("com.android.browser.application_id", Long.toString((((long) str.hashCode()) << 32) | ((long) intent.hashCode())));
        return intent;
    }

    private static Bitmap getIconBackground(Context context, BookmarkIconType bookmarkIconType, int i) {
        if (bookmarkIconType == BookmarkIconType.ICON_HOME_SHORTCUT) {
            ?? drawableForDensity = context.getResources().getDrawableForDensity(R.mipmap.ic_launcher_shortcut_browser_bookmark, i);
            if (drawableForDensity instanceof BitmapDrawable) {
                return drawableForDensity.getBitmap();
            }
            return null;
        }
        if (bookmarkIconType == BookmarkIconType.ICON_INSTALLABLE_WEB_APP) {
            ?? drawableForDensity2 = context.getResources().getDrawableForDensity(R.mipmap.ic_launcher_browser, i);
            if (drawableForDensity2 instanceof BitmapDrawable) {
                return drawableForDensity2.getBitmap();
            }
            return null;
        }
        return null;
    }

    private static void drawTouchIconToCanvas(Bitmap bitmap, Canvas canvas, Rect rect) {
        Rect rect2 = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        Paint paint = new Paint(1);
        paint.setFilterBitmap(true);
        canvas.drawBitmap(bitmap, rect2, rect, paint);
        Path path = new Path();
        path.setFillType(Path.FillType.INVERSE_WINDING);
        RectF rectF = new RectF(rect);
        rectF.inset(1.0f, 1.0f);
        path.addRoundRect(rectF, 8.0f, 8.0f, Path.Direction.CW);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        canvas.drawPath(path, paint);
    }

    private static void drawFaviconToCanvas(Context context, Bitmap bitmap, Canvas canvas, Rect rect, BookmarkIconType bookmarkIconType) {
        int dimensionPixelSize;
        Paint paint = new Paint(3);
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        if (bookmarkIconType == BookmarkIconType.ICON_WIDGET) {
            paint.setColor(context.getResources().getColor(R.color.bookmarkWidgetFaviconBackground));
        } else {
            paint.setColor(-1);
        }
        int dimensionPixelSize2 = context.getResources().getDimensionPixelSize(R.dimen.favicon_size);
        if (bookmarkIconType == BookmarkIconType.ICON_WIDGET) {
            dimensionPixelSize = canvas.getWidth();
        } else {
            dimensionPixelSize = context.getResources().getDimensionPixelSize(R.dimen.favicon_padded_size);
        }
        float f = (dimensionPixelSize - dimensionPixelSize2) / 2;
        float f2 = dimensionPixelSize / 2;
        float fExactCenterX = rect.exactCenterX() - f2;
        float fExactCenterY = rect.exactCenterY() - f2;
        if (bookmarkIconType != BookmarkIconType.ICON_WIDGET) {
            fExactCenterY -= f;
        }
        float f3 = dimensionPixelSize;
        RectF rectF = new RectF(fExactCenterX, fExactCenterY, fExactCenterX + f3, f3 + fExactCenterY);
        canvas.drawRoundRect(rectF, 3.0f, 3.0f, paint);
        rectF.inset(f, f);
        canvas.drawBitmap(bitmap, (Rect) null, rectF, (Paint) null);
    }

    static Uri getBookmarksUri(Context context) {
        return BrowserContract.Bookmarks.CONTENT_URI;
    }

    static void displayRemoveBookmarkDialog(final long j, String str, final Context context, final Message message) {
        new AlertDialog.Builder(context).setIconAttribute(android.R.attr.alertDialogIcon).setMessage(context.getString(R.string.delete_bookmark_warning, str)).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (message != null) {
                    message.sendToTarget();
                }
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        context.getContentResolver().delete(ContentUris.withAppendedId(BrowserContract.Bookmarks.CONTENT_URI, j), null, null);
                    }
                }).start();
            }
        }).setNegativeButton(R.string.cancel, (DialogInterface.OnClickListener) null).show();
    }

    static void displayRemoveFolderDialog(final long j, String str, final Context context, final Message message) {
        new AlertDialog.Builder(context).setIcon(android.R.drawable.ic_dialog_alert).setMessage(context.getString(R.string.delete_folder_warning, str)).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (message != null) {
                    message.sendToTarget();
                }
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        deleteFoldBookmarks(j);
                    }

                    private void deleteFoldBookmarks(long j2) {
                        Cursor cursorQuery = context.getContentResolver().query(BookmarkUtils.getBookmarksUri(context), new String[]{"_id"}, "parent = ? AND deleted = ?", new String[]{j2 + "", "0"}, null);
                        deleteBookmarkById(j2);
                        while (cursorQuery.moveToNext()) {
                            deleteFoldBookmarks(cursorQuery.getInt(0));
                        }
                        cursorQuery.close();
                    }

                    private void deleteBookmarkById(long j2) {
                        context.getContentResolver().delete(ContentUris.withAppendedId(BrowserContract.Bookmarks.CONTENT_URI, j2), null, null);
                    }
                }).start();
            }
        }).setNegativeButton(R.string.cancel, (DialogInterface.OnClickListener) null).show();
    }
}
