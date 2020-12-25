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

        // ответ на определенное сообщение
        //sendMessage.setReplyToMessageId(message.getMessageId());

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

    /*public void onUpdateReceive1d(Update update) {
        //Создание объекта при помощи метода объекта update
        //Этот объект будет хранить сообщение
        Message message = update.getMessage();
        //message.getDate();
        if (message == null) return;

        String chatID = message.getChatId().toString();
        // вынести в отдельный метод к примеру public String anyMethod(String inputString, String ID, HashMap<String, User> users)
        // -------------------------------------------------------------------------------------------------------------
        if(message.getText().equals("/help")){
            String str = "Этот бот предназначен для ежедневного опроса людей о здоровье, " +
                    "бот вам сообщит, если ваши симптомы могут быть признаком covid-19, " +
                    "а также сообщит об безотложной нужде обратиться в больницу";
            sendMsg(chatID, str);
            return;
        }
        if(message.getText().equals("/setting")){
            String str = "Этот пунк находиться в разработке";
            sendMsg(chatID, str);
            return;
        }

        if (!users.containsKey(chatID)) {
            cache.loadCache(Messenger.TELEGRAM); // 11111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111
            if(!users.containsKey(chatID)) {
                User user = new User(chatID, UserStatus.registration);
                user.setMessenger(Messenger.TELEGRAM); // 11111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111
                users.put(chatID, user);
                String out = "Зарегистрируйтесь чтобы получать рассылку\n\nНапишите свое ИМЯ и ФАМИЛИЮ через пробел";
                sendMsg(chatID, out);
                return;
            }
        }

        User user = users.get(chatID);
        UserStatus status = user.getStatus();

        switch (status) {
            case registration:
                String registration = message.getText();

                // Валидация работает НОРМАЛЬНО
                if (!registration.trim().contains(" ") || !validation(registration, 48, 57, false)) {
                    sendMsg(chatID, "Похоже вы ошиблись при вводе, пожалуйста повторите попытку!");
                    return;
                }

                String[] FIO = registration.split(" ");
                user.setName(FIO[0]);
                user.setSurname(FIO[1]);
                dataBase.putUser(user);

                sendMsg(chatID, "Вы успешно зарегестрированны!");

                status = UserStatus.waiting;

                users.put(chatID, user);
                break;

            case temperature: // перенести отправку сообщения ботом в другое место
                String temperature = message.getText();

                if(!validation(temperature, 44, 57, true) || temperature.length() < 4) {
                    sendMsg(chatID, "Возможно вы ошиблись!\nВведите температуру в формате XX,X или XX.X .");
                    return;
                }

                try {
                    user.setTemperature(Float.parseFloat(temperature.replace(',','.')));
                } catch (Exception ignored){
                    sendMsg(chatID, "Возможно вы ошиблись!\nВведите температуру в формате XX,X или XX.X .");
                    return;
                }

                status = UserStatus.feeling;

                // Различие.Различие.Различие.Различие.Различие.Различие.Различие.Различие.Различие.Различие.Различие.Различие.
                SendMessage sendMessageTemperature = new SendMessage();
                setButtons(sendMessageTemperature, true, new String[][]{
                        {"1. Кашель."},
                        {"2. Утомляемость."},
                        {"3. Боль в грудной клетке."},
                        {"4. Боли в мышцах и горле."},
                        {"5. Затрудненное дыхание и одышку."},
                        {"6. Нарушения речи или движения."},
                        {"7. Потеря вкуса и обоняния."},
                        {"8. Были ли контакты с больными или с подозрением."},
                        {"0. Закончить опрос"}
                });
                // Различие.Различие.Различие.Различие.Различие.Различие.Различие.Различие.Различие.Различие.Различие.Различие.

                sendMsg(sendMessageTemperature, chatID, "Выберите, используя клавиатуру, какие из перечисленных симптомов у вас есть:" +
                        "\n1.Кашель." +
                        "\n2.Утомляемость." +
                        "\n3.Боль в грудной клетке." +
                        "\n4.Боли в мышцах и горле." +
                        "\n5.Затрудненное дыхание и одышку." +
                        "\n6.Нарушения речи или движения." +
                        "\n7.Потеря вкуса и обоняния." +
                        "\n8.Были контакты с больными или с подозрением." +
                        "\n0.Закончить опрос"
                );
                // Различие.Различие.Различие.Различие.Различие.Различие.Различие.Различие.Различие.Различие.Различие.Различие.
                sendMessageTemperature.setReplyMarkup(new ReplyKeyboardMarkup().setSelective(false));
                // Различие.Различие.Различие.Различие.Различие.Различие.Различие.Различие.Различие.Различие.Различие.Различие.
                break;

            case feeling:
                // ОПРОС ГОТОВ, НООООООО, нужно сделать оправку сообщени если сходиться очень много симтомов, также отправить сообщения ответственным по вопросу здоровья
                String feeling = message.getText();
                boolean[] answer = user.getAnswers();

                if (feeling.charAt(0) != '0'){
                    answer[Integer.parseInt("" + feeling.charAt(0)) - 1] = true;

                    return;
                }

                status = UserStatus.waiting;

                // Различие.Различие.Различие.Различие.Различие.Различие.Различие.Различие.Различие.Различие.Различие.Различие.

                SendMessage sendMessageFeeling = new SendMessage();
                setButtons(sendMessageFeeling, false, new String[][]{{"/help"}, {"/setting"}});
                sendMsg(sendMessageFeeling, chatID, "Спасибо, что прошли опрос! Следуюший опрос начнется в тоже время");

                // Различие.Различие.Различие.Различие.Различие.Различие.Различие.Различие.Различие.Различие.Различие.Различие.


                // Добавления опроса в БД
                StringBuilder sql1 = new StringBuilder();
                StringBuilder values = new StringBuilder();

                for (int i = 0; i < 8; i++) {
                    sql1.append(", symptoms_").append(i + 1);

                    if (answer[i]) values.append(", " + 1);
                    else values.append(", " + 0);
                }


                // INSERT INTO history (user_id, temperature, symptoms_1, symptoms_2, symptoms_3, symptoms_4, symptoms_5, symptoms_6, symptoms_7, symptoms_8) VALUES ((SELECT id FROM users where chatID='1433624697'), 36.6, 1, 1, 1, 1, 0, 0, 0, 0)
                dataBase.insertUpdateSql("INSERT INTO history (user_id, temperature" + sql1 + ")" +
                        " VALUES ((SELECT id FROM users where chatID='" + chatID + "')," + user.getTemperature() + values + ")");

                // TODO: нужно сделать подсчет СТЕПЕНИ РИСКА, а также создать в базе данных пункт или определить его в программе,
                // TODO: который позволит определить ответственное лицо и сделать отправку сообщений ему
                *//*

                // еще думать и думать на этим....................................

                1.Кашель; (20) +++++
                2.Утомляемость; (5)
                3.Боль в грудной клетке; (15) +++++
                4.Боли в мышцах и горле;(5)
                5.Затрудненное дыхание и одышка;(15) +++++
                6.Нарушения речи или движения;(5)
                7.Потеря вкуса и обоняния; (10) // один из уникальных симптомов, стоит выделить отдельно
                // 20+5+15+5+15+5+10 = 75
                // 75 * 1.4 =
                // 75 * 0.9
                // < 30% = здоров почти как БЫК
                // > 30% = есть схожие симтомы, но скорее всего это что-то другое
                // > 40% = есть вероятность
                // > 50% = сходитсья уже довольно много
                // > 65% = этому человеку нужно держаться подальше от людей и пройти обследование
                // > 80% = срочно на карантин и в больницу
                // > 100% = вообще трэшак
                8.Были контакты с больными или с подозрением (*N.NN)

                // Изначальный вариант анализа ответов.
                // Возможно плохой, так что лучше в процентном соотношении
                [1,1,1,1,1,1,1,1] - У вас сходяться все симптомы! Чтобы обезопасить близких и других людей, старайтесь держаться на расстоянии, а также вам нужно немедленно обратиться в больницу
                [1,0,1,0,1,0,1,1] - Ваши симтопмы могут говорить о том, что у вас covid-19! Вам следует держаться на расстояни от людей и носить маску, также вам срочно нужно обратитсья в больницу


                Сухой кашель считается основным признаком коронавируса.
                Причем он появляется приступами, которые могут продолжаться от нескольких минут до часа.
                Боль в горле при этом присутствует не всегда.

                Самочувствие может меняться ежедневно.
                Периодически человек будет чувствовать себя почти выздоровевшим,
                но на следующий день может ощутить симптоматику с новыми силами.

                !!!!!!!!
                повышение температуры выше 38.5 градусов, которая сохраняется дольше трех дней;
                ощущение тесноты, нехватки воздуха, боли в груди;
                увеличение частоты вдохов (больше 20 в минуту);
                учащение сердцебиения (от 100 ударов за минуту);
                снижение артериального давления ниже 90 мм рт. ст.;
                обморочное состояние вплоть до потери сознания;
                посинение губ.
                !!!!!!!!

                Есть несколько советов, как отличить коронавирус от простуды у взрослых.
                Во-первых, Covid-19 не развивается через 2-3 дня после заражения.
                Во-вторых, он редко протекает с невысокой температурой, насморком, чиханием и слезоточивостью.
                Эти симптомы характерны для ОРВИ.

                !!!!!!!!
                Немедленно обратитесь к врачу, если у вас повысилась температура,
                появились одышка, кашель, боль в грудной клетке, нарушения речи или движения.
                !!!!!!!!
                *//*
                break;

            case waiting:
                sendMsg(chatID, user.getName() + ", пожалуйста ожидайте следующего опроса.");
                break;
        }

        user.setStatus(status);
        // -------------------------------------------------------------------------------------------------------------
    }
   */


}
