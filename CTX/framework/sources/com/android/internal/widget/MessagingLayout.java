package com.android.internal.widget;

import android.app.Notification;
import android.app.Person;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Icon;
import android.net.wifi.WifiEnterpriseConfig;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.RemotableViewMethod;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;
import android.widget.RemoteViews;
import android.widget.TextView;
import com.android.internal.R;
import com.android.internal.graphics.ColorUtils;
import com.android.internal.util.NotificationColorUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Pattern;

@RemoteViews.RemoteView
public class MessagingLayout extends FrameLayout {
    private static final float COLOR_SHIFT_AMOUNT = 60.0f;
    private ArrayList<MessagingGroup> mAddedGroups;
    private Icon mAvatarReplacement;
    private int mAvatarSize;
    private CharSequence mConversationTitle;
    private boolean mDisplayImagesAtEnd;
    private ArrayList<MessagingGroup> mGroups;
    private List<MessagingMessage> mHistoricMessages;
    private boolean mIsOneToOne;
    private int mLayoutColor;
    private int mMessageTextColor;
    private List<MessagingMessage> mMessages;
    private MessagingLinearLayout mMessagingLinearLayout;
    private CharSequence mNameReplacement;
    private Paint mPaint;
    private int mSenderTextColor;
    private boolean mShowHistoricMessages;
    private Paint mTextPaint;
    private TextView mTitleView;
    private Person mUser;
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[!@#$%&*()_+=|<>?{}\\[\\]~-]");
    private static final Consumer<MessagingMessage> REMOVE_MESSAGE = new Consumer() {
        @Override
        public final void accept(Object obj) {
            ((MessagingMessage) obj).removeMessage();
        }
    };
    public static final Interpolator LINEAR_OUT_SLOW_IN = new PathInterpolator(0.0f, 0.0f, 0.2f, 1.0f);
    public static final Interpolator FAST_OUT_LINEAR_IN = new PathInterpolator(0.4f, 0.0f, 1.0f, 1.0f);
    public static final Interpolator FAST_OUT_SLOW_IN = new PathInterpolator(0.4f, 0.0f, 0.2f, 1.0f);
    public static final View.OnLayoutChangeListener MESSAGING_PROPERTY_ANIMATOR = new MessagingPropertyAnimator();

    public MessagingLayout(Context context) {
        super(context);
        this.mMessages = new ArrayList();
        this.mHistoricMessages = new ArrayList();
        this.mGroups = new ArrayList<>();
        this.mPaint = new Paint(1);
        this.mTextPaint = new Paint();
        this.mAddedGroups = new ArrayList<>();
    }

    public MessagingLayout(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mMessages = new ArrayList();
        this.mHistoricMessages = new ArrayList();
        this.mGroups = new ArrayList<>();
        this.mPaint = new Paint(1);
        this.mTextPaint = new Paint();
        this.mAddedGroups = new ArrayList<>();
    }

    public MessagingLayout(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mMessages = new ArrayList();
        this.mHistoricMessages = new ArrayList();
        this.mGroups = new ArrayList<>();
        this.mPaint = new Paint(1);
        this.mTextPaint = new Paint();
        this.mAddedGroups = new ArrayList<>();
    }

    public MessagingLayout(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mMessages = new ArrayList();
        this.mHistoricMessages = new ArrayList();
        this.mGroups = new ArrayList<>();
        this.mPaint = new Paint(1);
        this.mTextPaint = new Paint();
        this.mAddedGroups = new ArrayList<>();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mMessagingLinearLayout = (MessagingLinearLayout) findViewById(R.id.notification_messaging);
        this.mMessagingLinearLayout.setMessagingLayout(this);
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        int iMax = Math.max(displayMetrics.widthPixels, displayMetrics.heightPixels);
        this.mMessagingLinearLayout.setClipBounds(new Rect(0, 0, iMax, iMax));
        this.mTitleView = (TextView) findViewById(16908310);
        this.mAvatarSize = getResources().getDimensionPixelSize(R.dimen.messaging_avatar_size);
        this.mTextPaint.setTextAlign(Paint.Align.CENTER);
        this.mTextPaint.setAntiAlias(true);
    }

    @RemotableViewMethod
    public void setAvatarReplacement(Icon icon) {
        this.mAvatarReplacement = icon;
    }

    @RemotableViewMethod
    public void setNameReplacement(CharSequence charSequence) {
        this.mNameReplacement = charSequence;
    }

    @RemotableViewMethod
    public void setDisplayImagesAtEnd(boolean z) {
        this.mDisplayImagesAtEnd = z;
    }

    @RemotableViewMethod
    public void setData(Bundle bundle) {
        List<Notification.MessagingStyle.Message> messagesFromBundleArray = Notification.MessagingStyle.Message.getMessagesFromBundleArray(bundle.getParcelableArray(Notification.EXTRA_MESSAGES));
        List<Notification.MessagingStyle.Message> messagesFromBundleArray2 = Notification.MessagingStyle.Message.getMessagesFromBundleArray(bundle.getParcelableArray(Notification.EXTRA_HISTORIC_MESSAGES));
        setUser((Person) bundle.getParcelable(Notification.EXTRA_MESSAGING_PERSON));
        this.mConversationTitle = null;
        TextView textView = (TextView) findViewById(R.id.header_text);
        if (textView != null) {
            this.mConversationTitle = textView.getText();
        }
        addRemoteInputHistoryToMessages(messagesFromBundleArray, bundle.getCharSequenceArray(Notification.EXTRA_REMOTE_INPUT_HISTORY));
        bind(messagesFromBundleArray, messagesFromBundleArray2, bundle.getBoolean(Notification.EXTRA_SHOW_REMOTE_INPUT_SPINNER, false));
    }

    private void addRemoteInputHistoryToMessages(List<Notification.MessagingStyle.Message> list, CharSequence[] charSequenceArr) {
        if (charSequenceArr == null || charSequenceArr.length == 0) {
            return;
        }
        for (int length = charSequenceArr.length - 1; length >= 0; length--) {
            list.add(new Notification.MessagingStyle.Message(charSequenceArr[length], 0L, (Person) null, true));
        }
    }

    private void bind(List<Notification.MessagingStyle.Message> list, List<Notification.MessagingStyle.Message> list2, boolean z) {
        List<MessagingMessage> listCreateMessages = createMessages(list2, true);
        List<MessagingMessage> listCreateMessages2 = createMessages(list, false);
        ArrayList<MessagingGroup> arrayList = new ArrayList<>(this.mGroups);
        addMessagesToGroups(listCreateMessages, listCreateMessages2, z);
        removeGroups(arrayList);
        this.mMessages.forEach(REMOVE_MESSAGE);
        this.mHistoricMessages.forEach(REMOVE_MESSAGE);
        this.mMessages = listCreateMessages2;
        this.mHistoricMessages = listCreateMessages;
        updateHistoricMessageVisibility();
        updateTitleAndNamesDisplay();
    }

    private void removeGroups(ArrayList<MessagingGroup> arrayList) {
        int size = arrayList.size();
        for (int i = 0; i < size; i++) {
            final MessagingGroup messagingGroup = arrayList.get(i);
            if (!this.mGroups.contains(messagingGroup)) {
                List<MessagingMessage> messages = messagingGroup.getMessages();
                Runnable runnable = new Runnable() {
                    @Override
                    public final void run() {
                        MessagingLayout.lambda$removeGroups$0(this.f$0, messagingGroup);
                    }
                };
                boolean zIsShown = messagingGroup.isShown();
                this.mMessagingLinearLayout.removeView(messagingGroup);
                if (zIsShown && !MessagingLinearLayout.isGone(messagingGroup)) {
                    this.mMessagingLinearLayout.addTransientView(messagingGroup, 0);
                    messagingGroup.removeGroupAnimated(runnable);
                } else {
                    runnable.run();
                }
                this.mMessages.removeAll(messages);
                this.mHistoricMessages.removeAll(messages);
            }
        }
    }

    public static void lambda$removeGroups$0(MessagingLayout messagingLayout, MessagingGroup messagingGroup) {
        messagingLayout.mMessagingLinearLayout.removeTransientView(messagingGroup);
        messagingGroup.recycle();
    }

    private void updateTitleAndNamesDisplay() {
        Icon avatarSymbolIfMatching;
        ArrayMap arrayMap = new ArrayMap();
        ArrayMap arrayMap2 = new ArrayMap();
        for (int i = 0; i < this.mGroups.size(); i++) {
            MessagingGroup messagingGroup = this.mGroups.get(i);
            CharSequence senderName = messagingGroup.getSenderName();
            if (messagingGroup.needsGeneratedAvatar() && !TextUtils.isEmpty(senderName) && !arrayMap.containsKey(senderName)) {
                char cCharAt = senderName.charAt(0);
                if (arrayMap2.containsKey(Character.valueOf(cCharAt))) {
                    CharSequence charSequence = (CharSequence) arrayMap2.get(Character.valueOf(cCharAt));
                    if (charSequence != null) {
                        arrayMap.put(charSequence, findNameSplit((String) charSequence));
                        arrayMap2.put(Character.valueOf(cCharAt), null);
                    }
                    arrayMap.put(senderName, findNameSplit((String) senderName));
                } else {
                    arrayMap.put(senderName, Character.toString(cCharAt));
                    arrayMap2.put(Character.valueOf(cCharAt), senderName);
                }
            }
        }
        ArrayMap arrayMap3 = new ArrayMap();
        for (int i2 = 0; i2 < this.mGroups.size(); i2++) {
            MessagingGroup messagingGroup2 = this.mGroups.get(i2);
            boolean z = messagingGroup2.getSender() == this.mUser;
            CharSequence senderName2 = messagingGroup2.getSenderName();
            if (messagingGroup2.needsGeneratedAvatar() && !TextUtils.isEmpty(senderName2) && ((!this.mIsOneToOne || this.mAvatarReplacement == null || z) && (avatarSymbolIfMatching = messagingGroup2.getAvatarSymbolIfMatching(senderName2, (String) arrayMap.get(senderName2), this.mLayoutColor)) != null)) {
                arrayMap3.put(senderName2, avatarSymbolIfMatching);
            }
        }
        for (int i3 = 0; i3 < this.mGroups.size(); i3++) {
            MessagingGroup messagingGroup3 = this.mGroups.get(i3);
            CharSequence senderName3 = messagingGroup3.getSenderName();
            if (messagingGroup3.needsGeneratedAvatar() && !TextUtils.isEmpty(senderName3)) {
                if (this.mIsOneToOne && this.mAvatarReplacement != null && messagingGroup3.getSender() != this.mUser) {
                    messagingGroup3.setAvatar(this.mAvatarReplacement);
                } else {
                    Icon iconCreateAvatarSymbol = (Icon) arrayMap3.get(senderName3);
                    if (iconCreateAvatarSymbol == null) {
                        iconCreateAvatarSymbol = createAvatarSymbol(senderName3, (String) arrayMap.get(senderName3), this.mLayoutColor);
                        arrayMap3.put(senderName3, iconCreateAvatarSymbol);
                    }
                    messagingGroup3.setCreatedAvatar(iconCreateAvatarSymbol, senderName3, (String) arrayMap.get(senderName3), this.mLayoutColor);
                }
            }
        }
    }

    public Icon createAvatarSymbol(CharSequence charSequence, String str, int i) {
        boolean z;
        float f;
        float f2;
        if (str.isEmpty() || TextUtils.isDigitsOnly(str) || SPECIAL_CHAR_PATTERN.matcher(str).find()) {
            Icon iconCreateWithResource = Icon.createWithResource(getContext(), R.drawable.messaging_user);
            iconCreateWithResource.setTint(findColor(charSequence, i));
            return iconCreateWithResource;
        }
        Bitmap bitmapCreateBitmap = Bitmap.createBitmap(this.mAvatarSize, this.mAvatarSize, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmapCreateBitmap);
        float f3 = this.mAvatarSize / 2.0f;
        int iFindColor = findColor(charSequence, i);
        this.mPaint.setColor(iFindColor);
        canvas.drawCircle(f3, f3, f3, this.mPaint);
        if (ColorUtils.calculateLuminance(iFindColor) <= 0.5d) {
            z = false;
        } else {
            z = true;
        }
        this.mTextPaint.setColor(z ? -16777216 : -1);
        Paint paint = this.mTextPaint;
        if (str.length() == 1) {
            f = this.mAvatarSize;
            f2 = 0.5f;
        } else {
            f = this.mAvatarSize;
            f2 = 0.3f;
        }
        paint.setTextSize(f * f2);
        canvas.drawText(str, f3, (int) (f3 - ((this.mTextPaint.descent() + this.mTextPaint.ascent()) / 2.0f)), this.mTextPaint);
        return Icon.createWithBitmap(bitmapCreateBitmap);
    }

    private int findColor(CharSequence charSequence, int i) {
        double dCalculateLuminance = NotificationColorUtil.calculateLuminance(i);
        return NotificationColorUtil.getShiftedColor(i, (int) (((float) (((double) ((float) (((double) (((Math.abs(charSequence.hashCode()) % 5) / 4.0f) - 0.5f)) + Math.max(0.30000001192092896d - dCalculateLuminance, 0.0d)))) - Math.max(0.30000001192092896d - (1.0d - dCalculateLuminance), 0.0d))) * 60.0f));
    }

    private String findNameSplit(String str) {
        String[] strArrSplit = str.split(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
        if (strArrSplit.length > 1) {
            return Character.toString(strArrSplit[0].charAt(0)) + Character.toString(strArrSplit[1].charAt(0));
        }
        return str.substring(0, 1);
    }

    @RemotableViewMethod
    public void setLayoutColor(int i) {
        this.mLayoutColor = i;
    }

    @RemotableViewMethod
    public void setIsOneToOne(boolean z) {
        this.mIsOneToOne = z;
    }

    @RemotableViewMethod
    public void setSenderTextColor(int i) {
        this.mSenderTextColor = i;
    }

    @RemotableViewMethod
    public void setMessageTextColor(int i) {
        this.mMessageTextColor = i;
    }

    public void setUser(Person person) {
        this.mUser = person;
        if (this.mUser.getIcon() == null) {
            Icon iconCreateWithResource = Icon.createWithResource(getContext(), R.drawable.messaging_user);
            iconCreateWithResource.setTint(this.mLayoutColor);
            this.mUser = this.mUser.toBuilder().setIcon(iconCreateWithResource).build();
        }
    }

    private void addMessagesToGroups(List<MessagingMessage> list, List<MessagingMessage> list2, boolean z) {
        ArrayList arrayList = new ArrayList();
        ArrayList arrayList2 = new ArrayList();
        findGroups(list, list2, arrayList, arrayList2);
        createGroupViews(arrayList, arrayList2, z);
    }

    private void createGroupViews(List<List<MessagingMessage>> list, List<Person> list2, boolean z) {
        this.mGroups.clear();
        int i = 0;
        while (i < list.size()) {
            List<MessagingMessage> list3 = list.get(i);
            CharSequence charSequence = null;
            MessagingGroup messagingGroupCreateGroup = null;
            for (int size = list3.size() - 1; size >= 0; size--) {
                messagingGroupCreateGroup = list3.get(size).getGroup();
                if (messagingGroupCreateGroup != null) {
                    break;
                }
            }
            if (messagingGroupCreateGroup == null) {
                messagingGroupCreateGroup = MessagingGroup.createGroup(this.mMessagingLinearLayout);
                this.mAddedGroups.add(messagingGroupCreateGroup);
            }
            messagingGroupCreateGroup.setDisplayImagesAtEnd(this.mDisplayImagesAtEnd);
            messagingGroupCreateGroup.setLayoutColor(this.mLayoutColor);
            messagingGroupCreateGroup.setTextColors(this.mSenderTextColor, this.mMessageTextColor);
            Person person = list2.get(i);
            if (person != this.mUser && this.mNameReplacement != null) {
                charSequence = this.mNameReplacement;
            }
            messagingGroupCreateGroup.setSender(person, charSequence);
            messagingGroupCreateGroup.setSending(i == list.size() - 1 && z);
            this.mGroups.add(messagingGroupCreateGroup);
            if (this.mMessagingLinearLayout.indexOfChild(messagingGroupCreateGroup) != i) {
                this.mMessagingLinearLayout.removeView(messagingGroupCreateGroup);
                this.mMessagingLinearLayout.addView(messagingGroupCreateGroup, i);
            }
            messagingGroupCreateGroup.setMessages(list3);
            i++;
        }
    }

    private void findGroups(List<MessagingMessage> list, List<MessagingMessage> list2, List<List<MessagingMessage>> list3, List<Person> list4) {
        CharSequence name;
        int size = list.size();
        int i = 0;
        ArrayList arrayList = null;
        CharSequence charSequence = null;
        while (i < list2.size() + size) {
            MessagingMessage messagingMessage = i < size ? list.get(i) : list2.get(i - size);
            boolean z = arrayList == null;
            Person senderPerson = messagingMessage.getMessage().getSenderPerson();
            if (senderPerson != null) {
                name = senderPerson.getKey() == null ? senderPerson.getName() : senderPerson.getKey();
            } else {
                name = null;
            }
            if ((true ^ TextUtils.equals(name, charSequence)) | z) {
                arrayList = new ArrayList();
                list3.add(arrayList);
                if (senderPerson == null) {
                    senderPerson = this.mUser;
                }
                list4.add(senderPerson);
                charSequence = name;
            }
            arrayList.add(messagingMessage);
            i++;
        }
    }

    private List<MessagingMessage> createMessages(List<Notification.MessagingStyle.Message> list, boolean z) {
        ArrayList arrayList = new ArrayList();
        for (int i = 0; i < list.size(); i++) {
            Notification.MessagingStyle.Message message = list.get(i);
            MessagingMessage messagingMessageFindAndRemoveMatchingMessage = findAndRemoveMatchingMessage(message);
            if (messagingMessageFindAndRemoveMatchingMessage == null) {
                messagingMessageFindAndRemoveMatchingMessage = MessagingMessage.createMessage(this, message);
            }
            messagingMessageFindAndRemoveMatchingMessage.setIsHistoric(z);
            arrayList.add(messagingMessageFindAndRemoveMatchingMessage);
        }
        return arrayList;
    }

    private MessagingMessage findAndRemoveMatchingMessage(Notification.MessagingStyle.Message message) {
        for (int i = 0; i < this.mMessages.size(); i++) {
            MessagingMessage messagingMessage = this.mMessages.get(i);
            if (messagingMessage.sameAs(message)) {
                this.mMessages.remove(i);
                return messagingMessage;
            }
        }
        for (int i2 = 0; i2 < this.mHistoricMessages.size(); i2++) {
            MessagingMessage messagingMessage2 = this.mHistoricMessages.get(i2);
            if (messagingMessage2.sameAs(message)) {
                this.mHistoricMessages.remove(i2);
                return messagingMessage2;
            }
        }
        return null;
    }

    public void showHistoricMessages(boolean z) {
        this.mShowHistoricMessages = z;
        updateHistoricMessageVisibility();
    }

    private void updateHistoricMessageVisibility() {
        int size = this.mHistoricMessages.size();
        int i = 0;
        while (true) {
            int i2 = 8;
            if (i >= size) {
                break;
            }
            MessagingMessage messagingMessage = this.mHistoricMessages.get(i);
            if (this.mShowHistoricMessages) {
                i2 = 0;
            }
            messagingMessage.setVisibility(i2);
            i++;
        }
        int size2 = this.mGroups.size();
        for (int i3 = 0; i3 < size2; i3++) {
            MessagingGroup messagingGroup = this.mGroups.get(i3);
            List<MessagingMessage> messages = messagingGroup.getMessages();
            int size3 = messages.size();
            int i4 = 0;
            for (int i5 = 0; i5 < size3; i5++) {
                if (messages.get(i5).getVisibility() != 8) {
                    i4++;
                }
            }
            if (i4 > 0 && messagingGroup.getVisibility() == 8) {
                messagingGroup.setVisibility(0);
            } else if (i4 == 0 && messagingGroup.getVisibility() != 8) {
                messagingGroup.setVisibility(8);
            }
        }
    }

    @Override
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        super.onLayout(z, i, i2, i3, i4);
        if (!this.mAddedGroups.isEmpty()) {
            getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    for (MessagingGroup messagingGroup : MessagingLayout.this.mAddedGroups) {
                        if (messagingGroup.isShown()) {
                            MessagingPropertyAnimator.fadeIn(messagingGroup.getAvatar());
                            MessagingPropertyAnimator.fadeIn(messagingGroup.getSenderView());
                            MessagingPropertyAnimator.startLocalTranslationFrom(messagingGroup, messagingGroup.getHeight(), MessagingLayout.LINEAR_OUT_SLOW_IN);
                        }
                    }
                    MessagingLayout.this.mAddedGroups.clear();
                    MessagingLayout.this.getViewTreeObserver().removeOnPreDrawListener(this);
                    return true;
                }
            });
        }
    }

    public MessagingLinearLayout getMessagingLinearLayout() {
        return this.mMessagingLinearLayout;
    }

    public ArrayList<MessagingGroup> getMessagingGroups() {
        return this.mGroups;
    }
}
