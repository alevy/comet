package edu.washington.cs.activedht.code;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.aelitis.azureus.core.dht.db.DHTDBValue;

import edu.washington.cs.activedht.operations.DHTOperation;
import edu.washington.cs.activedht.util.Constants;

class InsecureThreadFactory implements ThreadFactory {
	public static final String INSECURE_CODE_THREAD_GROUP = "insecure";
	ThreadGroup insecure_thread_group;
	
	protected InsecureThreadFactory() {
		insecure_thread_group =
			new ThreadGroup(INSECURE_CODE_THREAD_GROUP);
	}
	
	@Override
	public Thread newThread(Runnable target) {
		return new Thread(insecure_thread_group, target);
	}
}

class MyRejectedExecutionHandler implements RejectedExecutionHandler {
	@Override
	public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
		// TODO(roxana): drop task silently for now.
	}
}

public class ActiveCodeRunner implements Constants {
	public static final String INSECURE_CODE_THREAD_GROUP = "insecure";
	public static final int CORE_POOL_SIZE  = 5;
	public static final int MAX_POOL_SIZE   = 10;
	public static final int KEEP_ALIVE_TIME = 100;
	public static final int BLOCKING_QUEUE_CAPACITY = NUM_MAX_VALUES;
	
	private ThreadPoolExecutor handler_pool;

	public ActiveCodeRunner() {
		handler_pool = new ThreadPoolExecutor(
			CORE_POOL_SIZE, MAX_POOL_SIZE,
			KEEP_ALIVE_TIME, TimeUnit.MILLISECONDS,
			new ArrayBlockingQueue<Runnable>(BLOCKING_QUEUE_CAPACITY),
			new InsecureThreadFactory(),
			new MyRejectedExecutionHandler());
	}
	
	public Future startTask(DHTOperation operation, DHTDBValue value) {
		return handler_pool.submit(operation, );
	}
}
