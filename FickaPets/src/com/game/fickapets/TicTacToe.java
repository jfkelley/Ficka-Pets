package com.game.fickapets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;

public class TicTacToe extends Minigame {

	@Override
	public String getName() {
		return getResources().getString(R.string.ticTacToe);
	}

	@Override
	public String getInstructions() {
		return "";
	}

	private static final int CHOOSE_DIFFICULTY = 0;

	private int difficulty = 0;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		final TTTState state = new TTTState();
		final TTTView view = new TTTView(this, state);
		view.setOnTouchListener(new OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
					if (state.isOver()) {
						state.reset();
						view.invalidate();
						return true;
					}
					if (state.currentPlayer == 1) {
						float x = event.getX();
						float y = event.getY();
						if (view.index(x) == -1 || view.index(y) == -1) return false;
	
						if (state.playMove(view.index(x), view.index(y), state.currentPlayer)) {
							view.invalidate();
							if (!state.isOver()) {
								state.makeMove(getStrength());
								view.invalidate();
							}
							return true;
						} else {
							return false;
						}
					}
				}
				return false;
			}
		});

		setContentView(view);

		showDialog(CHOOSE_DIFFICULTY);
	}

	private double getStrength() {
		switch(difficulty) {
		case 0: return 0.5;
		case 1: return 0.6;
		case 2: return 0.8;
		case 3: return 0.95;
		default: return 1.0;
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		if (id == CHOOSE_DIFFICULTY) {
			final String[] choices = {"Easy", "Medium", "Hard", "Impossible"};
			AlertDialog alert = new AlertDialog.Builder(this)
			.setTitle("Choose a difficulty")
			.setItems(choices, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int item) {
					difficulty = item;
				}
			})
			.create();
			return alert;
		} else {
			return null;
		}
	}

	private static class TTTView extends View {

		private static final int MARGIN = 20;
		private static final int LINE_WIDTH = 10;

		private float squareSize;

		private final TTTState state;

		public TTTView(Context context, TTTState state) {
			super(context);
			this.state = state;
			setBackgroundColor(Color.BLACK);
		}

		@Override
		protected void onDraw(Canvas canvas) {
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

	private static class TTTState {
		private int[][] board;
		private int currentPlayer;

		public TTTState() {
			board = new int[3][3];
			currentPlayer = 1;
		}

		public int toInt() {
			int sum = 0;
			int n = 0;
			for (int r = 0; r < board.length; r++) {
				for (int c = 0; c < board[r].length; c++) {
					int val = board[r][c] + 1;
					sum += val * Math.round(Math.pow(3, n));
					n++;
				}
			}
			return sum;
		}

		public String toString() {
			String result = "";
			for (int r = 0; r < board.length; r++) {
				for (int c = 0; c < board[r].length; c++) {
					int val = board[r][c];
					if (val == 0) result += "-";
					else if (val == 1) result += "X";
					else result += "O";
				}
				if (r != board.length - 1) result += "\n";
			}
			return result;
		}

		public boolean playMove(int r, int c, int player) {
			if (board[r][c] == 0) {
				board[r][c] = player;
				currentPlayer = -currentPlayer;
				return true;
			} else {
				return false;
			}
		}

		private boolean undoMove(int r, int c) {
			if (board[r][c] != 0) {
				board[r][c] = 0;
				currentPlayer = -currentPlayer;
				return true;
			} else {
				return false;
			}
		}

		public boolean isOver() {
			if (getWinner() != 0) return true;
			for (int r = 0; r < board.length; r++) {
				for (int c = 0; c < board[r].length; c++) {
					if (board[r][c] == 0) return false;
				}
			}
			return true;
		}

		public int getWinner() {
			for (int n = -1; n <= 1; n += 2) {
				if (allEqual(n, board[0][0], board[0][1], board[0][2])) return n;
				if (allEqual(n, board[1][0], board[1][1], board[1][2])) return n;
				if (allEqual(n, board[2][0], board[2][1], board[2][2])) return n;
				if (allEqual(n, board[0][0], board[1][0], board[2][0])) return n;
				if (allEqual(n, board[0][1], board[1][1], board[2][1])) return n;
				if (allEqual(n, board[0][2], board[1][2], board[2][2])) return n;
				if (allEqual(n, board[0][0], board[1][1], board[2][2])) return n;
				if (allEqual(n, board[0][2], board[1][1], board[2][0])) return n;
			}
			return 0;
		}

		private static boolean allEqual(int target, int... vals) {
			for (int val : vals) {
				if (val != target) return false;
			}
			return true;
		}

		private Map<Integer, Double> cache = new HashMap<Integer, Double>();

		private double evaluateBoard() {
			int n = toInt();
			if (cache.containsKey(n)) return cache.get(n);
			if (getWinner() != 0) {
				cache.put(n, (double)getWinner());
				return getWinner();
			}
			if (isOver()) {
				cache.put(n, 0.0);
				return 0;
			}
			double max = -2;
			double total = 0;
			int nMoves = 0;
			for (int r = 0; r < board.length; r++) {
				for (int c = 0; c < board[r].length; c++) {
					if (board[r][c] == 0) {
						nMoves++;
						playMove(r, c, currentPlayer);
						double result = evaluateBoard();
						undoMove(r, c);
						total += result * currentPlayer;
						if (result * currentPlayer > max) {
							max = result * currentPlayer;
						}
					}
				}
			}
			double finalResult = (0.95 * max + 0.05 * total / nMoves) * currentPlayer;
			cache.put(n, finalResult);
			return finalResult;
		}

		public void makeMove(double strength) {
			if (Math.random() <= strength) {
				makeBestMove();
			} else {
				makeRandomMove();
			}
		}

		private void makeBestMove() {
			double max = -2;
			int bestR = -1;
			int bestC = -1;
			for (int r = 0; r < board.length; r++) {
				for (int c = 0; c < board[r].length; c++) {
					if (board[r][c] == 0) {
						playMove(r, c, currentPlayer);
						double result = evaluateBoard();
						undoMove(r, c);
						if (result * currentPlayer > max) {
							max = result * currentPlayer;
							bestR = r;
							bestC = c;
						}
					}
				}
			}
			playMove(bestR, bestC, currentPlayer);
		}

		private void makeRandomMove() {
			List<Integer> possibleR = new ArrayList<Integer>();
			List<Integer> possibleC = new ArrayList<Integer>();
			for (int r = 0; r < board.length; r++) {
				for (int c = 0; c < board[r].length; c++) {
					if (board[r][c] == 0) {
						possibleR.add(r);
						possibleC.add(c);
					}
				}
			}
			int i = (int)(Math.random() * possibleR.size());
			playMove(possibleR.get(i), possibleC.get(i), currentPlayer);
		}

		public void reset() {
			currentPlayer = 1;
			for (int r = 0; r < board.length; r++) {
				for (int c = 0; c < board[r].length; c++) {
					board[r][c] = 0;
				}
			}
		}
	}

}
