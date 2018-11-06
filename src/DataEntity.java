public class DataEntity {
    public enum dataType {
        _Boolean, _Integer, _Decimal, _Text
    }

    public enum interfaceMode {
        internal, external
    }

    private String name;
    private dataType type;
    private interfaceMode source;
    private interfaceMode destination;

    public DataEntity(String deName, dataType deType) {
        name = deName;
        type = deType;
        source = interfaceMode.internal;
        destination = interfaceMode.internal;
    }
}
