package com.android.camera.app;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.android.camera.app.CameraAppUI;
import com.android.camera.util.Gusterpolator;
import com.android.camera.widget.Cling;
import com.android.camera.widget.ExternalViewerButton;
import com.android.camera2.R;

class FilmstripBottomPanel implements CameraAppUI.BottomPanel {
    private static final int ANIM_DURATION = 150;
    private final View mControlLayout;
    private final AppController mController;
    private ImageButton mDeleteButton;
    private ImageButton mEditButton;
    private final ViewGroup mLayout;
    private CameraAppUI.BottomPanel.Listener mListener;
    private final View mMiddleFiller;
    private ProgressBar mProgressBar;
    private View mProgressErrorLayout;
    private TextView mProgressErrorText;
    private View mProgressLayout;
    private TextView mProgressText;
    private ImageButton mShareButton;
    private boolean mTinyPlanetEnabled;
    private ExternalViewerButton mViewButton;

    public FilmstripBottomPanel(AppController controller, ViewGroup bottomControlsLayout) {
        this.mController = controller;
        this.mLayout = bottomControlsLayout;
        this.mMiddleFiller = this.mLayout.findViewById(R.id.filmstrip_bottom_control_middle_filler);
        this.mControlLayout = this.mLayout.findViewById(R.id.bottom_control_panel);
        setupEditButton();
        setupViewButton();
        setupDeleteButton();
        setupShareButton();
        setupProgressUi();
    }

    @Override
    public void setListener(CameraAppUI.BottomPanel.Listener listener) {
        this.mListener = listener;
    }

    @Override
    public void setClingForViewer(int viewerType, Cling cling) {
        this.mViewButton.setClingForViewer(viewerType, cling);
    }

    @Override
    public void clearClingForViewer(int viewerType) {
        this.mViewButton.clearClingForViewer(viewerType);
    }

    @Override
    public Cling getClingForViewer(int viewerType) {
        return this.mViewButton.getClingForViewer(viewerType);
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            this.mLayout.setVisibility(0);
        } else {
            this.mLayout.setVisibility(4);
        }
    }

    @Override
    public void setEditButtonVisibility(boolean visible) {
        this.mEditButton.setVisibility(visible ? 0 : 8);
        updateMiddleFillerLayoutVisibility();
    }

    @Override
    public void setEditEnabled(boolean enabled) {
        this.mEditButton.setEnabled(enabled);
    }

    @Override
    public void setViewerButtonVisibility(int state) {
        this.mViewButton.setState(state);
        updateMiddleFillerLayoutVisibility();
    }

    @Override
    public void setViewEnabled(boolean enabled) {
        this.mViewButton.setEnabled(enabled);
    }

    @Override
    public void setTinyPlanetEnabled(boolean enabled) {
        this.mTinyPlanetEnabled = enabled;
    }

    @Override
    public void setDeleteButtonVisibility(boolean visible) {
        this.mDeleteButton.setVisibility(visible ? 0 : 4);
    }

    @Override
    public void setDeleteEnabled(boolean enabled) {
        this.mDeleteButton.setEnabled(enabled);
    }

    @Override
    public void setShareButtonVisibility(boolean visible) {
        this.mShareButton.setVisibility(visible ? 0 : 4);
    }

    @Override
    public void setShareEnabled(boolean enabled) {
        this.mShareButton.setEnabled(enabled);
    }

    @Override
    public void setProgressText(CharSequence text) {
        this.mProgressText.setText(text);
    }

    @Override
    public void setProgress(int progress) {
        this.mProgressBar.setProgress(progress);
    }

    @Override
    public void showProgressError(CharSequence message) {
        hideControls();
        hideProgress();
        this.mProgressErrorLayout.setVisibility(0);
        this.mProgressErrorText.setText(message);
    }

    @Override
    public void hideProgressError() {
        this.mProgressErrorLayout.setVisibility(4);
    }

    @Override
    public void showProgress() {
        this.mProgressLayout.setVisibility(0);
        hideProgressError();
    }

    @Override
    public void hideProgress() {
        this.mProgressLayout.setVisibility(4);
    }

    @Override
    public void showControls() {
        this.mControlLayout.setVisibility(0);
    }

    @Override
    public void hideControls() {
        this.mControlLayout.setVisibility(4);
    }

    private void setupEditButton() {
        this.mEditButton = (ImageButton) this.mLayout.findViewById(R.id.filmstrip_bottom_control_edit);
        this.mEditButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (FilmstripBottomPanel.this.mTinyPlanetEnabled) {
                    FilmstripBottomPanel.this.mController.openContextMenu(FilmstripBottomPanel.this.mEditButton);
                } else if (FilmstripBottomPanel.this.mListener != null) {
                    FilmstripBottomPanel.this.mListener.onEdit();
                }
            }
        });
        this.mController.registerForContextMenu(this.mEditButton);
        this.mEditButton.setLongClickable(false);
    }

    private void setupViewButton() {
        this.mViewButton = (ExternalViewerButton) this.mLayout.findViewById(R.id.filmstrip_bottom_control_view);
        this.mViewButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (FilmstripBottomPanel.this.mListener != null) {
                    FilmstripBottomPanel.this.mListener.onExternalViewer();
                }
            }
        });
    }

    private void setupDeleteButton() {
        this.mDeleteButton = (ImageButton) this.mLayout.findViewById(R.id.filmstrip_bottom_control_delete);
        this.mDeleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (FilmstripBottomPanel.this.mListener != null) {
                    FilmstripBottomPanel.this.mListener.onDelete();
                }
            }
        });
    }

    private void setupShareButton() {
        this.mShareButton = (ImageButton) this.mLayout.findViewById(R.id.filmstrip_bottom_control_share);
        this.mShareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (FilmstripBottomPanel.this.mListener != null) {
                    FilmstripBottomPanel.this.mListener.onShare();
                }
            }
        });
    }

    private void setupProgressUi() {
        this.mProgressLayout = this.mLayout.findViewById(R.id.bottom_progress_panel);
        this.mProgressText = (TextView) this.mLayout.findViewById(R.id.bottom_session_progress_text);
        this.mProgressBar = (ProgressBar) this.mLayout.findViewById(R.id.bottom_session_progress_bar);
        this.mProgressBar.setMax(100);
        this.mProgressLayout.setVisibility(4);
        this.mProgressErrorText = (TextView) this.mLayout.findViewById(R.id.bottom_progress_error_text);
        this.mProgressErrorLayout = this.mLayout.findViewById(R.id.bottom_progress_error_panel);
        this.mProgressErrorLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (FilmstripBottomPanel.this.mListener != null) {
                    FilmstripBottomPanel.this.mListener.onProgressErrorClicked();
                }
            }
        });
    }

    private void updateMiddleFillerLayoutVisibility() {
        if (this.mEditButton.getVisibility() == 0 && this.mViewButton.getVisibility() == 0) {
            this.mMiddleFiller.setVisibility(4);
        } else {
            this.mMiddleFiller.setVisibility(8);
        }
    }

    public void show() {
        ObjectAnimator animator = ObjectAnimator.ofFloat(this.mLayout, "translationY", this.mLayout.getHeight(), 0.0f);
        animator.setDuration(150L);
        animator.setInterpolator(Gusterpolator.INSTANCE);
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                FilmstripBottomPanel.this.mViewButton.updateClingVisibility();
            }
        });
        this.mViewButton.hideClings();
        animator.start();
    }

    public void hide() {
        int offset = this.mLayout.getHeight();
        if (this.mLayout.getTranslationY() < offset) {
            ObjectAnimator animator = ObjectAnimator.ofFloat(this.mLayout, "translationY", this.mLayout.getTranslationY(), offset);
            animator.setDuration(150L);
            animator.setInterpolator(Gusterpolator.INSTANCE);
            this.mViewButton.hideClings();
            animator.start();
        }
    }
}
