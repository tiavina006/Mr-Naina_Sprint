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


### Sprint 8 : Upload de fichier
Objectif : Transmettre un fichier depuis un formulaire HTML/JSP vers une méthode d'action du contrôleur.

Principe :
- Le formulaire doit utiliser `enctype="multipart/form-data"` pour envoyer le fichier avec les autres données
- Le framework doit savoir qu'un fichier accompagne la requête et le rendre disponible dans la méthode d'action
- Ce qui est disponible dans l'action : un objet `MaFile` (et non un simple tableau de bytes nu)

Classe `MaFile` (`com.sprint.framework.file`) :
- `fileName` : nom original du fichier
- `fileType` : type / extension (content-type)
- `data` : tableau de bytes
- `size` : taille en octets
- Instanciée par le framework à partir du `Part` de la requête
- `save()` : enregistre dans le répertoire configuré (`upload.path`)
- `saveTo(directory)` : enregistre dans un répertoire explicite
- Le développeur choisit ensuite la destination : BDD ou système de fichiers

Configuration :
- `@MultipartConfig` sur `FrontController` (support natif Servlet 3.0, `request.getPart`)
- Chemin de stockage configurable via le context-param `upload.path` (jamais en dur dans le code)

Realisation :
- Creation de `com.sprint.framework.file.MaFile`
- `FrontController.bindMaFile` : lecture du `Part`, nom, content-type, bytes ; `BindingException` si fichier absent
- `FrontController.invoke` : detection du type `MaFile` et injection de l'objet
- `web.xml` : context-param `upload.path`
- Vue `formulaireUpload.jsp` + action `GET /employe/formulaire-upload` et `POST /employe/upload` (`String nom, MaFile fichier`)
- Tests : `MaFileTest` (getters, `saveTo`, `save` configure, absence de repertoire), binding `MaFile` dans `FrontControllerBindingTest`


### Sprint 9 : Gestion de Session
Objectif : Rendre une session propre au framework disponible dans la méthode d'action, sans exposer `HttpSession` au développeur.

Classe `MaSession` (`com.sprint.framework.session`) :
- `id` (identifiant client)
- `attributes` : `Map<String, Object>`
- `get(key)` / `set(key, value)` / `remove(key)` / `invalidate()` (creation, modification, suppression)
- `lastAccessTime` + `touch()` + `isExpired(timeoutMs)` pour l'expiration

Gestion des sessions :
- `SessionManager` maintient un `Map<String, MaSession>` : une instance par client
- Le client est identifié par l'ID de l'`HttpSession` (utilisé en interne uniquement)
- `getOrCreate(request)` : retrouve ou crée la session du client
- Nettoyage paresseux (`cleanExpired`) a chaque requete pour eviter les fuites memoire
- Timeout configurable via le context-param `session.timeout` (en secondes, defaut 1800)

Realisation :
- Creation de `com.sprint.framework.session.MaSession` et `SessionManager`
- `FrontControllerListener` : instancie et stocke le `SessionManager` dans le `ServletContext`, lit `session.timeout`
- `FrontController` : detection du type `MaSession` et injection dans la methode d'action
- `web.xml` : context-param `session.timeout`
- Vues `login.jsp`, `profil.jsp` + actions `GET/POST /employe/login` et `GET /employe/profil` (`MaSession session`)
- Tests : `MaSessionTest` (get/set/remove/invalidate/expiration), `SessionManagerTest` (meme client = meme instance, clients differents, nettoyage lazy, suppression), injection `MaSession` dans `FrontControllerBindingTest`


### Sprint 10 : Authentification et autorisation
Objectif : Verifier l'authentification puis les droits d'acces avant d'executer une methode d'action du controleur.

Principe :
- Une methode (ou une classe de controleur) peut exiger une authentification, un role ou une permission.
- Le framework controle ces droits avant `Method.invoke` ; l'action n'est jamais appelee si l'acces est refuse.
- Le developpeur n'utilise que `MaSession`, `@Authenticated` et `@Authorized`, jamais `HttpSession`.

Authentification :
- `@Authenticated` sur une methode (ou une classe) exige un utilisateur connecte.
- La variable de session qui indique la connexion est configuree par l'init-param/context-param `auth.session-key` declare dans `web.xml`.
- `FrontController.init()` lit cette cle depuis le `ServletContext` (defaut `authorization.principal`).
- Apres verification, `authenticated=true` et `principal` sont exposes en attributs de requete avec `setAttribute`.
- `Authorization.checkAuthenticated(session, cle)` leve `AccessDeniedException` (HTTP 401) si la variable est absente.

Annotation `@Authorized` (`com.sprint.framework.annotation`) :
- cible `METHOD` et `TYPE`, retention `RUNTIME`
- `roles()` : liste de roles exiges (au moins un role doit correspondre)
- `permissions()` : liste de permissions exigees (toutes doivent etre presentes)
- sans attribut : exige seulement un utilisateur authentifie
- l'annotation de methode est prioritaire ; sinon l'annotation de classe s'applique

Classe `Authorization` (`com.sprint.framework.security`) :
- constantes d'attributs de session : `authorization.principal`, `authorization.roles`, `authorization.permissions`
- `login(session, principal, roles...)` / `login(session, principal, roles, permissions)` : cree l'utilisateur
- `logout(session)` : supprime les donnees d'autorisation
- `isAuthenticated`, `hasRole`, `hasPermission`, `isAllowed` : lecture des droits
- `check(session, authorized)` : leve `AccessDeniedException`

Integration framework :
- `FrontController.authorize` : lit `@Authorized` sur la methode puis sur la classe et verifie la `MaSession`
- `AccessDeniedException` (`authenticated = false`) : HTTP 401 (authentification requise)
- `AccessDeniedException` (`authenticated = true`) : HTTP 403 (droits insuffisants)

Realisation :
- Creation de `com.sprint.framework.annotation.Authenticated` et `Authorized`
- Creation de `com.sprint.framework.security.Authorization`
- Creation de `com.sprint.framework.exception.AccessDeniedException`
- `FrontController` : lecture de `auth.session-key` dans `init()`, controle d'authentification/autorisation avant l'instanciation du controleur, exposition des attributs `authenticated`/`principal`, reponses 401/403
- `FrontControllerListener` : publication de la cle d'authentification dans le `ServletContext`
- `web.xml` : `context-param` + `init-param` `auth.session-key`
- Test-app : `POST /employe/login` attribue le role `admin` a l'utilisateur `admin`, sinon `user` ; `GET /employe/profil` est protegee par `@Authenticated` ; `GET /employe/admin` est protegee par `@Authorized(roles = "admin")` ; `GET /employe/logout` nettoie la session
- Vues : `admin.jsp` + liens dans `profil.jsp`
- Tests : `AuthorizationTest` (cle de session configuree, `@Authenticated`, attributs de requete, authentification, role de classe/methode, priorite methode, roles multiples, permissions, logout, roles separes par virgule) et `EmployeControllerTest` (role attribue au login, protection des actions profil/admin, logout)


### Sprint 10 bis : Gestion des roles et singleton des controleurs
Objectif : Verifier un role precis ou plusieurs roles avant l'execution, et reutiliser une seule instance de chaque controleur.

Autorisation par role :
- `@Authorized(roles = "PROF")` n'autorise que les utilisateurs possedant exactement ce role.
- `@Authorized(roles = {"PROF", "ADMIN"})` autorise l'acces si l'utilisateur possede au moins un des roles listes.
- Les roles sont lus depuis `MaSession` (`authorization.roles`) et compares au nom declare dans l'annotation.
- Un utilisateur authentifie sans le role requis recoit HTTP 403 ; un utilisateur non authentifie recoit HTTP 401.

Singleton des controleurs :
- `ControllerRegistry` (`com.sprint.framework.mvc`) conserve une instance par classe de controleur dans une `Map<Class<?>, Object>`.
- `getOrCreate(Class<?>)` verifie d'abord si l'instance existe ; sinon elle est creee puis memorisee.
- `FrontControllerListener` publie le registre dans le `ServletContext` (`ControllerRegistry.ATTRIBUTE`).
- `FrontController.init()` recupere le registre existant du contexte ; s'il est absent, il le cree et le publie.
- `ProcessRequest` utilise `controllerRegistry().getOrCreate(controllerClass)` au lieu de `controllerClass.newInstance()`.

Realisation :
- Creation de `com.sprint.framework.mvc.ControllerRegistry`
- `FrontController` : attribut `controllerRegistry`, initialisation dans `init()`, acces paresseux, reutilisation des instances
- `FrontControllerListener` : publication du registre partage dans le `ServletContext`
- Tests : `ControllerSingletonTest` (meme instance pour une classe, instances distinctes par classe, registre partage du `FrontController`) et `AuthorizationTest` (role `PROF`, role `ADMIN`, role inconnu refuse pour `@Authorized(roles = {"PROF", "ADMIN"})`)
