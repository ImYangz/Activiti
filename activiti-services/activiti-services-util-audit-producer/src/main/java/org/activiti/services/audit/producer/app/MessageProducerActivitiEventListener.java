package org.activiti.services.audit.producer.app;

import org.activiti.engine.delegate.event.ActivitiEvent;
import org.activiti.engine.delegate.event.ActivitiEventListener;
import org.activiti.services.audit.producer.app.events.converter.EventConverterContext;
import org.activiti.services.model.events.ProcessEngineEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
public class MessageProducerActivitiEventListener implements ActivitiEventListener {

    private final AuditProducerChannels producer;

    private final EventConverterContext converterContext;

    @Autowired
    public MessageProducerActivitiEventListener(AuditProducerChannels producer,
                                                EventConverterContext converterContext) {
        this.producer = producer;
        this.converterContext = converterContext;
    }

    @Override
    public void onEvent(ActivitiEvent event) {
        ProcessEngineEvent newEvent = converterContext.from(event);
        if (newEvent != null) {
            producer.auditProducer().send(MessageBuilder.withPayload(newEvent).build());
        }
    }

    @Override
    public boolean isFailOnException() {
        return false;
    }
}
