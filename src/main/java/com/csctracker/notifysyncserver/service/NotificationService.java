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
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class NotificationService {

    private final ObjectMapper objectMapper;
    private final Conversor<MessageDTO, OutputMessage> conversor;
    private final Conversor<Message, MessageDTO> conversorMessageDTO;
    private final UserInfoService userInfoService;
    private final NotificationSyncRepository notificationSyncRepository;

    public NotificationService(UserInfoService userInfoService, NotificationSyncRepository notificationSyncRepository) {
        this.userInfoService = userInfoService;
        this.notificationSyncRepository = notificationSyncRepository;
        objectMapper = new ObjectMapper()
                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(SerializationFeature.WRITE_DATES_WITH_ZONE_ID, false)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);
        conversor = new Conversor<>(MessageDTO.class, OutputMessage.class);
        conversorMessageDTO = new Conversor<>(Message.class, MessageDTO.class);
    }

    public Message grava(MessageDTO messageDTO, Principal principal) {
        Message entity = conversorMessageDTO.toE(messageDTO);
        entity.setDateSynced(new Date());
        entity.setUser(userInfoService.getUser(principal));
        return notificationSyncRepository.save(entity);
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

    public OutputMessage get(Principal principal, Long id) throws JsonProcessingException {
        var message = notificationSyncRepository.findByUserAndId(userInfoService.getUser(principal), id);
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
