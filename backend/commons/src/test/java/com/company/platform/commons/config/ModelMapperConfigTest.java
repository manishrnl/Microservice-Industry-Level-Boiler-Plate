package com.company.platform.commons.config;

import org.testng.annotations.Test;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;

import static org.testng.Assert.*;

class ModelMapperConfigTest {

    @Test
    void modelMapperUsesStrictMatchingAndSkipsNullValues() {
        ModelMapper mapper = new ModelMapperConfig().modelMapper();

        assertEquals(mapper.getConfiguration().getMatchingStrategy(), MatchingStrategies.STRICT);
        assertTrue(mapper.getConfiguration().isSkipNullEnabled());
    }
}
