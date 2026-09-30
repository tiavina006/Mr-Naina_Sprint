package com.sprint.framework;

import com.sprint.framework.mapping.Mapping;
import com.sprint.framework.mapping.HttpMethod;
import com.sprint.framework.mapping.MappingRegistry;
import com.sprint.framework.annotation.RequestParam;
import com.sprint.framework.annotation.APIREST;
import com.sprint.framework.json.JsonSerializer;
import com.sprint.framework.mvc.ModelAndView;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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

    private Object invoke(Method method, Object controller, HttpServletRequest request)
            throws ReflectiveOperationException {
        Class<?>[] parameterTypes = method.getParameterTypes();
        if (parameterTypes.length == 0) {
            return method.invoke(controller);
        }
        Object[] arguments = new Object[parameterTypes.length];
        for (int index = 0; index < parameterTypes.length; index++) {
            if (parameterTypes[index].equals(HttpServletRequest.class)) {
                arguments[index] = request;
            } else if (method.getParameters()[index].isAnnotationPresent(RequestParam.class)) {
                RequestParam requestParam = method.getParameters()[index].getAnnotation(RequestParam.class);
                String value = request.getParameter(requestParam.value());
                if (value == null && requestParam.required()) {
                    throw new IllegalArgumentException("Parametre obligatoire absent: " + requestParam.value());
                }
                arguments[index] = convert(value, parameterTypes[index]);
            } else {
                throw new IllegalArgumentException("Parametre non supporte: " + parameterTypes[index].getName());
            }
        }
        return method.invoke(controller, arguments);
    }

    private Object convert(String value, Class<?> targetType) {
        if (value == null) {
            return null;
        }
        if (targetType.equals(String.class)) {
            return value;
        }
        if (targetType.equals(Integer.class) || targetType.equals(Integer.TYPE)) {
            return Integer.valueOf(value);
        }
        if (targetType.equals(Long.class) || targetType.equals(Long.TYPE)) {
            return Long.valueOf(value);
        }
        if (targetType.equals(Boolean.class) || targetType.equals(Boolean.TYPE)) {
            return Boolean.valueOf(value);
        }
        throw new IllegalArgumentException("Type de parametre non supporte: " + targetType.getName());
    }

    protected void outsprint(String geturl) {
        getServletContext().log(geturl);
    }

    public MappingRegistry getRegistry() {
        return registry;
    }
}