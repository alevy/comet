package edu.washington.cs.activedht.code.insecure.sandbox;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import edu.washington.cs.activedht.code.insecure.exceptions.ActiveCodeExecutionInterruptedException;
import edu.washington.cs.activedht.code.insecure.exceptions.InitializationException;
import edu.washington.cs.activedht.code.insecure.exceptions.NotAnActiveObjectException;
import edu.washington.cs.activedht.db.coderunner.InvalidActiveObjectException;
import edu.washington.cs.activedht.util.Constants;

public class ActiveCodeSandboxImpl<RETURN_TYPE>
implements ActiveCodeSandbox<RETURN_TYPE>,
           Constants {
	public static final String INSECURE_CODE_THREAD_GROUP = "insecure";
	
	private ThreadPoolExecutor handler_pool;
	private long active_code_execution_timeout;
	
	private boolean is_initialized = false;

	@SuppressWarnings("unchecked")
	public ActiveCodeSandboxImpl(long active_code_execution_timeout) {
		handler_pool = new ThreadPoolExecutor(
			CORE_POOL_SIZE, MAX_POOL_SIZE,
			KEEP_ALIVE_TIME, TimeUnit.MILLISECONDS,
			new ArrayBlockingQueue<Runnable>(BLOCKING_QUEUE_CAPACITY),
			new InsecureThreadFactory(),
			new ActiveCodeSandboxImpl.MyRejectedExecutionHandler());
		this.active_code_execution_timeout =
			active_code_execution_timeout;
	}
	
	// ActiveCodeSandbox interface:
	
	@Override
	public void init() throws InitializationException {
		// TODO(roxana): Set up all the required things here.
		
		handler_pool.prestartCoreThread();
		
		// This should be the last statement.
		is_initialized = true;
	}
	
	@Override
	public boolean isInitialized() { return is_initialized; }
	
	@Override
	public void stop() { handler_pool.purge(); }

	@Override
	public RETURN_TYPE executeWithinSandbox(Callable<RETURN_TYPE> task)
	throws ActiveCodeExecutionInterruptedException {
		assert(isInitialized());
		
		long start_ts = System.currentTimeMillis();

		boolean finished_successfully = false;
		String interruption_reason = "Execution timed out";
		
		RETURN_TYPE result = null;
		Future<RETURN_TYPE> f = handler_pool.submit(task);
		while ((System.currentTimeMillis() - start_ts) <
				active_code_execution_timeout) {
			try {
				result = f.get(ACTIVE_CODE_CHECK_EXECUTION_INTERVAL_NANOS,
					       	   TimeUnit.NANOSECONDS);
				finished_successfully = true;
				break;  // got the result.
			} catch (InterruptedException e) {
				continue;
			} catch (ExecutionException e) {  // computation threw an exception
				// e.printStackTrace();
				return null;
			} catch (TimeoutException e) {  // task needed to be canceled
				if (! usageWithinLimits(task)) {
					interruption_reason = "Usage limits exceeded";
					break;
				}
				else continue;
			}
		}

		// Cancel the task.
		if (! finished_successfully) {
			f.cancel(true);
			throw new ActiveCodeExecutionInterruptedException(
					interruption_reason);
		}

		return result;
	}
	
	private boolean usageWithinLimits(Callable<RETURN_TYPE> task) {
		// TODO(roxana): Implement.
		return true;
	}
	
	// Util functions:
	
	public int getNumPendingTasks() { return handler_pool.getActiveCount(); }
	
	/**
	 * Class for handling rejections of tasks due to overflown work queue.
	 * @author roxana
	 */
	private class MyRejectedExecutionHandler
	implements RejectedExecutionHandler {
		@Override
		public void rejectedExecution(Runnable r, 
				                      ThreadPoolExecutor executor) {
			// TODO(roxana): drop task silently for now.
		}
	}
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
		super(ActiveCodeSandboxImpl.INSECURE_CODE_THREAD_GROUP);
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

