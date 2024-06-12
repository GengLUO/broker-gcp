package be.kuleuven.dsgt4.broker.domain;

import java.util.List;

public class BookingTransaction {
    private String transactionId;
    private String userId;
    private List<String> flightIds;
    private List<String> hotelIds;
    private String status;

    // Constructors
    public BookingTransaction() {}

    public BookingTransaction(String transactionId, String userId, List<String> flightIds, List<String> hotelIds, String status) {
        this.transactionId = transactionId;
        this.userId = userId;
        this.flightIds = flightIds;
        this.hotelIds = hotelIds;
        this.status = status;
    }

    // Getters and Setters
    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public List<String> getFlightIds() {
        return flightIds;
    }

    public void setFlightIds(List<String> flightIds) {
        this.flightIds = flightIds;
    }

    public List<String> getHotelIds() {
        return hotelIds;
    }

    public void setHotelIds(List<String> hotelIds) {
        this.hotelIds = hotelIds;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
