
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
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

// ...Lagring og lasting...
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.BufferedReader;



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
    static final int  UNIT_SIZE        = 28;
    static final int  ARCHER_UNIT_SIZE = 24;
    

    // --- Spawn cooldowns ---
    static final long SOLDIER_COOLDOWN_NS = 1_750_000_000L;
    static final long ARCHER_COOLDOWN_NS  = 2_500_000_000L;
    static final long KNIGHT_COOLDOWN_NS  = 5_000_000_000L;
    static final long ENEMY_COOLDOWN_NS   = 2_000_000_000L;
    private long lastTime = 0;

    // --- Save popup ---
    private long LastSaveTime = 0;

    // --- Menu buttons ---
    static final int BUTTON_X = WIDTH / 2 - 100;
    static final int BUTTON_Y = HEIGHT / 2;
    static final int BUTTON_WIDTH  = 200;
    static final int BUTTON_HEIGHT = 50;
    static final int BUTTON_GAP    = 20;

    static final int TRYAGAIN_X = WIDTH / 2 - 100;
    static final int TRYAGAIN_Y = HEIGHT / 2 + 80;
    static final int TRYAGAIN_WIDTH = 200;
    static final int TRYAGAIN_HEIGHT = 50;


    // --- Health ---
    static final int MAX_HEALTH = 25;

    // --- Game state enum ---
    enum GameState { MENU, PLAYING, PAUSED, GAME_OVER }
    private GameState gameState = GameState.MENU;

    // ---------------------------------------------------------------
    // Abstract Unit base class
    // ---------------------------------------------------------------
    static abstract class Unit implements Drawable {
        double x, y;
        boolean isPlayer;
        double speed;
        double maxHealth;
        double health;
        double damage;
        double attackRange;
        boolean fighting;

        Unit(double x, double y, boolean isPlayer) {
            this.x = x;
            this.y = y;
            this.isPlayer = isPlayer;
        }

        public abstract void draw(GraphicsContext gc);

        

        void update(double deltaSeconds) {
            if(!fighting){
                x += (isPlayer ? 1 : -1) * speed * deltaSeconds;
            }
        }

        boolean hasReachedEnemyBase() {
            if (isPlayer) return x + UNIT_SIZE >= RIGHT_BASE_X;
            else          return x <= LEFT_BASE_X + BASE_WIDTH;
        }
    }

    // ---------------------------------------------------------------
    // Soldier — rask, lett tropp
    // ---------------------------------------------------------------
    static class Soldier extends Unit {
        Soldier(double x, double y, boolean isPlayer) {
            super(x, y, isPlayer);
            this.speed = 60.0;
            this.health = 30.0;
            this.maxHealth = 30.0;
            this.damage = 10.0;
            this.attackRange = 40.0;
        }

        @Override
        public void draw(GraphicsContext gc) {
            Color body    = isPlayer ? Color.CORNFLOWERBLUE : Color.TOMATO;
            Color outline = isPlayer ? Color.DARKBLUE       : Color.DARKRED;

            gc.setFill(body);
            gc.fillRect(x, y - UNIT_SIZE, UNIT_SIZE, UNIT_SIZE);
            gc.setStroke(outline);
            gc.setLineWidth(2);
            gc.strokeRect(x, y - UNIT_SIZE, UNIT_SIZE, UNIT_SIZE);

            double headR = UNIT_SIZE * 0.35;
            double headX = x + UNIT_SIZE / 2.0 - headR;
            double headY = y - UNIT_SIZE - headR * 2 - 2;
            gc.setFill(body.brighter());
            gc.fillOval(headX, headY, headR * 2, headR * 2);
            gc.strokeOval(headX, headY, headR * 2, headR * 2);

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
    // Knight — treg, bulky tropp
    // ---------------------------------------------------------------
    static class Knight extends Unit {
        Knight(double x, double y, boolean isPlayer) {
            super(x, y, isPlayer);
            this.speed = 35.0;
            this.health = 80.0;
            this.maxHealth = 80.0;
            this.damage = 15.0;
            this.attackRange = 40.0;

        }

        @Override
        public void draw(GraphicsContext gc) {
            Color body    = isPlayer ? Color.DARKSLATEBLUE : Color.DARKRED;
            Color outline = isPlayer ? Color.MIDNIGHTBLUE  : Color.MAROON;

            gc.setFill(body);
            gc.fillRect(x, y - 38, 34, 38);
            gc.setStroke(outline);
            gc.setLineWidth(2);
            gc.strokeRect(x, y - 38, 34, 38);

            gc.setFill(body.brighter());
            gc.fillOval(x + 5, y - 52, 24, 20);
            gc.strokeOval(x + 5, y - 52, 24, 20);

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
    // Archer — medium fart, lett tropp
    // ---------------------------------------------------------------
    static class Archer extends Unit {
        Archer(double x, double y, boolean isPlayer) {
            super(x, y, isPlayer);
            this.speed = 50.0;
            this.health = 20.0;
            this.maxHealth = 20.0;
            this.damage = 8.0;
            this.attackRange = 120.0;

        }

        @Override
        public void draw(GraphicsContext gc) {
            Color body    = isPlayer ? Color.CYAN      : Color.ORANGE;
            Color outline = isPlayer ? Color.DARKCYAN  : Color.DARKORANGE;

            gc.setFill(body);
            gc.fillRect(x, y - ARCHER_UNIT_SIZE, ARCHER_UNIT_SIZE, ARCHER_UNIT_SIZE);
            gc.setStroke(outline);
            gc.setLineWidth(2);
            gc.strokeRect(x, y - ARCHER_UNIT_SIZE, ARCHER_UNIT_SIZE, ARCHER_UNIT_SIZE);

            double headR = ARCHER_UNIT_SIZE * 0.35;
            double headX = x + ARCHER_UNIT_SIZE / 2.0 - headR;
            double headY = y - ARCHER_UNIT_SIZE - headR * 2 - 2;
            gc.setFill(body.brighter());
            gc.fillOval(headX, headY, headR * 2, headR * 2);
            gc.strokeOval(headX, headY, headR * 2, headR * 2);
        }
    }

    // ---------------------------------------------------------------
    // Game state fields
    // ---------------------------------------------------------------
    private final List<Unit> units = new ArrayList<>();

    private long lastSoldierSpawn = 0;
    private long lastArcherSpawn  = 0;
    private long lastKnightSpawn  = 0;
    private long lastEnemySpawn   = 0;

    private int playerScore = 0;
    private int enemyScore  = 0;

    private int playerHealth = MAX_HEALTH;
    private int enemyHealth  = MAX_HEALTH;

    private final InputHandler input = new InputHandler();
    private final FileHandler fileHandler = new FileHandler();

    // ---------------------------------------------------------------
    // JavaFX entry point
    // ---------------------------------------------------------------
    @Override
    public void start(Stage stage) {
        Canvas canvas = new Canvas(WIDTH, HEIGHT);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        StackPane root = new StackPane(canvas);
        Scene scene = new Scene(root, WIDTH, HEIGHT);

        scene.setOnKeyPressed(e -> input.handleKeyPressed(e.getCode()));
        scene.setOnKeyReleased(e -> input.handleKeyReleased(e.getCode()));

        canvas.setOnMouseClicked(e -> {
            if (gameState == GameState.MENU) {
                if (e.getX() > BUTTON_X && e.getX() < BUTTON_X + BUTTON_WIDTH &&
                    e.getY() > BUTTON_Y && e.getY() < BUTTON_Y + BUTTON_HEIGHT) {
                    gameState = GameState.PLAYING;
                }
                if (e.getX() > BUTTON_X && e.getX() < BUTTON_X + BUTTON_WIDTH &&
                    e.getY() > BUTTON_Y + BUTTON_HEIGHT + BUTTON_GAP &&
                    e.getY() < BUTTON_Y + BUTTON_HEIGHT + BUTTON_GAP + BUTTON_HEIGHT){
                        loadGame();
                        gameState = GameState.PLAYING;
                    }
            }

            if (gameState == GameState.GAME_OVER) {
                if (e.getX() > TRYAGAIN_X && e.getX() < TRYAGAIN_X + TRYAGAIN_WIDTH &&
                    e.getY() > TRYAGAIN_Y && e.getY() < TRYAGAIN_Y + TRYAGAIN_HEIGHT) {
                    resetGame();
                }
            }
            
        });

        AnimationTimer timer = new AnimationTimer() {

            @Override
            public void handle(long now) {
                if (lastTime == 0) { lastTime = now; return; }
                double delta = (now - lastTime) / 1_000_000_000.0;
                // ...Feilhåndtering ved lag, capper fps og sakker ned koden, for å forhindre at units teleporterer...
                delta = Math.min(delta, 0.05);
                // ...Feilhåndtering ved lag, capper fps og sakker ned koden, for å forhindre at units teleporterer...
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
    // Reset
    // ---------------------------------------------------------------
    private void resetGame() {
        units.clear();
        playerHealth  = MAX_HEALTH;
        enemyHealth   = MAX_HEALTH;
        playerScore   = 0;
        enemyScore    = 0;
        lastSoldierSpawn = 0;
        lastArcherSpawn  = 0;
        lastKnightSpawn  = 0;
        lastEnemySpawn   = 0;
        gameState = GameState.PLAYING;
    }

    // ---------------------------------------------------------------
    // Update
    // ---------------------------------------------------------------
    private void update(long now, double delta) {

        if (input.pausePressed){
                input.pausePressed = false;
                if (gameState == GameState.PLAYING) gameState = GameState.PAUSED;
                else if (gameState == GameState.PAUSED) gameState = GameState.PLAYING;
            }

        if (gameState == GameState.PLAYING) {

            // Player spawning
        
            if (input.spawnSoldier && (now - lastSoldierSpawn) >= SOLDIER_COOLDOWN_NS) {
                spawnUnit(true, "soldier");
                lastSoldierSpawn = now;
            }
            if (input.spawnArcher && (now - lastArcherSpawn) >= ARCHER_COOLDOWN_NS) {
                spawnUnit(true, "archer");
                lastArcherSpawn = now;
            }
            if (input.spawnKnight && (now - lastKnightSpawn) >= KNIGHT_COOLDOWN_NS) {
                spawnUnit(true, "knight");
                lastKnightSpawn = now;
            }
            

            // Enemy auto-spawning
            if ((now - lastEnemySpawn) >= ENEMY_COOLDOWN_NS) {
                double roll = Math.random();
                String type = roll < 0.33 ? "soldier" : roll < 0.66 ? "archer" : "knight";
                spawnUnit(false, type);
                lastEnemySpawn = now;
            }

            // Move units and check base collisions
            Iterator<Unit> it = units.iterator();
            while (it.hasNext()) {
                Unit u = it.next();
                u.update(delta);
                if (u.health <= 0){
                    if (u.isPlayer) enemyScore++;
                    else playerScore++;
                    it.remove();
                    continue;
                }
                if (u.hasReachedEnemyBase()) {
                    if (u.isPlayer) enemyHealth--;
                    else            playerHealth--;
                    it.remove();
                }
            }
            
            if (input.save) {
                    saveGame();
                    input.save = false;
                }
            // ...COLLISION...
            for (Unit u : units){
                u.fighting = false;
                for (Unit other : units){
                    if (other.isPlayer != u.isPlayer){
                        double distance = Math.abs(u.x - other.x);
                        if (distance < u.attackRange){
                            u.fighting = true;
                            other.health -= u.damage * delta;
                        }

                    }
                }
            }
            // ... Feilhåndtering dersom health går under null før Game over sjekkes
            playerHealth = Math.max(playerHealth, 0);
            enemyHealth = Math.max(enemyHealth, 0);
            // ... -||- ...

            // Check for game over
            if (playerHealth <= 0 || enemyHealth <= 0) {
                gameState = GameState.GAME_OVER;
            }
        }
    }

    private void spawnUnit(boolean isPlayer, String type) {
        double spawnX = isPlayer
                ? LEFT_BASE_X + BASE_WIDTH + 4
                : RIGHT_BASE_X - UNIT_SIZE - 4;

        Unit u = switch (type) {
            case "archer" -> new Archer(spawnX, GROUND_Y, isPlayer);
            case "knight" -> new Knight(spawnX, GROUND_Y, isPlayer);
            default       -> new Soldier(spawnX, GROUND_Y, isPlayer);
        };
        units.add(u);
    }

    // ---------------------------------------------------------------
    // Render
    // ---------------------------------------------------------------
    private void render(GraphicsContext gc) {
        if (gameState == GameState.MENU) {
            renderMenu(gc);
        } else if (gameState == GameState.PLAYING) {
            renderGame(gc);
        } else if (gameState == GameState.GAME_OVER) {
            renderGameOver(gc);
        }else if (gameState == GameState.PAUSED){
            renderPaused(gc);
        }
    }

    private void drawCooldownBars(GraphicsContext gc, long now) {
        int barWidth  = 120;
        int barHeight = 16;
        int startX    = 20;
        int startY    = 55;
        int gap       = 24;

        String[] labels    = {"Q: Soldier", "W: Archer", "E: Knight"};
        long[]   cooldowns = {SOLDIER_COOLDOWN_NS, ARCHER_COOLDOWN_NS, KNIGHT_COOLDOWN_NS};
        long[]   lastSpawn = {lastSoldierSpawn, lastArcherSpawn, lastKnightSpawn};
        Color[]  colors    = {Color.CORNFLOWERBLUE, Color.CYAN, Color.DARKSLATEBLUE};

        for (int i = 0; i < 3; i++) {
            long elapsed  = now - lastSpawn[i];
            double filled = Math.min((double) elapsed / cooldowns[i], 1.0);
            boolean ready = filled >= 1.0;

            // Background
            gc.setFill(Color.color(0, 0, 0, 0.5));
            gc.fillRect(startX, startY + i * gap, barWidth, barHeight);

            // Fill
            gc.setFill(ready ? Color.LIMEGREEN : colors[i]);
            gc.fillRect(startX, startY + i * gap, barWidth * filled, barHeight);

            // Border
            gc.setStroke(Color.WHITE);
            gc.setLineWidth(1);
            gc.strokeRect(startX, startY + i * gap, barWidth, barHeight);

            // Label
            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("Arial", FontWeight.BOLD, 11));
            gc.fillText(ready ? labels[i] + " READY" : labels[i], startX + 4, startY + i * gap + 12);
        }
    }

    private void renderGame(GraphicsContext gc) {
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

        drawHealthBar(gc, LEFT_BASE_X,  playerHealth);
        drawHealthBar(gc, RIGHT_BASE_X, enemyHealth);

        for (Unit u : units) u.draw(gc);

        drawHUD(gc);
        drawCooldownBars(gc, lastTime);

        // ...Saved popup...
        if (System.nanoTime() - LastSaveTime < 2_000_000_000L){
            gc.setFill(Color.web("#00000088"));
            gc.fillRoundRect(WIDTH / 2 - 80, 60, 160, 36, 8, 8);
            gc.setFill(Color.LIMEGREEN);
            gc.setFont(Font.font("Arial", FontWeight.BOLD, 16 ));
            gc.fillText("Game saved!", WIDTH / 2.0 - 48,84);
        }
    }

    private void renderMenu(GraphicsContext gc) {
        gc.setFill(Color.web("#3167e4"));
        gc.fillRect(0, 0, WIDTH, HEIGHT);

        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 62));
        gc.fillText("Age of Warish", WIDTH / 2.0 - 180, HEIGHT / 2.0 - 80);

        // New Game button
        gc.setFill(Color.DARKGRAY);
        gc.fillRoundRect(BUTTON_X, BUTTON_Y, BUTTON_WIDTH, BUTTON_HEIGHT, 10, 10);
        drawCentredText(gc, "New Game", BUTTON_X, BUTTON_Y, BUTTON_WIDTH, BUTTON_HEIGHT, 20, Color.WHITE);

        // Continue button (greyed out)
        gc.setFill(Color.GRAY);
        gc.fillRoundRect(BUTTON_X, BUTTON_Y + BUTTON_HEIGHT + BUTTON_GAP, BUTTON_WIDTH, BUTTON_HEIGHT, 10, 10);
        drawCentredText(gc, "Continue", BUTTON_X, BUTTON_Y + BUTTON_HEIGHT + BUTTON_GAP, BUTTON_WIDTH, BUTTON_HEIGHT, 20, Color.LIGHTGRAY);
    }

    private void renderGameOver(GraphicsContext gc) {
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, WIDTH, HEIGHT);

        gc.setFill(Color.RED);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 62));
        gc.fillText("GAME OVER", WIDTH / 2.0 - 180, HEIGHT / 2.0 - 60);

        String result = playerHealth <= 0 ? "You Lose!" : "You Win!";
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 32));
        Text t = new Text(result);
        t.setFont(gc.getFont());
        double tw = t.getLayoutBounds().getWidth();
        gc.fillText(result, WIDTH / 2.0 - tw / 2, HEIGHT / 2.0 + 20);

        // Try Again button
        gc.setFill(Color.DARKGRAY);
        gc.fillRoundRect(TRYAGAIN_X, TRYAGAIN_Y, TRYAGAIN_WIDTH, TRYAGAIN_HEIGHT, 10, 10);
        drawCentredText(gc, "Try Again", TRYAGAIN_X, TRYAGAIN_Y, TRYAGAIN_WIDTH, TRYAGAIN_HEIGHT, 20, Color.WHITE);
    }
    // ...Paused Game...
    private void renderPaused(GraphicsContext gc){
        renderGame(gc);

        gc.setFill(Color.color(0, 0, 0, 0.5));
        gc.fillRect(0, 0, WIDTH, HEIGHT);

        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 62));
        gc.fillText("PAUSED", WIDTH / 2.0 - 130, HEIGHT / 2.0 - 20);

        gc.setFill(Color.LIGHTGRAY);
        gc.setFont(Font.font("Arial", FontWeight.NORMAL, 18));
        gc.fillText("Press ESCAPE to resume", WIDTH / 2.0 - 80, HEIGHT / 2.0 + 30);

    }

    

    // Helper to draw centred text inside a button rectangle
    private void drawCentredText(GraphicsContext gc, String text, int bx, int by, int bw, int bh, int fontSize, Color colour) {
        gc.setFont(Font.font("Arial", FontWeight.BOLD, fontSize));
        Text helper = new Text(text);
        helper.setFont(gc.getFont());
        double tw = helper.getLayoutBounds().getWidth();
        double th = helper.getLayoutBounds().getHeight();
        gc.setFill(colour);
        gc.fillText(text, bx + (bw - tw) / 2, by + (bh + th) / 2 - 4);
    }

    // ---------------------------------------------------------------
    // Drawing helpers
    // ---------------------------------------------------------------
    private void drawHealthBar(GraphicsContext gc, int x, int health) {
        int barWidth  = 120;
        int barHeight = 14;
        int barY      = GROUND_Y - BASE_HEIGHT - 30;

        gc.setFill(Color.web("#3a3a3a"));
        gc.fillRect(x, barY, barWidth, barHeight);

        double fillWidth = ((double) health / MAX_HEALTH) * barWidth;
        gc.setFill(Color.LIMEGREEN);
        gc.fillRect(x, barY, fillWidth, barHeight);

        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1);
        gc.strokeRect(x, barY, barWidth, barHeight);

        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 10));
        gc.fillText(health + "/" + MAX_HEALTH, x + 30, barY + 11);
    }

    private void drawBase(GraphicsContext gc, int x, int y, boolean isPlayer) {
        Color stone     = Color.web("#8B7355");
        Color darkStone = Color.web("#5C4033");
        Color wood      = Color.web("#8B4513");

        gc.setFill(stone);
        gc.fillRect(x, y, BASE_WIDTH, BASE_HEIGHT);
        gc.setStroke(darkStone);
        gc.setLineWidth(2);
        gc.strokeRect(x, y, BASE_WIDTH, BASE_HEIGHT);

        gc.setStroke(darkStone);
        gc.setLineWidth(1);
        gc.strokeLine(x + 10, y + 30, x + BASE_WIDTH - 10, y + 30);
        gc.strokeLine(x + 5,  y + 60, x + BASE_WIDTH - 5,  y + 60);
        gc.strokeLine(x + 10, y + 90, x + BASE_WIDTH - 10, y + 90);

        gc.setFill(wood);
        for (int i = 0; i < 5; i++) {
            int stakeX = x + 8 + i * 16;
            gc.fillRect(stakeX, y - 20, 8, 22);
            gc.fillPolygon(
                new double[]{stakeX, stakeX + 4, stakeX + 8},
                new double[]{y - 20,  y - 32,     y - 20}, 3
            );
        }

        gc.setFill(Color.web("#2b1a0e"));
        gc.fillOval(x + BASE_WIDTH / 2 - 14, y + BASE_HEIGHT - 40, 28, 38);

        int poleX = isPlayer ? x + BASE_WIDTH - 8 : x + 8;
        gc.setStroke(wood);
        gc.setLineWidth(3);
        gc.strokeLine(poleX, y - 20, poleX + 3, y - 50);

        int flagDir = isPlayer ? -1 : 1;
        gc.setFill(Color.web("#F5F5DC"));
        gc.fillOval(poleX + flagDir * 2, y - 54, 10, 8);
        gc.fillOval(poleX + flagDir * 2, y - 38, 10, 8);
        gc.fillRect(poleX + flagDir * 4, y - 50, 5, 16);

        gc.setFill(Color.web("#F5F5DC"));
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
        gc.fillText("KILLS: " + playerScore, 100, 28);

        gc.setFill(Color.TOMATO);
        gc.fillText("ENEMY", WIDTH - 200, 28);
        gc.setFill(Color.WHITE);
        gc.fillText("KILLS: " + enemyScore, WIDTH - 120, 28);

        gc.setFill(Color.web("#ffe066"));
        gc.setFont(Font.font("Arial", FontWeight.NORMAL, 13));
        gc.fillText("Q: Soldier  W: Archer  E: Knight  S: Save", WIDTH / 2.0 - 115, 28);    }

    private void saveGame() {
        fileHandler.save(playerHealth, enemyHealth, playerScore, enemyScore);
        LastSaveTime = System.nanoTime();
    }

    private void loadGame() {
        int[] values = fileHandler.load();
        playerHealth = values[0];
        enemyHealth  = values[1];
        playerScore  = values[2];
        enemyScore   = values[3];
    }

    // ---------------------------------------------------------------
    public static void main(String[] args) {
        launch(args);
    }
}