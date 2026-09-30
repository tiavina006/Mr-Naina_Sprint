# Sprint 0 et objectif Spring-like

## Structure

- `framework` produit `sprint-framework-0.1.0-SNAPSHOT.jar`.
- `test-app` produit `sprint-test-app.war` et contient l'application de test.
- `FrontController` est le servlet declare dans `test-app/src/main/webapp/WEB-INF/web.xml`.

## Flux `/employe/liste`

1. Tomcat envoie la requete au `FrontController`.
2. `doGet` calcule l'URL avec `doList`.
3. `ProcessRequest` retrouve la classe et la methode dans le registre.
4. Le registre a decouvert `EmployeController` grace a `@Controller` et `liste()` grace a `@GetMapping`.
5. Le resultat de `liste()` est ecrit dans la reponse, puis `outsprint` journalise l'URL.

## Creer une annotation

Une annotation declare :

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface GetMapping {
    String value();
}
```

- `@Target` definit ou l'annotation peut etre appliquee.
- `@Retention(RUNTIME)` permet de la recuperer par reflexion pendant l'execution.
- `value` est son attribut obligatoire, utilise comme nom court dans `@GetMapping("/chemin")`.

Pour creer un contrôleur, annoter la classe avec `@Controller`, puis annoter une methode avec `@GetMapping`.
`@RequestParam("nom")` peut etre appliquee a un parametre de methode ; le framework recupere alors sa valeur dans la requete et la convertit en `String`, `int`, `long` ou `boolean`.

## Construire le JAR et le WAR

Depuis PowerShell :

```powershell
.\scripts\build-framework.ps1
mvn clean package
```

Le script genere le JAR, le duplique dans `framework/target`, le copie dans `test-app/lib` et ajoute le dossier du JAR au PATH utilisateur. Un nouveau terminal est necessaire pour relire le PATH.

## Tester avec Tomcat

1. Copier `test-app/target/sprint-test-app.war` dans le dossier `webapps` de Tomcat.
2. Demarrer Tomcat avec `bin\startup.bat`.
3. Ouvrir `http://localhost:8080/sprint-test-app/employe/liste` dans le navigateur.

La page affiche `Liste des employes`. Une URL sans mapping renvoie HTTP 404.

## ModelAndView et REST

- `GET /sprint-test-app/employe/liste` invoque `liste()`, place `titre` et `employes` dans la requete, puis forward vers `WEB-INF/views/employes.jsp`.
- `GET /sprint-test-app/api/employes` invoque une methode `@APIREST` et serialise automatiquement sa collection en JSON.
- `GET /sprint-test-app/api/employe-json` utilise `@APIREST(alreadyJson = true)` et renvoie directement la chaine JSON fournie par le developpeur.

Une methode non REST qui ne retourne pas `ModelAndView` provoque une `ServletException`. Une methode `@APIREST` n'utilise jamais `RequestDispatcher`.

Le test automatisé se lance avec `mvn -pl test-app -am test`.