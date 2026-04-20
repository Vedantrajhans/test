package com.vedant.concert_platform.service;

import com.opencsv.CSVReaderHeaderAware;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvValidationException;
import com.vedant.concert_platform.dto.HierarchyDto;
import com.vedant.concert_platform.entity.Organizer;
import com.vedant.concert_platform.entity.Promoter;
import com.vedant.concert_platform.entity.User;
import com.vedant.concert_platform.entity.enums.Role;
import com.vedant.concert_platform.exception.BadRequestException;
import com.vedant.concert_platform.exception.ConflictException;
import com.vedant.concert_platform.exception.ResourceNotFoundException;
import com.vedant.concert_platform.repository.OrganizerRepository;
import com.vedant.concert_platform.repository.PromoterRepository;
import com.vedant.concert_platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PromoterManagementService {
    private static final String DEFAULT_ORGANIZER_TYPE = "Concert Organizer";
    private static final String DEFAULT_ORGANIZER_PASSWORD = "ChangeMe@12345";

    private final UserRepository userRepository;
    private final PromoterRepository promoterRepository;
    private final OrganizerRepository organizerRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public HierarchyDto.OrganizerResponse createOrganizer(HierarchyDto.CreateOrganizerRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email already exists");
        }
        Promoter promoter = getCurrentPromoter();

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(DEFAULT_ORGANIZER_PASSWORD));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setRole(Role.ORGANIZER);
        user.setFirstLogin(true);
        user = userRepository.save(user);

        Organizer organizer = new Organizer();
        organizer.setUser(user);
        organizer.setPromoter(promoter);
        organizer.setOrganizerType(request.getOrganizerType());
        organizer.setCompanyName(request.getCompanyName());
        organizer.setCity(request.getCity());
        organizer.setState(request.getState());
        organizer = organizerRepository.save(organizer);

        return mapOrganizer(organizer);
    }

    @Transactional(readOnly = true)
    public List<HierarchyDto.OrganizerResponse> listOrganizers() {
        Promoter promoter = getCurrentPromoter();
        return organizerRepository.findByPromoterId(promoter.getId()).stream()
                .map(this::mapOrganizer)
                .toList();
    }

    @Transactional
    public HierarchyDto.OrganizerResponse updateOrganizer(Long organizerId, HierarchyDto.UpdateOrganizerRequest request) {
        Promoter promoter = getCurrentPromoter();
        Organizer organizer = organizerRepository.findById(organizerId)
                .orElseThrow(() -> new ResourceNotFoundException("Organizer not found"));

        if (!organizer.getPromoter().getId().equals(promoter.getId())) {
            throw new ResourceNotFoundException("Organizer not found in your account");
        }

        organizer.getUser().setFirstName(request.getFirstName());
        organizer.getUser().setLastName(request.getLastName());
        organizer.setOrganizerType(defaultOrganizerType(request.getOrganizerType()));
        organizer.setCompanyName(safeTrim(request.getCompanyName()));
        organizer.setCity(safeTrim(request.getCity()));
        organizer.setState(safeTrim(request.getState()));
        if (request.getStatus() != null) {
            organizer.setStatus(request.getStatus());
            organizer.getUser().setStatus(request.getStatus());
        }

        return mapOrganizer(organizerRepository.save(organizer));
    }

    @Transactional
    public void deleteOrganizer(Long organizerId) {
        Promoter promoter = getCurrentPromoter();
        Organizer organizer = organizerRepository.findById(organizerId)
                .orElseThrow(() -> new ResourceNotFoundException("Organizer not found"));

        if (!organizer.getPromoter().getId().equals(promoter.getId())) {
            throw new ResourceNotFoundException("Organizer not found in your account");
        }

        organizer.setStatus(com.vedant.concert_platform.entity.enums.Status.INACTIVE);
        organizer.getUser().setStatus(com.vedant.concert_platform.entity.enums.Status.INACTIVE);
        organizerRepository.save(organizer);
    }

    /** HARD DELETE — permanently removes organizer + user record */
    @Transactional
    public void hardDeleteOrganizer(Long organizerId) {
        Promoter promoter = getCurrentPromoter();
        Organizer organizer = organizerRepository.findById(organizerId)
                .orElseThrow(() -> new ResourceNotFoundException("Organizer not found"));
        if (!organizer.getPromoter().getId().equals(promoter.getId())) {
            throw new ResourceNotFoundException("Organizer not found in your account");
        }
        Long userId = organizer.getUser().getId();
        organizerRepository.delete(organizer);
        userRepository.deleteById(userId);
    }

    // Fix #21: per-row error reporting — don't roll back entire import for one bad row
    public HierarchyDto.CsvImportResult importOrganizers(MultipartFile file) {
        Promoter promoter = getCurrentPromoter();
        HierarchyDto.CsvImportResult result = new HierarchyDto.CsvImportResult();
        List<HierarchyDto.CsvRowError> rowErrors = new ArrayList<>();

        try (Reader reader = new InputStreamReader(file.getInputStream());
             CSVReaderHeaderAware csvReader = new CSVReaderHeaderAware(reader)) {
            Map<String, String> row;
            int rowNumber = 1;
            while ((row = csvReader.readMap()) != null) {
                rowNumber++;
                String email = safeTrim(row.get("email"));
                if (email == null || email.isBlank()) {
                    rowErrors.add(new HierarchyDto.CsvRowError(rowNumber, "email", "Email is required"));
                    continue;
                }
                if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
                    rowErrors.add(new HierarchyDto.CsvRowError(rowNumber, "email", "Invalid email format: " + email));
                    continue;
                }
                try {
                    processSingleOrganizerRow(row, email, promoter, result);
                } catch (ConflictException ex) {
                    rowErrors.add(new HierarchyDto.CsvRowError(rowNumber, "email", ex.getMessage()));
                } catch (Exception ex) {
                    rowErrors.add(new HierarchyDto.CsvRowError(rowNumber, "unknown", ex.getMessage()));
                }
            }
        } catch (IOException e) {
            throw new BadRequestException("Invalid CSV file: " + e.getMessage());
        } catch (CsvValidationException e) {
            throw new BadRequestException("CSV parse error: " + e.getMessage());
        }

        result.setRowErrors(rowErrors);
        return result;
    }

    @Transactional
    protected void processSingleOrganizerRow(Map<String, String> row, String email, Promoter promoter, HierarchyDto.CsvImportResult result) {
        Organizer organizer = organizerRepository.findByUserEmail(email).orElse(null);
        if (organizer == null) {
            User user = new User();
            user.setEmail(email);
            user.setPassword(passwordEncoder.encode(DEFAULT_ORGANIZER_PASSWORD));
            user.setFirstName(safeTrim(row.get("firstName")));
            user.setLastName(safeTrim(row.get("lastName")));
            user.setRole(Role.ORGANIZER);
            user.setFirstLogin(true);
            user = userRepository.save(user);

            organizer = new Organizer();
            organizer.setUser(user);
            organizer.setPromoter(promoter);
            organizer.setOrganizerType(defaultOrganizerType(row.get("organizerType")));
            organizer.setCompanyName(safeTrim(row.get("companyName")));
            organizer.setCity(safeTrim(row.get("city")));
            organizer.setState(safeTrim(row.get("state")));
            organizerRepository.save(organizer);
            result.setCreatedCount(result.getCreatedCount() + 1);
        } else {
            if (!organizer.getPromoter().getId().equals(promoter.getId())) {
                throw new ConflictException("Cannot update organizer belonging to another promoter: " + email);
            }
            organizer.getUser().setFirstName(safeTrim(row.get("firstName")));
            organizer.getUser().setLastName(safeTrim(row.get("lastName")));
            organizer.setOrganizerType(defaultOrganizerType(row.get("organizerType")));
            organizer.setCompanyName(safeTrim(row.get("companyName")));
            organizer.setCity(safeTrim(row.get("city")));
            organizer.setState(safeTrim(row.get("state")));
            organizerRepository.save(organizer);
            result.setUpdatedCount(result.getUpdatedCount() + 1);
        }
    }

    // Filter in Java to avoid lower(bytea) PostgreSQL type error
    @Transactional(readOnly = true)
    public byte[] exportOrganizers(String city, String state, String organizerType, String search) {
        Promoter promoter = getCurrentPromoter();
        String cityParam   = (city != null && !city.isBlank())           ? city.trim().toLowerCase()           : null;
        String stateParam  = (state != null && !state.isBlank())         ? state.trim().toLowerCase()          : null;
        String typeParam   = (organizerType != null && !organizerType.isBlank()) ? organizerType.trim()        : null;
        String searchParam = (search != null && !search.isBlank())       ? search.trim().toLowerCase()         : null;

        // Fetch all for promoter (simple query, no lower() in DB), then filter in Java
        List<Organizer> all = organizerRepository.findByPromoterId(promoter.getId());

        List<Organizer> organizers = all.stream().filter(o -> {
            if (cityParam != null && (o.getCity() == null || !o.getCity().toLowerCase().contains(cityParam)))
                return false;
            if (stateParam != null && (o.getState() == null || !o.getState().toLowerCase().contains(stateParam)))
                return false;
            if (typeParam != null && !typeParam.equals(o.getOrganizerType()))
                return false;
            if (searchParam != null) {
                String email = o.getUser().getEmail() != null ? o.getUser().getEmail().toLowerCase() : "";
                String fn    = o.getUser().getFirstName() != null ? o.getUser().getFirstName().toLowerCase() : "";
                String ln    = o.getUser().getLastName() != null ? o.getUser().getLastName().toLowerCase() : "";
                String co    = o.getCompanyName() != null ? o.getCompanyName().toLowerCase() : "";
                if (!email.contains(searchParam) && !fn.contains(searchParam)
                        && !ln.contains(searchParam) && !co.contains(searchParam))
                    return false;
            }
            return true;
        }).toList();

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             OutputStreamWriter writer = new OutputStreamWriter(outputStream);
             CSVWriter csvWriter = new CSVWriter(writer)) {
            csvWriter.writeNext(new String[]{"email", "firstName", "lastName", "organizerType", "companyName", "city", "state"});
            for (Organizer organizer : organizers) {
                csvWriter.writeNext(new String[]{
                        organizer.getUser().getEmail(),
                        organizer.getUser().getFirstName(),
                        organizer.getUser().getLastName(),
                        organizer.getOrganizerType(),
                        organizer.getCompanyName(),
                        organizer.getCity(),
                        organizer.getState()
                });
            }
            csvWriter.flush();
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new BadRequestException("Failed to export CSV: " + e.getMessage());
        }
    }

    private Promoter getCurrentPromoter() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getRole() != Role.PROMOTER) {
            throw new BadRequestException("Only promoter can access this resource");
        }
        if (!user.isMfaEnabled()) {
            throw new BadRequestException("MFA is required for this action");
        }
        return promoterRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Promoter profile not found"));
    }

    private HierarchyDto.OrganizerResponse mapOrganizer(Organizer organizer) {
        HierarchyDto.OrganizerResponse response = new HierarchyDto.OrganizerResponse();
        response.setId(organizer.getId());
        response.setEmail(organizer.getUser().getEmail());
        response.setFirstName(organizer.getUser().getFirstName());
        response.setLastName(organizer.getUser().getLastName());
        response.setOrganizerType(organizer.getOrganizerType());
        response.setCompanyName(organizer.getCompanyName());
        response.setCity(organizer.getCity());
        response.setState(organizer.getState());
        response.setStatus(organizer.getStatus());
        response.setFirstLoginRequired(organizer.getUser().isFirstLogin());
        return response;
    }

    private String safeTrim(String value) {
        return value == null ? null : value.trim();
    }

    private String defaultOrganizerType(String organizerType) {
        String value = safeTrim(organizerType);
        return (value == null || value.isBlank()) ? DEFAULT_ORGANIZER_TYPE : value;
    }
}
