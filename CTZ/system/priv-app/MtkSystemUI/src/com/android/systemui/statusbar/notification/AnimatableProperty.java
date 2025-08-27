package com.android.systemui.statusbar.notification;

import android.util.FloatProperty;
import android.util.Property;
import android.view.View;
import com.android.systemui.R;
import java.util.function.BiConsumer;
import java.util.function.Function;

/* loaded from: classes.dex */
public abstract class AnimatableProperty {
    public static final AnimatableProperty X = from(View.X, R.id.x_animator_tag, R.id.x_animator_tag_start_value, R.id.x_animator_tag_end_value);
    public static final AnimatableProperty Y = from(View.Y, R.id.y_animator_tag, R.id.y_animator_tag_start_value, R.id.y_animator_tag_end_value);

    public abstract int getAnimationEndTag();

    public abstract int getAnimationStartTag();

    public abstract int getAnimatorTag();

    public abstract Property getProperty();

    public static <T extends View> AnimatableProperty from(String str, final BiConsumer<T, Float> biConsumer, final Function<T, Float> function, final int i, final int i2, final int i3) {
        final Property property = new FloatProperty<T>(str) { // from class: com.android.systemui.statusbar.notification.AnimatableProperty.1
            /* JADX DEBUG: Method merged with bridge method: get(Ljava/lang/Object;)Ljava/lang/Object; */
            /* JADX WARN: Incorrect types in method signature: (TT;)Ljava/lang/Float; */
            @Override // android.util.Property
            public Float get(View view) {
                return (Float) function.apply(view);
            }

            /* JADX DEBUG: Method merged with bridge method: setValue(Ljava/lang/Object;F)V */
            /* JADX WARN: Incorrect types in method signature: (TT;F)V */
            @Override // android.util.FloatProperty
            public void setValue(View view, float f) {
                biConsumer.accept(view, Float.valueOf(f));
            }
        };
        return new AnimatableProperty() { // from class: com.android.systemui.statusbar.notification.AnimatableProperty.2
            @Override // com.android.systemui.statusbar.notification.AnimatableProperty
            public int getAnimationStartTag() {
                return i2;
            }

            @Override // com.android.systemui.statusbar.notification.AnimatableProperty
            public int getAnimationEndTag() {
                return i3;
            }

            @Override // com.android.systemui.statusbar.notification.AnimatableProperty
            public int getAnimatorTag() {
                return i;
            }

            @Override // com.android.systemui.statusbar.notification.AnimatableProperty
            public Property getProperty() {
                return property;
            }
        };
    }

    public static <T extends View> AnimatableProperty from(final Property<T, Float> property, final int i, final int i2, final int i3) {
        return new AnimatableProperty() { // from class: com.android.systemui.statusbar.notification.AnimatableProperty.3
            @Override // com.android.systemui.statusbar.notification.AnimatableProperty
            public int getAnimationStartTag() {
                return i2;
            }

            @Override // com.android.systemui.statusbar.notification.AnimatableProperty
            public int getAnimationEndTag() {
                return i3;
            }

            @Override // com.android.systemui.statusbar.notification.AnimatableProperty
            public int getAnimatorTag() {
                return i;
            }

            @Override // com.android.systemui.statusbar.notification.AnimatableProperty
            public Property getProperty() {
                return property;
            }
        };
    }
}
