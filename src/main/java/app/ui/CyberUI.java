package app.ui;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;
import java.awt.*;

public final class CyberUI {
    private static boolean LIGHT = false; // stato corrente

    private CyberUI(){}

    public static boolean isLight(){ return LIGHT; }
    public static void setLight(boolean light){ LIGHT = light; install(); }

    public static void install() { if (LIGHT) installLight(); else installDark(); }

    public static void installDark() {
        FlatDarkLaf.setup();
        var bg      = new ColorUIResource(0x0d0b1e);
        var panel   = new ColorUIResource(0x12122b);
        var text    = new ColorUIResource(0xe6e6e6);
        var neonMag = new ColorUIResource(0xff00ff);
        var neonCyn = new ColorUIResource(0x00e5ff);
        var mono    = new FontUIResource(new Font("Consolas", Font.PLAIN, 13));
        base(mono, panel, text);

        UIManager.put("Button.background", bg);
        UIManager.put("Button.foreground", text);
        UIManager.put("Button.focusedBorderColor", neonCyn);
        UIManager.put("Button.hoverBorderColor", neonMag);
        UIManager.put("Component.focusColor", neonMag);
        UIManager.put("ScrollBar.thumb", new ColorUIResource(0x0f2a35));
        UIManager.put("ScrollBar.track", bg);
        UIManager.put("ProgressBar.foreground", neonCyn);
        UIManager.put("ProgressBar.background", bg);
        UIManager.put("TextComponent.caretForeground", neonMag);
        UIManager.put("TextComponent.selectionBackground", new ColorUIResource(0x4a004a));
        UIManager.put("TextComponent.background", bg);
        UIManager.put("TextComponent.foreground", text);
    }

    public static void installLight() {
        FlatLightLaf.setup();
        var bg      = new ColorUIResource(0xffffff);
        var panel   = new ColorUIResource(0xf5f7f8);
        var text    = new ColorUIResource(0x101010);
        var neonGrn = new ColorUIResource(0x00ff66);
        var mono    = new FontUIResource(new Font("Consolas", Font.PLAIN, 13));
        base(mono, panel, text);

        UIManager.put("Button.background", bg);
        UIManager.put("Button.foreground", text);
        UIManager.put("Button.focusedBorderColor", neonGrn);
        UIManager.put("Button.hoverBorderColor", neonGrn);
        UIManager.put("Component.focusColor", neonGrn);
        UIManager.put("ScrollBar.thumb", new ColorUIResource(0xcfead8));
        UIManager.put("ScrollBar.track", bg);
        UIManager.put("ProgressBar.foreground", neonGrn);
        UIManager.put("ProgressBar.background", new ColorUIResource(0xe9ecef));
        UIManager.put("TextComponent.caretForeground", neonGrn);
        UIManager.put("TextComponent.selectionBackground", new ColorUIResource(0xc9ffe6));
        UIManager.put("TextComponent.background", bg);
        UIManager.put("TextComponent.foreground", text);
    }

    private static void base(FontUIResource mono, ColorUIResource panel, ColorUIResource text){
        UIManager.put("defaultFont", mono);
        UIManager.put("Panel.background", panel);
        UIManager.put("Label.foreground", text);
        UIManager.put("Component.innerFocusWidth", 1);
        UIManager.put("Component.arc", 14);
        UIManager.put("Button.arc", 18);
        UIManager.put("TextComponent.arc", 12);
    }
}
