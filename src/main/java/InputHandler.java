import javafx.scene.input.KeyCode;

public class InputHandler {
    public boolean spawnSoldier = false;
    public boolean spawnArcher  = false;
    public boolean spawnKnight  = false;

    public void handleKeyPressed(KeyCode code) {
        if (code == KeyCode.Q) spawnSoldier = true;
        if (code == KeyCode.W) spawnArcher  = true;
        if (code == KeyCode.E) spawnKnight  = true;
    }

    public void handleKeyReleased(KeyCode code) {
        if (code == KeyCode.Q) spawnSoldier = false;
        if (code == KeyCode.W) spawnArcher  = false;
        if (code == KeyCode.E) spawnKnight  = false;
    }
}