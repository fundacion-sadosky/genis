import com.opencsv.bean.CsvToBeanBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.swing.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Arrays;
import java.util.List;

public class genisACodisForm {

    String inFileName = "";
    String outFilePath = "";
    JPanel rootPanel;
    private JButton inFilenameButton;
    private JLabel inFileNameLabel;
    private JButton convertirButton;
    private JTextField dateTextField;
    private JTextField hourTextField;
    private JTextField minuteTextField;
    private JTextField sourceORITextField;
    private JTextField destinationORITextField;
    private JTextField instrumentIDTextField;
    private JTextField ucnSidTextField;
    private JTextField livescanTextField;
    private JTextField offenseDescriptionTextField;
    private JTextField outFilenameTextField;
    private JButton outFilenameButton;
    private JLabel outFilenameLabel;
    private JTextField userTextField;

    public genisACodisForm() {

        inFilenameButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                final JFileChooser in_fc = new JFileChooser();
                int returnVal = in_fc.showOpenDialog(inFilenameButton.getParent());
                inFileName = in_fc.getSelectedFile().getAbsolutePath();
                inFileNameLabel.setText(inFileName);
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

        convertirButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                // de genis a codis
                String datetime = makeDatetime(dateTextField.getText(), hourTextField.getText(), minuteTextField.getText(), convertirButton.getParent());
                System.out.println("fecha:"+datetime);

                // chequeo que los campos no estén vacíos
                String[] fieldsToCheck = {inFileName, outFilePath, outFilenameTextField.getText(),
                        sourceORITextField.getText(), destinationORITextField.getText(), instrumentIDTextField.getText(), userTextField.getText()};

                String[] fieldsDescriptions = {"un archivo de entrada", "una ubicación para el archivo de salida", "un nombre para el archivo de salida",
                    "el ORI del organismo origen", "el ORI del organismo destinatario", "el ID del instrumento de extracción de ADN", "el usuario del sistema CODIS asociado a este archivo"};

                boolean fieldsOK = checkFields(fieldsToCheck, convertirButton.getParent(), fieldsDescriptions);

                if (fieldsOK & !datetime.isEmpty()) {
                    List<Marker> markers = null;
                    try {
                        markers = new CsvToBeanBuilder(new FileReader(inFileName))
                                .withType(Marker.class).withSeparator('\t').build().parse();
                    } catch (FileNotFoundException e) {
                        throw new RuntimeException(e);
                    }

                    // Create a DocumentBuilder
                    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder builder = null;
                    try {
                        builder = factory.newDocumentBuilder();
                    } catch (ParserConfigurationException e) {
                        throw new RuntimeException(e);
                    }


                    // Create a new Document
                    Document document = builder.newDocument();

                    // Create root element
                    Element root = document.createElement("CODISRapidImportFile");
                    document.appendChild(root);

                    // Header
                    Element header = document.createElement("HEADER");
                    root.appendChild(header);

                    Element msgVersion = document.createElement("MESSAGEVERSION");
                    header.appendChild(msgVersion);
                    msgVersion.appendChild(document.createTextNode("1.0"));

                    Element msgType = document.createElement("MESSAGETYPE");
                    header.appendChild(msgType);
                    msgType.appendChild(document.createTextNode("Rapid Import"));

                    Element msgID = document.createElement("MESSAGEID");
                    header.appendChild(msgID);
                    msgID.appendChild(document.createTextNode("1"));

                    Element msgDatetime = document.createElement("MESSAGEDATETIME");
                    header.appendChild(msgDatetime);
                    msgDatetime.appendChild(document.createTextNode(datetime));

                    Element msgCreator = document.createElement("MSGCREATORUSERID");
                    header.appendChild(msgCreator);
                    msgCreator.appendChild(document.createTextNode(userTextField.getText()));

                    Element destinationORI = document.createElement("DESTINATIONORI");
                    header.appendChild(destinationORI);
                    destinationORI.appendChild(document.createTextNode(destinationORITextField.getText()));

                    Element sourceORI = document.createElement("SOURCEORI");
                    header.appendChild(sourceORI);
                    sourceORI.appendChild(document.createTextNode(sourceORITextField.getText()));

                    // Device

                    Element device = document.createElement("DEVICE");
                    root.appendChild(device);

                    Element instrumentID = document.createElement("INSTRUMENTID");
                    device.appendChild(instrumentID);
                    instrumentID.appendChild(document.createTextNode(instrumentIDTextField.getText()));

                    // Specimen

                    String currentSpecimenName = "";
                    Element currentSpecimenNode = document.createElement("SPECIMEN");
                    for (Marker marker : markers){
                        if (!marker.sampleName.equals(currentSpecimenName)){ // si cambié de nombre de especimen

                            // actualizo el especimen actual
                            currentSpecimenName = marker.sampleName;

                            // tomo los datos de ese especimen
                            String ucn;
                            String sid;
                            String livescan;
                            String offense;
                            String specimenDatetime;
                            boolean specimenFieldsOK;

                            // muestro el form hasta que lo llenen bien
                            do {
                                codisSpecimenData specimenDialog = new codisSpecimenData(currentSpecimenName);
                                specimenDialog.setSize(600,400);
                                specimenDialog.setVisible(true);

                                ucn = specimenDialog.ucnTextField.getText();
                                sid = specimenDialog.sidTextField.getText();
                                livescan = specimenDialog.livescanTextField.getText();
                                offense = specimenDialog.offenseDescriptionTextField.getText();

                                String specimenDate = specimenDialog.fingerDateTextField.getText();
                                String specimenHour = specimenDialog.fingerHourTextField.getText();
                                String specimenMinutes = specimenDialog.fingerMinuteTextField.getText();
                                specimenDatetime = makeDatetime(specimenDate, specimenHour, specimenMinutes, convertirButton.getParent());

                                String[] specimenFieldsToCheck = {livescan, offense};
                                String[] specimenFieldsDescriptions = {"el Livescan Unique Event Identifier", "el cargo por el cual se realiza la detención"};
                                specimenFieldsOK = checkFields(specimenFieldsToCheck, convertirButton.getParent(), specimenFieldsDescriptions);

                                if (ucn.isEmpty() & sid.isEmpty()){
                                    JOptionPane.showMessageDialog(convertirButton.getParent() ,"Ingrese al menos uno de los identificadores UCN o SID.", "Error", JOptionPane.ERROR_MESSAGE);
                                    specimenFieldsOK = false;
                                }
                            } while (!specimenFieldsOK | specimenDatetime.isEmpty());

                            // creo un nodo para este especimen
                            currentSpecimenNode = document.createElement("SPECIMEN");
                            root.appendChild(currentSpecimenNode);
                            Element specimenID = document.createElement("SPECIMENID");
                            currentSpecimenNode.appendChild(specimenID);
                            specimenID.appendChild(document.createTextNode(marker.sampleName));

                            Element specimenCategory = document.createElement("SPECIMENCATEGORY");
                            currentSpecimenNode.appendChild(specimenCategory);
                            String codisCategory = translateCategoryToCodis(marker.specimenCategory);
                            specimenCategory.appendChild(document.createTextNode(codisCategory));

                            if (!sid.isEmpty()) {
                                Element sidNode = document.createElement("SID");
                                currentSpecimenNode.appendChild(sidNode);
                                sidNode.appendChild(document.createTextNode(sid));
                            }

                            if (!ucn.isEmpty()) {
                                Element ucnNode = document.createElement("FBI_NUMBER_UCN");
                                currentSpecimenNode.appendChild(ucnNode);
                                ucnNode.appendChild(document.createTextNode(ucn));
                            }

                            Element livescanIdentifier = document.createElement("UNIQUEEVENTID");
                            currentSpecimenNode.appendChild(livescanIdentifier);
                            livescanIdentifier.appendChild(document.createTextNode(livescan));

                            Element dateNode = document.createElement("FINGERPRINTDATE");
                            currentSpecimenNode.appendChild(dateNode);
                            dateNode.appendChild(document.createTextNode(specimenDatetime));

                            Element offenseDescription = document.createElement("ARRESTOFFENSECATEGORY");
                            currentSpecimenNode.appendChild(offenseDescription);
                            offenseDescription.appendChild(document.createTextNode(offense));
                        }
                        // agrego este marcador al especimen actual
                        Element currentSpecimenLocus = document.createElement("LOCUS");
                        currentSpecimenNode.appendChild(currentSpecimenLocus);

                        Element specimenLocusName = document.createElement("LOCUSNAME");
                        currentSpecimenLocus.appendChild(specimenLocusName);
                        specimenLocusName.appendChild(document.createTextNode(marker.marker));

                        Element specimenLocusKit = document.createElement("KIT");
                        currentSpecimenLocus.appendChild(specimenLocusKit);
                        specimenLocusKit.appendChild(document.createTextNode(marker.kit));

                        Element specimenLocusAllele = document.createElement("ALLELE");
                        currentSpecimenLocus.appendChild(specimenLocusAllele);
                        Element specimenLocusAlleleValue = document.createElement("ALLELEVALUE");
                        specimenLocusAllele.appendChild(specimenLocusAlleleValue);
                        specimenLocusAlleleValue.appendChild(document.createTextNode(marker.allele1));

                        List<String> alleles = Arrays.asList(marker.allele2, marker.allele3, marker.allele4, marker.allele5, marker.allele6, marker.allele7, marker.allele8);
                        for (String allele : alleles){
                            if (allele.isEmpty()){
                                break;
                            } else {
                                specimenLocusAllele = document.createElement("ALLELE");
                                currentSpecimenLocus.appendChild(specimenLocusAllele);
                                specimenLocusAlleleValue = document.createElement("ALLELEVALUE");
                                specimenLocusAllele.appendChild(specimenLocusAlleleValue);
                                specimenLocusAlleleValue.appendChild(document.createTextNode(allele));
                            }
                        }
                    }

                    // Write to XML file
                    TransformerFactory transformerFactory = TransformerFactory.newInstance();
                    Transformer transformer = null;
                    try {
                        transformer = transformerFactory.newTransformer();
                    } catch (TransformerConfigurationException e) {
                        throw new RuntimeException(e);
                    }
                    DOMSource source = new DOMSource(document);

                    // Specify your local file path
                    StreamResult result = new StreamResult(outFilePath+"/"+outFilenameTextField.getText()+".xml");
                    try {
                        transformer.transform(source, result);
                    } catch (TransformerException e) {
                        throw new RuntimeException(e);
                    }
                    JOptionPane.showMessageDialog(convertirButton.getParent(),
                            "Archivo convertido con éxito.",
                            "Éxito",
                            JOptionPane.PLAIN_MESSAGE);
                    System.out.println("XML file created successfully!");
                }


            }
        });
    }

    public String makeDatetime(String date, String hour, String minutes, Container frame){
        String dateRegex = "^\\d{4}-\\d{2}-\\d{2}$"; //para corroborar que la fecha tenga formato aaaa-mm-dd
        String hourRegex = "^\\d{2}$"; //para corroborar que las horas y minutos son dos dígitos

        String result = "";
        if (date.matches(dateRegex)){
            String[] dateParts = date.split("-");
            int year = Integer.parseInt(dateParts[0]);
            int month = Integer.parseInt(dateParts[1]);
            int day = Integer.parseInt(dateParts[2]);

            boolean dateok = (year > 1900) & (year < 2500) & (month > 0) & (month < 13) & (day > 0) & (day < 32);
            if (!dateok){
                JOptionPane.showMessageDialog(frame ,"Fecha no válida. Recuerde que tiene que respetar el formato aaaa-mm-dd, por ejemplo: 2022-12-18", "Error", JOptionPane.ERROR_MESSAGE);
                return result;
            }
            if (hour.matches(hourRegex) & minutes.matches(hourRegex)){
                boolean timeok = (Integer.parseInt(hour) < 25) & (Integer.parseInt(minutes) < 60);
                if (!timeok){
                    JOptionPane.showMessageDialog(frame ,"Hora/minutos no válidos.", "Error", JOptionPane.ERROR_MESSAGE);
                    return result;
                }
                result = date+"T"+hour+":"+minutes+":00";
            } else {
                JOptionPane.showMessageDialog(frame ,"Hora/minutos no válidos. Ingrese dos dígitos para la hora y dos dígitos para los minutos", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(frame ,"Fecha no válida. Recuerde que tiene que respetar el formato aaaa-mm-dd, por ejemplo: 2022-12-18", "Error", JOptionPane.ERROR_MESSAGE);
        }
        return result;
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

    private String translateCategoryToCodis(String genisCategory){
        String codisCategory = "";
        if (genisCategory.equals("Evidencia Parcial")){
            codisCategory = "Forensic Partial";
        } else if (genisCategory.equals("Evidencia Completa")) {
            codisCategory = "Forensic, Unknown";
        } else if (genisCategory.equals("Evidencia Mezcla")) {
            codisCategory = "Forensic Mixture";
        } else if (genisCategory.equals("Sospechoso")) {
            codisCategory = "Suspect";
        } else if (genisCategory.equals("Condenado")) {
            codisCategory = "Convicted Offender";
        } else {
            JOptionPane.showMessageDialog(convertirButton.getParent(), "Categoría no válida: " + genisCategory, "Error", JOptionPane.ERROR_MESSAGE);
        }
        return codisCategory;
    }

}
