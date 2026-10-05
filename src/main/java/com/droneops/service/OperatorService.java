package com.droneops.service;

import com.droneops.dto.OperatorRequest;
import com.droneops.dto.OperatorResponse;
import com.droneops.exception.BusinessRuleException;
import com.droneops.exception.ResourceNotFoundException;
import com.droneops.mapper.OperatorMapper;
import com.droneops.repository.OperatorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OperatorService {

    private final OperatorRepository operatorRepository;
    private final OperatorMapper mapper;

    @Transactional
    public OperatorResponse create(OperatorRequest request) {
        if (operatorRepository.existsByEmailIgnoreCase(request.email().trim())) {
            throw new BusinessRuleException("Bu e-posta adresiyle kayıtlı bir operatör zaten var.");
        }
        return mapper.toResponse(operatorRepository.save(mapper.toEntity(request)));
    }

    public List<OperatorResponse> findAll() {
        return operatorRepository.findAll().stream().map(mapper::toResponse).toList();
    }

    public OperatorResponse findById(Long id) {
        return operatorRepository.findById(id).map(mapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Operatör", id));
    }
}
