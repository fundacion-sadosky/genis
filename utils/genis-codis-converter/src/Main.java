import javax.swing.*;

public class Main {
    public static void main(String[] args) {// Create the options for the dialog
        Object[] options = {"De CODIS a GENis", "De GENis a CODIS"};

        // Show the dialog
        int n = JOptionPane.showOptionDialog(null,
                "¿Qué conversión quiere realizar?",
                "Conversión",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]);

        // Check which option was chosen and show the corresponding form
        if (n == JOptionPane.YES_OPTION) {
            codisAGenis();
        } else if (n == JOptionPane.NO_OPTION) {
            genisACodis();
        }
    }

    private static void codisAGenis() {
        JFrame frame = new JFrame("Conversor de CODIS a GENis");
        frame.setContentPane(new codisAGenisForm().rootPanel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    private static void genisACodis() {
        JFrame form2 = new JFrame("Conversor de GENis a CODIS");
        form2.setContentPane(new genisACodisForm().rootPanel);
        form2.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        form2.pack();
        form2.setVisible(true);
    }
}