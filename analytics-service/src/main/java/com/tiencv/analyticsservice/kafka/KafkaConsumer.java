package com.tiencv.analyticsservice.kafka;

import com.google.protobuf.InvalidProtocolBufferException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import patient.events.PatientEvent;

@Service
public class KafkaConsumer {
    private static final Logger log = LoggerFactory.getLogger(KafkaConsumer.class);

    // Define the KafkaListener for consuming messages
    // Consume messages from the "patient" topic
    // The groupId is used to identify the consumer group
    @KafkaListener(topics = "patient", groupId = "analytics-service")
    public void consumeEvent(byte[] event) {
        try {
            PatientEvent patientEvent = PatientEvent.parseFrom(event);
            // Process the event
            log.info("Received PatientEvent from Kafka: [Patient Id: {}, Patient Name: {}, Patient Email: {}.]",
                    patientEvent.getPatientId(),
                    patientEvent.getPatientName(),
                    patientEvent.getPatientEmail()
            );

        } catch (InvalidProtocolBufferException e) {
            log.error("Error parsing PatientEvent from Kafka message: {}", e.getMessage());
        }
    }
}
