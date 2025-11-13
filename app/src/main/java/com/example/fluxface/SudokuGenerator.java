package com.example.fluxface;

import java.util.ArrayList;
import java.util.Collections;

public class SudokuGenerator {

    int[][] grid;
    int[][] solvedGrid; // 1. ADD THIS VARIABLE
    int N = 9;
    int SRN = 3;

    public SudokuGenerator() {
        grid = new int[N][N];
        solvedGrid = new int[N][N]; // 2. INITIALIZE IT
    }

    // 3. ADD THIS HELPER FUNCTION
    private void copyGrid(int[][] from, int[][] to) {
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                to[i][j] = from[i][j];
            }
        }
    }

    // 4. ADD A "GETTER" FOR THE SOLUTION
    public int[][] getSolution() {
        return this.solvedGrid;
    }

    public int[][] generatePuzzle(String difficulty) {
        // ... (your existing code for clearing the grid) ...
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                grid[i][j] = 0;
            }
        }

        fillGrid();

        // 5. SAVE THE SOLUTION before poking holes
        copyGrid(grid, solvedGrid);

        // ... (your existing code for setting holesToPoke) ...
        int holesToPoke;
        switch (difficulty) {
            case "EASY":
                holesToPoke = 36;
                break;
            case "HARD":
                holesToPoke = 49;
                break;
            case "NORMAL":
            default:
                holesToPoke = 40;
                break;
        }

        pokeHoles(holesToPoke);
        return grid;
    }

    // ... (Your fillGrid, isValid, and pokeHoles methods remain unchanged) ...

    private boolean fillGrid() {
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                if (grid[i][j] == 0) {
                    // Create a shuffled list of numbers 1-9
                    ArrayList<Integer> numbers = new ArrayList<>();
                    for (int num = 1; num <= N; num++) numbers.add(num);
                    Collections.shuffle(numbers);

                    for (int num : numbers) {
                        if (isValid(i, j, num)) {
                            grid[i][j] = num;
                            if (fillGrid()) {
                                return true; // Move to the next cell
                            }
                            grid[i][j] = 0; // Backtrack
                        }
                    }
                    return false; // No valid number works, backtrack
                }
            }
        }
        return true; // All cells are filled
    }

    // Checks if placing a number is valid
    private boolean isValid(int row, int col, int num) {
        // Check row
        for (int j = 0; j < N; j++) {
            if (grid[row][j] == num) {
                return false;
            }
        }
        // Check column
        for (int i = 0; i < N; i++) {
            if (grid[i][col] == num) {
                return false;
            }
        }
        // Check 3x3 box
        int boxRowStart = row - row % SRN;
        int boxColStart = col - col % SRN;
        for (int i = 0; i < SRN; i++) {
            for (int j = 0; j < SRN; j++) {
                if (grid[boxRowStart + i][boxColStart + j] == num) {
                    return false;
                }
            }
        }
        // It's a valid move
        return true;
    }

    // Randomly removes numbers from the solved grid
    private void pokeHoles(int holes) {
        ArrayList<Integer> cellIndices = new ArrayList<>();
        for (int i = 0; i < N * N; i++) cellIndices.add(i);
        Collections.shuffle(cellIndices);

        for (int i = 0; i < holes; i++) {
            int cellIndex = cellIndices.get(i);
            int row = cellIndex / N;
            int col = cellIndex % N;
            if (grid[row][col] != 0) {
                grid[row][col] = 0; // Set cell to empty
            }
        }
    }
}