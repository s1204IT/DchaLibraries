package androidx.car.utils;

import android.view.View;
import androidx.car.R;

public class ListItemBackgroundResolver {
    public static void setBackground(View view, int currentPosition, int totalItems) {
        if (currentPosition < 0) {
            throw new IllegalArgumentException("currentPosition cannot be less than zero.");
        }
        if (currentPosition >= totalItems) {
            throw new IndexOutOfBoundsException("currentPosition: " + currentPosition + "; totalItems: " + totalItems);
        }
        if (totalItems == 1) {
            view.setBackgroundResource(R.drawable.car_card_rounded_background);
            return;
        }
        if (currentPosition == 0) {
            view.setBackgroundResource(R.drawable.car_card_rounded_top_background);
        } else if (currentPosition == totalItems - 1) {
            view.setBackgroundResource(R.drawable.car_card_rounded_bottom_background);
        } else {
            view.setBackgroundResource(R.drawable.car_card_background);
        }
    }
}
