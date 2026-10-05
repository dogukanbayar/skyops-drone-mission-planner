package com.droneops.mapper;

import com.droneops.domain.Operator;
import com.droneops.dto.OperatorRequest;
import com.droneops.dto.OperatorResponse;
import org.springframework.stereotype.Component;

@Component
public class OperatorMapper {

    public Operator toEntity(OperatorRequest request) {
        Operator operator = new Operator();
        operator.setFullName(request.fullName().trim());
        operator.setEmail(request.email().trim().toLowerCase());
        return operator;
    }

    public OperatorResponse toResponse(Operator operator) {
        return new OperatorResponse(operator.getId(), operator.getFullName(), operator.getEmail());
    }
}
