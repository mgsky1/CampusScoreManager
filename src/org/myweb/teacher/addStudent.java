package org.myweb.teacher;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.myweb.dao.Dao;

import com.opensymphony.xwork2.ActionSupport;

public class addStudent extends ActionSupport {
	private String realName;
	private String loginName;
	private String pwd;
	private String tel;
	private String address;
	private int grade;
	private Dao db = new Dao();

	public String getRealName() {
		return realName;
	}

	public String getLoginName() {
		return loginName;
	}

	public String getPwd() {
		return pwd;
	}

	public String getTel() {
		return tel;
	}

	public String getAddress() {
		return address;
	}

	public int getGrade() {
		return grade;
	}

	public void setRealName(String realName) {
		this.realName = realName;
	}

	public void setLoginName(String loginName) {
		this.loginName = loginName;
	}

	public void setPwd(String pwd) {
		this.pwd = pwd;
	}

	public void setTel(String tel) {
		this.tel = tel;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public void setGrade(int grade) {
		this.grade = grade;
	}

	public String execute() {
		try {
				PreparedStatement state0 = db
						.getConn()
						.prepareStatement(
								"insert into user(realName,password,priviledge,loginName) values (?,?,0,?)");
				state0.setObject(1, this.realName);
				state0.setObject(2, this.pwd);
				state0.setObject(3, this.loginName);

				int isSuucess1 = db.executeUpdate(state0);
				String id = null;

				PreparedStatement state1 = db.getConn().prepareStatement(
						"select max(id) from user");
				ResultSet set = db.executeQuery(state1);
				while (set.next()) {
					id = set.getString(1);
				}

				PreparedStatement state2 = db
						.getConn()
						.prepareStatement(
								"insert into student(id,tel,address,grade) values(?,?,?,?)");
				state2.setObject(1, id);
				state2.setObject(2, this.tel);
				state2.setObject(3, this.address);
				state2.setObject(4, this.grade);

				int isSuccess2 = db.executeUpdate(state2);

				if (isSuucess1 == 0 && isSuccess2 == 0) {
					return "SUCCESS";
				} else {
					addActionError("添加失败");
					return "FAILD";
				}
		} catch (Exception e) {
			addActionError("内部服务器错误");
			return "ERROR";
		}
	}
}
