package com.EasyMovieTexture;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.view.Surface;
import android.widget.LinearLayout;

import com.google.android.exoplayer2.AL.ALCmd;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.util.Util;

import mipesc.video.streaming.exoplayerhlsextension.ExoFactory;
import mipesc.video.streaming.exoplayerhlsextension.tracking.AnalyticsProvider;
import mipesc.video.streaming.exoplayerhlsextension.tracking.Event;
import mipesc.video.streaming.exoplayerhlsextension.tracking.EventCollector;
import mipesc.video.streaming.exoplayerhlsextension.tracking.LoggingAnalyticsProvider;

/**
 * Created by lihongsheng on 11/14/17.
 */

public class MyExoplayer{
    private static final String USER_AGENT = "ExoPlayerHlsExtension";
    private SimpleExoPlayer player;
    private ExoFactory exoFactory;
    private LinearLayout debugRootView;
    private EventCollector eventCollector;
    private AnalyticsProvider analyticsProvider;
    private int resumeWindow;
    private long resumePosition;

    private boolean mPlayVideoWhenForegrounded;

    private long targetState = 0;
    private boolean startPos = false;

    private Context m_context;
    MyExoplayer(Activity m_activity){
        m_context = m_activity.getApplicationContext();
        exoFactory = new ExoFactory(m_context, USER_AGENT);
        analyticsProvider = new LoggingAnalyticsProvider();
        eventCollector = new EventCollector(m_context, analyticsProvider);
    }
    public void initializePlayer(Surface m_Surface) {
        System.out.println("initializePlayer");
        if (player == null) {
            ALCmd.CURRENT_MOVE_STATE = ALCmd.MOVE_STATE_FORWARD;
//            System.out.println("player == nul");
            eventCollector.signal(new Event(Event.EventType.PLAYBACK_INIT));
//            System.out.println("player == nul 1");
            player = exoFactory.buildExoPlayer();
//            System.out.println("player == nul 2");
//            player.addListener(this);
            player.setPlayWhenReady(true);
            boolean haveResumePosition = resumeWindow != C.INDEX_UNSET;
            if (haveResumePosition) {
//                System.out.println("player.seekTo(resumeWindow, resumePosition);");
                player.seekTo(resumeWindow, resumePosition);
            }
            Uri trackUri = Uri.parse("http://10.213.122.118:8080/video/hls/furion/index.m3u8");//全一样
            ALCmd.MOVE_STATE_LOOK_DOWN_URL = "http://10.213.122.118:8080/video/hls/furion/index.m3u8";
            ALCmd.MOVE_STATE_LOOK_UP_URL = "http://10.213.122.118:8080/video/hls/furion/index.m3u8";

//            Uri trackUri = Uri.parse("http://203.91.121.132:8080/hls/vr30m/index.m3u8");//CDN 地址
//            ALCmd.MOVE_STATE_LOOK_DOWN_URL = "http://203.91.121.132:8080/hls/vr30UD/index.m3u8";
//            ALCmd.MOVE_STATE_LOOK_UP_URL = "http://203.91.121.132:8080/hls/vr30DU/index.m3u8";
            //10.213.122.139
//            Uri trackUri = Uri.parse("http://10.213.122.139:8080/hls/vr30m/index.m3u8");//本地 Mac 地址
//            ALCmd.MOVE_STATE_LOOK_DOWN_URL = "http://10.213.122.139:8080/hls/vr30UD/index.m3u8";
//            ALCmd.MOVE_STATE_LOOK_UP_URL = "http://10.213.122.139:8080/hls/vr30DU/index.m3u8";
//            System.out.println("player == nul 3");
            int type = Util.inferContentType(trackUri);
//            System.out.println("player == nul 4");
            MediaSource mediaSource = exoFactory.buildMediaSource(exoFactory.buildDataSourceFactory(true), trackUri, "");
//            System.out.println("player == nul 5");
            player.setVideoSurface(m_Surface);
//            System.out.println("player == nul 6");
            player.prepare(mediaSource, !haveResumePosition, false);
            //暂时注释
//            player.setPlayWhenReady(false);
        }
    }

    public void ChangedMoveState(long state){
        if(state == 1)
        {
            //前进
            ALCmd.CURRENT_MOVE_STATE = ALCmd.MOVE_STATE_FORWARD;
        }else if(state == 2)
        {
            //后退
            ALCmd.CURRENT_MOVE_STATE = ALCmd.MOVE_STATE_BACK;
        }
        //模式原因注释,演示华为暂时注释
        if(state != 0)
        {
            return;
        }

        if(state == 6)
        {
            ALCmd.CURRENT_MOVE_STATE = ALCmd.MOVE_STATE_LOOK_UP;
        }
        if(state == 7)
        {
            ALCmd.CURRENT_MOVE_STATE = ALCmd.MOVE_STATE_LOOK_DOWN;
        }
    }

    public void SeekTo(long state)
    {
        if(targetState == state)
        {
            //防止重复
            return;
        }
        targetState = state;
        if(state == 0)
        {
            player.setPlayWhenReady(false);
        }else if (state == 1)
        {
            player.seekTo(0);
            player.setPlayWhenReady(true);
        }else if (state == 2)
        {
            player.seekTo(10000);
            player.setPlayWhenReady(true);
        }else if (state == 3)
        {
            player.seekTo(20000);
            player.setPlayWhenReady(true);
        }else if (state == 4)
        {
            player.seekTo(30000);
            player.setPlayWhenReady(true);
        }
    }

    public void Stop()
    {
        // Store off if we were playing so we know if we should start when we're foregrounded again.
        mPlayVideoWhenForegrounded = player.getPlayWhenReady();
        // Store off the last position our player was in before we paused it.
        resumePosition = player.getCurrentPosition();
        // Pause the player
        player.setPlayWhenReady(false);
    }

    public void Start() {
        if (player == null) {
            // init player
            System.out.println("player == nul Start");
        }
        // Seek to the last position of the player.
        player.seekTo(resumePosition);
        // Put the player into the last state we were in.
        player.setPlayWhenReady(mPlayVideoWhenForegrounded);
    }
}
