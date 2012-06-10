package com.game.fickapets;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.math3.distribution.ExponentialDistribution;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;

public class CatchCoins extends Activity implements SensorEventListener {
	
	public static final String NAME = "Coin Catch";
	public static final String INSTRUCTIONS = "Tilt your phone to move your pet side to side. Catch falling coins, and avoid falling rocks! If three rocks hit you, the game is over.";

	private GameState state;
	private GameThread thread;
	private SensorManager sensorManager;
	private Sensor accelerometer;
	private volatile int tilt = 0;
	private static final double X_CUTOFF = 0.1;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		state = new GameState();
		setContentView(state.view);
		sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
		accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		thread = new GameThread();
	}

	private class GameState {
		public static final double PET_SPEED = 0.2;
		public double FALL_SPEED = 0.15;

		private ExponentialDistribution nextCoinDist;
		private ExponentialDistribution nextBombDist;
		private long nextCoin = -1;
		private long nextBomb = -1;
		private double coinsPerSecond = 0.3;
		private double bombsPerSecond = 0.15;

		private List<FallingObject> fallers;
		private int petX = Integer.MIN_VALUE;

		private static final long DIFFICULTY_INCREASE_TIME = 8000;
		private long t = 0;

		private volatile boolean gameIsRunning = true;
		private volatile boolean canCollide = true;
		private int lives = 3;

		private CatchCoinsView view;

		public GameState() {
			view = new CatchCoinsView(CatchCoins.this);
			view.setBackgroundColor(Color.rgb(240, 240, 255));
			fallers = new LinkedList<FallingObject>();
			updateDistributions();
		}

		private class FallingObject {
			public static final int OBJECT_WIDTH = 50;

			private int x, y;
			public final double speed;
			public boolean isCoin;
			public FallingObject(int x, int y, boolean isCoin, double speed) {
				this.x = x; this.y = y; this.isCoin = isCoin; this.speed = speed;
			}

			public FallingObject(boolean isCoin) {
				this(	(int)(Math.random() * (view.getWidth() - OBJECT_WIDTH)),
						-OBJECT_WIDTH,
						isCoin,
						FALL_SPEED - 0.05 + Math.random() * 0.1
						);
			}
		}

		public synchronized void step(long diff) {
			t += diff;
			if (t > DIFFICULTY_INCREASE_TIME) {
				increaseDifficulty();
				t -= DIFFICULTY_INCREASE_TIME;
			}
			updatePet(diff);
			updateFallers(diff);
			addNewFallers(diff);
			if (canCollide) {
				checkForCollisions();
			}
			removeOldFallers();
			view.invalidate();
		}

		private void increaseDifficulty() {
			FALL_SPEED += 0.01;
			coinsPerSecond += 0.05;
			bombsPerSecond += 0.08;
			updateDistributions();
		}

		private void updateDistributions() {
			nextCoinDist = new ExponentialDistribution(1000 / coinsPerSecond);
			nextBombDist = new ExponentialDistribution(1000 / bombsPerSecond);
		}

		private void updatePet(long diff) {
			petX += tilt * diff * PET_SPEED;
			if (petX < 0) petX = 0;
			if (petX + view.pet.getWidth() > view.getWidth()) petX = view.getWidth() - view.pet.getWidth();
		}

		private void updateFallers(long diff) {
			for (FallingObject faller : fallers) {
				faller.y += diff * faller.speed;
			}
		}

		private void addNewFallers(long diff) {
			if (nextCoin == -1) {
				nextCoin = System.currentTimeMillis() + (long)nextCoinDist.sample();
			}
			if (nextBomb == -1) {
				nextBomb = System.currentTimeMillis() + (long)nextBombDist.sample();
			}

			while (System.currentTimeMillis() > nextCoin) {
				fallers.add(new FallingObject(true));
				nextCoin += (long)nextCoinDist.sample();
			}

			while (System.currentTimeMillis() > nextBomb) {
				fallers.add(new FallingObject(false));
				nextBomb += (long)nextBombDist.sample();
			}
		}

		private void checkForCollisions() {
			Iterator<FallingObject> iter = fallers.iterator();
			while (iter.hasNext()) {
				FallingObject faller = iter.next();
				if (faller.y + FallingObject.OBJECT_WIDTH > view.getHeight() - view.pet.getHeight() &&
					faller.x > petX - FallingObject.OBJECT_WIDTH &&
					faller.x < petX + view.pet.getWidth()) {
					iter.remove();
					if (faller.isCoin) {
						User.theUser(CatchCoins.this).addCoin();
					} else {
						canCollide = false;
						lives--;
						if (lives == 0) {
							gameIsRunning = false;
						} else {
							new AsyncTask<Object, Integer, Object>(){
								private static final long QUARTER_SECOND = 500;
								
								protected Object doInBackground(Object... params) {
									for (int i = 0; i < 4; i++) {
										publishProgress(0);
										try {
											Thread.sleep(QUARTER_SECOND);
										} catch (InterruptedException e) {
											e.printStackTrace();
											return null;
										}
										publishProgress(1);
										try {
											Thread.sleep(QUARTER_SECOND);
										} catch (InterruptedException e) {
											e.printStackTrace();
											return null;
										}
									}
									return null;
								}
								
								protected void onProgressUpdate(Integer... progress) { view.petVisible = progress[0] == 1; }

								protected void onPostExecute(Object result) { canCollide = true; }
								
							}.execute();
						}
					}
					return;
				}
			}
		}

		private void removeOldFallers() {
			// using an iterator is faster because it's a linked list
			Iterator<FallingObject> iter = fallers.iterator();
			while (iter.hasNext()) {
				FallingObject faller = iter.next();
				if (faller.y > view.getHeight()) {
					iter.remove();
				}
			}
		}

		private class CatchCoinsView extends View {
			private volatile boolean petVisible = true;
			private final Bitmap pet;
			private final Bitmap bomb;
			private final Bitmap coin;
			private static final int PET_WIDTH = 100;

			public CatchCoinsView(Context context) {
				super(context);
				Bitmap unscaledPet = BitmapFactory.decodeResource(getResources(), Pet.thePet(CatchCoins.this).getSmallImage(CatchCoins.this));
				pet = Bitmap.createScaledBitmap(unscaledPet, PET_WIDTH, unscaledPet.getHeight() * PET_WIDTH / unscaledPet.getWidth(), false);
				bomb = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.bomb), FallingObject.OBJECT_WIDTH, FallingObject.OBJECT_WIDTH, false);
				coin = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.coin), FallingObject.OBJECT_WIDTH, FallingObject.OBJECT_WIDTH, false);
			}

			@Override
			protected void onDraw(Canvas canvas) {
				Paint paint = new Paint();
				if (petVisible) {
					if (petX == Integer.MIN_VALUE) petX = (getWidth() - pet.getWidth()) / 2;
					int petY = getHeight() - pet.getHeight();
					canvas.drawBitmap(pet, petX, petY, paint);
				}
				
				for (FallingObject faller : state.fallers) {
					Bitmap bmp = faller.isCoin ? coin : bomb;
					canvas.drawBitmap(bmp, faller.x, faller.y, paint);
				}
				
				paint.setTextAlign(Align.LEFT);
				paint.setColor(Color.rgb(180, 0, 0));
				paint.setTextSize(30.0f);
				canvas.drawText("Coins: " + User.theUser(CatchCoins.this).getCoins(), 5, 35, paint);
			}

			@Override
			protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec) {
				setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.getSize(heightMeasureSpec));
			}
		}
	}

	private class GameThread extends AsyncTask<Object, Object, Object> {
		private static final int PAUSE_TIME = 20;
		private volatile long lastUpdate;

		@Override
		protected Object doInBackground(Object... params) {
			lastUpdate = System.currentTimeMillis();
			while (state.gameIsRunning) {
				publishProgress((Object)null);
				try {
					Thread.sleep(PAUSE_TIME);
				} catch (InterruptedException e) {
					e.printStackTrace();
					return null;
				}
			}
			return null;
		}

		protected void onProgressUpdate(Object... progress) {
			long now = System.currentTimeMillis();
			long diff = now - lastUpdate;
			state.step(diff);
			lastUpdate = now;
		}

		protected void onPostExecute(Object result) {
		}
	}

	protected void onResume() {
		super.onResume();
		sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
		thread.execute((Object)null);
	}

	protected void onPause() {
		super.onPause();
		sensorManager.unregisterListener(this);
		thread.cancel(true);
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) { }

	@Override
	public void onSensorChanged(SensorEvent event) {
		double totalAcceleration = magnitude(event.values[0], event.values[1], event.values[2]);
		double xAcceleration = event.values[0];
		if (Math.abs(xAcceleration) / totalAcceleration < X_CUTOFF) {
			tilt = 0;
		} else {
			tilt = xAcceleration > 0 ? -1 : 1;
		}
	}

	private double magnitude(float... fs) {
		double total = 0.0;
		for (float f : fs) { total += f*f; }
		return Math.sqrt(total);
	}

}
