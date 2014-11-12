package com.echain.net;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;

public class NetServer {
	private int mPort;
	private int mAllowedConnection;
	private ServerSocket mServerSocket;
	private ServerThread mServerThread;
	private IClientStateListener mListener;
	private LinkedList<Messenger> mConnectedClients;
	
	public NetServer(int port, int allowedConnection) {
		mPort = port;
		mAllowedConnection = allowedConnection;
		mConnectedClients = new LinkedList<Messenger>();
	}
	
	@Override
	protected void finalize() {
		stop();
		try {
			super.finalize();
		} catch (Throwable e) {
		}
	}
	
	public boolean start() {
		try {
			mServerSocket = new ServerSocket(mPort);
		} catch (IOException e) {
			return false;
		}
		mServerThread = new ServerThread();
		mServerThread.start();
		return true;
	}
	
	public void stop() {
		try {
			mServerSocket.close();
		} catch (IOException e) {
		}
		for (Messenger messenger : mConnectedClients) {
			messenger.stop();
		}
		mConnectedClients.clear();
		try {
			mServerThread.join();
		} catch (InterruptedException e) {
		}
	}
	
	public void setClientStateListener(IClientStateListener listener) {
		mListener = listener;
	}
	
	public void disconnect(Messenger messenger) {
		messenger.stop();
		mConnectedClients.remove(messenger);
	}
	
	private class ServerThread extends Thread {
		@Override
		public void run() {
			while (true) {
				try {
					Socket clientSocket = mServerSocket.accept();
					Messenger messenger = new Messenger(clientSocket, mNotifyDisconnection);
					messenger.start();
					mConnectedClients.add(messenger);
					if (mListener != null)
						mListener.onClientConnected(messenger);
				} catch (IOException e) {
					break;
				}
			}
		}
	}
	
	private Messenger.INotifyDisconnection mNotifyDisconnection = new Messenger.INotifyDisconnection() {
		@Override
		public void remoteDisconnected(Messenger messenger) {
			mConnectedClients.remove(messenger);
			if (mListener != null)
				mListener.onClientDisconnected(messenger);
		}
	};
	
	public interface IClientStateListener {
		public void onClientConnected(Messenger messenger);
		public void onClientDisconnected(Messenger messenger);
	}
}
