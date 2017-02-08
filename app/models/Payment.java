package models;

import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "payment")
public class Payment {
	public static final String ACCOUNT_NUM = "901338972";
	public static final String PRIVATE_KEY = "4FBA6BED-D519-4036-B595-8DF7C965A903";
	public static final String PUBLIC_KEY = "DE961933-6B42-4809-9BDF-07DA4F3881CD";
	
	public static final String CLIENT_ID = "AZEqphfzbNIakHIzMtcMObJyk88_lsLjfufaUrrmPXFba_bDBf_moWLAubeR0d-nZl46P0D_-KsFpxrZ";
	public static final String SECRET = "ELo8G3cbwPoWkTJYVmCp9rUvKLQRlfpKLzZgfpg53LS9KCxoWUjTym3rllXNVSu1KD4XC6jfzoFjsnCf";
	
	@Id
	@GeneratedValue
	@JsonIgnore
	public long id;
	
	@Column(name="transaction_id")
	public String transactionId;
	
	public String state;
	
	@Column(name="card_type")
	public String cardType;
	
	@Column(name="first_name")
	public String firstName;
	
	@Column(name="last_name")
	public String lastName;
	
	@Column(name="exp_month")
	public int expMonth;
	
	@Column(name="exp_year")
	public int expYear;
	
	@Column(name="card_number")
	public String cardNumber;
	
	public String amount;
	
	public String currency;
	
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name="pay_datetime")
	public Date payDatetime;
	
	@ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "user_id") 
    public User user;
	
	public Payment(){}
	
	public Payment(User user){
		this.user = user;
		this.payDatetime = new Date();
	}
}





















