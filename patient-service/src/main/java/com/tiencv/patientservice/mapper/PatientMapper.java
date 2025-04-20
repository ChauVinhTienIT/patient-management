package com.tiencv.patientservice.mapper;

import com.tiencv.patientservice.dto.PatientResponseDTO;
import com.tiencv.patientservice.model.Patient;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PatientMapper {
    PatientResponseDTO toPatientResponseDTO(Patient patient);

    Patient toPatient(PatientResponseDTO patientResponseDTO);
}
