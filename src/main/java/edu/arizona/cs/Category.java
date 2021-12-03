public class Category {
    private String catName;
    private String parentTitleName;

    public Category(String parentName, String catName){
        parentTitleName = parentName;
        this.catName = catName;
    }

    public String getCatName() {
        return catName;
    }

    public String getParentTitleName() {
        return parentTitleName;
    }
}