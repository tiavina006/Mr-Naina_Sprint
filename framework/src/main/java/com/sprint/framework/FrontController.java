package com.sprint.framework;

import com.sprint.framework.mapping.Mapping;
import com.sprint.framework.mapping.HttpMethod;
import com.sprint.framework.mapping.MappingRegistry;
import com.sprint.framework.annotation.RequestParam;
import com.sprint.framework.annotation.APIREST;
import com.sprint.framework.annotation.Authenticated;
import com.sprint.framework.annotation.Authorized;
import com.sprint.framework.exception.AccessDeniedException;
import com.sprint.framework.exception.BindingException;
import com.sprint.framework.file.MaFile;
import com.sprint.framework.json.JsonSerializer;
import com.sprint.framework.mvc.ControllerRegistry;
import com.sprint.framework.mvc.ModelAndView;
import com.sprint.framework.security.Authorization;
import com.sprint.framework.session.MaSession;
import com.sprint.framework.session.SessionManager;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.Map;

@MultipartConfig
public class FrontController extends HttpServlet {
    private final MappingRegistry registry = new MappingRegistry();
    private String controllerPackage;
    private String authSessionKey = Authorization.DEFAULT_SESSION_KEY;
    private SessionManager sessionManager;
    private ControllerRegistry controllerRegistry;

    @Override
    public void init() throws ServletException {
        controllerPackage = getInitParameter("controller-package");
        authSessionKey = resolveAuthSessionKey();
        Object sharedSessionManager = getServletContext().getAttribute(SessionManager.ATTRIBUTE);
        if (sharedSessionManager instanceof SessionManager) {
            sessionManager = (SessionManager) sharedSessionManager;
        }
        Object sharedControllers = getServletContext().getAttribute(ControllerRegistry.ATTRIBUTE);
        if (sharedControllers instanceof ControllerRegistry) {
            controllerRegistry = (ControllerRegistry) sharedControllers;
        } else {
            controllerRegistry = new ControllerRegistry();
            getServletContext().setAttribute(ControllerRegistry.ATTRIBUTE, controllerRegistry);
        }
        Object sharedMappings = getServletContext().getAttribute(FrontControllerListener.MAPPINGS_ATTRIBUTE);
        if (sharedMappings instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<com.sprint.framework.mapping.UrlMethode, Mapping> mappings =
                    (Map<com.sprint.framework.mapping.UrlMethode, Mapping>) sharedMappings;
            registry.setMappings(mappings);
            return;
        }
        if (controllerPackage == null || controllerPackage.trim().isEmpty()) {
            throw new ServletException("L'init-param controller-package est obligatoire");
        }
        registry.scan(controllerPackage);
    }

    private String resolveAuthSessionKey() {
        Object sharedKey = getServletContext().getAttribute(Authorization.SESSION_KEY_ATTRIBUTE);
        if (sharedKey instanceof String && !((String) sharedKey).trim().isEmpty()) {
            return ((String) sharedKey).trim();
        }
        String key = getInitParameter(Authorization.SESSION_KEY_INIT_PARAM);
        if (key == null || key.trim().isEmpty()) {
            key = getServletContext().getInitParameter(Authorization.SESSION_KEY_INIT_PARAM);
        }
        return key == null || key.trim().isEmpty()
                ? Authorization.DEFAULT_SESSION_KEY : key.trim();
    }

    @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String geturl = doList(request);
        ProcessRequest(request, response, geturl);
        outsprint(geturl);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String postUrl = doList(request);
        ProcessRequest(request, response, postUrl, HttpMethod.POST);
        outsprint(postUrl);
    }

    protected String doList(HttpServletRequest request) {
        String contextPath = request.getContextPath();
        String path = request.getRequestURI().substring(contextPath.length());
        return path.isEmpty() ? "/" : path;
    }

        protected void ProcessRequest(HttpServletRequest request, HttpServletResponse response, String geturl)
            throws ServletException, IOException {
            ProcessRequest(request, response, geturl, HttpMethod.GET);
        }

        protected void ProcessRequest(HttpServletRequest request, HttpServletResponse response, String url,
                                      HttpMethod httpMethod) throws ServletException, IOException {
            Mapping mapping = registry.get(url, httpMethod);
        if (mapping == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Aucun mapping " + httpMethod + " pour " + url);
            return;
        }
        Method action = mapping.getMethod();
        Class<?> controllerClass = mapping.getControllerClass();
        try {
            authorize(action, controllerClass, request);
            Object controller = controllerRegistry().getOrCreate(controllerClass);
            Object result = invoke(action, controller, request);
            APIREST apiRest = action.getAnnotation(APIREST.class);
            if (apiRest != null) {
                writeJson(response, result, apiRest.alreadyJson());
            } else {
                dispatchView(request, response, result);
            }
        } catch (AccessDeniedException exception) {
            int status = exception.isAuthenticated()
                    ? HttpServletResponse.SC_FORBIDDEN
                    : HttpServletResponse.SC_UNAUTHORIZED;
            response.sendError(status, exception.getMessage());
        } catch (ReflectiveOperationException exception) {
            throw new ServletException("Erreur lors de l'invocation de " + url, exception);
        }
    }

    void authorize(Method action, Class<?> controllerClass, HttpServletRequest request) {
        Authenticated authenticated = action.getAnnotation(Authenticated.class);
        if (authenticated == null) {
            authenticated = controllerClass.getAnnotation(Authenticated.class);
        }
        Authorized authorized = action.getAnnotation(Authorized.class);
        if (authorized == null) {
            authorized = controllerClass.getAnnotation(Authorized.class);
        }
        if (authenticated == null && authorized == null) {
            return;
        }
        MaSession session = sessionManager().getOrCreate(request);
        if (authenticated != null) {
            Authorization.checkAuthenticated(session, authSessionKey);
        }
        if (authorized != null) {
            Authorization.check(session, authorized, authSessionKey);
        }
        Object principal = session.get(authSessionKey);
        if (principal == null) {
            principal = Authorization.getPrincipal(session);
        }
        request.setAttribute(Authorization.AUTHENTICATED_REQUEST_ATTRIBUTE, Boolean.TRUE);
        request.setAttribute(Authorization.PRINCIPAL_REQUEST_ATTRIBUTE, principal);
    }

    private void dispatchView(HttpServletRequest request, HttpServletResponse response, Object result)
            throws ServletException, IOException {
        if (!(result instanceof ModelAndView)) {
            throw new ServletException("Une action non REST doit retourner ModelAndView");
        }
        ModelAndView modelAndView = (ModelAndView) result;
        addArgToRequest(request, modelAndView.getData());
        request.getRequestDispatcher(modelAndView.getView()).forward(request, response);
    }

    private void addArgToRequest(HttpServletRequest request, Map<String, Object> data) {
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            request.setAttribute(entry.getKey(), entry.getValue());
        }
    }

    private void writeJson(HttpServletResponse response, Object result, boolean alreadyJson) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().print(alreadyJson ? String.valueOf(result) : JsonSerializer.toJson(result));
    }

    Object invoke(Method method, Object controller, HttpServletRequest request)
            throws ReflectiveOperationException {
        Parameter[] parameters = method.getParameters();
        Class<?>[] parameterTypes = method.getParameterTypes();
        if (parameters.length == 0) {
            return method.invoke(controller);
        }
        Object[] arguments = new Object[parameters.length];
        for (int index = 0; index < parameters.length; index++) {
            Class<?> parameterType = parameterTypes[index];
            if (parameterType.equals(HttpServletRequest.class)) {
                arguments[index] = request;
                continue;
            }
            RequestParam requestParam = parameters[index].getAnnotation(RequestParam.class);
            String parameterName = requestParam != null ? requestParam.value() : parameters[index].getName();
            if (parameterType.equals(MaSession.class)) {
                arguments[index] = sessionManager().getOrCreate(request);
                continue;
            }
            if (parameterType.equals(MaFile.class)) {
                arguments[index] = bindMaFile(parameterName, request);
                continue;
            }
            if (!isSimpleType(parameterType)) {
                arguments[index] = bindObject(parameterType, request);
                continue;
            }
            arguments[index] = defaultValue(parameterType);
            String value = request.getParameter(parameterName);
            if (value == null) {
                if (requestParam != null && requestParam.required()) {
                    throw new BindingException("Parametre obligatoire absent: " + parameterName);
                }
                continue;
            }
            arguments[index] = convert(value, parameterType, parameterName);
        }
        return method.invoke(controller, arguments);
    }

    private MaFile bindMaFile(String parameterName, HttpServletRequest request) {
        try {
            Part part = request.getPart(parameterName);
            if (part == null || part.getSize() == 0) {
                throw new BindingException("Fichier obligatoire absent: " + parameterName);
            }
            String fileName = part.getSubmittedFileName();
            if (fileName == null || fileName.isEmpty()) {
                fileName = parameterName;
            }
            String uploadPath = getServletConfig() != null
                    ? getServletContext().getInitParameter("upload.path") : null;
            try (InputStream input = part.getInputStream()) {
                return new MaFile(fileName, part.getContentType(), MaFile.readAllBytes(input), uploadPath);
            }
        } catch (IOException | ServletException exception) {
            throw new BindingException("Impossible de lire le fichier " + parameterName, exception);
        }
    }

    ControllerRegistry controllerRegistry() {
        if (controllerRegistry == null && getServletConfig() != null) {
            Object shared = getServletContext().getAttribute(ControllerRegistry.ATTRIBUTE);
            if (shared instanceof ControllerRegistry) {
                controllerRegistry = (ControllerRegistry) shared;
            } else {
                controllerRegistry = new ControllerRegistry();
                getServletContext().setAttribute(ControllerRegistry.ATTRIBUTE, controllerRegistry);
            }
        }
        if (controllerRegistry == null) {
            controllerRegistry = new ControllerRegistry();
        }
        return controllerRegistry;
    }

    SessionManager sessionManager() {
        if (sessionManager == null && getServletConfig() != null) {
            Object shared = getServletContext().getAttribute(SessionManager.ATTRIBUTE);
            if (shared instanceof SessionManager) {
                sessionManager = (SessionManager) shared;
            }
        }
        if (sessionManager == null) {
            sessionManager = new SessionManager(SessionManager.DEFAULT_TIMEOUT_MS);
        }
        return sessionManager;
    }

    static Object bindObject(Class<?> type, HttpServletRequest request) throws ReflectiveOperationException {
        Object target;
        try {
            target = type.newInstance();
        } catch (InstantiationException | IllegalAccessException exception) {
            throw new BindingException("Impossible d'instancier l'objet " + type.getName()
                    + " (constructeur sans argument requis)", exception);
        }
        for (Class<?> current = type; current != null && !current.equals(Object.class);
             current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {
                    continue;
                }
                if (!isSimpleType(field.getType())) {
                    continue;
                }
                String value = request.getParameter(field.getName());
                if (value == null) {
                    continue;
                }
                Object converted = convert(value, field.getType(), field.getName());
                Method setter = findSetter(current, field);
                if (setter != null) {
                    setter.setAccessible(true);
                    setter.invoke(target, converted);
                } else {
                    field.setAccessible(true);
                    field.set(target, converted);
                }
            }
        }
        return target;
    }

    private static Method findSetter(Class<?> type, Field field) {
        String setterName = "set" + Character.toUpperCase(field.getName().charAt(0)) + field.getName().substring(1);
        for (Method method : type.getDeclaredMethods()) {
            if (!method.getName().equals(setterName) || method.getParameterTypes().length != 1) {
                continue;
            }
            if (boxed(method.getParameterTypes()[0]).equals(boxed(field.getType()))) {
                return method;
            }
        }
        return null;
    }

    private static Class<?> boxed(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }
        if (type.equals(Integer.TYPE)) return Integer.class;
        if (type.equals(Long.TYPE)) return Long.class;
        if (type.equals(Double.TYPE)) return Double.class;
        if (type.equals(Float.TYPE)) return Float.class;
        if (type.equals(Boolean.TYPE)) return Boolean.class;
        if (type.equals(Short.TYPE)) return Short.class;
        if (type.equals(Byte.TYPE)) return Byte.class;
        if (type.equals(Character.TYPE)) return Character.class;
        return type;
    }

    static boolean isSimpleType(Class<?> type) {
        return type.equals(String.class)
                || type.equals(Integer.TYPE) || type.equals(Integer.class)
                || type.equals(Long.TYPE) || type.equals(Long.class)
                || type.equals(Double.TYPE) || type.equals(Double.class)
                || type.equals(Float.TYPE) || type.equals(Float.class)
                || type.equals(Boolean.TYPE) || type.equals(Boolean.class)
                || type.equals(Short.TYPE) || type.equals(Short.class)
                || type.equals(Byte.TYPE) || type.equals(Byte.class)
                || type.equals(Character.TYPE) || type.equals(Character.class);
    }

    static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        if (type.equals(Integer.TYPE)) return 0;
        if (type.equals(Long.TYPE)) return 0L;
        if (type.equals(Double.TYPE)) return 0d;
        if (type.equals(Float.TYPE)) return 0f;
        if (type.equals(Boolean.TYPE)) return Boolean.FALSE;
        if (type.equals(Short.TYPE)) return (short) 0;
        if (type.equals(Byte.TYPE)) return (byte) 0;
        return '\0';
    }

    static Object convert(String value, Class<?> targetType, String parameterName) {
        if (value == null) {
            return defaultValue(targetType);
        }
        try {
            if (targetType.equals(String.class)) {
                return value;
            }
            if (targetType.equals(Integer.class) || targetType.equals(Integer.TYPE)) {
                return Integer.valueOf(value);
            }
            if (targetType.equals(Long.class) || targetType.equals(Long.TYPE)) {
                return Long.valueOf(value);
            }
            if (targetType.equals(Double.class) || targetType.equals(Double.TYPE)) {
                return Double.valueOf(value);
            }
            if (targetType.equals(Float.class) || targetType.equals(Float.TYPE)) {
                return Float.valueOf(value);
            }
            if (targetType.equals(Boolean.class) || targetType.equals(Boolean.TYPE)) {
                return Boolean.valueOf(value);
            }
            if (targetType.equals(Short.class) || targetType.equals(Short.TYPE)) {
                return Short.valueOf(value);
            }
            if (targetType.equals(Byte.class) || targetType.equals(Byte.TYPE)) {
                return Byte.valueOf(value);
            }
            if (targetType.equals(Character.class) || targetType.equals(Character.TYPE)) {
                if (value.length() != 1) {
                    throw new BindingException("Valeur invalide pour le parametre " + parameterName
                            + ": attendu un seul caractere, recu '" + value + "'");
                }
                return value.charAt(0);
            }
        } catch (NumberFormatException exception) {
            throw new BindingException("Valeur invalide pour le parametre " + parameterName
                    + " (type " + targetType.getSimpleName() + "): '" + value + "'", exception);
        }
        throw new BindingException("Type de parametre non supporte: " + targetType.getName());
    }

    protected void outsprint(String geturl) {
        getServletContext().log(geturl);
    }

    public MappingRegistry getRegistry() {
        return registry;
    }

    void setAuthSessionKey(String authSessionKey) {
        this.authSessionKey = authSessionKey == null || authSessionKey.trim().isEmpty()
                ? Authorization.DEFAULT_SESSION_KEY : authSessionKey.trim();
    }
}