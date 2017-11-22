package models;

import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;

import com.fasterxml.jackson.annotation.JsonIgnore;

import play.db.jpa.JPA;
import tools.Utils;

@Entity
@Table(name="account")
public class Account{
	@Transient
	@JsonIgnore
	public static final String MOBILE_PREFIX = "MOB_";
	
	@Transient
	@JsonIgnore
	public static final String PASSWORD_PREFIX = "PWD_";
	
	@Id
	@GeneratedValue	
	@JsonIgnore
	public long id;

	@Column(name="dial_code")
	public int dialCode;
	
	public String mobile;
	
	public String password;
	
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name="creation_datetime")
	public Date creationDateTime;	
	
	public String token;
	
	@OneToOne(fetch = FetchType.LAZY, mappedBy = "account", cascade = CascadeType.ALL)
	@LazyToOne(LazyToOneOption.NO_PROXY)
	public User user;
	
	public Account(){}
	
	public Account(int dialCode, String mobile, String password){
		this.dialCode = dialCode;
		this.mobile = mobile;
		this.password = Utils.md5(password);
		this.creationDateTime = new Date();
		this.token = Utils.genernateAcessToken(this.creationDateTime, this.mobile);
	}
	
	public static Account findByToken(String token){
		Account account = JPA.em().createQuery("from Account ac where ac.token = :token", Account.class)
		.setParameter("token", token).getSingleResult();
		return account;
	}
}
