package edu.washington.cs.activedht.code.insecure;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import edu.washington.cs.activedht.util.Constants;

/**
 * The sandbox for running active code securely.
 * 
 * Two dimensions of sandboxing:
 * 1. Security:
 *    - The sandboxed code can only access a tiny set of resources.
 *    
 *    Mostly using the Java sandbox for this.
 *
 * 2. Resouce isolation:
 *    - The sandbox is limited in terms of memory consumption and runtime.
 *
 * @author roxana
 */
public class ActiveCodeSandbox<V> implements Constants {
	public static final String INSECURE_CODE_THREAD_GROUP = "insecure";
	
	private ThreadPoolExecutor handler_pool;
	private long active_code_execution_timeout;
	
    // Helper classes:
	
	/**
	 * Class for handling rejections of tasks due to overflown work queue.
	 * @author roxana
	 */
	class MyRejectedExecutionHandler implements RejectedExecutionHandler {
		@Override
		public void rejectedExecution(Runnable r,
				                      ThreadPoolExecutor executor) {
			// TODO(roxana): drop task silently for now.
		}
	}

	public ActiveCodeSandbox(long active_code_execution_timeout) {
		handler_pool = new ThreadPoolExecutor(
			CORE_POOL_SIZE, MAX_POOL_SIZE,
			KEEP_ALIVE_TIME, TimeUnit.MILLISECONDS,
			new ArrayBlockingQueue<Runnable>(BLOCKING_QUEUE_CAPACITY),
			new InsecureThreadFactory(),
			new ActiveCodeSandbox.MyRejectedExecutionHandler());
		
		this.active_code_execution_timeout = active_code_execution_timeout;
		
		setupSandbox();
	}
	
	private void setupSandbox() {
		// TODO(roxana): Set up all the required things here.
		
		handler_pool.prestartCoreThread();
	}

	/**
	 * Executes a task within the sandbox, waits for it to finish, and returns
	 * the result.
	 * @param task
	 * @return
	 */
	public V executeWithinSandbox(Callable<V> task) {
		long start_ts = System.currentTimeMillis();

		V result = null;
		Future<V> f = handler_pool.submit(task);
		while ((System.currentTimeMillis() - start_ts) <
			   active_code_execution_timeout) {
			try {
				result = f.get(ACTIVE_CODE_CHECK_EXECUTION_INTERVAL_NANOS,
						       TimeUnit.NANOSECONDS);
			} catch (InterruptedException e) {
				continue;
			} catch (ExecutionException e) {  // computation threw an exception
				e.printStackTrace();
				return null;
			} catch (TimeoutException e) {  // task needed to be canceled
				if (! usageWithinLimits(task)) break;
				else continue;
			}
			
			// Got the result.
			break;
		}

		if (!f.isDone()) f.cancel(true);
		return result;
	}
	
	private boolean usageWithinLimits(Callable<V> task) {
		// TODO(roxana): Implement.
		return true;
	}
	
	// Util functions:
	
	public int getNumPendingTasks() { return handler_pool.getActiveCount(); }
}

// Helper classes:

/**
 * Singleton for the INSECURE thread group.
 * 
 * @author roxana
 */
class InsecureThreadGroupSingleton extends ThreadGroup {	
	private static InsecureThreadGroupSingleton object = null;
	private final static Object lock = new Object();
	
	private InsecureThreadGroupSingleton() {
		super(ActiveCodeSandbox.INSECURE_CODE_THREAD_GROUP);
	}
	
	public static InsecureThreadGroupSingleton getInstance() {
		synchronized(lock) {
			if (object == null) object = new InsecureThreadGroupSingleton();
		}
		return object;
	}
}

/**
 * Factory for creating threads in the INSECURE domain.
 * @author roxana
 */
class InsecureThreadFactory implements ThreadFactory {
	ThreadGroup insecure_thread_group;
	
	protected InsecureThreadFactory() {
		insecure_thread_group = InsecureThreadGroupSingleton.getInstance();
	}
	
	@Override
	public Thread newThread(Runnable target) {
		return new Thread(insecure_thread_group, target);
	}
}

