package com.tiencv.patientservice.service;

import com.tiencv.patientservice.dto.PatientResponseDTO;
import com.tiencv.patientservice.mapper.PatientMapper;
import com.tiencv.patientservice.model.Patient;
import com.tiencv.patientservice.repository.PatientRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PatientService {
    private final PatientRepository patientRepository;
    private final PatientMapper patientMapper;

    // Constructor injection for better testability and immutability
    public PatientService(PatientRepository patientRepository, PatientMapper patientMapper) {
        this.patientRepository = patientRepository;
        this.patientMapper = patientMapper;
    }

    public List<PatientResponseDTO> getAllPatients() {
        List<Patient> patients = patientRepository.findAll();
        return patients.stream()
                .map(patientMapper::toPatientResponseDTO)
                .toList();
    }
}
