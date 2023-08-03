package nu.fgv.register.server.event;

import lombok.Getter;
import lombok.ToString;
import org.springframework.context.ApplicationEvent;

@Getter
@ToString
public class SpringEvent extends ApplicationEvent {

    private final Event.EventType event;
    private final Event.SourceType sourceType;

    public SpringEvent(final Object sourceObject, final Event.EventType event, final Event.SourceType source) {
        super(sourceObject);
        this.event = event;
        this.sourceType = source;
    }

}