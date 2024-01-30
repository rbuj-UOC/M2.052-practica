package web;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.configuration.Configuration;

import utils.HttpUtils;
import utils.HttpUtils.HttpException;
import dat.algorithms.leader.LeaderElection;
import dat.algorithms.transaction.ResourceManager;
import dat.algorithms.transaction.TransactionCoordinator;
import dat.core.DAT;

/**
 * Implements a cache server that serves content from the data server and
 * also behaves as a resource manager for the cache.
 * 
 * The only operation supported over the resources is UPDATE which
 * replaces the current content with the given one. If the given
 * content is null, the object is deleted.
 * 
 * 
 * Assumes that local threads can read but can't modify cache entries.
 * Modifications come by the messages from the coordinator.
 * 
 * Also assumes that modifications for a single entry are serialized by
 * the coordinator, so any lock for an already locked entry will be refused.
 * 
 * The following code is thread safe for multiple local reads and one modification
 * from the coordinator, but assumes that there is only one concurrent modification
 * (the requests messages are serialized by the node, see DAT documentation). 
 * 
 * Each cache entry is locked individually. Local read requests are
 * locked while the resource is being modified. If multiple modification
 * request are received, there are queued and are processed before the local
 * read can proceed. This may lead to unbounded delay of reads, but
 * it is assumed that his rarely will happen. 
 * 
 * 
 * @author Pablo Chacin
 *
 */
public class CacheServer extends WebServer implements ResourceManager, Runnable {

    /**
     * Local cache. Map with the content of objects, stored as binary data
     */
    protected Map<String, byte[]> cache;
    /**
     * Locks for resources
     */
    protected EntryLock locks;
    /**
     * Register the previous state for changes pending for commit
     */
    protected Map<String, byte[]> rollBackLog;
    protected TransactionCoordinator coordinator;
    protected LeaderElection election;
    protected String dataServer;
    protected String electionAlgorithm;
    protected String transactionAlgorithm;

    public CacheServer() {
        this.cache = new HashMap<String, byte[]>();
        this.rollBackLog = new HashMap<String, byte[]>();
        this.locks = new EntryLock();

    }

    @Override
    public void start(Configuration configuration) {
        super.start(configuration);

        dataServer = configuration.getString("dataserver");
        electionAlgorithm = configuration.getString("leader");
        transactionAlgorithm = configuration.getString("transaction");

        this.election = (LeaderElection) DAT.getAlgorithm(electionAlgorithm, LeaderElection.class);
        this.election.electLeader();

        this.coordinator = (TransactionCoordinator) DAT.getAlgorithm(transactionAlgorithm,
                TransactionCoordinator.class);
        this.coordinator.setResourceManager(this);
    }

    /**
     * Return content from the cache. If not present, get it from the data server;
     */
    @Override
    protected byte[] getContent(String object) throws HttpException {

        byte[] content;

        //lock entry
        locks.reserveEntry(object);

        //get content from cache, if not present, retrieve from data server
        try {
            content = cache.get(object);
            if (content == null) {

                content = HttpUtils.get(dataServer, object);
                cache.put(object, content);
            }
        } catch (IOException e) {
            throw new HttpException(HTTP_SERVER_ERROR);
        } finally {
            //release entry
            locks.releaseEntry(object);
        }
        return content;
    }

    @Override
    /**
     * Request the transaction to the coordinator. The local update will be done when the
     * coordinator sends the request to all members
     */
    protected void delete(String object) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void putContent(String object, byte[] content) throws HttpException {
        //request the execution of the transaction
        boolean executed = coordinator.executeTransaction(object, "PUT", content);

        if (!executed) {
            throw new HttpException(HTTP_SERVER_ERROR);
        }
    }

    //@Override
    public boolean lock(String resource, String transaction) {
        //get the entry. Prevent further local access
        // - locks.reserveEntry(resource);
        // + locks.reserveEntry(resource, transaction);
        locks.reserveEntry(resource, transaction);

        return true;
    }

    //@Override
    public void abort(String resource, String transaction) {
        byte[] content = rollBackLog.remove(resource);

        if (content == null) {
            DAT.getLog().debug("Previous state for resource " + resource + " not found in RollBack");
            return;
        }

        cache.put(resource, content);

        //update in data server
        if (election.isLeader()) {
            try {
                HttpUtils.put(dataServer, resource, content);
            } catch (IOException e) {
                DAT.getLog().warn("unable to restore resource state in abort: " + resource, e);
                return;
            }
        }

        unlock(resource, transaction);
    }

    //@Override
    /**
     * Updates the content of an entry in the local cache. If the server is the leader
     * also updates the entry in the data server
     */
    public void commit(String resource, String transaction) {

        //delete pending operation
        rollBackLog.remove(resource);

        //release entry
        unlock(resource, transaction);
    }

    //@Override
    /**
     * Process a modification request coming from the coordinator.
     *
     * Assumes the entry was locked in the lock method to prevent local access.
     */
    public boolean apply(String resource, String transaction, String operation, Object data) {

        //the only supported operation is update
        if (!operation.equals("PUT")) {
            return false;
        }

        //save current state
        rollBackLog.put(resource, cache.get(resource));

        byte[] content = (byte[]) data;

        //update local cache
        cache.put(resource, content);
        DAT.getLog().info("PUT: update local cache");

        //update in data server
        if (election.isLeader()) {
            try {
                HttpUtils.put(dataServer, resource, content);
                DAT.getLog().info("PUT: update server cache");
            } catch (IOException e) {
                DAT.getLog().warn("unable to update resource " + resource, e);
                return false;
            }
        }

        return true;
    }

    /**
     *
     * Releases a resource locked for a given transaction. If the transaction requesting
     * the unlock is not the transaction currently locking the resource, the request is
     * ignored.
     *
     * Assumes that the cache entry has been locked by the {@link #lock(String, String)} method
     */
    public void unlock(String resource, String transaction) {
        // - locks.releaseEntry(resource);
        // + locks.releaseEntry(resource, transaction);
        locks.releaseEntry(resource, transaction);
    }

    @Override
    public void run() {
        Configuration param = dat.core.DAT.getAppParameters();
        start(param);
    }
}
