import java.sql.*;

public class DataBase {
    //DESKTOP-0HJNGIA\MSSQL  price_tag
    private final String dataBase = "HealthBot";
    private final String url      = "jdbc:sqlserver://DESKTOP-0HJNGIA\\MSSQL;databaseName=" + dataBase;
    private final String user     = "sa";
    private final String password = "1234";

    private Connection connection;
    private Statement  statement;

    /*public BD() {
        try {
            // создаем что-то нужное
            connection = DriverManager.getConnection(url, user, password);
            // создаем что-то нужное из чего-то нужного
            Statement statement = connection.createStatement();


            // пример кода для вставки новой строки в таблицу category
//          statement.executeUpdate("INSERT INTO category (name) VALUES ('THE TEST')");


            // вызываем метод executeQuery из объекта statement и передаем строку с SQL запросом
            // результат выполнения записываем в переменную result
            ResultSet result = statement.executeQuery("SELECT * FROM category where id=5");

            // перебераем внутри цикла объект result. с помощью метода getString() получаем строку в указаном столбце по имени или индексу
            // В.А.Ж.Н.О!!! если не использовать метод "next()" то просто взять результат(строку) не получиться даже если она одна
            // изначально значение - это 0 любой результат находиться начиная с 1 строки
            while(result.next()){
                String name = result.getString("name");
                System.out.println(name);
            }


            // пример вывода значения БЕЗ использование цикла
            result = statement.executeQuery("SELECT * FROM category where id=7");
            result.next();
            System.out.println(result.getString("name"));


            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }*/

    public void openConnection(){
        try {
            connection = DriverManager.getConnection(url, this.user, password);
            statement = connection.createStatement();

        } catch (SQLException e) {
            e.printStackTrace();
        }

    }
    public void closeConnection(){
        try {
            connection.close();
            statement = null;
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public void putUser(User user){
        String sql;
        if(findUser(user)) {
            String id = user.getMessenger().getTypeID() + "=" + user.getID();
            String target = "target='" + user.getMessenger().getTarget() + "'";

            sql = "UPDATE users SET " + id + ", " + target + " WHERE name='" + user.getName() + "' AND surname='" + user.getSurname() + "'";
        }else {

            String typeID = user.getMessenger().getTypeID();

            String name = "'" + user.getName() + "'";
            String surname = "'" + user.getSurname() + "'";
            String ID = "'" + user.getID() + "'";
            String target = "'" + user.getMessenger().getTarget() + "'";

            sql = "INSERT INTO users (name, surname, " + typeID + ", target) VALUES (" + name + "," + surname + "," + ID + "," + target + ")";
        }
        insertUpdateSql(sql);
    }

    private boolean findUser(User user){
        ResultSet resultSet;
        String sql = "SELECT * FROM users WHERE name='" + user.getName() + "' AND surname='" + user.getSurname() + "'";

        resultSet = insertQuerySql(sql);

        try {
            resultSet.next();
            resultSet.getString("name");
            resultSet.getString("surname");
            closeConnection();
            return true;
        }catch (SQLException ignored){ }

        closeConnection();
        return false;
    }

    public ResultSet insertQuerySql(String sqlRequest){
        openConnection();

        ResultSet result = null;
        try {
            result = statement.executeQuery(sqlRequest);
        } catch (SQLException e) {
            System.out.println("Запрос не был вополнен! Строка запроса:");
            System.out.println(sqlRequest);
            System.out.println("Текст ошибки:");
            System.out.println(e.toString());
        }
        return result;
    }
    public void insertUpdateSql(String sqlRequest){
        openConnection();
        try {
            statement.executeUpdate(sqlRequest);
        } catch (SQLException e) {
            System.out.println("Запрос не был вополнен! Строка запроса:");
            System.out.println(sqlRequest);
            System.out.println("Текст ошибки:");
            System.out.println(e.toString());
        }

        closeConnection();
    }

}
