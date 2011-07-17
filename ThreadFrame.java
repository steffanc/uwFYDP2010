package graphics;

import java.awt.Dimension;
import java.awt.Toolkit;

public class ThreadFrame {
	
	public int _radius = 0; 
	public int _offSetRadius = 0;
	
	public ThreadFrame() {}
	
	public Thread [] resizeThreadFrame( Thread [] threadArray, int objCount ) {
		int listSize = objCount;
		
		if ( listSize != 0 ) {
			
			Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
			double xMiddle = screenSize.getWidth() / 2;
			// double yMiddle = ( screenSize.getHeight() / 2 ) - CONSTANT.verticalOffSet;
			double yMiddle = _radius + CONSTANT.verticalOffSet;
			
			if ( listSize == 1 ) {

				threadArray[0]._threadCircle.x = xMiddle;
				threadArray[0]._threadCircle.y = yMiddle;
				
			} else {
				
				// Increase the radius of the circle to fit in all nodes
				if ( _radius == 0 ) _radius = 1;
				double threadSpace = ( 2 * Math.PI * _radius ) / listSize;
				
				while( threadSpace < ( CONSTANT.minThreadRadius + CONSTANT.threadOffset ) ) {
					_radius++;
					threadSpace = ( 2 * Math.PI * ( _radius ) ) / listSize;
				}
				
				double angle = threadSpace / _radius;
				double tempAngle = 0;
				double xMidCoord = 0, yMidCoord = 0;
				double xCorCoord = 0, yCorCoord = 0;
				
				// Draw all points on the circle
				for ( int i = 0; i < listSize; i++ ) {
					tempAngle = angle * i;
					
					// Point is to the right
					if ( tempAngle >= 0 && tempAngle <= Math.PI ) {
						xMidCoord = xMiddle + ( Math.abs( Math.sin( tempAngle ) ) * _radius );
					} else {
					// Point is to the left
						xMidCoord = xMiddle - ( Math.abs( Math.sin( tempAngle ) ) * _radius );
					}
					// Point is down
					if ( tempAngle >= (Math.PI / 2) && tempAngle <= ( 1.5 * Math.PI ) ) {
						yMidCoord = yMiddle + ( Math.abs( Math.cos( tempAngle ) ) * _radius );
					} else {
					// Point is up
						yMidCoord = yMiddle - ( Math.abs( Math.cos( tempAngle ) ) * _radius );
					}
					
					xCorCoord = xMidCoord - ( threadArray[i]._threadCircle.width/2 );
					yCorCoord = yMidCoord - ( threadArray[i]._threadCircle.height/2 );
					
					// Assign coordinates to the thread
					threadArray[i]._threadCircle.x = xCorCoord;
					threadArray[i]._threadCircle.y = yCorCoord;
					
					// Update any attached message arrows
					threadArray[i].repositionMessageArrows();
				}
			}
		}

		return threadArray;
	}
}
