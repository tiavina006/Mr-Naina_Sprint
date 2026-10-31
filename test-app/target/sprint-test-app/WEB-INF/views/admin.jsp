<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<head><title>${titre}</title></head>
<body>
<h1>${titre}</h1>
<p>Bienvenue <strong>${utilisateur}</strong>, vous avez le role admin.</p>
<p><a href="${pageContext.request.contextPath}/employe/profil">Retour au profil</a></p>
<p><a href="${pageContext.request.contextPath}/employe/logout">Se deconnecter</a></p>
</body>
</html>
