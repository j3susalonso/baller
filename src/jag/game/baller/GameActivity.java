package jag.game.baller;

import java.util.concurrent.TimeUnit;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.SurfaceHolder.Callback;
import android.view.View.OnTouchListener;

public class GameActivity extends Activity implements Callback, OnTouchListener {
	
	private SurfaceView surface;
	private SurfaceHolder holder;
	private Paint backgroundPaint;
	private SensorManager sensorManager;
	private Sensor accelerometer;
	private long lastAccelUpdate = 0;
	private Bitmap bitmap;
	private int angle;
	private GameLoop gameLoop;
	private GameField field;
	private final static long DRAW_DELAY   = 10; //Milliseconds delay for redrawing each canvas.
	private final static long UPDATE_DELAY = 50; //Milliseconds delay for updating the acceleration.
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	setContentView(R.layout.ball);
    	//Surface
    	surface = (SurfaceView) findViewById(R.id.ball_surface);
    	surface.setClickable(true);
        surface.setOnTouchListener(this);
        //Surface holder
    	holder = surface.getHolder();
    	holder.addCallback(this);
    	//Background
        backgroundPaint = new Paint();
		backgroundPaint.setColor(Color.rgb(200, 242, 255));
		//Bitmap simulating the ball
		bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ball);
		//Accelometer
		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
	    accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
	    //Game field
		field = new GameField(bitmap.getWidth()/2);
		//Game loop
		gameLoop = new GameLoop();
    }

	protected void onPause() {
	    super.onPause();
	    //Unregister listener
	    sensorManager.unregisterListener(sensorEventListener);
	    //Disable vibrator
	    field.setVibrator(null);
	    //Reset acceleration
	    field.setAccel(0, 0);
	}
	
	protected void onResume() {
		super.onResume();
		//Register a listener for the orientation and accelerometer sensors
		sensorManager.registerListener(sensorEventListener, accelerometer, SensorManager.SENSOR_DELAY_GAME);
		//Get instance of Vibrator from current Context
		Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		field.setVibrator(vibrator);
	}
	
	protected void onDestroy(){
        super.onDestroy();
    }

	public void surfaceChanged(SurfaceHolder holder, int format, int width,int height) {
		field.setSize(width, height);
	}

	public void surfaceCreated(SurfaceHolder holder) {
		gameLoop.start();
	}
	
	private void draw() {
		Canvas c = null;
		try {
			//Get surface area
			c = holder.lockCanvas(); 
			if (c != null) {
				//Background
				c.drawRect(0, 0, c.getWidth(), c.getHeight(), backgroundPaint);
				//Get ball position
				float ballPosX, ballPosY;
				synchronized (field.LOCK) { 
					ballPosX = field.getBallPosX();
					ballPosY = field.getBallPosY();
				}
				//Rotate ball
				if (angle++ > 360) angle = 0;
				c.rotate(angle, ballPosX, ballPosY);
				//Draw ball
				float left = ballPosX - bitmap.getWidth()/2;
				float top =  ballPosY - bitmap.getHeight()/2;
				c.drawBitmap(bitmap, left, top, null);
			}
		} finally {
			if (c != null) {
				//Release surface area
				holder.unlockCanvasAndPost(c);
			}
		}
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		try {
			field.setSize(0,0);
			gameLoop.gameStop();
		} finally {
			gameLoop = null;
		}
	}
	
	public boolean onTouch(View v, MotionEvent event) {
		float x = event.getX();
		float y = event.getY();
		field.checkBallHit(x, y);
		return true;
	}

	public void onClick(View v) {
		int[] l = new int[2];
		v.getLocationOnScreen(l);
		int x = l[0];
		int y = l[1];
		field.checkBallHit(x, y);
	}
	
	private class GameLoop extends Thread {
		private volatile boolean running = true;
		
		@Override
		public void run() {
			while (running) {
				try {
					TimeUnit.MILLISECONDS.sleep(DRAW_DELAY);
					draw();
					field.updateBallPosition();
				} catch (InterruptedException ie) {
					running = false;
				}
			}
		}
		
		public void gameStop() {
			running = false;
			interrupt();
		}
	}
	
	private SensorEventListener sensorEventListener = new SensorEventListener() {

		public void onAccuracyChanged(Sensor sensor, int accuracy) {
		}

	    public void onSensorChanged(SensorEvent event) {
	        float x = event.values[0];
	        float y = event.values[1];
			long currentTime = System.currentTimeMillis();
			if (currentTime - lastAccelUpdate > UPDATE_DELAY) {
				lastAccelUpdate = currentTime;
				field.setAccel(x, y);
			}
	    }
	};

}
