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
    static final int WIDTH    = 1024;
    static final int HEIGHT   = 600;
    static final int GROUND_Y = 450;

    // --- Bases ---
    static final int BASE_WIDTH   = 80;
    static final int BASE_HEIGHT  = 120;
    static final int LEFT_BASE_X  = 20;
    static final int RIGHT_BASE_X = WIDTH - LEFT_BASE_X - BASE_WIDTH;

    // --- Unit config ---
    static final int  UNIT_SIZE         = 28;
    static final long SPAWN_COOLDOWN_NS = 1_000_000_000L;

    // ...Menu... 
    enum GameState {MENU, PLAYING}
    private GameState gameState = GameState.MENU;

    
    static final int BUTTON_X = WIDTH / 2 -100;
    static final int BUTTON_Y = HEIGHT / 2;
    static final int BUTTON_WIDTH = 200;
    static final int BUTTON_HEIGHT = 50;
    

    


    // ---------------------------------------------------------------
    // Abstract Unit base class
    // ---------------------------------------------------------------
    static abstract class Unit {
        double x, y;
        boolean isPlayer;
        double speed;

        Unit(double x, double y, boolean isPlayer) {
            this.x = x;
            this.y = y;
            this.isPlayer = isPlayer;
        }

        abstract void draw(GraphicsContext gc);

        void update(double deltaSeconds) {
            x += (isPlayer ? 1 : -1) * speed * deltaSeconds;
        }

        boolean hasReachedEnemyBase() {
            if (isPlayer) return x + UNIT_SIZE >= RIGHT_BASE_X;
            else          return x <= LEFT_BASE_X + BASE_WIDTH;
        }
    }

    // ---------------------------------------------------------------
    // Soldier — fast, light unit
    // ---------------------------------------------------------------
    static class Soldier extends Unit {
        Soldier(double x, double y, boolean isPlayer) {
            super(x, y, isPlayer);
            this.speed = 60.0;
        }

        @Override
        void draw(GraphicsContext gc) {
            Color body    = isPlayer ? Color.CORNFLOWERBLUE : Color.TOMATO;
            Color outline = isPlayer ? Color.DARKBLUE       : Color.DARKRED;

            // Body
            gc.setFill(body);
            gc.fillRect(x, y - UNIT_SIZE, UNIT_SIZE, UNIT_SIZE);
            gc.setStroke(outline);
            gc.setLineWidth(2);
            gc.strokeRect(x, y - UNIT_SIZE, UNIT_SIZE, UNIT_SIZE);

            // Head
            double headR = UNIT_SIZE * 0.35;
            double headX = x + UNIT_SIZE / 2.0 - headR;
            double headY = y - UNIT_SIZE - headR * 2 - 2;
            gc.setFill(body.brighter());
            gc.fillOval(headX, headY, headR * 2, headR * 2);
            gc.strokeOval(headX, headY, headR * 2, headR * 2);

            // Direction indicator
            double cx = x + UNIT_SIZE / 2.0;
            double cy = y - UNIT_SIZE / 2.0;
            gc.setFill(Color.WHITE);
            if (isPlayer) {
                gc.fillPolygon(
                    new double[]{cx + 4, cx + 12, cx + 4},
                    new double[]{cy - 5,  cy,      cy + 5}, 3
                );
            } else {
                gc.fillPolygon(
                    new double[]{cx - 4, cx - 12, cx - 4},
                    new double[]{cy - 5,  cy,      cy + 5}, 3
                );
            }
        }
    }

    // ---------------------------------------------------------------
    // Knight — slow, bulky unit
    // ---------------------------------------------------------------
    static class Knight extends Unit {
        Knight(double x, double y, boolean isPlayer) {
            super(x, y, isPlayer);
            this.speed = 35.0;
        }

        @Override
        void draw(GraphicsContext gc) {
            Color body    = isPlayer ? Color.DARKSLATEBLUE : Color.DARKRED;
            Color outline = isPlayer ? Color.MIDNIGHTBLUE  : Color.MAROON;

            // Larger body
            gc.setFill(body);
            gc.fillRect(x, y - 38, 34, 38);
            gc.setStroke(outline);
            gc.setLineWidth(2);
            gc.strokeRect(x, y - 38, 34, 38);

            // Helmet
            gc.setFill(body.brighter());
            gc.fillOval(x + 5, y - 52, 24, 20);
            gc.strokeOval(x + 5, y - 52, 24, 20);

            // Direction indicator
            double cx = x + 17;
            double cy = y - 19;
            gc.setFill(Color.WHITE);
            if (isPlayer) {
                gc.fillPolygon(
                    new double[]{cx + 4, cx + 12, cx + 4},
                    new double[]{cy - 5,  cy,      cy + 5}, 3
                );
            } else {
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

    private boolean spawnKeyHeld = false;

    // ---------------------------------------------------------------
    // JavaFX entry point
    // ---------------------------------------------------------------
    @Override
    public void start(Stage stage) {
        Canvas canvas = new Canvas(WIDTH, HEIGHT);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        StackPane root = new StackPane(canvas);
        Scene scene = new Scene(root, WIDTH, HEIGHT);

        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.SPACE) spawnKeyHeld = true;
        });
        scene.setOnKeyReleased(e -> {
            if (e.getCode() == KeyCode.SPACE) spawnKeyHeld = false;
        });
        scene.setOnMouseClicked(e -> {
            if (gameState == GameState.MENU){
                if (e.getX() > BUTTON_X && e.getX() < BUTTON_X + BUTTON_WIDTH && 
                    e.getY() > BUTTON_Y && e.getY() < BUTTON_Y + BUTTON_HEIGHT){
                        gameState = GameState.PLAYING;
                    }
            }
        });

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

        stage.setTitle("Age of Warish");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();

    }

    // ---------------------------------------------------------------
    // Update
    // ---------------------------------------------------------------
    private void update(long now, double delta) {
        if (gameState == GameState.PLAYING){
        if (spawnKeyHeld && (now - lastPlayerSpawn) >= SPAWN_COOLDOWN_NS) {
            spawnUnit(true);
            lastPlayerSpawn = now;
        }
        if ((now - lastEnemySpawn) >= SPAWN_COOLDOWN_NS) {
            spawnUnit(false);
            lastEnemySpawn = now;
        }

        Iterator<Unit> it = units.iterator();
        while (it.hasNext()) {
            Unit u = it.next();
            u.update(delta);
            if (u.hasReachedEnemyBase()) {
                if (u.isPlayer) playerScore++;
                else            enemyScore++;
                it.remove();
            }
        }}
    }

    private void spawnUnit(boolean isPlayer) {
        double spawnX = isPlayer
                ? LEFT_BASE_X + BASE_WIDTH + 4
                : RIGHT_BASE_X - UNIT_SIZE - 4;

        Unit u = Math.random() < 0.5
                ? new Soldier(spawnX, GROUND_Y, isPlayer)
                : new Knight(spawnX, GROUND_Y, isPlayer);
        units.add(u);
    }

    // ---------------------------------------------------------------
    // Render
    // ---------------------------------------------------------------
    private void render(GraphicsContext gc) {

        if (gameState == GameState.MENU) {
            renderMenu(gc);
        } else {
            renderGame(gc);
        }
    };
    private void renderGame(GraphicsContext gc){
        gc.setFill(Color.web("#87CEEB"));
        gc.fillRect(0, 0, WIDTH, HEIGHT);

        gc.setFill(Color.web("#6aab6a"));
        gc.fillOval(-80, 320, 400, 200);
        gc.fillOval(300, 340, 350, 180);
        gc.fillOval(700, 330, 420, 190);

        gc.setFill(Color.web("#5a8a3a"));
        gc.fillRect(0, GROUND_Y, WIDTH, HEIGHT - GROUND_Y);
        gc.setFill(Color.web("#7bc142"));
        gc.fillRect(0, GROUND_Y, WIDTH, 10);

        drawBase(gc, LEFT_BASE_X,  GROUND_Y - BASE_HEIGHT, true);
        drawBase(gc, RIGHT_BASE_X, GROUND_Y - BASE_HEIGHT, false);

        for (Unit u : units) u.draw(gc);

        drawHUD(gc);
    }
    private void renderMenu(GraphicsContext gc){
        gc.setFill(Color.web("#3167e4ff"));
        gc.fillRect(0, GROUND_Y, WIDTH, HEIGHT - GROUND_Y);

    }

    private void drawBase(GraphicsContext gc, int x, int y, boolean isPlayer) {
        Color wall = isPlayer ? Color.STEELBLUE      : Color.FIREBRICK;
        Color roof = isPlayer ? Color.DARKBLUE        : Color.DARKRED;
        Color door = isPlayer ? Color.web("#1a3a5c")  : Color.web("#5c1a1a");
        Color flag = isPlayer ? Color.YELLOW          : Color.ORANGE;

        gc.setFill(wall);
        gc.fillRect(x, y, BASE_WIDTH, BASE_HEIGHT);
        gc.setStroke(roof);
        gc.setLineWidth(2);
        gc.strokeRect(x, y, BASE_WIDTH, BASE_HEIGHT);

        int merlon = BASE_WIDTH / 5;
        gc.setFill(roof);
        for (int i = 0; i < 3; i++) {
            gc.fillRect(x + merlon * i + merlon / 4, y - 16, merlon / 2 + 2, 18);
        }

        int dw = BASE_WIDTH / 3;
        int dh = BASE_HEIGHT / 3;
        int dx = x + (BASE_WIDTH - dw) / 2;
        int dy = y + BASE_HEIGHT - dh;
        gc.setFill(door);
        gc.fillRoundRect(dx, dy, dw, dh, 8, 8);

        gc.setFill(Color.web("#fffbe6"));
        gc.fillRect(x + BASE_WIDTH / 2 - 8, y + BASE_HEIGHT / 4, 16, 16);
        gc.setStroke(roof);
        gc.strokeRect(x + BASE_WIDTH / 2 - 8, y + BASE_HEIGHT / 4, 16, 16);

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

        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 11));
        String label = isPlayer ? "YOUR BASE" : "ENEMY BASE";
        gc.fillText(label, x + (isPlayer ? 4 : 2), y - 58);
    }

    private void drawHUD(GraphicsContext gc) {
        gc.setFill(Color.color(0, 0, 0, 0.55));
        gc.fillRect(0, 0, WIDTH, 44);

        gc.setFont(Font.font("Arial", FontWeight.BOLD, 18));

        gc.setFill(Color.CORNFLOWERBLUE);
        gc.fillText("PLAYER", 20, 28);
        gc.setFill(Color.WHITE);
        gc.fillText("Score: " + playerScore, 100, 28);

        gc.setFill(Color.TOMATO);
        gc.fillText("ENEMY", WIDTH - 200, 28);
        gc.setFill(Color.WHITE);
        gc.fillText("Score: " + enemyScore, WIDTH - 120, 28);

        gc.setFill(Color.web("#ffe066"));
        gc.setFont(Font.font("Arial", FontWeight.NORMAL, 13));
        gc.fillText("Hold [SPACE] to spawn a unit", WIDTH / 2.0 - 95, 28);
    }

    // ---------------------------------------------------------------
    public static void main(String[] args) {
        launch(args);
    }
}