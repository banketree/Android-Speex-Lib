package com.libspeex.test.helper;

import android.content.Context;
import android.media.AudioManager;
import android.os.Message;


import com.eotu.audio.SpeexDecoder;
import com.thinkcore.event.TEventIdUtils;
import com.thinkcore.utils.THandler;
import com.thinkcore.utils.log.TLog;

import java.io.File;

public class AudioPlayer {
    private static final String Tag = AudioPlayer.class.getSimpleName();

    private String mCurrentPlayPath = null;
    private SpeexDecoder mSpeexdec = null;
    private Thread mThread = null;

    public static final int Audio_Stop_Play = TEventIdUtils.getNextEvent();

    private static AudioPlayer mThis = null;
    private AudioListener mAudioListener;
    private THandler<Object> mTHandler;

    private AudioPlayer() {
    }

    public static AudioPlayer getInstance() {
        if (null == mThis) {
            synchronized (AudioPlayer.class) {
                mThis = new AudioPlayer();
            }
        }
        return mThis;
    }

    public void initConfig() {
        if (mTHandler == null) {
            mTHandler = new THandler<Object>(this) {

                @Override
                public void handleMessage(Message msg) {
                    super.handleMessage(msg);

                    if (msg.what == Audio_Stop_Play) {
                        stopAnimation();
                    }
                }
            };
        }
    }

    public void release() {
        if (isPlaying()) {
            stopPlayer();
        }
        mThis = null;
    }

    // 语音播放的模式
    public void setAudioMode(int mode, Context ctx) {
        if (mode != AudioManager.MODE_NORMAL
                && mode != AudioManager.MODE_IN_CALL) {
            return;
        }
        AudioManager audioManager = (AudioManager) ctx
                .getSystemService(Context.AUDIO_SERVICE);
        audioManager.setMode(mode);
    }

    /**
     * messagePop调用
     */
    public int getAudioMode(Context ctx) {
        AudioManager audioManager = (AudioManager) ctx
                .getSystemService(Context.AUDIO_SERVICE);
        return audioManager.getMode();
    }

    private void stopAnimation() {
        if (mAudioListener != null) {
            mAudioListener.onStop();
        }
    }

    private void onStopPlayer() {
        mCurrentPlayPath = null;

        try {
            if (null != mThread) {
                mThread.interrupt();
                mThread = null;
                Thread.currentThread().interrupt();
            } else {
            }
        } catch (Exception e) {
            TLog.e(Tag, e.getMessage());
        }
    }

    public void stopPlayer() {
        onStopPlayer();
        mTHandler.sendEmptyMessage(Audio_Stop_Play);
    }

    public boolean isPlaying() {
        return null != mThread;
    }

    public void startPlay(String filePath, AudioListener audioListener)
            throws Exception {
        mAudioListener = audioListener;

        onStopPlayer();

        this.mCurrentPlayPath = filePath;
        try {
            mSpeexdec = new SpeexDecoder(new File(mCurrentPlayPath));
            RecordPlayThread rpt = new RecordPlayThread();
            if (null == mThread)
                mThread = new Thread(rpt);
            mThread.start();
        } catch (Exception e) { // 关闭动画很多地方需要写，是不是需要重新考虑一下
            TLog.e(Tag, e.getMessage());
            mTHandler.sendEmptyMessage(Audio_Stop_Play);
            throw new Exception();
        }
    }

    class RecordPlayThread extends Thread {
        public void run() {
            try {
                if (null != mSpeexdec)
                    mSpeexdec.decode();
            } catch (Exception e) {
                TLog.e(Tag, e.getMessage());
            } finally {
                mTHandler.sendEmptyMessage(Audio_Stop_Play);
            }
        }
    }

    ;

    public String getCurrentPlayPath() {
        return mCurrentPlayPath;
    }

    /**
     * modify speexdec 由于线程模型
     */
    public interface AudioListener {
        public void onStop();
    }
}
