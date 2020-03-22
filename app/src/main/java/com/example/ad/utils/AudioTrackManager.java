package com.example.ad.utils;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;

public class AudioTrackManager {
    public static final String TAG = "AudioTrackManager";
    private AudioTrack audioTrack;
    private Thread recordThread;
    private boolean isStart = false;
    private static AudioTrackManager mInstance;
    private int bufferSize;
    private byte[] audioPcm = null;

    public AudioTrackManager() {
        bufferSize = AudioTrack.getMinBufferSize(16000, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 16000, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize * 2, AudioTrack.MODE_STREAM);
    }

    /**
     * 获取单例引用
     *
     * @return
     */
    public static AudioTrackManager getInstance() {
        if (mInstance == null) {
            synchronized (AudioTrackManager.class) {
                if (mInstance == null) {
                    mInstance = new AudioTrackManager();
                }
            }
        }
        return mInstance;
    }

    /**
     * 销毁线程方法
     */
    private void destroyThread() {
        try {
            isStart = false;
            while (null != recordThread && Thread.State.RUNNABLE == recordThread.getState()) {
                try {
                    Thread.sleep(500);
                    recordThread.interrupt();
                } catch (Exception e) {
                    recordThread = null;
                }
            }
            recordThread = null;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            recordThread = null;
        }
    }

    /**
     * 启动播放线程
     */
    private void startThread() {
        isStart = true;
        if (recordThread == null) {
            recordThread = new Thread(recordRunnable);
            recordThread.start();
        }
    }

    /**
     * 播放线程
     */
    Runnable recordRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
                byte[] tempBuffer = new byte[bufferSize];
                int len = 0;
                int readCount = 0;
                if (null != audioPcm && (audioPcm.length > 0)) {
                    int totalCount = audioPcm.length;
                    while (readCount < totalCount && isStart) {
                        if (readCount + tempBuffer.length > totalCount) {
                            len = totalCount - readCount;
                        } else {
                            len = tempBuffer.length;
                        }
                        System.arraycopy(audioPcm, readCount,tempBuffer, 0, len);
                        audioTrack.play();
                        audioTrack.write(tempBuffer, 0, len);
                        readCount += len;
                    }
                }
                stopPlay();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    };

    /**
     * 开始播放
     */
    public void startPlay(byte[] pcm) {
        try {
            destroyThread();
            startThread();
            audioPcm = new byte[pcm.length];
            System.arraycopy(pcm, 0, audioPcm, 0, pcm.length);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 停止播放
     */
    public void stopPlay() {
        try {
            destroyThread();
            if (audioTrack != null) {
                if (audioTrack.getState() == AudioRecord.STATE_INITIALIZED) {
                    audioTrack.stop();
                }
//                if (audioTrack != null) {
//                    audioTrack.release();
//                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
