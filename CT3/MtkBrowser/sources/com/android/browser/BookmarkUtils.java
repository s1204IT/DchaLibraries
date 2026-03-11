package com.android.browser;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.graphics.drawable.PaintDrawable;
import android.net.Uri;
import android.os.Message;
import com.android.browser.provider.BrowserContract;

public class BookmarkUtils {

    enum BookmarkIconType {
        ICON_INSTALLABLE_WEB_APP,
        ICON_HOME_SHORTCUT,
        ICON_WIDGET;

        public static BookmarkIconType[] valuesCustom() {
            return values();
        }
    }

    static Bitmap createIcon(Context context, Bitmap touchIcon, Bitmap favicon, BookmarkIconType type) {
        ActivityManager am = (ActivityManager) context.getSystemService("activity");
        int iconDimension = am.getLauncherLargeIconSize();
        int iconDensity = am.getLauncherLargeIconDensity();
        return createIcon(context, touchIcon, favicon, type, iconDimension, iconDensity);
    }

    static Drawable createListFaviconBackground(Context context) {
        PaintDrawable faviconBackground = new PaintDrawable();
        Resources res = context.getResources();
        int padding = res.getDimensionPixelSize(R.dimen.list_favicon_padding);
        faviconBackground.setPadding(padding, padding, padding, padding);
        faviconBackground.getPaint().setColor(context.getResources().getColor(R.color.bookmarkListFaviconBackground));
        faviconBackground.setCornerRadius(res.getDimension(R.dimen.list_favicon_corner_radius));
        return faviconBackground;
    }

    private static Bitmap createIcon(Context context, Bitmap touchIcon, Bitmap favicon, BookmarkIconType type, int iconDimension, int iconDensity) {
        Bitmap bm = Bitmap.createBitmap(iconDimension, iconDimension, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bm);
        Rect iconBounds = new Rect(0, 0, bm.getWidth(), bm.getHeight());
        if (touchIcon != null) {
            drawTouchIconToCanvas(touchIcon, canvas, iconBounds);
        } else {
            Bitmap icon = getIconBackground(context, type, iconDensity);
            if (icon != null) {
                Paint p = new Paint(3);
                canvas.drawBitmap(icon, (Rect) null, iconBounds, p);
            }
            if (favicon != null) {
                drawFaviconToCanvas(context, favicon, canvas, iconBounds, type);
            }
        }
        canvas.setBitmap(null);
        return bm;
    }

    static Intent createAddToHomeIntent(Context context, String url, String title, Bitmap touchIcon, Bitmap favicon) {
        Intent i = new Intent("com.android.launcher.action.INSTALL_SHORTCUT");
        Intent shortcutIntent = createShortcutIntent(url);
        i.putExtra("android.intent.extra.shortcut.INTENT", shortcutIntent);
        i.putExtra("android.intent.extra.shortcut.NAME", title);
        i.putExtra("android.intent.extra.shortcut.ICON", createIcon(context, touchIcon, favicon, BookmarkIconType.ICON_HOME_SHORTCUT));
        i.putExtra("duplicate", false);
        return i;
    }

    static Intent createShortcutIntent(String url) {
        Intent shortcutIntent = new Intent("android.intent.action.VIEW", Uri.parse(url));
        long urlHash = url.hashCode();
        long uniqueId = (urlHash << 32) | ((long) shortcutIntent.hashCode());
        shortcutIntent.putExtra("com.android.browser.application_id", Long.toString(uniqueId));
        return shortcutIntent;
    }

    private static Bitmap getIconBackground(Context context, BookmarkIconType type, int density) {
        if (type == BookmarkIconType.ICON_HOME_SHORTCUT) {
            Drawable drawable = context.getResources().getDrawableForDensity(R.mipmap.ic_launcher_shortcut_browser_bookmark, density);
            if (drawable instanceof BitmapDrawable) {
                BitmapDrawable bd = (BitmapDrawable) drawable;
                return bd.getBitmap();
            }
            return null;
        }
        if (type == BookmarkIconType.ICON_INSTALLABLE_WEB_APP) {
            Drawable drawable2 = context.getResources().getDrawableForDensity(R.mipmap.ic_launcher_browser, density);
            if (drawable2 instanceof BitmapDrawable) {
                BitmapDrawable bd2 = (BitmapDrawable) drawable2;
                return bd2.getBitmap();
            }
            return null;
        }
        return null;
    }

    private static void drawTouchIconToCanvas(Bitmap touchIcon, Canvas canvas, Rect iconBounds) {
        Rect src = new Rect(0, 0, touchIcon.getWidth(), touchIcon.getHeight());
        Paint paint = new Paint(1);
        paint.setFilterBitmap(true);
        canvas.drawBitmap(touchIcon, src, iconBounds, paint);
        Path path = new Path();
        path.setFillType(Path.FillType.INVERSE_WINDING);
        RectF rect = new RectF(iconBounds);
        rect.inset(1.0f, 1.0f);
        path.addRoundRect(rect, 8.0f, 8.0f, Path.Direction.CW);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        canvas.drawPath(path, paint);
    }

    private static void drawFaviconToCanvas(Context context, Bitmap favicon, Canvas canvas, Rect iconBounds, BookmarkIconType type) {
        int faviconPaddedRectDimension;
        Paint p = new Paint(3);
        p.setStyle(Paint.Style.FILL_AND_STROKE);
        if (type == BookmarkIconType.ICON_WIDGET) {
            p.setColor(context.getResources().getColor(R.color.bookmarkWidgetFaviconBackground));
        } else {
            p.setColor(-1);
        }
        int faviconDimension = context.getResources().getDimensionPixelSize(R.dimen.favicon_size);
        if (type == BookmarkIconType.ICON_WIDGET) {
            faviconPaddedRectDimension = canvas.getWidth();
        } else {
            faviconPaddedRectDimension = context.getResources().getDimensionPixelSize(R.dimen.favicon_padded_size);
        }
        float padding = (faviconPaddedRectDimension - faviconDimension) / 2;
        float x = iconBounds.exactCenterX() - (faviconPaddedRectDimension / 2);
        float y = iconBounds.exactCenterY() - (faviconPaddedRectDimension / 2);
        if (type != BookmarkIconType.ICON_WIDGET) {
            y -= padding;
        }
        RectF r = new RectF(x, y, faviconPaddedRectDimension + x, faviconPaddedRectDimension + y);
        canvas.drawRoundRect(r, 3.0f, 3.0f, p);
        r.inset(padding, padding);
        canvas.drawBitmap(favicon, (Rect) null, r, (Paint) null);
    }

    static Uri getBookmarksUri(Context context) {
        return BrowserContract.Bookmarks.CONTENT_URI;
    }

    static void displayRemoveBookmarkDialog(final long id, String title, final Context context, final Message msg) {
        new AlertDialog.Builder(context).setIconAttribute(android.R.attr.alertDialogIcon).setMessage(context.getString(R.string.delete_bookmark_warning, title)).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                if (msg != null) {
                    msg.sendToTarget();
                }
                final long j = id;
                final Context context2 = context;
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        Uri uri = ContentUris.withAppendedId(BrowserContract.Bookmarks.CONTENT_URI, j);
                        context2.getContentResolver().delete(uri, null, null);
                    }
                };
                new Thread(runnable).start();
            }
        }).setNegativeButton(R.string.cancel, (DialogInterface.OnClickListener) null).show();
    }

    static void displayRemoveFolderDialog(final long id, String title, final Context context, final Message msg) {
        new AlertDialog.Builder(context).setIcon(android.R.drawable.ic_dialog_alert).setMessage(context.getString(R.string.delete_folder_warning, title)).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                if (msg != null) {
                    msg.sendToTarget();
                }
                final long j = id;
                final Context context2 = context;
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        deleteFoldBookmarks(j);
                    }

                    private void deleteFoldBookmarks(long id2) {
                        ContentResolver cr = context2.getContentResolver();
                        Uri uri = BookmarkUtils.getBookmarksUri(context2);
                        Cursor cursor = cr.query(uri, new String[]{"_id"}, "parent = ? AND deleted = ?", new String[]{id2 + "", "0"}, null);
                        deleteBookmarkById(id2);
                        while (cursor.moveToNext()) {
                            deleteFoldBookmarks(cursor.getInt(0));
                        }
                        cursor.close();
                    }

                    private void deleteBookmarkById(long id2) {
                        ContentResolver cResolver = context2.getContentResolver();
                        Uri uri = ContentUris.withAppendedId(BrowserContract.Bookmarks.CONTENT_URI, id2);
                        cResolver.delete(uri, null, null);
                    }
                };
                new Thread(runnable).start();
            }
        }).setNegativeButton(R.string.cancel, (DialogInterface.OnClickListener) null).show();
    }
}
