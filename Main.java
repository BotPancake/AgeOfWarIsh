import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Main extends Application {

    // --- Window & World ---
    static final int WIDTH  = 1024;
    static final int HEIGHT = 600;
    static final int GROUND_Y = 450;          // y-level of the ground

    // --- Bases ---
    static final int BASE_WIDTH  = 80;
    static final int BASE_HEIGHT = 120;
    static final int LEFT_BASE_X  = 20;
    static final int RIGHT_BASE_X = WIDTH - LEFT_BASE_X - BASE_WIDTH;

    // --- Unit config ---
    static final double UNIT_SPEED   = 60.0;  // pixels per second
    static final int    UNIT_SIZE    = 28;
    static final long   SPAWN_COOLDOWN_NS = 3_000_000_000L; // 3 seconds

    // ---------------------------------------------------------------
    // Simple Unit class
    // ---------------------------------------------------------------
    static class Unit {
        double x, y;
        boolean isPlayer;   // true = player (left side), false = enemy (right side)

        Unit(double x, double y, boolean isPlayer) {
            this.x = x;
            this.y = y;
            this.isPlayer = isPlayer;
        }

        /** Move toward the opposing base. */
        void update(double deltaSeconds) {
            if (isPlayer) {
                x += UNIT_SPEED * deltaSeconds;
            } else {
                x -= UNIT_SPEED * deltaSeconds;
            }
        }

        /** Has this unit walked past the enemy base? */
        boolean hasReachedEnemyBase() {
            if (isPlayer)  return x + UNIT_SIZE >= RIGHT_BASE_X;
            else           return x <= LEFT_BASE_X + BASE_WIDTH;
        }

        void draw(GraphicsContext gc) {
            Color body = isPlayer ? Color.CORNFLOWERBLUE : Color.TOMATO;
            Color outline = isPlayer ? Color.DARKBLUE : Color.DARKRED;

            // Body (rectangle torso)
            gc.setFill(body);
            gc.fillRect(x, y - UNIT_SIZE, UNIT_SIZE, UNIT_SIZE);
            gc.setStroke(outline);
            gc.setLineWidth(2);
            gc.strokeRect(x, y - UNIT_SIZE, UNIT_SIZE, UNIT_SIZE);

            // Head (circle on top)
            double headR = UNIT_SIZE * 0.35;
            double headX = x + UNIT_SIZE / 2.0 - headR;
            double headY = y - UNIT_SIZE - headR * 2 - 2;
            gc.setFill(body.brighter());
            gc.fillOval(headX, headY, headR * 2, headR * 2);
            gc.strokeOval(headX, headY, headR * 2, headR * 2);

            // Direction indicator (tiny triangle showing facing direction)
            double cx = x + UNIT_SIZE / 2.0;
            double cy = y - UNIT_SIZE / 2.0;
            if (isPlayer) {
                gc.setFill(Color.WHITE);
                gc.fillPolygon(
                    new double[]{cx + 4, cx + 12, cx + 4},
                    new double[]{cy - 5,  cy,      cy + 5}, 3
                );
            } else {
                gc.setFill(Color.WHITE);
                gc.fillPolygon(
                    new double[]{cx - 4, cx - 12, cx - 4},
                    new double[]{cy - 5,  cy,      cy + 5}, 3
                );
            }
        }
    }

    // ---------------------------------------------------------------
    // Game state
    // ---------------------------------------------------------------
    private final List<Unit> units = new ArrayList<>();

    private long lastPlayerSpawn = 0;
    private long lastEnemySpawn  = 0;

    private int playerScore = 0;
    private int enemyScore  = 0;

    private boolean spawnKeyHeld = false;   // Space bar

    // ---------------------------------------------------------------
    // JavaFX entry points
    // ---------------------------------------------------------------
    @Override
    public void start(Stage stage) {
        Canvas canvas = new Canvas(WIDTH, HEIGHT);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        StackPane root = new StackPane(canvas);
        Scene scene = new Scene(root, WIDTH, HEIGHT);

        // Key handling
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.SPACE) spawnKeyHeld = true;
        });
        scene.setOnKeyReleased(e -> {
            if (e.getCode() == KeyCode.SPACE) spawnKeyHeld = false;
        });

        // Game loop
        AnimationTimer timer = new AnimationTimer() {
            private long lastTime = 0;

            @Override
            public void handle(long now) {
                if (lastTime == 0) { lastTime = now; return; }
                double delta = (now - lastTime) / 1_000_000_000.0;
                lastTime = now;

                update(now, delta);
                render(gc);
            }
        };
        timer.start();

        stage.setTitle("Age of War");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    // ---------------------------------------------------------------
    // Update
    // ---------------------------------------------------------------
    private void update(long now, double delta) {

        // --- Spawn player unit when SPACE is held (respects cooldown) ---
        if (spawnKeyHeld && (now - lastPlayerSpawn) >= SPAWN_COOLDOWN_NS) {
            spawnUnit(true);
            lastPlayerSpawn = now;
        }

        // --- Auto-spawn enemy unit ---
        if ((now - lastEnemySpawn) >= SPAWN_COOLDOWN_NS) {
            spawnUnit(false);
            lastEnemySpawn = now;
        }

        // --- Move units and remove those that reached the enemy base ---
        Iterator<Unit> it = units.iterator();
        while (it.hasNext()) {
            Unit u = it.next();
            u.update(delta);
            if (u.hasReachedEnemyBase()) {
                if (u.isPlayer) playerScore++;
                else            enemyScore++;
                it.remove();
            }
        }
    }

    private void spawnUnit(boolean isPlayer) {
        double spawnX = isPlayer
                ? LEFT_BASE_X + BASE_WIDTH + 4
                : RIGHT_BASE_X - UNIT_SIZE - 4;
        units.add(new Unit(spawnX, GROUND_Y, isPlayer));
    }

    // ---------------------------------------------------------------
    // Render
    // ---------------------------------------------------------------
    private void render(GraphicsContext gc) {
        // Sky gradient background
        gc.setFill(Color.web("#87CEEB"));
        gc.fillRect(0, 0, WIDTH, HEIGHT);

        // Distant hills
        gc.setFill(Color.web("#6aab6a"));
        gc.fillOval(-80, 320, 400, 200);
        gc.fillOval(300, 340, 350, 180);
        gc.fillOval(700, 330, 420, 190);

        // Ground
        gc.setFill(Color.web("#5a8a3a"));
        gc.fillRect(0, GROUND_Y, WIDTH, HEIGHT - GROUND_Y);
        gc.setFill(Color.web("#7bc142"));
        gc.fillRect(0, GROUND_Y, WIDTH, 10);

        // Draw bases
        drawBase(gc, LEFT_BASE_X,  GROUND_Y - BASE_HEIGHT, true);
        drawBase(gc, RIGHT_BASE_X, GROUND_Y - BASE_HEIGHT, false);

        // Draw units
        for (Unit u : units) u.draw(gc);

        // HUD
        drawHUD(gc);
    }

    private void drawBase(GraphicsContext gc, int x, int y, boolean isPlayer) {
        Color wall    = isPlayer ? Color.STEELBLUE        : Color.FIREBRICK;
        Color roof    = isPlayer ? Color.DARKBLUE         : Color.DARKRED;
        Color door    = isPlayer ? Color.web("#1a3a5c")   : Color.web("#5c1a1a");
        Color flag    = isPlayer ? Color.YELLOW           : Color.ORANGE;

        // Main wall
        gc.setFill(wall);
        gc.fillRect(x, y, BASE_WIDTH, BASE_HEIGHT);
        gc.setStroke(roof);
        gc.setLineWidth(2);
        gc.strokeRect(x, y, BASE_WIDTH, BASE_HEIGHT);

        // Battlements (3 merlons on top)
        int merlon = BASE_WIDTH / 5;
        gc.setFill(roof);
        for (int i = 0; i < 3; i++) {
            gc.fillRect(x + merlon * i + merlon / 4, y - 16, merlon / 2 + 2, 18);
        }

        // Door
        int dw = BASE_WIDTH / 3;
        int dh = BASE_HEIGHT / 3;
        int dx = x + (BASE_WIDTH - dw) / 2;
        int dy = y + BASE_HEIGHT - dh;
        gc.setFill(door);
        gc.fillRoundRect(dx, dy, dw, dh, 8, 8);

        // Window
        gc.setFill(Color.web("#fffbe6"));
        gc.fillRect(x + BASE_WIDTH / 2 - 8, y + BASE_HEIGHT / 4, 16, 16);
        gc.setStroke(roof);
        gc.strokeRect(x + BASE_WIDTH / 2 - 8, y + BASE_HEIGHT / 4, 16, 16);

        // Flag pole and flag
        int poleX = isPlayer ? x + BASE_WIDTH - 6 : x + 6;
        gc.setStroke(Color.LIGHTGRAY);
        gc.setLineWidth(2);
        gc.strokeLine(poleX, y - 16, poleX, y - 50);
        gc.setFill(flag);
        if (isPlayer) {
            gc.fillPolygon(
                new double[]{poleX, poleX - 18, poleX},
                new double[]{y - 50, y - 42, y - 33}, 3
            );
        } else {
            gc.fillPolygon(
                new double[]{poleX, poleX + 18, poleX},
                new double[]{y - 50, y - 42, y - 33}, 3
            );
        }

        // Label
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 11));
        String label = isPlayer ? "YOUR BASE" : "ENEMY BASE";
        gc.fillText(label, x + (isPlayer ? 4 : 2), y - 58);
    }

    private void drawHUD(GraphicsContext gc) {
        // Semi-transparent top bar
        gc.setFill(Color.color(0, 0, 0, 0.55));
        gc.fillRect(0, 0, WIDTH, 44);

        gc.setFont(Font.font("Arial", FontWeight.BOLD, 18));

        // Player side
        gc.setFill(Color.CORNFLOWERBLUE);
        gc.fillText("PLAYER", 20, 28);
        gc.setFill(Color.WHITE);
        gc.fillText("Score: " + playerScore, 100, 28);

        // Enemy side
        gc.setFill(Color.TOMATO);
        gc.fillText("ENEMY", WIDTH - 200, 28);
        gc.setFill(Color.WHITE);
        gc.fillText("Score: " + enemyScore, WIDTH - 120, 28);

        // Center hint
        gc.setFill(Color.web("#ffe066"));
        gc.setFont(Font.font("Arial", FontWeight.NORMAL, 13));
        gc.fillText("Hold [SPACE] to spawn a unit", WIDTH / 2.0 - 95, 28);
    }

    // ---------------------------------------------------------------
    public static void main(String[] args) {
        launch(args);
    }
}