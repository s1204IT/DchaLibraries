package com.googlecode.mp4parser.authoring;

public abstract class AbstractTrack implements Track {
    private boolean enabled = true;
    private boolean inMovie = true;
    private boolean inPreview = true;
    private boolean inPoster = true;

    @Override
    public boolean isEnabled() {
        return this.enabled;
    }

    @Override
    public boolean isInMovie() {
        return this.inMovie;
    }

    @Override
    public boolean isInPreview() {
        return this.inPreview;
    }

    @Override
    public boolean isInPoster() {
        return this.inPoster;
    }

    public void setEnabled(boolean z) {
        this.enabled = z;
    }

    public void setInMovie(boolean z) {
        this.inMovie = z;
    }

    public void setInPreview(boolean z) {
        this.inPreview = z;
    }

    public void setInPoster(boolean z) {
        this.inPoster = z;
    }
}
