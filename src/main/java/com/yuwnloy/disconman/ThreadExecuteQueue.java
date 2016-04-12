package com.yuwnloy.disconman;

import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;
/**
 * 
 * @author xiaoguang
 *
 * @date 2015��9��22��
 */
public class ThreadExecuteQueue {
        private final static String CLASS_NAME = ThreadExecuteQueue.class.getName();
        private static Logger s_logger = Logger.getLogger(CLASS_NAME);
	private static ThreadExecuteQueue instance = null;
	public static synchronized ThreadExecuteQueue getInstance(){
		if(instance==null)
			instance = new ThreadExecuteQueue();
		return instance;
	}
	private LinkedList<Thread> queue = new LinkedList<Thread>();
	private Thread runthread = null;
	private ThreadExecuteQueue(){
		
	}	
	
	public void execute(Thread th){
	    final String loggerMethodName = "execute";
	    s_logger.logp(Level.FINER, CLASS_NAME, loggerMethodName, "Begin to execute ThreadExecuteQueue");
            synchronized(ThreadExecuteQueue.class){
		instance.put(th);			
		if(runthread == null || !runthread.isAlive()){
			runthread = new Thread(new Runnable(){
				public void run() {
					while(true){
                                            synchronized(ThreadExecuteQueue.class){
						Thread t = instance.take();
                                                if(t!=null){
                                                        s_logger.logp(Level.FINER, CLASS_NAME, loggerMethodName, "Thread run Begin, CLASS_NAME = " + t.getClass().getName());
                                                        if (t.getClass() == MBeanInvocationHandler.StoreAttributeWorker.class){
                                                            MBeanInvocationHandler.StoreAttributeWorker tt = (MBeanInvocationHandler.StoreAttributeWorker)t;
                                                            s_logger.logp(Level.FINER, CLASS_NAME, loggerMethodName, "Thread run Set"
                                                                         + ", keyName = " + tt.getKeyName() 
                                                                         + "AttributeName = " + tt.getAttributeName() 
                                                                         + "Value = " + tt.getValue());
                                                        }
                                                        else{
                                                            s_logger.logp(Level.FINER, CLASS_NAME, loggerMethodName, "Thread run Get");
                                                        }
							t.run();
                                                        try{
                                                            //wait 1 millisecond to avoid to create duplication xml backup file name 
                                                            Thread.sleep(1);
                                                        }catch (InterruptedException e) {
                                                            e.printStackTrace();
                                                            s_logger.logp(Level.WARNING, CLASS_NAME, loggerMethodName, "ThreadExecuteQueue sleep with InterruptedException");
                                                        }
                                                        s_logger.logp(Level.FINER, CLASS_NAME, loggerMethodName, "Thread run End, CLASS_NAME = " + t.getClass().getName());
						}else{
							runthread = null;
							break;
						}
                                            }
					}
				}			
			});
			runthread.start();
		}
            }
	    s_logger.logp(Level.FINER, CLASS_NAME, loggerMethodName, "End to execute ThreadExecuteQueue");
	}
	
	public synchronized void put(Thread th){
                final String loggerMethodName = "put";
                s_logger.logp(Level.FINER, CLASS_NAME, loggerMethodName, "Begin to put a thread to ThreadExecuteQueue, CLASS_NAME = " + th.getClass().getName());
                if (th.getClass() == MBeanInvocationHandler.StoreAttributeWorker.class){
                    MBeanInvocationHandler.StoreAttributeWorker tt = (MBeanInvocationHandler.StoreAttributeWorker)th;
                    s_logger.logp(Level.FINER, CLASS_NAME, loggerMethodName, "Thread put"
                                 + ", keyName = " + tt.getKeyName() 
                                 + "AttributeName = " + tt.getAttributeName() 
                                 + "Value = " + tt.getValue());
                }
		queue.add(th);
                s_logger.logp(Level.FINER, CLASS_NAME, loggerMethodName, "End to put a thread to ThreadExecuteQueue, CLASS_NAME = " + th.getClass().getName());
	}
	public synchronized Thread take(){
                final String loggerMethodName = "take";
                s_logger.logp(Level.FINER, CLASS_NAME, loggerMethodName, "Begin to take a thread from ThreadExecuteQueue");            
                if(queue.isEmpty()){
                    s_logger.logp(Level.FINER, CLASS_NAME, loggerMethodName, "End to take queue empty from ThreadExecuteQueue and return null");
                    return null;
                }
		Thread th = queue.getFirst();
		queue.removeFirst();
                if (th.getClass() == MBeanInvocationHandler.StoreAttributeWorker.class){
                    MBeanInvocationHandler.StoreAttributeWorker tt = (MBeanInvocationHandler.StoreAttributeWorker)th;
                    s_logger.logp(Level.FINER, CLASS_NAME, loggerMethodName, "Thread take, CLASS_NAME = " + th.getClass().getName()
                                 + ", keyName = " + tt.getKeyName() 
                                 + "AttributeName = " + tt.getAttributeName() 
                                 + "Value = " + tt.getValue());
                }
                s_logger.logp(Level.FINER, CLASS_NAME, loggerMethodName, "End to take a thread from ThreadExecuteQueue, CLASS_NAME = " + th.getClass().getName());
                return th;
	}
	
}

