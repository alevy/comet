package edu.washington.cs.activedht.util;

public interface Constants {
	public static final int KB 			= 1024;
	public static final int MB 			= 1024 * KB;
	public static final int GB 			= 1024 * MB;
	
	public static final long MS 		= 1L;
	public static final long SECONDS 	= 1000L * MS;
	public static final long MINUTES 	= 60L * SECONDS;
	public static final long HOURS 		= 60L * MINUTES;
	public static final long DAYS 		= 24L * HOURS;
	
	public static final int FILE_READ_BLOCK_SIZE = KB;
	
	// Workload limits:
	public static final int MAX_ADMISSIBLE_CLASS_SIZE = 10 * KB;
	public static final int NUM_MAX_VALUES = 100;
	
	
	// ActiveCodeRunner:
	public static final int CORE_POOL_SIZE  = 5;
	public static final int MAX_POOL_SIZE   = 10;
	public static final int KEEP_ALIVE_TIME = 100;
	public static final int BLOCKING_QUEUE_CAPACITY = NUM_MAX_VALUES;
	public static final long ACTIVE_CODE_EXECUTION_TIMEOUT = 50 * MS;
	public static final long ACTIVE_CODE_CHECK_EXECUTION_INTERVAL_NANOS =
		10000;
	
	// ActiveCodeWrapper:
	public static final int  MAX_NUM_DHT_ACTIONS_PER_EVENT = 2;
	public static final long MAX_TIME_RUN_DHT_ACTIONS_PER_EVENT = 20 * SECONDS;
	public static final int  NUM_SIMULTANEOUS_DHT_ACTIONS_PER_OBJECT = 2;
	
	// ActiveDHTDB:
	public static final long ACTIVE_CODE_PERIODIC_TIMER_INTERVAL =
		20 * MINUTES;
}
