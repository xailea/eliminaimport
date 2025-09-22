package app.ui;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;

public class Launcher {
    private JFrame frame;

    public static void main(String[] args){ open(); }

    public static void open(){
        try { CyberUI.install(); } catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new Launcher().show());
    }

    private void show(){
        frame = new JFrame("Le verità scientifiche non si decidono a maggioranza");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setSize(680, 380);
        frame.setLayout(new BorderLayout(8,8));

        var top = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 8));
        var themeToggle = new JToggleButton("Tema chiaro/scuro");
        themeToggle.setSelected(CyberUI.isLight());
        themeToggle.addActionListener(e -> {
            CyberUI.setLight(themeToggle.isSelected());
            frame.dispose();
            Launcher.open();
        });
        themeToggle.setBorder(new LineBorder(new Color(0x00ff66), 2, true));
        top.add(themeToggle);
        frame.add(top, BorderLayout.NORTH);

        var panel = new JPanel(new GridBagLayout());
        var c = new GridBagConstraints();
        c.insets = new Insets(12,12,12,12);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0; c.gridy = 0;

        var btnInclude = new JButton("Analizza include");
        var btnJs      = new JButton("Analizza JavaScript");

        final Color neonMag = new Color(0xff00ff);
        final Color neonCyn = new Color(0x00e5ff);
        final Font monoBold = new Font("Consolas", Font.BOLD, 16);
        btnInclude.setFont(monoBold);
        btnJs.setFont(monoBold);
        btnInclude.setBorder(new LineBorder(neonMag, 2, true));
        btnJs.setBorder(new LineBorder(neonCyn, 2, true));
        btnInclude.setPreferredSize(new Dimension(340, 68));
        btnJs.setPreferredSize(new Dimension(340, 68));

        panel.add(btnInclude, c);
        c.gridy = 1;
        panel.add(btnJs, c);

        frame.add(panel, BorderLayout.CENTER);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        btnInclude.addActionListener(e -> { new App(); frame.dispose(); });
        btnJs.addActionListener(e -> { new JavaScriptAnalyzerApp(); frame.dispose(); });
    }
}
