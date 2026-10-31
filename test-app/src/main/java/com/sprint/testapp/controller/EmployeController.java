package com.sprint.testapp.controller;

import com.sprint.framework.annotation.Controller;
import com.sprint.framework.annotation.GetMapping;
import com.sprint.framework.annotation.RequestParam;
import com.sprint.framework.annotation.UrlMapping;
import com.sprint.framework.annotation.APIREST;
import com.sprint.framework.mapping.HttpMethod;
import com.sprint.framework.mvc.ModelAndView;
import com.sprint.testapp.model.Employe;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
public class EmployeController {
    @GetMapping("/employe/liste")
    public ModelAndView liste() {
        return new ModelAndView("/WEB-INF/views/employes.jsp")
                .addAttribut("titre", "Liste des employes")
                .addAttribut("employes", Arrays.asList("Naina", "Amina"));
    }

    @GetMapping("/employe/formulaire")
    public ModelAndView formulaire() {
        return new ModelAndView("/WEB-INF/views/formulaire.jsp")
                .addAttribut("titre", "Ajouter un employe");
    }

    @UrlMapping(value = "/employe/save", method = HttpMethod.POST)
    public ModelAndView save(@RequestParam("nom") String nom, int age, double salaire) {
        return new ModelAndView("/WEB-INF/views/employes.jsp")
                .addAttribut("titre", "Employe enregistre")
                .addAttribut("employes", nom + " (age=" + age + ", salaire=" + salaire + ")");
    }

    @GetMapping("/employe/formulaire-objet")
    public ModelAndView formulaireObjet() {
        return new ModelAndView("/WEB-INF/views/formulaireObjet.jsp")
                .addAttribut("titre", "Ajouter un employe (objet)");
    }

    @UrlMapping(value = "/employe/save-objet", method = HttpMethod.POST)
    public ModelAndView saveObjet(Employe employe) {
        return new ModelAndView("/WEB-INF/views/employes.jsp")
                .addAttribut("titre", "Employe enregistre (objet lie)")
                .addAttribut("employes", employe.getNom()
                        + " (age=" + employe.getAge() + ", salaire=" + employe.getSalaire() + ")");
    }

    @GetMapping("/api/employes")
    @APIREST
    public List<String> apiListe() {
        return Arrays.asList("Naina", "Amina");
    }

    @GetMapping("/api/employe-json")
    @APIREST(alreadyJson = true)
    public String apiJson() {
        return "{\"status\":\"ok\"}";
    }
}