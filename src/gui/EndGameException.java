package gui;

public class EndGameException extends Exception {

	private static final long serialVersionUID = 1L;
	private String message = "Bag is empty, the game is over";
	
	public EndGameException() {
		super();
	}
	
	@Override
	public String getMessage() {
		return message;
	}
}
