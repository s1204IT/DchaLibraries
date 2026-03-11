package com.android.browser;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ConsoleMessage;
import android.webkit.WebView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import java.util.Vector;

class ErrorConsoleView extends LinearLayout {
    private TextView mConsoleHeader;
    private int mCurrentShowState;
    private ErrorConsoleListView mErrorList;
    private Vector<ConsoleMessage> mErrorMessageCache;
    private Button mEvalButton;
    private EditText mEvalEditText;
    private LinearLayout mEvalJsViewGroup;
    private boolean mSetupComplete;
    private WebView mWebView;

    public ErrorConsoleView(Context context) {
        super(context);
        this.mCurrentShowState = 2;
        this.mSetupComplete = false;
    }

    private void commonSetupIfNeeded() {
        if (this.mSetupComplete) {
            return;
        }
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService("layout_inflater");
        inflater.inflate(R.layout.error_console, this);
        this.mConsoleHeader = (TextView) findViewById(R.id.error_console_header_id);
        this.mErrorList = (ErrorConsoleListView) findViewById(R.id.error_console_list_id);
        this.mEvalJsViewGroup = (LinearLayout) findViewById(R.id.error_console_eval_view_group_id);
        this.mEvalEditText = (EditText) findViewById(R.id.error_console_eval_text_id);
        this.mEvalButton = (Button) findViewById(R.id.error_console_eval_button_id);
        this.mEvalButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ErrorConsoleView.this.mWebView != null) {
                    ErrorConsoleView.this.mWebView.loadUrl("javascript:" + ((Object) ErrorConsoleView.this.mEvalEditText.getText()));
                }
                ErrorConsoleView.this.mEvalEditText.setText("");
            }
        });
        this.mConsoleHeader.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ErrorConsoleView.this.mCurrentShowState == 0) {
                    ErrorConsoleView.this.showConsole(1);
                } else {
                    ErrorConsoleView.this.showConsole(0);
                }
            }
        });
        if (this.mErrorMessageCache != null) {
            for (ConsoleMessage msg : this.mErrorMessageCache) {
                this.mErrorList.addErrorMessage(msg);
            }
            this.mErrorMessageCache.clear();
        }
        this.mSetupComplete = true;
    }

    public void addErrorMessage(ConsoleMessage consoleMessage) {
        if (this.mSetupComplete) {
            this.mErrorList.addErrorMessage(consoleMessage);
            return;
        }
        if (this.mErrorMessageCache == null) {
            this.mErrorMessageCache = new Vector<>();
        }
        this.mErrorMessageCache.add(consoleMessage);
    }

    public void clearErrorMessages() {
        if (this.mSetupComplete) {
            this.mErrorList.clearErrorMessages();
        } else {
            if (this.mErrorMessageCache == null) {
                return;
            }
            this.mErrorMessageCache.clear();
        }
    }

    public int numberOfErrors() {
        if (this.mSetupComplete) {
            return this.mErrorList.getCount();
        }
        if (this.mErrorMessageCache == null) {
            return 0;
        }
        return this.mErrorMessageCache.size();
    }

    public void setWebView(WebView webview) {
        this.mWebView = webview;
    }

    public void showConsole(int show_state) {
        commonSetupIfNeeded();
        switch (show_state) {
            case 0:
                this.mConsoleHeader.setVisibility(0);
                this.mConsoleHeader.setText(R.string.error_console_header_text_minimized);
                this.mErrorList.setVisibility(8);
                this.mEvalJsViewGroup.setVisibility(8);
                break;
            case 1:
                this.mConsoleHeader.setVisibility(0);
                this.mConsoleHeader.setText(R.string.error_console_header_text_maximized);
                this.mErrorList.setVisibility(0);
                this.mEvalJsViewGroup.setVisibility(0);
                break;
            case 2:
                this.mConsoleHeader.setVisibility(8);
                this.mErrorList.setVisibility(8);
                this.mEvalJsViewGroup.setVisibility(8);
                break;
        }
        this.mCurrentShowState = show_state;
    }

    public int getShowState() {
        if (this.mSetupComplete) {
            return this.mCurrentShowState;
        }
        return 2;
    }

    private static class ErrorConsoleListView extends ListView {
        private ErrorConsoleMessageList mConsoleMessages;

        public ErrorConsoleListView(Context context, AttributeSet attributes) {
            super(context, attributes);
            this.mConsoleMessages = new ErrorConsoleMessageList(context);
            setAdapter((ListAdapter) this.mConsoleMessages);
        }

        public void addErrorMessage(ConsoleMessage consoleMessage) {
            this.mConsoleMessages.add(consoleMessage);
            setSelection(this.mConsoleMessages.getCount());
        }

        public void clearErrorMessages() {
            this.mConsoleMessages.clear();
        }

        private static class ErrorConsoleMessageList extends BaseAdapter implements ListAdapter {

            private static final int[] f3androidwebkitConsoleMessage$MessageLevelSwitchesValues = null;
            private LayoutInflater mInflater;
            private Vector<ConsoleMessage> mMessages = new Vector<>();

            private static int[] m84getandroidwebkitConsoleMessage$MessageLevelSwitchesValues() {
                if (f3androidwebkitConsoleMessage$MessageLevelSwitchesValues != null) {
                    return f3androidwebkitConsoleMessage$MessageLevelSwitchesValues;
                }
                int[] iArr = new int[ConsoleMessage.MessageLevel.values().length];
                try {
                    iArr[ConsoleMessage.MessageLevel.DEBUG.ordinal()] = 4;
                } catch (NoSuchFieldError e) {
                }
                try {
                    iArr[ConsoleMessage.MessageLevel.ERROR.ordinal()] = 1;
                } catch (NoSuchFieldError e2) {
                }
                try {
                    iArr[ConsoleMessage.MessageLevel.LOG.ordinal()] = 5;
                } catch (NoSuchFieldError e3) {
                }
                try {
                    iArr[ConsoleMessage.MessageLevel.TIP.ordinal()] = 2;
                } catch (NoSuchFieldError e4) {
                }
                try {
                    iArr[ConsoleMessage.MessageLevel.WARNING.ordinal()] = 3;
                } catch (NoSuchFieldError e5) {
                }
                f3androidwebkitConsoleMessage$MessageLevelSwitchesValues = iArr;
                return iArr;
            }

            public ErrorConsoleMessageList(Context context) {
                this.mInflater = (LayoutInflater) context.getSystemService("layout_inflater");
            }

            public void add(ConsoleMessage consoleMessage) {
                this.mMessages.add(consoleMessage);
                notifyDataSetChanged();
            }

            public void clear() {
                this.mMessages.clear();
                notifyDataSetChanged();
            }

            @Override
            public boolean areAllItemsEnabled() {
                return false;
            }

            @Override
            public boolean isEnabled(int position) {
                return false;
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public Object getItem(int position) {
                return this.mMessages.get(position);
            }

            @Override
            public int getCount() {
                return this.mMessages.size();
            }

            @Override
            public boolean hasStableIds() {
                return true;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view;
                ConsoleMessage error = this.mMessages.get(position);
                if (error == null) {
                    return null;
                }
                if (convertView == null) {
                    view = this.mInflater.inflate(android.R.layout.two_line_list_item, parent, false);
                } else {
                    view = convertView;
                }
                TextView headline = (TextView) view.findViewById(android.R.id.text1);
                TextView subText = (TextView) view.findViewById(android.R.id.text2);
                headline.setText(error.sourceId() + ":" + error.lineNumber());
                headline.setTextColor(-1);
                subText.setText(error.message());
                switch (m84getandroidwebkitConsoleMessage$MessageLevelSwitchesValues()[error.messageLevel().ordinal()]) {
                    case 1:
                        subText.setTextColor(-65536);
                        return view;
                    case 2:
                        subText.setTextColor(-16776961);
                        return view;
                    case 3:
                        subText.setTextColor(Color.rgb(255, 192, 0));
                        return view;
                    default:
                        subText.setTextColor(-3355444);
                        return view;
                }
            }
        }
    }
}
