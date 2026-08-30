<%@ include file="../layout/taglibs.jspf" %>
<%--
  The configurable business rules.

  This screen is the maintainability requirement in practice: the minimum
  donation interval is edited here and takes effect on the next eligibility
  check, with no code change and no redeployment.

  The interval is given its own panel rather than sitting as one row in a
  table of key/value pairs, because it is the one setting that decides whether
  a donor is turned away.
--%>
<fmt:message var="pageTitle" key="settings.title" scope="request"/>
<c:set var="nav" value="admin" scope="request" />
<%@ include file="../layout/header.jspf" %>

<div class="mb-4">
  <h1 class="bd-page-title"><fmt:message key="settings.title"/></h1>
  <p class="bd-page-sub">
    <fmt:message key="settings.subtitle"/>
  </p>
</div>

<div class="row g-4">
  <div class="col-12 col-lg-7">

    <%-- ---- the interval, given its own panel ---- --%>
    <div class="bdcms-verdict bdcms-verdict-eligible mb-4">
      <div class="bdcms-verdict-body">
        <div class="bd-eyebrow"><fmt:message key="settings.minInterval"/></div>

        <div class="d-flex align-items-baseline gap-3 mt-2 mb-3">
          <span style="font:600 44px/1 var(--bd-sans);letter-spacing:-.02em">
            <c:out value="${settings[intervalKey]}" default="${defaultInterval}" />
          </span>
          <span style="font:400 17px/1 var(--bd-sans);color:var(--bd-ink-60)"><fmt:message key="settings.days"/></span>
        </div>

        <p style="font:400 13.5px/1.55 var(--bd-sans);color:var(--bd-ink-60);max-width:460px">
          <fmt:message key="settings.intervalBody"/>
          <fmt:message key="settings.defaultIs">
            <fmt:param value="${defaultInterval}"/>
          </fmt:message>
        </p>

        <form method="post" action="${ctx}/app/admin/settings" class="row g-2 align-items-end">
          <input type="hidden" name="_csrf" value="${csrfToken}">
          <input type="hidden" name="settingKey" value="${fn:escapeXml(intervalKey)}">
          <div class="col-auto">
            <label for="intervalValue" class="form-label"><fmt:message key="settings.newInterval"/></label>
            <input type="number" class="form-control font-monospace" id="intervalValue"
                   name="settingValue" value="${fn:escapeXml(settings[intervalKey])}"
                   min="1" max="365" required style="width:140px">
          </div>
          <div class="col-auto">
            <button type="submit" class="btn btn-danger"><fmt:message key="settings.updateInterval"/></button>
          </div>
        </form>
      </div>
    </div>

    <%-- ---- everything else ---- --%>
    <div class="card">
      <div class="card-header"><fmt:message key="settings.other"/></div>
      <div class="table-responsive">
        <table class="table mb-0">
          <thead class="table-light">
            <tr>
              <th><fmt:message key="settings.setting"/></th>
              <th><fmt:message key="settings.value"/></th>
              <th class="text-end"><fmt:message key="settings.change"/></th>
            </tr>
          </thead>
          <tbody>
            <c:forEach var="entry" items="${settings}">
              <c:if test="${entry.key ne intervalKey}">
                <tr>
                  <td class="font-monospace" style="font-size:12.5px">
                    <c:out value="${entry.key}" />
                  </td>
                  <td style="font-weight:500"><c:out value="${entry.value}" /></td>
                  <td>
                    <form method="post" action="${ctx}/app/admin/settings"
                          class="row g-1 justify-content-end">
                      <input type="hidden" name="_csrf" value="${csrfToken}">
                      <input type="hidden" name="settingKey" value="${fn:escapeXml(entry.key)}">
                      <div class="col-7">
                        <label class="visually-hidden" for="v-${entry.key}">
                          <fmt:message key="settings.valueFor">
                            <fmt:param value="${entry.key}"/>
                          </fmt:message>
                        </label>
                        <input type="text" class="form-control form-control-sm" id="v-${entry.key}"
                               name="settingValue" value="${fn:escapeXml(entry.value)}" required>
                      </div>
                      <div class="col-auto">
                        <button type="submit" class="btn btn-sm btn-quiet"><fmt:message key="common.save"/></button>
                      </div>
                    </form>
                  </td>
                </tr>
              </c:if>
            </c:forEach>
          </tbody>
        </table>
      </div>
    </div>
  </div>

  <div class="col-12 col-lg-5">
    <div class="card">
      <div class="card-header"><fmt:message key="settings.whyDatabase"/></div>
      <div class="card-body" style="font:400 13px/1.6 var(--bd-sans);color:var(--bd-ink-60)">
        <p>
          <fmt:message key="settings.whyBody1"/>
        </p>
        <p class="mb-0">
          <fmt:message key="settings.whyBody2"/>
        </p>
      </div>
    </div>

    <div class="alert alert-warning mt-3">
      <strong><fmt:message key="settings.careTitle"/></strong>
      <fmt:message key="settings.careBody"/>
    </div>
  </div>
</div>

<%@ include file="../layout/footer.jspf" %>
