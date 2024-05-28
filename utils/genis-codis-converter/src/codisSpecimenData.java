import javax.swing.*;
import java.awt.event.*;

public class codisSpecimenData extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    public JTextField ucnTextField;
    public JTextField livescanTextField;
    public JTextField offenseDescriptionTextField;
    public JTextField fingerDateTextField;
    public JTextField fingerMinuteTextField;
    public JTextField fingerHourTextField;
    private JLabel titleLabel;
    public JTextField sidTextField;
    private JLabel specimenIDLabel;

    public codisSpecimenData(String specimenID) {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        titleLabel.setText("Información del espécimen "+specimenID);

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    private void onOK() {
        // add your code here
        dispose();
    }

    private void onCancel() {
        // add your code here if necessary
        dispose();
    }

    public static void main(String[] args) {
//        codisSpecimenData dialog = new codisSpecimenData();
//        dialog.pack();
//        dialog.setVisible(true);
//        System.exit(0);
    }
}
