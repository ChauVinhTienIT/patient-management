package com.tiencv.patientservice.service;

import com.tiencv.patientservice.dto.request.PatientRequestDTO;
import com.tiencv.patientservice.dto.response.PatientResponseDTO;

import java.util.List;
import java.util.UUID;

public interface PatientService {
    List<PatientResponseDTO> getAllPatients();
    PatientResponseDTO createPatient(PatientRequestDTO patientRequestDTO);
    PatientResponseDTO updatePatient(PatientRequestDTO patientRequestDTO, UUID id);
    PatientResponseDTO deletePatient(UUID id);
}
