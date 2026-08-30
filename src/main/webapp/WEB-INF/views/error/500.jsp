<%--
  Generic failure page.

  Deliberately shows no stack trace and no exception message: an internal error
  can carry SQL fragments or file paths, and this system holds identity data.
  The detail goes to the server log, where only an administrator sees it.
--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<!DOCTYPE html>
<html lang="${lang}">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title><fmt:message key="error.500.title"/> &middot; BDCMS</title>
  <link rel="stylesheet" href="${ctx}/assets/vendor/bootstrap/bootstrap.min.css">
  <link rel="stylesheet" href="${ctx}/assets/css/bdcms.css">
</head>
<body class="bdcms">
<div class="container py-5">
  <div class="row justify-content-center">
    <div class="col-12 col-md-7 text-center">
      <div style="font:600 84px/1 var(--bd-mono);letter-spacing:-.03em;color:var(--bd-crimson)">500</div>
      <h1 style="font:600 22px/1.3 var(--bd-sans);letter-spacing:-.015em;margin:14px 0 12px"><fmt:message key="error.500.title"/></h1>
      <p style="font:400 14px/1.6 var(--bd-sans);color:var(--bd-ink-60)">
        <fmt:message key="error.500.body"/>
      </p>
      <p style="font:400 12.5px/1.6 var(--bd-sans);color:var(--bd-ink-50)">
        <fmt:message key="error.500.detail"/>
      </p>
      <a href="${ctx}/app/dashboard" class="btn btn-danger"><fmt:message key="error.backToDashboard"/></a>
    </div>
  </div>
</div>
</body>
</html>
