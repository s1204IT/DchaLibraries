package org.hamcrest;

import org.hamcrest.core.AllOf;
import org.hamcrest.core.AnyOf;
import org.hamcrest.core.CombinableMatcher;
import org.hamcrest.core.DescribedAs;
import org.hamcrest.core.Every;
import org.hamcrest.core.Is;
import org.hamcrest.core.IsAnything;
import org.hamcrest.core.IsCollectionContaining;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsInstanceOf;
import org.hamcrest.core.IsNot;
import org.hamcrest.core.IsNull;
import org.hamcrest.core.IsSame;
import org.hamcrest.core.StringContains;
import org.hamcrest.core.StringEndsWith;
import org.hamcrest.core.StringStartsWith;

public class CoreMatchers {
    public static <T> Matcher<T> allOf(Iterable<Matcher<? super T>> iterable) {
        return AllOf.allOf(iterable);
    }

    @SafeVarargs
    public static <T> Matcher<T> allOf(Matcher<? super T>... matcherArr) {
        return AllOf.allOf(matcherArr);
    }

    public static <T> AnyOf<T> anyOf(Iterable<Matcher<? super T>> iterable) {
        return AnyOf.anyOf(iterable);
    }

    @SafeVarargs
    public static <T> AnyOf<T> anyOf(Matcher<? super T>... matcherArr) {
        return AnyOf.anyOf(matcherArr);
    }

    public static <LHS> CombinableMatcher.CombinableBothMatcher<LHS> both(Matcher<? super LHS> matcher) {
        return CombinableMatcher.both(matcher);
    }

    public static <LHS> CombinableMatcher.CombinableEitherMatcher<LHS> either(Matcher<? super LHS> matcher) {
        return CombinableMatcher.either(matcher);
    }

    public static <T> Matcher<T> describedAs(String str, Matcher<T> matcher, Object... objArr) {
        return DescribedAs.describedAs(str, matcher, objArr);
    }

    public static <U> Matcher<Iterable<? extends U>> everyItem(Matcher<U> matcher) {
        return Every.everyItem(matcher);
    }

    public static <T> Matcher<T> is(Matcher<T> matcher) {
        return Is.is((Matcher) matcher);
    }

    public static <T> Matcher<T> is(T t) {
        return Is.is(t);
    }

    public static void is(Class<?> cls) {
    }

    public static <T> Matcher<T> isA(Class<T> cls) {
        return Is.isA(cls);
    }

    public static Matcher<Object> anything() {
        return IsAnything.anything();
    }

    public static Matcher<Object> anything(String str) {
        return IsAnything.anything(str);
    }

    public static <T> Matcher<Iterable<? super T>> hasItem(Matcher<? super T> matcher) {
        return IsCollectionContaining.hasItem((Matcher) matcher);
    }

    public static <T> Matcher<Iterable<? super T>> hasItem(T t) {
        return IsCollectionContaining.hasItem(t);
    }

    @SafeVarargs
    public static <T> Matcher<Iterable<T>> hasItems(Matcher<? super T>... matcherArr) {
        return IsCollectionContaining.hasItems((Matcher[]) matcherArr);
    }

    @SafeVarargs
    public static <T> Matcher<Iterable<T>> hasItems(T... tArr) {
        return IsCollectionContaining.hasItems(tArr);
    }

    public static <T> Matcher<T> equalTo(T t) {
        return IsEqual.equalTo(t);
    }

    public static Matcher<Object> equalToObject(Object obj) {
        return IsEqual.equalToObject(obj);
    }

    public static <T> Matcher<T> any(Class<T> cls) {
        return IsInstanceOf.any(cls);
    }

    public static <T> Matcher<T> instanceOf(Class<?> cls) {
        return IsInstanceOf.instanceOf(cls);
    }

    public static <T> Matcher<T> not(Matcher<T> matcher) {
        return IsNot.not((Matcher) matcher);
    }

    public static <T> Matcher<T> not(T t) {
        return IsNot.not(t);
    }

    public static Matcher<Object> notNullValue() {
        return IsNull.notNullValue();
    }

    public static <T> Matcher<T> notNullValue(Class<T> cls) {
        return IsNull.notNullValue(cls);
    }

    public static Matcher<Object> nullValue() {
        return IsNull.nullValue();
    }

    public static <T> Matcher<T> nullValue(Class<T> cls) {
        return IsNull.nullValue(cls);
    }

    public static <T> Matcher<T> sameInstance(T t) {
        return IsSame.sameInstance(t);
    }

    public static <T> Matcher<T> theInstance(T t) {
        return IsSame.theInstance(t);
    }

    public static Matcher<String> containsString(String str) {
        return StringContains.containsString(str);
    }

    public static Matcher<String> containsStringIgnoringCase(String str) {
        return StringContains.containsStringIgnoringCase(str);
    }

    public static Matcher<String> startsWith(String str) {
        return StringStartsWith.startsWith(str);
    }

    public static Matcher<String> startsWithIgnoringCase(String str) {
        return StringStartsWith.startsWithIgnoringCase(str);
    }

    public static Matcher<String> endsWith(String str) {
        return StringEndsWith.endsWith(str);
    }

    public static Matcher<String> endsWithIgnoringCase(String str) {
        return StringEndsWith.endsWithIgnoringCase(str);
    }
}
