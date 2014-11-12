package com.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class HelloWorld {
	private Object mLock = new Object();
	private static ArrayList<String> array = new ArrayList<String>();
	
	HelloWorld() {
		mThread.start();
	}
	
	void testException() {
		try {
			testException2();
		} finally {
		}
	}
	
	void testException2() {
		if (true) {
			throw new RuntimeException();
			//throw new Exception();  // Exception need to be surrounded in try / catch
		}
	}
	
	public static void __main(String[] args) {
		/*String sss = "/dev/null\0asec list";
		if (sss == null)
			System.out.printf("sss == null\n");
		else
			System.out.printf(sss);
		
		array.add("bbb");
		array.add("aaa");
		Object[] aa = array.toArray();
		int hi = aa.length;
		int lo = 0;
		int runHi = lo + 1;
		String s1 = (String)aa[runHi++];
		String s2 = (String)aa[runHi];
		Arrays.sort(aa);
		
		// 測試子類別的function name和父類別的function name相同，參數卻不同，能不能透過子類別乎叫到原父類別的function
		BBB b = new BBB();
		b.foo();
		
		String str = null;
		boolean isEqual = str.equals("TEST");*/
		
		String s1 = "s1";
		String s2 = "s1";
		boolean isEqual = s1 == s2;
		
		System.out.printf("Hello World\n");
		HelloWorld hello = new HelloWorld();
		try {
			hello.testException();
		} catch(RuntimeException e) {
			
		}
		
		System.out.printf("End\n");
	}
	
	private void foo1() {
		synchronized (mLock) {
			foo2();
		}
	}
	
	private void foo2() {
		synchronized (mLock) {
			System.out.printf("TID(" + Thread.currentThread().getId() + ") is in foo2 now\n");
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	private Runnable mRunnable = new Runnable() {
		@Override
		public void run() {
			foo2();
		}
	};
	
	private Thread mThread = new Thread(mRunnable);
}
