package com.tbaumeist.quadcontroller;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import android.os.Handler;

public class CommunicationManager {

	private int leftX, leftY, rightX, rightY;

	private final int DELAY = 1000;
	private boolean stopTimer = false;
	private Handler timerHandler = new Handler();
	private Runnable timerRunnable = new Runnable() {

		@Override
		public void run() {
			if(stopTimer)
				return;
			try {
				mOutputStream.write(new String(getCommString()).getBytes());
				mOutputStream.write('\n');
			} catch (IOException e) {
				e.printStackTrace();
			}
			timerHandler.postDelayed(this, DELAY);
		}
	};

	private SerialPort serialPort;
	protected OutputStream mOutputStream;
	private InputStream mInputStream;
	private ReadThread mReadThread;
	private CommunicationManagerListener listener;

	private class ReadThread extends Thread {
		@Override
		public void run() {
			super.run();
			while (!isInterrupted()) {
				int size;
				try {
					byte[] buffer = new byte[64];
					if (mInputStream == null)
						return;
					size = mInputStream.read(buffer);
					if (size > 0) {
						listener.OnMessageReceived(buffer, size);
					}
				} catch (IOException e) {
					e.printStackTrace();
					return;
				}
			}
		}
	}

	private ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);

	public CommunicationManager(SerialPort port,
			CommunicationManagerListener listen) {
		this.listener = listen;
		this.serialPort = port;
	}

	public void setLeft(int x, int y) {
		lock.writeLock().lock();
		leftX = x;
		leftY = y;
		lock.writeLock().unlock();
	}

	public void setRight(int x, int y) {
		lock.writeLock().lock();
		rightX = x;
		rightY = y;
		lock.writeLock().unlock();
	}

	public String getCommString() {
		lock.readLock().lock();
		int rX = rightX;
		int rY = rightY;
		int lX = leftX;
		int lY = leftY;
		lock.readLock().unlock();

		return "AB";
	}

	public void initializeComm() {
		Thread t = new Thread() {
			public void run() {
				// TODO: init comm with quad-copter

				try {
					sleep(4000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				listener.OnInitializedFinished();
			}
		};
		t.start();
	}

	public void startComm() {
		mOutputStream = serialPort.getOutputStream();
		mInputStream = serialPort.getInputStream();

		/* Create a receiving thread */
		mReadThread = new ReadThread();
		mReadThread.start();
		stopTimer = false;
		timerHandler.postDelayed(timerRunnable, 0);
	}

	public void stopComm() {
		// TODO: send disconnect commands

		stopTimer = true;
		if (mReadThread != null)
			mReadThread.interrupt();
	}
}
