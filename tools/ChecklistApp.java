import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.*;
import java.util.*;

/**
 * BeatBlocks QA Checklist — Standalone GUI
 * Run with: java tools/ChecklistApp.java
 * No Minecraft dependency.
 */
public class ChecklistApp extends JFrame {

    private static final String[] CHECKLIST_ITEMS = {
        // Keybinds
        "Alt+I opens overlay",
        "Alt+S does nothing (no response anywhere)",
        "X alone does NOT open overlay (requireAltModifier=true)",
        "K toggles play/pause in-game",
        "L skips next track in-game",
        "J goes to previous track in-game",
        "Keybinds visible in Controls > BeatBlocks",

        // Default Mode
        "Default Mode opens settings/status only (not full BeatBlocks UI)",
        "Mode selector shows Default and Enhanced",
        "Enhanced Mode greyed out when bridge unavailable",
        "Unavailable message shown with reason",
        "GitHub setup link displayed",

        // Enhanced Mode
        "Enhanced Mode selectable when bridge connected",
        "Selecting Enhanced immediately opens full BeatBlocks UI",
        "Settings gear icon visible top-left in Enhanced overlay",
        "Gear icon switches to Default/settings screen",

        // Enhanced UI
        "Sidebar navigation with all tabs",
        "Now Playing tab shows cover, song, artist",
        "Playlists tab loads from library",
        "Liked Songs tab loads",
        "Albums tab loads",
        "Queue tab shows upcoming tracks",
        "Bottom player bar visible",
        "Play/pause button works",
        "Previous button works",
        "Next button works",
        "Shuffle toggle works",
        "Repeat cycles off > context > track",
        "Volume display shows percentage",
        "Progress bar visible with seek",
        "Cover art loads without blocky upscaling",

        // HUD
        "HUD visible at top-right when playing",
        "HUD ~283x70px at 1920x1080 GUI Scale Auto",
        "HUD has prev/play/next buttons",
        "HUD has mini progress bar",
        "HUD hides when overlay is open",
        "HUD hides during F1 mode",
        "HUD scales with resolution",

        // Overlay appearance
        "Overlay is NOT fullscreen (game visible behind)",
        "No blur effects anywhere",
        "Crisp dark panels with sharp borders",
        "No tiny unreadable text",

        // Input isolation
        "WASD does NOT move player while overlay open",
        "E does NOT open inventory while overlay open",
        "Q does NOT drop items while overlay open",
        "Mouse clicks stay in overlay",
        "Scroll stays in overlay",
        "After closing overlay, all controls work",

        // Reconnection
        "Close BeatBlocks > HUD disappears",
        "Reopen BeatBlocks > reconnects",
        "spicetify apply > reconnects after restart",
        "Close Minecraft > BeatBlocks keeps playing",

        // Performance
        "FPS impact < 5 with overlay closed",
        "FPS impact < 10 with overlay open",
        "No render-thread blocking during loads",
        "Cover art cache prevents repeated downloads",

        // Documentation
        "README setup instructions are accurate",
        "Diagnostics tab shows clear status",
        "Error messages are actionable",
        "Config file created on first launch",
    };

    private final JCheckBox[] checkBoxes;
    private final JTextField[] noteFields;
    private final JComboBox<String>[] statusBoxes;

    @SuppressWarnings("unchecked")
    public ChecklistApp() {
        super("BeatBlocks QA Checklist");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 700);

        checkBoxes = new JCheckBox[CHECKLIST_ITEMS.length];
        noteFields = new JTextField[CHECKLIST_ITEMS.length];
        statusBoxes = new JComboBox[CHECKLIST_ITEMS.length];

        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        for (int i = 0; i < CHECKLIST_ITEMS.length; i++) {
            JPanel row = new JPanel(new BorderLayout(8, 0));
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));

            checkBoxes[i] = new JCheckBox((i + 1) + ". " + CHECKLIST_ITEMS[i]);
            checkBoxes[i].setFont(new Font("SansSerif", Font.PLAIN, 12));
            row.add(checkBoxes[i], BorderLayout.WEST);

            statusBoxes[i] = new JComboBox<>(new String[]{"—", "PASS", "FAIL", "SKIP"});
            statusBoxes[i].setPreferredSize(new Dimension(70, 24));
            row.add(statusBoxes[i], BorderLayout.CENTER);

            noteFields[i] = new JTextField();
            noteFields[i].setPreferredSize(new Dimension(200, 24));
            noteFields[i].setToolTipText("Optional note");
            row.add(noteFields[i], BorderLayout.EAST);

            listPanel.add(row);
            listPanel.add(Box.createVerticalStrut(2));
        }

        JScrollPane scrollPane = new JScrollPane(listPanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        // Bottom buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton diagBtn = new JButton("Run Diagnostics");
        diagBtn.addActionListener(e -> runDiagnostics());
        buttonPanel.add(diagBtn);

        JButton exportMd = new JButton("Export Markdown");
        exportMd.addActionListener(e -> exportReport("md"));
        buttonPanel.add(exportMd);

        JButton exportHtml = new JButton("Export HTML");
        exportHtml.addActionListener(e -> exportReport("html"));
        buttonPanel.add(exportHtml);

        add(scrollPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        setLocationRelativeTo(null);
    }

    private void runDiagnostics() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== BeatBlocks Diagnostics ===\n\n");

        // Check port
        try {
            Socket s = new Socket();
            s.connect(new InetSocketAddress("127.0.0.1", 50321), 2000);
            s.close();
            sb.append("Bridge port 50321: LISTENING\n");
        } catch (Exception e) {
            sb.append("Bridge port 50321: NOT LISTENING\n");
        }

        // Check Spicetify
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"spicetify", "--version"});
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String ver = r.readLine();
            p.waitFor();
            sb.append("Spicetify CLI: " + (ver != null ? ver : "unknown") + "\n");
        } catch (Exception e) {
            sb.append("Spicetify CLI: NOT FOUND\n");
        }

        // Check extension file
        String extPath = System.getenv("APPDATA") + "\\spicetify\\Extensions\\beatblocks-api.js";
        sb.append("Extension file: " + (new File(extPath).exists() ? "FOUND" : "NOT FOUND") + "\n");
        sb.append("Extension path: " + extPath + "\n");

        // Check desktop player process (Windows: Spotify.exe)
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"tasklist", "/FI", "IMAGENAME eq Spotify.exe"});
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String output = "";
            String line;
            while ((line = r.readLine()) != null) output += line + "\n";
            p.waitFor();
            sb.append("Desktop player: " + (output.contains("Spotify.exe") ? "RUNNING" : "NOT RUNNING") + "\n");
        } catch (Exception e) {
            sb.append("Desktop player: UNKNOWN\n");
        }

        JTextArea area = new JTextArea(sb.toString());
        area.setFont(new Font("Monospaced", Font.PLAIN, 12));
        area.setEditable(false);
        JScrollPane sp = new JScrollPane(area);
        sp.setPreferredSize(new Dimension(500, 300));
        JOptionPane.showMessageDialog(this, sp, "Diagnostics", JOptionPane.INFORMATION_MESSAGE);
    }

    private void exportReport(String format) {
        String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(LocalDateTime.now());
        String filename = "beatblocks-qa-" + timestamp + "." + format;

        StringBuilder sb = new StringBuilder();
        int pass = 0, fail = 0, skip = 0, untested = 0;
        for (int i = 0; i < CHECKLIST_ITEMS.length; i++) {
            String status = (String) statusBoxes[i].getSelectedItem();
            if ("PASS".equals(status)) pass++;
            else if ("FAIL".equals(status)) fail++;
            else if ("SKIP".equals(status)) skip++;
            else untested++;
        }

        if ("html".equals(format)) {
            sb.append("<!DOCTYPE html><html><head><title>BeatBlocks QA Report</title>");
            sb.append("<style>body{font-family:sans-serif;max-width:800px;margin:auto;padding:20px}");
            sb.append("table{border-collapse:collapse;width:100%}th,td{border:1px solid #ddd;padding:6px;text-align:left}");
            sb.append(".pass{color:green}.fail{color:red;font-weight:bold}.skip{color:orange}</style></head><body>");
            sb.append("<h1>BeatBlocks QA Report</h1>");
            sb.append("<p>Generated: " + LocalDateTime.now() + "</p>");
            sb.append("<p>PASS: " + pass + " | FAIL: " + fail + " | SKIP: " + skip + " | Untested: " + untested + "</p>");
            sb.append("<table><tr><th>#</th><th>Check</th><th>Status</th><th>Notes</th></tr>");
            for (int i = 0; i < CHECKLIST_ITEMS.length; i++) {
                String status = (String) statusBoxes[i].getSelectedItem();
                String cssClass = "PASS".equals(status) ? "pass" : "FAIL".equals(status) ? "fail" : "SKIP".equals(status) ? "skip" : "";
                sb.append("<tr><td>").append(i + 1).append("</td><td>").append(CHECKLIST_ITEMS[i]);
                sb.append("</td><td class='").append(cssClass).append("'>").append(status);
                sb.append("</td><td>").append(noteFields[i].getText()).append("</td></tr>");
            }
            sb.append("</table></body></html>");
        } else {
            sb.append("# BeatBlocks QA Report\n\n");
            sb.append("Generated: " + LocalDateTime.now() + "\n\n");
            sb.append("**PASS:** " + pass + " | **FAIL:** " + fail + " | **SKIP:** " + skip + " | **Untested:** " + untested + "\n\n");
            sb.append("| # | Check | Status | Notes |\n");
            sb.append("|---|-------|--------|-------|\n");
            for (int i = 0; i < CHECKLIST_ITEMS.length; i++) {
                String status = (String) statusBoxes[i].getSelectedItem();
                sb.append("| ").append(i + 1).append(" | ").append(CHECKLIST_ITEMS[i]);
                sb.append(" | ").append(status);
                sb.append(" | ").append(noteFields[i].getText()).append(" |\n");
            }
        }

        try {
            Path outDir = Paths.get("test-reports");
            Files.createDirectories(outDir);
            Path outFile = outDir.resolve(filename);
            Files.writeString(outFile, sb.toString());
            JOptionPane.showMessageDialog(this, "Report saved: " + outFile.toAbsolutePath(),
                    "Export", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Failed to save: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
            catch (Exception ignored) {}
            new ChecklistApp().setVisible(true);
        });
    }
}
