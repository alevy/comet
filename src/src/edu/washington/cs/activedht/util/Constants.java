package edu.washington.cs.activedht.util;

public interface Constants {
	public static final int KB = 1024;
	public static final int MB = 1024 * KB;
	public static final int GB = 1024 * MB;
	
	public static final int FILE_READ_BLOCK_SIZE = KB;
	
	// Workload limits:
	public static final int MAX_ADMISSIBLE_CLASS_SIZE = 10 * KB;
	public static final int NUM_MAX_VALUES = 100;
	
	
	// ActiveCodeRunner:
	public static final int CORE_POOL_SIZE  = 5;
	public static final int MAX_POOL_SIZE   = 10;
	public static final int KEEP_ALIVE_TIME = 100;
	public static final int BLOCKING_QUEUE_CAPACITY = NUM_MAX_VALUES;
	public static final long ACTIVE_CODE_EXECUTION_TIMEOUT = 50;  // ms
	public static final long ACTIVE_CODE_CHECK_EXECUTION_INTERVAL_NANOS = 1000; 
	
	// ActiveCodeWrapper:
	public static final int MAX_NUM_DHT_ACTIONS_PER_EVENT = 2;
	public static final int MAX_TIME_RUN_DHT_ACTIONS_PER_EVENT = 100000;  // ms
}
