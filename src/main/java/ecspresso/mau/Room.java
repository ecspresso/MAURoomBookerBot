package ecspresso.mau;

public enum Room {
    G8249("G8:249", Building.G8),
    G8252("G8:252", Building.G8),
    G8253("G8:253", Building.G8),
    G8255("G8:255", Building.G8),
    G8256("G8:256", Building.G8),
    G8260("G8:260", Building.G8),
    // G8262("G8:262", Building.G8),
    // G8265("G8:265", Building.G8),
    NIA0301("NI:A0301", Building.NIAGARA),
    NIA0322("NI:A0322", Building.NIAGARA),
    NIA0401("NI:A0401", Building.NIAGARA),
    NIA0422("NI:A0422", Building.NIAGARA),
    NIA0515("NI:A0515", Building.NIAGARA),
    NIB0303("NI:B0303", Building.NIAGARA),
    NIB0305("NI:B0305", Building.NIAGARA),
    NIB0321("NI:B0321", Building.NIAGARA),
    NIC0301("NI:C0301", Building.NIAGARA),
    NIC0305("NI:C0305", Building.NIAGARA),
    NIC0306("NI:C0306", Building.NIAGARA),
    NIC0309("NI:C0309", Building.NIAGARA),
    NIC0312("NI:C0312", Building.NIAGARA),
    NIC0325("NI:C0325", Building.NIAGARA),
    NIC0401("NI:C0401", Building.NIAGARA);

    private final String name;
    private final Building building;

    Room(String name, Building building) {
        this.name = name;
        this.building = building;
    }

    @Override
    public String toString() {
        return name;
    }

    public Building getBuilding() {
        return building;
    }
}
