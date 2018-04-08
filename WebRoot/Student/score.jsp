<%@ page language="java" import="java.util.*" pageEncoding="UTF-8"%>
<%@ taglib prefix="s" uri="/struts-tags" %>
<!DOCTYPE html>
<html>
	<head>
		<meta charset="utf-8">
		<title>学生成绩信息管理系统</title>
		<link href="../bootstrap/css/bootstrap.min.css" rel="stylesheet">
		<link href="../css/publicModify.css" rel="stylesheet"/>
		<style>
			th{
				text-align: center;
			}
			td{
				text-align: center;
			}
		</style>
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
        	<h2>查看成绩</h2>
        </section>
        
        <table class="table table-striped table-hover table-bordered">
        	<thead><th>科目</th><th>成绩</th><th>所修学年</th></thead>
        	<s:iterator value="stuScore">
        	 <s:if test="%{score < 60}">
        	  <tr class="danger"><td><s:property value="subject"/></td><td><s:property value="score"/></td><td><s:property value="grade"/></td></tr>
        	 </s:if>
        	 <s:else>
        	   <tr class="success"><td><s:property value="subject"/></td><td><s:property value="score"/></td><td><s:property value="grade"/></td></tr>
        	 </s:else>
        	</s:iterator>
        </table>
        
     </div>
    
	</body>
</html>
