package com.example.chema.monisport.utils;

import android.os.Handler;
import android.util.Log;

import com.example.chema.monisport.Reproduccion;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;

/**
 * Created by Chema on 02/11/2017.
 */

public class customExoPlayerListener implements Player.EventListener {
    /**
     * Tag for Log
     */
    private static final String TAG = "ActividadPlayerListener";

    private Reproduccion reproductor;
    private Runnable mTimer2;
    private final Handler mHandler = new Handler();

    public customExoPlayerListener(Reproduccion r){
        reproductor=r;
    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        int segundoExtra=0;
        if(reproductor.getPlayer().getCurrentPosition()%1000>500){
            segundoExtra=1;
        }
        reproductor.setTiempoActual((reproductor.getPlayer().getCurrentPosition()/1000)+segundoExtra);
        reproductor.actualizarGraficas();
        if(playWhenReady){
            reproductor.setReproduciendo(true);
            startVideo();
        }else{
            reproductor.setReproduciendo(false);
            mHandler.removeCallbacks(mTimer2);
        }
        if(playbackState==Player.STATE_ENDED){
            reproductor.setReproduciendo(false);
            mHandler.removeCallbacks(mTimer2);
        }
    }

    @Override
    public void onTimelineChanged(Timeline timeline, Object manifest) {}
    @Override
    public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {}
    @Override
    public void onLoadingChanged(boolean isLoading) {}
    @Override
    public void onRepeatModeChanged(int repeatMode) {}
    @Override
    public void onPlayerError(ExoPlaybackException error) {}
    @Override
    public void onPositionDiscontinuity() {}
    @Override
    public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {}

    private void startVideo(){
        mHandler.removeCallbacks(mTimer2);
        mTimer2 = new Runnable() {
            @Override
            public void run() {
                int segundoExtra=0;
                if(reproductor.getPlayer().getCurrentPosition()%1000>500){
                    segundoExtra=1;
                }
                reproductor.setTiempoActual((reproductor.getPlayer().getCurrentPosition()/1000)+segundoExtra);

                reproductor.actualizarGraficas();
                mHandler.postDelayed(this, 1000);
            }
        };
        mHandler.postDelayed(mTimer2, 0);
    }

}
