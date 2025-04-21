package com.tiencv.patientservice.controller;

import com.tiencv.patientservice.dto.request.PatientRequestDTO;
import com.tiencv.patientservice.dto.response.PatientResponseDTO;
import com.tiencv.patientservice.dto.validator.CreatePatientValidationGroup;
import com.tiencv.patientservice.service.PatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.groups.Default;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/patients")
@Tag(name = "Patient", description = "API for managing patients")
public class PatientController {
    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @GetMapping
    @Operation(summary = "Get all patients", description = "Retrieve a list of all patients")
    public ResponseEntity<List<PatientResponseDTO>> getAllPatients() {
        return ResponseEntity.ok().body(patientService.getAllPatients());
    }

    @PostMapping
    @Operation(summary = "Create a new patient", description = "Create a new patient with the provided details")
    public ResponseEntity<PatientResponseDTO> createPatient(
            @Validated({Default.class, CreatePatientValidationGroup.class})
            @RequestBody
            PatientRequestDTO patientRequestDTO
    ) {
        PatientResponseDTO createdPatient = patientService.createPatient(patientRequestDTO);
        return ResponseEntity.ok().body(createdPatient);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing patient", description = "Update the details of an existing patient")
    public ResponseEntity<PatientResponseDTO> updatePatient(
            @PathVariable("id")
            UUID id,

            @Validated({Default.class})
            @RequestBody
            PatientRequestDTO patientRequestDTO
    ) {
        PatientResponseDTO updatedPatient = patientService.updatePatient(patientRequestDTO, id);
        return ResponseEntity.ok().body(updatedPatient);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a patient", description = "Delete a patient by ID")
    public ResponseEntity<PatientResponseDTO> deletePatient(
            @PathVariable("id")
            UUID id
    ) {
        PatientResponseDTO deletedPatient = patientService.deletePatient(id);
        return ResponseEntity.noContent().build();
    }
}
