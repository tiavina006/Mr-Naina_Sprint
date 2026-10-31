package com.sprint.framework;

import com.sprint.framework.annotation.RequestParam;
import com.sprint.framework.exception.BindingException;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class FrontControllerBindingTest {

    public static class DemoController {
        public String save(String nom, int age, double salaire) {
            return nom + "|" + age + "|" + salaire;
        }

        public String saveAnnote(@RequestParam("nom") String nom,
                                 @RequestParam(value = "age", required = false) int age) {
            return nom + "|" + age;
        }

        public String saveAvecRequest(String nom, HttpServletRequest request) {
            return nom + "|" + request.getParameter("source");
        }

        public Personne savePersonne(Personne personne) {
            return personne;
        }

        public String saveSansConstructeur(Runnable runnable) {
            return "jamais";
        }
    }

    public static class Personne {
        private String nom;
        private int age;
        private double salaire;
        private java.util.Date naissance;
        private boolean nomViaSetter;

        public String getNom() {
            return nom;
        }

        public void setNom(String nom) {
            this.nomViaSetter = true;
            this.nom = nom;
        }

        public boolean isNomViaSetter() {
            return nomViaSetter;
        }

        public int getAge() {
            return age;
        }

        public double getSalaire() {
            return salaire;
        }

        public java.util.Date getNaissance() {
            return naissance;
        }
    }

    private static HttpServletRequest request(Map<String, String> params) {
        return (HttpServletRequest) Proxy.newProxyInstance(
                FrontControllerBindingTest.class.getClassLoader(),
                new Class<?>[]{HttpServletRequest.class},
                (proxy, method, args) -> {
                    if ("getParameter".equals(method.getName())) {
                        return params.get(args[0]);
                    }
                    return null;
                });
    }

    private static Map<String, String> params(String... keyValue) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < keyValue.length; i += 2) {
            map.put(keyValue[i], keyValue[i + 1]);
        }
        return map;
    }

    private Object invoke(String methodName, Class<?>[] parameterTypes, Map<String, String> params)
            throws Exception {
        FrontController frontController = new FrontController();
        Method method = DemoController.class.getMethod(methodName, parameterTypes);
        return frontController.invoke(method, new DemoController(), request(params));
    }

    @Test
    public void bindsStringIntAndDoubleByParameterName() throws Exception {
        Object result = invoke("save",
                new Class<?>[]{String.class, int.class, double.class},
                params("nom", "Naina", "age", "30", "salaire", "1234.56"));

        assertEquals("Naina|30|1234.56", result);
    }

    @Test
    public void bindsAnnotatedParametersByAnnotationValue() throws Exception {
        Object result = invoke("saveAnnote",
                new Class<?>[]{String.class, int.class},
                params("nom", "Amina", "age", "25"));

        assertEquals("Amina|25", result);
    }

    @Test
    public void missingOptionalParameterGetsDefaultValue() throws Exception {
        Object result = invoke("saveAnnote",
                new Class<?>[]{String.class, int.class},
                params("nom", "Amina"));

        assertEquals("Amina|0", result);
    }

    @Test
    public void missingRequiredParameterThrowsBindingException() throws Exception {
        try {
            invoke("saveAnnote",
                    new Class<?>[]{String.class, int.class},
                    params("age", "25"));
            fail("BindingException attendue");
        } catch (BindingException exception) {
            assertTrue(exception.getMessage().contains("nom"));
        }
    }

    @Test
    public void invalidNumericValueThrowsBindingException() throws Exception {
        try {
            invoke("save",
                    new Class<?>[]{String.class, int.class, double.class},
                    params("nom", "Naina", "age", "trente", "salaire", "1.5"));
            fail("BindingException attendue");
        } catch (BindingException exception) {
            assertTrue(exception.getMessage().contains("age"));
        }
    }

    @Test
    public void complexObjectIsInstantiatedAndFilledFromRequest() throws Exception {
        Personne personne = (Personne) invoke("savePersonne",
                new Class<?>[]{Personne.class},
                params("nom", "Naina", "age", "30", "salaire", "1234.56",
                        "naissance", "2000-01-01"));

        assertEquals("Naina", personne.getNom());
        assertEquals(30, personne.getAge());
        assertEquals(1234.56, personne.getSalaire(), 0d);
        assertNull(personne.getNaissance());
        assertTrue(personne.isNomViaSetter());
    }

    @Test
    public void complexObjectUsesSetterWhenItExists() throws Exception {
        Personne personne = (Personne) invoke("savePersonne",
                new Class<?>[]{Personne.class},
                params("nom", "Amina"));

        assertTrue(personne.isNomViaSetter());
        assertEquals("Amina", personne.getNom());
    }

    @Test
    public void absentObjectFieldsKeepDefaultValues() throws Exception {
        Personne personne = (Personne) invoke("savePersonne",
                new Class<?>[]{Personne.class},
                params());

        assertNull(personne.getNom());
        assertEquals(0, personne.getAge());
        assertEquals(0d, personne.getSalaire(), 0d);
        assertFalse(personne.isNomViaSetter());
    }

    @Test
    public void objectWithoutNoArgsConstructorThrowsBindingException() throws Exception {
        try {
            invoke("saveSansConstructeur",
                    new Class<?>[]{Runnable.class},
                    params());
            fail("BindingException attendue");
        } catch (BindingException exception) {
            assertTrue(exception.getMessage().contains("constructeur sans argument"));
        }
    }

    @Test
    public void httpServletRequestIsInjected() throws Exception {
        Object result = invoke("saveAvecRequest",
                new Class<?>[]{String.class, HttpServletRequest.class},
                params("nom", "Naina", "source", "formulaire"));

        assertEquals("Naina|formulaire", result);
    }

    @Test
    public void simpleTypesAreRecognized() {
        assertTrue(FrontController.isSimpleType(String.class));
        assertTrue(FrontController.isSimpleType(int.class));
        assertTrue(FrontController.isSimpleType(Double.class));
        assertTrue(FrontController.isSimpleType(double.class));
        assertTrue(FrontController.isSimpleType(boolean.class));
        assertFalse(FrontController.isSimpleType(java.util.Date.class));
        assertFalse(FrontController.isSimpleType(String[].class));
    }

    @Test
    public void defaultValuesFollowPrimitiveRules() {
        assertEquals(0, FrontController.defaultValue(int.class));
        assertEquals(0L, FrontController.defaultValue(long.class));
        assertEquals(0d, (Double) FrontController.defaultValue(double.class), 0d);
        assertEquals(Boolean.FALSE, FrontController.defaultValue(boolean.class));
        assertNull(FrontController.defaultValue(Integer.class));
        assertNull(FrontController.defaultValue(String.class));
    }
}
