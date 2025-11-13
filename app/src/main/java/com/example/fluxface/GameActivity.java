package com.example.fluxface; // Make sure this matches your package name

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.Stack;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;

public class GameActivity extends AppCompatActivity {

    private TextView selectedCell = null;
    private LinearLayout gameLayout;
    private Drawable originalCellBackground = null;
    private int[][] solutionGrid = new int[9][9];
    private String currentDifficulty = "NORMAL";

    // --- Toolbar & Tools ---
    private Stack<Move> moveStack = new Stack<>();
    private ImageButton undoButton, eraseButton, checkButton, backButton;
    private int undoStreak = 0;

    // --- Timer & Score ---
    private TextView timerText, scoreText;
    private int score = 0;
    private Handler timerHandler = new Handler(Looper.getMainLooper());
    private long startTime = 0L;

    // --- Audio ---
    private MediaPlayer gameMusicPlayer;
    private SoundPool soundPool;
    private int clickSoundId;

    // --- Settings ---
    private boolean isMusicMuted = false;
    private boolean isSfxMuted = false;

    // --- For Highlighting ---
    private ArrayList<TextView> highlightedCells = new ArrayList<>();
    private int cellBackgroundColor;
    private int thickLineColor;

    // Helper class to store a move
    private static class Move {
        final TextView cell;
        final String previousText;
        Move(TextView cell, String previousText) {
            this.cell = cell;
            this.previousText = previousText;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        // --- Load preferences ---
        SharedPreferences prefs = getSharedPreferences(SettingsActivity.PREFS_NAME, MODE_PRIVATE);
        isMusicMuted = prefs.getBoolean(SettingsActivity.MUSIC_MUTED, false);
        isSfxMuted = prefs.getBoolean(SettingsActivity.SFX_MUTED, false);

        // --- Initialize Audio ---
        if (!isSfxMuted) {
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            soundPool = new SoundPool.Builder()
                    .setMaxStreams(3)
                    .setAudioAttributes(audioAttributes)
                    .build();
            clickSoundId = soundPool.load(this, R.raw.ui_button_click, 1);
        }

        if (!isMusicMuted) {
            gameMusicPlayer = MediaPlayer.create(this, R.raw.celtic_harp_323844);
            gameMusicPlayer.setLooping(true);
            gameMusicPlayer.start();
        }

        // Find new UI elements
        timerText = findViewById(R.id.timerText);
        scoreText = findViewById(R.id.scoreText);
        backButton = findViewById(R.id.btnBack);
        gameLayout = findViewById(R.id.game_layout);

        // Handle system back/pan gestures
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                showExitWarningDialog();
            }
        });

        TextView modeText = findViewById(R.id.modeText);
        currentDifficulty = getIntent().getStringExtra("DIFFICULTY");

        undoButton = findViewById(R.id.btnUndo);
        eraseButton = findViewById(R.id.btnErase);
        checkButton = findViewById(R.id.btnCheckSolution);

        // Set icons (using Vector Assets is recommended)
        undoButton.setImageResource(R.drawable.undo);
        eraseButton.setImageResource(R.drawable.eraser);
        checkButton.setImageResource(R.drawable.check);
        backButton.setImageResource(R.drawable.ic_back_arrow);

        if (currentDifficulty != null) {
            SudokuGenerator generator = new SudokuGenerator();
            int[][] currentPuzzle = generator.generatePuzzle(currentDifficulty);
            this.solutionGrid = generator.getSolution();

            // Apply style based on difficulty
            switch (currentDifficulty) {
                case "HARD":
                    modeText.setText("Challenge Mode");
                    applyStyle(R.color.challenge_bg, R.color.challenge_text, R.color.challenge_thick_lines,
                            Typeface.MONOSPACE, R.color.challenge_button_bg, R.color.challenge_button_text);
                    break;
                case "EASY":
                    modeText.setText("Calm Mode");
                    applyStyle(R.color.calm_bg, R.color.calm_text, R.color.calm_thick_lines,
                            Typeface.SANS_SERIF, R.color.calm_button_bg, R.color.calm_button_text);
                    break;
                default:
                    modeText.setText("Normal Mode");
                    applyStyle(R.color.normal_bg, R.color.normal_text, R.color.normal_thick_lines,
                            Typeface.SERIF, R.color.normal_button_bg, R.color.normal_button_text);
                    break;
            }

            populateGrid(currentPuzzle);
            setupCellListeners();
            setupNumberPadListeners();
            setupToolbarListeners();
            setupBackgroundDeselectListener();
            startTimer();
            updateScore(0);
        }
    }

    // --- Audio management for THIS activity ---
    @Override
    protected void onStop() {
        super.onStop();
        if (gameMusicPlayer != null && gameMusicPlayer.isPlaying()) {
            gameMusicPlayer.pause();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (gameMusicPlayer == null && !isMusicMuted) {
            gameMusicPlayer = MediaPlayer.create(this, R.raw.celtic_harp_323844);
            gameMusicPlayer.setLooping(true);
        }
        if (gameMusicPlayer != null && !gameMusicPlayer.isPlaying() && !isMusicMuted) {
            gameMusicPlayer.start();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopTimer();

        if (gameMusicPlayer != null) {
            gameMusicPlayer.stop();
            gameMusicPlayer.release();
            gameMusicPlayer = null;
        }
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }

        MusicManager.start(this); // Resume menu music
    }
    // ------------------------------------

    private void startTimer() {
        startTime = System.currentTimeMillis();
        timerHandler.postDelayed(timerRunnable, 0);
    }

    private void stopTimer() {
        timerHandler.removeCallbacks(timerRunnable);
    }

    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            long millis = System.currentTimeMillis() - startTime;
            int seconds = (int) (millis / 1000);
            int minutes = seconds / 60;
            seconds = seconds % 60;
            if (timerText != null) timerText.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
            timerHandler.postDelayed(this, 500);
        }
    };

    private void updateScore(int amountToAdd) {
        score += amountToAdd;
        if (scoreText != null) scoreText.setText("Score: " + score);
    }

    private void playClickSound() {
        if (!isSfxMuted && soundPool != null) {
            soundPool.play(clickSoundId, 1, 1, 0, 0, 1);
        }
    }

    private void setupToolbarListeners() {
        if (checkButton != null) checkButton.setOnClickListener(v -> {
            playClickSound();
            validateBoard();
        });

        if (eraseButton != null)
            eraseButton.setOnClickListener(v -> {
                playClickSound();
                // --- MODIFIED: Check if cell is clickable ---
                if (selectedCell != null && selectedCell.isClickable() && !TextUtils.isEmpty(selectedCell.getText())) {
                    moveStack.push(new Move(selectedCell, selectedCell.getText().toString()));
                    selectedCell.setText("");
                    updateScore(-5);
                    undoStreak = 0;
                    clearNumberHighlights();
                }
            });

        if (undoButton != null)
            undoButton.setOnClickListener(v -> {
                playClickSound();
                if (!moveStack.isEmpty()) {
                    Move lastMove = moveStack.pop();
                    lastMove.cell.setText(lastMove.previousText);
                    undoStreak++;
                    if (undoStreak >= 3) {
                        updateScore(-30);
                        undoStreak = 0;
                    }

                    clearNumberHighlights();

                    if (selectedCell == lastMove.cell) {
                        selectedCell.setBackground(originalCellBackground);
                        selectedCell = null;
                    }
                }
            });

        if (backButton != null) backButton.setOnClickListener(v -> {
            playClickSound();
            showExitWarningDialog();
        });
    }

    // ... (Your showExitWarningDialog, showNotFinishedDialog, showWinDialog, and saveBestTime methods) ...
    private void showExitWarningDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Exit Game")
                .setMessage("Do you really want to exit? Your progress will not be saved.")
                .setPositiveButton("Yes", (dialog, which) -> finish())
                .setNegativeButton("No", null)
                .show();
    }

    private void showNotFinishedDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Not Finished")
                .setMessage("The puzzle is not fully attempted. Keep going!")
                .setPositiveButton("OK", null)
                .show();
    }

    private void showWinDialog(long timeInSeconds) {
        // stopTimer() and updateScore() are now called inside validateBoard() -> logGameResult()
        new AlertDialog.Builder(this)
                .setTitle("Congratulations!")
                .setMessage("Puzzle Solved!\nYour Score: " + score + "\nTime: " + (timerText != null ? timerText.getText().toString() : ""))
                .setPositiveButton("Play Again", (dialog, which) -> recreate())
                .setNegativeButton("Main Menu", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }

    private void showWrongSolutionDialog() {
        stopTimer(); // Stop the timer
        new AlertDialog.Builder(this)
                .setTitle("Oho! Not Quite...")
                .setMessage("The solution is not correct. Keep trying?")
                .setPositiveButton("Continue", (dialog, which) -> {
                    startTimer(); // Resume the timer
                })
                .setNegativeButton("Main Menu", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }

    /**
     * Logs the result of the game (win or loss) to the database.
     */
// ... (code before this method is unchanged) ...

    /**
     * Logs the result of the game (win or loss) to the database.
     * This is the single, correct method for saving game data.
     */

    private void saveBestTime(boolean isWin, long newTimeInSeconds) {
        // Get the current user's email from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String userEmail = prefs.getString("userEmail", null);

        if (userEmail == null) {
            // User is not logged in, don't save score to database
            Toast.makeText(this, "Log in to save your score!", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseHelper dbHelper = new DatabaseHelper(this);
        int userId = dbHelper.getUserId(userEmail);

        if (userId != -1) {
            // THIS CALL WAS CAUSING THE ERROR
            dbHelper.addGameResult(userId, currentDifficulty, (int) newTimeInSeconds, score, isWin);

            // Check if it's a new best time
            int bestTime = dbHelper.getBestTime(userId, currentDifficulty);
            if (newTimeInSeconds == bestTime) {
                Toast.makeText(this, "New Best Time!", Toast.LENGTH_SHORT).show();
            }
        }
    }
    private void logGameResult(boolean isWin, long timeInSeconds) {
        // Apply win bonus if they won
        if (isWin) {
            int timeBonus = Math.max(0, 1000 - (int) timeInSeconds);
            updateScore(timeBonus);
        }

        // Stop the timer
        stopTimer();

        // Get the current user's email from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String userEmail = prefs.getString("userEmail", null);

        if (userEmail == null) {
            // User is not logged in, don't save score to database
            if (isWin) {
                Toast.makeText(this, "Log in to save your score!", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        DatabaseHelper dbHelper = new DatabaseHelper(this);
        int userId = dbHelper.getUserId(userEmail);

        if (userId != -1) {
            // Add the game result (win or loss) to the database
            dbHelper.addGameResult(userId, currentDifficulty, (int) timeInSeconds, score, isWin);

            // Check if it's a new best time if the user won
            if (isWin) {
                // This call correctly saves the best time if the new time is better
                dbHelper.getBestTime(userId, currentDifficulty);

                // Check if the time just saved is now the best time, to show a toast
                int bestTime = dbHelper.getBestTime(userId, currentDifficulty);
                if ((int) timeInSeconds == bestTime) {
                    Toast.makeText(this, "New Best Time!", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
    // --- END OF DELETED METHOD ---

    private void setupBackgroundDeselectListener() {
        gameLayout.setOnClickListener(v -> {
            // Check if a cell is currently selected
            if (selectedCell != null) {
                // Restore its original complex background
                int[] pos = (int[]) selectedCell.getTag();
                selectedCell.setBackground(createCellBackground(pos[0], pos[1], cellBackgroundColor, thickLineColor));

                // Clear any number highlights
                clearNumberHighlights();

                // Deselect the cell
                selectedCell = null;
            }
        });
    }


    private void setupNumberPadListeners() {
        GridLayout numberPad = findViewById(R.id.number_pad);
        for (int i = 0; i < numberPad.getChildCount(); i++) {
            if (numberPad.getChildAt(i) instanceof Button) {
                final Button numberButton = (Button) numberPad.getChildAt(i);
                numberButton.setOnClickListener(v -> {
                    playClickSound();
                    String newText = numberButton.getText().toString();

                    // --- Update highlights ---
                    clearNumberHighlights();
                    highlightMatchingNumbers(newText);

                    if (selectedCell != null) {
                        String oldText = selectedCell.getText().toString();

                        // --- MODIFIED: Check if cell is clickable ---
                        if (selectedCell.isClickable() && TextUtils.isEmpty(oldText)) {
                            moveStack.push(new Move(selectedCell, ""));
                            selectedCell.setText(newText);
                            updateScore(10);
                            undoStreak = 0;
                        } else if (!selectedCell.isClickable()){
                            // It's a pre-filled cell, do nothing
                        } else {
                            Toast.makeText(GameActivity.this, "Use the eraser to change a number", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        }
    }

    private void validateBoard() {
        int[][] playerGrid = getPlayerGrid();
        boolean isComplete = true;

        // --- First, check if the board is even full ---
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                if (playerGrid[i][j] == 0) {
                    isComplete = false;
                    break;
                }
            }
            if (!isComplete) {
                break;
            }
        }

        // --- If not complete, show the pop-up and STOP ---
        if (!isComplete) {
            showNotFinishedDialog();
            return;
        }

        // --- If board IS complete, check the RULES ---
        boolean isCorrect = isSudokuSolutionValid(playerGrid);

        long timeInSeconds = (System.currentTimeMillis() - startTime) / 1000;

        if (isCorrect) {
            // Board is full and correct
            logGameResult(true, timeInSeconds);
            showWinDialog(timeInSeconds);
        } else {
            // Board is full but incorrect
            logGameResult(false, timeInSeconds);
            showWrongSolutionDialog();
        }
    }


    // java
    private void applyStyle(int backgroundColorRes, int textColorRes, int thickLineColorRes, Typeface font, int buttonBgRes, int buttonTextRes) {

        this.cellBackgroundColor = ContextCompat.getColor(this, backgroundColorRes);
        this.thickLineColor = ContextCompat.getColor(this, thickLineColorRes);

        findViewById(R.id.game_layout).setBackgroundColor(cellBackgroundColor);
        TableLayout grid = findViewById(R.id.sudoku_grid);
        grid.setBackgroundColor(thickLineColor);

        for (int i = 0; i < 9; i++) {
            TableRow row = (TableRow) grid.getChildAt(i);
            for (int j = 0; j < 9; j++) {
                TextView cell = (TextView) row.getChildAt(j);
                cell.setBackground(createCellBackground(i, j, cellBackgroundColor, thickLineColor));
                cell.setTextColor(ContextCompat.getColor(this, textColorRes));
                cell.setTypeface(font);
            }
        }

        GridLayout numberPad = findViewById(R.id.number_pad);
        ColorStateList buttonTint = ColorStateList.valueOf(ContextCompat.getColor(this, buttonBgRes));
        int buttonTextColor = ContextCompat.getColor(this, buttonTextRes);

        for (int i = 0; i < numberPad.getChildCount(); i++) {
            if (numberPad.getChildAt(i) instanceof Button) {
                Button numberButton = (Button) numberPad.getChildAt(i);
                numberButton.setBackgroundTintList(buttonTint);
                numberButton.setTextColor(buttonTextColor);
            }
        }

        // --- MODIFIED: Your existing tinting logic ---
        int iconColor = ContextCompat.getColor(this, buttonTextRes);
        if (backgroundColorRes == R.color.challenge_bg) {
            int whiteColor = ContextCompat.getColor(this, R.color.white);
            if (undoButton != null) ImageViewCompat.setImageTintList(undoButton, ColorStateList.valueOf(whiteColor));
            if (checkButton != null) ImageViewCompat.setImageTintList(checkButton, ColorStateList.valueOf(whiteColor));
            if (eraseButton != null) ImageViewCompat.setImageTintList(eraseButton, null);
            if (backButton != null) ImageViewCompat.setImageTintList(backButton, ColorStateList.valueOf(whiteColor)); // Make back white
        } else {
            if (undoButton != null) ImageViewCompat.setImageTintList(undoButton, null);
            if (checkButton != null) ImageViewCompat.setImageTintList(checkButton, null);
            if (eraseButton != null) ImageViewCompat.setImageTintList(eraseButton, null);
            int blackColor = ContextCompat.getColor(this, R.color.black);
            if (backButton != null) ImageViewCompat.setImageTintList(backButton, ColorStateList.valueOf(blackColor));
        }

        // Timer and score text color rules: Calm/Normal => black, otherwise use provided textColorRes
        TextView timer = findViewById(R.id.timerText);
        TextView scoreTv = findViewById(R.id.scoreText);
        if (timer != null && scoreTv != null) {
            if (backgroundColorRes == R.color.calm_bg || backgroundColorRes == R.color.normal_bg) {
                int blackColor = ContextCompat.getColor(this, R.color.black);
                timer.setTextColor(blackColor);
                scoreTv.setTextColor(blackColor);
            } else {
                int modeTextColor = ContextCompat.getColor(this, textColorRes);
                timer.setTextColor(modeTextColor);
                scoreTv.setTextColor(modeTextColor);
            }
        }
    }


    // --- Unchanged Methods ---

    private Drawable createCellBackground(int row, int col, int cellBackgroundColor, int thickLineColor) {
        Drawable[] layers = new Drawable[3];
        layers[0] = new ColorDrawable(thickLineColor);
        layers[1] = new ColorDrawable(ContextCompat.getColor(this, R.color.sudoku_thin_lines));
        layers[2] = new ColorDrawable(cellBackgroundColor);

        LayerDrawable layerDrawable = new LayerDrawable(layers);
        int thick = 4;
        int thin = 1;

        layerDrawable.setLayerInset(1, (col % 3 == 0) ? thick : 0, (row % 3 == 0) ? thick : 0, 0, 0);
        layerDrawable.setLayerInset(2, ((col % 3 == 0) ? thick : 0) + thin, ((row % 3 == 0) ? thick : 0) + thin, (col == 8) ? thick : thin, (row == 8) ? thick : thin);

        return layerDrawable;
    }

    private void populateGrid(int[][] puzzle) {
        TableLayout grid = findViewById(R.id.sudoku_grid);
        for (int i = 0; i < 9; i++) {
            TableRow row = (TableRow) grid.getChildAt(i);
            for (int j = 0; j < 9; j++) {
                TextView cell = (TextView) row.getChildAt(j);

                // --- NEW: Store row/col in the cell's tag ---
                cell.setTag(new int[]{i, j});

                int number = puzzle[i][j];
                if (number == 0) {
                    cell.setText("");
                    cell.setClickable(true); // <-- This is key
                } else {
                    cell.setText(String.valueOf(number));
                    cell.setClickable(false); // <-- This is key
                }
            }
        }
    }

    // --- THIS IS THE MODIFIED METHOD ---
    private void setupCellListeners() {
        TableLayout grid = findViewById(R.id.sudoku_grid);
        for (int i = 0; i < 9; i++) {
            TableRow row = (TableRow) grid.getChildAt(i);
            for (int j = 0; j < 9; j++) {
                final TextView cell = (TextView) row.getChildAt(j);

                if (cell.isClickable()) {
                    cell.setOnClickListener(v -> {
                        // 1. Deselect the previous cell
                        if (selectedCell != null) {
                            int[] pos = (int[]) selectedCell.getTag();
                            selectedCell.setBackground(createCellBackground(pos[0], pos[1], cellBackgroundColor, thickLineColor));
                        }

                        clearNumberHighlights();

                        // 2. Select the new cell
                        selectedCell = (TextView) v;
                        // originalCellBackground = selectedCell.getBackground(); // <-- DELETE THIS LINE
                        selectedCell.setBackgroundColor(Color.YELLOW); // Highlight new cell
                    });
                }
            }
        }
    }

    private int[][] getPlayerGrid() {
        int[][] grid = new int[9][9];
        TableLayout table = findViewById(R.id.sudoku_grid);
        for (int i = 0; i < 9; i++) {
            TableRow row = (TableRow) table.getChildAt(i);
            for (int j = 0; j < 9; j++) {
                TextView cell = (TextView) row.getChildAt(j);
                String text = cell.getText().toString();
                if (TextUtils.isEmpty(text)) {
                    grid[i][j] = 0;
                } else {
                    grid[i][j] = Integer.parseInt(text);
                }
            }
        }
        return grid;
    }

    // --- ADDED THESE TWO NEW METHODS ---

    /**
     * Finds all cells with the same number and highlights them.
     */
    private void highlightMatchingNumbers(String number) {
        if (number == null || number.isEmpty()) {
            return;
        }

        TableLayout grid = findViewById(R.id.sudoku_grid);
        for (int i = 0; i < 9; i++) {
            TableRow row = (TableRow) grid.getChildAt(i);
            for (int j = 0; j < 9; j++) {
                TextView cell = (TextView) row.getChildAt(j);
                if (cell.getText().toString().equals(number)) {
                    if (cell != selectedCell) {
                        cell.setBackgroundColor(Color.LTGRAY);
                        highlightedCells.add(cell);
                    }
                }
            }
        }
    }

    /**
     * Clears all previously highlighted number cells.
     */
    private void clearNumberHighlights() {
        for (TextView cell : highlightedCells) {
            // Restore its original complex background
            int[] pos = (int[]) cell.getTag();
            cell.setBackground(createCellBackground(pos[0], pos[1], cellBackgroundColor, thickLineColor));
        }
        highlightedCells.clear();
    }

    /**
     * Checks if a completed 9x9 grid follows all Sudoku rules.
     */
    private boolean isSudokuSolutionValid(int[][] grid) {
        for (int i = 0; i < 9; i++) {
            // Check row i
            if (!isUnitValid(grid[i])) {
                return false;
            }

            // Check column i
            int[] column = new int[9];
            for (int j = 0; j < 9; j++) {
                column[j] = grid[j][i];
            }
            if (!isUnitValid(column)) {
                return false;
            }
        }

        // Check 3x3 subgrids
        for (int i = 0; i < 9; i += 3) {
            for (int j = 0; j < 9; j += 3) {
                int[] block = new int[9];
                int index = 0;
                for (int row = i; row < i + 3; row++) {
                    for (int col = j; col < j + 3; col++) {
                        block[index++] = grid[row][col];
                    }
                }
                if (!isUnitValid(block)) {
                    return false;
                }
            }
        }

        return true; // All checks passed
    }

    /**
     * Helper method to check if a single unit (row, col, or block) is valid.
     * A unit is valid if it contains numbers 1-9 exactly once.
     */
    private boolean isUnitValid(int[] unit) {
        if (unit.length != 9) return false;
        Set<Integer> set = new HashSet<>();
        for (int number : unit) {
            if (number < 1 || number > 9 || !set.add(number)) {
                return false; // Contains duplicate or invalid number
            }
        }
        return set.size() == 9;
    }
}