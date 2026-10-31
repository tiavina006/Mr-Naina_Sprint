<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<head><title>${titre}</title></head>
<body>
<h1>${titre}</h1>
<form method="post" enctype="multipart/form-data"
      action="${pageContext.request.contextPath}/employe/upload">
    <p>
        <label for="nom">Nom</label>
        <input type="text" id="nom" name="nom" required/>
    </p>
    <p>
        <label for="fichier">Fichier</label>
        <input type="file" id="fichier" name="fichier" required/>
    </p>
    <p><button type="submit">Envoyer</button></p>
</form>
</body>
</html>
