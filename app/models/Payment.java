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
//	For Development
//	public static final String CLIENT_ID = "AZEqphfzbNIakHIzMtcMObJyk88_lsLjfufaUrrmPXFba_bDBf_moWLAubeR0d-nZl46P0D_-KsFpxrZ";
//	public static final String SECRET = "ELo8G3cbwPoWkTJYVmCp9rUvKLQRlfpKLzZgfpg53LS9KCxoWUjTym3rllXNVSu1KD4XC6jfzoFjsnCf";
	
//	For Production
	public static final String PAYPAL_CLIENT_ID = "AQRIvu7V8JzNVvDMbNSUF-44o1bKaC8Be5XsqaOLqB3MVx49euFox4WagStIMNGhG0WSv0zMgTxItykN";
	public static final String PAYPAL_SECRET = "ECbzJtDJf679QgpTCYB5vm8e0y1EsZTFQL1KxeLAgEetKO-8MdQ_3hqom-_Fai5JbBLI0ZjLlCej_YnZ";
	
	public static final String ALIPAY_GATEWAY_NEW = "https://mapi.alipay.com/gateway.do?";
	public static final String ALIPAY_PARTNER_ID = "2088621726632683";
	public static final String ALIPAY_CODE = "tgonu410228f8gjbju33hpf2mcu2v0pn";
	
	@Id
	@GeneratedValue
	@JsonIgnore
	public long id;
	
	@Column(name="transaction_id")
	public String transactionId;
	
	public String status;
	
	public String method;
	
	public String amount;
	
	public String currency;
	
	@Column(name="notify_id")
	public String notifyId;
	
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name="pay_datetime")
	public Date payDatetime;
	
	@ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "user_id") 
    public User user;
	
	@ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "lesson_id") 
	public Lesson lesson;
	
	public Payment(){}
	
	public Payment(User user, Lesson lesson){
		this.user = user;
		this.lesson = lesson;
		this.payDatetime = new Date();
	}
	
	
}





















