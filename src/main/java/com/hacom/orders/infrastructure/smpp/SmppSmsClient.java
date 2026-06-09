package com.hacom.orders.infrastructure.smpp;

import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.SmppSession;
import com.cloudhopper.smpp.SmppSessionConfiguration;
import com.cloudhopper.smpp.impl.DefaultSmppClient;
import com.cloudhopper.smpp.pdu.SubmitSm;
import com.cloudhopper.smpp.pdu.SubmitSmResp;
import com.cloudhopper.smpp.type.Address;
import com.cloudhopper.smpp.type.RecoverablePduException;
import com.cloudhopper.smpp.type.SmppChannelException;
import com.cloudhopper.smpp.type.SmppTimeoutException;
import com.cloudhopper.smpp.type.UnrecoverablePduException;
import com.hacom.orders.domain.port.SmsSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;

@Service
public class SmppSmsClient implements SmsSender, DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(SmppSmsClient.class);

    private final DefaultSmppClient client;
    private SmppSession session;

    @Value("${smpp.host}")
    private String host;

    @Value("${smpp.port}")
    private int port;

    @Value("${smpp.system-id}")
    private String systemId;

    @Value("${smpp.password}")
    private String password;

    @Value("${smpp.source-ton}")
    private byte sourceTon;

    @Value("${smpp.source-npi}")
    private byte sourceNpi;

    @Value("${smpp.source-address}")
    private String sourceAddress;

    @Value("${smpp.dest-ton}")
    private byte destTon;

    @Value("${smpp.dest-npi}")
    private byte destNpi;

    public SmppSmsClient() {
        this.client = new DefaultSmppClient();
    }

    @PostConstruct
    public void connect() {
        try {
            log.info("Connecting to SMPP server at {}:{}", host, port);

            SmppSessionConfiguration config = new SmppSessionConfiguration();
            config.setHost(host);
            config.setPort(port);
            config.setSystemId(systemId);
            config.setPassword(password);
            config.setType(com.cloudhopper.smpp.SmppBindType.TRANSMITTER);
            config.setBindTimeout(5000L);
            config.setConnectTimeout(5000L);

            session = client.bind(config);

            log.info("Successfully connected to SMPP server");
        } catch (Exception e) {
            log.error("Failed to connect to SMPP server: {}", e.getMessage());
            // Don't throw - allow app to start without SMPP for development
        }
    }

    @Override
    public void sendSms(String destinationNumber, String message) {
        log.debug("Sending SMS to {}: {}", destinationNumber, message);

        if (session == null || !session.isBound()) {
            log.warn("SMPP session is not connected. Cannot send SMS to {}", destinationNumber);
            return;
        }

        try {
            SubmitSm submitSm = new SubmitSm();
            submitSm.setSourceAddress(new Address(sourceTon, sourceNpi, sourceAddress));
            submitSm.setDestAddress(new Address(destTon, destNpi, destinationNumber));
            submitSm.setShortMessage(message.getBytes(StandardCharsets.UTF_8));
            submitSm.setDataCoding((byte) 0); // Default alphabet

            SubmitSmResp response = session.submit(submitSm, 10000);

            if (response.getCommandStatus() == SmppConstants.STATUS_OK) {
                log.info("SMS sent successfully to {}, message ID: {}", destinationNumber, response.getMessageId());
            } else {
                log.warn("SMPP submission returned non-zero status: {} for destination {}",
                        response.getCommandStatus(), destinationNumber);
            }
        } catch (RecoverablePduException | UnrecoverablePduException
                 | SmppTimeoutException | SmppChannelException e) {
            log.error("Error sending SMS via SMPP to {}: {}", destinationNumber, e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("SMS sending interrupted for {}: {}", destinationNumber, e.getMessage());
        }
    }

    @Override
    public void destroy() {
        if (session != null && session.isBound()) {
            log.info("Closing SMPP session");
            try {
                session.unbind(5000L);
                session.close();
                session.destroy();
            } catch (Exception e) {
                log.warn("Error closing SMPP session: {}", e.getMessage());
            }
        }
        if (client != null) {
            client.destroy();
            log.info("SMPP client destroyed");
        }
    }
}