package com.vedant.concert_platform.service;

import com.vedant.concert_platform.dto.HierarchyDto;
import com.vedant.concert_platform.entity.Producer;
import com.vedant.concert_platform.entity.Promoter;
import com.vedant.concert_platform.entity.User;
import com.vedant.concert_platform.entity.enums.Role;
import com.vedant.concert_platform.entity.enums.Status;
import com.vedant.concert_platform.exception.BadRequestException;
import com.vedant.concert_platform.exception.ConflictException;
import com.vedant.concert_platform.exception.ResourceNotFoundException;
import com.vedant.concert_platform.repository.ProducerRepository;
import com.vedant.concert_platform.repository.PromoterRepository;
import com.vedant.concert_platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
        return promoterRepository.findByProducerId(producer.getId()).stream().map(this::mapPromoter).toList();
    }

    @Transactional
    public HierarchyDto.PromoterResponse updatePromoter(Long promoterId, HierarchyDto.UpdatePromoterRequest request) {
        Producer producer = getCurrentProducer();
        Promoter promoter = promoterRepository.findById(promoterId)
                .orElseThrow(() -> new ResourceNotFoundException("Promoter not found"));
        if (!promoter.getProducer().getId().equals(producer.getId())) {
            throw new ResourceNotFoundException("Promoter not found in your account");
        }
        promoter.getUser().setFirstName(request.getFirstName());
        promoter.getUser().setLastName(request.getLastName());
        if (request.getStatus() != null) {
            promoter.setStatus(request.getStatus());
            promoter.getUser().setStatus(request.getStatus());
        }
        return mapPromoter(promoterRepository.save(promoter));
    }

    /** Soft-deactivate */
    @Transactional
    public void deactivatePromoter(Long promoterId) {
        Producer producer = getCurrentProducer();
        Promoter promoter = promoterRepository.findById(promoterId)
                .orElseThrow(() -> new ResourceNotFoundException("Promoter not found"));
        if (!promoter.getProducer().getId().equals(producer.getId())) {
            throw new ResourceNotFoundException("Promoter not found in your account");
        }
        promoter.setStatus(Status.INACTIVE);
        promoter.getUser().setStatus(Status.INACTIVE);
        promoterRepository.save(promoter);
    }

    /** HARD DELETE — permanently removes promoter + user record */
    @Transactional
    public void hardDeletePromoter(Long promoterId) {
        Producer producer = getCurrentProducer();
        Promoter promoter = promoterRepository.findById(promoterId)
                .orElseThrow(() -> new ResourceNotFoundException("Promoter not found"));
        if (!promoter.getProducer().getId().equals(producer.getId())) {
            throw new ResourceNotFoundException("Promoter not found in your account");
        }
        Long userId = promoter.getUser().getId();
        promoterRepository.delete(promoter);
        userRepository.deleteById(userId);
    }

    /** List ALL platform users (any role) for producer oversight */
    @Transactional(readOnly = true)
    public Page<HierarchyDto.UserSummary> listAllUsers(Role role, Status status, Pageable pageable) {
        getCurrentProducer(); // auth check only
        Page<User> page;
        if (role != null && status != null) {
            page = userRepository.findByRoleAndStatus(role, status, pageable);
        } else if (role != null) {
            page = userRepository.findByRole(role, pageable);
        } else if (status != null) {
            page = userRepository.findByStatus(status, pageable);
        } else {
            page = userRepository.findAll(pageable);
        }
        return page.map(u -> {
            HierarchyDto.UserSummary s = new HierarchyDto.UserSummary();
            s.setId(u.getId());
            s.setEmail(u.getEmail());
            s.setFirstName(u.getFirstName());
            s.setLastName(u.getLastName());
            s.setRole(u.getRole());
            s.setStatus(u.getStatus());
            return s;
        });
    }

    /** Hard delete ANY user by ID (producer-only superpower) */
    @Transactional
    public void hardDeleteUser(Long userId) {
        getCurrentProducer();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getRole() == Role.PRODUCER) {
            throw new BadRequestException("Cannot delete producer account");
        }
        userRepository.delete(user);
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
