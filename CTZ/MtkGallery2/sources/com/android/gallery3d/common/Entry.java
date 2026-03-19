package com.android.gallery3d.common;

import com.mediatek.gallery3d.video.BookmarkEnhance;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public abstract class Entry {
    public static final String[] ID_PROJECTION = {BookmarkEnhance.COLUMN_ID};

    @Column(BookmarkEnhance.COLUMN_ID)
    public long id = 0;

    @Target({ElementType.FIELD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Column {
        String defaultValue() default "";

        boolean fullText() default false;

        boolean indexed() default false;

        boolean unique() default false;

        String value();
    }

    @Target({ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Table {
        String value();
    }
}
