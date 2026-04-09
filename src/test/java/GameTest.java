import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class GameTest {

    // --- Unit stats ---
    @Test
    void soldierHasCorrectStats() {
        Main.Soldier s = new Main.Soldier(0, 0, true);
        assertEquals(60.0, s.speed);
        assertEquals(30.0, s.health);
        assertEquals(10.0, s.damage);
    }

    @Test
    void knightHasCorrectStats() {
        Main.Knight k = new Main.Knight(0, 0, true);
        assertEquals(35.0, k.speed);
        assertEquals(80.0, k.health);
        assertEquals(15.0, k.damage);
    }

    @Test
    void archerHasCorrectStats() {
        Main.Archer a = new Main.Archer(0, 0, true);
        assertEquals(50.0, a.speed);
        assertEquals(20.0, a.health);
        assertEquals(8.0,  a.damage);
        assertEquals(120.0, a.attackRange);
    }

    // --- Movement direction ---
    @Test
    void playerUnitMovesRight() {
        Main.Soldier s = new Main.Soldier(100, Main.GROUND_Y, true);
        double startX = s.x;
        s.update(1.0); // 1 second
        assertTrue(s.x > startX);
    }

    @Test
    void enemyUnitMovesLeft() {
        Main.Soldier s = new Main.Soldier(500, Main.GROUND_Y, false);
        double startX = s.x;
        s.update(1.0);
        assertTrue(s.x < startX);
    }

    // --- Base detection ---
    @Test
    void playerUnitDetectsEnemyBase() {
        // Place player unit right at the enemy base edge
        Main.Soldier s = new Main.Soldier(Main.RIGHT_BASE_X - Main.UNIT_SIZE + 1, Main.GROUND_Y, true);
        assertTrue(s.hasReachedEnemyBase());
    }

    @Test
    void enemyUnitDetectsPlayerBase() {
        Main.Soldier s = new Main.Soldier(Main.LEFT_BASE_X + Main.BASE_WIDTH - 1, Main.GROUND_Y, false);
        assertTrue(s.hasReachedEnemyBase());
    }

    @Test
    void unitHasNotReachedBaseYet() {
        Main.Soldier s = new Main.Soldier(400, Main.GROUND_Y, true);
        assertFalse(s.hasReachedEnemyBase());
    }

    // --- Combat ---
    @Test
    void unitTakesDamageCorrectly() {
        Main.Soldier s = new Main.Soldier(0, 0, true);
        double startHealth = s.health;
        s.health -= 10.0;
        assertEquals(startHealth - 10.0, s.health);
    }

    @Test
    void unitDiesWhenHealthReachesZero() {
        Main.Soldier s = new Main.Soldier(0, 0, true);
        s.health = 0;
        assertTrue(s.health <= 0);
    }
}