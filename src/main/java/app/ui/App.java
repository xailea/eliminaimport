package app.ui;

import app.core.IncludeUsageService;
import app.core.Refactorer;
import app.core.Report;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.io.File;
import java.nio.file.Paths;

public class App {
    private JFrame frame;
    private JButton startBtn, browseAppBtn, browseIncBtn, backBtn;
    private JProgressBar bar;
    private JTextArea log;
    private JTextField appField, incField;
    private JCheckBox applyMoves;

    public App(){
        frame = new JFrame("Include Analyzer");
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.setSize(920, 560);

        backBtn = new JButton("← Indietro");
        backBtn.addActionListener(e -> { frame.dispose(); Launcher.open(); });
        var header = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        header.add(backBtn);

        appField = new JTextField(Paths.get(".").toAbsolutePath().normalize().toString());
        incField = new JTextField("");
        browseAppBtn = new JButton("Seleziona App");
        browseIncBtn = new JButton("Seleziona Includes");
        startBtn = new JButton("Avvia");
        applyMoves = new JCheckBox("Sposta include NON usati in 'include inutilizzati/'", true);
        bar = new JProgressBar(); bar.setIndeterminate(false);
        log = new JTextArea(); log.setEditable(false); log.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        browseAppBtn.addActionListener(e -> pickFolder(appField, "Seleziona la cartella 'app'"));
        browseIncBtn.addActionListener(e -> pickFolder(incField, "Seleziona la cartella degli include"));
        startBtn.addActionListener(e -> runScan());

        // griglia percorsi
        var grid = new JPanel(new GridBagLayout());
        var c = new GridBagConstraints(); c.insets = new Insets(4,4,4,4); c.fill = GridBagConstraints.HORIZONTAL;

        c.gridx=0; c.gridy=0; grid.add(new JLabel("Cartella App:"), c);
        c.gridx=1; c.weightx=1; grid.add(appField, c);
        c.gridx=2; c.weightx=0; grid.add(browseAppBtn, c);

        c.gridx=0; c.gridy=1; c.weightx=0; grid.add(new JLabel("Cartella Includes:"), c);
        c.gridx=1; c.weightx=1; grid.add(incField, c);
        c.gridx=2; c.weightx=0; grid.add(browseIncBtn, c);

        c.gridx=1; c.gridy=2; c.gridwidth=2; grid.add(applyMoves, c);

        var top = new JPanel(new BorderLayout(8,8));
        top.add(header, BorderLayout.NORTH);
        top.add(grid,   BorderLayout.CENTER);

        var center = new JPanel(new BorderLayout(8,8));
        center.add(new JScrollPane(log), BorderLayout.CENTER);
        center.add(bar, BorderLayout.SOUTH);

        final Color neonMag = new Color(0xff00ff);
        final Color neonCyn = new Color(0x00e5ff);
        final Color panelBg = CyberUI.isLight() ? new Color(0xf5f7f8) : new Color(0x12122b);
        final Font  monoBold = new Font("Consolas", Font.BOLD, 13);

        startBtn.setFont(monoBold);
        startBtn.setBorder(new LineBorder(neonCyn, 2, true));
        browseAppBtn.setBorder(new LineBorder(neonMag, 1, true));
        browseIncBtn.setBorder(new LineBorder(neonMag, 1, true));
        backBtn.setBorder(new LineBorder(neonMag, 1, true));

        grid.setBorder(BorderFactory.createTitledBorder(new LineBorder(neonMag, 1, true), "Percorsi"));
        center.setBorder(BorderFactory.createTitledBorder(new LineBorder(neonCyn, 1, true), "Log"));
        top.setBackground(panelBg);
        grid.setBackground(panelBg);
        center.setBackground(panelBg);
        frame.getContentPane().setBackground(panelBg);

        frame.setLayout(new BorderLayout(8,8));
        frame.add(top,    BorderLayout.NORTH);
        frame.add(center, BorderLayout.CENTER);
        frame.add(startBtn, BorderLayout.SOUTH);

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

    private void runScan(){
        startBtn.setEnabled(false); browseAppBtn.setEnabled(false); browseIncBtn.setEnabled(false);
        bar.setIndeterminate(true);
        log.setText("Start include-usage...\n");

        var appRoot = Paths.get(appField.getText());
        var incRoot  = Paths.get(incField.getText().isBlank()
                ? appRoot.resolve("private.includes").toString()
                : incField.getText());
        var unusedDir = appRoot.resolve("include inutilizzati");
        var report = new Report();
        var refactorer = new Refactorer();
        final boolean apply = applyMoves.isSelected();

        new Thread(() -> {
            try {
                var svc = new IncludeUsageService(
                        appRoot, incRoot, unusedDir, apply,
                        msg -> SwingUtilities.invokeLater(() -> log.append(msg + "\n")),
                        report, refactorer
                );
                svc.execute();
                var out = report.writeUnusedTxt(appRoot, "include-unused");
                SwingUtilities.invokeLater(() -> log.append("Report TXT: " + out.toAbsolutePath() + "\n"));
            } catch (Exception ex){
                SwingUtilities.invokeLater(() -> log.append("Errore: " + ex.getMessage() + "\n"));
            } finally {
                SwingUtilities.invokeLater(() -> {
                    bar.setIndeterminate(false);
                    startBtn.setEnabled(true); browseAppBtn.setEnabled(true); browseIncBtn.setEnabled(true);
                });
            }
        }).start();
    }
}
