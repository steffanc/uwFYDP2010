package graphics;

import java.lang.String;
import java.util.Iterator;
import java.util.LinkedList;
import java.awt.Color;
import java.awt.geom.Ellipse2D;

public class Thread {
	
	public Ellipse2D.Double _threadCircle;
	public int _position;
	public int _radius;
	public int _procID, _tid;
	public boolean _isDead = false;
	public Color _threadColour = new Color(0, 255, 0);
	public long _lastTimeStamp = 0;
	
	private int R = 0, G = 255, B = 0;
	
	// Message structures
	// public LinkedList<String> _messageConn = new LinkedList<String>();
	public LinkedList<Message> _inComingMessages = new LinkedList<Message>();
	Message _outGoingMessage = null;
	
	public Thread( int radius, int positionX, int positionY, int procID, int tid, long lastTimeStamp, int position ) {
		// Create a new Ellipse to represent the circle
		_threadCircle = new Ellipse2D.Double( positionX, positionY, radius, radius );
		_procID = procID;
		_tid = tid;
		_lastTimeStamp = lastTimeStamp;
		_position = position;
	}
	
	public void resizeThread( int increment ) {
		// Increase the radius of the thread circle and move the position of the circle
		// to accomodate the increase
		_radius += increment;
		double centerX = _threadCircle.getCenterX();
		double centerY = _threadCircle.getCenterY();
		double cornerX = centerX - ( _threadCircle.width/2 + increment );
		double cornerY = centerY - ( _threadCircle.height/2 + increment );
		_threadCircle.setFrameFromCenter(centerX, centerY, cornerX, cornerY);
		
		// Update any message arrows that are attached to the thread
		repositionMessageArrows();
	}
	
	// When the thread is resized or moved, need to redraw the message arrows attached
	public void repositionMessageArrows() {
		Iterator<Message> it = _inComingMessages.iterator();
		while ( it.hasNext() ) {
			Message temp = it.next();
			temp.arrowUpdate();
		}
		
		if ( _outGoingMessage != null )
			_outGoingMessage.arrowUpdate();
	}
	
	public void updateThreadColour( boolean resetColour ) {
		if ( resetColour ) {
			// Back to happy green if the thread has just sent or received a message
			R = 0;
			G = 255;
			B = 0;
		} else {
			// Sequence: Green, Yellow, Red, Purple
			// up R, down G, up Blue
			// Yellow is RGB: 255, 255, 0
			// Purple is RGB: 220, 0, 255
			
			// Fade to this colour ( each iteration is 100 ms )
			if ( R < 220 ) {
				R += ( ( R + 2 ) <= 220 ) ? 2 : 0;
			} else if ( G > 0 ) {
				G -= ( ( R - 5 ) >= 0 ) ? 5 : 0;
			} else if ( B < 255 ) {
				B += ( ( B + 5 ) <= 255 ) ? 5 : 0;
			}
		}	
		_threadColour = new Color(R, G, B);
	}
	
	public void updateMessageAlpha() {
		// Fade the message arrow out over 2 seconds
		if ( _outGoingMessage != null && _outGoingMessage._frequency > 0.0f ) {
			_outGoingMessage._frequency -= 0.05f;
			if( _outGoingMessage._frequency < 0 ) {
				_outGoingMessage._frequency = 0.0f;
				messageFinish();
			}
		}
	}
	
	public void setIncomingMessage( Message toReceive ) {
		_inComingMessages.add( toReceive );
	}
	
	public void setOutGoingMessage( Message toSend ) {
		if( _outGoingMessage != null ) {
			// Reduce the thread currently receiving the message
			_outGoingMessage._endThread.resizeThread( -CONSTANT.threadSizeInc );
		}
		// Increase size of new thread receiving the message
		_outGoingMessage = toSend;
		toSend._endThread.resizeThread( CONSTANT.threadSizeInc );
	}
	
	public void messageFinish() {
		// Message has faded to blank, so update the thread size it was attached to
		_outGoingMessage._endThread.resizeThread( -CONSTANT.threadSizeInc );
		
		// Remove the message from both sending and receiving threads
		_outGoingMessage._endThread._inComingMessages.remove( _outGoingMessage );
		_outGoingMessage = null;
	}
}
