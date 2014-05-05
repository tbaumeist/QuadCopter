package com.tbaumeist.quadcontroller;

import java.io.IOException;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.hoho.android.usbserial.driver.UsbSerialDriver;

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
				mUsbDriver.write(new String(getCommString()).getBytes(), 0);
			} catch (IOException e) {
				listener.OnError(e.getMessage());
			}
			timerHandler.postDelayed(this, DELAY);
		}
	};

	private UsbSerialDriver mUsbDriver;
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
					if (mUsbDriver == null)
						return;
					size = mUsbDriver.read(buffer, 0);
					if (size > 0) {
						byte[] buf = new byte[size];
						System.arraycopy(buffer, 0, buf, 0, size);
						listener.OnMessageReceived(buf);
					}
				} catch (IOException e) {
					listener.OnError(e.getMessage());
					return;
				}
			}
		}
	}

	private ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);

	public CommunicationManager(UsbSerialDriver driver,
			CommunicationManagerListener listen)  {
		this.listener = listen;
		this.mUsbDriver = driver;
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

		return "AB\n";
	}

	public void initializeComm() {
		Thread t = new Thread() {
			public void run() {
				try {
					mUsbDriver.open();
					mUsbDriver.setDTR(false);
					mUsbDriver.setRTS(false);
					mUsbDriver.setParameters(9600, 8, UsbSerialDriver.STOPBITS_1, UsbSerialDriver.PARITY_NONE);
					mUsbDriver.setDTR(true);
					mUsbDriver.setRTS(true);
				} catch (IOException e) {
					listener.OnError(e.getMessage());
					return;
				}
				listener.OnInitializedFinished();
			}
		};
		t.start();
	}

	public void startComm() {
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
