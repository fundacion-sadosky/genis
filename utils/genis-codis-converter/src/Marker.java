import com.opencsv.bean.CsvBindByName;

public class Marker{
    @CsvBindByName(column = "Sample Name", required = true)
    public String sampleName;
    @CsvBindByName(column = "Specimen Category", required = true)
    public String specimenCategory;
    @CsvBindByName(column = "UD1", required = true)
    public String user;
    @CsvBindByName(column = "UD2", required = true)
    public String kit;
    @CsvBindByName(column = "Marker", required = true)
    public String marker;
    @CsvBindByName(column = "Allele 1", required = true)
    public String allele1;
    @CsvBindByName(column = "Allele 2")
    public String allele2;
    @CsvBindByName(column = "Allele 3")
    public String allele3;
    @CsvBindByName(column = "Allele 4")
    public String allele4;
    @CsvBindByName(column = "Allele 5")
    public String allele5;
    @CsvBindByName(column = "Allele 6")
    public String allele6;
    @CsvBindByName(column = "Allele 7")
    public String allele7;
    @CsvBindByName(column = "Allele 8")
    public String allele8;

    public Marker() {
    }

}
