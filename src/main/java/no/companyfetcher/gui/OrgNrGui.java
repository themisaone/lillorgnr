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
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class OrgNrGui extends JFrame {

    private static final Dimension ACTION_BUTTON_SIZE = new Dimension(220, 32);
    private static final int HELP_TEXT_WIDTH_PX = 860;
    private static final int LOG_AREA_MIN_HEIGHT_PX = 240;

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
        setMinimumSize(new Dimension(900, 680));
        setSize(900, 780);
        setLocationRelativeTo(null);

        logArea.setEditable(false);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);

        JPanel content = new JPanel(new GridBagLayout());
        content.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        content.add(buildButtonPanel(), gbc);

        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setPreferredSize(new Dimension(0, LOG_AREA_MIN_HEIGHT_PX));
        logScroll.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        JPanel logPanel = new JPanel(new BorderLayout(0, 6));
        logPanel.add(new JLabel("Log"), BorderLayout.NORTH);
        logPanel.add(logScroll, BorderLayout.CENTER);

        gbc.gridy = 1;
        gbc.insets = new Insets(12, 0, 0, 0);
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        content.add(logPanel, gbc);

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

        JPanel modePanel = new JPanel(new GridBagLayout());
        GridBagConstraints modePanelGbc = westGbc();
        modePanelGbc.weightx = 1.0;
        modePanelGbc.fill = GridBagConstraints.HORIZONTAL;
        modePanel.add(new JSeparator(), modePanelGbc);

        JPanel modeRow = new JPanel(new GridBagLayout());
        GridBagConstraints modeGbc = westGbc();
        modeGbc.insets = new Insets(0, 0, 0, 8);
        modeRow.add(new JLabel("Velg data som skal hentes:"), modeGbc);
        modeGbc.gridx = 1;
        modeRow.add(proffOnlyMode, modeGbc);
        modeGbc.gridx = 2;
        modeGbc.insets = new Insets(0, 0, 0, 0);
        modeRow.add(proffAndMtbMode, modeGbc);

        modePanelGbc.gridy = 1;
        modePanelGbc.insets = new Insets(8, 0, 8, 0);
        modePanel.add(modeRow, modePanelGbc);

        ButtonGroup modeGroup = new ButtonGroup();
        modeGroup.add(proffOnlyMode);
        modeGroup.add(proffAndMtbMode);
        modeSelectors.add(proffOnlyMode);
        modeSelectors.add(proffAndMtbMode);

        modePanelGbc.gridy = 2;
        modePanelGbc.insets = new Insets(0, 0, 0, 0);
        modePanel.add(new JSeparator(), modePanelGbc);
        section.add(modePanel, BorderLayout.NORTH);

        String excelFile = Configuration.load().getExcelFile();
        JPanel actions = new JPanel(new GridBagLayout());
        GridBagConstraints rowGbc = new GridBagConstraints();
        rowGbc.gridx = 0;
        rowGbc.gridy = 0;
        rowGbc.weightx = 1.0;
        rowGbc.fill = GridBagConstraints.HORIZONTAL;
        rowGbc.insets = new Insets(2, 0, 2, 0);

        actions.add(buildActionRow(
                actionButton("Rediger OrgNrs.txt", () ->
                        editInputFile(Path.of(Configuration.load().getProffAquaInputFile()))),
                "Kopier organisasjonsnummer i filen. Tomme linjer er ufarlige, "
                        + "men du kan bruke «Rens org.nr» og «Lagre» før du utfører «Hent data»."
        ), rowGbc);
        addRowSeparator(actions, rowGbc);

        actions.add(buildActionRow(
                actionButton("Hent data", () ->
                        runTask("Data-henting", () ->
                                ApplicationTasks.fetchAll(Configuration.load(), selectedMode()))),
                "Henter data fra Proff.no og (hvis «Proff og MTB» er valgt) også fra "
                        + "Fiskeridirektoratet (aqua) og MTB-gebyrer. Resultatet lagres i CSV-filen."
        ), rowGbc);
        addRowSeparator(actions, rowGbc);

        actions.add(buildActionRow(
                actionButton("Vis resultat-CSV", () ->
                        viewResultFile(Path.of(Configuration.load().getOutputFile()))),
                "Viser CSV-filen med hentede data. Sjekk at alt ser riktig ut før du "
                        + "slår sammen til Excel."
        ), rowGbc);
        addRowSeparator(actions, rowGbc);

        actions.add(buildMergeRow(excelFile), rowGbc);
        addRowSeparator(actions, rowGbc);

        section.add(actions, BorderLayout.CENTER);

        return section;
    }

    private void addRowSeparator(JPanel panel, GridBagConstraints gbc) {
        gbc.gridy++;
        gbc.insets = new Insets(8, 0, 8, 0);
        panel.add(new JSeparator(), gbc);
        gbc.gridy++;
        gbc.insets = new Insets(2, 0, 2, 0);
    }

    private JPanel buildActionRow(JButton button, String helpText) {
        JPanel row = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = westGbc();
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        row.add(wrapHelpLabel(helpText), gbc);

        gbc = westGbc();
        gbc.gridy = 1;
        gbc.insets = new Insets(6, 0, 0, 0);
        row.add(button, gbc);
        return row;
    }

    private JPanel buildMergeRow(String excelFile) {
        JPanel row = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = westGbc();
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        row.add(wrapHelpLabel(
                "Kopierer data fra CSV til Excel-filen (kolonner E, G, I ved «Bare Proff», "
                        + "eller E, G, I, K, M ved «Proff og MTB»). Lukk Excel-filen først.<br>"
                        + "Excel-filen som oppdateres er: " + excelFile
        ), gbc);

        JButton mergeButton = actionButton("Slå sammen til Excel", () ->
                runMergeTask("Excel-sammenslåing", mergeColorSelector));

        gbc = westGbc();
        gbc.gridy = 1;
        gbc.insets = new Insets(6, 0, 0, 0);
        row.add(mergeButton, gbc);

        gbc.gridx = 1;
        gbc.insets = new Insets(6, 8, 0, 0);
        row.add(new JLabel("Markeringsfarge:"), gbc);

        gbc.gridx = 2;
        row.add(mergeColorSelector, gbc);
        return row;
    }

    private GridBagConstraints westGbc() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        return gbc;
    }

    private JLabel wrapHelpLabel(String text) {
        JLabel label = new JLabel(
                "<html><body style='width:" + HELP_TEXT_WIDTH_PX + "px;text-align:left'>"
                        + text
                        + "</body></html>"
        );
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
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

    private JButton actionButton(String text, Runnable action) {
        JButton button = new JButton(text);
        button.addActionListener(event -> action.run());
        button.setPreferredSize(ACTION_BUTTON_SIZE);
        button.setMinimumSize(ACTION_BUTTON_SIZE);
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
        GuiDisplayConfig.applyBeforeGuiStartup();
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }
            GuiDisplayConfig.applyUiFonts();

            OrgNrGui gui = new OrgNrGui();
            gui.setVisible(true);
        });
    }
}
