<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<head><title>${titre}</title></head>
<body>
<h1>${titre}</h1>
<form method="post" action="${pageContext.request.contextPath}/employe/save-objet">
    <p>
        <label for="nom">Nom</label>
        <input type="text" id="nom" name="nom" required/>
    </p>
    <p>
        <label for="age">Age</label>
        <input type="number" id="age" name="age"/>
    </p>
    <p>
        <label for="salaire">Salaire</label>
        <input type="number" step="0.01" id="salaire" name="salaire"/>
    </p>
    <p><button type="submit">Soumettre</button></p>
</form>
</body>
</html>
