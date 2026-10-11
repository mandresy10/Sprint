<%@ page contentType="text/html;charset=UTF-8" language="java" %>
    <html>

    <head>
        <title>Formulaire Employé</title>
    </head>

    <body>
        <h2>Ajouter un employé</h2>
        <form action="${pageContext.request.contextPath}/employee/save" method="POST">
            <div>
                <label>Nom :</label>
                <input type="text" name="nom" required />
            </div>
            <div>
                <label>Prénom :</label>
                <input type="text" name="prenom" required />
            </div>
            <div>
                <label>Âge :</label>
                <input type="number" name="age" required />
            </div>
            <div>
                <label>Poste :</label>
                <input type="text" name="poste" required />
            </div>
            <button type="submit">Valider</button>
        </form>
    </body>

    </html>