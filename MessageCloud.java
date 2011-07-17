package graphics;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import sun.awt.windows.ThemeReader;

public class MessageCloud {
	JFrame _frame;
	JPanel _vis;
	
	// Used for keeping track of the progress bar
	String _dbName = null; // Prompt user for dbName
	int _minSid = 0, _maxSid = 0, _currentSid = 0;
	
	// Hold thread, process, message info
	Thread [] _threadArray;
	int _objCountT = 0;
	Process [] _processArray;
	int _objCountP = 0;
	Message [] _messageArray;
	int _objCountM = 0;
	
	Dimension _screenSize;
	
	// Mouse location and control button flags
	Point2D _mouseLocation = new Point2D.Double();
	boolean _isPaused = false;
	boolean _stepForward = false;
	boolean _stepBackward = false;	
	
	ThreadFrame _tFrame = new ThreadFrame();
	
	// Easily look up Threads and Processes according to their id converted to string
	HashMap<String, Thread> _tidLookUp = new HashMap<String, Thread>();
	HashMap<String, Process> _procIdLookUp = new HashMap<String, Process>();
	
	public MessageCloud() {
		//Prompt the user for the database name.
		new PromptDB();
		
		// Initialize java application window
		_frame = new JFrame( "Message Cloud" );
		_frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
		_vis = new JPanel();
	    
	    // Set the preferred size of the visual together with the button panels
	    // this is for the proper scrolling.
		_screenSize = Toolkit.getDefaultToolkit().getScreenSize();
	    _vis.setPreferredSize( new Dimension ( _screenSize.width, _screenSize.height + 1100 ) ); // Offset by 1100 to make room for process table
	    _vis.setLayout( new BorderLayout( 1,1 ) );
	    
	    //Create the visualization panel and add a mouse listener
	    JPanel cloud = new Cloud();
	    cloud.addMouseMotionListener(new MouseMotionListener()
	    {
	    	//The response to both mouse moved and mouse dragged is the same.
	    	public void mouseMoved(MouseEvent e) 
	    	{ 
	    		getMouseCoordinates(e);
	    	} 
	    	public void mouseDragged(MouseEvent e) 
	    	{ 
	    		getMouseCoordinates(e);
	    	} 
	    });
	    
	    // Use this to turn on and off thread display the process that was clicked
	    cloud.addMouseListener(new MouseListener()
	    {
	    	public void mousePressed(MouseEvent e) 
	    	{ 
	    	} 
	    	
	    	public void mouseReleased(MouseEvent e) 
	    	{ 
	    	} 
	    	
	    	public void mouseClicked(MouseEvent e) 
	    	{ 
	    		changeProcessDisplay(e);
	    	}
	    	
	    	public void mouseEntered(MouseEvent e) 
	    	{ 
	    	}
	    	
	    	public void mouseExited(MouseEvent e) 
	    	{ 
	    	} 
	    	
	    });
	    
	    // Create button panels.
	    JPanel masterButtonPanel = new JPanel();
	    masterButtonPanel.setLayout( new BorderLayout( 2,1 ) );
	    
	    JPanel controlButtonPanel = new JPanel();
	    addControlButtons(controlButtonPanel);
	    
	    JPanel processButtonPanel = new JPanel();
	    addProcessButtons( processButtonPanel );
	    
	    masterButtonPanel.add( BorderLayout.WEST, controlButtonPanel );
	    masterButtonPanel.add( BorderLayout.EAST, processButtonPanel );
	    _vis.add(BorderLayout.NORTH, masterButtonPanel);
	    _vis.add(cloud);
	    
	    // This is the scroll pane to allow for scrolling. 
	    JScrollPane scrollPane = new JScrollPane( _vis, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, 				
	    		JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED );
	    
	    _frame.getContentPane().add( scrollPane );
	    _frame.setBounds( 0, 0, _screenSize.width, _screenSize.height );
	    _frame.setVisible( true );
	    
	    //The viewport change listener is added to re-draw the visual if the scroll occurs.
	    JViewport viewPort = scrollPane.getViewport();
	    viewPort.addChangeListener(new ChangeListener()
	    {
	    	public void stateChanged(ChangeEvent e)
	    	{
	    		_frame.repaint();
	    	}
	    });
	}
	
	/************************************* BUTTONS AND ACTIONS **************************************/
	
	public void addControlButtons(JPanel buttonPanel)
	{
	    JButton playButton = new JButton("Play");
		JButton pauseButton = new JButton("Pause");
		JButton stepForwardButton = new JButton("Step Forward");
		JButton stepBackwardButton = new JButton("Step Backward");
	    
	    buttonPanel.setLayout(new BorderLayout(1,1));
	    buttonPanel.add(BorderLayout.WEST, playButton);
	    buttonPanel.add(BorderLayout.CENTER, pauseButton);
	    // buttonPanel.add(BorderLayout.WEST, stepBackwardButton);
	    buttonPanel.add(BorderLayout.EAST, stepForwardButton);
	    
	    playButton.addActionListener(new ActionListener()
	    { 
	    	public void actionPerformed(ActionEvent e) 
	    	{ 
	    		playButtonPressed();
	    	} 
	    });
	    
	    pauseButton.addActionListener(new ActionListener()
	    { 
	    	public void actionPerformed(ActionEvent e) 
	    	{ 
	    		pauseButtonPressed();
	    	} 
	    });
	    
	    stepForwardButton.addActionListener(new ActionListener()
	    { 
	    	public void actionPerformed(ActionEvent e) 
	    	{ 
	    		stepForwardPressed();
	    	} 
	    });
	    
	    stepBackwardButton.addActionListener(new ActionListener()
	    { 
	    	public void actionPerformed(ActionEvent e) 
	    	{ 
	    		stepBackwardPressed();
	    	} 
	    });
	}
	
	public void addProcessButtons( JPanel buttonPanel )
	{
		JButton showAll = new JButton("Show All Processes");
		JButton hideAll = new JButton("Hide All Processes");

		buttonPanel.setLayout(new BorderLayout(1,1));
		buttonPanel.add(BorderLayout.CENTER, showAll);
		buttonPanel.add(BorderLayout.EAST, hideAll);

		showAll.addActionListener(new ActionListener()
		{ 
			public void actionPerformed(ActionEvent e) 
			{ 
				showAllProcesses();
			} 
		});

		hideAll.addActionListener(new ActionListener()
		{ 
			public void actionPerformed(ActionEvent e) 
			{ 
				hideAllProcesses();
			} 
		});	    
	}
	
	// Play button unpauses the program. 
	public void playButtonPressed() {
		_isPaused = false;
	}
	  
	// Pause button pauses the program.
	public void pauseButtonPressed() {
		_isPaused = true;
	}
	  
	  
	// Step forward button forces the program to display only next state change and then pause again.  
	public void stepForwardPressed() {
		if (_isPaused)
		{
			_stepForward = true;
		}
	}
	
	// Step backward button forces the program to display only previous state and then pause again.  
	public void stepBackwardPressed() {
		if (_isPaused)
		{
			_stepBackward = true;
		}
	}
	
	public void checkButtonState() {
		while(_isPaused) {
			if(_stepForward){
				_stepForward = false;
				break;
			}
		}
	}
	
	// Update the mouseLocation object once the mouse event (motion) occurs
	// and it occurs a lot.
	public void getMouseCoordinates(MouseEvent e) {
		_mouseLocation = e.getPoint();
		_frame.repaint();
	}
	
	//If the mouse is clicked in the process area, change it's display status
	public void changeProcessDisplay( MouseEvent e ) {
		Point2D clickLocation = e.getPoint();
		for (int i = 0; i < _objCountP; i++) {
			if ( _processArray[i]._procArea.contains( clickLocation ) ) {
				_processArray[i]._isDisplayed = !_processArray[i]._isDisplayed;
				break;
			}
		}
		_frame.repaint();
	}
	
	public void showAllProcesses()
	{
		for (int i = 0; i < _objCountP; i++) {
			_processArray[i]._isDisplayed = true;
		} 
		_frame.repaint();  
	}

	public void hideAllProcesses()
	{
		for (int i = 0; i < _objCountP; i++) {
			_processArray[i]._isDisplayed = false;
		} 
		_frame.repaint();  
	}
	
	/************************************* DRAW OBJECTS **************************************/
	
	public class Cloud extends JPanel {
	    public void paint( Graphics g ) {  	
	    	Graphics2D g2d = ( Graphics2D ) g;
		    g2d.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
		    drawThreads( g2d );
		    drawProcessTable( g2d );
		    drawProgressBar( g2d );
	    }
	}
	
	public void drawProgressBar(Graphics2D g2d) 
	{	
		int startX, startY, endX, endY;
		int widthX, widthY;
		final int sideOffSet = 40; // How far away from the edge of the screen
		final int verticalOffset = 50;
		double progress;

		startX = sideOffSet;
		endX = (int) _vis.getBounds().getWidth()- sideOffSet;

		// startY = (int) _screenSize.getHeight()/2 + _tFrame._radius;
		startY = _tFrame._radius * 2 + CONSTANT.progressBarVOffSet;
		endY = startY + CONSTANT.progressBarHeight;

		widthX = endX - startX;
		widthY = endY - startY;

		g2d.setColor(Color.black);
		g2d.setStroke(new BasicStroke(2));
		g2d.drawRect(startX, startY, widthX, widthY);
		
		// Percentage completed
		progress = (double) (_currentSid - _minSid)/(_maxSid - _minSid);

		widthX = (int) (progress * widthX);

		g2d.setColor(Color.blue);
		g2d.fillRect(startX, startY, widthX, widthY);

	}
	
	public void drawThreads( Graphics2D thread2D ) {
		for ( int i = 0; i < _objCountT; i++ ) {
			// Only draw threads if their process status is set to display them
			if ( _procIdLookUp.get( Integer.toString( _threadArray[i]._procID ) )._isDisplayed ) {
				thread2D.setColor( _threadArray[i]._threadColour );
				thread2D.fill( _threadArray[i]._threadCircle );
				thread2D.draw( _threadArray[i]._threadCircle );
				
				if( _threadArray[i]._outGoingMessage != null ) {
					drawMessageArrow( thread2D, _threadArray[i]._outGoingMessage );
				}
				
				// Draw the text
				DisplayText( thread2D, _threadArray[i] );
			}
		}
	}
	
	public void DisplayText( Graphics2D text2D, Thread textThread ) {
		FontMetrics fm = text2D.getFontMetrics();
		text2D.setColor( Color.black );
		
		// Draw the PID on the respective circle
		String textProcID = Integer.toString( textThread._procID );
		int textW = fm.stringWidth( textProcID );
		text2D.drawString( textProcID, (int)textThread._threadCircle.getCenterX() - (textW/2), (int)textThread._threadCircle.getCenterY() );
		
		// Draw the TID on the respective circle
		String textTid = Integer.toString( textThread._tid );
		textW = fm.stringWidth( textTid );
		text2D.drawString( textTid, (int)textThread._threadCircle.getCenterX() - (textW/2), (int)textThread._threadCircle.getCenterY() + 10 );
	}
	

	public void updateThreadsAndMessages() {
		// This is done separately from the drawing phase
		for ( int i = 0; i < _objCountT; i++ ) {
			// Fade the message arrow
			_threadArray[i].updateMessageAlpha();
			
			// Decrease thread happiness since it has not interacted with a message
			_threadArray[i].updateThreadColour( false );
		}
	}
	
	public void drawMessageArrow( Graphics2D arrowTwoD, Message outGoing ) {
		arrowTwoD.setColor( outGoing._latency );
		// Use this to fade out the message arrow over 2 seconds
		arrowTwoD.setComposite( makeComposite( outGoing._frequency ) );
		
		arrowTwoD.fill( outGoing._rectangleOut );
		arrowTwoD.draw( outGoing._rectangleOut );
		
		arrowTwoD.fill( outGoing._headOut );
		arrowTwoD.draw( outGoing._headOut );
		
		// Reset back to original value
		arrowTwoD.setComposite( makeComposite( 1.0f ) );
	}
	
	private AlphaComposite makeComposite( float alpha ) {
	    int type = AlphaComposite.SRC_OVER;
	    return( AlphaComposite.getInstance(type, alpha) );
  	}
	
	// Draw the process table.
	public void drawProcessTable( Graphics2D g2d ) {
		int tableWidth = 500;
		int tableOffSet = 20;
		int rowHeight = 20;
		int rowOffSet;
		
		// Start coordinates of the table
		double startX = _screenSize.getWidth()/2 - tableWidth/2;
		// double startY = _screenSize.getHeight()/2 + _tFrame._radius + 20; // 20 is for the progress bar
		double startY = _tFrame._radius * 2 + CONSTANT.processTableVOffSet;
		
		g2d.setColor(Color.black);
		g2d.setStroke(new BasicStroke(2));
		
		// Draw column title rectangles
		g2d.drawRect((int) startX, (int) startY, 60, rowHeight);
		g2d.drawRect((int) startX + 60, (int) startY, 100, rowHeight);
		g2d.drawRect((int) startX + 160, (int) startY, 300, rowHeight);
		g2d.drawRect((int) startX + 460, (int) startY, 100, rowHeight);
		
		Font font = new Font("Arial", Font.BOLD, 12);
		g2d.setFont(font);
		
		g2d.drawString("Display", (int) startX + 10, (int) startY + 15);
		g2d.drawString("Process ID", (int) startX + 70, (int) startY + 15);
		g2d.drawString("Process Name", (int) startX + 170, (int) startY + 15);
		g2d.drawString("Thread Count", (int) startX + 470, (int) startY + 15);
		
		int count = 0;
		for (int i = 0; i < _objCountP; i++) 
		{
			if ( !_processArray[i]._isDead ) {
				count++;				  
				rowOffSet = rowHeight*count;

				// Rectangles to hold strings
				g2d.setColor( Color.BLACK );
				g2d.drawRect((int) startX, (int) startY + rowOffSet, 60, rowHeight);
				// Update the process click area to turn threads on and off
				_processArray[i]._procArea = new Rectangle2D.Double( (int) startX, (int) startY + rowOffSet, 60, rowHeight ); 
				
				g2d.drawRect((int) startX + 60, (int) startY + rowOffSet, 100, rowHeight);
				g2d.drawRect((int) startX + 160, (int) startY + rowOffSet, 300, rowHeight);
				g2d.drawRect((int) startX + 460, (int) startY + rowOffSet, 100, rowHeight);
				
				
				// Yes Green is process threads are being displayed
				// No Red if user has turned them off
				if (_processArray[i]._isDisplayed)
				{
					g2d.setColor( new Color(0, 150, 0) );
					g2d.drawString( "YES", (int) startX + 10, (int) startY + rowOffSet + 15 );
				}
				else
				{
					g2d.setColor( Color.red );
					g2d.drawString( "NO", (int) startX + 10, (int) startY + rowOffSet + 15 );
				}
				// Strings
				g2d.setColor( Color.BLACK );
				g2d.drawString( Integer.toString( _processArray[i]._procId ), (int) startX + 70, (int) startY + rowOffSet + 15 );
				g2d.drawString( _processArray[i]._name, (int) startX + 170, (int) startY + rowOffSet + 15);
				g2d.drawString( Integer.toString( _processArray[i]._threadCount ), (int) startX + 470, (int) startY + rowOffSet + 15 );
			}
		}
		
		// Since the end of the Process table is the most Southern point in the visual
		// update the preferred size of the vis panel so that the scrolling works properly.
		int newHeight = (int) startY + 20*count + 100;
		
		// Check if the diameter of the thread frame is larger than the width of the screen
		// If so, then update the horizontal scroll bar
		int newWidth = _tFrame._radius*2;
		
		int heightNow = _vis.getBounds().height;
		int widthNow = _vis.getBounds().width;
		
		if (newHeight < heightNow)
			newHeight = heightNow;
		
		if (newWidth < widthNow)
			newWidth = widthNow;
		
		if ( newWidth > widthNow || newHeight > heightNow ) {
			_vis.setPreferredSize(new Dimension (widthNow, newHeight));
			_vis.setBounds(0, 0, widthNow, newHeight);
		}
	}
	
	
//	// If the mouse is clicked in the process display area of the process table, toggle its display status
//	public void changeProcessDisplay(MouseEvent e)
//	{	
//		int proc_id = -1;
//		boolean display = true;
//		  
//		Point2D clickLocation = e.getPoint();
//		for (int i = 0; i < procs.length; i++)
//		{
//			if (procs[i].procArea.contains(clickLocation))
//			{
//				display = procs[i].isDisplayed;
//				procs[i].isDisplayed = !display;
//				proc_id = procs[i].proc_id;
//				break;
//			}
//		}
//	  
//		if (proc_id > 0)
//		{
//			for (int i = 0; i < cpus.length; i++)
//			{
//				for (int j = 0; j < cpus[i].threads.length; j++) 
//				{
//					if (cpus[i].threads[j].proc_id == proc_id) 
//					{
//						cpus[i].threads[j].isDisplayed = !display;
//					}
//				}
//			} 
//		}
//	  
//		frame.repaint();
//	}
		
	/*************************************** GET OBJECTS FROM DATABASE ******************************************/
	
	public void getThread( ResultSet rs ) {
		try {
			// Retrieve process info
			int pid = 0, procId = 0;
			String procName = null;
			
			pid			= rs.getInt(  "pid" );
			procId 		= rs.getInt( "proc_id" );
			procName	= rs.getString( "name" );			
			
			// Check to see if we need to create a new process
			if( !_procIdLookUp.containsKey( Integer.toString( procId ) ) ) {
				Process newProc = new Process( procId, pid, procName );
				_processArray[ _objCountP ] = newProc;
				_procIdLookUp.put( Integer.toString( procId ), newProc );
				_objCountP++;
			} else {
				// Increment the thread count is process already exists
				_procIdLookUp.get( Integer.toString( procId ) ).addNewThread();
			}
			
			// Retrieve thread info and add new thread
			int openPorts = 0;
		    int tid = 0;
		    long timeStamp = 0;
		    	
	    	openPorts	= rs.getInt( "open_ports" );
	    	openPorts 	= CONSTANT.minThreadRadius;
	    	tid 		= rs.getInt( "tid" );
	    	timeStamp	= rs.getLong( "timestamp" );
	    	
	    	_threadArray[ _objCountT ] = new Thread( openPorts, 0, 0, procId, tid, timeStamp, _objCountT );
	    	_threadArray = _tFrame.resizeThreadFrame( _threadArray, _objCountT + 1 );
	    	_tidLookUp.put( Integer.toString( procId ) + Integer.toString( tid ), _threadArray[ _objCountT ] );
	    	_objCountT++;
	    } catch ( Exception e ) {
	        e.printStackTrace();
	    }
	}
	
	public void updateThread( ResultSet rs, int procId, int tid ) {
		try {
			// Check to see if the thread has been killed, else update the thread colour
			String state = rs.getString( "state" );
			if (state.trim().compareToIgnoreCase( "THDEAD" ) == 0) {
				Thread tidToDelete = _tidLookUp.remove( Integer.toString( procId ) + Integer.toString( tid ) );
				tidToDelete._threadColour = Color.WHITE;
				tidToDelete._isDead = true;
				_objCountT--;
				
				// Update the process thread count as well. If 0 mark process for deletion later on
				int threadCount = _procIdLookUp.get( Integer.toString( procId ) )._threadCount--;
				if ( threadCount == 0 ) {
					Process pidToDelete = _procIdLookUp.remove( Integer.toString( procId ) );
					pidToDelete._isDead = true;
					_objCountP--;
				}
			} 
//			else {
//				// Update the thread colour
//				long timeStamp = 0;
//		    	timeStamp = rs.getLong( "timestamp" );
//		    	_tidLookUp.get( Integer.toString( procId ) + Integer.toString( tid ) ).updateThreadColour( true );
//			}
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	}
	
	public void getMessage( ResultSet rs ) {		
		try {
		    int sTid = 0, sProcId = 0;
		    int rTid = 0, rProcId = 0;
		    int sid = 0, rcvid = 0, size = 0, latency = 0;
		    String type = null;
			
	    	// Sending thread and process ids
			sTid 	= rs.getInt( "stid" );
	    	sProcId = rs.getInt( "sproc_id" );
	    	String sendingKey = Integer.toString(sProcId) + Integer.toString(sTid);
	    	
	    	// Receivng thread and process ids
	    	rTid 	= rs.getInt( "rtid" );
	    	rProcId = rs.getInt( "rproc_id" );
	    	String receivingKey = Integer.toString(rProcId) + Integer.toString(rTid);
	    	
	    	// The rest of the information
	    	sid 	= rs.getInt( "sid" );
	    	size 	= rs.getInt( "size" );
	    	latency = rs.getInt( "latency" );
	    	type 	= rs.getString( "type" );
	    	
	    	// Most sizes are between 1 - 100, therefore to display a reasonable size on screen, let's limit the displayed size to 20
	    	// If in the odd case, the size is > 100, then display a size of 30 to let the user know the difference
	    	if ( size <= 100 ) {
	    		size = (size / 100) * 20;
	    	} else if ( size <= 2000 ) {
	    		size = 20 + (size / 2000) * 5;
	    	} else {
	    		size = 30;
	    	}
	    	
	    	// ORIGINAL !!! size = (size > 100) ? 30 : (size / 100) * 20;
	    	
	    	// Just for demo purposes
	    	Color lat = new Color( (int)( ( Math.random() * latency ) % 255 ), (int)( ( Math.random() * latency ) % 255 ), (int)( ( Math.random() * latency ) % 255 ) );
	    	
	    	// Make sure that we have the sending and receiving tids
	    	if ( _tidLookUp.containsKey( sendingKey ) && _tidLookUp.containsKey( receivingKey ) ) {
	    		Thread sendingThread = _tidLookUp.get( sendingKey );
	    		Thread receivingThread = _tidLookUp.get( receivingKey );
	    		Message message = new Message( size, sendingThread, receivingThread, lat );
	    		
	    		// Allow lookup of message on both ends so that we can update the message position when
	    		// either thread is resized
	    		receivingThread.setIncomingMessage( message );
	    		sendingThread.setOutGoingMessage( message );
	    		
	    		// Reset the thread colours back to green since they have just interacted with a message
	    		sendingThread.updateThreadColour( true );
	    		receivingThread.updateThreadColour( true );
	    		
				_objCountM++;
	    	}
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	}
	
//	public void getProcess( ResultSet rs ) {
//		// Add new process to the process array
//		try {		    
//		    String name;
//		    int procId = 0, pid = 0;
//		    
//		    procId = rs.getInt( "proc_id" );
//		    pid = rs.getInt( "pid" );
//	    	name = rs.getString( "name" );
//	    	
//	    	_processArray[ _objCountP ] = new Process( procId, pid, name );
//	    	_procIdLookUp.put( Integer.toString( procId ), _processArray[ _objCountP ] );
//	    	_pidLookUp.put( procId, pid );
//	    } catch (Exception e) {
//	        e.printStackTrace();
//	    }
//	}
//	
//	public void initializeProcessArray( ResultSet rs ) {
//		// Add new processes to the process hash map before the program is run
//		try {
//			String name;
//		    int procId = 0, pid = 0;
//		    
//			while( rs.next() ) {
//			    procId = rs.getInt( "proc_id" );
//			    pid = rs.getInt( "pid" );
//		    	name = rs.getString( "name" );
//		    	
//		    	_procIdLookUp.put( Integer.toString( procId ), _processArray[ _objCountP ] );
//		    	_objCountP++;
//			}
//	    } catch (Exception e) {
//	        e.printStackTrace();
//	    }
//	}
	
	/*******************************************DATABASE CONNECT / DISCONNECT ************************************/
	
	public class PromptDB
	{
		JFrame frame = new JFrame("Please Choose the Database");
		JPanel panel = new JPanel();
		JLabel label = new JLabel("Please Enter the Database Name:");
		JTextField text = new JTextField("bm_basicmath");
		JButton button = new JButton("OK");
	  
		boolean isButtonPressed = false;
	    
		public PromptDB()
		{		  
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setBounds(500, 500, 300, 150);
		  
			//panel.setPreferredSize(new Dimension(400,200));
			panel.setLayout(new BorderLayout(1,20));
			  
			panel.add(BorderLayout.NORTH, label);
			panel.add(BorderLayout.CENTER, text);
			panel.add(BorderLayout.SOUTH, button);
			  
			frame.getContentPane().add(panel);
			frame.setVisible(true);
			   
			button.addActionListener(new ActionListener()
			{ 
				public void actionPerformed(ActionEvent e) 
				{ 
					buttonPressed();
				} 
			});
			  		  
			while(!isButtonPressed)
			{
			
			}
		}
		  
		public void buttonPressed()
		{
			_dbName = text.getText().trim();
			frame.dispose();
			isButtonPressed = true;
		}
	}
	
	public Connection openDbConnection() {
		Connection openConnection = null;
		try {
		    String userName = "visidebug";
		    String password = "fydp10";
		    String url = "jdbc:mysql://klagenfurt.uwaterloo.ca/" + _dbName; // //127.0.0.1:3306/" + _dbName;
		    
		    Class.forName ("com.mysql.jdbc.Driver").newInstance();
		    openConnection = DriverManager.getConnection (url, userName, password);               
		    System.out.println ("Database connection established");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return openConnection;
	}
	
	public void closeDbConnection( Connection closeConnection ) {
		// Close the connection to the database before terminating the program
		try {	
			closeConnection.close();
			System.out.println ("Database connection terminated");
		} catch (Exception e) { /* ignore close errors */ }
	}
	
	/**************************************************** MAIN ***************************************************/
	
	public static void main(String[] args) {
		
		MessageCloud messageCloud = new MessageCloud();
		
		// Try to connect to the database before doing anything else
		Connection dbConnection = messageCloud.openDbConnection();
		
	    // Prepare query statements to retrieve information from the database
		String queryThread = String.format( 
				"SELECT t.proc_id, pid, name, tid, state, cpu, priority, open_ports, s.sid, timestamp " +
				"FROM thread t " +
				"JOIN process p ON p.proc_id = t.proc_id " +
				"RIGHT JOIN snapshot s ON s.sid = t.sid " +
				// "JOIN message m on (m.stid = t.tid OR m.rtid = t.tid) " +
				"JOIN message m on ( m.sproc_id = t.proc_id OR m.rproc_id = t.proc_id ) " +
				"GROUP BY t.proc_id, pid, name, tid, state, cpu, priority, open_ports, sid, timestamp " +
				"ORDER BY s.sid"
				);
		
		String queryMessage = String.format(
				"SELECT *" +
				"FROM message"
				);
		
		String queryProcess = String.format(
				"SELECT *" +
				"FROM process"
				);
		
		try {
			Statement stmtThread 	= dbConnection.createStatement();
		    Statement stmtMessage 	= dbConnection.createStatement();
		    Statement stmtProcess 	= dbConnection.createStatement();
			
		    // Execute above queries
			ResultSet rsThread 	= stmtThread.executeQuery( queryThread );
			ResultSet rsMessage = stmtMessage.executeQuery( queryMessage );
			ResultSet rsProcess = stmtProcess.executeQuery( queryProcess );
			
			// Allocate space for each array depending on how many rows are in the database
			rsThread.last();
			messageCloud._threadArray = new Thread[ rsThread.getRow() ];
			messageCloud._maxSid = rsThread.getInt( "sid" ); // Progress bar
			
			rsMessage.last();
			messageCloud._messageArray = new Message[ rsMessage.getRow() ];
			
			rsProcess.last();
			messageCloud._processArray = new Process[ rsProcess.getRow() ];
			
			int procId = 0; // Auto incremented in database to have unique id for each process !!!THIS IS NOT THE PID!!!
			int tid = 0;	// thread id
			int threadSid = 0, messageSid = 0;
			
			// Reset database row position back to start
			rsThread.beforeFirst();
			rsMessage.first();
			rsProcess.beforeFirst();

			boolean needMinSid = true;
			
			messageSid = rsMessage.getInt( "sid" );
			while( rsThread.next() ) {
				if( rsThread.getString( "state" ) != null ) {
					
					// Update the message alpha value
					messageCloud.updateThreadsAndMessages();
					
					messageCloud.checkButtonState();
					System.out.println( rsThread.getRow() ); //DEBUG
					
					procId 		= rsThread.getInt( "proc_id" );
					tid 		= rsThread.getInt( "tid" );
					threadSid 	= rsThread.getInt( "sid" );
					
					// Keep track of sids for the progress bar
					if ( needMinSid ) {
						messageCloud._minSid = threadSid;
						needMinSid = false;
					}
					messageCloud._currentSid = threadSid;
					
					// Check to see if the thread already exists, if so update the thread, if not create the new thread
					// and display it on the drawing
					if ( messageCloud._tidLookUp.containsKey( Integer.toString( procId ) + Integer.toString( tid ) ) ) {
						messageCloud.updateThread( rsThread, procId, tid );
					} else {
						messageCloud.getThread( rsThread );
					}
					
					// If we have passed a message timestamp (sid), then display the message arrow on screen 
					if ( messageSid != -1 && messageSid < threadSid	) {
						// Make sure that none of the fields are null
						// NOTE: we will never have an actual value of zero
						if( rsMessage.getInt( "sproc_id" ) != 0 && rsMessage.getInt( "stid" ) != 0
								&& rsMessage.getInt( "rproc_id" ) != 0 && rsMessage.getInt( "rtid" ) != 0 )	{
							messageCloud.getMessage( rsMessage );
						}
						// Read in next value or set to -1 if no more
						messageSid = -1;
						if( rsMessage.next() )
							messageSid = rsMessage.getInt( "sid" );
					}
				}
				
				java.lang.Thread.currentThread().sleep(100);
				messageCloud._frame.repaint();
			}
			
			// Let the final arrows fade from the drawing, 20 iterations is equal to 2 seconds which is fade time
			for ( int i = 0; i < 20; i++ ) {
				java.lang.Thread.currentThread().sleep(100);
				messageCloud.updateThreadsAndMessages();
				messageCloud._frame.repaint();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		// Close the database connection
		messageCloud.closeDbConnection(dbConnection);
	}
}
