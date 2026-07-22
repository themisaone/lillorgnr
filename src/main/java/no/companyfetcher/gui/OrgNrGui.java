package no.companyfetcher.gui;

import no.companyfetcher.config.Configuration;
import no.companyfetcher.model.ReportRunMode;
import no.companyfetcher.output.EmptyValueProcessing;
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
    private static final int WIDE_DIALOG_WIDTH_PX = 520;
    private static final int INFO_DIALOG_WIDTH_PX = 380;
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
        Configuration configuration = Configuration.load();

        JPanel topPanel = new JPanel(new GridBagLayout());
        GridBagConstraints topGbc = westGbc();
        topGbc.weightx = 1.0;
        topGbc.fill = GridBagConstraints.HORIZONTAL;
        topPanel.add(new JSeparator(), topGbc);

        topGbc.gridy = 1;
        topGbc.insets = new Insets(8, 0, 8, 0);
        topPanel.add(buildConfigInfoPanel(configuration), topGbc);

        topGbc.gridy = 2;
        topGbc.insets = new Insets(0, 0, 8, 0);
        topPanel.add(new JSeparator(), topGbc);

        JPanel modeRow = new JPanel(new GridBagLayout());
        GridBagConstraints modeGbc = westGbc();
        modeGbc.insets = new Insets(0, 0, 0, 8);
        modeRow.add(new JLabel("Velg data som skal hentes:"), modeGbc);
        modeGbc.gridx = 1;
        modeRow.add(proffOnlyMode, modeGbc);
        modeGbc.gridx = 2;
        modeGbc.insets = new Insets(0, 0, 0, 0);
        modeRow.add(proffAndMtbMode, modeGbc);

        topGbc.gridy = 3;
        topGbc.insets = new Insets(0, 0, 8, 0);
        topPanel.add(modeRow, topGbc);

        topGbc.gridy = 4;
        topGbc.insets = new Insets(0, 0, 0, 0);
        topPanel.add(new JSeparator(), topGbc);

        ButtonGroup modeGroup = new ButtonGroup();
        modeGroup.add(proffOnlyMode);
        modeGroup.add(proffAndMtbMode);
        modeSelectors.add(proffOnlyMode);
        modeSelectors.add(proffAndMtbMode);

        section.add(topPanel, BorderLayout.NORTH);

        String excelFile = configuration.getExcelFile();
        JPanel actions = new JPanel(new GridBagLayout());
        GridBagConstraints rowGbc = new GridBagConstraints();
        rowGbc.gridx = 0;
        rowGbc.gridy = 0;
        rowGbc.weightx = 1.0;
        rowGbc.fill = GridBagConstraints.HORIZONTAL;
        rowGbc.insets = new Insets(2, 0, 2, 0);

        actions.add(buildActionRow(
                1,
                actionButton("Rediger OrgNrs.txt", () ->
                        editInputFile(Path.of(Configuration.load().getProffAquaInputFile()))),
                "Kopier organisasjonsnummer i filen. Tomme linjer er ufarlige, "
                        + "men du kan bruke «Rens org.nr» og «Lagre» før du utfører «Hent data»."
        ), rowGbc);
        addRowSeparator(actions, rowGbc);

        actions.add(buildActionRow(
                2,
                actionButton("Hent data", this::confirmAndFetchData),
                "Henter data fra Proff.no og (hvis «Proff og MTB» er valgt) også fra "
                        + "Fiskeridirektoratet (aqua) og MTB-gebyrer. Resultatet lagres i CSV-filen."
        ), rowGbc);
        addRowSeparator(actions, rowGbc);

        actions.add(buildActionRow(
                3,
                actionButton("Vis resultat-CSV", () ->
                        viewResultFile(Path.of(Configuration.load().getOutputFile()))),
                "Viser CSV-filen med hentede data. Sjekk at alt ser riktig ut før du "
                        + "slår sammen til Excel."
        ), rowGbc);
        addRowSeparator(actions, rowGbc);

        actions.add(buildActionRow(
                4,
                actionButton("Sett markeringsfarge", this::showHighlightColorDialog),
                "Setter markeringsfarge som skal brukes i neste steg."
        ), rowGbc);
        addRowSeparator(actions, rowGbc);

        actions.add(buildMergeRow(5, excelFile), rowGbc);
        addRowSeparator(actions, rowGbc);

        section.add(actions, BorderLayout.CENTER);

        return section;
    }

    private JPanel buildConfigInfoPanel(Configuration configuration) {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = westGbc();
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 4, 0);

        panel.add(new JLabel("Regnskap for: " + configuration.getAccountingYear()), gbc);

        gbc.gridy = 1;
        panel.add(new JLabel("Excel dokument som oppdateres: " + configuration.getExcelFile()), gbc);

        gbc.gridy = 2;
        panel.add(new JLabel("Hvis regnskap ikke funnet: " + describeEmptyValueProcessing(configuration)), gbc);
        return panel;
    }

    private String describeEmptyValueProcessing(Configuration configuration) {
        return configuration.getEmptyValueProcessing() == EmptyValueProcessing.CLEAR
                ? "Fjern eksisterende verdi og set farge"
                : "Ikke forandre eksisterende felt";
    }

    private void addRowSeparator(JPanel panel, GridBagConstraints gbc) {
        gbc.gridy++;
        gbc.insets = new Insets(8, 0, 8, 0);
        panel.add(new JSeparator(), gbc);
        gbc.gridy++;
        gbc.insets = new Insets(2, 0, 2, 0);
    }

    private JPanel buildActionRow(int step, JButton button, String helpText) {
        return buildButtonWithDescription(step, button, helpText);
    }

    private JPanel buildMergeRow(int step, String excelFile) {
        JButton mergeButton = actionButton("Slå sammen til Excel", this::confirmAndMergeToExcel);
        return buildButtonWithDescription(
                step,
                mergeButton,
                "Kopierer data fra CSV til Excel-filen (kolonner E, G, I ved «Bare Proff», "
                        + "eller E, G, I, K, M ved «Proff og MTB»). Lukk Excel-filen først.\n"
                        + "Excel-filen som oppdateres er: " + excelFile
        );
    }

    private JPanel buildButtonWithDescription(int step, JButton button, String helpText) {
        JPanel row = new JPanel(new BorderLayout(12, 0));

        JPanel left = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = westGbc();
        gbc.anchor = GridBagConstraints.CENTER;
        left.add(createStepLabel(step), gbc);

        gbc.gridx = 1;
        gbc.insets = new Insets(0, 8, 0, 0);
        left.add(button, gbc);

        row.add(left, BorderLayout.WEST);

        JPanel helpPanel = new JPanel(new BorderLayout());
        helpPanel.setOpaque(false);
        helpPanel.add(createHelpText(helpText), BorderLayout.NORTH);
        row.add(helpPanel, BorderLayout.CENTER);
        return row;
    }

    private JLabel createStepLabel(int step) {
        return new JLabel("Steg " + step + ":");
    }

    private JTextArea createHelpText(String text) {
        JTextArea area = new JTextArea(text.replace("<br>", "\n"));
        area.setEditable(false);
        area.setFocusable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setOpaque(false);
        area.setBorder(BorderFactory.createEmptyBorder());
        area.setFont(UIManager.getFont("Label.font"));
        area.setForeground(UIManager.getColor("Label.foreground"));
        return area;
    }

    private void confirmAndFetchData() {
        int confirm = showWideConfirmDialog(
                "Hent data",
                """
                        Har du fullført steg 1 og lagt til nye organisasjonsnummer?

                        Fortsette med data-henting?""",
                JOptionPane.QUESTION_MESSAGE
        );
        if (confirm != JOptionPane.OK_OPTION) {
            return;
        }
        runFetchTask();
    }

    private void runFetchTask() {
        appendLog("Starter: Data-henting (" + modeLabel(selectedMode()) + ") ...");
        setControlsEnabled(false);

        new SwingWorker<String, String>() {
            private GuiLogBridge logBridge;

            @Override
            protected String doInBackground() {
                logBridge = GuiLogBridge.attach(this::publish);
                try {
                    return ApplicationTasks.fetchAll(Configuration.load(), selectedMode());
                } finally {
                    logBridge.close();
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
                    String summary = get().strip();
                    appendLog(summary);
                    appendLog("Ferdig: Data-henting");
                    showFetchSummaryDialog(summary);
                } catch (Exception e) {
                    String message = e.getMessage() == null ? e.toString() : e.getMessage();
                    appendLog("FEIL i Data-henting: " + message);
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

    private void showHighlightColorDialog() {
        JComboBox<String> colorPicker = new JComboBox<>(HIGHLIGHT_COLORS);
        colorPicker.setSelectedItem(mergeColorSelector.getSelectedItem());

        int result = JOptionPane.showConfirmDialog(
                this,
                colorPicker,
                "Sett markeringsfarge",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );
        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        String selectedColor = (String) colorPicker.getSelectedItem();
        mergeColorSelector.setSelectedItem(selectedColor);
        appendLog("Markeringsfarge satt til: " + selectedColor);
    }

    private void confirmAndMergeToExcel() {
        runMergeTask("Excel-sammenslåing", mergeColorSelector);
    }

    private GridBagConstraints westGbc() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        return gbc;
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
        String excelFile = Configuration.load().getExcelFile();
        String selectedColor = String.valueOf(colorSelector.getSelectedItem());
        int confirm = showWideConfirmDialog(
                "Slå sammen til Excel",
                """
                        Sjekk følgende før sammenslåing:

                        • Har du satt riktig farge? (Valgt farge: %s)
                        • Har du sjekket at filen som oppdateres er riktig? (<nobr>%s</nobr>)
                        • Har du lukket filen?

                        Fortsette?""".formatted(selectedColor, excelFile),
                JOptionPane.QUESTION_MESSAGE
        );
        if (confirm != JOptionPane.OK_OPTION) {
            return;
        }

        ReportRunMode mode = selectedMode();
        runBackground(taskName, () -> ApplicationTasks.mergeToExcel(Configuration.load(), mode, selectedColor));
    }

    private int showWideConfirmDialog(String title, String message, int messageType) {
        return JOptionPane.showConfirmDialog(
                this,
                wrapDialogMessage(appendSpacingBeforeButtons(message)),
                title,
                JOptionPane.OK_CANCEL_OPTION,
                messageType
        );
    }

    private String appendSpacingBeforeButtons(String message) {
        if (message.contains("Fortsette")) {
            return message.stripTrailing() + "\n\n";
        }
        return message;
    }

    private String wrapDialogMessage(String message) {
        return wrapDialogMessage(message, WIDE_DIALOG_WIDTH_PX);
    }

    private String wrapDialogMessage(String message, int widthPx) {
        return "<html><body style='width:" + widthPx + "px'>"
                + message.replace("\n", "<br>")
                + "</body></html>";
    }

    private void showFetchSummaryDialog(String summary) {
        showInfoDialog("Data-henting fullført", summary + "\n\n");
    }

    private void showInfoDialog(String title, String message) {
        JOptionPane.showMessageDialog(
                this,
                wrapDialogMessage(message, INFO_DIALOG_WIDTH_PX),
                title,
                JOptionPane.INFORMATION_MESSAGE
        );
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
            GuiDisplayConfig.applyUiLabels();

            OrgNrGui gui = new OrgNrGui();
            gui.setVisible(true);
        });
    }
}
