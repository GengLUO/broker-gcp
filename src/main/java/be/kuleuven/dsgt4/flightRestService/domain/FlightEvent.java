package be.kuleuven.dsgt4.flightRestService.domain;

import java.util.Map;

import org.springframework.context.ApplicationEvent;

public class FlightEvent extends ApplicationEvent {
    private Map<String, Object> messageData;

    public FlightEvent(Object source, Map<String, Object> messageData) {
        super(source);
        this.messageData = messageData;
    }

    public Map<String, Object> getMessageData() {
        return messageData;
    }
}