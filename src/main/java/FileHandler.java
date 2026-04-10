import java.io.*;

public class FileHandler {

    private static final String SAVE_FILE = "save.txt";

    public void save(int playerHealth, int enemyHealth, int playerScore, int enemyScore) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(SAVE_FILE));
            writer.write("playerHealth=" + playerHealth); writer.newLine();
            writer.write("enemyHealth="  + enemyHealth);  writer.newLine();
            writer.write("playerScore="  + playerScore);  writer.newLine();
            writer.write("enemyScore="   + enemyScore);   writer.newLine();
            writer.close();
        } catch (Exception e) {
            System.out.println("Could not save: " + e.getMessage());
        }
    }

    public int[] load() {
        int[] values = {25, 25, 0, 0}; // default verdier

        File saveFile = new File(SAVE_FILE);
        if (!saveFile.exists()) {
            System.out.println("No save file found!");
            return values;
        }

        try {
            BufferedReader reader = new BufferedReader(new FileReader(SAVE_FILE));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("=");
                switch (parts[0]) {
                    case "playerHealth" -> values[0] = Integer.parseInt(parts[1]);
                    case "enemyHealth"  -> values[1] = Integer.parseInt(parts[1]);
                    case "playerScore"  -> values[2] = Integer.parseInt(parts[1]);
                    case "enemyScore"   -> values[3] = Integer.parseInt(parts[1]);
                }
            }
            reader.close();
        } catch (Exception e) {
            System.out.println("Could not load: " + e.getMessage());
        }
        return values;
    }
}