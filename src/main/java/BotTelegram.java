import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

public class BotTelegram extends TelegramLongPollingBot implements BotPrototype{
    private String botToken;
    private String botName;

    private final HashMap<String, User> users;
    private final DataBase              dataBase;
    private final BotsCache             cache;

    private HashMap<String, BotPrototype> bots;
    public BotTelegram setBots(HashMap<String, BotPrototype> bots){
        this.bots = bots;
        return this;
    }

    public BotTelegram(BotsCache cache, DataBase dataBase){
        this.cache = cache;
        this.dataBase = dataBase;

        users = cache.getUsersTelegram();

        FileInputStream fis;
        Properties property = new Properties();

        try {
            fis = new FileInputStream("src/main/resources/config.properties");
            property.load(fis);

            botToken = property.getProperty("telegram.token");
            botName = property.getProperty("telegram.name");

        } catch (IOException e) {
            System.err.println("ОШИБКА: Файл свойств отсуствует!");
        }

        Thread sanding = new Thread(() -> startSanding(users, 16, 45, 30));
        sanding.start();
    }


    @Override
    public void sendMsg(String chatID, String text){
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);

        // определяет в какой чат нужно отправить сообщение
        sendMessage.setChatId(chatID);

        sendMessage.setText(text);

        // отправка сообщения
        try {
            //setButtons(sendMessage);
            sendMessage(sendMessage);
        }catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    @Override
    public void sendMsg(String[][] exampleKeyboard, boolean select, String ID, String text){
        SendMessage sendMessage = new SendMessage();
        // -------------------------------------Клавиатура--------------------------------------------------------------
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();

        sendMessage.setReplyMarkup(replyKeyboardMarkup);

        replyKeyboardMarkup.setSelective(select);//
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboard(false);


        List<KeyboardRow> keyboardRowList = new ArrayList<>();

        for (int i = 0; i < exampleKeyboard.length; i++) {
            // создаем объект KeyboardRow
            KeyboardRow keyboardRow = new KeyboardRow();
            for (int j = 0; j < exampleKeyboard[i].length; j++) {
                // добавляем кнопки в этот объект
                keyboardRow.add(exampleKeyboard[i][j]);
            }
            // добавляем объект(строку) keyboardRow1 в список
            keyboardRowList.add(keyboardRow);
        }
        // устанавливаем эту клавиатуру
        replyKeyboardMarkup.setKeyboard(keyboardRowList);
        // -------------------------------------Клавиатура-конец--------------------------------------------------------

        sendMessage.enableMarkdown(true);
        // определяет в какой чат нужно отправить сообщение
        sendMessage.setChatId(ID);
        // устанавливаем текст сообщения
        sendMessage.setText(text);

        try {
            sendMessage(sendMessage);
        }catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onUpdateReceived(Update update){
        Message message = update.getMessage();
        if (message == null) return;

        String inputText = message.getText();
        String chatID = message.getChatId().toString();

        textReview(inputText, chatID, Messenger.TELEGRAM, bots, cache, dataBase);
    }
    @Override
    public String getBotUsername() {
        return botName;
    }
    @Override
    public String getBotToken() {
        return botToken;
    }

}
