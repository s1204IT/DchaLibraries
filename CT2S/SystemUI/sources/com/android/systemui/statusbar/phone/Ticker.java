package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.service.notification.StatusBarNotification;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.ImageSwitcher;
import android.widget.TextSwitcher;
import android.widget.TextView;
import com.android.internal.statusbar.StatusBarIcon;
import com.android.systemui.R;
import com.android.systemui.statusbar.StatusBarIconView;
import java.util.ArrayList;

public abstract class Ticker {
    private Context mContext;
    private float mIconScale;
    private ImageSwitcher mIconSwitcher;
    private TextPaint mPaint;
    private TextSwitcher mTextSwitcher;
    private View mTickerView;
    private Handler mHandler = new Handler();
    private ArrayList<Segment> mSegments = new ArrayList<>();
    private Runnable mAdvanceTicker = new Runnable() {
        @Override
        public void run() {
            while (true) {
                if (Ticker.this.mSegments.size() <= 0) {
                    break;
                }
                Segment seg = (Segment) Ticker.this.mSegments.get(0);
                if (seg.first) {
                    Ticker.this.mIconSwitcher.setImageDrawable(seg.icon);
                }
                CharSequence text = seg.advance();
                if (text == null) {
                    Ticker.this.mSegments.remove(0);
                } else {
                    Ticker.this.mTextSwitcher.setText(text);
                    Ticker.this.scheduleAdvance();
                    break;
                }
            }
            if (Ticker.this.mSegments.size() == 0) {
                Ticker.this.tickerDone();
            }
        }
    };

    public abstract void tickerDone();

    public abstract void tickerHalting();

    public abstract void tickerStarting();

    public static boolean isGraphicOrEmoji(char c) {
        int gc = Character.getType(c);
        return (gc == 15 || gc == 16 || gc == 0 || gc == 13 || gc == 14 || gc == 12) ? false : true;
    }

    private final class Segment {
        int current;
        boolean first;
        Drawable icon;
        int next;
        StatusBarNotification notification;
        CharSequence text;

        StaticLayout getLayout(CharSequence substr) {
            int w = (Ticker.this.mTextSwitcher.getWidth() - Ticker.this.mTextSwitcher.getPaddingLeft()) - Ticker.this.mTextSwitcher.getPaddingRight();
            return new StaticLayout(substr, Ticker.this.mPaint, w, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, true);
        }

        CharSequence rtrim(CharSequence substr, int start, int end) {
            while (end > start && !Ticker.isGraphicOrEmoji(substr.charAt(end - 1))) {
                end--;
            }
            if (end > start) {
                return substr.subSequence(start, end);
            }
            return null;
        }

        CharSequence getText() {
            if (this.current > this.text.length()) {
                return null;
            }
            CharSequence substr = this.text.subSequence(this.current, this.text.length());
            StaticLayout l = getLayout(substr);
            int lineCount = l.getLineCount();
            if (lineCount > 0) {
                int start = l.getLineStart(0);
                int end = l.getLineEnd(0);
                this.next = this.current + end;
                return rtrim(substr, start, end);
            }
            throw new RuntimeException("lineCount=" + lineCount + " current=" + this.current + " text=" + ((Object) this.text));
        }

        CharSequence advance() {
            this.first = false;
            int index = this.next;
            int len = this.text.length();
            while (index < len && !Ticker.isGraphicOrEmoji(this.text.charAt(index))) {
                index++;
            }
            if (index >= len) {
                return null;
            }
            CharSequence substr = this.text.subSequence(index, this.text.length());
            StaticLayout l = getLayout(substr);
            int lineCount = l.getLineCount();
            for (int i = 0; i < lineCount; i++) {
                int start = l.getLineStart(i);
                int end = l.getLineEnd(i);
                if (i == lineCount - 1) {
                    this.next = len;
                } else {
                    this.next = l.getLineStart(i + 1) + index;
                }
                CharSequence result = rtrim(substr, start, end);
                if (result != null) {
                    this.current = index + start;
                    return result;
                }
            }
            this.current = len;
            return null;
        }

        Segment(StatusBarNotification n, Drawable icon, CharSequence text) {
            this.notification = n;
            this.icon = icon;
            this.text = text;
            int index = 0;
            int len = text.length();
            while (index < len && !Ticker.isGraphicOrEmoji(text.charAt(index))) {
                index++;
            }
            this.current = index;
            this.next = index;
            this.first = true;
        }
    }

    public Ticker(Context context, View sb) {
        this.mContext = context;
        Resources res = context.getResources();
        int outerBounds = res.getDimensionPixelSize(R.dimen.status_bar_icon_size);
        int imageBounds = res.getDimensionPixelSize(R.dimen.status_bar_icon_drawing_size);
        this.mIconScale = imageBounds / outerBounds;
        this.mTickerView = sb.findViewById(R.id.ticker);
        this.mIconSwitcher = (ImageSwitcher) sb.findViewById(R.id.tickerIcon);
        this.mIconSwitcher.setInAnimation(AnimationUtils.loadAnimation(context, android.R.anim.ft_avd_toarrow_rectangle_path_6_animation));
        this.mIconSwitcher.setOutAnimation(AnimationUtils.loadAnimation(context, android.R.anim.ft_avd_tooverflow_rectangle_1_animation));
        this.mIconSwitcher.setScaleX(this.mIconScale);
        this.mIconSwitcher.setScaleY(this.mIconScale);
        this.mTextSwitcher = (TextSwitcher) sb.findViewById(R.id.tickerText);
        this.mTextSwitcher.setInAnimation(AnimationUtils.loadAnimation(context, android.R.anim.ft_avd_toarrow_rectangle_path_6_animation));
        this.mTextSwitcher.setOutAnimation(AnimationUtils.loadAnimation(context, android.R.anim.ft_avd_tooverflow_rectangle_1_animation));
        TextView text = (TextView) this.mTextSwitcher.getChildAt(0);
        this.mPaint = text.getPaint();
    }

    public void addEntry(StatusBarNotification n) {
        int initialCount = this.mSegments.size();
        if (initialCount > 0) {
            Segment seg = this.mSegments.get(0);
            if (n.getPackageName().equals(seg.notification.getPackageName()) && n.getNotification().icon == seg.notification.getNotification().icon && n.getNotification().iconLevel == seg.notification.getNotification().iconLevel && charSequencesEqual(seg.notification.getNotification().tickerText, n.getNotification().tickerText)) {
                return;
            }
        }
        Drawable icon = StatusBarIconView.getIcon(this.mContext, new StatusBarIcon(n.getPackageName(), n.getUser(), n.getNotification().icon, n.getNotification().iconLevel, 0, n.getNotification().tickerText));
        CharSequence text = n.getNotification().tickerText;
        Segment newSegment = new Segment(n, icon, text);
        int i = 0;
        while (i < this.mSegments.size()) {
            Segment seg2 = this.mSegments.get(i);
            if (n.getId() == seg2.notification.getId() && n.getPackageName().equals(seg2.notification.getPackageName())) {
                this.mSegments.remove(i);
                i--;
            }
            i++;
        }
        this.mSegments.add(newSegment);
        if (initialCount == 0 && this.mSegments.size() > 0) {
            Segment seg3 = this.mSegments.get(0);
            seg3.first = false;
            this.mIconSwitcher.setAnimateFirstView(false);
            this.mIconSwitcher.reset();
            this.mIconSwitcher.setImageDrawable(seg3.icon);
            this.mTextSwitcher.setAnimateFirstView(false);
            this.mTextSwitcher.reset();
            this.mTextSwitcher.setText(seg3.getText());
            tickerStarting();
            scheduleAdvance();
        }
    }

    private static boolean charSequencesEqual(CharSequence a, CharSequence b) {
        if (a.length() != b.length()) {
            return false;
        }
        int length = a.length();
        for (int i = 0; i < length; i++) {
            if (a.charAt(i) != b.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    public void removeEntry(StatusBarNotification n) {
        for (int i = this.mSegments.size() - 1; i >= 0; i--) {
            Segment seg = this.mSegments.get(i);
            if (n.getId() == seg.notification.getId() && n.getPackageName().equals(seg.notification.getPackageName())) {
                this.mSegments.remove(i);
            }
        }
    }

    public void halt() {
        this.mHandler.removeCallbacks(this.mAdvanceTicker);
        this.mSegments.clear();
        tickerHalting();
    }

    public void reflowText() {
        if (this.mSegments.size() > 0) {
            Segment seg = this.mSegments.get(0);
            CharSequence text = seg.getText();
            this.mTextSwitcher.setCurrentText(text);
        }
    }

    public void scheduleAdvance() {
        this.mHandler.postDelayed(this.mAdvanceTicker, 3000L);
    }
}
