import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
enum Messenger{
    TELEGRAM("telegram", "chatID"), VK("vk","vk_userID"), UNDEFINED("UNDEFINED","UNDEFINED");

    private final String target;
    private final String typeID;

    Messenger(String target, String typeID) {
        this.target = target;
        this.typeID = typeID;
    }

}

enum UserStatus {
    registration, temperature, feeling, waiting, setting, change, delete
}

@Getter
@Setter
@ToString
public class User {

    private final String     ID;
    private final boolean[]  answers;
    private       float      temperature;
    private       String     name;
    private       String     surname;
    private       UserStatus status;
    private       Messenger  messenger;

    private       double     riskLevel;

    public User(String name, String surname, String id, UserStatus status){
        answers         = new boolean[8];
        ID              = id;
        this.name       = name;
        this.surname    = surname;
        this.status     = status;
        this.messenger  = Messenger.UNDEFINED;
    }

    public User(String chatID, UserStatus status){
        answers     = new boolean[8];
        ID          = chatID;
        this.status = status;
        this.messenger  = Messenger.UNDEFINED;
    }

    public void addRiskLevel(double riskLevel){
        this.riskLevel += riskLevel;
    }

}
