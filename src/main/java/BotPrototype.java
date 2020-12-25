import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public interface BotPrototype {

    // проверяет содержиться ли СИМВОЛЫ из СПИСКА в ПЕРЕДАНОЙ СТРОКЕ. 3 параметр проверяет должны ли символы содердаться или нет
    default boolean validation(String validationString, String[] validationsChars, boolean validChars){
        boolean contains = false;

        for (String validationsChar : validationsChars) {
            if (validationString.contains(validationsChar)) {
                contains = true;
                break;
            }
        }

        return contains == validChars;
    }
    // проверяет содержиться ли СИМВОЛЫ из диапазона переданного 2 и 3 параметрами
    default boolean validation(String validationString, int startRange, int endRange, boolean validChars){
        boolean contains = false;

        for (int i = startRange; i <= endRange; i++) {
            char temp = (char) i;
            if (validationString.contains(""+temp)) {
                contains = true;
                break;
            }
        }
        return contains == validChars;
    }


    // Метод для рассылки сообщений пользователям, а также отправная точка опроса пользователей
    default void startSanding(HashMap<String, User> users, int hour, int minute, int second) {
        GregorianCalendar calendarTarget  = new GregorianCalendar();
        GregorianCalendar calendarNow = new GregorianCalendar();

        calendarTarget.set(Calendar.HOUR_OF_DAY, hour); // время опроса - часы
        calendarTarget.set(Calendar.MINUTE,      minute); // время опроса - минуты
        calendarTarget.set(Calendar.SECOND,      second); // если не указать секунды бот будет спамить!!!!!

        if(calendarNow.after(calendarTarget)) { // false   // now(15:52) > target(15:54)
            calendarTarget.add(Calendar.DAY_OF_MONTH, 1);

            long previously =  60_000 * 10; // колличество минут на которое поток проснется раньше (60_000 * X) - где X - это кол-во минут
            long now = calendarNow.getTime().getTime();
            long target = calendarTarget.getTime().getTime() - previously;

            try { Thread.sleep(target - now);} // 86_400_000
            catch (InterruptedException ignored) { }
        }

        while (calendarNow.before(calendarTarget)){ // now(15:53) < target(15:54)
            calendarNow.setTime(new Date());
            try { Thread.sleep(5000); }
            catch (InterruptedException ignored) { }
        }

        List<String> keys = new ArrayList<>(users.keySet());

        for (String key : keys) {
            User user = users.get(key);
            if(user.getStatus() == UserStatus.temperature)
                continue;
            user.setStatus(UserStatus.temperature);
            sendMsg(user.getID(), "Доброе утро, " + user.getName() + "! Какая у вас температура?");
        }

        try { Thread.sleep(3000);
        } catch (InterruptedException ignored) { }

        startSanding(users, hour, minute, second);
    }

    void sendMsg(String ID, String text);

    void sendMsg(String[][] keyboardSample, boolean select, String ID, String text);


    // TODO: HashMap<String, BotPrototype> bots можно вынести в BotCache и разгрузить метод и DataBase dataBase впринципе также можно вынести в BotCache
    default void textReview(String inputText, String ID, Messenger messenger, HashMap<String, BotPrototype> bots, BotsCache cache, DataBase dataBase){
        HashMap<String, User> users;
        String[][] keyboardSample;

        switch (messenger){
            case TELEGRAM: users = cache.getUsersTelegram();
                break;
            case VK: users = cache.getUsersVK();
                break;
            default: System.out.println("ОШИБКА! НЕ ПРАВИЛЬНЫЙ ТИП МЕССЕНДЖЕРА");
                return;
        }


        if (!users.containsKey(ID)) {
            cache.loadAllCache();
            if (!users.containsKey(ID)) {
                User user = new User(ID, UserStatus.registration);
                user.setMessenger(messenger);
                users.put(ID, user);
                keyboardSample = new String[][] {{"Отмена"}};
                sendMsg(keyboardSample, true, ID, "Зарегистрируйтесь чтобы получать рассылку\n\nНапишите свое ИМЯ и ФАМИЛИЮ через пробел");
                return;
            }
        }

        User user = users.get(ID);
        UserStatus status = user.getStatus();

        if(inputText.equals("/help")){
            String str = "Этот бот предназначен для ежедневного опроса людей о здоровье, " +
                    "бот вам сообщит, если ваши симптомы могут быть признаком covid-19, " +
                    "а также сообщит об безотложной нужде обратиться в больницу";
            sendMsg(ID, str);
            return;
        }
        else if(inputText.equals("/setting")){
            String str = "Выберите что вы хотите настроить:" +
                    "\n• Изменить место рассылки" +
                    "\n• Отписаться от рассылки" +
                    "\n• Назад";
            keyboardSample = new String[][] {
                    {"Изменить место рассылки"},
                    {"Отписаться от рассылки"},
                    {"Отмена"}};


            user.setStatus(UserStatus.setting);

            sendMsg(keyboardSample,true,ID, str);
            return;
        }

        switch (status) {
            case registration:
                if(inputText.equals("Отмена"))
                    return;
                if (!inputText.trim().contains(" ") || !validation(inputText, 48, 57, false)) {
                    sendMsg(ID, "Похоже вы ошиблись при вводе, пожалуйста повторите попытку!");
                    return;
                }

                String[] FIO = inputText.split(" ");
                user.setName(FIO[0]);
                user.setSurname(FIO[1]);
                dataBase.putUser(user);

                sendMsg(ID, "Вы успешно зарегестрированны!");

                status = UserStatus.waiting;

                users.put(ID, user);
                break;

            case temperature:
                if (!validation(inputText, 44, 57, true) || inputText.length() < 4) {
                    sendMsg(ID, "Возможно вы ошиблись!\nВведите температуру в формате XX,X или XX.X .");
                    return;
                }

                try {
                    user.setTemperature(Float.parseFloat(inputText.replace(',', '.')));
                } catch (Exception ignored) {
                    sendMsg(ID, "Возможно вы ошиблись!\nВведите температуру в формате XX,X или XX.X .");
                    return;
                }

                status = UserStatus.feeling;

                keyboardSample = new String[][]{
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
                sendMsg(keyboardSample, true, ID, "Выберите, используя клавиатуру, какие из перечисленных симптомов у вас есть:" +
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
                boolean[] answer = user.getAnswers();

                if (inputText.charAt(0) != '0') {
                    answer[Integer.parseInt("" + inputText.charAt(0)) - 1] = true;
                    return;
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

                // TODO: добавить обработчик действий при разных уровнях риска
                if(user.getRiskLevel() > 40){
                    String feelingTemp = "У " + user.getName() + " " + user.getSurname() + ", есть подозрение на covid-19";
                    User respons = cache.getResponsible();
                    BotPrototype botPrototype;

                    if(respons.getMessenger().getTarget().equals("telegram"))
                        botPrototype = bots.get("telegram");
                    else
                        botPrototype = bots.get("vk");
                    //botPrototype = bots.get(respons.getMessenger().getTarget());

                    botPrototype.sendMsg(respons.getID(), feelingTemp);
                }
                // =================================================================================================
                // -------------------------------------------------------------------------------------------------


                status = UserStatus.waiting;


                keyboardSample = new String[][]{{"/help","/setting"}};
                sendMsg(keyboardSample, false, ID, "Спасибо, что прошли опрос! Следуюший опрос начнется в тоже время.\nУровень риска: " + user.getRiskLevel());

                StringBuilder sql = new StringBuilder();
                StringBuilder values = new StringBuilder();

                for (int i = 0; i < 8; i++) {
                    sql.append(", symptoms_").append(i + 1);

                    if (answer[i]) values.append(", " + 1);
                    else values.append(", " + 0);
                }

                // INSERT INTO history (user_id, temperature, symptoms_1, symptoms_2, symptoms_3, symptoms_4, symptoms_5, symptoms_6, symptoms_7, symptoms_8) VALUES ((SELECT id FROM users where chatID='1433624697'), 36.6, 1, 1, 1, 1, 0, 0, 0, 0)
                dataBase.insertUpdateSql("INSERT INTO history (user_id, temperature" + sql + ")" + " VALUES ((SELECT id FROM users where " + messenger.getTypeID() + "='" + ID + "')," + user.getTemperature() + values + ")");
                break;

            case waiting:
                keyboardSample = new String[][]{{"/help","/setting"}};
                sendMsg(keyboardSample, true, ID, user.getName() + ", пожалуйста ожидайте следующего опроса.");
                break;

            case setting:
                if(inputText.equals("Изменить место рассылки")){
                    status = UserStatus.change;
                    String settingTemp = "Где вы хотите получать рассылку:" +
                            "\n• ВКонтакте" +
                            "\n• Телеграмме" +
                            "\n• Назад";
                    keyboardSample = new String[][] {{"ВКонтакте"}, {"Телеграмм"}, {"Назад"}};

                    sendMsg(keyboardSample, true, ID, settingTemp);
                }

                if (inputText.equals("Отписаться от рассылки")) {
                    status = UserStatus.delete;
                    users.remove(ID);
                }

                if (inputText.equals("Отмена")) {
                    status = UserStatus.waiting;

                    keyboardSample = new String[][]{{"/help","/setting"}};
                    sendMsg(keyboardSample, true, ID, user.getName() + ", пожалуйста ожидайте следующего опроса.");
                }

                break;
            case change:
                ResultSet resultSet;
                Messenger changeTo;

                if(inputText.equals("ВКонтакте")) {
                    changeTo = Messenger.VK;
                    resultSet = dataBase.insertQuerySql("SELECT " + changeTo.getTypeID() + " FROM users WHERE " + messenger.getTypeID() + "=" + ID);

                    try {
                        resultSet.next();
                        if(resultSet.getString(changeTo.getTypeID()) == null) {
                            sendMsg(ID, "У меня, к сожалению нет вашего ВКонтакте" +
                                    "\nНайдите меня там и зарегистрируйтесь");
                            return;
                        }
                        if(messenger == changeTo){
                            sendMsg(ID, "Вы уже получаете рассылку в ВКонтакте!");
                            return;
                        }

                        users.remove(ID);
                        dataBase.insertUpdateSql("UPDATE users SET target='" + changeTo.getTarget() + "' WHERE " + messenger.getTypeID() + "=" + ID);
                        cache.getUsersVK().put(ID, user);

                        keyboardSample = new String[][]{{"/help","/setting"}};
                        sendMsg(keyboardSample, true, ID, "Теперь рассылка будет в ВКонтакте!");

                        status = UserStatus.waiting;
                    } catch (Exception e) {
                        System.out.println("ERROR");
                        System.out.println(e.toString());
                    }

                }

                if(inputText.equals("Телеграмм")) {
                    changeTo = Messenger.TELEGRAM;
                    resultSet = dataBase.insertQuerySql("SELECT " + changeTo.getTypeID() + " FROM users WHERE " + messenger.getTypeID() + "=" + ID);

                    try {
                        resultSet.next();
                        if(resultSet.getString(changeTo.getTypeID()) == null) {
                            sendMsg(ID, "У меня, к сожалению нет вашего Телеграмма" +
                                    "\nНайдите меня там и зарегистрируйтесь");
                            return;
                        }

                        if(messenger == changeTo){
                            sendMsg(ID, "Вы уже получаете рассылку в Телеграмме!");
                            return;
                        }

                        users.remove(ID);
                        dataBase.insertUpdateSql("UPDATE users SET target='" + changeTo.getTarget() + "' WHERE " + messenger.getTypeID() + "=" + ID);
                        cache.getUsersVK().put(ID, user);

                        keyboardSample = new String[][]{{"/help","/setting"}};
                        sendMsg(keyboardSample, true, ID, "Теперь рассылка будет в Телеграмме!");

                        status = UserStatus.waiting;
                    } catch (Exception e) {
                        System.out.println("ERROR");
                        System.out.println(e.toString());
                    }

                }

                if(inputText.equals("Назад")){
                    String str = "Выберите что вы хотите настроить:" +
                            "\n• Изменить место рассылки" +
                            "\n• Отписаться от рассылки" +
                            "\n• Назад";
                    keyboardSample = new String[][] {
                            {"Изменить место рассылки"},
                            {"Отписаться от рассылки"},
                            {"Отмена"}};

                    status = UserStatus.setting;

                    sendMsg(keyboardSample,true,ID, str);
                }

                break;
        }

        user.setStatus(status);
    }

    /*

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
                */

}
