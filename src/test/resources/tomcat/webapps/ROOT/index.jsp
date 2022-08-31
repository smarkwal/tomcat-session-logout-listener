<%@ page contentType="text/plain; charset=UTF-8" %>
<%@ page import="java.security.Principal"%>
<%
	if (session != null) {
		session.setAttribute("time", System.currentTimeMillis());
		out.println("session.id=" + session.getId());
	}
	Principal principal = request.getUserPrincipal();
	if (principal != null) {
		out.println("principal.name=" + principal.getName());
	}
%>