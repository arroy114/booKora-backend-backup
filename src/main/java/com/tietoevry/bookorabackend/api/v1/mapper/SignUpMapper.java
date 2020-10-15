package com.tietoevry.bookorabackend.api.v1.mapper;

import com.tietoevry.bookorabackend.api.v1.model.SignUpDTO;
import com.tietoevry.bookorabackend.model.Employee;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface SignUpMapper {

    SignUpMapper INSTANCE = Mappers.getMapper(SignUpMapper.class);

    @Mapping(target = "roles", ignore = true)
    Employee signUpDTOtoEmployee(SignUpDTO signUpDTO);
}
