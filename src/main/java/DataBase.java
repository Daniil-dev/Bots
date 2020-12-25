import java.sql.*;

public class DataBase {
    private final String dataBase = "Name your database";
    private final String url      = "your link to the database" + dataBase;
    private final String user     = "User";
    private final String password = "Password";

    private Connection connection;
    private Statement  statement;

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
