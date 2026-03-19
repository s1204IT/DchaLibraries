package android.support.v4.content.res;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.res.FontResourcesParserCompat;
import android.support.v4.graphics.TypefaceCompat;
import android.util.Log;
import android.util.TypedValue;
import java.io.IOException;
import org.xmlpull.v1.XmlPullParserException;

public final class ResourcesCompat {
    public static Drawable getDrawable(Resources res, int id, Resources.Theme theme) throws Resources.NotFoundException {
        if (Build.VERSION.SDK_INT >= 21) {
            return res.getDrawable(id, theme);
        }
        return res.getDrawable(id);
    }

    public static abstract class FontCallback {
        public abstract void onFontRetrievalFailed(int i);

        public abstract void onFontRetrieved(Typeface typeface);

        public final void callbackSuccessAsync(final Typeface typeface, Handler handler) {
            if (handler == null) {
                handler = new Handler(Looper.getMainLooper());
            }
            handler.post(new Runnable() {
                @Override
                public void run() {
                    FontCallback.this.onFontRetrieved(typeface);
                }
            });
        }

        public final void callbackFailAsync(final int reason, Handler handler) {
            if (handler == null) {
                handler = new Handler(Looper.getMainLooper());
            }
            handler.post(new Runnable() {
                @Override
                public void run() {
                    FontCallback.this.onFontRetrievalFailed(reason);
                }
            });
        }
    }

    public static Typeface getFont(Context context, int id, TypedValue value, int style, FontCallback fontCallback) throws Resources.NotFoundException {
        if (context.isRestricted()) {
            return null;
        }
        return loadFont(context, id, value, style, fontCallback, null, true);
    }

    private static Typeface loadFont(Context context, int id, TypedValue value, int style, FontCallback fontCallback, Handler handler, boolean isRequestFromLayoutInflator) {
        Resources resources = context.getResources();
        resources.getValue(id, value, true);
        Typeface typeface = loadFont(context, resources, value, id, style, fontCallback, handler, isRequestFromLayoutInflator);
        if (typeface == null && fontCallback == null) {
            throw new Resources.NotFoundException("Font resource ID #0x" + Integer.toHexString(id) + " could not be retrieved.");
        }
        return typeface;
    }

    private static Typeface loadFont(Context context, Resources wrapper, TypedValue value, int id, int style, FontCallback fontCallback, Handler handler, boolean isRequestFromLayoutInflator) {
        int i;
        if (value.string == null) {
            throw new Resources.NotFoundException("Resource \"" + wrapper.getResourceName(id) + "\" (" + Integer.toHexString(id) + ") is not a Font: " + value);
        }
        String file = value.string.toString();
        if (!file.startsWith("res/")) {
            if (fontCallback != null) {
                fontCallback.callbackFailAsync(-3, handler);
            }
            return null;
        }
        Typeface typeface = TypefaceCompat.findFromCache(wrapper, id, style);
        if (typeface != null) {
            if (fontCallback != null) {
                fontCallback.callbackSuccessAsync(typeface, handler);
            }
            return typeface;
        }
        try {
            if (file.toLowerCase().endsWith(".xml")) {
                try {
                    XmlResourceParser rp = wrapper.getXml(id);
                    FontResourcesParserCompat.FamilyResourceEntry familyEntry = FontResourcesParserCompat.parse(rp, wrapper);
                    if (familyEntry != null) {
                        i = -3;
                        try {
                            return TypefaceCompat.createFromResourcesFamilyXml(context, familyEntry, wrapper, id, style, fontCallback, handler, isRequestFromLayoutInflator);
                        } catch (IOException e) {
                            e = e;
                            Log.e("ResourcesCompat", "Failed to read xml resource " + file, e);
                            if (fontCallback != null) {
                            }
                            return null;
                        } catch (XmlPullParserException e2) {
                            e = e2;
                            Log.e("ResourcesCompat", "Failed to parse xml resource " + file, e);
                            if (fontCallback != null) {
                            }
                            return null;
                        }
                    }
                    try {
                        Log.e("ResourcesCompat", "Failed to find font-family tag");
                        if (fontCallback != null) {
                            fontCallback.callbackFailAsync(-3, handler);
                        }
                        return null;
                    } catch (IOException e3) {
                        e = e3;
                        i = -3;
                        Log.e("ResourcesCompat", "Failed to read xml resource " + file, e);
                        if (fontCallback != null) {
                        }
                        return null;
                    } catch (XmlPullParserException e4) {
                        e = e4;
                        i = -3;
                        Log.e("ResourcesCompat", "Failed to parse xml resource " + file, e);
                        if (fontCallback != null) {
                        }
                        return null;
                    }
                } catch (IOException e5) {
                    e = e5;
                    i = -3;
                } catch (XmlPullParserException e6) {
                    e = e6;
                    i = -3;
                }
            } else {
                i = -3;
                try {
                    Typeface typeface2 = TypefaceCompat.createFromResourcesFontFile(context, wrapper, id, file, style);
                    if (fontCallback != null) {
                        try {
                            if (typeface2 != null) {
                                fontCallback.callbackSuccessAsync(typeface2, handler);
                            } else {
                                fontCallback.callbackFailAsync(-3, handler);
                            }
                        } catch (IOException e7) {
                            e = e7;
                            Log.e("ResourcesCompat", "Failed to read xml resource " + file, e);
                            if (fontCallback != null) {
                            }
                            return null;
                        } catch (XmlPullParserException e8) {
                            e = e8;
                            Log.e("ResourcesCompat", "Failed to parse xml resource " + file, e);
                            if (fontCallback != null) {
                            }
                            return null;
                        }
                    }
                    return typeface2;
                } catch (IOException e9) {
                    e = e9;
                } catch (XmlPullParserException e10) {
                    e = e10;
                }
            }
        } catch (IOException e11) {
            e = e11;
            i = -3;
        } catch (XmlPullParserException e12) {
            e = e12;
            i = -3;
        }
        if (fontCallback != null) {
            fontCallback.callbackFailAsync(i, handler);
        }
        return null;
    }
}
