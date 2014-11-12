package com.echain.net;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.util.LinkedList;

public class Messenger {
	public int sessionId;
	
	private ReceivingThread mReceivingThread;
	private SendingThread mSendingThread;
	private Socket mSocket;
	private LinkedList<Package> mPkgQueue;
	private INotifyDisconnection mNotify;
	private IRecvListener mListener;
	private boolean mRunning;
	
	Messenger(Socket socket, INotifyDisconnection notify) {
		mSocket = socket;
		mNotify = notify;
		mPkgQueue = new LinkedList<Package>();
	}
	
	@Override
	protected void finalize() {
		stop();
		try {
			super.finalize();
		} catch (Throwable e) {
		}
	}

	void start() {
		mRunning = true;
		mReceivingThread = new ReceivingThread();
		mReceivingThread.start();
		mSendingThread = new SendingThread();
		mSendingThread.start();
	}
	
	void stop() {
		mRunning = false;
		synchronized (mPkgQueue) {
			mPkgQueue.notify();
		}
		try {
			mSocket.close();
			mReceivingThread.join();
			mSendingThread.join();
			
		} catch (Exception e) {
		}
	}
	
	public void registerIRecvListener(IRecvListener listener) {
		mListener = listener;
	}
	
	public void unregisterIRecvListener(IRecvListener listener) {
		mListener = null;
	}
	
	private void dispatchPackage(Package pkg) {
		if (mListener != null)
			mListener.onRecvPackage(pkg);
	}
	
	private void notifyDisconnection() {
		if (mNotify != null)
			mNotify.remoteDisconnected(this);
		if (mListener != null)
			mListener.onDisconnected();
	}
	
	public void sendPackage(Package pkg) {
		synchronized (mPkgQueue) {
			mPkgQueue.offer(pkg);
			mPkgQueue.notify();
		}
	}
	
	private class ReceivingThread extends Thread {
		@Override
		public void run() {
			ObjectInputStream in = null;
			try {
				in = new ObjectInputStream(mSocket.getInputStream());
				while (mRunning) {
					Package pkg = (Package)in.readObject();
					if (pkg != null) {
						dispatchPackage(pkg);
					} else {
						//
					}
				}
			} catch (Exception e) {
				// remote socket is closed
				if (mRunning) {
					mRunning = false;
					synchronized (mPkgQueue) {
						mPkgQueue.notify();
					}
					notifyDisconnection();
				}
			} finally {
				try {
					if (in != null)
						in.close();
				} catch (IOException e1) {
				}
			}
		}
	}
	
	private class SendingThread extends Thread {
		@Override
		public void run() {
			ObjectOutputStream out = null;
			try {
				out = new ObjectOutputStream(mSocket.getOutputStream());
				synchronized (mPkgQueue) {
					while (mRunning) {
						mPkgQueue.wait();
						while (mPkgQueue.size() > 0) {
							Package pkg = mPkgQueue.poll();
							out.writeObject(pkg);
							out.flush();
						}
					}
				}
			} catch (Exception e) {
			} finally {
				try {
					if (out != null)
						out.close();
				} catch (IOException e) {
				}
			}
		}
	}
	
	interface INotifyDisconnection {
		public void remoteDisconnected(Messenger messenger);
	}
	
	public interface IRecvListener {
		public void onRecvPackage(Package pkg);
		public void onDisconnected();
	}
	
	public static abstract class Package implements Serializable {
		private static final long serialVersionUID = -4559814833046019426L;
		public int what;
	}
}
