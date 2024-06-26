package com.google.common.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
@Target({ElementType.ANNOTATION_TYPE, ElementType.CONSTRUCTOR, ElementType.FIELD, ElementType.METHOD, ElementType.TYPE})
@GwtCompatible
@Documented
@Retention(RetentionPolicy.CLASS)
/* loaded from: a.zip:com/google/common/annotations/Beta.class */
public @interface Beta {
}
