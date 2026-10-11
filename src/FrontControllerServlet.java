package src;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import src.annotation.Controller;
import src.annotation.JsonResponse;
import src.annotation.JsonObjet;
import src.annotation.Param;
import src.annotation.Url;

public class FrontControllerServlet extends HttpServlet {

    // Remplace la simple liste par une table de routage URL -> (Controller, Method)
    private final Map<String, Mapping> urlMapping = new HashMap<>();
    private String packageName;

    @Override
    public void init() throws ServletException {
        ServletConfig config = getServletConfig();
        String configuredPackage = config != null ? config.getInitParameter("base-package") : null;
        packageName = configuredPackage != null && !configuredPackage.isBlank()
                ? configuredPackage
                : "projet.main";

        try {
            scanPackage(packageName);
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    private void scanPackage(String packageName)
            throws IOException, ClassNotFoundException, URISyntaxException {
        String path = packageName.replace('.', '/');
        URL url = Thread.currentThread().getContextClassLoader().getResource(path);

        if (url == null) {
            throw new IOException("Package introuvable : " + packageName);
        }

        if ("jar".equals(url.getProtocol())) {
            scanJar(path, url);
        } else if ("file".equals(url.getProtocol())) {
            scanDirectory(packageName, new File(url.toURI()));
        }
    }

    private void scanJar(String path, URL url)
            throws IOException, ClassNotFoundException {
        JarURLConnection connection = (JarURLConnection) url.openConnection();
        JarFile jarFile = connection.getJarFile();
        Enumeration<JarEntry> entries = jarFile.entries();

        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String name = entry.getName();

            if (name.startsWith(path) && name.endsWith(".class") && !entry.isDirectory()) {
                String className = name.replace('/', '.').substring(0, name.length() - 6);
                registerIfController(className);
            }
        }
    }

    private void scanDirectory(String packageName, File folder) throws ClassNotFoundException {
        File[] files = folder.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                scanDirectory(packageName + "." + file.getName(), file);
                continue;
            }

            if (file.getName().endsWith(".class")) {
                String className = packageName + "." + file.getName().replace(".class", "");
                registerIfController(className);
            }
        }
    }

    private void registerIfController(String className) throws ClassNotFoundException {
        Class<?> clazz = Class.forName(className);

        if (clazz.isAnnotationPresent(Controller.class)) {
            Controller controllerAnno = clazz.getAnnotation(Controller.class);
            String urlBase = controllerAnno.urlBase();

            // Parcourt les méthodes pour enregistrer les routes définies par @Url
            for (Method method : clazz.getDeclaredMethods()) {
                if (method.isAnnotationPresent(Url.class)) {
                    Url urlAnno = method.getAnnotation(Url.class);
                    String fullUrl = cleanUrl(urlBase + urlAnno.value());
                    urlMapping.put(fullUrl, new Mapping(clazz, method));
                }
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        processRequest(req, res);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        processRequest(req, res);
    }

    public void processRequest(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        String uri = req.getRequestURI();
        String contextPath = req.getContextPath();
        String route = cleanUrl(uri.substring(contextPath.length()));

        Mapping mapping = urlMapping.get(route);

        if (mapping == null) {
            res.setStatus(HttpServletResponse.SC_NOT_FOUND);
            res.getWriter().println("URL non trouvée : " + route);
            return;
        }

        try {
            Class<?> controllerClass = mapping.getControllerClass();
            Object controller = controllerClass.getDeclaredConstructor().newInstance();
            Method method = mapping.getMethod();

            // 1. IDENTIFICATION ET CONVERSION DES PARAMÈTRES DU FORMULAIRE (@Param)
            Object[] args = resolveMethodArguments(method, req);

            // 2. IDENTIFICATION DU TYPE DE RETOUR DE LA MÉTHODE
            Class<?> returnType = method.getReturnType();
            System.out.println("=== [Sprint 7] Méthode exécutée : " + method.getName() + " ===");
            System.out.println("=== Type de retour : " + returnType.getName() + " ===");

            // 3. EXÉCUTION DE LA MÉTHODE AVEC LES ARGUMENTS INJECTÉS
            Object result = method.invoke(controller, args);

            // Gestion des réponses (JSON ou Vue standard / Redirection)
            if (method.isAnnotationPresent(JsonResponse.class)) {
                res.setContentType("application/json");
                res.setCharacterEncoding("UTF-8");
                res.getWriter().println(convertirJson(result));

            } else if (method.isAnnotationPresent(JsonObjet.class)) {
                res.setContentType("application/json");
                res.setCharacterEncoding("UTF-8");
                res.getWriter().println(convertirObjetEnJson(result));

            } else {
                // Comportement par défaut (ex: RequestDispatcher vers la JSP)
                if (result instanceof String viewPath) {
                    req.getRequestDispatcher(viewPath).forward(req, res);
                } else {
                    res.setContentType("text/plain");
                    res.setCharacterEncoding("UTF-8");
                    res.getWriter().println(result);
                }
            }

        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    /**
     * Analyse les paramètres de la méthode, lit les valeurs HTTP et gère la conversion de types.
     * * Analyse les paramètres de la méthode :
 * - Si annoté par @Param, récupère le paramètre HTTP spécifique.
 * - Sinon, effectue le Data Binding automatique pour un objet complexe (ex: Employee).
     */
    private Object[] resolveMethodArguments(Method method, HttpServletRequest req) {
    Parameter[] parameters = method.getParameters();
    Object[] args = new Object[parameters.length];

    for (int i = 0; i < parameters.length; i++) {
        Parameter parameter = parameters[i];
        Class<?> paramType = parameter.getType();

        if (parameter.isAnnotationPresent(Param.class)) {
            // Cas 1 : Paramètre simple annoté par @Param("nom")
            Param paramAnnotation = parameter.getAnnotation(Param.class);
            String paramName = paramAnnotation.value();
            String rawValue = req.getParameter(paramName);
            args[i] = convertValue(rawValue, paramType);

        } else {
            // Cas 2 : Objet complexe (Data Binding automatique)
            try {
                Object complexObj = paramType.getDeclaredConstructor().newInstance();
                var fields = paramType.getDeclaredFields();

                for (var field : fields) {
                    field.setAccessible(true);
                    String fieldName = field.getName();
                    String rawValue = req.getParameter(fieldName);

                    if (rawValue != null) {
                        Object convertedValue = convertValue(rawValue, field.getType());
                        field.set(complexObj, convertedValue);
                    }
                }
                args[i] = complexObj;
                System.out.println("-> Objet bindé avec succès : " + paramType.getSimpleName());

            } catch (Exception e) {
                throw new RuntimeException("Erreur lors du binding de l'objet : " + paramType.getName(), e);
            }
        }
    }

    return args;
}

    private Object convertValue(String value, Class<?> targetType) {
        if (value == null) {
            return targetType.isPrimitive() ? 0 : null;
        }

        if (targetType == String.class) {
            return value;
        } else if (targetType == int.class || targetType == Integer.class) {
            return Integer.parseInt(value);
        } else if (targetType == long.class || targetType == Long.class) {
            return Long.parseLong(value);
        } else if (targetType == double.class || targetType == Double.class) {
            return Double.parseDouble(value);
        } else if (targetType == boolean.class || targetType == Boolean.class) {
            return Boolean.parseBoolean(value);
        }

        return value;
    }

    private String cleanUrl(String url) {
        if (url == null) return "/";
        String cleaned = url.replaceAll("//+", "/");
        if (!cleaned.startsWith("/")) {
            cleaned = "/" + cleaned;
        }
        return cleaned;
    }

    private String convertirJson(Object objet) {
        if (objet == null) return "null";
        return "{ \"message\": \"" + objet.toString() + "\" }";
    }

    private String convertirObjetEnJson(Object objet) {
        if (objet == null) return "null";

        StringBuilder json = new StringBuilder();
        json.append("{");
        var fields = objet.getClass().getDeclaredFields();

        for (int i = 0; i < fields.length; i++) {
            var field = fields[i];
            field.setAccessible(true);

            try {
                Object value = field.get(objet);
                json.append("\"").append(field.getName()).append("\":");

                if (value instanceof String || value instanceof Character) {
                    json.append("\"").append(value).append("\"");
                } else {
                    json.append(value);
                }

                if (i < fields.length - 1) {
                    json.append(",");
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        json.append("}");
        return json.toString();
    }
}