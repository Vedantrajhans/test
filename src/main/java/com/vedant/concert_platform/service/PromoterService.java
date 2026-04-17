package com.vedant.concert_platform.service;

import com.vedant.concert_platform.entity.Promoter;
import com.vedant.concert_platform.repository.PromoterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PromoterService {

    private final PromoterRepository promoterRepository;

    public List<Promoter> getAll() {
        return promoterRepository.findAll();
    }

    public Promoter create(Promoter promoter) {
        return promoterRepository.save(promoter);
    }
}
