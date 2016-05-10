package com.yuwnloy.disconman.utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadPool {
	public static ExecutorService singleThreadPool = Executors.newSingleThreadExecutor();
	public static void Execute(Runnable runnable){
		if(singleThreadPool.isShutdown()){
			singleThreadPool = null;
			singleThreadPool = Executors.newSingleThreadExecutor();
		}
		singleThreadPool.execute(runnable);
	}
	
	public static void shutdown(){
		if(singleThreadPool!=null&&!singleThreadPool.isShutdown()){
			singleThreadPool.shutdown();
		}
	}
}
