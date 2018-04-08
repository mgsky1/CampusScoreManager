<%@ page language="java" import="java.util.*" pageEncoding="UTF-8"%>
<%@ taglib prefix="s" uri="/struts-tags" %>

<!-- 快速建站 灵感来自Bootstrap哈哈哈-->
<!DOCTYPE html>
<html>
	<head>
		<meta charset="utf-8">
		<title>学生信息管理系统</title>
		<link href="bootstrap/css/bootstrap.min.css" rel="stylesheet">
        <link href="css/login.css" rel="stylesheet" />
        <s:head/>
        <style>
           .errorMessage{
               color: white;
               list-style: none;
               text-align: center;
           }
        </style>
	</head>
	<body>
	<div class="container">

      <form class="form-signin" action="login" method="post">
        <h2 class="form-signin-heading">用户登陆</h2>
        <input type="text" class="form-control" placeholder="姓名拼音缩写" name="userName" autofocus="autofocus"> 
        <br/>
        <input type="password" class="form-control" placeholder="密码" name="password">
        <input type="submit" value="登陆" class="btn btn-lg btn-primary btn-block">
      </form>
     <s:fielderror/>
    </div> 
	</body>
</html>
