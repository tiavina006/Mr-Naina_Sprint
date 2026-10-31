<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<head><title>${titre}</title></head>
<body>
<h1>${titre}</h1>
<form method="post" action="${pageContext.request.contextPath}/employe/login">
    <p>
        <label for="utilisateur">Utilisateur</label>
        <input type="text" id="utilisateur" name="utilisateur" required/>
    </p>
    <p><button type="submit">Se connecter</button></p>
</form>
</body>
</html>
