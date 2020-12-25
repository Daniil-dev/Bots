import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.exceptions.TelegramApiRequestException;

import java.util.HashMap;


public class Main {
    public static void main(String[] args) {
        BotsCache cache = new BotsCache();
        DataBase dataBase = new DataBase();

        HashMap<String, BotPrototype> bots = new HashMap<>();

        BotVK vk;
        BotTelegram telegram;


        TelegramBotsApi telegramBotsApi;
        try {
            ApiContextInitializer.init();
            telegramBotsApi = new TelegramBotsApi();
            telegramBotsApi.registerBot(telegram = new BotTelegram(cache, dataBase).setBots(bots)); // telegram
                                        vk       = new BotVK      (cache,  dataBase).setBots(bots); // vk

            bots.put("telegram", telegram);
            bots.put("vk", vk);

            vk.run();
        }
        catch (TelegramApiRequestException ignored){}




    }
}
