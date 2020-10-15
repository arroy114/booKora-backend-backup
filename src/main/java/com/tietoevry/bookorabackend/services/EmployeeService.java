package com.tietoevry.bookorabackend.services;

import com.tietoevry.bookorabackend.api.v1.model.*;

public interface EmployeeService {
    EmployeeListDTO getAllEmployees();

    EmployeeDTO getEmployeeById(Long id);

    MessageDTO createNewEmployee(SignUpDTO signUpDTO);

    JwtDTO logIn(LogInDTO logInDTO);

    EmployeeDTO saveEmployeeByDTO(Long id, EmployeeDTO employeeDTO);

    //EmployeeDTO patchEmployee(Long id, EmployeeDTO employeeDTO);

    void deleteEmployeeDTO(Long id);
}
