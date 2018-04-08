<%@ page language="java" import="java.util.*" pageEncoding="UTF-8"%>
<%@ taglib prefix="s" uri="/struts-tags" %>
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
        		<h2>修改密码</h2>
        	</section>
        	
        	<form class="form-inline" method="post" action="ModifyPwd">
        	   <div class="form-item">
        	    <section class="inner-item">
        		  <label>原密码：</label>
        		  <input type="password" class="form-control" name="pwd_original" id="pwd_original" required="required" autofocus="autofocus"/>
        		</section>
        	   </div>
        	   
        		<div class="form-item">
        		  <section class="inner-item">
        		    <label>新密码：</label>
        		    <input type="password" class="form-control" name="pwd_new" id="pwd_new" required="required" />
        		  </section>
        	   </div>
        	   
        	   <div class="form-item">
        	   	<section class="inner-item">
        		  <label>确认密码：</label>
        		  <input type="password" class="form-control" name="pwd_confirm" id="pwd_confirm" required="required" />
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
        
	</body>
</html>
