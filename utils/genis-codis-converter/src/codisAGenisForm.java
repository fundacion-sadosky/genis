import com.opencsv.CSVWriter;
import com.opencsv.CSVWriterBuilder;
import org.w3c.dom.*;
import org.xml.sax.SAXException;
import javax.swing.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.*;
import java.util.List;

/*
TO-DOs:
- specimen form: si pone cancel o cerrar que salga del todo
*/


public class codisAGenisForm {
    String inFileName = "";
    String outFilePath = "";
    public codisAGenisForm() {
        ImageIcon logoIcon = new ImageIcon("logo.png");
        logoLabel.setIcon(logoIcon);
        convertButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {

                //obtengo el nombre de usuario
                String user = userTextField.getText();

                String[] fieldsToCheck = {user, inFileName, outFilePath, outFilenameTextField.getText()};
                String[] fieldDescriptions = {"un nombre de usuario", "un archivo de entrada", "una ubicación para el archivo de salida", "un nombre para el archivo de salida"};

                if (checkFields(fieldsToCheck, convertButton.getParent(), fieldDescriptions)){
//                  // inicializo el escritor del csv de salida
                    CSVWriter writer;
                    System.out.println(outFilePath);
                    try {
                        writer = (CSVWriter) new CSVWriterBuilder(new FileWriter(outFilePath + "/" + outFilenameTextField.getText() + ".csv"))
                                .withSeparator('\t')
                                .build();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                    // le agrego header
                    String[] header = "Sample Name	Specimen Category	UD1	UD2	Marker	Allele 1	Allele 2	Allele 3	Allele 4	Allele 5	Allele 6	Allele 7	Allele 8".split("\t");
                    writer.writeNext(header);

                    // inicializo el lector de la entrada
                    DocumentBuilder builder = null;
                    try {
                        builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                    } catch (ParserConfigurationException e) {
                        throw new RuntimeException(e);
                    }
                    Document doc;
                    try {
                        doc = builder.parse(new File(inFileName));
                    } catch (SAXException | IOException e) {
                        throw new RuntimeException(e);
                    }
                    doc.normalize();

                    // recorro especímenes
                    NodeList specimens = doc.getElementsByTagName("SPECIMEN");
                    for (int specimenIndex = 0; specimenIndex < specimens.getLength(); specimenIndex++) {

                        // inicializo los campos que busco de cada especimen
                        String specimenID = "";
                        String specimenCategory = "";

                        // recorro los campos de este especimen
                        Node specimen = specimens.item(specimenIndex);
                        NodeList camposSpecimen = specimen.getChildNodes();
                        for (int i = 0; i < camposSpecimen.getLength(); i++) {
                            Node iesimoCampo = camposSpecimen.item(i);
                            String name = iesimoCampo.getNodeName();
                            if (name.equals("SPECIMENID")) {
                                specimenID = iesimoCampo.getTextContent();
                            } else if (name.equals("SPECIMENCATEGORY")) {
                                String codisCategory = iesimoCampo.getTextContent();
                                specimenCategory = translateCategoryToGenis(codisCategory, specimenID);
                            } else if (name.equals("LOCUS")) {
                                String marker = "";
                                String kit = "";
                                List<String> valorAlelos = new ArrayList<>(); //inicializo lista vacia de alelos
                                NodeList camposLocus = iesimoCampo.getChildNodes();
                                for (int j = 0; j < camposLocus.getLength(); j++) {
                                    Node jesimoCampo = camposLocus.item(j);
                                    String nameJesimoCampo = jesimoCampo.getNodeName();
                                    if (nameJesimoCampo.equals("LOCUSNAME")) {
                                        marker = jesimoCampo.getTextContent();
                                    } else if (nameJesimoCampo.equals("KIT")) {
                                        kit = jesimoCampo.getTextContent();
                                    } else if (nameJesimoCampo.equals("ALLELE")) {
                                        NodeList camposAlelos = jesimoCampo.getChildNodes();
                                        // String valorAlelo = camposAlelos.item(1).getFirstChild().getTextContent();
                                        String valorAlelo = "";
                                        int k = 0;
                                        String nombreCampo = "";
                                        while (!nombreCampo.equals("ALLELEVALUE")){
                                            nombreCampo = camposAlelos.item(k).getNodeName();
                                            k=k+1;
                                        }
                                        valorAlelo = camposAlelos.item(k-1).getFirstChild().getTextContent();
                                        valorAlelos.add(valorAlelo);
                                    }
                                }

                                ArrayList<String> nuevaLinea = new ArrayList<String>();
                                nuevaLinea.add(specimenID);
                                nuevaLinea.add(specimenCategory);
                                nuevaLinea.add(user);
                                nuevaLinea.add(kit);
                                nuevaLinea.add(marker);
                                nuevaLinea.addAll(valorAlelos);
                                int emptyFieldsNumber = 8 - valorAlelos.size();
                                for (int j = 0; j < emptyFieldsNumber; j++) {
                                    nuevaLinea.add("");
                                }
                                String[] nuevaLineaArray = nuevaLinea.toArray(new String[0]);
                                writer.writeNext(nuevaLineaArray);
                            }

                        }

                    }
                    try {
                        writer.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    JOptionPane.showMessageDialog(convertButton.getParent(),
                            "Archivo convertido con éxito.",
                            "Éxito",
                            JOptionPane.PLAIN_MESSAGE);
                    System.out.println("Éxito");
                }
            }
        });

        inFilenameButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                final JFileChooser in_fc = new JFileChooser();
                int returnVal = in_fc.showOpenDialog(inFilenameButton.getParent());
                inFileName = in_fc.getSelectedFile().getAbsolutePath();
                inFilenameLabel.setText(inFileName);
            }
        });
        outFilenameButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                final JFileChooser out_fc = new JFileChooser();
                out_fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                int returnVal = out_fc.showOpenDialog(outFilenameButton.getParent());
                outFilePath = out_fc.getSelectedFile().getAbsolutePath();
                outFilenameLabel.setText(outFilePath +"/");

            }
        });
    }

    public JPanel rootPanel;
    private JLabel nameLabel;
    private JButton convertButton;
    private JTextField userTextField;
    private JButton inFilenameButton;
    private JButton outFilenameButton;
    private JLabel inFilenameLabel;
    private JLabel outFilenameLabel;
    private JTextField outFilenameTextField;
    private JLabel logoLabel;

    private String translateCategoryToGenis(String codisCategory, String specimenID){
        String genisCategory = "";
        String suggestedCategory = "Evidencia Parcial";
        if (codisCategory.equals("Forensic Partial")){
            suggestedCategory = "Evidencia Parcial";
        } else if (codisCategory.equals("Forensic Unknown") || codisCategory.equals("Forensic, Unknown")) {
            suggestedCategory = "Evidencia Completa";
        } else if (codisCategory.equals("Forensic Mixture")) {
            suggestedCategory = "Evidencia Mezcla";
        } else if (codisCategory.equals("Suspect")) {
            suggestedCategory = "Sospechoso";
        } else if (codisCategory.equals("Convicted Offender")) {
            suggestedCategory = "Condenado";
        }

        String mensaje = "La categoría en CODIS del perfil " + specimenID + " es \""+ codisCategory +"\", ¿a qué categoría de GENis corresponde?";
        genisCategory = (String) JOptionPane.showInputDialog(null,
                mensaje, "Categoría GENis:",
                JOptionPane.INFORMATION_MESSAGE, null,
                null, suggestedCategory);

        return genisCategory;
    }

    public boolean checkField(String field, Container frame, String description){
        if (field.isEmpty()){
            JOptionPane.showMessageDialog(frame ,"Ingrese "+description, "Error", JOptionPane.ERROR_MESSAGE);
        }
        return !field.isEmpty();
    }

    public boolean checkFields(String[] fields, Container frame, String[] descriptions){
        boolean fieldsOK = true;
        for (int i = 0; i < fields.length; i++) {
            fieldsOK = fieldsOK & checkField(fields[i], frame, descriptions[i]);
            if (!fieldsOK) break;
        }
        return fieldsOK;
    }
}

