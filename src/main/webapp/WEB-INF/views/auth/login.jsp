<%@ include file="../layout/taglibs.jspf" %>
<%--
  FR-1: staff sign-in.

  Split screen from the mockups: the dark half carries the product claim, the
  light half the form. Nothing competes with the two fields.

  This page does not use the shared header - it has no navigation bar, because
  there is nowhere to navigate to before signing in.
--%>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<fmt:message var="lblSignIn" key="login.title"/>
<fmt:message var="lblFillForm" key="login.fillForm"/>
<!DOCTYPE html>
<html lang="${lang}">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <meta name="robots" content="noindex, nofollow">
  <title>${lblSignIn} &middot; BDCMS</title>
  <link rel="stylesheet" href="${ctx}/assets/vendor/bootstrap/bootstrap.min.css">
  <link rel="stylesheet" href="${ctx}/assets/css/bdcms.css">
  <link rel="icon" href="${ctx}/assets/img/favicon.svg" type="image/svg+xml">
</head>
<body class="bdcms">

<div class="bdcms-login">

  <%-- The switcher is repeated here because this page does not use the shared
       shell, and an operator who cannot read the sign-in screen has no way to
       reach the switcher that lives behind it. --%>
  <div class="bdcms-langswitch bdcms-langswitch-float" role="group">
    <a class="${lang eq 'en' ? 'active' : ''}" hreflang="en"
       href="${fn:escapeXml(langSwitchBase)}lang=en">EN</a>
    <a class="${lang eq 'my' ? 'active' : ''}" hreflang="my"
       href="${fn:escapeXml(langSwitchBase)}lang=my">&#4121;&#4156;&#4116;&#4154;&#4121;&#4140;</a>
  </div>

  <div class="bdcms-login-brand">
    <div class="d-flex align-items-center gap-2">
      <span class="bdcms-drop" aria-hidden="true"></span>
      <span style="font:500 12px/1 var(--bd-mono);letter-spacing:.14em">BDCMS</span>
    </div>

    <div>
      <%-- The claim carries its own <br>, because the line breaks fall in
           different places in each language. fmt:message writes the bundle
           value through unescaped, which is what makes that work and is safe
           here: the string is ours, not a donor's. --%>
      <div class="bdcms-login-claim"><fmt:message key="login.claim"/></div>
      <div class="bdcms-login-claim-sub">
        <fmt:message key="login.claimSub"/>
      </div>
    </div>

    <div class="bdcms-login-foot">
      Blood Donation Card Management System<br>
      Section D &middot; Group 3 &middot; Jakarta EE on Tomcat 10.1
    </div>
  </div>

  <div class="bdcms-login-panel">
    <div class="bdcms-login-form">

      <div class="mb-4">
        <h1 style="font:600 26px/1.2 var(--bd-sans);letter-spacing:-.015em;margin:0">${lblSignIn}</h1>
        <p style="font:400 14px/1.5 var(--bd-sans);color:var(--bd-ink-55,rgba(23,21,15,.55));margin:6px 0 0">
          <fmt:message key="login.subtitle"/>
        </p>
      </div>

      <c:if test="${not empty errorMessage}">
        <div class="alert alert-danger py-2 px-3 mb-3"><c:out value="${errorMessage}" /></div>
      </c:if>
      <c:if test="${not empty infoMessage}">
        <div class="alert alert-info py-2 px-3 mb-3"><c:out value="${infoMessage}" /></div>
      </c:if>

      <form method="post" action="${ctx}/login" autocomplete="off" novalidate>
        <input type="hidden" name="_csrf" value="${csrfToken}">
        <c:if test="${not empty next}">
          <input type="hidden" name="next" value="${fn:escapeXml(next)}">
        </c:if>

        <div class="mb-3">
          <label for="username" class="form-label"><fmt:message key="login.username"/></label>
          <input type="text" class="form-control font-monospace" id="username" name="username"
                 value="${fn:escapeXml(username)}" required autofocus
                 maxlength="40" autocomplete="username">
        </div>

        <div class="mb-4">
          <label for="password" class="form-label"><fmt:message key="login.password"/></label>
          <input type="password" class="form-control" id="password" name="password"
                 required autocomplete="current-password">
        </div>

        <button type="submit" class="btn btn-danger w-100 btn-lg">${lblSignIn}</button>
      </form>

      <%-- Seeded demonstration accounts. Shown only when
           bdcms.demo.credentials is on, so a real deployment turns them off
           with one property rather than a code change. --%>
      <c:if test="${not empty demoAccounts}">
        <div class="bdcms-demo mt-4">
          <div class="d-flex align-items-baseline justify-content-between gap-2 mb-2">
            <span class="bd-eyebrow"><fmt:message key="login.demoAccounts"/></span>
            <span class="bdcms-demo-warn"><fmt:message key="login.demoWarn"/></span>
          </div>

          <div class="d-flex flex-column gap-1">
            <c:forEach var="a" items="${demoAccounts}">
              <button type="button" class="bdcms-demo-row"
                      data-username="${fn:escapeXml(a.username)}"
                      data-password="${fn:escapeXml(a.password)}"
                      title="${fn:escapeXml(lblFillForm)}">
                <span class="bdcms-demo-user"><c:out value="${a.username}" /></span>
                <span class="bdcms-demo-pass"><c:out value="${a.password}" /></span>
                <span class="badge ${a.admin ? 'text-bg-danger' : 'text-bg-secondary'}">
                  <c:out value="${a.role}" />
                </span>
              </button>
            </c:forEach>
          </div>

          <div class="bdcms-demo-hint">
            <fmt:message key="login.demoHint"/>
          </div>
        </div>
      </c:if>

      <div class="d-flex gap-2 align-items-start mt-4"
           style="padding:13px 15px;background:var(--bd-wash);border-radius:var(--bd-r)">
        <span class="bdcms-dot bdcms-dot-grey" style="width:5px;height:5px;margin-top:7px"></span>
        <div style="font:400 12.5px/1.5 var(--bd-sans);color:rgba(23,21,15,.55)">
          <fmt:message key="login.audit"/>
        </div>
      </div>

    </div>
  </div>
</div>

<script src="${ctx}/assets/vendor/bootstrap/bootstrap.bundle.min.js"></script>
<script>
  // Click a demonstration row to fill the form. Deliberately does not submit:
  // the assessor should see which account was chosen before signing in.
  document.querySelectorAll('.bdcms-demo-row').forEach(function (row) {
      row.addEventListener('click', function () {
          document.getElementById('username').value = row.dataset.username;
          document.getElementById('password').value = row.dataset.password;
          document.getElementById('password').focus();
      });
  });
</script>
</body>
</html>
