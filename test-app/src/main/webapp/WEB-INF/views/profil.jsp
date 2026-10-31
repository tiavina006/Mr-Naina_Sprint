<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<head><title>${titre}</title></head>
<body>
<h1>${titre}</h1>
<p>Utilisateur connecte : <strong>${utilisateur}</strong></p>
<p><a href="${pageContext.request.contextPath}/employe/profil">Voir le profil</a></p>
<p><a href="${pageContext.request.contextPath}/employe/admin">Espace administrateur</a></p>
<p><a href="${pageContext.request.contextPath}/employe/logout">Se deconnecter</a></p>
</body>
</html>
