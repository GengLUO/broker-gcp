package be.kuleuven.dsgt4.broker.domain;

public class Customer {

    private String name;
    private String passportNumber;
    private String phone;
    private String birthdate;
    private String citizenship;
    private String desiredSourceCountry;
    private String desiredDestinationCountry;

    public Customer(String name, String passportNumber, String phone, String birthdate, String citizenship,
                    String desiredSourceCountry, String desiredDestinationCountry) {
        this.name = name;
        this.passportNumber = passportNumber;
        this.phone = phone;
        this.birthdate = birthdate;
        this.citizenship = citizenship;
        this.desiredSourceCountry = desiredSourceCountry;
        this.desiredDestinationCountry = desiredDestinationCountry;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassportNumber() {
        return passportNumber;
    }

    public void setPassportNumber(String passportNumber) {
        this.passportNumber = passportNumber;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getBirthdate() {
        return birthdate;
    }

    public void setBirthdate(String birthdate) {
        this.birthdate = birthdate;
    }

    public String getCitizenship() {
        return citizenship;
    }

    public void setCitizenship(String citizenship) {
        this.citizenship = citizenship;
    }

    public String getDesiredSourceCountry() {
        return desiredSourceCountry;
    }

    public void setDesiredSourceCountry(String desiredSourceCountry) {
        this.desiredSourceCountry = desiredSourceCountry;
    }

    public String getDesiredDestinationCountry() {
        return desiredDestinationCountry;
    }

    public void setDesiredDestinationCountry(String desiredDestinationCountry) {
        this.desiredDestinationCountry = desiredDestinationCountry;
    }
}
