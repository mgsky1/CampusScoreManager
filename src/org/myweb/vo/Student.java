package org.myweb.vo;
/*
 *学生实体类 
 * Written By 小远
 * 2016-12-07
 */
public class Student 
{
    private String id;
    private String name;
    private String tel;
    private String address;
    private String loginName;
    private String password;
    private int grade;
    
    public String getPassword() {
		return password;
	}
    
    public void setLoginName(String loginName) {
		this.loginName = loginName;
	}
    
    public void setName(String name) {
		this.name = name;
	}
    
    public void setId(String id) {
		this.id = id;
	}
    
    public void setAddress(String address) {
		this.address = address;
	}
    
    public void setGrade(int grade) {
		this.grade = grade;
	}
    
    public void setTel(String tel) {
		this.tel = tel;
	}
    
    public String getAddress() {
		return address;
	}
    
    public int getGrade() {
		return grade;
	}
    
    public String getId() {
		return id;
	}
    
    public String getTel() {
		return tel;
	}
    
    
    public String getName() {
		return name;
	}
    
    public String getLoginName() {
		return loginName;
	}
    
    public void setPassword(String password) {
		this.password = password;
	}
}
