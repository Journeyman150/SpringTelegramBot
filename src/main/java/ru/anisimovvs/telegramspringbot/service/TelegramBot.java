package ru.anisimovvs.telegramspringbot.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.anisimovvs.telegramspringbot.config.BotConfig;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {

    final BotConfig config;
    static final String HELP_TEXT = "Этот проект создан в качестве заготвоки для последующей реализации идей для бота на Java Spring\n\n" +
            "Список реализованных команд: \n\n" +
            "Введите /start чтобы увидеть привествие\n\n" +
            "Введите /help чтобы вывести описание бота";

    public TelegramBot (BotConfig config) {
        this.config = config;
        List<BotCommand> listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand("/start", "приветствие"));
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

            switch (messageText) {
                case "/start" -> startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                case "/help" -> sendMessage(chatId, HELP_TEXT);
                default -> sendMessage(chatId, "Извини, команда не поддерживается:(");
            }
        }
    }

    private void startCommandReceived(long chatId, String name) {
        String answer = "Привет, " + name + "! Чем я могу тебе помочь?";
        sendMessage(chatId, answer);
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
}
