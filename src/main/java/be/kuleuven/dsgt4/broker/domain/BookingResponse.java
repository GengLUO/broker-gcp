package be.kuleuven.dsgt4.broker.domain;

public class BookingResponse {
    private String transactionId;
    private boolean success;

    // Constructors
    public BookingResponse() {}

    public BookingResponse(String transactionId, boolean success) {
        this.transactionId = transactionId;
        this.success = success;
    }

    // Getters and Setters
    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
}
