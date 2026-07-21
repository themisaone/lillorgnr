package no.companyfetcher.gui;

import javax.swing.UIManager;
import java.awt.Font;

final class GuiDisplayConfig {

    private GuiDisplayConfig() {
    }

    static void applyBeforeGuiStartup() {
        if (!isWindows()) {
            return;
        }
        if (System.getProperty("sun.java2d.uiScale") == null) {
            System.setProperty("sun.java2d.uiScale.enabled", "true");
        }
    }

    static void applyUiFonts() {
        if (!isWindows()) {
            return;
        }

        float minSize = 14f;
        for (String key : new String[]{
                "Button.font",
                "Label.font",
                "RadioButton.font",
                "ComboBox.font",
                "TitledBorder.font"
        }) {
            Font font = UIManager.getFont(key);
            if (font != null) {
                UIManager.put(key, font.deriveFont(Math.max(font.getSize2D() + 4f, minSize)));
            }
        }
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }
}
