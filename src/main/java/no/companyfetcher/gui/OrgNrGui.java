package no.companyfetcher.gui;

import no.companyfetcher.config.Configuration;
import no.companyfetcher.tasks.ApplicationTasks;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;

public class OrgNrGui extends JFrame {

    private static final String[] HIGHLIGHT_COLORS = {
            "LIGHT_GREEN",
            "LIGHT_YELLOW",
            "LIGHT_BLUE",
            "LIGHT_ORANGE"
    };

    private final JTextArea logArea = new JTextArea();
    private final JComboBox<String> proffColorSelector = new JComboBox<>(HIGHLIGHT_COLORS);
    private final JComboBox<String> aquaColorSelector = new JComboBox<>(HIGHLIGHT_COLORS);

    public OrgNrGui() {
        super("Org.nr verktøy");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(720, 560));
        setLocationRelativeTo(null);

        logArea.setEditable(false);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);

        JPanel content = new JPanel(new BorderLayout(8, 8));
        content.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        content.add(buildButtonPanel(), BorderLayout.NORTH);
        content.add(new JScrollPane(logArea), BorderLayout.CENTER);

        setContentPane(content);
        appendLog("Klar. Lukk Excel før du slår sammen data.");
    }

    private JPanel buildButtonPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(4, 0, 4, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        panel.add(buildSection(
                "Proff",
                () -> Path.of(Configuration.load().getProffAquaInputFile()),
                () -> Path.of(Configuration.load().getProffOutputFile()),
                "Hent data fra Proff",
                () -> runTask("Proff-henting", () -> ApplicationTasks.fetchProff(Configuration.load())),
                "Slå sammen Proff CSV til Excel",
                proffColorSelector,
                () -> runMergeTask("Proff-sammenslåing", proffColorSelector, ApplicationTasks::mergeProffToExcel)
        ), gbc);

        gbc.gridy++;
        panel.add(createSeparator(), gbc);

        gbc.gridy++;
        panel.add(buildSection(
                "Akvakultur",
                () -> Path.of(Configuration.load().getProffAquaInputFile()),
                () -> Path.of(Configuration.load().getAquaOutputFile()),
                "Hent akvakulturkapasitet",
                () -> runTask("Akvakultur-henting", () -> ApplicationTasks.fetchAqua(Configuration.load())),
                "Slå sammen Aqua CSV til Excel (kolonne K)",
                aquaColorSelector,
                () -> runMergeTask("Akvakultur-sammenslåing", aquaColorSelector, ApplicationTasks::mergeAquaToExcel)
        ), gbc);

        gbc.gridy++;
        panel.add(createSeparator(), gbc);

        gbc.gridy++;
        panel.add(buildMtbSection(), gbc);

        return panel;
    }

    private JPanel buildSection(
            String title,
            Supplier<Path> inputFile,
            Supplier<Path> outputFile,
            String fetchLabel,
            Runnable fetchAction,
            String mergeLabel,
            JComboBox<String> colorSelector,
            Runnable mergeAction
    ) {
        JPanel section = new JPanel(new BorderLayout(6, 6));
        section.setBorder(BorderFactory.createTitledBorder(title));

        JPanel row0 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        row0.add(button("Rediger inndatafil", () -> editInputFile(inputFile.get())));
        row0.add(button("Vis resultat-CSV", () -> viewResultFile(outputFile.get())));
        section.add(row0, BorderLayout.NORTH);

        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        row1.add(button(fetchLabel, fetchAction));
        section.add(row1, BorderLayout.CENTER);

        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        row2.add(button(mergeLabel, mergeAction));
        row2.add(new JLabel("Markeringsfarge:"));
        row2.add(colorSelector);
        section.add(row2, BorderLayout.SOUTH);

        return section;
    }

    private JPanel buildMtbSection() {
        JPanel section = new JPanel(new BorderLayout(6, 6));
        section.setBorder(BorderFactory.createTitledBorder("MTB"));

        JPanel row0 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        row0.add(button("Rediger inndatafil", () -> editInputFile(Path.of(Configuration.load().getMtbInputFile()))));
        row0.add(button("Vis resultat-CSV", () -> viewResultFile(Path.of(Configuration.load().getMtbOutputFile()))));
        section.add(row0, BorderLayout.NORTH);

        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        row1.add(button("Beregn MTB-gebyrer", () ->
                runTask("MTB-beregning", () -> ApplicationTasks.runMtbCalc(Configuration.load()))));
        section.add(row1, BorderLayout.CENTER);

        return section;
    }

    private void editInputFile(Path file) {
        appendLog("Redigerer: " + file);
        FileViewer.view(file, this, true, () -> appendLog("Lagret: " + file));
    }

    private void viewResultFile(Path file) {
        appendLog("Viser: " + file);
        FileViewer.view(file, this, false, null);
    }

    private JButton button(String text, Runnable action) {
        JButton button = new JButton(text);
        button.addActionListener(event -> action.run());
        return button;
    }

    private JPanel createSeparator() {
        JPanel separator = new JPanel();
        separator.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, separator.getForeground()));
        separator.setPreferredSize(new Dimension(10, 8));
        return separator;
    }

    private void runTask(String taskName, Supplier<String> task) {
        runBackground(taskName, () -> task.get());
    }

    private void runMergeTask(
            String taskName,
            JComboBox<String> colorSelector,
            MergeTask mergeTask
    ) {
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Lukk Excel-filen før sammenslåing.\nFortsette?",
                "Slå sammen til Excel",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.INFORMATION_MESSAGE
        );
        if (confirm != JOptionPane.OK_OPTION) {
            return;
        }

        String color = (String) colorSelector.getSelectedItem();
        runBackground(taskName, () -> mergeTask.run(Configuration.load(), color));
    }

    private void runBackground(String taskName, Supplier<String> task) {
        appendLog("Starter: " + taskName + " ...");
        setButtonsEnabled(false);

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                return task.get();
            }

            @Override
            protected void done() {
                setButtonsEnabled(true);
                try {
                    appendLog(get());
                    appendLog("Ferdig: " + taskName);
                } catch (Exception e) {
                    String message = e.getMessage() == null ? e.toString() : e.getMessage();
                    appendLog("FEIL i " + taskName + ": " + message);
                    JOptionPane.showMessageDialog(
                            OrgNrGui.this,
                            message,
                            "Feil",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        }.execute();
    }

    private void setButtonsEnabled(boolean enabled) {
        for (var button : getAllButtons(getContentPane())) {
            button.setEnabled(enabled);
        }
    }

    private java.util.List<JButton> getAllButtons(java.awt.Container container) {
        java.util.List<JButton> buttons = new java.util.ArrayList<>();
        for (var component : container.getComponents()) {
            if (component instanceof JButton button) {
                buttons.add(button);
            } else if (component instanceof java.awt.Container child) {
                buttons.addAll(getAllButtons(child));
            }
        }
        return buttons;
    }

    private void appendLog(String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        logArea.append("[" + timestamp + "] " + message.trim() + System.lineSeparator());
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }

            OrgNrGui gui = new OrgNrGui();
            gui.setVisible(true);
        });
    }

    @FunctionalInterface
    private interface MergeTask {
        String run(Configuration configuration, String highlightColorName);
    }
}
