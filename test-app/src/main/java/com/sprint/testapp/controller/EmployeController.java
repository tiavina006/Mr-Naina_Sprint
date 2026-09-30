package com.sprint.testapp.controller;

import com.sprint.framework.annotation.Controller;
import com.sprint.framework.annotation.GetMapping;
import com.sprint.framework.annotation.APIREST;
import com.sprint.framework.mvc.ModelAndView;

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