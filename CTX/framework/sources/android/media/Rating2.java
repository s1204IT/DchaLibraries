package android.media;

import android.media.update.ApiLoader;
import android.media.update.Rating2Provider;
import android.os.Bundle;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public final class Rating2 {
    public static final int RATING_3_STARS = 3;
    public static final int RATING_4_STARS = 4;
    public static final int RATING_5_STARS = 5;
    public static final int RATING_HEART = 1;
    public static final int RATING_NONE = 0;
    public static final int RATING_PERCENTAGE = 6;
    public static final int RATING_THUMB_UP_DOWN = 2;
    private final Rating2Provider mProvider;

    @Retention(RetentionPolicy.SOURCE)
    public @interface StarStyle {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface Style {
    }

    public Rating2(Rating2Provider rating2Provider) {
        this.mProvider = rating2Provider;
    }

    public String toString() {
        return this.mProvider.toString_impl();
    }

    public Rating2Provider getProvider() {
        return this.mProvider;
    }

    public boolean equals(Object obj) {
        return this.mProvider.equals_impl(obj);
    }

    public int hashCode() {
        return this.mProvider.hashCode_impl();
    }

    public static Rating2 fromBundle(Bundle bundle) {
        return ApiLoader.getProvider().fromBundle_Rating2(bundle);
    }

    public Bundle toBundle() {
        return this.mProvider.toBundle_impl();
    }

    public static Rating2 newUnratedRating(int i) {
        return ApiLoader.getProvider().newUnratedRating_Rating2(i);
    }

    public static Rating2 newHeartRating(boolean z) {
        return ApiLoader.getProvider().newHeartRating_Rating2(z);
    }

    public static Rating2 newThumbRating(boolean z) {
        return ApiLoader.getProvider().newThumbRating_Rating2(z);
    }

    public static Rating2 newStarRating(int i, float f) {
        return ApiLoader.getProvider().newStarRating_Rating2(i, f);
    }

    public static Rating2 newPercentageRating(float f) {
        return ApiLoader.getProvider().newPercentageRating_Rating2(f);
    }

    public boolean isRated() {
        return this.mProvider.isRated_impl();
    }

    public int getRatingStyle() {
        return this.mProvider.getRatingStyle_impl();
    }

    public boolean hasHeart() {
        return this.mProvider.hasHeart_impl();
    }

    public boolean isThumbUp() {
        return this.mProvider.isThumbUp_impl();
    }

    public float getStarRating() {
        return this.mProvider.getStarRating_impl();
    }

    public float getPercentRating() {
        return this.mProvider.getPercentRating_impl();
    }
}
