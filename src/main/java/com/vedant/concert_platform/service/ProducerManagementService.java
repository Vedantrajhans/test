package com.vedant.concert_platform.service;

import com.vedant.concert_platform.dto.HierarchyDto;
import com.vedant.concert_platform.entity.Producer;
import com.vedant.concert_platform.entity.Promoter;
import com.vedant.concert_platform.entity.User;
import com.vedant.concert_platform.entity.enums.Role;
import com.vedant.concert_platform.exception.BadRequestException;
import com.vedant.concert_platform.exception.ConflictException;
import com.vedant.concert_platform.exception.ResourceNotFoundException;
import com.vedant.concert_platform.repository.ProducerRepository;
import com.vedant.concert_platform.repository.PromoterRepository;
import com.vedant.concert_platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProducerManagementService {
    private final UserRepository userRepository;
    private final ProducerRepository producerRepository;
    private final PromoterRepository promoterRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public HierarchyDto.PromoterResponse createPromoter(HierarchyDto.CreatePromoterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email already exists");
        }

        Producer producer = getCurrentProducer();
        ensureMfaEnabled(producer.getUser());

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setRole(Role.PROMOTER);
        user.setFirstLogin(false);
        user = userRepository.save(user);

        Promoter promoter = new Promoter();
        promoter.setUser(user);
        promoter.setProducer(producer);
        promoter = promoterRepository.save(promoter);
        return mapPromoter(promoter);
    }

    @Transactional(readOnly = true)
    public List<HierarchyDto.PromoterResponse> listPromoters() {
        Producer producer = getCurrentProducer();
        ensureMfaEnabled(producer.getUser());
        return promoterRepository.findByProducerId(producer.getId()).stream().map(this::mapPromoter).toList();
    }

    private Producer getCurrentProducer() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getRole() != Role.PRODUCER) {
            throw new BadRequestException("Only producer can access this resource");
        }
        return producerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Producer profile not found"));
    }

    private void ensureMfaEnabled(User user) {
        if (!user.isMfaEnabled()) {
            throw new BadRequestException("MFA is required for this action");
        }
    }

    private HierarchyDto.PromoterResponse mapPromoter(Promoter promoter) {
        HierarchyDto.PromoterResponse response = new HierarchyDto.PromoterResponse();
        response.setId(promoter.getId());
        response.setEmail(promoter.getUser().getEmail());
        response.setFirstName(promoter.getUser().getFirstName());
        response.setLastName(promoter.getUser().getLastName());
        response.setStatus(promoter.getStatus());
        return response;
    }
}
