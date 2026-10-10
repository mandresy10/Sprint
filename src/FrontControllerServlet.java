package src;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
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

public class FrontControllerServlet extends HttpServlet {

    private final List<Class<?>> listClassAnnotated = new ArrayList<>();

    private String packageName;

    @Override
    public void init() throws ServletException {

        ServletConfig config = getServletConfig();

        String configuredPackage =
                config != null
                        ? config.getInitParameter("base-package")
                        : null;

        packageName =
                configuredPackage != null && !configuredPackage.isBlank()
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

        URL url =
                Thread.currentThread()
                        .getContextClassLoader()
                        .getResource(path);

        if (url == null) {
            throw new IOException(
                    "Package introuvable : " + packageName
            );
        }

        if ("jar".equals(url.getProtocol())) {

            scanJar(path, url);

        } else if ("file".equals(url.getProtocol())) {

            scanDirectory(
                    packageName,
                    new File(url.toURI())
            );
        }
    }

    private void scanJar(String path, URL url)
            throws IOException, ClassNotFoundException {

        JarURLConnection connection =
                (JarURLConnection) url.openConnection();

        JarFile jarFile = connection.getJarFile();

        Enumeration<JarEntry> entries =
                jarFile.entries();

        while (entries.hasMoreElements()) {

            JarEntry entry = entries.nextElement();

            String name = entry.getName();

            if (name.startsWith(path)
                    && name.endsWith(".class")
                    && !entry.isDirectory()) {

                String className =
                        name.replace('/', '.')
                                .substring(
                                        0,
                                        name.length() - 6
                                );

                registerIfController(className);
            }
        }
    }

    private void scanDirectory(
            String packageName,
            File folder
    ) throws ClassNotFoundException {

        File[] files = folder.listFiles();

        if (files == null) {
            return;
        }

        for (File file : files) {

            if (file.isDirectory()) {

                scanDirectory(
                        packageName + "." + file.getName(),
                        file
                );

                continue;
            }

            if (file.getName().endsWith(".class")) {

                String className =
                        packageName
                                + "."
                                + file.getName()
                                        .replace(".class", "");

                registerIfController(className);
            }
        }
    }

    private void registerIfController(String className)
            throws ClassNotFoundException {

        Class<?> clazz = Class.forName(className);

        if (clazz.isAnnotationPresent(Controller.class)) {

            listClassAnnotated.add(clazz);
        }
    }

    @Override
    protected void doGet(
            HttpServletRequest req,
            HttpServletResponse res
    ) throws ServletException, IOException {

        processRequest(req, res);
    }

    @Override
    protected void doPost(
            HttpServletRequest req,
            HttpServletResponse res
    ) throws ServletException, IOException {

        processRequest(req, res);
    }

    public void processRequest(
            HttpServletRequest req,
            HttpServletResponse res
    ) throws ServletException, IOException {

        String uri = req.getRequestURI();

        String contextPath = req.getContextPath();

        // Exemple :
        // uri         = /Framework/test/hello
        // contextPath = /Framework
        // route       = /test/hello

        String route =
                uri.substring(contextPath.length());

        String[] parts = route.split("/");

        if (parts.length < 3) {

            res.setStatus(
                    HttpServletResponse.SC_NOT_FOUND
            );

            res.getWriter().println("URL invalide");

            return;
        }

        String urlBase = "/" + parts[1];

        String methodName = parts[2];

        // Recherche du Controller

        for (Class<?> controllerClass : listClassAnnotated) {

            Controller controllerAnnotation =
                    controllerClass.getAnnotation(
                            Controller.class
                    );

            if (!controllerAnnotation
                    .urlBase()
                    .equals(urlBase)) {

                continue;
            }

            try {

                // Création de l'objet Controller

                Object controller =
                        controllerClass
                                .getDeclaredConstructor()
                                .newInstance();

                // Recherche de la méthode

                var method =
                        controllerClass
                                .getDeclaredMethod(methodName);

                // Exécution de la méthode

                Object result =
                        method.invoke(controller);

                // Vérification de @JsonResponse

                if (method.isAnnotationPresent(
                        JsonResponse.class)) {

                    res.setContentType(
                            "application/json"
                    );

                    res.setCharacterEncoding("UTF-8");

                    String json =
                            convertirJson(result);

                    PrintWriter out =
                            res.getWriter();

                    out.println(json);

                } else if(method.isAnnotationPresent(JsonObjet.class)){
                        res.setContentType("application/json");
                        res.setCharacterEncoding("UTF-8");

                        String json = convertirObjetEnJson(result);

                        PrintWriter out = res.getWriter();
                        out.println(json);

                } else {

                    res.setContentType(
                            "text/plain"
                    );

                    res.setCharacterEncoding("UTF-8");

                    PrintWriter out =
                            res.getWriter();

                    out.println(result);
                }

                return;

            } catch (Exception e) {

                throw new ServletException(e);
            }
        }

        // Aucun Controller trouvé

        res.setStatus(
                HttpServletResponse.SC_NOT_FOUND
        );

        res.getWriter().println(
                "Controller introuvable"
        );
    }

    private String convertirJson(Object objet) {

        if (objet == null) {

            return "null";
        }

        return "{ \"message\": \""
                + objet.toString()
                + "\" }";
    }

    private String convertirObjetEnJson(Object objet) {

    if (objet == null) {
        return "null";
    }

    StringBuilder json = new StringBuilder();
    json.append("{");

    var fields = objet.getClass().getDeclaredFields();

    for (int i = 0; i < fields.length; i++) {
        var field = fields[i];
        field.setAccessible(true);

        try {

            Object value = field.get(objet);

            json.append("\"")
                .append(field.getName())
                .append("\":");

            if (value instanceof String) {

                json.append("\"")
                    .append(value)
                    .append("\"");

            } else if (value instanceof Character) {

                json.append("\"")
                    .append(value)
                    .append("\"");

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