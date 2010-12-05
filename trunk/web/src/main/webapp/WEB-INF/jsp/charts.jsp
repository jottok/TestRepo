<%--
 * Licensed under the GPL License.  You may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * THIS PACKAGE IS PROVIDED "AS IS" AND WITHOUT ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTIES OF
 * MERCHANTIBILITY AND FITNESS FOR A PARTICULAR PURPOSE.
--%>

<%@ page contentType="text/html;charset=UTF-8" language="java" session="false" %>
<%@ taglib uri='http://java.sun.com/jsp/jstl/core' prefix='c' %>
<%@ taglib uri='http://java.sun.com/jsp/jstl/fmt' prefix='fmt' %>
<%@ taglib uri='http://java.sun.com/jsp/jstl/functions' prefix='fn' %>
<%@ taglib uri="http://displaytag.sf.net" prefix="display" %>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring" %>
<%@ taglib uri="/WEB-INF/tags/probe.tld" prefix="probe" %>

<html>

	<head>
		<title><spring:message code="probe.jsp.title.charts"/></title>
		<link type="text/css" rel="stylesheet" href="${pageContext.request.contextPath}<spring:theme code='status.css'/>"/>
		<script type="text/javascript" language="javascript" src="<c:url value='/js/prototype.js'/>"></script>
		<script type="text/javascript" language="javascript" src="<c:url value='/js/scriptaculous.js'/>"></script>
		<script type="text/javascript" language="javascript" src="<c:url value='/js/Tooltip.js'/>"></script>
		<script type="text/javascript" language="javascript" src="<c:url value='/js/behaviour.js'/>"></script>
		<script type="text/javascript" language="javascript" src="<c:url value='/js/func.js'/>"></script>
	</head>

	<c:set var="chartWidth" value="400"/>
	<c:set var="chartHeight" value="250"/>

	<c:url value="/chart.png" var="reqimg" scope="page">
		<c:param name="p" value="connector"/>
		<c:param name="xz" value="${chartWidth}"/>
		<c:param name="yz" value="${chartHeight}"/>
		<c:param name="l" value="false"/>
	</c:url>

	<c:url value="/chart.png" var="traffimg" scope="page">
		<c:param name="p" value="traffic"/>
		<c:param name="xz" value="${chartWidth}"/>
		<c:param name="yz" value="${chartHeight}"/>
		<c:param name="xl" value="Bytes"/>
		<c:param name="s1c" value="#95FE8B"/>
		<c:param name="s1o" value="#009406"/>
		<c:param name="s2c" value="#FDFB8B"/>
		<c:param name="s2o" value="#D9CB00"/>
		<c:param name="l" value="false"/>
	</c:url>

	<c:set var="navTabConnectors" value="active" scope="request"/>

	<body>

		<c:forEach items="${pools}" var="pool" varStatus="poolStatus">

			<%--
				create "reset connector" url
			--%>
			<c:url value="/app/connectorReset.htm" var="reset_url">
				<c:param name="cn" value="${pool.name}"/>
			</c:url>

			<%--
				create  "remember group visibility" url
			--%>
			<c:url value="/remember.ajax" var="remember_url">
				<c:param name="cn" value="${pool.name}"/>
			</c:url>

			<%--
				create style of the div based on user cookies
			--%>
			<c:choose>
				<c:when test="${cookie[pool.name].value == 'off'}">
					<c:set var="style_collapse" value="display:none"/>
					<c:set var="style_expand" value=""/>
				</c:when>
				<c:otherwise>
					<c:set var="style_collapse" value=""/>
					<c:set var="style_expand" value="display:none"/>
				</c:otherwise>
			</c:choose>

			<div class="connectorChartHeader">
				<span class="headerTitle" onclick="togglePanel('poolInfo-${pool.name}', '${remember_url}')">
					<img class="lnk" src="${pageContext.request.contextPath}<spring:theme code='section.collapse.img'/>" alt="collapse" id="visible_poolInfo-${pool.name}" style="${style_collapse}"/>
					<img class="lnk" src="${pageContext.request.contextPath}<spring:theme code='section.expand.img'/>" alt="expand" id="invisible_poolInfo-${pool.name}" style="${style_expand}"/>
					${pool.name}
				</span>
				<span class="actions">
					<a href="${reset_url}">
						<img border="0" src="${pageContext.request.contextPath}<spring:theme code='reset.gif'/>" alt="reset"/>
					</a>
				</span>
			</div>
	
			<div class="poolInfo" id="poolInfo-${pool.name}" style="${style_collapse}">

				<div class="poolCharts">

					<div class="chartContainer">
						<dl>
							<dt><spring:message code="probe.jsp.charts.requests.title"/></dt>
							<dd class="image">
								<a href="<c:url value='/zoomchart.htm'/>?sp=${pool.name}&p=connector"><img
										id="req-${pool.name}"
										border="0" src="${reqimg}&sp=${pool.name}"
										width="${chartWidth}"
										height="${chartHeight}"
										alt="+"/></a>
							</dd>
							<dd id="dd-req-${pool.name}">
								<div class="ajax_activity"/>
							</dd>
						</dl>
					</div>

					<script type="text/javascript">
						new Ajax.ImgUpdater('req-${pool.name}', ${probe:max(collectionPeriod, 5)});
						new Ajax.PeriodicalUpdater('dd-req-${pool.name}', '<c:url value="/cnreqdetails.ajax"/>?cn=${pool.name}', {frequency: 3});
					</script>

					<div class="chartContainer">
						<dl>
							<dt><spring:message code="probe.jsp.charts.traffic.title"/></dt>
							<dd class="image">
								<a href="<c:url value='/zoomchart.htm'/>?sp=${pool.name}&p=traffic"><img
										id="traf-${pool.name}"
										border="0" src="${traffimg}&sp=${pool.name}"
										width="${chartWidth}"
										height="${chartHeight}"
										alt="+"/></a>
							</dd>
							<dd id="dd-traf-${pool.name}">
								<div class="ajax_activity"/>
							</dd>
						</dl>
					</div>

					<script type="text/javascript">
						new Ajax.ImgUpdater('traf-${pool.name}', ${probe:max(collectionPeriod, 5)});
						new Ajax.PeriodicalUpdater('dd-traf-${pool.name}', '<c:url value="/cntrafdetails.ajax"/>?cn=${pool.name}', {frequency: 3});
					</script>

				</div>

				<div class="threadInfo">
					<span class="name"><spring:message code="probe.jsp.status.currentThreadCount"/></span>&nbsp;${pool.currentThreadCount}
					&nbsp;
					<span class="name"><spring:message code="probe.jsp.status.currentThreadsBusy"/></span>&nbsp;${pool.currentThreadsBusy}
					&nbsp;
					<span class="name"><spring:message code="probe.jsp.status.maxThreads"/></span>&nbsp;${pool.maxThreads}
					&nbsp;
					<span class="name"><spring:message code="probe.jsp.status.maxSpareThreads"/></span>&nbsp;${pool.maxSpareThreads}
					&nbsp;
					<span class="name"><spring:message code="probe.jsp.status.minSpareThreads"/></span>&nbsp;${pool.minSpareThreads}
				</div>
				<div class="processorInfo">
					<span class="name"><spring:message code="probe.jsp.status.processor.maxTime"/></span>&nbsp;${pool.maxTime}
					&nbsp;
					<span class="name"><spring:message code="probe.jsp.status.processor.processingTime"/></span>&nbsp;${pool.processingTime}
				</div>

				<div class="workerInfo">
					<c:choose>
						<c:when test="${! empty pool.requestProcessors}">
							<display:table name="${pool.requestProcessors}"
									class="genericTbl" cellspacing="0"
									requestURI="" id="rp" defaultsort="7" defaultorder="descending">
								<display:column title="&nbsp;" width="18px" class="leftmost">
									<c:choose>
										<c:when test="${! empty rp.remoteAddrLocale.country && rp.remoteAddrLocale.country != '**'}">
											<img border="0" src="<c:url value='/flags/${fn:toLowerCase(rp.remoteAddrLocale.country)}.gif'/>"
													alt="${rp.remoteAddrLocale.country}" title="${rp.remoteAddrLocale.displayCountry}"/>
										</c:when>
										<c:otherwise>
											&nbsp;
										</c:otherwise>
									</c:choose>
								</display:column>

								<display:column width="90px"
										nowrap="" sortable="true" titleKey="probe.jsp.status.wrk.col.remoteAddr">
									<a id="ip_${pool.name}_${rp_rowNum}" href="#">${rp.remoteAddr}</a>

									<script type="text/javascript">
										addAjaxTooltip('ip_${pool.name}_${rp_rowNum}',
										'ttdiv', '<c:url value="/whois.ajax?ip=${rp.remoteAddr}"/>');
									</script>

								</display:column>

								<display:column width="60px" sortable="true" sortProperty="stage"
										titleKey="probe.jsp.status.wrk.col.stage">
									<c:choose>
										<c:when test="${rp.stage == 1}">
											<spring:message code="probe.jsp.status.wrk.stage.parse"/>
										</c:when>
										<c:when test="${rp.stage == 2}">
											<spring:message code="probe.jsp.status.wrk.stage.prepare"/>
										</c:when>
										<c:when test="${rp.stage == 3}">
											<spring:message code="probe.jsp.status.wrk.stage.service"/>
										</c:when>
										<c:when test="${rp.stage == 4}">
											<spring:message code="probe.jsp.status.wrk.stage.endInput"/>
										</c:when>
										<c:when test="${rp.stage == 5}">
											<spring:message code="probe.jsp.status.wrk.stage.endOutput"/>
										</c:when>
										<c:when test="${rp.stage == 6}">
											<spring:message code="probe.jsp.status.wrk.stage.keepAlive"/>
										</c:when>
										<c:when test="${rp.stage == 7}">
											<spring:message code="probe.jsp.status.wrk.stage.ended"/>
										</c:when>
										<c:when test="${rp.stage == 0}">
											<spring:message code="probe.jsp.status.wrk.stage.new"/>
										</c:when>
										<c:otherwise>?</c:otherwise>
									</c:choose>
								</display:column>
								<display:column width="80px" sortable="true" sortProperty="processingTime"
										titleKey="probe.jsp.status.wrk.col.processingTime">
									<probe:duration value="${rp.processingTime}"/>
								</display:column>
								<display:column width="60px" sortable="true" sortProperty="bytesReceived"
										titleKey="probe.jsp.status.wrk.col.in">
									<probe:volume value="${rp.bytesReceived}"/>
								</display:column>
								<display:column width="60px" sortable="true" sortProperty="bytesSent"
										titleKey="probe.jsp.status.wrk.col.out">
									<probe:volume value="${rp.bytesSent}"/>
								</display:column>

								<c:if test="${workerThreadNameSupported}">
									<display:column width="130px" sortable="true" titleKey="probe.jsp.status.wrk.col.thread">
										<c:choose>
											<c:when test="${! empty rp.workerThreadName}">
												<a id="thr${rp.workerThreadName}">
													${rp.workerThreadName}
												</a>
												<script type="text/javascript">
													addAjaxTooltip('thr${rp.workerThreadName}', 'ttdiv', '<c:url value="/app/threadstack.ajax"/>?name=${rp.workerThreadName}');
												</script>
											</c:when>
											<c:otherwise>
												&nbsp;
											</c:otherwise>
										</c:choose>
									</display:column>
								</c:if>

								<display:column sortable="true" titleKey="probe.jsp.status.wrk.col.url" >
									<c:choose>
										<c:when test="${rp.stage == 3 && ! empty rp.currentUri}">
											${rp.method}&nbsp;${rp.currentUri}<c:if test="${! empty rp.currentQueryString}">?${rp.currentQueryString}</c:if>
										</c:when>
										<c:otherwise>
											&nbsp;
										</c:otherwise>
									</c:choose>
								</display:column>
							</display:table>
						</c:when>
						<c:otherwise>
							<spring:message code="probe.jsp.status.wrk.empty"/>
						</c:otherwise>
					</c:choose>
				</div>
			</div>
		</c:forEach>

		<div id="ttdiv" class="tooltip" style="display: none;">
			<div class="tt_top">
				<span id="tt_title" style="display: none;"></span>
				<a id="ttdiv_close" href="#"><spring:message code="probe.jsp.tooltip.close"/></a>
			</div>
			<div class="tt_content" id="tt_content"></div>
		</div>

		<script type="text/javascript">
			var rules = {
				'#ttdiv_close': function(e) {
					e.onclick = function(e) {
						Effect.Fade('ttdiv');
						return false
					}
				}
			}
			Behaviour.register(rules);
		</script>

	</body>
</html>