package com.mediatek.gallery3d.video;

public interface IMovieList {
    void add(IMovieItem iMovieItem);

    IMovieItem getNext(IMovieItem iMovieItem);

    IMovieItem getPrevious(IMovieItem iMovieItem);

    int index(IMovieItem iMovieItem);

    int size();
}
