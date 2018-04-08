<%@ page language="java" import="java.util.*" pageEncoding="UTF-8"%>
<%@ taglib prefix="s" uri="/struts-tags" %>
<jsp:useBean id="teacher" scope="session" class="org.myweb.vo.Teacher"/>
<!-- 快速建站 灵感来自Bootstrap-->
<!DOCTYPE html>
<html>
	<head>
		<meta charset="utf-8">
		<title>学生成绩信息管理系统</title>
		<link href="../../bootstrap/css/bootstrap.min.css" rel="stylesheet">
		<link href="../../css/publicModify.css" rel="stylesheet"/>
	</head>
	<body>
		<nav class="navbar navbar-inverse navbar-fixed-top" role="navigation">
              <div class="container">
                 <a class="navbar-brand" href="../index.jsp">学生成绩信息管理系统</a>
                 
                 <div id="navbar" class="navbar-collapse collapse">
                 <ul class="nav navbar-nav pull-right">
                   <li><a href='/javaEE-final/logout'>退出登陆</a></li>
                </ul>
            </div>
                 
              </div>
        </nav>
        
        <div class="container marginBar">
        	<section class="title">
        		<h2>修改个人信息</h2>
        	</section>
        	
        	<form class="form-inline" method="post" action="ModifyInfo">
        	   <div class="form-item">
        	    <section class="inner-item">
        		  <label>真实姓名：</label>
        		  <input type="text" readonly="readonly" class="form-control" name="realName" id="realName" value="<%=teacher.getName()%>"/>
        		</section>
        	   </div>
        	   
        		<div class="form-item">
        		  <section class="inner-item">
        		    <label>登陆名：</label>
        		    <input type="text" class="form-control" name="loginName" id="loginName" value="<%=session.getAttribute("loginName").toString()%>"/>
        		  </section>
        	   </div>
        	   
        	   
        	   <div class="form-item">
        		  <section class="inner-item">
        		    <label>联系电话：</label>
        		    <input type="text" class="form-control" name="tel" id="tel" value="<%=teacher.getTel()%>"/>
        		  </section>
        	   </div>
        	   <button class="btn btn-lg btn-primary btn-block" type="submit" style="width: 100px; margin: 0 auto;">提交</button>
        	
        	</form>
        	<s:fielderror/>
        </div>
        
	</body>
</html>
