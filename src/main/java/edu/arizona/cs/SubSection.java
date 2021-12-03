public class SubSection {
    private String parentTitleName;
    private String subSecName;

    public SubSection(String parent,String name){
        parentTitleName = parent;
        subSecName = name;
    }

    public String getParentTitleName() {
        return parentTitleName;
    }

    public String getSubSecName() {
        return subSecName;
    }
}