package com.csctracker.notifysyncserver.service;

import com.csctracker.dto.Conversor;
import com.csctracker.notifysyncserver.dto.MessageDTO;
import com.csctracker.notifysyncserver.dto.NotificationSyncDTO;
import com.csctracker.notifysyncserver.dto.OutputMessage;
import com.csctracker.notifysyncserver.model.Message;
import com.csctracker.notifysyncserver.repository.NotificationSyncRepository;
import com.csctracker.securitycore.service.UserInfoService;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class NotificationService {

    private final ObjectMapper objectMapper;
    private final Conversor<MessageDTO, OutputMessage> conversor;
    private final Conversor<Message, MessageDTO> conversorMessageDTO;
    private final UserInfoService userInfoService;
    private final NotificationSyncRepository notificationSyncRepository;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final ConfigsService configsService;

    private Date lastSync = new Date();

    public NotificationService(UserInfoService userInfoService, NotificationSyncRepository notificationSyncRepository, SimpMessagingTemplate simpMessagingTemplate, ConfigsService configsService) {
        this.userInfoService = userInfoService;
        this.notificationSyncRepository = notificationSyncRepository;
        this.simpMessagingTemplate = simpMessagingTemplate;
        this.configsService = configsService;
        objectMapper = new ObjectMapper()
                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(SerializationFeature.WRITE_DATES_WITH_ZONE_ID, false)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);
        conversor = new Conversor<>(MessageDTO.class, OutputMessage.class);
        conversorMessageDTO = new Conversor<>(Message.class, MessageDTO.class);
    }

    public void grava(MessageDTO messageDTO, Principal principal) {
        Message entity = conversorMessageDTO.toE(messageDTO);
        entity.setUser(userInfoService.getUser(principal));
        entity.setUuid(UUID.randomUUID().toString());
        entity.setDateSynced(new Date());
        log.info("Gravando mensagem: {}", entity.getUuid());
        sendToCLient(entity);
        notificationSyncRepository.save(entity);
    }

    @Scheduled(fixedRate = 10, timeUnit = TimeUnit.SECONDS)
    public void sendToCLient() {
        Date date = new Date(new Date().getTime() - (1000 * 60 * 5));
        log.info("sendToCLient {} - {}", date, lastSync);
        notificationSyncRepository.findByDateSentIsNullAndDateSyncedBetween(date, lastSync).forEach(this::sendToCLient);
        lastSync = new Date();
    }

    public void sendToCLient(Message message) {
        log.info("sendToCLient {}", message.getUuid());
        simpMessagingTemplate.convertAndSend("/topic/" + message.getUser().getEmail(), new OutputMessage(message.getUuid(), null, null, null, null));
    }

    public List<OutputMessage> get(Principal principal) throws JsonProcessingException {
        List<Message> messages = notificationSyncRepository.findByUserAndDateSentIsNull(userInfoService.getUser(principal));
        for (Message message : messages) {
            message.setDateSent(new Date());
            notificationSyncRepository.save(message);
        }
        List<MessageDTO> messageDTOS = conversorMessageDTO.toD(messages);

        List<OutputMessage> outputMessages = new ArrayList<>();
        for (MessageDTO messageDTO : messageDTOS) {
            outputMessages.add(convertMessage(messageDTO));
        }
        return outputMessages;
    }

    public OutputMessage get(Principal principal, String id) throws JsonProcessingException {
        var message = notificationSyncRepository.findByUserAndUuid(userInfoService.getUser(principal), id);
        if (message == null) {
            message = new Message();
        }
        message.setDateSent(new Date());
        notificationSyncRepository.save(message);
        var messageDTO = conversorMessageDTO.toD(message);
        return convertMessage(messageDTO);
    }

    public OutputMessage convertMessage(MessageDTO messageDTO) throws JsonProcessingException {
        NotificationSyncDTO notificationSyncDTO = objectMapper.readValue(messageDTO.getText(), NotificationSyncDTO.class);
        messageDTO.setFrom(notificationSyncDTO.getTitle());
        messageDTO.setText(notificationSyncDTO.getText());
        messageDTO.setApp(notificationSyncDTO.getAppName());
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss");
        simpleDateFormat.setTimeZone(configsService.getTimeZone());
        messageDTO.setTime(simpleDateFormat.format(new Date(notificationSyncDTO.getSystemTime())));
        return conversor.toD(messageDTO);
    }
}
