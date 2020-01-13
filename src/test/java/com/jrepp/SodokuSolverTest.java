package com.jrepp;

import static com.google.common.truth.Truth.assertThat;

import java.util.Arrays;
import java.util.BitSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SodokuSolverTest {
  SodokuSolver.Board board;

  // https://www.websudoku.com/?level=1&set_id=8443424605
  static String EASY_1 =
      "0, 1, 6,  0, 0, 2,  0, 0, 0,"
          + "0, 0, 0,  8, 0, 0,  0, 1, 0,"
          + "2, 0, 0,  0, 3, 0,  5, 0, 9,"
          + "6, 2, 8,  3, 0, 0,  4, 0, 0,"
          + "7, 4, 5,  1, 0, 8,  9, 6, 3,"
          + "0, 0, 9,  0, 0, 6,  7, 8, 2,"
          + "8, 0, 3,  0, 1, 0,  0, 0, 5,"
          + "0, 6, 0,  0, 0, 7,  0, 0, 0,"
          + "0, 0, 0,  9, 0, 0,  8, 2, 0";

  // https://www.websudoku.com/?level=2&set_id=1938763555
  static String MEDIUM_1 =
      "0, 0, 8,  5, 0, 0,  0, 0, 6,"
          + "0, 0, 1,  0, 7, 6,  0, 0, 0,"
          + "0, 5, 9,  1, 4, 0,  0, 7, 0,"
          + "0, 0, 4,  0, 0, 2,  0, 0, 0,"
          + "5, 0, 0,  4, 0, 7,  0, 0, 2,"
          + "0, 0, 0,  9, 0, 0,  4, 0, 0,"
          + "0, 1, 0,  0, 9, 4,  5, 6, 0,"
          + "0, 0, 0,  6, 8, 0,  3, 0, 0,"
          + "7, 0, 0,  0, 0, 5,  9, 0, 0,";

  // https://www.websudoku.com/?level=4&set_id=9372712401
  static String EVIL_1 =
      "1, 0, 0,  6, 0, 0,  9, 5, 0,"
          + "0, 0, 0,  5, 0, 7,  0, 0, 0,"
          + "0, 0, 0,  0, 3, 0,  0, 6, 0,"
          + "0, 2, 0,  0, 0, 3,  0, 0, 8,"
          + "0, 3, 9,  0, 0, 0,  6, 4, 0,"
          + "4, 0, 0,  9, 0, 0,  0, 1, 0,"
          + "0, 9, 0,  0, 7, 0,  0, 0, 0,"
          + "0, 0, 0,  2, 0, 9,  0, 0, 0,"
          + "0, 7, 8,  0, 0, 5,  0, 0, 9,";

  @BeforeEach
  void setup() {
    board = SodokuSolver.emptyBoard();
  }

  @Test
  void testLongBits() {
    long a = Long.MAX_VALUE;
    assertThat(Long.bitCount(a)).isEqualTo(63);

    int v = 9;
    int col = 8;
    long rowValue = 0x555555L;
    long setValue = ((long) v << (col * 4));
    long maskValue = (0xfL << (col * 4));
    assertThat((rowValue & maskValue)).isEqualTo(0);

    long newValue = setValue | rowValue;
    assertThat(newValue >> (col * 4) & 0xf).isEqualTo(v);
  }

  @Test
  void testPrint() {
    board.print();
  }

  @Test
  void testRules() {
    board.fillCell(0, 0, 1);
    assertThat(board.canFill(1, 8, 1)).isTrue();
    assertThat(board.canFill(8, 1, 1)).isTrue();
    assertThat(board.canFill(0, 0, 1)).isFalse();
    assertThat(board.canFill(8, 0, 1)).isFalse();
    assertThat(board.canFill(0, 8, 1)).isFalse();
    assertThat(board.canFill(2, 2, 1)).isFalse();

    board.fillCell(8, 8, 2);
    assertThat(board.canFill(6, 6, 1)).isTrue();
    assertThat(board.canFill(6, 8, 2)).isFalse();
    assertThat(board.canFill(8, 6, 2)).isFalse();
  }

  @Test
  void testBlockIndex() throws Exception {
    assertThat(board.blockIndex(0, 0)).isEqualTo(0);
    assertThat(board.blockIndex(3, 0)).isEqualTo(1);
    assertThat(board.blockIndex(3, 8)).isEqualTo(7);
    assertThat(board.blockIndex(3, 3)).isEqualTo(4);
    assertThat(board.blockIndex(6, 8)).isEqualTo(8);
  }

  @Test
  void testClearCell() {
    board.fillCell(4, 4, 9);
    BitSet block = SodokuSolver.Board.ruleSet();
    block.set(0);
    block.set(9);
    assertThat(board.blockSet(board.blockIndex(4, 4))).isEqualTo(block);

    board.clearCell(4, 4);
    block.clear(9);
    assertThat(block.get(9)).isFalse();
    assertThat(block.get(0)).isTrue();
    assertThat(board.blockSet(board.blockIndex(4, 4))).isEqualTo(block);
    assertThat(board.blockRules.get(board.blockIndex(4, 4))).isEqualTo(block);
    assertThat(board.rowRules.get(4)).isEqualTo(block);
    assertThat(board.colRules.get(4)).isEqualTo(block);
    assertThat(board.rowPopulation(4).isEmpty());
  }

  @Test
  void testScenario1() throws Exception {
    board.parseCSV(EASY_1);
  }

  @Test
  void testQueries() throws Exception {
    board.parseCSV(EASY_1);
    // do some queries around the solvable position at 4,4
    BitSet col = SodokuSolver.Board.ruleSet();
    Arrays.stream("0, 0, 3, 0, 0, 0, 1, 0, 0".split("\\s*,\\s*"))
        .forEach((v) -> col.set(Integer.valueOf(v)));
    BitSet row = SodokuSolver.Board.ruleSet();
    Arrays.stream("7, 4, 5,  1, 0, 8,  9, 6, 3".split("\\s*,\\s*"))
        .forEach((v) -> row.set(Integer.valueOf(v)));
    // block:
    BitSet block = SodokuSolver.Board.ruleSet();
    Arrays.stream("3, 0, 0, 1, 0, 8, 0, 0, 6".split("\\s*,\\s*"))
        .forEach((v) -> block.set(Integer.valueOf(v)));

    assertThat(board.rowSet(4)).isEqualTo(row);
    assertThat(board.colSet(4)).isEqualTo(col);
    assertThat(board.blockSet(board.blockIndex(4, 4))).isEqualTo(block);

    assertThat(row.cardinality()).isEqualTo(9);
    assertThat(row.nextClearBit(0)).isEqualTo(2);
    assertThat(row.nextClearBit(3)).isEqualTo(10);
    assertThat(col.cardinality()).isEqualTo(3);
    assertThat(col.nextClearBit(0)).isEqualTo(2);
    assertThat(col.nextClearBit(3)).isEqualTo(4);
    assertThat(block.cardinality()).isEqualTo(5);
    assertThat(block.nextClearBit(0)).isEqualTo(2);
    assertThat(block.nextClearBit(3)).isEqualTo(4);

    BitSet allSet = board.allInUseAt(4, 4);
    assertThat(allSet.cardinality()).isEqualTo(9);
    assertThat(allSet.nextClearBit(0)).isEqualTo(2);

    assertThat(board.rowPopulation(4).nextClearBit(0)).isEqualTo(4);
    assertThat(board.rowPopulation(5).nextClearBit(0)).isEqualTo(0);
    assertThat(board.rowPopulation(6).nextClearBit(0)).isEqualTo(1);
  }

  @Test
  void testSolveEasy() throws Exception {
    board.parseCSV(EASY_1);
    boolean solved = board.solve(false);
    assertThat(solved).isTrue();
    board.print();
  }

  @Test
  void testSolveMedium() throws Exception {
    board.parseCSV(MEDIUM_1);
    boolean solved = board.solve(false);
    assertThat(solved).isTrue();
  }

  @Test
  void testSolveEvil() throws Exception {
    board.parseCSV(EVIL_1);
    boolean solved = board.solve(true);
    assertThat(solved).isTrue();
  }
}
