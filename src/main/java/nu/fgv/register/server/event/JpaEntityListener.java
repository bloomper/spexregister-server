package nu.fgv.register.server.event;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;
import nu.fgv.register.server.news.News;
import nu.fgv.register.server.spex.Spex;
import nu.fgv.register.server.spex.SpexDetails;
import nu.fgv.register.server.spex.category.SpexCategory;
import nu.fgv.register.server.spexare.Spexare;
import nu.fgv.register.server.spexare.activity.Activity;
import nu.fgv.register.server.spexare.activity.spex.SpexActivity;
import nu.fgv.register.server.spexare.activity.task.TaskActivity;
import nu.fgv.register.server.spexare.activity.task.actor.Actor;
import nu.fgv.register.server.spexare.address.Address;
import nu.fgv.register.server.spexare.consent.Consent;
import nu.fgv.register.server.spexare.membership.Membership;
import nu.fgv.register.server.spexare.toggle.Toggle;
import nu.fgv.register.server.tag.Tag;
import nu.fgv.register.server.task.Task;
import nu.fgv.register.server.task.category.TaskCategory;
import nu.fgv.register.server.user.User;
import nu.fgv.register.server.user.authority.Authority;
import nu.fgv.register.server.user.state.State;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class JpaEntityListener {

    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    public JpaEntityListener(final ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    public JpaEntityListener() {
    }

    @PrePersist
    private void atCreate(final Object sourceObject) {
        if (isSpexareRelatedChange(sourceObject) || isSpexRelatedChange(sourceObject)) {
            constructEvent(sourceObject, Event.EventType.UPDATE).ifPresent(applicationEventPublisher::publishEvent);
        } else {
            constructEvent(sourceObject, Event.EventType.CREATE).ifPresent(applicationEventPublisher::publishEvent);
        }
    }

    @PreUpdate
    private void atUpdate(final Object sourceObject) {
        constructEvent(sourceObject, Event.EventType.UPDATE).ifPresent(applicationEventPublisher::publishEvent);
    }

    @PreRemove
    private void atRemove(final Object sourceObject) {
        if (isSpexareRelatedChange(sourceObject) || isSpexRelatedChange(sourceObject)) {
            constructEvent(sourceObject, Event.EventType.UPDATE).ifPresent(applicationEventPublisher::publishEvent);
        } else {
            constructEvent(sourceObject, Event.EventType.REMOVE).ifPresent(applicationEventPublisher::publishEvent);
        }
    }

    private Optional<SpringEvent> constructEvent(final Object sourceObject, final Event.EventType event) {
        final Event.SourceType source;

        if (sourceObject instanceof News) {
            source = Event.SourceType.NEWS;
        } else if (sourceObject instanceof SpexDetails || isSpexRelatedChange(sourceObject)) {
            source = Event.SourceType.SPEX;
        } else if (sourceObject instanceof SpexCategory) {
            source = Event.SourceType.SPEX_CATEGORY;
        } else if (sourceObject instanceof Spexare || isSpexareRelatedChange(sourceObject)) {
            source = Event.SourceType.SPEXARE;
        } else if (sourceObject instanceof Tag) {
            source = Event.SourceType.TAG;
        } else if (sourceObject instanceof Task) {
            source = Event.SourceType.TASK;
        } else if (sourceObject instanceof TaskCategory) {
            source = Event.SourceType.TASK_CATEGORY;
        } else if (sourceObject instanceof User) {
            source = Event.SourceType.USER;
        } else if (sourceObject instanceof Authority) {
            source = Event.SourceType.AUTHORITY;
        } else if (sourceObject instanceof State) {
            source = Event.SourceType.STATE;
        } else {
            source = null;
        }
        return Optional.ofNullable(source).map(s -> new SpringEvent(sourceObject, event, s));
    }

    private boolean isSpexareRelatedChange(final Object sourceObject) {
        return sourceObject instanceof Activity ||
                sourceObject instanceof SpexActivity ||
                sourceObject instanceof TaskActivity ||
                sourceObject instanceof Actor ||
                sourceObject instanceof Address ||
                sourceObject instanceof Consent ||
                sourceObject instanceof Membership ||
                sourceObject instanceof Toggle;
    }

    private boolean isSpexRelatedChange(final Object sourceObject) {
        return sourceObject instanceof Spex;
    }
}
