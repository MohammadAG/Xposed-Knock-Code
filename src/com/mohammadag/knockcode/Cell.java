package com.mohammadag.knockcode;

public class Cell {
	private int mRow;
	private int mColumn;

	Cell(int row, int column) {
		mRow = row;
		mColumn = column;
	}

	public int getRow() {
		return mRow;
	}

	public int getColumn() {
		return mColumn;
	}
}