package com.jrepp;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.Random;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class SodokuSolver {
  public static void main(String[] args) {
    Board board = new Board();
    board.print();
    int count = 6;
    while (count > 0) {
      board.randomFill();
    }
    board.print();
  }

  public static Board emptyBoard() {
    return new Board();
  }

  static class Board {
    static final int ROWS = 9;
    static final int COLS = 9;
    static final int BLOCK_SIZE = 9;
    static final int BLOCK_STRIDE = 3;
    static Iterator<Long> randoms = new Random().ints().asLongStream().iterator();

    public static Integer[] ALL_VALUES = new Integer[] {1, 2, 3, 4, 5, 6, 7, 8, 9};

    // The total number of values in a set is ALL_VALUES
    // plus the sentinel value of 0
    public static int TOTAL_CARDINALITY = ALL_VALUES.length + 1;

    long[] cells;
    ArrayList<BitSet> colRules;
    ArrayList<BitSet> rowRules;
    ArrayList<BitSet> blockRules;
    BitSet population;

    Board() {
      assert (Long.bitCount(Integer.MAX_VALUE) >= COLS * Integer.bitCount(COLS));
      cells = new long[Board.ROWS];
      colRules = new ArrayList<>(Board.COLS);
      rowRules = new ArrayList<>(Board.ROWS);
      blockRules = new ArrayList<>((Board.ROWS * Board.COLS) / Board.BLOCK_SIZE);
      population = new BitSet(Board.ROWS * Board.COLS);
      calculateRules();
    }

    // Write solver here
    public boolean solve(boolean trace) {
      boolean canSolve = true;
      CellPos pos = new CellPos(0, 0, false, 0);

      // Find the next valid position to solve
      findSolvePosition(pos);
      if (!pos.valid) {
        System.out.println("[solve] no valid positions");
        return false;
      }

      // Try all valid remaining moves
      final BitSet inUse = allInUseAt(pos.col, pos.row);
      return tryValid(pos.col, pos.row, inUse, trace);
    }

    private void findSolvePosition(CellPos pos) {
      pos.set(0, 0, false, 0);

      int bestCardinality = 0;
      for (int row = 0; row < Board.ROWS; ++row) {
        for (int col = 0; col < Board.COLS; ++col) {
          if (get(col, row) != 0) {
            continue;
          }

          BitSet inUse = allInUseAt(col, row);

          // Find the cell with the highest number of known values,
          // that still has a 0 (empty cell)
          final int cardinality = inUse.cardinality();
          if (cardinality > bestCardinality && cardinality < TOTAL_CARDINALITY && inUse.get(0)) {
            bestCardinality = cardinality;
            pos.set(col, row, true, cardinality);
            if (cardinality == TOTAL_CARDINALITY - 1) {
              // Only one choice, use it
              return;
            }
          }
        }
      }
    }

    private boolean tryValid(int col, int row, BitSet inUse, boolean trace) {
      int nextChoice = inUse.nextClearBit(1);
      while (nextChoice < TOTAL_CARDINALITY) {
        if (trace) {
          System.out.println(
              "[solve] <" + col + ", " + row + "> trying " + nextChoice + " of " + inUse);
        }
        boolean result = fillCell(col, row, nextChoice);
        assert (result);
        print();

        // Test if the board is solved
        if (population.cardinality() == Board.COLS * Board.ROWS) {
          if (trace) {
            System.out.println("[solve] Puzzle solved!");
          }
          return true;
        }

        // Try solving the rest of board with this choice
        boolean trySolve = solve(trace);
        if (!trySolve) {
          if (trace) {
            System.out.println(
                "[solve] <" + col + ", " + row + "> (try) failed with " + nextChoice);
          }
          clearCell(col, row);
          nextChoice = inUse.nextClearBit(nextChoice + 1);
        } else {
          // A valid solution was found, move on to other cells
          return true;
        }
      }

      return false;
    }

    public BitSet allInUseAt(int col, int row) {
      BitSet inUse = new BitSet();
      inUse.or(rowRules.get(row));
      inUse.or(colRules.get(col));
      inUse.or(blockRules.get(blockIndex(col, row)));
      return inUse;
    }

    public int get(int col, int row) {
      final long rowValue = cells[row];
      return (int) (rowValue >> (col * 4) & 0xfL);
    }

    private void set(int col, int row, int b) {
      final long rowValue = cells[row];
      final long setValue = ((long) b << (col * 4));
      final long maskValue = (0xfL << (col * 4));
      assert ((rowValue & maskValue) == 0);
      cells[row] = rowValue | setValue;
    }

    private void clear(int col, int row) {
      final long rowValue = cells[row];
      final long maskValue = (0xfL << (col * 4));
      cells[row] = rowValue & ~maskValue;
    }

    void calculateRules() {
      colRules.clear();
      rowRules.clear();
      blockRules.clear();

      IntStream.range(0, Board.COLS).forEach((x) -> colRules.add(colSet(x)));
      IntStream.range(0, Board.ROWS).forEach((y) -> rowRules.add(rowSet(y)));
      IntStream.range(0, Board.BLOCK_SIZE).forEach((i) -> blockRules.add(blockSet(i)));
    }

    void eachInBlock(int lx, int ly, Consumer<Integer> consumer) {
      int rx = lx + BLOCK_STRIDE;
      int ry = ly + BLOCK_STRIDE;
      assert (rx <= Board.COLS);
      assert (ry <= Board.ROWS);
      for (int ix = lx; ix < rx; ix++) {
        for (int iy = ly; iy < ry; iy++) {
          consumer.accept(get(ix, iy));
        }
      }
    }

    BitSet blockSet(int index) {
      int x = index % BLOCK_STRIDE;
      int y = index / BLOCK_STRIDE;
      BitSet block = Board.ruleSet();
      eachInBlock(x * BLOCK_STRIDE, y * BLOCK_STRIDE, block::set);
      return block;
    }

    Stream<Integer> rowStream(int row) {
      return IntStream.range(0, Board.COLS).mapToObj((col) -> get(col, row));
    }

    Stream<Integer> colStream(int col) {
      return IntStream.range(0, Board.ROWS).mapToObj((row) -> get(col, row));
    }

    public static BitSet ruleSet() {
      return new BitSet(TOTAL_CARDINALITY);
    }

    BitSet rowSet(int row) {
      BitSet rowSet = Board.ruleSet();
      rowStream(row).forEach(rowSet::set);
      return rowSet;
    }

    BitSet colSet(int column) {
      BitSet colSet = Board.ruleSet();
      colStream(column).forEach(colSet::set);
      return colSet;
    }

    void printRow() {
      final char c = '-';
      IntStream.range(0, 4 + (Board.COLS * 4)).forEach((i) -> System.out.print(c));
      System.out.println();
    }

    void print() {
      System.out.println("   | 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 |");
      printRow();
      for (int y = 0; y < Board.ROWS; ++y) {
        System.out.print(' ');
        System.out.print(y);
        System.out.print(" |");
        for (int x = 0; x < Board.COLS; ++x) {
          final int b = get(x, y);
          System.out.print(' ');
          if (b == 0) {
            System.out.print(' ');
          } else {
            System.out.print(b);
          }
          System.out.print(' ');
          if (x > 0 && x < Board.COLS - 1 && (x + 1) % 3 == 0) {
            System.out.print('!');
          } else {
            System.out.print('|');
          }
        }
        System.out.println();
        if (y > 0 && (y + 1) % 3 == 0) {
          printRow();
        }
      }
    }

    void parseCSV(String board) throws Exception {
      String[] cells = board.split("\\s*,\\s*");
      if (cells.length != 81) {
        throw new Exception("Invalid cell count " + cells.length);
      }

      int index = 0;
      for (String cell : cells) {
        final int b = Integer.parseInt(cell);
        final int x = index % 9;
        final int y = index / 9;
        index++;
        if (b == 0) {
          continue;
        }

        if (!fillCell(x, y, b)) {
          throw new Exception(
              "Invalid cell value " + cell + " at " + x + ", " + y + " (index: " + index + ")");
        }
      }
    }

    boolean randomFill() {
      int b = Board.ALL_VALUES[Math.abs(randoms.next().intValue()) % Board.ALL_VALUES.length];
      int x = Math.abs(randoms.next().intValue()) % Board.COLS;
      int y = Math.abs(randoms.next().intValue()) % Board.ROWS;
      return fillCell(x, y, b);
    }

    private void setValue(BitSet set, int b) {
      set.set(b);
      if (set.size() == 10) {
        set.clear(0);
      }
    }

    boolean fillCell(int x, int y, int b) {
      if (canFill(x, y, b)) {
        set(x, y, b);
        setValue(rowRules.get(y), b);
        setValue(colRules.get(x), b);
        setValue(blockRules.get(blockIndex(x, y)), b);

        population.set((y * Board.COLS) + x);
        return true;
      }
      return false;
    }

    void clearCell(int x, int y) {
      final int b = get(x, y);
      if (b == 0) {
        return;
      }
      clear(x, y);
      rowRules.get(y).clear(b);
      colRules.get(x).clear(b);
      blockRules.get(blockIndex(x, y)).clear(b);
      population.clear((y * Board.COLS) + x);
    }

    boolean canFill(int x, int y, int b) {
      if (population.get((y * Board.COLS) + x)) {
        return false;
      }
      if (rowRules.get(y).get(b)) {
        return false;
      }
      if (colRules.get(x).get(b)) {
        return false;
      }
      int i = blockIndex(x, y);
      if (blockRules.get(i).get(b)) {
        return false;
      }
      return true;
    }

    BitSet rowPopulation(int row) {
      return population.get(row * 9, (row * 9) + 9);
    }

    int blockIndex(int x, int y) {
      int block_x = x / BLOCK_STRIDE;
      int block_y = y / BLOCK_STRIDE;
      return (block_y * BLOCK_STRIDE) + block_x;
    }
  }

  private static class CellPos {
    int col;
    int row;
    boolean valid;
    int cardinality;

    CellPos(int c, int r, boolean v, int arity) {
      col = c;
      row = r;
      valid = v;
      cardinality = arity;
    }

    void set(int c, int r, boolean v, int arity) {
      col = c;
      row = r;
      valid = v;
      cardinality = arity;
    }

    @Override
    public String toString() {
      return "<" + col + "," + row + ">";
    }
  }
}
