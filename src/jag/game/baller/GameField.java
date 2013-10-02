package jag.game.baller;

import android.os.Vibrator;

public class GameField {
	private final float pixelsPerMeter = 10;
	private final float ballRadius;
	private float ballPosX, ballPosY;
	private int fieldWidth, fieldHeight;
	private float velocityX, velocityY;
	private float accelX, accelY;
    private static final float bounceFactor = 0.8f;
    // if the ball bounces and the velocity is less than this constant, stop bouncing.
    private static final float STOP_BOUNCE_THRESHOLD = 5f;
    private static final float HIT_BOUNCE = -80;
    private volatile long lastTimeMs = -1;
	public final Object LOCK = new Object();
	private Vibrator vibrator;

	public GameField(int ballRadius) {
		this.ballRadius = ballRadius;
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
    
    public void updateBallPosition() {
        long curTime = System.currentTimeMillis();
        if (lastTimeMs < 0) {
            lastTimeMs = curTime;
            return;
        }
        
        float lWidth, lHeight;
        float lAx, lAy, lVx, lVy;
		float lBallY;
		float lBallX;
		
        synchronized (LOCK) {
            lWidth  = fieldWidth;
            lHeight = fieldHeight;
            lBallX  = ballPosX;
            lBallY  = ballPosY;
            lVx     = velocityX;            
            lVy     = velocityY;
            lAx     = accelX;
            lAy     = accelY;
        }

        long elapsedMs = curTime - lastTimeMs;
        lastTimeMs = curTime;
        
        // update the velocity (meters / second)
        lVx += elapsedMs * lAx * pixelsPerMeter / 1000;
        lVy += elapsedMs * lAy * pixelsPerMeter / 1000;

        // update the position
        lBallX += lVx * elapsedMs * pixelsPerMeter / 1000;
        lBallY += lVy * elapsedMs * pixelsPerMeter / 1000;
        
        boolean bouncedX = false;
        boolean bouncedY = false;

        if (lBallY - ballRadius < 0) {
            lBallY = ballRadius;
            lVy = -lVy * bounceFactor;
            bouncedY = true;
        } else if (lBallY + ballRadius > lHeight) {
            lBallY = lHeight - ballRadius;
            lVy = -lVy * bounceFactor;
            bouncedY = true;
        }
        if (bouncedY && Math.abs(lVy) < STOP_BOUNCE_THRESHOLD) {
            lVy = 0;  
            bouncedY = false;
        }

        if (lBallX - ballRadius < 0) {
        	lBallX = ballRadius;
        	lVx = -lVx * bounceFactor;
        	bouncedX = true;
        } else if (lBallX + ballRadius > lWidth) {
            lBallX = lWidth - ballRadius;
            lVx = -lVx * bounceFactor;
            bouncedX = true;
        }
        if (bouncedX && Math.abs(lVx) < STOP_BOUNCE_THRESHOLD) {
        	lVx = 0;
        	bouncedX = false;
        }
        

        // Update fields
        synchronized (LOCK) {
        	ballPosX = lBallX;
        	ballPosY = lBallY;
            velocityX = lVx;
            velocityY = lVy;
        }
        
        //Vibrate if bouncing
        if (bouncedX || bouncedY) {
        	if(vibrator !=null)	vibrator.vibrate(20L);
        }
    }

	public void checkBallHit(float x, float y) {
		int w = (int) ballRadius;
		int h = (int) ballRadius;
		float rx = ballPosX;
		float ry = ballPosY;
		
		if (x < rx - w || x > rx + w || y < ry - w || y > ry + h) {
			return;
		}
		
		float lVx, lVy;
		lVx =   0;
		lVy = HIT_BOUNCE;
		
		synchronized (LOCK) {
			velocityX = lVx;
			velocityY = lVy;
		}
	}
}
