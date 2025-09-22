package app.ui;

import com.formdev.flatlaf.FlatDarkLaf;
import javax.swing.*;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;
import java.awt.*;

public final class CyberUI {
    private CyberUI(){}

    public static void install() {
        FlatDarkLaf.setup();

        var bg      = new ColorUIResource(0x0d0b1e);
        var panel   = new ColorUIResource(0x12122b);
        var text    = new ColorUIResource(0xe6e6e6);
        var neonMag = new ColorUIResource(0xff00ff);
        var neonCyn = new ColorUIResource(0x00e5ff);
        var mono    = new FontUIResource(new Font("Consolas", Font.PLAIN, 13));

        UIManager.put("defaultFont", mono);
        UIManager.put("Panel.background", panel);
        UIManager.put("Label.foreground", text);
        UIManager.put("Component.focusColor", neonMag);
        UIManager.put("Component.innerFocusWidth", 1);
        UIManager.put("Component.arc", 14);
        UIManager.put("Button.arc", 18);
        UIManager.put("TextComponent.arc", 12);

        UIManager.put("Button.background", bg);
        UIManager.put("Button.foreground", text);
        UIManager.put("Button.focusedBorderColor", neonCyn);
        UIManager.put("Button.hoverBorderColor", neonMag);

        UIManager.put("ScrollBar.thumb", new ColorUIResource(0x0f2a35));
        UIManager.put("ScrollBar.track", bg);

        UIManager.put("ProgressBar.foreground", neonCyn);
        UIManager.put("ProgressBar.background", bg);

        UIManager.put("TextComponent.caretForeground", neonMag);
        UIManager.put("TextComponent.selectionBackground", new ColorUIResource(0x4a004a));
        UIManager.put("TextComponent.background", bg);
        UIManager.put("TextComponent.foreground", text);
    }
}
