package com.tietoevry.bookorabackend.services;

import com.tietoevry.bookorabackend.api.v1.mapper.EmployeeMapper;
import com.tietoevry.bookorabackend.api.v1.mapper.SignUpMapper;
import com.tietoevry.bookorabackend.api.v1.model.*;
import com.tietoevry.bookorabackend.controllers.EmployeeController;
import com.tietoevry.bookorabackend.model.ConfirmationToken;
import com.tietoevry.bookorabackend.model.Employee;
import com.tietoevry.bookorabackend.model.Role;
import com.tietoevry.bookorabackend.model.RoleEnum;
import com.tietoevry.bookorabackend.repositories.ConfirmationTokenRepository;
import com.tietoevry.bookorabackend.repositories.EmployeeRepository;
import com.tietoevry.bookorabackend.repositories.RoleRepository;
import com.tietoevry.bookorabackend.security.jwt.JwtUtils;
import com.tietoevry.bookorabackend.security.services.UserDetailsImpl;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class EmployeeServiceImp implements EmployeeService {

    private final AuthenticationManager authenticationManager;
    private final EmployeeMapper employeeMapper;
    private final SignUpMapper signUpMapper;
    private final PasswordEncoder encoder;
    private final JwtUtils jwtUtils;
    private final EmployeeRepository employeeRepository;
    private final EmailSenderService emailSenderService;
    private final RoleRepository roleRepository;
    private final ConfirmationTokenRepository confirmationTokenRepository;

    public EmployeeServiceImp(AuthenticationManager authenticationManager, EmployeeMapper employeeMapper, SignUpMapper signUpMapper, PasswordEncoder encoder, JwtUtils jwtUtils, EmployeeRepository employeeRepository, EmailSenderService emailSenderService, RoleRepository roleRepository, ConfirmationTokenRepository confirmationTokenRepository) {
        this.authenticationManager = authenticationManager;
        this.employeeMapper = employeeMapper;
        this.signUpMapper = signUpMapper;
        this.encoder = encoder;
        this.jwtUtils = jwtUtils;
        this.employeeRepository = employeeRepository;
        this.emailSenderService = emailSenderService;
        this.roleRepository = roleRepository;
        this.confirmationTokenRepository = confirmationTokenRepository;
    }

    @Override
    public EmployeeListDTO getAllEmployees() {
        List<EmployeeDTO> employeeDTOList = employeeRepository.findAll().stream().map(employee -> {
            EmployeeDTO employeeDTO = employeeMapper.employeeToEmployeeDTO(employee);
            employeeDTO.setEmployeeUrl(getEmployeeUrl(employee.getId()));
            return employeeDTO;
        }).collect(Collectors.toList());
        return new EmployeeListDTO(employeeDTOList);
    }

    @Override
    public EmployeeDTO getEmployeeById(Long id) {
        return employeeRepository
                .findById(id)
                .map(employeeMapper::employeeToEmployeeDTO)
                .map(employeeDTO -> {
                    employeeDTO.setEmployeeUrl(getEmployeeUrl(id));
                    return employeeDTO;
                })
                .orElseThrow(RuntimeException::new);//TODO make exception handler
    }

    @Override
    public MessageDTO createNewEmployee(SignUpDTO signUpDTO) {

        if(existedByEmail(signUpDTO.getEmail())){
            return new MessageDTO("Error: Email is already in use!");
        }
        else{
            //Mapping signDTO to employee
            Employee employee = signUpMapper.signUpDTOtoEmployee(signUpDTO);

            //encode password
            employee.setPassword(encoder.encode(signUpDTO.getPassword()));

            //Check if the roles exist, if exist add to employee
            Set<String> strRoles = signUpDTO.getRoles();
            Set<Role> roles = new HashSet<>();
            if (strRoles == null) {
                Role userRole = roleRepository.findByName(RoleEnum.ROLE_USER)
                        .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                roles.add(userRole);
            } else {
                strRoles.forEach(role -> {
                    if ("admin".equals(role)) {
                        Role adminRole = roleRepository.findByName(RoleEnum.ROLE_ADMIN)
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                        roles.add(adminRole);
                    }
                });
            }

            //Always add employee as a user
            Role userRole = roleRepository.findByName(RoleEnum.ROLE_USER)
                    .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
            roles.add(userRole);

            employee.setRoles(roles);
            Employee savedEmployee = employeeRepository.save(employee);

            ConfirmationToken confirmationToken = new ConfirmationToken(savedEmployee);
            confirmationTokenRepository.save(confirmationToken);

            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setTo(employee.getEmail());
            mailMessage.setSubject("Complete Registration!");
            mailMessage.setFrom("oslomet8@gmail.com");
            mailMessage.setText("To confirm your account, please click here : "
                    +"http://localhost:8080/confirm-account?token="+confirmationToken.getConfirmationToken());

            emailSenderService.sendEmail(mailMessage);

            return new MessageDTO("User registered successfully!");
        }

    }

    @Override
    public JwtDTO logIn(LogInDTO logInDTO) {

        //Authenticate and return an Authentication object that can be used to find user information
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(logInDTO.getEmail(), logInDTO.getPassword()));

        //Update SecurityContext using Authentication object
        SecurityContextHolder.getContext().setAuthentication(authentication);

        //Generate JWT
        String jwt = jwtUtils.generateJwtToken(authentication);

        //Get UserDetails from Authentication object
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal(); //authentication.getPrincipal() return object of org.springframework.security.core.userdetails.User

        //Get roles from UserDetails
        List<String> roles = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toList());

        return new JwtDTO(jwt,
                userDetails.getId(),
                userDetails.getEmail(),
                roles);
    }

    @Override
    public EmployeeDTO saveEmployeeByDTO(Long id, EmployeeDTO employeeDTO) {
        Employee employeeToSave = employeeMapper.employeeDTOtoEmployee((employeeDTO));
        employeeToSave.setId(id);

        return saveAndReturnDTO(employeeToSave);
    }

    @Override
    public void deleteEmployeeDTO(Long id) {
        employeeRepository.deleteById(id);
    }

    private String getEmployeeUrl(Long id) {
        return EmployeeController.BASE_URL + "/" + id;
    }

    private EmployeeDTO saveAndReturnDTO(Employee employee) {
        Employee savedEmloyee = employeeRepository.save(employee);
        EmployeeDTO employeeDTO = employeeMapper.employeeToEmployeeDTO(savedEmloyee);
        employeeDTO.setEmployeeUrl(getEmployeeUrl(savedEmloyee.getId()));
        return employeeDTO;
    }

    private boolean existedByEmail(String email){
        Employee employee = employeeRepository.findByEmailIgnoreCase(email);
        if (employee != null) return true;
        return false;
    }
}
