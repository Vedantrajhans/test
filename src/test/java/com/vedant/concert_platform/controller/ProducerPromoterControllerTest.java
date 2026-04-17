package com.vedant.concert_platform.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vedant.concert_platform.entity.Producer;
import com.vedant.concert_platform.entity.Promoter;
import com.vedant.concert_platform.service.ProducerService;
import com.vedant.concert_platform.service.PromoterService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({ProducerController.class, PromoterController.class})
@AutoConfigureMockMvc(addFilters = false)
class ProducerPromoterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProducerService producerService;

    @MockBean
    private PromoterService promoterService;

    @Test
    void getProducers_shouldReturnList() throws Exception {
        Producer producer = new Producer();
        producer.setId(1L);
        producer.setName("Live Nation");
        producer.setEmail("contact@livenation.com");

        when(producerService.getAll()).thenReturn(List.of(producer));

        mockMvc.perform(get("/api/producers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Live Nation"));
    }

    @Test
    void createProducer_shouldReturnCreatedProducer() throws Exception {
        Producer request = new Producer();
        request.setName("AEG Presents");
        request.setEmail("hello@aegpresents.com");

        Producer response = new Producer();
        response.setId(2L);
        response.setName(request.getName());
        response.setEmail(request.getEmail());

        when(producerService.create(any(Producer.class))).thenReturn(response);

        mockMvc.perform(post("/api/producers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.email").value("hello@aegpresents.com"));
    }

    @Test
    void getPromoters_shouldReturnList() throws Exception {
        Promoter promoter = new Promoter();
        promoter.setId(1L);
        promoter.setName("City Events");
        promoter.setEmail("events@city.com");

        when(promoterService.getAll()).thenReturn(List.of(promoter));

        mockMvc.perform(get("/api/promoters"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("City Events"));
    }

    @Test
    void createPromoter_shouldReturnCreatedPromoter() throws Exception {
        Promoter request = new Promoter();
        request.setName("John Doe Promotions");
        request.setEmail("john@promotions.com");

        Promoter response = new Promoter();
        response.setId(2L);
        response.setName(request.getName());
        response.setEmail(request.getEmail());

        when(promoterService.create(any(Promoter.class))).thenReturn(response);

        mockMvc.perform(post("/api/promoters")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.email").value("john@promotions.com"));
    }
}
