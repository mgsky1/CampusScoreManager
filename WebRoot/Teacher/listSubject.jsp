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
        	<h2>当前所教课程</h2>
        </section>
 <button type="button" class="btn btn-success"  onclick="window.location.href='addSubject.jsp'">添加课程</button>
        <table class="table table-striped table-hover">
        	<thead><th>课程</th><th>年级</th><th>操作</th></thead>
        	<s:iterator value="subject">
        	   <tr>
        	     <td><s:property value="name"/></td>
        	     <td>大学<s:property value="grade"/>年级</td>
        	     <td>
        	     <button type="button" class="btn btn-sm btn-warning" onclick="window.location.href='modifySubject.jsp?id=<s:property value="id"/>'">修改</button>
        	       &nbsp;
        	        <button type="button" class="btn btn-sm btn-danger" onclick="confirm_delete('<s:property value="id"/>')">删除</button>       	        
        	     </td>
        	  </tr>
        	</s:iterator>
        </table>
        
     </div>
	</body>
	
		<script type="text/javascript">
	  function confirm_delete(cid)
	  {
	    if(confirm('确定删除该课程吗？'))
	    {
	        window.location.href='delSubject?id='+cid; 
	    }
	  }
	</script>
	
</html>
