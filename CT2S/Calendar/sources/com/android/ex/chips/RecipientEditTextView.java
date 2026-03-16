package com.android.ex.chips;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Parcelable;
import android.text.Editable;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.QwertyKeyListener;
import android.text.util.Rfc822Token;
import android.text.util.Rfc822Tokenizer;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.DragEvent;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Filterable;
import android.widget.ListAdapter;
import android.widget.ListPopupWindow;
import android.widget.ListView;
import android.widget.MultiAutoCompleteTextView;
import android.widget.ScrollView;
import android.widget.TextView;
import com.android.ex.chips.BaseRecipientAdapter;
import com.android.ex.chips.DropdownChipLayouter;
import com.android.ex.chips.PhotoManager;
import com.android.ex.chips.RecipientAlternatesAdapter;
import com.android.ex.chips.recipientchip.DrawableRecipientChip;
import com.android.ex.chips.recipientchip.InvisibleRecipientChip;
import com.android.ex.chips.recipientchip.ReplacementDrawableSpan;
import com.android.ex.chips.recipientchip.VisibleRecipientChip;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RecipientEditTextView extends MultiAutoCompleteTextView implements DialogInterface.OnDismissListener, ActionMode.Callback, GestureDetector.OnGestureListener, View.OnClickListener, AdapterView.OnItemClickListener, TextView.OnEditorActionListener, DropdownChipLayouter.ChipDeleteListener, RecipientAlternatesAdapter.OnCheckedItemChangedListener {
    private int mActionBarHeight;
    private final Runnable mAddTextWatcher;
    private ListPopupWindow mAddressPopup;
    private View mAlternatePopupAnchor;
    private AdapterView.OnItemClickListener mAlternatesListener;
    private ListPopupWindow mAlternatesPopup;
    private boolean mAttachedToWindow;
    private int mAvatarPosition;
    private int mCheckedItem;
    private Drawable mChipBackground;
    private Drawable mChipBackgroundPressed;
    private Drawable mChipDelete;
    private float mChipFontSize;
    private float mChipHeight;
    private int mChipTextEndPadding;
    private int mChipTextStartPadding;
    private String mCopyAddress;
    private Dialog mCopyDialog;
    private Bitmap mDefaultContactPhoto;
    private Runnable mDelayedShrink;
    private boolean mDisableDelete;
    private boolean mDragEnabled;
    private DropdownChipLayouter mDropdownChipLayouter;
    private GestureDetector mGestureDetector;
    private Runnable mHandlePendingChips;
    private Handler mHandler;
    private IndividualReplacementTask mIndividualReplacements;
    private Drawable mInvalidChipBackground;
    private float mLineSpacingExtra;
    private int mMaxLines;
    private ReplacementDrawableSpan mMoreChip;
    private TextView mMoreItem;
    private boolean mNoChips;
    final ArrayList<String> mPendingChips;
    private int mPendingChipsCount;
    private RecipientEntryItemClickedListener mRecipientEntryItemClickedListener;
    private ArrayList<DrawableRecipientChip> mRemovedSpans;
    private ScrollView mScrollView;
    private DrawableRecipientChip mSelectedChip;
    private boolean mShouldShrink;
    ArrayList<DrawableRecipientChip> mTemporaryRecipients;
    private final int mTextHeight;
    private TextWatcher mTextWatcher;
    private MultiAutoCompleteTextView.Tokenizer mTokenizer;
    private boolean mTriedGettingScrollView;
    private AutoCompleteTextView.Validator mValidator;
    private Paint mWorkPaint;
    private static final String SEPARATOR = String.valueOf(',') + String.valueOf(' ');
    private static final Pattern PHONE_PATTERN = Pattern.compile("(\\+[0-9]+[\\- \\.]*)?(1?[ ]*\\([0-9]+\\)[\\- \\.]*)?([0-9][0-9\\- \\.][0-9\\- \\.]+[0-9])");
    private static final int DISMISS = "dismiss".hashCode();
    private static int sSelectedTextColor = -1;
    private static int sExcessTopPadding = -1;

    public interface RecipientEntryItemClickedListener {
        void onRecipientEntryItemClicked(int i, int i2);
    }

    public RecipientEditTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mChipBackground = null;
        this.mChipDelete = null;
        this.mWorkPaint = new Paint();
        this.mPendingChips = new ArrayList<>();
        this.mPendingChipsCount = 0;
        this.mNoChips = false;
        this.mShouldShrink = true;
        this.mDragEnabled = false;
        this.mAddTextWatcher = new Runnable() {
            @Override
            public void run() {
                if (RecipientEditTextView.this.mTextWatcher == null) {
                    RecipientEditTextView.this.mTextWatcher = new RecipientTextWatcher();
                    RecipientEditTextView.this.addTextChangedListener(RecipientEditTextView.this.mTextWatcher);
                }
            }
        };
        this.mHandlePendingChips = new Runnable() {
            @Override
            public void run() {
                RecipientEditTextView.this.handlePendingChips();
            }
        };
        this.mDelayedShrink = new Runnable() {
            @Override
            public void run() {
                RecipientEditTextView.this.shrink();
            }
        };
        setChipDimensions(context, attrs);
        this.mTextHeight = calculateTextHeight();
        if (sSelectedTextColor == -1) {
            sSelectedTextColor = context.getResources().getColor(android.R.color.white);
        }
        this.mAlternatesPopup = new ListPopupWindow(context);
        this.mAlternatesPopup.setBackgroundDrawable(null);
        this.mAddressPopup = new ListPopupWindow(context);
        this.mAddressPopup.setBackgroundDrawable(null);
        this.mCopyDialog = new Dialog(context);
        this.mAlternatesListener = new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long rowId) {
                RecipientEditTextView.this.mAlternatesPopup.setOnItemClickListener(null);
                RecipientEditTextView.this.replaceChip(RecipientEditTextView.this.mSelectedChip, ((RecipientAlternatesAdapter) adapterView.getAdapter()).getRecipientEntry(position));
                Message delayed = Message.obtain(RecipientEditTextView.this.mHandler, RecipientEditTextView.DISMISS);
                delayed.obj = RecipientEditTextView.this.mAlternatesPopup;
                RecipientEditTextView.this.mHandler.sendMessageDelayed(delayed, 300L);
                RecipientEditTextView.this.clearComposingText();
            }
        };
        setInputType(getInputType() | 524288);
        setOnItemClickListener(this);
        setCustomSelectionActionModeCallback(this);
        this.mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == RecipientEditTextView.DISMISS) {
                    ((ListPopupWindow) msg.obj).dismiss();
                } else {
                    super.handleMessage(msg);
                }
            }
        };
        this.mTextWatcher = new RecipientTextWatcher();
        addTextChangedListener(this.mTextWatcher);
        this.mGestureDetector = new GestureDetector(context, this);
        setOnEditorActionListener(this);
        setDropdownChipLayouter(new DropdownChipLayouter(LayoutInflater.from(context), context));
    }

    private int calculateTextHeight() {
        Rect textBounds = new Rect();
        TextPaint paint = getPaint();
        textBounds.setEmpty();
        paint.getTextBounds("a", 0, "a".length(), textBounds);
        textBounds.left = 0;
        textBounds.right = 0;
        return textBounds.height();
    }

    public void setDropdownChipLayouter(DropdownChipLayouter dropdownChipLayouter) {
        this.mDropdownChipLayouter = dropdownChipLayouter;
        this.mDropdownChipLayouter.setDeleteListener(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.mAttachedToWindow = false;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mAttachedToWindow = true;
    }

    @Override
    public boolean onEditorAction(TextView view, int action, KeyEvent keyEvent) {
        if (action == 6) {
            if (commitDefault()) {
                return true;
            }
            if (this.mSelectedChip != null) {
                clearSelectedChip();
                return true;
            }
            if (focusNext()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        InputConnection connection = super.onCreateInputConnection(outAttrs);
        int imeActions = outAttrs.imeOptions & 255;
        if ((imeActions & 6) != 0) {
            outAttrs.imeOptions ^= imeActions;
            outAttrs.imeOptions |= 6;
        }
        if ((outAttrs.imeOptions & 1073741824) != 0) {
            outAttrs.imeOptions &= -1073741825;
        }
        outAttrs.actionId = 6;
        outAttrs.actionLabel = getContext().getString(R.string.action_label);
        return connection;
    }

    DrawableRecipientChip getLastChip() {
        DrawableRecipientChip[] chips = getSortedRecipients();
        if (chips == null || chips.length <= 0) {
            return null;
        }
        DrawableRecipientChip last = chips[chips.length - 1];
        return last;
    }

    @Override
    public void onSelectionChanged(int start, int end) {
        DrawableRecipientChip last = getLastChip();
        if (last != null && start < getSpannable().getSpanEnd(last)) {
            setSelection(Math.min(getSpannable().getSpanEnd(last) + 1, getText().length()));
        }
        super.onSelectionChanged(start, end);
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (!TextUtils.isEmpty(getText())) {
            super.onRestoreInstanceState(null);
        } else {
            super.onRestoreInstanceState(state);
        }
    }

    @Override
    public Parcelable onSaveInstanceState() {
        clearSelectedChip();
        return super.onSaveInstanceState();
    }

    @Override
    public void append(CharSequence text, int start, int end) {
        if (this.mTextWatcher != null) {
            removeTextChangedListener(this.mTextWatcher);
        }
        super.append(text, start, end);
        if (!TextUtils.isEmpty(text) && TextUtils.getTrimmedLength(text) > 0) {
            String displayString = text.toString();
            if (!displayString.trim().endsWith(String.valueOf(','))) {
                super.append(SEPARATOR, 0, SEPARATOR.length());
                displayString = displayString + SEPARATOR;
            }
            if (!TextUtils.isEmpty(displayString) && TextUtils.getTrimmedLength(displayString) > 0) {
                this.mPendingChipsCount++;
                this.mPendingChips.add(displayString);
            }
        }
        if (this.mPendingChipsCount > 0) {
            postHandlePendingChips();
        }
        this.mHandler.post(this.mAddTextWatcher);
    }

    @Override
    public void onFocusChanged(boolean hasFocus, int direction, Rect previous) {
        super.onFocusChanged(hasFocus, direction, previous);
        if (!hasFocus) {
            shrink();
        } else {
            expand();
        }
    }

    private int getExcessTopPadding() {
        if (sExcessTopPadding == -1) {
            sExcessTopPadding = (int) (this.mChipHeight + this.mLineSpacingExtra);
        }
        return sExcessTopPadding;
    }

    @Override
    public <T extends ListAdapter & Filterable> void setAdapter(T adapter) {
        super.setAdapter(adapter);
        BaseRecipientAdapter baseAdapter = (BaseRecipientAdapter) adapter;
        baseAdapter.registerUpdateObserver(new BaseRecipientAdapter.EntriesUpdatedObserver() {
            @Override
            public void onChanged(List<RecipientEntry> entries) {
                if (entries != null && entries.size() > 0) {
                    RecipientEditTextView.this.scrollBottomIntoView();
                }
            }
        });
        baseAdapter.setDropdownChipLayouter(this.mDropdownChipLayouter);
    }

    protected void scrollBottomIntoView() {
        if (this.mScrollView != null && this.mShouldShrink) {
            int[] location = new int[2];
            getLocationOnScreen(location);
            int height = getHeight();
            int currentPos = location[1] + height;
            int desiredPos = ((int) this.mChipHeight) + this.mActionBarHeight + getExcessTopPadding();
            if (currentPos > desiredPos) {
                this.mScrollView.scrollBy(0, currentPos - desiredPos);
            }
        }
    }

    @Override
    public void performValidation() {
    }

    private void shrink() {
        if (this.mTokenizer != null) {
            long contactId = this.mSelectedChip != null ? this.mSelectedChip.getEntry().getContactId() : -1L;
            if (this.mSelectedChip != null && contactId != -1 && !isPhoneQuery() && contactId != -2) {
                clearSelectedChip();
            } else {
                if (getWidth() <= 0) {
                    this.mHandler.removeCallbacks(this.mDelayedShrink);
                    this.mHandler.post(this.mDelayedShrink);
                    return;
                }
                if (this.mPendingChipsCount > 0) {
                    postHandlePendingChips();
                } else {
                    Editable editable = getText();
                    int end = getSelectionEnd();
                    int start = this.mTokenizer.findTokenStart(editable, end);
                    DrawableRecipientChip[] chips = (DrawableRecipientChip[]) getSpannable().getSpans(start, end, DrawableRecipientChip.class);
                    if (chips == null || chips.length == 0) {
                        Editable text = getText();
                        int whatEnd = this.mTokenizer.findTokenEnd(text, start);
                        if (whatEnd < text.length() && text.charAt(whatEnd) == ',') {
                            whatEnd = movePastTerminators(whatEnd);
                        }
                        int selEnd = getSelectionEnd();
                        if (whatEnd != selEnd) {
                            handleEdit(start, whatEnd);
                        } else {
                            commitChip(start, end, editable);
                        }
                    }
                }
                this.mHandler.post(this.mAddTextWatcher);
            }
            createMoreChip();
        }
    }

    private void expand() {
        if (this.mShouldShrink) {
            setMaxLines(Integer.MAX_VALUE);
        }
        removeMoreChip();
        setCursorVisible(true);
        Editable text = getText();
        setSelection((text == null || text.length() <= 0) ? 0 : text.length());
        if (this.mTemporaryRecipients != null && this.mTemporaryRecipients.size() > 0) {
            new RecipientReplacementTask().execute(new Void[0]);
            this.mTemporaryRecipients = null;
        }
    }

    private CharSequence ellipsizeText(CharSequence text, TextPaint paint, float maxWidth) {
        paint.setTextSize(this.mChipFontSize);
        if (maxWidth <= 0.0f && Log.isLoggable("RecipientEditTextView", 3)) {
            Log.d("RecipientEditTextView", "Max width is negative: " + maxWidth);
        }
        return TextUtils.ellipsize(text, paint, maxWidth, TextUtils.TruncateAt.END);
    }

    private Bitmap createSelectedChip(RecipientEntry contact, TextPaint paint) {
        paint.setColor(sSelectedTextColor);
        ChipBitmapContainer bitmapContainer = createChipBitmap(contact, paint, this.mChipBackgroundPressed, getResources().getColor(R.color.chip_background_selected));
        if (bitmapContainer.loadIcon) {
            loadAvatarIcon(contact, bitmapContainer);
        }
        return bitmapContainer.bitmap;
    }

    private Bitmap createUnselectedChip(RecipientEntry contact, TextPaint paint) {
        paint.setColor(getContext().getResources().getColor(android.R.color.black));
        ChipBitmapContainer bitmapContainer = createChipBitmap(contact, paint, getChipBackground(contact), getDefaultChipBackgroundColor(contact));
        if (bitmapContainer.loadIcon) {
            loadAvatarIcon(contact, bitmapContainer);
        }
        return bitmapContainer.bitmap;
    }

    private ChipBitmapContainer createChipBitmap(RecipientEntry contact, TextPaint paint, Drawable overrideBackgroundDrawable, int backgroundColor) {
        ChipBitmapContainer result = new ChipBitmapContainer();
        Rect backgroundPadding = new Rect();
        if (overrideBackgroundDrawable != null) {
            overrideBackgroundDrawable.getPadding(backgroundPadding);
        }
        int height = (int) this.mChipHeight;
        int iconWidth = contact.isValid() ? (height - backgroundPadding.top) - backgroundPadding.bottom : 0;
        float[] widths = new float[1];
        paint.getTextWidths(" ", widths);
        CharSequence ellipsizedText = ellipsizeText(createChipDisplayText(contact), paint, (((calculateAvailableWidth() - iconWidth) - widths[0]) - backgroundPadding.left) - backgroundPadding.right);
        int textWidth = (int) paint.measureText(ellipsizedText, 0, ellipsizedText.length());
        int startPadding = contact.isValid() ? this.mChipTextStartPadding : this.mChipTextEndPadding;
        int width = Math.max(iconWidth * 2, textWidth + startPadding + this.mChipTextEndPadding + iconWidth + backgroundPadding.left + backgroundPadding.right);
        result.bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result.bitmap);
        if (overrideBackgroundDrawable != null) {
            overrideBackgroundDrawable.setBounds(0, 0, width, height);
            overrideBackgroundDrawable.draw(canvas);
        } else {
            this.mWorkPaint.reset();
            this.mWorkPaint.setColor(backgroundColor);
            float radius = height / 2;
            canvas.drawRoundRect(new RectF(0.0f, 0.0f, width, height), radius, radius, this.mWorkPaint);
        }
        int textX = shouldPositionAvatarOnRight() ? this.mChipTextEndPadding + backgroundPadding.left : ((width - backgroundPadding.right) - this.mChipTextEndPadding) - textWidth;
        canvas.drawText(ellipsizedText, 0, ellipsizedText.length(), textX, getTextYOffset(height), paint);
        int iconX = shouldPositionAvatarOnRight() ? (width - backgroundPadding.right) - iconWidth : backgroundPadding.left;
        result.left = iconX;
        result.top = backgroundPadding.top;
        result.right = iconX + iconWidth;
        result.bottom = height - backgroundPadding.bottom;
        return result;
    }

    private void drawIcon(ChipBitmapContainer bitMapResult, Bitmap icon) {
        Canvas canvas = new Canvas(bitMapResult.bitmap);
        RectF src = new RectF(0.0f, 0.0f, icon.getWidth(), icon.getHeight());
        RectF dst = new RectF(bitMapResult.left, bitMapResult.top, bitMapResult.right, bitMapResult.bottom);
        drawIconOnCanvas(icon, canvas, src, dst);
    }

    private boolean shouldPositionAvatarOnRight() {
        boolean isRtl = Build.VERSION.SDK_INT >= 17 && getLayoutDirection() == 1;
        boolean assignedPosition = this.mAvatarPosition == 0;
        return isRtl ? !assignedPosition : assignedPosition;
    }

    private void loadAvatarIcon(final RecipientEntry contact, final ChipBitmapContainer bitmapContainer) {
        boolean drawPhotos = true;
        long contactId = contact.getContactId();
        if (isPhoneQuery()) {
            if (contactId == -1) {
                drawPhotos = false;
            }
        } else if (contactId == -1 || contactId == -2) {
            drawPhotos = false;
        }
        if (drawPhotos) {
            byte[] origPhotoBytes = contact.getPhotoBytes();
            if (origPhotoBytes == null) {
                getAdapter().fetchPhoto(contact, new PhotoManager.PhotoManagerCallback() {
                    @Override
                    public void onPhotoBytesPopulated() {
                        onPhotoBytesAsynchronouslyPopulated();
                    }

                    @Override
                    public void onPhotoBytesAsynchronouslyPopulated() {
                        byte[] loadedPhotoBytes = contact.getPhotoBytes();
                        Bitmap icon = BitmapFactory.decodeByteArray(loadedPhotoBytes, 0, loadedPhotoBytes.length);
                        tryDrawAndInvalidate(icon);
                    }

                    @Override
                    public void onPhotoBytesAsyncLoadFailed() {
                        tryDrawAndInvalidate(RecipientEditTextView.this.mDefaultContactPhoto);
                    }

                    private void tryDrawAndInvalidate(Bitmap icon) {
                        RecipientEditTextView.this.drawIcon(bitmapContainer, icon);
                        if (Looper.myLooper() == Looper.getMainLooper()) {
                            RecipientEditTextView.this.invalidate();
                        } else {
                            RecipientEditTextView.this.post(new Runnable() {
                                @Override
                                public void run() {
                                    RecipientEditTextView.this.invalidate();
                                }
                            });
                        }
                    }
                });
            } else {
                Bitmap icon = BitmapFactory.decodeByteArray(origPhotoBytes, 0, origPhotoBytes.length);
                drawIcon(bitmapContainer, icon);
            }
        }
    }

    Drawable getChipBackground(RecipientEntry contact) {
        return contact.isValid() ? this.mChipBackground : this.mInvalidChipBackground;
    }

    private int getDefaultChipBackgroundColor(RecipientEntry contact) {
        return getResources().getColor(contact.isValid() ? R.color.chip_background : R.color.chip_background_invalid);
    }

    protected float getTextYOffset(int height) {
        return height - ((height - this.mTextHeight) / 2);
    }

    protected void drawIconOnCanvas(Bitmap icon, Canvas canvas, RectF src, RectF dst) {
        Matrix matrix = new Matrix();
        BitmapShader shader = new BitmapShader(icon, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        matrix.reset();
        matrix.setRectToRect(src, dst, Matrix.ScaleToFit.FILL);
        shader.setLocalMatrix(matrix);
        this.mWorkPaint.reset();
        this.mWorkPaint.setShader(shader);
        this.mWorkPaint.setAntiAlias(true);
        this.mWorkPaint.setFilterBitmap(true);
        this.mWorkPaint.setDither(true);
        canvas.drawCircle(dst.centerX(), dst.centerY(), dst.width() / 2.0f, this.mWorkPaint);
        this.mWorkPaint.reset();
        this.mWorkPaint.setColor(0);
        this.mWorkPaint.setStyle(Paint.Style.STROKE);
        this.mWorkPaint.setStrokeWidth(1.0f);
        this.mWorkPaint.setAntiAlias(true);
        canvas.drawCircle(dst.centerX(), dst.centerY(), (dst.width() / 2.0f) - 0.5f, this.mWorkPaint);
        this.mWorkPaint.reset();
    }

    private DrawableRecipientChip constructChipSpan(RecipientEntry contact, boolean pressed) {
        Bitmap tmpBitmap;
        TextPaint paint = getPaint();
        float defaultSize = paint.getTextSize();
        int defaultColor = paint.getColor();
        if (pressed) {
            tmpBitmap = createSelectedChip(contact, paint);
        } else {
            tmpBitmap = createUnselectedChip(contact, paint);
        }
        Drawable result = new BitmapDrawable(getResources(), tmpBitmap);
        result.setBounds(0, 0, tmpBitmap.getWidth(), tmpBitmap.getHeight());
        VisibleRecipientChip recipientChip = new VisibleRecipientChip(result, contact);
        recipientChip.setExtraMargin(this.mLineSpacingExtra);
        paint.setTextSize(defaultSize);
        paint.setColor(defaultColor);
        return recipientChip;
    }

    private int calculateOffsetFromBottom(int line) {
        int actualLine = getLineCount() - (line + 1);
        return (-((((int) this.mChipHeight) * actualLine) + getPaddingBottom() + getPaddingTop())) + getDropDownVerticalOffset();
    }

    private int calculateOffsetFromBottomToTop(int line) {
        return -((int) (((this.mChipHeight + (2.0f * this.mLineSpacingExtra)) * Math.abs(getLineCount() - line)) + getPaddingBottom()));
    }

    private float calculateAvailableWidth() {
        return (((getWidth() - getPaddingLeft()) - getPaddingRight()) - this.mChipTextStartPadding) - this.mChipTextEndPadding;
    }

    private void setChipDimensions(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.RecipientEditTextView, 0, 0);
        Resources r = getContext().getResources();
        this.mChipBackground = a.getDrawable(R.styleable.RecipientEditTextView_chipBackground);
        this.mChipBackgroundPressed = a.getDrawable(R.styleable.RecipientEditTextView_chipBackgroundPressed);
        this.mInvalidChipBackground = a.getDrawable(R.styleable.RecipientEditTextView_invalidChipBackground);
        this.mChipDelete = a.getDrawable(R.styleable.RecipientEditTextView_chipDelete);
        if (this.mChipDelete == null) {
            this.mChipDelete = r.getDrawable(R.drawable.ic_cancel_wht_24dp);
        }
        int dimensionPixelSize = a.getDimensionPixelSize(R.styleable.RecipientEditTextView_chipPadding, -1);
        this.mChipTextEndPadding = dimensionPixelSize;
        this.mChipTextStartPadding = dimensionPixelSize;
        if (this.mChipTextStartPadding == -1) {
            int dimension = (int) r.getDimension(R.dimen.chip_padding);
            this.mChipTextEndPadding = dimension;
            this.mChipTextStartPadding = dimension;
        }
        int overridePadding = (int) r.getDimension(R.dimen.chip_padding_start);
        if (overridePadding >= 0) {
            this.mChipTextStartPadding = overridePadding;
        }
        int overridePadding2 = (int) r.getDimension(R.dimen.chip_padding_end);
        if (overridePadding2 >= 0) {
            this.mChipTextEndPadding = overridePadding2;
        }
        this.mDefaultContactPhoto = BitmapFactory.decodeResource(r, R.drawable.ic_contact_picture);
        this.mMoreItem = (TextView) LayoutInflater.from(getContext()).inflate(R.layout.more_item, (ViewGroup) null);
        this.mChipHeight = a.getDimensionPixelSize(R.styleable.RecipientEditTextView_chipHeight, -1);
        if (this.mChipHeight == -1.0f) {
            this.mChipHeight = r.getDimension(R.dimen.chip_height);
        }
        this.mChipFontSize = a.getDimensionPixelSize(R.styleable.RecipientEditTextView_chipFontSize, -1);
        if (this.mChipFontSize == -1.0f) {
            this.mChipFontSize = r.getDimension(R.dimen.chip_text_size);
        }
        this.mAvatarPosition = a.getInt(R.styleable.RecipientEditTextView_avatarPosition, 1);
        this.mDisableDelete = a.getBoolean(R.styleable.RecipientEditTextView_disableDelete, false);
        this.mMaxLines = r.getInteger(R.integer.chips_max_lines);
        this.mLineSpacingExtra = r.getDimensionPixelOffset(R.dimen.line_spacing_extra);
        TypedValue tv = new TypedValue();
        if (context.getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
            this.mActionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data, getResources().getDisplayMetrics());
        }
        a.recycle();
    }

    public void setOnFocusListShrinkRecipients(boolean shrink) {
        this.mShouldShrink = shrink;
    }

    @Override
    public void onSizeChanged(int width, int height, int oldw, int oldh) {
        super.onSizeChanged(width, height, oldw, oldh);
        if (width != 0 && height != 0) {
            if (this.mPendingChipsCount > 0) {
                postHandlePendingChips();
            } else {
                checkChipWidths();
            }
        }
        if (this.mScrollView == null && !this.mTriedGettingScrollView) {
            ViewParent parent = getParent();
            while (parent != null && !(parent instanceof ScrollView)) {
                parent = parent.getParent();
            }
            if (parent != null) {
                this.mScrollView = (ScrollView) parent;
            }
            this.mTriedGettingScrollView = true;
        }
    }

    private void postHandlePendingChips() {
        this.mHandler.removeCallbacks(this.mHandlePendingChips);
        this.mHandler.post(this.mHandlePendingChips);
    }

    private void checkChipWidths() {
        DrawableRecipientChip[] chips = getSortedRecipients();
        if (chips != null) {
            for (DrawableRecipientChip chip : chips) {
                Rect bounds = chip.getBounds();
                if (getWidth() > 0 && bounds.right - bounds.left > (getWidth() - getPaddingLeft()) - getPaddingRight()) {
                    replaceChip(chip, chip.getEntry());
                }
            }
        }
    }

    void handlePendingChips() {
        if (getViewWidth() > 0 && this.mPendingChipsCount > 0) {
            synchronized (this.mPendingChips) {
                Editable editable = getText();
                if (this.mPendingChipsCount <= 50) {
                    int i = 0;
                    while (i < this.mPendingChips.size()) {
                        String current = this.mPendingChips.get(i);
                        int tokenStart = editable.toString().indexOf(current);
                        int tokenEnd = (current.length() + tokenStart) - 1;
                        if (tokenStart >= 0) {
                            if (tokenEnd < editable.length() - 2 && editable.charAt(tokenEnd) == ',') {
                                tokenEnd++;
                            }
                            createReplacementChip(tokenStart, tokenEnd, editable, i < 2 || !this.mShouldShrink);
                        }
                        this.mPendingChipsCount--;
                        i++;
                    }
                    sanitizeEnd();
                } else {
                    this.mNoChips = true;
                }
                if (this.mTemporaryRecipients != null && this.mTemporaryRecipients.size() > 0 && this.mTemporaryRecipients.size() <= 50) {
                    if (hasFocus() || this.mTemporaryRecipients.size() < 2) {
                        new RecipientReplacementTask().execute(new Void[0]);
                        this.mTemporaryRecipients = null;
                    } else {
                        this.mIndividualReplacements = new IndividualReplacementTask();
                        this.mIndividualReplacements.execute(new ArrayList(this.mTemporaryRecipients.subList(0, 2)));
                        if (this.mTemporaryRecipients.size() > 2) {
                            this.mTemporaryRecipients = new ArrayList<>(this.mTemporaryRecipients.subList(2, this.mTemporaryRecipients.size()));
                        } else {
                            this.mTemporaryRecipients = null;
                        }
                        createMoreChip();
                    }
                } else {
                    this.mTemporaryRecipients = null;
                    createMoreChip();
                }
                this.mPendingChipsCount = 0;
                this.mPendingChips.clear();
            }
        }
    }

    int getViewWidth() {
        return getWidth();
    }

    void sanitizeEnd() {
        int end;
        if (this.mPendingChipsCount <= 0) {
            DrawableRecipientChip[] chips = getSortedRecipients();
            Spannable spannable = getSpannable();
            if (chips != null && chips.length > 0) {
                this.mMoreChip = getMoreChip();
                if (this.mMoreChip != null) {
                    end = spannable.getSpanEnd(this.mMoreChip);
                } else {
                    end = getSpannable().getSpanEnd(getLastChip());
                }
                Editable editable = getText();
                int length = editable.length();
                if (length > end) {
                    if (Log.isLoggable("RecipientEditTextView", 3)) {
                        Log.d("RecipientEditTextView", "There were extra characters after the last tokenizable entry." + ((Object) editable));
                    }
                    editable.delete(end + 1, length);
                }
            }
        }
    }

    void createReplacementChip(int tokenStart, int tokenEnd, Editable editable, boolean visible) {
        if (!alreadyHasChip(tokenStart, tokenEnd)) {
            String token = editable.toString().substring(tokenStart, tokenEnd);
            String trimmedToken = token.trim();
            int commitCharIndex = trimmedToken.lastIndexOf(44);
            if (commitCharIndex != -1 && commitCharIndex == trimmedToken.length() - 1) {
                token = trimmedToken.substring(0, trimmedToken.length() - 1);
            }
            RecipientEntry entry = createTokenizedEntry(token);
            if (entry != null) {
                DrawableRecipientChip chip = null;
                try {
                    if (!this.mNoChips) {
                        chip = visible ? constructChipSpan(entry, false) : new InvisibleRecipientChip(entry);
                    }
                } catch (NullPointerException e) {
                    Log.e("RecipientEditTextView", e.getMessage(), e);
                }
                editable.setSpan(chip, tokenStart, tokenEnd, 33);
                if (chip != null) {
                    if (this.mTemporaryRecipients == null) {
                        this.mTemporaryRecipients = new ArrayList<>();
                    }
                    chip.setOriginalText(token);
                    this.mTemporaryRecipients.add(chip);
                }
            }
        }
    }

    private static boolean isPhoneNumber(String number) {
        if (TextUtils.isEmpty(number)) {
            return false;
        }
        Matcher match = PHONE_PATTERN.matcher(number);
        return match.matches();
    }

    RecipientEntry createTokenizedEntry(String token) {
        if (TextUtils.isEmpty(token)) {
            return null;
        }
        if (isPhoneQuery() && isPhoneNumber(token)) {
            return RecipientEntry.constructFakePhoneEntry(token, true);
        }
        Rfc822Token[] tokens = Rfc822Tokenizer.tokenize(token);
        boolean isValid = isValid(token);
        if (isValid && tokens != null && tokens.length > 0) {
            String display = tokens[0].getName();
            if (!TextUtils.isEmpty(display)) {
                return RecipientEntry.constructGeneratedEntry(display, tokens[0].getAddress(), isValid);
            }
            String display2 = tokens[0].getAddress();
            if (!TextUtils.isEmpty(display2)) {
                return RecipientEntry.constructFakeEntry(display2, isValid);
            }
        }
        String validatedToken = null;
        if (this.mValidator != null && !isValid) {
            validatedToken = this.mValidator.fixText(token).toString();
            if (!TextUtils.isEmpty(validatedToken)) {
                if (validatedToken.contains(token)) {
                    Rfc822Token[] tokenized = Rfc822Tokenizer.tokenize(validatedToken);
                    if (tokenized.length > 0) {
                        validatedToken = tokenized[0].getAddress();
                        isValid = true;
                    }
                } else {
                    validatedToken = null;
                    isValid = false;
                }
            }
        }
        if (TextUtils.isEmpty(validatedToken)) {
            validatedToken = token;
        }
        return RecipientEntry.constructFakeEntry(validatedToken, isValid);
    }

    private boolean isValid(String text) {
        if (this.mValidator == null) {
            return true;
        }
        return this.mValidator.isValid(text);
    }

    private static String tokenizeAddress(String destination) {
        Rfc822Token[] tokens = Rfc822Tokenizer.tokenize(destination);
        if (tokens != null && tokens.length > 0) {
            return tokens[0].getAddress();
        }
        return destination;
    }

    @Override
    public void setTokenizer(MultiAutoCompleteTextView.Tokenizer tokenizer) {
        this.mTokenizer = tokenizer;
        super.setTokenizer(this.mTokenizer);
    }

    @Override
    public void setValidator(AutoCompleteTextView.Validator validator) {
        this.mValidator = validator;
        super.setValidator(validator);
    }

    @Override
    protected void replaceText(CharSequence text) {
    }

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        if (keyCode != 4 || this.mSelectedChip == null) {
            return super.onKeyPreIme(keyCode, event);
        }
        clearSelectedChip();
        return true;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case 61:
                if (event.hasNoModifiers()) {
                    if (this.mSelectedChip != null) {
                        clearSelectedChip();
                    } else {
                        commitDefault();
                    }
                }
                break;
        }
        return super.onKeyUp(keyCode, event);
    }

    private boolean focusNext() {
        View next = focusSearch(130);
        if (next == null) {
            return false;
        }
        next.requestFocus();
        return true;
    }

    private boolean commitDefault() {
        if (this.mTokenizer == null) {
            return false;
        }
        Editable editable = getText();
        int end = getSelectionEnd();
        int start = this.mTokenizer.findTokenStart(editable, end);
        if (!shouldCreateChip(start, end)) {
            return false;
        }
        int whatEnd = movePastTerminators(this.mTokenizer.findTokenEnd(getText(), start));
        if (whatEnd != getSelectionEnd()) {
            handleEdit(start, whatEnd);
            return true;
        }
        return commitChip(start, end, editable);
    }

    private void commitByCharacter() {
        if (this.mTokenizer != null) {
            Editable editable = getText();
            int end = getSelectionEnd();
            int start = this.mTokenizer.findTokenStart(editable, end);
            if (shouldCreateChip(start, end)) {
                commitChip(start, end, editable);
            }
            setSelection(getText().length());
        }
    }

    private boolean commitChip(int start, int end, Editable editable) {
        char charAt;
        ListAdapter adapter = getAdapter();
        if (adapter != null && adapter.getCount() > 0 && enoughToFilter() && end == getSelectionEnd() && !isPhoneQuery()) {
            if (!isValidEmailAddress(editable.toString().substring(start, end).trim())) {
                int selectedPosition = getListSelection();
                if (selectedPosition == -1) {
                    submitItemAtPosition(0);
                } else {
                    submitItemAtPosition(selectedPosition);
                }
            }
            dismissDropDown();
            return true;
        }
        int tokenEnd = this.mTokenizer.findTokenEnd(editable, start);
        if (editable.length() > tokenEnd + 1 && ((charAt = editable.charAt(tokenEnd + 1)) == ',' || charAt == ';')) {
            tokenEnd++;
        }
        String text = editable.toString().substring(start, tokenEnd).trim();
        clearComposingText();
        if (text == null || text.length() <= 0 || text.equals(" ")) {
            return false;
        }
        RecipientEntry entry = createTokenizedEntry(text);
        if (entry != null) {
            QwertyKeyListener.markAsReplaced(editable, start, end, "");
            CharSequence chipText = createChip(entry, false);
            if (chipText != null && start > -1 && end > -1) {
                editable.replace(start, end, chipText);
            }
        }
        if (end == getSelectionEnd()) {
            dismissDropDown();
        }
        sanitizeBetween();
        return true;
    }

    void sanitizeBetween() {
        DrawableRecipientChip[] recips;
        if (this.mPendingChipsCount <= 0 && (recips = getSortedRecipients()) != null && recips.length > 0) {
            DrawableRecipientChip last = recips[recips.length - 1];
            DrawableRecipientChip beforeLast = null;
            if (recips.length > 1) {
                beforeLast = recips[recips.length - 2];
            }
            int startLooking = 0;
            int end = getSpannable().getSpanStart(last);
            if (beforeLast != null) {
                startLooking = getSpannable().getSpanEnd(beforeLast);
                Editable text = getText();
                if (startLooking != -1 && startLooking <= text.length() - 1) {
                    if (text.charAt(startLooking) == ' ') {
                        startLooking++;
                    }
                } else {
                    return;
                }
            }
            if (startLooking >= 0 && end >= 0 && startLooking < end) {
                getText().delete(startLooking, end);
            }
        }
    }

    private boolean shouldCreateChip(int start, int end) {
        return !this.mNoChips && hasFocus() && enoughToFilter() && !alreadyHasChip(start, end);
    }

    private boolean alreadyHasChip(int start, int end) {
        if (this.mNoChips) {
            return true;
        }
        DrawableRecipientChip[] chips = (DrawableRecipientChip[]) getSpannable().getSpans(start, end, DrawableRecipientChip.class);
        return (chips == null || chips.length == 0) ? false : true;
    }

    private void handleEdit(int start, int end) {
        if (start == -1 || end == -1) {
            dismissDropDown();
            return;
        }
        Editable editable = getText();
        setSelection(end);
        String text = getText().toString().substring(start, end);
        if (!TextUtils.isEmpty(text)) {
            RecipientEntry entry = RecipientEntry.constructFakeEntry(text, isValid(text));
            QwertyKeyListener.markAsReplaced(editable, start, end, "");
            CharSequence chipText = createChip(entry, false);
            int selEnd = getSelectionEnd();
            if (chipText != null && start > -1 && selEnd > -1) {
                editable.replace(start, selEnd, chipText);
            }
        }
        dismissDropDown();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (this.mSelectedChip != null && keyCode == 67) {
            if (this.mAlternatesPopup != null && this.mAlternatesPopup.isShowing()) {
                this.mAlternatesPopup.dismiss();
            }
            removeChip(this.mSelectedChip);
        }
        switch (keyCode) {
            case 23:
            case 66:
                if (event.hasNoModifiers()) {
                    if (commitDefault()) {
                        return true;
                    }
                    if (this.mSelectedChip != null) {
                        clearSelectedChip();
                        return true;
                    }
                    if (focusNext()) {
                        return true;
                    }
                }
                break;
        }
        return super.onKeyDown(keyCode, event);
    }

    Spannable getSpannable() {
        return getText();
    }

    private int getChipStart(DrawableRecipientChip chip) {
        return getSpannable().getSpanStart(chip);
    }

    private int getChipEnd(DrawableRecipientChip chip) {
        return getSpannable().getSpanEnd(chip);
    }

    @Override
    protected void performFiltering(CharSequence text, int keyCode) {
        boolean isCompletedToken = isCompletedToken(text);
        if (enoughToFilter() && !isCompletedToken) {
            int end = getSelectionEnd();
            int start = this.mTokenizer.findTokenStart(text, end);
            Spannable span = getSpannable();
            DrawableRecipientChip[] chips = (DrawableRecipientChip[]) span.getSpans(start, end, DrawableRecipientChip.class);
            if (chips != null && chips.length > 0) {
                dismissDropDown();
                return;
            }
        } else if (isCompletedToken) {
            dismissDropDown();
            return;
        }
        super.performFiltering(text, keyCode);
    }

    boolean isCompletedToken(CharSequence text) {
        if (TextUtils.isEmpty(text)) {
            return false;
        }
        int end = text.length();
        int start = this.mTokenizer.findTokenStart(text, end);
        String token = text.toString().substring(start, end).trim();
        if (TextUtils.isEmpty(token)) {
            return false;
        }
        char atEnd = token.charAt(token.length() - 1);
        return atEnd == ',' || atEnd == ';';
    }

    private void clearSelectedChip() {
        if (this.mSelectedChip != null) {
            unselectChip(this.mSelectedChip);
            this.mSelectedChip = null;
        }
        setCursorVisible(true);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isFocused()) {
            return super.onTouchEvent(event);
        }
        boolean handled = super.onTouchEvent(event);
        int action = event.getAction();
        boolean chipWasSelected = false;
        if (this.mSelectedChip == null) {
            this.mGestureDetector.onTouchEvent(event);
        }
        if (this.mCopyAddress == null && action == 1) {
            float x = event.getX();
            float y = event.getY();
            int offset = putOffsetInRange(x, y);
            DrawableRecipientChip currentChip = findChip(offset);
            if (currentChip != null) {
                if (action == 1) {
                    if (this.mSelectedChip != null && this.mSelectedChip != currentChip) {
                        clearSelectedChip();
                        this.mSelectedChip = selectChip(currentChip);
                    } else if (this.mSelectedChip == null) {
                        setSelection(getText().length());
                        commitDefault();
                        this.mSelectedChip = selectChip(currentChip);
                    } else {
                        onClick(this.mSelectedChip);
                    }
                }
                chipWasSelected = true;
                handled = true;
            } else if (this.mSelectedChip != null && shouldShowEditableText(this.mSelectedChip)) {
                chipWasSelected = true;
            }
        }
        if (action == 1 && !chipWasSelected) {
            clearSelectedChip();
            return handled;
        }
        return handled;
    }

    private void scrollLineIntoView(int line) {
        if (this.mScrollView != null) {
            this.mScrollView.smoothScrollBy(0, calculateOffsetFromBottom(line));
        }
    }

    private void showAlternates(final DrawableRecipientChip currentChip, final ListPopupWindow alternatesPopup) {
        new AsyncTask<Void, Void, ListAdapter>() {
            @Override
            protected ListAdapter doInBackground(Void... params) {
                return RecipientEditTextView.this.createAlternatesAdapter(currentChip);
            }

            @Override
            protected void onPostExecute(ListAdapter result) {
                View view;
                if (RecipientEditTextView.this.mAttachedToWindow) {
                    int line = RecipientEditTextView.this.getLayout().getLineForOffset(RecipientEditTextView.this.getChipStart(currentChip));
                    int bottomOffset = RecipientEditTextView.this.calculateOffsetFromBottomToTop(line);
                    ListPopupWindow listPopupWindow = alternatesPopup;
                    if (RecipientEditTextView.this.mAlternatePopupAnchor != null) {
                        view = RecipientEditTextView.this.mAlternatePopupAnchor;
                    } else {
                        view = RecipientEditTextView.this;
                    }
                    listPopupWindow.setAnchorView(view);
                    alternatesPopup.setVerticalOffset(bottomOffset);
                    alternatesPopup.setAdapter(result);
                    alternatesPopup.setOnItemClickListener(RecipientEditTextView.this.mAlternatesListener);
                    RecipientEditTextView.this.mCheckedItem = -1;
                    alternatesPopup.show();
                    ListView listView = alternatesPopup.getListView();
                    listView.setChoiceMode(1);
                    if (RecipientEditTextView.this.mCheckedItem != -1) {
                        listView.setItemChecked(RecipientEditTextView.this.mCheckedItem, true);
                        RecipientEditTextView.this.mCheckedItem = -1;
                    }
                }
            }
        }.execute((Void[]) null);
    }

    private ListAdapter createAlternatesAdapter(DrawableRecipientChip chip) {
        return new RecipientAlternatesAdapter(getContext(), chip.getContactId(), chip.getDirectoryId(), chip.getLookupKey(), chip.getDataId(), getAdapter().getQueryType(), this, this.mDropdownChipLayouter, constructStateListDeleteDrawable());
    }

    private ListAdapter createSingleAddressAdapter(DrawableRecipientChip currentChip) {
        return new SingleRecipientArrayAdapter(getContext(), currentChip.getEntry(), this.mDropdownChipLayouter, constructStateListDeleteDrawable());
    }

    private StateListDrawable constructStateListDeleteDrawable() {
        StateListDrawable deleteDrawable = new StateListDrawable();
        if (!this.mDisableDelete) {
            deleteDrawable.addState(new int[]{android.R.attr.state_activated}, this.mChipDelete);
        }
        deleteDrawable.addState(new int[0], null);
        return deleteDrawable;
    }

    @Override
    public void onCheckedItemChanged(int position) {
        ListView listView = this.mAlternatesPopup.getListView();
        if (listView != null && listView.getCheckedItemCount() == 0) {
            listView.setItemChecked(position, true);
        }
        this.mCheckedItem = position;
    }

    private int putOffsetInRange(float x, float y) {
        int offset;
        if (Build.VERSION.SDK_INT >= 14) {
            offset = getOffsetForPosition(x, y);
        } else {
            offset = supportGetOffsetForPosition(x, y);
        }
        return putOffsetInRange(offset);
    }

    private int putOffsetInRange(int o) {
        int offset = o;
        Editable text = getText();
        int length = text.length();
        int realLength = length;
        for (int i = length - 1; i >= 0 && text.charAt(i) == ' '; i--) {
            realLength--;
        }
        if (offset >= realLength) {
            return offset;
        }
        Editable editable = getText();
        while (offset >= 0 && findText(editable, offset) == -1 && findChip(offset) == null) {
            offset--;
        }
        return offset;
    }

    private static int findText(Editable text, int offset) {
        if (text.charAt(offset) != ' ') {
            return offset;
        }
        return -1;
    }

    private DrawableRecipientChip findChip(int offset) {
        DrawableRecipientChip[] chips = (DrawableRecipientChip[]) getSpannable().getSpans(0, getText().length(), DrawableRecipientChip.class);
        for (DrawableRecipientChip chip : chips) {
            int start = getChipStart(chip);
            int end = getChipEnd(chip);
            if (offset >= start && offset <= end) {
                return chip;
            }
        }
        return null;
    }

    String createAddressText(RecipientEntry entry) {
        String trimmedDisplayText;
        Rfc822Token[] tokenized;
        String display = entry.getDisplayName();
        String address = entry.getDestination();
        if (TextUtils.isEmpty(display) || TextUtils.equals(display, address)) {
            display = null;
        }
        if (isPhoneQuery() && isPhoneNumber(address)) {
            trimmedDisplayText = address.trim();
        } else {
            if (address != null && (tokenized = Rfc822Tokenizer.tokenize(address)) != null && tokenized.length > 0) {
                address = tokenized[0].getAddress();
            }
            Rfc822Token token = new Rfc822Token(display, address, null);
            trimmedDisplayText = token.toString().trim();
        }
        int index = trimmedDisplayText.indexOf(",");
        return (this.mTokenizer == null || TextUtils.isEmpty(trimmedDisplayText) || index >= trimmedDisplayText.length() + (-1)) ? trimmedDisplayText : (String) this.mTokenizer.terminateToken(trimmedDisplayText);
    }

    String createChipDisplayText(RecipientEntry entry) {
        String display = entry.getDisplayName();
        String address = entry.getDestination();
        if (TextUtils.isEmpty(display) || TextUtils.equals(display, address)) {
            display = null;
        }
        if (TextUtils.isEmpty(display)) {
            return !TextUtils.isEmpty(address) ? address : new Rfc822Token(display, address, null).toString();
        }
        return display;
    }

    private CharSequence createChip(RecipientEntry entry, boolean pressed) {
        String displayText = createAddressText(entry);
        if (TextUtils.isEmpty(displayText)) {
            return null;
        }
        int textLength = displayText.length() - 1;
        SpannableString chipText = new SpannableString(displayText);
        if (!this.mNoChips) {
            try {
                DrawableRecipientChip chip = constructChipSpan(entry, pressed);
                chipText.setSpan(chip, 0, textLength, 33);
                chip.setOriginalText(chipText.toString());
            } catch (NullPointerException e) {
                Log.e("RecipientEditTextView", e.getMessage(), e);
                return null;
            }
        }
        onChipCreated(entry);
        return chipText;
    }

    protected void onChipCreated(RecipientEntry entry) {
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        int charactersTyped;
        if (position >= 0 && (charactersTyped = submitItemAtPosition(position)) > -1 && this.mRecipientEntryItemClickedListener != null) {
            this.mRecipientEntryItemClickedListener.onRecipientEntryItemClicked(charactersTyped, position);
        }
    }

    private int submitItemAtPosition(int position) {
        RecipientEntry entry = createValidatedEntry(getAdapter().getItem(position));
        if (entry == null) {
            return -1;
        }
        clearComposingText();
        int end = getSelectionEnd();
        int start = this.mTokenizer.findTokenStart(getText(), end);
        Editable editable = getText();
        QwertyKeyListener.markAsReplaced(editable, start, end, "");
        CharSequence chip = createChip(entry, false);
        if (chip != null && start >= 0 && end >= 0) {
            editable.replace(start, end, chip);
        }
        sanitizeBetween();
        return end - start;
    }

    private RecipientEntry createValidatedEntry(RecipientEntry item) {
        if (item == null) {
            return null;
        }
        String destination = item.getDestination();
        if (!isPhoneQuery() && item.getContactId() == -2) {
            return RecipientEntry.constructGeneratedEntry(item.getDisplayName(), destination, item.isValid());
        }
        if (RecipientEntry.isCreatedRecipient(item.getContactId()) && (TextUtils.isEmpty(item.getDisplayName()) || TextUtils.equals(item.getDisplayName(), destination) || (this.mValidator != null && !this.mValidator.isValid(destination)))) {
            return RecipientEntry.constructFakeEntry(destination, item.isValid());
        }
        return item;
    }

    DrawableRecipientChip[] getSortedRecipients() {
        DrawableRecipientChip[] recips = (DrawableRecipientChip[]) getSpannable().getSpans(0, getText().length(), DrawableRecipientChip.class);
        ArrayList<DrawableRecipientChip> recipientsList = new ArrayList<>(Arrays.asList(recips));
        final Spannable spannable = getSpannable();
        Collections.sort(recipientsList, new Comparator<DrawableRecipientChip>() {
            @Override
            public int compare(DrawableRecipientChip first, DrawableRecipientChip second) {
                int firstStart = spannable.getSpanStart(first);
                int secondStart = spannable.getSpanStart(second);
                if (firstStart < secondStart) {
                    return -1;
                }
                if (firstStart > secondStart) {
                    return 1;
                }
                return 0;
            }
        });
        return (DrawableRecipientChip[]) recipientsList.toArray(new DrawableRecipientChip[recipientsList.size()]);
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        return false;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    ReplacementDrawableSpan getMoreChip() {
        MoreImageSpan[] moreSpans = (MoreImageSpan[]) getSpannable().getSpans(0, getText().length(), MoreImageSpan.class);
        if (moreSpans == null || moreSpans.length <= 0) {
            return null;
        }
        return moreSpans[0];
    }

    private MoreImageSpan createMoreSpan(int count) {
        String moreText = String.format(this.mMoreItem.getText().toString(), Integer.valueOf(count));
        this.mWorkPaint.set(getPaint());
        this.mWorkPaint.setTextSize(this.mMoreItem.getTextSize());
        this.mWorkPaint.setColor(this.mMoreItem.getCurrentTextColor());
        int width = ((int) this.mWorkPaint.measureText(moreText)) + this.mMoreItem.getPaddingLeft() + this.mMoreItem.getPaddingRight();
        int height = (int) this.mChipHeight;
        Bitmap drawable = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(drawable);
        int adjustedHeight = height;
        Layout layout = getLayout();
        if (layout != null) {
            adjustedHeight -= layout.getLineDescent(0);
        }
        canvas.drawText(moreText, 0, moreText.length(), 0.0f, adjustedHeight, this.mWorkPaint);
        Drawable result = new BitmapDrawable(getResources(), drawable);
        result.setBounds(0, 0, width, height);
        return new MoreImageSpan(result);
    }

    void createMoreChipPlainText() {
        Editable text = getText();
        int start = 0;
        int end = 0;
        for (int i = 0; i < 2; i++) {
            end = movePastTerminators(this.mTokenizer.findTokenEnd(text, start));
            start = end;
        }
        int tokenCount = countTokens(text);
        MoreImageSpan moreSpan = createMoreSpan(tokenCount - 2);
        SpannableString chipText = new SpannableString(text.subSequence(end, text.length()));
        chipText.setSpan(moreSpan, 0, chipText.length(), 33);
        text.replace(end, text.length(), chipText);
        this.mMoreChip = moreSpan;
    }

    int countTokens(Editable text) {
        int tokenCount = 0;
        int start = 0;
        while (start < text.length()) {
            start = movePastTerminators(this.mTokenizer.findTokenEnd(text, start));
            tokenCount++;
            if (start >= text.length()) {
                break;
            }
        }
        return tokenCount;
    }

    void createMoreChip() {
        if (this.mNoChips) {
            createMoreChipPlainText();
            return;
        }
        if (this.mShouldShrink) {
            ReplacementDrawableSpan[] tempMore = (ReplacementDrawableSpan[]) getSpannable().getSpans(0, getText().length(), MoreImageSpan.class);
            if (tempMore.length > 0) {
                getSpannable().removeSpan(tempMore[0]);
            }
            DrawableRecipientChip[] recipients = getSortedRecipients();
            if (recipients == null || recipients.length <= 2) {
                this.mMoreChip = null;
                return;
            }
            Spannable spannable = getSpannable();
            int numRecipients = recipients.length;
            int overage = numRecipients - 2;
            MoreImageSpan moreSpan = createMoreSpan(overage);
            this.mRemovedSpans = new ArrayList<>();
            int totalReplaceStart = 0;
            int totalReplaceEnd = 0;
            Editable text = getText();
            for (int i = numRecipients - overage; i < recipients.length; i++) {
                this.mRemovedSpans.add(recipients[i]);
                if (i == numRecipients - overage) {
                    totalReplaceStart = spannable.getSpanStart(recipients[i]);
                }
                if (i == recipients.length - 1) {
                    totalReplaceEnd = spannable.getSpanEnd(recipients[i]);
                }
                if (this.mTemporaryRecipients == null || !this.mTemporaryRecipients.contains(recipients[i])) {
                    int spanStart = spannable.getSpanStart(recipients[i]);
                    int spanEnd = spannable.getSpanEnd(recipients[i]);
                    recipients[i].setOriginalText(text.toString().substring(spanStart, spanEnd));
                }
                spannable.removeSpan(recipients[i]);
            }
            if (totalReplaceEnd < text.length()) {
                totalReplaceEnd = text.length();
            }
            int end = Math.max(totalReplaceStart, totalReplaceEnd);
            int start = Math.min(totalReplaceStart, totalReplaceEnd);
            SpannableString chipText = new SpannableString(text.subSequence(start, end));
            chipText.setSpan(moreSpan, 0, chipText.length(), 33);
            text.replace(start, end, chipText);
            this.mMoreChip = moreSpan;
            if (!isPhoneQuery() && getLineCount() > this.mMaxLines) {
                setMaxLines(getLineCount());
            }
        }
    }

    void removeMoreChip() {
        DrawableRecipientChip[] recipients;
        if (this.mMoreChip != null) {
            Spannable span = getSpannable();
            span.removeSpan(this.mMoreChip);
            this.mMoreChip = null;
            if (this.mRemovedSpans != null && this.mRemovedSpans.size() > 0 && (recipients = getSortedRecipients()) != null && recipients.length != 0) {
                int end = span.getSpanEnd(recipients[recipients.length - 1]);
                Editable editable = getText();
                for (DrawableRecipientChip chip : this.mRemovedSpans) {
                    String token = (String) chip.getOriginalText();
                    int chipStart = editable.toString().indexOf(token, end);
                    int chipEnd = Math.min(editable.length(), token.length() + chipStart);
                    end = chipEnd;
                    if (chipStart != -1) {
                        editable.setSpan(chip, chipStart, chipEnd, 33);
                    }
                }
                this.mRemovedSpans.clear();
            }
        }
    }

    private DrawableRecipientChip selectChip(DrawableRecipientChip currentChip) {
        if (shouldShowEditableText(currentChip)) {
            CharSequence text = currentChip.getValue();
            Editable editable = getText();
            Spannable spannable = getSpannable();
            int spanStart = spannable.getSpanStart(currentChip);
            int spanEnd = spannable.getSpanEnd(currentChip);
            spannable.removeSpan(currentChip);
            editable.delete(spanStart, spanEnd);
            setCursorVisible(true);
            setSelection(editable.length());
            editable.append(text);
            return constructChipSpan(RecipientEntry.constructFakeEntry((String) text, isValid(text.toString())), true);
        }
        int start = getChipStart(currentChip);
        int end = getChipEnd(currentChip);
        getSpannable().removeSpan(currentChip);
        boolean showAddress = currentChip.getContactId() == -2 || getAdapter().forceShowAddress();
        if (showAddress) {
            try {
                if (this.mNoChips) {
                    return null;
                }
            } catch (NullPointerException e) {
                Log.e("RecipientEditTextView", e.getMessage(), e);
                return null;
            }
        }
        DrawableRecipientChip newChip = constructChipSpan(currentChip.getEntry(), true);
        Editable editable2 = getText();
        QwertyKeyListener.markAsReplaced(editable2, start, end, "");
        if (start == -1 || end == -1) {
            Log.d("RecipientEditTextView", "The chip being selected no longer exists but should.");
        } else {
            editable2.setSpan(newChip, start, end, 33);
        }
        newChip.setSelected(true);
        if (shouldShowEditableText(newChip)) {
            scrollLineIntoView(getLayout().getLineForOffset(getChipStart(newChip)));
        }
        if (showAddress) {
            showAddress(newChip, this.mAddressPopup);
        } else {
            showAlternates(newChip, this.mAlternatesPopup);
        }
        setCursorVisible(false);
        return newChip;
    }

    private boolean shouldShowEditableText(DrawableRecipientChip currentChip) {
        long contactId = currentChip.getContactId();
        return contactId == -1 || (!isPhoneQuery() && contactId == -2);
    }

    private void showAddress(final DrawableRecipientChip currentChip, final ListPopupWindow popup) {
        if (this.mAttachedToWindow) {
            int line = getLayout().getLineForOffset(getChipStart(currentChip));
            int bottomOffset = calculateOffsetFromBottomToTop(line);
            popup.setAnchorView(this.mAlternatePopupAnchor != null ? this.mAlternatePopupAnchor : this);
            popup.setVerticalOffset(bottomOffset);
            popup.setAdapter(createSingleAddressAdapter(currentChip));
            popup.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    RecipientEditTextView.this.unselectChip(currentChip);
                    popup.dismiss();
                }
            });
            popup.show();
            ListView listView = popup.getListView();
            listView.setChoiceMode(1);
            listView.setItemChecked(0, true);
        }
    }

    private void unselectChip(DrawableRecipientChip chip) {
        int start = getChipStart(chip);
        int end = getChipEnd(chip);
        Editable editable = getText();
        this.mSelectedChip = null;
        if (start == -1 || end == -1) {
            Log.w("RecipientEditTextView", "The chip doesn't exist or may be a chip a user was editing");
            setSelection(editable.length());
            commitDefault();
        } else {
            getSpannable().removeSpan(chip);
            QwertyKeyListener.markAsReplaced(editable, start, end, "");
            editable.removeSpan(chip);
            try {
                if (!this.mNoChips) {
                    editable.setSpan(constructChipSpan(chip.getEntry(), false), start, end, 33);
                }
            } catch (NullPointerException e) {
                Log.e("RecipientEditTextView", e.getMessage(), e);
            }
        }
        setCursorVisible(true);
        setSelection(editable.length());
        if (this.mAlternatesPopup != null && this.mAlternatesPopup.isShowing()) {
            this.mAlternatesPopup.dismiss();
        }
    }

    @Override
    public void onChipDelete() {
        if (this.mSelectedChip != null) {
            removeChip(this.mSelectedChip);
        }
        this.mAddressPopup.dismiss();
        this.mAlternatesPopup.dismiss();
    }

    void removeChip(DrawableRecipientChip chip) {
        Spannable spannable = getSpannable();
        int spanStart = spannable.getSpanStart(chip);
        int spanEnd = spannable.getSpanEnd(chip);
        Editable text = getText();
        int toDelete = spanEnd;
        boolean wasSelected = chip == this.mSelectedChip;
        if (wasSelected) {
            this.mSelectedChip = null;
        }
        while (toDelete >= 0 && toDelete < text.length() && text.charAt(toDelete) == ' ') {
            toDelete++;
        }
        spannable.removeSpan(chip);
        if (spanStart >= 0 && toDelete > 0) {
            text.delete(spanStart, toDelete);
        }
        if (wasSelected) {
            clearSelectedChip();
        }
    }

    void replaceChip(DrawableRecipientChip chip, RecipientEntry entry) {
        boolean wasSelected = chip == this.mSelectedChip;
        if (wasSelected) {
            this.mSelectedChip = null;
        }
        int start = getChipStart(chip);
        int end = getChipEnd(chip);
        getSpannable().removeSpan(chip);
        Editable editable = getText();
        CharSequence chipText = createChip(entry, false);
        if (chipText != null) {
            if (start == -1 || end == -1) {
                Log.e("RecipientEditTextView", "The chip to replace does not exist but should.");
                editable.insert(0, chipText);
            } else if (!TextUtils.isEmpty(chipText)) {
                int toReplace = end;
                while (toReplace >= 0 && toReplace < editable.length() && editable.charAt(toReplace) == ' ') {
                    toReplace++;
                }
                editable.replace(start, toReplace, chipText);
            }
        }
        setCursorVisible(true);
        if (wasSelected) {
            clearSelectedChip();
        }
    }

    public void onClick(DrawableRecipientChip chip) {
        if (chip.isSelected()) {
            clearSelectedChip();
        }
    }

    private boolean chipsPending() {
        return this.mPendingChipsCount > 0 || (this.mRemovedSpans != null && this.mRemovedSpans.size() > 0);
    }

    @Override
    public void removeTextChangedListener(TextWatcher watcher) {
        this.mTextWatcher = null;
        super.removeTextChangedListener(watcher);
    }

    private boolean isValidEmailAddress(String input) {
        return (TextUtils.isEmpty(input) || this.mValidator == null || !this.mValidator.isValid(input)) ? false : true;
    }

    private class RecipientTextWatcher implements TextWatcher {
        private RecipientTextWatcher() {
        }

        @Override
        public void afterTextChanged(Editable s) {
            char last;
            if (!TextUtils.isEmpty(s)) {
                if (!RecipientEditTextView.this.chipsPending()) {
                    if (RecipientEditTextView.this.mSelectedChip != null) {
                        if (!RecipientEditTextView.this.isGeneratedContact(RecipientEditTextView.this.mSelectedChip)) {
                            RecipientEditTextView.this.setCursorVisible(true);
                            RecipientEditTextView.this.setSelection(RecipientEditTextView.this.getText().length());
                            RecipientEditTextView.this.clearSelectedChip();
                        } else {
                            return;
                        }
                    }
                    int length = s.length();
                    if (length > 1) {
                        if (RecipientEditTextView.this.lastCharacterIsCommitCharacter(s)) {
                            RecipientEditTextView.this.commitByCharacter();
                            return;
                        }
                        int end = RecipientEditTextView.this.getSelectionEnd() == 0 ? 0 : RecipientEditTextView.this.getSelectionEnd() - 1;
                        int len = RecipientEditTextView.this.length() - 1;
                        if (end != len) {
                            last = s.charAt(end);
                        } else {
                            last = s.charAt(len);
                        }
                        if (last == ' ' && !RecipientEditTextView.this.isPhoneQuery()) {
                            String text = RecipientEditTextView.this.getText().toString();
                            int tokenStart = RecipientEditTextView.this.mTokenizer.findTokenStart(text, RecipientEditTextView.this.getSelectionEnd());
                            String sub = text.substring(tokenStart, RecipientEditTextView.this.mTokenizer.findTokenEnd(text, tokenStart));
                            if (RecipientEditTextView.this.isValidEmailAddress(sub)) {
                                RecipientEditTextView.this.commitByCharacter();
                                return;
                            }
                            return;
                        }
                        return;
                    }
                    return;
                }
                return;
            }
            Spannable spannable = RecipientEditTextView.this.getSpannable();
            DrawableRecipientChip[] chips = (DrawableRecipientChip[]) spannable.getSpans(0, RecipientEditTextView.this.getText().length(), DrawableRecipientChip.class);
            for (DrawableRecipientChip chip : chips) {
                spannable.removeSpan(chip);
            }
            if (RecipientEditTextView.this.mMoreChip != null) {
                spannable.removeSpan(RecipientEditTextView.this.mMoreChip);
            }
            RecipientEditTextView.this.clearSelectedChip();
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (before - count == 1) {
                int selStart = RecipientEditTextView.this.getSelectionStart();
                DrawableRecipientChip[] repl = (DrawableRecipientChip[]) RecipientEditTextView.this.getSpannable().getSpans(selStart, selStart, DrawableRecipientChip.class);
                if (repl.length > 0) {
                    Editable editable = RecipientEditTextView.this.getText();
                    int tokenStart = RecipientEditTextView.this.mTokenizer.findTokenStart(editable, selStart);
                    int tokenEnd = RecipientEditTextView.this.mTokenizer.findTokenEnd(editable, tokenStart) + 1;
                    if (tokenEnd > editable.length()) {
                        tokenEnd = editable.length();
                    }
                    editable.delete(tokenStart, tokenEnd);
                    RecipientEditTextView.this.getSpannable().removeSpan(repl[0]);
                    return;
                }
                return;
            }
            if (count > before && RecipientEditTextView.this.mSelectedChip != null && RecipientEditTextView.this.isGeneratedContact(RecipientEditTextView.this.mSelectedChip) && RecipientEditTextView.this.lastCharacterIsCommitCharacter(s)) {
                RecipientEditTextView.this.commitByCharacter();
            }
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }
    }

    public boolean lastCharacterIsCommitCharacter(CharSequence s) {
        char last;
        int end = getSelectionEnd() == 0 ? 0 : getSelectionEnd() - 1;
        int len = length() - 1;
        if (end != len) {
            last = s.charAt(end);
        } else {
            last = s.charAt(len);
        }
        return last == ',' || last == ';';
    }

    public boolean isGeneratedContact(DrawableRecipientChip chip) {
        long contactId = chip.getContactId();
        return contactId == -1 || (!isPhoneQuery() && contactId == -2);
    }

    void handlePasteClip(ClipData clip) {
        if (clip != null) {
            ClipDescription clipDesc = clip.getDescription();
            boolean containsSupportedType = clipDesc.hasMimeType("text/plain") || clipDesc.hasMimeType("text/html");
            if (containsSupportedType) {
                removeTextChangedListener(this.mTextWatcher);
                ClipDescription clipDescription = clip.getDescription();
                for (int i = 0; i < clip.getItemCount(); i++) {
                    String mimeType = clipDescription.getMimeType(i);
                    boolean supportedType = "text/plain".equals(mimeType) || "text/html".equals(mimeType);
                    if (supportedType) {
                        CharSequence pastedItem = clip.getItemAt(i).getText();
                        if (!TextUtils.isEmpty(pastedItem)) {
                            Editable editable = getText();
                            int start = getSelectionStart();
                            int end = getSelectionEnd();
                            if (start < 0 || end < 1) {
                                editable.append(pastedItem);
                            } else if (start == end) {
                                editable.insert(start, pastedItem);
                            } else {
                                editable.append(pastedItem, start, end);
                            }
                            handlePasteAndReplace();
                        }
                    }
                }
                this.mHandler.post(this.mAddTextWatcher);
            }
        }
    }

    @Override
    public boolean onTextContextMenuItem(int id) {
        if (id != 16908322) {
            return super.onTextContextMenuItem(id);
        }
        ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService("clipboard");
        handlePasteClip(clipboard.getPrimaryClip());
        return true;
    }

    private void handlePasteAndReplace() {
        ArrayList<DrawableRecipientChip> created = handlePaste();
        if (created != null && created.size() > 0) {
            IndividualReplacementTask replace = new IndividualReplacementTask();
            replace.execute(created);
        }
    }

    ArrayList<DrawableRecipientChip> handlePaste() {
        String text = getText().toString();
        int originalTokenStart = this.mTokenizer.findTokenStart(text, getSelectionEnd());
        String lastAddress = text.substring(originalTokenStart);
        int tokenStart = originalTokenStart;
        int prevTokenStart = 0;
        DrawableRecipientChip findChip = null;
        ArrayList<DrawableRecipientChip> created = new ArrayList<>();
        if (tokenStart != 0) {
            while (tokenStart != 0 && findChip == null && tokenStart != prevTokenStart) {
                prevTokenStart = tokenStart;
                tokenStart = this.mTokenizer.findTokenStart(text, tokenStart);
                findChip = findChip(tokenStart);
                if (tokenStart == originalTokenStart && findChip == null) {
                    break;
                }
            }
            if (tokenStart != originalTokenStart) {
                if (findChip != null) {
                    tokenStart = prevTokenStart;
                }
                while (tokenStart < originalTokenStart) {
                    int tokenEnd = movePastTerminators(this.mTokenizer.findTokenEnd(getText().toString(), tokenStart));
                    commitChip(tokenStart, tokenEnd, getText());
                    DrawableRecipientChip createdChip = findChip(tokenStart);
                    if (createdChip == null) {
                        break;
                    }
                    tokenStart = getSpannable().getSpanEnd(createdChip) + 1;
                    created.add(createdChip);
                }
            }
        }
        if (isCompletedToken(lastAddress)) {
            Editable editable = getText();
            int tokenStart2 = editable.toString().indexOf(lastAddress, originalTokenStart);
            commitChip(tokenStart2, editable.length(), editable);
            created.add(findChip(tokenStart2));
        }
        return created;
    }

    int movePastTerminators(int tokenEnd) {
        if (tokenEnd >= length()) {
            return tokenEnd;
        }
        char atEnd = getText().toString().charAt(tokenEnd);
        if (atEnd == ',' || atEnd == ';') {
            tokenEnd++;
        }
        if (tokenEnd < length() && getText().toString().charAt(tokenEnd) == ' ') {
            tokenEnd++;
        }
        return tokenEnd;
    }

    private class RecipientReplacementTask extends AsyncTask<Void, Void, Void> {
        private RecipientReplacementTask() {
        }

        private DrawableRecipientChip createFreeChip(RecipientEntry entry) {
            try {
                if (RecipientEditTextView.this.mNoChips) {
                    return null;
                }
                return RecipientEditTextView.this.constructChipSpan(entry, false);
            } catch (NullPointerException e) {
                Log.e("RecipientEditTextView", e.getMessage(), e);
                return null;
            }
        }

        @Override
        protected void onPreExecute() {
            List<DrawableRecipientChip> originalRecipients = new ArrayList<>();
            DrawableRecipientChip[] existingChips = RecipientEditTextView.this.getSortedRecipients();
            for (DrawableRecipientChip drawableRecipientChip : existingChips) {
                originalRecipients.add(drawableRecipientChip);
            }
            if (RecipientEditTextView.this.mRemovedSpans != null) {
                originalRecipients.addAll(RecipientEditTextView.this.mRemovedSpans);
            }
            List<DrawableRecipientChip> replacements = new ArrayList<>(originalRecipients.size());
            for (DrawableRecipientChip chip : originalRecipients) {
                if (RecipientEntry.isCreatedRecipient(chip.getEntry().getContactId()) && RecipientEditTextView.this.getSpannable().getSpanStart(chip) != -1) {
                    replacements.add(createFreeChip(chip.getEntry()));
                } else {
                    replacements.add(null);
                }
            }
            processReplacements(originalRecipients, replacements);
        }

        @Override
        protected Void doInBackground(Void... params) {
            if (RecipientEditTextView.this.mIndividualReplacements != null) {
                RecipientEditTextView.this.mIndividualReplacements.cancel(true);
            }
            final ArrayList<DrawableRecipientChip> recipients = new ArrayList<>();
            DrawableRecipientChip[] existingChips = RecipientEditTextView.this.getSortedRecipients();
            for (DrawableRecipientChip drawableRecipientChip : existingChips) {
                recipients.add(drawableRecipientChip);
            }
            if (RecipientEditTextView.this.mRemovedSpans != null) {
                recipients.addAll(RecipientEditTextView.this.mRemovedSpans);
            }
            ArrayList<String> addresses = new ArrayList<>();
            for (int i = 0; i < recipients.size(); i++) {
                DrawableRecipientChip chip = recipients.get(i);
                if (chip != null) {
                    addresses.add(RecipientEditTextView.this.createAddressText(chip.getEntry()));
                }
            }
            BaseRecipientAdapter adapter = RecipientEditTextView.this.getAdapter();
            adapter.getMatchingRecipients(addresses, new RecipientAlternatesAdapter.RecipientMatchCallback() {
                @Override
                public void matchesFound(Map<String, RecipientEntry> entries) {
                    ArrayList<DrawableRecipientChip> replacements = new ArrayList<>();
                    for (DrawableRecipientChip temp : recipients) {
                        RecipientEntry entry = null;
                        if (temp != null && RecipientEntry.isCreatedRecipient(temp.getEntry().getContactId()) && RecipientEditTextView.this.getSpannable().getSpanStart(temp) != -1) {
                            entry = RecipientEditTextView.this.createValidatedEntry(entries.get(RecipientEditTextView.tokenizeAddress(temp.getEntry().getDestination())));
                        }
                        if (entry != null) {
                            replacements.add(RecipientReplacementTask.this.createFreeChip(entry));
                        } else {
                            replacements.add(null);
                        }
                    }
                    RecipientReplacementTask.this.processReplacements(recipients, replacements);
                }

                @Override
                public void matchesNotFound(Set<String> unfoundAddresses) {
                    List<DrawableRecipientChip> replacements = new ArrayList<>(unfoundAddresses.size());
                    for (DrawableRecipientChip temp : recipients) {
                        if (temp != null && RecipientEntry.isCreatedRecipient(temp.getEntry().getContactId()) && RecipientEditTextView.this.getSpannable().getSpanStart(temp) != -1) {
                            if (unfoundAddresses.contains(temp.getEntry().getDestination())) {
                                replacements.add(RecipientReplacementTask.this.createFreeChip(temp.getEntry()));
                            } else {
                                replacements.add(null);
                            }
                        } else {
                            replacements.add(null);
                        }
                    }
                    RecipientReplacementTask.this.processReplacements(recipients, replacements);
                }
            });
            return null;
        }

        private void processReplacements(final List<DrawableRecipientChip> recipients, final List<DrawableRecipientChip> replacements) {
            if (replacements != null && replacements.size() > 0) {
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        int start;
                        Editable text = new SpannableStringBuilder(RecipientEditTextView.this.getText());
                        int i = 0;
                        for (DrawableRecipientChip chip : recipients) {
                            DrawableRecipientChip replacement = (DrawableRecipientChip) replacements.get(i);
                            if (replacement != null) {
                                RecipientEntry oldEntry = chip.getEntry();
                                RecipientEntry newEntry = replacement.getEntry();
                                boolean isBetter = RecipientAlternatesAdapter.getBetterRecipient(oldEntry, newEntry) == newEntry;
                                if (isBetter && (start = text.getSpanStart(chip)) != -1) {
                                    int end = Math.min(text.getSpanEnd(chip) + 1, text.length());
                                    text.removeSpan(chip);
                                    SpannableString displayText = new SpannableString(RecipientEditTextView.this.createAddressText(replacement.getEntry()).trim() + " ");
                                    displayText.setSpan(replacement, 0, displayText.length() - 1, 33);
                                    text.replace(start, end, displayText);
                                    replacement.setOriginalText(displayText.toString());
                                    replacements.set(i, null);
                                    recipients.set(i, replacement);
                                }
                            }
                            i++;
                        }
                        RecipientEditTextView.this.setText(text);
                    }
                };
                if (Looper.myLooper() != Looper.getMainLooper()) {
                    RecipientEditTextView.this.mHandler.post(runnable);
                } else {
                    runnable.run();
                }
            }
        }
    }

    private class IndividualReplacementTask extends AsyncTask<ArrayList<DrawableRecipientChip>, Void, Void> {
        private IndividualReplacementTask() {
        }

        @Override
        protected Void doInBackground(ArrayList<DrawableRecipientChip>... params) {
            final ArrayList<DrawableRecipientChip> originalRecipients = params[0];
            ArrayList<String> addresses = new ArrayList<>();
            for (int i = 0; i < originalRecipients.size(); i++) {
                DrawableRecipientChip chip = originalRecipients.get(i);
                if (chip != null) {
                    addresses.add(RecipientEditTextView.this.createAddressText(chip.getEntry()));
                }
            }
            BaseRecipientAdapter adapter = RecipientEditTextView.this.getAdapter();
            adapter.getMatchingRecipients(addresses, new RecipientAlternatesAdapter.RecipientMatchCallback() {
                @Override
                public void matchesFound(Map<String, RecipientEntry> entries) {
                    final RecipientEntry entry;
                    for (final DrawableRecipientChip temp : originalRecipients) {
                        if (RecipientEntry.isCreatedRecipient(temp.getEntry().getContactId()) && RecipientEditTextView.this.getSpannable().getSpanStart(temp) != -1 && (entry = RecipientEditTextView.this.createValidatedEntry(entries.get(RecipientEditTextView.tokenizeAddress(temp.getEntry().getDestination()).toLowerCase()))) != null) {
                            RecipientEditTextView.this.mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    RecipientEditTextView.this.replaceChip(temp, entry);
                                }
                            });
                        }
                    }
                }

                @Override
                public void matchesNotFound(Set<String> unfoundAddresses) {
                }
            });
            return null;
        }
    }

    private class MoreImageSpan extends ReplacementDrawableSpan {
        public MoreImageSpan(Drawable b) {
            super(b);
            setExtraMargin(RecipientEditTextView.this.mLineSpacingExtra);
        }
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent event) {
        if (this.mSelectedChip == null) {
            float x = event.getX();
            float y = event.getY();
            int offset = putOffsetInRange(x, y);
            DrawableRecipientChip currentChip = findChip(offset);
            if (currentChip != null) {
                if (this.mDragEnabled) {
                    startDrag(currentChip);
                } else {
                    showCopyDialog(currentChip.getEntry().getDestination());
                }
            }
        }
    }

    private int supportGetOffsetForPosition(float x, float y) {
        if (getLayout() == null) {
            return -1;
        }
        int line = supportGetLineAtCoordinate(y);
        return supportGetOffsetAtCoordinate(line, x);
    }

    private float supportConvertToLocalHorizontalCoordinate(float x) {
        return Math.min((getWidth() - getTotalPaddingRight()) - 1, Math.max(0.0f, x - getTotalPaddingLeft())) + getScrollX();
    }

    private int supportGetLineAtCoordinate(float y) {
        return getLayout().getLineForVertical((int) (Math.min((getHeight() - getTotalPaddingBottom()) - 1, Math.max(0.0f, y - getTotalPaddingLeft())) + getScrollY()));
    }

    private int supportGetOffsetAtCoordinate(int line, float x) {
        return getLayout().getOffsetForHorizontal(line, supportConvertToLocalHorizontalCoordinate(x));
    }

    private void startDrag(DrawableRecipientChip currentChip) {
        String address = currentChip.getEntry().getDestination();
        ClipData data = ClipData.newPlainText(address, address + ',');
        startDrag(data, new RecipientChipShadow(currentChip), null, 0);
        removeChip(currentChip);
    }

    @Override
    public boolean onDragEvent(DragEvent event) {
        switch (event.getAction()) {
            case 1:
                return event.getClipDescription().hasMimeType("text/plain");
            case 2:
            case 4:
            default:
                return false;
            case 3:
                handlePasteClip(event.getClipData());
                return true;
            case 5:
                requestFocus();
                return true;
        }
    }

    private final class RecipientChipShadow extends View.DragShadowBuilder {
        private final DrawableRecipientChip mChip;

        public RecipientChipShadow(DrawableRecipientChip chip) {
            this.mChip = chip;
        }

        @Override
        public void onProvideShadowMetrics(Point shadowSize, Point shadowTouchPoint) {
            Rect rect = this.mChip.getBounds();
            shadowSize.set(rect.width(), rect.height());
            shadowTouchPoint.set(rect.centerX(), rect.centerY());
        }

        @Override
        public void onDrawShadow(Canvas canvas) {
            this.mChip.draw(canvas);
        }
    }

    private void showCopyDialog(String address) {
        int btnTitleId;
        if (this.mAttachedToWindow) {
            this.mCopyAddress = address;
            this.mCopyDialog.setTitle(address);
            this.mCopyDialog.setContentView(R.layout.copy_chip_dialog_layout);
            this.mCopyDialog.setCancelable(true);
            this.mCopyDialog.setCanceledOnTouchOutside(true);
            Button button = (Button) this.mCopyDialog.findViewById(android.R.id.button1);
            button.setOnClickListener(this);
            if (isPhoneQuery()) {
                btnTitleId = R.string.copy_number;
            } else {
                btnTitleId = R.string.copy_email;
            }
            String buttonTitle = getContext().getResources().getString(btnTitleId);
            button.setText(buttonTitle);
            this.mCopyDialog.setOnDismissListener(this);
            this.mCopyDialog.show();
        }
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        this.mCopyAddress = null;
    }

    @Override
    public void onClick(View v) {
        ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService("clipboard");
        clipboard.setPrimaryClip(ClipData.newPlainText("", this.mCopyAddress));
        this.mCopyDialog.dismiss();
    }

    protected boolean isPhoneQuery() {
        return getAdapter() != null && getAdapter().getQueryType() == 1;
    }

    @Override
    public BaseRecipientAdapter getAdapter() {
        return (BaseRecipientAdapter) super.getAdapter();
    }

    private static class ChipBitmapContainer {
        Bitmap bitmap;
        float bottom;
        float left;
        boolean loadIcon;
        float right;
        float top;

        private ChipBitmapContainer() {
            this.loadIcon = true;
        }
    }
}
