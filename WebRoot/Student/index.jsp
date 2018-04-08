<%@page import="java.sql.ResultSet"%>
<%@page import="java.sql.PreparedStatement"%>
<%@ page language="java" import="java.util.*" pageEncoding="UTF-8"%>
<%
String path = request.getContextPath();
String basePath = request.getScheme()+"://"+request.getServerName()+":"+request.getServerPort()+path+"/";
%>
<jsp:useBean id="stu" scope="session" class="org.myweb.vo.Student"/>
<jsp:useBean id="db" scope="page" class="org.myweb.dao.Dao"/>
<%
    /*使用JavaBeen构造学生对象，实例stu的作用范围为session，db的作用范围为当前页面*/
    PreparedStatement stat = db.getConn().prepareStatement("select * from student where id = ?");
    stat.setObject(1, session.getAttribute("id"));
    ResultSet rs = db.executeQuery(stat);
    while(rs.next())
    {
       stu.setId(rs.getString(1));
       stu.setGrade(rs.getInt(4));
       stu.setTel(rs.getString(2));
       stu.setAddress(rs.getString(3));
       stu.setName(session.getAttribute("user").toString());
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
      <div class="jumbotron" style="background-image: url('../images/stu_bg.png');color:white">
        <h1>欢迎, ${stu.name}同学!</h1>
        <p>学校:三明学院信息工程学院 专业:计算机科学与技术  年级:<%
           if(stu.getGrade() == 1)
           {
              out.print("大一");
           }
           else if(stu.getGrade() == 2)
           {
              out.print("大二");
           }
           else if(stu.getGrade() == 3)
           {
              out.print("大三");
           }
           else
           {
              out.print("大四");
           }
         %> 学生ID:${stu.id} </p>
      </div>
      <div class="page-header">
        <h1>操作</h1>
      </div>
      
      <p>
        <button type="button" class="btn btn-lg btn-danger"  onclick="window.location.href='Account/modifyPersonalInfo.jsp'">修改个人信息</button>
        <button type="button" class="btn btn-lg btn-primary" onclick="window.location.href='Account/modifyPassword.jsp'">修改密码</button>
        <button type="button" class="btn btn-lg btn-success" onclick="window.location.href='getScore'">查看成绩</button>
        <!--<button type="button" class="btn btn-lg btn-info">Info</button>
        <button type="button" class="btn btn-lg btn-warning">Warning</button>
        <button type="button" class="btn btn-lg btn-danger">Danger</button>-->
      </p>
      
     </div>
	</body>
</html>
