package no.companyfetcher.gui;

import no.companyfetcher.config.Configuration;
import no.companyfetcher.model.ReportRunMode;
import no.companyfetcher.tasks.ApplicationTasks;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
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
import java.util.ArrayList;
import java.util.List;

public class OrgNrGui extends JFrame {

    private static final String[] HIGHLIGHT_COLORS = {
            "LIGHT_GREEN",
            "LIGHT_YELLOW",
            "LIGHT_BLUE",
            "LIGHT_ORANGE"
    };

    private final JTextArea logArea = new JTextArea();
    private final JComboBox<String> mergeColorSelector = new JComboBox<>(HIGHLIGHT_COLORS);
    private final JRadioButton proffOnlyMode = new JRadioButton("Bare Proff", true);
    private final JRadioButton proffAndMtbMode = new JRadioButton("Proff og MTB");
    private final List<JRadioButton> modeSelectors = new ArrayList<>();

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

        panel.add(buildSection(), gbc);
        return panel;
    }

    private JPanel buildSection() {
        JPanel section = new JPanel(new BorderLayout(6, 6));
        section.setBorder(BorderFactory.createTitledBorder("Org.nr data"));

        JPanel modeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        ButtonGroup modeGroup = new ButtonGroup();
        modeGroup.add(proffOnlyMode);
        modeGroup.add(proffAndMtbMode);
        modeSelectors.add(proffOnlyMode);
        modeSelectors.add(proffAndMtbMode);
        modeRow.add(proffOnlyMode);
        modeRow.add(proffAndMtbMode);
        section.add(modeRow, BorderLayout.NORTH);

        JPanel row0 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        row0.add(button("Rediger OrgNrs.txt", () ->
                editInputFile(Path.of(Configuration.load().getProffAquaInputFile()))));
        row0.add(button("Vis resultat-CSV", () ->
                viewResultFile(Path.of(Configuration.load().getOutputFile()))));

        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        row1.add(button("Hent data", () ->
                runTask("Data-henting", () ->
                        ApplicationTasks.fetchAll(Configuration.load(), selectedMode()))));

        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        row2.add(button("Slå sammen til Excel", () ->
                runMergeTask("Excel-sammenslåing", mergeColorSelector)));
        row2.add(new JLabel("Markeringsfarge:"));
        row2.add(mergeColorSelector);

        JPanel actions = new JPanel(new BorderLayout(6, 6));
        actions.add(row0, BorderLayout.NORTH);
        actions.add(row1, BorderLayout.CENTER);
        actions.add(row2, BorderLayout.SOUTH);
        section.add(actions, BorderLayout.CENTER);

        return section;
    }

    private ReportRunMode selectedMode() {
        return proffAndMtbMode.isSelected() ? ReportRunMode.PROFF_AND_MTB : ReportRunMode.PROFF_ONLY;
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

    private void runTask(String taskName, java.util.function.Supplier<String> task) {
        runBackground(taskName, () -> task.get());
    }

    private void runMergeTask(String taskName, JComboBox<String> colorSelector) {
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

        ReportRunMode mode = selectedMode();
        String color = (String) colorSelector.getSelectedItem();
        runBackground(taskName, () -> ApplicationTasks.mergeToExcel(Configuration.load(), mode, color));
    }

    private void runBackground(String taskName, java.util.function.Supplier<String> task) {
        runBackground(taskName, task, true);
    }

    private void runBackground(String taskName, java.util.function.Supplier<String> task, boolean streamLogs) {
        appendLog("Starter: " + taskName + " (" + modeLabel(selectedMode()) + ") ...");
        setControlsEnabled(false);

        new SwingWorker<String, String>() {
            private GuiLogBridge logBridge;

            @Override
            protected String doInBackground() {
                if (streamLogs) {
                    logBridge = GuiLogBridge.attach(this::publish);
                }
                try {
                    return task.get();
                } finally {
                    if (logBridge != null) {
                        logBridge.close();
                    }
                }
            }

            @Override
            protected void process(java.util.List<String> chunks) {
                for (String line : chunks) {
                    appendRawLog(line);
                }
            }

            @Override
            protected void done() {
                setControlsEnabled(true);
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

    private String modeLabel(ReportRunMode mode) {
        return mode == ReportRunMode.PROFF_ONLY ? "Bare Proff" : "Proff og MTB";
    }

    private void setControlsEnabled(boolean enabled) {
        for (var button : getAllButtons(getContentPane())) {
            button.setEnabled(enabled);
        }
        for (var radio : modeSelectors) {
            radio.setEnabled(enabled);
        }
        mergeColorSelector.setEnabled(enabled);
    }

    private List<JButton> getAllButtons(java.awt.Container container) {
        List<JButton> buttons = new ArrayList<>();
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
        appendRawLog("[" + timestamp + "] " + message.trim());
    }

    private void appendRawLog(String message) {
        Runnable action = () -> {
            logArea.append(message.trim() + System.lineSeparator());
            logArea.setCaretPosition(logArea.getDocument().getLength());
        };
        if (SwingUtilities.isEventDispatchThread()) {
            action.run();
        } else {
            SwingUtilities.invokeLater(action);
        }
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
}
