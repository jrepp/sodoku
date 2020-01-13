# Sudoku Solver in Java

A simple sudoku solver written in Java.

[Solver](./src/main/java/com/jrepp/SudokuSolver.java)

[Tests](./src/test/java/com/jrepp/SudokuSolverTest.java)


### Building

./gradlew  or gradlew.bat


### Explanation

The primary board state is 9 64bit longs, cells being stored in 4 bit subsections of each long.

Zero is used as a sentinel value

Each row, column and block has a BitSet representing the numbers currently present (including 0)

The solver finds the cell with the highest cardinality of known answers.

For each cell the set of viable answers is tried. For each valid choice the solver recurses.


### Example usage

```java
    String boardString = /* Use a comma separated list of 81 cells */
    SudokuSolver.Board board = SudodkuSolver.emptyBoard();
    board.parseCSV(boardString);
    board.solve(SudokuSolver.Trace.summary()); 
    board.print();
```
