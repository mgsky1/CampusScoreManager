<%@page import="java.sql.*"%>
<%@ page language="java" import="java.util.*" pageEncoding="UTF-8"%>
<%
String path = request.getContextPath();
String basePath = request.getScheme()+"://"+request.getServerName()+":"+request.getServerPort()+path+"/";
%>

<jsp:useBean id="db" scope="page" class="org.myweb.dao.Dao"/>
<jsp:useBean id="teacher" scope="session" class="org.myweb.vo.Teacher"/>
<%
     /*使用JavaBean构造老师对象，其中实例teacher的作用范围为session，实例db的作用范围为当前页面*/
     PreparedStatement stat = db.getConn().prepareStatement("select * from teacher where id = ?");
     stat.setObject(1, session.getAttribute("id"));
     ResultSet rs = db.executeQuery(stat);
     while(rs.next())
     {
        teacher.setId(rs.getString(1));
        teacher.setTel(rs.getString(2));
        teacher.setName(session.getAttribute("user").toString());
     }
 %>
<!-- 快速建站 灵感来自Bootstrap哈哈哈-->
<!DOCTYPE html>
<html>
	<head>
		<meta charset="utf-8">
		<title>学生成绩信息管理系统</title>
		<link href="../bootstrap/css/bootstrap.min.css" rel="stylesheet">
	</head>
	<body>
		<nav class="navbar navbar-inverse navbar-fixed-top" role="navigation">
      <div class="container">
          <a class="navbar-brand" href="#">学生成绩信息管理系统</a>
          
          <div id="navbar" class="navbar-collapse collapse">
                 <ul class="nav navbar-nav pull-right">
                   <li><a href='/javaEE-final/logout'>退出登陆</a></li>
                </ul>
            </div>
          
      </div>
    </nav>
     <div class="container theme-showcase" role="main">

      <!-- Main jumbotron for a primary marketing message or call to action -->
      <div class="jumbotron" style="background-image: url('../images/teacher_bg.png');color:white">
        <h1>欢迎, ${teacher.name}老师!</h1>
        <p>学校:三明学院信息工程学院</p>
      </div>
      <div class="page-header">
        <h1>操作</h1>
      </div>
      
      <p>
        <button type="button" class="btn btn-lg btn-danger"  onclick="window.location.href='Account/modifyPersonalInfo.jsp'">修改个人信息</button>
        <button type="button" class="btn btn-lg btn-primary"  onclick="window.location.href='Account/modifyPassword.jsp'">修改密码</button>
        <button type="button" class="btn btn-lg btn-success" onclick="window.location.href='listStuModify'">成绩查看与修改</button>
        <button type="button" class="btn btn-lg btn-info" onclick="window.location.href='listStu'">录入成绩</button>
        <button type="button" class="btn btn-lg btn-warning" onclick="window.location.href='listSubject'">所教科目管理</button>
        <button type="button" class="btn btn-lg btn-default" onclick="window.location.href='stuManagement'">学生管理</button>
      </p>
      
     </div>
	</body>
</html>
