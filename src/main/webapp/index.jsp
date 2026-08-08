<%--
  Entry point. Signed-in users go to the dashboard, everyone else to the login
  page. Nothing is rendered here, so the application has no public landing page
  that could leak information about the deployment.
--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:choose>
  <c:when test="${not empty sessionScope['bdcms.user']}">
    <c:redirect url="/app/dashboard" />
  </c:when>
  <c:otherwise>
    <c:redirect url="/login" />
  </c:otherwise>
</c:choose>
