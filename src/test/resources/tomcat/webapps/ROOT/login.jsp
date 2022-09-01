<%@ page contentType="text/html; charset=UTF-8" %>
<% response.setHeader("X-LoginPage", "true"); %>
<!DOCTYPE html>
<html lang="en">
	<head>
		<title>Login</title>
	</head>
	<body>
		<form action="/j_security_check" method="post">
			<label>
				Username:
				<input type="text" name="j_username" />
			</label>
			<label>
				Password:
				<input type="password" name="j_password" />
			</label>
			<input type="submit" value="Login" />
		</form>
	</body>
</html>
