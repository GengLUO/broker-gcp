package be.kuleuven.dsgt4.broker.domain;

public enum ItemType {
    FLIGHT("flight"),
    HOTEL("hotel");

    private final String type;

    ItemType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public static ItemType fromString(String text) {
        for (ItemType itemType : ItemType.values()) {
            if (itemType.type.equalsIgnoreCase(text)) {
                return itemType;
            }
        }
        throw new IllegalArgumentException("No enum constant " + ItemType.class.getCanonicalName() + " with text " + text);
    }
}
