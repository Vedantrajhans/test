package com.vedant.concert_platform.service;

import com.opencsv.CSVReaderHeaderAware;
import com.opencsv.CSVWriter;
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
    public HierarchyDto.CsvImportResult importOrganizers(MultipartFile file) {
        Promoter promoter = getCurrentPromoter();
        HierarchyDto.CsvImportResult result = new HierarchyDto.CsvImportResult();

        try (Reader reader = new InputStreamReader(file.getInputStream());
             CSVReaderHeaderAware csvReader = new CSVReaderHeaderAware(reader)) {
            Map<String, String> row;
            while ((row = csvReader.readMap()) != null) {
                String email = safeTrim(row.get("email"));
                if (email == null || email.isBlank()) {
                    continue;
                }
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
                        throw new ConflictException("Cannot update organizer belonging to another promoter");
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
        } catch (IOException e) {
            throw new BadRequestException("Invalid CSV file: " + e.getMessage());
        }

        return result;
    }

    @Transactional(readOnly = true)
    public byte[] exportOrganizers() {
        Promoter promoter = getCurrentPromoter();
        List<Organizer> organizers = organizerRepository.findByPromoterId(promoter.getId());

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
