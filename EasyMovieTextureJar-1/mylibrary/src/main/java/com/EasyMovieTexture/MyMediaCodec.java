package com.EasyMovieTexture;


import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.Surface;
import android.widget.MediaController;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by lihongsheng on 10/17/17.
 */

public class MyMediaCodec {
//    private static final String SAMPLE = Environment.getExternalStorageDirectory() + "/viking.mp4";
    public MediaExtractor extractor;
    public MediaCodec decoder = null;
    public Surface renderSurface;
    private ByteBuffer[] inputBuffers = null;
    private ByteBuffer[] outputBuffers = null;
    private BufferInfo info;
    private ReadThread mReadThread = null;
    private DisplayThread mRDisplayThread = null;
    private boolean isPlayerStop = false;

    private static final String TAG = "MyMediaCodec";

    //Address
    public String myNginxIp = "http://10.11.12.100:8080/";
    public String myDownloadFileName = "viking.mp4";
    public String sdcardPath = Environment.getExternalStorageDirectory() + "/DownloadManager/";
    //Address

    //For Seek
    private PlayerThread mPlayer = null;
    private MediaController mediaController;
    private long lastPresentationTimeUs;
    private boolean seeked = false;
    private long startMs;
    private long diff = 0;
    private long lastSeekedTo = 0;
    private long lastCorrectPresentationTimeUs = 0;
    //For Seek



    MyMediaCodec(){
        extractor = new MediaExtractor();
    }

    public void play(){
        //注释第一种播放方式
//        //For init
//        Init();
//        //readThread
//        mReadThread = new ReadThread();
//        mReadThread.start();
//        //DisplayThread
//        mRDisplayThread = new DisplayThread();
//        mRDisplayThread.start();
        //注释第一种播放方式
        System.out.println("mPlayer = new PlayerThread();");
        mPlayer = new PlayerThread();
        mPlayer.extractor = extractor;
        System.out.println("mPlayer.start();");
        mPlayer.start();

    }

    private void setDataSource(String str)
    {
//        extractor.setDataSource(str);
    }

    public void stop(){
        isPlayerStop = true;
    }

    public void resume(){
        isPlayerStop = false;
    }

    public void setSurface(Surface surface){
        renderSurface = surface;
        if(renderSurface != null)
        {
            Log.d(TAG,"renderSurface != null  1");
        }
    }

    private void Init(){
        for (int i = 0; i < extractor.getTrackCount(); i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("video/")) {
                try {
                    extractor.selectTrack(i);
                    decoder = MediaCodec.createDecoderByType(mime);
                    if(renderSurface != null)
                    {
                        Log.d(TAG,"renderSurface != null  2");
                    }
                    Log.d(TAG,"s=======s=======s=======");
                    decoder.configure(format, renderSurface, null, 0);
                    Log.d(TAG,"s=======s=======s=======");
                }catch (IOException e)
                {
                    e.printStackTrace();
                }
                break;
            }
        }
        if (decoder == null) {
            Log.e("DecodeActivity", "Can't find video info!");
            return;
        }
        decoder.start();
        inputBuffers = decoder.getInputBuffers();
        outputBuffers = decoder.getOutputBuffers();
        info = new BufferInfo();
    }

    //ReadThread as ijk
    private class ReadThread extends Thread{
        @Override
        public void run() {
            while (!Thread.interrupted()){
                if(isPlayerStop)
                {
                    continue;
                }
                boolean isEOS = false;
                if (!isEOS) {
                    int inIndex = decoder.dequeueInputBuffer(10000);
                    if (inIndex >= 0) {
                        ByteBuffer buffer = inputBuffers[inIndex];
                        int sampleSize = extractor.readSampleData(buffer, 0);
                        if (sampleSize < 0) {
                            // We shouldn't stop the playback at this point, just pass the EOS
                            // flag to decoder, we will get it again from the
                            // dequeueOutputBuffer
                            Log.d("queueInputBuffer_if", String.valueOf(inIndex));
                            decoder.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            isEOS = true;
                        } else {
                            Log.d("queueInputBuffer_else", String.valueOf(inIndex));
                            decoder.queueInputBuffer(inIndex, 0, sampleSize, extractor.getSampleTime(), 0);
                            extractor.advance();
                        }
                    }
                }
            }
        }
    }

    //DisplayThread as ijk
    private class DisplayThread extends Thread {
        @Override
        public void run() {
            long startMs = System.currentTimeMillis();

            while (!Thread.interrupted()) {
                if(isPlayerStop)
                {
                    continue;
                }
                int outIndex = decoder.dequeueOutputBuffer(info, 10000);
                Log.d("dequeueOutputBuffer", String.valueOf(outIndex));
                switch (outIndex) {
                    case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                        Log.d("DecodeActivity", "INFO_OUTPUT_BUFFERS_CHANGED");
                        outputBuffers = decoder.getOutputBuffers();
                        break;
                    case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                        Log.d("DecodeActivity", "New format " + decoder.getOutputFormat());
                        break;
                    case MediaCodec.INFO_TRY_AGAIN_LATER:
                        Log.d("DecodeActivity", "dequeueOutputBuffer timed out!");
                        break;
                    default:
                        ByteBuffer buffer = outputBuffers[outIndex];
                        Log.v("DecodeActivity", "We can't use this buffer but render it due to the API limit, " + buffer);

                        // We use a very simple clock to keep the video FPS, or the video
                        // playback will be too fast
                        while (info.presentationTimeUs / 1000 > System.currentTimeMillis() - startMs) {
                            try {
                                sleep(10);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                                break;
                            }
                        }
                        Log.d("releaseOutputBuffer", String.valueOf(outIndex));
                        decoder.releaseOutputBuffer(outIndex, true);
                        //playerStop();
                        break;
                }


                // All decoded frames have been rendered, we can stop playing now
                if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    Log.d("DecodeActivity", "OutputBuffer BUFFER_FLAG_END_OF_STREAM");
                    break;
                }
            }

            decoder.stop();
            decoder.release();
            extractor.release();
        }
    }




    private class PlayerThread extends Thread {
        Handler handler = new Handler();
        //		MediaController.MediaPlayerControl mediaPlayerControl;
        Handler handler1 = new Handler();
        long lastOffset = 0;
        boolean isPlaying = false;
        private MediaExtractor extractor;
        private MediaCodec decoder;
        private Surface surface;
        private BufferInfo info;
        private long duration;
        private AudioTrack audioTrack;

//		public PlayerThread(MediaController.MediaPlayerControl mediaPlayerControl, Surface surface) {
//			this.surface = surface;
//			this.mediaPlayerControl = mediaPlayerControl;
//		}


        @Override
        public void run() {
            MediaCodecPlay();
        }

        private void MediaCodecPlay() {
//            extractor = new MediaExtractor();
            try {

                System.out.println("setDataSource: "+ sdcardPath + myDownloadFileName);
                extractor.setDataSource(sdcardPath + myDownloadFileName);
            }catch (IOException e)
            {
                e.printStackTrace();
            }
            MediaFormat format = null;
            for (int i = 0; i < extractor.getTrackCount(); i++) {
                format = extractor.getTrackFormat(i);
                String mime = format.getString(MediaFormat.KEY_MIME);
                duration = format.getLong(MediaFormat.KEY_DURATION) / 1000;

                Log.d(TAG, "MIME: " + mime);

                if (mime.startsWith("video/")) {
                    try {
                        extractor.selectTrack(i);
                        decoder = MediaCodec.createDecoderByType(mime);
                        decoder.configure(format, renderSurface, null, 0);
                        break;
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                }
            }

            decoder.start();

            ByteBuffer[] inputBuffers = decoder.getInputBuffers();
            ByteBuffer[] outputBuffers = decoder.getOutputBuffers();
            info = new BufferInfo();
            startMs = System.currentTimeMillis();
            //play();

            boolean isEOS = false;
            while (!Thread.interrupted()) {
                if (!isEOS) {
                    int inIndex = decoder.dequeueInputBuffer(1000);
                    if (inIndex >= 0) {
                        ByteBuffer buffer = inputBuffers[inIndex];

                        int sampleSize = extractor.readSampleData(buffer, 0);
                        if (sampleSize < 0) {
                            // We shouldn't stop the playback at this point, just pass the EOS
                            // flag to decoder, we will get it again from the
                            // dequeueOutputBuffer
                            Log.d("DecodeActivity", "InputBuffer BUFFER_FLAG_END_OF_STREAM");
                            decoder.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            isEOS = true;
                        } else {
                            Log.d(TAG, "Queue Input Buffer at position: " + info.presentationTimeUs);
                            decoder.queueInputBuffer(inIndex, 0, sampleSize, extractor.getSampleTime(), 0);
                            extractor.advance();
                        }
                    }
                }

                int outIndex = decoder.dequeueOutputBuffer(info, 1000);

                if (info.presentationTimeUs < lastPresentationTimeUs) {      // correct timing playback issue for some videos
                    startMs = System.currentTimeMillis();
                    lastCorrectPresentationTimeUs = lastPresentationTimeUs;
                }


                switch (outIndex) {
                    case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                        Log.d("DecodeActivity", "INFO_OUTPUT_BUFFERS_CHANGED");
                        outputBuffers = decoder.getOutputBuffers();
                        break;
                    case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                        Log.d("DecodeActivity", "New format " + decoder.getOutputFormat());
                        break;
                    case MediaCodec.INFO_TRY_AGAIN_LATER:
                        Log.d("DecodeActivity", "dequeueOutputBuffer timed out!");
                        break;
                    default:
                        ByteBuffer buffer = outputBuffers[outIndex];

                        //Log.v("DecodeActivity", "We can't use this buffer but render it due to the API limit, " + buffer);

//                        We use a very simple clock to keep the video FPS, or the video
//                        playback will be too fast

                        Log.d(TAG, "Original Presentation time: " + info.presentationTimeUs / 1000 + ", Diff PT: " + (info.presentationTimeUs / 1000 - lastOffset) + " : System Time: " + (System.currentTimeMillis() - startMs));

                        lastPresentationTimeUs = info.presentationTimeUs;

                        if (seeked && Math.abs(info.presentationTimeUs / 1000 - lastOffset) < 100)
                            seeked = false;

                        while (!seeked && (info.presentationTimeUs / 1000 - lastOffset) > System.currentTimeMillis() - startMs) {
                            try {
                                sleep(5);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                                break;
                            }
                        }

                        decoder.releaseOutputBuffer(outIndex, true);
                        break;
                }

                // All decoded frames have been rendered, we can stop playing now
                if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    Log.d("DecodeActivity", "OutputBuffer BUFFER_FLAG_END_OF_STREAM");
                    break;
                }
            }

            decoder.stop();
            decoder.release();
            extractor.release();
        }
        public void seekTo(int time) {
            seeked = true;
            Log.d(TAG, "SeekTo Requested to : " + time);
            Log.d(TAG, "SampleTime Before SeekTo : " + extractor.getSampleTime() / 1000);
            Log.d(TAG, "SampleTime Before SeekTo : " + extractor.getSampleTime());
            extractor.seekTo(time*1000, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
            Log.d(TAG, "SampleTime After SeekTo : " + extractor.getSampleTime() / 1000);
            Log.d(TAG, "SampleTime After SeekTo : " + extractor.getSampleTime());
            lastOffset = extractor.getSampleTime() / 1000;
            Log.d(TAG, "lastOffset : " + lastOffset);
            startMs = System.currentTimeMillis();
            Log.d(TAG, "startMs : " + startMs);
            diff = (lastOffset - lastPresentationTimeUs / 1000);
            Log.d(TAG, "SeekTo with diff : " + diff);
        }
    }

}
