<%@ page language="java" import="java.util.*" pageEncoding="UTF-8"%>
<%@ taglib prefix="s" uri="/struts-tags" %>
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
        	<h2>添加课程</h2>
        </section>
        
        <form class="form-inline" method="post" action="addSubject">
        	
        	<div class="form-item">
        	    <section class="inner-item">
        		  <label>课程名：</label>
        		  <input type="text" class="form-control" name="subjectName" id="subjectName"/>
        		</section>
        	</div>
        	
        	 <div class="form-item">
        	   	<section class="inner-item">
        		  <label>年级：</label>
        		  <select id="grade" name="grade" class="form-control" style="width: 195px;">
        		  	<option value="1" selected>大一</option>
        		  	<option value="2">大二</option>
        		  	<option value="3">大三</option>
        		  	<option value="4">大四</option>
        		  </select>
        		</section>
        	  </div>	
        	  
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
