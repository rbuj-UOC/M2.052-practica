package web;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import dat.core.DAT;

/**
 * Maintains a queue of locks for individual named entries.
 * 
 * Each queue is implemented as a BlockingQueue with capacity for one (1) element.
 * To lock an entry, a put is tried in the queue. If the queue is empty, the put
 * continues, but if it already has an element, blocks. 
 * 
 * To release an entry, the element is removed.
 * 
 * @author Pablo Chacin
 *
 */
public class EntryLock {


	/**
	 * Lock to prevent concurrent access to lock queue from multiple threads
	 */
	protected Lock lock;

	/**
	 * Map used to maintain the queues od lock for individual entries on the cache.
	 */
	protected Map<String,BlockingQueue>entryLocks;

	public EntryLock(){
		this.lock = new ReentrantLock();
		this.entryLocks = new HashMap<String,BlockingQueue>();
	}


	/**
	 * Checks if the entry and, if so, locks it and waits it to be released.
	 * Controls local concurrent access from multiple threads.
	 * 
	 * @param entry
	 */
	protected void reserveEntry(String entry,String owner){
		//prevent concurrent access from concurrent local threads
		lock.lock();

		//get the entry's blocking queue
		BlockingQueue entryLock= entryLocks.get(entry);

		//if entry's lock has not been initialized, initialize it
		if(entryLock == null){
			entryLock = new ArrayBlockingQueue<String>(1);
			entryLocks.put(entry,entryLock);
		}

		try{
			entryLock.put(owner);
		} catch (InterruptedException e) {
			DAT.getLog().warn("Interrupted while waiting for lock");
		}

		lock.unlock();
	}


	/**
	 * Convenience method that allows an "anonymous" lock
	 * 
	 * @param entry
	 */
	public void reserveEntry(String entry){
		reserveEntry(entry,"");
	}


	/**
	 * Releases a locked entry in the cache. 
	 * @see #lockEntry(String)
	 * 
	 * @param entry
	 */
	public void releaseEntry(String entry,String owner){

		//prevent concurrent access from concurrent local threads 
		lock.lock();

		BlockingQueue entryLock = entryLocks.get(entry);

		if(entryLock != null){
			entryLock.remove();
		}
		else{
			DAT.getLog().debug("Releasing an unlocked entry " + entry);
		}

		lock.unlock();

	}


	public void releaseEntry(String entry) {
		releaseEntry(entry,"");
	}

}
