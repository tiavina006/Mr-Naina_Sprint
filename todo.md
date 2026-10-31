## Sprint 0 : Front controller
- creation de FrontController
- creation du fonction processRequest
- apple de process request avec doGet et doPost

## Sprint 1 : Annotation
- creation de l'annotation "Controller"
- creation des classes utils :
    - UtilAnalyser : fonction chercher tous les classes dans le projet (selon package ou non)
    - ScanAnnotation : fonction trouver tous les classes qui contient les annotations à chercher
- ajout de l'attribut classes dans FrontController
- Overide de la fonction init() pour charger tous les classes lors du lancement
- Modification de processRequest pour afficher tous les classes trouver

##  Sprint 2 : Url Mapping
- creation de l'annotation "UrlMaping"
- ajout de l'argument url dans l'annotation
- Ajout de fonction dans ScanAnnotation :
    - fonction getMethodWithAnnotation : fonction qui retourne tous les methodes d'une classe qui contient une annotation
- creation d'un classe Mapping qui contient class et method 
- ajout d'un attribut HashMap mapping dans FrontController :
    - clé : url de la map (String)
    - value : la nouvelle classe Mapping
- creation d'une methode initMapping dans FrontController pour remplir le hashMap
    - parcour des listes de classes initialiser
    - appel de la fonction getMethodWithAnnotation
    - creation des mapping 
- appel de initMapping dans init
- changement de processRequest 
    - afficher une erreur en cas de mapping introuvable et afficher tous les url disponible
    - afficher l'url, sa classe et methode correspondant si url dispo
- Refactor : creation des variables static final pour garder les class des annotation


## Sprint 3 

### Sprint 3 : suite de Sprint 2
Prise en charge des types de methode http (GET/POST)
- Creation d'une classe UrlMethode :
    - attributs :
        - url (String)
        - methode (String ou creation d'un Enum java) GET/POST
    - Override de la fonction equals pour des futurs comparaison
- Ajout d'un nouveau attribut dans l'annotation UrlMapping : method pour definir le methode http
- Changement de la clé de l'ancien map par la nouvelle classe
- Mise à jour de la fonction initMApping

### Sprint 3-Bis
- Invocation de la methode qui contient le l'url par reflexion

### Sprint 4-Special
- Creation de la classe FrontControllerListner.java :
    - implement : ServletContextListner
    - Override de la fonction ContextInitialized
    - transfert des fonctions de l'ancien init pour initialiser 
    - envoye du mapping gener par ServletContext
- Recupaeration des maps depuis le FrontController par Context


### Sprint 5-Support de vue ModelAndView
- Creation de la classe ModelAndView :
    - attributs : String view, Map<String, Object> data
    - fonction addAttribut(clé, object) pour ajouter des élements à la map
    - constructeur avec le parametre view
    - fonction setview
- Modification de ProcessRequest sur l'invocation:
    - exception si le type de retour n'est pas ModelAndView
    - caster le resultat de retour 
    - creation d'un fonction pour ajouter data du model dans le request SErvlet : addArgToRequest(req, map)
    - creation d'un dispatcher qui envoye vers la page correspondant au vu

### Sprint 6 : REST API
- On va implémenter Web API pour retourner un JSON
Objectif: 
    - Un méthode d'action dans le controlleur ne retroune pas un Vue mais renvoie un JSON
    - Pas de requestDispatcher

Comment : 
    - ajouter un annotaion @APIREST (qui va renvoyer un JSON) au niveau methode
        - Avec un attribut True-false pour voir si elle est déjà en JSON ou pas
    Coté Framework
    -Tester l'existence de cette annotation au niveau de la  methode : return JSON sinon dispatcher (Content-Type: application/json)
    2 type
    - La création du JSON se crée au niveau framework
    - Du coté developpeur aussi, si le type est String mais si c'est une autre type on appel la fonction toJSON du framework


### Sprint 7 : Data Binding des types primitifs / simples (String, int, double, etc.)
Objectif : Lier les données envoyées depuis une vue (formulaire HTML/JSP) vers les paramètres de la méthode du contrôleur.

Processus depuis la vue :
- L'utilisateur remplit un formulaire d'ajout et clique sur le bouton Soumettre
- Le formulaire envoie une requête vers une URL ciblée (ex : l'URL correspondante à la méthode save)

Traitement par le Front Controller (Servlet) :
- Le Front Servlet intercepte la requête, identifie la méthode du contrôleur associée à l'URL et vérifie si la méthode HTTP (GET, POST, etc.) correspond
- Il inspecte ensuite les paramètres attendus par la méthode cible (via Reflection API : `Method.getParameters()`)

Gestion du Data Binding (Cas sans objet) :
- La méthode du contrôleur accepte des paramètres individuels (ex: String nom, int age, double salaire)
- Si un paramètre est un objet complexe, le framework lève une exception `BindingException` (la gestion des objets est réservée au Sprint 7 bis)
- Instanciation par défaut : les arguments sont initialisés à null (wrapper/String) ou à la valeur par défaut du type primitif (0, false, '\0')
- Récupération des valeurs : le Servlet récupère la quantité et les noms des arguments de la méthode à appeler via la réflexion
- Il fait correspondre les noms/types des paramètres de la requête HTTP avec ceux de la méthode :
    - nom via `@RequestParam("nom")` si présent, sinon via le nom du paramètre de la méthode (flag `-parameters` activé dans le pom parent)
    - type via conversion : String, int, long, double, float, boolean, short, byte, char (+ leurs wrappers)
- La méthode du contrôleur est invoquée dynamiquement en lui passant le tableau d'arguments résolus (`Method.invoke(controller, arguments)`)

Realisation :
- Creation de l'exception `BindingException` (com.sprint.framework.exception) : paramètre obligatoire absent, valeur invalide, objet complexe
- Refactor de `FrontController.invoke` : boucle sur les paramètres via réflexion, valeur par défaut, injection de HttpServletRequest
- Ajout de `FrontController.isSimpleType` / `defaultValue` / `convert` : support de double, float, short, byte, char en plus de String, int, long, boolean
- Activation de `<parameters>true</parameters>` (maven-compiler-plugin) pour le binding par nom de paramètre
- Vue : `formulaire.jsp` (formulaire POST) + action `@UrlMapping("/employe/save", method=POST)` avec String nom, int age, double salaire
- Tests : `FrontControllerBindingTest` (binding par nom/annotation, valeurs par défaut, exception requis/invalide/objet complexe, injection request)


### Sprint 7 bis : Data Binding d'un objet complexe
Objectif : Lier automatiquement les champs du formulaire à un objet transmis en argument de la méthode d'action.

Prise en charge d'un argument Objet :
- La méthode d'action du contrôleur reçoit un argument de type objet (ex : Employe, Personne)

Invocations et boucle sur les propriétés :
- Lors de l'invocation, le framework détecte un argument qui n'est ni un type simple ni HttpServletRequest
- Il instantie l'objet (`Class.newInstance()` — constructeur sans argument obligatoire, sinon `BindingException`)
- Il parcourt (boucle sur) les champs de l'objet (y compris les champs de la hiérarchie, statiques/synthétiques ignorés)
- Pour chaque champ, le Servlet fait un `request.getParameter(nomDuChamp)` pour vérifier si une valeur correspondante est présente
- Si une valeur existe, elle est convertie au bon type (conversion des types simples) puis injectée :
    - via le setter `setXxx` s'il existe (recherche par réflexion, compatibilité de type)
    - sinon par accès direct via réflexion (`field.setAccessible(true)` + `field.set`)
- Les champs de type non simple (objet imbriqué, List...) sont ignorés (restent à leur valeur par défaut)
- La méthode du contrôleur est ensuite invoquée avec cet objet complété

Realisation :
- `FrontController.bindObject(type, request)` : instanciation, boucle sur les champs, `findSetter` / accès direct
- `FrontController.invoke` : un argument non simple est lié comme objet (plus d'exception pour les objets)
- Vue : `formulaireObjet.jsp` + action `GET /employe/formulaire-objet` et `POST /employe/save-objet` avec argument `Employe` (model `com.sprint.testapp.model.Employe`)
- Tests : `FrontControllerBindingTest` (remplissage objet, setter vs champ direct, valeurs par défaut, champ non simple ignoré, absence de constructeur sans argument)


### Upload ana fichier    
### Gestion de Session
### Authaurisation