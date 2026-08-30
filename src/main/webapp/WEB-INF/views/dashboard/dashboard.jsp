<%@ include file="../layout/taglibs.jspf" %>
<%--
  Shift dashboard.

  Four counters, then the scan panel as the loudest object on the page -
  starting a donation is what staff are here to do - then recent activity, with
  centre settings and today's blood-group mix on a rail.
--%>
<fmt:message var="pageTitle" key="nav.dashboard" scope="request"/>
<c:set var="nav" value="dashboard" scope="request" />
<c:set var="topTitle" value="${todayLong}" scope="request" />
<%-- Two placeholders rather than concatenation: the name and the centre fall
     on opposite sides of the verb in Burmese. --%>
<fmt:message var="topSub" key="dash.signedInAs" scope="request">
  <fmt:param value="${currentUser.fullName}"/>
  <fmt:param value="${centreName}"/>
</fmt:message>
<fmt:message var="lblOpenHistory" key="dash.openHistory"/>
<%@ include file="../layout/header.jspf" %>

<div class="row g-4">
  <div class="col-12 col-xxl-9">

    <%-- ---- counters ---- --%>
    <div class="row g-3 mb-4">
      <div class="col-6 col-lg-3">
        <div class="bdcms-stat">
          <div class="bdcms-stat-head">
            <span class="bdcms-stat-badge"><svg class="ic" aria-hidden="true"><use href="#i-users"/></svg></span>
            <span class="bdcms-stat-label"><fmt:message key="dash.registeredDonors"/></span>
          </div>
          <div class="bdcms-stat-value">${donorCount}</div>
          <div class="bdcms-stat-foot"><fmt:message key="dash.totalInSystem"/></div>
          <svg class="ic bdcms-stat-ghost" aria-hidden="true"><use href="#i-users"/></svg>
        </div>
      </div>

      <div class="col-6 col-lg-3">
        <div class="bdcms-stat">
          <div class="bdcms-stat-head">
            <span class="bdcms-stat-badge"><svg class="ic" aria-hidden="true"><use href="#i-card"/></svg></span>
            <span class="bdcms-stat-label"><fmt:message key="dash.activeCards"/></span>
          </div>
          <div class="bdcms-stat-value">${activeCardCount}</div>
          <div class="bdcms-stat-foot"><fmt:message key="dash.currentlyUsable"/></div>
          <svg class="ic bdcms-stat-ghost" aria-hidden="true"><use href="#i-card"/></svg>
        </div>
      </div>

      <div class="col-6 col-lg-3">
        <div class="bdcms-stat">
          <div class="bdcms-stat-head">
            <span class="bdcms-stat-badge"><svg class="ic" aria-hidden="true"><use href="#i-drop"/></svg></span>
            <span class="bdcms-stat-label"><fmt:message key="dash.donationsThisMonth"/></span>
          </div>
          <div class="bdcms-stat-value">${donationsThisMonth}</div>
          <div class="bdcms-stat-foot"><fmt:message key="dash.sinceFirst"/></div>
          <svg class="ic bdcms-stat-ghost" aria-hidden="true"><use href="#i-trend"/></svg>
        </div>
      </div>

      <div class="col-6 col-lg-3">
        <div class="bdcms-stat">
          <div class="bdcms-stat-head">
            <span class="bdcms-stat-badge"><svg class="ic" aria-hidden="true"><use href="#i-shield"/></svg></span>
            <span class="bdcms-stat-label"><fmt:message key="dash.verifications7"/></span>
          </div>
          <div class="bdcms-stat-value">${verificationsThisWeek}</div>
          <div class="bdcms-stat-foot">
            <c:choose>
              <c:when test="${failedVerifications > 0}">
                <span class="bdcms-stat-alert">
                  <fmt:message key="dash.failedCount">
                    <fmt:param value="${failedVerifications}"/>
                  </fmt:message>
                </span>
              </c:when>
              <c:otherwise><fmt:message key="dash.noneFailed"/></c:otherwise>
            </c:choose>
          </div>
          <svg class="ic bdcms-stat-ghost" aria-hidden="true"><use href="#i-shield"/></svg>
        </div>
      </div>
    </div>

    <%-- ---- the scan panel ---- --%>
    <form method="get" action="${ctx}/app/verify" class="bdcms-scanbar mb-4">
      <div class="bdcms-scanbar-head">
        <span class="bdcms-scanbar-icon"><svg class="ic ic-lg" aria-hidden="true"><use href="#i-scan"/></svg></span>
        <div>
          <div class="bdcms-scanbar-title"><fmt:message key="dash.startDonation"/></div>
          <div class="bdcms-scanbar-sub"><fmt:message key="dash.scanOrType"/></div>
        </div>
      </div>

      <div class="bdcms-scanfield">
        <label for="cardNo" class="visually-hidden"><fmt:message key="verify.cardNumber"/></label>
        <input type="text" class="form-control" id="cardNo" name="cardNo"
               placeholder="BDC-2026-XXXXXXXX" autocomplete="off" autofocus
               maxlength="32" spellcheck="false">
        <svg class="ic" aria-hidden="true"><use href="#i-search"/></svg>
      </div>

      <button type="submit" class="btn btn-on-crimson"><fmt:message key="dash.lookUp"/></button>
    </form>

    <%-- ---- recent activity ---- --%>
    <div class="bd-section-head">
      <h2 class="bd-section-title"><fmt:message key="dash.recentActivity"/></h2>
      <c:if test="${currentUser.admin}">
        <a class="bd-section-link" href="${ctx}/app/admin/verification-log">
          <fmt:message key="dash.viewAuditLog"/> <svg class="ic ic-sm" aria-hidden="true"><use href="#i-arrow-right"/></svg>
        </a>
      </c:if>
    </div>

    <div class="card mb-4">
      <div class="table-responsive">
        <table class="table table-hover mb-0">
          <thead class="table-light">
            <tr>
              <th><fmt:message key="dash.when"/></th>
              <th><fmt:message key="common.donor"/></th>
              <th><fmt:message key="common.card"/></th>
              <th><fmt:message key="dash.action"/></th>
              <th><fmt:message key="common.result"/></th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            <c:forEach var="d" items="${recentDonations}">
              <tr>
                <td style="white-space:nowrap">
                  <svg class="ic ic-sm me-1" aria-hidden="true" style="color:var(--bd-faint)"><use href="#i-clock"/></svg>
                  ${d.donationDateDisplay}
                </td>
                <td>
                  <a href="${ctx}/app/donations/history?donorId=${d.donorId}">
                    <c:out value="${d.donorName}" />
                  </a>
                </td>
                <td class="font-monospace" style="font-size:12.5px"><c:out value="${d.cardNo}" /></td>
                <td><fmt:message key="common.donation"/></td>
                <td><span class="badge text-bg-success"><fmt:message key="dash.recorded"/></span></td>
                <td class="text-end">
                  <a href="${ctx}/app/donations/history?donorId=${d.donorId}" aria-label="${fn:escapeXml(lblOpenHistory)}">
                    <svg class="ic bd-row-go" aria-hidden="true"><use href="#i-chev-right"/></svg>
                  </a>
                </td>
              </tr>
            </c:forEach>

            <c:forEach var="a" items="${recentAttempts}">
              <c:if test="${not a.result.success}">
                <tr>
                  <td style="white-space:nowrap">
                    <svg class="ic ic-sm me-1" aria-hidden="true" style="color:var(--bd-faint)"><use href="#i-clock"/></svg>
                    ${a.attemptedAtDisplay}
                  </td>
                  <td>
                    <c:choose>
                      <c:when test="${not empty a.donorId}">
                        <a href="${ctx}/app/donations/history?donorId=${a.donorId}">
                          <c:out value="${a.donorName}" />
                        </a>
                      </c:when>
                      <c:otherwise>
                        <span style="color:var(--bd-faint)"><fmt:message key="dash.unmatched"/></span>
                      </c:otherwise>
                    </c:choose>
                  </td>
                  <td class="font-monospace" style="font-size:12.5px"><c:out value="${a.cardNo}" /></td>
                  <td><fmt:message key="common.verification"/></td>
                  <td>
                    <span class="badge text-bg-${a.result.badgeClass}">
                      <fmt:message key="${a.result.messageKey}"/>
                    </span>
                  </td>
                  <td></td>
                </tr>
              </c:if>
            </c:forEach>

            <c:if test="${empty recentDonations and empty recentAttempts}">
              <tr>
                <td colspan="6" class="text-center py-5" style="color:var(--bd-muted)">
                  <fmt:message key="dash.nothingYet"/>
                </td>
              </tr>
            </c:if>
          </tbody>
        </table>
      </div>
    </div>
  </div>

  <%-- ---- right rail ---- --%>
  <div class="col-12 col-xxl-3">
    <div class="bdcms-rail mb-3">
      <div class="bdcms-rail-head">
        <svg class="ic" aria-hidden="true"><use href="#i-gear"/></svg> <fmt:message key="dash.centreSettings"/>
      </div>

      <div style="padding-bottom:16px;border-bottom:1px solid var(--bd-line-soft)">
        <div style="font:400 13px/1 var(--bd-sans);color:var(--bd-muted)">
          <fmt:message key="dash.minInterval"/>
        </div>
        <div class="bdcms-rail-big">
          <fmt:message key="dash.intervalDays"><fmt:param value="${intervalDays}"/></fmt:message>
        </div>
        <div style="font:400 12.5px/1.5 var(--bd-sans);color:var(--bd-muted)">
          <fmt:message key="dash.intervalNote"/>
        </div>
      </div>

      <div class="bd-eyebrow mt-3 mb-3"><fmt:message key="dash.donorsByGroup"/></div>
      <div class="d-flex flex-column gap-2">
        <c:forEach var="g" items="${bloodGroupMix}">
          <div class="bdcms-bar-row">
            <span class="bdcms-bar-label ${g.share < 25 ? 'bdcms-bar-label-muted' : ''}">
              <c:out value="${g.label}" />
            </span>
            <div class="bdcms-bar-track">
              <div class="bdcms-bar-fill ${g.share < 25 ? 'bdcms-bar-fill-3' : ''}"
                   style="width:${g.share}%"></div>
            </div>
            <span class="bdcms-bar-value">${g.count}</span>
          </div>
        </c:forEach>
        <c:if test="${empty bloodGroupMix}">
          <div style="font:400 12.5px/1.5 var(--bd-sans);color:var(--bd-muted)">
            <fmt:message key="dash.noDonors"/>
          </div>
        </c:if>
      </div>
    </div>

    <c:if test="${lostCardCount > 0}">
      <div class="card">
        <div class="card-body">
          <span class="bdcms-stat-badge mb-3"><svg class="ic" aria-hidden="true"><use href="#i-alert-file"/></svg></span>
          <div style="font:600 15px/1.3 var(--bd-sans);color:var(--bd-ink)">
            <%-- The English plural "s" used to be spliced in here with a
                 <c:if>. Burmese has no such inflection, so the whole phrase now
                 comes from the bundle and each language pluralises its own
                 way - or, in Burmese, not at all. --%>
            <fmt:message key="dash.cardsLost"><fmt:param value="${lostCardCount}"/></fmt:message>
          </div>
          <div style="font:400 13px/1.5 var(--bd-sans);color:var(--bd-muted);margin-top:7px">
            <fmt:message key="dash.cardsLostNote"/>
          </div>
          <a class="btn btn-sm btn-outline-danger mt-3" href="${ctx}/app/donors"><fmt:message key="dash.viewDetails"/></a>
        </div>
      </div>
    </c:if>
  </div>
</div>

<%@ include file="../layout/footer.jspf" %>
