package com.csctracker.notifysyncserver.service;

import com.csctracker.notifysyncserver.dto.OutputMessage;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ReflectionUtils;
import reactor.core.publisher.FluxSink;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

@Configuration
public class MessageRecivedEventPublisher implements ApplicationListener<MessageEvent>, Consumer<FluxSink<OutputMessage>> {

    private final Executor executor;
    private final BlockingQueue<MessageEvent> queue = new LinkedBlockingQueue<>();

    public MessageRecivedEventPublisher(@Qualifier("brokerChannelExecutor") Executor executor) {
        this.executor = executor;
    }

    @Override
    public void onApplicationEvent(MessageEvent messageEvent) {
        this.queue.offer(messageEvent);
    }

    @Override
    public void accept(FluxSink<OutputMessage> linhaFluxSink) {
        this.executor.execute(() -> {
            while (true)
                try {
                    MessageEvent messageEvent = queue.take();
                    linhaFluxSink.next(messageEvent.getMessage());
                } catch (InterruptedException e) {
                    ReflectionUtils.rethrowRuntimeException(e);
                }
        });
    }
}
