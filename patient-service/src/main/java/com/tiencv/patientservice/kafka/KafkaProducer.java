package com.tiencv.patientservice.kafka;

import com.tiencv.patientservice.model.Patient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import patient.events.PatientEvent;

@Slf4j
@Service
public class KafkaProducer {
    // Define the KafkaTemplate for sending messages
    private final KafkaTemplate<String, byte[]> kafkaTemplate;

    public KafkaProducer(KafkaTemplate<String, byte[]> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendEvent(Patient patient) {
        PatientEvent event = PatientEvent.newBuilder()
                .setPatientId(patient.getId().toString())
                .setPatientName(patient.getName())
                .setPatientEmail(patient.getEmail())
                .setEventType("PATIENT_CREATED")
                .build();

        // Send the event to the Kafka topic
        try {
            kafkaTemplate.send("patient", event.getPatientId(), event.toByteArray());
            log.info("Sent PatientCreated event to Kafka: {}", event);
        } catch (Exception e) {
            // Handle the exception
            log.error("Error sending PatientCreated event to Kafka: {}", event);
        }
    }

}
