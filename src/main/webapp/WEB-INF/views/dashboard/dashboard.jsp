<%@ include file="../layout/taglibs.jspf" %>
<%--
  Shift dashboard.

  Layout follows the mockup: four counters, then the scan bar as the loudest
  object on the page - starting a donation is what staff are here to do - then
  recent activity, with centre settings and today's blood-group mix on a rail.
--%>
<c:set var="pageTitle" value="Dashboard" scope="request" />
<c:set var="nav" value="dashboard" scope="request" />
<%@ include file="../layout/header.jspf" %>

<div class="row g-4">
  <div class="col-12 col-xl-9">

    <div class="d-flex flex-wrap align-items-end justify-content-between gap-2 mb-4">
      <div>
        <h1 class="bd-page-title"><c:out value="${todayLong}" /></h1>
        <p class="bd-page-sub">
          Signed in as <c:out value="${currentUser.fullName}" /> &middot;
          <c:out value="${centreName}" />
        </p>
      </div>
      <div class="bd-eyebrow">SESSION ENDS AFTER 20 MIN IDLE</div>
    </div>

    <%-- ---- counters ---- --%>
    <div class="row g-3 mb-4">
      <div class="col-6 col-lg-3">
        <div class="bdcms-stat">
          <div class="bdcms-stat-label">Registered donors</div>
          <div class="bdcms-stat-value">${donorCount}</div>
        </div>
      </div>
      <div class="col-6 col-lg-3">
        <div class="bdcms-stat">
          <div class="bdcms-stat-label">Active cards</div>
          <div class="bdcms-stat-value">${activeCardCount}</div>
        </div>
      </div>
      <div class="col-6 col-lg-3">
        <div class="bdcms-stat">
          <div class="bdcms-stat-label">Donations this month</div>
          <div class="bdcms-stat-value">${donationsThisMonth}</div>
        </div>
      </div>
      <div class="col-6 col-lg-3">
        <div class="bdcms-stat">
          <div class="bdcms-stat-label">Verifications (7 days)</div>
          <div class="d-flex align-items-baseline gap-2">
            <div class="bdcms-stat-value">${verificationsThisWeek}</div>
            <c:if test="${failedVerifications > 0}">
              <div class="bdcms-stat-note bdcms-stat-note-alert">${failedVerifications} failed</div>
            </c:if>
          </div>
        </div>
      </div>
    </div>

    <%-- ---- the scan bar ---- --%>
    <form method="get" action="${ctx}/app/verify" class="bdcms-scanbar mb-4">
      <div style="flex:none">
        <div class="bdcms-scanbar-title">Start a donation</div>
        <div class="bdcms-scanbar-sub">Scan the card QR or type the number</div>
      </div>
      <div style="flex:1;min-width:220px">
        <label for="cardNo" class="visually-hidden">Card number</label>
        <input type="text" class="form-control" id="cardNo" name="cardNo"
               placeholder="BDC-2026-XXXXXXXX" autocomplete="off" autofocus
               maxlength="32" spellcheck="false">
      </div>
      <button type="submit" class="btn btn-danger" style="flex:none;height:48px">Look up donor</button>
    </form>

    <%-- ---- recent activity ---- --%>
    <div class="d-flex align-items-baseline justify-content-between mb-2">
      <div style="font:600 14px/1 var(--bd-sans)">Recent activity</div>
      <c:if test="${currentUser.admin}">
        <a class="small" href="${ctx}/app/admin/verification-log">View audit log</a>
      </c:if>
    </div>

    <div class="card mb-4">
      <div class="table-responsive">
        <table class="table table-hover mb-0">
          <thead class="table-light">
            <tr><th>When</th><th>Donor</th><th>Card</th><th>Action</th><th>Result</th></tr>
          </thead>
          <tbody>
            <c:forEach var="d" items="${recentDonations}">
              <tr>
                <td class="font-monospace" style="color:var(--bd-ink-60);white-space:nowrap">
                  ${d.donationDateDisplay}
                </td>
                <td>
                  <a href="${ctx}/app/donations/history?donorId=${d.donorId}">
                    <c:out value="${d.donorName}" />
                  </a>
                </td>
                <td class="font-monospace" style="font-size:12.5px"><c:out value="${d.cardNo}" /></td>
                <td style="color:var(--bd-ink-60)">Donation</td>
                <td><span class="badge text-bg-success">Recorded</span></td>
              </tr>
            </c:forEach>
            <c:forEach var="a" items="${recentAttempts}">
              <c:if test="${not a.result.success}">
                <tr>
                  <td class="font-monospace" style="color:var(--bd-ink-60);white-space:nowrap">
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
                        <span style="color:var(--bd-ink-50)">&mdash; unmatched &mdash;</span>
                      </c:otherwise>
                    </c:choose>
                  </td>
                  <td class="font-monospace" style="font-size:12.5px"><c:out value="${a.cardNo}" /></td>
                  <td style="color:var(--bd-ink-60)">Verification</td>
                  <td>
                    <span class="badge text-bg-${a.result.badgeClass}">
                      <c:out value="${a.result.label}" />
                    </span>
                  </td>
                </tr>
              </c:if>
            </c:forEach>
            <c:if test="${empty recentDonations and empty recentAttempts}">
              <tr>
                <td colspan="5" class="text-center py-5" style="color:var(--bd-ink-50)">
                  Nothing recorded yet. Scan a card above to begin.
                </td>
              </tr>
            </c:if>
          </tbody>
        </table>
      </div>
    </div>
  </div>

  <%-- ---- side rail ---- --%>
  <div class="col-12 col-xl-3">
    <div class="bdcms-rail mb-3">
      <div class="bd-eyebrow mb-3">Centre settings</div>

      <div style="padding-bottom:16px;border-bottom:1px solid rgba(23,21,15,.1)">
        <div style="font:400 13px/1 var(--bd-sans);color:rgba(23,21,15,.6)">
          Minimum donation interval
        </div>
        <div style="font:600 22px/1 var(--bd-sans);margin:6px 0">${intervalDays} days</div>
        <div style="font:400 12px/1.4 var(--bd-sans);color:var(--bd-ink-50)">
          Configurable per centre &mdash; held in settings, not in code
        </div>
      </div>

      <div class="bd-eyebrow mt-3 mb-3">Donors by blood group</div>
      <div class="d-flex flex-column gap-2">
        <c:forEach var="g" items="${bloodGroupMix}">
          <div class="bdcms-bar-row">
            <span class="bdcms-bar-label ${g.share < 15 ? 'bdcms-bar-label-muted' : ''}">
              <c:out value="${g.label}" />
            </span>
            <div class="bdcms-bar-track">
              <div class="bdcms-bar-fill ${g.share < 15 ? 'bdcms-bar-fill-muted' : ''}"
                   style="width:${g.share}%"></div>
            </div>
            <span class="bdcms-bar-value">${g.count}</span>
          </div>
        </c:forEach>
        <c:if test="${empty bloodGroupMix}">
          <div style="font:400 12.5px/1.5 var(--bd-sans);color:var(--bd-ink-50)">
            No donors registered yet.
          </div>
        </c:if>
      </div>
    </div>

    <c:if test="${lostCardCount > 0}">
      <div class="card">
        <div class="card-body">
          <div style="font:600 13px/1.3 var(--bd-sans)">
            ${lostCardCount} card<c:if test="${lostCardCount != 1}">s</c:if> reported lost
          </div>
          <div style="font:400 12.5px/1.45 var(--bd-sans);color:rgba(23,21,15,.55);margin-top:6px">
            Set to INACTIVE. Donation history stays on the donor record.
          </div>
        </div>
      </div>
    </c:if>
  </div>
</div>

<%@ include file="../layout/footer.jspf" %>
