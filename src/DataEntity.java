import java.util.Comparator;
import java.util.List;

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

    public DataEntity(String deName, String deType) {
        dataType typeToAdd;
        if(deType.equals("Boolean"))
            typeToAdd = dataType._Boolean;
        else if (deType.equals("Integer"))
            typeToAdd = dataType._Integer;
        else if (deType.equals("Decimal"))
            typeToAdd = dataType._Decimal;
        else
            typeToAdd = dataType._Text;
        name = deName;
        type = typeToAdd;
        source = interfaceMode.internal;
        destination = interfaceMode.internal;
    }

    public DataEntity(String deName, String deType, String inputSource, String routing) {
        this(deName, deType);
        if(inputSource.equals("external"))
            source = interfaceMode.external;
        if(routing.equals("external"))
            destination = interfaceMode.external;
    }

    public boolean equals(DataEntity de) {
        return name.equals(de.name);
    }

    public String getName() {return name;}
    public interfaceMode getSource() {return source;}
    public interfaceMode getDestination() {return destination;}

    public static boolean isMember(DataEntity de, List<DataEntity> deList) {
        if(deList.size() > 0)
            for(DataEntity deMember : deList)
                if(deMember.equals(de))
                    return true;
        return false;
    }

    public static Comparator<DataEntity> comparator = new Comparator<>() {
        @Override
        public int compare(DataEntity o1, DataEntity o2) {
            return o1.getName().compareTo(o2.getName());
        }
    };
}
