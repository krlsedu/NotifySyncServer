package com.csctracker.notifysyncserver.service;

import com.csctracker.notifysyncserver.dto.MessageDTO;
import com.csctracker.notifysyncserver.dto.NotificationSyncDTO;
import com.csctracker.notifysyncserver.dto.OutputMessage;
import com.csctracker.notifysyncserver.model.Message;
import com.csctracker.notifysyncserver.repository.NotificationSyncRepository;
import com.csctracker.securitycore.dto.Conversor;
import com.csctracker.securitycore.model.User;
import com.csctracker.securitycore.service.UserInfoService;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class NotificationService {

    private final ObjectMapper objectMapper;
    private final Conversor<MessageDTO, OutputMessage> conversor;
    private final Conversor<Message, MessageDTO> conversorMessageDTO;
    private final UserInfoService userInfoService;
    private final NotificationSyncRepository notificationSyncRepository;

    private final SimpMessagingTemplate simpMessagingTemplate;

    public NotificationService(UserInfoService userInfoService, NotificationSyncRepository notificationSyncRepository, SimpMessagingTemplate simpMessagingTemplate) {
        this.userInfoService = userInfoService;
        this.notificationSyncRepository = notificationSyncRepository;
        this.simpMessagingTemplate = simpMessagingTemplate;
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
        sendToCLient(entity);
        notificationSyncRepository.save(entity);
    }

    @Scheduled(fixedRate = 5000)
    public void sendToCLient() {
        Date date = new Date(new Date().getTime() - (1000 * 60 * 5));
        notificationSyncRepository.findByDateSentIsNullAndDateSyncedAfter(date).forEach(this::sendToCLient);
    }

    public void sendToCLient(Message message) {
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
        messageDTO.setTime(new SimpleDateFormat("HH:mm:ss").format(new Date(notificationSyncDTO.getSystemTime())));
        return conversor.toD(messageDTO);
    }

    public User getUser(Principal principal) {
        User user = userInfoService.getUser(principal);
        user.setPassword(null);
        return user;
    }
}
