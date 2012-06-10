package com.game.fickapets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.TextView;

public class TicTacToe extends Activity {
	
	public static final String NAME = "Tic-Tac-Toe";
	public static final String INSTRUCTIONS = "Take turns with the computer to place symbols on the grid. First to connect three in a row wins! Play higher difficulty for more coins.";

	private static final int CHOOSE_DIFFICULTY = 0;

	private int difficulty = 0;
	
	private TTTState state;
	private TTTView view;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.ttt);
		
		updateCoinText();
		
		state = new TTTState();
		view = (TTTView)findViewById(R.id.TTTView);
		view.setState(state);
		view.setOnTouchListener(new OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
					if (state.isOver()) {
						resetGame(null);
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
							} else {
								if (state.getWinner() == 1) {
									User.theUser(TicTacToe.this).addCoins(getCoinReward());
									updateCoinText();
								}
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

		showDialog(CHOOSE_DIFFICULTY);
	}
	
	public void resetGame(View ignored) {
		state.reset();
		view.invalidate();
	}
	
	private void updateCoinText() {
		TextView text = (TextView)findViewById(R.id.TTTCoinText);
		text.setText("Coins: " + User.theUser(this).getCoins());
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
	
	private int getCoinReward() {
		switch(difficulty) {
		case 0: return 2;
		case 1: return 5;
		case 2: return 10;
		case 3: return 100;
		default: return 0;
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

	public static class TTTState {
		public int[][] board;
		public int currentPlayer;

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
