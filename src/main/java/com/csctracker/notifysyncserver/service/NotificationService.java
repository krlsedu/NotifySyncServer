package com.csctracker.notifysyncserver.service;

import com.csctracker.dto.Conversor;
import com.csctracker.notifysyncserver.dto.MessageDTO;
import com.csctracker.notifysyncserver.dto.NotificationSyncDTO;
import com.csctracker.notifysyncserver.dto.OutputMessage;
import com.csctracker.notifysyncserver.model.Message;
import com.csctracker.notifysyncserver.repository.NotificationSyncRepository;
import com.csctracker.securitycore.service.UserInfoService;
import com.csctracker.service.ConfigsService;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import kong.unirest.json.JSONException;
import kong.unirest.json.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.security.Principal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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
    private final ApplicationEventPublisher publisher;

    private Date lastSync = new Date();

    public NotificationService(UserInfoService userInfoService, NotificationSyncRepository notificationSyncRepository, ConfigsService configsService, SimpMessagingTemplate simpMessagingTemplate, ApplicationEventPublisher publisher) {
        this.userInfoService = userInfoService;
        this.notificationSyncRepository = notificationSyncRepository;
        this.simpMessagingTemplate = simpMessagingTemplate;
        this.configsService = configsService;
        this.publisher = publisher;
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
        if (entity.getUuid() == null) {
            entity.setUuid(UUID.randomUUID().toString());
        }
        if (entity.getFrom() == null) {
            entity.setFrom(entity.getApp());
        }
        if (entity.getDateSent() == null) {
            entity.setDateSent(new Date());
        }
        entity.setDateSynced(new Date());
        log.info("Gravando mensagem: {}", entity.getUuid());
        sendToCLient(entity);
        try {
            publisher.publishEvent(new MessageEvent(convertMessage(conversorMessageDTO.toD(entity))));
        } catch (JsonProcessingException e) {
            log.error("Erro ao converter mensagem para JSON", e);
        }
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
        simpMessagingTemplate.convertAndSend("/topic/" + message.getUser().getEmail(),
                new OutputMessage(message.getUuid(), null, null, null, message.getApp(), null, null, null, null));
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

    public Flux<OutputMessage> buscaTodos() throws JsonProcessingException {
        return Flux.fromIterable(getMessages());
    }

    public List<OutputMessage> getMessages() throws JsonProcessingException {
        List<Message> messages = notificationSyncRepository.findByUserAndAppIsNotNullOrderByIdDesc(userInfoService.getUser(), PageRequest.ofSize(100));
        messages.sort(Comparator.comparing(Message::getId));
        List<MessageDTO> messageDTOS = conversorMessageDTO.toD(messages);

        List<OutputMessage> outputMessages = new ArrayList<>();
        for (MessageDTO messageDTO : messageDTOS) {
            OutputMessage e = convertMessage(messageDTO);
            e.setData(null);
            outputMessages.add(e);
        }
        return outputMessages;
    }

    public List<OutputMessage> getMessagesDate(String date) throws JsonProcessingException {
        var sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS 00:00");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date dateTo = null;
        try {
            dateTo = sdf.parse(date);
        } catch (ParseException e) {
            return new ArrayList<>();
        }
        List<Message> messages = notificationSyncRepository.findByUserAndAppIsNotNullAndDateSyncedGreaterThanOrderByIdAsc(userInfoService.getUser(), dateTo);
        List<MessageDTO> messageDTOS = conversorMessageDTO.toD(messages);

        List<OutputMessage> outputMessages = new ArrayList<>();
        for (MessageDTO messageDTO : messageDTOS) {
            OutputMessage e = convertMessage(messageDTO);
            e.setData(null);
            outputMessages.add(e);
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
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss");
        simpleDateFormat.setTimeZone(configsService.getTimeZone());
        if (messageDTO.getApp() == null) {
            messageDTO.setApp("unknown");
        }
        switch (messageDTO.getApp()) {
            case "andorid-notification-log":
                NotificationSyncDTO notificationSyncDTO = objectMapper.readValue(messageDTO.getText(), NotificationSyncDTO.class);
                messageDTO.setFrom(notificationSyncDTO.getTitle());
                messageDTO.setText(notificationSyncDTO.getText());
                messageDTO.setApp(notificationSyncDTO.getAppName());
                messageDTO.setTime(simpleDateFormat.format(new Date(notificationSyncDTO.getSystemTime())));
                return conversor.toD(messageDTO);
            case "CscTrackerInvest":
                if (messageDTO.getFrom() == null) {
                    messageDTO.setFrom(messageDTO.getApp());
                }
                messageDTO.setTime(simpleDateFormat.format(new Date()));
                return conversor.toD(messageDTO);
            case "CscTrackerDesktop":
                messageDTO.setMachine(messageDTO.getFrom());
                messageDTO.setApp(messageDTO.getFrom());
                messageDTO.setTime(simpleDateFormat.format(new Date()));
                var notification = new JSONObject(messageDTO.getText());

                JSONObject binding = notification.getJSONObject("visual").getJSONObject("binding");
                var template = binding.get("template").toString();
                switch (template) {
                    case "ToastGeneric":
                        var sb = new StringBuilder();
                        var count = new AtomicInteger();
                        binding.getJSONArray("text").forEach(o -> {
                            if (count.get() == 0) {
                                messageDTO.setFrom(o.toString().trim());
                            } else {
                                try {
                                    var objO = new JSONObject(o.toString());
                                    var st = objO.get("placement").toString();
                                    if (st.equals("attribution")) {
                                        messageDTO.setApp(objO.get("").toString().trim());
                                    } else {
                                        sb.append(objO.get("").toString().trim());
                                    }
                                } catch (Exception e) {
                                    sb.append(o.toString().trim()).append("\n");
                                }
                            }
                            count.getAndIncrement();
                        });
                        messageDTO.setText(sb.toString());
                        break;
                    case "ToastImageAndText04":
                        var sbs = new StringBuilder();
                        var count2 = new AtomicInteger();
                        binding.getJSONArray("text").forEach(o -> {
                            var obj = new JSONObject(o.toString());
                            var text = obj.get("").toString();
                            if (count2.get() == 0) {
                                messageDTO.setFrom(text);
                            } else {
                                sbs.append(text).append("\n");
                            }
                            count2.getAndIncrement();
                        });
                        messageDTO.setText(sbs.toString());
                        break;
                    default:
                        try {
                            messageDTO.setText(binding.getJSONArray("text").toString());
                        } catch (JSONException e) {
                            messageDTO.setText(binding.toString());
                        }
                        break;
                }
                return conversor.toD(messageDTO);
            default:
                messageDTO.setTime(simpleDateFormat.format(new Date()));
                if (messageDTO.getFrom() == null) {
                    messageDTO.setFrom("Unknown");
                }
                return conversor.toD(messageDTO);
        }
    }
}
