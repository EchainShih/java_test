package com.test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class Test {
	public static void __main(String[] argv) {
		if (argv[0].equals("s")) {
			runServer();
		} else if (argv[0].equals("c")) {
			runClient();
		}
	}
	
	public static void runServer() {
		try {
			ServerSocket server = new ServerSocket(1234);
			
			System.out.printf("waiting for client...\n");
			Socket socket = server.accept();
			System.out.printf("client connected...\n");
			
			Thread.sleep(3000);
			
			OutputStream out = socket.getOutputStream();
			out.write("Hello client!".getBytes());
			out.flush();
			
			Thread.sleep(2000);
			server.close();
			System.out.printf("server closed\n");
			
			Thread.sleep(2000);
			socket.close();
			System.out.printf("socket closed\n");
			
			Thread.sleep(2000);
			out.close();
			System.out.printf("out closed\n");
			
			
			
		} catch (Exception e) {
			System.out.printf("server exception: " + e.toString() + "\n");
		}
	}
	
	public static void runClient() {
		Socket socket = new Socket();
		try {
			System.out.printf("connecting to server...\n");
			socket.connect(new InetSocketAddress("localhost", 1234));
			System.out.printf("server connected...\n");
			
			InputStream in = socket.getInputStream();
			byte[] buff = new byte[1024];
			
			socket.close();
			for (int i = 0; i < 2; i ++) {
				System.out.printf("going to read...\n");
				int len = in.read(buff);
				System.out.printf("read len = %d\n", len);
				if (len > 0)
					System.out.printf(new String(buff, 0, len) + "\n");
			}
			
			in.close();
			System.out.printf("closed");
		} catch (IOException e) {
			System.out.printf("client exception: " + e.toString() + "\n");
		} finally {
			/*try {
				socket.close();
			} catch (IOException e) {
			}*/
		}
	}
}
