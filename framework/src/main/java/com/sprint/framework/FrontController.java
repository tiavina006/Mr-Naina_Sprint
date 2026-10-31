package com.sprint.framework;

import com.sprint.framework.mapping.Mapping;
import com.sprint.framework.mapping.HttpMethod;
import com.sprint.framework.mapping.MappingRegistry;
import com.sprint.framework.annotation.RequestParam;
import com.sprint.framework.annotation.APIREST;
import com.sprint.framework.exception.BindingException;
import com.sprint.framework.json.JsonSerializer;
import com.sprint.framework.mvc.ModelAndView;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.Map;

public class FrontController extends HttpServlet {
    private final MappingRegistry registry = new MappingRegistry();
    private String controllerPackage;

    @Override
    public void init() throws ServletException {
        controllerPackage = getInitParameter("controller-package");
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
        try {
            Object controller = mapping.getControllerClass().newInstance();
            Method action = mapping.getMethod();
            Object result = invoke(action, controller, request);
            APIREST apiRest = action.getAnnotation(APIREST.class);
            if (apiRest != null) {
                writeJson(response, result, apiRest.alreadyJson());
            } else {
                dispatchView(request, response, result);
            }
        } catch (ReflectiveOperationException exception) {
            throw new ServletException("Erreur lors de l'invocation de " + url, exception);
        }
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
            if (!isSimpleType(parameterType)) {
                arguments[index] = bindObject(parameterType, request);
                continue;
            }
            arguments[index] = defaultValue(parameterType);
            RequestParam requestParam = parameters[index].getAnnotation(RequestParam.class);
            String parameterName = requestParam != null ? requestParam.value() : parameters[index].getName();
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
}