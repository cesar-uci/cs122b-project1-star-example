<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
ss<html>
<head>
    <title>Fabflix Dashboard</title>
    <style>
        body { font-family: sans-serif; padding: 2em; }
        .container { max-width: 800px; margin: auto; }
        h1, h3 { margin-top: 1.5em; }
        form { margin-bottom: 1em; }
        label { display: block; margin: 0.3em 0; }
        input { width: 100%; padding: 0.3em; }
        button { margin-top: 0.5em; padding: 0.5em; }
    </style>
</head>
<body>
<div class="container">
    <h1>Fabflix Dashboard</h1>
    <p>
        Logged in as <b>${sessionScope.empEmail}</b>
        [<a href="${pageContext.request.contextPath}/logout">Logout</a>]
    </p>

    <c:if test="${not empty sessionScope.msg}">
        <p style="color:green"><b>${sessionScope.msg}</b></p>
        <c:remove var="msg" scope="session"/>
    </c:if>

    <!-- Add New Star -->
    <h3>Add New Star</h3>
    <form method="post" action="${pageContext.request.contextPath}/_dashboard">
        <input type="hidden" name="action" value="addStar"/>
        <label>Name: <input name="starName" required/></label>
        <label>Birth Year: <input name="birthYear" type="number"/></label>
        <button type="submit">Add Star</button>
    </form>

    <!-- Add New Movie -->
    <h3>Add New Movie</h3>
    <form method="post" action="${pageContext.request.contextPath}/_dashboard">
        <input type="hidden" name="action" value="addMovie"/>
        <label>Title: <input name="title" required/></label>
        <label>Year: <input name="year" type="number" required/></label>
        <label>Director: <input name="director" required/></label>
        <label>Star Name: <input name="star" required/></label>
        <label>Genre Name: <input name="genre" required/></label>
        <button type="submit">Add Movie</button>
    </form>

    <!-- Database Metadata -->
    <h3>Database Tables &amp; Columns</h3>
    <c:forEach var="entry" items="${schema}">
        <h4>Table: ${entry.key}</h4>
        <ul>
            <c:forEach var="col" items="${entry.value}">
                <li>${col}</li>
            </c:forEach>
        </ul>
    </c:forEach>
</div>
</body>
</html>
