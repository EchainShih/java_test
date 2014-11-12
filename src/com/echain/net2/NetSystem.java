package com.echain.net2;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Set;

public class NetSystem implements Runnable {
	private boolean mRunning;
	private ServerSocketChannel mServerChannel;
	private HashMap<Integer, Messenger> mMessengers = new HashMap<Integer, Messenger>();
	private int mNextSessionId = 0;
	private Selector mSelector;
	IListener mListener;
	
	public NetSystem() {
	}
	
	public void createServer(int port) throws Exception {
		if (mRunning)
			return;
		
		mSelector = Selector.open();
		mServerChannel = ServerSocketChannel.open();
		mServerChannel.configureBlocking(false);
		mServerChannel.bind(new InetSocketAddress(port));
		mServerChannel.register(mSelector, SelectionKey.OP_ACCEPT);
		start();
	}
	
	public Integer createClient(String host, int port) throws Exception {
		if (mRunning)
			return null;
		
		mSelector = Selector.open();
		SocketChannel socketChannel = SocketChannel.open();
		socketChannel.configureBlocking(false);
		socketChannel.connect(new InetSocketAddress("localhost", port));
		
		while(!socketChannel.finishConnect() ){
			
		}
		
		Integer id = Integer.valueOf(mNextSessionId);
		SelectionKey key = socketChannel.register(mSelector, SelectionKey.OP_READ);
		key.attach(id);
		Integer sessionId = Integer.valueOf(mNextSessionId++);
		synchronized (mMessengers) {
			Messenger messenger = new Messenger(this, socketChannel, key, sessionId);
			mMessengers.put(id, messenger);
		}
		start();
		return sessionId;
	}
	
	private void start() {
		mRunning = true;
		Thread thread = new Thread(this);
		thread.start();
	}
	
	public void stop() throws Exception {
		synchronized (mMessengers) {
			for (Integer id : mMessengers.keySet()) {
				Messenger messenger = mMessengers.get(id);
				messenger.stop();
			}
		}
		mMessengers.clear();
		if (mServerChannel != null)
			mServerChannel.close();
		mRunning = false;
		mSelector.wakeup();
	}
	
	public boolean disconnectClient(Integer sessionId) throws Exception{
		synchronized (mMessengers) {
			Messenger messenger = mMessengers.get(sessionId);
			if (messenger != null) {
				messenger.stop();
				mMessengers.remove(sessionId);
			} else {
				System.out.print("Session(" + sessionId.toString() + " does not exist\n");
				return false;
			}
		}
		return true;
	}
	
	public void registerListener(IListener listener) {
		mListener = listener;
	}
	
	public boolean sendPackage(Integer sessionId, Package pkg) {
		Messenger messenger = mMessengers.get(sessionId);
		if (messenger != null) {
			messenger.sendPackage(pkg);
			mSelector.wakeup();
		} else {
			System.out.print("Session(" + sessionId.toString() + " does not exist\n");
			return false;
		}
		return true;
	}

	@Override
	public void run() {
		try {
			while (mRunning) {
				mSelector.select();
				Set<SelectionKey> readKeys = mSelector.selectedKeys();
				
				for (SelectionKey key : readKeys) {
					if (key.isAcceptable()) {
						SocketChannel socketChannel = mServerChannel.accept();
						if (socketChannel != null) {
							socketChannel.configureBlocking(false);
							Integer sessionId = Integer.valueOf(mNextSessionId++);
							SelectionKey k = socketChannel.register(mSelector, SelectionKey.OP_READ);
							k.attach(sessionId);
							synchronized (mMessengers) {
								Messenger messenger = new Messenger(this, socketChannel, k, sessionId);
								mMessengers.put(sessionId, messenger);
								if (mListener != null)
									mListener.onConnected(sessionId);
							}
						}
					} else if (key.isWritable() && (key.interestOps() & SelectionKey.OP_WRITE) == SelectionKey.OP_WRITE) {
						Integer sessionId = (Integer)key.attachment();
						Messenger messenger = mMessengers.get(sessionId);
						if (!messenger.doSendPackage()) {
							messenger.stop();
							mMessengers.remove(sessionId);
						}
					} else if (key.isReadable()) {
						Integer sessionId = (Integer)key.attachment();
						Messenger messenger = mMessengers.get(sessionId);
						if (!messenger.doReadPackage()) {
							messenger.stop();
							mMessengers.remove(sessionId);
						}
					}
				}
			}
		} catch (Exception e) {
			System.out.print(e.toString() + "\n");
		}
	}
	
	public interface IListener {
		public void onRecvPackage(Integer sessionId, Package pkg);
		public void onConnected(Integer sessionId);
		public void onDisconnected(Integer sessionId);
	}
	
	public static class Package implements Serializable {
		private static final long serialVersionUID = -7609603393003678742L;
		public int what;
	}
}
