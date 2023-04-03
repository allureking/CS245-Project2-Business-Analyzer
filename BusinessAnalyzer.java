import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.Year;
import java.util.*;

class Data{
    public static String[] attributes;
    String[] values;
}

public class BusinessAnalyzer {

    /**
     * read the file to list
     * @param filepath file path
     * @param list list container
     * @throws IOException
     */
    public static void readData(String filepath, List<Data> list) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(filepath));
        String line;
        String head = br.readLine();
        String[] columns = head.split(",");
        Data.attributes = columns;
        while((line = br.readLine()) != null){
            Data data = new Data();
            int index = 0;
            data.values = new String[Data.attributes.length];
            String current = "";
            boolean ignore = false;

            for (int i = 0; i < line.length(); i++) {
                if(line.charAt(i) == ',' && !ignore){
                    data.values[index++] = current;
                    current = "";
                    continue;
                }

                current += line.charAt(i);
                if(line.charAt(i) == '"'){
                    ignore = !ignore;
                }
            }
            data.values[index] = current;

            list.add(data);
        }
    }

    /**
     * main entry point
     * @param args, args[0] should be filepath, args[1] should be list type
     */
    public static void main(String[] args) {
        if(args.length != 2){
            System.out.println("Usage: java BusinessAnalyzer <CSVFilePath> <ListType(AL/LL)>");
            return;
        }


        List<Data> list;
        if(args[1].toLowerCase().equalsIgnoreCase("AL")){
            list = new ArrayList<>();
        }else if(args[1].equalsIgnoreCase("LL")){
            list = new LinkedList<>();
        }else{
            System.out.println("Unknown data structure");
            return;
        }

        try {
            readData(args[0], list);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        List<List<Data>> processedData = process(list, args[1]);

        Scanner scanner = new Scanner(System.in);
        Queue<String> history = new LinkedList<>();
        while(true){
            System.out.print("Command: ");
            String command = scanner.nextLine();
            if(command.equalsIgnoreCase("quit")){
                break;
            }

            if(command.equalsIgnoreCase("history")){
                for (String s : history) {
                    System.out.println(s);
                }
            } else if (command.toUpperCase().startsWith("ZIP")) {
                String zip = command.split(" ")[1];
                zipSummary(processedData, zip);
            } else if (command.toUpperCase().startsWith("NAICS")) {
                String code = command.split(" ")[1];
                codeSummary(processedData, code);
            } else if(command.equalsIgnoreCase("Summary")){
                summary(processedData);
            }

            history.add(command);
        }
    }

    /**
     * summary by the NAICS Code
     * @param processedData list of list, each of one type of NAICS Code
     * @param code NAICS code
     */
    private static void codeSummary(List<List<Data>> processedData, String code) {
        int index = -1;
        for (int i = 0; i < processedData.size(); i++) {
            if(!processedData.get(i).get(0).values[16].contains("-")){
                continue;
            }
            String[] range = processedData.get(i).get(0).values[16].split("-");
            if(Integer.parseInt(code) >= Integer.parseInt(range[0]) &&
                    Integer.parseInt(code) <= Integer.parseInt(range[1])) {
                index = i;
                break;
            }
        }

        int total = 0;
        Set<String> zips = new HashSet<>();
        Set<String> neighbor = new HashSet<>();
        for (Data data : processedData.get(index)) {
            total += 1;
            neighbor.add(data.values[23]);
            zips.add(data.values[14]);
        }

        System.out.println("Total Businesses: "+total);
        System.out.println("Zip Codes: "+zips.size());
        System.out.println("Neighborhood: "+neighbor.size());
    }

    /**
     * summary by the zip code
     * @param processedData list of list, each of one type of NAICS Code
     * @param zip zip code
     */
    private static void zipSummary(List<List<Data>> processedData, String zip) {
        int total = 0;
        Set<String> type = new HashSet<>();
        Set<String> neighbor = new HashSet<>();
        for (List<Data> dataList : processedData) {
            for (Data data : dataList) {
                if(data.values[14].strip().equals(zip)){
                    total += 1;
                    neighbor.add(data.values[23]);
                    type.add(data.values[16]);
                }
            }
        }

        System.out.println(zip+" Business Summary");
        System.out.println("Total Businesses: "+total);
        System.out.println("Business Types: "+type.size());
        System.out.println("Neighborhood: "+neighbor.size());
    }

    /**
     * process the data from list to list of list
     * @param list the list of records in csv
     * @param type the list type
     * @return list of list, each of one type of NAICS code
     */
    private static List<List<Data>> process(List<Data> list, String type) {
        List<List<Data>> processed;
        if(type.equalsIgnoreCase("AL")){
            processed = new ArrayList<>();
        }else{
            processed = new LinkedList<>();
        }

        for (Data data : list) {
            int index = -1;
            String code = data.values[16];
            for (int i = 0; i < processed.size(); i++) {
                if(processed.get(i).get(0).values[16].equalsIgnoreCase(code)){
                    index = i;
                    break;
                }
            }

            if(index == -1){
                if(type.equalsIgnoreCase("AL")){
                    processed.add(new ArrayList<>());
                }else{
                    processed.add(new LinkedList<>());
                }
                processed.get(processed.size()-1).add(data);
            }else{
                processed.get(index).add(data);
            }
        }
        return processed;
    }

    /**
     * do summary
     * @param list list of list, each of one type of NAICS code
     */
    private static void summary(List<List<Data>> list) {
        int newBusiness = 0;
        int closed = 0;
        int total = 0;

        String lastYear = Year.now().minusYears(1).toString();

        for (List<Data> dataList : list) {
            for (Data data : dataList) {
                if(data.values[8].endsWith(lastYear)){
                    newBusiness++;
                }
                if(data.values[9].strip().length() != 0){
                    closed++;
                }
                total++;
            }
        }

        System.out.println("Total Businesses: "+total);
        System.out.println("Closed Businesses: "+closed);
        System.out.println("New Business in last year: "+ newBusiness);
    }
}
