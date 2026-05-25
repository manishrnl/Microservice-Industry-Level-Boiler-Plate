package com.company.platform.user.model;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

class UserModelEntityTest {

    @Test
    void profilePrePersistAndPreUpdateMaintainTimestamps() {
        UserProfile profile = new UserProfile();

        profile.prePersist();
        profile.preUpdate();

        assertNotNull(profile.getCreatedAt());
        assertNotNull(profile.getUpdatedAt());
    }

    @Test
    void identityAndContactTouchUpdatedAt() {
        UserIdentityDocument identity = new UserIdentityDocument();
        UserContactDetails contact = new UserContactDetails();

        identity.touch();
        contact.touch();

        assertNotNull(identity.getUpdatedAt());
        assertNotNull(contact.getUpdatedAt());
    }
}
