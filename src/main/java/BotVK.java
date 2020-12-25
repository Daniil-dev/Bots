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
        
        // устанавливает время рассылки: часы, миныту, секунды
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

}
