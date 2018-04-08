<%@page import="java.sql.ResultSet"%>
<%@page import="java.sql.PreparedStatement"%>
<%@ page language="java" import="java.util.*" pageEncoding="UTF-8"%>
<%@ taglib prefix="s" uri="/struts-tags" %>
<jsp:useBean id="db" scope="page" class="org.myweb.dao.Dao"/>
<jsp:useBean id="subject" scope="page" class="org.myweb.vo.Subject"/>
<%
   if(request.getParameter("id") != null)
   {
      String id = request.getParameter("id");
      String tid = session.getAttribute("id").toString();
      String sql = "select id , name , grade from subject where id = ? and teacher = ?";
      PreparedStatement state = db.getConn().prepareStatement(sql);
      state.setObject(1, id);
      state.setObject(2, tid);
      ResultSet set = db.executeQuery(state);
      set.next();
      subject.setId(set.getInt(1));
      subject.setName(set.getString(2));
      session.setAttribute("subjectNow", set.getString(2));
      subject.setGrade(set.getInt(3));
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
        	<h2>修改课程</h2>
        </section>
        
        <form class="form-inline" method="post" action="modifySubject">
        	
        	<div class="form-item">
        	    <section class="inner-item">
        		  <label>课程名：</label>
        		  <input type="text" class="form-control" name="subjectName" id="subjectName" value="<%=subject.getName()%>"/>
        		</section>
        	</div>
        	
        	 <div class="form-item">
        	   	<section class="inner-item">
        		  <label>年级：</label>
        		  <select id="grade" name="grade" class="form-control" style="width: 195px;">
        		  	 <%
        		     switch(subject.getGrade())
        		     {
        		        case 1 : {out.println("<option value='1' selected>大一</option><option value='2'>大二</option><option value='3'>大三</option><option value='4'>大四</option>");break;}
        		        case 2 : {out.println("<option value='1'>大一</option><option value='2' selected>大二</option><option value='3'>大三</option><option value='4'>大四</option>");break;}
        		        case 3 : {out.println("<option value='1'>大一</option><option value='2'>大二</option><option value='3' selected>大三</option><option value='4'>大四</option>");break;}
        		        case 4 : {out.println("<option value='1'>大一</option><option value='2'>大二</option><option value='3'>大三</option><option value='4' selected>大四</option>");break;}
        		     }
        		   %>
        		  </select>
        		</section>
        	  </div>	
        	  <input type="hidden" value="<%=subject.getId()%>" name="id" id="id"/>
        	 <button class="btn btn-lg btn-primary btn-block" type="submit" style="width: 100px; margin: 0 auto;">提交</button>
        	  <s:if test="hasActionErrors()">
                <div class="errors">
                    <s:actionerror/>
                 </div>
             </s:if>
        </form>
        <s:fielderror/>
     </div>
     <script src="../js/jquery-1.9.1.js"></script>
	<script src="../bootstrap/js/bootstrap.min.js"></script>
	</body>
</html>
