package leader.lcr;

import java.util.Collections;
import java.util.List;

import dat.Algorithm;
import dat.DatException;
import dat.Event;
import dat.Message;
import dat.Node;
import dat.algorithms.leader.LeaderElection;
import dat.algorithms.membership.MembershipAlgorithm;
import dat.core.DAT;
import dat.network.NodeAddress;



/**
 * 
 * Elects a leader for a group of nodes using the LCR (LeLann, Chang, Roberts) algorithm.
 * 
 * 
 * @author Pablo Chacin
 *
 */
public class LCR implements LeaderElection, Algorithm {

	/**
	 * Higest ID seen by this node so far in an election process
	 */
	private String higestId;

	/**
	 * The leader of the group, according to the last election process
	 */
	protected NodeAddress leader=null;

	/**
	 * The node on which this instance of the algorithm runs
	 */
	protected Node node;

	/**
	 * Indicates an ongoing election process 
	 */
	protected boolean inElection=false;
	
	/**
	 *  Group Membership algorithms
	 */
	protected MembershipAlgorithm membership;


	@Override
	/**
	 * Returns the current leader. If none has been elected,starts the 
	 * election process.
	 * 
	 * @return the NodeAddress of the last elected leader
	 */
	public NodeAddress getLeader() {
		if(leader !=null){
			return leader;
		}
		else{
			return electLeader();
		}
	}

	@Override
	/**
	 * Indicates if the curent node is the current leader 
	 * 
	 * @return a boolean indicating if the node is the leader (true) or not (false)
	 */
	public boolean isLeader() {

		NodeAddress currentLeader = getLeader();
		return node.getAddress().equals(currentLeader);

	}


	/**
	 * Request a new leader. The application may suspect the current leader is not
	 * longer valid (for example, it is unresponsive ) 
	 */
	public NodeAddress electLeader(){

		if(!inElection)
			startElection();

		//wait for the election process to end
		while(leader == null){
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {}
		}
		return leader;
	}

	
	/**
	 * Starts the election process by sending an ElectionRequestMessage to 
	 * the next neighbor in the virtual ring formed by the node's IDs.
	 * 
	 */
	protected synchronized void  startElection(){

		
		inElection = true;
		leader = null;
		
		//prepare the election message, proposing itself as leaders
		Message message = new ElectionRequestMessage();
		message.setObject("candidate", higestId);

		//TODO: Send an ElectionRequestMessage to the next neighbor in the ring.
		//      must check for exception sending the message in case this neighbor
		//      is not running. If this is the case, look for the next in the ring, 
		//      if any.
		//
		//      If none of the neighbors is available (this is the sole node runnning)
		//      declare itself as a leader and put inElection as false
		//      

	}
	@Override
	public void handleEvent(Event event) {
	
		//there are no events defined in this algorithm, so ignore any 
		node.getLog().warn("Invalid Event Triggered " +event.toString() );

	}

	@Override
	/**
	 * Handles a generic message. If this method is called, then there is an error 
	 * in the algorirthm. More likely, a wrong message class is being used.
	 */
	public void handleMessage(Message message) {


			node.getLog().warn("Invalid Message Received " +message.toString() );
			

	}

	/**
	 * Handle a request for electing a new leader.
	 * 
	 * The request message has the attribute "candidate" which has id of the node
	 * with the highest id found so far in the process.
	 * 
	 * If the node's id matches the candidate's id, then this node is the elected leader
	 * and must send an ElectionResultMessage to all neighbors.
	 * 
	 * Otherwise, the node updates its highest known id with the highest
	 * between the current known and the candidate and passes this information to the next
	 * neighbor in the virtual ring sending an ElectionRequestMessage.
	 * 
	 * @param request an ElectionRequestMessage proposing a candidate for leader
	 */
	public void handleMessage(ElectionRequestMessage request) {
		
		//retrieve the proposed leader from the message
		String candidate = request.getString("candidate");
		
		//TODO: check if this is the leader. If so, inform all neighbors
		
		//TODO: if not the leader, propagage request to next node in the ring

	}

	
	/**
	 * Handle the notification of an election. The sender of the message is the new leader.
	 * The reception of this message ends the election process.
	 * 
	 * @param message
	 */
	public void handleMessage(ElectionResultMessage message) {
		
		leader = message.getSender();
		inElection = false;
		
	}


	/**
	 * Initializes the execution of the algorithm in this node.
	 * 
	 * @param node the Node on which this instance of the algoritms is executed.
	 * 
	 */
	@Override
	public void init(Node node) {
		this.node = node;
		this.higestId = node.getAddress().getLocation();
		this.membership = (MembershipAlgorithm) node.getAlgorithm(node.getParameters().getString("membership"), 
                MembershipAlgorithm.class);
		
	}



	/**
	 * Supporting method. Selects the next neighbor, according to their ID, starting with a 
	 * given "base" node. It is use to travese the virtual "ring" of nodes.
	 * 
	 * For example, calling getNextNeighbor(DAT.getNode()) return the next 
	 * node of the current node. 
	 * 
	 * @return the NodeAddress of the next node in the virtual ring, if any.
	 */
	private NodeAddress getNextNeighbor(NodeAddress base){
		
		List<NodeAddress> neighbors = membership.getKnownNodes();

		Collections.sort(neighbors);
		
		for(NodeAddress n: neighbors){
			if(n.compareTo(base) > 0){
				return n;
			}
		}

		return neighbors.get(0);

	}


}