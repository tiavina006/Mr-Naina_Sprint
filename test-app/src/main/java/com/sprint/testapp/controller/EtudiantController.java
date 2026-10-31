package com.sprint.testapp.controller;

import com.sprint.framework.annotation.Controller;
import com.sprint.framework.annotation.GetMapping;
import com.sprint.framework.mvc.ModelAndView;

@Controller
public class EtudiantController {
    private int appels;

    @GetMapping("/etudiant/methode1")
    public ModelAndView methode1() {
        appels++;
        return vue("Methode 1");
    }

    @GetMapping("/etudiant/methode2")
    public ModelAndView methode2() {
        appels++;
        return vue("Methode 2");
    }

    public int getAppels() {
        return appels;
    }

    private ModelAndView vue(String methode) {
        return new ModelAndView("/WEB-INF/views/employes.jsp")
                .addAttribut("titre", "Etudiant - " + methode)
                .addAttribut("employes", "appels=" + appels
                        + ", instance=" + System.identityHashCode(this));
    }
}
