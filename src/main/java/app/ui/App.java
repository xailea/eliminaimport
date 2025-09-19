package app.ui;

import app.core.*;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.file.*;

public class App {
    private JFrame frame;
    private JButton startBtn, browseAppBtn, browseIncBtn;
    private JProgressBar bar;
    private JTextArea log;
    private JTextField appField, incField;
    private JCheckBox applyMoves;

    public static void main(String[] args){
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}
        SwingUtilities.invokeLater(App::new);
    }

    public App(){
        frame = new JFrame("Include Analyzer");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setSize(920, 560);

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

        var grid = new JPanel(new GridBagLayout());
        grid.setBorder(BorderFactory.createTitledBorder("Percorsi"));
        var c = new GridBagConstraints(); c.insets = new Insets(4,4,4,4); c.fill = GridBagConstraints.HORIZONTAL;

        c.gridx=0; c.gridy=0; grid.add(new JLabel("Cartella App:"), c);
        c.gridx=1; c.weightx=1; grid.add(appField, c);
        c.gridx=2; c.weightx=0; grid.add(browseAppBtn, c);

        c.gridx=0; c.gridy=1; c.weightx=0; grid.add(new JLabel("Cartella Includes:"), c);
        c.gridx=1; c.weightx=1; grid.add(incField, c);
        c.gridx=2; c.weightx=0; grid.add(browseIncBtn, c);

        c.gridx=1; c.gridy=2; c.gridwidth=2; grid.add(applyMoves, c);

        var center = new JPanel(new BorderLayout(8,8));
        center.setBorder(BorderFactory.createTitledBorder("Log"));
        center.add(new JScrollPane(log), BorderLayout.CENTER);
        center.add(bar, BorderLayout.SOUTH);

        frame.setLayout(new BorderLayout(8,8));
        frame.add(grid, BorderLayout.NORTH);
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
        startBtn.setEnabled(false);
        browseAppBtn.setEnabled(false);
        browseIncBtn.setEnabled(false);
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
