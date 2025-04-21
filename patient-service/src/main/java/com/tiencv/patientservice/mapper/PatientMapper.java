package com.tiencv.patientservice.mapper;

import com.tiencv.patientservice.dto.request.PatientRequestDTO;
import com.tiencv.patientservice.dto.response.PatientResponseDTO;
import com.tiencv.patientservice.model.Patient;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PatientMapper {
    PatientResponseDTO toPatientResponseDTO(Patient patient);
    Patient toPatient(PatientRequestDTO patientRequestDTO);
}
