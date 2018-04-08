<%@page import="java.sql.ResultSet"%>
<%@page import="java.sql.PreparedStatement"%>
<%@ page language="java" import="java.util.*" pageEncoding="UTF-8"%>
<%@ taglib prefix="s" uri="/struts-tags" %>
<jsp:useBean id="db" scope="page" class="org.myweb.dao.Dao"/>
<jsp:useBean id="stu" scope="session" class="org.myweb.vo.Student"/>
<%
   if(request.getParameter("id") != null)
   {
     String id = request.getParameter("id");
     String sql = "SELECT `user`.realName , `user`.loginName , `user`.`password` , student.tel , student.address , student.grade FROM `user` , student WHERE `user`.id = ?";
     PreparedStatement state = db.getConn().prepareStatement(sql);
     state.setObject(1, id);
     ResultSet set = db.executeQuery(state);
     set.next();
     stu.setName(set.getString(1));
     stu.setLoginName(set.getString(2));
     stu.setPassword(set.getString(3));
     stu.setTel(set.getString(4));
     stu.setAddress(set.getString(5));
     stu.setGrade(set.getInt(6));
     stu.setId(id);
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
        	<h2>修改学生信息</h2>
        </section>
        
        <form class="form-inline" method="post" action="modifyStu">
        	
        	<div class="form-item">
        	    <section class="inner-item">
        		  <label>真实姓名：</label>
        		  <input type="text" class="form-control" name="realName" id="realName" value="<%=stu.getName()%>" readonly="readonly"/>
        		</section>
        	</div>
        	
        	<div class="form-item">
        	    <section class="inner-item">
        		  <label>学生ID：</label>
        		  <input type="text" class="form-control" name="id" id="id" value="<%=stu.getId()%>" readonly="readonly"/>
        		</section>
        	</div>
        	
        	<div class="form-item">
        	    <section class="inner-item">
        		  <label>登陆名：</label>
        		  <input type="text" class="form-control" name="loginName" id="loginName" data-toggle="tooltip" data-placement="right" title="建议使用学生姓名缩写作为默认登录名" value="<%=stu.getLoginName()%>"/>
        		</section>
        	</div>
        	
        	<div class="form-item">
        	    <section class="inner-item">
        		  <label>登陆密码：</label>
        		  <input type="text" class="form-control" name="pwd" value="<%=stu.getPassword()%>" />
        		</section>
        	</div>
        	
        	
        	<div class="form-item">
        	    <section class="inner-item">
        		  <label>电话号码：</label>
        		  <input type="text" class="form-control" name="tel" value="<%=stu.getTel()%>"/>
        		</section>
        	</div>
        	
        	<div class="form-item">
        	    <section class="inner-item">
        		  <label>家庭住址：</label>
        		  <input type="text" class="form-control" name="address" value="<%=stu.getAddress()%>"/>
        		</section>
        	</div>
        	
        	 <div class="form-item">
        	   	<section class="inner-item">
        		  <label>年级：</label>
        		  <select id="grade" name="grade" class="form-control" style="width: 195px;">
        		  <%
        		     switch(stu.getGrade())
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
	<script>
		$('#loginName').tooltip("hide")
	</script>
	</body>
</html>
