import lombok.Getter;
import lombok.Setter;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

@Getter
@Setter
public class BotsCache {
    private final HashMap<String, User> usersTelegram;
    private final HashMap<String, User> usersVK;
    private       User                  responsible;

    public BotsCache() {
        usersTelegram = new HashMap<>();
        usersVK       = new HashMap<>();

        loadCache(Messenger.TELEGRAM);
        loadCache(Messenger.VK);
    }

    public void loadCache(Messenger messenger){
        String sql = "SELECT * FROM users where target='" + messenger.getTarget() + "'";
        ResultSet result;
        DataBase dataBase = new DataBase();
        if ((result = dataBase.insertQuerySql(sql)) == null)
            return;

        try {
            while (result.next()) {
                User user = new User(
                        result.getString("name"),
                        result.getString("surname"),
                        result.getString(messenger.getTypeID()),
                        UserStatus.waiting);

                user.setMessenger(messenger);

                switch (messenger){
                    case TELEGRAM: usersTelegram.put(result.getString(messenger.getTypeID()), user); break;
                    case VK:       usersVK.put(result.getString(messenger.getTypeID()), user); break;
                }

                if(result.getString("responsible").equals("1")) {
                    responsible = user;
                }
            }
            dataBase.closeConnection();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public void loadAllCache(){
        Messenger[] messengers = Messenger.values();
        for (Messenger messenger : messengers) {
            loadCache(messenger);
        }

    }


}
