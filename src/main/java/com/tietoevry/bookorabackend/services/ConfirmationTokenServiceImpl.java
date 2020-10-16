package com.tietoevry.bookorabackend.services;

import com.tietoevry.bookorabackend.api.v1.mapper.EmployeeMapper;
import com.tietoevry.bookorabackend.api.v1.model.MessageDTO;
import com.tietoevry.bookorabackend.model.ConfirmationToken;
import com.tietoevry.bookorabackend.model.Employee;
import com.tietoevry.bookorabackend.repositories.ConfirmationTokenRepository;
import com.tietoevry.bookorabackend.repositories.EmployeeRepository;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;

@Service
public class ConfirmationTokenServiceImpl implements ConfirmationTokenService {

    private final ConfirmationTokenRepository confirmationTokenRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeMapper employeeMapper;

    public ConfirmationTokenServiceImpl(ConfirmationTokenRepository confirmationTokenRepository, EmployeeRepository employeeRepository, EmployeeMapper employeeMapper) {
        this.confirmationTokenRepository = confirmationTokenRepository;
        this.employeeRepository = employeeRepository;
        this.employeeMapper = employeeMapper;
    }

    @Override
    public MessageDTO checkToken(String confirmationToken) {

        ConfirmationToken token = confirmationTokenRepository.findByConfirmationToken(confirmationToken);

        if(token != null){


            if(token.getExpiryDate().before(new Timestamp(System.currentTimeMillis())) ){
                return new MessageDTO("Error: token has expired!");
            }

            Employee employee = employeeRepository.findByEmailIgnoreCase(token.getEmployee().getEmail());
            employee.setEnabled(true);
            Employee savedEmployee = employeeRepository.save(employee);
            return new MessageDTO("Your account is activated!");
        }
        else{
            return new MessageDTO("Error: The link is invalid or broken!");
        }
    }
}
