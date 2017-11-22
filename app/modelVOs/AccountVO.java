package modelVOs;

import models.Account;
import models.Role;

public class AccountVO {
	public long accountId;
	public String token;
	public int dialCode;
	public String mobile;
	public Role role;
	public String username;
	public String realName;
	
	public AccountVO(Account account){
		this.accountId = account.id;
		this.token = account.token;
		this.dialCode = account.dialCode;
		this.mobile = account.mobile;
		if(account.user != null){
			this.role = account.user.role;
			this.username = account.user.username;
			this.realName = account.user.realName;
		}
	}

}
