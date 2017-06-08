package com.libspeex.test.helper;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import com.eotu.audio.SpeexEncoder;
import com.thinkcore.utils.log.TLog;

public class AudioRecorder implements Runnable {
	private static final String Tag = AudioRecorder.class.getSimpleName();

	private volatile boolean mIsRecording;
	private final Object mMutex = new Object();
	private static final int mFrequency = 8000;
	private static final int mAudioEncoding = AudioFormat.ENCODING_PCM_16BIT;
	public static int mPackagesize = 160;// 320
	public static int MAX_SOUND_RECORD_TIME = 30;// 30秒

	private String mFileName = null;
	private float mRecordTime = 0;
	private long mStartTime = 0;
	private long mEndTime = 0;
	private long mMaxVolumeStart = 0;
	private long mMaxVolumeEnd = 0;
	private int mVolume = 0;
	private static AudioRecord mRecordInstance = null;
	public static final String Field_Suffix = ".spx";
	private RecorderListener mListener;

	public AudioRecorder(String fileName) {
		super();
		mFileName = fileName;
		mVolume = 0;
	}

	public void startRecord() {
		setRecording(true);
		new Thread(this).start();
	}

	public void stopRecord(RecorderListener listener) {
		mListener = listener;
		if (isRecording()) {
			setRecording(false);
		}
	}

	public void run() {
		try {
			TLog.d(Tag, "chat#audio#in audio thread");
			SpeexEncoder encoder = new SpeexEncoder(this.mFileName);
			Thread encodeThread = new Thread(encoder);
			encoder.setRecording(true);
			TLog.d(Tag, "chat#audio#encoder thread starts");
			encodeThread.start();

			synchronized (mMutex) {
				while (!this.mIsRecording) {
					try {
						mMutex.wait();
					} catch (InterruptedException e) {
						throw new IllegalStateException("Wait() interrupted!",
								e);
					}
				}
			}

			android.os.Process
					.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

			int bufferRead = 0;
			int bufferSize = AudioRecord.getMinBufferSize(mFrequency,
					AudioFormat.CHANNEL_IN_MONO, mAudioEncoding);

			short[] tempBuffer = new short[mPackagesize];
			try {
				if (null == mRecordInstance) {
					mRecordInstance = new AudioRecord(
							MediaRecorder.AudioSource.MIC, mFrequency,
							AudioFormat.CHANNEL_IN_MONO, mAudioEncoding,
							bufferSize);
				}

				mRecordInstance.startRecording();
				mRecordTime = 0;
				mStartTime = System.currentTimeMillis();
				mMaxVolumeStart = System.currentTimeMillis();
				while (this.mIsRecording) {
					mEndTime = System.currentTimeMillis();
					mRecordTime = (float) ((mEndTime - mStartTime) / 1000.0f);
					if (mRecordTime >= MAX_SOUND_RECORD_TIME) {
						// MessageActivity.getUiHandler().sendEmptyMessage(
						// HandlerConstant.RECORD_AUDIO_TOO_LONG);
						break;
					}

					bufferRead = mRecordInstance.read(tempBuffer, 0,
							mPackagesize);
					if (bufferRead == AudioRecord.ERROR_INVALID_OPERATION) {
						throw new IllegalStateException(
								"read() returned AudioRecord.ERROR_INVALID_OPERATION");
					} else if (bufferRead == AudioRecord.ERROR_BAD_VALUE) {
						throw new IllegalStateException(
								"read() returned AudioRecord.ERROR_BAD_VALUE");
					} else if (bufferRead == AudioRecord.ERROR_INVALID_OPERATION) {
						throw new IllegalStateException(
								"read() returned AudioRecord.ERROR_INVALID_OPERATION");
					}
					encoder.putData(tempBuffer, bufferRead);
					mMaxVolumeEnd = System.currentTimeMillis();
					setMaxVolume(tempBuffer, bufferRead);
				}
			} catch (Exception e) {
				TLog.e(Tag, e.getMessage());
			} finally {
				encoder.setRecording(false);
				if (mRecordInstance != null) {
					mRecordInstance.stop();
					mRecordInstance.release();
					mRecordInstance = null;
				} else {
				}
			}
		} catch (Exception e) {
			TLog.e(Tag, e.getMessage());
		}

		if (mListener != null)
			mListener.onStop(mFileName);
	}

	private void setMaxVolume(short[] buffer, int readLen) {
		try {
			if (mMaxVolumeEnd - mMaxVolumeStart < 100) {
				return;
			}
			mMaxVolumeStart = mMaxVolumeEnd;
			int max = 0;
			for (int i = 0; i < readLen; i++) {
				if (Math.abs(buffer[i]) > max) {
					max = Math.abs(buffer[i]);
				}
			}

			mVolume = max;
		} catch (Exception e) {
			TLog.e(Tag, e.getMessage());
		}
	}

	public int getRecordVolume() {
		return mVolume;
	}

	public float getRecordTime() {
		return mRecordTime;
	}

	public void setRecordTime(float len) {
		mRecordTime = len;
	}

	public void setRecording(boolean isRec) {
		synchronized (mMutex) {
			this.mIsRecording = isRec;
			if (this.mIsRecording) {
				mMutex.notify();
			}
		}
	}

	public boolean isRecording() {
		synchronized (mMutex) {
			return mIsRecording;
		}
	}

	/**
	 * modify speexdec 由于线程模型
	 * */
	public interface RecorderListener {
		public void onStop(String filePath);
	}
}
