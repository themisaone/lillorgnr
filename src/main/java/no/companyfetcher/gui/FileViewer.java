package no.companyfetcher.gui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.IOException;
import no.companyfetcher.input.OrgNrInputNormalizer;
import java.nio.file.Files;
import java.nio.file.Path;

final class FileViewer {

    private FileViewer() {
    }

    static void view(Path file, java.awt.Component parent, boolean editable, Runnable onSaved) {
        if (!editable && !Files.exists(file)) {
            JOptionPane.showMessageDialog(
                    parent,
                    "Filen finnes ikke:\n" + file.toAbsolutePath(),
                    "Vis fil",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        try {
            String content = Files.exists(file) ? Files.readString(file) : "";
            JTextArea textArea = new JTextArea(content);
            textArea.setEditable(editable);
            textArea.setCaretPosition(0);
            textArea.setLineWrap(false);

            JScrollPane scrollPane = new JScrollPane(textArea);
            scrollPane.setBorder(BorderFactory.createLineBorder(Color.GRAY));

            JDialog dialog = new JDialog(
                    JOptionPane.getFrameForComponent(parent),
                    file.getFileName().toString(),
                    true
            );

            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
            if (editable) {
                JButton cleanButton = new JButton("Rens org.nr");
                cleanButton.addActionListener(event -> {
                    String cleaned = OrgNrInputNormalizer.cleanFileContent(textArea.getText());
                    textArea.setText(cleaned);
                    JOptionPane.showMessageDialog(
                            dialog,
                            "Komma og mellomrom er fjernet fra org.nr-linjer.\nLagre for å beholde endringene.",
                            "Rens org.nr",
                            JOptionPane.INFORMATION_MESSAGE
                    );
                });
                buttonPanel.add(cleanButton);

                JButton saveButton = new JButton("Lagre");
                saveButton.addActionListener(event -> {
                    if (saveFile(file, textArea.getText(), parent)) {
                        if (onSaved != null) {
                            onSaved.run();
                        }
                    }
                });
                buttonPanel.add(saveButton);
            }

            JButton closeButton = new JButton("Lukk");
            closeButton.addActionListener(event -> {
                if (editable && hasUnsavedChanges(file, textArea.getText())) {
                    int choice = JOptionPane.showConfirmDialog(
                            dialog,
                            "Du har ulagrede endringer.\nLukke uten å lagre?",
                            "Ulagrede endringer",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.WARNING_MESSAGE
                    );
                    if (choice != JOptionPane.YES_OPTION) {
                        return;
                    }
                }
                dialog.dispose();
            });
            buttonPanel.add(closeButton);

            JPanel dialogContent = new JPanel(new BorderLayout(0, 12));
            dialogContent.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
            dialogContent.add(scrollPane, BorderLayout.CENTER);
            dialogContent.add(buttonPanel, BorderLayout.SOUTH);

            dialog.setLayout(new BorderLayout());
            dialog.add(dialogContent, BorderLayout.CENTER);

            dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
            dialog.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosing(java.awt.event.WindowEvent event) {
                    closeButton.doClick();
                }
            });

            dialog.setSize(new Dimension(700, 500));
            dialog.setLocationRelativeTo(parent);
            dialog.setVisible(true);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(
                    parent,
                    "Kunne ikke lese fil:\n" + e.getMessage(),
                    editable ? "Rediger fil" : "Vis fil",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private static boolean hasUnsavedChanges(Path file, String currentContent) {
        try {
            String savedContent = Files.exists(file) ? Files.readString(file) : "";
            return !savedContent.equals(currentContent);
        } catch (IOException e) {
            return true;
        }
    }

    private static boolean saveFile(Path file, String content, java.awt.Component parent) {
        try {
            Path parentDir = file.getParent();
            if (parentDir != null) {
                Files.createDirectories(parentDir);
            }
            Files.writeString(file, content);
            JOptionPane.showMessageDialog(
                    parent,
                    "Filen er lagret.",
                    "Lagre",
                    JOptionPane.INFORMATION_MESSAGE
            );
            return true;
        } catch (IOException e) {
            JOptionPane.showMessageDialog(
                    parent,
                    "Kunne ikke lagre fil:\n" + e.getMessage(),
                    "Lagre",
                    JOptionPane.ERROR_MESSAGE
            );
            return false;
        }
    }
}
