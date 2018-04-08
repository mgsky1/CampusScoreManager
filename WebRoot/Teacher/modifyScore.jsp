<%@ page import="org.myweb.teacher.GetScoreDetail"%>
<%@ page language="java" import="java.util.*" pageEncoding="UTF-8"%>
<%@ taglib prefix="s" uri="/struts-tags" %>
<%
  GetScoreDetail score  = new GetScoreDetail(Integer.parseInt(request.getParameter("id")));
  session.setAttribute("scoreId", request.getParameter("id").toString());
  if(score.getStuName() == null)
  {
    out.println("<script>alert('该课程已不是当前老师授课，不可修改成绩！');history.go(-1)</script>");
    return;
  }
 %>
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
        	<h2>修改成绩</h2>
        </section>
        
           <form class="form-inline" method="post" action="modifyScore">
         	 <div class="form-item">
        	    <section class="inner-item">
        		  <label>学生ID：</label>
        		  <input type="text" class="form-control" name="stuId" id="stuId" readonly="readonly" value="<%=score.getStuId()%>"/>
        		</section>
        	</div>
        	
         	 <div class="form-item">
        	    <section class="inner-item">
        		  <label>学生姓名：</label> 
        		  <input type="text" class="form-control" name="stuName" id="stuName" readonly="readonly" value="<%=score.getStuName()%>"/>
        		</section>
        	</div>
        	
        	<div class="form-item">
        	    <section class="inner-item">
        		  <label>年级：</label>
        		  <input type="text" class="form-control" name="grade" id="grade" readonly="readonly" value="<%=score.getGrade() %>"/>
        		</section>
        	</div>
        	
        	<div class="form-item">
        	    <section class="inner-item">
        		  <label>科目：</label>
        		  <input type="text" class="form-control" name="subject" id="subject" readonly="readonly" value="<%=score.getSubject()%>"/>
        		</section>
        	</div>
        	
        	<div class="form-item">
        	    <section class="inner-item">
        		  <label>成绩：</label>
        		  <input type="text" class="form-control" name="score" id="score" value="<%=score.getScore()%>" pattern="^[0-9]*$" title="成绩必须是数字"/>
        		</section>
        	</div>
        	<input type="hidden" name="pid" value="<%=request.getParameter("id").toString()%>">
        	 <button class="btn btn-lg btn-primary btn-block" type="submit" style="width: 100px; margin: 0 auto;">提交</button>
        	 <s:if test="hasActionErrors()">
                <div class="errors">
                    <s:actionerror/>
                 </div>
             </s:if>
         </form>
        <s:fielderror/>
	</body>
</html>
