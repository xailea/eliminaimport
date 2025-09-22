package app.ui;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.io.File;
import java.nio.file.Paths;

public class JavaScriptAnalyzerApp {
    private JFrame frame;
    private JTextArea log;
    private JTextField rootField;
    private JButton startBtn, browseBtn, backBtn;

    public JavaScriptAnalyzerApp(){
        frame = new JFrame("JavaScript Analyzer");
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.setSize(920, 560);
        frame.setLayout(new BorderLayout(8,8));

        backBtn = new JButton("← Indietro");
        backBtn.addActionListener(e -> { frame.dispose(); Launcher.open(); });
        var header = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        header.add(backBtn);

        rootField = new JTextField(Paths.get(".").toAbsolutePath().normalize().toString());
        browseBtn = new JButton("Seleziona Radice");
        var grid = new JPanel(new GridBagLayout());
        var c = new GridBagConstraints();
        c.insets = new Insets(4,4,4,4);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx=0; c.gridy=0; grid.add(new JLabel("Cartella radice (app):"), c);
        c.gridx=1; c.weightx=1; grid.add(rootField, c);
        c.gridx=2; c.weightx=0; grid.add(browseBtn, c);

        var top = new JPanel(new BorderLayout(8,8));
        top.add(header, BorderLayout.NORTH);
        top.add(grid,   BorderLayout.CENTER);

        log = new JTextArea(); log.setEditable(false);
        var center = new JPanel(new BorderLayout(8,8));
        center.setBorder(BorderFactory.createTitledBorder("Log"));
        center.add(new JScrollPane(log), BorderLayout.CENTER);

        startBtn  = new JButton("Avvia");

        frame.add(top, BorderLayout.NORTH);
        frame.add(center, BorderLayout.CENTER);
        frame.add(startBtn, BorderLayout.SOUTH);

        final Color neonMag = new Color(0xff00ff);
        final Color neonCyn = new Color(0x00e5ff);
        final Color panelBg = CyberUI.isLight() ? new Color(0xf5f7f8) : new Color(0x12122b);
        backBtn.setBorder(new LineBorder(neonMag, 1, true));
        browseBtn.setBorder(new LineBorder(neonMag, 1, true));
        startBtn.setBorder(new LineBorder(neonCyn, 2, true));
        top.setBackground(panelBg);
        grid.setBackground(panelBg);
        center.setBackground(panelBg);
        frame.getContentPane().setBackground(panelBg);

        browseBtn.addActionListener(e -> pickFolder(rootField, "Seleziona cartella radice"));
        startBtn.addActionListener(e -> log.append("TODO: implementare analisi JavaScript.\n"));

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void pickFolder(JTextField target, String title) {
        var startDir = new File(target.getText());
        var chooser = new JFileChooser(startDir.exists() ? startDir : new File(System.getProperty("user.home")));
        chooser.setDialogTitle(title);
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setMultiSelectionEnabled(false);
        chooser.setAcceptAllFileFilterUsed(false);
        if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            var dir = chooser.getSelectedFile().toPath().toAbsolutePath().normalize();
            target.setText(dir.toString());
        }
    }
}
