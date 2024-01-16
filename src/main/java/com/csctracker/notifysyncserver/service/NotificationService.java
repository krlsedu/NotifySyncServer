package com.csctracker.notifysyncserver.service;

import com.csctracker.configs.ApplicationInfo;
import com.csctracker.dto.Conversor;
import com.csctracker.notifysyncserver.dto.MessageDTO;
import com.csctracker.notifysyncserver.dto.MessageRepositoryDTO;
import com.csctracker.notifysyncserver.dto.NotificationSyncDTO;
import com.csctracker.notifysyncserver.dto.OutputMessageDTO;
import com.csctracker.notifysyncserver.model.Message;
import com.csctracker.notifysyncserver.repository.NotificationSyncRepository;
import com.csctracker.securitycore.service.UserInfoService;
import com.csctracker.service.ConfigsService;
import com.csctracker.service.RequestInfo;
import com.csctracker.utils.EnvReader;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import kong.unirest.Unirest;
import kong.unirest.json.JSONException;
import kong.unirest.json.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.security.Principal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.csctracker.configs.CustomHttpTraceFilter.CORRELATION_ID_LOG_VAR_NAME;

@Service
@Slf4j
public class NotificationService {

    private final ObjectMapper objectMapper;
    private final Conversor<MessageDTO, OutputMessageDTO> conversor;
    private final Conversor<Message, MessageDTO> conversorMessageDTO;
    private final UserInfoService userInfoService;
    private final NotificationSyncRepository notificationSyncRepository;
    private final ConfigsService configsService;
    private final ApplicationEventPublisher publisher;
    private final ApplicationInfo applicationInfo;

    private Date lastSync = new Date();

    public NotificationService(UserInfoService userInfoService, NotificationSyncRepository notificationSyncRepository, ConfigsService configsService, ApplicationEventPublisher publisher, ApplicationInfo applicationInfo) {
        this.userInfoService = userInfoService;
        this.notificationSyncRepository = notificationSyncRepository;
        this.configsService = configsService;
        this.publisher = publisher;
        this.applicationInfo = applicationInfo;
        objectMapper = new ObjectMapper()
                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(SerializationFeature.WRITE_DATES_WITH_ZONE_ID, false)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);
        conversor = new Conversor<>(MessageDTO.class, OutputMessageDTO.class);
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
        save_w_repository(entity);
    }

    @Scheduled(fixedRate = 10, timeUnit = TimeUnit.SECONDS)
    public void sendToCLient() {
        Date date = new Date(new Date().getTime() - (1000 * 60 * 5));
        MDC.put(CORRELATION_ID_LOG_VAR_NAME, "Scheduled_notify-sync-" + UUID.randomUUID());
        log.info("sendToCLient {} - {}", date, lastSync);
        notificationSyncRepository.findByDateSentIsNullAndDateSyncedBetween(date, lastSync).forEach(this::sendToCLient);
        lastSync = new Date();
    }

    public void save_w_repository(Message message) {
        message.setRequestId(MDC.get(CORRELATION_ID_LOG_VAR_NAME));
        var post = Unirest.post(EnvReader.readEnv("URL_REPOSITORY") + "messages");
        var headers = RequestInfo.getHeaders();
        if (!headers.containsKey("authorization")) {
            post.header("authorization", "Bearer " + EnvReader.readEnv("API_TOKEN"));
        }
        for (Map.Entry<String, String> header : headers.entrySet()) {
            switch (header.getKey().toLowerCase()) {
                case "content-length":
                    break;
                default:
                    post.header(header.getKey(), header.getValue());
                    break;
            }
        }
        var messageRepositoryDTO = new MessageRepositoryDTO();
        messageRepositoryDTO.setId(message.getId());
        messageRepositoryDTO.setUuid(message.getUuid());
        messageRepositoryDTO.setApp(message.getApp());
        messageRepositoryDTO.setNotify_from(message.getFrom());
        messageRepositoryDTO.setText(message.getText());
        messageRepositoryDTO.setOperation(message.getOperation());
        messageRepositoryDTO.setDate_sent(message.getDateSent());
        messageRepositoryDTO.setDate_synced(message.getDateSynced());
        messageRepositoryDTO.setData(message.getData());
        messageRepositoryDTO.setNotfy_time(message.getTime());
        messageRepositoryDTO.setRequest_id(message.getRequestId());
        messageRepositoryDTO.setUser_id(message.getUser().getId());
        var response = post
                .body(messageRepositoryDTO)
                .asString();

        response.ifFailure(e -> {
            log.error("Erro ao salvar a mensagem" + e.getBody());
            log.error(e.toString());
        });
    }

    public void sendToCLient(Message message) {
        log.info("sendToClient {}", message.getUuid());
        var post = Unirest.post("http://rabbit:8080/notification");
        var headers = RequestInfo.getHeaders();
        for (Map.Entry<String, String> header : headers.entrySet()) {
            switch (header.getKey().toLowerCase()) {
                case "content-length":
                    break;
                default:
                    post.header(header.getKey(), header.getValue());
                    break;
            }
        }
        var messageDTO = conversorMessageDTO.toD(message);
        try {
            messageDTO.setOutputMessageDTO(convertMessage(conversorMessageDTO.toD(message)));
        } catch (JsonProcessingException e) {
            log.error("Erro ao converter mensagem para JSON", e);
        }
        log.info("Enviando mensagem para o cliente: {}", messageDTO);
        var response = post
                .body(messageDTO)
                .asString();
        response.ifFailure(e -> log.error("Erro ao enviar mensagem para o cliente", e));
    }

    public List<OutputMessageDTO> get(Principal principal) throws JsonProcessingException {
        List<Message> messages = notificationSyncRepository.findByUserAndDateSentIsNull(userInfoService.getUser(principal));
        for (Message message : messages) {
            message.setDateSent(new Date());
            save_w_repository(message);
        }
        List<MessageDTO> messageDTOS = conversorMessageDTO.toD(messages);

        List<OutputMessageDTO> outputMessageDTOS = new ArrayList<>();
        for (MessageDTO messageDTO : messageDTOS) {
            outputMessageDTOS.add(convertMessage(messageDTO));
        }
        return outputMessageDTOS;
    }

    public Flux<OutputMessageDTO> buscaTodos() throws JsonProcessingException {
        return Flux.fromIterable(getMessages());
    }

    public List<OutputMessageDTO> getMessages() throws JsonProcessingException {
        List<Message> messages = notificationSyncRepository.findByUserAndAppIsNotNullOrderByIdDesc(userInfoService.getUser(), PageRequest.ofSize(100));
        messages.sort(Comparator.comparing(Message::getId));
        List<MessageDTO> messageDTOS = conversorMessageDTO.toD(messages);

        List<OutputMessageDTO> outputMessageDTOS = new ArrayList<>();
        for (MessageDTO messageDTO : messageDTOS) {
            OutputMessageDTO e = convertMessage(messageDTO);
            e.setData(null);
            outputMessageDTOS.add(e);
        }
        return outputMessageDTOS;
    }

    public List<OutputMessageDTO> getMessagesDate(String date) throws JsonProcessingException {
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

        List<OutputMessageDTO> outputMessageDTOS = new ArrayList<>();
        for (MessageDTO messageDTO : messageDTOS) {
            OutputMessageDTO e = convertMessage(messageDTO);
            e.setData(null);
            outputMessageDTOS.add(e);
        }
        return outputMessageDTOS;
    }

    public OutputMessageDTO get(Principal principal, String id) throws JsonProcessingException {
        var message = notificationSyncRepository.findByUserAndUuid(userInfoService.getUser(principal), id);
        if (message == null) {
            message = new Message();
        }
        message.setDateSent(new Date());
        save_w_repository(message);
        var messageDTO = conversorMessageDTO.toD(message);
        return convertMessage(messageDTO);
    }

    public OutputMessageDTO convertMessage(MessageDTO messageDTO) throws JsonProcessingException {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss");
        simpleDateFormat.setTimeZone(configsService.getTimeZone());
        if (messageDTO.getApp() == null) {
            messageDTO.setApp("unknown");
        }
        if (messageDTO.getForce() == null) {
            messageDTO.setForce(false);
        }
        switch (messageDTO.getApp()) {
            case "andorid-notification-log":
                NotificationSyncDTO notificationSyncDTO = objectMapper.readValue(messageDTO.getText(), NotificationSyncDTO.class);
                messageDTO.setFrom(notificationSyncDTO.getTitle());
                messageDTO.setText(notificationSyncDTO.getText());
                messageDTO.setApp(notificationSyncDTO.getAppName());
                messageDTO.setTime(simpleDateFormat.format(new Date(notificationSyncDTO.getSystemTime())));
                return conversor.toD(messageDTO);
            case "Jenkins":
                if (messageDTO.getOperation() == null) {
                    messageDTO.setOperation("unknown");
                }
                messageDTO.setForce(true);
                if (messageDTO.getFrom() == null) {
                    messageDTO.setFrom(messageDTO.getApp());
                }
                messageDTO.setTime(simpleDateFormat.format(new Date()));
                return conversor.toD(messageDTO);
            case "CscTrackerInvest":
                if (messageDTO.getOperation() == null) {
                    messageDTO.setOperation("unknown");
                }
                switch (messageDTO.getOperation()) {
                    case "buySellRecommendation":
                        messageDTO.setForce(true);
                        break;
                    default:
                        messageDTO.setForce(false);
                }
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
                        try {
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
                        } catch (Exception e) {
                            sb.append(binding.toString());
                        }
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
                if (messageDTO.getOperation() == null) {
                    messageDTO.setOperation("");
                }
                switch (messageDTO.getOperation()) {
                    case "urgent":
                        messageDTO.setForce(true);
                        break;
                    default:
                        messageDTO.setForce(false);
                }
                return conversor.toD(messageDTO);
        }
    }
}
