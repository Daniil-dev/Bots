import com.vk.api.sdk.client.TransportClient;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.GroupActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.httpclient.HttpTransportClient;
import com.vk.api.sdk.objects.messages.*;
import com.vk.api.sdk.queries.messages.MessagesGetLongPollHistoryQuery;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

public class BotVK implements BotPrototype{

    private TransportClient transportClient;
    private VkApiClient     vkApi;
    private GroupActor      actor;
    private Integer         ts;

    private String botToken;
    private int botGroupID;

    private final HashMap<String, User> users;
    private final DataBase              dataBase;
    private final BotsCache             cache;

    private HashMap<String, BotPrototype> bots;
    public BotVK setBots(HashMap<String, BotPrototype> bots){
         this.bots = bots;
         return this;
    }


    BotVK(BotsCache cache, DataBase dataBase) {
        FileInputStream fis;
        Properties property = new Properties();

        try {
            fis = new FileInputStream("src/main/resources/config.properties");
            property.load(fis);

            botToken = property.getProperty("vk.token");
            botGroupID = Integer.parseInt(property.getProperty("vk.groupID"));

        } catch (IOException e) {
            System.err.println("ОШИБКА: Файл свойств отсуствует!");
        }


        transportClient = new HttpTransportClient();
        vkApi           = new VkApiClient(transportClient);
        actor           = new GroupActor(botGroupID, botToken);

        this.cache = cache;
        this.dataBase = dataBase;

        users = cache.getUsersVK();

        try { ts = vkApi.messages().getLongPollServer(actor).execute().getTs(); }
        catch (ApiException | ClientException e) { e.printStackTrace(); }

        // TODO: Придумать другое использование метода startSanding
        Thread sanding = new Thread(() -> startSanding(users, 22, 57, 20));
        sanding.start();
    }


    void inputMessage(){
        try {
            MessagesGetLongPollHistoryQuery historyQuery = vkApi.messages().getLongPollHistory(actor).ts(ts);
            List<Message> messages = historyQuery.execute().getMessages().getItems();

            if (messages.isEmpty()) {
                Thread.sleep(300);
                return;
            }

            for (Message message : messages) {
                String inputText = message.getText();
                String userID = message.getPeerId().toString();

                if (message.getFromId() == -201197245)
                    continue;

                textReview(inputText, userID, Messenger.VK, bots, cache, dataBase);
            }

            ts = vkApi.messages().getLongPollServer(actor).execute().getTs();
            Thread.sleep(300);
        } catch (Exception e) {
            System.out.println("Произошла ошибка в методе inputMessage() в классе BotVK");
            e.printStackTrace();
        }
    }

    @Override
    public void sendMsg(String userID, String text) {
        try {
            vkApi.messages()
                    .send(actor)
                    .message(text)
                    .userId(Integer.parseInt(userID))
                    .randomId(new Random().nextInt(10000))
                    .execute();
        } catch (ApiException | ClientException e) {
            e.printStackTrace();
        }
    }
    @Override
    public void sendMsg(String[][] exampleKeyboard, boolean select, String ID, String text){
        List<List<KeyboardButton>> myKeyboard = new ArrayList<>();

        for (String[] strings : exampleKeyboard) {
            List<KeyboardButton> line = new ArrayList<>();
            for (String string : strings) {
                line.add(new KeyboardButton().setAction(new KeyboardButtonAction()
                        .setLabel(string)
                        .setType(TemplateActionTypeNames.TEXT))
                        .setColor(KeyboardButtonColor.DEFAULT));
            }
            myKeyboard.add(line);
        }

        Keyboard keyboard = new Keyboard();
        keyboard.setButtons(myKeyboard);

        try {
            vkApi.messages()
                    .send(actor)
                    .message(text)
                    .userId(Integer.parseInt(ID))
                    .randomId(new Random().nextInt(10000))
                    .keyboard(keyboard)
                    .execute();
        } catch (ApiException | ClientException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        while (true){
            inputMessage();
        }
    }


    /*
    @SneakyThrows
    void inputMessag1e(){
        MessagesGetLongPollHistoryQuery historyQuery = vkApi.messages().getLongPollHistory(actor).ts(ts);
        List<Message> messages = historyQuery.execute().getMessages().getItems();


        if (messages.isEmpty()) {
            Thread.sleep(300);
            return;
        }

        for (Message message : messages) {
            String userID = "" + message.getPeerId();

            // System.out.println("peerID = "+userID);
            // System.out.println("userID = "+message.getFromId());
            // System.out.println("text   = "+message.getText() + "\n\n");
            // peerID = 144873193
            // userID = -201197245
            // text   = Доброе утро, Даниил! Какая у вас температура?
            if(message.getFromId() == -201197245)
                continue;


            if(message.getText().equals("/help")){
                String str = "Этот бот предназначен для ежедневного опроса людей о здоровье, " +
                        "бот вам сообщит, если ваши симптомы могут быть признаком covid-19, " +
                        "а также сообщит об безотложной нужде обратиться в больницу";
                sendMsg(userID, str);
                continue;
            }

            if(message.getText().equals("/setting")){
                String str = "Этот пунк находиться в разработке";
                sendMsg(userID, str);
                continue;
            }


            if (!users.containsKey(userID)) {
                cache.loadCache(Messenger.VK);  // 11111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111
                if (!users.containsKey(userID)) {
                    User user = new User(userID, UserStatus.registration);
                    user.setMessenger(Messenger.VK);  // 11111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111
                    users.put(userID, user);
                    String out = "Зарегистрируйтесь чтобы получать рассылку\n\nНапишите свое ИМЯ и ФАМИЛИЮ через пробел";
                    sendMsg(userID, out);
                    continue;
                }
            }

            User user = users.get(userID);
            UserStatus status = user.getStatus();

            switch (status) {
                case registration:
                    String registration = message.getText();

                    if (!registration.trim().contains(" ") || !validation(registration, 48, 57, false)) {
                        sendMsg(userID, "Похоже вы ошиблись при вводе, пожалуйста повторите попытку!");
                        continue;
                    }

                    String[] FIO = registration.split(" ");
                    user.setName(FIO[0]);
                    user.setSurname(FIO[1]);
                    dataBase.putUser(user);

                    sendMsg(userID, "Вы успешно зарегестрированны!");

                    status = UserStatus.waiting;

                    users.put(userID, user);
                    break;

                case temperature:
                    String temperature = message.getText();

                    if (!validation(temperature, 44, 57, true) || temperature.length() < 4) {
                        sendMsg(userID, "Возможно вы ошиблись!\nВведите температуру в формате XX,X или XX.X .");
                        continue;
                    }

                    try {
                        user.setTemperature(Float.parseFloat(temperature.replace(',', '.')));
                    } catch (Exception ignored) {
                        sendMsg(userID, "Возможно вы ошиблись!\nВведите температуру в формате XX,X или XX.X .");
                        continue;
                    }

                    status = UserStatus.feeling;
                    // Различие.Различие.Различие.Различие.Различие.Различие.Различие.Различие.Различие.Различие.Различие.Различие.
                    String[][] keyboardTemplateTemperature = new String[][]{
                            {"1. Кашель."},
                            {"2. Утомляемость."},
                            {"3. Боль в грудной клетке."},
                            {"4. Боли в мышцах и горле."},
                            {"5. Затрудненное дыхание и одышку."},
                            {"6. Нарушения речи или движения."},
                            {"7. Потеря вкуса и обоняния."},
                            {"8. Были контакты с больными."},
                            {"0. Закончить опрос"}
                    };
                    // Различие.Различие.Различие.Различие.Различие.Различие.Различие.Различие.Различие.Различие.Различие.Различие.


                    sendMsg(keyboardTemplateTemperature, userID, "Выберите, используя клавиатуру, какие из перечисленных симптомов у вас есть:" +
                            "\n1.Кашель." +
                            "\n2.Утомляемость." +
                            "\n3.Боль в грудной клетке." +
                            "\n4.Боли в мышцах и горле." +
                            "\n5.Затрудненное дыхание и одышку." +
                            "\n6.Нарушения речи или движения." +
                            "\n7.Потеря вкуса и обоняния." +
                            "\n8.Были контакты с больными." +
                            "\n0.Закончить опрос"
                    );
                    break;

                case feeling:

                    // TODO: добавить обработчик действий при разных уровнях риска
                    String feeling = message.getText();
                    boolean[] answer = user.getAnswers();

                    if (feeling.charAt(0) != '0') {
                        answer[Integer.parseInt("" + feeling.charAt(0)) - 1] = true;
                        continue;
                    }


                    // -------------------------------------------------------------------------------------------------
                    // =================================================================================================
                    boolean[] userAnswers = user.getAnswers();
                    for (int i = 0; i < userAnswers.length - 1; i++) {
                        if(userAnswers[i]){
                            switch (i){
                                case 0:
                                    user.addRiskLevel(25);
                                    break;
                                case 2:
                                case 4:
                                    user.addRiskLevel(15);
                                    break;
                                case 6:
                                    user.addRiskLevel(10);
                                    break;
                                case 1:
                                case 3:
                                case 5:
                                    user.addRiskLevel(5);
                                    break;
                            }
                        }
                    }

                    user.setRiskLevel(user.getRiskLevel() * (userAnswers[7] ? 0.9 : 1.4));

                    if(user.getTemperature() > 38.2)
                        user.addRiskLevel(25);
                    else if (user.getTemperature() > 37.2)
                        user.addRiskLevel(15);
                    else
                        user.addRiskLevel(5);

                    if(user.getRiskLevel() > 40){
                        String feelingTemp = "У " + user.getName() + " " + user.getSurname() + ", есть подозрение";
                        User respons = cache.getResponsible();
                        System.out.println("----------------------------------------- \n\nthis is respons " + respons + "\n\n -----------------------------------------");
                        BotPrototype dfdf;

                        if(respons.getMessenger().getTarget().equals("telegram"))
                            dfdf = bots.get("telegram");
                        else
                            dfdf = bots.get("vk");
                        //dfdf = bots.get(respons.getMessenger().getTarget());

                        System.out.println();
                        dfdf.sendMsg(respons.getID(), feelingTemp);
                    }
                    // =================================================================================================
                    // -------------------------------------------------------------------------------------------------


                    status = UserStatus.waiting;


                    // Различие.Различие.Различие.Различие.Различие.Различие.Различие.Различие.Различие.Различие.Различие.Различие.
                    String[][] keyboardTemplateFeeling = new String[][]{{"!help","!setting"}};

                    sendMsg(keyboardTemplateFeeling, userID, "Спасибо, что прошли опрос! Следуюший опрос начнется в тоже время.\nУровень риска: " + user.getRiskLevel());
                    // Различие.Различие.Различие.Различие.Различие.Различие.Различие.Различие.Различие.Различие.Различие.Различие.

                    StringBuilder sql = new StringBuilder();
                    StringBuilder values = new StringBuilder();

                    for (int i = 0; i < 8; i++) {
                        sql.append(", symptoms_").append(i + 1);

                        if (answer[i]) values.append(", " + 1);
                        else values.append(", " + 0);
                    }

                    System.out.println("INSERT INTO history (user_id, temperature" + sql + ")" +
                            " VALUES ((SELECT id FROM users where vk_userID='" + userID + "')," + user.getTemperature() + values + ")");
                    // INSERT INTO history (user_id, temperature, symptoms_1, symptoms_2, symptoms_3, symptoms_4, symptoms_5, symptoms_6, symptoms_7, symptoms_8) VALUES ((SELECT id FROM users where chatID='1433624697'), 36.6, 1, 1, 1, 1, 0, 0, 0, 0)
                    dataBase.insertUpdateSql("INSERT INTO history (user_id, temperature" + sql + ")" +
                            " VALUES ((SELECT id FROM users where vk_userID='" + userID + "')," + user.getTemperature() + values + ")");

                    break;

                case waiting:
                    sendMsg(userID, user.getName() + ", пожалуйста ожидайте следующего опроса.");
                    break;
            }

            user.setStatus(status);

        }

        ts = vkApi.messages().getLongPollServer(actor).execute().getTs();
        Thread.sleep(300);


    }

    */

}
