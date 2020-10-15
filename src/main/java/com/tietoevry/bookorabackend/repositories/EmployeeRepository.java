package com.tietoevry.bookorabackend.repositories;

import com.tietoevry.bookorabackend.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Employee findByEmailIgnoreCase(String email);
    Optional<Employee> findByEmail(String email);
    Boolean existsByEmail(String email);
}
