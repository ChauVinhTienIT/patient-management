package com.tiencv.patientservice.service;

import com.tiencv.patientservice.dto.request.PatientRequestDTO;
import com.tiencv.patientservice.dto.response.PatientResponseDTO;
import com.tiencv.patientservice.exception.EmailAlreadyExitsException;
import com.tiencv.patientservice.exception.PatientNotFoundException;
import com.tiencv.patientservice.grpc.BillingServiceGrpcClient;
import com.tiencv.patientservice.kafka.KafkaProducer;
import com.tiencv.patientservice.mapper.PatientMapper;
import com.tiencv.patientservice.model.Patient;
import com.tiencv.patientservice.repository.PatientRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class PatientServiceImpl implements PatientService {
    private final PatientRepository patientRepository;
    private final PatientMapper patientMapper;
    private final BillingServiceGrpcClient billingServiceGrpcClient;
    private final KafkaProducer kafkaProducer;

    // Constructor injection for better testability and immutability
    public PatientServiceImpl(PatientRepository patientRepository, PatientMapper patientMapper, BillingServiceGrpcClient billingServiceGrpcClient, KafkaProducer kafkaProducer, KafkaProducer kafkaProducer1) {
        this.patientRepository = patientRepository;
        this.patientMapper = patientMapper;
        this.billingServiceGrpcClient = billingServiceGrpcClient;
        this.kafkaProducer = kafkaProducer1;
    }

    @Override
    public List<PatientResponseDTO> getAllPatients() {
        List<Patient> patients = patientRepository.findAll();
        return patients.stream()
                .map(patientMapper::toPatientResponseDTO)
                .toList();
    }

    @Override
    public PatientResponseDTO createPatient(PatientRequestDTO patientRequestDTO) {
        Patient patient = patientMapper.toPatient(patientRequestDTO);
        // Check if the patient already exists by email
        if (patientRepository.existsByEmail(patient.getEmail())) {
            throw new EmailAlreadyExitsException("Patient with this email" + patient.getEmail() + " already exists");
        }

        Patient savedPatient = patientRepository.save(patient);

        // Create a billing account for the patient using gRPC
        billingServiceGrpcClient.createBillingAccount(
                savedPatient.getId().toString(),
                savedPatient.getName(),
                savedPatient.getEmail()
        );

        kafkaProducer.sendEvent(savedPatient);
        return patientMapper.toPatientResponseDTO(savedPatient);
    }

    @Override
    public PatientResponseDTO updatePatient(PatientRequestDTO patientRequestDTO, UUID id) {
        // Check if the patient exists
        Patient existingPatient = patientRepository.findById(id)
                .orElseThrow(() -> new PatientNotFoundException("Patient not found with Id: " + id));

        // Check if the email is already in use by another patient
        if (patientRepository.existsByEmailAndIdNot(patientRequestDTO.getEmail(), existingPatient.getId())) {
            throw new EmailAlreadyExitsException("Patient with this email" + patientRequestDTO.getEmail() + " already exists");
        }

        // Update the existing patient with new data
        existingPatient.setName(patientRequestDTO.getName());
        existingPatient.setEmail(patientRequestDTO.getEmail());
        existingPatient.setAddress(patientRequestDTO.getAddress());
        existingPatient.setDateOfBirth(LocalDate.parse(patientRequestDTO.getDateOfBirth()));

        Patient updatedPatient = patientRepository.save(existingPatient);
        return patientMapper.toPatientResponseDTO(updatedPatient);
    }

    @Override
    public PatientResponseDTO deletePatient(UUID id) {
        // Check if the patient exists
        Patient existingPatient = patientRepository.findById(id)
                .orElseThrow(() -> new PatientNotFoundException("Patient not found with Id: " + id));

        // Delete the patient
        patientRepository.delete(existingPatient);

        // Return the deleted patient details
        return patientMapper.toPatientResponseDTO(existingPatient);
    }
}
