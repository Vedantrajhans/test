package com.vedant.concert_platform.controller;

import com.vedant.concert_platform.dto.HierarchyDto;
import com.vedant.concert_platform.service.PromoterManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/promoter")
@RequiredArgsConstructor
public class PromoterController {
    private final PromoterManagementService promoterManagementService;

    @PostMapping("/organizers")
    @PreAuthorize("hasRole('PROMOTER')")
    public ResponseEntity<HierarchyDto.OrganizerResponse> createOrganizer(@Valid @RequestBody HierarchyDto.CreateOrganizerRequest request) {
        return ResponseEntity.ok(promoterManagementService.createOrganizer(request));
    }

    @GetMapping("/organizers")
    @PreAuthorize("hasRole('PROMOTER')")
    public ResponseEntity<List<HierarchyDto.OrganizerResponse>> listOrganizers() {
        return ResponseEntity.ok(promoterManagementService.listOrganizers());
    }

    @PutMapping("/organizers/{organizerId}")
    @PreAuthorize("hasRole('PROMOTER')")
    public ResponseEntity<HierarchyDto.OrganizerResponse> updateOrganizer(
            @PathVariable Long organizerId,
            @Valid @RequestBody HierarchyDto.UpdateOrganizerRequest request) {
        return ResponseEntity.ok(promoterManagementService.updateOrganizer(organizerId, request));
    }

    @DeleteMapping("/organizers/{organizerId}")
    @PreAuthorize("hasRole('PROMOTER')")
    public ResponseEntity<Void> deleteOrganizer(@PathVariable Long organizerId) {
        promoterManagementService.deleteOrganizer(organizerId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/organizers/{organizerId}/hard")
    @PreAuthorize("hasRole('PROMOTER')")
    public ResponseEntity<java.util.Map<String, String>> hardDeleteOrganizer(@PathVariable Long organizerId) {
        promoterManagementService.hardDeleteOrganizer(organizerId);
        return ResponseEntity.ok(java.util.Map.of("message", "Organizer permanently deleted"));
    }

    @PostMapping(value = "/organizers/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('PROMOTER')")
    public ResponseEntity<HierarchyDto.CsvImportResult> importOrganizers(@RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(promoterManagementService.importOrganizers(file));
    }

    @GetMapping("/organizers/export")
    @PreAuthorize("hasRole('PROMOTER')")
    public ResponseEntity<byte[]> exportOrganizers(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String organizerType,
            @RequestParam(required = false) String search) {
        byte[] csv = promoterManagementService.exportOrganizers(city, state, organizerType, search);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=organizers.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }
}
