package com.echain.net2;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;

class Messenger {
	private NetSystem mNetSystem;
	private SocketChannel mSocketChannel;
	private SelectionKey mKey;
	private Integer mSessionId;
	private LinkedList<NetSystem.Package> mOutPkgs = new LinkedList<NetSystem.Package>();
	private ByteBuffer mBuffer;
	private byte[] mData;
	private ByteArrayOutputStream mBos;
	ByteArrayInputStream mBis;
	
	public Messenger(NetSystem netSystem, SocketChannel socketChannel, SelectionKey key, Integer sessionId) throws Exception {
		mNetSystem = netSystem;
		mSocketChannel = socketChannel;
		mKey = key;
		mSessionId = sessionId;
		
		mBos = new ByteArrayOutputStream();
		mBuffer = ByteBuffer.allocate(1024);
		mData = new byte[1024];
		mBis = new ByteArrayInputStream(mData);
	}
	
	@Override
	protected void finalize() {
		try {
			stop();
			super.finalize();
		} catch (Exception e) {
		} catch (Throwable t) {
		}
	}
	
	public void stop() throws Exception {
		if (mSocketChannel.isConnected())
			mSocketChannel.close();
		mBos.close();
		mBis.close();
	}
	
	private NetSystem.IListener getListener() {
		return mNetSystem.mListener;
	}
	
	public void sendPackage(NetSystem.Package pkg) {
		synchronized (mOutPkgs) {
			mOutPkgs.push(pkg);
		}
		mKey.interestOps(mKey.interestOps() | SelectionKey.OP_WRITE);
	}
	
	public boolean doSendPackage() {
		boolean result = false;
		try {
			synchronized (mOutPkgs) {
				while (mOutPkgs.size() > 0) {
					mBos.reset();
					ObjectOutputStream out = new ObjectOutputStream(mBos);
					NetSystem.Package pkg = mOutPkgs.poll();
					out.writeObject(pkg);

					byte[] data = mBos.toByteArray();
					mBuffer.limit(data.length);
					mBuffer.put(data);
					mBuffer.flip();  // important
					while (mBuffer.hasRemaining())
						mSocketChannel.write(mBuffer);
					mBuffer.clear();
					result = true;
				};
			}
		} catch (Exception e) {
			System.out.print(e.toString() + "\n");
		}
		
		mKey.interestOps(mKey.interestOps() ^ SelectionKey.OP_WRITE);
		return result;
	}
	
	public boolean doReadPackage() {
		boolean result = true;
		try {
			mBis.reset();
			int len = 0;
			int totalLen = 0;
			while ((len = mSocketChannel.read(mBuffer)) > 0) {
				totalLen += len;
			}
			
			if (totalLen > 0) {
				mBuffer.flip();  // important
				mBuffer.get(mData, 0, totalLen);
				ObjectInputStream in = new ObjectInputStream(mBis);
				if (getListener() != null)
					getListener().onRecvPackage(mSessionId, (NetSystem.Package)in.readObject());
				mBuffer.clear();
			} else if (len < 0) {
				result = false;
				if (getListener() != null)
					getListener().onDisconnected(mSessionId);
			}
		} catch (Exception e) {
			System.out.print(e.toString() + "\n");
			result = false;
			if (getListener() != null)
				getListener().onDisconnected(mSessionId);
		}
		return result;
	}
}
