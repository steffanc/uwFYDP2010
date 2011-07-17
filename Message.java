package graphics;

import java.awt.Color;
import java.awt.Polygon;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;

public class Message {
	
	// Points to define rectangle
	private Point2D.Double _botLeft = new Point2D.Double();
	private Point2D.Double _topLeft = new Point2D.Double();
	
	// Points to define outgoing triangle arrow head
	private Point2D.Double _outOne 	 = new Point2D.Double();
	private Point2D.Double _outTwo 	 = new Point2D.Double();
	private Point2D.Double _outThree = new Point2D.Double();
	
	// Polygons representing the arrow parts
	public Polygon _rectangleOut = new Polygon();
	public Polygon _headOut 	 = new Polygon();
	
	// Message attributes
	public int _thickness;
	public Color _latency;
	public float _frequency = 1.0f; // Arrow is completely solid when first sent
	
	// Start and ending threads
	Thread _startThread;
	Thread _endThread;

	public Message( int thickness, Thread startThread, Thread endThread, Color latency ) {
		_startThread 	= startThread;
		_endThread 		= endThread;
		_thickness 		= ( thickness != 0 ) ? thickness : 5;
		_latency 		= latency;
		arrowCreate( _thickness, _startThread._threadCircle, _endThread._threadCircle, _latency );
	}
	
	public void arrowUpdate() {
		arrowCreate( _thickness, _startThread._threadCircle, _endThread._threadCircle, _latency );
	}
		
	public void arrowCreate( int thickness, Ellipse2D.Double startCircle, Ellipse2D.Double endCircle, Color latency ) {

		// Create points to be located on the closest point of the perimeter between both circles
		double height1 = startCircle.getHeight()/2;
		double height2 = endCircle.getHeight()/2 + Math.sqrt(3)*thickness;
		Point2D.Double perim1 = new Point2D.Double();
		Point2D.Double perim2 = new Point2D.Double();
		
		// Length of x and y distances between circle centers
		double lenX = startCircle.getCenterX() - endCircle.getCenterX();
		double lenY = startCircle.getCenterY() - endCircle.getCenterY();
		
		// Calculate the angle at which the rectangle of the arrow base is leaning
		double triWidth = Math.abs( lenX );
		double triHeight = Math.abs( lenY );
		double angle1 = Math.atan( triHeight / triWidth );
		double angle2 = Math.PI/2 - angle1;
		
		// Calculate where the points should be located on the perimeters of both circles
		// These points will not be on the circle if there is an arrow head
		// Calculate the point of the arrow head touching the circle
		if ( lenY >= 0 ) {
			_outOne.y = endCircle.getCenterY()  + Math.sin( angle1 ) * endCircle.getHeight()/2;
			
			perim1.y = startCircle.getCenterY() - Math.sin( angle1 ) * height1;
			perim2.y = endCircle.getCenterY() 	+ Math.sin( angle1 ) * height2;
		} else {
			_outOne.y = endCircle.getCenterY() 	- Math.sin( angle1 ) * endCircle.getHeight()/2;
			
			perim1.y = startCircle.getCenterY() + Math.sin( angle1 ) * height1;
			perim2.y = endCircle.getCenterY() 	- Math.sin( angle1 ) * height2;
		}
		
		if ( lenX <= 0 ) {
			_outOne.x = endCircle.getCenterX() 	- Math.cos( angle1 ) * endCircle.getHeight()/2;
			
			perim1.x = startCircle.getCenterX() + Math.cos( angle1 ) * height1;
			perim2.x = endCircle.getCenterX() 	- Math.cos( angle1 ) * height2;
		} else {
			_outOne.x = endCircle.getCenterX() 	+ Math.cos( angle1 ) * endCircle.getHeight()/2;
			
			perim1.x = startCircle.getCenterX() - Math.cos( angle1 ) * height1;
			perim2.x = endCircle.getCenterX() 	+ Math.cos( angle1 ) * height2;
		}
		
		// Create 4 corner points for the outline of the rectangle arrow base
		// Calculate the other two points of the arrow heads
		if ( lenY >= 0 ) {
			_botLeft.x = ( perim1.x 	- Math.cos( angle2 )*( thickness/2 ) );
			_topLeft.x = ( perim2.x 	- Math.cos( angle2 )*( thickness/2 ) );
			
			_outTwo.x = ( perim2.x 		+ Math.cos( angle2 ) * thickness );
			_outThree.x = ( perim2.x 	- Math.cos( angle2 ) * thickness );
		} else {
			_botLeft.x = ( perim1.x 	+ Math.cos( angle2 )*( thickness/2 ) );
			_topLeft.x = ( perim2.x 	+ Math.cos( angle2 )*( thickness/2 ) );
			
			_outTwo.x = ( perim2.x 		- Math.cos( angle2 ) * thickness );
			_outThree.x = ( perim2.x 	+ Math.cos( angle2 ) * thickness );
		}
		
		if ( lenX <= 0 ) {
			_botLeft.y = ( perim1.y 	- Math.sin( angle2 )*( thickness/2 ) );
			_topLeft.y = ( perim2.y 	- Math.sin( angle2 )*( thickness/2 ) );
			
			_outTwo.y = ( perim2.y 		+ Math.sin( angle2 ) * thickness );
			_outThree.y = ( perim2.y 	- Math.sin( angle2 ) * thickness );
		} else {
			_botLeft.y = ( perim1.y 	+ Math.sin( angle2 )*( thickness/2 ) );
			_topLeft.y = ( perim2.y 	+ Math.sin( angle2 )*( thickness/2 ) );

			_outTwo.y = ( perim2.y 		- Math.sin( angle2 ) * thickness );
			_outThree.y = ( perim2.y 	+ Math.sin( angle2 ) * thickness );
		}
		
		// Create outbound rectangle
		_rectangleOut.reset();
		_rectangleOut.addPoint( (int)_botLeft.x, 	(int)_botLeft.y );
		_rectangleOut.addPoint( (int)perim1.x, 	(int)perim1.y );
		_rectangleOut.addPoint( (int)perim2.x, 	(int)perim2.y );
		_rectangleOut.addPoint( (int)_topLeft.x, 	(int)_topLeft.y );
		
		// Create outbound arrow head
		_headOut.reset();
		_headOut.addPoint( (int)_outOne.x, 	(int)_outOne.y );
		_headOut.addPoint( (int)perim2.x, 	(int)perim2.y );
		_headOut.addPoint( (int)_outThree.x, 	(int)_outThree.y );
	}
}
