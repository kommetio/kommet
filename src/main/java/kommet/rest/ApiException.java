package kommet.rest;

/** Reports an API failure with its HTTP status and client-facing message. */
public class ApiException extends RuntimeException
{
	private static final long serialVersionUID = 1L;
	private final int status;

	/** Creates a failure response description. */
	public ApiException(int status, String message)
	{
		super(message);
		this.status = status;
	}

	/** Returns the HTTP status for this failure. */
	public int getStatus()
	{
		return status;
	}
}