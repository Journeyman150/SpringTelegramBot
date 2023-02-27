package ru.anisimovvs.telegramspringbot.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.anisimovvs.telegramspringbot.config.BotConfig;
import ru.anisimovvs.telegramspringbot.model.User;
import ru.anisimovvs.telegramspringbot.model.UserRepository;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {

    private UserRepository userRepository;
    final BotConfig config;
    static final String HELP_TEXT = "Этот проект создан в качестве заготвоки для последующей реализации идей для бота на Java Spring\n\n" +
            "Список реализованных команд: \n\n" +
            "Введите /start чтобы зарегистироваться\n\n" +
            "Введите /help чтобы вывести описание бота";

    @Autowired
    public TelegramBot (BotConfig config, UserRepository userRepository) {
        this.config = config;
        this.userRepository = userRepository;
        List<BotCommand> listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand("/start", "регистрация"));
        listOfCommands.add(new BotCommand("/mydata", "отобразить мои данные"));
        listOfCommands.add(new BotCommand("/deletedata", "удалить данные обо мне"));
        listOfCommands.add(new BotCommand("/help", "описание бота"));
        listOfCommands.add(new BotCommand("/settings", "настройки"));

        try {
            this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error("Error setting bot's command list: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();
            String name = update.getMessage().getChat().getFirstName();

            switch (messageText) {
                case "/start" -> {
                    boolean registered = registerUser(update.getMessage());
                    if (registered) {
                        sendMessage(chatId, "Привет, " + name + "! Твой аккаунт зарегистрирован. Чем я могу тебе помочь?");
                    } else {
                        sendMessage(chatId, name + ", твой аккаунт уже зарегистрирован.");
                    }
                }
                case "/help" ->
                        sendMessage(chatId, HELP_TEXT);
                default ->
                        sendMessage(chatId, "Извини, команда не поддерживается:(");
            }
        }
    }

    private void sendMessage (long chatId, String answer) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(answer);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error occurred: " + e.getMessage());
            e.printStackTrace();
        }
        log.info("Replied to chat " + chatId + ". Message: " + message.getText());
    }

    private boolean registerUser(Message message) {
        if (userRepository.findById(message.getChatId()).isPresent()) {
            return false;
        } else {
            User user = new User();
            Long chatId = message.getChatId();
            Chat chat = message.getChat();
            user.setChatId(chatId);
            user.setUserName(chat.getUserName());
            user.setFirstName(chat.getFirstName());
            user.setLastName(chat.getLastName());
            user.setRegisteredAt(new Timestamp(System.currentTimeMillis()));

            userRepository.save(user);
            log.info("User registered: " + user);
            return true;
        }
    }
}
