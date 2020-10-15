package com.tietoevry.bookorabackend.model;

public enum RoleEnum {

    //Has to use prefix ROLE_ because in class SecurityExpressionRoot, private String defaultRolePrefix = "ROLE_";
    ROLE_USER,
    ROLE_ADMIN
}
