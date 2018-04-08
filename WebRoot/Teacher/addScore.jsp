<%@page import="java.sql.ResultSet"%>
<%@page import="java.sql.PreparedStatement"%>
<%@ page language="java" import="java.util.*" pageEncoding="UTF-8"%>
<%@ taglib prefix="s" uri="/struts-tags" %>
 <jsp:useBean id="db" scope="page" class="org.myweb.dao.Dao"/>
<%
   int id = -1;
   ResultSet set = null;
   ResultSet set1 = null;
   if(request.getParameter("id") != null)
   {
      id = Integer.parseInt(request.getParameter("id").toString());
      String tid = session.getAttribute("id").toString();
      String sql = "SELECT `subject`.id , `subject`.`name` FROM `subject` WHERE `subject`.grade = (SELECT grade FROM student WHERE student.id = ?) AND `subject`.id NOT in (SELECT score.`subject` FROM score WHERE score.stu = ?) AND `subject`.teacher = ?";
      PreparedStatement state = db.getConn().prepareStatement(sql);
      state.setObject(1, id);
      state.setObject(2, id);
      state.setObject(3, tid);
      set = db.executeQuery(state);
      String sql1 = "select grade from student where id = ?";
      PreparedStatement state1 = db.getConn().prepareStatement(sql1);
      state1.setObject(1, id);
      set1 = db.executeQuery(state1);
      set1.next();
   }
 %>

<!--
	快速建站 灵感来自Bootstrap
	其实也没有什么设计。。。
-->
<!DOCTYPE html>
<html>
	<head>
		<meta charset="utf-8">
		<title>学生成绩信息管理系统</title>
		<link href="http://cdn.bootcss.com/bootstrap/3.3.0/css/bootstrap.min.css" rel="stylesheet">
		<link href="../css/publicModify.css" rel="stylesheet"/>
		<script src="../js/jquery-1.9.1.js"></script>
	</head>
	<body>
	 <nav class="navbar navbar-inverse navbar-fixed-top" role="navigation">
      <div class="container">
          <a class="navbar-brand" href="index.jsp">学生成绩信息管理系统</a>
          
          <div id="navbar" class="navbar-collapse collapse">
                 <ul class="nav navbar-nav pull-right">
                   <li><a href='/javaEE-final/logout'>退出登陆</a></li>
                </ul>
            </div>
          
      </div>
     </nav>
     
     <div class="container marginBar">
     	<section class="title">
        	<h2>录入成绩</h2>
        	<form class="form-inline" method="post" action="addScore">
        		
        	
        	<div class="form-item">
        	    <section class="inner-item">
        		  <label>学生ID：</label>
        		  <input type="text" class="form-control" name="stuId" id="stuId" value="<%=id%>" readonly="readonly"/>
        		</section>
        	</div>
        	
        	
        	<div class="form-item">
        	   	<section class="inner-item">
        		  <label>学生年级：</label>
        		  <input type="text" class="form-control" name="grade" value="大学<%=set1.getString(1)%>年级" readonly="readonly"/>
        		</section>
        	  </div>
        	  
        	  <div class="form-item">
        	   	<section class="inner-item">
        		  <label>课程：</label>
        		  <select id="subject" name="subject" class="form-control" style="width: 195px;">
        		  <%
        		     while(set.next())
        		     {
        		        out.println("<option value='"+set.getString(1)+"'>"+set.getString(2)+"</option>");
        		     }
        		   %>
        		  </select>
        		</section>
        	  </div>	
        	  
        	  <div class="form-item">
        	    <section class="inner-item">
        		  <label>成绩：</label>
        		  <input type="text" class="form-control" name="score" id="score" pattern="^[0-9]*$" title="成绩必须是数字"/>
        		</section>
        	</div>
        	
        		 <button class="btn btn-lg btn-primary btn-block" type="submit" style="width: 100px; margin: 0 auto;">提交</button>
        		 <s:if test="hasActionErrors()">
                <div class="errors">
                    <s:actionerror/>
                 </div>
             </s:if>
        	</form>
        </section>
      <s:fielderror/>
	</body>
</html>
