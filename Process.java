package graphics;

import java.awt.geom.Rectangle2D;

public class Process {
	public int _threadCount;
	public int _procId;
	public int _pid;
	public String _name;
	public Boolean _isDead = false;			// Set to true when there are no more threads left
	public Boolean _wasEncountered = false; // Have we encountered the procId from the threads yet
	public Boolean _isDisplayed = true;		// State assigned in process table
	public Rectangle2D.Double _procArea = null; 
	
	public Process( int procId, int pid, String name ) {
		_procId = procId;
		_pid = pid;
		_name = name;
		_threadCount++;
	}
	
	public void addNewThread() {
		_threadCount++;
	}
}
