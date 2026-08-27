package com.novamart.notification.service;

import com.novamart.notification.domain.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Delivery mechanism for notifications.
 *
 * <p><b>This is a mock.</b> It writes the message to the service log and reports
 * success. Nothing is emailed or texted. Local development has no SMTP or SMS
 * credentials, and shipping invented ones would only produce silent failures.
 *
 * <p>Replacing it with a real transport means implementing {@link #deliver}
 * against a mail or SMS client. Nothing else in the service needs to change,
 * which is the point of keeping the transport behind its own class.
 */
@Component
public class MockNotificationTransport {

    private static final Logger log = LoggerFactory.getLogger(MockNotificationTransport.class);

    /**
     * @return true when the transport accepted the message
     */
    public boolean deliver(Notification notification) {
        // The recipient address is logged because in this mock the log IS the
        // outbox: without it there would be no way to demonstrate that the right
        // message reached the right person. A real transport would log an id and
        // leave the address to the provider.
        log.info("""
                        [MOCK DELIVERY - no message actually sent]
                          channel   : {}
                          to        : {}
                          type      : {}
                          subject   : {}
                          reference : {}
                          body      : {}""",
                notification.getChannel(),
                notification.getRecipient(),
                notification.getType(),
                notification.getSubject(),
                notification.getReferenceId(),
                notification.getBody());
        return true;
    }
}
