package jag.game.baller;

import android.os.Vibrator;

public class GameField {
	
	private final float ballRadius;//Ball radius
	private float ballPosX, ballPosY;//Ball position
	private int fieldWidth, fieldHeight;//Field dimensions
	private float velX, velY;//Ball velocity
	private float accelX, accelY;//Ball acceleration
    private volatile long lastUpdateTime = -1;//Update time
	public final Object LOCK = new Object();//Lock
	private Vibrator vibrator;//Vibrator
	private static final float PIXELS_METER = 10;//In order to make it faster, use this constant for increasing the velocity
	private static final float BOUNCE_FACTOR = 0.8f;//Bouncing factor
    private static final float STOP_BOUNCE_THRESHOLD = 5f;//Stop bouncing if the ball velocity is lower than this threshold
    private static final float HIT_BOUNCE = -80;//If the ball is hit, the velocity on Y axis will be this constant.
    
	public GameField(int ballRadius) {
		this.ballRadius = ballRadius;
	}
    
    public void updateBallPosition() {
    	long curTime = System.currentTimeMillis();
        if (lastUpdateTime < 0) {
            lastUpdateTime = curTime;
            return;
        }
        long elapsedTime = curTime - lastUpdateTime;
        lastUpdateTime = curTime;
        calculateBallPosition(elapsedTime);
        checkBouncing();
    }
    
    public void calculateBallPosition(long elapsedTime) {
        float lAccelx, lAccely, lVelx, lVely;
		float lBallY;
		float lBallX;
		//Retrieve fields
        synchronized (LOCK) {
            lBallX  = ballPosX;
            lBallY  = ballPosY;
            lVelx   = velX;            
            lVely   = velY;
            lAccelx  = accelX;
            lAccely  = accelY;
        }      
        // update the velocity (meters / second)
        lVelx += elapsedTime * lAccelx * PIXELS_METER / 1000;
        lVely += elapsedTime * lAccely * PIXELS_METER / 1000;
        // update the position
        lBallX += lVelx * elapsedTime * PIXELS_METER / 1000;
        lBallY += lVely * elapsedTime * PIXELS_METER / 1000; 
        // Update fields
        synchronized (LOCK) {
        	ballPosX = lBallX;
        	ballPosY = lBallY;
            velX = lVelx;
            velY = lVely;
        }
    }

    
    public void checkBouncing() {
        
        float lWidth, lHeight;
        float lVelX, lVelY;
		float lBallY;
		float lBallX;
		
        synchronized (LOCK) {
            lWidth  = fieldWidth;
            lHeight = fieldHeight;
            lBallX  = ballPosX;
            lBallY  = ballPosY;
            lVelX     = velX;            
            lVelY     = velY;
        }
        
        boolean bouncedX = false;
        boolean bouncedY = false;

        //Bounce Y
        if (lBallY - ballRadius < 0) {
            lBallY = ballRadius;
            lVelY = -lVelY * BOUNCE_FACTOR;
            bouncedY = true;
        } else if (lBallY + ballRadius > lHeight) {
            lBallY = lHeight - ballRadius;
            lVelY = -lVelY * BOUNCE_FACTOR;
            bouncedY = true;
        }
        if (bouncedY && Math.abs(lVelY) < STOP_BOUNCE_THRESHOLD) {
            lVelY = 0;  
            bouncedY = false;
        }

        //Bounce X
        if (lBallX - ballRadius < 0) {
        	lBallX = ballRadius;
        	lVelX = -lVelX * BOUNCE_FACTOR;
        	bouncedX = true;
        } else if (lBallX + ballRadius > lWidth) {
            lBallX = lWidth - ballRadius;
            lVelX = -lVelX * BOUNCE_FACTOR;
            bouncedX = true;
        }
        if (bouncedX && Math.abs(lVelX) < STOP_BOUNCE_THRESHOLD) {
        	lVelX = 0;
        	bouncedX = false;
        }
        
        // Update fields
        synchronized (LOCK) {
        	ballPosX = lBallX;
        	ballPosY = lBallY;
            velX = lVelX;
            velY = lVelY;
        }
        
        //Vibrate if bouncing
        if (bouncedX || bouncedY) {
        	if(vibrator !=null){
        		vibrator.vibrate(20L);
        	}
        }
    }
    
	public void checkBallHit(float x, float y) {
		float width = ballRadius;
		float heigth = ballRadius;
		float rx = ballPosX;
		float ry = ballPosY;
		
		if (x < rx - width || x > rx + width || y < ry - width || y > ry + heigth) {
			return;
		}
		
		synchronized (LOCK) {
			velX = 0;
			velY = HIT_BOUNCE;
		}
	}
	
	public float getBallPosX() {
		return ballPosX;
	}

	public float getBallPosY() {
		return ballPosY;
	}

	public void setBallPosY(float ballPosY) {
		this.ballPosY = ballPosY;
	}

	public void setBallPosX(float ballPosX) {
		this.ballPosX = ballPosX;
	}

	public void setAccel(float ax, float ay) {
		synchronized (LOCK) {
			this.accelX = -ax;
			this.accelY = ay;
		}
	}
	
	public void setSize(int width, int height) {
		synchronized (LOCK) {
			this.fieldWidth = width;
			this.fieldHeight = height;
		}
	}
	
    public void setVibrator(Vibrator v) {
    	vibrator = v;
    }
}
