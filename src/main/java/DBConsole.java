import java.io.*;
import java.sql.*;
import java.util.Properties;
import java.util.Scanner;

public class DBConsole {

    private static final String PROPERTIES_PATH = "classes\\config.properties";
    private static Properties properties = new Properties();

    private enum listOfSupportedSQL {
        select, insert, update, delete, alter, create, drop;

        public static boolean contains(String s) {
            for (listOfSupportedSQL sql : values())
                if (sql.name().equals(s)) {
                    return true;
                }
            return false;
        }
    }

    private static String generateTableHTML(String newInput, Statement statement) throws SQLException {
        StringBuilder xmlBuilder = new StringBuilder("<tr>");
        try (ResultSet resultSet = statement.executeQuery(newInput)) {
            //table header
            for (int i = 1; i <= resultSet.getMetaData().getColumnCount(); i++) {
                String columnName = resultSet.getMetaData().getColumnName(i);
                xmlBuilder.append("<td>" + columnName + "</td>");
            }
            xmlBuilder.append("</tr>");

            //table body
            while (resultSet.next()) {
                xmlBuilder.append("<tr>");
                for (int i = 1; i <= resultSet.getMetaData().getColumnCount(); i++) {
                    String columnValue = resultSet.getString(resultSet.getMetaData().getColumnName(i));
                    xmlBuilder.append("<td>" + columnValue + "</td>");
                }
                xmlBuilder.append("</tr>");
            }
        }
        return xmlBuilder.toString();
    }

    private static void printTable(String newInput, Statement statement) throws SQLException, IOException {
        String htmlTable = generateTableHTML(newInput, statement);
        System.out.println(htmlTable.replace("<tr>", "\n").replace("<td>", " ")
                .replaceAll("</tr>|</td>", " "));

        try (Scanner scanner = new Scanner(new File(properties.getProperty("template_path")));
             FileWriter fileWriter = new FileWriter(properties.getProperty("output_html"))
        ) {
            String templateContent = scanner.useDelimiter("\\Z").next();
            templateContent = templateContent.replace("$body", htmlTable);
            fileWriter.append(templateContent);
            fileWriter.flush();
        }
    }

    private static void processSQL(String newInput, Statement statement) throws SQLException, IOException {
        String[] split = newInput.toLowerCase().split(" ");

        if ("select".equals(split[0])) {
            printTable(newInput, statement);
        } else if (listOfSupportedSQL.contains(split[0])) {
            int rowsCount = statement.executeUpdate(newInput);
            System.out.println(rowsCount);
        } else {
            System.out.println("Unsupported statement");
        }
    }

    public static void main(String args[]) throws SQLException, IOException {
        try (FileInputStream fileInputStream = new FileInputStream(PROPERTIES_PATH)) {
            properties.load(fileInputStream);

            try (Scanner scanner = new Scanner(System.in);
                 Connection connection = DriverManager.getConnection(properties.getProperty("dbpath"));
                 Statement statement = connection.createStatement()
            ) {
                while (scanner.hasNext()) {
                    try {
                        String newInput = scanner.nextLine();
                        processSQL(newInput, statement);
                    } catch (Exception e) {
                        System.out.println("SQL statement can't be processed: " + e.getMessage());
                    }
                }
            }
        }
    }
}
