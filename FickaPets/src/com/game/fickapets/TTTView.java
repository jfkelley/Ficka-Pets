package com.game.fickapets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import com.game.fickapets.TicTacToe.TTTState;

public class TTTView extends View {

	private static final int MARGIN = 20;
	private static final int LINE_WIDTH = 10;

	private float squareSize;

	private TTTState state;

	public TTTView(Context context, AttributeSet attrs, int defStyle, TTTState state) {
		super(context, attrs, defStyle);
		this.state = state;
		setBackgroundColor(Color.BLACK);
	}
	
	public TTTView(Context context, AttributeSet attrs, int defStyle) {
		this (context, attrs, defStyle, null);
	}
	
	public TTTView(Context context, AttributeSet attrs) {
		this(context, attrs, 0, null);
	}
	
	public TTTView(Context context) {
		this(context, null, 0, null);
	}
	
	public void setState(TTTState state) {
		this.state = state;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		if (state == null) return;
		Paint paint = new Paint();
		paint.setColor(Color.rgb(240, 240, 255));
		squareSize = (float)((getWidth() - 2 * LINE_WIDTH - 2 * MARGIN) / 3.0);
		int x1 = (int)Math.round(MARGIN + squareSize);
		int x2 = (int)Math.round(MARGIN + squareSize * 2 + LINE_WIDTH);
		int y1 = (int)Math.round(MARGIN + squareSize);
		int y2 = (int)Math.round(MARGIN + squareSize * 2 + LINE_WIDTH);
		canvas.drawRect(x1, MARGIN, x1 + LINE_WIDTH, getHeight() - MARGIN, paint);
		canvas.drawRect(x2, MARGIN, x2 + LINE_WIDTH, getHeight() - MARGIN, paint);
		canvas.drawRect(MARGIN, y1, getWidth() - MARGIN, y1 + LINE_WIDTH, paint);
		canvas.drawRect(MARGIN, y2, getWidth() - MARGIN, y2 + LINE_WIDTH, paint);

		for (int r = 0; r < state.board.length; r++) {
			for (int c = 0; c < state.board[r].length; c++) {
				if (state.board[r][c] == 1) {
					drawX(MARGIN * 2 + r * (squareSize + LINE_WIDTH), MARGIN * 2 + c * (squareSize + LINE_WIDTH), squareSize - MARGIN*2, squareSize - MARGIN*2, canvas);
				} else if (state.board[r][c] == -1) {
					drawO(MARGIN * 2 + r * (squareSize + LINE_WIDTH), MARGIN * 2 + c * (squareSize + LINE_WIDTH), squareSize - MARGIN*2, squareSize - MARGIN*2, canvas);
				}
			}
		}
	}

	public int index(float x) {
		if (x >= MARGIN && x <= MARGIN + squareSize) return 0;
		if (x >= MARGIN + squareSize + LINE_WIDTH && x <= MARGIN + squareSize * 2 + LINE_WIDTH) return 1;
		if (x >= MARGIN + squareSize * 2 + LINE_WIDTH * 2 && x <= MARGIN + squareSize * 3 + LINE_WIDTH * 2) return 2;
		return -1;
	}

	private void drawX(float x, float y, float w, float h, Canvas canvas) {
		Paint paint = new Paint();
		paint.setColor(Color.rgb(255, 200, 200));
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeWidth(LINE_WIDTH);
		canvas.drawLine(x, y, x + w, y + h, paint);
		canvas.drawLine(x + w, y, x, y + h, paint);
	}

	private void drawO(float x, float y, float w, float h, Canvas canvas) {
		Paint paint = new Paint();
		paint.setColor(Color.rgb(200, 255, 200));
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeWidth(LINE_WIDTH);
		canvas.drawOval(new RectF(x, y, x + w, y + h), paint);
	}

	@Override
	protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec) {
		int w = MeasureSpec.getSize(widthMeasureSpec);
		int h = MeasureSpec.getSize(heightMeasureSpec);
		setMeasuredDimension(Math.min(w, h), Math.min(w, h));
	}

}