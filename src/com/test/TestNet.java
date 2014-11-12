package com.test;

import java.io.IOException;
import java.util.LinkedList;

import com.echain.net.Messenger;
import com.echain.net.NetClient;
import com.echain.net.NetServer;
import com.echain.net.Messenger.Package;

public class TestNet {
	public static void __main(String[] argv) {
		if (argv[0].equals("s")) {
			Server s = new Server();
			s.run();
		} else if (argv[0].equals("c")) {
			Client c = new Client();
			c.run();
		}
	}
	
	public static class MyPkg extends Messenger.Package {
		private static final long serialVersionUID = -3236793643013347088L;
		
		public String msg;
		public MyPkg() {
			what = 1;
		}
	}
	
	public static class Server implements NetServer.IClientStateListener {
		private NetServer mServer;
		private LinkedList<Messenger> mMessenger = new LinkedList<Messenger>();
		
		public void run() {
			mServer = new NetServer(1234, 1);
			mServer.setClientStateListener(this);
			mServer.start();
			
			try {
				while (true) {
					String cmd = "";
					char c;
					while ((c = (char)System.in.read()) > 0) {
						System.out.print(c);
						if (c == '\r') {
						} else if (c == '\n') {
							break;
						} else {
							cmd += c;
						}
					}
					
					if (cmd.equals("exit")) {
						mServer.stop();
						break;
					} else {
						MyPkg pkg = new MyPkg();
						pkg.msg = cmd;
						for (Messenger m : mMessenger) {
							m.sendPackage(pkg);
						}
					}
				}
			} catch (IOException e) {
				
			}
		}

		@Override
		public void onClientConnected(Messenger messenger) {
			System.out.print("Client connected\n");
			messenger.registerIRecvListener(mRecvListener);
			mMessenger.add(messenger);
		}

		@Override
		public void onClientDisconnected(Messenger messenger) {
			System.out.print("Client disconnected\n");
			mMessenger.remove(messenger);
		}
		
		private Messenger.IRecvListener mRecvListener = new Messenger.IRecvListener() {
			public void onRecvPackage(Package pkg) {
				MyPkg myPkg = (MyPkg)pkg;
				System.out.print("From Client: " + myPkg.msg + "\n");
			}
			
			@Override
			public void onDisconnected() {
				// 
			}
		};
	}
	
	public static class Client {
		private NetClient mClient;
		private Messenger mMessenger;
		
		public void run() {
			mClient = new NetClient();
			mMessenger = mClient.connect("localhost", 1234);
			if (mMessenger != null) {
				System.out.print("Connect to server:\n");
				mMessenger.registerIRecvListener(mRecvListener);
			} else {
				System.out.print("Connect to server failed\n");
				return;
			}
			
			try {
				while (true) {
					String cmd = "";
					char c;
					while ((c = (char)System.in.read()) > 0) {
						System.out.print(c);
						if (c == '\r') {
						} else if (c == '\n') {
							break;
						} else {
							cmd += c;
						}
					}
					
					if (cmd.equals("exit")) {
						mClient.disconnect();
						break;
					} else {
						if (mMessenger != null) {
							MyPkg pkg = new MyPkg();
							pkg.msg = cmd;
							mMessenger.sendPackage(pkg);
						}
					}
				}
			} catch (IOException e) {
				
			}
		}
		
		private Messenger.IRecvListener mRecvListener = new Messenger.IRecvListener() {
			public void onRecvPackage(Package pkg) {
				MyPkg myPkg = (MyPkg)pkg;
				System.out.print("Echo: " + myPkg.msg + "\n");
			}
			
			@Override
			public void onDisconnected() {
				mMessenger = null;
				try {
					System.out.write("exit\r\n".getBytes());
					System.out.flush();
					System.exit(0);
				} catch (IOException e) {
				}
			}
		};
	}
}
