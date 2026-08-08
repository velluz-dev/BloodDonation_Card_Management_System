<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Not permitted &middot; BDCMS</title>
  <link rel="stylesheet" href="${ctx}/assets/vendor/bootstrap/bootstrap.min.css">
  <link rel="stylesheet" href="${ctx}/assets/css/bdcms.css">
</head>
<body class="bdcms">
<div class="container py-5">
  <div class="row justify-content-center">
    <div class="col-12 col-md-7 text-center">
      <div class="display-1 fw-bold text-danger">403</div>
      <h1 class="h4 mb-3">You do not have permission for this page</h1>
      <p class="text-muted">
        This area is restricted to administrators. If you believe you should have
        access, ask an administrator to review your role.
      </p>
      <a href="${ctx}/app/dashboard" class="btn btn-danger">Back to dashboard</a>
    </div>
  </div>
</div>
</body>
</html>
