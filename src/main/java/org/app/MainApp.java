package org.app;

import org.app.dto.TranzitDto;
import org.app.service.ExcelService;
import org.app.service.TransitXmlParser;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Insets;
import java.awt.TextField;
import java.io.File;
import java.nio.file.Path;

public class MainApp extends JFrame {

    private final JTextField xmlPath = new JTextField();
    private final JButton btnSelectXml = new JButton("Select XML");
    private final JButton btnGenerate = new JButton("Generate Excel");
    private final JTextArea log = new JTextArea();

    private File selectedXml;

    public MainApp() {
        super("Transit XML → Excel (Swing)");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(720, 420);
        setLocationRelativeTo(null);

        xmlPath.setEditable(false);

        JPanel fileRow = new JPanel(new BorderLayout(8, 8));
        fileRow.add(new JLabel("XML:"), BorderLayout.WEST);
        fileRow.add(xmlPath, BorderLayout.CENTER);
        fileRow.add(btnSelectXml, BorderLayout.EAST);

        btnGenerate.setEnabled(false);

        JPanel north = new JPanel();
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        north.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        north.add(fileRow);
        north.add(Box.createVerticalStrut(10));
        north.add(btnGenerate);

        log.setEditable(false);
        log.setLineWrap(true);
        log.setWrapStyleWord(true);
        JScrollPane logScroll = new JScrollPane(log,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        getContentPane().setLayout(new BorderLayout(8, 8));
        getContentPane().add(north, BorderLayout.NORTH);
        getContentPane().add(logScroll, BorderLayout.CENTER);

        // Actions
        btnSelectXml.addActionListener(e -> chooseXml());
        btnGenerate.addActionListener(e -> generateExcel());
    }

    private void chooseXml() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("XML files", "xml"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            selectedXml = chooser.getSelectedFile();
            xmlPath.setText(selectedXml.getAbsolutePath());
            btnGenerate.setEnabled(true);
            log.append("Selected XML: " + selectedXml.getName() + "\n");
        }
    }

    private void generateExcel() {
        if (selectedXml == null) return;
        try {
            // 1) Parse XML → DTO (missing values → "")
            TransitXmlParser parser = new TransitXmlParser();
            TranzitDto dto = parser.parse(selectedXml);
            log.append("XML parsed. HouseConsigment count: " + dto.getHouseConsigmentList().size() + "\n");

            // 2) Clone template and write a couple of example fields into same folder as XML
            ExcelService excel = new ExcelService();
            Path out = excel.cloneTemplateAndWrite(
                    "/template.xlsx",                              // resource path
                    selectedXml.getParentFile().toPath(),         // output dir (XML folder)
                    "TransitOutput.xlsx",                          // output file name
                    dto);

            log.append("Excel generated: " + out.toAbsolutePath() + "\n");
            JOptionPane.showMessageDialog(this,
                    "Done! Excel generated:\n" + out.toAbsolutePath(),
                    "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            ex.printStackTrace();
            log.append("Error: " + ex.getMessage() + "\n");
            JOptionPane.showMessageDialog(this,
                    "Error: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        // Optional: use system look & feel for native feel
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> new MainApp().setVisible(true));
    }
}
