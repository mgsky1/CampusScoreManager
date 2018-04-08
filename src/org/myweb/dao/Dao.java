package org.myweb.dao;
/*
 * 数据库操作类
 * Refference to csdn
 * Amend By 小远
 * 2016-12-07
 */
import java.sql.*;

public class Dao {
	    Connection con = null;  //MySQL连接对象
	    Statement stat = null;  //查询用的Statement对象
	    ResultSet rs = null;    //查询所得的结果集
	      
	 /*********构造方法,进行数据库连接**********/
	    public Dao() {  
	        try {  
	        Class.forName("com.mysql.jdbc.Driver");  
	        con = DriverManager.getConnection("jdbc:mysql://localhost:3308/smxy_class","root","root");  
	        stat = con.createStatement();  
	        } catch (Exception e) {  
	        // TODO: handle exception  
	        con = null;  
	        }  
	    }  
	  /*******查询方法，返回结果集*********/  
	    public ResultSet executeQuery(PreparedStatement state) {  
	      try {
			rs =  state.executeQuery();
		  } catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			rs = null;
		  }
	      return rs;
	    }  
	  /********删除，修改，增加*************/    
	    public int executeUpdate(PreparedStatement state) {  
	        try {  
	         state.executeUpdate();
	        return 0;  
	        } catch (Exception e) {  
	        // TODO: handle exception  
	        }  
	        return -1;  
	    } 
	    
	  /*****获得数据库连接*****/
	    public Connection getConn()
	    {
	    	return this.con;
	    }
}
