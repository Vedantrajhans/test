package com.vedant.concert_platform.service;

import com.vedant.concert_platform.entity.Producer;
import com.vedant.concert_platform.repository.ProducerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProducerService {

    private final ProducerRepository producerRepository;

    public List<Producer> getAll() {
        return producerRepository.findAll();
    }

    public Producer create(Producer producer) {
        return producerRepository.save(producer);
    }
}
