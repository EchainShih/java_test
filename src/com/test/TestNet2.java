package com.test;

import java.util.ArrayList;

import com.echain.net2.NetSystem;

public class TestNet2 {
	public static void main(String[] argv) {
		if (argv.length != 1) {
			System.out.print("server or client?\n");
			return;
		}
		
		if (argv[0].equals("s")) {
			Server s = new Server();
			s.run();
		} else if (argv[0].equals("c")) {
			Client c = new Client();
			c.run();
		}
	}
	
	public static class MyPkg extends NetSystem.Package {
		private static final long serialVersionUID = 7085855447037406918L;
		
		public String msg;
		public MyPkg() {
			what = 1;
		}
	}
	
	public static class Server implements NetSystem.IListener {
		private NetSystem mServer;
		private ArrayList<Integer> mSessions = new ArrayList<Integer>();
		
		public void run() {
			try {
				mServer = new NetSystem();
				mServer.registerListener(this);
				mServer.createServer(1234);
				System.out.print("Server created, waiting for client...\n");
				
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
					} else if (cmd.indexOf("down") == 0) {
						String[] ss = cmd.split(" ");
						if (ss.length > 1) {
							Integer sessionId = Integer.valueOf(ss[1]);
							if (mServer.disconnectClient(sessionId)) {
								mSessions.remove(sessionId);
								System.out.print("Client disconnected, sessionId = " + sessionId.intValue() + "\n");
							}
						}
					} else {
						MyPkg pkg = new MyPkg();
						pkg.msg = cmd;
						for (Integer sessionId : mSessions) {
							mServer.sendPackage(sessionId, pkg);
						}
					}
				}
			} catch (Exception e) {
				System.out.print(e.toString() + "\n");
			}
		}

		@Override
		public void onRecvPackage(Integer sessionId, NetSystem.Package pkg) {
			MyPkg myPkg = (MyPkg)pkg;
			System.out.print("From Client: " + myPkg.msg + "\n");
		}

		@Override
		public void onConnected(Integer sessionId) {
			System.out.print("Client connected, sessionId = " + sessionId.intValue() + "\n");
			mSessions.add(sessionId);
		}

		@Override
		public void onDisconnected(Integer sessionId) {
			System.out.print("Client disconnected, sessionId = " + sessionId.intValue() + "\n");
			mSessions.remove(sessionId);
		}
	}
	
	public static class Client implements NetSystem.IListener {
		private NetSystem mClient;
		private Integer mSessionId;
		
		public void run() {
			try {
				mClient = new NetSystem();
				mClient.registerListener(this);
				mSessionId = mClient.createClient("localhost", 1234);
				System.out.print("Connect to server:\n");
				
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
						mClient.stop();
						break;
					} else {
						if (mClient != null) {
							MyPkg pkg = new MyPkg();
							pkg.msg = cmd;
							mClient.sendPackage(mSessionId, pkg);
						}
					}
				}
			} catch (Exception e) {
				System.out.print(e.toString() + "\n");
				System.out.print("Connect to server failed\n");
			}
		}

		@Override
		public void onRecvPackage(Integer sessionId, NetSystem.Package pkg) {
			MyPkg myPkg = (MyPkg)pkg;
			System.out.print("Echo: " + myPkg.msg + "\n");
		}

		@Override
		public void onConnected(Integer sessionId) {
		}

		@Override
		public void onDisconnected(Integer sessionId) {
			try {
				mClient.stop();
				mSessionId = null;
				mClient = null;
				
				System.out.write("exit\r\n".getBytes());
				System.out.flush();
				System.exit(0);
			} catch (Exception e) {
				System.out.print(e.toString());
			}
		}
	}
}
