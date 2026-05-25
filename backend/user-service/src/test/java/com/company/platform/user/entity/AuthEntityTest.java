package com.company.platform.user.entity;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

class AuthEntityTest {

    @Test
    void authRolePrePersistCreatesId() {
        AuthRole role = new AuthRole();
        role.setName("ADMIN");

        role.prePersist();

        assertNotNull(role.getId());
    }
}
