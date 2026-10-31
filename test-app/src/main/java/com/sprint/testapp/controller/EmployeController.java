package com.sprint.testapp.controller;

import com.sprint.framework.annotation.Controller;
import com.sprint.framework.annotation.GetMapping;
import com.sprint.framework.annotation.RequestParam;
import com.sprint.framework.annotation.UrlMapping;
import com.sprint.framework.annotation.APIREST;
import com.sprint.framework.annotation.Authenticated;
import com.sprint.framework.annotation.Authorized;
import com.sprint.framework.file.MaFile;
import com.sprint.framework.mapping.HttpMethod;
import com.sprint.framework.mvc.ModelAndView;
import com.sprint.framework.security.Authorization;
import com.sprint.framework.session.MaSession;
import com.sprint.testapp.model.Employe;

import java.io.IOException;
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
                .addAttribut("employes", Arrays.asList("karoty", "votaby"));
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
        return Arrays.asList("karoty", "votaby");
    }

    @GetMapping("/api/employe-json")
    @APIREST(alreadyJson = true)
    public String apiJson() {
        return "{\"status\":\"ok\"}";
    }

    @GetMapping("/employe/formulaire-upload")
    public ModelAndView formulaireUpload() {
        return new ModelAndView("/WEB-INF/views/formulaireUpload.jsp")
                .addAttribut("titre", "Uploader un fichier");
    }

    @UrlMapping(value = "/employe/upload", method = HttpMethod.POST)
    public ModelAndView upload(String nom, MaFile fichier) throws IOException {
        fichier.save();
        return new ModelAndView("/WEB-INF/views/employes.jsp")
                .addAttribut("titre", "Fichier recu")
                .addAttribut("employes", nom + " a envoye " + fichier.getFileName()
                        + " (" + fichier.getSize() + " octets, type=" + fichier.getFileType() + ")");
    }

    @GetMapping("/employe/login")
    public ModelAndView loginForm() {
        return new ModelAndView("/WEB-INF/views/login.jsp")
                .addAttribut("titre", "Connexion");
    }

    @UrlMapping(value = "/employe/login", method = HttpMethod.POST)
    public ModelAndView login(@RequestParam("utilisateur") String utilisateur, MaSession session) {
        session.set("utilisateur", utilisateur);
        String role = "admin".equalsIgnoreCase(utilisateur) ? "admin" : "user";
        Authorization.login(session, utilisateur, role);
        return new ModelAndView("/WEB-INF/views/profil.jsp")
                .addAttribut("titre", "Connecte")
                .addAttribut("utilisateur", utilisateur);
    }

    @GetMapping("/employe/logout")
    public ModelAndView logout(MaSession session) {
        Authorization.logout(session);
        session.remove("utilisateur");
        return new ModelAndView("/WEB-INF/views/login.jsp")
                .addAttribut("titre", "Deconnecte");
    }

    @GetMapping("/employe/profil")
    @Authenticated
    public ModelAndView profil(MaSession session) {
        return new ModelAndView("/WEB-INF/views/profil.jsp")
                .addAttribut("titre", "Profil")
                .addAttribut("utilisateur", session.get("utilisateur"));
    }

    @GetMapping("/employe/admin")
    @Authorized(roles = "admin")
    public ModelAndView admin(MaSession session) {
        return new ModelAndView("/WEB-INF/views/admin.jsp")
                .addAttribut("titre", "Espace administrateur")
                .addAttribut("utilisateur", session.get("utilisateur"));
    }
}