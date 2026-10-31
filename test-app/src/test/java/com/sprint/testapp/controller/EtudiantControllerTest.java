package com.sprint.testapp.controller;

import com.sprint.framework.mapping.HttpMethod;
import com.sprint.framework.mapping.MappingRegistry;
import com.sprint.framework.mvc.ControllerRegistry;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class EtudiantControllerTest {

    @Test
    public void allActionsOfTheSameControllerShareOneInstance() throws Exception {
        ControllerRegistry registry = new ControllerRegistry();

        EtudiantController premierAppel = (EtudiantController) registry.getOrCreate(EtudiantController.class);
        premierAppel.methode1();
        EtudiantController secondAppel = (EtudiantController) registry.getOrCreate(EtudiantController.class);
        secondAppel.methode2();

        assertSame(premierAppel, secondAppel);
        assertEquals(2, secondAppel.getAppels());
        assertTrue(registry.size() == 1);
    }

    @Test
    public void bothEtudiantActionsAreMapped() {
        MappingRegistry registry = new MappingRegistry();
        registry.scan("com.sprint.testapp.controller");

        assertEquals(EtudiantController.class,
                registry.get("/etudiant/methode1", HttpMethod.GET).getControllerClass());
        assertEquals(EtudiantController.class,
                registry.get("/etudiant/methode2", HttpMethod.GET).getControllerClass());
    }
}
