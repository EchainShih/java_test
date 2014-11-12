package com.echain.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public class NetClient {
	private Messenger mMessenger;
	
	public NetClient() {
		
	}
	
	@Override
	protected void finalize() {
		disconnect();
		try {
			super.finalize();
		} catch (Throwable e) {
		}
	}
	
	public Messenger connect(String host, int port) {
		if (mMessenger == null) {
			Socket socket = new Socket();
			try {
				socket.bind(null);
				socket.connect(new InetSocketAddress(host, port));
				mMessenger = new Messenger(socket, mNotifyDisconnection);
				mMessenger.start();
			} catch (IOException e) {
				try {
					socket.close();
				} catch (IOException exception) {
				}
			}
		}
		return mMessenger;
	}
	
	public void disconnect() {
		if (mMessenger != null) {
			mMessenger.stop();
			mMessenger = null;
		}
	}
	
	private Messenger.INotifyDisconnection mNotifyDisconnection = new Messenger.INotifyDisconnection() {
		@Override
		public void remoteDisconnected(Messenger messenger) {
			if (mMessenger == messenger)
				mMessenger = null;
		}
	};
}
