package gui;

@Deprecated
public class TokenException extends Exception {

	private static final long serialVersionUID = 7722696398091018463L;
	private String message = "Token is not present, operation is not allowed";
	
	public TokenException() {
		super();
	}
	
	@Override
	public String getMessage() {
		return message;
	}
}
